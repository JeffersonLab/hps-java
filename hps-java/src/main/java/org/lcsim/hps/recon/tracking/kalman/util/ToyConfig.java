package org.lcsim.hps.recon.tracking.kalman.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Main class in a primitive runtime configuration utility.
 * Parses the input file into records, holds a collection of
 * records and provides access by parameter name.  Provides
 * error checking of the global structure of the file and
 * for attempts to access non-existent parameters.
 *
 *@author $Author: jeremy $
 *@version $Id: ToyConfig.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

//
// Work list:
// 1) Should I make the copy c'tor and assignment operators private?
//    or is this even a sensible question in java?
// 2) The following record will fail to parse correctly.
//    String name = "//This is not a comment";
// 3) Catch exceptions??
// 4) Rename toString so that it can throw??

public class ToyConfig{

    /**
     * Manage creation of the singleton instance and access to it.
     *
     * @return The singleton instance of this class.
     */
    static public ToyConfig getInstance() throws ToyConfigException{
	if ( instance == null ){
	    instance = new ToyConfig();
	}
	return instance;
    }


    /**
     * Read the input file to populate this class.
     *
     * @return The singleton instance of this class.
     */
    private ToyConfig( ) throws ToyConfigException{
	ReadFile();
    }


    /**
     * Change the configuration file to be read. Gives a warning if
     * a configuration file has already been read.
     *
     */
    public static void setConfigFile( String file ){
	if ( instance == null ){
	    configfile = file;
	} else {
	    System.err.println("Configuration file already read;" +
			       " setConfigFile() will be ignored." );
	}
    }

    /**
     * Return a Set<String> containing all variable names found in
     * the configuration file.
     *
     * @return a Set<String> containing all variable names.
     */
    public Set<String> getAllNames(){
	return rmap.keySet();
    }

    // Accessors to named parameters, separated by data type.
    // All checking is done in the ToyConfigRecord class.


