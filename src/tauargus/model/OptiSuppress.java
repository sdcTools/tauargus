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

/**
 *
 * @author ahnl
 * Class to run modular/Hitas , optimal (FullJJ),Network, CTA and the rounder
 * They have a lot in common.
 * 
 */

import argus.utils.SystemUtils;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import tauargus.extern.dataengine.TauArgus;
import tauargus.extern.tauhitas.HiTaSCtrl;
import tauargus.extern.tauhitas.ICallback;
import tauargus.extern.tauhitas.IProgressListener;
import tauargus.extern.taurounder.RCallback;
import tauargus.extern.taurounder.RProgressListener;
import tauargus.extern.taurounder.RounderCtrl;
import tauargus.gui.DialogStopTime;
import tauargus.gui.FrameMain;
import tauargus.service.TableService;
import tauargus.utils.ExecUtils;
import tauargus.utils.StrUtils;
import tauargus.utils.TauArgusUtils;
import static tauargus.utils.TauArgusUtils.ShowWarningMessage;
import tauargus.utils.Tokenizer;

public class OptiSuppress {
    public static double JJZero, JJZero1, JJZero2, JJFeasTol, JJOptTol, JJMinViola, JJMaxSlack, CTATolerance;
    public static int JJInfinity, JJMaxColslp, JJMaxRowslp, JJMaxCutsPool, JJMaxCutsIter;
             
    private static final String[] SOLVERNAME={"NO","XPRESS", "CPLEX", "SCIP" }; 
    private static final String DOTS = "....................";
    private static final TauArgus TAUARGUS = Application.getTauArgusDll();    
    private static final HiTaSCtrl TAUHITAS = Application.getTauHitasDll();
    private static final RounderCtrl TAUROUNDER = Application.getRounder();
    
    private static int UB, LB, TimeSoFar, nSuppressed;
    private static double DUB, DLB, Diff;
    private static BufferedWriter out;
    private static final Logger LOGGER = Logger.getLogger(OptiSuppress.class.getName());
    
    public static void loadJJParamFromRegistry(){
        //Initialize the JJ-parameters first, then get them from the registry.
        //If getting them from the registry fails, they still have the default value.  
        JJZero = 0.0000001;
        JJZero1 = 0.0000001;
        JJZero2 = 0.0000000001;
        JJInfinity = 2140000000;
        JJFeasTol = 0.000001;
        JJMinViola = 0.0001;
        JJOptTol = 0.000000001;
        JJMaxSlack = 0.01;
      
        JJMaxColslp = 50000;
        JJMaxRowslp = 15000;
        JJMaxCutsPool = 500000;
        JJMaxCutsIter = 50;
        
        CTATolerance  = 0.00001;
        
        try{
            JJZero = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "zero", "0.0000001"));
            JJZero1 = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "zero1", "0.0000001"));
            JJZero2 = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "zero2", "0.0000000001"));
            JJInfinity = StrUtils.toInteger(SystemUtils.getRegString("solveroptions", "infinity", "2140000000"));
            JJFeasTol = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "feastol", "0.000001"));
            JJOptTol = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "opttol", "0.000000001"));
            JJMinViola = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "minviola", "0.0001"));
            JJMaxSlack = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "maxslack", "0.01"));
     
            JJMaxColslp = StrUtils.toInteger(SystemUtils.getRegString("solveroptions", "maxcolslp", "50000"));
            JJMaxRowslp = StrUtils.toInteger(SystemUtils.getRegString("solveroptions", "maxrowslp", "15000"));
            JJMaxCutsPool = StrUtils.toInteger(SystemUtils.getRegString("solveroptions", "maxcutspool", "500000"));
            JJMaxCutsIter = StrUtils.toInteger(SystemUtils.getRegString("solveroptions", "maxcutsiter", "50"));
      
            CTATolerance = StrUtils.toDouble(SystemUtils.getRegString("solveroptions", "ctatolerance", "0.00001"));
            
          } catch (ArgusException ex){ }             
    }
    
    //From hitas header file:      
    //#define JJZERO          101
    //#define JJZERO1         102
    //#define JJZERO2         103
    //#define JJINF           104
    //#define JJMAXCOLSLP     105
    //#define JJMAXROWSLP     106
    //#define JJMAXCUTSPOOL   107
    //#define JJMAXCUTSITER   108
    //#define JJMINVIOLA      109
    //#define JJMAXSLACK      110
    //#define JJFEASTOL       111
    //#define JJOPTTOL        112   
    private static void setJJParamIntauHitas(){
      TAUHITAS.SetJJconstantsDbl(101, JJZero);
      TAUHITAS.SetJJconstantsDbl(102, JJZero1);
      TAUHITAS.SetJJconstantsDbl(103, JJZero2);
      TAUHITAS.SetJJconstantsDbl(104, JJInfinity);
      TAUHITAS.SetJJconstantsInt(105, JJMaxColslp);
      TAUHITAS.SetJJconstantsInt(106, JJMaxRowslp);
      TAUHITAS.SetJJconstantsInt(107, JJMaxCutsPool);
      TAUHITAS.SetJJconstantsInt(108, JJMaxCutsIter);
      TAUHITAS.SetJJconstantsDbl(109, JJMinViola);
      TAUHITAS.SetJJconstantsDbl(110, JJMaxSlack);
      TAUHITAS.SetJJconstantsDbl(111, JJFeasTol);
      TAUHITAS.SetJJconstantsDbl(112, JJOptTol);
    }
    
