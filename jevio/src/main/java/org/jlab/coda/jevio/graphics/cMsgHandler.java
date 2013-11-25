package org.jlab.coda.jevio.graphics;

import org.jlab.coda.cMsg.*;
import org.jlab.coda.jevio.*;

import java.nio.IntBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * This class handles all cMsg communications using a singleton pattern.
 * It is expecting each message to contain bytes corresponding to an evio
 * event in the byteArray field. It also looks for an xml format dictionary
 * in a String payload item called "dictionary" (case sensitive).
 * The endianness of the byte array is set in cmsg by the setByteArrayEndian
 * method and, of course, must be set by the sender.
 *
 * @author timmer
 * @date Oct 19, 2009
 */
public class cMsgHandler {

    /** Handle all cmsg communications with this object. */
    private cMsg cmsg;

    /** Uniform Domain Locator (UDL) used to specify cmsg server to connect to. */
    private String udl;

    /** Subject to subscribe to for receiving evio event filled messages. */
    private String subject;

    /** Type to subscribe to for receiving evio event filled messages. */
    private String type;

    /** Handle cmsg subscription with this object. */
    private cMsgSubscriptionHandle handle;

    /** Callback to run when receiving a message. */
    private myCallback callback;

    /** Queue of received EvioEvent objects (parsed cMsg messages). */
    private BlockingQueue<EvioEvent> eventQueue;

    /** Last message to be retrieved from the queue after a new connection is made. */
    private cMsgMessage lastRetrievedMessage;

    /** Last xml string to be loaded as a dictionary from a message. */
    private String lastDictionaryLoaded;

    /**
     * This class defines the callback to be run when a message matching
     * our subscription arrives.
     */
    private class myCallback extends cMsgCallbackAdapter {
        /**
         * Callback method definition.
         * @param msg        message received from cmsg server
         * @param userObject object passed as an argument which was set when the client
         *                   orginally subscribed to a subject and type of message.
         */
        public void callback(cMsgMessage msg, Object userObject) {
            // check to see if message may contain evio event (there is a byte array)
            byte[] data = msg.getByteArray();
            if (data == null) return;

            // decode messages into events & store on the Q if there is room,
            // else it disappears
            extractEvents(msg);
        }
    }

    /**
     * Singleton handler.
     */
    private static cMsgHandler handler = new cMsgHandler();

    /**
     * Returns the handler <code>cMsgHandler</code> object.
     *
     * @return the singleton cmsg handler.
     */
    public static cMsgHandler getInstance() {
        return handler;
    }

    /**
     * Constructor.
     */
    private cMsgHandler() {
        // create cmsg callback to handle incoming messages
        callback = new myCallback();
        // create list to hold 1000 incoming events
        eventQueue = new LinkedBlockingQueue<EvioEvent>(1000);
    }

    /**
     * Get current subscription's subject.
     * @return current subscription's subject.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Get current subscription's type.
     * @return current subscription's type.
     */
    public String getType() {
        return type;
    }

    /**
     * Disconnect from the cmsg server that is currently connected to.
     *
     * @throws cMsgException
     */
    public void disconnect() {
        if (cmsg == null || !cmsg.isConnected()) {
            return;
        }

        try {
            cmsg.disconnect();
        }
        catch (cMsgException e) {
            // if this fails it's disconnected anyway
        }

        // each new connection means resubscribing
        handle  = null;
        subject = null;
        type    = null;

        return;
    }

    /**
     * Connect to the specified cmsg server.
     *
     * @param udl UDL used to specify a cmsg server to connect to
     * @throws cMsgException
     */
    public void connect(String udl) throws cMsgException {
        if (cmsg == null) {
            // must create a unique cmsg client name
            String name = "evioViewer_" + System.currentTimeMillis();
            String descr = "evio event viewer";
            cmsg = new cMsg(udl, name, descr);
            // store udl
            this.udl = udl;
        }

        // if we're already connected ...
        if (cmsg.isConnected()) {
            // if to same server, just return
            if (udl.equals(this.udl)) {
                return;
            }
            // otherwise disconnect from old server, before reconnecting to new one
            else {
                cmsg.disconnect();
            }
        }

        // if using new udl, recreate cmsg object
        if (!udl.equals(this.udl)) {
            String name = "evioViewer_" + System.currentTimeMillis();
            String descr = "evio event viewer";
            cmsg = new cMsg(udl, name, descr);
            // store udl
            this.udl = udl;
        }

        // connect to cmsg server
        cmsg.connect();

        // allow receipt of messages
        cmsg.start();

        // each new connection means resubscribing
        subject = null;
        type = null;

        return;
    }

