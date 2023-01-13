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

import argus.utils.SystemUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.apache.commons.io.FilenameUtils;
import tauargus.extern.dataengine.TauArgus;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.Metadata;
import tauargus.model.Variable;

public class TauArgusUtils {
    private static TauArgus tauArgus = Application.getTauArgusDll();
    
    public static int getNumberOfCodes(int varIndex) {
        int[] numberOfCodes = new int[1];
        tauArgus.GetVarNumberOfCodes(varIndex, numberOfCodes, new int[1]);
        return numberOfCodes[0];
    }
    
    public static int getNumberOfActiveCodes(int varIndex) {
        int[] numberOfCodes = new int[1];
        tauArgus.GetVarNumberOfCodes(varIndex, new int[1], numberOfCodes);
        return numberOfCodes[0];
    }
    
    public static int getCodeIndex(int varIndex, String code) {
        String[] codeString = new String[1];

        int codeIndex = 0;
        while (tauArgus.GetVarCode(varIndex, codeIndex, new int[1], codeString, new int[1], new int[1])) {
            if (code.equals(codeString[0])) {
                return codeIndex;
            }
            codeIndex++;
        }
        return -1;
    }
    
    public static String getCode(int varIndex, int index){
        String[] codeString = new String[1]; codeString[0]="";
        try{ tauArgus.GetVarCode(varIndex, index, new int[1], codeString, new int[1], new int[1]);
        } catch (Exception ex){}        
        return codeString[0];
    }
    
    public static String getCodeLevel(int varIndex, int index, int[] level){
        String[] codeString = new String[1]; codeString[0]="";
//        level = new int[1];
        try{ tauArgus.GetVarCode(varIndex, index, new int[1], codeString, new int[1], level);
        } catch (Exception ex){}        
        return codeString[0];
    }

    public static Integer getVarCodeLevelChildren (int varIndex, int index, int[] level){
      int[] nChild = new int[1];
       tauArgus.GetVarCodeProperties(varIndex, index, new int[1], new int[1], new int[1], level, nChild, new String[1]); 
       return nChild[0];
    } 
    
    public static void setVariable(int varIndex) throws ArgusException {
        Variable variable = Application.getVariable(varIndex); 
        int nMissings = variable.numberOfMissings();

        if (variable.metadata.dataOrigin == Metadata.DATA_ORIGIN_MICRO) {
            if (!tauArgus.SetVariable(variable.index, variable.bPos, variable.varLen, variable.nDecimals, nMissings, nMissings >= 1 ? variable.missing[0] : "",
                    nMissings >= 2 ? variable.missing[1] : "", variable.totCode, variable.type == tauargus.model.Type.REQUEST, variable.type == tauargus.model.Type.REQUEST ? variable.requestCode[0] : "", variable.type == tauargus.model.Type.REQUEST ? variable.requestCode[1] : "", variable.isCategorical(),
                    variable.isNumeric(), variable.type == tauargus.model.Type.WEIGHT, (variable.hierarchical != Variable.HIER_NONE), variable.type == tauargus.model.Type.HOLDING, variable.type == tauargus.model.Type.RECORD_KEY)) {
                throw new ArgusException("Error in specification of variable " + variable.name);
            }
        } else if (variable.metadata.dataOrigin == Metadata.DATA_ORIGIN_TABULAR) {
            if (!tauArgus.SetVariableForTable(variable.index, nMissings, nMissings >= 1 ? variable.missing[0] : "", 
                                              nMissings >= 2 ? variable.missing[1] : "", 
                                              variable.totCode, variable.nDecimals, variable.type == tauargus.model.Type.REQUEST, 
                                              variable.type == tauargus.model.Type.REQUEST ? variable.requestCode[0] : null, 
                                              (variable.hierarchical != Variable.HIER_NONE), variable.isNumeric(), variable.varLen)) {
                throw new ArgusException("Error in specification of variable " + variable.index);
            }
        } else {
            throw new ArgusException("Unsupported data format");
        }

        if (variable.hierarchical == Variable.HIER_FILE) {
            int errorCode = tauArgus.SetHierarchicalCodelist(variable.index, variable.metadata.getFilePath(variable.hierFileName), variable.leadingString);
            if (errorCode != 1) {
                throw new ArgusException("SetHierarchicalcodelist " + variable.hierFileName + "\nfor variable " + variable.name + " returned an error\nErrorCode : " + tauArgus.GetErrorString(errorCode));
            }
        }

        if (variable.hierarchical == Variable.HIER_LEVELS) {
            int nLevels = 0;
            for (int j = 0; j < Variable.MAX_NUMBER_OF_HIER_LEVELS; j++) {
                if (variable.hierLevels[j] != 0) {
                    nLevels = j + 1;
                }
            }
            if (!tauArgus.SetHierarchicalDigits(variable.index, nLevels, variable.hierLevels)) {
                throw new ArgusException("SetHierarchicalDigits for variable " + variable.name + " went wrong");
            }
        }
    }
    
