package org.lcsim.hps.recon.tracking.kalman.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * A class to hold one record within the primitive 
 * RunTimeConfiguration utility.
 *
 *@author $Author: jeremy $
 *@version $Id: ToyConfigRecord.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */

//
// This class holds a single record from the configuration file.
// It parses the record, checks for internal consistency and provides
// accesors for information in the record.
// 
// Notes:
// 1) Supported types:
//    String, int, double, boolean.
//    List<String>, List<Integer>, List<Double>, List<Boolean>.
//    Note that scalar types are primitives but List types are objects.
//
// 2) Supports empty lists and empty strings.
//
//
// Work list:
// 1) Is List the right interface for the return values?
// 2) Is it aproblem that I return int for scalars and Integer for integer lists?
//    Similarly for boolean and double? Should I make it symmetric?
// 3) Should I make copy and assignment c'tors private or non-existant?
//    Needed in C++ what about Java?
// 4) Rename toString so that it can throw?
// 5) Do the type conversion in c'tor so that we fail immediately
//    and not later on when someone tries to read the value.
// 6) Add extra accessors to return other arrays as an option to Lists.
// 7) Add accessor to return a scalar as a list of length 1??
// 8) 
//

public class ToyConfigRecord {
    
    // Constructor.
    public ToyConfigRecord( String record ) throws ToyConfigException{
	this.record = record;
	Parse();
    }


    /**
     * Returns a copy of the input record as it was found in the input file
     * but with line breaks removed.
     *
     * @return A copy of the raw record.
     */
    public String getRecord(){
	return record;
    }

    /**
     * Returns the type field of this record.
     * @return The type.
     */
    public String getType (){
	return Type;
    }

    /**
     * Returns the variable name field of this record.
     * @return The variable name.
     */
    public String getName (){
	return Name;
    }

    /**
     * Returns the comment field, if any, of this record.
     * @return The comment, if any, that is part of this record.
     */
    public String getComment(){
	return comment;
    }

    /**
     * Returns true if the record contains nothing more than a comment or if the
     * the record is blank.
     *
     * @return true if the record is a pure comment or is blank; false otherwise.
     */
    public boolean isCommentOrBlank(){
	return isCommentOrBlank;
    }

    // Accessors to return supported data types.

    /**
     * Return the value as string.  This will work for any data type.
     * @return The value as a string.
     */
    public String getString (){
	return Values.get(0);
    }

    /**
     * Return the value as an int.  Only works for recrods that are of type int.
     *
     * @return The value of an int record.
     */
    public int getInt () throws ToyConfigException{
	CheckType("int");
	try{
	    return Integer.valueOf(Values.get(0));
	} catch(NumberFormatException e){
	    throw new ToyConfigException("Cannot parse this value as an integer: "
				      + record );
	}
    }

    /**
     * Return the value as an double.  Only works for records that are of type double.
     * 
     * @return The value of a double record.
     */
    public double getDouble () throws ToyConfigException{
	CheckType("double");
	double x=0.;
	try{
	    x = Double.valueOf(Values.get(0));
	} catch(NumberFormatException e){
	    throw new ToyConfigException("Cannot parse this value as a double: "
				      + record );
	}
	return x;
    }

    /**
     * Return the value as a boolean.  Only works for records that are of type boolean.
     * 
     * @return The value of a boolean record.
     */
    public boolean getBoolean() throws ToyConfigException {
	CheckType("boolean");
	boolean b=false;
	try{
	    b = Boolean.valueOf(Values.get(0));
	} catch(NumberFormatException e){
	    throw new ToyConfigException("Cannot parse this value as a boolean: "
				      + record );
	}
	return b;
    }

    /**
     * Return the value as a list of strings.  Works for all record types.
     *
     * @return The value of a the record.
     */
    // Can return any type of list as a list of strings.
    public List<String> getListString() throws ToyConfigException{
	AnyList();
	return Values;
    }

    /**
     * Return the value as a List<Integer>.  Only works for records that are of type List<Integer>.
     * 
     * @return The value of a List<int> record.
     */
    public List<Integer> getListInteger() throws ToyConfigException {
	CheckType("List<int>");
	List<Integer> l = new ArrayList<Integer>();
	for ( String v : Values ){
	    Integer I =0;
	    try{
		I = Integer.valueOf(v);
	    } catch(NumberFormatException e){
		throw new ToyConfigException("Cannot parse this value as a List<int>: "
					  + record );
	    }
	    l.add(I);
	}
	return l;
    }

