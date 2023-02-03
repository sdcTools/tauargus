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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import tauargus.extern.dataengine.TauArgus;
import static tauargus.model.TableSet.MAX_GH_MITER_RATIO;
import tauargus.service.TableService;
import tauargus.utils.TauArgusUtils;
import tauargus.utils.Tokenizer;

/**
 *
 * @author ahnl
 */
public class SaveTable {
    public static boolean writeAddStatus;
    public static boolean writeSupppressEmpty;
    public static boolean writeVarnamesOnFirstLine;
    public static boolean writeEmbedQuotes;
    public static boolean writeSBSHierarchicalLevels;
    public static boolean writeIntermediateStatusOnly;
    public static boolean writeIntermediateAddAudit;
    public static boolean writeIntermediateUseHolding;
    public static boolean writeJJRemoveBogus;
    public static boolean writeCKMOriginalValues;
    public static boolean writeCKMDifferences;
    public static boolean writeCKMCellKeys;
    
    private static final String[] HI = {"Individual", "Holding"};
    
    private static final TauArgus tauArgus = Application.getTauArgusDll();
    /**
     * Writes a table in one of the 7 formats
     * @param tableSet
     * @param selectedFormat
     * @throws ArgusException 
     */
    
    private static String addOption ( String optStr, boolean active ){
        if (active) { return optStr + "+";}
        else        { return optStr + "-";}
    }
    
    static public void writeTable(final TableSet tableSet, int selectedFormat)throws ArgusException{
        int respType = 0; 
        String hs;
        //Boolean oke= true; Not used ????
        int[] dimSequence = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        if (tableSet.rounded ){respType = 1;}
        if (tableSet.ctaProtect) {respType = 2;}
        if (tableSet.ckmProtect) {respType = 3;}
        tableSet.safeFileFormat = selectedFormat;
        hs = "";
        hs = hs + addOption ("HL",SaveTable.writeSBSHierarchicalLevels);
        hs = hs + addOption ("SO",SaveTable.writeIntermediateStatusOnly);        
        hs = hs + addOption ("AR",SaveTable.writeIntermediateAddAudit);
        hs = hs + addOption ("HI",SaveTable.writeIntermediateUseHolding);
        hs = hs + addOption ("AS",SaveTable.writeAddStatus);
        hs = hs + addOption ("SE",SaveTable.writeSupppressEmpty);
        hs = hs + addOption ("FL",SaveTable.writeVarnamesOnFirstLine);
        hs = hs + addOption ("QU",SaveTable.writeEmbedQuotes);
        hs = hs + addOption ("TR",SaveTable.writeJJRemoveBogus);
        hs = hs + addOption ("AO",SaveTable.writeCKMOriginalValues);
        hs = hs + addOption ("AD",SaveTable.writeCKMDifferences);
        hs = hs + addOption ("AC",SaveTable.writeCKMCellKeys);
        tableSet.safeFileOptions= hs;                
        
        switch (selectedFormat) {
            case TableSet.FILE_FORMAT_CSV:
                if (!tauArgus.WriteCSV(tableSet.index, tableSet.safeFileName, writeEmbedQuotes, dimSequence, respType)){
                    throw new ArgusException ("An unexpected error occurred when writing the CVS file to " + tableSet.safeFileName);
                }
                break;
            case TableSet.FILE_FORMAT_PIVOT_TABLE:
                hs = makeFirstLine(tableSet, respType);
                if (!tauArgus.WriteCellRecords(tableSet.index, tableSet.safeFileName, 0,
                                                false, writeSupppressEmpty, hs, writeAddStatus, writeEmbedQuotes, respType)){
                    throw new ArgusException ("An unexpected error occurred when writing the CVS file to " + tableSet.safeFileName);
                }
                break;
            case TableSet.FILE_FORMAT_CODE_VALUE:
                if (writeVarnamesOnFirstLine) {
                    hs = makeFirstLine(tableSet, respType);
                }
                else {hs = "";}
                if (!tauArgus.WriteCellRecords(tableSet.index, tableSet.safeFileName, 0, false, writeSupppressEmpty, hs, writeAddStatus, writeEmbedQuotes, respType)){
                    throw new ArgusException ("An unexpected error occurred when writing the Code-value file to " + tableSet.safeFileName);
                }
                break;
            case TableSet.FILE_FORMAT_SBS:
                if (!tauArgus.WriteCellRecords(tableSet.index, tableSet.safeFileName, 1, writeSBSHierarchicalLevels, true, "", true, writeEmbedQuotes, respType)){
                    throw new ArgusException ("An unexpected error occurred when writing the SBSS file to " + tableSet.safeFileName);
                }
                break;
            case TableSet.FILE_FORMAT_INTERMEDIATE: 
                // The GUI thread (EDT) should not be used for long running tasks, 
                // so use the SwingWorker class
                // However  in batch this has been disabled.
                if (Application.isBatch()){
                    try{
                        tableSet.write(tableSet.safeFileName,
                                        writeSupppressEmpty,
                                        writeIntermediateStatusOnly,
                                        writeIntermediateUseHolding,
                                        writeIntermediateAddAudit,
                                        writeEmbedQuotes,
                                        null);
                        return;  
                    }
                    catch (IOException ex) {}                            
                }
                else{
                    final SwingWorker<Void, Void> worker = new ProgressSwingWorker<Void, Void>(ProgressSwingWorker.SINGLE, "Saving table") {
                        // called in a separate thread...
                        @Override
                        protected Void doInBackground() throws Exception {
                            super.doInBackground();
                            tableSet.write(tableSet.safeFileName,
                                            writeSupppressEmpty,
                                            writeIntermediateStatusOnly,
                                            writeIntermediateUseHolding,
                                            writeIntermediateAddAudit,
                                            writeEmbedQuotes,
                                            getPropertyChangeListener());
                            return null;
                        }

                        // called on the GUI thread
                        @Override
                        public void done() {
                            super.done();
                            try {
                                get();
                            } 
                            catch (InterruptedException ex) {} 
                            catch (ExecutionException ex) {JOptionPane.showMessageDialog(null, ex.getCause().getMessage());}
                        }
                    };
                    worker.execute();
                }
                break;
            case TableSet.FILE_FORMAT_JJ:
                writeJJ(tableSet, "", false, false, 0, writeJJRemoveBogus, false);
                break;
            case TableSet.FILE_FORMAT_CKM:
                try{
                    tableSet.writeCKM(writeCKMOriginalValues, writeCKMDifferences, writeCKMCellKeys, 
                                      writeSupppressEmpty, writeEmbedQuotes, null);
                } catch (IOException ex) {}                            
                break;                
        }
        
        SystemUtils.writeLogbook("Table: " + tableSet.toString() + " has been written\n"+
                                 "Output file name: "+tableSet.safeFileName);
    }
    
    /**
     * writes the variable names  on the first line,needed fro pivot table and SBS
     * @param tableSet
     * @param respType
     * @return 
     */
    private static String makeFirstLine (TableSet tableSet, int respType){
        String hs;
        String quote = "";
        if (writeEmbedQuotes) quote = "\"";
        hs = "";
        for (int i=0; i<tableSet.expVar.size();i++){
            hs = hs + quote + tableSet.expVar.get(i).name + quote +",";
        }
        hs = hs.substring(0, hs.length()-1);
        if (tableSet.respVar  == Application.getFreqVar()){ 
            hs = hs + "," + quote + "Freq" + quote;
        }
        else{
            hs = hs + "," + quote + tableSet.respVar.name;
            if (respType == 1){hs = hs + "_Round";}
            if (respType == 2){hs = hs + "_CTA";}
            hs = hs + quote;
        }
        //TODO CTA and rounded
        if (writeAddStatus){
            if ( respType == 0 ){hs = hs + "," + quote + "Status" + quote;}       
            if ( (respType == 1) || (respType == 2) ){hs = hs + "," + quote + tableSet.respVar.name + "_Orig" + quote;}       
            if ( respType == 2 ){hs = hs + "," + quote + "Status" + quote;}       
        }
        return hs;
    }
    
