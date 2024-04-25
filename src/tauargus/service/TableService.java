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

package tauargus.service;

import argus.utils.SystemUtils;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import tauargus.extern.dataengine.IProgressListener;
import tauargus.extern.dataengine.TauArgus;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.Metadata;
import tauargus.model.SpssUtilsTau;
import tauargus.model.TableSet;
import tauargus.model.Type;
import tauargus.model.Variable;
import tauargus.model.batch;
import tauargus.utils.TauArgusUtils;

public class TableService {
    
    private static final Logger LOGGER = Logger.getLogger(TableService.class.getName());
    
    private static final TauArgus TAUARGUS = Application.getTauArgusDll();

// anco 1.6
//    static ArrayList<TableSet> tables = new ArrayList<>();
    static ArrayList<TableSet> tables = new ArrayList<>();

    public static int numberOfTables() {
        return tables.size();
    }

    public static TableSet getTable(int tableIndex) {
        return tables.get(tableIndex);
    }

    public static String getTableDescription (TableSet tableSet){
        String hs; int i; 
        hs = tableSet.expVar.get(0).name;
        for (i=1;i<tableSet.expVar.size();i++){
            hs = hs + " x " +tableSet.expVar.get(i).name;}
        hs = hs + " | "+ tableSet.respVar.name;
        return hs;
    }
    public static void addTable(TableSet table) {

        table.index = tables.size();
        tables.add(table);
    }

    public static TableSet removeTable(int tableIndex) {
        TableSet table = tables.get(tableIndex);
        tables.remove(tableIndex);
        for (int i = tableIndex; i < tables.size(); i++) {
            tables.get(i).index--;
        }
        return table;
    }

    public static void clearTables() {
        tables.clear();
    }
    
    public static void undoSuppress(int index){
        TableSet tableSet = getTable(index);   
        TAUARGUS.UndoSecondarySuppress(tableSet.index, 1);
        tableSet.suppressed = TableSet.SUP_NO;
        tableSet.solverUsed = Application.SOLVER_NO; 
        tableSet.ghMiterMessage = "";
        tableSet.linkSuppressed = false;
        tableSet.nSecond = 0;
        tableSet.rounded = false;
        tableSet.ctaProtect = false;
        tableSet.ckmProtect = false;
        tableSet.roundTime = 0;
        tableSet.processingTime = 0;
        tableSet.hasBeenAudited = false;
        tableSet.safeFileName = "";
        tableSet.suppressINFO = "";
        tableSet.inverseWeight = false;
    }
    
    public static boolean isCopy(TableSet tableSet) {
        int n = tables.size();
        for (int i = 0; i < n; i++) {
            if (tableSet.equals(tables.get(i))) {
                return true;
            }
        }
        return false;
    }

    // Never used ?????
    /*boolean hasLinkedTables() {
        int n = numberOfTables();
        if (n <= 1) {
            return false;
        }
        for (int i = 1; i < n; i++) {
            if (getTable(i).respVar != getTable(0).respVar) {
                return false;
            }
        }
        int[][] link = new int[n][n];
        int[][] linkN = new int[n][n];
        int[][] linkT = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 1; j < n; j++) {
                int t = 0;
                for (int ii = 0; ii < getTable(i).expVar.size(); ii++) {
                    for (int jj = 0; jj < getTable(j).expVar.size(); jj++) {
                        if (getTable(i).expVar.get(ii) == getTable(j).expVar.get(jj)) {
                            t = 1;
                        }
                    }
                }
                link[i][j] = t;
                linkT[i][j] = t;
            }
        }
        for (int i = 0; i < n; i++) {
            for (int ii = 0; ii < n; ii++) {
                System.arraycopy(linkT[ii], 0, linkN[ii], 0, n);
            }
            for (int ii = 0; ii < n; ii++) {
                for (int jj = 0; jj < n; jj++) {
                    int S = 0;
                    for (int j = 0; j < n; j++) {
                        S = S + link[ii][j] * linkN[j][jj];
                    }
                    linkT[ii][jj] = S;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (link[i][j] == 0) {
                    return false;
                }
            }
        }
        return true;
    }*/