    /**
     * Return the value as a List<Double>.  Only works for records that are of type List<Double>.
     * 
     * @return return the value of a List<double> record.
     */
    public List<Double> getListDouble() throws ToyConfigException {
	CheckType("List<double>");
	List<Double> l = new ArrayList<Double>();
	for ( String v : Values ){
	    Double D = 0.;
	    try{
		D = Double.valueOf(v);
	    } catch(NumberFormatException e){
		throw new ToyConfigException("Cannot parse this value as a List<double>: "
					  + record );
	    }
	    l.add(D);
	}
	return l;
    }

    /**
     * Return the value as a List<Boolean>.  Only works for records that are of type List<Boolean>.
     * 
     * @return the value of a List<boolean> record.
     */
    public List<Boolean> getListBoolean() throws ToyConfigException {
	CheckType("List<boolean>");
	List<Boolean> l = new ArrayList<Boolean>();
	for ( String v : Values ){
	    Boolean D = false;
	    try{
		D = Boolean.valueOf(v);
	    } catch(NumberFormatException e){
		throw new ToyConfigException("Cannot parse this value as a List<boolean>: "
					  + record );
	    }
	    l.add(D);
	}
	return l;
    }

    /**
     * 
     * Format the record as a string with standard spacing, ignoring the spacing
     * on the input line. If the data are strings, then enclose each string in 
     * quotes, even if it has no embedded spaces.
     *
     * 
     * @return A formatted copy of the record.
     */
    // I would like this to throw but it cannot since it overrides a method of the base 
    // class "Object" that does not throw an exception.
    public String toString() {
	if ( isCommentOrBlank ){
	    return comment;
	}
	StringBuffer s = new StringBuffer(Type);
	s.append(" ");
	s.append(Name);
	s.append(" = ");
	try{
	    if ( isList ) {
		s.append("{ ");
		if ( Type.equals("List<int>") ){
		    boolean first = true;
		    for ( Integer I : getListInteger() ){
			if( !first ){
			    s.append(", ");
			} else{
			    first = false;
			}
			s.append(I.toString());
		    }
		} else if ( Type.equals("List<double>")){
		    boolean first = true;
		    for ( Double D : getListDouble() ){
			if( !first ){
			    s.append(", ");
			} else{
			    first = false;
			}
			s.append(D.toString());
		    }
		} else if ( Type.equals("List<boolean>")){
		    boolean first = true;
		    for ( Boolean B : getListBoolean() ){
			if( !first ){
			    s.append(", ");
			} else{
			    first = false;
			}
			s.append(B.toString());
		    }
		} else {
		    boolean first = true;
		    for ( String B : getListString() ){
			if( !first ){
			    s.append(", \"");
			} else{
			    s.append("\"");
			    first = false;
			}
			s.append(B.toString());
			s.append( "\"");		}
		}
		s.append(" }");
	    }else{
		if ( Type.equals("int") ){
		    try{
			Integer I = getInt();
			s.append(I.toString());
		    }catch(ToyConfigException e){
			s.append("???");
		    }
		} else if ( Type.equals("double")){
		    try{
			Double D = getDouble();
			s.append(D.toString());
		    } catch (ToyConfigException e){
			s.append("???");
		    }
		} else if ( Type.equals("boolean")){
		    Boolean B = getBoolean();
		    s.append( B.toString());
		} else {
		    s.append( "\"");
		    s.append( getString() );
		    s.append( "\"");
		}
	    }
	}catch (ToyConfigException e) {
	    System.out.println (e.getMessage() );
	    s.append(" [Error formating this item], ");
	}
	s.append(";");
	return s.toString();
    }

    // Private instance data.

    // A copy of the record as it came in.
    // An external class does the concatenation of multiple line records into a single string.
    // Present implementation also strips comments - but that could change in the future.
    private String record;

    // Record with comments and enclosing white space stripped out.
    private String barerecord;

    // Comment field, including the // delimiter.
    private String comment = "";

    // Data type.
    private String Type = null;

    // Name of the datum.
    private String Name = null;

    // The value field - not yet parsed into components.
    private String Value = null;

    // The value field, parsed into components.
    private List<String> Values = null;

    // State data.
    private boolean isCommentOrBlank = false;
    private boolean isList           = false;