    /**
     * Subscribe to the given subject and type.
     * If no connection to a cmsg server exists, nothing is done.
     * If an identical subscription already exists nothing is done.
     * If an older subscription exists, it is replaced by the new one.
     *
     * @param subject subject to subscribe to.
     * @param type type to subscribe to.
     * @return <code>true</code> if the subscription exists or was made, else <code>false</code>.
     * @throws cMsgException
     */
    public boolean subscribe(String subject, String type) throws cMsgException {
        // can't subscribe without connection or with null args
        if (cmsg == null || !cmsg.isConnected() ||
            subject == null || type == null ||
            subject.length() < 1 || type.length() < 1) {
            handle = null;
            return false;
        }
        // already subscribed to this subject & type
        else if (subject.equals(this.subject) && type.equals(this.type)) {
            return true;
        }

        // only want 1 subscription at a time for receiving evio messages
        if (handle != null) {
            try {
                cmsg.unsubscribe(handle);
            }
            catch (cMsgException e) { }
        }

        handle = cmsg.subscribe(subject, type, callback, null);
        this.subject = subject;
        this.type = type;

        return true;
    }

    /**
     * Take a cMsg message and extract any evio events in it
     * and place it on the event queue. If there is no room on
     * the queue, the event disappears.
     *
     * @return evio event from the queue or null if none.
     */
    private void extractEvents(cMsgMessage msg) {
        try {
            EvioEvent ev;
            ByteBuffer buf = ByteBuffer.wrap(msg.getByteArray());
//            ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
//            if (msg.getByteArrayEndian() == cMsgConstants.endianLittle) {
//System.out.println("cMsgHandler: set byte order to little endian");
//                byteOrder = ByteOrder.LITTLE_ENDIAN;
//            }
//            buf.order(byteOrder);
//            System.out.println("extract Events:");
//            IntBuffer ibuf = buf.asIntBuffer();
//            for (int i=0; i < 8; i++) {
//                System.out.println("  Buf(" + i + ") = " + Integer.toHexString(ibuf.get(i)));
//            }
            EvioReader reader = new EvioReader(buf);
            String dictionary = reader.getDictionaryXML();

            while ( (ev = reader.parseNextEvent()) != null) {
                ev.setDictionaryXML(dictionary);
//System.out.println("EV:\n" + ev.toXML());
                eventQueue.offer(ev);
            }
        }
        catch (IOException e) {
            // data in wrong format so try next msg
        }
        catch (EvioException e) {
            // data in wrong format so try next msg
        }
    }

    /**
     * Get next evio event from the queue. If there is no properly formatted message
     * on the queue and one does not appear within .1 seconds, null is returned.
     *
     * @return evio event from the queue or null if none.
     */
    public EvioEvent getNextEvent() {
        try {
            return eventQueue.poll(100, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) { }
        return null;
    }

    /**
     * Clear the entire message and event queues - all events.
     */
    public void clearQueue() {
        eventQueue.clear();
    }

    /**
     * Clear the specified number of events in the queue.
     * @param numberToDelete number of queue events to delete
     */
    public void clearQueue(int numberToDelete) {              //TODO check changes
        if (numberToDelete <= eventQueue.size()) {
            for (int i=0; i < numberToDelete; i++) {
                eventQueue.poll();
            }
            return;
        }

        eventQueue.clear();
    }

    /**
     * Get the size (number of messages) of the event queue.
     *
     * @return size of the event queue.
     */
    public int getQueueSize() {
        return eventQueue.size();
    }

    /**
     * Get the last dictionary in xml string form to be loaded from a message.
     * @return the last dictionary in xml string form to be loaded from a message.
     */
    public String getDictionaryString() {
        return lastDictionaryLoaded;
    }

    /**
     * Set the dictionary to the one contained in the last retrieved event/message (if there is one).
     *
     * @return <code>true</code> if the dictionary was set, else <code>false</code>.
     */
    public boolean setDictionary() {  //TODO: change this way of setting dictionaries, to 1 w/ each event
        return setDictionary(lastRetrievedMessage);
    }

    /**
     * Given a message, set the dictionary to one contained in the message if there is one.
     *
     * @param msg message which may contain an xml dictionary
     * @return <code>true</code> if the dictionary was set, else <code>false</code>.
     */
    public boolean setDictionary(cMsgMessage msg) {  //TODO: change this way of setting dictionaries
        if (msg == null) {
            return false;
        }

        // look for dictionary info in message payload
        cMsgPayloadItem payloadItem = msg.getPayloadItem("dictionary");
        if (payloadItem == null) {
            return false;
        }

        try {
            String dictionary = payloadItem.getString();
            NameProvider.setProvider(NameProviderFactory.createNameProvider(dictionary));
            lastDictionaryLoaded = dictionary;
            return true;
        }
        catch (cMsgException e) {
        }

        return false;
    }

    /**
     * Get the byte order of the last retrieved event/message (
     * either {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}).
     * NOTE: sender of the event is responsible for correctly setting the
     * byte order.
     *
     * @return byte order of event in message,
     *        {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
     */
    public ByteOrder getByteOrder() {
        return getByteOrder(lastRetrievedMessage);
    }

    /**
     * Given a message, get the byte order of the contained event (
     * either {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}).
     * NOTE: sender of the event is responsible for correctly setting the
     * byte order.
     * 
     * @param msg message with contained event
     * @return byte order of event in message,
     *        {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
     */
    public ByteOrder getByteOrder(cMsgMessage msg) {
        int endian = msg.getByteArrayEndian();
        if (endian == cMsgConstants.endianLittle) {
            return ByteOrder.LITTLE_ENDIAN;
        }
        return ByteOrder.BIG_ENDIAN;
    }

}
