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

package tauargus.model;

import argus.utils.StrUtils;
import argus.utils.SystemUtils;
import argus.utils.Tokenizer;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import static tauargus.utils.TauArgusUtils.ShowWarningMessage;

public class Metadata implements Cloneable {
    public static final int DATA_ORIGIN_MICRO = 1;
    public static final int DATA_ORIGIN_TABULAR = 2;
    public static final int DATA_ORIGIN_TABULAR_PLUS_META = 3;

    public static final int DATA_FILE_TYPE_FIXED = 1;
    public static final int DATA_FILE_TYPE_FREE = 2;
    public static final int DATA_FILE_TYPE_FREE_WITH_META = 3;
    public static final int DATA_FILE_TYPE_SPSS = 4;

    private static final Logger LOGGER = Logger.getLogger(Metadata.class.getName());
    
    public int index;
    public int dataOrigin = DATA_ORIGIN_MICRO;
    public int dataFileType = DATA_FILE_TYPE_FIXED;
    public String metaFile = "";
    public String dataFile = "";
    //public String mataPath = ""; // Never used????
    public String fieldSeparator = ";";

    public List<Variable> variables = new ArrayList<>();
    public String safeStatus = "S";
    public String unSafeStatus = "U";
    public String protectStatus = "P";

    public Metadata(boolean isTable) {
        if (isTable) {
            dataOrigin = DATA_ORIGIN_TABULAR;
        }
    }
    
    public boolean contains(Type type) {
        for (Variable variable : variables) {
            if (variable.type == type) {
                return true;
            }
        }
        return false;
    }
    
    public int count(Type type) {
        int n = 0;
        for (Variable variable : variables) {
            if (variable.type == type) {
                n++;
            }
        }
        return n;
    }
    
    public Variable find(Type type) {
        for (Variable variable : variables) {
            if (variable.type == type) {
                return variable;
            }
        }
        return null;
    }

    public Variable find(String name) {
        for (Variable variable : variables) {
            if (variable.name.equalsIgnoreCase(name)) {
                return variable;
            }
        }
        return null;
    }

    public int indexOf(Type type) {
        for (int varIndex = 0; varIndex < variables.size(); varIndex++) {
            Variable variable = variables.get(varIndex);
            if (variable.type == type) {
                return varIndex;
            }
        }
        return -1;
    }

    public int indexOf(String name) {
        for (int varIndex = 0; varIndex < variables.size(); varIndex++) {
            Variable variable = variables.get(varIndex);
            if (variable.name.equalsIgnoreCase(name)) {
                return varIndex;
            }
        }
        return -1;
    }

    public boolean containsWeightVariable() {
        return contains(Type.WEIGHT);
    }

    public boolean containsHoldingVariable() {
        return contains(Type.HOLDING);
    }

    public boolean containsRequestVariable() {
        return contains(Type.REQUEST);
    }

    public boolean containsExplanatoryVariable() {
        return contains(Type.CATEGORICAL) || contains(Type.CAT_RESP);
    }

    public boolean containsResponseVariable() {
        return contains(Type.RESPONSE) || contains(Type.CAT_RESP);
    }

    public boolean containsCostVariable() {
        return contains(Type.COST);
    }

    public boolean containsFrequencyVariable() {
        return contains(Type.FREQUENCY);
    }

    public boolean containsStatusVariable() {
        return contains(Type.STATUS);
    }
    
    public boolean containsRecordKey(){
        return contains(Type.RECORD_KEY);
    }

    public boolean containsDistanceFunction() {
        for (Variable variable : variables) {
            if (variable.isCategorical() && variable.hasDistanceFunction) {
                return true;
            }
        }
        return false;
    }
    
    public int numberOfExplanatoryVariables() {
        return count(Type.CATEGORICAL) + count(Type.CAT_RESP);
    }

    public int numberOfResponseVariables() {
        return count(Type.RESPONSE) + count(Type.CAT_RESP);
    }

    public int numberOfFrequencyVariables() {
        return count(Type.FREQUENCY);
    }

    public int numberOfTopNVariables() {
        return count(Type.TOP_N);
    }

    public int[] indicesOfExplanatoryVariables() {
        int[] indices = new int[numberOfExplanatoryVariables()];
        int n = 0;
        for (int i = 0; i < variables.size(); i++) {
            Variable variable = variables.get(i);
            if (variable.isCategorical()) {
                indices[n++] = variable.index;
            }
        }
        return indices;
    }

    
    public static void createMetadata(Boolean forTables, String dataFile, String metadataFile) throws ArgusException{
    
                Metadata metadata = new Metadata(forTables);
                metadata.dataFile = dataFile;
                metadata.metaFile = metadataFile;
                try {
                    if (forTables){metadata.readTableMetadata();}
                    else {metadata.readMicroMetadata();}
                    Application.addMetadata(metadata);
// Anco 1.6
// Hier was een dialog om eoa reden onderdrukt
//                } catch (ArgusException | FileNotFoundException ex) {
//                } catch (ArgusException ex) {
  //                  JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());
                } catch (FileNotFoundException ex) {
                    throw new ArgusException (ex.getMessage());
  //                  JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());
                } 
    }
 /*
  * Read the metadata from the SPSS system file
  */
  
