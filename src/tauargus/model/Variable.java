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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import tauargus.extern.dataengine.TauArgus;
import tauargus.utils.TauArgusUtils;
import tauargus.utils.Tokenizer;

public class Variable implements Cloneable {
    
    private static final Logger LOGGER = Logger.getLogger(Variable.class.getName());
    
    private final TauArgus tauArgus = Application.getTauArgusDll();

    // Determines lengths of fixed sized arrays being used
    public static final int MAX_NUMBER_OF_MISSINGS = 2;
    public static final int MAX_NUMBER_OF_REQUESTS = 2;
    public static final int MAX_NUMBER_OF_DIST = 5;
    public static final int MAX_NUMBER_OF_HIER_LEVELS = 10;

    // Possible values for variable hierarchical;
    public static final int HIER_NONE = 0;
    public static final int HIER_LEVELS = 1;
    public static final int HIER_FILE = 2;
    
    public int index; // index for interfacing with TauArgus dll
    
    public Metadata metadata = null;
    public Variable originalVariable;

    public String name = "";
    public Type type;
    public int bPos = 1; // Only used if data file type is fixed format
    public int varLen; 
    public int nDecimals; // Only used if hasDecimals() returns true

    // Only used by variables of type 'Categorical'
    public String[] missing;
    public String totCode = "";
    public boolean hasDistanceFunction;
    public int[] distanceFunction;
    public String codeListFile = "";
    public int hierarchical;
    public int hierLevelsSum;
    public int[] hierLevels;
    public String hierFileName = "";
    public String leadingString = ".";

    // Only used by variable of type 'RecordKey'
    public String PTableFile = "";      // For CKM on count tables
    public String PTableFileCont = "";  // For CKM on magnitude tables
    public String PTableFileSep = "";   // For CKM on magnitude tables
    
    // Used for CKM
    public boolean zerosincellkey = false; // default: false
    public boolean CKMapply_even_odd = false; // default: false
    public String CKMType = "N";           // default: not allowed to apply CKM
    public int CKMTopK = 1;                // default: only largest observation used, topK = T = 1
    public boolean CKMseparation = false;  // default: small values not treated differently
    public double CKMm1squared = 0;        // default: no variance for small values
    public String CKMscaling = "";         // default: there is no default. Needs to specificied when using CKM with magnitude table
    public double CKMsigma0 = -1;          // default "unknown" for parameters sigma0, sigma1, xstar, q
    public double CKMsigma1 = -1;          // default "unknown" for parameters sigma0, sigma1, xstar, q
    public double CKMxstar = -1;           // default "unknown" for parameters sigma0, sigma1, xstar, q
    public double CKMq = -1;               // default "unknown" for parameters sigma0, sigma1, xstar, q
    public double[] CKMepsilon;            // for parameters epsilon2, epsilon3, ..., epsilonT
    public double muC = 0;                 // default: no additional perturbation for sensitivecells
            
    // Only used by variables of type 'Request'
    public String[] requestCode;

    // Only used in case of a recoded variable of type 'Categorical'
    public boolean recoded;
    public String currentRecodeFile = "";
    public String currentRecodeCodeListFile = "";

    public boolean inTable;

    public boolean truncatable;
    public int truncLevels;
    public static String RecodeWarning;
    
    public Variable(Metadata metadata) {
        this.metadata = metadata;
        this.originalVariable = this;
    }
    
    public boolean isNumeric() {
        return type.isNumeric();
    }
    
    public boolean hasDecimals() {
        return type.hasDecimals();
    }
    
    public boolean isCategorical() {
        return type.isCategorical();
    }
    
    public boolean isResponse() {
        return type.isResponse();
    }
    
    public boolean isRecordKey() {
        return type.isRecordKey();
    }   
    
    public boolean isTotalCode(String code) {
        return code.equalsIgnoreCase(totCode);
    }

    public boolean isTotalCode2(String code) {
        return code.trim().equals(totCode) || code.trim().equals("");
    }
    
