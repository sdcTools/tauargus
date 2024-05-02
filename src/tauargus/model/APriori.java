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
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import tauargus.extern.dataengine.TauArgus;
import tauargus.service.TableService;
import tauargus.utils.TauArgusUtils;
import tauargus.utils.Tokenizer;

/**
 *
 * @author whcg
 */
public class APriori {
    /*!
        The class contains N coderanges \f$ C_1, ..., C_N \f$
        This class iterates over the cartesian product of their values
        \f$ C_1 \times ... \times C_N\f$.

    */
    public class CartesianProductIterator
    {
        List<int[]> codeRange = new ArrayList<>();
        int[] codeCntr ;
        int LinearCntr = 0;
        Integer currCntr = 0;
        
        public CartesianProductIterator(List<int[]> cr)
        {
            codeRange = cr;
            codeCntr = new int[cr.size()];
        }
        /*!
            Start at the first combination of values
        */
        public void Reset()
        {
            LinearCntr = 0;
            
            for(int i=0; i<codeCntr.length; i++)
                codeCntr[i] = 0;
        }
        /*!
            Have all values been iterated?
        */
        public boolean EOR()
        {
            if( currCntr >= codeCntr.length)
                return true;
            
            if( currCntr == codeCntr.length-1 )
                if( codeCntr[currCntr]>=codeRange.get(currCntr).length)
                    return true;
            
            return false;
        }
        //  coderange:
        //     0: 13, 14
        //     1: 2,3,4
        /*!
            Print the the current code values inside the iterator
        */        
        public String Currpos()
        {
            String s = LinearCntr+":";
            for(int i=0; i<codeCntr.length; i++)
                s+=codeCntr[i]+",";
            
            return s;
        }
        /*!
            Goto the next value.
            A bit like counting in a N-ary system
        */        
        public int[] Next()
        {
            LinearCntr++;
            //
            // get next counter value
            //
            if( codeCntr[currCntr] < codeRange.get(currCntr).length-1)
            {
                codeCntr[currCntr]++;
            }
            else
            {
                // look for the next digit to increase
                while(currCntr < codeCntr.length && 
                        codeCntr[currCntr]>=codeRange.get(currCntr).length-1)
                    currCntr++;
                // going a bit like from 99 --> 100 
                // reset alle lower counters, e.g.  9 --> 0
                // increase the next digit: 0 --> 1
                if(!EOR() )
                {
                    for(int i=0; i<currCntr; i++)
                        codeCntr[i] = 0;

                    codeCntr[currCntr]++;
                    
                    currCntr=0;
                }
            }
            //
            // fill return value
            //
            return Current();
        }
        
        /*!
            The current code values inside the iterator
        */        
        int[] Current()
        {
            int[] res = new int[codeCntr.length];
            
            if(!EOR())
            {
            for(int i=0; i<codeCntr.length; i++)
                res[i] = codeRange.get(i)[codeCntr[i]];
            }
            
            return res;
        }
    }
    
    List<APrioriCell> Cells = new ArrayList<>();
    
    /*!
           The mapping of the columns from the input safe file
           e.g. (A,B) becomes (Other,A)
           then ColumnMap becomes
           (1,0)                       --> column 1 will be another variable
           (2,1)                       --> column 2 uses A 
     */
    public Map<Integer,Integer> ColumnMap = new HashMap<>();
    
    public int SelectedColumnsCount = 0;
    
    public List<String> VarNames = new ArrayList<>();
    
    /*!
        The new status values
    */
    public enum ChangeStatus { 
        Omit    (0, 'o'), 
        Safe    (1, 's'), 
        Unsafe  (2, 'u'), 
        Protect (3, 'p');
            
        private final int _idx;
        private final char _symbol;
            
        private ChangeStatus(int idx, char s) {
            _idx=idx;
            _symbol = s;
        }
        
        public  int getIndex()
        {
            return _idx;
        }
        
        public char getSymbol()
        {
            return _symbol;
        }
        