  /*
   * checks the metadata,read from the RDA file with the metadata, found in the SPSS  system file
   * 
   */  
    public void readMicroMetadata(BufferedReader reader) throws ArgusException {
        boolean CKMspecified = false;
        dataOrigin = DATA_ORIGIN_MICRO;
        dataFileType = DATA_FILE_TYPE_FIXED;
        String hs; // = "";
        Variable variable = null;
        Tokenizer tokenizer = new Tokenizer(reader);
        while (tokenizer.nextLine() != null) {
            String token = tokenizer.nextToken();
            if (tokenizer.getLineNumber() == 1) {
                if (token.equals("<SEPARATOR>")) {
                    dataFileType = DATA_FILE_TYPE_FREE;
                    fieldSeparator = tokenizer.nextToken();
                    continue;
                }
                if (token.equals("<SPSS>")) {
                    dataFileType = DATA_FILE_TYPE_SPSS;
                    continue;
                }
            }

            if (token.charAt(0) != '<') {
                variable = new Variable(this);
                variables.add(variable);
                variable.name = token;
                if (dataFileType != Metadata.DATA_FILE_TYPE_FREE) {
                    variable.bPos = Integer.parseInt(tokenizer.nextToken());
                }
                else {
                    variable.bPos = 0;
                }
                variable.varLen = Integer.parseInt(tokenizer.nextToken());
                variable.missing = new String[Variable.MAX_NUMBER_OF_MISSINGS];
                for (int i = 0; i < Variable.MAX_NUMBER_OF_MISSINGS; i++) {
                    variable.missing[i] = tokenizer.nextToken();
                    if (variable.missing[i].length() > variable.varLen) {
                        throw new ArgusException("Missing value (" + variable.missing[i] + ") too long in line " + tokenizer.getLineNumber());
                    }
                    variable.missing[i] = variable.normaliseMissing(variable.missing[i]);
                }
            }
            else {
                if (variable == null) {
                    throw new ArgusException("A variable should be declared before giving its properties");
                }
                switch (token) {
                    case "<RECODEABLE>":
                    case "<RECODABLE>":
                        if (variable.type == Type.RESPONSE) {
                            variable.type = Type.CAT_RESP;
                        } else {
                            variable.type = Type.CATEGORICAL;
                        }
                        break;
                    case "<TOTCODE>":
                        variable.totCode = tokenizer.nextToken();
                        break;
                    case "<DISTANCE>":
                        variable.hasDistanceFunction = true;
                        variable.distanceFunction = new int[Variable.MAX_NUMBER_OF_DIST];
                        for (int i = 0; i < Variable.MAX_NUMBER_OF_DIST; i++) {
                            token = tokenizer.nextToken();
                            if (StringUtils.isBlank(token)) {
                                if (i == 0) {
                                    variable.distanceFunction[i] = 1;
                                } else {
                                    variable.distanceFunction[i] = variable.distanceFunction[i - 1];
                                }
                            } else {
                                variable.distanceFunction[i] = Integer.valueOf(token);
                            }
                        }
                        break;
                    case "<CODELIST>":
                        variable.codeListFile = tokenizer.nextToken();
                        break;
                    case "<HIERARCHICAL>":
                        variable.hierarchical = Variable.HIER_FILE;
                        break;
                    case "<HIERLEVELS>":
                        variable.hierarchical = Variable.HIER_LEVELS;
                        variable.hierLevels = new int[Variable.MAX_NUMBER_OF_HIER_LEVELS];
                        int nHierLevels = 0;
                        int x = 0;
                        while (!(token = tokenizer.nextToken()).equals("")) {
                            variable.hierLevels[nHierLevels] = Integer.parseInt(token);
                            x += variable.hierLevels[nHierLevels];
                            nHierLevels++;
                        }
                        variable.hierLevelsSum = x;
                        if (x != variable.varLen) {
                            throw new ArgusException("Error in specification of hierarchical levels for variable " + variable.name);
                        }
                        break;
                    case "<HIERLEADSTRING>":
                        variable.leadingString = tokenizer.nextToken();
                        break;
                    case "<HIERCODELIST>":
                        variable.hierFileName = tokenizer.nextToken();
                        break;
                    case "<NUMERIC>":
                        hs = tokenizer.nextToken();
                        if (!hs.equals("")) {
                            throw new ArgusException("Unknown token (" + hs + ") in line " + tokenizer.getLineNumber() + " after keyword <NUMERIC>.");
                        }
                        if (variable.type == Type.CATEGORICAL) {
                            variable.type = Type.CAT_RESP;
                        } else {
                            variable.type = Type.RESPONSE;
                        }
                        break;
                    case "<WEIGHT>":
                        variable.type = Type.WEIGHT;
                        break;
                    case "<HOLDING>":
                        variable.type = Type.HOLDING;
                        break;
                    case "<REQUEST>":
                        variable.type = Type.REQUEST;
                        variable.requestCode = new String[Variable.MAX_NUMBER_OF_REQUESTS];
                        for (int i=0; i<Variable.MAX_NUMBER_OF_REQUESTS; i++) {
                            variable.requestCode[i] = tokenizer.nextToken();
                        }
                        break;
                    case "<DECIMALS>":
                        variable.nDecimals = Integer.parseInt(tokenizer.nextToken());
                        break;
                    case "<TRUNCABLE>":
                        variable.truncatable = true;
                        break;
                    case "<RECORDKEY>":
                        if (this.containsRecordKey()){
                            throw new ArgusException("Only one <RECORDKEY> variable is allowed. Please check your .rda-file.");
                        }
                        hs = tokenizer.nextToken();
                        if (!hs.equals("")) {
                            throw new ArgusException("Unknown token (" + hs + ") in line " + tokenizer.getLineNumber() + " after keyword <RECORDKEY>.");
                        }
                        variable.type = Type.RECORD_KEY;
                        break;
                    case "<PFILE_FREQ>":
                    case "<PFILE>":
                        variable.PTableFile = tokenizer.nextToken();
                        break;
                    case "<PFILE_CONT>":
                        variable.PTableFileCont = tokenizer.nextToken();
                        break;
                    case "<PFILE_SEP>":
                        variable.PTableFileSep = tokenizer.nextToken();
                        break;
                    case "<CKM>":
                        if (!variable.isResponse()) throw new ArgusException("<CKM> tag only allowed for numeric variables.");
                        hs = tokenizer.nextField("(").toUpperCase();
                        CKMspecified = true;
                        switch(hs){
                            case "N":
                                CKMspecified = false;
                            case "M":
                            case "D":
                            case "V":
                                variable.CKMType=hs;
                                break;
                            case "T":
                                variable.CKMType=hs;
                                try{
                                    variable.CKMTopK = Integer.parseInt(tokenizer.nextField(")"));
                                }
                                catch (NumberFormatException ex){
                                    throw new ArgusException("topK in <CKM> T(topK) should be integer\n" + ex.getMessage());
                                }
                                break;
                            default:
                                throw new ArgusException("Unknown type <CKM> " + hs + " for variable " + variable.name);
                        }
                        if (CKMspecified) {
                            variable.CKMepsilon = new double[variable.CKMTopK];
                            variable.CKMepsilon[0] = 1.0;
                        }
                        break;
                    case "<INCLUDEZEROS>":
                        if (!CKMspecified) throw new ArgusException("<INCLUDEZEROS> only allowed after <CKM> is specified.");
                        if (variable.isResponse()) {
                                variable.zerosincellkey = tokenizer.nextToken().equals("Y");
                        }
                        else{
                            throw new ArgusException("<INCLUDEZEROS> only allowed for numeric variables.");
                        }
                        break;
                    case "<PARITY>":
                        if (!CKMspecified) throw new ArgusException("<PARITY> only allowed after <CKM> is specified.");
                        if (variable.isResponse()) {
                                variable.CKMapply_even_odd = ("Y".equals(tokenizer.nextToken()));
                        }
                        else{
                            throw new ArgusException("<INCLUDEZEROS> only allowed for numeric variables.");
                        }
                        break;
                    case "<SCALING>": // Can much nicer and needs exception checking!
                        if (!CKMspecified) throw new ArgusException("<SCALING> only allowed after <CKM> is specified.");
                        if (!variable.isResponse()) throw new ArgusException("<SCALING> tag only allowed for numeric variables.");
                        hs = tokenizer.nextField("(").toUpperCase();
                        if (!(hs.equals("N") || hs.equals("F"))){
                            throw new ArgusException("Unknown type <SCALING> " + hs + " for variable " + variable.name);
                        }
                        else{
                            try{  
                                variable.CKMscaling = hs;
                                int T = variable.CKMTopK;

                                if (variable.CKMscaling.equals("F")){
                                    hs = tokenizer.nextField(",");
                                    variable.CKMsigma0 = Double.parseDouble(hs);
                                    hs = tokenizer.nextField(",");
                                    variable.CKMsigma1 = Double.parseDouble(hs);
                                    hs = tokenizer.nextField(",");
                                    variable.CKMxstar = Double.parseDouble(hs);
                                    if (T>=2){
                                        hs = tokenizer.nextField(",");
                                        variable.CKMq = Double.parseDouble(hs);
                                        for (int i=2; i<T;i++){
                                            hs = tokenizer.nextField(",");
                                            variable.CKMepsilon[i-1] = Double.parseDouble(hs);
                                        }
                                        hs = tokenizer.nextField(")");
                                        variable.CKMepsilon[T-1] = Double.parseDouble(hs);
                                    }
                                    else{
                                        hs = tokenizer.nextField(")");
                                        variable.CKMq = Double.parseDouble(hs);
                                    }
                                }
                                if (variable.CKMscaling.equals("N")){
                                    if (T>=2){
                                        hs = tokenizer.nextField(",");
                                        variable.CKMsigma1 = Double.parseDouble(hs);
                                        for (int i=2; i<T;i++){
                                            hs = tokenizer.nextField(",");
                                            variable.CKMepsilon[i-1] = Double.parseDouble(hs);
                                        }   
                                        hs = tokenizer.nextField(")");
                                        variable.CKMepsilon[T-1] = Double.parseDouble(hs);
                                    }
                                    else{
                                        hs = tokenizer.nextField(")");
                                        variable.CKMsigma1 = Double.parseDouble(hs);
                                    }
                                }
                            }
                            catch(Exception ex){
                                throw new ArgusException("Exception when reading <SCALING> parameters:\n"+ex.getMessage()+
                                        "\nCheck number of parameters\nCheck number of epsilons related to TopK\nCheck format double/integer");
                            }
                        }
                        break;
                    case "<SEPARATION>":
                        if (!CKMspecified) throw new ArgusException("<SEPARATION> only allowed after <CKM> is specified.");
                        if (!variable.isResponse()) throw new ArgusException("<SEPARATION> tag only allowed for numeric variables.");
                        variable.CKMseparation = tokenizer.nextToken().equals("Y");
                        break;
                    default:
                        throw new ArgusException("Unknown keyword (" + token + ") in line " + tokenizer.getLineNumber());
                }
            }
        }
        verify();
        tokenizer.close();
    }

