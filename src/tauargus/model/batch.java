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

import argus.utils.SystemUtils;
import argus.utils.Tokenizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.apache.commons.io.FilenameUtils;
import tauargus.extern.dataengine.TauArgus;
import tauargus.gui.PanelTable;
import static tauargus.model.Application.clearMetadatas;
import static tauargus.model.Application.clearVariables;
import static tauargus.model.Metadata.DATA_ORIGIN_MICRO;
import tauargus.service.TableService;
import tauargus.utils.StrUtils;
import tauargus.utils.TauArgusUtils;
   

/**
 *
 * @author ahnl
 */
public class batch {
    public static final int BATCH_OK = 0;
    public static final int BATCH_GOINTERACTIVE= 1;
    public static final int BATCH_NORMAL_ERROR = 2;
    public static final int BATCH_READ_ERROR = 3;
    static Metadata metadata;
    static Tokenizer tokenizer;
    private static String batchDataPath = "";
    private static String batchFilePath = "";
    private static final TauArgus TAUARGUS = Application.getTauArgusDll();    
        
    private static final Logger LOGGER = Logger.getLogger(PanelTable.class.getName());        
       
/**
 * Writes progress info both to the progress window and the logbook
 * @param s the message string
 */    
    static public void reportProgress(String s){
        
        SystemUtils.writeLogbook(s);

        reportProgressInfo(s);
    }   
    
    static public void reportProgressInfo(String s){
        if(Application.windowInfoIsOpen){
            Application.windowInfo.addText(s);
        } else {
            System.out.println(s);            
        }    
    }  
    
    public static void setBatchDataPath (String f){
        batchDataPath = FilenameUtils.normalizeNoEndSeparator(f);
        if (!batchDataPath.endsWith("\\")){batchDataPath = batchDataPath + "\\";}
    }

    private static boolean checkBatchDataPath (){
        if ( !batchDataPath.equals("")){
            File f = new File(batchDataPath);
            if (!f.isDirectory()) {
                SystemUtils.writeLogbook("Argus batch data directory ("+batchDataPath+") could not be found");  
                return false;
            }
        }
        return true;
    }

    public static String getBatchDataPath(){
        return batchDataPath;
    }

