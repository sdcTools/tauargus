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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JOptionPane;
import tauargus.extern.dataengine.TauArgus;
import tauargus.gui.DialogLinkedTables;
import tauargus.service.TableService;
import tauargus.utils.ExecUtils;
import tauargus.utils.TauArgusUtils;

/**
 *
 * @author Hundepool
 */
public class LinkedTables {
    public static String[] coverVariablesName; 
    public static int[] coverVariablesIndex; 
    public static int[] coverSourceTab; 
    public static int[] coverVarSize;
    public static int[][] toCoverIndex;
    public static int coverDim;
    static String[][] coverCodelist; static int maxCoverCodeList;
    private static int[][] volgNoCL;
    private static final String nullen = "00000000";
    private static final TauArgus tauArgus = Application.getTauArgusDll();
    
    public static boolean TestLinkedPossible()throws ArgusException{
        int i; String respVar0Name;
        if( TableService.numberOfTables() < 2)
        {throw new ArgusException ("For linked tables at least 2 tables are needed");}
        respVar0Name= TableService.getTable(0).respVar.name;
        for (i=1;i<TableService.numberOfTables();i++){
            if (!respVar0Name.equals(TableService.getTable(i).respVar.name)){
               throw new ArgusException ("For linked tables the same respons variable is needed");
            }
        }    
        return true; 
    }
    
    public static boolean runLinkedModular(DialogLinkedTables Moeder, double MSC, double LM, double UM) throws ArgusException{
        String hs = ""; int i,j; TableSet tableSet0, tableSet;
        Date startDate = new Date();
        if (coverDim > 10){
            throw new ArgusException ("The total number of explanatory variables for the Modular approach should not exceed 10");
        }
        for (i=0;i<TableService.numberOfTables();i++){
            hs = hs + "\n"+ (i+1) + ": "+ TableService.getTableDescription(TableService.getTable(i));        } 
        SystemUtils.writeLogbook("Start of modular linked tables procedure\n"+
                                 TableService.numberOfTables()+" tables." + hs);
        tableSet0 = TableService.getTable(0);
        TauArgusUtils.DeleteFile(Application.getTempFile("tempTot.txt"));
        for (i=0;i<TableService.numberOfTables();i++){
          tableSet = TableService.getTable(i);
          j = tableSet.expVar.size();
          if (j > 4) {
              throw new ArgusException ("The max. dimension of the individual tables must not exceed 4");
          }
          TableService.undoSuppress(i);
          tableSet.singletonSingletonCheck = tableSet0.singletonSingletonCheck; 
          tableSet.singletonMultipleCheck = tableSet0.singletonMultipleCheck; 
          tableSet.minFreqCheck = tableSet0.minFreqCheck;
          tableSet.maxHitasTime = tableSet0.maxHitasTime;
        }
        checkCodeList(); //checkCodeLists  //prepareLinked
        exportTables(MSC, LM, UM);//exportTables
        //checkHierarchies werd in de oude versie ook niet gedaan.
        try{
            runCoverTable();
        }
        catch (ArgusException ex){
            //JOptionPane.showMessageDialog(null, ex.getMessage());
            throw(ex);
            //return false;
        }
        
        readResultsBack();
        
        Date endDate = new Date();
        long diff = endDate.getTime()-startDate.getTime();
        diff = diff / 1000;
        if ( diff == 0){ diff = 1;}

        hs = "";
        for (i=0;i<TableService.numberOfTables();i++){
            tableSet=TableService.getTable(i); 
            tableSet.processingTime = (int) diff;
            tableSet.linkSuppressed = true;
            hs = hs + tableSet.CountSecondaries()+ " suppressions in table "+(i+1) + "\n";
        }
        SystemUtils.writeLogbook("End of modular linked tables procedure\n"+
                                 TableService.numberOfTables()+" tables.\n" + 
                                 hs +  
                                 "Processing time: " + diff + " seconds");
        return true;
    }
    