    /**
     * Get a specified parameter as a string.  Works for all record types.
     *
     * @return the value of the parameter.
     */
    public String getString ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getString();
    }

    /**
     * Get a specified parameter as a String, if not present in the file
     * return the value specified by the second argument.
     *
     * @return the value of the parameter as an String.
     */
    public String getString ( String name, String def ){
	String i=def;
	try{
	    ToyConfigRecord r = getRecord(name);
	    i = r.getString();
	} catch ( ToyConfigException e ){
	}
	return i;
    }

    /**
     * Get a specified parameter as a int.
     *
     * @return the value of the parameter as an int.
     */
    public int getInt ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getInt();
    }

    /**
     * Get a specified parameter as a int, if not present in the file
     * return the value specified by the second argument.
     *
     * @return the value of the parameter as an int.
     */
    public int getInt ( String name, int def ){
	int i=def;
	try{
	    ToyConfigRecord r = getRecord(name);
	    i = r.getInt();
	} catch ( ToyConfigException e ){
	}

	return i;
    }

    /**
     * Get a specified parameter as a double.
     *
     * @return the value of the parameter as an double.
     */
    public double getDouble ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getDouble();
    }

    /**
     * Get a specified parameter as a double, if not present in the file
     * return the value specified by the second argument.
     *
     * @return the value of the parameter as an double.
     */
    public double getDouble ( String name, double def ){
	double d=def;
	try{
	    ToyConfigRecord r = getRecord(name);
	    d = r.getDouble();
	} catch ( ToyConfigException e ){
	}

	return d;
    }


    /**
     * Get a specified parameter as a boolean.
     *
     * @return the value of the parameter as an boolean.
     */
    public boolean getBoolean ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getBoolean();
    }

    /**
     * Get a specified parameter as a boolean, if not present in the file
     * return the value specified by the second argument.
     *
     * @return the value of the parameter as an boolean.
     */
    public boolean getBoolean ( String name, boolean def ){
	boolean b=def;
	try{
	    ToyConfigRecord r = getRecord(name);
	    b = r.getBoolean();
	} catch ( ToyConfigException e ){
	}

	return b;
    }


    /**
     * Get a specified parameter as a List<String>. Works for all parameter types.
     *
     * @return the value of the parameter as a List<String>.
     */
    public List<String> getListString ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getListString();
    }


    /**
     * Get a specified parameter as a List<Integer>.
     *
     * @return the value of the parameter as a List<Integer>.
     */
    public List<Integer> getListInteger ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getListInteger();
    }

    /**
     * Get a specified parameter as an array int[].
     *
     * @return the value of the parameter as an array int[].
     */
    public int[] getArrayInt ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	List<Integer> l = r.getListInteger();

	// Copy to array format.
	int[] a = new int[l.size()];
	for ( int i=0; i<l.size(); ++i ){
	    a[i]=(l.get(i)).intValue();
	}
	return a;
    }


    /**
     * Get a specified parameter as a List<Double>.
     *
     * @return the value of the parameter as a List<Double>.
     */
    public List<Double> getListDouble ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getListDouble();
    }

    /**
     * Get a specified parameter as an array double[].
     *
     * @return the value of the parameter as an array double[].
     */
    public double[] getArrayDouble ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	List<Double> l = r.getListDouble();

	// Copy to array format.
	double[] a = new double[l.size()];
	for ( int i=0; i<l.size(); ++i ){
	    a[i]=(l.get(i)).doubleValue();
	}
	return a;
    }

    /**
     * Get a specified parameter as a List<Boolean>.
     *
     * @return the value of the parameter as a List<Boolean>.
     */
    public List<Boolean> getListBoolean ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	return r.getListBoolean();
    }

    /**
     * Get a specified parameter as an array boolean[].
     *
     * @return the value of the parameter as an array boolean[].
     */
    public boolean[] getArrayBoolean ( String name ) throws ToyConfigException{
	ToyConfigRecord r = getRecord(name);
	List<Boolean> l = r.getListBoolean();

	// Copy to array format.
	boolean[] a = new boolean[l.size()];
	for ( int i=0; i<l.size(); ++i ){
	    a[i]=(l.get(i)).booleanValue();
	}
	return a;
    }


    /**
     * Return a the parameter as a formatted string.
     *
     * @return a formatted copy of the requested parameter.
     */
    public String toString ( String name ){
	try { 
	    ToyConfigRecord r = getRecord(name);
	    return r.toString();
	} catch ( ToyConfigException e ){
	    return e.getMessage();
	}
    }


    /**
     * Return the complete record as s formatted string.
     *
     * @return a formatted copy of the record for the requested parameter.
     */
    public ToyConfigRecord getRecord ( String name ) throws ToyConfigException{
	ToyConfigRecord r = rmap.get(name);
	if ( r == null ) {
	    throw new ToyConfigException ("No such parameter: " + name 
				       + " in file: " + configfile );
	}
	return r;
    }

    /**
     * Return the name of the input file.
     *
     * @return name of the input file.
     */
    public String ToyConfigfile(){
	return configfile;
    }

    /**
     * Print a formatted copy of the full configuration file
     * to the specified output stream.
     *
     */
    public void printAll( PrintStream out){
	for ( ToyConfigRecord r : image ){
	    out.println( r.toString() );
	}
	out.println("");
    }

    /**
     * Print a formatted copy of the full configuration file
     * to System.out.
     *
     */
    public void printAll( ){
	printAll(System.out);
    }

    // Private instance data.

    // Access to the configuration data, keyed by parameter name.
    Map<String,ToyConfigRecord> rmap = null;

    // Access to the info in rmap in the order in which the records were present
    // in the input file.
    List<ToyConfigRecord> image = null;

    // Name of the configuration file, initialized to its default value.
    private static String configfile = "runtime.conf";

    // Singleton instance.
    private static ToyConfig instance = null;

    // Private methods, other than the constructor.


    /**
     * Read the input file, break it into records.
     * Keep a copy of the input file in the orginal record order.
     * Create a map to access records by parameter name.
     *
     */
    private void ReadFile() throws ToyConfigException{

	rmap  = new HashMap<String,ToyConfigRecord>();
	image = new ArrayList<ToyConfigRecord>();

	try {
	    FileReader fr = new FileReader(configfile);
	    BufferedReader file = new BufferedReader(fr);

	    // Can make this a single loop.

	    String line;
	    while ( (line = file.readLine()) != null ){

		// Remove comments.
		line = StripComment(line);

		// Add extension lines if needed.
		if ( WantsExtension(line) ){

		    StringBuffer all = new StringBuffer(line.substring(0,line.length()));
		    String nextline;
		    while ( (nextline = file.readLine()) != null ){
			nextline = StripComment(nextline);
			if ( WantsExtension(nextline) ){
			    if ( nextline.length() != 0 ) 
			    all.append(nextline.substring(0,nextline.length()).trim());
			    line = all.toString();
			} else{
			    all.append(nextline.trim());
			    line = all.toString();
			    break;
			}
		    }
		}

		ToyConfigRecord r = new ToyConfigRecord(line);
		image.add(r);
		if ( !r.isCommentOrBlank() ) {
		    rmap.put(r.getName(),r);
		}
	    }
	    
	} catch (IOException e){
	    throw new ToyConfigException( "Error reading configuration file: "  
					  + configfile 
					  );
	}
    }

    /**
     * Test to see if this record is complete.
     * 
     * A valid end of line indicator is a semi-colon as the last non-blank character
     * before any comments.  Otherwise this record needs an extension.
     *
     * @return true if this record is incomplete and false if it is complete.
     */
    private boolean WantsExtension( String s){
	int icomment = s.indexOf("//");
	String line = ( icomment == -1 ) ? s.trim() : s.substring(0,icomment).trim();
	if ( line.length() == 0 ) return false;
	return ( !line.endsWith(";")) ;
    }

    /**
     * Remove, comments, trailing white space and leading whitespace input string.
     *  - a comment begins with // and continues for the rest of the line.
     *
     * This will give a wrong result on a line like:
     * String name = "//This is not supposed to be a comment"; 
     *
     * @return a copy of the input with comments and insignificant whitespace removed.
     */
    private String StripComment( String s){

	// Find comment delimiter if present.
	int islash = s.indexOf("//");

	if ( islash < 0 ){
	    return s.trim();
	}

	return s.substring(0,islash).trim();

    }

}