    private static String giveRightFile (String f) throws ArgusException {
        String hs;
        // If the file name does not contain : or / or \ we assuem that the full path si given.
        //  else we first search in the directory of the arb file and then in the batch data directory (Param 4 when calling TAU)      
        hs = f;
        if ( !hs.contains(":") && !hs.contains("\\") && !hs.contains("/")){
            hs = batchFilePath + f;
            if (!TauArgusUtils.ExistFile(hs) ) {
                hs = getBatchDataPath() + f;
            }
        }
        if (!TauArgusUtils.ExistFile(hs)){
            throw new ArgusException ("file: "+ f + " could not be found.");
        }
        return hs;
    }
    
/**
 * The main routine for running the batch version.\n
 * Note: there is a distinction between running from the commandline and from the menu.\n
 * In case of an error the handling is different.\n
 * The routine is a big switch statement calling separate routines in a more complex case.
 * 
 * @param batchFile name of the batch file
 * @return the result of the batch run. (the BATCH_ parameters specified above)
 */
    public static int runBatchProcess(String batchFile){
        String token; 
        boolean tabularInput, firstCommand, silent; 
        int status;
        String dataFile, metaDataFile, hs; 
        String currentCommand = "";
        dataFile = ""; metaDataFile = ""; tabularInput = false;
        tokenizer = null;
        token = "";
        status = 0;
        if (!checkBatchDataPath()){ return BATCH_NORMAL_ERROR;}
        batchFilePath = TauArgusUtils.getFilePath(batchFile);
        firstCommand  = true; silent = false;
        if (Application.batchType() == Application.BATCH_FROMMENU) {
            Application.openInfoWindow(true);
            Application.windowInfo.clearText(); // Start with an empty canvas, even when restarting a batchjob in same session
            Application.windowInfo.addLabel("Progress of the batch proces");
        }    
        TAUARGUS.CleanAll();
        clearMetadatas();
        clearVariables();
        try{
            tokenizer = new Tokenizer(new BufferedReader(new FileReader(batchFile)));
        }catch (Exception ex) {
            if (Application.batchType() == Application.BATCH_FROMMENU){
                JOptionPane.showMessageDialog(null, "Argus batch file (" + batchFile + ") could not be opened");
            } else{
                SystemUtils.writeLogbook("Argus batch file (" + batchFile + ") could not be opened");  
            }
            return BATCH_NORMAL_ERROR;       
        }
        SystemUtils.writeLogbook("Start of batch procedure; file: " + batchFile);
//status 0 Start
//       1 Data/Tablefile found
//       2 Metafile found
//       3 SpecifyTables found       
//       4 SafetyRule found
        try{
            while (tokenizer.nextLine() != null) {
                currentCommand = tokenizer.getLine();
                reportProgress(currentCommand);
                token = tokenizer.nextToken();
                if (token.startsWith("\\\\") || token.startsWith("//")){token="<COMMENT>";}
                else{//test for first command to be silent
                    if ( firstCommand && !token.equals("<SILENT>")){
                        firstCommand = false; 
                        silent = false;  
                        //do sometning to initiate a progress window
                    }  
                }
                switch (token) {
                    case "<COMMENT>":
                        break;
                    case "<ANCO>": 
                        Application.setAnco(true); 
                        break;
                    case ("<JJ>"): //TODO Run a JJ file}
                        reportProgress("<JJ> not yet implemented. Skipping this command.");
                        break;
                    case ("<OPENMICRODATA>"):
                        dataFile =  tokenizer.nextToken();                  
                        tabularInput = false;
                        if (status != 0){ throw new ArgusException ("This keyword (" + token + ") is not allowed in this position"); }
                        status = 1;
                        dataFile = giveRightFile(dataFile);
                        break;
                    case ("<OPENTABLEDATA>"):
                        dataFile =  tokenizer.nextToken();
                        tabularInput = true;
                        if (!(status == 0 || status == 4)){ throw new ArgusException ("This keyword (" + token + ") is not allowed in this position"); }
                        dataFile = giveRightFile(dataFile);
                        status = 1;    
                        break;
                    case "<OPENMETADATA>":   
                        metaDataFile =  tokenizer.nextToken();
                        if (dataFile.equals("")){ throw new ArgusException ("A data file must be specified first"); }                       
                        if (status != 1){ throw new ArgusException ("This keyword (" + token + ") is not allowed in this position"); }
                        status = 2;
                        metaDataFile = giveRightFile(metaDataFile);
                        Metadata.createMetadata(tabularInput, dataFile, metaDataFile);
                        dataFile = "";
                        break;
                    case("<SPECIFYTABLE>"):
                        if (status != 2 && status != 4){ throw new ArgusException ("This keyword (" + token + ") is not allowed in this position"); }
                        if (!specifyTableBatch ()){throw new ArgusException ("Something wrong in specifyTableBatch()");}
                        status = 3;
                        break;
                    case ("<CLEAR>"):
                        TableService.clearTables();
                        TAUARGUS.CleanAll();
                        clearMetadatas();
                        clearVariables();
                        status = 0;
                        break;
                    case ("<SAFETYRULE>"):
                        if (status != 3)  { throw new ArgusException ("This keyword (" + token + ") is not allowed in this position"); }
                        if (!readSafetyRuleBatch()) {throw new ArgusException ("Something wrong in readSafetyRuleBatch()");}
                        status = 4;
                        break;
                    case ("<READMICRODATA>"):  
                        if (status != 4) { throw new ArgusException ("This keyword (" + token + ") is not allowed in this position"); }
                        if (!readMicrodataBatch()) {throw new ArgusException ("Something wrong in readMicrodataBatch()");}
                        reportProgress("Tables from microdata have been read");
                        break;
                    case ("<READTABLE>"): 
                        if (status == 3) { throw new ArgusException ("TODO Add automatic Use Status"); }
                        if (status != 4){ throw new ArgusException ("This keyword (" + token + ") is not allowed in this position"); }
//                        token = tokenizer.nextToken();
//                        if (token.equals("")){token = "0";}
//                        if (!token.equals("0") && !token.equals("1") && !token.equals("2")) { 
//                            throw new ArgusException ("Illegal parameter (" + token + ") for ReadTable");}
//                        int computeTotals = 0;
//                        if ( token.equals("1")){computeTotals = 1;}
//                        if ( token.equals("2")){computeTotals = 2;}
                        if (!readTablesBatch()) {throw new ArgusException ("Something wrong in readTablesBatch()");}
                        reportProgress("Tables have been read");
                        break;
                    case ("<APRIORY>"): 
                    case ("<APRIORI>"): 
                        //“Filename”, TabNo, Separator
                        boolean expandBogus = false, ignoreError = true;
                        String fName = tokenizer.nextToken();
                        hs = batchFilePath + fName;
                        if (!TauArgusUtils.ExistFile(fName) && TauArgusUtils.ExistFile(hs)){fName = hs;}                        
                        if (!TauArgusUtils.ExistFile(fName)){ throw new ArgusException ("Apriori file: " + fName + " could not be found");}
                        hs = tokenizer.nextChar();
                        if (!hs.equals(",")){throw new ArgusException ("a , was expected here");}
                        hs = tokenizer.nextField(",");
                        int tabno = StrUtils.toInteger(hs);
                        if ((tabno <= 0) || (tabno > TableService.numberOfTables() ) ){
                            throw new  ArgusException ("Illegal table number found: " + hs);}
                        tabno = tabno - 1;
                        if (!tokenizer.testNextChar().equals("\"")){
                            throw new ArgusException ("A quote was expected here before the separator");}
                        String sep = tokenizer.nextToken();  
                        if (sep.equals("")){ throw new ArgusException ("No separator was specified");}
                        hs = tokenizer.nextChar();
                        if (!hs.equals("")){
                            if (!hs.equals(",")) { throw new ArgusException ("A comma was expected here");}
                            hs = tokenizer.nextField(",");
                            if(!hs.equals("")){
                                if (hs.equals("1")){ignoreError=true;}
                                else if (hs.equals("0")){ignoreError=false;}
                                    else {throw new ArgusException("Illegal field (" + hs + ") for ignore error was specified");}
                            }
                            hs =tokenizer.nextField(",");
                            if (!hs.equals("")){expandBogus = hs.equals("1");}
                        }
                        int[][] aPrioryStatus = new int[5][2];
                        try{ TableSet.processAprioryFile(fName, tabno, sep, ignoreError, expandBogus, true, aPrioryStatus);}
                        catch (ArgusException ex ) {
                            TableSet.CloseAprioriFiles(expandBogus, aPrioryStatus);                            
                            throw new ArgusException ("An error has occurred when processing apriori file " + fName  );
                        }  
                        reportProgress("Apriori file " + fName + " has been read");
                        break;
                    case ("<COVER>"): // ProtectCoverTable = True
                        //Should only be used to protect the cover table  
                        //When TAU creates a batch sub-process to protect the cover table  
                        //Not for normal users; not in the manual.  
                        Application.setProtectCoverTable(true);
                        TableSet tableSet = TableService.getTable(0);
                        tableSet.additivity = TableSet.ADDITIVITY_NOT_REQUIRED;              
                        break;
                    case ("<SUPPRESS>"):  
                        if (status != 4) {throw new ArgusException ("This keyword (" + token + ") is not allowed in this position");}
                        suppressBatch();
                        break;
                    case ("<WRITETABLE>"): 
                        writeTableBatch();
                        break;  
    //Was used by Space Time Research, no longer supported
    //              case ("<REPORTSTR>"):   
    //                  break;    
    // Case "<REPORTSTR>": If Not ReportSTR(Staart) Then GoTo FOUTEINDE
    // Something special for Space Time Reserach
                    case ("<RECODE>"):
                        reportProgress("<RECODE> not yet implemented. Skipping this command.");
                        break;
                    case ("<SOLVER>"):
                        hs = tokenizer.nextField(",");
                        if (hs.equalsIgnoreCase("XPRESS")){
                            Application.solverSelected = Application.SOLVER_XPRESS;  
                        }
                        else{
                            if (hs.equalsIgnoreCase("CPLEX")){
                                Application.solverSelected = Application.SOLVER_CPLEX;  
                            }
                            else
                                if (hs.equalsIgnoreCase("FREE")){
                                    Application.solverSelected = Application.SOLVER_SOPLEX;                            
                                }
                                else{
                                    throw new ArgusException("Unknown solver (" + hs + ") selected");
                                }
                        }
                        // check for license file
                        hs = tokenizer.nextField(",");
                        if (!hs.isEmpty()){
                            hs = StrUtils.unQuote(hs);
                            if (!TauArgusUtils.ExistFile(hs)){
                                throw new ArgusException("Cplex License file (" + hs + ") does not exist");                               
                            }
                            hs = TauArgusUtils.getFullFileName(hs);
                            SystemUtils.putRegString("optimal", "cplexlicensefile", hs); 
                        }
                        break;
                    case ("<GOINTERACTIVE>"):   
                        return 1;     
                    case ("<LOGBOOK>"): 
                        hs = StrUtils.unQuote(tokenizer.getLine());
                        SystemUtils.setLogbook(hs);
                        tokenizer.clearLine();
                        break;    
                    case ("<VERSIONINFO>"): 
                        hs = StrUtils.unQuote(tokenizer.getLine());
                        writeVersionInfo(hs);
                        tokenizer.clearLine();
                        break;                   
                    default:
                        throw new ArgusException ("Unknown keyword " + token);
                }   // end switch
            }
            tokenizer.close();
        }
        catch (ArgusException ex) {
            if (Application.batchType()== Application.BATCH_FROMMENU) {
                SystemUtils.writeLogbook("Error in batch file");   
                JOptionPane.showMessageDialog(null, "Error in batch file\nCommand: " + currentCommand + "\n" + ex.getMessage());
            }
            else {SystemUtils.writeLogbook("Error in batch file");}
            return BATCH_NORMAL_ERROR;                  
        }
        finally {
            if (Application.batchType()== Application.BATCH_FROMMENU){
                Application.openInfoWindow(false);
                Application.setBatch(Application.BATCH_NOBATCH); // Makes sure you can use the menu afterwards
            }
        }
        return BATCH_OK;
    }


