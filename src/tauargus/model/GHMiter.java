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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import tauargus.extern.dataengine.TauArgus;
import tauargus.service.TableService;
import tauargus.utils.ExecUtils;
import tauargus.utils.TauArgusUtils;

/**
 *
 * @author ahnl
 * This package contains all routines needed to run the hypercube/GHMiter method
 * Routines are available for creating the input files for GHMiter
 *                            checking and adapting the input files 
 *                            Retrieving the solution and storing the suppression  
 *  * 
 */
public class GHMiter {
    // private TauArgus tauArgus;
    private static final Logger logger = Logger.getLogger(GHMiter.class.getName());
    private static String Token;
    //private TableSet tableSet; Not used?????
    private static final TauArgus tauArgus = Application.getTauArgusDll();
    private static DecimalFormat ghMiterDecimalFormat;
    public static boolean ShowProto002;
    
    public static void RunGHMiter(TableSet tableSet) throws ArgusException{
        Date startDate = new Date();
        ShowProto002 = false;
        SystemUtils.writeLogbook("Start of the hypercube protection for table " + TableService.getTableDescription(tableSet));

        ghMiterDecimalFormat =  SystemUtils.getInternalDecimalFormat(tableSet.respVar.nDecimals);
        //WriteEingabe ;
        Integer ReturnVal = tauArgus.WriteGHMITERDataCell(Application.getTempFile("EINGABE.TMP"), tableSet.index, false);
        if (!(ReturnVal == 1)) { // Something wrong writing EINGABE
            throw new ArgusException( "Unable to write the file EINGABE for the Hypercube");
        }
        SchrijfSTEUER(tableSet.index, "");
        CleanGHMiterFiles();
        //SchijfTABELLE Nog toevoegen parameters voor linked tables;
        SchrijfTABELLE(Application.getTempFile("TABELLE"), tableSet.index, false, 0);
        //OpschonenEingabe Apriory percentage??
        OpschonenEINGABE(tableSet.ghMiterAprioryPercentage, tableSet, Application.getTempFile("EINGABE"));
        //Run GHMiter
        RunGHMiterEXE();
        //ReadSecondariesBack
        ReadSecondariesGHMiter(tableSet, "AUSGABE");
        Date endDate = new Date();
        long diff = endDate.getTime()-startDate.getTime();
        diff = diff / 1000;
        if ( diff == 0){ diff = 1;}
        tableSet.processingTime = (int) diff;
        tableSet.suppressed = TableSet.SUP_GHMITER;
        SystemUtils.writeLogbook("End of hypercube protection. Time used "+ diff+ " seconds\n" + 
                                    "Number of suppressions: " +tableSet.nSecond); 
    }

    static boolean ReadSecondariesGHMiter(TableSet tableSet, String ausgabe) throws ArgusException {
        int[] NSec; String hs;
        boolean oke;
        NSec = new int[1];
        File f = new File(Application.getTempFile(ausgabe));
        oke = f.exists();
        if (!oke) {
            hs = "The file "+ausgabe+" could not be found";
            if (TauArgusUtils.ExistFile(Application.getTempFile("PROTO002"))){
                hs = "See file PROTO002"; ShowProto002 = true;
            } 
            hs = "The hypercube could not be applied\n" + hs;
            throw new ArgusException(hs);
        }
        
        testProto003(tableSet);

        int OkeCode = tauArgus.SetSecondaryGHMITER(Application.getTempFile(ausgabe), tableSet.index, NSec, false);
        //OkeCode = 4007;
        if (OkeCode == 4007){
            if (Application.isAnco()) tableSet.ghMiterMessage = "Some frozen/protected cells needed to be suppressed\n"; 
            writeFrozen(tableSet.expVar.size());
            OkeCode = 1;
        }
            
        if (OkeCode != 1) { //GHMiter failed
            TableService.undoSuppress(tableSet.index);
        } else {
            tableSet.nSecond = NSec[0];
            tableSet.suppressed = TableSet.SUP_GHMITER;
        }

        return (OkeCode == 1);
    }
    