    public static void readMicrodata(final PropertyChangeListener propertyChangeListener) throws ArgusException {
        final PropertyChangeSupport pcs = new PropertyChangeSupport(TableService.class);
        pcs.addPropertyChangeListener(propertyChangeListener);
        pcs.firePropertyChange("activityMain", null, "Start reading microdata...");

        Metadata metadata = Application.getMetadata(0);

        Application.setVariables();
        for (int i = 1; i < TableService.numberOfTables(); i++) {
            TableSet tableSet1 = TableService.getTable(i);
            for (int j = 0; j < i; j++) {
                TableSet tableSet2 = TableService.getTable(j);
                for (int k = 0; k < tableSet1.expVar.size(); k++) {
                    for (int l = 0; l < tableSet2.expVar.size(); l++) {
                        if (tableSet1.expVar.get(k) == tableSet2.expVar.get(l)) {
                            try {
                                Variable variable = (Variable) tableSet1.expVar.get(k).clone();
                                Application.addVariable(variable);
                                tableSet1.expVar.set(k, variable);
                            } catch (CloneNotSupportedException ex) {
                                LOGGER.log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < TableService.numberOfTables(); i++) {
            TableSet tableSet = TableService.getTable(i);
            StringBuilder s = new StringBuilder();
            s.append("Table ").append(i).append(": ").append(tableSet.toString());
            LOGGER.info(s.toString());
        }

        for (int i = 0; i < Application.numberOfVariables(); i++) {
            Variable variable = Application.getVariable(i);
            variable.inTable = false;
            variable.recoded = false;
        }

        for (int i = 0; i < TableService.numberOfTables(); i++) {
            TableSet tableSet = TableService.getTable(i);
            tableSet.hasRealFreq = true;
            tableSet.suppressed = TableSet.SUP_NO;
            tableSet.ckmProtect = false;
            tableSet.undoAudit();
            tableSet.nSecond = 0;
            tableSet.processingTime = 0;
            tableSet.singletonUsed = false;
            tableSet.rounded = false;
            for (int j = 0; j < tableSet.expVar.size(); j++) {
                Variable variable = tableSet.expVar.get(j);
                variable.inTable = true;
            }
        }
        try{
            TAUARGUS.CleanAll();
        }
        catch(Exception ex){
            throw new ArgusException("Something wrong:\n"+ex.getMessage());
        }
        IProgressListener progressListener = new IProgressListener() {
            @Override
            public void UpdateProgress(final int percentage) {
                pcs.firePropertyChange("progressMain", null, percentage);
            }
        };
        TAUARGUS.SetProgressListener(progressListener);
        
        if (metadata.dataFileType == Metadata.DATA_FILE_TYPE_FREE) {
            TAUARGUS.SetInFileInfo(false, metadata.fieldSeparator);
        }
        else{ // Is this indeed in all other cases ????? PWOF 18-10-2016
            TAUARGUS.SetInFileInfo(true, metadata.fieldSeparator);
        }
            
        try{
            TauArgusUtils.setVariables();
        }
        catch(ArgusException ex){
            TableService.clearTables();
            throw new ArgusException(ex.getMessage());
        }
        pcs.firePropertyChange("activityMain", null, "Exploring datafile...");
        pcs.firePropertyChange("progressMain", null, 0);
        String s = metadata.dataFile;
        if (metadata.dataFileType == Metadata.DATA_FILE_TYPE_SPSS) {
            s = Application.getTempFile("SPSSTemp.dat");
            SpssUtilsTau.exportSpssData(metadata.dataFile);            
        }
        batch.reportProgress("Start explore file: "+ s);
        
        // using arrays of size 1 for output parameters
        int[] errorCode = new int[]{0};
        int[] errorLine = new int[]{0};
        int[] errorVarIndex = new int[]{0};
        
        if (!TAUARGUS.ExploreFile(s, errorCode, errorLine, errorVarIndex)) {
            s = "";
            if (errorVarIndex[0] >= 0 && errorVarIndex[0] < Application.numberOfVariables()) {
                s = "\nVariable: " + Application.getVariable(errorVarIndex[0]).name;
            }
            throw new ArgusException("ExploreFile went wrong in line" + errorLine[0] + "\n" + StringUtils.strip(TAUARGUS.GetErrorString(errorCode[0])) + s);
        }
        if (!TAUARGUS.SetNumberTab(TableService.numberOfTables())) {
            throw new ArgusException("SetNumberTab went wrong");
        }
        for (int i = 0; i < TableService.numberOfTables(); i++) {
            TableSet table = TableService.getTable(i);
            boolean isFreq = table.isFrequencyTable();
            int ish = table.indexOfShadowVariable();
            if (isFreq && ish <= 0) { // ish == 0 in case of FreqVar without ShadowVar
                ish = Application.numberOfVariables();
            }
            int[] expVar = table.indicesOfExplanatoryVariables();
            int requestVariableIndex = metadata.indexOf(Type.REQUEST);
            if (!TAUARGUS.SetTable(i, expVar.length, expVar, isFreq, table.indexOfResponseVariable(), ish, table.indexOfCostVariable(), 
                                        table.indexOfCellKeyVariable(), table.respVar.CKMType, table.respVar.CKMTopK,
                                        table.lambda, table.maxScaleCost, requestVariableIndex, table.missingIsSafe)) {
                throw new ArgusException("SetTable went wrong for table" + (i + 1));
            }
            boolean isPiep = table.piepRule[0] || table.piepRule[1];
            int[] frequencyMarge = new int[TableSet.MAX_FREQ_PAR_SETS];
            int[] minFreq = new int[TableSet.MAX_FREQ_PAR_SETS];
            for (int j = 0; j < TableSet.MAX_FREQ_PAR_SETS; j++) {
                frequencyMarge[j] = table.frequencyMarge[j];
                minFreq[j] = table.minFreq[j];
                if (minFreq[j] == 0) {
                    minFreq[j] = 1;
                }
            }
            if (frequencyMarge[0] < frequencyMarge[1] && table.holding && table.minFreq[1] > 0) {
                frequencyMarge[0] = frequencyMarge[1];
            }
            if (!TAUARGUS.SetTableSafety(i, table.domRule, table.domN, table.domK, table.pqRule, table.pqP, table.pqQ, table.pqN, 
                                         minFreq, table.piepPercentage, table.piepMarge, table.piepMinFreq, isPiep, table.weighted, 
                                         table.weighted, table.holding, table.zeroUnsafe, false, 10, table.zeroRange, table.manualMarge, 
                                         frequencyMarge)) {
                throw new ArgusException("SetTableSafety went wrong for table" + (i + 1));
            }
        }
        pcs.firePropertyChange("progressMain", null, 100);
        pcs.firePropertyChange("activityMain", null, "Computing tables...");
        pcs.firePropertyChange("progressMain", null, 0);
        batch.reportProgress("Start computing tables");
        int[] tableIndex = new int[]{0};
        for (int i = 0; i < TableService.numberOfTables(); i++) {
            tableIndex[0] = i;
            if (!TAUARGUS.ComputeTables(errorCode, tableIndex)) {
                throw new ArgusException("Error occurred when computing table" + (tableIndex[0] + 1) + TAUARGUS.GetErrorString(errorCode[0]));
            }
            else{
                double[] xMaxTemp = new double[]{0.0};
                double xMin = TAUARGUS.GetMinimumCellValue(i, xMaxTemp);
                double xMax = xMaxTemp[0];
                if (xMax > 0){
                    xMax = 1.5 * xMax;}
                else {xMax = 0;}
                if (xMin > 0) {
                    xMin = 0;
                }
                if (xMin < 0) {
                    xMin = 1.5 * xMin;
                }
                TableSet tableSet = TableService.getTable(i);
                tableSet.maxTabVal = xMax;
                tableSet.minTabVal = xMin;
                tableSet.isAdditive = true;
                tableSet.clearHistory();
                
                SystemUtils.writeLogbook("Table: "+TableService.getTable(i).toString()  +" has been specified");
            }
        } 
        pcs.firePropertyChange("progressMain", null, 100);
        LOGGER.info("Compute tables completed");
        SystemUtils.writeLogbook("Tables have been computed");        
    }
    
  
    public static void addAdditivityParamBatch( int additParam, boolean keepStatus){
            for (TableSet table : tables) {
                table.additivity = additParam;
                table.keepStatus = keepStatus;
            }
    }
    
    public static void setUseStatusOnly(boolean useStatusOnly){
        for (TableSet table : tables){
            table.useStatusOnly = useStatusOnly;
        }
    }
    
    public static void readTables(PropertyChangeListener propertyChangeListener) throws ArgusException, FileNotFoundException, IOException {
        PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(TableService.class);
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
        
        for (TableSet table : tables) {
            Metadata metadata = table.metadata;
            table.clearHistory();
            metadata.setVariableLengthFromData(propertyChangeSupport);
            for (Variable variable : metadata.variables) {
                if (variable.isCategorical()) {
                    // TODO WAT MOET IK HIERMEE??????????????
                    if (variable.missing[0].equals("X")) {
                        variable.missing[0] = StringUtils.repeat("X", variable.varLen);
                    }
                }
            }
        }

        Application.setVariables();

        try {
            TAUARGUS.CleanAll();
            TauArgusUtils.setVariables();
            TAUARGUS.ThroughTable();
            TAUARGUS.SetNumberTab(tables.size());
            for (TableSet table : tables) {
                table.read(propertyChangeSupport);
            }
        }
        catch (IOException | ArgusException ex) {
            TAUARGUS.CleanAll();
            throw ex;
        }
   }
    
    public static String IndexToCodesString (TableSet tableSet, int c){
        String hs; int i, j, i1;
        int[] nc = new int[10]; int[] c1 = new int[10];
        int[] something = new int[1]; int[] n = new int[1];
        int[] ct = new int[1]; int[] isMissing = new int[1]; int[] level = new int[1];
        String[] codeString = new String[1];
        int ne = tableSet.expVar.size();
        hs = "";
        i1 = c;
        for (i=0;i<ne-1;i++){
          j = tableSet.expVar.get(i+1).index;                    
          TAUARGUS.GetVarNumberOfCodes(j, n, something);
          nc[i]=n[0];
        }
        nc[ne-1]=1;
        for (i=ne-3;i>=0;i--){nc[i] = nc[i] * nc[i+1];}
        for (i=0;i<ne;i++){c1[i] = (int) (i1 / nc[i]);
           i1 = i1 - nc[i]* (c1[i]); 
        }
        for(i=0;i<ne;i++){
          j = tableSet.expVar.get(i).index;                    
          TAUARGUS.GetVarCode(j, c1[i], ct, codeString, isMissing, level);
          if (codeString[0].equals("")){codeString[0] = "Total";}
          hs = hs + codeString[0] + ",";
           //GetVarCode(int VarIndex, int CodeIndex, int[] CodeType, String[] CodeString, int[] IsMissing, int[] Level)
        }
        hs = hs.substring(0, hs.length()-1);
        return hs;
    }
    
//Function IndexToCodes(SelTab As Long, Ind As Long) As String
//Dim i As Long, Hs As String, NC(1 To 10) As Long, Oke As Boolean
//Dim Nx As Long, CI(1 To 10) As Long, ii As Long, ItC As String
//Dim CT As Long, IL As Long, Im As Long
//ItC = ""
//ii = Ind
//With TableSetStruct(SelTab)
// For i = 1 To .NExpVar - 1
//  If Not frmMain.ArgOCX.GetVarNumberOfCodes(.ExpVar(i + 1), NC(i), Nx) Then GoTo FOUT
// Next i
// NC(.NExpVar) = 1
// For i = .NExpVar - 2 To 1 Step -1
//  NC(i) = NC(i) * NC(i + 1)
// Next i
// For i = 1 To .NExpVar
//  CI(i) = ii \ NC(i)
//  ii = ii - NC(i) * CI(i)
// Next i
// For i = 1 To .NExpVar
//  If Not frmMain.ArgOCX.GetVarCode(.ExpVar(i), CI(i), CT, Hs, Im, IL) Then GoTo FOUT
//  If Hs = "" Then Hs = "Total"
//  If Len(Hs) < 5 Then Hs = Space(5 - Len(Hs)) + Hs
//  ItC = ItC + Hs + ";"
// Next i
// ItC = Left(ItC, Len(ItC) - 1)
//End With
//IndexToCodes = ItC
//Exit Function
//FOUT:
//IndexToCodes = "??????"
//End Function    
}