    static void writeVersionInfo (String f) throws ArgusException{
        if (!f.contains(":") && !f.contains("\\") && !f.contains("/")){
            if (batchDataPath.equals("")){
                f = batchFilePath + f;
            } else {
                f = getBatchDataPath() + f;
            }
        }
        try(BufferedWriter out = new BufferedWriter(new FileWriter(f))){
            out.write("TAU-ARGUS version: " + Application.getFullVersion() + "; build: " + Application.BUILD);
        } 
        catch(IOException ex){throw new ArgusException ("An error occurred when writing the version info file");}          
    }
   
   /**
   * Writes a table in batch. 
   * Reads the commend and all its parameters.
   * Then performs the actual write process
   * @throws ArgusException 
   **/
    static void writeTableBatch()throws ArgusException{
        String[] tail = new String[1];  String hs, optString, plusMin; 
        TableSet tableSet;
        tail[0]=tokenizer.getLine(); Integer tabNo, outputType;
        tokenizer.clearLine();
        hs = nextChar(tail);
        if ( !hs.equals("(") ) {throw new ArgusException("( expected; not a " + hs + "");}
        hs = nextToken(tail);   
        tabNo = Integer.parseInt(hs);
        if (tabNo > TableService.numberOfTables()){throw new ArgusException("Illegal Table number");}
        tabNo = tabNo -1;
        hs = nextChar(tail);
        if ( !hs.equals(",") ) {throw new ArgusException(", expected; not a " + hs + "");}
        hs = nextToken(tail);
        // OutputType as specified (start counting from 1)
        // 1: CSV, 2: CSV for pivot, 3: Code Value, 4: SBS, 5: Intermediate, 6: JJ, 7: CKM
        outputType = Integer.parseInt(hs);
        if (outputType < 1 || outputType > 7) {throw new ArgusException("Unknown output type (" + hs + ":");}
        // OutputType for internal use (start counting from 0)
        // 0: CSV, 1: CSV for pivot, 2: Code Value, 3: SBS, 4: Intermediate, 5: JJ, 6: CKM
        outputType--;
        hs = nextChar(tail);
        if ( !hs.equals(",") ) {throw new ArgusException(", expected; not a " + hs + "");}
        //First the defaults:
        SaveTable.writeAddStatus = false;
        SaveTable.writeSupppressEmpty = false;
        SaveTable.writeVarnamesOnFirstLine = (outputType == 1);
        SaveTable.writeEmbedQuotes = true;
        SaveTable.writeSBSHierarchicalLevels = false;
        SaveTable.writeIntermediateStatusOnly = false;
        SaveTable.writeIntermediateAddAudit = false;
        SaveTable.writeIntermediateUseHolding = false;
        SaveTable.writeJJRemoveBogus = false;
        SaveTable.writeCKMOriginalValues = false;
        SaveTable.writeCKMDifferences = false;
        SaveTable.writeCKMCellKeys = false;
        hs = nextToken(tail).toUpperCase();
        //loop over the different options
        while (! hs.equals("")){
            if (hs.length() < 2){throw new ArgusException("Unknown output type (" + hs + ")");}
            optString = hs.substring(0,2);
            hs = hs.substring(2).trim();
            plusMin =  hs.substring(0,1);
            hs = hs.substring(1).trim();
            if (!plusMin.equals("+") && !plusMin.equals("-")){throw new ArgusException("+ or - expected; not a " + plusMin + "");}
            switch (optString){
                case "HL": {SaveTable.writeSBSHierarchicalLevels = plusMin.equals("+");break;} //Add Hier level
                case "SO": {SaveTable.writeIntermediateStatusOnly = plusMin.equals("+");break;} //Status Only
                case "AR": {SaveTable.writeIntermediateAddAudit = plusMin.equals("+");break;} //Add Audit Results
                case "HI": {SaveTable.writeIntermediateUseHolding = plusMin.equals("+");break;} //Use Holding info
                case "AS": {SaveTable.writeAddStatus = plusMin.equals("+");break;} //Add Status
                case "SE": {SaveTable.writeSupppressEmpty = plusMin.equals("+");break;} //Suppress Empty
                case "FL": {SaveTable.writeVarnamesOnFirstLine = plusMin.equals("+");break;} //Varnames on First Line
                case "QU": {SaveTable.writeEmbedQuotes = plusMin.equals("+");break;} //Embed in Quotes YES
                case "TR": {SaveTable.writeJJRemoveBogus = plusMin.equals("+");break;} //Remove Trivial Levels
                case "AO": {SaveTable.writeCKMOriginalValues = plusMin.equals("+");break;} //Add original cell values
                case "AD": {SaveTable.writeCKMDifferences = plusMin.equals("+");break;} //Add difference original perturbed
                case "AC": {SaveTable.writeCKMCellKeys = plusMin.equals("+");break;} //Add Cellkey
                default: {throw new ArgusException("Unknown output type ("+ optString + ")");}    
            }
        }
        hs = nextChar(tail);
        if ( !hs.equals(",") ) {throw new ArgusException(", expected; not a " + hs + "");}
        hs = nextToken(tail);
        tableSet = TableService.getTable(tabNo);
        if (!hs.contains(":") && !hs.contains("\\") && !hs.contains("/")){
            if (batchDataPath.equals("")){
                hs = batchFilePath + hs;
            } else {
               hs = getBatchDataPath() + hs;
            }
        }
        tableSet.safeFileName = hs;
        if (((outputType == 3 || outputType == 4) && (tableSet.ckmProtect || tableSet.ctaProtect)) ||
            ((outputType == 5) && (tableSet.rounded)) || ((outputType == 6) && (!tableSet.ckmProtect))) 
            {throw new ArgusException("Chosen output type " + Integer.toString(outputType+1) + " is not allowed for this type of protection\n");}
        SaveTable.writeTable (tableSet, outputType);
        SaveTable.writeReport(tableSet);
    }
    