/**
 * The network solution is only possible for 2-dim tables and 
 * only the first RespVar can be hierarchical 
 * The version for 2 non-hierarchical variables has not been included.
 * @param tableSet
 * @throws ArgusException 
 */    
    public static void TestNetwork(TableSet tableSet)throws ArgusException{
        if (tableSet.expVar.size() !=2){
          throw new  ArgusException("The network is only possible for a 2 dim table");
        }
        if (tableSet.expVar.get(0).hierarchical == Variable.HIER_NONE  ){
          throw new  ArgusException("The network needs a hierarchy in the first dimension");
        }
        if (tableSet.expVar.get(1).hierarchical != Variable.HIER_NONE  ){
          throw new  ArgusException("The network does not allow a hierarchy in the second dimension");
        }
    }
 /**
  * Applies the network solution (by Jordi) 
  * Writes the table in a special format (AMPL);
  * runs the program (main1H2D)
  * loads the secondaries
  * @param tableSet
  * @throws ArgusException 
  */   
    public static void RunNetwork(TableSet tableSet)throws ArgusException{
        double ms = 0; int[] errorCode = new int[1]; String hs = ""; int result;
        ArrayList<String> commandline = new ArrayList<>();
        Date startDate = new Date();  
        String temp = Application.getTempDir();
        SystemUtils.writeLogbook("Start of Network protecton procedure");   
        if (!TAUARGUS.WriteHierarchicalTableInAMPLFormat(Application.getTempFile("NetInH.tmp"), temp, tableSet.index, ms, errorCode)){
            throw new ArgusException("Error preparing intermediate file\n" + 
                                     "for the hierarchical network solution (" + errorCode[0] + ")");
        }
       try {
         hs = SystemUtils.getApplicationDirectory(OptiSuppress.class).getCanonicalPath();
         } 
       catch (IOException | URISyntaxException ex) {JOptionPane.showMessageDialog(null, ex.toString());}
       hs = "\"" + hs + "/main1H2D.exe\"";
       commandline.add(StrUtils.unQuote(hs));
       commandline.add(Application.getTempFile("NetInH.tmp"));
       commandline.add(temp);
       
       if (tableSet.networkSolverType == 2) {
           commandline.add("-s");
           commandline.add("d");
       }
       if (tableSet.networkPrimariesOrder == 2) {
           commandline.add("-m");
           commandline.add("a");
       }
       if (tableSet.networkPrimariesOrder == 3) {
           commandline.add("-m");
           commandline.add("d");      
       }
       TauArgusUtils.writeBatchFileForExec("RunNetwork", commandline);
       result = ExecUtils.execCommand(commandline, Application.getTempDir(),false,"Run Network program");
       if (result != 0) { //something wrong
         if ((result >= -6) && (result < 0 )){
             throw new ArgusException(TAUARGUS.GetErrorString(4500 - result));
         }
         throw new ArgusException("Error occurred running the 1H2D Network solution (code = " + result + ")");    
       }    
       if (!TAUARGUS.SetSecondaryFromHierarchicalAMPL( Application.getTempFile("sec_1H2D.out"), tableSet.index, errorCode)){
        throw new ArgusException("Error occurred while reading the results of network; code (" + errorCode[0] + ")");     
       }
       tableSet.suppressed =  TableSet.SUP_NETWORK;  
       if (tableSet.networkSolverType == 1){tableSet.suppressINFO = "PPRN solver used<br>";}
       if (tableSet.networkSolverType == 2){tableSet.suppressINFO = "Dijkstra solver used<br>";}
       if (tableSet.networkPrimariesOrder == 1){tableSet.suppressINFO += "Normal order";}
       if (tableSet.networkPrimariesOrder == 2){tableSet.suppressINFO += "Ascending order";}
       if (tableSet.networkPrimariesOrder == 3){tableSet.suppressINFO += "Descending order";}
       tableSet.nSecond = tableSet.CountSecondaries(); 
       Date endDate = new Date();
       long diff = endDate.getTime()-startDate.getTime();
       tableSet.processingTime = (int) diff / 1000;
       SystemUtils.writeLogbook("End of Network protection procedure; number of secondaries: "+tableSet.nSecond);   
    }
    
  /**
   * runs the audit procedure; an external Delphi/Lazarus program called intervalle
   * Originates from Ilmenau, extended by Anco and Destatis
   * First a parameter file is written 
   * @param tableSet
   * @throws ArgusException 
   */
    public static void RunAudit (TableSet tableSet) throws ArgusException{
        //Run the intervalle program
        ArrayList<String> commandline = new ArrayList<>();
        int tabNo = tableSet.index; String hs; int result, i;
        Date startDate = new Date();  
        int cn; double lpl,upl,cVal; boolean oke;
        tableSet.undoAudit();
        SystemUtils.writeLogbook("Start of Audit procedure");       
        SaveTable.writeJJ(tableSet, Application.getTempFile("Anneke.JJ"), false, false, 0, false, false);
        //Write Rounding parameter file
        try (BufferedWriter out2 = new BufferedWriter(new FileWriter(Application.getTempFile("Anneke.txt")))) {
            out2.write(Application.getTempFile("Anneke.JJ")); out2.newLine();
            out2.write ("NoLongerNeeded"); out2.newLine();
            out2.write (Application.getTempFile("Anneke.Out")); out2.newLine();
            out2.write (Application.getTempFile("Anneke.txt")); out2.newLine();
            out2.write ("primsec"); out2.newLine();
            out2.write ("without"); out2.newLine();
            out2.write ("2"); out2.newLine();
            out2.write ("1"); out2.newLine();
            out2.write ("0"); out2.newLine();
            out2.write ("0"); out2.newLine();
            out2.write ("50"); out2.newLine();
            out2.write ("50"); out2.newLine();
            out2.write ("0"); out2.newLine();
            out2.write ("5"); out2.newLine();
            if  (Application.solverSelected == Application.SOLVER_NO) {out2.write ("GLPK");}
            if  (Application.solverSelected == Application.SOLVER_CPLEX) {out2.write ("cplex");}
            if  (Application.solverSelected == Application.SOLVER_XPRESS) {out2.write ("xpress");}
            if  (Application.solverSelected == Application.SOLVER_SOPLEX) {out2.write ("GLPK");}
            out2.newLine();
                
            if  (Application.solverSelected == Application.SOLVER_CPLEX) {
                out2.write (TauArgusUtils.GetCplexLicenceFile());}
            else{out2.write ("NoLicenceSpecified");}
            out2.newLine();
        }
        catch (IOException ex){
          throw new ArgusException("A problem was encountered when writing the parameter file for audit");} 
        
        TauArgusUtils.DeleteFile(Application.getTempFile("Anneke.Out"));

        hs = "";
        try{
          hs = SystemUtils.getApplicationDirectory(OptiSuppress.class).getCanonicalPath();
          hs = hs + "/intervalle.exe";
          if (!TauArgusUtils.ExistFile(hs)){ throw new ArgusException("The audit program \""+ hs + "\" could not be found");}
          commandline.add(hs);
          commandline.add(Application.getTempFile("Anneke.txt"));
          commandline.add(Application.getTempFile("Audit.log"));
          TauArgusUtils.writeBatchFileForExec("RunAudit", commandline);
          } 
        catch (IOException | URISyntaxException | ArgusException ex){
            JOptionPane.showMessageDialog(null, ex.toString());
        }
        //Run the intervalle program
        result = ExecUtils.execCommand(commandline, Application.getTempDir(),false, "Run Audit program");
        
        if (result != 0){ throw new ArgusException("The audit program was not completed successfully");}
        if (!TauArgusUtils.ExistFile(Application.getTempFile("Anneke.Out"))){
            throw new ArgusException("The audit program was not completed successfully\nNo output file found");
        } 
        try{
            BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("Anneke.Out")));
            Tokenizer tokenizer = new Tokenizer(in);
            BufferedWriter out2 = new BufferedWriter(new FileWriter(Application.getTempFile("audit_"+tabNo+ ".html")));
            //KopHTML
            int lineNumber = 0;
            for (i=0;i<4;i++){hs = tokenizer.nextLine();}  //first 4 lines are skipped
            SaveTable.writeKopHtml(tableSet, out2, false);
            out2.write("<!--XXXXXXXXXXXXXX-->"); out2.newLine();
            out2.write("<h2>Overview of not-properly protected cells</h2>"); out2.newLine();
            while ((tokenizer.nextLine()) != null) {
                hs = tokenizer.nextField(";");
                cn = Integer.parseInt(hs);
                lpl = Double.parseDouble(tokenizer.nextField(";"));
                upl = Double.parseDouble(tokenizer.nextField(";"));
                cVal = Double.parseDouble(tokenizer.nextField(";"));
                oke = TAUARGUS.SetRealizedLowerAndUpper(tabNo, cn, upl, lpl);
                if(!oke){throw new ArgusException("An error has occurred when reading the audit results for cell: "+cn);}
                if (tokenizer.getLine().equals("u;1")){
                    lineNumber++;
                    if(lineNumber==1){
                        out2.write("<table>"); out2.newLine();
                        out2.write("  <tr>"); out2.newLine();
                        for (i=0;i<tableSet.expVar.size();i++) {
                            out2.write("    <td>"+tableSet.expVar.get(i).name  +"</td>"); out2.newLine();
                        }
                        out2.write("    <td>Lower Prot.</td>");out2.newLine();
                        out2.write("    <td>Cell value</td>");out2.newLine();
                        out2.write("    <td>Upper Prot.</td> ");out2.newLine();
                        out2.write("    <td>Exact</td>"); out2.newLine();
                        out2.write("  </tr>"); out2.newLine();
                    }
                    hs = TableService.IndexToCodesString(tableSet, cn);
                    hs = hs.replace(",", "</td><td align=\"Right\">");
                    hs = "  <tr><td align=\"Right\">" + hs;
                    hs = hs + "<td align=\"Right\">"+lpl + 
                             "</td><td align=\"Right\">" + cVal + 
                             "</td><td align=\"Right\">" + upl + "</td><td>";
                    if ((cVal == lpl) && (cVal == upl)){ hs = hs + "YES"; tableSet.auditExactDisclosure++;}   
                    else {hs = hs +"&nbsp;";tableSet.auditPartialDisclosure++;}
                    
                    hs = hs + "</td></tr>";
                    out2.write(hs); out2.newLine();
                }
            }
            if(lineNumber>0){out2.write("</table>"); out2.newLine();}
            out2.write("<!--XXXXXXXXXXXXXX-->"); out2.newLine();
            SaveTable.writeStaartHTML(out2);
            out2.close();
            tableSet.hasBeenAudited = true;   
            SystemUtils.writeLogbook("End of Audit procedure\n" + 
                                     tableSet.auditExactDisclosure + " cells could be exactly disclosed\n"+
                                     tableSet.auditPartialDisclosure + " cells could be partially disclosed");              
            Date endDate = new Date();
            long diff = endDate.getTime()-startDate.getTime();
            diff = diff / 1000;
            //tableSet.processingTime = (int) diff;
            tokenizer.close(); 
        }
        catch(IOException ex){throw new ArgusException ("An error has occurred when retrieving the audit results");}
    }
    
    public static void RunCTA (TableSet tableSet, boolean doExpert)throws ArgusException, FileNotFoundException, IOException{
        String command = "", tolStr, solFile = "", solver = "", hs = ""; double eps;
        ArrayList<String> commandline = new ArrayList<>();
        int result, tabNo, i;
        int cn; double orgVal, ctaVal;
        int[] nSec; nSec = new int[1];
        String[] solverNames = {"cplex", "xpress", "glpk", "cbc", "sym"};
        tabNo = tableSet.index;
        Date startDate = new Date();  
        SystemUtils.writeLogbook("Start of CTA run");
        SaveTable.writeJJ(tableSet, Application.getTempFile("CTA.IN"), false, false, 0, false, false);
        loadJJParamFromRegistry();
        
        try {
            command = SystemUtils.getApplicationDirectory(OptiSuppress.class).getCanonicalPath();
        } catch (IOException | URISyntaxException ex) {JOptionPane.showMessageDialog(null, ex.toString());}

        if (doExpert){ // clean possible output files
            for(i=0;i<solverNames.length;i++){TauArgusUtils.DeleteFile(Application.getTempFile("CTA_"+solverNames[i]+".sol"));}
        }
        if (SystemUtils.isWindows()){
            if (doExpert){command = "\"" + command + "/GUICTA.exe\"";}
            else         {command = "\"" + command + "/mainCTA.exe\"";}
        }
        else{ // For the moment this is assumed to be Linux
            if (doExpert){command = "\"" + command + "/GUICTA\"";}
            else         {command = "\"" + command + "/main_CTA\"";}
        }   
        
        if (!TauArgusUtils.ExistFile(StrUtils.unQuote(command))){ throw new ArgusException("CTA-program could not be found");}
        commandline.add(StrUtils.unQuote(command));
        command = command + " "+ StrUtils.quote(Application.getTempFile("CTA.IN"));
        commandline.add(Application.getTempFile("CTA.IN"));
        command = command + " \""+Application.getTempDir()+ "\"";
        commandline.add(Application.getTempDir());
        eps = CTATolerance;
        if (!doExpert){
            tolStr = Double.toString(CTATolerance); //   "1.0E-5";
            command = command + " -e " + tolStr;
            commandline.add("-e");
            commandline.add(tolStr);
          
            commandline.add("-s");
            switch (Application.solverSelected){
                case Application.SOLVER_CPLEX:  
                     commandline.add("c");
                     command = command + " -s c"; 
                     solver = "cplex";  break;
                case Application.SOLVER_XPRESS: 
                     commandline.add("x");
                     commandline.add("-M");
                     commandline.add("d");
                     command = command + " -s x -M d"; 
                     solver = "xpress"; break; // OEM Xpress has no barrier
                case Application.SOLVER_SOPLEX: 
                     commandline.add("b");
                     command = command + " -s b"; 
                     solver = "cbc";   break;//CBC gekozen ipv Soplex
                case Application.SOLVER_NO: throw new ArgusException("No solver has been selected so CTA cannot be applied");            
            }
        }
        try (BufferedWriter out2 = new BufferedWriter(new FileWriter(Application.getTempFile("CTA.bat")))) {
            out2.write(command); out2.newLine();
            out2.write ("pause"); out2.newLine();
        }
        catch (Exception ex){JOptionPane.showMessageDialog(null, ex.toString());}
        
        solFile = Application.getTempDir() + "/CTA_" + solver + ".sol";        
        TauArgusUtils.DeleteFile(solFile);
        
        // In expert mode, no additional progress window needed, i.e. silent = true = doExpert
        result = ExecUtils.execCommand(commandline, Application.getTempDir(),doExpert,"Run CTA program");
//Return values of CTA
//#define CTA_OK 0 // table successfully created, but CTA not yet solved
//#define CTA_OPTIMAL_SOLUTION 1 // optimal solution (within tolerance) found
//#define CTA_TIME_LIMIT_INFEAS 2 // time limit exhausted with no feasible solution
//#define CTA_TIME_LIMIT_FEAS 3 // time limit exhausted with feasible solution
//#define CTA_INFEASIBLE 4 // optimization terminated (not by time limit) with no feasible solution
//#define CTA_FEASIBLE 5 // feasible solution found, likely  not optimal
//#define CTA_FIRST_FEASIBLE 6 //first feasible solution found, likely not optimal
//#define CTA_REPAIRINF_FEAS 7 // repair infeasibility procedure successfully finished
//#define CTA_REPAIRINF_INFEAS 8 // repair infeasibility procedure still reported problem is infeasible
//#define CTA_REPAIRINF_ABNORMAL 9 // repair infeasibility procedure abnormally finished (error, numerical instatibilites...)
//#define CTA_OTHERWISE 10 // other situations from solver with no feasible solution
//1 3 5 6 are reasonable OK return values
        if (!doExpert){
            if ( !((result == 1)||(result == 3)||(result == 5)||(result == 6)) ){
                throw new ArgusException("Incorrect end of CTA-program; Error code: "+ result);           
            }
            if (result == 1) {hs = "CTA_optimal solution (within tolerance) found";}
            if (result == 3) {hs = "CTA_time limit exhausted with feasible solution";}
            if (result == 5) {hs = "CTA_feasible solution found, likely  not optimal";} 
            if (result == 6) {hs = "CTA_first feasible solution found, likely not optimal";} 
            SystemUtils.writeLogbook(hs);
            tableSet.suppressINFO = "Standard CTA solution has been applied<br>" +
                                    "Solver used : " + solver + "<br>" + hs;
        }
        if (doExpert){ // try to find a solutions file;  we take the first non-empty one
            solFile = ""; long lastTime, lt;
            lastTime = 0;
            for(i=0;i<solverNames.length;i++){
                hs = Application.getTempFile("CTA_"+solverNames[i]+".sol");  
                if (TauArgusUtils.ExistFile(hs)){
                    hs = Application.getTempFile("CTA_"+solverNames[i]+".sol"); 
                    if (TauArgusUtils.FileLength(hs)>0) {
                        lt = TauArgusUtils.FileLastModified(hs);
                        if (lt > lastTime){
                            lastTime = lt;  
                            solFile = hs;   
                            solver = solverNames[i];
                        }
                    }
                }  
            }
            if (TauArgusUtils.FileLength(solFile)==0){ throw new ArgusException("Solution file of CTA-program is empty");}
            tableSet.suppressINFO = "Expert CTA solution has been applied<br>Solver used : " + solver+ "<br>";
        }
        
        //Read rthe results back
        nSec[0]=0;

        try(BufferedReader reader = new BufferedReader(new FileReader(solFile))) { 
            Tokenizer tokenizer = new Tokenizer(reader);
            while ((tokenizer.nextLine()) != null) {
                cn = Integer.parseInt(tokenizer.nextToken());
                orgVal = Double.parseDouble(tokenizer.nextToken());
                ctaVal = Double.parseDouble(tokenizer.nextToken());
                if (Math.abs(orgVal-ctaVal) < eps){ctaVal = orgVal;}
                TAUARGUS.SetCTAValues(tabNo, cn, orgVal, ctaVal, nSec);
            }
            tokenizer.close();
        }
        catch(FileNotFoundException | NumberFormatException ex){JOptionPane.showMessageDialog(null, ex.toString());}
         
        Date endDate = new Date();
        long diff = endDate.getTime()-startDate.getTime();
        diff = diff / 1000;
        tableSet.processingTime = (int) diff;
        
        tableSet.ctaProtect = true; 
        tableSet.suppressed = TableSet.SUP_CTA;
        i = tableSet.CountSecondaries();
        tableSet.nSecond = tableSet.nSecond + i;  //does this make sense?
        batch.reportProgress("CTA run successfully completed\n" +
                             "Number of secondaries: " + tableSet.suppressed + 
                             "Processing time: " + StrUtils.timeToString(tableSet.processingTime));
    } 
   
    private static void verdubbelHitasTxt(){
        /*
            Add an articificial secodn dimension in Hitastab
            Add HitasV2.txt
            Modify NFS.txt
        */  
        File fileKlad = new File(Application.getTempFile("Hitastab.kld"));
        File fileHitas = new File(Application.getTempFile("Hitastab.txt"));
        if (fileKlad.exists()) { fileKlad.delete();}
        fileHitas.renameTo(new File(Application.getTempFile("Hitastab.kld")));  
        String hs, kop, zeroline = "0.0 z 0 0";
        int aant;
        try{
            BufferedWriter out2 = new BufferedWriter(new FileWriter(Application.getTempFile("Hitastab.txt")));
            BufferedReader in =  new BufferedReader(new FileReader(Application.getTempFile("Hitastab.kld")));
            //while (in.ready()){
            try {
                hs = in.readLine();
            } catch (IOException ex){
                hs = null;
            }
            aant = hs.substring(12).indexOf(' ') + 6;
            while (hs != null){
                kop = hs.substring(0, 11);
                out2.write(kop.substring(0, 6) + "    0 " + hs.substring(12)); out2.newLine();
                out2.write(kop.substring(0, 6) + "    1 " + hs.substring(12)); out2.newLine();
                out2.write(kop.substring(0, 6) + "    2 " + String.format("%" + aant + "s",zeroline)); out2.newLine();
                try {
                    hs = in.readLine();
                } catch (IOException ex){
                    hs = null;
                }
            }
            in.close();
            out2.close();
            out2 = new BufferedWriter(new FileWriter(Application.getTempFile("hitasv2.txt")));
            out2.write("var_2"); out2.newLine();
            out2.write(".0");out2.newLine();
            out2.write(".1");out2.newLine();
            out2.close();
            out2 = new BufferedWriter(new FileWriter(Application.getTempFile("NFS.txt")));
            out2.write("2");out2.newLine();
            out2.write (Application.getTempFile("hitasv1.txt")); out2.newLine();
            out2.write (Application.getTempFile("hitasv2.txt")); out2.newLine();
            out2.write (Application.getTempFile("Hitastab.txt")); out2.newLine();
            out2.write (Application.getTempFile("hitassec.txt")); out2.newLine();
            out2.close();
        }
        catch (IOException ex){}
    }
    
    /**
     * Modular aka HiTas is the popular solution for cell cell suppression.\n
     * Based on the optimal solution provided by JJ Salazar the table is broken down in smaller pieces and protected.\n
     * This is necessary because the optimal solution is often far too hard to compute on a PC.\n
     * First the parameter file NPF and the file with the names of the files (NFS) are written to Temp using PrepareHITAS.\n
     * PrepareHitas also writes the data in a format suitable for Modular.\n 
     * Some extra info is written to NPF\n
     * Then ControleerHITAStabtxt does some final checks.\n
     * In case of a single column table Modular has a problem with its own data structures. It cannot handle a single column table.\n
     * So the table is extended with a column of empty cells using verdubbelHitasTxt.\n
     * The parameters for modular are retrieved from the registry and given to Modular 
     * using loadJJParamFromRegistry and setJJParamIntauHitas.\n
     * After removing some files in TEMP AHitas will protect the table.\n
     * ReadSecondaries will finally give the results back to the engine.
     * 
     * @param VarNo
     * @param expVarNo
     * @param propertyChangeListener
     * @param tableSet
     * @return
     * @throws ArgusException
     * @throws FileNotFoundException
     * @throws IOException 
     */
    
    public static boolean rewriteHitasv(int expVarNo, int VarNo ) throws ArgusException {
        int i, n, l; 
        String hs;
        char dot = DOTS.charAt(0);
        hs = "hitasv" + Integer.toString(VarNo+1);
        File fileKlad = new File(Application.getTempFile(hs+".kld"));
        File fileHitasVtxt = new File(Application.getTempFile(hs+".txt"));
        if (fileKlad.exists()) { fileKlad.delete();}
        fileHitasVtxt.renameTo(new File(Application.getTempFile(hs+".kld")));  

        try{
            BufferedWriter out2 = new BufferedWriter(new FileWriter(Application.getTempFile(hs+".txt")));
            BufferedReader in =  new BufferedReader(new FileReader(Application.getTempFile(hs+".kld")));
            while (in.ready()){
                hs = in.readLine();
                l = hs.length();
                i=0;
                while ( (i<l) && (hs.charAt(i) == dot)) {i++;}
                hs = hs.substring(i);
                hs = hs.replace (".", "_");
                hs = DOTS.substring(0, i) + hs;
                out2.write(hs);
                out2.newLine();
            }
            out2.close();
            in.close();
            if (fileKlad.exists() && !Application.isAnco()) { fileKlad.delete();}
        }
        catch (IOException ex){
            throw new ArgusException("A problem was encountered when rewriting the file hitasv");
        }  

        return true;
    }
    
    public static boolean RunModular(TableSet tableSet, final PropertyChangeListener propertyChangeListener) throws ArgusException, FileNotFoundException, IOException{
        boolean Oke; 
        int i, returnValue; 
        String hs, fs1, fs2; 
        Variable variable; 
        BufferedWriter tmpOut ;
        final PropertyChangeSupport pcs = new PropertyChangeSupport(TableService.class);
        pcs.addPropertyChangeListener(propertyChangeListener);
        IProgressListener progressListener = new IProgressListener(){
            @Override
            public void UpdateGroups(final int percentage) {
                pcs.firePropertyChange("progressMain", null, percentage);
            }
            @Override
            public void UpdateTables(final int percentage) {
                pcs.firePropertyChange("progressDetail", null, percentage);
            }
        };
        TAUHITAS.SetProgressListener(progressListener);
        pcs.firePropertyChange("activityMain", null, "Groups");
        pcs.firePropertyChange("progressMain", null, 0);
        pcs.firePropertyChange("activityDetail", null, "Tables");
        pcs.firePropertyChange("progressDetail", null, 0);

        Date startDate = new Date();  
        SystemUtils.writeLogbook("Start of the modular protection for table " + TableService.getTableDescription(tableSet));

        // checking for too many dimensions and additivity
        if (!Application.isProtectCoverTable()){
            if (tableSet.expVar.size()== 4) {
                hs = "The table has 4 dimensions.\n" + 
                     "Running Modular can take a lot of time and maybe it is difficult to obtain a correct result.\n" +
                     "Please check the results carefully.\n";
                int warningResult = ShowWarningMessage(hs);
                if (warningResult == 0 ) {
                    throw new ArgusException("Modular has not been completed"); 
                }
            }
            
            if (tableSet.expVar.size() > 4 ) {
                if (Application.isAnco()) {
                    hs = "The table has more than 4 dimensions.\n" + 
                         "Running Modular can take a lot of time and is error-prone.\n" +
                         "Please check the results carefully.\n";
                    int warningResult = ShowWarningMessage(hs);
                    if (warningResult == 0 ) {
                        throw new ArgusException("Modular has not been completed"); //overlapString);
                    }
                }
                else{
                    hs = "The table has more than 4 dimensions.\n" + 
                         "Running Modular is not possible.\n";   
                    throw new ArgusException(hs); //overlapString);
                }
            }
            
            if (tableSet.additivity == TableSet.ADDITIVITY_NOT_REQUIRED){
                hs = "The table might be not additive.\n" + 
                     "Running Modular successfully is not guaranteed.\n";
                int warningResult = ShowWarningMessage(hs);
                if (warningResult == 0 ) {
                    throw new ArgusException(hs); //overlapString);
                }
            }
        }

    
        Oke = TAUARGUS.PrepareHITAS(tableSet.index, Application.getTempFile("NPF.txt"), Application.getTempFile("NFS.txt"), Application.getTempDir()+"/");
        if (!Oke){throw new ArgusException("An unknown error occurred when preparing the files for Modular");} 
        // TestHitasFiles; check for a bogus at the top level
        // rewrite hitasv?.txt here again without DOTS in the codes
        // Note that this procedure will faill if the code starts with a dot!!!!
        // Ideally this is coorrected in the TauArgus dll itself
        for(i=0;i<tableSet.expVar.size();i++){
            rewriteHitasv(tableSet.expVar.get(i).index, i); 
        }
        int n= tableSet.respVar.nDecimals;
        try{
            tmpOut = new BufferedWriter(new FileWriter(Application.getTempFile("NPF.txt"), true));
            tmpOut.write("[Frequency rule]"); tmpOut.newLine();
            tmpOut.write("MINCOUNT="+tableSet.minFreq[0]); tmpOut.newLine();tmpOut.newLine();
            
            tmpOut.write("[Safety ranges]"); tmpOut.newLine();
            tmpOut.write("LOWERMARG="+ SystemUtils.getRegString("optimal", "LOWERMARG", "0.99")); tmpOut.newLine();
            tmpOut.write("UPPERMARG="+ SystemUtils.getRegString("optimal", "UPPERMARG", "1.01")); tmpOut.newLine(); tmpOut.newLine();
            
            tmpOut.write("[Misc]"); tmpOut.newLine();
            tmpOut.write("DECIMALS=" + n); tmpOut.newLine();
            // TODO vogelen voor PQ regel
            tmpOut.write ("MAXWEIGHT="+ tableSet.maxScaleCost); tmpOut.newLine();
            tmpOut.write ("MINTABVAL="+ tableSet.minTabVal); tmpOut.newLine();
            tmpOut.write ("MAXTABVAL="+ tableSet.maxTabVal); tmpOut.newLine(); tmpOut.newLine();
            
            tmpOut.write("[Costs]"); tmpOut.newLine();
            if (tableSet.costFunc == TableSet.COST_DIST){
                tmpOut.write("[Distance cost function]"); tmpOut.newLine();
                tmpOut.write("DISTANCE=1"); tmpOut.newLine();
                for(i=0;i<tableSet.expVar.size();i++){
                    variable = tableSet.expVar.get(i);
                    hs = "D" + Integer.toString(i+1).trim() + " =";
                    tmpOut.write(hs);                
                    if (variable.hasDistanceFunction ){
                        for (int j=0;j<Variable.MAX_NUMBER_OF_DIST;j++){
                            tmpOut.write(" "+variable.distanceFunction[j]);
                        }  
                    }
                    else {
                        for (int j=0;j<Variable.MAX_NUMBER_OF_DIST;j++){
                            tmpOut.write(" 1"); // Equal cost 1 for up to distance MAX_NUMBER_OF_DIST
                        }
                    }
                    tmpOut.newLine();
                }
            }  
            else{
                tmpOut.write("DISTANCE=0"); tmpOut.newLine();
            }
            tmpOut.close();
        }
        catch (IOException ex){
            throw new ArgusException("A problem was encountered when writing the file NPF");
        }  
        if (tableSet.expVar.size() == 1){}
//TODO Dummy variable toevoegen voor HITAS  (SjoemelBestandenVoorHitas1) 
// Easter egg for Sarah  
            if ( !Application.isBatch() && Application.isAnco() ){ 
             fs1 = "";   
             try{fs1 = SystemUtils.getApplicationDirectory(FrameMain.class).getCanonicalPath();}
             catch (IOException | URISyntaxException ex){JOptionPane.showMessageDialog(null, ex.toString());}
             fs1 = fs1 +"\\SarahStop.txt";  
             fs2 = SystemUtils.getRegString("general", "datadir", "");
             fs2 = TauArgusUtils.getFilePath(fs2);
             fs2 = fs2 + "\\SarahStop.txt"; 
             if ( TauArgusUtils.ExistFile(fs1) ||
                  TauArgusUtils.ExistFile(fs2) ) {
                TauArgusUtils.ShowInfoMessage("Sarah, you can modify the input files");
             }
            }  
               
            ControleerHITAStabtxt(tableSet); 
            if (tableSet.expVar.size() == 1) {verdubbelHitasTxt();}
       
            // Clean up possible left over temp-files previous run of modular/HiTaS
            TauArgusUtils.DeleteFile(Application.getTempFile("BT.dat"));
            TauArgusUtils.DeleteFile(Application.getTempFile("JJUit.dat"));
            TauArgusUtils.DeleteFile(Application.getTempFile("InFeas.dat"));
            TauArgusUtils.DeleteFile(Application.getTempFile("hitassec.txt"));
            TauArgusUtils.DeleteFile(Application.getTempFile("HiTaS.log")); 
            TauArgusUtils.DeleteFile(Application.getTempFile("cspSCIP.bra"));
            TauArgusUtils.DeleteFile(Application.getTempFile("cspSCIP.heu"));
            TauArgusUtils.DeleteFile(Application.getTempFile("cspSCIP.sta"));
            TauArgusUtils.DeleteFile(Application.getTempFile("CSPlogfile.txt"));
            TauArgusUtils.DeleteFile(Application.getTempFile("sdcnet.lp"));
            TauArgusUtils.DeleteFile(Application.getTempFile("sdc.lp"));
            TauArgusUtils.DeleteFile(Application.getTempFile("problem.lp"));
            TauArgusUtils.DeleteFile(Application.getTempFile("hierinfo.dat"));

        //Run Hitas
        hs = "";
        if (Application.solverSelected == Application.SOLVER_CPLEX) hs = TauArgusUtils.GetCplexLicenceFile();
        
        tableSet.maxHitasTime = Application.generalMaxHitasTime;
 
        TAUHITAS.SetDebugMode(Application.SaveDebugHiTaS);
   
        loadJJParamFromRegistry();
        setJJParamIntauHitas();
      
        returnValue = TAUHITAS.AHiTaS(Application.getTempFile("NPF.txt"), Application.getTempFile("NFS.txt"), tableSet.maxHitasTime, 
                                        hs, Application.getTempDir()+"/", SOLVERNAME[Application.solverSelected], 
                                        tableSet.singletonSingletonCheck, tableSet.singletonMultipleCheck, tableSet.minFreqCheck);
        if (returnValue > 0) {        
            hs = TAUHITAS.GetErrorString(returnValue);
            //XPRESS or CPLEX or SCIP
            //if (returnValue != 0) {throw new ArgusException ("Error in modular suppression procedure\n" + hs);}
            throw new ArgusException ("Error in modular suppression procedure\n" + hs);
        }
        ReadSecondaries(tableSet);
        tableSet.suppressINFO = ReadHitasINFO("hitas.log");
        if (tableSet.expVar.size() == 1) {tableSet.nSecond = tableSet.nSecond /2;}
        Date endDate = new Date();
        long diff = endDate.getTime()-startDate.getTime();
        diff = diff / 1000;
        tableSet.processingTime = (int) diff;
        SystemUtils.writeLogbook("End of modular protection. Time used "+ diff+ " seconds\n" + 
                                 "Number of suppressions: " +tableSet.nSecond); 
        TAUHITAS.SetProgressListener(null);
        pcs.removePropertyChangeListener(propertyChangeListener);
        return true;
    }

    public static String ReadHitasINFO (String fn){
        String hs, regel;
        try{ 
            BufferedReader  in  = new BufferedReader(new FileReader(Application.getTempFile(fn))); 
            regel = in.readLine(); hs = "";
            regel = in.readLine();
            while (!regel.contains("Start at")){
                hs = hs + regel + "<br>";  
                regel = in.readLine();
            }
            while  (hs.endsWith("<br>")) {hs = hs.substring(0, hs.length()-4);}
            return hs;
        }
        catch (IOException ex){return "";}
    }

    private static void ReadSecondaries(TableSet tableSet) throws ArgusException{
        int[] nSecondaries = new int[1]; 
        File fileSecondaries = new File(Application.getTempFile("hitassec.txt"));
        if (! fileSecondaries.exists() ) {throw new ArgusException("Error in modular;\nNo outputfile found");}
        TAUARGUS.SetSecondaryHITAS(tableSet.index , nSecondaries);
        tableSet.nSecond = nSecondaries[0];
        tableSet.suppressed = TableSet.SUP_HITAS;
        tableSet.solverUsed = Application.solverSelected;
    }
    
    private static void ControleerHITAStabtxt(TableSet tableSet){
        String regelOut, token, status, freq; 
        int  i;
        Double respVal, lpl, epsilon, x;
        // GTIndex is never used???
        //int[] GTIndex = new int[tableSet.expVar.size()];
        //for (i=0;i<tableSet.expVar.size();i++){
        //    GTIndex[i] = 0;
        //}
    
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
    
        DecimalFormat normalPrecision = new DecimalFormat();
        DecimalFormat extraPrecision = new DecimalFormat();
        // first rename Hitastab.txt
        File fileKlad = new File(Application.getTempFile("Hitastab.kld"));
        File fileHitas = new File(Application.getTempFile("Hitastab.txt"));
        if (fileKlad.exists()) { fileKlad.delete();}
        fileHitas.renameTo(new File(Application.getTempFile("Hitastab.kld")));
    
        i = tableSet.respVar.nDecimals;
        normalPrecision.setMinimumFractionDigits(i);
        normalPrecision.setMaximumFractionDigits(i);
        extraPrecision.setMinimumFractionDigits(i+3);
        extraPrecision.setMaximumFractionDigits(i+3);
        extraPrecision.setDecimalFormatSymbols(symbols);
        normalPrecision.setDecimalFormatSymbols(symbols);
        extraPrecision.setGroupingUsed(false);
        normalPrecision.setGroupingUsed(false);
    
        epsilon = 1.0 / ( 10^(i+1));

        try(BufferedWriter out2 = new BufferedWriter(new FileWriter(Application.getTempFile("Hitastab.txt")))) {
            Tokenizer tokenizer;
            tokenizer = null;
            try{
                tokenizer = new Tokenizer(new BufferedReader(new FileReader(Application.getTempFile("Hitastab.kld"))));
            } catch (FileNotFoundException ex) {JOptionPane.showMessageDialog(null, ex.toString());}
            double x1, x2;
            x1 = Math.abs(tableSet.maxTabVal);
            x2 = Math.abs(tableSet.minTabVal);
            x1 = Math.max(x1,x2);
            int aant = (int) Math.ceil((Math.log(x1)/Math.log(10)));
            if (x2<0)
                aant = aant + 1;
            if (tableSet.respVar.nDecimals > 0)
                aant = aant + tableSet.respVar.nDecimals + 1;
            while (tokenizer.nextLine() != null) {
                regelOut = "";
                for (i=0;i<tableSet.expVar.size();i++){
                    token = tokenizer.nextToken();
                    regelOut = regelOut +String.format("%6s", token);
                }
                if (tableSet.expVar.size()==1){regelOut=regelOut+regelOut;}
                regelOut=regelOut.substring(1);
                token = tokenizer.nextToken();  //Value
                respVal = Double.valueOf(token);
                regelOut = regelOut + " " + String.format("%" + aant + "s", token);
                status = tokenizer.nextToken();  //Status
                freq = tokenizer.nextToken();
                if ( (respVal == 0) && status.equals("z") ){
                    if ( Application.isProtectCoverTable()){
                        if (tokenizer.getLine().equals("0")) {regelOut = regelOut + " n ";}
                        else {regelOut = regelOut + " z ";}
                    }
                    else {regelOut = regelOut + " z ";}
                    regelOut = regelOut + freq + " "+ tokenizer.getLine();
                    tokenizer.clearLine();
                }
                else{
                    regelOut = regelOut + " "+ status;
                    if (status.equals("u")){ //check LPL UPL
                        token = freq;
                        lpl = Double.valueOf(token);
                        x = respVal - tableSet.minTabVal;
                        if (x < 0 ) {x = 0.0;} // should not occur
                        if (lpl + epsilon > x) {lpl = x - epsilon;}
                        if (lpl < 0 ) {lpl = 0.0;}
                        regelOut = regelOut + " "+ extraPrecision.format(lpl);
                        regelOut = regelOut + " " + tokenizer.nextToken(); //UPL
                        freq = tokenizer.nextToken();
                    }
                    regelOut = regelOut + " " + freq; //Freq
                    token = tokenizer.nextToken();
                    if ("-".equals(token.substring(0,1))) {token = token.substring(1);}
                    regelOut = regelOut + " " + token; //Cost
                }
                out2.write (regelOut);
                out2.newLine();
            }
            tokenizer.close();        
        }
        catch (HeadlessException | IOException | NumberFormatException ex){JOptionPane.showMessageDialog(null, ex.toString());}
    }
    
    public static void RunUWE(TableSet tableSet)throws ArgusException, IOException{
        String hs;
        int[] nSec = new int[1];
        ArrayList<String> commandline = new ArrayList<>();
        Date startDate = new Date();  
        SystemUtils.writeLogbook("Start UWE protection for table:"+tableSet.toString());
        SaveTable.writeJJ(tableSet, Application.getTempFile("UWE.jj"), false, true, tableSet.minFreq[0], false, false);
        TauArgusUtils.DeleteFile(Application.getTempFile("s.txt"));
        TauArgusUtils.DeleteFile(Application.getTempFile("s1.txt"));
        try{
            hs = SystemUtils.getApplicationDirectory(OptiSuppress.class).getCanonicalPath();
        }
        catch (IOException | URISyntaxException ex){
            throw new ArgusException("Exception thrown: "+ex.getMessage());
        }
        hs = hs + "/EXP_ExternalUnpickerOnJJ.exe";
        commandline.add(hs);
        if (!TauArgusUtils.ExistFile(hs)) {
            throw new ArgusException("The UWE unpick program could not be found");
        }
        commandline.add(Application.getTempFile("UWE"));
        commandline.add("Orig");
        TauArgusUtils.writeBatchFileForExec("UWE1", commandline);
        int result = ExecUtils.execCommand(commandline, Application.getTempDir(),false, "Run UWE program step 1");
        if (result !=0){
            throw new ArgusException("The first step of UWE failed");
        }
        //step 2
        commandline.clear();
        try{
            hs = SystemUtils.getApplicationDirectory(OptiSuppress.class).getCanonicalPath();
        } catch (IOException | URISyntaxException ex){
            throw new ArgusException("Exception thrown: "+ex.getMessage());
        }    
        hs = hs + "/COIN_LargeTables.exe";
        if (!TauArgusUtils.ExistFile(hs)) {
            throw new ArgusException("The UWE unpick program could not be found");
        }
        commandline.add(hs);
        commandline.add("UWE");
        commandline.add("GroupedLP");
        TauArgusUtils.writeBatchFileForExec("UWE2", hs);
        result = ExecUtils.execCommand(commandline, Application.getTempDir(),false, "Run UWE program step 2");
        if (result !=0){
            throw new ArgusException("The second step of UWE failed");
        }
        if (!TauArgusUtils.ExistFile(Application.getTempFile("s.txt")) ){
            throw new ArgusException("The second step of UWE did not produce a file with secondaries.\n"+
                                     "File: "+Application.getTempFile("s.txt")+" could not be found");
        }
        BufferedWriter  out2;
        //modify the output for reading the secondaries
        try (BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("s.txt")))) {
            out2 = new BufferedWriter(new FileWriter(Application.getTempFile("s1.txt")));
            out2.write("fop");
            out2.newLine();
            out2.write("fop");
            out2.newLine();
            while ( (hs = in.readLine()) != null){
                hs = hs + " m";
                out2.write(hs); out2.newLine();
            }
        }
        out2.close();
      
        TAUARGUS.SetSecondaryJJFORMAT(tableSet.index, Application.getTempFile("s1.txt"), false, nSec);

        tableSet.nSecond = nSec[0];
        Date endDate = new Date();
        long diff = endDate.getTime()-startDate.getTime();
        diff = diff / 1000;
        tableSet.processingTime = (int) diff;
        tableSet.suppressed = TableSet.SUP_UWE;
        SystemUtils.writeLogbook("End of UWE protection. Time used " + diff + " seconds\n" + 
                                 "Number of suppressions: " + tableSet.nSecond);     
    }
  
    /**
    * Calls the JJ-Rounder for the tableSet.\n
 First writes a JJ file, using SaveTable.writeJJ\n
 Checks a bit the JJ file and optionally replaces the weight/cost to unity, using correctRoundJJ\n
 If required it will partitionate the JJ-file in smaller pieces using splitJJ,  
 using the first spanning variable as the blocking variable.\n
 Then the JJ TAUROUNDER is called and the result is given back to the engine.
    * 
    * @param tableSet
    * @param propertyChangeListener
    * @throws ArgusException
    * @throws IOException 
    */
    
    public static void RunRounder(TableSet tableSet, final PropertyChangeListener propertyChangeListener) throws ArgusException, IOException{
        DialogStopTime rdialog = new DialogStopTime(null,true);
        RCallback jRCallback = new RCallback(){
            @Override
            public int SetExtraTime(){
                rdialog.HideForRounder();
                rdialog.SetUB(DUB);
                rdialog.SetLB(DLB);
                rdialog.SetDiff(100.0*(DUB-DLB)/DLB);
                rdialog.setVisible(true);
                return rdialog.AddTime;
            }
        };

        
        int i, j, j1, nPart=0; 
        String solutionString, hs, xs, Solvername, LicenceFile;
        int solutionType, maxRoundTime, result;
        double[] upperBound = new double[] { 1.0E40 }; 
        double[] lowerBound = new double[] { 0.0 };
        double[] maxJump = new double[1]; 
        int[] numberJump = new int[1]; 
        int[] errorCode = new int[1]; 
        /*int[] solutionType = new int[1];*/
        /*int stopRule;*/
        double[] usedTime = new double[1];
        
        final PropertyChangeSupport pcs = new PropertyChangeSupport(TableService.class);
        pcs.addPropertyChangeListener(propertyChangeListener);
        RProgressListener progressListener = new RProgressListener(){
            @Override
            public void UpdateLowerBound(final double val) {
                pcs.firePropertyChange("lower", null, val);
                DLB = val;
            }
            @Override
            public void UpdateUpperBound(final double val) {
                pcs.firePropertyChange("upper", null, val);
                DUB = val;
            }
            @Override
            public void UpdateNumberOpenSubProb(final int val) {
                pcs.firePropertyChange("open", null, val);
            }
            @Override
            public void UpdateNumberClosedSubProb(final int val) {
                pcs.firePropertyChange("closed", null, val);
            }            
        };
        TAUROUNDER.SetProgressListener(progressListener);
        TAUROUNDER.SetCallback(jRCallback);
        
        //TODO test on MaxTableValue
        Date startDate = new Date();
        Solvername = Application.getSolverName(Application.solverSelected);
        hs = "Start of the rounding procedure for table: " + tableSet.toString();
        if(tableSet.roundPartitions > 0){hs = hs + " (with partitions)";}
        SystemUtils.writeLogbook(hs);
        try{
            if (tableSet.roundPartitions > 0){TauArgusUtils.DeleteFileWild("JJ*.IN", Application.getTempDir());}  
            SaveTable.writeJJ(tableSet, Application.getTempFile("JJ.IN"), true, false, 0, false, false);
            tableSet.roundedInfo = "";
            tableSet.roundTime = 0;
            tableSet.roundJumps = 0;
            tableSet.roundMaxJump += 0;
            tableSet.roundJumps = 0;
            
            maxRoundTime = tableSet.roundMaxTime; // Argument of DoRound is in minutes, just like tableSet.roundMaxTime
            for (i=0;i<2;i++){tableSet.roundSolType[i] = 0;}
            correctRoundJJ(Application.getTempFile("JJ.IN"), tableSet.roundBase, (tableSet.roundPartitions==0), tableSet.roundUnitCost);
            if(tableSet.roundPartitions>0){ nPart = splitJJ(tableSet);}

            // Files ready to round           
            /*
             #define JJZERO          101
             #define JJINF           102
             #define JJMINVIOLA      103
             #define JJMAXSLACK      104
             #define JJMAXTIME       105
            */
            double jjRoundZero = SystemUtils.getRegDouble("optimal", "jjRoundZero", 0.0000001);
            TAUROUNDER.SetDoubleConstant(101, jjRoundZero);
            double jjRoundInf = SystemUtils.getRegDouble("optimal", "jjRoundInf", 21400000000000.0);
            TAUROUNDER.SetDoubleConstant(102, jjRoundInf);
            double jjRoundMinViola = SystemUtils.getRegDouble("optimal", "jjRoundMinViola", 0.0001);
            TAUROUNDER.SetDoubleConstant(103, jjRoundMinViola);
            double jjRoundMaxSlack = SystemUtils.getRegDouble("optimal", "jjRoundMaxSlack", 0.01);
            TAUROUNDER.SetDoubleConstant(104, jjRoundMaxSlack);               

            double X = tableSet.roundBase;
            solutionString = "";
            LicenceFile = "";
            if (Application.solverSelected == Application.SOLVER_CPLEX) LicenceFile = TauArgusUtils.GetCplexLicenceFile();
           
            if (tableSet.roundPartitions > 0){ //round all the partitions
                j1 = 1;  
                solutionString = "<h2>Rounding procedure was applied with partitioning";
                if (tableSet.roundPartitions == 2){
                    solutionString = solutionString + "<br>with artificial totals added";
                }
                if (tableSet.roundPartitions == 3){ 
                    j1 = 0;
                    solutionString = solutionString + "<br>with " + tableSet.roundNumberofBlocks + " blocks";
                }
                hs = "</h2>\n<table>\n" ;
                hs = hs +  "<tr><th width=\"10%\" height=\"11\">No</th>\n";
                hs = hs +  "    <th width=\"15%\" height=\"11\">Sol. type</th>\n";
                hs = hs +  "    <th width=\"15%\" height=\"11\">Time</th>\n";
                hs = hs +  "    <th width=\"15%\" height=\"11\">LowerBound</th>\n";
                hs = hs +  "    <th width=\"15%\" height=\"11\">UpperBound</th>\n";
                hs = hs +  "    <th width=\"15%\" height=\"11\">Number of Jumps</th>\n";
                hs = hs +  "    <th width=\"15%\" height=\"11\">Max. Jump</th></tr>\n";
                solutionString = solutionString + hs;
                tableSet.processingTime = 0;
                for(j=j1;j<nPart;j++){
                    Date startDatePart = new Date();
                    xs = Integer.toString(j);
                    TauArgusUtils.DeleteFile(Application.getTempFile("JJ"+xs+".OUT"));
                    TauArgusUtils.DeleteFile(Application.getTempFile("JJ"+xs+".OUT.RAPID"));
                    TauArgusUtils.DeleteFile(Application.getTempFile("JJRound"+xs+".OUT"));
                    TauArgusUtils.DeleteFile(Application.getTempFile("JJStat"+xs+".OUT"));
                    result = TAUROUNDER.DoRound(Solvername, Application.getTempFile("JJ"+xs+".IN"), X, upperBound, lowerBound, 0,  
                                              /*stopRule,*/
                                              Application.getTempFile("JJ"+xs+".OUT"), 
                                              Application.getTempFile("JJstat"+xs+".OUT"),
                                              LicenceFile, 
                                              Application.getTempFile("JJRound"+xs+".log"),
                                              maxRoundTime, 0,
                                              Application.getTempDir()+"/",
                                              maxJump, numberJump , usedTime, /*solutionType,*/
                                              errorCode); //, activityListener );
                    // Only Optimal is implemented currently, so set manually solutionType = 0;
                    solutionType = 0; 
                    //if (solutionType > 2) {throw new ArgusException("Rounding error code = "+TAUARGUS.GetErrorString(errorCode[0]) + "\noccured in subtable "+j);}             
                    if (result > 0) {throw new ArgusException("Rounding error code = "+TAUARGUS.GetErrorString(errorCode[0]) + "\noccured in subtable "+j);}
                    tableSet.roundMaxJump = Math.max(tableSet.roundMaxJump, maxJump[0]);
                    tableSet.roundJumps = Math.max(tableSet.roundJumps,numberJump[0]);
                    if (maxJump[0] > tableSet.roundMaxJump){tableSet.roundMaxJump = maxJump[0];}
                    //SOLUTION TYPE IS ZOEK!!!!!!!!!!!!! Ik neem aan dat de retrun valeu nu de solution type is
                    //NEE DUS!!!!!!!!
                    //return value > 0 is error; has nothing to do with solutionType !!!!!
                    if (solutionType  > 2){
                        if (TauArgusUtils.ExistFile(Application.getTempFile("JJRound"+xs+".OUT.RAPID"))){
                            TauArgusUtils.renameFile(Application.getTempFile("JJRound"+xs+".OUT.RAPID"),Application.getTempFile("JJRound"+xs+".OUT"));
                        }
                    }
                    tableSet.roundSolType[solutionType]++;
                    hs =  "<tr><td align=\"Right\">" + xs + "</td><td align=\"Right\">";
                    if (solutionType == 2){hs = hs + "Rapid";}
                    if (solutionType == 1){hs = hs + "Feasible";}
                    if (solutionType == 0){hs = hs + "Optimal";}
                    Date endDate = new Date();
                    long diff = endDate.getTime()-startDatePart.getTime();
                    diff = diff / 1000;
                    hs = hs + "</td><td align=\"Right\">" + diff + 
                              "</td><td align=\"Right\">" + upperBound[0] + 
                              "</td><td align=\"Right\">" + lowerBound[0] + 
                              "</td><td align=\"Right\">" + numberJump[0] + 
                              "</td><td align=\"Right\">" + maxJump[0] +
                              "</td></tr>\n\r";
                    solutionString = solutionString + hs;
                }
                solutionString = solutionString + "</table>"; 
                joinRounded(tableSet,nPart);
            }
            else{ // round as a single table
                result = TAUROUNDER.DoRound(Solvername, Application.getTempFile("JJ.IN"), X, upperBound, lowerBound, 0,  
                                          Application.getTempFile("JJ.OUT"), 
                                          Application.getTempFile("JJstat.OUT"),
                                          LicenceFile,
                                          Application.getTempFile("JJRound.log"),
                                          maxRoundTime, 0,  //Max time,zero restricted
                                          Application.getTempDir()+"/",   // NamePathExe
                                          maxJump, numberJump , usedTime, errorCode); //, activityListener );
                solutionType = 0; // Only Optimal is currently implemented
                //SOLUTION TYPE IS ZOEK!!!!!!!!!!!!! Ik neem aan dat de return value nu de solution type is
                //NEE DUS!!!!!!!!
                //return value > 0 is error; has nothing to do with solutionType !!!!!
                if (solutionType  == 1){
                    if (TauArgusUtils.ExistFile(Application.getTempFile("JJRound.OUT.RAPID"))){
                        TauArgusUtils.renameFile(Application.getTempFile("JJRound.OUT.RAPID"),Application.getTempFile("JJRound.OUT"));
                    }
                }                
                //if (solutionType>2) {throw new ArgusException("Rounding error: "+TAUARGUS.GetErrorString(errorCode[0]));}  
                if (result > 0) {throw new ArgusException("Rounding error: "+TAUARGUS.GetErrorString(errorCode[0]));}  
                tableSet.roundMaxJump = maxJump[0];
                tableSet.roundJumps = numberJump[0];
                tableSet.roundSolType[solutionType]++;             
                solutionString = "Solution Type: <b>";
                if(solutionType ==0){solutionString=solutionString+"Optimal";}
                if(solutionType ==1){solutionString=solutionString+"First feasible";}
                if(solutionType ==2){solutionString=solutionString+"Rapid";}
                solutionString=solutionString+"</b>" + 
                     "; LowerBound:" + StrUtils.formatDouble(lowerBound[0], tableSet.respVar.nDecimals) +
                     ", UpperBound: "+ StrUtils.formatDouble(upperBound[0], tableSet.respVar.nDecimals) + "<br>";
            }
            Date endDate = new Date();
            long diff = endDate.getTime()-startDate.getTime();
            diff = diff / 1000;
            tableSet.processingTime = (int)diff;  

            TAUARGUS.SetRoundedResponse(Application.getTempFile("JJ.OUT"), tableSet.index);
            tableSet.roundedInfo = solutionString;
            tableSet.rounded = true;
            tableSet.suppressed = TableSet.SUP_ROUNDING;
            tableSet.solverUsed = Application.solverSelected;
//            hs = ""; Not used????
//            if (tableSet.roundUnitCost){ hs = "\nUnit cost function has been used.";} Not used????
            SystemUtils.writeLogbook("Table has been rounded\n" + 
                                        "Rounding base:   "+ tableSet.roundBase + "\n"+
                                        "Max jump:        " + StrUtils.formatDouble(tableSet.roundMaxJump, tableSet.respVar.nDecimals) + "\n"+
                                        "Number of steps: " + tableSet.roundJumps + "\n" +
                                        "Lowerbound:" + StrUtils.formatDouble(lowerBound[0], tableSet.respVar.nDecimals) + "\n" +
                                        "Upperbound:" + StrUtils.formatDouble(upperBound[0], tableSet.respVar.nDecimals));
            SystemUtils.writeLogbook("End of the rounding procedure");
        } catch(ArgusException ex){ 
            throw new ArgusException("Error in the rounding procedure\n" + ex.getMessage());
        }
        finally{ // cleanup
            rdialog.dispose();
            jRCallback.delete();
        }
    } 
    
    private static boolean correctRoundJJ(String fn, double base, boolean noPartitions, boolean unitCost) throws ArgusException{
        String regelUit, hs; 
        int i, n, p; 
        double cellValue;
        String[] regel = new String[1];
        TauArgusUtils.renameFile (fn, Application.getTempFile("klad"));
        try(BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("klad")));){
            BufferedWriter out2 = new BufferedWriter(new FileWriter(fn));
            regel[0] = in.readLine(); out2.write(regel[0]); out2.newLine();
            regel[0] = in.readLine(); out2.write(regel[0]); out2.newLine();
            n = Integer.parseInt(regel[0]);
            for (i=0;i<n;i++){
                regel[0] = in.readLine(); 
                p = regel[0].indexOf("u");
                if (p > 0 ){ 
                    regelUit = ""; // regel[0].substring(0,p+1);
                    hs = TauArgusUtils.GetSimpleToken(regel);
                    regelUit = regelUit + hs + " ";
                    hs = TauArgusUtils.GetSimpleToken(regel);
                    regelUit = regelUit + hs + " ";             
                    cellValue = Double.parseDouble(hs);
                    hs = TauArgusUtils.GetSimpleToken(regel);
                    if (unitCost) {hs ="1";}
                    regelUit = regelUit + hs + " ";             
                    regelUit = regelUit + TauArgusUtils.GetSimpleToken(regel) + "  ";
                    regelUit = regelUit + " " + TauArgusUtils.GetSimpleToken(regel);
                    regelUit = regelUit + " " + TauArgusUtils.GetSimpleToken(regel);
                    //somehow the protection interval should be in [0,base], but what about negative values?             
                    if (cellValue < base) {
                        regelUit = regelUit +  " " + cellValue;
                        cellValue = base = cellValue;
                        regelUit = regelUit +  " " + cellValue + " 0";
                    } else{
                        regelUit = regelUit + " "+ regel[0];  
                    }
                    out2.write(regelUit); out2.newLine();
                } else{
                    if (unitCost) {
                        regelUit = ""; // regel[0].substring(0,p+1);
                        hs = TauArgusUtils.GetSimpleToken(regel);
                        regelUit = regelUit + hs + " ";
                        hs = TauArgusUtils.GetSimpleToken(regel);
                        regelUit = regelUit + hs + " ";             
                        hs = TauArgusUtils.GetSimpleToken(regel);
                        if (unitCost) {hs ="1";}
                        regelUit = regelUit + hs + " ";
                        out2.write (regelUit + regel[0]);
                    }
                    else {out2.write(regel[0]);}
                    out2.newLine();
                }
            } //end loop over the cells
            if (noPartitions){
                regel[0] = in.readLine();
                if (regel[0].trim().equals("1")){  //somehow JJ does not like one restriction only
                    out2.write("2");  out2.newLine(); 
                    regel[0] = in.readLine();
                    out2.write(regel[0]); out2.newLine();
                    out2.write(regel[0]); out2.newLine();
                }
                out2.write(regel[0]); out2.newLine();
                regel[0] = in.readLine();
                while (regel[0] != null){
                    out2.write(regel[0]); out2.newLine();
                    regel[0] = in.readLine();            
                }          
            }
            else{ //Partitions but what about the single restiction problem?? 
                regel[0] = in.readLine();
                while (regel[0] != null){
                    out2.write(regel[0]); out2.newLine();
                    regel[0] = in.readLine();            
                }    
            }
            in.close();
            out2.close();
            return true;
        }
        catch(Exception ex){ 
            throw new ArgusException("Error in checking the JJ file for rounding");
        }
    }
    
    private static void openOut(int i, boolean append){
       String hs;
       hs = Integer.toString(i);
       if (!append) {TauArgusUtils.DeleteFile(Application.getTempFile("JJ"+ hs + ".IN"));}
       try{ out  = new BufferedWriter(new FileWriter(Application.getTempFile("JJ"+ hs + ".IN"), true));}            
       catch (IOException ex){}
    }
   
    private static void closeOut(){
       try{ out.close();}
       catch (IOException ex){}
    }
   
   
    private static int splitJJ(TableSet tableSet) throws ArgusException {
        Variable EV1; String hs;
        int i, j, k, nRest, p, q;
        int nRel, nRelTot;
        int[] nc = new int[1]; int[]nac = new int[1];
        BufferedReader in;
        try{
            in = new BufferedReader(new FileReader(Application.getTempFile("jj.in")));
            EV1 = tableSet.expVar.get(0);
            TAUARGUS.GetVarNumberOfCodes (EV1.index, nc, nac);
            //deci = tableSet.respVar.nDecimals; Not used ????
            hs = in.readLine();
            hs = in.readLine();
            j = StrUtils.toInteger(hs);
            nRest = j / nac[0];
            //first split the data part.
            if ((tableSet.roundPartitions==1)||(tableSet.roundPartitions==2)){
                if (EV1.hierarchical==Variable.HIER_NONE){
                    for(i=0;i<nac[0];i++){
                        openOut(i,false);
                        out.write("0"); out.newLine();
                        out.write(nRest+ " "); out.newLine();
                        for(j=0;j<nRest;j++){
                            hs = in.readLine().trim();
                            p = hs.indexOf(" ");
                            hs = j+ " "+ hs.substring(p);
                            out.write(hs); out.newLine();
                        }
                        closeOut();
                    } 
                }
                else{throw new ArgusException("Only a non hierarchical first exp var imlemented");}
            }
            else{throw new ArgusException("Only a non hierarchical first exp var imlemented");}
    
            hs = in.readLine();
            j = StrUtils.toInteger(hs);
            //The restrictions
            String hs1, hs2;
            nRel = 0; nRelTot = 0;
            if ((tableSet.roundPartitions==1)||(tableSet.roundPartitions==2)){
                if (EV1.hierarchical==Variable.HIER_NONE){  
                    for (i=0;i<nRest;i++){hs = in.readLine();}
                    j = j - nRest;
                    j = j / nac[0];
                    BufferedWriter jjRelTot;
                    try (BufferedWriter jjRel = new BufferedWriter(new FileWriter(Application.getTempFile("JJREL.IN")))) {
                        jjRelTot = new BufferedWriter(new FileWriter(Application.getTempFile("JJRELTOT.IN")));
                        jjRel.write(j+" ");
                        jjRel.newLine();
                        for (i=0;i<nRest;i++){
                            jjRelTot.write("0 3 :" +i + " (-1)" +(nRest + i) + " (1)" + (2 * nRest + i) + " (1)"); jjRelTot.newLine();  
                            nRelTot++;
                        }   i = 0;
                        while((hs = in.readLine()) != null) {
                            p = hs.indexOf(":");
                            q = hs.indexOf("(");
                            j = StrUtils.toInteger(hs.substring(p+1,q).trim());
                            if(j<nRest){
                                jjRel.write(hs); jjRel.newLine();
                                nRel++;
                                jjRelTot.write(hs); jjRelTot.newLine();
                                nRelTot++;
                                i++;
                                p = hs.indexOf(":");
                                hs1 = hs.substring(0, p+1);
                                hs2 = hs1;
                                hs = hs.substring(p+1);
                                while ((hs != null)  && !hs.equals("")){
                                    p = hs.indexOf("(");
                                    q = hs.indexOf(")");
                                    k = StrUtils.toInteger(hs.substring(0, p));
                                    hs1 = hs1 + " " + (k+nRest) + " " + hs.substring(p,q+1);
                                    hs2 = hs2 + " " + (k+nRest*2) + " " + hs.substring(p,q+1);
                                    hs = hs.substring(q+1).trim();
                                }
                                jjRelTot.write(hs1); jjRelTot.newLine();
                                jjRelTot.write(hs2); jjRelTot.newLine();
                                nRelTot = nRelTot + 2;
                            }
                        }
                    }
                    jjRelTot.close();
                    //Add the relations to the JJ files
                    for (i=0;i<nac[0];i++){
                        openOut(i,true);
                        try (BufferedReader relIn = new BufferedReader(new FileReader(Application.getTempFile("JJREL.IN")))) {
                            hs = relIn.readLine();
                            if (nRel == 1){ //if we have only one relation JJ has some problem. So we double the same relation. Who cares
                                out.write(hs); out.newLine();
                                hs =  relIn.readLine();
                                out.write(hs); out.newLine();
                                out.write(hs); out.newLine();  
                            }else{
                                out.write(hs); out.newLine();
                                for (j=0;j<nRel;j++){
                                    hs =  relIn.readLine();
                                    out.write(hs); out.newLine();
                                }
                            }
                            out.close();
                        }
                    }
                    in.close();
                    return nac[0];
                }   
                else{throw new ArgusException("Only a non hierarchical first exp var imlemented");}
            }
            else{throw new ArgusException("Only a non hierarchical first exp var imlemented");}
        } 
        catch(IOException | ArgusException ex){throw new ArgusException("Error when splitting the JJ file for rounding\n" + ex.getMessage());}
    }
 
   
    private static void joinRounded(TableSet tableSet, int nPart) throws ArgusException 
    {
    // First copy all the subfiles to the master file
    // On the fly compute the aggregate level
    // add the aggegate info at the end  
        int nSubSize, i, n, p1, p2; String hs, regel;
        nSubSize=tableSet.numberOfCells()/nPart;
        double[][] xx = new double[nSubSize][2];
        for (i=0;i<nSubSize;i++){xx[i][0] = 0; xx[i][1] = 0;}
        try(BufferedWriter roundResult = new BufferedWriter(new FileWriter(Application.getTempFile("JJ.OUT")));){
            n=nSubSize;
            for(i=1;i<nPart;i++){
                hs = Integer.toString(i);     
                try (BufferedReader roundIn = new BufferedReader(new FileReader(Application.getTempFile("JJ"+hs+".out")))) {
                    for (int j=0;j<nSubSize;j++){
                        regel = roundIn.readLine();
                        p1 = regel.indexOf("from");
                        p2 = regel.indexOf("to");
                        hs = regel.substring(p1+4, p2);
                        xx[j][0] += StrUtils.toDouble(regel.substring(p1+4, p2));
                        hs = regel.substring(p2+2);
                        xx[j][1] += StrUtils.toDouble(regel.substring(p2+2));
                        roundResult.write(n + " "+regel.substring(p1)); roundResult.newLine();
                        n++;
                    }
                }
            }
            for (int j=0;j<nSubSize;j++){
                roundResult.write("" + j + " from " + xx[j][0] + " to "+ xx[j][1]); roundResult.newLine();
            } 
            roundResult.close();
        } 
        catch (IOException ex){JOptionPane.showMessageDialog(null, ex.toString());}
        catch (ArgusException ex){throw new ArgusException("Error when joining the JJ file for rounding\n" + ex.getMessage());}
    }

    public static void RunOptimal(TableSet tableSet, final PropertyChangeListener propertyChangeListener, Boolean inverseWeight, Boolean externalJJFile, int maxTime) throws ArgusException, FileNotFoundException, IOException{
        DialogStopTime dialog = new DialogStopTime(null,true);    
        ICallback jCallback = new ICallback(){
            @Override
            public int SetStopTime(){
                dialog.SetUB(UB);
                dialog.SetLB(LB);
                dialog.SetDiff(Diff);
                dialog.SetTimeSoFar(TimeSoFar);
                dialog.SetNSuppressed(nSuppressed);
                dialog.setVisible(true);
                return dialog.AddTime;
            }
        };

        int result; 
        //double apBound = 0.5; Not used ????
        String hs;
        int[] nSecondary = new int[1]; int maxTimeAllowed;
        // First check for the max. dimension of the table. 
        if (tableSet.expVar.size() > 4 ) {
            if (Application.isAnco()) {
                hs = "The table has more than 4 dimensions.\n" + 
                     "Running Optimal can take a lot of time and is error-prone.\n" +
                     "Please check the results carefully.\n";
                int warningResult = ShowWarningMessage(hs);
                if (warningResult == 0) {
                    throw new ArgusException("Optimal has not been completed");
                }
            }
            else{
                hs = "The table has more than 4 dimensions.\n" + 
                     "Running Optimal is not possible.\n";   
                throw new ArgusException(hs); 
            }
        }
          
        final PropertyChangeSupport pcs = new PropertyChangeSupport(TableService.class);
        pcs.addPropertyChangeListener(propertyChangeListener);
        IProgressListener progressListener = new IProgressListener(){
            @Override
            public void UpdateUB(final int percentage) {
                pcs.firePropertyChange("value1", null, percentage);
                UB = percentage;
            }
            @Override
            public void UpdateLB(final int percentage) {
                pcs.firePropertyChange("value2", null, percentage);
                LB = percentage;
            }
            @Override
            public void UpdateDiscrepancy(final double percentage) {
                pcs.firePropertyChange("value3", null, 100*percentage); // convert to percentage notation
                Diff = 100*percentage;
            }
            @Override
            public void UpdateTime(final int seconds) {
                pcs.firePropertyChange("value4", null, seconds);
                TimeSoFar = seconds;
            }
            @Override
            public void UpdateNSuppressed(final int value) {
                nSuppressed = value;    
            }
        };
        TAUHITAS.SetProgressListener(progressListener);
        pcs.firePropertyChange("label1", null, "Upper Bound:");
        pcs.firePropertyChange("label2", null, "Lower Bound:");
        pcs.firePropertyChange("label3", null, "Discrepancy:");
        pcs.firePropertyChange("label4", null, "Time used:");
           
        TAUHITAS.SetCallback(jCallback);
         
        Date startDate = new Date();  
            
        TauArgusUtils.DeleteFile(Application.getTempFile("JJ.OUT"));//DeleteFile (Temp + "\JJ.OUT")
        TauArgusUtils.DeleteFile(Application.getTempFile("JJ2.OUT"));//DeleteFile (Temp + "\JJ2.OUT") 
              
        if(!externalJJFile){
            SaveTable.writeJJ(tableSet, Application.getTempFile("JJ.IN"), false, true, tableSet.minFreq[0], false, inverseWeight);
            maxTimeAllowed =  tableSet.maxTimeOptimal;
        }  
        else {
            maxTimeAllowed = maxTime;   
        }
        //result = 9998; //Impossible to be returned???

        if (Application.solverSelected == Application.SOLVER_CPLEX) hs = TauArgusUtils.GetCplexLicenceFile();
        else hs ="";
        TAUHITAS.SetDebugMode(Application.SaveDebugHiTaS);
        
        loadJJParamFromRegistry();
        setJJParamIntauHitas();
        
        result = TAUHITAS.FullJJ(Application.getTempFile("JJ.IN"), Application.getTempFile("JJ.OUT"), 
                                    maxTimeAllowed, hs, Application.getTempDir()+"/", SOLVERNAME[Application.solverSelected]);
        if (result > 1){
            if (result == 8000 || result == 8001) {throw new ArgusException(TAUARGUS.GetErrorString(result));}
            throw new ArgusException("No optimal solution found\n"+TAUARGUS.GetErrorString(result) +
                                     "\nSee also file: " + Application.getTempFile("FullJJ.log"));
        }
        try(BufferedReader  in  = new BufferedReader(new FileReader(Application.getTempFile("JJ.OUT"))); BufferedWriter out2 = new BufferedWriter(new FileWriter(Application.getTempFile("JJ2.OUT")));){
            out2.write ("fop"); out2.newLine();
            out2.write ("fop"); out2.newLine();
            while((hs = in.readLine()) != null) { 
                out2.write(hs + " m"); out2.newLine();
            } 
            in.close();         
        } 
        catch (IOException ex){
            throw new ArgusException("An error occured while processing the output of Optimal");
        }
        finally{ // cleanup
            dialog.dispose();
            jCallback.delete();
        }
        if (externalJJFile){return;}
        TAUARGUS.SetSecondaryJJFORMAT(tableSet.index, Application.getTempFile("JJ2.OUT"), false, nSecondary);
        tableSet.suppressINFO = ReadHitasINFO("fulljj.log");
        //TestTRivialSolution
        tableSet.nSecond = nSecondary[0];
        Date endDate = new Date();
        long diff = endDate.getTime()-startDate.getTime();
        diff = diff / 1000;
  
        tableSet.processingTime = (int) diff;
        tableSet.suppressed = TableSet.SUP_JJ_OPT;
        tableSet.solverUsed = Application.solverSelected;
        tableSet.inverseWeight = inverseWeight;
        SystemUtils.writeLogbook("End of Optimal protection. Time used " + diff + " seconds\n" + 
                                 "Number of suppressions: " + tableSet.nSecond); 
        TAUHITAS.SetProgressListener(null);
        pcs.removePropertyChangeListener(propertyChangeListener);
    }
        
    public static void ProtectJJFormat (String JJInputFile) {
// Run the JJ file        
        final SwingWorker <Integer, Void> worker = new ProgressSwingWorker<Integer, Void>(ProgressSwingWorker.DOUBLE,"Modular approach") {
            @Override
            protected Integer doInBackground() throws ArgusException, Exception{
                super.doInBackground(); 
                try{
                    OptiSuppress.RunOptimal(null, new PropertyChangeListener(){
                        @Override
                        public void propertyChange(PropertyChangeEvent evt){}
                    }, false, true, 1);
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
    
    /*
     * Add the secondaries to a JJ file.
     * Only used for the option to protect a JJ file directly
     * @param JJInputFile
     * @param JJOutputFile 
     */
    public static int addSecondariesToJJFile (String JJInputFile, String JJOutputFile ){
        int i, n, nSec; String hs, regel; int curr;  
        BufferedReader in,sec;  BufferedWriter out2;
        try{
            in = new BufferedReader(new FileReader(JJInputFile));
            sec = new BufferedReader(new FileReader(Application.getTempFile(Application.getTempFile("JJ.OUT"))));
            out2 = new BufferedWriter(new FileWriter(JJOutputFile));
            regel = in.readLine();
            out2.write(regel);out2.newLine();
            regel = in.readLine();
            out2.write(regel);out2.newLine();
            n = Integer.parseInt(regel);
            nSec = 0;
            if (sec.ready()){
                hs = sec.readLine().trim(); 
                curr = Integer.parseInt(hs);} 
            else {curr = 999999999;}
            for(i=0;i<n;i++){
                regel = in.readLine();
                if (i==curr){
                    if (regel.contains("s")){regel= regel.replace("s", "m"); nSec++;}
                    if (sec.ready()){hs = sec.readLine().trim(); curr = Integer.parseInt(hs);} else{curr = 999999999;}
                }
                out2.write(regel); out2.newLine();
            }
            while (in.ready()){
                regel = in.readLine();
                out2.write(regel); out2.newLine();
            }
            in.close(); sec.close(); out2.close();
            return nSec;
        }
        catch (FileNotFoundException ex){return -1;}
        catch (IOException ex){return -2;}    
    }
   
    //Not used???
    /*static void TestTrivialSolution (TableSet tableSet) throws ArgusException{
        CellStatusStatistics stat = tableSet.getCellStatusStatistics();
        int nSafe = stat.freq[CellStatus.SAFE.getValue()] + stat.freq[CellStatus.SAFE_MANUAL.getValue()];
        if (nSafe == 0) {throw new ArgusException("All cells have been suppressed.\n" + 
                                                  "The problem might be infeasible"); }
    }*/

    public static boolean RunCellKeyCont(TableSet tableSet, String PTableFileCont, String PTableFileSep)throws ArgusException, FileNotFoundException, IOException{
        long startTime = new Date().getTime();
        int result;
        Variable var = tableSet.respVar;

        result = TAUARGUS.SetCellKeyValuesCont(tableSet.index, tableSet.cellkeyVar.metadata.getFilePath(PTableFileCont), 
                                var.CKMseparation ? tableSet.cellkeyVar.metadata.getFilePath(PTableFileSep) : "", 
                                var.CKMType, var.CKMTopK, var.zerosincellkey, var.CKMapply_even_odd, var.CKMseparation, 
                                var.CKMm1squared, var.CKMscaling, var.CKMsigma0, var.CKMsigma1, var.CKMxstar, var.CKMq, 
                                var.CKMepsilon, var.muC);
        switch(result){ // Some error
            case -90: throw new ArgusException("No ptable file for continuous CKM found (error -90)");
            case -91: throw new ArgusException("No ptable file for small values found (error -91)");
            case -92: throw new ArgusException("No ptable file for small values found (error -92)");
            case -93: throw new ArgusException("No even and odd specified in ptable file, while asking for treating them differently (error -93)");
            case -94: throw new ArgusException("No all specified in ptable file, while asking for not treating even and odd differently (error -94)");
        }

        long endTime = new Date().getTime();
        long diff = (endTime - startTime)/1000;
        tableSet.processingTime = (int)diff; 
        
        tableSet.suppressINFO = "Cell Key Method has been applied<br>";
        tableSet.suppressed = TableSet.SUP_CKM;
        tableSet.ckmProtect = true;
        batch.reportProgress("The Cell Key Method successfully completed in " + tableSet.processingTime + " seconds");
        
        return true;
    }
    
    public static boolean RunCellKey(TableSet tableSet, String PTableFile) throws ArgusException, FileNotFoundException, IOException{
        // Assumptions on format of p-table:
        // i runs from 0 to maxNi
        // for each i map of pij for which pij > 0
        
        // Currently only reading ptable from file as given in metadata is possible
        long startTime = new Date().getTime();
        int getmin[]={0}, getmax[]={0};
        int result = TAUARGUS.SetCellKeyValuesFreq(tableSet.index, tableSet.cellkeyVar.metadata.getFilePath(PTableFile), getmin, getmax);
        tableSet.minDiff = getmin[0];
        tableSet.maxDiff = getmax[0];
        
        tableSet.CalculateCKMInfo();
        tableSet.CalculateCKMInfoLoss();
        
        if (result == -9 || result == -1){ // error 
            throw new ArgusException("Some error in call of SetCellKeyValuesFreq(...)");
        }
        long endTime = new Date().getTime();
        long diff = (endTime - startTime)/1000;
        tableSet.processingTime = (int)diff; 
        
        tableSet.suppressINFO = "Cell Key Method has been applied<br>";
        tableSet.suppressed = TableSet.SUP_CKM;
        tableSet.ckmProtect = true;
        batch.reportProgress("The Cell Key Method successfully completed in " + tableSet.processingTime + " seconds");
        
        return true;
    }
}
