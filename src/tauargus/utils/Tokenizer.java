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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

public class Tokenizer {

    private static final Logger LOGGER = Logger.getLogger(Tokenizer.class.getName());
    
    private BufferedReader reader;
    private String line;
    private String value;
    private int lineNumber = 0;

    public Tokenizer(BufferedReader reader) {
        this.reader = reader;
    }

    // close underlying BufferedReader
    public void close(){
        try{
            this.reader.close();
        }
        catch (Exception ex) {}
    }
    
    public String nextLine() {
        do {
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                line = null;
            }
            line = StringUtils.replaceChars(line, '\t', ' ');
            line = StringUtils.strip(line);
            lineNumber++;
        } while (line != null && (line.equals("") || line.startsWith("//")));
        return line;
    }

    public String nextToken() {
        int beginIndex;
        int endIndex;
        int newBeginIndex;
        if (line.startsWith("//")) {
            value = "";
            line = "";
            return value;
        } else if (line.startsWith("\"")) {
            beginIndex = 1;
            endIndex = line.indexOf("\"", 1);
            newBeginIndex = endIndex + 1;
        } else if (line.startsWith(",") || line.startsWith("|") || line.startsWith("(") || line.startsWith(")")) {
            beginIndex = 0;
            endIndex = 1;
            newBeginIndex = 1;
        } else {
            beginIndex = 0;
            endIndex = line.indexOf(" ");
            newBeginIndex = endIndex + 1;
        }
        if (endIndex != -1) {
            value = line.substring(beginIndex, endIndex);
            line = StringUtils.strip(line.substring(newBeginIndex));
        } else {
            value = line.substring(beginIndex);
            line = "";
        }
        if (value.startsWith("<")) {
            value = value.toUpperCase();
        }
        return value;
    }
    
    public String nextField(String separator) {
        int index = line.indexOf(separator);
        if (index == -1) {
            value = line.trim();
            line = "";
        }
        else {
            value = line.substring(0, index).trim();
            line = line.substring(index + 1).trim();
        }
        int length = value.length();
        if (length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, length - 1);
        }
        return value;
    }
    public String nextChar() {
        if (line.length() == 1) {
            value = line;
            line = "";
        }
        else {
            value = line.substring(0, 1);
            line = line.substring(1).trim();
        }
        return value;
    }

    public String testNextChar() {
        if (line.equals("")) {value = "";}    
        else                 {value = line.substring(0, 1);}
        return value;
    }

    public String getLine() {
        return line;
    }
    
    public void clearLine(){
        line = "";
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