    // Writes table in JJ-format to file to beable to perform audit on CKM-protecetd freq count table
    // I.e. assumptions: 
    //      cells are integers
    //      cells are non-negative
    //      forRounding = false
    //      singleton = false
    //      withBogusRemoval = false
    //      inverseWeight = false
    // xmin and xmax are not needed: for each cell aprioribound equals perturbed value +/- maxD
    // STILL TO BE IMPLEMENTED CORRECTLY
    public static void writeJJforCKMaudit(TableSet tableSet, String fileName, boolean forRounding, 
                                            boolean singleton, int minFreq, boolean withBogusRemoval, 
                                            boolean inverseWeight)throws ArgusException{
        double xMin, xMax;
        xMin = tableSet.minTabVal;
        xMax = tableSet.maxTabVal;
        if ("".equals(fileName)) {fileName = tableSet.safeFileName;} 
        if (!tauArgus.WriteJJFormat(tableSet.index, fileName, xMin, xMax, withBogusRemoval, false, forRounding)){
            throw new ArgusException("An unexpected error occurred when writing the JJ-file");
        }
        if (singleton) {addSingletonToJJ(tableSet, fileName, minFreq);}
        opschonenJJFileforKomma(fileName, tableSet, inverseWeight);
    }
        
    public static void writeJJ(TableSet tableSet, String fileName, boolean forRounding, 
                               boolean singleton, int minFreq, boolean withBogusRemoval, 
                               boolean inverseWeight)throws ArgusException{
        double xMin, xMax;
        xMin = tableSet.minTabVal;
        xMax = tableSet.maxTabVal;
        if ("".equals(fileName)) {fileName = tableSet.safeFileName;} 
        if (!tauArgus.WriteJJFormat(tableSet.index, fileName, xMin, xMax, withBogusRemoval, false, forRounding)){
            throw new ArgusException("An unexpected error occurred when writing the JJ-file");
        }
        if (singleton) {addSingletonToJJ(tableSet, fileName, minFreq);}
        opschonenJJFileforKomma(fileName, tableSet, inverseWeight);
    }
    /**
     * For the singleton protection in Optimal and UWE artificial cells are added.
     * The combination of two singletons or a sing-mult is added as an extra cell with minimal protection level.
     * This will cause at least an extra suppression the row/column.
     * This is even more efficient than the old method in Modular. In Modular the prot. levels of some cells was increased, 
     * and this caused overprotection in the other directions.
     * @param tableSet
     * @param fileName
     * @param minFreq
     * @throws ArgusException 
     */
    public static void addSingletonToJJ(TableSet tableSet, String fileName, int minFreq)throws ArgusException{
        String hs; 
        int nRec, nr, d, i, j; 
        double minLPL;
        int nSing, nMult, nFreq, addRec;
        boolean singTrick; String lb="", ub="";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
    
        DecimalFormat normalPrecision = new DecimalFormat();
        normalPrecision.setMinimumFractionDigits(0);
        normalPrecision.setMaximumFractionDigits(0);
        normalPrecision.setDecimalFormatSymbols(symbols);
        normalPrecision.setGroupingUsed(false);
        
        File fileJJKlad = new File(Application.getTempFile("klad.jj"));
        File fileJJ = new File(fileName);
        if (fileJJKlad.exists()) { fileJJKlad.delete();}         
        fileJJ.renameTo(new File(Application.getTempFile("klad.jj")));        
         
        Tokenizer JJKlad;
        JJKlad = null;
        try{
            BufferedReader bf = new BufferedReader(new FileReader(Application.getTempFile("klad.jj")));
            JJKlad = new Tokenizer(bf);
        } catch (Exception ex) {}
         
        try{
            BufferedReader JJFreq = new BufferedReader(new FileReader(fileName + ".frq"));
            BufferedWriter JJAddCell = new BufferedWriter(new FileWriter(Application.getTempFile("AddCell.jj")));
            BufferedWriter JJAddRel = new BufferedWriter(new FileWriter(Application.getTempFile("AddRel.jj")));
            JJKlad.nextLine();hs = JJKlad.getLine();
            JJKlad.nextLine();hs = JJKlad.getLine();
            nRec = Integer.parseInt(hs);
            double[] XV = new double[nRec];
            int[] FQ = new int[nRec];
            String[] ST = new String[nRec];
            int[] WT = new int[nRec];
            d = tableSet.respVar.nDecimals;
            normalPrecision.setMinimumFractionDigits(d);
            normalPrecision.setMaximumFractionDigits(d);
            minLPL = 1.0/(Math.pow(10,d));           
            //Read cells
            for(i=0;i<nRec;i++){
                JJKlad.nextLine();
                hs = JJKlad.nextToken();
                XV[i] = Double.parseDouble(JJKlad.nextToken());
                WT[i] = Integer.parseInt(JJKlad.nextToken());
                ST[i] = JJKlad.nextToken();
                hs = JJFreq.readLine().trim();
                int space = hs.lastIndexOf(" ");
                FQ[i]= Integer.parseInt(hs.substring(0,space));
                if (i==0){ lb = JJKlad.nextToken(); ub = JJKlad.nextToken();}
            }
            JJKlad.nextLine();
            nr = Integer.parseInt(JJKlad.nextToken());
            addRec = 0;
            //Read relations
            String extraRel;  double x; int f; double pl;
            for(i=0;i<nr;i++){              
                extraRel="";  
                JJKlad.nextLine();  
                for (j=0;j<6;j++) {hs = JJKlad.nextToken();}
                nSing = 0; nMult =0; nFreq = 0; x=0; f=0;
                while ( !JJKlad.getLine().equals("")){
                    j = Integer.parseInt(JJKlad.nextToken());
                    hs = JJKlad.nextToken(); hs = JJKlad.nextToken();
                    if (ST[j].equals("u")){
                        if (FQ[j] == 1){ nSing++; }
                        else           { nMult++; }
                        nFreq = nFreq + FQ[j];
                        extraRel = extraRel + " "+ j+" (1)";
                        x=x+XV[j]; f=f+FQ[j];
                    }
                }
                singTrick = false;     
                if ((nSing==2) && (nMult==0) && tableSet.singletonSingletonCheck) singTrick = true;
                if ((nSing==1) && (nMult==1) && tableSet.singletonMultipleCheck) singTrick = true;
                if ((nFreq<minFreq) && (nFreq>0) &&tableSet.minFreqCheck && (nSing+nMult>0)) singTrick = true;
                if (singTrick) {
                    j = nRec + addRec;
                    extraRel = j + " (-1) " + extraRel;
                    j = nSing + nMult + 1;
                    extraRel =  "0 "+ j + " : "+ extraRel;
                    JJAddRel.write(extraRel); JJAddRel.newLine();
                    hs = String.valueOf(nRec + addRec) + " "+ normalPrecision.format(x);
                    hs = hs +  " 1 u " + lb + " "+ ub +" ";
                    pl = x * 0.0001;
                    if (pl < minLPL) {pl=minLPL;}
                    hs = hs + "0 "+  normalPrecision.format(pl) + " 0.0";
                    JJAddCell.write(hs); JJAddCell.newLine();
                    addRec++;
                }  
            }
            JJAddRel.close(); JJAddCell.close();// bf.close();
            //Bring everything together
            BufferedWriter JJFile = new BufferedWriter(new FileWriter(fileName));
            BufferedReader JJKladIn = new BufferedReader(new FileReader(Application.getTempFile("klad.jj")));
            BufferedReader JJAddCellIn = new BufferedReader(new FileReader(Application.getTempFile("AddCell.jj")));
            BufferedReader JJAddRelIn = new BufferedReader(new FileReader(Application.getTempFile("AddRel.jj")));
            JJFile.write("0");   JJFile.newLine();
            JJFile.write (nRec+addRec+""); JJFile.newLine();
            hs = JJKladIn.readLine();hs = JJKladIn.readLine();
            for (i=0;i<nRec;i++){
                hs = JJKladIn.readLine();
                JJFile.write (hs); JJFile.newLine();
            }
            for (i=0;i<addRec;i++){
                hs = JJAddCellIn.readLine();
                JJFile.write (hs); JJFile.newLine();
            }
            JJFile.write (nr+addRec+""); JJFile.newLine();
            hs = JJKladIn.readLine();
            for (i=0;i<nr;i++){
                hs = JJKladIn.readLine();
                JJFile.write (hs); JJFile.newLine();
            }
            for (i=0;i<addRec;i++){
                hs = JJAddRelIn.readLine();
                JJFile.write (hs); JJFile.newLine();
            }
            JJFile.close(); JJAddRelIn.close(); JJAddCellIn.close(); JJKladIn.close();
            JJKlad.close();
        }
        catch (IOException ex) {
            throw new ArgusException("An error occured when adding additional relations to the JJ file");
        }
    }
    