    static void writeFrozen(int nDim ){
        String regel, EINGABEst = "", AUSGABEst; 
        int i; 
        double x;
        try{
            BufferedReader eingabe = new BufferedReader(new FileReader(Application.getTempFile("EINGABE")));  
            BufferedReader ausgabe = new BufferedReader(new FileReader(Application.getTempFile("AUSGABE")));  
            BufferedWriter frozen = new BufferedWriter(new FileWriter(Application.getTempFile("frozen.txt")));  
            frozen.write("Overview of frozen cells");frozen.newLine();
            frozen.write("Cell value and codes");frozen.newLine();
            while((EINGABEst = eingabe.readLine()) != null) {
                AUSGABEst = ausgabe.readLine();
                EINGABEst = EINGABEst.trim();
                AUSGABEst = AUSGABEst.trim();
                AUSGABEst = GetToken(AUSGABEst);  
                if (Token.equals("1129")){ // a frozen cell found
                    for(i=0;i<=2;i++){EINGABEst = GetToken(EINGABEst);}
                    regel = Token; //The cell value
                    for(i=0;i<=3;i++){EINGABEst = GetToken(EINGABEst);} // Tehremainder are the spanning codes
                    x = Double.parseDouble(Token);
                    if (x == 0){
                        for (i=0;i<nDim;i++){ EINGABEst = GetToken(EINGABEst);}
                        regel = regel + " : " + EINGABEst;
                        frozen.write(regel); frozen.newLine();
                    }
                }
            }
            frozen.close();
            eingabe.close();
            ausgabe.close();
        } 
        catch(Exception ex){}      
    }
    
