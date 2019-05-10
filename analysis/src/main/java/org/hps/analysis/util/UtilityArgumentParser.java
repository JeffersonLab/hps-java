package org.hps.analysis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.util.BashParameter;

/**
 * <code>UtilityArgumentParser</code> is designed to automate the
 * handling of command line argument parsing. It enables the user to
 * define what arguments are valid and the easily access whether or
 * not those arguments have been defined and, where applicable, what
 * their values are.
 * <br/><br/>
 * The utility allows for different types of arguments to be defined.
 * These include flag arguments which represent arguments that do
 * not accept values (i.e. <code>-h</code>), single-value arguments
 * which represent arguments that require a value but can only occur
 * once (i.e. <code>-R $RUN_NUMBER</code>), and multiple-value
 * arguments which represents arguments that take a value and can
 * occur more than once (i.e. <code>-i $INPUT_FILE_1 -i
 * $INPUT_FILE_2</code>).
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
class UtilityArgumentParser {
    private final String command;
    private List<Argument> arguments = new ArrayList<Argument>();
    private Map<String, Argument> optionMap = new HashMap<String, Argument>();
    
    /**
     * Instantiates a command line argument parser for the specified
     * command.
     * @param command - The command.
     */
    UtilityArgumentParser(String command) {
        this.command = command;
    }
    
    /**
     * Adds a new {@link org.hps.analysis.util.FlagArgument
     * FlagArgument} with the specified parameters to the set of
     * allowed command line arguments.
     * @param shortOption - Defines the short option text used to
     * declare this argument on the command line.
     * @param fullOption - Defines the full option text used to
     * declare this argument on the command line.
     * @param description - Defines the description text used for
     * displaying usage information.
     * @param required - Specifies whether or not the argument is
     * required for the associated command to run.
     */
    void addFlagArgument(String shortOption, String fullOption, String description, boolean required) {
        addArgument(new FlagArgument(shortOption, fullOption, description, required));
    }
    
    /**
     * Adds a new {@link org.hps.analysis.util.SingleValueArgument
     * SingleValueArgument} with the specified parameters to the set
     * of allowed command line arguments.
     * @param shortOption - Defines the short option text used to
     * declare this argument on the command line.
     * @param fullOption - Defines the full option text used to
     * declare this argument on the command line.
     * @param description - Defines the description text used for
     * displaying usage information.
     * @param required - Specifies whether or not the argument is
     * required for the associated command to run.
     */
    void addSingleValueArgument(String shortOption, String fullOption, String description, boolean required) {
        addArgument(new SingleValueArgument(shortOption, fullOption, description, required));
    }
    
    /**
     * Adds a new {@link org.hps.analysis.util.MultipleValueArgument
     * MultipleValueArgument} with the specified parameters to the
     * set of allowed command line arguments.
     * @param shortOption - Defines the short option text used to
     * declare this argument on the command line.
     * @param fullOption - Defines the full option text used to
     * declare this argument on the command line.
     * @param description - Defines the description text used for
     * displaying usage information.
     * @param required - Specifies whether or not the argument is
     * required for the associated command to run.
     */
    void addMultipleValueArgument(String shortOption, String fullOption, String description, boolean required) {
        addArgument(new MultipleValueArgument(shortOption, fullOption, description, required));
    }
    
    /**
     * Finds the argument, if it exists, which uses the specified
     * command line option text.
     * @param optionText - The command line text that corresponds to
     * the argument.
     * @return Returns the {@link org.hps.analysis.util.Argument
     * Argument} object that corresponds to the indicated command
     * line option text.
     * @throws RuntimeException Occurs if there is no argument that
     * corresponds to the specified command line argument text.
     */
    Argument getArgument(String optionText) {
        if(optionMap.containsKey(optionText)) {
            return optionMap.get(optionText);
        } else {
            throw new RuntimeException("Unrecognized option \"" + optionText + "\".");
        }
    }
    
    /**
     * Returns a BASH-formatted description of the valid command line
     * arguments.
     * @return Returns the description as a {@link java.util.String
     * String} object.
     */
    String getHelpText() {
        // Get the longest short option.
        int longestShortOption = 0;
        for(Argument arg : arguments) {
            if(arg.getShortOptionText() != null) { longestShortOption = Math.max(longestShortOption, arg.getShortOptionText().length()); };
        }
        
        // Get the longest full option.
        int longestFullOption = 0;
        for(Argument arg : arguments) {
            if(arg.getFullOptionText() != null) { longestFullOption = Math.max(longestFullOption, arg.getFullOptionText().length()); }
        }
        
        // Get the longest description.
        int longestDescription = 0;
        for(Argument arg : arguments) {
            if(arg.getDescription() != null) { longestDescription = Math.max(longestDescription, arg.getDescription().length()); }
        }
        
        // Output the command prompt command.
        StringBuffer textBuffer = new StringBuffer();
        textBuffer.append(BashParameter.format(command, BashParameter.TEXT_LIGHT_BLUE));
        textBuffer.append('\n');
        
        // Output each argument.
        for(Argument arg : arguments) {
            textBuffer.append(getFormattedArgumentText(arg, longestShortOption, longestFullOption, longestDescription));
            textBuffer.append('\n');
        }
        
        // Return the result.
        return textBuffer.toString();
    }
    
    /**
     * Specifies whether or not the argument corresponding to the
     * specified command line option text is defined or not.
     * @param optionText - The command line option text.
     * @return Returns <code>true</code> if the argument is defined
     * and <code>false</code> otherwise.
     */
    boolean isDefined(String optionText) {
        if(optionMap.containsKey(optionText)) {
            return optionMap.get(optionText).isDefined();
        } else { return false; }
    }
    
    /**
     * Parses the command line arguments.
     * @param args - The command line arguments.
     */
    void parseArguments(String[] args) {
        // Iterate over the options and store them in the appropriate
        // argument.
        for(int i = 0; i < args.length; i++) {
            // If the option is a valid option, parse it.
            if(optionMap.containsKey(args[i])) {
                // Get the argument.
                Argument arg = optionMap.get(args[i]);
                
                // How the argument is handled depends on its type.
                // Flag arguments are set to defined if the option is
                // seen anywhere at all.
                if(arg instanceof FlagArgument) {
                    ((FlagArgument) arg).setDefined(true);
                }
                
                // Single-value arguments can only set once. If there
                // are multiple instances, it causes an exception.
                if(arg instanceof SingleValueArgument) {
                    if(arg.isDefined()) {
                        throw new RuntimeException("Option \"" + arg.getShortOptionText() + "\" appears more than once, but does not allow for mutliple instances.");
                    } else {
                        ((SingleValueArgument) arg).setValue(args[++i]);
                    }
                }
                
                // Multiple-value arguments can appear as many times
                // as needed.
                if(arg instanceof MultipleValueArgument) {
                    ((MultipleValueArgument) arg).addValue(args[++i]);
                }
            }
            
            // Otherwise, through an exception.
            else {
                throw new RuntimeException("Unrecognized option \"" + args[i] + "\".");
            }
        }
    }
    
    /**
     * Creates an exception indicating that a required argument is
     * not defined.
     */
    void throwRequiredArgumentMissingError() {
        throw new RuntimeException("One or more required arguments are missing. See usage information for more details.");
    }
    
    /**
     * Checks that all of the required options are defined.
     * @throws RuntimeException Occurs if a required argument is not
     * defined.
     */
    boolean verifyRequirements() throws RuntimeException {
        // Iterate over the arguments. If there is an instance of a
        // required argument where no values have been defined, then
        // the requirements are not satisfied.
        for(Argument arg : arguments) {
            if(arg.isRequired() && !arg.isDefined()) {
                return false;
            }
        }
        
        // If all required arguments are defined, then the
        // requirements are satisfied.
        return true;
    }
    
    /**
     * Adds an argument to the set of valid arguments.
     * @param arg - The argument to add.
     */
    private void addArgument(Argument arg) {
        // Map the options to the argument object. If the option
        // already exists, throw an error.
        mapOption(arg.getFullOptionText(), arg, optionMap);
        mapOption(arg.getShortOptionText(), arg, optionMap);
        
        // Create the argument object.
        arguments.add(arg);
    }
    
    /**
     * Gets the BASH-formatted text that describes an argument.
     * @param arg - The argument.
     * @param shortLength - The length of the longest short version
     * of the command line option text that corresponds to the
     * argument.
     * @param fullLength - The length of the longest long version
     * of the command line option text that corresponds to the
     * argument.
     * @param descriptionLength - The length of the longest
     * description for an argument.
     * @return Returns a {java.util.String String} object that
     * describes the argument. It is formatted to display color in a
     * BASH prompt.
     */
    private static final String getFormattedArgumentText(Argument arg, int shortLength, int fullLength, int descriptionLength) {
        // Create a buffer to contain the argument text.
        StringBuffer outputBuffer = new StringBuffer("\t\t");
        
        // Process the short option.
        String shortText = String.format("%-" + shortLength + "s", arg.getShortOptionText() == null ? "" : arg.getShortOptionText());
        outputBuffer.append(BashParameter.format(shortText, BashParameter.TEXT_YELLOW, BashParameter.PROPERTY_BOLD));
        outputBuffer.append('\t');
        
        // Process the full option.
        String fullText = String.format("%-" + fullLength + "s", arg.getFullOptionText() == null ? "" : arg.getFullOptionText());
        outputBuffer.append(BashParameter.format(fullText, BashParameter.TEXT_YELLOW, BashParameter.PROPERTY_BOLD));
        outputBuffer.append('\t');
        
        // Process the description.
        String descriptionText = String.format("%-" + descriptionLength + "s", arg.getDescription() == null ? "" : arg.getDescription());
        outputBuffer.append(descriptionText);
        outputBuffer.append(' ');
        
        // Indicate whether the argument is mandatory or not.
        if(arg.isRequired()) {
            outputBuffer.append(BashParameter.format("[ REQUIRED ]", BashParameter.TEXT_RED, BashParameter.PROPERTY_BOLD));
        } else {
            outputBuffer.append(BashParameter.format("[ OPTIONAL ]", BashParameter.TEXT_LIGHT_GREY, BashParameter.PROPERTY_DIM));
        }
        
        // Return the resultant string.
        return outputBuffer.toString();
    }
    
    /**
     * Maps a command line option text to an argument.
     * @param text - The command line option text.
     * @param arg - The argument.
     * @param map - The map.
     */
    private static final void mapOption(String text, Argument arg, Map<String, Argument> map) {
        if(text == null) { return; }
        if(map.containsKey(text)) {
            throw new RuntimeException("Argument " + text + " is already defined.");
        } else {
            map.put(text, arg);
        }
    }
}