package org.hps.users.kmccarty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class Buffer<E> implements Iterable<E> {
	private int itemsCount = 0;
	private final LinkedList<Collection<E>> buffer = new LinkedList<Collection<E>>();
	
	public Buffer(int size) {
		for(int i = 0; i < size; i++) {
			buffer.addLast(new ArrayList<E>(0));
		}
	}
	
	public void add(List<E> bufferEntry) {
		itemsCount = itemsCount - buffer.getFirst().size() + bufferEntry.size();
		buffer.removeFirst();
		buffer.addLast(bufferEntry);
	}
	
	public void clear() {
		buffer.clear();
		for(int i = 0; i < buffer.size(); i++) {
			buffer.addLast(new ArrayList<E>(0));
		}
	}
	
	public boolean contains(E item) {
		for(Collection<E> bufferEntry : buffer) {
			for(E element : bufferEntry) {
				if(item == null && element == null) {
					return true;
				} else if(item != null && item.equals(element)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean containsAll(Collection<E> items) {
		for(E item : items) {
			if(!contains(item)) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean isEmpty() {
		for(Collection<E> bufferEntry : buffer) {
			if(!bufferEntry.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Iterator<E> iterator() {
		return new BufferIterator();
	}
	
	public Iterator<Collection<E>> listIterator() {
		return buffer.iterator();
	}
	
	public int getBufferSize() {
		return buffer.size();
	}
	
	public int getItemCount() {
		return itemsCount;
	}
	
	public Collection<E> getOldest() {
		return buffer.getFirst();
	}
	
	private class BufferIterator implements Iterator<E> {
		private Iterator<Collection<E>> bufferIterator = buffer.iterator();
		private Iterator<E> listIterator = bufferIterator.next().iterator();
		
		@Override
		public boolean hasNext() {
			// If the current buffer entry has another element,
			// then there exists a next element.
			if(listIterator.hasNext()) {
				return true;
			}
			
			// Otherwise, search for a subsequent buffer entry
			// that does have a next element.
			else {
				// Iterate over buffer lists until either there
				// are no more buffer lists, or there is a list
				// that contains an element.
				while(bufferIterator.hasNext() && !listIterator.hasNext()) {
					listIterator = bufferIterator.next().iterator();
				}
				
				// At this point, either the current buffer entry
				// has a next element, or the end of the buffer
				// has been reached.
				return listIterator.hasNext();
			}
		}
		
		@Override
		public E next() {
			// If the current list iterator has another element,
			// this should be returned.
			if(listIterator.hasNext()) {
				return listIterator.next();
			}
			
			// Otherwise, the next element of the next buffer
			// entry should be returned, if possible.
			else {
				// Iterate over buffer lists until either there
				// are no more buffer lists, or there is a list
				// that contains an element.
				while(bufferIterator.hasNext() && !listIterator.hasNext()) {
					listIterator = bufferIterator.next().iterator();
				}
				
				// If the list iterator has a next element, it
				// should be returned.
				if(listIterator.hasNext()) {
					return listIterator.next();
				}
				
				// Otherwise, this is the end of the buffer.
				else {
					throw new NoSuchElementException();
				}
			}
		}
	}
}