    // Private methods.

    /**
     * 
     * Parse this record, starting from its input string.
     * 
     */
    private void Parse () throws ToyConfigException{

	// Find comment delimiter if present.
	int islash = record.indexOf("//");

	// Extract comment, if any.
	if ( islash >= 0 ) comment = record.substring(islash);

	// Extract the part of the record that preceeds the comment.
	// Trim leading and trailing whitespace.
	String tmp = (islash < 0 ) ? record.trim() : record.substring(0,islash).trim();

	// Line is blank or contains only a comment.
	if ( tmp.length() == 0 ){
	    isCommentOrBlank = true;
	    return;
	}

	// Check for syntax of a complete record.
	if ( tmp.charAt(tmp.length()-1) != ';' ){
	    throw new ToyConfigException ("Not terminated by Semicolon: " + record);
	}

	// Strip the trailing semicolon and the leading and trailing whitespace.
	barerecord = tmp.substring(0,tmp.length()-1).trim();
	
	// Split the line into: type name = value;
	SplitLine();

	// Parse the value part of the record.
	ParseValue();

    }


    /**
     * 
     * Split this record into 3 fields: Type Name = Value; 
     * 
     */
    private void SplitLine() throws ToyConfigException{

	// Is there an equals sign with enough space before and after it?
	// The minimal line is:
	// t n=v
	// where,
	// t = type
	// n = name
	// v = value
	// the space between t and n is significant.
	// No embedded whitespace allowed within t or n.
	// So the first legal spot for the equals sign is:
	//   - must be at index 3 or greater
	//   - the first equals sign in the line must not be the last non-whitespace character in the line.
	// Remember that barerecord has leading and trailing spaces trimmed.
	int iequal = barerecord.indexOf("=");
	if ( iequal < 3 || iequal >= barerecord.length()-1){
	    throw new ToyConfigException( "Misplaced equals sign in record: " + record );
	}

	// The value part of the field, to be parsed elsewhere.
	Value = barerecord.substring(iequal+1).trim();
	
	// Extract type and name fields.
	String first = barerecord.substring(0,iequal);
	String [] tmp = first.split("[ \t]+");
	if ( tmp.length != 2 || Value.length() < 1 ){
	    throw new ToyConfigException( "Too many files in record: " + record );
	}
	Type = tmp[0];
	Name = tmp[1];

    }