    public boolean isMissing(String code) {
        for (int m = 0; m < Variable.MAX_NUMBER_OF_MISSINGS; m++) {
            if (StringUtils.isNotEmpty(missing[m]) && code.equals(missing[m]))  {
                return true;
            }
        }
        return false;
    }
    
    public String getTotalCode() {
        if (totCode.equals("")) {
            return "Total";
        } else {
            return totCode;
        }
    }

    public int numberOfMissings() {
        int nMissings = 0;
        if (isCategorical()) {
            while (nMissings < MAX_NUMBER_OF_MISSINGS && StringUtils.isNotEmpty(missing[nMissings])) {
                nMissings++;
            }
        }
        return nMissings;
    }

    public String normaliseMissing(String missing) {
        if (StringUtils.isBlank(missing)) {
            missing = "";
        } else {
            missing = padCode(missing);
        }
        return missing;
    }
    
    public String normaliseCode(String code) {
        if (StringUtils.isNotEmpty(code) && hierarchical == HIER_NONE) {
            code = padCode(code);
        } 
        return code;
    }
    
    public String padCode(String code) {
        return StringUtils.leftPad(code, varLen);
    }
    
    public RecodeInfo readRecodeFile(String fileName) throws ArgusException, FileNotFoundException, IOException {
        String recodeData = "";
        String missing1 = "";
        String missing2 = "";
        String codeList = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {              
            Tokenizer tokenizer = new Tokenizer(reader);
            while ((tokenizer.nextLine()) != null) {
                String hs;
                hs = tokenizer.getLine();
                String token = tokenizer.nextToken();
                if (token.equals("<MISSING>")) {
                    missing1 = tokenizer.nextToken();
                    if (missing1.equals("")) {
                        throw new ArgusException("No Missing Values found after <MISSING>");
                    }
                    token = tokenizer.nextToken();
                    if (token.equals(",")) {
                        token = tokenizer.nextToken();
                    }
                    missing2 = token;
                } else if (token.equals("<CODELIST>")) {
                    codeList = tokenizer.nextToken();
                } else if (!token.equals("")) {
                    recodeData = recodeData + hs + "\n";
                }
            }
            return new RecodeInfo(recodeData, missing1, missing2, codeList);
        }
    }