    static void readResultsBack()throws ArgusException{
        // in fact we convert the output into aprory files and run them
        int[] codeIndex = new int[coverDim]; int j; TableSet tableSet;
        String[] codes = new String[coverDim];
        String[] totCodes = new String[coverDim];
        String[] regel = new String[1]; Boolean Oke;
        if (!TauArgusUtils.ExistFile(Application.getTempFile("tempTot.txt"))){
           throw new ArgusException("The results of the protection of the cover table could not be found");}        
  
      for (int i=0;i<TableService.numberOfTables();i++){
        tableSet = TableService.getTable(i);
        try{ 
          for (j=0;j<coverDim;j++){codeIndex[j] = -1; totCodes[j] = "\"" + coverCodelist[j][0] +"\"" ;}
          //for (j=0;j<coverDim;j++){codeIndex[j] = -1; totCodes[j] = "\"Total\"" ;}
          for (j=0;j<tableSet.expVar.size();j++) {codeIndex[toCoverIndex[i][j]]=toCoverIndex[i][j];}
          
          BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile("temp"+i+".hst")));
          BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("tempTot.txt")));
          
          regel[0] = in.readLine();
          while (regel[0] != null){
          if (regel[0].endsWith(",12")){ //secondary found
            Oke = true;
            for (j=0;j<coverDim;j++){
              codes[j] = TauArgusUtils.GetSimpleSepToken(regel, ",");  
              //TODO echte test op Total; dit gaat mis
              if ((codeIndex[j] == -1) && ((!codes[j].equals("\"\""))&&!codes[j].equals(totCodes[j]))) {Oke = false;} //retain total code of not relevant variables
            }
            if (Oke) { 
//             for (j=0;j<tableSet.expVar.size();j++) {out.write ("\""+ codes[toCoverIndex[i][j]]+ "\";");}
              for (j=0;j<tableSet.expVar.size();j++) {
                if (codes[toCoverIndex[i][j]].equals("\"Total\"")){
                    out.write("\"\";");
                } else {
                    out.write (codes[toCoverIndex[i][j]]+ ";");
                }}
              out.write("ml"); out.newLine();
              }
            }
            regel[0] = in.readLine();
          }
          in.close(); 
          out.close();    
        } catch (Exception ex){ throw new ArgusException (ex.getMessage()+ "\nError retrieving the results of modular "+(i+1) );} 
      }
      // Apriory files have been made. Read them back
      for (int i=0;i<TableService.numberOfTables();i++){
        tableSet = TableService.getTable(i); 
        tableSet.suppressINFO = OptiSuppress.ReadHitasINFO("hitas.log");
        tableSet.solverUsed = Application.solverSelected;
        int[][] aPrioryStatus = new int[5][2];
        if (TableSet.processAprioryFile(Application.getTempFile("temp"+i+".hst"), i, ";", true, false, false, aPrioryStatus)!=0){
             TableService.undoSuppress(i);
             throw new ArgusException("Error retrieving linked suppression pattern for table "+(i+1));
        } else {
//            tableSet.nSecond = NSec[0];
            tableSet.suppressed = TableSet.SUP_HITAS; 
            tableSet.nSecond = aPrioryStatus[1][0];
         }          
      }
    }
    
    static void runCoverTable()throws ArgusException{
     String appDir;
     ArrayList<String> commandline = new ArrayList<>();
     //java -jar "D:\TauJava\tauargus\dist\TauArgus.jar"  "C:\Users\ahnl\AppData\Local\Temp\tempTot.arb"
     //hs = "java -jar \"";
     try{  appDir = SystemUtils.getApplicationDirectory(LinkedTables.class).toString(); }
     catch (Exception ex){ throw new ArgusException(ex.getMessage()+"\nError running the cover table:\nCan't find TauArgus.jar");}
     //hs = hs + appDir + "/TauArgus.jar\" \"" + Application.getTempFile("tempTot.arb") + "\"";
     commandline.add("java");
     commandline.add("-jar");
     commandline.add(appDir+"/TauArgus.jar");
     commandline.add(Application.getTempFile("tempTot.arb"));
     if (Application.batchType() != Application.BATCH_COMMANDLINE) {//hs = hs + " /v";}
        commandline.add("/v"); 
     }
     TauArgusUtils.writeBatchFileForExec( "TempTot", commandline);
     SystemUtils.writeLogbook("Start of the Tau-Argus procedure for the modular linked tables procedure");
     int result = ExecUtils.execCommand(commandline, null, false, "Run cover table");
     if (result != 0) { throw new ArgusException("Error running the cover table\nReturned value: "+result);}
    }
    
    static void exportTables(double MSC, double LM, double UM)throws ArgusException{
        // write intermedaite files fro each table temp+i+.tab
        // then merge them into one file tempTot.tab
        // Then write the corresponding metadata file
        int i, j; TableSet tableSet; String regelOut;
        String[] codes = new String[coverDim];String [] regel = new String[1];
        for (i=0;i<TableService.numberOfTables();i++){
            tableSet = TableService.getTable(i);
            try{
            tableSet.write(Application.getTempFile("temp"+i+".tab"),
                           false, false, false, false, false, null);}
            catch (Exception ex){ throw new ArgusException (ex.getMessage()+ "\nError writing intermediate table "+(i+1) );
            }
        }
        try{
          BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile("tempTot.tab")));
          for (i=0;i<TableService.numberOfTables();i++){
            tableSet = TableService.getTable(i);  
            BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("temp"+i+".tab")));
            for (j=0;j<coverDim;j++){codes[j] = "\"\"";}
            regel[0] = in.readLine();
            while (regel[0] != null){
              regelOut = "";
              for(j=0;j<tableSet.expVar.size();j++){
                codes[toCoverIndex[i][j]] = TauArgusUtils.GetSimpleSepToken(regel, ";"); 
              }
              for(j=0;j<coverDim;j++){
                regelOut = regelOut + codes[j] + ";";               
              }
              regelOut = regelOut + regel[0];
              regel[0] = in.readLine();
              out.write(regelOut); out.newLine();
            } in.close();              
          } out.close(); 
          // now the RDA file
         BufferedWriter rda = new BufferedWriter(new FileWriter(Application.getTempFile("tempTot.rda")));
         tableSet = TableService.getTable(0);  
         rda.write("<SEPARATOR>  \";\""); rda.newLine();
         for (i=0;i<coverDim;i++){
           j=coverSourceTab[i];  
           BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("temp"+j+".rda")));
            regelOut = in.readLine();
            // This assumes that the name of a variable does not appear in any text before the definition of the variable
            while (regelOut.indexOf(coverVariablesName[i],0) == -1) {regelOut = in.readLine();}
            rda.write(regelOut); rda.newLine();
            regelOut = in.readLine();
            while (regelOut.indexOf("<",0) != -1) {
             rda.write(regelOut); rda.newLine(); regelOut = in.readLine();
            }
            in.close(); 
         }
         
         BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("temp0.rda")));
         regelOut = in.readLine();
         while (regelOut.indexOf(tableSet.respVar.name,0) == -1) {regelOut = in.readLine();}
         while (regelOut != null){
           rda.write(regelOut); rda.newLine();
           regelOut = in.readLine();
         }
         in.close();
         rda.close();