    public static void setVariables() throws ArgusException {
        if (!tauArgus.SetNumberVar(Application.numberOfVariables())) {
            throw new ArgusException("SetNumberVar went wrong.");
        }

        for (int varIndex = 0; varIndex < Application.numberOfVariables(); varIndex++) {
            setVariable(varIndex);
        }
    }
    
    public static boolean DeleteFile (String Fn){
        File f1 = new File(Fn);
        boolean Oke = f1.delete();
        return Oke;
    }
    
    public static boolean ExistFile (String Fn){
        File f1 = new File(Fn);
        boolean Oke = f1.exists();
        return Oke;
    }
    
    public static String getFilePath(String  Fn){
      File f1 = new File(Fn);
      String hs; int l;
      hs = f1.getName();
      l = hs.length();
      hs = f1.getAbsolutePath();
      l= hs.length()-l;
      hs = hs.substring(0,l);
      return hs;        
    }
    
    public static String getFullFileName(String Fn) throws ArgusException{
        File f1 = new File(Fn);        
        String hs; hs = "";
        if (!ExistFile(Fn)){
            throw new ArgusException ("File: "+Fn + " does not exist.");
        }
        try{
            hs = f1.getCanonicalPath();
        }
        catch (IOException ex) {}
        return hs;
    }
    
    public static void getDataDirFromRegistry( javax.swing.JFileChooser fileChooser){
        String hs = SystemUtils.getRegString("general", "datadir", "");
        if (!hs.equals("")){
            File file = new File(hs); 
            fileChooser.setCurrentDirectory(file);
        }
    }
    
    public static void putDataDirInRegistry(String f){
        if (!f.equals("")){
          SystemUtils.putRegString("general", "datadir", f);  
        }  
    }
    
    public static String addPathExt(String fileName, String dataFile, String ext){
        String hs;
        hs = fileName;
        if (hs.equals("")){return hs;}
        if (!(fileName.contains(":") || fileName.contains("\\")|| fileName.contains("/"))){
         hs = getFilePath(dataFile) + hs;
        }
        if (FilenameUtils.getExtension(hs).equals("")) {hs = hs + ext;}
        return hs;
    }
        
    
    public static long FileLength(String Fn){
      File f1 = new File(Fn);
      long l = 0;
      if (f1.exists()){l=f1.length();}
      return l;
    }
    
    public static long FileLastModified (String Fn){
      File f1 = new File(Fn);
      long l = 0;
      l = f1.lastModified();
      return l;
    }
    
//   public static boolean DeleteFileWild2 (String fn, String pad){
//    
//    final String hs = fn;  Boolean Oke = true; 
//    final File folder = new File(pad);       
//    final File[] files = folder.listFiles( new FilenameFilter() {
//    @Override
//     public boolean accept( final File dir,
//                          final String name ) {
//       return name.matches( hs );
//      }
//    } );
//    for ( final File file : files ) {
//     if ( !file.delete() ) { Oke = false;
//         System.err.println( "Can't remove " + file.getAbsolutePath() );
//     }
//    }
//    return Oke;
//}