    static boolean readTablesBatch()throws ArgusException{
        try{
            String token, hs;
            String[] tail = new String[1];
            int additivity;
            boolean keepStatus = false;
            tail[0] = tokenizer.nextToken();
            hs = nextChar(tail);
            if (hs.equals("")){hs = "0";}
            if (!hs.equals("0") && !hs.equals("1") && !hs.equals("2")) { 
                throw new ArgusException ("Illegal parameter (" + hs + ") for ReadTable");}
            additivity = Integer.parseInt(hs); // additivity will be 0, 1 or 2
            if (!tail[0].equals("")){
                hs = nextChar(tail);
                if (hs.equals("T")||hs.equals("t")) keepStatus = true;
                else {
                    if (hs.equals("F")||hs.equals("f")) keepStatus = false;
                    else throw new ArgusException("Invalid option " + hs + "\n");
                }
            }
            
            TableService.addAdditivityParamBatch(additivity, keepStatus);
            TableService.readTables(null);
        }catch (IOException ex){
            TableService.clearTables();
            throw new ArgusException("\nError reading tables.\n"+ex.getMessage());
        }catch (ArgusException ex){
            if (Application.batchType()== Application.BATCH_FROMMENU) {JOptionPane.showMessageDialog(null, ex.getMessage()+"; something wrong");}
            TableService.clearTables();
            return false;
        }
        return true;
    }

    static boolean readMicrodataBatch() throws ArgusException {
        Date startDate = new Date(); 
        long timeDiff;
        TableService.readMicrodata(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {}
        });
        Date endDate = new Date();
        timeDiff = (endDate.getTime() - startDate.getTime()) / 1000;
        SystemUtils.writeLogbook("Micro data file read; processing time " + timeDiff + " seconds");
        return true;
    }

