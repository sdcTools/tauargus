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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Math.abs;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import tauargus.extern.dataengine.TauArgus;
import tauargus.gui.DialogErrorApriori;
import tauargus.service.TableService;
import tauargus.utils.TauArgusUtils;
import tauargus.utils.Tokenizer;

public class TableSet {

    private static final Logger LOGGER = Logger.getLogger(TableSet.class.getName());
    private static final double EPSILON = 1.0e-10;

    public static final int MAX_EXP_VAR = 10;
    public static final int MAX_RESP_VAR = 10;
    public static final int MAX_TOP_N_VAR = 10;
    public static final int MAX_GH_MITER_RATIO = 11;
    public static final int MAX_ROUND_SOL_TYPE = 3;
    public static final int MAX_RULE_PAR_SETS = 4;
    public static final int MAX_FREQ_PAR_SETS = 2;
    public static final int MAX_PIEPS = 2;

    public static final int COST_FREQ = -1;
    public static final int COST_UNITY = -2;
    public static final int COST_DIST = -3;
    public static final int COST_VAR = -4; // Not used in communication with dll
    public static final int COST_RESPONSE = -5; // Not used in communication with dll

    public static final int SUP_NO = 0;
    public static final int SUP_JJ_OPT = 1;
    public static final int SUP_GHMITER = 2;
    public static final int SUP_HITAS = 3;
    public static final int SUP_NETWORK = 4;
    public static final int SUP_SINGLETON = 5;
    public static final int SUP_ROUNDING = 6;
    public static final int SUP_MARGINAL = 7;
    public static final int SUP_UWE = 8;
    public static final int SUP_CTA = 9;
    public static final int SUP_CKM = 10;

    public static final int ADDITIVITY_CHECK = 0;
    public static final int ADDITIVITY_RECOMPUTE = 1;
    public static final int ADDITIVITY_NOT_REQUIRED = 2;
    
    public static final boolean KEEP_STATUS = false; // Default: do not keep status when recomputing additivity

    public static final int FILE_FORMAT_UNKNOWN = -1;
    public static final int FILE_FORMAT_CSV = 0;
    public static final int FILE_FORMAT_PIVOT_TABLE = 1;
    public static final int FILE_FORMAT_CODE_VALUE = 2;
    public static final int FILE_FORMAT_SBS = 3;
    public static final int FILE_FORMAT_INTERMEDIATE = 4;
    public static final int FILE_FORMAT_JJ = 5;
    public static final int FILE_FORMAT_CKM = 6;

    public static final String DATA_FILE_EXTENSION = ".tab";
    public static final String METADATA_FILE_EXTENSION = ".rda";

    static BufferedWriter outApriori, outStatus, outCost, outProtL,  outBound;
    
    private static final TauArgus tauArgus = Application.getTauArgusDll();
    public Metadata metadata;

    public int index;  //cannot be static!!!!!!!!!!!!

    // explanatory variables...
    public List<Variable> expVar = new ArrayList<>();
    // result variables...
    public Variable respVar;

    // shadow variable
    public Variable shadowVar;
    
    // variable to calculate cellkey
    public Variable cellkeyVar;

    // cost func/variable/parameters
    public int costFunc = COST_VAR;
    // Next fields only used if costFunc == COST_VAR
    public Variable costVar;
    public double lambda = 1;
    public double maxScaleCost = 20000;
    public boolean inverseWeight = false;

    // dom  rule...
    public boolean domRule = false;
    public int[] domN = new int[MAX_RULE_PAR_SETS];
    public int[] domK = new int[MAX_RULE_PAR_SETS];
    {
        for (int i = 0; i < MAX_RULE_PAR_SETS; i++) {
            domN[i] = 0;
            domK[i] = 0;
        }
    }
    // PQ rule...
    public boolean pqRule = false;
    public int[] pqP = new int[MAX_RULE_PAR_SETS];
    public int[] pqQ = new int[MAX_RULE_PAR_SETS];
    public int[] pqN = new int[MAX_RULE_PAR_SETS];
    {
        for (int i = 0; i < MAX_RULE_PAR_SETS; i++) {
            pqP[i] = 0;
            pqQ[i] = 100;
            pqN[i] = 0;
        }
    }
    // piep  rule...
    public boolean[] piepRule = new boolean[2]; // why not one boolean?
    public int[] piepPercentage = new int[MAX_RULE_PAR_SETS];
    public int[] piepMinFreq = new int[2];
    public int[] piepMarge = new int[2];
    {
        for (int i = 0; i < MAX_RULE_PAR_SETS; i++) {
            piepPercentage[i] = 0;
        }
        for (int i = 0; i < MAX_PIEPS; i++) {
            piepRule[i] = false;
            piepMinFreq[i] = 0;
            piepMarge[i] = 0;
        }
    }
    // minimum frequency rule...
    public boolean frequencyRule = false;
    public boolean emptyNonStructural = false;
    public int[] minFreq = new int[MAX_FREQ_PAR_SETS];
    public int[] frequencyMarge = new int[MAX_FREQ_PAR_SETS];
    {
        for (int i = 0; i < MAX_FREQ_PAR_SETS; i++) {
            minFreq[i] = 0;
            frequencyMarge[i] = 1;
        }
    }
    // zero unsafe...
    public boolean zeroUnsafe = false;
    public double zeroRange = 0;
    // other...
    public int manualMarge = 20;
    public boolean weighted = false;
    public boolean missingIsSafe = false;
    public boolean holding = false;
    public boolean CellKeyAvailable = false;
    ////////////////////////////////////////////
    public boolean singletonUsed = false;
    public int suppressed = SUP_NO;
    public int solverUsed = Application.SOLVER_NO;
    public String suppressINFO = "";
    public boolean readFreqOnlyTable = false;
    int historyUsed = 0;
    public boolean linkSuppressed = false;
    public int nPrim = 0;
    public int nSecond = 0;
    public String safeFileName = "";
    public int safeFileFormat = FILE_FORMAT_UNKNOWN;
    public String safeFileOptions = "";
    public double ratio = 0;
    public String ghMiterMessage = "";
    public int ghMiterSize = 0;
    public boolean ghMiterApplySingleton = true;
    public boolean ghMiterApriory = true;
    public int ghMiterAprioryPercentage = 100;
    public int ghMiterSubcode = 0;
    public int ghMiterSubtable = 0;
    public int maxHitasTime = 1;
    public int maxTimeOptimal = 10;
    public boolean singletonSingletonCheck = true;
    public boolean singletonMultipleCheck = true;
    public boolean minFreqCheck = true;
    public int processingTime = 0;
    public boolean ctaProtect = false;
    public boolean ckmProtect = false;
    public int networkSolverType = 1;
    public int networkPrimariesOrder = 1;
    public int networkMaxProtLevel = 20;
    // parametes for the rounder
    public boolean rounded = false;
    String roundedInfo = "";
    public int roundTime = 0;
    public int roundMaxTime = 1;
    public int roundJumps = 0;
    public double roundMaxJump = 0;
    public int roundBase = 0;
    public int roundMaxStep = 0;
    public int roundPartitions = 0;
    public int roundNumberofBlocks = 0;
    public int roundStoppingRule = 2;
    public boolean roundAddIntermediateTotals = false;
    public boolean roundUnitCost = false;

    int[] roundSolType = new int[MAX_ROUND_SOL_TYPE];
    {
        for (int i = 0; i < MAX_ROUND_SOL_TYPE; i++) {
            roundSolType[i] = 0;
        }
    }
    //int modularOptions = 0; //Never used???
    //boolean scalingUsed = false; //Never used???
    public double minTabVal = 0;
    public double maxTabVal;
    public int minDiff = 0;
    public int maxDiff = 0;
    public boolean computeTotals = false;
    public boolean keepStatus = KEEP_STATUS;
    public boolean useStatusOnly = false;
    public int additivity = ADDITIVITY_CHECK;
    //boolean negIsAbsolute = false; //Never used???
    public boolean hasBeenAudited = false;
    public int auditExactDisclosure = 0;
    public int auditPartialDisclosure = 0;
    public boolean isAdditive = false;
    public boolean hasRealFreq = false;
    int[] ghMiterRatio = new int[MAX_GH_MITER_RATIO];
    {
        for (int i = 0; i < MAX_GH_MITER_RATIO; i++) {
            ghMiterRatio[i] = 0;
        }
    }
    //int APriory = -1; //Never used???
    
    private final TreeMap<Integer,Long> CKMStatistics = new TreeMap<>(); // Can be final???
    private long nEmpty;
    
    private static String[][] codeList;
    private static int[][] codeListLevel;
    private static int[][] codeListNChild;
    
    // for Bogus level routines
    private static final int[] isParent = new int[1];
    private static final int[] isActive = new int[1];
    private static final int[] isMissing = new int[1];
    private static final int[] level = new int[1];
    private static final int[] nChild = new int[1];
    private static final String[] code = new String[1];
    
    private final CKMInfoLoss InfoLoss = new CKMInfoLoss();
    
    // To be able to set lower and uppermarg per table
    private double lowermarg = 0.99; //default
    private double uppermarg = 1.01; //default
    
    public void SetLowerMarg(double value){
        this.lowermarg = value;
    }
    
    public double GetLowerMarg(){
        return this.lowermarg;
    }

    public void SetUpperMarg(double value){
        this.uppermarg = value;
    }

    public double GetUpperMarg(){
        return this.uppermarg;
    }

    public TableSet(Metadata metadata) {
        this.metadata = metadata;
//        pqRule = false;
//        pqP[0] = 15;
//        pqQ[0] = 100;
//        pqN[0] = 1;
//        piepMarge[0] = 10;
//        piepMinFreq[0] = 3;
//        frequencyMarge[0] = 10;
//        zeroRange = 10;
//        manualMarge = 10;
// TODO Strange place to fill structure here. Problem in batch.
        if (metadata.dataOrigin == Metadata.DATA_ORIGIN_TABULAR && !Application.isBatch()) {
            for (Variable variable : metadata.variables) {
                if (variable.type == tauargus.model.Type.CATEGORICAL) {
                    expVar.add(variable);
                }
            }
            respVar = metadata.find(tauargus.model.Type.RESPONSE);
            if (respVar == null) {
                respVar = metadata.find(tauargus.model.Type.FREQUENCY);
                if (respVar != null) {
                    readFreqOnlyTable = true;
                }
            }

            shadowVar = metadata.find(tauargus.model.Type.SHADOW);
            if (shadowVar == null) {
                shadowVar = respVar;
            }
            
            costVar = metadata.find(tauargus.model.Type.COST);
            if (costVar == null) {
                costVar = respVar;
            }
        }else{ // find out if a record key is available in the data
            CellKeyAvailable = !(metadata.find(tauargus.model.Type.RECORD_KEY)==null);
        }
    }

    public long numberOfEmpty(){
        return nEmpty;
    }
    
    public void setNumberOfEmpty(long numberEmpty){
        nEmpty=numberEmpty;
    }
    
    public TreeMap<Integer, Long> getCKMStats(){
        return CKMStatistics;
    }
    
    public int indexOfResponseVariable() {
        return Application.indexOfVariable(respVar);
    }

    public boolean isFrequencyTable(){
        return respVar == Application.getFreqVar();
    }
    
    public int indexOfShadowVariable() {
        int iShadow = Application.indexOfVariable(shadowVar);
        if (iShadow == -1) {
            iShadow = indexOfResponseVariable();
        }
        return iShadow;
    }
    
    public int indexOfCellKeyVariable(){
        int iCellKey = Application.indexOfVariable(cellkeyVar);
        return iCellKey;
    }

    public int indexOfCostVariable() {
        int iCost;
        switch (costFunc) {
            case COST_RESPONSE:
                iCost = indexOfResponseVariable();
                break;
            case COST_VAR:
                iCost = Application.indexOfVariable(costVar);
                if (iCost == -1) {
                    if (respVar != Application.getFreqVar()) {
                        iCost = indexOfResponseVariable();}
                    else {
                        iCost = COST_FREQ ;
                    }
                }   break;
            default:
                iCost = costFunc;
                break;
        }
        return iCost;
    }
    