// and now the batchfile
         
        out = new BufferedWriter(new FileWriter(Application.getTempFile("tempTot.arb")));
        out.write("<OPENTABLEDATA> \""+ Application.getTempFile("tempTot.tab")+"\""); out.newLine();
        out.write("<OPENMETADATA>  \""+ Application.getTempFile("tempTot.rda")+"\""); out.newLine();
        out.write("<SPECIFYTABLE>    ");
        for (j=0;j<coverDim;j++) {out.write("\""+coverVariablesName[j]+"\"");}
        String hs = "|\""+tableSet.respVar.name+"\"";
        out.write(hs+hs);
        if (testDistFunction()){ out.write("|\"-3\"");}
        else                   {out.write(hs);}
        out.newLine();
        // Specific solver may have been set in batchfile (.arb) 
        switch(Application.solverSelected){
            case Application.SOLVER_CPLEX:  out.write("<SOLVER>  CPLEX"); out.newLine(); break;
            case Application.SOLVER_XPRESS: out.write("<SOLVER>  XPRESS"); out.newLine(); break;
            case Application.SOLVER_SOPLEX: out.write("<SOLVER>  FREE"); out.newLine(); break;
        }
        out.write("<SAFETYRULE>"); out.newLine();
        out.write("<COVER>"); out.newLine();
        out.write("<READTABLE>"); out.newLine();
        out.write("<SUPPRESS>   MOD(1, " + Application.generalMaxHitasTime + ", ");
        hs = "0"; if (tableSet.singletonSingletonCheck){hs = "1";} out.write( hs+", ");
        hs = "0"; if (tableSet.singletonMultipleCheck){hs = "1";} out.write( hs+", ");
        hs = "0"; if (tableSet.minFreqCheck){hs = "1";} out.write( hs);
        //if (tableSet.maxScaleCost != 20000){
        //    out.write (", "+tableSet.maxScaleCost);
        //}
        out.write(", "+ String.valueOf(MSC)); // Always write, even when default
        out.write(", " + String.valueOf(LM));
        out.write(", " + String.valueOf(UM));
        out.write (")"); out.newLine();
        out.write("<WRITETABLE> (1, 3, AS+,\""+Application.getTempFile("tempTot.txt")+"\")"); out.newLine();
        out.close();
        TauArgusUtils.DeleteFile(Application.getTempFile("tempTot.txt"));