    /**
     * Some final checking of the JJ file. E.g. the true inequality LB <x-lpl < x < x+ulp < UB
     * is guaranteed by adding an epsilon in an extra decimal 
     * @param fileName
     * @param tableSet
     * @param inverseWeight
     * @throws ArgusException 
     */
    public static void opschonenJJFileforKomma(String fileName, TableSet tableSet, Boolean inverseWeight)throws ArgusException{
        int d, i, j, nRec; double[] xx = new double[4]; String hs, regelOut;
        double resp, cost;
        d = tableSet.respVar.nDecimals;
        double epsilon1 = 1.0/(Math.pow(10,d+1)); 
        double epsilon = 1.0/(Math.pow(10,d)); 
        //DecimalFormat normalFormat = SystemUtils.getInternalDecimalFormat(d); Not used ?????
        DecimalFormat normalFormat1 = SystemUtils.getInternalDecimalFormat(d+1);
        DecimalFormat normalFormat3 = SystemUtils.getInternalDecimalFormat(d+3);
    
        File fileJJKlad = new File(Application.getTempFile("JJklad.jj"));
        File fileJJ = new File(fileName);
        if (fileJJKlad.exists()) { fileJJKlad.delete();}         
        fileJJ.renameTo(new File(Application.getTempFile("JJklad.jj")));        

        Tokenizer JJKlad;
        JJKlad = null;
        try{
            BufferedReader bf = new BufferedReader(new FileReader(Application.getTempFile("JJklad.jj")));
            JJKlad = new Tokenizer(bf);
        } catch (Exception ex) {}
         
        try{
            BufferedWriter JJFile = new BufferedWriter(new FileWriter(fileName));
            JJKlad.nextLine();hs = JJKlad.getLine(); JJFile.write(hs); JJFile.newLine();
            JJKlad.nextLine();hs = JJKlad.getLine();JJFile.write(hs); JJFile.newLine();
            nRec = Integer.parseInt(hs);
            for (i=0;i<nRec;i++){
                JJKlad.nextLine();
                hs = JJKlad.nextToken();
                regelOut = hs;// hs.format("%7s");
                hs = JJKlad.nextToken();
                resp = Double.parseDouble(hs);
                regelOut = regelOut + " "+ hs;
                hs = JJKlad.nextToken().replace("-", " ");  //cost
                if (inverseWeight){
                    cost = Double.parseDouble(hs);
                    if(cost==0) cost = 1;
                    cost = tableSet.maxScaleCost/cost;
                    j = (int) cost;
                    if (j == 0) j =1;
                    hs = Integer.toString(j);
                }                   
                regelOut = regelOut + " "+ hs;
                hs = JJKlad.nextToken(); //status
                regelOut = regelOut + " "+ hs;
                if (hs.equals("z")){
                    regelOut = regelOut + " "+ JJKlad.getLine();
                }else{
                    for (j=0;j<4;j++){xx[j] = Double.parseDouble(JJKlad.nextToken());}  
                    if ( xx[0] < tableSet.minTabVal){xx[0]= tableSet.minTabVal;} //check lbound
                    if (resp-xx[2]< xx[0]) {xx[2]= resp - xx[0];} //check LPL  
                    xx[2] = realLPL(xx[2], resp, epsilon1, tableSet.minTabVal);
                    xx[3] = realUPL(xx[3], epsilon);
                    for (j=0;j<2;j++){
                        regelOut = regelOut + " " + normalFormat1.format(xx[j]);
                    } 
                    for (j=2;j<4;j++){regelOut = regelOut + " " + normalFormat3.format(xx[j]);} 
                    regelOut = regelOut + " " + JJKlad.getLine(); 
                }   
                regelOut.replace(",", ".");
                JJFile.write(regelOut); JJFile.newLine();
            }
            while (JJKlad.nextLine() != null) {
                JJFile.write (JJKlad.getLine());JJFile.newLine();
            } 
            JJFile.close();
            JJKlad.close();
        } catch (Exception ex) {
            throw new ArgusException ("An error occured when checking the JJ file");
        }
    }



    static private double realLPL(double LPL, double resp, double epsilon, double minTabVal){
        double x, rlpl;
        rlpl = LPL;
        x = resp - minTabVal; // The space below
        if (x<0) {x=0;} // should not be possible
        if (rlpl + epsilon> x){rlpl = x-epsilon;}
        if (rlpl<0){rlpl=0;}
        return rlpl;
    }
    
    static private double realUPL (double UPL, double epsilon)  {
        double rupl;  //vague/forgotten why this is needed
        rupl = UPL;
        if (rupl<epsilon){rupl = epsilon;}
        return rupl;
    }

    /**
    * Writes the first lines of an HTML file
    * @param tableSet
    * @param out
    * @param kort 
    */
    static public void writeKopHtml(TableSet tableSet, BufferedWriter out, boolean kort) throws ArgusException {
        String hs; int i;
        try {
            out.write("<!DOCTYPE HTML PUBLIC \" -//W3C//DTD HTML 4.0 Transitional//EN\"   \"http://www.w3.org/TR/REC-html40/loose.dtd\">\n");
            out.write ("<html>\n");        
            out.write ("<head>\n");     
            out.write("<title>&tau;-ARGUS report</title>\n");
            try{
                hs = SystemUtils.getApplicationDirectory(SaveTable.class).getCanonicalPath();
            } catch (Exception ex){
                throw new ArgusException("Exception thrown: "+ex.getMessage());
            }
            hs = "file:///" +hs + "/tauARGUS.css";
            hs = hs.replace("\\", "/");
            out.write(" <link rel=\"stylesheet\" type=\"text/css\" href=\"" + hs + "\">\n");
            out.write("</head>\n");
            out.write("<body>\n");
            if (kort) return;
            out.write("<h1>&tau;-ARGUS Report </h1><p>\n");
            Date date = new Date();
            hs = date.toString();
            out.write(hs + "\n");
        
            out.write("<table>\n");
            out.write("<tr><td width=\"15%\" height=\"11\">Original file:</td>\n");
            out.write("<td width=\"85%\" height=\"11\">");
            out.write( tableSet.metadata.dataFile);
            out.write( "</td></tr>\n");
            out.write("<tr><td>Meta file:</td><td>");
            out.write( tableSet.metadata.metaFile);
            out.write( "</td></tr>\n");
            if (!tableSet.safeFileName.equals("")){
                out.write("<tr><td>Table file:</td><td>" + tableSet.safeFileName + "</td></tr>\n");
            }
            out.write("</table>\n");
            out.write("<p>\n");     

            if (tableSet.metadata.dataOrigin ==  Metadata.DATA_ORIGIN_MICRO) {
                out.write ("<h2>Table generated from microdata</h2>\n");
            }else{
                out.write("<h2>Table read as table</h2>\n");
            }
           
            out.write("<h2>Table structure</h2>"); out.newLine();
            printTableInfo(tableSet, out);
       
            if (tableSet.linkSuppressed){
                out.write("<h2>Table has been suppressed with the linked table procedure</h2>"); out.newLine();
                out.write("The other tables:<br>"); out.newLine();
                for (i=0;i<TableService.numberOfTables();i++){
                    if (i!=tableSet.index) { printTableInfo(TableService.getTable(i), out); }
                }
            }       

            if ( tableSet.costFunc == TableSet.COST_DIST){
                out.write("<h2>Distance function used</h2>\n");
                out.write("<table>\n");
                out.write("<tr><th width=\"40%\" height=\"11\">Var</th>\n");
                for(i=1;i<tauargus.model.Variable.MAX_NUMBER_OF_DIST;i++){
                    out.write("<th width=\"10%\" height=\"11\">"+ i+ "</th>\n");
                }
                out.write("</tr>\n");
                for (i=1; i<tableSet.expVar.size(); i++){
                    out.write("<tr><td>" +tableSet.expVar.get(i-1).name  + ":</td>\n");
                    if (tableSet.expVar.get(i-1).hasDistanceFunction){
                        for (int j=1;j< tauargus.model.Variable.MAX_NUMBER_OF_DIST;j++){
                            out.write("<td>"+tableSet.expVar.get(i-1).distanceFunction[j-1]+ "</td>\n");
                        } 
                    } else {
                        for (int j=1;j< tauargus.model.Variable.MAX_NUMBER_OF_DIST;j++){
                            out.write("<td>1</td>\n");            
                        }
                    }
                    out.write("</tr>\n");
                }
                out.write("</table>\n");
            }
            if (tableSet.lambda != 1) {
                out.write("<br>Lambda transformation for cost function: "+tableSet.lambda+"\n" );  
            }
            if (tableSet.computeTotals){ 
                out.write("<br>Missing totals have been computed\n");
                if (tableSet.keepStatus){
                    out.write("<br>Statusses are kept. This could potentially cause inconsistencies between statussen.\n");
                    out.write("<br>E.g. when unsafe cells are copied to empty totals or when protected cells are used.\n");
                    out.write("<br>Be sure to check the results.");
                }
            }    
        }
        catch (IOException ex) {}
    }
  