    public Cell getCell(int[] dimIndex) {
        // use arrays of length 1 for getting output values...
        double[] response = {0.0};
        double[] roundedResponse = {0.0};
        double[] CTAValue = {0.0};
        double[] CKMValue = {0.0};
        double[] shadow = {0.0};
        double[] cost = {0.0};
        double[] cellkey = {0.0};
        double[] cellkeynozeros = {0.0};
        int[] freq = {0};
        int[] status = {0};
        int[] holdingFreq = {0};
        double[] peepCell = {0.0};
        double[] peepHolding = {0.0};
        int[] peepSortCell = {0};
        int[] peepSortHolding = {0};
        double[] lower = {0.0};
        double[] upper = {0.0};
        double[] realizedLower = {0.0};
        double[] realizedUpper = {0.0};
        Cell cell = new Cell();
        if (!tauArgus.GetTableCell(index, dimIndex, response, roundedResponse, CTAValue, CKMValue, shadow, cost, cellkey, cellkeynozeros, freq, status, cell.maxScore, cell.maxScoreWeight, holdingFreq, cell.holdingMaxScore, cell.holdingNrPerMaxScore, peepCell, peepHolding, peepSortCell, peepSortHolding, lower, upper, realizedLower, realizedUpper)) {
            return null;
        }
        cell.response = response[0];
        cell.roundedResponse = roundedResponse[0];
        cell.CTAValue = CTAValue[0];
        cell.CKMValue = CKMValue[0];
        cell.shadow = shadow[0];
        cell.cost = cost[0];
        cell.cellkey = cellkey[0];
        cell.cellkeynozeros = cellkeynozeros[0];
        cell.freq = freq[0];
        cell.setStatusAndAuditByValue(status[0]);
        cell.holdingFreq = holdingFreq[0];
        cell.peepCell = peepCell[0];
        cell.peepHolding = peepHolding[0];
        cell.peepSortCell = peepSortCell[0];
        cell.peepSortHolding = peepSortHolding[0];
        cell.lower = lower[0];
        cell.upper = upper[0];
        cell.realizedLower = realizedLower[0];
        cell.realizedUpper = realizedUpper[0];
        return cell;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int j = 0; j < expVar.size(); j++) {
            Variable variable = expVar.get(j);
            s.append(variable.name);
            if (j != expVar.size() - 1) {
                s.append(" x ");
            }
        }
        s.append(" | ").append(respVar.name);
        return s.toString();
    }

    public int CountSecondaries(){
        CellStatusStatistics stat = getCellStatusStatistics();
        int nSec = stat.freq[CellStatus.SECONDARY_UNSAFE.getValue()] + stat.freq[CellStatus.SECONDARY_UNSAFE_MANUAL.getValue()];
        return nSec;
    }

    public CellStatusStatistics getCellStatusStatistics() {
        CellStatusStatistics statistics = new CellStatusStatistics();
        if (!tauArgus.GetCellStatusStatistics(index, statistics.freq, statistics.cellFreq, statistics.holdingFreq, statistics.cellResponse, statistics.cellCost)) {
            return null;
        }
        int totalIndex = CellStatus.size();
        statistics.freq[totalIndex] = 0;
        statistics.cellFreq[totalIndex] = 0;
        statistics.holdingFreq[totalIndex] = 0;
        statistics.cellResponse[totalIndex] = 0;
        statistics.cellCost[totalIndex] = 0;
        for (int i = CellStatus.size() - 1; i > 0; i--) {
            statistics.freq[i] = statistics.freq[i - 1];
            statistics.cellFreq[i] = statistics.cellFreq[i - 1];
            statistics.holdingFreq[i] = statistics.holdingFreq[i - 1];
            statistics.cellResponse[i] = statistics.cellResponse[i - 1];
            statistics.cellCost[i] = statistics.cellCost[i - 1];

            statistics.freq[totalIndex] += statistics.freq[i];
            statistics.cellFreq[totalIndex] += statistics.cellFreq[i];
            statistics.holdingFreq[totalIndex] += statistics.holdingFreq[i];
            statistics.cellResponse[totalIndex] += statistics.cellResponse[i];
            statistics.cellCost[totalIndex] += statistics.cellCost[i];
        }

        statistics.freq[0] = 0;
        statistics.cellFreq[0] = 0;
        statistics.holdingFreq[0] = 0;
        statistics.cellResponse[0] = 0;
        statistics.cellCost[0] = 0;
        return statistics;
    }

    public int numberOfTopNNeeded() {
        int topN = 0;
        for (int i = 0; i < 2; i++) {
            if (domRule) {
                topN = Math.max(topN, domN[i]);
            }
            if (pqRule) {
                if (pqN[i] != 0) {
                    topN = Math.max(topN, pqN[i] + 1);
                }
            }
        }
        return topN;
    }

    public int numberOfHoldingTopNNeeded() {
        int topN = 0;
        for (int i = 2; i < 4; i++) {
            if (domRule) {
                topN = Math.max(topN, domN[i]);
            }
            if (pqRule) {
                if (pqN[i] != 0) {
                    topN = Math.max(topN, pqN[i] + 1);
                }
            }
        }
        return topN;
    }


    public int[] indicesOfExplanatoryVariables() {
        int[] indices = new int[expVar.size()];
        int n = 0;
        for (int i = 0; i < expVar.size(); i++) {
            indices[n++] = expVar.get(i).index;
        }
        return indices;
    }

    public int numberOfCells(){
        int n=1;
        int[] nc = new int[1];
        int[] nac = new int[1];
        for (int i=0;i<expVar.size();i++){
            tauArgus.GetVarNumberOfCodes (expVar.get(i).index, nc, nac);
            n=n*nac[0];
        }
        return n;
    }

    private boolean buildCell(Tokenizer tokenizer, String[] codes, Cell cell) throws ArgusException {
        final double MYEPSILON = 0.000000001;
        boolean hasStatus = false; 
        String hs;

        int iExp = 0;
        int iTop = 0;
        cell.response = Cell.UNKNOWN;
        cell.shadow = Cell.UNKNOWN;
        cell.cost = Cell.UNKNOWN;
        cell.freq = Cell.UNKNOWN; 
        for (int i = 0; i < TableSet.MAX_TOP_N_VAR; i++) {
            cell.maxScore[i] = 0;
            cell.holdingMaxScore[i] = 0;
        }
        cell.lower = Cell.UNKNOWN;
        cell.upper = Cell.UNKNOWN;
        cell.status = CellStatus.SAFE;
        for (int j = 0; j < metadata.variables.size(); j++) {
            String value = tokenizer.nextField(metadata.fieldSeparator);
            Variable variable = metadata.variables.get(j);
            switch (variable.type) {
                case CATEGORICAL:
                    if (variable.isTotalCode2(value)) {
                        codes[iExp] = "";
                    } else {
                        codes[iExp] = variable.normaliseCode(value);
                    }
                    iExp++;
                    break;
                case RESPONSE:
                    if (value.equals("") || value.equals("-")){ cell.status = CellStatus.EMPTY;}
                    else{
                        try {cell.response = StrUtils.toDouble(value);} 
                        catch(argus.model.ArgusException ex){throw new ArgusException (ex.getMessage());}
                    }
                    break;
                case SHADOW:
                    if ((value.equals("") || value.equals("-"))){break;}
                    try {cell.shadow = StrUtils.toDouble(value);} 
                    catch(argus.model.ArgusException ex){throw new ArgusException (ex.getMessage());}
                    break;
                case COST:
                    if ((value.equals("") || value.equals("-"))){break;}
                    try{ cell.cost = StrUtils.toDouble(value);} 
                    catch(argus.model.ArgusException ex){throw new ArgusException (ex.getMessage());}
                    break;
                case FREQUENCY:
                    if (value.equals("") || value.equals("-")){ cell.status = CellStatus.EMPTY; break;}// cell.freq=0;}
                    try{cell.freq = StrUtils.toInteger(value);} 
                    catch(argus.model.ArgusException ex){throw new ArgusException (ex.getMessage());}
                    break;
                case TOP_N:
                    if ((value.equals("") || value.equals("-"))){iTop++;break;}
                    try{cell.maxScore[iTop] = StrUtils.toDouble(value);} 
                    catch(argus.model.ArgusException ex){throw new ArgusException (ex.getMessage());}
                    iTop++;
                    break;
                case LOWER_PROTECTION_LEVEL:
                    if ((value.equals("") || value.equals("-"))){break;}
                    try{cell.lower = StrUtils.toDouble(value);} 
                    catch(argus.model.ArgusException ex){throw new ArgusException (ex.getMessage());}
                    break;
                case UPPER_PROTECTION_LEVEL:
                    if ((value.equals("") || value.equals("-"))){break;}
                    try{cell.upper = StrUtils.toDouble(value);} 
                    catch(argus.model.ArgusException ex){throw new ArgusException (ex.getMessage());}
                    break;
                case STATUS:
                    hasStatus = true;
                    value = value.toUpperCase();
                    if (value.equals("")) {cell.status = CellStatus.EMPTY;}
                    else if (value.equals("E")) {
                        cell.status = CellStatus.EMPTY;
//                     If the cell status is empty we should do nothing as default status is already empty
//                     But for the cover tabel it is needed as we need to make a distinction
                        if (Application.isProtectCoverTable()){
                         cell.status = CellStatus.PROTECT_MANUAL;//CellStatus.EMPTY;
                         cell.freq = 1;
                        }
                    } else if (value.equals(metadata.safeStatus)) {
                        cell.status = CellStatus.SAFE_MANUAL;
                    } else if (value.equals(metadata.unSafeStatus)) {
                        cell.status = CellStatus.UNSAFE_MANUAL;
                    } else if (value.equals(metadata.protectStatus)) {
                        cell.status = CellStatus.PROTECT_MANUAL;
                    } else {
                        if (value.equals("M")) {
                            cell.status = CellStatus.SECONDARY_UNSAFE;
                            suppressed = SUP_HITAS;
                        } else {
                            throw new ArgusException("Unknown status(" + value + ")");
                        }
                    }
            } // end of switch statement
        } // end loop over all variables;
        

        if (readFreqOnlyTable) {
            cell.response = cell.freq;
        }
 
        // Checking for consistency of empty cells etc
        //1
        if (cell.response == Cell.UNKNOWN && cell.freq == Cell.UNKNOWN){
            if (cell.status == CellStatus.SAFE ||cell.status == CellStatus.SAFE_MANUAL) {cell.status = CellStatus.EMPTY;}
            if (cell.status != CellStatus.EMPTY){
                throw new ArgusException("An empty cell cannot have a status different from empty");             
            } 
        }
        //2
        if (cell.response == Cell.UNKNOWN && cell.freq != Cell.UNKNOWN){
            throw new ArgusException("An empty cell cannot have a real frequency");                         
        }
       
        //3
        if ((cell.response != Cell.UNKNOWN && cell.response != 0)  && cell.freq == 0){
            throw new ArgusException("A real cell cannot have a frequency zero");                         
        }

        //4 ook nog naar kijken Anco 7 mei 2017
        if (cell.response != Cell.UNKNOWN && cell.freq == Cell.UNKNOWN){
            cell.freq = 1;
            hs = "";
            for (int j=0;j<iExp;j++){
                if (codes[j].equals("")) {hs =hs + "Total";}
                else {hs = hs + codes[j];}
                if (j+1<iExp) {hs=hs+",";}
            }
            SystemUtils.writeLogbook("A real cell with no frequency is strange;"
                                      + " In cell ("+ hs+") freq = 1 has been imputed.");
        }
        //5
        if (cell.response == Cell.UNKNOWN && cell.freq == Cell.UNKNOWN){
            cell.status = CellStatus.EMPTY;
        }
        
        //6
        //If an empty cell has a status <> empty we will overrule this
        if (cell.response == Cell.UNKNOWN && hasStatus){
            if ( !cell.status.isEmpty()){
                cell.status = CellStatus.EMPTY; 
            }                
        }
        
        //7
        if ((cell.response != Cell.UNKNOWN && cell.response != 0) && 
            (cell.freq != Cell.UNKNOWN && cell.freq != 0)  &&
                cell.status == CellStatus.EMPTY){
           throw new ArgusException("A non-empty cell cannot have a status empty");                                     
        }

        if (cell.status == CellStatus.UNSAFE_MANUAL) {
            if (cell.lower == Cell.UNKNOWN) {
                cell.lower = Math.abs(cell.response * manualMarge / 100);
                if (cell.lower > cell.response) {
                    cell.lower = cell.response;
                }
            }
            if (cell.upper == Cell.UNKNOWN) {
                cell.upper = Math.abs(cell.response * manualMarge / 100);
                if (cell.upper > cell.response) {
                    cell.upper = cell.response;
                }
            }
        } else {
            cell.lower = 0;
            cell.upper = 0;
        }
        
        if (cell.shadow == Cell.UNKNOWN) {
            cell.shadow = cell.response;
        }
        if (cell.cost == Cell.UNKNOWN) {
            cell.cost = abs(cell.response);
        }
        if (cell.cost == 0) {
            cell.cost = 0.0001;
        }
        if (cell.cost < 0 ) {throw new ArgusException("Negative cost value found");}
        
        // test for TopN <= total; BUT only if topN is given;
        double X = 0;
        if (iTop > 0){
            for (int j = 0; j < iTop; j++) {
                X = X + cell.maxScore[j];
            }
            if (X > cell.response + MYEPSILON) {
                throw new ArgusException("Sum of topN " + X + " should not exceed the cell total " + cell.response);
            }
            if (X > cell.response) {
                cell.maxScore[0] = cell.maxScore[0] - MYEPSILON;
            }
            for (int j = 1; j < iTop; j++) {
                if (cell.maxScore[j] > cell.maxScore[j - 1]) {
                    throw new ArgusException("Error in the order of the TopN.\n"
                                                + (j - 1) + " = " + cell.maxScore[j - 1] + "\n"
                                                + j + " = " + cell.maxScore[j]
                                                + "\n.This is not allowed.");
                }
            }
        }

        if (cell.freq == 0 && cell.response == 0) {
            return false;
        }
        if (!useStatusOnly && (cell.status != CellStatus.EMPTY)) {
            cell.status = CellStatus.UNKNOWN;
        }
        return true;
    }

    void updateProgress(long Fread, long Flen, int LineNo, PropertyChangeSupport propertyChangeSupport) {
        if (LineNo % 100 == 0) {
            int percentage = (int)(Fread / Flen);
            propertyChangeSupport.firePropertyChange("progressMain", null, percentage);
        }
    }

    private static boolean getTableCell (int tabNo, int[] dimIndex, double[] CellResp, int[] CellStatus, 
                                         double[] CellLower, double[] CellUpper, double[] CellCost, 
                                         double[] CellKey, double[] CellKeyNoZeros){
        double[] CR = new double[1];
        double[] Lower = new double[1];
        double[] Upper = new double[1];
        int[] Status = new int[1];
        double[] Shadow = new double[1];
        double[] Cost = new double[1];
        double[] MaxScore = new double[MAX_TOP_N_VAR];
        double[] MaxScoreWeight = new double[MAX_TOP_N_VAR];
        double[] RealLower = new double[1];
        double[] RealUpper = new double[1];
        double[] CK = new double[1]; //CellKey
        double[] CKNZ = new double[1]; //CellKeyNoZeros      
        double[] xcta = new double[1];
        double[] xckm = new double[1];
        double[] hms = new double[MAX_TOP_N_VAR];
        double[] peep = new double[1];
        double[] peephold = new double[1];

        double[] ix = new double[1];
        int[] cf = new int[1];
        int[] cfh = new int[1];
        int[] holdnr = new int[1];
        int[] peepsrt = new int[1];
        int[] peepsrthold = new int[1];
        boolean oke;       
        oke = tauArgus.GetTableCell(tabNo, dimIndex, CR, ix, xcta,
                                    xckm, Shadow, Cost, CK, CKNZ, 
                                    cf, Status, MaxScore, MaxScoreWeight, 
                                    cfh, hms, holdnr,
                                    peep, peephold, peepsrt, peepsrthold,
                                    Lower, Upper, RealLower, RealUpper);

        CellResp[0] = CR[0];
        CellStatus[0] = Status[0];
        CellLower[0] = Lower[0];
        CellUpper[0] = Upper[0];
        CellCost[0] = Cost[0];
        CellKey[0] = CK[0];
        CellKeyNoZeros[0] = CKNZ[0];
        return oke;
    }

    public void read(PropertyChangeSupport propertyChangeSupport) throws ArgusException, FileNotFoundException, IOException {
        int[] errorCodeArr = new int[1];
        int[] varListIndexArr = new int[1];
        String[] codes = new String[expVar.size()];
        int[] dimIndex = new int[expVar.size()];
        computeTotals = (additivity == TableSet.ADDITIVITY_RECOMPUTE);
        hasRealFreq = metadata.containsFrequencyVariable();
        boolean continueBogusCovertable;
        int[][] bogusRange = new int[expVar.size()][2];
        int[] bogusIndex = new int[expVar.size()];
        int nDim, i;

        int[] varlist = indicesOfExplanatoryVariables();
        TauArgusUtils.DeleteFile(Application.getTempFile("AdditErr.txt"));
        // start Added 26-08-2022
        TauArgusUtils.DeleteFile(Application.getTempFile("AddErr.JJ"));
        TauArgusUtils.DeleteFile(Application.getTempFile("AddErr.JJ.frq"));
        TauArgusUtils.DeleteFile(Application.getTempFile("additerr.jj"));
        TauArgusUtils.DeleteFile(Application.getTempFile("additerr.jj.frq"));
        TauArgusUtils.DeleteFile(Application.getTempFile("JJklad.jj"));
        // end Added 26-08-2022
        nDim = expVar.size();
        File[] files = SystemUtils.getFiles(metadata.dataFile);
        for (File file : files) {
            FileInputStream fis = null;
            FileChannel fileChannel = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;

            try {
                fis = new FileInputStream(file);
                fileChannel = fis.getChannel();
                isr = new InputStreamReader(fis);
                reader = new BufferedReader(isr);
                
                Tokenizer tokenizer = new Tokenizer(reader);
                propertyChangeSupport.firePropertyChange("activityMain", null, "Phase 2 for file: " + file.getName());
                long fileLength = file.length();
                nextline:
                while (tokenizer.nextLine() != null) {
                    updateProgress(fileChannel.position(), fileLength, tokenizer.getLineNumber(), propertyChangeSupport);
                    int iExp = 0;
                    for (int j = 0; j < metadata.variables.size(); j++) {
                        Variable variable = metadata.variables.get(j);
                        String value = tokenizer.nextField(metadata.fieldSeparator);
                        if (variable.type == Type.CATEGORICAL) {
                            if (variable.isTotalCode(value)) {
                                if (Application.isProtectCoverTable()) {
                                    codes[iExp] = "";
                                } else {
                                    continue nextline;
                                }
                            } else {
                                codes[iExp] = variable.normaliseCode(value);
                            }
                            if (variable.isMissing(value)) {
                                continue nextline;
                            }
                            iExp++;
                        }
                    }

                    boolean b = false;
                    int errorCode = 0;
                    int varListIndex = -1;
                    int numberOfRetries = 0;

                    while (!b && numberOfRetries < this.expVar.size()){ // geen varlist.length) {
                        b = tauArgus.SetInCodeList(this.expVar.size(), varlist, codes, errorCodeArr, varListIndexArr);
                        if (!b) {
                            errorCode = errorCodeArr[0];
                            if (errorCode != Application.ERR_CODENOTINCODELIST) {
                                break;
                            }
                            varListIndex = varListIndexArr[0];
                            Variable variable = expVar.get(varListIndex);
                            if (variable.hierarchical == Variable.HIER_FILE) {
                                codes[varListIndex] = variable.padCode(codes[varListIndex]);
                            }
                            numberOfRetries++;
                        }
                    }
                    if (!b) {
                        String s = "";
                        for (int j = 0; j < varlist.length; j++) {
                            s = s + "\"" + codes[j] + "\", ";
                        }
                        s = s.substring(0, s.length() - 2);
                        throw new ArgusException(" Error in line " + tokenizer.getLineNumber() + " of table file: " + file.getCanonicalPath() + "\n"
                                + tauArgus.GetErrorString(errorCode) + "\n"
                                        + " Codes: " + s + "\n"
                                                + " code nr: " + varListIndex);
                    }
                }
            }
            finally{fis.close();
            fileChannel.close();
            isr.close();
            reader.close();
            }
        }
        
        if (!tauArgus.SetTotalsInCodeList(varlist.length, varlist, errorCodeArr, varListIndexArr)) {
            int errorCode = errorCodeArr[0];
            String error = tauArgus.GetErrorString(errorCode);
            throw new ArgusException("Error completing code list\n" + error);
        }

        if (!tauArgus.SetTable(index, varlist.length, varlist, isFrequencyTable(), 
                                indexOfResponseVariable(), indexOfShadowVariable(), indexOfCostVariable(), indexOfCellKeyVariable(), 
                                respVar.CKMType, respVar.CKMTopK,
                                lambda, maxScaleCost, 0, missingIsSafe)) {
            throw new ArgusException("Error in specifying table " + index);
        }
        boolean hasMaxScore = metadata.numberOfTopNVariables() > 0;
        if (!tauArgus.SetTableSafetyInfo(index, hasMaxScore,
                  domRule, domN, domK,
                  pqRule, pqP, pqQ, pqN,
                  frequencyRule, frequencyMarge[0], minFreq[0],
                  useStatusOnly, manualMarge,
                  //zeroUnsafe, (int)Math.round(zeroRange), false, 10, errorCodeArr)) {
                  zeroUnsafe, zeroRange, false, 10, errorCodeArr)) {
            int errorCode = errorCodeArr[0];
            throw new ArgusException("Error in specifying table " + index + "\n" + tauArgus.GetErrorString(errorCode));
        }

        // go through file second time;

        propertyChangeSupport.firePropertyChange("activityMain", null, "Phase 3");

        // prepare the code lists for checking the doubles in a cover table
        int varNo;
        if ((Application.isProtectCoverTable()) ){
            //for (i=0;i<expVar.size();i++){ varNo = expVar.get(i).index;
            //    if (nMax < TauArgusUtils.getNumberOfActiveCodes(varNo)){nMax = TauArgusUtils.getNumberOfActiveCodes(varNo);}
            //}
            // To save memory, making use of Jagged Arrays, i.e. each row can have different number of elements
            //codeList = new String[expVar.size()][nMax];
            //codeListLevel  = new int[expVar.size()][nMax];
            //codeListNChild  = new int[expVar.size()][nMax];   
            codeList = new String[expVar.size()][];
            codeListLevel  = new int[expVar.size()][];
            codeListNChild  = new int[expVar.size()][];   
            int n;      
            for (i=0;i<expVar.size();i++){ 
                varNo = expVar.get(i).index;
                codeList[i] = new String[TauArgusUtils.getNumberOfActiveCodes(varNo)];
                codeListLevel[i] = new int[TauArgusUtils.getNumberOfActiveCodes(varNo)];
                codeListNChild[i] = new int[TauArgusUtils.getNumberOfActiveCodes(varNo)];
                n = -1;
                for (int j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                    tauArgus.GetVarCodeProperties(varNo, j, isParent, isActive, isMissing, level, nChild, code);
                    if (isActive[0] == 1){
                        n++;
                        codeList[i][n] = code[0].trim();
                        codeListLevel[i][n] = level[0];
                        codeListNChild[i][n] = nChild[0];
                    } 
                }
            }          
        }

        PrintWriter writer = null;
        int nInconsistent = 0, nInconsistentSevere = 0;

        if (Application.isProtectCoverTable()) {
            File file = new File(Application.getTempDir(), "Inconsistent.txt");
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writer.println("Overview of inconsistencies in the linked tables");
            writer.println("report generated: " + SystemUtils.now());
            writer.println("------------------------------------------------");
            writer.close();
        }

// prepare the code lists for checking the doubles in a cover table
// Lijkt dubbel op    
        if ((Application.isProtectCoverTable()) ){
            //for (i=0;i<expVar.size();i++){ varNo = expVar.get(i).index;
            //    if (nMax < TauArgusUtils.getNumberOfActiveCodes(varNo)){nMax = TauArgusUtils.getNumberOfActiveCodes(varNo);}
            //}
            //codeList = new String[expVar.size()][nMax];
            codeList = new String[expVar.size()][];
            for (i=0;i<expVar.size();i++){ 
                varNo = expVar.get(i).index;
                codeList[i] = new String[TauArgusUtils.getNumberOfActiveCodes(varNo)];
                for (int j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                    codeList[i][j] = TauArgusUtils.getCode(varNo, j).trim();
                }
            }
        }
        
        for (File file : files) {
            FileInputStream fis = null;
            FileChannel fileChannel = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;
            try {
                fis = new FileInputStream(file);
                fileChannel = fis.getChannel();
                isr = new InputStreamReader(fis);
                reader = new BufferedReader(isr);
                if (Application.isProtectCoverTable()){
                    File fileIncon = new File(Application.getTempDir(), "Inconsistent.txt");
                    writer = new PrintWriter(new BufferedWriter(new FileWriter(fileIncon)),true/*append=true*/);
                    for (i=0;i<nDim;i++){
                        varNo = expVar.get(i).index;  
                        for (int j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                            tauArgus.GetVarCodeProperties(varNo, j, isParent, isActive, isMissing, level, nChild, code); 
                            writer.print(j + " code:" + code[0] + "; parent: " + isParent[0] + "; nchild:" + nChild[0] +
                                    "; level:"+ level[0] +"\n");
                        }        
                    }
                    writer.close();
                }
        
                Tokenizer tokenizer = new Tokenizer(reader);
                long fileLength = file.length();
                while (tokenizer.nextLine() != null) {
                    String line = tokenizer.getLine();
                    updateProgress(fileChannel.position(), fileLength, tokenizer.getLineNumber(), propertyChangeSupport);
                    try {
                        Cell cell = new Cell();
                        if (!buildCell(tokenizer, codes, cell)) {
                            continue;
                        }
                        //prepare the loop for the bogus imputation for the cover table
                        continueBogusCovertable = false;
                        if (Application.isProtectCoverTable()){
                            for (i=0;i<nDim;i++){
                                //bogusIndex[i] = -1;
                                //varNo = expVar.get(i).index;
                                //for (int j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                                //    if (codes[i].trim().equals(codeList[i][j])) {bogusIndex[i] = j; break;}
                                //}
                                //Faster to use buildin search. indexOf returns -1 if not found
                                bogusIndex[i] = Arrays.asList(codeList[i]).indexOf(codes[i].trim());
                                if (codes[i].equals(expVar.get(i).totCode)){bogusIndex[i] = 0;}
                            }
                            nDim = expVar.size();
                            continueBogusCovertable = true;
                            for (i=0;i<nDim;i++){ 
                                bogusRange[i][0] = bogusIndex[i];
                                bogusRange[i][1] = bogusIndex[i];
                            }
                            
                            for (i=0;i<expVar.size();i++){        
                                varNo = expVar.get(i).index;
                                bogusRange[i][0] = getBogusMother(i, bogusIndex[i]); //varNo
                                bogusRange[i][1] = getBogusChild(i, varNo, bogusIndex[i]);  //varNo
                                bogusIndex[i] = bogusRange[i][0];
                                if (bogusRange[i][0] == 0) {codes[i]="";}
                                else {codes[i] = getVarCode(varNo, bogusRange[i][0]);}
                            }
                        }
                        do { //This will become a real loop if it is a cover table and there is a bogus level  
                            boolean b = false;
                            int errorCode = 0;
                            int numberOfRetries = 0;
                            // Prevent to submit empty cells
                            if (cell.status == CellStatus.EMPTY) {b=true;} 
                            // SystemUtils.writeLogbook(codes[0]+","+ codes[1]+","+ codes[2]);
                            while (!b && numberOfRetries < varlist.length) {
                                b = tauArgus.SetInTable(index, codes, cell.shadow, cell.cost, cell.response, cell.freq, cell.maxScore, cell.holdingMaxScore,
                                cell.status.getValue(), cell.lower, cell.upper, errorCodeArr, varListIndexArr);
                                if (!b) {
                                    errorCode = errorCodeArr[0];
                                    if (errorCode == Application.ERR_CODEDOESNOTEXIST) {
                                        int varListIndex = varListIndexArr[0];
                                        numberOfRetries++;
                                        Variable variable = expVar.get(varListIndex);
                                        if (variable.hierarchical == Variable.HIER_FILE) {
                                            codes[varListIndex] = variable.padCode(codes[varListIndex]);
                                        }
                                    } else {
                                    // Has been filled already;
                                    // We could check on conflicting values/statusses;
                                        if (errorCode == Application.ERR_CELLALREADYFILLED){
                                            numberOfRetries = varlist.length + 1;
                                            if (!(Application.isProtectCoverTable()) ) {
                                                String s = "\n Codes:";
                                                for (int j = 0; j < varlist.length; j++) {
                                                    s = s + codes[j] + ",";
                                                }
                                                throw new ArgusException(tauArgus.GetErrorString(errorCode) + s);
                                            } else {
                                                b=true;
                                                // Check for conflicting values/statuses for cover table;
                                                // determine dimarray;
                                                // get cel;
                                                // make overview;
                                                for (i=0;i<expVar.size();i++){
                                                    //dimIndex[i] = -1;
                                                    //varNo = expVar.get(i).index;
                                                    //codes[i]=codes[i].trim();
                                                    //for (int j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                                                    //    if (codes[i].trim().equals(codeList[i][j])) {dimIndex[i] = j; break;}
                                                    //}
                                                    //Making use of build-in search is faster
                                                    //indexOf returns -1 if not found
                                                    dimIndex[i] = Arrays.asList(codeList[i]).indexOf(codes[i].trim());
                                                }
                    
                                                Cell doubleCell = new Cell();
                                                doubleCell = getCell(dimIndex);

                                                if (doubleCell.response != cell.response ||
                                                        doubleCell.status != cell.status||
                                                        doubleCell.lower != cell.lower ||
                                                        doubleCell.upper != cell.upper) {
                                                    nInconsistent++;
                                                    String hs = "codes: ";
                                                    for (i=0;i<expVar.size();i++){
                                                        if (codes[i].equals("")) { hs = hs + "Total, ";}
                                                        else{ hs = hs + codes[i]+", ";}
                                                    }
                                                    hs = hs.substring(0, hs.length() - 2);
                                                    File fileIncon = new File(Application.getTempDir(), "Inconsistent.txt");
                                                    writer = new PrintWriter(new BufferedWriter(new FileWriter(fileIncon)),true/*append=true*/);

                                                    writer.println(hs);
                                                    writer.println("line:" + tokenizer.getLineNumber());

                                                    if (doubleCell.response != cell.response) {
                                                        writer.println("values " + doubleCell.response + " <> " + cell.response);
                                                    } else {
                                                        writer.println("value " + cell.response );
                                                    }
                                                    if (doubleCell.status != cell.status) {
                                                        writer.println("status1 " + doubleCell.status.getDescription());
                                                        writer.println("status2 " + cell.status.getDescription());
                                                        nInconsistentSevere++;
                                                    }
                                                    if (doubleCell.lower != cell.lower) {
                                                        writer.println("LPL    " + doubleCell.lower + " <> " + cell.lower);
                                                    }
                                                    if (doubleCell.upper != cell.upper) {
                                                        writer.println("UPL    " + doubleCell.upper + " <> " + cell.upper);
                                                    }
                                                    writer.println("------------------------------------------------");
                                                }
                                                doubleCell = null;
                                                writer.close();
                                            }
                                        }
                                    }
                                }
                            }

                            if (!b) {
                                SystemUtils.writeLogbook("Something wrong");
                                throw new ArgusException(tauArgus.GetErrorString(errorCode));
                            }

                            // The bogus loop for the cover table
                            if (Application.isProtectCoverTable()){
                                //increase the loop
                                bogusIndex[nDim-1]++;
                                int n = nDim-1;
                                while (bogusIndex[n]> bogusRange[n][1] ){
                                    bogusIndex[n] =  bogusRange[n][0];
                                    if (n > 0) {n--; bogusIndex[n]++;}
                                    else continueBogusCovertable = false;
                                }
                                if (continueBogusCovertable )
                                    for (i=0;i<expVar.size();i++){
                                        varNo = expVar.get(i).index;
                                        if (bogusRange[i][0] == 0) {codes[i]="";}
                                        else {codes[i] = getVarCode(varNo, bogusIndex[i]);}
                                    }
                            }
                        }
                        while (continueBogusCovertable);
                    }
                    catch (ArgusException ex) {
                        throw new ArgusException("Error in file " + file.getCanonicalPath() + " on line " + tokenizer.getLineNumber() + ".\n"
                                + "Line = " + line + "\n"
                                + ex.getMessage());
                    }
                } // per record;
            }
            finally {
                fis.close();
                fileChannel.close();
                isr.close();
                reader.close();
            }
        } // per file;
        if (Application.isProtectCoverTable()) {
            //writer.close();
            File file = new File(Application.getTempDir(), "Inconsistent.cnt");
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writer.println("" +nInconsistent); 
            writer.println( "" + nInconsistentSevere);
            writer.close();
            if (nInconsistent > 0) {
                SystemUtils.writeLogbook("" + nInconsistent + " inconsistencies found in cover table\n"
                        + "see als file Inconsistent.txt in the temp directory (" + Application.getTempDir() + ")");
                LOGGER.info("" + nInconsistent + " inconsistencies found in cover table\n"
                        + "see als file Inconsistent.txt in the temp directory (" + Application.getTempDir() + ")");
            }
            if (nInconsistentSevere>0){throw new ArgusException(""+nInconsistentSevere+" severe inconsistencies found in the cover table\n"+
                                           "the protection of the cover table was terminated");}
        }

        boolean setCalculatedTotalsAsSafe = !frequencyRule && !domRule && !pqRule;
        String fileNameJJ = Application.getTempFile("additerr.jj");
        String fileNameTxt = Application.getTempFile("additerr.txt");
        File fileJJ = new File(fileNameJJ);
        File fileTxt = new File(fileNameTxt);
        fileJJ.delete();
        fileTxt.delete();
        this.isAdditive = true;
        //index
        int[] StatusList = null;
        double[] LPLList = null;
        double[] UPLList = null;
        if (computeTotals && keepStatus){
        // Save status of all cells
            int[] Status = new int[1];
            double[] LPL = new double[1];
            double[] UPL = new double[1];
            StatusList = new int[this.numberOfCells()];
            LPLList = new double[this.numberOfCells()];
            UPLList = new double[this.numberOfCells()];
            for (int nc=0;nc<this.numberOfCells();nc++){
                tauArgus.GetTableCellStatus(this.index, nc, Status);
                StatusList[nc]=Status[0];
                tauArgus.GetTableCellProtectionLevels(this.index, nc, LPL, UPL);
                LPLList[nc]=LPL[0];
                UPLList[nc]=UPL[0];
            }
        }
        if (!tauArgus.CompletedTable(index, errorCodeArr, fileNameJJ, computeTotals, setCalculatedTotalsAsSafe, Application.isProtectCoverTable())) {
            this.isAdditive = false;
            if (this.additivity == TableSet.ADDITIVITY_NOT_REQUIRED || Application.isProtectCoverTable()) {}
            else{
                if (errorCodeArr[0] != Application.ERR_TABLENOTADDITIVE) {throw new ArgusException(tauArgus.GetErrorString(errorCodeArr[0]));}
                else{ //ask to generate an overview
                    MakeAdditErrorList(index);
                    throw new ArgusException(tauArgus.GetErrorString(errorCodeArr[0]));
                }
            }
        }
        if (computeTotals && keepStatus){
        // Reload status of all cells
            if (StatusList == null) throw new ArgusException("Something went wrong in reloading cell status: StatusList = null\n");
            for (int nc=0; nc<this.numberOfCells(); nc++) {
                // Only keep Status if not empty and not unknown
                if (!(StatusList[nc]==CellStatus.UNKNOWN.getValue() || StatusList[nc]==CellStatus.EMPTY.getValue() || StatusList[nc]==CellStatus.EMPTY_NONSTRUCT.getValue())){
                    tauArgus.SetTableCellStatus(this.index, nc, StatusList[nc]);
                    tauArgus.SetTableCellProtectionLevels(this.index, nc, LPLList[nc], UPLList[nc]);
                }
            }
        }

        double[] XMaxArr = new double[1];
        double XMin = tauArgus.GetMinimumCellValue(index, XMaxArr);
        double XMax = XMaxArr[0];
        if (XMax > 0) {XMax = 1.5 * XMax;}
        else {XMax = 0;}
        if (XMin > 0) {
            XMin = 0;
        }
        if (XMin < 0) {
            XMin = 1.5 * XMin;
        }
        maxTabVal = XMax;
        minTabVal = XMin;
    }
      
    public void write(String fileName, boolean suppressEmpty, boolean simple, boolean holdinglevel, boolean withAudit, boolean EmbedQuotes, PropertyChangeListener propertyChangeListener) throws IOException, ArgusException {
        PropertyChangeSupport propertyChangeSupport = null;
        if (!Application.isBatch()){
            propertyChangeSupport  = new PropertyChangeSupport(this);
            propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
        }  

        int nExpVar = expVar.size();
        int[] maxDim = new int[nExpVar];
        int[] dimArray = new int[nExpVar];

        // including empty cells
        int numberOfCells = 1;
        for (int i = 0; i < nExpVar; i++) {
            dimArray[i] = 0;
            maxDim[i] = TauArgusUtils.getNumberOfActiveCodes(expVar.get(i).index);
            numberOfCells = numberOfCells * maxDim[i];
        }
        int numberOfTopNNeeded = 0;
        if (!simple) {
            numberOfTopNNeeded = holdinglevel ? numberOfHoldingTopNNeeded() : numberOfTopNNeeded();
        }

        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter writer = null;
        try {
            fw = new FileWriter(fileName);
            bw = new BufferedWriter(fw);
            writer = new PrintWriter(bw);

            VarCode varCode = new VarCode();
            int cellIndex = 0;
            int nDec = respVar.nDecimals;
            DecimalFormat mdecimalFormat = new DecimalFormat();
            mdecimalFormat.setMinimumFractionDigits(nDec);
            mdecimalFormat.setMaximumFractionDigits(nDec);
            mdecimalFormat.setGroupingUsed(false);
            for (;;) {
                Cell cell = getCell(dimArray);
                if (cell.status != CellStatus.EMPTY || !suppressEmpty) {
                    for (int j = 0; j < expVar.size(); j++) {
                        Variable variable = expVar.get(j);
                        varCode.setCode(variable.index, dimArray[j]);
                        String codeString = varCode.getCodeString();
                        if (codeString.equals("")) {
                            codeString = variable.getTotalCode();
                        }
                        if (EmbedQuotes){
                            writer.print(StrUtils.quote(codeString) + ";");
                        }
                        else{
                            writer.print(codeString + ";");
                        }
                    }
                    if (!rounded) writer.print(mdecimalFormat.format(cell.response) + ";");
                    if (rounded) writer.print(mdecimalFormat.format(cell.roundedResponse) + ";");
                    // for freq tables no double column resp and freq nor shadow variable
                    if (!isFrequencyTable()){
                        if (!holdinglevel) {
                            writer.print(cell.freq + ";");
                        } else {
                            writer.print(cell.holdingFreq + ";");
                        }
                        if (!simple) {
                            writer.print(mdecimalFormat.format(cell.shadow) + ";");
                        }
                    } 
                    writer.print(mdecimalFormat.format(cell.cost) + ";");
                    if (!simple) {
                        if (holdinglevel) {
                            for (int j = cell.holdingFreq; j < numberOfTopNNeeded; j++) {
                                cell.holdingMaxScore[j] = 0;
                            }
                            for (int j = 0; j < numberOfTopNNeeded; j++) {
                                writer.print(mdecimalFormat.format(cell.holdingMaxScore[j]) + ";");
                            }
                        } else {
                            for (int j = cell.freq; j < numberOfTopNNeeded; j++) {
                                cell.maxScore[j] = 0;
                            }
                            for (int j = 0; j < numberOfTopNNeeded; j++) {
                                writer.print(mdecimalFormat.format(cell.maxScore[j]) + ";");
                            }
                        }
                    }
                    if (cell.status.isEmpty()) {
                        writer.print("E");
                    } else {
                        writer.print(cell.status.getCategory().getSymbol());
                    }
                    writer.print(";" + mdecimalFormat.format(cell.lower) + ";" + mdecimalFormat.format(cell.upper));
                    if (withAudit) {
                        writer.print(";" + mdecimalFormat.format(cell.realizedLower) + ";" + mdecimalFormat.format(cell.realizedUpper) + ";" + mdecimalFormat.format(cell.realizedUpper - cell.realizedLower));
                        if (cell.response == 0 || cell.status.isSafeNotProtected()) {
                            writer.print(";0;0;0");
                        } else {
                            mdecimalFormat.setMinimumFractionDigits(2);
                            mdecimalFormat.setMaximumFractionDigits(2);
                            writer.print(";"
                                    + mdecimalFormat.format(100 * (cell.response - cell.realizedLower) / cell.response) + ";"
                                    + mdecimalFormat.format(100 * (cell.realizedUpper - cell.response) / cell.response) + ";"
                                    + mdecimalFormat.format(100 * (cell.realizedUpper - cell.realizedLower) / cell.response));
                            mdecimalFormat.setMinimumFractionDigits(nDec);
                            mdecimalFormat.setMaximumFractionDigits(nDec);
                        }
                    }
                    writer.println();
                }
                cellIndex++;
                if (!Application.isBatch()){
                    if (cellIndex % 1000 == 0) {
                        int percentage = (int)(100L * cellIndex / numberOfCells);
                        propertyChangeSupport.firePropertyChange("progressMain", null, percentage);
                        propertyChangeSupport.firePropertyChange("activityMain", null, "(" + cellIndex + ")");
                    }
                }

                // dimArray ophogen
                int k = nExpVar;
                while (k-- > 0) {
                    dimArray[k]++;
                    if (dimArray[k] < maxDim[k]) {
                        break;
                    } else {
                        dimArray[k] = 0;
                    }
                }
                if (k == -1) {
                    break;
                }
            }
        }
        finally {
            writer.close();
        }

        String metadataFileName = StrUtils.replaceExtension(fileName, METADATA_FILE_EXTENSION);
        metadata.writeTableMetadata(metadataFileName, nExpVar, expVar.toArray(new Variable[expVar.size()]), respVar, shadowVar, costFunc, costVar, numberOfTopNNeeded, simple, withAudit);
    }
    
    public void writeCKM(boolean addOrig, boolean addDiff, boolean addCellKey, boolean suppressEmpty, 
                         boolean EmbedQuotes, PropertyChangeListener propertyChangeListener) throws IOException, ArgusException {
        PropertyChangeSupport propertyChangeSupport = null;
        if (!Application.isBatch()){
            propertyChangeSupport  = new PropertyChangeSupport(this);
            propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
        }  

        int nExpVar = expVar.size();
        int[] maxDim = new int[nExpVar];
        int[] dimArray = new int[nExpVar];

        // including empty cells
        int numberOfCells = 1;
        for (int i = 0; i < nExpVar; i++) {
            dimArray[i] = 0;
            maxDim[i] = TauArgusUtils.getNumberOfActiveCodes(expVar.get(i).index);
            numberOfCells = numberOfCells * maxDim[i];
        }

        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter writer = null;
        try {
            fw = new FileWriter(safeFileName);
            bw = new BufferedWriter(fw);
            writer = new PrintWriter(bw);

            VarCode varCode = new VarCode();
            int cellIndex = 0;
            int nDec = respVar.nDecimals;
            DecimalFormat mdecimalFormat = new DecimalFormat();
            mdecimalFormat.setMinimumFractionDigits(nDec);
            mdecimalFormat.setMaximumFractionDigits(nDec);
            mdecimalFormat.setGroupingUsed(false);
            
            DecimalFormat CKdecimalFormat = new DecimalFormat();
            CKdecimalFormat.setMinimumFractionDigits(cellkeyVar.nDecimals);
            CKdecimalFormat.setMaximumFractionDigits(cellkeyVar.nDecimals);
            CKdecimalFormat.setGroupingUsed(false);
            
            for (;;) {
                Cell cell = getCell(dimArray);
                
                if (cell.status != CellStatus.EMPTY || !suppressEmpty) {
                    for (int j = 0; j < expVar.size(); j++) {
                        Variable variable = expVar.get(j);
                        varCode.setCode(variable.index, dimArray[j]);
                        String codeString = varCode.getCodeString();
                        if (codeString.equals("")) {
                            codeString = variable.getTotalCode();
                        }
                        if (EmbedQuotes){
                            writer.print(StrUtils.quote(codeString) + ";");
                        }
                        else{
                            writer.print(codeString + ";");
                        }
                    }

                    writer.print(mdecimalFormat.format(cell.CKMValue));
                    
                    if (addOrig){
                        writer.print(";" + mdecimalFormat.format(cell.response));
                    }
                    if (addDiff){
                        writer.print(";" + mdecimalFormat.format(cell.CKMValue - cell.response));
                    }
                    if (addCellKey){
                            writer.print(";" + CKdecimalFormat.format((this.isFrequencyTable() || respVar.zerosincellkey) ? cell.cellkey : cell.cellkeynozeros));
                    }
                    writer.println();
                }
                cellIndex++;
                if (!Application.isBatch()){
                    if (cellIndex % 1000 == 0) {
                        int percentage = (int)(100L * cellIndex / numberOfCells);
                        propertyChangeSupport.firePropertyChange("progressMain", null, percentage);
                        propertyChangeSupport.firePropertyChange("activityMain", null, "(" + cellIndex + ")");
                    }
                }

                // dimArray ophogen
                int k = nExpVar;
                while (k-- > 0) {
                    dimArray[k]++;
                    if (dimArray[k] < maxDim[k]) {
                        break;
                    } else {
                        dimArray[k] = 0;
                    }
                }
                if (k == -1) {
                    break;
                }
            }
        }
        finally {
            writer.close();
        }

        String metadataFileName = StrUtils.replaceExtension(safeFileName, METADATA_FILE_EXTENSION);
        metadata.writeCKMMetadata(metadataFileName, nExpVar, expVar.toArray(new Variable[expVar.size()]), respVar, addOrig, addDiff, addCellKey);
    }    
    
    public void writeCKMStats(){
        FileWriter fw = null;
        BufferedWriter bw = null;
        try{
            fw = new FileWriter(StrUtils.replaceExtension(safeFileName, ".log"));
            bw = new BufferedWriter(fw);
            for (int i=this.minDiff;i<=this.maxDiff;i++){
                bw.write(Integer.toString(i) + ": ");
                bw.write(Long.toString(this.getCKMStats().get(i))+"\n");
            }
            bw.write("Empty: "+Long.toString(this.numberOfEmpty())+"\n");
            bw.write("Total: "+Long.toString(this.numberOfCells())+"\n");
            bw.close();
        }catch (IOException ex){}
    }
    
    public void undoAudit(){
      hasBeenAudited = false;
      auditExactDisclosure = 0;
      auditPartialDisclosure = 0;
    }

    public void readSafetyRule(Tokenizer tokenizer) throws ArgusException {
        boolean freqTab = false;

        int nPq = 0;
        int nDom = 0;
        int nFreq = 0;
        int nReq = 0;
//        int nRules = 0; Not used ?????

        String token = tokenizer.nextToken();
        if (token.equals("")) {
            useStatusOnly = true;
        }
        while (!token.equals("")) {
            String rule = token;
            if (!tokenizer.nextToken().equals("(")) {
                throw new ArgusException("Missing left parenthesis.");
            }
            List<Integer> parameters = new ArrayList<>();
            do {
                parameters.add(Integer.valueOf(tokenizer.nextToken()));
                token = tokenizer.nextToken();
            } while (token.equals(","));
            if (!tokenizer.nextToken().equals(")")) {
                throw new ArgusException("Missing right parenthesis.");
            }
            switch(rule) {
                case "P":
                    if (freqTab) {
                        throw new ArgusException("P rule not possible for frequency tables.");
                    }
                    if (nPq >= 4) {
                        throw new ArgusException("More than 4 P rules specified.");
                    }
                    if (parameters.size() != 2 && parameters.size() != 3) {
                        throw new ArgusException("P rule needs 2 parameters.");
                    }
                    pqP[nPq] = parameters.get(0);
                    if (parameters.size() == 3) {
                        pqQ[nPq] = parameters.get(1);
                        pqN[nPq] = parameters.get(2);
                    } else {
                        pqQ[nPq] = 100;
                        pqN[nPq] = parameters.get(1);
                    }
                    pqRule = true;
                    if (nPq >= 2 && pqP[nPq] > 0) {
                        holding = true;
                    }
                    nPq++;
                    break;
                case "NK":
                    if (freqTab) {
                        throw new ArgusException("NK rule not possible for frequency tables.");
                    }
                    if (nDom >= 4) {
                        throw new ArgusException("More than 4 NK rules specified.");
                    }
                    if (parameters.size() != 2) {
                        throw new ArgusException("NK rule needs 2 parameters.");
                    }
                    domN[nDom] = parameters.get(0);
                    domK[nDom] = parameters.get(1);
                    domRule = true;
                    if (nDom >= 2 /*&& domK[nDom] > 0*/) {
                        holding = true;
                    }
                    nDom++;
                    break;
                case "ZERO":
                    if (freqTab) {
                        throw new ArgusException("Zero rule not possible for frequency tables.");
                    }
                    if (parameters.size() != 1) {
                        throw new ArgusException("Zero unsafe rule needs 1 parameters.");
                    }
                    zeroRange =  parameters.get(0);
                    zeroUnsafe = true;
                    break;
                case "FREQ":
                    if (nFreq >= 2) {
                        throw new ArgusException("More than 2 frequency rules specified.");
                    }
                    if (parameters.size() != 2) {
                        throw new ArgusException("Frequency rule needs 2 parameters.");
                    }
                    int freq = parameters.get(0);
                    int marge = parameters.get(1);
                    if (freq < 0) {
                        throw new ArgusException("Minimum frequency should not be negative.");
                    }
                    if (freq > 0 && marge < 0) {
                        throw new ArgusException("Safety range for minimum frequency should not be negative.");
                    }
                    if (minFreq[nFreq] > 0 && frequencyMarge[nFreq] == 0) {
                        // SDCMsgBox "Zero margin for frequency rule might lead to poor protection", vbInformation;
                    }
                    minFreq[nFreq] = freq;
                    frequencyMarge[nFreq] = marge;
                    frequencyRule = true;
                    if (nFreq >= 1 && minFreq[nFreq] > 0) {
                        holding = true;
                    }
                    nFreq++;
                    break;
                case "REQ":
                    if (freqTab) {
                        throw new ArgusException("Request rule not possible for frequency tables.");
                    }
                    if (nReq >= 2) {
                        throw new ArgusException("More than 2 request rules specified.");
                    }
                    if (parameters.size() != 4) {
                        throw new ArgusException("Request rule needs 4 parameters.");
                    }
                    piepPercentage[2 * nReq] = parameters.get(0);
                    piepPercentage[2 * nReq + 1] = parameters.get(1);
                    piepMinFreq[nReq] = parameters.get(2);
                    piepMarge[nReq] = parameters.get(3);
                    if (piepPercentage[2 * nReq] > 0 || piepMinFreq[nReq] > 0) {
                        piepRule[nReq] = true;
                    }
                    if (nReq >= 1 && piepPercentage[2 * nReq] > 0) {
                        holding = true;
                    }
                    nReq++;
                    break;
                case "WGT":
                    if (parameters.size() != 1) {
                        throw new ArgusException("WGT command needs 1 parameter.");
                    }
                    int k = parameters.get(0);
                    // 0 = no weights, 1 = only weights, 2 = also adjust rules
                    if (k < 0 || k > 3) {
                        throw new ArgusException("Value for WGT should be 0 or 1.");
                    }
                    weighted = (k != 0);
                    if (weighted && !metadata.containsWeightVariable()) {
                        throw new ArgusException("There is no weight variable present");
                    }
                    break;
                case "MIS":
                    if (parameters.size() != 1) {
                        throw new ArgusException("MIS command needs 1 parameter.");
                    }
                    k = parameters.get(0);
                    if (k != 0 && k != 1) {
                        throw new ArgusException("Value for MIS should be 0 or 1.");
                    }
                    missingIsSafe = (k == 1);
                    break;
                case "MAN":
                    if (parameters.size() != 1) {
                        throw new ArgusException("MAN command needs 1 parameter.");
                    }
                    k = parameters.get(0);
                    if (k < 0 || k > 100) {
                        throw new ArgusException("Value for MAN should be between 0 and 100.");
                    }
                    manualMarge = k;
                    useStatusOnly = true;
                    break;
                default:
                    throw new ArgusException("Invalid command: " + rule);

            }
            //nRules++; Not used ?????
            token = tokenizer.nextToken();
            if (token.equals("|")) {
                token = tokenizer.nextToken();
            }
        }
        if (!useStatusOnly) {
            if (metadata.dataOrigin == Metadata.DATA_ORIGIN_TABULAR && metadata.numberOfTopNVariables() < numberOfTopNNeeded()) {
                throw new ArgusException("Not enough TopN variables specified to apply the safety rules");
            }
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

        TableSet tableSet = (TableSet) obj;

        boolean equal =
                expVar.equals(tableSet.expVar)
                && respVar == tableSet.respVar
                && shadowVar == tableSet.shadowVar
                && costVar == tableSet.costVar
                && domRule == tableSet.domRule
                && pqRule == tableSet.pqRule
                && weighted == tableSet.weighted
                && holding == tableSet.holding;
        for (int j=0; j<2; j++) {
            equal = equal
                    && piepRule[j] == tableSet.piepRule[j]
                    && minFreq[j] == tableSet.minFreq[j];
        }
        return equal;
    }
    
    public void clearHistory(){
        if (!Application.isProtectCoverTable()){
            TauArgusUtils.DeleteFile(Application.getTempFile("Apriori" + index + ".htm"));
            TauArgusUtils.DeleteFile(Application.getTempFile("HistStat" + index + ".htm"));
            TauArgusUtils.DeleteFile(Application.getTempFile("HistCost" + index + ".htm"));
            TauArgusUtils.DeleteFile(Application.getTempFile("HistPL" + index + ".htm"));
            TauArgusUtils.DeleteFile(Application.getTempFile("HistAB" + index + ".htm")); 
        }
        historyUsed = 0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.expVar);
        hash = 29 * hash + Objects.hashCode(this.respVar);
        hash = 29 * hash + Objects.hashCode(this.shadowVar);
        hash = 29 * hash + Objects.hashCode(this.costVar);
        hash = 29 * hash + (this.domRule ? 1 : 0);
        hash = 29 * hash + (this.pqRule ? 1 : 0);
        hash = 29 * hash + Arrays.hashCode(this.piepRule);
        hash = 29 * hash + Arrays.hashCode(this.minFreq);
        hash = 29 * hash + (this.weighted ? 1 : 0);
        hash = 29 * hash + (this.holding ? 1 : 0);
        return hash;
    }

    public static int getCellStatus(TableSet tableSet, int[] dimArray){
        int[] cellStatus = new int[1];
        double[] x = new double[1];
//        double[] x1 = new double[1]; Not used ?????
        double[] x2 = new double[1];
        double[] x3 = new double[1];
        double[] x4 = new double[MAX_TOP_N_VAR];
        double[] x5 = new double[MAX_TOP_N_VAR];
        double[] x6 = new double[1];
        double[] x7 = new double[1];
        double[] x8 = new double[1];
        double[] x9 = new double[1];
        double[] x10 = new double[1];
        double[] x11 = new double[1];
        double[] xcta = new double[1];
        double[] xckm = new double[1];
        double[] hms = new double[MAX_TOP_N_VAR];
        double[] peep = new double[1];
        double[] peephold = new double[1];

        //int[] ix = new int[1];
        double[] ix = new double[1];
        int[] cf = new int[1];
        int[] cfh = new int[1];
        int[] holdnr = new int[MAX_TOP_N_VAR];
        int[] peepsrt = new int[1];
        int[] peepsrthold = new int[1];
        tauArgus.GetTableCell(tableSet.index, dimArray, x, ix, xcta,
                                    xckm,
                                    x2, x3, x10, x11, cf, cellStatus, x4,
                                    x5, cfh,   hms, holdnr,
                                    peep, peephold, peepsrt,  peepsrthold,  x6,
                                    x7, x8, x9);
        return cellStatus[0];
    }

    private static int getBogusMother( int varNo, int index){
        int thislevel =  codeListLevel[varNo][index]; 
        while (index > 0){
            if ((codeListLevel[varNo][index-1] == thislevel-1) && (codeListNChild[varNo][index-1]== 1) ){
                index--; thislevel--;
            }
            else return index;
        }       
        return index;
    } 
   
    private static int getBogusChild (int varNo, int varIndex, int index){
        int maxCode = TauArgusUtils.getNumberOfActiveCodes(varIndex);
        int thislevel =  codeListLevel[varNo][index]; 
        while (index < maxCode-1){
            if ( (codeListLevel[varNo][index+1] == thislevel+1) && (codeListNChild[varNo][index] == 1) ){  
                index++; thislevel++;
                tauArgus.GetVarCodeProperties(varNo, index, isParent, isActive, isMissing, level, nChild, code);
            }
            else {return index;}
        }
        return index;    
    }
   
    private static String getVarCode(int varNo, int index){
        if (index>=0){
            tauArgus.GetVarCode(varNo, index, isActive, code, isMissing, level);
            if (code[0].equals("")){code[0] = "Total";} //metadata.variables.get(varNo).getTotalCode();}
        }else{
            code[0] = "Error";
        }
        return code[0]; 
    } 
   
    public static int processAprioryFile(String fileName, int tableNumber, String separator,
                                     boolean ignoreError, boolean expandBogus, boolean report, int[][] aPrioryStatus) throws ArgusException{
        String regel, apType; int i, j, n, nMax, oldStatus, newStatus, varNo;
        double x1,x2; boolean oke; String hs, lineInfo;
        double[] CellResp = new double[1];
        int[] CellStat = new int[1];
        double[] CellLower = new double[1];
        double[] CellUpper = new double[1];
        double[] CellCost = new double[1];
        double[] CellKey = new double[1];
        double[] CellKeyNoZeros = new double[1];
        TableSet tableSet = TableService.getTable(tableNumber);
        String[] codes = new String[tableSet.expVar.size()]; String codesString;
        int[] dimIndex = new int[tableSet.expVar.size()];
        int[][] bogusRange = new int[tableSet.expVar.size()][2];
        BufferedReader in;
        int AP_SAFE_MANUAL = CellStatus.SAFE_MANUAL.getValue();
        int AP_PROTECT =  CellStatus.PROTECT_MANUAL.getValue();
        int AP_UNSAFE_MANUAL = CellStatus.UNSAFE_MANUAL.getValue();
        int AP_SECONDARY_MANUAL = CellStatus.SECONDARY_UNSAFE_MANUAL.getValue();
        int AP_SECONDARY = CellStatus.SECONDARY_UNSAFE.getValue();
        int AP_ADJUST_COST = -1;
        int AP_ADJUST_PROT_LEVEL = -2;
        int AP_ADJUST_APRIORI_BOUND = -3;
        //boolean fileCompleted = false; Not used ?????
     
        outApriori = null; outStatus = null; outCost = null; outProtL = null;  outBound = null;
        tableSet.historyUsed++;
        lineInfo = "";
        nMax = 0;
        for (i=0;i<5;i++){            // Where does 5 come from?
            for (j=0;j<1;j++){
                aPrioryStatus[i][j] = 0;
            }
        }
             
        for (i=0;i<tableSet.expVar.size();i++){
            n = TauArgusUtils.getNumberOfActiveCodes(tableSet.expVar.get(i).index);
            if (n>nMax){nMax = n;}
        }
        
        //To save memory, making use of Jagged Arrays, i.e. each row can have different number of elements
        //codeList = new String[tableSet.expVar.size()][nMax];
        codeList = new String[tableSet.expVar.size()][]; 
        //codeListLevel  = new int[tableSet.expVar.size()][nMax];
        codeListLevel  = new int[tableSet.expVar.size()][];
        //codeListNChild  = new int[tableSet.expVar.size()][nMax];
        codeListNChild  = new int[tableSet.expVar.size()][];
     
        for (i=0;i<tableSet.expVar.size();i++){ varNo = tableSet.expVar.get(i).index;
            codeList[i] = new String[TauArgusUtils.getNumberOfActiveCodes(varNo)];
            codeListLevel[i] = new int[TauArgusUtils.getNumberOfActiveCodes(varNo)];
            codeListNChild[i] = new int[TauArgusUtils.getNumberOfActiveCodes(varNo)];
            n = -1;
            for (j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                codeList[i][j] = TauArgusUtils.getCode(varNo, j).trim();
                tauArgus.GetVarCodeProperties(varNo, j, isParent, isActive, isMissing, level, nChild, code);
                if (isActive[0] == 1){
                    n++;
                    codeList[i][n] = code[0].trim();
                    codeListLevel[i][n] = level[0];
                    codeListNChild[i][n] = nChild[0];
                } 
            }
        }

        try {in = new BufferedReader(new FileReader(fileName));}
        catch (FileNotFoundException ex){ throw new ArgusException("Apriory file "+fileName+" could not be found.");}
        Tokenizer tokenizer = new Tokenizer(in);
        // if report = false then no reporting is done as the procedure is used for setting secondaries in linked modular
        // The number of real secondaries is returned via aPrioryStatus[1][0];

        try {
            outApriori = new BufferedWriter(new FileWriter(Application.getTempFile("Apriori" + tableNumber + ".htm"), true));  
            outStatus  = new BufferedWriter(new FileWriter(Application.getTempFile("HistStat" + tableNumber + ".htm"), true));
            outCost    = new BufferedWriter(new FileWriter(Application.getTempFile("HistCost" + tableNumber + ".htm"), true));
            outProtL   = new BufferedWriter(new FileWriter(Application.getTempFile("HistPL" + tableNumber + ".htm"), true));
            outBound   = new BufferedWriter(new FileWriter(Application.getTempFile("HistAB" + tableNumber + ".htm"), true));
     
            if (report){
                outApriori.write(fileName); outApriori.newLine();
                outStatus.write("<h2>FILE " + fileName+ "</h2>"); outStatus.newLine();
                if (expandBogus)outStatus.write("<h2>Bogus expansion has been applied</h2>"); outStatus.newLine();
                
                outStatus.write("<h2>Status changes</h2>"); outStatus.newLine();      
                outStatus.write("<table><tr><td><b>Codes</b></td><td><b>Old status</b></td><td><b>New status</b></td><td><b>Result</b></td></tr>");outStatus.newLine();
       
                outCost.write("<h2>Cost function changes</h2>"); outCost.newLine();
                outCost.write("<table><tr><td><b>Codes</b></td><td><b>Old cost</b></td><td><b>New cost</b></td><td><b>Result</b></td></tr>");outCost.newLine();
                
                outProtL.write("<h2>Protection level changes</h2>"); outCost.newLine();
                outProtL.write("<table><tr><td><b>Codes</b></td><td><b>Old protection level</b></td><td><b>New protection level</b></td><td><b>Result</b></td></tr>");outProtL.newLine();
       
                outBound.write("<h2>Apriori bound changes</h2>"); outBound.newLine();
                outBound.write("<table><tr><td><b>Codes</b></td><td><b>Old apriory bound</b></td><td><b>New apriory bound</b></td><td><b>Result</b></td></tr>");outBound.newLine();
            }

            // Start processing the file
            boolean firstLine = true;
            while ((tokenizer.nextLine()) != null) {
                regel = tokenizer.getLine();  
                if (firstLine) { //test for a correct separator
                    if (!regel.contains(separator)){
                        throw new ArgusException("separator ("+separator+") not found in file "+fileName + "\n"+ 
                                                 "First line: "+ regel);
                    }
                    firstLine = false;
                }
                //Note I used to replace tabs by spaces.
                //First get the codes
                oke = true; codesString = "";
                for (i=0;i<tableSet.expVar.size();i++){
                    codes[i] = tokenizer.nextField(separator).trim();
                    codesString = codesString + codes[i]+";";
                    //dimIndex[i] = -1;
                    //varNo = tableSet.expVar.get(i).index;
                    //for (j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                    //    if (codes[i].equals(codeList[i][j])) {dimIndex[i] = j; break;}
                    //}
                    // Much faster to use buildin search
                    //indexOf returns -1 if not found;
                    dimIndex[i] = Arrays.asList(codeList[i]).indexOf(codes[i]);
                    if (codes[i].equals(tableSet.expVar.get(i).totCode)){dimIndex[i] = 0;}
                    lineInfo =  "Line number "+tokenizer.getLineNumber() + "\n"+ 
                                "Line: " + regel;
                    if (dimIndex[i] == -1 ){
                        if (!ignoreError){
                            hs = "Code number "+(i+1) + " code: "+codes[i]+ " could not be found\n"+
                                     "Apriori file "+ fileName + "\n";
                            if (!showError(hs, lineInfo)){return 1;}
                        } 
                        oke = false; codesString = codesString+"(incorrect)";
                    }
                }
                if (oke){ aPrioryStatus[0][0]++; }
                else    { aPrioryStatus[0][1]++; }

                apType = ""; newStatus = 0;
                x1=0; x2=0;
                if (oke){ //find the correct apriory transition
                    apType = tokenizer.nextField(separator).toUpperCase();
                    try{
                        switch(apType) {
                            case "S": newStatus = AP_SAFE_MANUAL; break;
                            case "U": newStatus = AP_UNSAFE_MANUAL; break;
                            case "P": newStatus = AP_PROTECT; break;
                            case "M": newStatus = AP_SECONDARY_MANUAL;  break;
                            case "ML": newStatus = AP_SECONDARY; break;
                            case "C":
                            case "W": newStatus = AP_ADJUST_COST;
                                    x1 = StrUtils.toDouble(tokenizer.nextField(separator));
                                    break;
                            case "AB": newStatus = AP_ADJUST_APRIORI_BOUND;
                                    x1 = StrUtils.toDouble(tokenizer.nextField(separator));
                                    x2 = StrUtils.toDouble(tokenizer.nextField(separator)); break;
                            case "PL": newStatus = AP_ADJUST_PROT_LEVEL;
                                    x1 = StrUtils.toDouble(tokenizer.nextField(separator));
                                    x2 = StrUtils.toDouble(tokenizer.nextField(separator)); break;
                            default: hs = "Illegal apriori command("+apType+") in line " +tokenizer.getLineNumber();
                                    aPrioryStatus[0][1]++; aPrioryStatus[0][0]--; 
                                    if (!ignoreError){           
                                        if (!showError(hs, lineInfo)){return 1;}        
                                    }
                                    oke = false;
                        } //switch
                    } catch(argus.model.ArgusException | ArgusException ex){throw new ArgusException("Error in apriori type " + apType);}     
                } // end if oke-loop
                // processs the information
                if (oke){ 
                //build the loop for the bogus levels
                    int nDim;
                    Boolean toGo;
                    nDim = tableSet.expVar.size();
                    //This for-loop is processed for expandBogus=true as well and then replaced in next for-loop?????
                    //So effectively only processed when expandBogus=false????
                    /*for (i=0;i<nDim;i++){ 
                        bogusRange[i][0] = dimIndex[i];
                        bogusRange[i][1] = dimIndex[i];
                    }*/
                    if (expandBogus){
                        for (i=0;i<nDim;i++){        
                            varNo = tableSet.expVar.get(i).index;
                            bogusRange[i][0] = getBogusMother(i, dimIndex[i]); //varNo
                            bogusRange[i][1] = getBogusChild(i, varNo, dimIndex[i]);
                            dimIndex[i] = bogusRange[i][0];
                        }
                    }
                    else{
                        for (i=0;i<nDim;i++){ 
                            bogusRange[i][0] = dimIndex[i];
                            bogusRange[i][1] = dimIndex[i];
                        }                        
                    }
                    toGo = true;

                    while (toGo){    
                        codesString = "";
                        for (i=0;i<tableSet.expVar.size();i++){        
                            codesString = codesString + getVarCode(tableSet.expVar.get(i).index,dimIndex[i])+ ";";         
                        }  
                        codesString = codesString.substring(0, codesString.length() - 1);
        
                        switch (apType){
                            case "S":
                            case "U":
                            case "P":
                            case "M":
                            case "ML":  //Status change
                                oldStatus = getCellStatus(tableSet, dimIndex);
                                oke = (newStatus == AP_SAFE_MANUAL) &&
                                        (oldStatus >= CellStatus.SAFE.getValue() && oldStatus <= CellStatus.PROTECT_MANUAL.getValue()) ||
                                        (newStatus == AP_UNSAFE_MANUAL) &&
                                        (oldStatus >= CellStatus.SAFE.getValue() && oldStatus <= CellStatus.UNSAFE_MANUAL.getValue()) ||
                                        (newStatus == AP_PROTECT) &&
                                        (oldStatus >= CellStatus.SAFE.getValue() && oldStatus <= CellStatus.SAFE_MANUAL.getValue()) ||
                                        (newStatus == AP_SECONDARY_MANUAL) &&
                                        (oldStatus >= CellStatus.SAFE.getValue() && oldStatus <= CellStatus.SAFE_MANUAL.getValue()) ||
                                        (newStatus == AP_SECONDARY) &&
                                        (oldStatus >= CellStatus.SAFE.getValue() && oldStatus <= CellStatus.SAFE_MANUAL.getValue());
                                if ((newStatus == AP_SECONDARY) && (oldStatus == CellStatus.SAFE_MANUAL.getValue())){newStatus = AP_SECONDARY_MANUAL ;}
                                if (oke) {oke = tauArgus.SetTableCellStatus(tableSet.index, dimIndex, newStatus);}
                                if (oke) { aPrioryStatus[1][0]++; }
                                else     { aPrioryStatus[1][1]++;
                                    if (!ignoreError){
                                        hs = "Illegal status transition for cell ("+codesString + ")\nLine " + tokenizer.getLineNumber();
                                        if (!showError(hs, lineInfo)){return 1;}
                                    }
                                }
                                if (report){
                                    outStatus.write("<tr><td>"+codesString+"</td><td>"+CellStatus.findByValue(oldStatus).getDescription()+
                                                    "</td><td>" +  CellStatus.findByValue(newStatus).getDescription()+ "</td><td>");
                                    if (oke){outStatus.write("&nbsp;");}
                                    else    {outStatus.write("not possible");}
                                    outStatus.write("</td></tr>"); outStatus.newLine();
                                }
                                break;
                            case "C":
                            case "W": //Change cost function
                                if(x1==0){x1=1;} //zero is a silly value
                                oke = (x1 > 0);
                                if (oke) {
                                    getTableCell (tableNumber, dimIndex, CellResp, CellStat, CellLower, CellUpper, CellCost, CellKey, CellKeyNoZeros);
                                    oke =tauArgus.SetTableCellCost(tableSet.index, dimIndex, x1);
                                }
                                if ( oke) { aPrioryStatus[2][0]++; }
                                else      { aPrioryStatus[2][1]++;
                                    if (!ignoreError){
                                        hs = "Illegal new cost value ("+x1+") for cell ("+codesString + ")\nLine " + tokenizer.getLineNumber();
                                        if (!showError(hs, lineInfo)){return 1;}
                                    }
                                }
                                if (report){
                                    outCost.write("<tr><td>"+codesString+"</td><td align=\"Right\">"+StrUtils.formatDouble(CellCost[0],tableSet.respVar.nDecimals)+
                                                    "</td><td align=\"Right\">" +  StrUtils.formatDouble(x1,tableSet.respVar.nDecimals)+ "</td><td>");
                                    if (oke){outCost.write("&nbsp;");}
                                    else    {outCost.write("not possible");}
                                    outCost.write("</td></tr>"); outCost.newLine();
                                }
                                break;
                            case "AB": throw new ArgusException ("Change apriory bounds is not yet possible; was neither in the old TAU-Argus");
                            case "PL": 
                                hs = "";
                                //if ( (x1<0) || (x2 < 0) || (x1+x2<0)) {
                                if ( (x1<0) || (x2<0) ) { // if (x1+x2)<0 then x1<0 and/or x2<0
                                    hs = "Illegal values for the protection levels: "+ x1 + ".. "+x2;
                                    oke = false;
                                }
                                if (oke){
                                    oldStatus = getCellStatus(tableSet, dimIndex);
                                    getTableCell (tableNumber, dimIndex, CellResp, CellStat, CellLower, CellUpper, CellCost, CellKey, CellKeyNoZeros);
                                    if ( (oldStatus < CellStatus.UNSAFE_RULE.getValue())||(oldStatus > CellStatus.UNSAFE_MANUAL.getValue()) ){
                                        oke = false; 
                                        hs = "Protection levels can only be changed for unsafe cells"; 
                                    }
                                }  
                                if (oke){
                                    if (!tauArgus.SetProtectionLevelsForResponseTable(tableSet.index, dimIndex, x1, x2)){
                                        hs = "Unable to change the protection level";
                                        oke = false;
                                    }
                                }
                                if (oke){aPrioryStatus[4][0]++;}
                                else    {
                                    if (!ignoreError){
                                        if (!showError(hs, lineInfo)){return 1;}  
                                    }  
                                    aPrioryStatus[4][1]++;    
                                }
                                if (report){
                                    outProtL.write("<tr><td>"+codesString+"</td><td align=\"Right\">("+StrUtils.formatDouble(CellLower[0],tableSet.respVar.nDecimals) + ","+ 
                                                   StrUtils.formatDouble(CellUpper[0],tableSet.respVar.nDecimals)+ 
                                                   ")</td><td align=\"Right\">(" +  StrUtils.formatDouble(x1,tableSet.respVar.nDecimals)+ ","+ 
                                                   StrUtils.formatDouble(x2,tableSet.respVar.nDecimals)+ ")</td><td>");
                                    if (oke){outProtL.write("&nbsp;");}
                                    else    {outProtL.write("not possible");}
                                    outProtL.write("</td></tr>"); outProtL.newLine();
                                }
                                break;
                        }//end switch
                        //increase the loop
                        dimIndex[nDim-1]++;
                        n = nDim-1;
                        while (dimIndex[n]> bogusRange[n][1] ){
                            dimIndex[n] =  bogusRange[n][0];
                            if (n > 0) {n--; dimIndex[n]++;}
                            else toGo = false;
                        }
                    } // end of the ToGo loop 
                } //end of the oke loop (correct codes)
            } //end of apriori file reached
            //fileCompleted = true; Not used ?????
            if (report) {
                outStatus.write("</table>"); outStatus.newLine();outStatus.write("<EOF>   ");outStatus.newLine();
                outCost.write("</table>"); outCost.newLine();    outCost.write  ("<EOF>   ");outCost.newLine();
                outProtL.write("</table>"); outProtL.newLine();  outProtL.write ("<EOF>   ");outProtL.newLine();
                outBound.write("</table>"); outBound.newLine();  outBound.write ("<EOF>   ");outBound.newLine();
                if (expandBogus) {outApriori.write("Trivial levels have been expanded");}
                else{outApriori.write("Trivial levels have not been expanded");}
                outApriori.newLine();
                for (i=0;i<=4;i++){
                    outApriori.write(aPrioryStatus[i][0]+ ";"+ aPrioryStatus[i][1]+ ";");  
                }
                outApriori.newLine();
                outProtL.close(); outStatus.close(); outCost.close(); outBound.close(); outApriori.close(); // Added PWOF 25-3-2014
            }
            tokenizer.close();
            SystemUtils.writeLogbook("Apriory file: " + fileName+ " has been applied\nto table: "+
                                        TableService.getTable(tableNumber).toString());
            if (!report) tableSet.historyUsed--;
            return 0;
        }
        catch (IOException ex){ throw new ArgusException("An unexpected error occurred when writing the report files for " +
                                       "the apriory file ("+ fileName + ").");
        }
    }
     
    static private  boolean showError(String errorMessage, String lineInfo)throws ArgusException{
        if(Application.isBatch()){
            throw new ArgusException(errorMessage+ lineInfo);  
        }else{
            DialogErrorApriori dialog = new DialogErrorApriori(null, true);
            dialog.SetaprioriErrorMessage(errorMessage + "\n\n"+ lineInfo);
            dialog.initWindow();
            dialog.setVisible(true);
            return dialog.GetaprioriErrorContinue();
        }
    }
   
    public static boolean CloseAprioriFiles(boolean expandBogus, int[][] aPrioryStatus){
        try{   
            outStatus.write("</table>"); outStatus.newLine();outStatus.write("<EOF>   ");outStatus.newLine();
            outCost.write("</table>"); outCost.newLine();    outCost.write  ("<EOF>   ");outCost.newLine();
            outProtL.write("</table>"); outProtL.newLine();  outProtL.write ("<EOF>   ");outProtL.newLine();
            outBound.write("</table>"); outBound.newLine();  outBound.write ("<EOF>   ");outBound.newLine();
            if (expandBogus) {outApriori.write("trivial levels have been expanded");}
            else{outApriori.write("trivial levels have not been expanded");}
            outApriori.newLine();
            for (int i=0;i<=4;i++){
                outApriori.write(aPrioryStatus[i][0]+ ";"+ aPrioryStatus[i][1]+ ";");  
            }
            outApriori.newLine();
            outProtL.close(); outStatus.close(); outCost.close(); outBound.close(); outApriori.close(); // Added PWOF 25-3-2014
            return true;
        }
        catch (IOException ex){}
        return false;
    }

    public static void MakeAdditErrorList(int tableIndex){
        int deci; String hs, hs1, regel; 
        double xTot,xSub, epsilon, upl;
        double[] x = new double[1]; 
        String spaces = "                            ";
        int i, n, cn, p, nRel; 
        Boolean oke;
        epsilon = 1.0E-14; upl = 2;
        //There is something open for incomplete/linked tables
        final String Streep = "--------------------------------------------------";
        TableSet tableSet = TableService.getTable(tableIndex);
        deci = tableSet.respVar.nDecimals;
        try{
            SaveTable.writeJJ(tableSet, Application.getTempFile("AddErr.JJ"), false, false, 1, false, false);
        }catch (ArgusException ex){}
       
        try(BufferedWriter out = new BufferedWriter(new FileWriter(Application.getTempFile("AdditErr.txt")));){
            BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("AddErr.JJ")));
            hs = in.readLine(); // skip first line
            hs = in.readLine();
            n=0;
            try {
                n = StrUtils.toInteger(hs);
            }catch(argus.model.ArgusException ex){}
            
            for (i=0;i<n;i++){hs = in.readLine();} // skip n lines
            hs = in.readLine(); 
            try{
                nRel = StrUtils.toInteger(hs);
            }catch(argus.model.ArgusException ex){}
            
            out.write("Overview of the non-additive cells"); out.newLine();
            out.write(Streep);  out.newLine();
            out.write("Table-file: "+ tableSet.metadata.dataFile);  out.newLine();
            out.write("Meta-file:  " + tableSet.metadata.metaFile);  out.newLine();
            Date date = new Date();
            hs = date.toString();
            out.write("Date: " + hs);  out.newLine();
            out.write(Streep);  out.newLine();
            while ((hs = in.readLine()) != null){   //0 4 : 0 (-1) 5 (1) 10 (1) 15 (1)
                p = hs.indexOf(":");
                try{
                    n = StrUtils.toInteger(hs.substring(1,p));
                }catch(argus.model.ArgusException ex){}
                
                hs = hs.substring(p+1);
                hs1 = hs;
                p = hs.indexOf("(");
                cn = 0;
                try{
                    cn = StrUtils.toInteger(hs.substring(0,p));
                }catch(argus.model.ArgusException ex){}
                
                tauArgus.GetTableCellValue(tableIndex, cn, x);
                xTot = x[0]; xSub = 0;
                for (i=1;i<n;i++){
                    hs1 = hs1.substring(p+4);
                    p = hs1.indexOf("(");
                    try{
                        cn = StrUtils.toInteger(hs1.substring(0,p));
                    }catch(argus.model.ArgusException ex){}
                    
                    tauArgus.GetTableCellValue(tableIndex, cn, x);
                    xSub = xSub + x[0];
                }
            // Differs from test in TauArgusJava.dll !!!!!!
            // oke = (Math.abs(xTot) <= 0.00001);
            // Math.ulp(1d) is machine precision in Java for double
 //           oke = (Math.abs(xTot) < Math.ulp(1d)*Math.abs(xTot)*2);
//            std::abs(x - y) < std::numeric_limits<double>::epsilon() * std::abs(x + y) * ulp 
//                              || std::abs(x - y) < epsilon            // in case x = y = 0
                oke = ((Math.abs(xTot - xSub) < epsilon * Math.abs(xTot + xSub) * upl) || (Math.abs(xTot - xSub) < epsilon));
            
                if (!oke){ //write an entry
                    p = hs.indexOf("(");
                    try{
                        cn = StrUtils.toInteger(hs.substring(0,p));
                    }catch(argus.model.ArgusException ex){}
                    
                    tauArgus.GetTableCellValue(tableIndex, cn, x);
                    regel = String.format(Locale.US, "%."+deci+"f", x[0]);
                    regel = spaces.substring(0, 19-regel.length())+ regel;
                    regel = regel  + "     Total: " + TableService.IndexToCodesString(tableSet, cn);
                    out.write(regel); out.newLine();
                    for (i=1;i<n;i++){
                        hs = hs.substring(p+4);
                        p = hs.indexOf("(");
                        try{
                            cn = StrUtils.toInteger(hs.substring(0,p));
                        }catch(argus.model.ArgusException ex){}
                        
                        tauArgus.GetTableCellValue(tableIndex, cn, x);
                        regel = String.format(Locale.US, "%."+deci+"f", x[0]);
                        regel = spaces.substring(0, 19-regel.length())+ regel;
                        regel = regel  + "  sub-cell: " + TableService.IndexToCodesString(tableSet, cn);
                        out.write(regel); out.newLine();
                    }
                    out.newLine();
                    regel = String.format(Locale.US, "%."+deci+"f", xTot-xSub);
                    regel = spaces.substring(0,19-regel.length())+ regel;
                    out.write(regel + "  Difference"); out.newLine();
                    out.write(Streep);  out.newLine();
                }
            }
            out.close();
        }catch (IOException ex){}
 }

    public static long computeMinRoundBase(TableSet table) {
        long minRoundBase;
        if (table.respVar == Application.getFreqVar()) {
            minRoundBase = table.minFreq[table.holding ? 1 : 0];
        }
        else {
            minRoundBase = (long)tauArgus.MaximumProtectionLevel(table.index);
        }
        if (minRoundBase < 1) minRoundBase = 1;
        return minRoundBase;
    }
    