    public void readMicroMetadata() throws ArgusException, FileNotFoundException {
        readMicroMetadata(new BufferedReader(new FileReader(new File(metaFile))));
    }
    
    public String getFilePath(String fileName) throws ArgusException {
        File f = new File(fileName);
        if (f.isFile()) {
            return fileName;
        }
        String dataDir = FilenameUtils.getFullPathNoEndSeparator(dataFile);
        String name = FilenameUtils.getName(fileName);
        f = new File(dataDir, name);
        if (f.isFile()) {
            return f.getPath();
        } else {
            throw new ArgusException("File with name " + fileName + " does not exist.");
        }
    }
    

    public void readTableMetadata(BufferedReader reader) throws ArgusException {
        dataOrigin = DATA_ORIGIN_TABULAR;
        dataFileType = DATA_FILE_TYPE_FREE;
        Variable variable = null;
        Tokenizer tokenizer = new Tokenizer(reader);
        while (tokenizer.nextLine() != null) {
            String token = tokenizer.nextToken();
            if (token.charAt(0) != '<') {
                variable = new Variable(this);
                variables.add(variable);
                variable.name = token;
                variable.missing = new String[Variable.MAX_NUMBER_OF_MISSINGS];
                for (int i = 0; i < Variable.MAX_NUMBER_OF_MISSINGS; i++) {
                    variable.missing[i] = variable.normaliseMissing(tokenizer.nextToken());
                }
            }
            else {
                switch (token) {
                    case "<SEPARATOR>":
                        fieldSeparator = tokenizer.nextToken();
                        break;
                    case "<SAFE>":
                        safeStatus = tokenizer.nextToken();
                        break;
                    case "<UNSAFE>":
                        unSafeStatus = tokenizer.nextToken();
                        break;
                    case "<PROTECT>":
                        protectStatus = tokenizer.nextToken();
                        break;
                    case "<RECODABLE>":
                    case "<RECODEABLE>":
                        variable.type = Type.CATEGORICAL;
                        variable.inTable = true;
                        break;
                    case "<TOTCODE>":
                        variable.totCode = tokenizer.nextToken();
                        break;
                    case "<DISTANCE>":
                        variable.hasDistanceFunction = true;
                        variable.distanceFunction = new int[Variable.MAX_NUMBER_OF_DIST];
                        for (int i = 0; i < Variable.MAX_NUMBER_OF_DIST; i++) {
                            token = tokenizer.nextToken();
                            if (StringUtils.isBlank(token)) {
                                if (i == 0) {
                                    variable.distanceFunction[i] = 1;
                                } else {
                                    variable.distanceFunction[i] = variable.distanceFunction[i - 1];
                                }
                            } else {
                                variable.distanceFunction[i] = Integer.valueOf(token);
                            }
                        }
                        break;
                    case "<CODELIST>":
                        variable.codeListFile = tokenizer.nextToken();
                        break;
                    case "<HIERARCHICAL>":
                        variable.hierarchical = Variable.HIER_FILE;
                        break;
                    case "<HIERLEVELS>":
                        variable.hierarchical = Variable.HIER_LEVELS;
                        variable.hierLevels = new int[Variable.MAX_NUMBER_OF_HIER_LEVELS];
                        int nHierLevels = 0;
                        int x = 0;
                        while (!(token = tokenizer.nextToken()).equals("")) {
                            variable.hierLevels[nHierLevels] = Integer.parseInt(token);
                            x += variable.hierLevels[nHierLevels];
                            nHierLevels++;
                        }
                        variable.hierLevelsSum = x;
                        if (variable.varLen == 0) {
                            variable.varLen  = x;
                        }
                        if (x != variable.varLen) {
                            throw new ArgusException("Error in specification of hierarchical levels for variable " + variable.name);
                        }
                        break;
                    case "<HIERLEADSTRING>":
                        variable.leadingString = tokenizer.nextToken();
                        break;
                    case "<HIERCODELIST>":
                        variable.hierFileName = tokenizer.nextToken();
                        break;
                    case "<NUMERIC>":
                        switch (tokenizer.nextToken()) {
                            case "":
                                variable.type = Type.RESPONSE;
                                break;
                            case "<SHADOW>":
                                variable.type = Type.SHADOW;
                                break;
                            case "<COST>":
                            case "<COSTVAR>":
                                variable.type = Type.COST;
                                break;
                            case "<LOWERPL>":
                                variable.type = Type.LOWER_PROTECTION_LEVEL;
                                break;
                            case "<UPPERPL>":
                                variable.type = Type.UPPER_PROTECTION_LEVEL;
                        }
                        break;
                    case "<FREQUENCY>":
                        variable.type = Type.FREQUENCY;
                        break;
                    case "<MAXSCORE>":
                        variable.type = Type.TOP_N;
                        break;
                    case "<STATUS>":
                        variable.type = Type.STATUS;
                        break;
                    case "<DECIMALS>":
                        variable.nDecimals = Integer.parseInt(tokenizer.nextToken());
                        break;
                    default:
                        throw new ArgusException("Unknown keyword (" + token + ") in line " + tokenizer.getLineNumber());
                }
            }
        }
        verify();
        tokenizer.close();
    }