    static void writeStaartHTML(BufferedWriter out ){
        try{
            out.write ("<br>&tau;-ARGUS version: " + Application.getFullVersion()+" (Build " + Application.BUILD + ")");  out.newLine();
            out.write ("<br>ArgusLib version: " + argus.model.Application.MAJOR + "." + argus.model.Application.MINOR + "." +
                                    argus.model.Application.REVISION + " (Build " + argus.model.Application.BUILD + ")"); out.newLine();
            out.write("</body>\n");
            out.write("</html\n");
        }
        catch (IOException ex) {}
    }
    
    /**
     * This routine writes the report file for the produced safe table.
     * The name of the report file is the same as the safe file, except the extention, that will be changed into html 
     * The report makes use of a CSS file tauARGUS.css
     * @param tableSet 
     */
    static public void writeReport(TableSet tableSet) {
        String reportfile, hs; double x;
        reportfile = tableSet.safeFileName;
        int i = reportfile.lastIndexOf(".");
        reportfile = reportfile.substring(0, i) + ".html";
        try { 
            BufferedWriter out = new BufferedWriter(new FileWriter(reportfile));
            writeKopHtml(tableSet, out, false);

            out.write("<p>\n");
            //out.write("<h2>Sensitivity Rule:</h2><h3>\n");
            out.write("<h2>Sensitivity Rule:</h2>\n");
            if (tableSet.ckmProtect && !tableSet.respVar.isResponse()){
                out.write("None used for Noise addition using the Cell Key Method<br>\n");
            }
            if (tableSet.domRule) {
                for (i=0;i<4;i++){
                    hs = prDOM(i, tableSet);
                    if (! hs.equals("")) {out.write(hs);}
                }  
            }
            if (tableSet.pqRule) {
                for (i=0;i<4;i++){
                    hs = prPQ(i, tableSet);
                    if (! hs.equals("")) {out.write(hs);}
                }  
            }
            if(tableSet.frequencyRule){
                for (i=0;i<2;i++){
                    if (tableSet.minFreq[i] !=0){
                        hs = "Minimun "+HI[i];
                        hs = hs + " cell frequency: " + tableSet.minFreq[i] + " ;safety margin: " + 
                                tableSet.frequencyMarge[i]+ "%";
                    if (tableSet.minFreq[i] == 0 ){hs = hs + "<b>Possible poor protection</b>";}
                    out.write (hs+"<br>\n");                               
                    }
                }
            }
//ToDo    
            if (tableSet.piepRule[0] || tableSet.piepRule[1]){
                for (i=0;i<tableSet.metadata.variables.size();i++){
                    Variable v = tableSet.metadata.variables.get(i);
                    if (v.requestCode != null){
                        out.write( "Request variable: " + v.name+ "<br>\n");
                        out.write( "&nbsp;&nbsp;codes("+v.requestCode[0]);
                        if (v.requestCode[1] != null) {out.write("," + v.requestCode[1]);}
                        out.write(")<br>\n");              
                    }  
                }           
                for (i=0;i<2;i++){
                    if (tableSet.piepRule[i]){
                        out.write("Request-rule on " + HI[i] + " level applied <br>\n"); 
                        out.write("&nbsp;&nbsp;percentages: " + tableSet.piepPercentage[2*i] + ", " + tableSet.piepPercentage[2*i+1]+"<br>\n" ); 
                        out.write("&nbsp;&nbsp;minfreq:     " + tableSet.piepMinFreq[i] + "<br>\n" ); 
                        out.write("&nbsp;&nbsp;safety margin: " + tableSet.piepMarge[i] + "%<br>\n" ); 
                    }
                }
            }  
        
            if (tableSet.zeroUnsafe){
                out.write("Zero-cells with contributors are treated as unsafe; safety margin: "+
                            tableSet.zeroRange+"%<br>\n");
            }

            if (!tableSet.ckmProtect) {
                out.write("Manual safety margin: " + tableSet.manualMarge + "%<br>\n");
                out.write("Missing codes have been considered ");
                if (!tableSet.missingIsSafe){out.write("un");}
                out.write ("safe<br>\n");
            }
                        
            if (tableSet.weighted){out.write("Sample weights have been used<br>\n");}
            if (tableSet.minTabVal !=0){out.write("Minimum lower bound for each cell " + tableSet.minTabVal + "<br>\n");} 

            //out.write("</h3>\n");
   
            switch (tableSet.suppressed) {
                case TableSet.SUP_JJ_OPT : 
                    out.write("<h2>Optimal Salazar solution</h2>\n");  
                    out.write("<h2>Solver used: "+Application.getSolverName(tableSet.solverUsed) +"</h2>\n");
                    if (tableSet.inverseWeight){out.write("<h2>The inverted weights have been used.</h2>\n");  }
                    out.write("<h6>"+tableSet.suppressINFO+"</h6>\n");
                    break;
                case TableSet.SUP_GHMITER :
                    out.write("<h2>GHMITER solution</h2>\n");     
                    out.write("<h3>GHMITER range ratio used: " + StrUtils.formatDouble(tableSet.ratio,3)  + " </h3>\n");
                    if (tableSet.ghMiterSize == 0){ out.write ("<h3>GHMITER normal model used</h3>\n");}
                        else if (tableSet.ghMiterSize == 1){ out.write ("<h3>GHMITER large model used</h3>\n");}
                            else {out.write("<h3>GHMITER large (manual specified) model used</h3>\n");}
                    hs = "";
                    if (! tableSet.ghMiterApplySingleton) {hs = " not";}
                    out.write("Singleton protection in GHMiter has" + hs + " been applied\n");              
                    break;
                case TableSet.SUP_HITAS :     
                    out.write("<h2>Modular (HITAS) Salazar solution</h2>\n");
                    out.write("<h2>Solver used: "+Application.getSolverName(tableSet.solverUsed) +"</h2>\n");
                    out.write("<h6>"+tableSet.suppressINFO+"</h6>\n");
                    out.write("<h3>Max time per subtable: " + tableSet.maxHitasTime + " minutes</h3>\n");
                    break;
                case TableSet.SUP_ROUNDING : 
                    out.write("<h2>Rounding procedure with "+Application.getSolverName(tableSet.solverUsed) +" applied <BR>rounding base " + tableSet.roundBase + "</h2>");
                    out.write( "&nbsp;&nbsp;&nbsp;&nbsp;Number of steps: " + tableSet.roundJumps + ";  Max distance: " + 
                                StrUtils.formatDouble(tableSet.roundMaxJump, tableSet.respVar.nDecimals)
                                +"<br>"); out.newLine();
                    out.write(tableSet.roundedInfo+"\n");
                    break;
                case TableSet.SUP_CTA :              
                    out.write("<h2>Controlled Tabular Adjustment has been applied</h2>\n");
                    out.write("<h3>" + tableSet.suppressINFO +"</h3>");
                    break;
                case TableSet.SUP_NETWORK : 
                    out.write("<h2>The network solution has been applied</h2>\n");
                    out.write("<h3>" + tableSet.suppressINFO +"</h3>");
                    break;
                case TableSet.SUP_NO :
                    out.write( "<h2>Not protected yet</h2>\n");
                    break;
                case TableSet.SUP_UWE :
                    out.write( "<h2>Protected with the experimental UWE-software</h2>\n");
                    break;
                case TableSet.SUP_CKM :
                    out.write("<h2>Protected with the Cell Key Method</h2>\n");
                    if (tableSet.respVar.isResponse()){
                        Variable var=tableSet.respVar;
                        out.write("<h3>Parameters used:</h3>\n");
                        out.write("<table>\n");
                        out.write("<tr><td width=\"20%\">CKM Type</td>\n");
                        out.write("<td width=\"80%\">");
                        switch(var.CKMType){
                            case "T": out.write("T: using TopK largest with TopK = "+var.CKMTopK); break;
                            case "M": out.write("M: using cell mean"); break;
                            case "D": out.write("D: using distance between largest and smallest"); break;
                            case "V": out.write("V: using cell value"); break;
                            default: out.write("N: CKM not allowed");
                        }
                        out.write("</td></tr>\n");
                        out.write("<tr><td>Scaling</td><td>");
                        if (var.CKMscaling.equals("F")){
                            out.write("F: using flex function with");
                            out.write(" &sigma;<sub>0</sub> = "+var.CKMsigma0);
                            out.write(", &sigma;<sub>1</sub> = "+var.CKMsigma1);
                            out.write(", x* = "+var.CKMxstar);
                            out.write(", q = "+var.CKMq);
                            for (int k=1;k<=var.CKMTopK;k++){
                                out.write(", &epsilon;<sub>"+k+"</sub> = "+var.CKMepsilon[k-1]);
                            }
                        }
                        if (var.CKMscaling.equals("N")){
                            out.write("N: simple version with &sigma;<sub>1</sub> = "+var.CKMsigma1);
                            for (int k=1;k<=var.CKMTopK;k++){
                                out.write(", &epsilon;<sub>"+k+"</sub> = "+var.CKMepsilon[k-1]);
                            }
                        }
                        out.write("</td></tr>\n");
                        out.write("<tr><td>Zero contributions</td>");
                        out.write("<td>"+ (var.zerosincellkey ? "Y: " : "N: "));
                        out.write("zero contributions are "+ (var.zerosincellkey ? "" : "not") +" included in recordkey</td></tr>\n");
                        out.write("<tr><td>Even/odd</td>");
                        out.write("<td>"+ (var.CKMapply_even_odd ? "Y: " : "N: "));
                        out.write("Even and odd number of contributors are "+ (var.CKMapply_even_odd ? "" : "not") +" treated differently</td></tr>\n");
                        out.write("<tr><td>Small cells</td>");
                        out.write("<td>" + (var.CKMseparation ? "Y: " : "N: "));
                        out.write("Small cells are " + (var.CKMseparation ? "" : "not") + " treated differently</td></tr>\n");
                        if (var.muC > 0){
                            out.write("<tr><td>Additional protection</td>");
                            out.write("<td>Additional protection with &mu;<sub>C</sub> = "+ var.muC +" for sensitive cells</td></tr>\n");
                        }
                        out.write("</table><p>\n");
                        
                        out.write("<h3>P-table used from files</h3>\n");
                        out.write("<table>\n");
                        out.write("<tr><td width=\"15%\">p-table for general cells</td>");
                        out.write("<td width=\"85%\">"+tableSet.cellkeyVar.metadata.getFilePath(tableSet.cellkeyVar.PTableFileCont)+"</td></tr>\n");
                        if (var.CKMseparation){
                               out.write("<tr><td>p-table for small cells</td>");
                               out.write("<td>"+tableSet.cellkeyVar.metadata.getFilePath(tableSet.cellkeyVar.PTableFileSep)+"</td></tr>\n");
                        }
                        out.write("</table>\n");
                    }
                    else{
                        out.write("<h3>P-table used from file\n\""+tableSet.cellkeyVar.metadata.getFilePath(tableSet.cellkeyVar.PTableFile)+"\"</h3>\n");
                    }
                    break;
            }

            if (tableSet.suppressed == TableSet.SUP_HITAS || tableSet.suppressed == TableSet.SUP_JJ_OPT ||
                tableSet.suppressed == TableSet.SUP_UWE ) {
                hs = tableSet.singletonSingletonCheck ? " " : " not ";
                out.write("<h3>Additional Singleton/Singleton option has" + hs + "been used<br>\n");
                
                hs = tableSet.singletonMultipleCheck ? " " : " not ";
                out.write ("Additional Singleton/Multiple option has" + hs + "been used<br>\n");
        
                hs = tableSet.minFreqCheck ? " " : " not ";
                out.write ("Additional Min. Frequency option has" + hs + "been used</h3>\n");
            }
    
            if (tableSet.suppressed == TableSet.SUP_GHMITER) {
                int j = 0;
                for (i=0;i<MAX_GH_MITER_RATIO;i++){j = j + tableSet.ghMiterRatio[i];}
                if (j > 0) {
                    out.write("<h2>Details on the GHMITER solution</h2>\n");
                    if (!tableSet.ghMiterMessage.equals("")){
                        out.write(tableSet.ghMiterMessage+ "<br>"); out.newLine();
                    }
                    out.write("Sliding protection ratio corresponding to safety rule employed: R =" + StrUtils.formatDouble(tableSet.ratio, 3) + ".<br>\n");
                    out.write("This ratio had to be reduced in some cases,\n");
                    out.write("because otherwise no feasible solution could be found.<br><br>\n");
                    out.write("Number of cases where sliding protection range was reduced,\n");
                    out.write("by finally confirmed sliding protection ranges:<br>\n");
                    if (j - tableSet.ghMiterRatio[MAX_GH_MITER_RATIO -2] - tableSet.ghMiterRatio[MAX_GH_MITER_RATIO -1] > 0){
                        out.write("<table border=\"1\" width =\"90%\"><tr>\n");
                        out.write("<td width=\"9%\">confirmed protection level</td>\n");
                        for (i=1;i<=10;i++){
                            out.write("<td width=\"9%\">R/" + i + "=" + StrUtils.formatDouble(tableSet.ratio / i, 3) + " </td>\n");
                        }                   
                        out.write("</tr><tr>\n");
                        out.write("<td width=\"9%\">n of cases</td>\n");
                        for (i=0;i<=9;i++){
                            out.write("<td width=\"9%\" align=\"right\">" + tableSet.ghMiterRatio[i] + " </td>\n");
                        }
                        out.write(" </tr></table>\n");
                        out.write("<p>\n");
                    }
                    if ( tableSet.ghMiterRatio[MAX_GH_MITER_RATIO -2] > 0){
                        out.write("In "+ tableSet.ghMiterRatio[MAX_GH_MITER_RATIO -2]+ 
                                    " cases the protection level had to be reduced to an 'infinitely' small (positive) value.<br>\n");
                    }
                    if ( tableSet.ghMiterRatio[MAX_GH_MITER_RATIO -1] > 0){
                        out.write("In "+ tableSet.ghMiterRatio[MAX_GH_MITER_RATIO -1]+ " cases the protection level was reduced to zero, making 'frozen' cells available\n");
                        out.write("for suppression (see the manual for illustration of how to trace these cells).<br> \n");
                    }
                    out.write("Note that the number of cases with range reduction reported by the statistics above is\n");
                    out.write("very likely to exceed the actual number of cells concerned, because cells belonging to\n");
                    out.write("multiple (sub-) tables are counted multiple times.\n");
                    out.write("<p>\n");
                }
            }
            if (tableSet.suppressed != TableSet.SUP_NO){
                out.write("<h2>Time used to protect the table: " + StrUtils.timeToString(tableSet.processingTime)+"</h2>\n");
            }
            if (tableSet.hasBeenAudited) {
                if((tableSet.auditExactDisclosure+tableSet.auditPartialDisclosure)==0) {
                    out.write("<h2>The audit did not find any disclosure problems</h2>\n");
                }
                else{
                    try{
                        BufferedReader audit = new BufferedReader(new FileReader(Application.getTempFile("audit_"+ tableSet.index+".html")));
                        int status = 0;
                        while ((hs=audit.readLine()) != null){
                            if (hs.contains("XXXXXXXX")) status++;
                            if (status == 1) out.write(hs+"\n");
                            if (status == 2) break;
                        }
                        audit.close();
                    } 
                    catch (IOException ex){}
                }
            } 

            out.write("<h2>Summary of the table</h2>\n");
       
            if (!tableSet.ckmProtect){
                int d= tableSet.respVar.nDecimals;
                CellStatusStatistics stat = tableSet.getCellStatusStatistics();
     
                out.write("<table>\n");
                out.write("<tr><th height=\"11\">&nbsp;</th>\n");
                out.write("<th height=\"11\">Status</th>\n");
                out.write("<th height=\"11\">Number of cells</th>\n");
                out.write("<th height=\"11\">Number of respondents</th>\n");
                if (tableSet.holding){ out.write("<th height=\"11\">Number of holdings freq.</th>\n");}
                out.write("<th height=\"11\">Response value</th>\n");
                out.write("<th height=\"11\">Cost value</th>\n");
                out.write("</tr>\n");

                for (i=CellStatus.SAFE.getValue();i<=CellStatus.EMPTY.getValue()+1;i++){
                    if ( (i!=CellStatus.UNSAFE_SINGLETON.getValue()) && (i!=CellStatus.UNSAFE_SINGLETON_MANUAL.getValue()) ){
                        out.write("<tr><td align=\"Right\">"+ i+ "</td>");
                        if (i<CellStatus.EMPTY.getValue()+1){
                            out.write("<td>" + CellStatus.findByValue(i).getDescription()+ "</td>");
                        } else {
                            out.write("<td>Total</td>");
                        }
                        out.write("<td align=\"Right\">" + stat.freq[i] + "</td>\n");
                        out.write("<td align=\"Right\">" + stat.cellFreq[i] + "</td>\n");
                        if (tableSet.holding){out.write("    <td align=\"Right\">" + stat.holdingFreq[i] + "</td>\n");}
                        x = stat.cellResponse[i];
                        hs = String.format(Locale.US, "%."+d+"f", x);
                        out.write("<td align=\"Right\">" + hs+ "</td>\n");
                        x = stat.cellCost[i];
                        hs = String.format(Locale.US, "%."+d+"f", x);
                        out.write("<td align=\"Right\">" + hs + "</td></tr>\n");
                    }
                }
                out.write("</table>\n");
                out.write("<p>\n");
            }
            else{
                if (!tableSet.respVar.isResponse()){ // for time being only in case of frequency count tables
                    out.write("<table>\n");
                    out.write("<tr><th height=\"11\">Added noise</th>");
                    out.write("<th height=\"11\">Number of cells</th>");
                    out.write("<th height=\"11\">Relative to non-empty cells</th>");
                    out.write("<th height=\"11\">Relative to all cells</th></tr>\n");
                    Integer[] TreeMapKeys = tableSet.getCKMStats().keySet().toArray(new Integer[tableSet.getCKMStats().size()]);
                    for (i=0;i<tableSet.getCKMStats().size();i++){
                        out.write("<tr><td align=\"Right\">"+ Integer.toString(TreeMapKeys[i]) + "</td>");
                        out.write("<td align=\"Right\">"+Long.toString(tableSet.getCKMStats().get(TreeMapKeys[i]))+"</td>");
                        out.write("<td align=\"Right\">"+String.format("%7.3f",100.0*tableSet.getCKMStats().get(TreeMapKeys[i])/(tableSet.numberOfCells()-tableSet.numberOfEmpty()))+"%</td>");
                        out.write("<td align=\"Right\">"+String.format("%7.3f",100.0*tableSet.getCKMStats().get(TreeMapKeys[i])/tableSet.numberOfCells())+"%</td></tr>\n");
                    }
                    out.write("<tr><td align=\"Right\"> Empty </td>");
                    out.write("<td align=\"Right\">"+Long.toString(tableSet.numberOfEmpty())+"</td>");
                    out.write("<td align=\"Right\"> --</td>");
                    out.write("<td align=\"Right\">"+String.format("%7.3f",100.0*tableSet.numberOfEmpty()/tableSet.numberOfCells())+"%</td></tr>\n");

                    out.write("<tr><td align=\"Right\"> Non-Empty </td>");
                    out.write("<td align=\"Right\">"+Long.toString(tableSet.numberOfCells()-tableSet.numberOfEmpty())+"</td>");
                    out.write("<td align=\"Right\">"+Long.toString(100)+"%</td>");
                    out.write("<td align=\"Right\">"+String.format("%7.3f",100.0*(1.0 - 1.0*tableSet.numberOfEmpty()/tableSet.numberOfCells()))+"%</td></tr>\n");

                    out.write("<tr><td align=\"Right\"> Total </td>");
                    out.write("<td align=\"Right\">"+Long.toString(tableSet.numberOfCells())+"</td>");
                    out.write("<td align=\"Right\"> --</td>");
                    out.write("<td align=\"Right\">"+Long.toString(100)+"%</td></tr>\n");
                    out.write("</table><br>\n");

                    out.write("<table>\n");
                    out.write("<tr><td><b>False non-zero-cells</b> (zero changed to non-zero)</td>");
                    out.write("<td>"+tableSet.GetCKMInfoLoss().GetFalseNonzeros()+"</td></tr>\n");
                    out.write("<tr><td><b>False zero-cells</b> (non-zero changed to zero)</td>");
                    out.write("<td>"+tableSet.GetCKMInfoLoss().GetFalseZeros()+"</td></tr>\n");
                    out.write("</table>\n");
                    out.write("<p>\n");
                    // Print InformationLoss calculated over NON_EMPTY cells: UseEmpty = false
                    WriteAdditionalInfoLoss(out,tableSet.GetCKMInfoLoss(),reportfile,false);
                    // Print InformationLoss calculated over ALL cells: UseEmpty = true
                    WriteAdditionalInfoLoss(out,tableSet.GetCKMInfoLoss(),reportfile,true);
                }
                else out.write("<h3>To be implemented</h3>\n");
            }
           
            if (tableSet.historyUsed>0) {
                BufferedReader outApriori, outStatus, outCost, outProtL;           
                outApriori = new BufferedReader(new FileReader(Application.getTempFile("Apriori" + tableSet.index + ".htm")));  
                outStatus  = new BufferedReader(new FileReader(Application.getTempFile("HistStat" + tableSet.index + ".htm")));
                outCost    = new BufferedReader(new FileReader(Application.getTempFile("HistCost" + tableSet.index + ".htm")));
                outProtL   = new BufferedReader(new FileReader(Application.getTempFile("HistPL" + tableSet.index + ".htm")));
                //outBound   = new BufferedReader(new FileReader(Application.getTempFile("HistAB" + tableSet.index + ".htm"))); Not Used ?????
                BufferedWriter outDetail;
                reportfile = reportfile.substring(0, reportfile.length()-5)+"_apriori.html";
                outDetail  = new BufferedWriter(new FileWriter(reportfile)); 
                writeKopHtml(tableSet, outDetail, true);
                outDetail.write("<h1>&tau;-ARGUS Apriory file Report </h1><p>");outDetail.newLine();
                int p; boolean readFurther;
                String okeString="", falseString="";
                out.write("<p>\n");
                for(i=0;i<tableSet.historyUsed;i++){
                    hs = outApriori.readLine();
                    out.write("<h2>Summary of the apriory information file: "+(i+1)+"</h2>\n");
                    out.write("<h3>"+hs+"</h3>\n");
                    hs = outApriori.readLine();
                    out.write("<h3>"+hs+"</h3>\n");
                    out.write("<table>\n");
                    out.write("<tr><th width=\"50%\"height=\"11\">&nbsp;</th>\n");
                    out.write("<th width=\"25%\" height=\"11\">Correct</th>\n");
                    out.write("<th width=\"25%\" height=\"11\">Incorrect</th></tr>\n");
                    hs = outApriori.readLine();
                    for(int j=0;j<=4;j++){
                        if (j!=3){  
                            out.write("<tr><td>");
                            out.write(APriori.getStatus(j));
                            out.write("</td><td  align=\"Right\">");
                            p = hs.indexOf(";");
                            if (j==0){okeString = hs.substring(0,p) ;}
                            out.write(hs.substring(0,p));
                            hs = hs.substring(p+1);
                            out.write("</td><td align=\"Right\">");
                            p = hs.indexOf(";");
                            if (j==0){falseString = hs.substring(0,p) ;}
                            out.write(hs.substring(0,p));
                            hs = hs.substring(p+1);
                            out.write("</td></tr>\n");             
                        }   
                        else{
                            p = hs.indexOf(";");
                            hs = hs.substring(p+1);
                            p = hs.indexOf(";");
                            hs = hs.substring(p+1);
                        }
                    }
                    out.write("</table>\n");
                    hs = outStatus.readLine();
                    outDetail.write(hs); outDetail.newLine();
                    outDetail.write("<h2>Number of lines processed</h2>");outDetail.newLine();
                    outDetail.write("<table>");outDetail.newLine();
                    outDetail.write("<tr><td>Feasible lines</td><td align=\"Right\">"+okeString+"</td></tr>");outDetail.newLine();
                    outDetail.write("<tr><td>Infeasible lines</td><td align=\"Right\">"+falseString+ "</td></tr>");outDetail.newLine();
                    outDetail.write("</table>");outDetail.newLine();
                    readFurther = true;
                    while (hs != null && readFurther){
                        hs = outStatus.readLine();
                        if (hs != null){
                            if (hs.length()>6){if (hs.substring(0, 5).equals("<EOF>")){readFurther = false;}}
                            if (readFurther){ outDetail.write(hs); outDetail.newLine();}
                        }   
                    }

                    hs = outCost.readLine();
                    outDetail.write(hs); outDetail.newLine();
                    readFurther = true;
                    while (hs != null && readFurther){
                        hs = outCost.readLine();
                        if (hs != null){
                            if (hs.length()>6){if (hs.substring(0, 5).equals("<EOF>")){readFurther = false;}}
                            if (readFurther){ outDetail.write(hs); outDetail.newLine();}
                        }
                    }

                    hs = outProtL.readLine();
                    outDetail.write(hs); outDetail.newLine();
                    readFurther = true;
                    while (hs != null && readFurther){
                        hs = outProtL.readLine();
                        if (hs != null){
                            if (hs.length()>6){if (hs.substring(0, 5).equals("<EOF>")){readFurther = false;}}
                            if (readFurther){ outDetail.write(hs); outDetail.newLine();}
                        }
                    }
                    outDetail.write("<br><br>"); outDetail.newLine();
                }
                out.write("</p>\n");          
                out.write("For more details click<a href=\"file:///"+reportfile+"\"> here</a>"); out.newLine(); 
                outApriori.close();
                outDetail.write("<br><a HREF=\"javascript:history.go(-1)\">back</a>"); outDetail.newLine();
                outDetail.write("</body>"); outDetail.newLine();
                outDetail.write("</html>"); outDetail.newLine();
                outDetail.close();         
            } 

            for (i=1;i<=tableSet.expVar.size();i++){
                int ind = tableSet.expVar.get(i-1).index; int n = 1;
                final String dots = " ..........";
                if(tableSet.expVar.get(i-1).hierarchical!= Variable.HIER_NONE){
                    out.write("<h2>Coding tree for variable " +tableSet.expVar.get(i-1).name + "</h2>\n");
                    if (tableSet.expVar.get(i-1).recoded){out.write("A recoding has been applied<br>"); out.newLine();}
                    out.write("<table>");out.newLine();
                    out.write("<tr><th>Codelist</th></tr>");out.newLine();
                    int[] level = new int[1];
                    while( n < TauArgusUtils.getNumberOfActiveCodes(ind)  ) {
                        hs = TauArgusUtils.getCodeLevel(ind, n, level);
                        out.write("<tr><td>"+dots.substring(0,level[0])+hs+"</td></tr>"); out.newLine();
                        n++;
                    }
                    out.write("</table>"); out.newLine();                 
                } else {
                    out.write("<h2>Codes for variable " + tableSet.expVar.get(i-1).name+ "</h2>\n");
                    if (tableSet.expVar.get(i-1).recoded){
                        out.write("A recoding has been applied<br>"); out.newLine();
                    } 
                    out.write("<table>");out.newLine();
                    out.write("<tr><th>Codelist</th></tr>");out.newLine();
                    while( n < TauArgusUtils.getNumberOfActiveCodes(ind)  ) {
                        hs = TauArgusUtils.getCode(ind, n);
                        out.write("<tr><td>"+hs+"</td></tr>"); out.newLine();
                        n++;
                    }
                    out.write("</table>"); out.newLine();
                }
            }

            writeStaartHTML(out);
            out.close();
        }catch (Exception ex) { JOptionPane.showMessageDialog(null, ex.getMessage());}
    }