/* Never used ???? And Empty function anyway...
    public static void writeJJ(TableSet tableSet, String fileName, boolean forRounding,
                               boolean singleton, int minFreq, boolean withBogusRemoval)throws ArgusException{
    }
*/
    public void CalculateCKMInfo(){
        CKMStatistics.clear();
        for (int i=this.minDiff; i<=this.maxDiff; i++){
            CKMStatistics.put(i, 0L);
        }
        nEmpty = 0L;
        
        int nExpVar = expVar.size();
        int[] maxDim = new int[nExpVar];
        int[] dimArray = new int[nExpVar];

        // including empty cells
        for (int i = 0; i < nExpVar; i++) {
            dimArray[i] = 0;
            maxDim[i] = TauArgusUtils.getNumberOfActiveCodes(expVar.get(i).index);
        }        
        
        for (;;) {
            Cell cell = getCell(dimArray);
                if (cell.status == CellStatus.EMPTY) {
                    nEmpty++;
                }else{
                    int Diff = (int) Math.round(cell.CKMValue-cell.response);
                    CKMStatistics.put(Diff, CKMStatistics.get(Diff)+1);
                }
 
            // adjust dimArray
            int k = nExpVar;
            while (k-- > 0) {
                dimArray[k]++;
                if (dimArray[k] < maxDim[k]) { break; }
                else { dimArray[k] = 0; }
            }
            if (k == -1) { break; }
        }
        
        // remove info of differences that are not present in the table 
        // i.e. remove entries with CKMStatistics(Diff) == 0
        for (Iterator<Map.Entry<Integer,Long>> it = CKMStatistics.entrySet().iterator();it.hasNext();){
            Map.Entry<Integer,Long> entry = it.next();
            if (entry.getValue()==0) it.remove();
        }
    }
    
    // Calculates mean 
    public void CalculateCKMInfoLoss(){
        final double[] percs = new double[]{0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.95,0.99};
        final double[] RADDRbreaks = new double[]{0.0,0.02,0.05,0.1,0.2,0.3,0.4,0.5,1.0};
        final double[] ADbreaks;
        int nFalseZeros = 0;
        int nFalseNonzeros = 0;
        int nExpVar = expVar.size();
        int[] maxDim = new int[nExpVar];
        int[] dimArray = new int[nExpVar];
        
        double[] AD = new double[this.numberOfCells()];
        double[] RAD = new double[this.numberOfCells()];
        double[] DR = new double[this.numberOfCells()];
        
        // including empty cells
        for (int i = 0; i < nExpVar; i++) {
            dimArray[i] = 0;
            maxDim[i] = TauArgusUtils.getNumberOfActiveCodes(expVar.get(i).index);
        }        
        
        int j=0;
        for (;;) {
            Cell cell = getCell(dimArray);
            if ((Math.abs(cell.CKMValue) <= EPSILON) && (Math.abs(cell.response) > EPSILON)) nFalseZeros++;
            if ((Math.abs(cell.response) <= EPSILON) && (Math.abs(cell.CKMValue) > EPSILON)) nFalseNonzeros++;
            
            AD[j] = Math.abs(cell.CKMValue - cell.response);
            // If cell is empty, it is not perturbed by default in frequency count tables, using standard CKM
            if (cell.status==CellStatus.EMPTY) RAD[j] = 0;
            else {
                if (Math.abs(cell.response) <= EPSILON){ // original cell value is (close to) zero
                    RAD[j] = Math.abs(cell.CKMValue - cell.response)/EPSILON;
                }
                else RAD[j] = Math.abs(cell.CKMValue - cell.response)/cell.response;
            }
            DR[j] = Math.abs(Math.sqrt(cell.CKMValue) - Math.sqrt(cell.response));
            j++;
            // adjust dimArray 
            int k = nExpVar;
            while (k-- > 0) {
                dimArray[k]++;
                if (dimArray[k] < maxDim[k]) { break; }
                else { dimArray[k] = 0; }
            }
            if (k == -1) { break; }            
        }
        
        this.InfoLoss.SetNumberOfCells(this.numberOfCells());
        this.InfoLoss.SetNumberOfEmpty((int) this.numberOfEmpty());
        this.InfoLoss.SetFalseZeros(nFalseZeros);
        this.InfoLoss.SetFalseNonzeros(nFalseNonzeros);
       
        this.InfoLoss.SetMean("AD",DoubleStream.of(AD).average().getAsDouble());
        this.InfoLoss.SetMean("RAD",DoubleStream.of(RAD).average().getAsDouble());
        this.InfoLoss.SetMean("DR",DoubleStream.of(DR).average().getAsDouble());
        
        double multiplier = ((double) this.numberOfCells())/(this.numberOfCells() - this.numberOfEmpty());
        this.InfoLoss.SetMean("ADnonempty",this.InfoLoss.GetMean("AD")*multiplier);
        this.InfoLoss.SetMean("RADnonempty",this.InfoLoss.GetMean("RAD")*multiplier);
        this.InfoLoss.SetMean("DRnonempty",this.InfoLoss.GetMean("DR")*multiplier);

        // Sort the values
        Arrays.sort(AD);
        Arrays.sort(RAD);
        Arrays.sort(DR);
        InfoLoss.setDiffs("AD", AD);
        InfoLoss.setDiffs("RAD", RAD);
        InfoLoss.setDiffs("DR", DR);
        
        // Calculations below this line make use of fact that 
        // AD, RAD and DA are sorted
        
        if (this.isFrequencyTable()){
            int ADbreakssize = Math.max(Math.abs(this.maxDiff),Math.abs(this.minDiff)) + 1;
            ADbreaks = new double[ADbreakssize];
            for (int i=0; i<ADbreakssize; i++) ADbreaks[i] = (double)i;
        }
        else{
            ADbreaks = new double[]{0.0,1.0,2.0,3.0,4.0,5.0};// Possibly different in case of continuous variables?
        }
        
        this.InfoLoss.SetECDFcounts("AD", ECDFcounts(AD,ADbreaks));
        this.InfoLoss.SetECDFcounts("RAD", ECDFcounts(RAD,RADDRbreaks));
        this.InfoLoss.SetECDFcounts("DR", ECDFcounts(DR,RADDRbreaks));
        
        this.InfoLoss.SetMaxs("AD",AD[AD.length-1]);
        this.InfoLoss.SetMaxs("RAD",RAD[RAD.length-1]);
        this.InfoLoss.SetMaxs("DR",DR[DR.length-1]);
        this.InfoLoss.SetMaxs("ADnonempty",AD[AD.length-1]);
        this.InfoLoss.SetMaxs("RADnonempty",RAD[RAD.length-1]);
        this.InfoLoss.SetMaxs("DRnonempty",DR[DR.length-1]);

        this.InfoLoss.SetMins("AD",AD[0]);
        this.InfoLoss.SetMins("RAD",RAD[0]);
        this.InfoLoss.SetMins("DR",DR[0]);
        // First this.numberOfEmpty() values are zero because of empty cells
        this.InfoLoss.SetMins("ADnonempty",AD[(int) this.numberOfEmpty()]);
        this.InfoLoss.SetMins("RADnonempty",RAD[(int) this.numberOfEmpty()]);
        this.InfoLoss.SetMins("DRnonempty",DR[(int) this.numberOfEmpty()]);
        
        this.InfoLoss.SetMedian("AD",percentiles(AD,0.5)[0]);
        this.InfoLoss.SetMedian("RAD",percentiles(RAD,0.5)[0]);
        this.InfoLoss.SetMedian("DR",percentiles(DR,0.5)[0]);
        this.InfoLoss.SetMedian("ADnonempty",percentiles(Arrays.copyOfRange(AD, (int) this.numberOfEmpty(), AD.length),0.5)[0]);
        this.InfoLoss.SetMedian("RADnonempty",percentiles(Arrays.copyOfRange(RAD, (int) this.numberOfEmpty(), AD.length),0.5)[0]);
        this.InfoLoss.SetMedian("DRnonempty",percentiles(Arrays.copyOfRange(DR, (int) this.numberOfEmpty(), AD.length),0.5)[0]);
        
        this.InfoLoss.SetPercentiles("AD",percentiles(AD,percs));
        this.InfoLoss.SetPercentiles("RAD",percentiles(RAD,percs));
        this.InfoLoss.SetPercentiles("DR",percentiles(DR,percs));
        this.InfoLoss.SetPercentiles("ADnonempty",percentiles(Arrays.copyOfRange(AD, (int) this.numberOfEmpty(), AD.length),percs));
        this.InfoLoss.SetPercentiles("RADnonempty",percentiles(Arrays.copyOfRange(RAD, (int) this.numberOfEmpty(), AD.length),percs));
        this.InfoLoss.SetPercentiles("DRnonempty",percentiles(Arrays.copyOfRange(DR, (int) this.numberOfEmpty(), AD.length),percs));
        
    }
    
    public CKMInfoLoss GetCKMInfoLoss(){
        return this.InfoLoss;
    }
    
    // calculates percentiles of sorted double array
    // perc is vector of values between 0 and 1
    private double[] percentiles(double[] data, double... perc){
        assert data.length > 0;
        double pos;
        double d;
        double[] values = new double[perc.length];
        for (int i=0; i<perc.length;i++){
            pos = perc[i]*(data.length + 1);
            if ((data.length == 1)||(pos < 1)) values[i] = data[0];
            else{
                if (pos >= data.length) values[i] = data[data.length-1];
                else{
                    d = pos - Math.floor(pos);
                    values[i] = (1.0-d)*data[(int)Math.floor(pos)-1] + d*data[(int)Math.floor(pos)];
                }
            }
        }
        return values;
    }

    // Returns number of cells <= threshold
    // arr[] is sorted array of data
    // breaks is a vector of thresholds
    // Essentially this returns the Empirical Cumulative Distribution Function counts
    private ECDF ECDFcounts(double arr[], double... breaks){
        int[] values = new int[breaks.length];
        int pos = 0;
        int ndata = arr.length;
        int nbreaks = breaks.length;
        
        for (int i=0; i<nbreaks; i++){
            if (pos < (ndata - 1)){
                pos = last(arr, pos, ndata - 1, breaks[i], ndata);
                values[i] = pos + 1;
            }
            else values[i] = values[i-1];
        }
        ECDF ecdf = new ECDF(nbreaks);
        ecdf.setBreaks(breaks);
        ecdf.setCounts(values);
        return ecdf;
    }
    
    // returns the index of LAST occurrence of a value <= x in a SORTED arr[0..n-1]
    // between index low and index high
    private static int last(double arr[], int low, int high, double x, int n) 
    { 
        if ((high >= low) && (x >= arr[0]))
        { 
            int mid = low + (high - low)/2;
            //if (( mid == n-1 || x < arr[mid+1]) && Math.abs(arr[mid] - x) <= EPSILON) 
            if (( mid == n-1 || x < arr[mid+1]) && arr[mid]<=x) 
                return mid; 
            else if (x < arr[mid]) 
                return last(arr, low, (mid -1), x, n); 
            else
                return last(arr, (mid + 1), high, x, n); 
        } 
        return -1; 
    } 
    
    /* if x is present in arr[] then returns the index of 
    FIRST occurrence of x in arr[0..n-1], otherwise 
    returns -1 */
    private static int first(double arr[], int low, int high, double x, int n) 
    { 
        if(high >= low) 
        { 
            int mid = low + (high - low)/2; 
            if( ( mid == 0 || x > arr[mid-1]) && Math.abs(arr[mid] - x) <= EPSILON) 
                return mid; 
             else if(x > arr[mid]) 
                return first(arr, (mid + 1), high, x, n); 
            else
                return first(arr, low, (mid -1), x, n); 
        } 
        return -1; 
    } 
}