        public static ChangeStatus findByValue(int value) 
        {
            for(ChangeStatus cs : values())
            {
                if( cs._idx==value)
                    return cs;
            }
            return null;
        }
        
        public static ChangeStatus findByValue(char value) 
        {
            for(ChangeStatus cs : values())
            {
                if( cs._symbol==value)
                    return cs;
            }
            return null;
        }
    }

    /*!
        \brief Mapping per Cell Status
        This mapping corresponds with the mapping as defined in DialogAPriori
     */
    public class Mapping {
        public CellStatus oldstat;      //< The status as defined by TauArgus
        public ChangeStatus newstat;    //< The new ChangeStatus for that oldstat 
        public boolean useCost;         //< if Cost was checked in DialogApriori 
        public Double costValue;        //< when useCost is true, use this value as the new cost
        
        public Mapping(CellStatus oldstat)
        {
            this.oldstat = oldstat;
            newstat = ChangeStatus.Omit;
            useCost = false;
            costValue = 1.0;
        }
                
        public void WriteToRegistry(String root)
        {
            SystemUtils.putRegString(root, oldstat.toString().toLowerCase()+"_newstat", Character.toString( newstat.getSymbol()));
            
            if( useCost)
                SystemUtils.putRegDouble(root, oldstat.toString().toLowerCase()+"_cost"   , costValue);
            else
                SystemUtils.removeRegKey(root, oldstat.toString().toLowerCase()+"_cost");
        }
        
        public void ReadFromRegistry(String root)
        {
            String newStatusSymbol = SystemUtils.getRegString(root, oldstat.toString().toLowerCase()+"_newstat", "o");
            newstat = ChangeStatus.findByValue(newStatusSymbol.charAt(0));
            
            Double cost= SystemUtils.getRegDouble(root, oldstat.toString().toLowerCase()+"_cost"   , -1.0);
            if( cost>0.0 )
            {
                useCost = true;
                costValue = cost;
            }
        }
    }
    
    public  ArrayList<Mapping> Mappings = new ArrayList<>();
    
    
    public ChangeStatus GetChangeStatus(CellStatus status)
    {
        return Mappings.get(status.getValue()).newstat;
    }

    
    public  void WriteMappingsToRegistry()
    {
        for(Mapping m : Mappings)
            m.WriteToRegistry("apriorimapping");
    }
    
    public  void ReadMappingsFromRegistry()
    {
        for(Mapping m : Mappings)
            m.ReadFromRegistry("apriorimapping");
    }
    
  
    public void InitColumnMap(int Number)
    {
        SelectedColumnsCount = Number;
        
        ColumnMap.clear();
        for(int i=1; i<=Number; i++)
            ColumnMap.put(i, 0);
    }
    
    /*!
        Where should the 
    */
    public int VariableMappedAtOutputColumn(int VarNo)
    {
        for (Map.Entry<Integer, Integer> entry : ColumnMap.entrySet())
        {
            if( entry.getValue()== VarNo)
                return entry.getKey();
        }
        
        return -1;
    }
    
    public String FileName;     //< The filename of the APriori file
    
    public APriori()
    {
        for (CellStatus cellStatus : CellStatus.values()) 
            Mappings.add(new Mapping(cellStatus));
    }
    
    public APriori(String FileName)
    {
        this.FileName = FileName;
        
        for (CellStatus cellStatus : CellStatus.values()) 
            Mappings.add(new Mapping(cellStatus));
    }
    
    /*!
        Read a saved table
        Assuming that the data has been saved by SaveTable.writeTable 
        using option TableSet.FILE_FORMAT_CODE_VALUE.
        Possible variations:
        1. Variables values may have quotes
        2. status value *may* be present
        3. the first line may contain the variable names (if 1  = true, these are all quoted)
    */
    private boolean IsHeaderLine(String line)
    {
        Pattern reIdentifier = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]+");
        boolean AllVars = true;
        for(String w: line.split(","))
        {   
            w = w.trim().toLowerCase();
            
            if( w.charAt(0)=='\"' && w.charAt(w.length()-1)=='\"'  )
                w = w.substring(1, w.length()-1);

            if( !reIdentifier.matcher(w).matches())
               AllVars = false;
            else
                VarNames.add(w);
        }
        