    static void printTableInfo(TableSet tableSet, BufferedWriter out){
        int j;
        try{
            out.write("<table>\n");
            out.write("<tr><th width=\"40%\" height=\"11\">Type</th>\n");
            out.write("<th width=\"40%\" height=\"11\">Var</th>\n");
            out.write("<th width=\"20%\" height=\"11\"># codes</th></tr>\n");

            out.write("<tr><td>Response var:</td>\n");
            if ("<freq>".equals(tableSet.respVar.name)){
                out.write("<td>&lt;freq&gt;</td><td>&nbsp;</td></tr>\n"); 
            }
            else{
                out.write("<td>" +tableSet.respVar.name + "</td><td>&nbsp;</td></tr>\n");
            }
            int n = tableSet.expVar.size();
            for (int i = 0; i < n; i++){
                out.write("<tr><td>Explanatory var" + (i+1) + ":</td>\n");
                out.write("<td>" + tableSet.expVar.get(i).name + "</td>\n");
                j = tableSet.expVar.get(i).index;
                out.write("<td align=\"Right\">" + tauargus.utils.TauArgusUtils.getNumberOfActiveCodes(j) + 
                          "</td></tr>\n");                  
            }
            if (!(tableSet.shadowVar == null))
                out.write("<tr><td>Shadow variable :</td><td>" + tableSet.shadowVar.name + "</td><td>&nbsp;</td></tr>\n");
            if (!(tableSet.costVar == null))
                out.write("<tr><td>Shadow variable :</td><td>" + tableSet.costVar.name + "</td><td>&nbsp;</td></tr>\n");
            if (tableSet.holding) out.write("Used holding info<br>\n");
            out.write("</table>\n");
        }catch (Exception ex) { }
    }