    public void writeRecodeFile(String fileName, RecodeInfo recodeInfo) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {            
            if (!recodeInfo.getMissing1().equals("") || !recodeInfo.getMissing2().equals("")) {
                writer.write("<MISSING> " + recodeInfo.getMissing1() + " " + recodeInfo.getMissing2() + "\n");
            }
            if (!recodeInfo.getCodeList().equals("")) {
                writer.write("<CODELIST> " + recodeInfo.getCodeList() + "\n");
            }
            writer.write(recodeInfo.getRecodeData() + "\n");
        }
    }

        public void writeRecodeTreeFile(String fileName) throws IOException {
         int[] isParent = new int[1];
         int[] isActive = new int[1];
         int[] isMissing = new int[1];
         int[] level = new int[1];
         int[] nChild = new int[1];
         String[] code = new String[1];
         String currentCode;
       int[] nc = new int[1]; int[]nac = new int[1]; 
       int i, j;
       try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
           writer.write("<TREERECODE>\n");
// write here the recoded tree   
            tauArgus.GetVarNumberOfCodes(index, nc, nac);
            for (i=0; i<nc[0]-1;i++){
               tauArgus.GetVarCodeProperties(index, i, isParent, isActive, isMissing, level, nChild, code); 
               if ( isActive[0] == 1 && nChild[0] > 0) { //is a node
                  currentCode = code[0]; 
                  tauArgus.GetVarCodeProperties(index, i+1, isParent, isActive, isMissing, level, nChild, code); 
                  if (isActive[0] == 0){
                     writer.write(currentCode + "\n"); 
                  }
               }   
            }
       }
    }

    public void recode(RecodeInfo recodeInfo) throws ArgusException {
        int nMissing = 0; String hs;
        if (StringUtils.isNotBlank(recodeInfo.getMissing1())) {
            nMissing = 1;
        }
        if (StringUtils.isNotBlank(recodeInfo.getMissing2())) {
            nMissing = 2;
        }

        int[] errorType = new int[1];
        int[] errorLine = new int[1];
        int[] errorPos = new int[1];
        String[] warning = new String[1];
        RecodeWarning = "";
//The end of lines in the recode string cause a problem
//The dll requires a "\r\n" as line separator.
        hs = recodeInfo.getRecodeData();
        hs = hs.replace("\n", "\r\n"); 
        if (tauArgus.DoRecode(index, hs, nMissing, recodeInfo.getMissing1(), recodeInfo.getMissing2(), errorType, errorLine, errorPos, warning)) {
            currentRecodeCodeListFile = recodeInfo.getCodeList();
            recoded = true;
            truncLevels = 0;
            RecodeWarning = warning[0];
            LOGGER.info("Recode: variable (" + name + ") has been recoded");
        } else {
            throw new ArgusException(tauArgus.GetErrorString(errorType[0]) + " " + warning[0] + " in recoding; line " + errorLine[0] + " pos " + errorPos[0]);
        }
    }
    
    public void applyRecodeTree(String fileName) throws ArgusException {
//Anco 1.6 try with resources     
//overigens wordt de reader niet gesloten ???????        
//        try (BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));) {        
      //BufferedReader reader = null;  
      try (BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)))){
      //{reader = new BufferedReader(new FileReader(new File(fileName)));                
            tauArgus.UndoRecode(index);
            String regel = reader.readLine();
            if (!StringUtils.equals(regel, "<TREERECODE>")) {
                throw new ArgusException("First line does not start with \"<TREERECODE>\"");
            }
            while ((regel = reader.readLine()) != null) {
                if (StringUtils.isNotBlank(regel)) {
                    int codeIndex = TauArgusUtils.getCodeIndex(index, regel);
                    if (codeIndex == -1) {
                        throw new ArgusException("Code (" + regel + ") not found");
                    } 
                    tauArgus.SetVarCodeActive(index, codeIndex, false);
                }
            }
            tauArgus.DoActiveRecode(index);
        }
        catch (Exception ex) {
            tauArgus.UndoRecode(index);
            throw new ArgusException("Error in reading tree status in recode file " + fileName + ": " + ex.getMessage());
        }
    }
    
    public void recode(String fileName) throws ArgusException, FileNotFoundException, IOException {
        if (hierarchical != HIER_NONE) {
            applyRecodeTree(fileName);
            LOGGER.info("Variable (" + name + ") recoded.\nRecode file used:" + fileName);
        } else {
            RecodeInfo recodeInfo = readRecodeFile(fileName);
            recode(recodeInfo);
            currentRecodeFile = fileName;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        final Variable variable = (Variable) obj;

        boolean equal = name.equals(variable.name)
                && type == variable.type
                && varLen == variable.varLen;
        
        if (variable.metadata.dataFileType != Metadata.DATA_FILE_TYPE_FREE){
            equal = equal && bPos == variable.bPos;
        }

        if (variable.isCategorical()) {
            equal = equal
                    && totCode.equals(variable.totCode)
                    && Arrays.equals(missing, variable.missing)
                    && hasDistanceFunction == variable.hasDistanceFunction
                    && (!hasDistanceFunction || Arrays.equals(distanceFunction, variable.distanceFunction))
                    && codeListFile.equals(variable.codeListFile)
                    && hierarchical == variable.hierarchical
                    && (hierarchical != Variable.HIER_LEVELS || (hierLevelsSum == variable.hierLevelsSum && Arrays.equals(hierLevels, variable.hierLevels)))
                    && (hierarchical != Variable.HIER_FILE || (leadingString.equals(variable.leadingString) && hierFileName.equals(variable.hierFileName)));
        }

        if (variable.hasDecimals()) {
            equal = equal
                    && nDecimals == variable.nDecimals;
        }

        if (variable.type == Type.REQUEST) {
            equal = equal && Arrays.equals(requestCode, variable.requestCode);
        }
        
        if (variable.type == Type.RECORD_KEY){
            equal = equal && PTableFile.equals(variable.PTableFile)
                    && PTableFileCont.equals(variable.PTableFileCont)
                    && PTableFileSep.equals(variable.PTableFileSep);
        }
        
        if (variable.type == Type.RESPONSE){
            equal = equal 
                    && Arrays.equals(CKMepsilon, variable.CKMepsilon)
                    && CKMType.equals(variable.CKMType)
                    && CKMTopK == variable.CKMTopK
                    && CKMseparation == variable.CKMseparation
                    && zerosincellkey == variable.zerosincellkey
                    && CKMapply_even_odd == variable.CKMapply_even_odd
                    && CKMm1squared == variable.CKMm1squared
                    && CKMscaling.equals(variable.CKMscaling)
                    && CKMsigma0 == variable.CKMsigma0
                    && CKMsigma1 == variable.CKMsigma1
                    && CKMxstar == variable.CKMxstar
                    && CKMq == variable.CKMq
                    && muC == variable.muC;
        }
        
        return equal;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.type);
        hash = 89 * hash + this.bPos;
        hash = 89 * hash + this.varLen;
        hash = 89 * hash + this.nDecimals;
        hash = 89 * hash + Arrays.deepHashCode(this.requestCode);
        hash = 89 * hash + (this.hasDistanceFunction ? 1 : 0);
        hash = 89 * hash + Arrays.hashCode(this.distanceFunction);
        hash = 89 * hash + this.hierarchical;
        hash = 89 * hash + Objects.hashCode(this.hierFileName);
        hash = 89 * hash + this.hierLevelsSum;
        hash = 89 * hash + Arrays.hashCode(this.hierLevels);
        hash = 89 * hash + Objects.hashCode(this.codeListFile);
        hash = 89 * hash + Objects.hashCode(this.leadingString);
        hash = 89 * hash + Arrays.deepHashCode(this.missing);
        hash = 89 * hash + Objects.hashCode(this.totCode);
        hash = 89 * hash + Objects.hashCode(this.PTableFile);
        hash = 89 * hash + Objects.hashCode(this.PTableFileCont);
        hash = 89 * hash + Objects.hashCode(this.PTableFileSep);
        hash = 89 * hash + (this.zerosincellkey ? 1 : 0);
        hash = 89 * hash + (this.CKMapply_even_odd ? 1 : 0);
        hash = 89 * hash + Objects.hashCode(this.CKMType);
        hash = 89 * hash + this.CKMTopK;
        hash = 89 * hash + (this.CKMseparation ? 1 : 0);
        hash = 89 * hash + Objects.hashCode(this.CKMscaling);

        return hash;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Variable variable = (Variable)super.clone(); 
        
        if (requestCode != null) {
            //variable.requestCode = (String[])requestCode.clone();
            variable.requestCode = requestCode.clone();
        }
        if (distanceFunction != null) {
            //variable.distanceFunction = (int[])distanceFunction.clone();
            variable.distanceFunction = distanceFunction.clone();
        }
        if (hierLevels != null) {
            //variable.hierLevels = (int[])hierLevels.clone();
            variable.hierLevels = hierLevels.clone();
        }
        if (missing != null) {
            //variable.missing = (String[])missing.clone();
            variable.missing = missing.clone();
        }
        if (CKMepsilon != null){
            //variable.CKMepsilon = (double[])CKMepsilon.clone();
            variable.CKMepsilon = CKMepsilon.clone();
        }
        variable.originalVariable = this;
        return variable;
    }
    
    public int GetDepthOfHierarchicalBoom(boolean Recoded)
    {
        if (hierarchical != HIER_NONE)
        {
            return tauArgus.GetVarHierarchyDepth(index, Recoded);
        }
        else return 1;
    }
}