    public void readTableMetadata() throws ArgusException, FileNotFoundException {
        readTableMetadata(new BufferedReader(new FileReader(new File(metaFile))));
    }
    
    public void writeHrcFile(Variable variable, String fileName, String leadingString) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        
            int varIndex = variable.index;
            int codeIndex = 1; // skip first entry (Total)
            VarCode varCode = new VarCode();
            while (varCode.setCode(varIndex, codeIndex++)) {
                if (!varCode.isMissing()) {
                    // Only level of total is 0, otherwise 1 or more
                    writer.print(StringUtils.repeat(leadingString, varCode.getLevel() - 1));
                    writer.println(varCode.getCodeString());
                }
            }
        }
        finally{ writer.close(); }
    }
    
    public void writeVariable(int dataFileType, int microTabularData, String fileName, PrintWriter writer, Variable variable) throws IOException, ArgusException {
        writer.print(variable.name);
        if (dataFileType != DATA_FILE_TYPE_FREE) {
             writer.print(" " + variable.bPos);
        }
        if (microTabularData == DATA_ORIGIN_MICRO) {
            writer.print(" " + variable.varLen);
        }
        if (variable.isCategorical()) {
            for (int j=0; j<Variable.MAX_NUMBER_OF_MISSINGS; j++) {
                if (StringUtils.isNotBlank(variable.missing[j])) {
                    writer.print(" " + StrUtils.quote(variable.missing[j]));
                }
            }
        }
        writer.println();

        // Variables of type CAT_RESP (CATEGORICAL/RESPONSE) should execute two cases!
        switch(variable.type) {
            case CATEGORICAL:
            case CAT_RESP:
                writer.println("    <RECODEABLE>");
                writer.println("    <TOTCODE> " + StrUtils.quote(variable.getTotalCode()));
                if (variable.hasDistanceFunction) {
                    writer.print("    <DISTANCE>");
                    for (int j = 0; j < Variable.MAX_NUMBER_OF_DIST; j++) {
                        writer.print(" " + variable.distanceFunction[j]);
                    }
                    writer.println();
                }
                if (StringUtils.isNotEmpty(variable.codeListFile)) {
                    String hs = variable.codeListFile;
                    // If no path is specified in codeListFile, datadir is assumed
                    if ( hs.indexOf("\\",0)>0 ||hs.indexOf(":",0)>0||hs.indexOf(":",0)>0){}
                    else { hs = variable.metadata.getFilePath(variable.codeListFile);}
                    writer.println("    <CODELIST> " + StrUtils.quote(hs));
                }
                if (variable.hierarchical != Variable.HIER_NONE) {
                    writer.println("    <HIERARCHICAL>");
                    if (variable.recoded) {
                        // Always write a hrc-file, also if before recoding we had HIER_LEVELS.
                        String leadingString = "@";
                        if (variable.hierarchical == Variable.HIER_FILE) {
                            leadingString = variable.leadingString;
                        }
                        String hrcFileName = StrUtils.replaceExtension(fileName, variable.index + "_New.HRC");
                        writeHrcFile(variable, hrcFileName, leadingString);
                        writer.println("    <HIERCODELIST> " + StrUtils.quote(hrcFileName));
                        writer.println("    <HIERLEADSTRING> " + StrUtils.quote("@"));
                    } else if (variable.hierarchical == Variable.HIER_LEVELS) {
                        writer.print("    <HIERLEVELS>");
                        for (int j = 0; j < Variable.MAX_NUMBER_OF_HIER_LEVELS; j++) {
                            if (variable.hierLevels[j] != 0) {
                                writer.print(" " + variable.hierLevels[j]);
                            }
                        }
                        writer.println();
                    } else if (variable.hierarchical == Variable.HIER_FILE) {
 //                       String hs = FilenameUtils.getAbsolutePath(variable.hierFileName);
                        String hs = variable.hierFileName;
                        if ( hs.indexOf("\\",0)>0 ||hs.indexOf(":",0)>0||hs.indexOf(":",0)>0){}
                        else { hs = variable.metadata.getFilePath(variable.hierFileName);}
                        writer.println("    <HIERCODELIST> " + StrUtils.quote(hs)); //variable.hierFileName));
                        writer.println("    <HIERLEADSTRING> " + StrUtils.quote(variable.leadingString));
                    }
                }
                if (variable.type != Type.CAT_RESP) {
                    break;
                }
            case RESPONSE:
                writer.println("    <NUMERIC>");
                if (!variable.CKMType.equals("N")){ // This means that CKM information for magnitude tables should/could be available
                    writer.print("    <CKM> " + variable.CKMType);
                    if (variable.CKMType.equals("T"))
                        writer.println("("+variable.CKMTopK+")");
                    writer.println("    <INCLUDEZEROS> " + (variable.zerosincellkey ? "Y" : "N"));
                    writer.println("    <PARITY> " + (variable.CKMapply_even_odd ? "Y" : "N"));
                    writer.print("    <SCALING> ");
                    String hs;
                    switch (variable.CKMscaling){
                        case "F": hs = "F(";
                                  hs += Double.toString(variable.CKMsigma0) + ",";
                                  hs += Double.toString(variable.CKMsigma1) + ",";
                                  hs += Double.toString(variable.CKMxstar) + ",";
                                  hs += Double.toString(variable.CKMq);
                                  for (int i=1; i<variable.CKMTopK; i++){
                                      hs += "," + variable.CKMepsilon[i];
                                  }
                                  hs += ")";
                                  writer.println(hs);
                                  break;
                        case "N": hs = "N(";
                                  hs += Double.toString(variable.CKMsigma1);
                                  for (int i=1; i<variable.CKMTopK; i++){
                                      hs += "," + variable.CKMepsilon[i];
                                  }
                                  hs += ")";
                                  writer.println(hs);
                                  break;
                        default: break;
                    }
                    writer.println("    <SEPARATION> " + (variable.CKMseparation ? "Y" : "N"));
                }
                break;
            case WEIGHT:
                writer.println("    <WEIGHT>");
                break;
            case HOLDING:
                writer.println("    <HOLDING>");
                break;
            case REQUEST:
                writer.print("    <REQUEST>");
                for (int j=0; j<Variable.MAX_NUMBER_OF_REQUESTS; j++) {
                    writer.print(" " + StrUtils.quote(variable.requestCode[j]));
                }
                writer.println();
                break;
            case RECORD_KEY:
                writer.println("    <RECORDKEY>");
                String hs = variable.PTableFile;
                if (StringUtils.isNotEmpty(hs)){
                    if ( hs.indexOf("\\",0)>0 || hs.indexOf(":",0)>0 || hs.indexOf(":",0)>0){}
                    else { hs = variable.metadata.getFilePath(variable.PTableFile);}
                    writer.println("    <PFILE_FREQ> " + StrUtils.quote(hs));
                }
                hs = variable.PTableFileCont;
                if (StringUtils.isNotEmpty(hs)){
                    if ( hs.indexOf("\\",0)>0 || hs.indexOf(":",0)>0 || hs.indexOf(":",0)>0){}
                    else { hs = variable.metadata.getFilePath(variable.PTableFileCont);}
                    writer.println("    <PFILE_CONT> " + StrUtils.quote(hs));
                }
                hs = variable.PTableFileSep;
                if (StringUtils.isNotEmpty(hs)){
                    if ( hs.indexOf("\\",0)>0 || hs.indexOf(":",0)>0 || hs.indexOf(":",0)>0){}
                    else { hs = variable.metadata.getFilePath(variable.PTableFileSep);}
                    writer.println("    <PFILE_SEP> " + StrUtils.quote(hs));
                }
                break;
            case SHADOW:
                writer.println("    <NUMERIC> <SHADOW>");
                break;
            case COST:
                writer.println("    <NUMERIC> <COSTVAR>");
                break;
            case FREQUENCY:
                writer.println("    <FREQUENCY>");
                break;
            case TOP_N:
                writer.println("    <MAXSCORE>");
                break;
            case LOWER_PROTECTION_LEVEL:
                writer.println("    <NUMERIC> <LOWERPL>");
                break;
            case UPPER_PROTECTION_LEVEL:
                writer.println("    <NUMERIC> <UPPERPL>");
                break;
            case STATUS:
                writer.println("    <STATUS>");
                break;
        }
        if (variable.hasDecimals() && variable.nDecimals > 0) {
            writer.println("    <DECIMALS> " + variable.nDecimals);
        }
        if (variable.truncatable) {
            writer.println("    <TRUNCABLE>");
        }
    }

    public void writeVariable(int dataFileType, int MicroTabularData, String filename, PrintWriter writer, Variable variable, Type type) throws IOException, ArgusException{
        // Used for variables of mixed type CAT_RESP to force them to be CATEGORICAL or RESPONSE.
        Type oldType = variable.type;
        variable.type = type;
        try {
            writeVariable(dataFileType, MicroTabularData, filename, writer, variable);
        }
        finally {
            variable.type = oldType;
        }
    }

    public void write(String fileName, Writer w) throws IOException, ArgusException {
        PrintWriter writer = null;
        try {  writer = new PrintWriter(w);
            if (dataFileType == DATA_FILE_TYPE_FREE) {
                writer.println("   <SEPARATOR> " + StrUtils.quote(fieldSeparator));
            }
            if (dataFileType == DATA_FILE_TYPE_SPSS) {
                writer.println("   <SPSS>");
            }
            if (dataOrigin != DATA_ORIGIN_MICRO) {
                writer.println("   <SAFE> " + StrUtils.quote(safeStatus));
                writer.println("   <UNSAFE> " + StrUtils.quote(unSafeStatus));
                writer.println("   <PROTECT> " + StrUtils.quote(protectStatus));
            }
            for (Variable variable : variables) {
                writeVariable(dataFileType, dataOrigin, fileName, writer, variable);
            }
        }
        finally {writer.close();}
    }

    public void write(String fileName) {
        try {
            write(fileName, new BufferedWriter(new FileWriter(new File(fileName))));
          } catch (ArgusException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void verify() throws ArgusException {
        boolean overlapFound = false; String overlapString = "";  
        if (variables.isEmpty()) {
             throw new ArgusException("No variables have been specified.");
        }

        if (numberOfExplanatoryVariables() == 0) {
            throw new ArgusException("No explanatory variables found.");
        }
        
        if (count(Type.RECORD_KEY) > 1) {
            throw new ArgusException("More than one record key variable found.");
        }
        
        if (dataOrigin == DATA_ORIGIN_TABULAR) {
            if (count(Type.SHADOW) > 1) {
                throw new ArgusException("More than one shadow variable found.");
            }
            if (count(Type.COST) > 1) {
                throw new ArgusException("More than one cost variable found.");
            }
            if (count(Type.FREQUENCY) > 1) {
                throw new ArgusException("More than one frequency variable found.");
            }
            if (count(Type.LOWER_PROTECTION_LEVEL) > 1) {
                throw new ArgusException("More than one lower protection variable found.");
            }
            if (count(Type.UPPER_PROTECTION_LEVEL) > 1) {
                throw new ArgusException("More than one upper protection variable found.");
            }
            if (count(Type.STATUS) > 1) {
                throw new ArgusException("More than one status variable found.");
            }
            if (count(Type.RESPONSE) == 0 && count(Type.FREQUENCY) == 0) {
                throw new ArgusException("No response or frequency variable found.");
            }
        }
        
        for (int i = 0; i < variables.size(); i++) {
            Variable variable = variables.get(i);
            if (dataFileType == DATA_FILE_TYPE_FIXED) {
                int b1 = variable.bPos;
                int e1 = b1 + variable.varLen;
                for (int j = i + 1; j < variables.size(); j++) {
                    Variable variable2 = variables.get(j);
                    int b2 = variable2.bPos;
                    int e2 = b2 + variable2.varLen;
                    if (b2 < e1 && e2 > b1) {
//                        throw new ArgusException("Variable " + variable.name + " and variable " + variable2.name + " overlap.");
                        if (overlapFound) {overlapString = overlapString + "\n";} 
                        overlapFound = true; 
                        overlapString = overlapString + "Variable " + variable.name + " and variable " + variable2.name + " overlap.";                      
                    }
                    if (StringUtils.equalsIgnoreCase(variable.name, variable2.name)) {
                        throw new ArgusException("Variable" + i + " and variable" + j + " have the same name.");
                    }
                }
            }
            if (variable.isCategorical()) {
                for (int k = 0; k < Variable.MAX_NUMBER_OF_MISSINGS - 1; k++) {
                    if (StringUtils.isEmpty(variable.missing[k]) && StringUtils.isNotEmpty(variable.missing[k + 1])) {
                        throw new ArgusException("Error in missing specification of variable " + variable.name + ".");
                    }
                }
                if (dataOrigin != Metadata.DATA_ORIGIN_MICRO && StringUtils.isEmpty(variable.totCode)) {
                    throw new ArgusException("Missing total specification of variable " + variable.name + ".");
                }
                if (dataFileType == Metadata.DATA_FILE_TYPE_FIXED && variable.varLen == 0) {
                    throw new ArgusException("Field length is 0 for variable " + variable.name + ".\nThis is not allowed in fixed format." );
                }
                if (variable.hierarchical == Variable.HIER_FILE) {
                    if (StringUtils.isBlank(variable.hierFileName)) {
                        throw new ArgusException("Error in specifying hierarchical structures for variable: " + variable.name
                                + "\nHierarchical file is not specified.");
                    }
                    if (StringUtils.isBlank(variable.leadingString)) {
                        throw new ArgusException("Error in specifying hierarchical structures for variable: " + variable.name
                                + "\nLeading string is not specified");
                    }
                }
                if (variable.hierarchical == Variable.HIER_LEVELS) {
                    if (variable.hierLevelsSum == 0) {
                        throw new ArgusException("Sum of number of digits of hierarchical levels may not be zero for variable " + variable.name + ".");
                    }
                }
            }
            
            if (variable.isResponse()){
                if (!variable.CKMType.equals("N")){
                    if (this.find(Type.RECORD_KEY) == null){
                        throw new ArgusException("CKM specified for continuous variable " + variable.name + " without having a RecordKey specified.");
                    }
                    if (StringUtils.isEmpty(this.find(Type.RECORD_KEY).PTableFileCont)){
                        throw new ArgusException("CKM specified for continuous variable " + variable.name + " without specification of ptable for continuous CKM.");
                    }
                    if (!(variable.CKMscaling.equals("F") || variable.CKMscaling.equals("N"))){
                        throw new ArgusException("Scaling should be set for variable " + variable.name + " when CKM is allowed.");
                    }
                }
            }

            if (variable.type == Type.REQUEST) {
                boolean emptyRequestCodes = true;
                for (String code : variable.requestCode) {
                    if (StringUtils.isNotEmpty(code)) {
                        emptyRequestCodes = false;
                    }
                }
                if (emptyRequestCodes) {
                    throw new ArgusException("There is no request code specified for variable " + variable.name + ".");
                }
            }
            
            if (variable.type == Type.RECORD_KEY){
                if (StringUtils.isEmpty(variable.PTableFile)){
                    throw new ArgusException("There is no ptable file specified for frequency CKM");
                }
            }
        }            
        if (overlapFound){int i = ShowWarningMessage(overlapString);
           if (i == 0 ) {
              throw new ArgusException(""); //overlapString);
          }
        }
    }
    
    public void writeTableMetadata(String fileName, int NExpVar, Variable[] ExpVar, Variable RespVar, Variable ShadowVar, final int CostFunc, Variable CostVar, int TopN, boolean Simple, boolean WithAudit) throws IOException, ArgusException {
        int dataFileType = DATA_FILE_TYPE_FREE;
        int microTabularData = DATA_ORIGIN_TABULAR;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter writer = null;
        try {
            fw = new FileWriter(fileName);
            bw = new BufferedWriter(fw);
            writer = new PrintWriter(bw);
            writer.println("   <SEPARATOR> " + StrUtils.quote(";"));
            for (int i = 0; i < NExpVar; i++) {
                Variable variable = ExpVar[i];
                writeVariable(dataFileType, microTabularData, fileName, writer, variable, Type.CATEGORICAL);
            }

            if (RespVar != Application.getFreqVar()){
                writeVariable(dataFileType, microTabularData, fileName, writer, RespVar, Type.RESPONSE);
            }    

            writer.println("FreqVar");
            writer.println("    <FREQUENCY>");

            if (RespVar != Application.getFreqVar()){
              if (!Simple) {
                  if (ShadowVar == null) {
                      writer.println(RespVar.name + "_Shadow");
                  } else {
                      writer.println(ShadowVar.name);
                 }
                  writer.println("    <NUMERIC> <SHADOW>");
              }
            }

            switch (CostFunc) {
                case TableSet.COST_FREQ:
                    writer.println("FreqCost");
                    break;
                case TableSet.COST_UNITY:
                    writer.println("UnitCost");
                    break;
                case TableSet.COST_DIST:
                    writer.println("DistCost");
                    break;
                case TableSet.COST_VAR:
                    if (CostVar == null) {
                      if (RespVar != Application.getFreqVar()){writer.println(RespVar.name + "_Cost");}
                      else                                    {writer.println("FreqVar_Cost");}
                    } else {
                      writer.println(CostVar.name);
                    }
            }
            writer.println("    <NUMERIC> <COSTVAR>");

            if (!Simple) {
                for (int i = 0; i < TopN; i++) {
                    writer.println("Top" + (i+1));
                    writer.println("    <MAXSCORE>");
                }
            }
            writer.println("StatusVar");
            writer.println("    <STATUS>");
            writer.println("LowerProtLevel");
            writer.println("    <NUMERIC> <LOWERPL>");
            writer.println("UpperProtLevel");
            writer.println("    <NUMERIC> <UPPERPL>");
            if (WithAudit) {
                writer.println("//The audit info cannot be read back into TauArgus");
                writer.println("//Lower auditlevel");
                writer.println("//Upper auditlevel");
                writer.println("//Audit range");
                writer.println("//Percentage lower auditrange");
                writer.println("//Percentage upper auditrange");
                writer.println("//Percentage audit range");
            }
        }
        finally {writer.close();}
    }

    public void writeCKMMetadata(String fileName, int NExpVar, Variable[] ExpVar, Variable RespVar, boolean addOrig, boolean addDiff, boolean addCellKey) throws IOException, ArgusException {
        int tmpDataFileType = DATA_FILE_TYPE_FREE;
        int microTabularData = DATA_ORIGIN_TABULAR;

        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter writer = null;
        try {
            fw = new FileWriter(fileName);
            bw = new BufferedWriter(fw);
            writer = new PrintWriter(bw);
            writer.println("   <SEPARATOR> " + StrUtils.quote(";"));
            for (int i = 0; i < NExpVar; i++) {
                Variable variable = ExpVar[i];
                writeVariable(tmpDataFileType, microTabularData, fileName, writer, variable, Type.CATEGORICAL);
            }

            writeVariable(tmpDataFileType, microTabularData, fileName, writer, RespVar, Type.RESPONSE);

            if (addOrig){
                writer.println("OrigVar");
                writer.println("    <NUMERIC>");
            }
            if (addDiff){
                writer.println("Difference");
                writer.println("    <NUMERIC>");
            }
            if (addCellKey){
                writer.println("CellKey");
                writer.println("    <CELLKEY>");
            }
        }
        finally {writer.close();}
    }

    
    private void updateProgress(long Fread, long Flen, int LineNo, PropertyChangeSupport propertyChangeSupport) {
        if (LineNo % 100 == 0) {
            int percentage = (int)(Fread / Flen);
            propertyChangeSupport.firePropertyChange("progressMain", null, percentage);
        }
    }
    
    public void setVariableLengthFromData(final PropertyChangeSupport propertyChangeSupport) throws ArgusException, IOException, FileNotFoundException { 
        LOGGER.info("Start reading data from file: " + dataFile);
        propertyChangeSupport.firePropertyChange("activityMain", null, "Determining length of variables from data file " + dataFile);

        for (Variable variable : variables) {
            variable.varLen = 0;
        }

        File[] files = SystemUtils.getFiles(dataFile);

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            BufferedReader reader = null;
            try {reader = new BufferedReader(new FileReader(file));
                long fileLength = file.length();
                long bytesRead = 0;
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    bytesRead = bytesRead + line.length() + 1;
                    updateProgress(bytesRead, fileLength, lineNumber, propertyChangeSupport);
                    line = line.trim();
                    if (StringUtils.isNotEmpty(line)) {
                        if (!line.contains(fieldSeparator)) {
                             throw new ArgusException("Separator:" + fieldSeparator + " not found in table file");
                        }

                        String[] values = line.split(fieldSeparator);
                        for (int j = 0; j < values.length; j++) {
                            if (j < variables.size()) {
                                Variable variable = variables.get(j);
                                if (!(variable.isCategorical() && variable.hierarchical == Variable.HIER_FILE)){
                                    String value = StrUtils.unQuote(values[j].trim());
                                    if (!value.equals(variable.totCode)) {
                                        if (value.length() > variable.varLen) {
                                            variable.varLen = value.length();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            finally {reader.close();}
        }

        for (Variable variable : variables) {
            if (variable.isCategorical() && variable.hierarchical==Variable.HIER_FILE) {
                // Bij hierfile de lengte halen uit de hier-file
                BufferedReader reader = null;
                try {reader = new BufferedReader(new FileReader(getFilePath(variable.hierFileName)));
                    int length = variable.leadingString.length();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        while (line.startsWith(variable.leadingString)) {
                            line = line.substring(length);
                        }
                        line = StringUtils.stripEnd(line, " \t");
                        if (line.length() > variable.varLen) {
                            variable.varLen = line.length();
                        }
                        if (line.equals(variable.totCode)) {
                            throw new ArgusException("Totalcode: " + line + " found in hierarchy file.\nRemove total code form hierarchy.\nTotal is added implicitly");
                        }
                    }
                }
                catch (FileNotFoundException ex) {
                    throw new ArgusException("File with hierarchical information: " + variable.hierFileName + " could not be found");
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + this.dataOrigin;
        hash = 41 * hash + this.dataFileType;
        hash = 41 * hash + Objects.hashCode(this.metaFile);
        hash = 41 * hash + Objects.hashCode(this.dataFile);
        hash = 41 * hash + Objects.hashCode(this.fieldSeparator);
        hash = 41 * hash + Objects.hashCode(this.variables);
        hash = 41 * hash + Objects.hashCode(this.safeStatus);
        hash = 41 * hash + Objects.hashCode(this.unSafeStatus);
        hash = 41 * hash + Objects.hashCode(this.protectStatus);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Metadata other = (Metadata) obj;

        return dataOrigin == other.dataOrigin
                && dataFileType == other.dataFileType
                && metaFile.equals(other.metaFile)
                && dataFile.equals(other.dataFile)
                && fieldSeparator.equals(other.fieldSeparator)
                && variables.equals(other.variables)
                && safeStatus.equals(other.safeStatus)
                && unSafeStatus.equals(other.unSafeStatus)
                && protectStatus.equals(other.protectStatus);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Metadata metadata = (Metadata)super.clone(); 

        metadata.variables = new ArrayList<>();
        for (Variable var : variables) metadata.variables.add(var);
        
        return metadata;
    }
}