/**
 * Read the <SUPPRESS> command in a batch file with all its parameters 
 * and calls the required suppression method
 * @throws ArgusException 
 */
    static void suppressBatch() throws ArgusException{
        String SuppressType, token, hs; 
        int i;
        boolean linked;
        String[] tail = new String[1];
        final TableSet tableset;
  
        SuppressType = tokenizer.nextField("(").toUpperCase();        
        tail[0]=tokenizer.getLine();
        tokenizer.clearLine();
        token = nextToken(tail);
        int n = Integer.parseInt(token);
        if (n < 0 || n > TableService.numberOfTables()){throw new ArgusException("Wrong table number in <SUPPRESS>");}
        linked = (n == 0);
        if (linked) {
            if (!(SuppressType.equals("GH") || SuppressType.equals("MOD"))){
                throw new ArgusException("Linked tables is only possible for Modular or Hypercube");
            }  
            for (i=0;i<TableService.numberOfTables();i++){TableService.undoSuppress(i);}
            tableset = tauargus.service.TableService.getTable(0);
        } 
        else {
            tableset = tauargus.service.TableService.getTable(n-1);
            TableService.undoSuppress(n-1);            
        }
        
        switch (SuppressType){
            case "GH"://TabNo, A priori Bounds Percentage, ModelSize, ApplySingleton) 
                token = nextChar(tail);
                if (token.equals(",")){
                    token = nextToken(tail);
                    tableset.ghMiterAprioryPercentage = Integer.parseInt(token);
                    token = nextChar(tail);                   
                }
                if (token.equals(",")){
                    token = nextToken(tail);
                    tableset.ghMiterSize = Integer.parseInt(token);
                    token = nextChar(tail);                   
                }
                if (token.equals(",")){
                    token = nextToken(tail);
                    tableset.ghMiterApplySingleton = token.equals("1");
                    token = nextChar(tail);                   
                }
                if (linked){
                    LinkedTables.buildCoverTable();  
                    LinkedTables.runLinkedGHMiter();
                    hs = "";
                    TableSet ts;
                    for (i=0;i<TableService.numberOfTables();i++){
                      ts = tauargus.service.TableService.getTable(i);
                      hs = hs + ts.CountSecondaries()+ " cells have been suppressed in table " + (i+1) + "\n";
                    }
                    hs = hs.substring(0, hs.length()-1);                            
                } 
                else { 
                    GHMiter.RunGHMiter(tableset);
                    hs = tableset.CountSecondaries()+ " cells have been suppressed";
                }  
                reportProgress("The hypercube procedure has been applied\n" + hs);
                break;
            case "MOD"://TabNo, MaxTimePerSubtable
                token = nextChar(tail);
                if (token.equals(",")){
                    token = nextToken(tail);
                    tableset.maxHitasTime = Integer.parseInt(token);
                    Application.generalMaxHitasTime = tableset.maxHitasTime;
                    // The generalMAxHitasTime is used in the RunModular procedure.
                    token = nextChar(tail); 
                }
                i=0;
                while (token.equals(",") && (i<3)){
                    i++;
                    token = nextToken(tail);
                    switch (i){
                        case 1: tableset.singletonSingletonCheck = token.equals("1"); break;
                        case 2: tableset.singletonMultipleCheck = token.equals("1"); break;
                        case 3: tableset.minFreqCheck = token.equals("1"); break;
                    }
                    token = nextChar(tail);
                }
                if (linked){
                    LinkedTables.buildCoverTable();  
                    LinkedTables.runLinkedModular(null);
                } 
                else{ // single table
                    if (token.equals(",")){ //MSC specified
                        token = nextToken(tail);
                        tableset.maxScaleCost = StrUtils.toDouble(token);
                        if (tableset.maxScaleCost <= 0){
                            throw new ArgusException("Illegal Max Scaling Cost: " + token);                        
                        }
                    }
                    try{ // Make sure that PropertyChanges are not processed in batch-mode by overriding propertyChange to do nothing
                        if (Application.batchType() == Application.BATCH_COMMANDLINE){  
                            OptiSuppress.RunModular(tableset, new PropertyChangeListener(){
                                @Override
                                public void propertyChange(PropertyChangeEvent evt){ }});
                            reportProgressInfo("The modular procedure has been applied\n" + 
                                                tableset.CountSecondaries() + " cells have been suppressed");
                        }
                        else{
                            final SwingWorker <Integer, Void> worker = new ProgressSwingWorker<Integer, Void>(ProgressSwingWorker.DOUBLE,"Modular approach") {
                                @Override
                                protected Integer doInBackground() throws ArgusException, Exception{
                                    super.doInBackground();
                                    OptiSuppress.RunModular(tableset, getPropertyChangeListener());
                                    reportProgressInfo("The modular procedure has been applied\n" + 
                                                        tableset.CountSecondaries() + " cells have been suppressed");
                                    return null;
                                }
                                @Override
                                protected void done(){
                                    super.done();
                                    try{
                                        get();
                                        tableset.undoAudit();
                                    } 
                                    catch (InterruptedException ex) {LOGGER.log(Level.SEVERE, null, ex);} 
                                    catch (ExecutionException ex) {JOptionPane.showMessageDialog(null, ex.getCause().getMessage());}
                                }
                            };
                            worker.execute();
                            while (!worker.isDone()){
                                try{Thread.sleep(1000);}
                                catch (InterruptedException ex) {}
                            }
                        }
                    }
                    catch (IOException  ex){
                        throw new ArgusException("Modular failed\n" + ex.getMessage());
                    }
                }
                break;
            case "OPT"://TabNo, MaxComputingTime
                if (n==0) {throw new ArgusException("Linked tables is not possible for Optimal");}
                token = nextChar(tail);
                if (token.equals(",")){
                    token = nextToken(tail);
                    tableset.maxTimeOptimal = Integer.parseInt(token);
                    token = nextChar(tail);    
                }
                
                if (Application.batchType() == Application.BATCH_COMMANDLINE){  
                    try{
                        OptiSuppress.RunOptimal(tableset, new PropertyChangeListener(){
                            @Override 
                            public void propertyChange(PropertyChangeEvent evt){} }, false, false, 1);
                    }catch (IOException ex) {}
                }  
                else { // From menu with swingworker
                    final SwingWorker <Integer, Void> worker = new ProgressSwingWorker<Integer, Void>(ProgressSwingWorker.DOUBLE,"Modular approach") {
                        @Override
                        protected Integer doInBackground() throws ArgusException, Exception{
                            super.doInBackground(); 
                            try{
                                OptiSuppress.RunOptimal(tableset, new PropertyChangeListener(){
                                    @Override
                                    public void propertyChange(PropertyChangeEvent evt){} }, false, false, 1);
                            }
                            catch (IOException ex) {}                    
                            return null;
                        }

                        @Override
                        protected void done(){
                            super.done();
                            try{ get(); } 
                            catch (InterruptedException ex) { LOGGER.log(Level.SEVERE, null, ex); } 
                            catch (ExecutionException ex) { JOptionPane.showMessageDialog(null, ex.getCause().getMessage()); }
                        }
                    };
                    worker.execute();
                    while (!worker.isDone()){
                        try{Thread.sleep(1000);}
                        catch (InterruptedException ex) {}
                    }  
                }
                reportProgressInfo("The optimal procedure has been applied\n" + 
                                    tableset.CountSecondaries() + " cells have been suppressed");
                break;
            case "RND"://TabNo, RoundingBase, Steps, Time, Partitions, StopRule
                        // OK after Roundbase: Default Step = 0 (no step), time = 10, Nopartitions part = 0, stoprule = 2(optimal)
                if (n==0) {throw new ArgusException("Linked tables is not possible for Rounding");}
                if (Application.solverSelected == Application.SOLVER_CPLEX)
                    reportProgressInfo("Whether controlled rounding can be used when Cplex is selected as solver, " +
                                       "depends on your specific license.\n Incorrect license may cause errors.");
                token = nextChar(tail);
                if (!token.equals(",")){throw new ArgusException("a komma(,) expected");}
                token = nextToken(tail);
                tableset.roundBase  = Integer.parseInt(token);
                long mrb = TableSet.computeMinRoundBase(tableset);
                if (tableset.roundBase < mrb){
                    throw new ArgusException("The rounding base is too small\n"+
                                             "A minimum of " + mrb + " is required");
                }
                //set the defaults
                tableset.roundMaxStep = 0;
                tableset.roundMaxTime = 10;
                tableset.roundPartitions = 0;
                tableset.roundStoppingRule = 2;                
                token = nextChar(tail); 
                
                if (token.equals(",")){ //steps
                    token = nextToken(tail); 
                    tableset.roundMaxStep  = StrUtils.toInteger(token); 
                    token = nextChar(tail); 
                }else{
                    if (!token.equals(")"))throw new ArgusException("a komma(,) or \")\" expected");
                }

                if (token.equals(",")){ //max time
                    token = nextToken(tail);
                    tableset.roundMaxTime  = Integer.parseInt(token); 
                    if (tableset.roundMaxTime <= 0)
                        throw new ArgusException("Illegal value for max time: " + tableset.roundMaxTime );
                    token = nextChar(tail); 
                }else{
                    if (!token.equals(")"))throw new ArgusException("a komma(,) or \")\" expected");
                }
                                
                if (token.equals(",")){ //partitions
                    token = nextToken(tail);
                    tableset.roundPartitions  = Integer.parseInt(token); 
                    if (tableset.roundPartitions < 0 || tableset.roundPartitions > 1)
                        throw new ArgusException("Illegal value for partitions: " + tableset.roundPartitions );
                    token = nextChar(tail); 
                }else{
                    if (!token.equals(")"))throw new ArgusException("a komma(,) or \")\" expected");
                }
                
                if (token.equals(",")){ //Stop rule
                    token = nextToken(tail);
                    tableset.roundStoppingRule  = Integer.parseInt(token); 
                    if (tableset.roundStoppingRule <= 0 || tableset.roundStoppingRule > 2)
                        throw new ArgusException("Illegal value for max time: " + tableset.roundStoppingRule );
                    token = nextChar(tail); 
                }else{
                    if (!token.equals(")")) throw new ArgusException("a komma(,) or \")\" expected");
                }
                
                if (token.equals(",")){ //Unit cost function
                    token = nextToken(tail);
                    if (token.equals("1")){
                        tableset.roundUnitCost = true;
                    }
                    else if (token.equals("0")){  
                        tableset.roundUnitCost = false;
                    }
                    else {throw new ArgusException("Illegal value UnitCost parameter: only 0 or 1 allowed");
                    }
                    token = nextChar(tail); 
                }else{
                    if (!token.equals(")"))throw new ArgusException("a komma(,) or \")\" expected");
                }
                
                // all parameters have been handeled. Run the rounder.
                if (Application.batchType() == Application.BATCH_COMMANDLINE){
                    try{
                        OptiSuppress.RunRounder(tableset, new PropertyChangeListener(){
                                                                    @Override
                                                                    public void propertyChange(PropertyChangeEvent evt){}
                                                                });
                    } catch (IOException ex) {throw new ArgusException(ex.getMessage());}
                }
                else //batch run from menu
                {
                    final SwingWorker <Integer, Void> worker = new ProgressSwingWorker<Integer, Void>(ProgressSwingWorker.ROUNDER,"Rounding") {
                    @Override
                    protected Integer doInBackground() throws ArgusException, Exception{
                        super.doInBackground(); 
                        try{
                            OptiSuppress.RunRounder(tableset, new PropertyChangeListener(){
                                                            @Override
                                                            public void propertyChange(PropertyChangeEvent evt){}
                                                        });
                        }
                        catch (IOException ex) {}                    
                        return null;
                    }

                    @Override
                    protected void done(){
                        super.done();
                        try{
                            get();
                        }
                        catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                        } catch (ExecutionException ex) {
                            JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                        }
                    }
                    };
                    worker.execute();
                    while (!worker.isDone()){
                        try{Thread.sleep(1000);}
                        catch (InterruptedException ex) {}
                    }  
                }
                reportProgressInfo("The table has been rounded\n" +
                                   "Number of steps: " + tableset.roundMaxStep + "\n" +
                                   "Max step: " + StrUtils.formatDouble(tableset.roundMaxJump, tableset.respVar.nDecimals));
                break;
            case "CTA":
                try{            
                    OptiSuppress.RunCTA (tableset, false);
                    reportProgress("CTA run completed");
                }catch (IOException ex){throw new ArgusException (ex.getMessage());}                
                break;   
            case "NET":
                OptiSuppress.TestNetwork(tableset);
                OptiSuppress.RunNetwork(tableset);
                break;
            case "CKM":
                // For frequency count tbales: TabNo ptablefile (optional)
                //      e.g., CKM(1) "ptableFREQ"
                // For magnitude tables: TabNo ptablefileCONT (optional) | ptablefileSEP (optional) | muC (optional)
                //      e.g., CKM(1) "ptableCONT" | "ptableSEP" | 2.7 Everything specified
                //            CKM(1) | "ptableSEP" | 2.7   Only ptablefileSEP and muC specified
                //            CKM(1) "ptableCONT" | | 2.7  Only ptablefileCONT and muC specified
                //            CKM(1) "ptableCONT"||  Only ptableCONT specified
                //            CKM(1) ||2.7  Only muC specified
                if (!tableset.CellKeyAvailable){
                    throw new ArgusException("CKM is not possible: no recordkey available");
                }
                
                if (tableset.holding){
                    throw new ArgusException("Sorry, Cell Key Method not available when \"holdings\" are used");
                }

                if (!readCKMparams(tail, tableset.cellkeyVar,tableset.respVar)) {}
                else {reportProgress("Some CKM parameters are read from batch file");}

                String hs1, hs2;
                
                try {
                    if (tableset.respVar.isResponse()){
                        hs1 = TauArgusUtils.getFullFileName(giveRightFile(tableset.cellkeyVar.PTableFileCont));
                        if (!TauArgusUtils.ExistFile(hs1)){
                            throw new ArgusException("\"ptable file (" + hs1 + ") does not exist\"");
                        }
                        hs2 = TauArgusUtils.getFullFileName(giveRightFile(tableset.cellkeyVar.PTableFileSep));
                        if (!TauArgusUtils.ExistFile(hs2) && !tableset.cellkeyVar.PTableFileSep.equals("")){
                            throw new ArgusException("\"ptable file (" + hs2 + ") does not exist\"");
                        }
                        if (OptiSuppress.RunCellKeyCont(tableset, hs1, hs2)){
                            reportProgress("Using ptable files " + hs1 + " and " + hs2);
                        }
                    }
                    else{
                        hs1 = TauArgusUtils.getFullFileName(giveRightFile(tableset.cellkeyVar.PTableFile));
                        if(OptiSuppress.RunCellKey(tableset, hs1)){
                            reportProgress("Using ptable file " + hs1);
                        }
                    }
                }
                catch (ArgusException | IOException ex){
                    throw new ArgusException(ex.getMessage());
                }
                break;  
            default: 
                throw new ArgusException ("Unknown suppression method (" + SuppressType + ") found");
        }   
    }
    
    /**
     * Usage:
     * 
     * For frequency count tables: TabNo ptablefileFREQ (optional)
     *      e.g., CKM(1) "ptableFREQ"
     *            CKM(1)
     * For magnitude tables: TabNo ptablefileCONT (optional) | ptablefileSEP (optional) | muC (optional)
     *      e.g., CKM(1) "ptableCONT" | "ptableSEP" | 2.7   //Everything specified
     *            CKM(1) | "ptableSEP" | 2.7)               //Only ptablefileSEP and muC specified
     *            CKM(1) "ptableCONT" || 2.7)               //Only ptablefileCONT and muC specified
     *            CKM(1) "ptableCONT"||                     //Only ptableCONT specified
     *            CKM(1) ||2.7                              //Only muC specified
     *            CKM(1)                                    //Nothing specified
     * @param tail
     * @param cellkeyVar
     * @param respVar
     * @return
     * @throws ArgusException 
     */
    static boolean readCKMparams(String[] tail, Variable cellkeyVar, Variable respVar) throws ArgusException{
        String token, hs;
        token = nextChar(tail);
        if (!token.equals(")")){throw new ArgusException("A ) is expected here");}
        // tail[0] now equals remainder of line that started with CKM(TabNo)
        if (tail[0].equals("")) return false; // Can be empty: then do nothing and return false
        
        if (respVar.isResponse()){
        // tail now contains a token delimiter ( , ) or |
            if (!testNextToken(tail)) return false;
            token = nextToken(tail); 
            if (!token.equals("")){  // first parameter should be ptableCONT
                if (token.substring(0,2).equals("//")) return false; // Comment, so do nothing and return false
                cellkeyVar.PTableFileCont = giveRightFile(token);
            }
            token = nextChar(tail);
            if (token.equals("|")){
                token = nextToken(tail);
                if (!token.equals("")){  // second paramter should be ptableSEP
                    if (token.substring(0,2).equals("//")) return false; // Comment, so do nothing and return false
                    cellkeyVar.PTableFileSep = giveRightFile(token);
                }
                token = nextChar(tail);
                if (token.equals("|")){
                    if (!tail[0].equals("")){ // remainder should be muC
                        try{
                            respVar.muC = Double.parseDouble(tail[0]);
                        }catch(Exception ex){
                            throw new ArgusException("muC = " + tail[0] + " is not a double");
                        }
                    }
                }
            }
        }
        else{// Frequency count table so only one optional parameter
            if (!tail[0].equals("")){ // there should be some filename
                hs = StrUtils.unQuote(tail[0].trim());
                if (hs.substring(0,2).equals("//")) 
                    return false; // Comment, so do nothing and return false
                cellkeyVar.PTableFile = giveRightFile(hs);
            }
        }
        return true;
    }
    
    static boolean readSafetyRuleBatch() throws ArgusException{
        String token, ruleType;
        String[] tail = new String[1];
        int nPRule=0; int nNKRule = 0; int nFreqRule = 0; int nPiepRule = 0;
 //<SAFETYRULE>     P(25,100,1)|P(0,100,0)|FREQ(3,30)|       
        TableSet tableset = tauargus.service.TableService.getTable(tauargus.service.TableService.numberOfTables()-1);
//       int TopN = tableset.metadata.numberOfTopNVariables();
//TODO We need to check whetehr the different rules are possible, given the metadata       
        ruleType = tokenizer.nextField("(").toUpperCase();
        if (ruleType.equals("")) {// No safety rule so use given status}
            tableset.useStatusOnly = true;
            return true;
        }
        tableset.useStatusOnly = true;
        tail[0] = "("+tokenizer.getLine();
        tokenizer.clearLine();
       
        while (!ruleType.equals("")){
            token = nextChar(tail);
            if ( !token.equals("(") ){ throw new ArgusException ("A ( is expected here"); }
            token = nextToken (tail); // everything should start with a (           
            if (!ruleType.equals("MAN")){tableset.useStatusOnly = false;}
            switch (ruleType) {
                case "P": //P: (p,n) with the n optional. (default = 1). 
                    if (tableset.isFrequencyTable()){throw new ArgusException ("p% rule cannot be applied to a frequency table");} 
                    if (nPRule > 3) {throw new ArgusException ("Too many p% rules specified");}
                    tableset.pqRule = true;
                    tableset.pqP[nPRule] = Integer.parseInt(token);
                    tableset.pqQ[nPRule] = 100;
                    tableset.pqN[nPRule] = 1;
                    token = nextChar(tail);
                    if (token.equals(",")){
                        token = nextToken (tail);  
                        tableset.pqN[nPRule] = Integer.parseInt(token);                  
                        token = nextChar(tail);
                    }
                    if (token.equals(",")){
                        token = nextToken (tail);
                        tableset.pqQ[nPRule] = tableset.pqN[nPRule];
                        tableset.pqN[nPRule] = Integer.parseInt(token);                  
                        token = nextChar(tail);
                    }      
                    if ((nPRule > 1) && (tableset.pqP[nPRule]>0))
                        tableset.holding = true;
                    nPRule++;
                    break;
                case "NK": //NK: (n,k). A n,k-dominance rule 
                    if (tableset.isFrequencyTable()){throw new ArgusException ("Dominance rule cannot be applied to a frequency table");} 
                    if (nNKRule > 3) {throw new ArgusException("Too many nk rules specified");}
                    tableset.domRule = true;
                    tableset.domN[nNKRule] = Integer.parseInt(token);  
                    token = nextChar(tail);
                    if ( !token.equals(",") ) { throw new ArgusException ("A \",\" is expected here"); }
                    token = nextToken (tail); 
                    tableset.domK[nNKRule] = Integer.parseInt(token);  
                    if ((nNKRule > 1) && (tableset.domK[nNKRule]>0)) 
                        tableset.holding = true;
                    nNKRule++;
                    token = nextChar(tail);
                    break;
                case "FREQ": //FREQ:(MinFreq, FrequencySafetyRange)
                    if (nFreqRule > 1) {throw new ArgusException("Too many frequency rules specified");}
                    tableset.frequencyRule = true;
                    tableset.minFreq[nFreqRule] = Integer.parseInt(token);  
                    token = nextChar(tail);
                    if ( !token.equals(",") ) { throw new ArgusException ("A \",\" is expected here"); }
                    token = nextToken (tail); 
                    tableset.frequencyMarge[nFreqRule] = Integer.parseInt(token);  
                    if ((nFreqRule > 0) && (tableset.minFreq[nFreqRule]>0))
                        tableset.holding = true;
                    nFreqRule++;
                    token = nextChar(tail);
                    break;
                case "ZERO": //ZERO: (ZeroSafetyRange)
                    if (tableset.isFrequencyTable()){throw new ArgusException ("Zero rule cannot be applied to a frequency table");} 
                    tableset.zeroUnsafe = true;
                    tableset.zeroRange = Double.parseDouble(token);
                    token = nextChar(tail);
                    break;
                case "REQ": //REQ: (Percent1, Percent2, MinFreq, SafetyMargin)
                    if (tableset.isFrequencyTable()){throw new ArgusException ("Request rule cannot be applied to a frequency table");} 
                    if (nPiepRule > 1) {throw new ArgusException("Too many request rules specified");}
                    tableset.piepRule[nPiepRule] = true;
                    tableset.piepPercentage[2*nPiepRule] = Integer.parseInt(token);   
                    token = nextChar(tail);
                    if ( !token.equals(",") ) { throw new ArgusException ("A \",\" is expected here"); }
                    token = nextToken (tail); 
                    tableset.piepPercentage[2*nPiepRule+1] = Integer.parseInt(token);   
                    token = nextChar(tail);
                    if ( !token.equals(",") ){ throw new ArgusException ("A \",\" is expected here"); }
                    token = nextToken (tail); 
                    tableset.piepMinFreq[nPiepRule] = Integer.parseInt(token);                 
                    token = nextChar(tail);
                    if ( !token.equals(",") ) { throw new ArgusException ("A \",\" is expected here"); }
                    token = nextToken (tail);
                    tableset.piepMarge[nPiepRule] = Integer.parseInt(token);   
              
                    if ((nPiepRule > 0) && (tableset.piepPercentage[2*nPiepRule] > 0))
                        tableset.holding = true;
                    nPiepRule++;
                    token = nextChar(tail);
                    break;
                case "WGT": //WGT: 0 no weights are used,  1 = apply weights 
                    if (!(token.equals("1") || token.equals("0"))) { throw new ArgusException ("A 0 or a 1 is expected here"); }
                    tableset.weighted = token.equals("1");
                    token = nextChar(tail);
                    break;
                case "MIS": //MIS: 0 = cells with a missing code are unsafe if the safety-rules are violated; 1 = these cells are always safe. 
                    if (!(token.equals("1") || token.equals("0"))) { throw new ArgusException ("A 0 or a 1 is expected here"); }
                    tableset.missingIsSafe = token.equals("1");
                    token = nextChar(tail);
                    break;
                case "MAN": //MAN: (Manual safety margin). 
                    tableset.manualMarge = Integer.parseInt(token); 
                    token = nextChar(tail);
                    break;
                default:  throw new ArgusException ("Unknown rule ("+ruleType+") found");
            } //end switch
         
            if (! token.equals(")")) { throw new ArgusException ("A \")\" is expected here"); }
            token = nextChar(tail);
            if (! (token.equals("|") || token.equals("")) ) { throw new ArgusException ("A \"|\" is expected here"); }
       
            ruleType = nextToken (tail).toUpperCase();
        } // end while
       return true;
    }
  
   /**
   * Returns the next token in a string, when various separators can occur
   * @param tail
   * @return
   * @throws ArgusException 
   */ 
    private static String nextToken( String[] tail )throws ArgusException{
        int[] p = new int[4]; int i, pMin; String hs;
        hs = "";
        if(! tail[0].equals("")){
            p[0] = tail[0].indexOf("(");
            p[1] = tail[0].indexOf(",");
            p[2] = tail[0].indexOf(")");
            p[3] = tail[0].indexOf("|");
            pMin = 100000;
            for (i=0;i<4;i++){ if (p[i] >=0 && pMin > p[i]){pMin = p[i];}}
            hs = tail[0].substring(0,pMin).trim();
            tail[0]=tail[0].substring(pMin).trim();
            if (pMin == 100000){throw new ArgusException("No separator found in string "+ tail[0]);}
        }
        if (hs.startsWith("\"") && hs.endsWith("\"")){hs = hs.substring(1, hs.length()-1);}
        return hs;
    }
    
    // Tests if tail contains a token, i.e. something starting with ( , ) or |
    private static boolean testNextToken( String[] tail )throws ArgusException{
        int[] p = new int[4]; int i, pMin;
        if(! tail[0].equals("")){
            p[0] = tail[0].indexOf("(");
            p[1] = tail[0].indexOf(",");
            p[2] = tail[0].indexOf(")");
            p[3] = tail[0].indexOf("|");
            pMin = 100000;
            for (i=0;i<4;i++){ if (p[i] >=0 && pMin > p[i]){pMin = p[i];}}
            if (pMin == 100000){return false;}
        }
        return true;
    }
    
    /**
     * returns the next character of a string and strips that character from the string
     * @param tail
     * @return 
     */
    private static String nextChar (String[] tail){
        return nextChar(tail, true);
    }
    
    private static String nextChar (String[] tail, boolean strip){
        String hs;
        hs = "";
        if(! tail[0].equals("")){
            hs =  tail[0].substring(0,1);
            if (strip) tail[0] = tail[0].substring(1);
        } 
        return hs;
    }

    /**
     * Processes the <SPECIFYTABLES> batch command.
     * reads the expl. variables, the response variable etc.
     * @return
     * @throws ArgusException 
    */
    static boolean specifyTableBatch() throws ArgusException{
        String hs; 
        int p;
        metadata = Application.getMetadata(Application.numberOfMetadatas()-1);
        TableSet tableSet = new TableSet(metadata);
        TableService.addTable(tableSet);

        // If available, set variable that contains recordkey
        tableSet.cellkeyVar = metadata.find(tauargus.model.Type.RECORD_KEY);

        // Exp vars first

        do{ 
            hs = tokenizer.nextToken();
            tableSet.expVar.add(metadata.find(hs));
            if (! (tokenizer.testNextChar().equals("|") || tokenizer.testNextChar().equals("\""))){
                throw new ArgusException ("A '|' or a '\"' is expected here");
            }
        }  
        while (!tokenizer.testNextChar().equals("|"));  
       //Respvar
        hs = tokenizer.nextChar(); //we know it is correct
        hs = tokenizer.nextField("|");
        if (hs.equalsIgnoreCase("<FREQ>")) {
            if (metadata.dataOrigin == DATA_ORIGIN_MICRO){
                tableSet.respVar = Application.getFreqVar();
            } else {
                tableSet.respVar = metadata.find(tauargus.model.Type.FREQUENCY);
            }
            tableSet.readFreqOnlyTable = true;
        }
        else {tableSet.respVar=metadata.find(hs);
            if (!tableSet.respVar.isNumeric()){throw new ArgusException ("Response variable (" + hs + ") is not numeric ");}
            if (tableSet.respVar == null) {throw new ArgusException ("Response variable (" + hs + ") not found ");}
        }
        if (tokenizer.getLine().equals("")) {return true;}
       
        //ShadowVar 
        hs = tokenizer.nextField("|");
        if ( !( hs.equals("") || hs.equals("|")) ){ //process Shadow
            tableSet.shadowVar=metadata.find(hs);
            if (tableSet.shadowVar == null) { throw new ArgusException ("Shadow variable (" + hs + ") not found "); }
        }
        if (tokenizer.getLine().equals("")) {return true;}

        //Costvar
        // - 1= freq;-2 = unity; -3 = distance
        hs = tokenizer.nextField(",");
        if ( !( hs.equals("") || hs.equals("|")) ){ //process costvar
            if (hs.equals("-1")){ // freq
                tableSet.costFunc = TableSet.COST_FREQ;   
            }
            else
            if (hs.equals("-2")){ //unity
                tableSet.costFunc = TableSet.COST_UNITY;                 
            }
            else
            if (hs.equals("-3")){//distance
                tableSet.costFunc = TableSet.COST_DIST;                 
            }
            else {
                tableSet.costVar = metadata.find(hs);
                tableSet.costFunc = TableSet.COST_VAR;
                if (tableSet.costVar == null) { throw new ArgusException ("Cost variable (" + hs + ") not found "); }
            }
        } 
        hs = tokenizer.getLine();
        if (hs.equals ("") ) {return true;}

        //lambda    
        hs = tokenizer.getLine(); 
        tokenizer.clearLine();
        if (!hs.equals("")){
            tableSet.lambda = StrUtils.toDouble(hs);      
        }
        return true;
    }       
}