    private static String prDOM(int i, TableSet tableSet){
        String hs;
        hs="";
        if (tableSet.domK[i] != 0) {
            hs = "Dominance rule ";  
            if (i<2) {hs = hs + "(Individual level)";} 
            else {hs = hs + "(Holding level)";}
            hs = hs + " with n = " + tableSet.domN[i]+
                       " and k = " + tableSet.domK[i] + "%<br>\n";      
        }
        return hs;
    }

    private static String prPQ(int i, TableSet tableSet){
        String hs;
        hs="";
        if (tableSet.pqP[i] != 0) {
            hs = "p% rule ";  
            if (i<2) {hs = hs + "(Individual level)";} 
            else {hs = hs + "(Holding level)";}
            hs = hs + " with p = " + tableSet.pqP[i]+"%";
            if (tableSet.pqQ[i] !=100 || Application.isAnco() ) {hs = hs + ", q = " + tableSet.pqQ[i] + "%";}
            hs = hs + " and n = " + tableSet.pqN[i] + "<br>\n";      
        }
        return hs;
    }

    private static void WriteAdditionalInfoLoss(BufferedWriter out, CKMInfoLoss InfoLoss, String report, boolean UseEmpty){
        String hs;
        String AD, RAD, DR;
        int i, counts, totalcount;
        if (UseEmpty){ // All cells
            AD = "AD";
            RAD= "RAD";
            DR = "DR";
        }
        else{ // Non-empty cells
            AD = "ADnonempty";
            RAD = "RADnonempty";
            DR = "DRnonempty";
        }
        try{
            if (UseEmpty) out.write("<h2><b>Additional information loss measures, calculated over all cells</b></h2>\n");
            else out.write("<h2><b>Additional information loss measures, calculated over non-empty cells</b></h2>\n");
               
            out.write("<table>\n");
            out.write("<tr><th align=\"Left\">AD</th><td>Absolute Difference</td><td>|pert - orig|</td></tr>\n");
            out.write("<tr><th align=\"Left\">RAD</th><td>Relative Absolute Difference</td><td>|pert - orig|/orig</td></tr>\n");
            out.write("<tr><th align=\"Left\">DR</th><td>Absolute Difference of square Roots</td><td>|sqrt(pert) - sqrt(orig)|</td></tr>\n");
            out.write("</table>\n");
            out.write("<p>\n");
                
            out.write("<b>Descriptives</b><br>\n");
            out.write("<table>\n");
            out.write("<tr><td></td><th>AD</th><th>RAD</th><th>DR</th></tr>\n");
            out.write("<tr><td>Mean</td>\n");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMean(AD))+"</td>");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMean(RAD))+"</td>");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMean(DR))+"</td></tr>\n");
            out.write("<tr><td>Median</td>\n");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMedian(AD))+"</td>");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMedian(RAD))+"</td>");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMedian(DR))+"</td></tr>\n");
            out.write("<tr><td>Max</td>\n");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMaxs(AD))+"</td>");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMaxs(RAD))+"</td>");
            out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetMaxs(DR))+"</td></tr>\n");
            out.write("</table><br>\n");
                
            out.write("<b>Percentiles</b><br>\n");
            final String[] Percents = {"P10","P20","P30","P40","P50","P60","P70","P80","P90","P95","P99"};
            out.write("<table>\n");
            out.write("<tr><td></td><th>AD</th><th>RAD</th><th>DR</th></tr>\n");
            for (i=0;i<Percents.length;i++){
                out.write("<tr><td>"+Percents[i]+"</td>");
                out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetPercentiles(AD)[i])+"</td>");
                out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetPercentiles(RAD)[i])+"</td>");
                out.write("<td align=\"Right\">"+String.format("%7.5f",InfoLoss.GetPercentiles(DR)[i])+"</td></tr>\n");
            }
            out.write("</table>\n");
            out.write("<p>\n");
                
            hs = report.substring(0, report.length()-5);
            double step = UseEmpty ? 1.0/InfoLoss.GetNumberOfCells() : 1.0/(InfoLoss.GetNumberOfCells() - InfoLoss.GetNumberOfEmpty());
                
            ECDFGraphBuilder builder = new ECDFGraphBuilder();
            ChartPanel cp = UseEmpty ? builder.CreateChart("AD", InfoLoss.GetDiffs("AD"), step) :
                                    builder.CreateChart("AD", Arrays.copyOfRange(InfoLoss.GetDiffs("AD"),InfoLoss.GetNumberOfEmpty(),InfoLoss.GetDiffs("AD").length), step);
            ChartUtilities.saveChartAsPNG(new File(hs+"_ECDF_"+AD+".png"), cp.getChart(), 600, 300);
                
            cp = UseEmpty ? builder.CreateChart("RAD", InfoLoss.GetDiffs("RAD"), step) :
                                    builder.CreateChart("RAD", Arrays.copyOfRange(InfoLoss.GetDiffs("RAD"),InfoLoss.GetNumberOfEmpty(),InfoLoss.GetDiffs("AD").length), step);
            ChartUtilities.saveChartAsPNG(new File(hs+"_ECDF_"+RAD+".png"),cp.getChart(), 600, 300);
                
            cp = UseEmpty ? builder.CreateChart("DR", InfoLoss.GetDiffs("DR"), step) :
                                    builder.CreateChart("DR", Arrays.copyOfRange(InfoLoss.GetDiffs("DR"),InfoLoss.GetNumberOfEmpty(),InfoLoss.GetDiffs("AD").length), step);
            ChartUtilities.saveChartAsPNG(new File(hs+"_ECDF_"+DR+".png"),cp.getChart(), 600, 300);
                
            hs = hs.substring(hs.lastIndexOf("\\")+1,hs.length());
                
            out.write("<b>Empirical Cumulative Distribution Functions (ECDF)</b><br>\n");
            out.write("Number and fraction of cells with Difference less than or equal to Value<br>\n");
            out.write("<div>\n<div style=\"float:left\">\n");
            out.write("<table>\n");
            out.write("<tr><th colspan=\"3\" algn=\"Left\">AD</th></tr>\n");
            out.write("<tr><td>Value</td><td>Count</td><td>Fraction (%)</td></tr>\n");

            totalcount = UseEmpty ? InfoLoss.GetNumberOfCells() : InfoLoss.GetNumberOfCells() - InfoLoss.GetNumberOfEmpty();
                
            for (i=0;i<InfoLoss.GetECDFcounts("AD").getBreaks().length;i++){
                out.write("<tr><td align=\"Right\">"+String.format("%.2f",InfoLoss.GetECDFcounts("AD").getBreaks()[i])+"</td>");
                counts = UseEmpty ? InfoLoss.GetECDFcounts("AD").getCounts()[i] : InfoLoss.GetECDFcounts("AD").getCounts()[i] - InfoLoss.GetNumberOfEmpty();
                out.write("<td align=\"Right\">"+String.format("%d", counts) + "</td>");
                out.write("<td align=\"Right\">"+String.format("%.2f",100.0*((double)counts/(double)totalcount))+"</td></tr>\n");
            }
            out.write("<tr><td align=\"Right\">Inf</td>");
            out.write("<td align=\"Right\">"+String.format("%d",totalcount)+"</td>");
            out.write("<td align=\"Right\">"+String.format("%.2f",100.0)+"</td></tr>\n");
            out.write("</table>\n");
            out.write("</div>\n");
            out.write("<div>\n");
            out.write("<img border=\"0\" src=\""+hs+"_ECDF_"+AD+".png\" width=\"600\" height=\"300\"/>\n");
            out.write("</div></div><br><br>\n");
                
            out.write("<div>\n<div style=\"float:left\">\n");                
            out.write("<table>\n");
            out.write("<tr><th colspan=\"3\" algn=\"Left\">RAD</th></tr>\n");
            out.write("<tr><td>Value</td><td>Count</td><td>Fraction (%)</td></tr>\n");
            for (i=0;i<InfoLoss.GetECDFcounts("RAD").getBreaks().length;i++){
                out.write("<tr><td align=\"Right\">"+String.format("%.2f",InfoLoss.GetECDFcounts("RAD").getBreaks()[i])+"</td>");
                counts = UseEmpty ? InfoLoss.GetECDFcounts("RAD").getCounts()[i] : InfoLoss.GetECDFcounts("RAD").getCounts()[i] - InfoLoss.GetNumberOfEmpty();
                out.write("<td align=\"Right\">"+String.format("%d",counts)+"</td>");
                out.write("<td align=\"Right\">"+String.format("%.2f",100.0*((double)counts/(double)totalcount))+"</td></tr>\n");
            }
            out.write("<tr><td align=\"Right\">Inf</td>");
            out.write("<td align=\"Right\">"+String.format("%d",totalcount)+"</td>");
            out.write("<td align=\"Right\">"+String.format("%.2f",100.0)+"</td></tr>\n");
            out.write("</table>\n");
            out.write("</div>\n");
            out.write("<div>\n");
            out.write("<img border=\"0\" src=\""+hs+"_ECDF_"+RAD+".png\" width=\"600\" height=\"300\"/>\n");
            out.write("</div></div><br><br>\n");
                
            out.write("<div>\n<div style=\"float:left\">\n");                
            out.write("<table>\n");
            out.write("<tr><th colspan=\"3\" algn=\"Left\">DR</th></tr>\n");
            out.write("<tr><td>Value</td><td>Count</td><td>Fraction (%)</td></tr>\n");
            for (i=0;i<InfoLoss.GetECDFcounts("DR").getBreaks().length;i++){
                out.write("<tr><td align=\"Right\">"+String.format("%.2f",InfoLoss.GetECDFcounts("DR").getBreaks()[i])+"</td>");
                counts = UseEmpty ? InfoLoss.GetECDFcounts("DR").getCounts()[i] : InfoLoss.GetECDFcounts("DR").getCounts()[i] - InfoLoss.GetNumberOfEmpty();
                out.write("<td align=\"Right\">"+String.format("%d",counts)+"</td>");
                out.write("<td align=\"Right\">"+String.format("%.2f",100.0*((double)counts/(double)totalcount))+"</td></tr>\n");
            }
            out.write("<tr><td align=\"Right\">Inf</td>");
            out.write("<td align=\"Right\">"+String.format("%d",totalcount)+"</td>");
            out.write("<td align=\"Right\">"+String.format("%.2f",100.0)+"</td></tr>\n");                
            out.write("</table>\n");
            out.write("</div>\n");
            out.write("<div>\n");
            out.write("<img border=\"0\" src=\""+hs+"_ECDF_"+DR+".png\" width=\"600\" height=\"300\"/>\n");
            out.write("</div></div><br><br>\n");
            out.write("<p>\n");
        }
        catch(Exception ex){}
    }
}