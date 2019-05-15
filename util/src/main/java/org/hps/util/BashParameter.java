package org.hps.util;

public enum BashParameter {
    DEFAULT(0, true, true, true), PROPERTY_BOLD(1, true, false, false), PROPERTY_DIM(2, true, false, false), PROPERTY_UNDERLINE(4, true, false, false),
    PROPERTY_BLINK(5, true, false, false), PROPERTY_REVERSE(7, true, false, false), PROPERTY_HIDDEN(8, true, false, false),
    
    TEXT_BLACK(30, false, true, false), TEXT_RED(31, false, true, false), TEXT_GREEN(32, false, true, false), TEXT_YELLOW(33, false, true, false),
    TEXT_BLUE(34, false, true, false), TEXT_MAGENTA(35, false, true, false), TEXT_CYAN(36, false, true, false), TEXT_LIGHT_GREY(37, false, true, false),
    TEXT_DARK_GREY(90, false, true, false), TEXT_LIGHT_RED(91, false, true, false), TEXT_LIGHT_GREEN(92, false, true, false),
    TEXT_LIGHT_YELLOW(93, false, true, false), TEXT_LIGHT_BLUE(94, false, true, false), TEXT_LIGHT_MAGENTA(95, false, true, false),
    TEXT_LIGHT_CYAN(96, false, true, false), TEXT_LIGHT_WHITE(97, false, true, false),
    
    BACKGROUND_BLACK(40, false, false, true), BACKGROUND_RED(41, false, false, true), BACKGROUND_GREEN(42, false, false, true),
    BACKGROUND_YELLOW(43, false, false, true), BACKGROUND_BLUE(44, false, false, true), BACKGROUND_MAGENTA(45, false, false, true),
    BACKGROUND_CYAN(46, false, false, true), BACKGROUND_LIGHT_GREY(47, false, false, true), BACKGROUND_DARK_GREY(100, false, false, true),
    BACKGROUND_LIGHT_RED(101, false, false, true), BACKGROUND_LIGHT_GREEN(102, false, false, true), BACKGROUND_LIGHT_YELLOW(103, false, false, true),
    BACKGROUND_LIGHT_BLUE(104, false, false, true), BACKGROUND_LIGHT_MAGENTA(105, false, false, true), BACKGROUND_LIGHT_CYAN(106, false, false, true),
    BACKGROUND_WHITE(107, false, false, true);
    
    private final int code;
    private final boolean isProperty;
    private final boolean isTextColor;
    private final boolean isBackgroundColor;
    
    private BashParameter(int code, boolean isProperty, boolean isTextColor, boolean isBackgroundColor) {
        this.code = code;
        this.isProperty = isProperty;
        this.isTextColor = isTextColor;
        this.isBackgroundColor = isBackgroundColor;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getTextCode() {
        return ((char) 27) + "[" + Integer.toString(getCode()) + "m";
    }
    
    public boolean isProperty() {
        return isProperty;
    }
    
    public boolean isTextColor() {
        return isTextColor;
    }
    
    public boolean isBackgroundColor() {
        return isBackgroundColor;
    }
    
    public static final String format(String text, BashParameter... params) {
        // Only one text and background color, at most, are
        // allowed. Track whether more than one is seen.
        boolean sawTextColor = false;
        boolean sawBackgroundColor = false;
        
        // Buffer the formatted text.
        StringBuffer buffer = new StringBuffer();
        
        for(BashParameter param : params) {
            // If the default parameter is declared, it must be
            // the only parameters defined, since it overrides
            // all other parameters.
            if(param == BashParameter.DEFAULT && params.length != 1) {
                throw new IllegalArgumentException("Error: The default style parameter can not be employed in conjunction with other style parameters.");
            }
            
            // Only one text color may be defined at a time, as
            // subsequent colors will override the earlier ones.
            if(param.isTextColor()) {
                if(sawTextColor) {
                    throw new IllegalArgumentException("Error: Only one text color may be defined.");
                } else {
                    sawTextColor = true;
                }
            }
            
            // Only one background color may be defined at a
            // time, as subsequent colors will override the
            // earlier ones.
            if(param.isBackgroundColor()) {
                if(sawBackgroundColor) {
                    throw new IllegalArgumentException("Error: Only one background color may be defined.");
                } else {
                    sawBackgroundColor = true;
                }
            }
            
            // Append the full bash text style code to the
            // buffer.
            buffer.append(param.getTextCode());
        }
        
        // Add the text to the buffer, and then reset the style
        // to the default.
        buffer.append(text);
        buffer.append(BashParameter.DEFAULT.getTextCode());
        
        // Return the text.
        return buffer.toString();
    }
}