        return AllVars;
    }

    private int GetFirstNumericColumn(String line)
    {
        int wc = -1;
        for(String w: line.split(","))
        {   
            wc++;

            try
            {
                Double.valueOf(w);
                return wc;
            }
            catch(NumberFormatException nfe) {}
        }
        
        return -1;  // no numeric field found....
    }
    
    public boolean AllCellsHaveAStatus()
    {
        for(APrioriCell c: Cells)
        {
            if( c.Status== CellStatus.UNKNOWN)
                return false;
        }
        
        return true;
    }
    
    public void ReadSafeFile(String safeFileName, String sep) throws ArgusException
    {
        try {
            File f = new File(safeFileName) ;
            Scanner scan = new Scanner(f);
            
            String line = scan.nextLine();

            // check if we start with a variable line: just identifier strings
            if(IsHeaderLine(line)){
                line = scan.nextLine();
            }
            else{ // No varnames on first line, so use "default"-naming
                for (int i=0; i<VarNames.size();i++)
                VarNames.set(i, "ExpVar"+ Integer.toString(i+1));
            }

          
            // at first real line, determine ceel values column = first numeric column
            int FirstNumericField = GetFirstNumericColumn(line);
            if( FirstNumericField<0)
                throw new ArgusException("No numeric field found in safe file '"+safeFileName+"'");
            
            Cells.clear();
            Cells.add(new APrioriCell(line, sep, FirstNumericField ,this)); // Add first encountered cell

            while (scan.hasNextLine()) // Continue with rest
            {                
                line = scan.nextLine();
                Cells.add(new APrioriCell(line, sep, FirstNumericField ,this));
            }
        } 
        catch (FileNotFoundException ex) {
            Logger.getLogger(APriori.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int getDimension()
    {
        if( Cells.size()>0)
            return Cells.get(0).Key.size();
        
        return 0;
    }

    /*!
      Writes the APriori file to disk
      
      \param aprioriFileName   the file name
      \param sep               the separator to be used when writing the file
     */
    public void WriteAprioriFile(String aprioriFileName, String sep, int outDim)
    { 
        try (BufferedWriter w = new BufferedWriter(new FileWriter(aprioriFileName));){
            for(APrioriCell c : Cells)
            {
                if( c.MustBeIncluded())
                {
                    w.write(c.Output(sep,outDim)); w.newLine();                  
                }
            }
        
            w.close();
            
        } catch (IOException ex) {
            Logger.getLogger(APriori.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    //! Just passing through ...
    public int ProcessAprioriFile(int tableNumber, String separator, 
                                  boolean ignoreError, boolean expandBogus, boolean report, int[][] aPrioryStatus) throws ArgusException
    {
        int result = processAprioryFile(FileName, tableNumber, separator, 
                                            ignoreError, expandBogus, report, aPrioryStatus);
        
        return result;
    }
    
    /*!
        The apriorifile may contain
    */
    private int NumberOfVarsInAprioryFile(String s, String sep)
    {
        String[] parts = s.split(sep);
        
        String lastValue = parts[parts.length-1];
        
        int NoOfFields = parts.length-1;
        
        boolean lastFieldIsANumber = true;
        try
        { 
            Double.valueOf(lastValue) ; 
        }
        catch( NumberFormatException e) 
        {
            lastFieldIsANumber = false;
        }
        
        if( lastFieldIsANumber)
            NoOfFields--;
        
        return NoOfFields;
    }
   
    BufferedWriter outCodes, outStatus, outCost, outBound;
    TauArgus tauArgus = Application.getTauArgusDll();
     
    int AP_SAFE_MANUAL = CellStatus.SAFE_MANUAL.getValue();
    int AP_PROTECT_MANUAL =  CellStatus.PROTECT_MANUAL.getValue();
    int AP_UNSAFE_MANUAL = CellStatus.UNSAFE_MANUAL.getValue();
    int AP_SECONDARY_UNSAFE_MANUAL = CellStatus.SECONDARY_UNSAFE_MANUAL.getValue();
    int AP_SECONDARY_UNSAFE = CellStatus.SECONDARY_UNSAFE.getValue();
    int AP_ADJUST_COST = -1;
    int AP_ADJUST_PROT_LEVEL = -2;
    int AP_ADJUST_APRIORI_BOUND = -3;

    // AP_STATUS: [1..4][1..2]
    // 2e index: 1 = goed, 2 = fout
    int AP_STATUS_LINE_READ        = 0;            
    int AP_STATUS_CHANGE_STATUS    = 1;  
    int AP_STATUS_CHANGE_COSTS     = 2;  
    int AP_STATUS_CHANGE_BOUNDS    = 3;  
    int AP_STATUS_CHANGE_PROTLEVEL = 4;
     
    int AP_STATUS_OKAY     = 0;
    int AP_STATUS_ERROR    = 1;

    public static String getStatus(int i)
    {
        switch(i)
        {
            case 0: 
                return "Lines read"; 
            case 1: 
                return "Status"; 
            case 2: 
                return "Cost"; 
            case 3: 
                return "APriori Bound"; 
            case 4: 
                return "Prot. level"; 
        }
         
        return "";
    }
    
    /*!
        Process one cell from the input table using the apriori file
        ------------------------------------------------------------
     
        \param tableSet     The table to which the apriori file is applied
        \param apType       The Change type
                            - C, W: change the cost for this cell
                            - AB: change the apriori bounds (NOT IMPLEMENTED)
                            - PL: change the protection level (if the cell is UNSAFE) 
                            - S, U, P, M, ML: change the status to this new value
                                * S: Safe
                                * U: Unsafe
                                * P: Protected
                                * M: Secondary Unsafe Manual
                                * ML: Secondary Unsafe
        \param x1,x2        Depending on apType
                            - C,W: x1 = new cost
                            - PL: (x1,x2) are the new protection boundaries
        \param newStatus    
        \param Kop
        \param dimIndex     Index of the cell within the table
        \param codesString  
        \param report       boolean to indicate whether output files should written (outCodes, outStatus, outCost, outBound)
        \param ignoreError  if true just continue with the next cell in case of an error; otherwise throw an exception
        \param aPrioryStatus An array indicating how many operations with well or wrong
                                - first index = 
                                        - AP_STATUS_LINE_READ        = 0            
                                        - AP_STATUS_CHANGE_STATUS    = 1  
                                        - AP_STATUS_CHANGE_COSTS     = 2  
                                        - AP_STATUS_CHANGE_BOUNDS    = 3  
                                        - AP_STATUS_CHANGE_PROTLEVEL = 4
                                 - second index = 
                                        - CORRECT = 0
                                        - ERROR = 1
        \param fileName     The apriori filename (CHECK if not class member !!!)
        \param tokenizer    Just included to get the linenumer correct for error messages
     
    */
    private void ProcessOneVarCode(TableSet tableSet, String apType, double x1, double x2, int newStatus, 
                                    String Kop, int[] dimIndex, String codesString, boolean report, boolean ignoreError, 
                                    int[][] aPrioryStatus, String fileName, Tokenizer tokenizer) throws ArgusException, IOException
    {
        int oldStatus = TableSet.getCellStatus(tableSet, dimIndex);
             
        if( CellStatus.findByValue(oldStatus) == CellStatus.EMPTY ) return; // do nothing 
            
        String ErrorPosition = "\nLine"+tokenizer.getLine()+"\nCell "+codesString;

        // met status is EMPTY mag niks gebeuren
        // igv Ignore gewoon doorgaan met de volgende waarde in de lus
        try{
            switch (apType)
            {
                case "C": 
                case "W": //Change cost function
                    if(x1<=0)
                    {
                        x1=1; //zero is a silly value
                    }

                    if (!tauArgus.SetTableCellCost(tableSet.index, dimIndex, x1))
                    {
                        if( !ignoreError )
                            throw new ArgusException ("Illegal new cost value "+ErrorPosition);   

                        aPrioryStatus[AP_STATUS_CHANGE_COSTS][AP_STATUS_ERROR]++;
                        if( report )
                            outCost.write(Kop + "<td>not</td><td>possible</td></tr>");
                    }             
                    else
                    {
                        if( report )
                        {
                            double OldCost = tableSet.getCell(dimIndex).cost;
                            outCost.write(Kop + "<td>"+OldCost+"</td><td>"+x1+"</td></tr>"); 
                        }
                        
                        aPrioryStatus[AP_STATUS_CHANGE_COSTS][AP_STATUS_OKAY]++;
                    }
                    break;
                case "AB": throw new ArgusException ("Change apriory bounds is not yet possible; was neither in the old TAU-Argus");
                            // whcg: ik zie geen code nog in de VB code
                           //break;
                case "PL": 
                   //KAN alleen bij primair onveilige cellen.
                    boolean errorOccurred = false;
                    if ( (x1<0) || (x2 < 0)) 
                    {
                         //Hs = "Illegal values for the protection levels" + "\r\n" + "lower bound:" + String.valueOf(x1) + "\r\n" + "upper bound:" + String.valueOf(x2);
                        if (!ignoreError)
                            throw new ArgusException ("Illegal values for the protection levels: "+ x1 + ".. "+x2+ErrorPosition);

                        if( report )
                            outBound.write( Kop + "<td>not</td><td>possible</td></tr>");

                        aPrioryStatus[AP_STATUS_CHANGE_PROTLEVEL][AP_STATUS_ERROR]++;  
                        errorOccurred = true;
                    }

                    oldStatus = TableSet.getCellStatus(tableSet, dimIndex);
                    if ( (oldStatus < CellStatus.UNSAFE_RULE.getValue())||(oldStatus > CellStatus.UNSAFE_MANUAL.getValue()) )
                    {
                        if (!ignoreError)
                           throw new ArgusException ("Protection levels can only be changed for unsafe cells (at line "+tokenizer.getLine()+")");

                        if( report )
                            outBound.write( Kop + "<td>not</td><td>possible</td></tr>");

                        aPrioryStatus[AP_STATUS_CHANGE_PROTLEVEL][AP_STATUS_ERROR]++;                                 
                        errorOccurred = true;
                    }
                    
                    if(!errorOccurred)
                    {
                        outBound.write( Kop + "<td>"+x1+"</td><td>"+x2+"</td></tr>");
                        aPrioryStatus[AP_STATUS_CHANGE_PROTLEVEL][AP_STATUS_OKAY]++;                                 
                    }
                    
                    //AP_SECONDARY_UNSAFE_MANUAL
                    break;
                case "S": 
                case "U": 
                case "P": 
                case "M": 
                case "ML":  //Status change
                    // changing PROTECTED --> UNSAFE is not allowed
                    oldStatus = TableSet.getCellStatus(tableSet, dimIndex);

                    // code van Robert, aangepast aan de mapping M, ML --> AP_...
                    if ((newStatus == AP_SECONDARY_UNSAFE) && (oldStatus == CellStatus.SAFE_MANUAL.getValue()))
                    {
                        newStatus = AP_SECONDARY_UNSAFE_MANUAL ;
                    }

                    if (report)
                    {
                        outStatus.write(Kop + "<td>"+CellStatus.findByValue(oldStatus).getDescription()+"</td><td>" +
                                                        CellStatus.findByValue(newStatus).getDescription()+"</td></tr>");
                    }

                    if (!tauArgus.SetTableCellStatus(tableSet.index, dimIndex, newStatus))
                    {
                        if( !ignoreError )
                            throw new ArgusException ("Illegal status transition for "+codesString + ErrorPosition);

                        if( report )
                            outStatus.write( Kop + "<td>not</td><td>possible</td></tr>");

                        aPrioryStatus[AP_STATUS_CHANGE_STATUS][AP_STATUS_ERROR]++;                                 
                    }
                    else
                    {
                        if( report )
                            {outStatus.write(";&nbsp;");}
                        
                        aPrioryStatus[AP_STATUS_CHANGE_STATUS][AP_STATUS_OKAY]++;                                 
                    }
                    break;
            }//end switch     
        }
        catch(IOException e){
            throw new ArgusException("An unexpected errro occurred when writing the report files for " + 
                                       "the apriory file ("+ fileName + ").");
        }
    }
     
    private String VerlengString(String s , int length) 
    {
        if( s.length()<length ){
            for(int i=0; i<length-s.length(); i++)
                s = " "+s;
        }
        return s;
    }
    
    public  int processAprioryFile(String fileName, int tableNumber, String separator, 
                                   boolean ignoreError, boolean expandBogus, boolean report, int[][] aPrioryStatus) throws ArgusException
    {
        String regel, apType; 
        int n, nMax, newStatus, varNo;
        double x1,x2; 
        boolean oke;
        TableSet tableSet = TableService.getTable(tableNumber);
        String[] codes = new String[tableSet.expVar.size()]; 
        String codesString;
        int[] dimIndex = new int[tableSet.expVar.size()];
        BufferedReader in; 

        nMax = 0;

        for (int i=0;i<5;i++){
            for (int j=0;j<1;j++){
                aPrioryStatus[i][j] = 0;  
            }  
        }

        for (int i=0;i<tableSet.expVar.size();i++){
            n = TauArgusUtils.getNumberOfActiveCodes(tableSet.expVar.get(i).index);
            if (n>nMax) nMax = n;
        }

        String[][] codeList = new String[tableSet.expVar.size()][nMax];
        for (int i=0;i<tableSet.expVar.size();i++){ 
            varNo = tableSet.expVar.get(i).index; 

            for (int j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++)
                codeList[i][j] = TauArgusUtils.getCode(varNo, j).trim();
        }

        try{
            in = new BufferedReader(new FileReader(fileName));
                
            Tokenizer tokenizer = new Tokenizer(in);

            // if report = false then no reporting is done as the procedure is used for setting secondaries in linked modular
            // The number of real secondaries is returned via aPrioryStatus[1][0];

            outCodes  = new BufferedWriter(new FileWriter(Application.getTempFile("HistCodes" + tableNumber + ".htm"), false));
            outStatus = new BufferedWriter(new FileWriter(Application.getTempFile("HistStat" + tableNumber + ".htm"), false));
            outCost   = new BufferedWriter(new FileWriter(Application.getTempFile("HistCost" + tableNumber + ".htm"), false));     
            outBound  = new BufferedWriter(new FileWriter(Application.getTempFile("HistPL" + tableNumber + ".htm"), false)); 

            if (report){  
                outCodes.write("<FILE>" + fileName); outCodes.newLine();
                outStatus.write("<FILE>" + fileName); outStatus.newLine();
                outCost.write("<FILE>" + fileName); outCost.newLine();
                outBound.write("<FILE>" + fileName); outBound.newLine();
                outStatus.write("<h3>Overview of status changes</h3>");
                outStatus.write("<table>" + "\r\n" + "<tr>");
                outCost.write("<h3>Overview of cost function changes</h3>");
                outCost.write("<table>" + "\r\n" + "<tr>");
                outBound.write("<h3>Overview of apriory bound/protection level changes</h3>");
                outBound.write("<table>" + "\r\n" + "<tr>");

                int k = (int)Math.floor(50 / (double)tableSet.expVar.size());
                for (int i=0;i<tableSet.expVar.size();i++){
                    Variable v = tableSet.expVar.get(i);
                      
                    outStatus.write("<td width=\""+ k+ "%\" >" + v.name + "</td>");
                    outCost.write("<td width=\""+ k+ "%\" >" + v.name + "</td>");
                    outBound.write("<td width=\""+ k+ "%\" >" + v.name + "</td>");
                }
                outStatus.write("<td>Old status</td><td>New status</td></tr>");
                outCost.write("<td>Old Cost</td><td>New cost</td></tr>");
                outBound.write("<td>Lower bound</td><td>Upper bound</td></tr>");
            }

            boolean firstLine = true;
            while ((tokenizer.nextLine()) != null){
                if (firstLine){ //test for a correct separator
                    regel = tokenizer.getLine();  
                    if (!regel.contains(separator)){
                        throw new ArgusException("separator ("+separator+") not found in file "+fileName);
                    } 

                    if( NumberOfVarsInAprioryFile(regel, separator)!=tableSet.expVar.size())
                        throw new ArgusException("APriory file contains "+NumberOfVarsInAprioryFile(regel, separator)+" fields, but current table has "+tableSet.expVar.size());
                } 

                firstLine = false;
                   
                //
                // First part of the line: read the codes
                //
                // result: dimIndex[var index] = code index, i.e. this array indicates a cell in the (mutidimensional) table tableSet
                //         or an error/exception is written/thrown if a code (from the input) does not exist
                //
                oke = true; codesString = ""; 
                for (int i=0;i<tableSet.expVar.size();i++){
                    codes[i] = tokenizer.nextField(separator).trim();
                       
                    int CodeLen = tableSet.expVar.get(i).varLen;                       
                    if (codes[i].trim().equals(tableSet.expVar.get(i).totCode)){
                        codes[i] = "";
                    }
                    else{
                        if (!(tableSet.expVar.get(i).hierarchical == Variable.HIER_LEVELS || 
                                tableSet.expVar.get(i).hierarchical == Variable.HIER_FILE)  ){
                                codes[i] = VerlengString(codes[i], CodeLen);
                        }
                    }
                       
                    codesString = codesString + codes[i]+";";
                       
                    if( codes[i].equals(""))
                        dimIndex[i] = 0;
                    else{
                        dimIndex[i] = -1;
                        varNo = tableSet.expVar.get(i).index;
                        for (int j=0;j<TauArgusUtils.getNumberOfActiveCodes(varNo);j++){
                            if (codes[i].equals(codeList[i][j])){
                                dimIndex[i] = j; 
                                break;
                            }
                        }
                    }
                       
                    if (dimIndex[i] == -1 ){ 
                        aPrioryStatus[AP_STATUS_LINE_READ][AP_STATUS_ERROR]++;
                        if (!ignoreError){
                            throw new ArgusException("Code number "+(i+1) + " code: '"+codes[i]+ "' could not be found\n"+ 
                                                     "Apriori file: '"+ fileName + "'\n"+
                                                     "Line number: "+tokenizer.getLineNumber());
                        }
                        else{ 
                            oke = false; codesString = codesString+"(incorrect)";
                        }
                    }
                }

                if(oke) aPrioryStatus[AP_STATUS_LINE_READ][AP_STATUS_OKAY]++;
                   
                codesString = codesString.substring(0,codesString.length()-1);

                if (!oke && report) outCodes.write(codesString); 

                //
                // Second part of the line: apriory setting
                //
                apType = ""; newStatus = 0;
                x1=0; x2=0;
                if (oke){ 
                    //
                    // find the correct apriory transition
                    // 
                    apType = tokenizer.nextField(separator).toUpperCase();
                    try{
                        switch(apType) 
                        {
                            case "S": newStatus = AP_SAFE_MANUAL; 
                                      break;
                            case "U": newStatus = AP_UNSAFE_MANUAL; 
                                      break;
                            case "P": newStatus = AP_PROTECT_MANUAL; 
                                      break;
                            case "M": newStatus = AP_SECONDARY_UNSAFE_MANUAL;  
                                      break;
                            case "ML": newStatus = AP_SECONDARY_UNSAFE; 
                                       break;
                            case "C": 
                            case "W": newStatus = AP_ADJUST_COST; 
                                      x1 = StrUtils.toDouble(tokenizer.nextField(separator)); 
                                      break;
                            case "AB": newStatus = AP_ADJUST_APRIORI_BOUND; 
                                       x1 = StrUtils.toDouble(tokenizer.nextField(separator));
                                       x2 = StrUtils.toDouble(tokenizer.nextField(separator)); 
                                       break;
                            case "PL": newStatus = AP_ADJUST_PROT_LEVEL; 
                                       x1 = StrUtils.toDouble(tokenizer.nextField(separator));
                                       x2 = StrUtils.toDouble(tokenizer.nextField(separator)); 
                                       break;
                            default: 
                                aPrioryStatus[AP_STATUS_LINE_READ][AP_STATUS_ERROR]++;
                                throw new ArgusException ("Illegal apriory command in line " +tokenizer.getLineNumber() +
                                                               "\nApriory file: " + fileName);
                                //Always stop here             
                        } //switch
                    } 
                    catch(argus.model.ArgusException | ArgusException ex){}

                    // When ignore=false in manual mode it should be possible to choose continue after error
                    // so no exceptions, or distinguise between Application.IsBatch=true and manual(false)

                    //
                    // For each exp var i of the current table cell (determined by the values of the .hst file just read)
                    // For each (bogus) code (if expandbogus), otherwise just the one value
                    //
                    //      Apply change as defined above in the previous switch
                    //

                    List<int[]> codeRange = new ArrayList<>();
                        
                    for (int var_i = 0; var_i < tableSet.expVar.size(); var_i++){
                        Variable v = tableSet.expVar.get(var_i);
                        CodeList tempCodeList = new CodeList(v.index, v.hierarchical != Variable.HIER_NONE);
                        int[] bogusCodes;
                            
                        if(!expandBogus){
                            bogusCodes = new int[1];
                            bogusCodes[0] = dimIndex[var_i];                               
                        }
                        else{
                            bogusCodes = tempCodeList.bogusRange(dimIndex[var_i]);
                        }
                            
                        codeRange.add(bogusCodes);
                    }
                        
/* Not Used ?????                        
                    boolean Istherarange = false;
                    for(int i=0; i<codeRange.size(); i++){
                        if( codeRange.get(i).length>2)
                            Istherarange = true;
                    }
                    if(Istherarange){
                        int x = 2;
                    }
*/                         
                        
                    CartesianProductIterator It = new CartesianProductIterator(codeRange);
                    It.Reset();
                        
                    while(!It.EOR()){
                        // Status EMPTY should not be changed
                        // When Ignore=true continue with next case in loop
                        dimIndex = It.Current();

                        String Kop = "<tr>";                                
                        for (int i = 0; i < tableSet.expVar.size(); i++){
                            //String t1 = codes[i]; Not used????
                            String t2 = codeList[i][dimIndex[i]];

                            Kop = Kop + "<td>" + t2 + "</td>";                                    
                        }

                        ProcessOneVarCode(tableSet, apType, x1, x2, newStatus, Kop, dimIndex, codesString, 
                                                 report, ignoreError, aPrioryStatus, fileName, tokenizer);

                        It.Next();
                            
                        //String tt = It.Currpos(); Not used????
                        //int qq = 2; Not used????
                            
                    }// for iterator   
                }//if oke
            }
                
            if (report){  
                outCodes.close();
                    
                outStatus.write("</table>");
                outStatus.close();
                outBound.write("</table>");
                outBound.close();
                outCost.write("</table>");
                outCost.close();
            }  
        }
        catch (IOException ex){ 
            throw new ArgusException("An unexpected errro occurred when writing the report files for " + 
                                     "the apriory file ("+ fileName + ").");
        }
     
        return 0;
    }// reading lines loop       
}