//        out = new BufferedWriter(new FileWriter(Application.getTempFile("tempTot.bat")));
//        out.write("java -jar \"D:\\TauJava\\tauargus\\dist\\TauArgus.jar\"  \"" + Application.getTempFile("tempTot.arb")+ "\""); out.newLine();
//        out.write ("pause"); out.newLine();
//        out.close();
        }
        catch (Exception ex){throw new ArgusException (ex.getMessage()+ "\nError writing cover table");}     
    }
        
    private static boolean testDistFunction(){
        int i; boolean b = false; TableSet tableSet;
         for (i=0;i<TableService.numberOfTables();i++){
            tableSet = TableService.getTable(i);
            if (tableSet.costFunc == TableSet.COST_DIST){ b = true;}
        }   
        return b;
    }    
    
    public static boolean runLinkedGHMiter() throws ArgusException{
        int i,  returnVal;TableSet tableSet0,tableSet; String hs;
        Date startDate = new Date();
        hs = "";
        for (i=0;i<TableService.numberOfTables();i++){
            hs = hs + "\n"+ (i+1) + " "+ TableService.getTableDescription(TableService.getTable(i));        } 
        SystemUtils.writeLogbook("Start of Hypercube linked tables procedure\n"+
                                 TableService.numberOfTables()+" tables" + hs);
        checkCodeList(); //checkCodeLists //prepareLinked
        //Write all EINGABEs
        for (i=0;i<TableService.numberOfTables();i++){   
            TableService.undoSuppress(i);
            returnVal=tauArgus.WriteGHMITERDataCell(Application.getTempFile("EINGABEAJ"+(i+1)+".TMP"), i, false);
             if (!(returnVal == 1)) { // Something wrong writing EINGABE
                throw new ArgusException( "Unable to write the file EINGABE for the table "+(i+1));}
        }
        //copy GHMiter parameters to other table (The dialog fills table 0, as does the batch command
        tableSet0 = TableService.getTable(0);
        for (i=1;i<TableService.numberOfTables();i++){
           tableSet = TableService.getTable(i);
           tableSet.ratio = tableSet0.ratio;
           tableSet.ghMiterSize = tableSet0.ghMiterSize;
           tableSet.ghMiterApplySingleton = tableSet0.ghMiterApplySingleton;
           tableSet.ghMiterApriory =tableSet0.ghMiterApriory;
           tableSet.ghMiterAprioryPercentage = tableSet0.ghMiterAprioryPercentage;
           tableSet.ghMiterSubcode = tableSet0.ghMiterSubcode;
           tableSet.ghMiterSubtable = tableSet0.ghMiterSubtable;   
        }
     
       for (i=0;i<TableService.numberOfTables();i++){       
          GHMiter.SchrijfSTEUER(i, "AJ"+(i+1));
          tableSet=TableService.getTable(i); 
          GHMiter.OpschonenEINGABE(tableSet.ghMiterAprioryPercentage, tableSet, Application.getTempFile("EINGABEAJ")+(i+1));
          //adjustEingabe: there was a function Adjusteingabe, but what does it do?          
       }
       GHMiter.CleanGHMiterFiles();
       volgNoCL = new int[maxCoverCodeList][coverDim];
       for (i=0;i<TableService.numberOfTables();i++){       
           AdjustEingabe(i);
           AdjustSteuer(i);
           TauArgusUtils.DeleteFile(Application.getTempFile("AUSGABE")+(i+1));
       }
       if (!Application.isAnco()){
           Boolean Oke = TauArgusUtils.DeleteFileWild("EINGABEAJ*", Application.getTempDir());
           Oke = TauArgusUtils.DeleteFileWild("EINGABEAJ*.TMP", Application.getTempDir());
           Oke = TauArgusUtils.DeleteFileWild("STEUERAJ*", Application.getTempDir());          
       }
       GHMiter.SchrijfTABELLE (Application.getTempFile("TABELLE"), 0, true, coverDim); 
       AppendTabelle();
       
       GHMiter.RunGHMiterEXE();
       
       Date endDate = new Date();
       long diff = endDate.getTime()-startDate.getTime();
       diff = diff / 1000;
       if ( diff == 0){ diff = 1;}

       if (!TauArgusUtils.ExistFile(Application.getTempFile("AUSGABE1"))){
             throw new ArgusException("GHMiter could not finish the linked protection succesfully;\n"+ 
                                      "see also file: "+ Application.getTempFile("proto002"));
         }
       hs = "";
       for (i=0;i<TableService.numberOfTables();i++){
         tableSet=TableService.getTable(i); 
         if (!GHMiter.ReadSecondariesGHMiter(tableSet, "AUSGABE"+(i+1))){ 
             throw new ArgusException("Unable to read the hypercube results for table "+(i+1));}
         tableSet.processingTime = (int) diff;
         tableSet.linkSuppressed = true;
         hs = hs + tableSet.CountSecondaries() + " suppressions in table "+(i+1) + "\n";
       }  
       SystemUtils.writeLogbook("End of Hypercube linked tables procedure\n"+
                                hs + 
                               (int) diff +" seconds processing time");

       return true;
    }

    static void AppendTabelle()throws ArgusException{
       int i, j, k, n; TableSet tableSet; Boolean Oke;
      try{
       BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile("TABELLE"), true));
       n = TableService.numberOfTables();
       for(i=0;i<n;i++){
           out.write("EINGABE"+(i+1)); out.newLine();
           out.write("STEUER"+(i+1)); out.newLine();
           out.write("AUSGABE"+(i+1)); out.newLine();
       }
       // per table list the variables of the cover table not in this table
       for(i=0;i<n;i++){
         tableSet = TableService.getTable(i);
         j = coverDim - tableSet.expVar.size();
         out.write(""+j); out.newLine();
         for (k=0;k<coverDim;k++){Oke = false;
           for (j=0;j<tableSet.expVar.size();j++){ 
             if (coverVariablesName[k].equals(tableSet.expVar.get(j).name)) {Oke = true;}
           }
           if (!Oke){
             out.write("'"+coverVariablesName[k]+"'"); out.newLine();
             out.write("00000000"); out.newLine();
           }  
         }
       }
       out.close();
       }catch (Exception ex){
       throw new ArgusException ("An error occurred when appending the file STEUER");}   
    }
    
    
    static void AdjustSteuer(int tabNo)throws ArgusException{
       int i, j, nc; String hs;
       String regelOut; String[] regel = new String[1];
       TableSet tableSet=TableService.getTable(tabNo);  
       int nv = tableSet.expVar.size();
       try{ 
       BufferedReader in  = new BufferedReader(new FileReader(Application.getTempFile("STEUERAJ")+(tabNo+1)));
       BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile("STEUER")+(tabNo+1)));
       for (i=0;i<6;i++){
         regel[0]= in.readLine();
         out.write(regel[0]); out.newLine();
       }
       for (i=0;i<nv;i++){
         regel[0]= in.readLine();  
         out.write("'"+tableSet.expVar.get(i).name+"'"); out.newLine();
         regel[0] = in.readLine();  
         out.write(regel[0]); out.newLine();
         j = tableSet.expVar.get(i).index;
         nc = TauArgusUtils.getNumberOfActiveCodes(j);
         while (nc>0){
           regel[0]= in.readLine();  
           regelOut = "";
           while (!regel[0].equals("")){
             hs = TauArgusUtils.GetSimpleToken(regel);
             j = Integer.parseInt(hs);
             regelOut = regelOut + plusNul(volgNoCL[j][i])+" ";
             nc=nc-1;}
           out.write(regelOut); out.newLine();             
         }
       }
       hs = in.readLine();
       while (!(hs == null)){
         out.write(hs); out.newLine();  
         hs = in.readLine();
       }
       in.close();
       out.close();
       }
       catch (Exception ex){
       throw new ArgusException ("An error occurred when reading and adjusting the file STEUER"+(tabNo+1)+
                                                      " for linked tables");}   
    }
    
    static void AdjustEingabe(int tabNo)throws ArgusException{
      String[] regel = new String[1]; String regelOut, hs;
      int i, j, k, c;
      // replace the codes in EINGABE to correspond with the cover table
      TableSet tableSet=TableService.getTable(tabNo);  
      int nc = tableSet.expVar.size();
      String[] codes = new String[nc]; String[] volgno = new String[nc];
      try{ 
       BufferedReader in  = new BufferedReader(new FileReader(Application.getTempFile("EINGABEAJ")+(tabNo+1)));
       BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile("EINGABE")+(tabNo+1)));
       regel[0] = in.readLine();
       while (!(regel[0] == null)){
         regel[0] = regel[0].trim();
         regelOut = "";
         for (i=0;i<7;i++){
           hs = TauArgusUtils.GetSimpleToken(regel);
           regelOut = regelOut + hs + " ";} 
//         regel[0] = regel[0].replace("'", "");
         for (i=0;i<nc;i++){volgno[i] = TauArgusUtils.GetQuoteToken(regel);
             volgno[i] = volgno[i].replace("'", "");}
         for (i=0;i<nc;i++){codes[i] = TauArgusUtils.GetQuoteToken(regel);
             codes[i] = codes[i].replace("'", ""); codes[i] = codes[i].trim();}
       for (i=0;i<nc;i++){
         k=toCoverIndex[tabNo][i];
         c=-1;
         for (j=0;j<coverVarSize[k];j++){
           if (codes[i].equals("")){c=0;}
           else{ if (codes[i].equals(coverCodelist[k][j].trim())){c=j;j=coverVarSize[k]+2;}             
           }
         }
         if (c==-1) {throw new ArgusException("An error occurred ");}                   
         j = Integer.parseInt(volgno[i]);
         volgNoCL[j][i] = c;
         regelOut = regelOut + "'" + plusNul(c) + "' ";           
       }
       for (i=0;i<nc;i++){regelOut = regelOut + "'"+codes[i]+"' ";}
       out.write(regelOut); out.newLine();
       regel[0] = in.readLine();
       }
           
       in.close();
       out.close();
       }
       catch (Exception ex){
       throw new ArgusException ("An error occurred when reading and adjusting the file EINGABE"+(tabNo+1)+
                                                      " for linked tables");}
    }
    
    static String plusNul(int c){
      String hs; int l;
      hs = "" + c;
      l = 8-hs.length();
      hs = nullen.substring(0,l)+hs;
      return hs;
    }

    static void checkCodeList()throws ArgusException{
        int i, j, k, vi, nt; TableSet tableSet;
        String hs, xs; Boolean found;
        //Make a list of all the codes in the cover table.
        //this will be used later for checking (modular) and futher processing in GHMiter.
        maxCoverCodeList = 0;
        nt = TableService.numberOfTables();
        for (i=0;i<nt;i++){ // all tables
          tableSet=TableService.getTable(i);  
          for (j=0;j<tableSet.expVar.size();j++){
            k = TauArgusUtils.getNumberOfActiveCodes(tableSet.expVar.get(j).index);
            if (maxCoverCodeList < k) {maxCoverCodeList = k;}            
            }
        }
        coverCodelist = new String[coverDim][maxCoverCodeList];
        for (i=0;i<coverDim;i++){
          for (j=1;j<TauArgusUtils.getNumberOfActiveCodes(coverVariablesIndex[i]);j++){
              coverCodelist[i][j] = TauArgusUtils.getCode(coverVariablesIndex[i], j);              
          }
          coverCodelist[i][0] = Application.getVariable(coverVariablesIndex[i]).getTotalCode();
        }
        //The actual checking
        for (i=0;i<nt;i++){ // all tables
          tableSet=TableService.getTable(i);  
          for (j=0;j<tableSet.expVar.size();j++){ //all expVars
            vi=tableSet.expVar.get(j).index;
            for(k=1;k<TauArgusUtils.getNumberOfActiveCodes(vi);k++){
              hs = TauArgusUtils.getCode(vi, k);
              int l=1; int cv = coverVariablesIndex[toCoverIndex[i][j]];
              found = false;
              while (l<TauArgusUtils.getNumberOfActiveCodes(cv)){
                xs = TauArgusUtils.getCode(cv,l);
                if (xs.equals(hs)){ found = true; break;}
                l++;
              } 
              if (!found) {throw new ArgusException("code ("+hs+") not found in the cover table\n"+
                                         "cover var "+coverVariablesName[cv]+"\n"+
                                         "base table "+String.valueOf(i+1));}
              }
          }
        }
    } 
    
    public static boolean buildCoverTable(){
        TableSet tableSet; String hs; int found, i, j, k;
        coverVariablesName = new String[10];
        coverVariablesIndex = new int[10];
        coverSourceTab = new int[10];
        coverVarSize = new int[10];
        coverDim = 0;
        int n = TableService.numberOfTables();
        toCoverIndex = new int [n][TableSet.MAX_RESP_VAR];

        for (i=0;i<n;i++){
            tableSet = TableService.getTable(i);
            for (j=0;j<tableSet.expVar.size();j++){
             hs = tableSet.expVar.get(j).name;
             found = -1;
             for (k=0;k<coverDim;k++){
              if (coverVariablesName[k].equals(hs)){found = k;}             
             }
             if (found == -1) { coverDim++;
                coverVariablesName[coverDim-1] = hs;
                coverVariablesIndex[coverDim-1] = tableSet.expVar.get(j).index;
                coverSourceTab[coverDim-1] = i;
                coverVarSize[coverDim-1] = TauArgusUtils.getNumberOfActiveCodes(tableSet.expVar.get(j).index);
                found = coverDim-1;
             } else {
                 k = TauArgusUtils.getNumberOfActiveCodes(tableSet.expVar.get(j).index);
                 if (coverVarSize[found]<k){
                  coverVarSize[found]=k;
                  coverSourceTab[found] = i;
                  coverVariablesIndex[found] = tableSet.expVar.get(j).index;
                 }
             }
             toCoverIndex[i][j] = found;
            }    
          }  
        return true;
    }
}
