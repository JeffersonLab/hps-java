package org.hps.users.phansson;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpMatcherTester {

    public RegExpMatcherTester() {
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args) {
        System.out.println("pattern " + args[0]);
        System.out.println("string " + args[1]);
        
        Matcher m = Pattern.compile(args[0]).matcher(args[1]);
        
        if(m.matches()) {
            System.out.println("matches!");
            for(int i=0; i<m.groupCount(); ++i) System.out.println("group " + i + ": " + m.group(i) );
            for(int i=0; i<5; ++i) System.out.println("group " + i + ": " + m.group(i) );
        } else {
            System.out.println("NO match");
        }
        
        
    }

}