    static void testProto003(TableSet tableSet)throws ArgusException {
        int i, nt; 
        String[] regel = new String[1]; 
        String hs;
        for (i=0;i<TableSet.MAX_GH_MITER_RATIO;i++) {tableSet.ghMiterRatio[i] = 0;} 
        if (!TauArgusUtils.ExistFile(Application.getTempFile("proto003"))){ return;}
        try{
            BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("proto003")));
            regel[0] = in.readLine().trim();
            nt = 0;
            for (i=0;i<TableSet.MAX_GH_MITER_RATIO;i++){
                hs = TauArgusUtils.GetSimpleToken(regel);
                if (!hs.equals("")) tableSet.ghMiterRatio[i] = Integer.parseInt((hs));
                nt = nt + tableSet.ghMiterRatio[i];
            }
            if (nt !=0 ) {tableSet.ghMiterMessage = tableSet.ghMiterMessage + "Some (" + 
                            nt + ") cells could not be fully protected\nSave table and see report file for more info.";}  
        }
        catch(Exception ex){}
    }
     
    public static void RunGHMiterEXE() throws ArgusException{
        ArrayList<String> commandline = new ArrayList<>();
        String GHMiter = "";
        try {
            GHMiter = SystemUtils.getApplicationDirectory(GHMiter.class).getCanonicalPath();
        } catch (Exception ex) {}

        //GHMiter = "\"" + GHMiter + "/Ghmiter4.exe\"";  //Results in double quotes (""GHMiter4.exe""), some systems cannot deal with that correctly 
        GHMiter += "\\Ghmiter4.exe";
        commandline.add(GHMiter);

        TauArgusUtils.writeBatchFileForExec( "RunGH", commandline);       
        int result = ExecUtils.execCommand(commandline, Application.getTempDir(),false, "Run Hypercube");
        if (result != 0){
            throw new ArgusException("A problem was encountered running the hypercube");}
    }

    static void OpschonenEINGABE(double aprioryPerc, TableSet tableSet, String fileName)throws ArgusException {
        int D, p, Flen, RespLen; String Hs, Regel;
        String Stat, Freq, ValueSt, minResp, maxResp; double Value, X;
        Variable variable = tableSet.respVar;
        D = variable.nDecimals;
        try{
            BufferedReader in  = new BufferedReader(new FileReader(fileName+".TMP"));
            Hs = in.readLine();
            Hs = Hs.trim();
            p = Hs.indexOf(" ");
            Hs = Hs.substring(p);
       
            Flen = Hs.length();
            Hs = Hs.trim();
            p = Hs.indexOf(" ");
            Hs = Hs.substring(p);
            Flen = Flen-Hs.length();
       
            RespLen = Hs.length();
            Hs = Hs.trim();
            p = Hs.indexOf(" ");
            Hs = Hs.substring(p);
            RespLen = RespLen-Hs.length();
            in.close();
            
            in  = new BufferedReader(new FileReader(fileName+".TMP"));    
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
       
            while((Hs = in.readLine()) != null) {
                Hs = Hs.trim();
                Hs = GetToken(Hs); Stat = Token;
                Hs = GetToken(Hs); Freq = Token;
                Hs = GetToken(Hs);  ValueSt = Token;
                Value = Double.parseDouble(ValueSt);
                if( (Freq.equals("2") ) & (Value == 0) & (Stat.equals("1"))) {Freq = "0";}
                if ( Freq.equals("1") & Stat.equals("1") ) { Freq = "2";}
                if ( Freq.equals("1") & Stat.equals("129") & !tableSet.ghMiterApplySingleton) {Freq = "2";} 
                Regel = AddLeadingSpaces(Stat,5) + 
                        AddLeadingSpaces(Freq,Flen) + 
                        AddLeadingSpaces(ValueSt,RespLen); 
                if ( aprioryPerc == 100 && tableSet.minTabVal == 0) {
                    //basically do nothing
                    Regel = Regel + "  " + Hs;             
                }
                else { // first 2 lousy one's
                    Hs = GetToken(Hs); Regel = Regel + " "+ Token;
                    Hs = GetToken(Hs); Regel = Regel + " "+ Token;
                    Hs = GetToken(Hs); maxResp = Token;
                    Hs = GetToken(Hs); minResp = Token;
                    if ( Double.parseDouble(minResp) == 0 && Double.parseDouble(maxResp) == 0) {X = 0;}
                    else { X = Math.abs(aprioryPerc / 100.0) * Value;}
                    Regel = Regel + " "+  String.format(Locale.US, "%."+D+"f", X); //ghMiterDecimalFormat.format(X);
                    if (Double.parseDouble(minResp) > 0){
                        if ((Value - X) < tableSet.minTabVal) { X = Value - tableSet.minTabVal;}
                    }
                    Regel = Regel + " "+  String.format(Locale.US, "%."+D+"f", X); //ghMiterDecimalFormat.format(X);
                    Regel = Regel + " "+ Hs;
                }
                out.write (Regel);  out.newLine();
            }
            out.close();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new ArgusException("A problem was encountered when preparing the file EINGABE");
        }
    }
 
    static boolean SchrijfTABELLE (String FileTABELLE, int tIndex, 
        boolean Linked, Integer CoverDim) throws ArgusException {
        Integer t1, t2, P1, P4, P5, NExpVar, D;
        int j, NA; 
        String Hs; 
        double CellResp, MinTVal;
        TableSet tableSet;
        if (!Linked) { t1 = tIndex; t2=tIndex;}
        else { t1 = 0; t2= TableService.numberOfTables()-1; }
        P4 = 0;  P5 = 0;
        // ANCO nog een loopje over de tabellen voor linked
        for (int t=t1;t<=t2;t++){
            tableSet = TableService.getTable(t);
            NExpVar = tableSet.expVar.size();
            for ( int i=1; i<=NExpVar; i++){
                Variable variable = tableSet.expVar.get(i-1);
                j=variable.index;
                NA = TauArgusUtils.getNumberOfActiveCodes(j);   
                if (P4 < NA) {P4=NA;}
                if (P5 < NExpVar) {P5 = NExpVar;}
            }
        }
        if (Linked) { P5=CoverDim; tableSet = TableService.getTable(0); }
        else        { tableSet = TableService.getTable(tIndex);}
        Hs = "";
        switch (tableSet.ghMiterSize){
            case 0: Hs = "50960000 200 6000"; 
                    break;
            case 1: P1 = 62500000 + 250 + 225000; 
                    P1 = (P1 + 63 + P4 + 100000) * 4;
                    Hs = Integer.toString(P1) + " 250 25000";
                    break;   
            case 2: P1 = 9 * tableSet.ghMiterSubtable;
                    P1 = (62500000 + tableSet.ghMiterSubcode + P1 + 63 * P4 + 100000) * 4;
                    Hs = Integer.toString(P1)  +" " + Integer.toString(tableSet.ghMiterSubcode)+" "+ Integer.toString(tableSet.ghMiterSubtable);
                    break;             
        }
     
        Hs = Hs + " " + Integer.toString(P4) + "  " + Integer.toString(P5);
     
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(FileTABELLE));
            out.write (Hs ); out.newLine();
            Hs = "0 0 1";
            out.write (Hs ); out.newLine();
            CellResp = 0;
            MinTVal = 0;
            D = 0;
            for (int t=t1;t<=t2;t++){
                tableSet = TableService.getTable(t);  
                CellResp = Math.max(CellResp, tableSet.maxTabVal);
                MinTVal = Math.min(MinTVal, tableSet.minTabVal);
                D = Math.max(D, tableSet.respVar.nDecimals);
            }
            Hs = String.format(Locale.US, "%."+D+"f", CellResp);
            if (MinTVal >=0) {Hs = "0.000005 "+ Hs;}
            else {Hs = String.format(Locale.US, "%."+D+"f", 1.5 * MinTVal) + " " +
                             String.format(Locale.US, "%."+D+"f", 1, CellResp - MinTVal);}
            out.write (Hs ); out.newLine();
            out.write("0.00"); out.newLine();
            if (MinTVal >= 0) {out.write("0");}
            else {out.write("1");}
            out.newLine();
            j = 0;
            for (int t=t1;t<=t2;t++) {
                tableSet = TableService.getTable(t); 
                j = Math.max(j, tableSet.expVar.size());
            }
            out.write (" " +j+"  1 ");  out.newLine();//MaxDim
            out.write (t2-t1+1+" "); out.newLine();
            out.close();
            return true;
        } catch (Exception ex) {
            throw new ArgusException("Unable to write the file TABELLE for the Hypercube");
        }
    }
         
    public static void SchrijfSTEUER(Integer TableNumber, String number) throws ArgusException {
        String HS, HS1;
        TableSet tableSet = TableService.getTable(TableNumber);
        double X, Y;
        X = 0;
        Y = 0;
        if (tableSet.domRule){
            if (tableSet.domK[0] != 0){ X = 100.0 / tableSet.domK[0]; X = 2 * (X-1);} 
            else{ X = 100.0 / tableSet.domK[2]; X = 2 * (X-1);} 
        }
        if (tableSet.pqRule){
            if (tableSet.pqP[0] != 0) {Y = 2 * tableSet.pqP[0] / 100.0; } 
            else {Y = 2 * tableSet.pqP[2] / 100.0; }
        }
        if (Y > X) { X = Y;}
        if (X == 0) {
            if (tableSet.frequencyRule){
                if (tableSet.frequencyMarge[0] != 0) { X= tableSet.frequencyMarge[0] * 2 /100.0;}
                else { X= tableSet.frequencyMarge[1] * 2 /100.0;}            
            }
            else if (tableSet.piepRule[0]){ X = tableSet.piepMarge[0] * 2 / 100.0;}
            else if (tableSet.piepRule[1]){ X = tableSet.piepMarge[1] * 2 / 100.0;} 
            else if (tableSet.manualMarge != 0) {X = tableSet.manualMarge * 2 / 100.0;}
        }           
        if (tableSet.ghMiterApriory){ HS1 = "0 1 0";}
        else {X = 0;  HS1 = " 0 0 0";}
        tableSet.ratio = X;
    
        // getting info on table-parameters
        HS = String.format(Locale.US,"%10.8f", X); // Using Locale.US to ensure the use decimalseparator = "."
        HS = HS + "  0.00";
        Integer OkeCode = tauArgus.WriteGHMITERSteuer(Application.getTempFile("STEUER"+number), HS, HS1, TableNumber);
        if (OkeCode != 1) {throw new ArgusException("Unable to write the file STEUER for the Hypercube");}
    }
    

    static void CleanGHMiterFiles() {
        TauArgusUtils.DeleteFileWild("PROTO*.*", Application.getTempDir());
        TauArgusUtils.DeleteFile(Application.getTempFile("proto001"));
        TauArgusUtils.DeleteFile(Application.getTempFile("proto002"));
        TauArgusUtils.DeleteFile(Application.getTempFile("proto003"));
        TauArgusUtils.DeleteFile(Application.getTempFile("AUSGABE"));
        TauArgusUtils.DeleteFile(Application.getTempFile("Ft17f001"));
        TauArgusUtils.DeleteFile(Application.getTempFile("Ft14f001"));
        TauArgusUtils.DeleteFile(Application.getTempFile("Ft09file"));
        TauArgusUtils.DeleteFile(Application.getTempFile("Ft10file"));
        TauArgusUtils.DeleteFile(Application.getTempFile("Ft12file"));
        TauArgusUtils.DeleteFileWild("Ft*fi*.*", Application.getTempDir());
        TauArgusUtils.DeleteFileWild("Ft*f0*.*", Application.getTempDir());
        TauArgusUtils.DeleteFile(Application.getTempFile("SCHNEID"));
        TauArgusUtils.DeleteFile(Application.getTempFile("MAMPTABI"));
        TauArgusUtils.DeleteFile(Application.getTempFile("VARIABLE"));
        TauArgusUtils.DeleteFile(Application.getTempFile("AGGPOSRC"));
        TauArgusUtils.DeleteFile(Application.getTempFile("ENDE"));
        TauArgusUtils.DeleteFile(Application.getTempFile("frozen.txt"));
    }

    static String GetToken(String St) {
        Integer p;
        String Hs;
        p = St.indexOf(" ");
        if (p == 0) {
            Token = St;
            Hs = "";
        } else {
            Token = St.substring(0, p);
            Hs = St.substring(p + 1).trim();
        }
        return Hs;
    }

    static String AddLeadingSpaces(String St, Integer Len) {
        Integer L;
        String Hs;
        L = St.length();
        L = Len - L;
        Hs = String.format(Locale.US,"%" + Len + "s", St); //??? Shouldn't this be L instead of Len ????
        return Hs;
    }
}
