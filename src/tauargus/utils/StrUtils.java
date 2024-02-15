/*
* Argus Open Source
* Software to apply Statistical Disclosure Control techniques
* 
* Copyright 2014 Statistics Netherlands
* 
* This program is free software; you can redistribute it and/or 
* modify it under the terms of the European Union Public Licence 
* (EUPL) version 1.1, as published by the European Commission.
* 
* You can find the text of the EUPL v1.1 on
* https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
* 
* This software is distributed on an "AS IS" basis without 
* warranties or conditions of any kind, either express or implied.
*/

package tauargus.utils;

//import java.io.File;
//import java.text.DecimalFormat;
//import java.text.DecimalFormatSymbols;
import java.util.Locale;
import tauargus.model.ArgusException;

public class StrUtils {
    public final static String QUOTE = "\"";
    
    /**
     * Gives the starting offset of a line within a string
     * @param str the string to be searched
     * @param lineNumber the linenumber (1-based) for which the starting offset is being searched
     * @return offset (zero-based) within the given string
     */
    public static int startingPosition(String str, int lineNumber) {
        int p = 0;
        for (int i = 0; i < lineNumber - 1; i++) {
            p = str.indexOf("\n", p) + 1;
        }
        return p;
    }

    /**
     * Gives the starting offset of a character within a string
     * @param str the string to be searched
     * @param lineNumber the linenumber (1-based) of the character for which the starting offset is being searched
     * @param position the position (1-based) within the line of the character for which the starting offset is being searched
     * @return offset (zero-based) within the given string
     */
    public static int startingPosition(String str, int lineNumber, int position) {
        return startingPosition(str, lineNumber) + position - 1;
    }
    
    public static String unQuote(String str) {
        int length = str.length();
        if (length > 1 && str.startsWith(QUOTE) && str.endsWith(QUOTE)) {
            return str.substring(1, length - 1);
        }
        return str;
    }
    
    public static String quote(String str) {
        return QUOTE + str + QUOTE;
    }
    
    public static double toDouble(String s) throws ArgusException {
        double d;

        s = s.trim();
        try {
            d = Double.parseDouble(s);
        } 
        catch (NumberFormatException ex) {
            throw new ArgusException("\"" + s + "\" is not numeric.");
        }
        return d;
    }

    public static int toInteger(String s) throws ArgusException {
        double d = toDouble(s);

        if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
            throw new ArgusException("\"" + s + "\" is too large to convert to integer.");
        }
        if (Math.abs(d - Math.round(d)) > 0.000000001) { 
            throw new ArgusException("\"" + s + "\" is not an integer.");
        }
        return (int)Math.round(d);
    }
    
    public static String formatDouble(double x, int d){
        return String.format(Locale.US, "%."+d+"f", x);
    }
    
    public static String replaceExtension(String fileName, String newExtension) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot != -1) {
            return fileName.substring(0, lastDot) + newExtension;
        } else {
            return fileName + newExtension;
        }
    }
    
    public static String timeToString(int s){
        String hs = ""; int h=0; int m=0;
        if (s >= 3600){
           h = s/3600;
           hs = " " + h+" hrs";
           s=s-h*3600;
        }
        if ((h>0) || (s>=60)){
           m=s/60; 
           hs = hs + " " + m + " min"; 
           s=s-m*60;
        }
        hs = hs + " " + s + " sec"; 
        return hs;  
    }
       
 }