    /**
     * 
     * Parse the Value part of the record.
     * 
     */
    private void ParseValue() throws ToyConfigException{

	// Check for a record that is a list.
	isList = ( Type.indexOf("List<") > -1 ) ? true: false;

	// Part of the string to parse.
	// Default is for non-lists.
	int iopen  = 0;
	int iclose = Value.length();

	// If this is a list, strip the enclosing {}.
	if ( isList ){
	    iopen  = Value.indexOf("{");
	    iclose = Value.lastIndexOf("}");
	    if ( ( iopen  < 0 ) ||
		 ( iclose < 0 ) ||
		 ( iclose < (iopen+1) ) ){
		throw new ToyConfigException( "Cannot parse record as a list: " +  record );
	    }
	    iopen = iopen + 1;
	} 

	// Remove {}, if present, and any leading and trailing whitespace.
	String listpart = Value.substring(iopen,iclose).trim();

	// Output of the parsing: one entry for each value in the list.
	Values = new ArrayList<String>();

	// Accumulate the next value in this variable.
	StringBuffer next = new StringBuffer();

	// Some predefined characters that hve special meaning.
	Character quote = Character.valueOf('\"');
	Character slash = Character.valueOf('\\');
	Character comma = Character.valueOf(',');

	// States:
	// 0: not within any value
	// 1: within a value that is not started by a quotation mark.
	// 2: within a value that is started by a quotation mark.
	// 3: into white space delimitation but have not yet found comma.
	// 10: last character was an escape, otherwise in state 0
	// 11: last character was an escape, otherwise in state 1
	// 12: last character was an escape, otherwise in state 2
	// 13: last character was an escape, otherwise in state 3

	int state=0;
	int i=-1;
	while (++i<listpart.length()){
	    
	    // Next Character, need both primitive and object representations.
	    char c = listpart.charAt(i);
	    Character C = c;

	    // If this character was escaped, add it to the next field
	    // and drop out of escape mode.
	    if ( state > 9 ){
		next.append(c);
		state = state - 10;
		continue;

	    } 
	    // Not within any list value.
	    else if ( state == 0 ){

		// Skip white space between tokens.
		if ( Character.isWhitespace(c) ) {
		    continue;

		} 

		// Starting a new item with a quote, strip the quote.
		else if ( C.compareTo(quote) == 0 ){
		    state = 2;
		    continue;

		} 

		// Starting a new item with a escape.
		else if ( C.compareTo(slash) == 0 ){
		    next.append(c);
		    state = 11;
		    continue;

		} 

		// Consecutive commas add an empty item to the list.
		// State stays at 0.
		else if ( C.compareTo(comma) == 0 && next.length() == 0 ){
		    Values.add("");
		    continue;

		} 
		// Starting a new item with neither escape nor quote.
		else {
		    next.append(c);
		    state = 1;
		    continue;
		}

	    } 

	    // In the middle of a field not started by a quote.
	    else if ( state == 1 ) {

		// End of item is marked by white space
		if ( Character.isWhitespace(c) ){
		    state = 3;
		    continue;
		    
		} 

		// End of item marked by comma without preceeding whitespace.
		else if ( C.compareTo(comma) == 0 ){
		    Values.add(next.toString());
		    next = new StringBuffer();
		    state = 0;
		    continue;
		    
		} 

		// Do not allow an unescaped quote in mid word ...
		else if ( C.compareTo(quote) == 0 ){
		    throw new ToyConfigException( "Unexpected \" character in record: " + record );
		} 

		// Next character is to be escaped.
		else if ( C.compareTo(slash) == 0 ) {
		    next.append(c);
		    state = 11;
		    continue;

		} 

		// Not a special character, just add it to the string.
		else{
		    next.append(c);
		    continue;
		}

	    } 

	    // In the middle of a field started by a quote.
	    else if ( state == 2 ){


		// Terminal quote marks the end of an item.
		if ( C.compareTo(quote) == 0 ){
		    state = 3;

		} 

		// Escape the next character.
		else if ( C.compareTo(slash) == 0 ) {
		    next.append(c);
		    state = 12;
		    continue;

		} 

		// Add character. Comma and white space are normal characters in this case.
		else {
		    next.append(c);
		    continue;
		}

	    } 

	    // Finished with a field but have not yet seen a comma or end of record.
	    else if ( state == 3 ){

		// Skip white space between fields.
		if ( Character.isWhitespace(c) ) {
		    continue;

		} 
		// Add the previous item to the output list.
		else if ( C.compareTo(comma) == 0 ) {
		    Values.add(next.toString());
		    next = new StringBuffer();
		    state = 0;
		    continue;
		}
	    } 

	    // On a legal record there is no way to reach this else.
	    else{
		throw new ToyConfigException("Confused state while parsing record: " + record );

	    }// end main branch, starting with: "if ( state > 9 )"

	} // end loop over characters


	// Record ended with an unterminated quote
	if ( state == 2 || state == 12 ){
	    throw new ToyConfigException("Unclosed quotes in this record: " + record );

	// Treat an end of record as the trailing delimiter for the last field.
	} else if ( state == 1 || state == 3 ){
	    Values.add(next.toString());

	// Last non-blank item was a non-quoted comma.
	// So add a blank item to the list.
	} else if ( isList && state ==0 ){
	    if ( Values.size() > 0 ){
		Values.add("");
	    }
	} 

	// On a legal record there is no way to reach this else.
	else{
	    throw new ToyConfigException("Confused final state while parsing: " + record );
	}

	// For a scalar record, make sure that there was exactly 1 item.
	if ( !isList ){
	    if( Values.size() != 1 ){
		throw new ToyConfigException("Too many values for a scalar type  record: " + record );
	    }
	}

    }
 

    /**
     * 
     * Check that the type of the current record matches the specified type.
     *
     */
    private void CheckType( String s) throws ToyConfigException{
	if ( Type.compareTo(s) !=0 ){
	    throw new ToyConfigException("Requested type (" + s + ") does not match record: "
				      + record );
	}
    }

    /**
     * 
     * Check that the type of the current record is one of the List types.
     *
     */
    private void AnyList() throws ToyConfigException{
	if ( Type.substring(0,5).compareTo("List<") != 0 ){
	    throw new ToyConfigException("Requested a list for a non-list record: "
				      + record );
	}
    }



}