   public static boolean DeleteFileWild (String fn, String pad){
       walkDir(new File(pad), Pattern.compile(fn));
       return true;
   }   
   
   private static void walkDir(final File dir, final Pattern pattern) {
     final File[] files = dir.listFiles();
     String hs;
     if (files != null) {
       for (final File file : files) {
         if (file.isDirectory()) {
 //          walkDir(file, pattern); Not needed
         } else if (pattern.matcher(file.getName()).matches()) {
             hs = file.getName();
             file.delete();
 //         System.out.println("file to delete: " + file.getAbsolutePath());
         }
       }
     }
   }

    public static void renameFile( String fileFrom, String fileTo){
      File fFrom = new File(fileFrom);       
      File fTo = new File(fileTo);
      if (fTo.exists()) { fTo.delete();}
      fFrom.renameTo(new File(fileTo));
    }
    
    public static void writeBatchFileForExec(String fn, String bat){
      if ( Application.isAnco()){
        try {
        BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile(fn)+".bat"));
        out.write(bat); out.newLine();
        out.write("pause"); out.newLine();
        out.close();
        }
        catch (IOException ex){}   
      }
    }
    public static void writeBatchFileForExec(String fn, List<String> command){
      int j;  
      if ( Application.isAnco()){
        try {
        BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile(fn)+".bat")); 
        for (int i=0;i<command.size();i++){
            out.write(StrUtils.quote(command.get(i))+" ");
        }
        out.newLine();
        out.write("pause"); out.newLine();
        out.close();        
       }
        catch (IOException ex){} 
      }   
    }
    
    public static void ShowInfoMessage(String InfoString){
            if (!Application.isBatch()){
                JOptionPane.showMessageDialog(null, InfoString);
            }
        }
    
    public static int ShowWarningMessage(String WarningString){
        /* If interactive show a message
              if teh message is ignore: do nothing es raise an exception
           If Batch (both versions) write a message to the logfile*/
        int i;
        if (Application.isBatch()){
          SystemUtils.writeLogbook("WARNING: "+WarningString); 
          return 1;
                }
        else{ /* interactive*/
           i = JOptionPane.showConfirmDialog(null, WarningString+ "\nDo you want to continue?", 
                                             "Warning", JOptionPane.YES_NO_OPTION);
           if (i == JOptionPane.YES_OPTION) { return 1;}
           else                             { return 0;}
        }
    }

     public static String GetSimpleSepToken(String[] st, String sep) {
        Integer p;
        String token, hs;
        p = st[0].indexOf(sep);
        if (p == -1) {
            token = st[0];
            hs = "";
        } else {
            token = st[0].substring(0, p);
            hs = st[0].substring(p + 1).trim();
        }
        st[0] = hs;
        return token;
    }
     public static String GetSimpleToken(String[] st){
       return GetSimpleSepToken(st, " ");
     }
     
       public static String GetQuoteToken(String[] St) {
        Integer p;
        String token, hs;
        p = St[0].indexOf("'",1);
        if (p == -1) {
            token = St[0];
            hs = "";
        } else {
            token = St[0].substring(1, p);
            hs = St[0].substring(p + 1).trim();
        }
        St[0] = hs;
        return token;
    }
       
    public static String GetCplexLicenceFile() throws ArgusException {
        String hs;
        hs = SystemUtils.getRegString("optimal", "cplexlicensefile", "access.ilm");
        //if needed add the application path
        // Assumed non-UNC paths!!!!!
        // Added !hs.startsWith("\\") to exclude UNC paths for Windows in if-statement
        if (!hs.contains(":") && !hs.contains("/") && !hs.contains("//") && !hs.startsWith("\\")){
            try{
                hs = SystemUtils.getApplicationDirectory(Application.class).getCanonicalPath()+  "\\" + hs ;
            }
           catch (Exception ex){
               throw new ArgusException("Exception thrown: " + ex.getMessage());
           }
        }
        if (!TauArgusUtils.ExistFile(hs)){ throw new ArgusException("Cplex licence file: "+ hs + " could not be found");}
        return hs;
    }   
}
