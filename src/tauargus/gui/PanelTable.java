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

package tauargus.gui;

import argus.utils.StrUtils;
import argus.utils.SystemUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.apache.commons.lang3.StringUtils;
import tauargus.extern.dataengine.TauArgus;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.Cell;
import tauargus.model.CellStatus;
import tauargus.model.CellStatusStatistics;
import tauargus.model.GHMiter;
import tauargus.model.Metadata;
import tauargus.model.OptiSuppress;
import tauargus.model.ProgressSwingWorker;
import tauargus.model.SuppressionMethod;
import static tauargus.model.SuppressionMethod.OPTIMAL;
import tauargus.model.TableSet;
import tauargus.model.Type;
import tauargus.model.VarCode;
import tauargus.model.Variable;
import tauargus.service.TableService;
import tauargus.utils.TableColumnResizer;
import tauargus.utils.TauArgusUtils;

public class PanelTable extends javax.swing.JPanel {

    final Map<Integer,javax.swing.JRadioButton> buttonMap;
    
    private static final Logger LOGGER = Logger.getLogger(PanelTable.class.getName());
   
    private JFrame getParentFrame() {
        Container container = this;
        while (!(container instanceof JFrame)) {
            container = container.getParent();
        }
        return (JFrame)container;
    }

    /**
     * Contains information needed for displaying a unique value of a variable
     */
    private static class Code {
        public static final int NODE = 0;
        public static final int EXPANDED = 1;
        public static final int COLLAPSED = 2;
        
        String label;
        byte level;
        byte state;
        boolean missing;
    }

    // Component arrays for easy reference...
    private ArrayList<javax.swing.JComboBox<String>> comboBoxSpan;
    private javax.swing.JLabel[] labelSpan;
    private javax.swing.JRadioButton[] radioButtonSuppress;
    
    private TauArgus tauArgus;

    private TableSet tableSet;
    private Code[][] codeList;
    private Code singleColumnCode;

    private final DecimalFormat doubleFormatter = new DecimalFormat();
    private final DecimalFormat integerFormatter = new DecimalFormat();

    private int[] expVarPermutation;
    private int rowExpVarIndex = 0;
    private int columnExpVarIndex = 1;
    private int[] rowCodeIndex;
    private int[] columnCodeIndex;
    
    private boolean isAdjusting = false; 
    private boolean isSingleColumn = false;

    public void enableHiddenFeatures (boolean activate) {
      String hs= "";
      buttonSecondary.setVisible(activate);
      //show the UWE radio button only if the software is available and Anco option is enabled
      try {
          hs = SystemUtils.getApplicationDirectory(PanelTable.class).getCanonicalPath();
      } catch (Exception ex) {}
      radioButtonUwe.setVisible(TauArgusUtils.ExistFile(hs+"/EXP_ExternalUnpickerOnJJ.exe") && activate);
      radioButtonMarginal.setVisible(activate);
      checkBoxInverseWeight.setVisible(activate);
    } 
    
    private Color getBackgroundColor(int level) {
        final int MIN_LEVEL_BRIGHTNESS = 255;
        final int MAX_LEVEL_BRIGHTNESS = 127;
        final int MAX_LEVELS = 11;
        int brightness = MIN_LEVEL_BRIGHTNESS + (MAX_LEVEL_BRIGHTNESS - MIN_LEVEL_BRIGHTNESS) * level / MAX_LEVELS;
        return new Color(brightness, brightness, brightness, 255);
    }
    
    private Color getBackgroundColor(Code code) {
        return getBackgroundColor(code.level);
    }
    
    private Color getBackgroundColor(Cell cell, Code code) {
        if (tableSet.hasBeenAudited){ //adapt background color if audit is not OK
            if (cell.status.isPrimaryUnsafe()){
              if ((cell.response == cell.realizedLower) && (cell.response == cell.realizedUpper)) {return Color.orange;}  //exact
              if ( (cell.response + cell.upper) > cell.realizedUpper){return Color.getHSBColor(255,100,100);}
              if ( (cell.response + cell.lower) < cell.realizedLower){return Color.getHSBColor(255,100,100);}
              return getBackgroundColor(code); // gray background depending on level in hierarchy
            } else {
              return getBackgroundColor(code); // gray background depending on level in hierarchy
            }
        } 
        else {
            return getBackgroundColor(code); // gray background depending on level in hierarchy
        }
    }

    private Color getCKMBackgroundColor(Cell cell, Code code){
        if (tableSet.ckmProtect){
            float maxColor = Math.max(Math.abs(tableSet.minDiff), Math.abs(tableSet.maxDiff));
            //float maxColor = (float) Math.max(Math.abs(tableSet.minDiff), Math.abs(tableSet.maxDiff));
            float diff = (float) Math.abs(cell.CKMValue - cell.response);
            if (diff >= maxColor) diff = maxColor;
            int R, G, B = 255; // darkest: (85,85,255) brightest: (235,235,255)
            R = (int) (235 - (235-85)*(diff-1)/(maxColor-1));
            G = R;
            if (diff > 0){
                return(new Color(R,G,B));
            }
            return getBackgroundColor(code); // gray background depending on level in hierarchy
        } else {
            return getBackgroundColor(code); // gray background depending on level in hierarchy
        }
    }
    
    private class ColumnHeaderRenderer extends DefaultTableCellRenderer {
        final TableCellRenderer renderer;

        public ColumnHeaderRenderer() {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel)renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column >= 1) {
                if (getColumnCode(column).missing) {
                    label.setForeground(Color.red);
                }
                
                label.setBackground(getBackgroundColor(getColumnCode(column)));
            }
            
            return label;
        }
    }
    
    /**
     * This renderer is used for row headers (column 0)
     */
    private class CodeRenderer extends javax.swing.table.DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Code code = (Code) value;

            StringBuilder s = new StringBuilder();
            for (int i=0; i<code.level; i++) {
                s.append("   ");
            }
            switch (code.state) {
                case Code.EXPANDED:
                    s.append("- ");
                    break;
                case Code.COLLAPSED:
                    s.append("+ ");
                    break;
                default:
                    s.append("   ");
                    break;
            }
            s.append(code.label);
            setText(s.toString());

            setForeground(code.missing ? Color.red : Color.black);
            setBackground(getBackgroundColor(code));

            setHorizontalAlignment(SwingConstants.LEFT);
            return this;
        }
    }

    /**
     * This renderer is used by table cells
     */
    private class CellRenderer extends javax.swing.table.DefaultTableCellRenderer {
       
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
                boolean hasFocus, int rowIndex, int vcolIndex) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, vcolIndex);
            Cell cell = (Cell) value;
            
            if (cell.status == CellStatus.EMPTY) {
                setText("-");
            } else if (checkBoxOutputView.isSelected() && !cell.status.isSafe()) {
                setText("X");
                setToolTipText(doubleFormatter.format(cell.response));
            } else if (tableSet.rounded) {
                setText(doubleFormatter.format(cell.roundedResponse));
            } else if (tableSet.ctaProtect) {
                setText(doubleFormatter.format(cell.CTAValue));
            } else if (tableSet.ckmProtect) {
                setText(doubleFormatter.format(cell.CKMValue));
            } else {
                setText(doubleFormatter.format(cell.response));
            }

            setForeground(cell.status.getForegroundColor());
            if (!(checkBoxColoredView.isEnabled() && checkBoxColoredView.isSelected())){
                setBackground(getBackgroundColor(cell, getRowCode(rowIndex)));
            }
            else{
                setBackground(getCKMBackgroundColor(cell, getRowCode(rowIndex)));
            }

            setHorizontalAlignment(SwingConstants.RIGHT);
            return this;
        }
    }

    private void initTableRenderers() {
        table.getTableHeader().setDefaultRenderer(new ColumnHeaderRenderer());
        table.setDefaultRenderer(Code.class, new CodeRenderer());
        table.setDefaultRenderer(Cell.class, new CellRenderer());
    }
    
    private void initTableSelectionListeners() {
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() != table.getTableHeader())
                    return;
                int col = table.getTableHeader().columnAtPoint(e.getPoint());
                if (col >= 1) {
                    // column header is selected
                    Code code = getColumnCode(col);
                    if (code.state != Code.NODE) {
                        if (code.state == Code.EXPANDED) {
                            code.state = Code.COLLAPSED;
                        } else {
                            code.state = Code.EXPANDED;
                        }
                        createColumnIndices();
                        ((AbstractTableModel)table.getModel()).fireTableStructureChanged();
                        adjustColumnWidths();
                    }
                }
            }
        });

        ListSelectionListener listSelectionListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int row = table.getSelectedRow();
                    int col = table.getSelectedColumn();
                    if (row >= 0 && col >= 0) {
                        if (col == 0) {
                            Code code = getRowCode(row);
                            if (code.state != Code.NODE) {
                                if (code.state == Code.EXPANDED) {
                                    code.state = Code.COLLAPSED;
                                } else {
                                    code.state = Code.EXPANDED;
                                }
                                createRowIndices();
                                ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                                adjustColumnWidths();
                            }
                        } else {
                            // cell is selected
                            Cell cell = getCell(row, col);
                            panelCellInformation.update(tableSet, cell, integerFormatter, doubleFormatter);
                            organiseSafetyButtons(cell.status);
                        }
                    }
                }
            }
        };

        // react on row changes
        table.getSelectionModel().addListSelectionListener(listSelectionListener);
        // .. and react on column changes
        table.getColumnModel().getSelectionModel().addListSelectionListener(listSelectionListener);
    }
    
    /**
     * Create arrays of component so we can easier handle them as a group
     */
    private void initComponentArrays() {
        comboBoxSpan = new ArrayList<>();
        comboBoxSpan.add(comboBoxSpan0);
        comboBoxSpan.add(comboBoxSpan1);
        comboBoxSpan.add(comboBoxSpan2);
        comboBoxSpan.add(comboBoxSpan3);
        comboBoxSpan.add(comboBoxSpan4);
        comboBoxSpan.add(comboBoxSpan5);
        comboBoxSpan.add(comboBoxSpan6);
        comboBoxSpan.add(comboBoxSpan7);

        labelSpan = new javax.swing.JLabel[] {
            labelSpan0,
            labelSpan1,
            labelSpan2,
            labelSpan3,
            labelSpan4,
            labelSpan5,
            labelSpan6,
            labelSpan7
        };
        
        radioButtonSuppress = new javax.swing.JRadioButton[] {
            radioButtonHyperCube,
            radioButtonModular,
            radioButtonOptimal,
            radioButtonNetwork,
            radioButtonUwe,
            radioButtonMarginal,
            radioButtonCta,            
            radioButtonRounding,
            radioButtonCellKey
        };
    }

    /**
     * Creates new form PanelTable
     */
    public PanelTable() {
        //String hs= "";
        initComponents();
        initComponentArrays();
        initTableRenderers();
        initTableSelectionListeners();
        radioButtonUwe.setVisible(false);
        radioButtonMarginal.setVisible(false);
        checkBoxInverseWeight.setVisible(false);
      
        buttonMap = new HashMap<>();
   /*         public static final int SUP_NO = 0;
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
    */
   
        buttonMap.put(TableSet.SUP_JJ_OPT,radioButtonOptimal);
        buttonMap.put(TableSet.SUP_GHMITER,radioButtonHyperCube);
        buttonMap.put(TableSet.SUP_HITAS,radioButtonModular);
        buttonMap.put(TableSet.SUP_NETWORK,radioButtonNetwork);
        buttonMap.put(TableSet.SUP_ROUNDING,radioButtonRounding);
        buttonMap.put(TableSet.SUP_MARGINAL,radioButtonMarginal);
        buttonMap.put(TableSet.SUP_UWE,radioButtonUwe);
        buttonMap.put(TableSet.SUP_CTA,radioButtonCta);
        buttonMap.put(TableSet.SUP_CKM,radioButtonCellKey);
        
        // disable reordering of columns
        table.getTableHeader().setReorderingAllowed(false);        
    }
    
    private int indexOfVariable(Variable variable) {
        for (int i = 0; i < tableSet.expVar.size(); i++) {
            if (variable == tableSet.expVar.get(i)) {
                return i;
            }
        }
        return -1;
    }
    
    public void setTable(TableSet tableSet) {
        this.tableSet = tableSet;
        this.tauArgus = Application.getTauArgusDll();
        isSingleColumn = (tableSet.expVar.size() == 1);

        createCodeList();
        
        // 2 codes are selected from the row and column variable.
        int n = tableSet.expVar.size() - 2;
        if (isSingleColumn){n=0;}
        for (int i = 0; i < n; i++) {
            labelSpan[i].setVisible(true);
            comboBoxSpan.get(i).setVisible(true);
        }
        for (int i = n; i < labelSpan.length; i++) {
            labelSpan[i].setVisible(false);
            comboBoxSpan.get(i).setVisible(false);
        }
        
        buttonCost.setVisible(tableSet.costFunc == TableSet.COST_VAR);
        isAdjusting = true;
        comboBoxDecimals.setSelectedIndex(Math.min(tableSet.respVar.nDecimals,comboBoxDecimals.getMaximumRowCount()));
        isAdjusting = false;
        Variable columnVar = null;
        if (!isSingleColumn) {columnVar = tableSet.expVar.get(1);}
        setRowColumnVariables(tableSet.expVar.get(0), columnVar);
        if (buttonMap.containsKey(tableSet.suppressed)) {
            buttonMap.get(tableSet.suppressed).setSelected(true);
        }
        else{
            if (tableSet.suppressed==TableSet.SUP_NO) radioButtonHyperCube.setSelected(true);
        }
        updateSuppressButtons();
    }
    
    public void setRowColumnVariables(Variable rowVariable, Variable columnVariable) {
        int VarDepth;
        int MaxLevelChoice;
        isAdjusting = true;
        rowExpVarIndex = indexOfVariable(rowVariable);
        columnExpVarIndex = indexOfVariable(columnVariable);
        createExpVarPermutation();
        // Automatically adjust maximum number of levels to choose from, depending on variable
        VarDepth = rowVariable.GetDepthOfHierarchicalBoom(rowVariable.recoded);
        MaxLevelChoice = comboBoxNrOfVertLevels.getItemCount()-1;
        if (VarDepth - MaxLevelChoice < 0)
        {
            for (int j=VarDepth;j<MaxLevelChoice;j++)
            {
                comboBoxNrOfVertLevels.removeItemAt(VarDepth+1);
            }
        }
        else
        {
            for (int j=MaxLevelChoice+1;j<=VarDepth;j++)
            {
                comboBoxNrOfVertLevels.addItem(Integer.toString(j));
            }
        }
        comboBoxNrOfVertLevels.setSelectedIndex(1);
        
        if (!isSingleColumn)
        {
            comboBoxNrOfHorLevels.setVisible(true);
            comboBoxNrOfHorLevels.setEnabled(true);
            LabelNrOfHorLevels.setVisible(true);
            LabelNrOfHorLevels.setEnabled(true);
            buttonSelectView.setEnabled(true);
            VarDepth = columnVariable.GetDepthOfHierarchicalBoom(columnVariable.recoded);
            MaxLevelChoice = comboBoxNrOfHorLevels.getItemCount()-1;
            if (VarDepth - MaxLevelChoice < 0)
            {
                for (int j=VarDepth;j<MaxLevelChoice;j++)
                {
                    comboBoxNrOfHorLevels.removeItemAt(VarDepth+1);
                }
            }
            else
            {
                for (int j=MaxLevelChoice+1;j<=VarDepth;j++)
                {
                    //comboBoxNrOfHorLevels.addItem(Integer.toString(j+1));
                    comboBoxNrOfHorLevels.addItem(Integer.toString(j));
                }
            }
            comboBoxNrOfHorLevels.setSelectedIndex(1);
        }
        else 
        {
            comboBoxNrOfHorLevels.setVisible(true);
            comboBoxNrOfHorLevels.setEnabled(false);
            LabelNrOfHorLevels.setVisible(true);
            LabelNrOfHorLevels.setEnabled(false);
            buttonSelectView.setEnabled(false);
            comboBoxNrOfHorLevels.removeAllItems();
            comboBoxNrOfHorLevels.addItem("0");
        }
        
        Variable RowVar = tableSet.expVar.get(rowExpVarIndex);
        String hs = tableSet.respVar.name + ": " + RowVar.name;
        if (RowVar.recoded) hs += "(R)";
        
        if (!isSingleColumn){
            Variable ColVar = tableSet.expVar.get(columnExpVarIndex);
            hs += " x " + ColVar.name;
            if (ColVar.recoded) hs += "(R)";
        }
        labelRowColVars.setText(hs);
        
        int n = tableSet.expVar.size();
        for (int i = 0; i < n - 2; i++) {
            labelSpan[i].setText(tableSet.expVar.get(expVarPermutation[i + 2]).name);
            if (tableSet.expVar.get(expVarPermutation[i + 2]).recoded) labelSpan[i].setText(tableSet.expVar.get(expVarPermutation[i + 2]).name + "(R)");
            int expvarIndex = expVarPermutation[i + 2];
            comboBoxSpan.get(i).removeAllItems();
            for (Code item : codeList[expvarIndex]) {
                comboBoxSpan.get(i).addItem(item.label);
            }
            comboBoxSpan.get(i).setSelectedIndex(0);
        }
        
        table.setModel(new AbstractTableModel() {
            @Override
            public int getColumnCount() {
                if (isSingleColumn){return 2 ;}
                else {return 1 + columnCodeIndex.length;}
            }

            @Override
            public int getRowCount() {
                return rowCodeIndex.length;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Code.class;
                } else {
                    return Cell.class;
                }
            }
            
            @Override
            public String getColumnName(int column) {
                if (column == 0) {
                    // upper left corner
                    return "";
                } else {
                    // column header
                    if (isSingleColumn){return "Total";}
                    Code code = getColumnCode(column);
                    String s = "";
                    if (code.state == Code.EXPANDED) {
                        s = "- ";
                    } else if (code.state == Code.COLLAPSED) {
                        s = "+ ";
                    }
                    s += code.label;
                    return s;
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    // row header
                    return getRowCode(rowIndex);
                } else {
                    return getCell(rowIndex, columnIndex);
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });

        isAdjusting = false;
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        ((AbstractTableModel)table.getModel()).fireTableStructureChanged();
        adjustColumnWidths();
        table.getSelectionModel().setSelectionInterval(0, 0);
        table.getColumnModel().getSelectionModel().setSelectionInterval(1, 1);
    }
    
    private void updateFormatters() {
        int fractionDigits = Integer.parseInt((String)comboBoxDecimals.getSelectedItem());
        doubleFormatter.setMinimumFractionDigits(fractionDigits);
        doubleFormatter.setMaximumFractionDigits(fractionDigits);
        doubleFormatter.setGroupingUsed(checkBoxThousandSeparator.isSelected());
        integerFormatter.setMinimumFractionDigits(0);
        integerFormatter.setMaximumFractionDigits(0);
        integerFormatter.setGroupingUsed(checkBoxThousandSeparator.isSelected());
        panelCellInformation.setFormatters(integerFormatter, doubleFormatter);
    }
    
    public void updateSuppressButtons() {
        int s = tableSet.suppressed; 
        int radioSelect = 0; 
        boolean CKMpossible;
        String hs = "";
        try {
           hs = SystemUtils.getApplicationDirectory(PanelTable.class).getCanonicalPath();
        } catch (Exception ex) {}
        
        if (Application.isAnco()) {
                radioButtonUwe.setVisible(TauArgusUtils.ExistFile(hs+"/EXP_ExternalUnpickerOnJJ.exe"));        
        }

        radioButtonNetwork.setVisible(TauArgusUtils.ExistFile(hs+"/main1H2D.exe")||TauArgusUtils.ExistFile(hs+"/main1H2D")); 
        
        CKMpossible = tableSet.CellKeyAvailable && (!tableSet.respVar.CKMType.equals("N") || tableSet.respVar.type.equals(Type.FREQUENCY));
        
        radioButtonCellKey.setVisible(CKMpossible);
        buttonChangePTable.setVisible(CKMpossible && radioButtonCellKey.isSelected());
        buttonChangePTable.setEnabled(radioButtonCellKey.isSelected());// && !tableSet.respVar.isResponse());
        if (CKMpossible){
            if (tableSet.respVar.isResponse()){
                labelPTable.setText("ptablefileCont: "+tableSet.cellkeyVar.PTableFileCont.substring(tableSet.cellkeyVar.PTableFileCont.lastIndexOf("\\")+1));
                if (tableSet.respVar.CKMseparation){
                    labelPTableSep.setText("ptablefileSep: "+tableSet.cellkeyVar.PTableFileSep.substring(tableSet.cellkeyVar.PTableFileSep.lastIndexOf("\\")+1));
                }
                else labelPTableSep.setText("");
            }
            else{
                labelPTable.setText("ptablefile: "+tableSet.cellkeyVar.PTableFile.substring(tableSet.cellkeyVar.PTableFile.lastIndexOf("\\")+1));
                labelPTableSep.setText("");
            }
        }
        else{
            //radioButtonHyperCube.setSelected(true);
        }
        labelPTable.setVisible(CKMpossible && radioButtonCellKey.isSelected());
        labelPTableSep.setVisible(CKMpossible && radioButtonCellKey.isSelected());
        
        for (int i = 0; i < radioButtonSuppress.length; i++) {
            radioButtonSuppress[i].setEnabled(s == TableSet.SUP_NO);
            if (radioButtonSuppress[i].isSelected()){radioSelect = i;}
        }
        labelPTable.setEnabled(s == TableSet.SUP_NO);
        labelPTableSep.setEnabled(s == TableSet.SUP_NO);
        
        checkBoxInverseWeight.setEnabled(radioButtonSuppress[radioSelect]== radioButtonOptimal);
        if (radioSelect == radioButtonSuppress.length-2 || s == TableSet.SUP_ROUNDING){ //Rounder selected
          buttonSuppress.setText("Round");
        }
        else{
            if (radioButtonCta.isSelected()) {
                buttonSuppress.setText("CTA");
            }
            else{
                if (radioButtonCellKey.isSelected()) {
                    buttonSuppress.setText("Cell Key");
                }
                else{
                    buttonSuppress.setText("Suppress");
                }
            }
        }
        
        checkBoxColoredView.setVisible(CKMpossible);
        boolean b = (s == TableSet.SUP_CKM) || (s == TableSet.SUP_NO);
        if (tableSet.respVar.isResponse()){ b=false;} // For now no colored view for magnitude tables
        checkBoxColoredView.setEnabled(b);
        if (!b) checkBoxColoredView.setSelected(false);
        if (s == TableSet.SUP_CKM) {
            checkBoxColoredView.setSelected(true);
            buttonChangePTable.setEnabled(false);
        }
        
        b = (s == TableSet.SUP_ROUNDING)  || (s == TableSet.SUP_CTA) || (s == TableSet.SUP_CKM);
        checkBoxOutputView.setEnabled(!b);
        if (b) {
            checkBoxOutputView.setSelected(false);
        }
        buttonSuppress.setEnabled(s == TableSet.SUP_NO || s == TableSet.SUP_SINGLETON);
        buttonUndoSuppress.setEnabled(!buttonSuppress.isEnabled());

        buttonRecode.setEnabled(s == TableSet.SUP_NO && tableSet.metadata.dataOrigin == Metadata.DATA_ORIGIN_MICRO); //btnSuppress.Enabled

        buttonAudit.setEnabled((buttonUndoSuppress.isEnabled() && !b) || Application.isAnco());
        buttonPriory.setEnabled(s == TableSet.SUP_NO);
    }

    private void createCodeList() {
        
        singleColumnCode = new Code();        
        singleColumnCode.label = "Total";
        singleColumnCode.level = Code.NODE;
        singleColumnCode.state = Code.EXPANDED;
        singleColumnCode.missing = false;
        
        codeList = new Code[tableSet.expVar.size()][];
        for (int i=0; i<tableSet.expVar.size(); i++) {
            Variable variable = tableSet.expVar.get(i);
            int varIndex = variable.index;
            int numberOfCodes = TauArgusUtils.getNumberOfCodes(varIndex);
            codeList[i] = new Code[numberOfCodes];
            for (int codeIndex = 0; codeIndex < numberOfCodes; codeIndex++) {
                VarCode varCode = new VarCode(varIndex, codeIndex);
                Code code = new Code();
                if (codeIndex != 0) {
                    code.label = varCode.getCodeString();
                } else if (StringUtils.isNotEmpty(variable.totCode)) {
                    code.label = variable.totCode;
                } else {
                    code.label = "Total";
                }
                code.level = (byte)varCode.getLevel();
                code.missing = varCode.isMissing();
                code.state = Code.NODE;
                codeList[i][codeIndex] = code;
            }
        }
    }
    
    private void setLevel(Code[] codeList, int level) {
        for (int codeIndex = 1; codeIndex < codeList.length; codeIndex++) {
            Code code = codeList[codeIndex];
            Code previousCode = codeList[codeIndex - 1];
            if (code.level > previousCode.level) {
                if (code.level < level) {
                    previousCode.state = Code.EXPANDED;
                } else {
                    previousCode.state = Code.COLLAPSED;
                }
            }
        }
    }
    
    private int[] createIndex(Code[] codeList) {
        int count = 0;
        boolean collapsedMode = false;
        int collapsedLevel = 0;
        for (Code code : codeList) {
            if (collapsedMode && code.level <= collapsedLevel) {
                collapsedMode = false;
            }
            if (!collapsedMode) {
                count++;
                if (code.state == Code.COLLAPSED) {
                    collapsedMode = true;
                    collapsedLevel = code.level;
                }
            }
        }

        int[] TmpRowCodeIndex = new int[count];
        int index = 0;
        collapsedMode = false;
        collapsedLevel = 0;
        for (int codeIndex = 0; codeIndex < codeList.length; codeIndex++) {
            Code code = codeList[codeIndex];
            if (collapsedMode && code.level <= collapsedLevel) {
                collapsedMode = false;
            }
            if (!collapsedMode) {
                TmpRowCodeIndex[index++] = codeIndex;
                if (code.state == Code.COLLAPSED) {
                    collapsedMode = true;
                    collapsedLevel = code.level;
                }
            }
        }
        return TmpRowCodeIndex;
    }
    
    private void createRowIndices() {
        rowCodeIndex = createIndex(codeList[rowExpVarIndex]);
    }
    
    private void createColumnIndices() {
        columnCodeIndex = createIndex(codeList[columnExpVarIndex]);
    }
    
    private int rowToRowCodeIndex(int row) {
        return rowCodeIndex[row];
    }
    
    private int columnToColumnCodeIndex(int column) {
        if(isSingleColumn){return 1;}
        return columnCodeIndex[column - 1];
    }
    
    private Code getRowCode(int row) {
        return codeList[rowExpVarIndex][rowToRowCodeIndex(row)];
    }
    
    private Code getColumnCode(int column) {
        if (isSingleColumn){return singleColumnCode;}
        
        return codeList[columnExpVarIndex][columnToColumnCodeIndex(column)];
    }
    
    private Cell getCell(int row, int column) {
        return tableSet.getCell(createDimArray(row, column));
    }
    
    private void createExpVarPermutation() {
        int n = tableSet.expVar.size();
        expVarPermutation = new int[n];
        
        expVarPermutation[0] = rowExpVarIndex;
        //This is not needed for a single column variable
        if (isSingleColumn){return;}
        expVarPermutation[1] = columnExpVarIndex;
        
        int expVarIndex = 0;
        for (int i = 2; i< n; i++) {
            while (expVarIndex == rowExpVarIndex || expVarIndex == columnExpVarIndex) {
                expVarIndex++;
            }
            expVarPermutation[i] = expVarIndex++;
        }
    }
    
    private int[] createDimArray(int row, int column) {
        int n = tableSet.expVar.size();
        int[] dimArray = new int[n];

        dimArray[rowExpVarIndex] = rowToRowCodeIndex(row);
        if (isSingleColumn){return dimArray;}
        dimArray[columnExpVarIndex] = columnToColumnCodeIndex(column);
        for (int i = 2; i < n; i++) {
            dimArray[expVarPermutation[i]] = comboBoxSpan.get(i-2).getSelectedIndex();
        }
                
        return dimArray;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupSuppress = new javax.swing.ButtonGroup();
        scrollPane = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        labelSpan0 = new javax.swing.JLabel();
        comboBoxSpan0 = new javax.swing.JComboBox<>();
        labelSpan1 = new javax.swing.JLabel();
        comboBoxSpan1 = new javax.swing.JComboBox<>();
        labelSpan2 = new javax.swing.JLabel();
        comboBoxSpan2 = new javax.swing.JComboBox<>();
        labelSpan3 = new javax.swing.JLabel();
        comboBoxSpan3 = new javax.swing.JComboBox<>();
        labelSpan4 = new javax.swing.JLabel();
        comboBoxSpan4 = new javax.swing.JComboBox<>();
        labelSpan5 = new javax.swing.JLabel();
        comboBoxSpan5 = new javax.swing.JComboBox<>();
        labelSpan6 = new javax.swing.JLabel();
        comboBoxSpan6 = new javax.swing.JComboBox<>();
        labelSpan7 = new javax.swing.JLabel();
        comboBoxSpan7 = new javax.swing.JComboBox<>();
        panelCellInformation = new tauargus.gui.PanelCellDetails();
        panelStatus = new javax.swing.JPanel();
        buttonSecondary = new javax.swing.JButton();
        buttonSafe = new javax.swing.JButton();
        buttonUnsafe = new javax.swing.JButton();
        buttonProtected = new javax.swing.JButton();
        buttonCost = new javax.swing.JButton();
        buttonPriory = new javax.swing.JButton();
        buttonNonStructEmpty = new javax.swing.JButton();
        buttonRecode = new javax.swing.JButton();
        panelSuppress = new javax.swing.JPanel();
        radioButtonHyperCube = new javax.swing.JRadioButton();
        radioButtonModular = new javax.swing.JRadioButton();
        radioButtonOptimal = new javax.swing.JRadioButton();
        radioButtonMarginal = new javax.swing.JRadioButton();
        radioButtonRounding = new javax.swing.JRadioButton();
        radioButtonCta = new javax.swing.JRadioButton();
        radioButtonUwe = new javax.swing.JRadioButton();
        checkBoxInverseWeight = new javax.swing.JCheckBox();
        buttonSuppress = new javax.swing.JButton();
        buttonUndoSuppress = new javax.swing.JButton();
        buttonAudit = new javax.swing.JButton();
        radioButtonNetwork = new javax.swing.JRadioButton();
        radioButtonCellKey = new javax.swing.JRadioButton();
        buttonChangePTable = new javax.swing.JButton();
        labelPTable = new javax.swing.JLabel();
        labelPTableSep = new javax.swing.JLabel();
        labelRowColVars = new javax.swing.JLabel();
        panelBottomButtons = new javax.swing.JPanel();
        buttonSelectView = new javax.swing.JButton();
        buttonTableSummary = new javax.swing.JButton();
        LabelNrOfHorLevels = new javax.swing.JLabel();
        comboBoxNrOfHorLevels = new javax.swing.JComboBox<>();
        LabelNrOfVertLevels = new javax.swing.JLabel();
        comboBoxNrOfVertLevels = new javax.swing.JComboBox<>();
        labelDecimals = new javax.swing.JLabel();
        comboBoxDecimals = new javax.swing.JComboBox();
        checkBoxOutputView = new javax.swing.JCheckBox();
        checkBoxThousandSeparator = new javax.swing.JCheckBox();
        checkBoxColoredView = new javax.swing.JCheckBox();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        table.setMinimumSize(new java.awt.Dimension(23, 23));
        table.setRowSelectionAllowed(false);
        scrollPane.setViewportView(table);

        labelSpan0.setText("Span");

        comboBoxSpan0.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        labelSpan1.setText("Span");

        comboBoxSpan1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        labelSpan2.setText("Span");

        comboBoxSpan2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        labelSpan3.setText("Span");

        comboBoxSpan3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        labelSpan4.setText("Span");

        comboBoxSpan4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        labelSpan5.setText("Span");

        comboBoxSpan5.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        labelSpan6.setText("Span");

        comboBoxSpan6.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        labelSpan7.setText("Span");

        comboBoxSpan7.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSpan7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSpanActionPerformed(evt);
            }
        });

        panelCellInformation.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Cell Information"));
        panelCellInformation.setMinimumSize(null);
        panelCellInformation.setPreferredSize(null);

        panelStatus.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Change status"));
        panelStatus.setToolTipText("");

        buttonSecondary.setText("Set to second.");
        buttonSecondary.setNextFocusableComponent(table);
        buttonSecondary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetStatusActionPerformed(evt);
            }
        });

        buttonSafe.setText("Set to safe");
        buttonSafe.setNextFocusableComponent(table);
        buttonSafe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetStatusActionPerformed(evt);
            }
        });

        buttonUnsafe.setText("Set to unsafe");
        buttonUnsafe.setNextFocusableComponent(table);
        buttonUnsafe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetStatusActionPerformed(evt);
            }
        });

        buttonProtected.setText("Set to protected");
        buttonProtected.setNextFocusableComponent(table);
        buttonProtected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetStatusActionPerformed(evt);
            }
        });

        buttonCost.setText("Set cost");
        buttonCost.setNextFocusableComponent(table);
        buttonCost.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCostActionPerformed(evt);
            }
        });

        buttonPriory.setText("A priory info");
        buttonPriory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPrioryActionPerformed(evt);
            }
        });

        buttonNonStructEmpty.setText("<html>All Non-<br>StructEmpty</html>");
        buttonNonStructEmpty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNonStructEmptyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelStatusLayout = new javax.swing.GroupLayout(panelStatus);
        panelStatus.setLayout(panelStatusLayout);
        panelStatusLayout.setHorizontalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStatusLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(buttonProtected, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonUnsafe, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonCost, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonSafe, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(19, 19, 19)
                .addGroup(panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonSecondary, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonNonStructEmpty, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonPriory, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        panelStatusLayout.setVerticalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStatusLayout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addGroup(panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSafe)
                    .addComponent(buttonSecondary))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonUnsafe)
                    .addComponent(buttonPriory))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelStatusLayout.createSequentialGroup()
                        .addComponent(buttonProtected)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCost))
                    .addComponent(buttonNonStructEmpty, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        buttonRecode.setText("Recode");
        buttonRecode.setPreferredSize(null);
        buttonRecode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRecodeActionPerformed(evt);
            }
        });

        panelSuppress.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Suppress"));

        buttonGroupSuppress.add(radioButtonHyperCube);
        radioButtonHyperCube.setSelected(true);
        radioButtonHyperCube.setText("Hypercube");
        radioButtonHyperCube.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonHyperCubeActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonModular);
        radioButtonModular.setText("Modular");
        radioButtonModular.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonModularActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonOptimal);
        radioButtonOptimal.setText("Optimal");
        radioButtonOptimal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonOptimalActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonMarginal);
        radioButtonMarginal.setText("Marginal");
        radioButtonMarginal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonMarginalActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonRounding);
        radioButtonRounding.setText("Rounding");
        radioButtonRounding.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonRoundingActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonCta);
        radioButtonCta.setText("CTA");
        radioButtonCta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCtaActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonUwe);
        radioButtonUwe.setText("uwe");
        radioButtonUwe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonUweActionPerformed(evt);
            }
        });

        checkBoxInverseWeight.setText("InverseWgt");
        checkBoxInverseWeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxInverseWeightActionPerformed(evt);
            }
        });

        buttonSuppress.setText("Suppress");
        buttonSuppress.setNextFocusableComponent(table);
        buttonSuppress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSuppressActionPerformed(evt);
            }
        });

        buttonUndoSuppress.setText("Undo");
        buttonUndoSuppress.setNextFocusableComponent(table);
        buttonUndoSuppress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUndoSuppressActionPerformed(evt);
            }
        });

        buttonAudit.setText("Audit");
        buttonAudit.setNextFocusableComponent(table);
        buttonAudit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAuditActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonNetwork);
        radioButtonNetwork.setText("Network");
        radioButtonNetwork.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonNetworkActionPerformed(evt);
            }
        });

        buttonGroupSuppress.add(radioButtonCellKey);
        radioButtonCellKey.setText("Cell Key Method");
        radioButtonCellKey.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCellKeyActionPerformed(evt);
            }
        });

        buttonChangePTable.setText("Change ptable");
        buttonChangePTable.setPreferredSize(new java.awt.Dimension(57, 23));
        buttonChangePTable.setRequestFocusEnabled(false);
        buttonChangePTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonChangePTableActionPerformed(evt);
            }
        });

        labelPTable.setText("ptable");
        labelPTable.setMaximumSize(new java.awt.Dimension(34, 14));
        labelPTable.setMinimumSize(new java.awt.Dimension(34, 14));
        labelPTable.setPreferredSize(new java.awt.Dimension(34, 14));

        labelPTableSep.setText("ptableSEP");

        javax.swing.GroupLayout panelSuppressLayout = new javax.swing.GroupLayout(panelSuppress);
        panelSuppress.setLayout(panelSuppressLayout);
        panelSuppressLayout.setHorizontalGroup(
            panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSuppressLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelSuppressLayout.createSequentialGroup()
                        .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelSuppressLayout.createSequentialGroup()
                                .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(radioButtonCellKey, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(radioButtonHyperCube, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(radioButtonModular, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(radioButtonOptimal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(radioButtonNetwork, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(radioButtonRounding, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(radioButtonCta, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)))
                                .addGap(31, 31, 31)
                                .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(buttonSuppress, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                                        .addComponent(buttonAudit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(buttonUndoSuppress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addComponent(buttonChangePTable, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelSuppressLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(labelPTableSep, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(labelPTable, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(10, 10, 10))
                    .addGroup(panelSuppressLayout.createSequentialGroup()
                        .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(radioButtonUwe, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(radioButtonMarginal, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(checkBoxInverseWeight)
                        .addGap(20, 20, 20))))
        );
        panelSuppressLayout.setVerticalGroup(
            panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSuppressLayout.createSequentialGroup()
                .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelSuppressLayout.createSequentialGroup()
                        .addComponent(buttonSuppress)
                        .addGap(3, 3, 3)
                        .addComponent(buttonUndoSuppress)
                        .addGap(18, 18, 18)
                        .addComponent(buttonAudit))
                    .addGroup(panelSuppressLayout.createSequentialGroup()
                        .addComponent(radioButtonHyperCube)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonModular)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonOptimal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonNetwork, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonCta)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonRounding)
                .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonCellKey)
                    .addComponent(buttonChangePTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelPTable, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelPTableSep)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelSuppressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxInverseWeight)
                    .addComponent(radioButtonUwe))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonMarginal)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonGroupSuppress.add(radioButtonModular);

        labelRowColVars.setText(" ");

        buttonSelectView.setText("Select view...");
        buttonSelectView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectViewActionPerformed(evt);
            }
        });

        buttonTableSummary.setText("Table summary");
        buttonTableSummary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTableSummaryActionPerformed(evt);
            }
        });

        LabelNrOfHorLevels.setText("Hor. levels:");

        comboBoxNrOfHorLevels.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        comboBoxNrOfHorLevels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxNrOfHorLevelsActionPerformed(evt);
            }
        });

        LabelNrOfVertLevels.setText("Vert. levels:");

        comboBoxNrOfVertLevels.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        comboBoxNrOfVertLevels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxNrOfVertLevelsActionPerformed(evt);
            }
        });

        labelDecimals.setText("Number of decimals:");

        comboBoxDecimals.setMaximumRowCount(15);
        comboBoxDecimals.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" }));
        comboBoxDecimals.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxDecimalsActionPerformed(evt);
            }
        });

        checkBoxOutputView.setText("Output view");
        checkBoxOutputView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxOutputViewActionPerformed(evt);
            }
        });

        checkBoxThousandSeparator.setText("3 dig. separator");
        checkBoxThousandSeparator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxThousandSeparatorActionPerformed(evt);
            }
        });

        checkBoxColoredView.setText("Colored view");
        checkBoxColoredView.setToolTipText("Currently only available for frequency count tables");
        checkBoxColoredView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxColoredViewActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelBottomButtonsLayout = new javax.swing.GroupLayout(panelBottomButtons);
        panelBottomButtons.setLayout(panelBottomButtonsLayout);
        panelBottomButtonsLayout.setHorizontalGroup(
            panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBottomButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(buttonTableSummary, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
                    .addComponent(buttonSelectView, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBottomButtonsLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(LabelNrOfHorLevels)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(comboBoxNrOfHorLevels, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBottomButtonsLayout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(LabelNrOfVertLevels)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(comboBoxNrOfVertLevels, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(12, 12, 12)
                .addGroup(panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBottomButtonsLayout.createSequentialGroup()
                        .addComponent(labelDecimals)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxDecimals, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelBottomButtonsLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(checkBoxThousandSeparator)))
                .addGap(30, 30, 30)
                .addGroup(panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxColoredView)
                    .addComponent(checkBoxOutputView))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelBottomButtonsLayout.setVerticalGroup(
            panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBottomButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSelectView)
                    .addComponent(LabelNrOfHorLevels)
                    .addComponent(comboBoxNrOfHorLevels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelDecimals, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboBoxDecimals, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxOutputView))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelBottomButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonTableSummary)
                    .addComponent(LabelNrOfVertLevels)
                    .addComponent(comboBoxNrOfVertLevels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxThousandSeparator)
                    .addComponent(checkBoxColoredView))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelRowColVars)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(labelSpan0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(labelSpan1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(labelSpan2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(labelSpan3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(labelSpan4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(labelSpan5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(labelSpan6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(labelSpan7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxSpan7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPane)
                    .addComponent(panelBottomButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelCellInformation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonRecode, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelSuppress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(10, 10, 10))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan0)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboBoxSpan0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelRowColVars)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSpan1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSpan2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSpan3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSpan4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSpan5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSpan6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelSpan7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSpan7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(scrollPane)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(panelBottomButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelCellInformation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRecode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelSuppress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void comboBoxDecimalsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxDecimalsActionPerformed
        updateFormatters();
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        adjustColumnWidths();
    }//GEN-LAST:event_comboBoxDecimalsActionPerformed

    private void checkBoxThousandSeparatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxThousandSeparatorActionPerformed
        updateFormatters();
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        adjustColumnWidths();
    }//GEN-LAST:event_checkBoxThousandSeparatorActionPerformed

    private void buttonSetStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetStatusActionPerformed
        Map<javax.swing.JButton, CellStatus> statusMap = new HashMap<>();
        statusMap.put(buttonSafe, CellStatus.SAFE_MANUAL);
        statusMap.put(buttonUnsafe, CellStatus.UNSAFE_MANUAL);
        statusMap.put(buttonSecondary, CellStatus.SECONDARY_UNSAFE);
        statusMap.put(buttonProtected, CellStatus.PROTECT_MANUAL);
        
        int r = table.getSelectedRow();
        int c = table.getSelectedColumn();
        
        javax.swing.JButton button = (javax.swing.JButton) evt.getSource();
        CellStatus newStatus = statusMap.get(button);
        if (!tauArgus.SetTableCellStatus(tableSet.index, createDimArray(r, c), newStatus.getValue())) {
            JOptionPane.showMessageDialog(this, "Unable to change the status");
        } else {
            ((AbstractTableModel)table.getModel()).fireTableCellUpdated(r, c);
            Cell cell = getCell(r, c);
            panelCellInformation.update(tableSet, cell, integerFormatter, doubleFormatter);
            organiseSafetyButtons(cell.status);
        }
    }//GEN-LAST:event_buttonSetStatusActionPerformed

    private void buttonNonStructEmptyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNonStructEmptyActionPerformed
        tauArgus.SetAllEmptyNonStructural (tableSet.index);
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
    }//GEN-LAST:event_buttonNonStructEmptyActionPerformed

    private void buttonCostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCostActionPerformed
        int r = table.getSelectedRow();
        int c = table.getSelectedColumn();
        
        Cell cell = tableSet.getCell(createDimArray(r, c));
        while (true) {
            String value = JOptionPane.showInputDialog(this, "Set new cost for (" + doubleFormatter.format(cell.cost) + ")", doubleFormatter.format(cell.cost));
            if (value == null) {
                // the user canceled the input
                return;
            }
            try {
                Double newCost = Double.parseDouble(value);
                if (newCost > 0) {
                    // the user entered correct input
                    if (!tauArgus.SetTableCellCost(tableSet.index, createDimArray(r, c), newCost)) {
                        JOptionPane.showMessageDialog(this, "Unable to modify the cost");
                    } else {
                        ((AbstractTableModel)table.getModel()).fireTableCellUpdated(r, c);
                        cell = getCell(r, c);      
                        panelCellInformation.update(tableSet, cell, integerFormatter, doubleFormatter);
                    }
                    return;
                } 
            }
            catch (NumberFormatException ex) {
                // incorrect input
            }
            // incorrect input
            if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(null, "Illegal value for cost function. Try again?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                break;
            }
        }
    }//GEN-LAST:event_buttonCostActionPerformed

    private boolean VraagMinTabVal(SuppressionMethod soort, TableSet table) {
        double[] xMax = new double[1];
        double xMin = tauArgus.GetMinimumCellValue(tableSet.index, xMax);

        double x = 0.0;
        if (xMin < 0) {
            if (soort == SuppressionMethod.OPTIMAL) {
                if (Application.isAnco()) {
                    if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this, "Negative cell values found.\nOptimal is not guaranteed.\nTry the modular or the hypercube as an alternative.\nDo you want to continue?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                        return false;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Negative cell values found.\nOptimal is not possible.\nTry the modular or the hypercube as an alternative");
                    return false;
                }
            }

            x = xMin * 1.5;
            boolean valid = false;
            while (!valid) {
                String input = JOptionPane.showInputDialog(this, "Minimum lower bound for each cell\n(Smallest cell = " + xMin + ")", String.valueOf(x));
                if (input == null) {
                    // cancel or close button is pressed
                    return false;
                }
                try {
                    x = Math.round(Double.parseDouble(input));
                    if (x <= xMin) {
                        valid = true;
                    }
                }
                catch (NumberFormatException ex) { }
                if (!valid) {
                    if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this, "Illegal value for Minimum Table value", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                        x = xMin;
                        valid = true;
                    } else {
                        if (x > 0) {
                            x = 0;
                        }
                    }
                }
            }
        }
        // This does not make sense ????!!!!!!!
        table.minTabVal = x;
        //if (xMax[0]<0) {xMax[0] = 0;}
        //table.maxTabVal = 1.5 * xMax[0];
        return true;
    }

    private void buttonRecodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRecodeActionPerformed
        DialogGlobalRecode dialog = new DialogGlobalRecode(getParentFrame(), true);
        dialog.showDialog(tableSet);
        int oldRowExpVarIndex = rowExpVarIndex;
        int oldColumnExpVarIndex = columnExpVarIndex;
        Variable RowVar= null, ColVar = null;
        // SetTable resets rowExpVarIndex and columnExpVarIndex to 0 and 1.
        setTable(tableSet);
        if (oldRowExpVarIndex != -1) RowVar = tableSet.expVar.get(oldRowExpVarIndex);
        if (oldColumnExpVarIndex != -1) ColVar = tableSet.expVar.get(oldColumnExpVarIndex);
        setRowColumnVariables(RowVar,ColVar);
    }//GEN-LAST:event_buttonRecodeActionPerformed

    private void checkBoxOutputViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxOutputViewActionPerformed
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        adjustColumnWidths();
    }//GEN-LAST:event_checkBoxOutputViewActionPerformed

    private void comboBoxNrOfHorLevelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxNrOfHorLevelsActionPerformed
        int maxLevel;
        if (!isSingleColumn){
            maxLevel = Integer.parseInt((String)comboBoxNrOfHorLevels.getSelectedItem());
            setLevel(codeList[columnExpVarIndex], maxLevel+1);
            createColumnIndices();
        }
        ((AbstractTableModel)table.getModel()).fireTableStructureChanged();
        adjustColumnWidths();
    }//GEN-LAST:event_comboBoxNrOfHorLevelsActionPerformed

    private void comboBoxNrOfVertLevelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxNrOfVertLevelsActionPerformed
        int maxLevel = Integer.parseInt((String)comboBoxNrOfVertLevels.getSelectedItem());
        setLevel(codeList[rowExpVarIndex], maxLevel+1);
        createRowIndices();
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        adjustColumnWidths();
    }//GEN-LAST:event_comboBoxNrOfVertLevelsActionPerformed

    private void adjustColumnWidths() {
        if (!isAdjusting) {
            TableColumnResizer.adjustColumnPreferredWidths(table, true);            
        }
    }
    
    private void comboBoxSpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxSpanActionPerformed
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        adjustColumnWidths();
    }//GEN-LAST:event_comboBoxSpanActionPerformed

    private void buttonSelectViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectViewActionPerformed
        DialogSelectRowColumn dialog = new DialogSelectRowColumn(null, true);
        if (dialog.showDialog(tableSet) == DialogSelectRowColumn.APPROVE_OPTION) {
            setRowColumnVariables(dialog.getRowVariable(), dialog.getColumnVariable());
        }
    }//GEN-LAST:event_buttonSelectViewActionPerformed

    private void buttonTableSummaryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTableSummaryActionPerformed
        DialogTableSummary dialog = new DialogTableSummary((JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this), true);
        dialog.showDialog(tableSet);
    }//GEN-LAST:event_buttonTableSummaryActionPerformed

    private void buttonPrioryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPrioryActionPerformed
       DialogReadApriori dialog = new DialogReadApriori(null, true);
       dialog.SetAprioyTable(tableSet.index);
       dialog.ShowDialog();  
      ((AbstractTableModel)table.getModel()).fireTableDataChanged();
    }//GEN-LAST:event_buttonPrioryActionPerformed

    private void checkBoxColoredViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxColoredViewActionPerformed
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        adjustColumnWidths();
    }//GEN-LAST:event_checkBoxColoredViewActionPerformed

    private void radioButtonCellKeyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonCellKeyActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonCellKeyActionPerformed

    private void radioButtonNetworkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonNetworkActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonNetworkActionPerformed

    private void buttonAuditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAuditActionPerformed
        final SwingWorker <Void, Void> worker = new SwingWorker<Void, Void>(){
            @Override
            protected Void doInBackground() throws ArgusException, Exception{
                OptiSuppress.RunAudit(tableSet);
                return null;
            }

            @Override
            protected void done(){
                try{
                    get();
                    JOptionPane.showMessageDialog(null, "The audit has been successfully performed\n" +
                                                    tableSet.auditExactDisclosure + " cells could be exactly disclosed\n"+
                                                    tableSet.auditPartialDisclosure + " cells could be partially disclosed\n");
                    ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                    adjustColumnWidths();
                    updateSuppressButtons();
                    if ((tableSet.auditExactDisclosure + tableSet.auditPartialDisclosure)>0){
                        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,"Do you want to see the report?")){
                            DialogHtmlViewer dialog = new DialogHtmlViewer(new javax.swing.JFrame(), true);
                            dialog.showDialog("Audit result","file:////"+ Application.getTempFile("audit_"+tableSet.index+ ".html"));
                        }
                    }
                }
                catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }//GEN-LAST:event_buttonAuditActionPerformed

    private void buttonUndoSuppressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUndoSuppressActionPerformed
        TableService.undoSuppress(tableSet.index);
        updateSuppressButtons();
        SystemUtils.writeLogbook("Protection for table: " + tableSet.toString() +  " has been removed");
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        TableColumnResizer.adjustColumnPreferredWidths(table, true);
    }//GEN-LAST:event_buttonUndoSuppressActionPerformed

    private void buttonSuppressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSuppressActionPerformed
        JFrame parentFrame = getParentFrame();
        SuppressionMethod Soort = SuppressionMethod.GHMITER;
        if (radioButtonHyperCube.isSelected()) Soort = SuppressionMethod.GHMITER;
        if (radioButtonModular.isSelected()) Soort = SuppressionMethod.HITAS;
        if (radioButtonOptimal.isSelected()) Soort = SuppressionMethod.OPTIMAL;
        if (radioButtonMarginal.isSelected()) Soort = SuppressionMethod.MARGINAL;
        if (radioButtonNetwork.isSelected()) Soort = SuppressionMethod.NETWORK;
        if (radioButtonUwe.isSelected()) Soort = SuppressionMethod.UWE;
        if (radioButtonCta.isSelected()) Soort = SuppressionMethod.CTA;
        if (radioButtonRounding.isSelected()) Soort = SuppressionMethod.ROUNDING;
        if (radioButtonCellKey.isSelected()) Soort = SuppressionMethod.CELLKEY;

        if (Soort.isAdditivityDesirable() && !tableSet.isAdditive) {
            if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this, "Table is not additive. Optimisation routines might be tricky\nDo you want to proceed?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                return;
            }
        }

        if (Soort.isUsingSuppliedCostFunction()) {
            if (tableSet.costFunc == TableSet.COST_DIST && !Soort.canUseDistanceFunction()) {
                JOptionPane.showMessageDialog(null, "Distance function is not available for this solution");
                return;
            }
        }

        CellStatusStatistics statistics = tableSet.getCellStatusStatistics();
        if (statistics == null) {
            return; // TODO Show message
        }
        int totalUnsafeCells  = statistics.totalPrimaryUnsafe();

        if (!Soort.isCosmetic() && !Soort.isCellKey()) {
            if (totalUnsafeCells == 0) {
                JOptionPane.showMessageDialog(this, "No unsafe cells found\nNo protection required");
                return;
            }
        }

        if (Soort == SuppressionMethod.OPTIMAL && totalUnsafeCells > 50) {
            if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this, "This table contains " + totalUnsafeCells + " unsafe cells\nthis might take a long time; do you want to proceed?", "Question", JOptionPane.YES_NO_OPTION)) {
                return;
            }
        }

        if (Soort.isMinMaxTableValueNeeded()) {
            if (!VraagMinTabVal(Soort, tableSet)) {
                return;
            }
        }

        switch(Soort) {
        case ROUNDING:
            if (Application.solverSelected == Application.SOLVER_CPLEX){
                JOptionPane.showMessageDialog(null, "Whether controlled rounding can be used when Cplex is selected as solver, depends on your specific license",
                    "", JOptionPane.INFORMATION_MESSAGE);
            }

            DialogRoundingParameters paramsR = new DialogRoundingParameters(parentFrame, true);
            if (paramsR.showDialog(tableSet) == DialogRoundingParameters.APPROVE_OPTION) {
                final SwingWorker <Integer, Void> worker = new ProgressSwingWorker<Integer, Void>(ProgressSwingWorker.ROUNDER,"Rounding") {
                    @Override
                    protected Integer doInBackground() throws ArgusException, Exception{
                        super.doInBackground();
                        OptiSuppress.RunRounder(tableSet, getPropertyChangeListener());
                        return null;
                    }

                    @Override
                    protected void done(){
                        super.done();
                        try{
                            get();
                            JOptionPane.showMessageDialog(null, "The table has been rounded\n"
                                + "Number of steps: " + tableSet.roundMaxStep + "\n"
                                + "Max step: " + StrUtils.formatDouble(tableSet.roundMaxJump, tableSet.respVar.nDecimals) + "\n"
                                + "Processing time: " + StrUtils.timeToString(tableSet.processingTime));
                            ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                            adjustColumnWidths();
                            updateSuppressButtons();
                        }
                        catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                        } catch (ExecutionException ex) {
                            JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                        }
                    }
                };

                worker.execute();
                }
                break;
            case CTA:  //do CTA
                final int i=JOptionPane.showConfirmDialog(parentFrame, "Do you prefer to use the expert version?", "Select CTA version", JOptionPane.YES_NO_CANCEL_OPTION);
                if ((i == JOptionPane.YES_OPTION)||(i == JOptionPane.NO_OPTION) ){
                    final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
                        @Override 
                        protected Void doInBackground() throws ArgusException, Exception{
                            OptiSuppress.RunCTA(tableSet, (i == JOptionPane.YES_OPTION));
                            return null;
                        } 
                        @Override 
                        protected void done(){
                            try{
                                get();
                                JOptionPane.showMessageDialog(null, "The CTA procedure has been completed\n" +
                                    tableSet.nSecond+ " cells have been modified\n"+
                                    StrUtils.timeToString(tableSet.processingTime) + " needed");
                                ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                                adjustColumnWidths();
                                updateSuppressButtons();
                            } catch (InterruptedException ex) {
                                LOGGER.log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                            }
                        }
                    };
                    worker.execute();
                }
                break;
            case UWE:
                DialogModularParameters uweParams = new DialogModularParameters(parentFrame, tableSet, false, true);
                if (uweParams.showDialog() == DialogModularParameters.APPROVE_OPTION){
                    final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
                        @Override 
                        protected Void doInBackground() throws ArgusException, Exception{
                            OptiSuppress.RunUWE(tableSet);
                            return null;
                        } 
                        @Override 
                        protected void done(){
                            try{
                                get();
                                JOptionPane.showMessageDialog(null, "The UWE procedure has finished the protection\n" +
                                    tableSet.nSecond+" cells have been suppressed\n"+
                                    StrUtils.timeToString(tableSet.processingTime) + " needed");
                                ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                                adjustColumnWidths();
                                updateSuppressButtons();
                            } catch (InterruptedException ex) {
                                LOGGER.log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                            }
                        }  
                    };
                    worker.execute();
                }
                break;
            case GHMITER:
                DialogHypercubeParameters paramsG = new DialogHypercubeParameters(parentFrame, true);
                if (paramsG.showDialog(tableSet) == DialogHypercubeParameters.APPROVE_OPTION) {
                    final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
                        @Override 
                        protected Void doInBackground() throws ArgusException, Exception{
                            GHMiter.RunGHMiter(tableSet);
                            return null;
                        } 
                        
                        @Override
                        protected void done(){
                            try{
                                get();
                                JOptionPane.showMessageDialog(null, "The Hypercube has finished the protection\n" +
                                                                    tableSet.nSecond+" cells have been suppressed\n"+
                                                                    tableSet.ghMiterMessage +
                                                                    StrUtils.timeToString(tableSet.processingTime) + " needed");
                                if (argus.utils.TauArgusUtils.ExistFile(Application.getTempFile("frozen.txt"))){
                                    DialogInfo Info = new DialogInfo(getParentFrame(), true);
                                    Info.addLabel("Overview of the frozen cells");
                                    try{
                                        Info.addTextFile(Application.getTempFile("frozen.txt"));}
                                    catch (ArgusException ex1){}
                                    Info.setVisible(true);
                                }
                                ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                                adjustColumnWidths();
                                updateSuppressButtons();                                
                            } catch (InterruptedException ex) {
                                LOGGER.log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                            }
                        }
                    };
                    worker.execute();
                }
                break;
            case HITAS:
                DialogModularParameters params = new DialogModularParameters(parentFrame, tableSet, false, true);
                if (params.showDialog() == DialogModularParameters.APPROVE_OPTION){
                    final SwingWorker <Integer, Void> worker = new ProgressSwingWorker<Integer, Void>(ProgressSwingWorker.DOUBLE,"Modular approach") {
                        @Override
                        protected Integer doInBackground() throws ArgusException, Exception{
                            super.doInBackground();
                            OptiSuppress.RunModular(tableSet, getPropertyChangeListener());
                            return null;
                        }

                        @Override
                        protected void done(){
                            super.done();
                            try{
                                get();
                                JOptionPane.showMessageDialog(null, "Modular has finished the protection\n"
                                    + tableSet.nSecond + " cells have been suppressed\n"
                                    + StrUtils.timeToString(tableSet.processingTime) + " needed");
                                tableSet.undoAudit();
                                ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                                adjustColumnWidths();
                                updateSuppressButtons();
                            }
                            catch (InterruptedException ex) {
                                LOGGER.log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                            }
                        }
                    };
                    worker.execute();
                }
                break;
            case OPTIMAL:
                params = new DialogModularParameters(parentFrame, tableSet, true, true);
                if (params.showDialog() == DialogModularParameters.APPROVE_OPTION){
                    final SwingWorker <Void, Void> worker = new ProgressSwingWorker<Void, Void>(ProgressSwingWorker.VALUES,"Optimal approach") {

                        // called in a separate thread...
                        @Override
                        protected Void doInBackground() throws ArgusException, Exception{
                            super.doInBackground();
                            OptiSuppress.RunOptimal(tableSet, getPropertyChangeListener(), checkBoxInverseWeight.isSelected(), false, 1);
                            return null;
                        }

                        // called on the GUI thread
                        @Override
                        protected void done(){
                            super.done();
                            try{
                                get();
                                JOptionPane.showMessageDialog(null, "Optimal has finished the protection\n"
                                    + tableSet.nSecond + " cells have been suppressed\n"
                                    + StrUtils.timeToString(tableSet.processingTime) + " needed");
                                tableSet.undoAudit();
                                ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                                adjustColumnWidths();
                                updateSuppressButtons();
                            }
                            catch (InterruptedException ex) {
                                LOGGER.log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                            }
                        }
                    };
                    worker.execute();
                }
                break;
            case NETWORK:
                try { OptiSuppress.TestNetwork(tableSet);
                }
                catch (ArgusException ex){
                    JOptionPane.showMessageDialog(null,ex.getMessage());
                    break;
                }
                DialogNetwork paramsN = new DialogNetwork(parentFrame, true, tableSet);
                
                if (paramsN.showDialog() == DialogRoundingParameters.APPROVE_OPTION) {
                    final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
                        @Override 
                        protected Void doInBackground() throws ArgusException, Exception{
                            OptiSuppress.RunNetwork(tableSet);
                            return null;
                        } 
                        @Override 
                        protected void done(){
                            try{
                                get();
                                JOptionPane.showMessageDialog(null, "The network has finished the protection\n" +
                                    tableSet.nSecond+" cells have been suppressed\n"
                                    + StrUtils.timeToString(tableSet.processingTime) + " needed");
                                ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                                adjustColumnWidths();
                                updateSuppressButtons();
                            } catch (InterruptedException ex) {
                                LOGGER.log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                            }
                        }  
                    };
                    worker.execute();                    
                }
                break;
            case MARGINAL:
                JOptionPane.showMessageDialog(null,"The marginal method still has to be implemented");
                break;
            case CELLKEY:
                if (tableSet.holding){
                    JOptionPane.showMessageDialog(null,"Sorry, Cell Key Method not available when \"holdings\" are used","Warning",JOptionPane.WARNING_MESSAGE);
                    break;
                }

                String message="";
                if (tableSet.respVar.isResponse()){ // magnitude table
                    if (tableSet.respVar.CKMType.equals("N")) message = "No cell key method specified for this variable";
                    if (tableSet.respVar.CKMscaling.isEmpty()) message += "\n<SCALING> is not specified but is mandatory in .rda-file for variable "+tableSet.respVar.name;
                    if (tableSet.respVar.CKMType.equals("T") && (tableSet.respVar.CKMTopK>1) && tableSet.respVar.CKMapply_even_odd) message += "\n<PARITY>=Y is not allowed when <CKM>=T(TopK) with TopK > 1"; 
                    if ((tableSet.respVar.CKMseparation) && (tableSet.cellkeyVar.PTableFileSep==null)) message += "\nWith <SEPARATION>=Y you need to specify a separate ptable for small values";
                }
                if (!message.isEmpty()){
                    JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
                    break;
                }
 
                if (tableSet.respVar.isResponse()){
                    DialogMuC GetmuC = new DialogMuC(parentFrame, tableSet, true);
                    if (tableSet.domRule || tableSet.frequencyRule || tableSet.pqRule || tableSet.piepRule[0] || tableSet.piepRule[1]) {
                        GetmuC.showDialog(); // returns DialogMuC.APPROVE_OPTION or DialogMuC.CANCEL_OPTION but return value is not (yet) used
                    }
                }
                
                final SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){
                    @Override 
                    protected Boolean doInBackground() throws ArgusException, Exception{
                        boolean result = false;
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        
                        if (tableSet.respVar.type == Type.FREQUENCY)
                            result = OptiSuppress.RunCellKey(tableSet, tableSet.cellkeyVar.PTableFile);
                        else
                            result = OptiSuppress.RunCellKeyCont(tableSet, tableSet.cellkeyVar.PTableFileCont, tableSet.cellkeyVar.PTableFileSep);
                        
                        setCursor(Cursor.getDefaultCursor());
                        return result;
                    } 
                    
                    @Override 
                    protected void done(){
                        try{
                            boolean result = get();
                            if (result)
                                JOptionPane.showMessageDialog(null, "The Cell Key Method has been applied succesfully in " + tableSet.processingTime + " seconds\n");
                            ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                            adjustColumnWidths();
                            updateSuppressButtons();
                        } 
                        catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                        } 
                        catch (ExecutionException ex) {
                            JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                        }
                        finally {setCursor(Cursor.getDefaultCursor());}
                    }
                };
                worker.execute();
                break;
            }
            tableSet.undoAudit();
    }//GEN-LAST:event_buttonSuppressActionPerformed

    private void checkBoxInverseWeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxInverseWeightActionPerformed
        // TODO add your handling code here:
        //        if (checkBoxInverseWeight.isSelected()){
            //          JOptionPane.showMessageDialog(null, "The inverse weight has not yet been implemented");
            //          checkBoxInverseWeight.setSelected(false);
            //        }
    }//GEN-LAST:event_checkBoxInverseWeightActionPerformed

    private void radioButtonUweActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonUweActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonUweActionPerformed

    private void radioButtonCtaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonCtaActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonCtaActionPerformed

    private void radioButtonRoundingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonRoundingActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonRoundingActionPerformed

    private void radioButtonMarginalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonMarginalActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonMarginalActionPerformed

    private void radioButtonOptimalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonOptimalActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonOptimalActionPerformed

    private void radioButtonModularActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonModularActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonModularActionPerformed

    private void radioButtonHyperCubeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonHyperCubeActionPerformed
        updateSuppressButtons();
    }//GEN-LAST:event_radioButtonHyperCubeActionPerformed

    private void buttonChangePTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonChangePTableActionPerformed
        DialogChangePTable dialog = new DialogChangePTable(this.getParentFrame(), true);
        if (dialog.showDialog(tableSet)==DialogChangePTable.APPROVE_OPTION) {
            if (tableSet.respVar.isResponse()){
                labelPTable.setText("ptablefileCont: "+tableSet.cellkeyVar.PTableFileCont.substring(tableSet.cellkeyVar.PTableFileCont.lastIndexOf("\\")+1));
            }
            else{
                labelPTable.setText("ptablefile: "+tableSet.cellkeyVar.PTableFile.substring(tableSet.cellkeyVar.PTableFile.lastIndexOf("\\")+1));                
            }
        }
    }//GEN-LAST:event_buttonChangePTableActionPerformed

    private void organiseSafetyButtons(CellStatus status) {
        if (status.isEmpty()) {
            buttonSafe.setEnabled(false);
            buttonUnsafe.setEnabled(false);
            buttonCost.setEnabled(false);
            buttonProtected.setEnabled(false);
            buttonSecondary.setEnabled(false);
        } else {
            switch(status.getCategory()) {
                case SAFE_PROTECTED:
                    buttonSafe.setEnabled(true);
                    buttonUnsafe.setEnabled(false);
                    buttonCost.setEnabled(false);
                    buttonProtected.setEnabled(false);
                    buttonSecondary.setEnabled(false);
                    break;
                case SAFE_NOT_PROTECTED:
                    buttonSafe.setEnabled(false);
                    buttonUnsafe.setEnabled(true);
                    buttonCost.setEnabled(true);
                    buttonProtected.setEnabled(true);
                    buttonSecondary.setEnabled(Application.isAnco() && tableSet.suppressed != TableSet.SUP_NO);
                    break;
                case PRIMARY_UNSAFE:
                    buttonSafe.setEnabled(true);
                    buttonUnsafe.setEnabled(false);
                    buttonCost.setEnabled(false);
                    buttonProtected.setEnabled(false);
                    buttonSecondary.setEnabled(false);
                    break;
                case SECONDARY_UNSAFE:
                    buttonSafe.setEnabled(Application.isAnco());
                    buttonUnsafe.setEnabled(false);
                    buttonCost.setEnabled(false);
                    buttonProtected.setEnabled(false);
                    buttonSecondary.setEnabled(false);
                    break;
            }
        }
        buttonNonStructEmpty.setEnabled(true);
        if (tableSet.suppressed != TableSet.SUP_NO) {
            buttonSafe.setEnabled(false);
            if (status == CellStatus.SECONDARY_UNSAFE) {
                // || status == CellStatus.SECONDARY_UNSAFE_MANUAL {
                buttonSafe.setEnabled(Application.isAnco());
            }
            buttonUnsafe.setEnabled(false);
            buttonProtected.setEnabled(false);
            buttonCost.setEnabled(false);
            buttonNonStructEmpty.setEnabled(false);
        }        
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LabelNrOfHorLevels;
    private javax.swing.JLabel LabelNrOfVertLevels;
    private javax.swing.JButton buttonAudit;
    private javax.swing.JButton buttonChangePTable;
    private javax.swing.JButton buttonCost;
    private javax.swing.ButtonGroup buttonGroupSuppress;
    private javax.swing.JButton buttonNonStructEmpty;
    private javax.swing.JButton buttonPriory;
    private javax.swing.JButton buttonProtected;
    private javax.swing.JButton buttonRecode;
    private javax.swing.JButton buttonSafe;
    private javax.swing.JButton buttonSecondary;
    private javax.swing.JButton buttonSelectView;
    private javax.swing.JButton buttonSuppress;
    private javax.swing.JButton buttonTableSummary;
    private javax.swing.JButton buttonUndoSuppress;
    private javax.swing.JButton buttonUnsafe;
    private javax.swing.JCheckBox checkBoxColoredView;
    private javax.swing.JCheckBox checkBoxInverseWeight;
    private javax.swing.JCheckBox checkBoxOutputView;
    private javax.swing.JCheckBox checkBoxThousandSeparator;
    private javax.swing.JComboBox comboBoxDecimals;
    private javax.swing.JComboBox<String> comboBoxNrOfHorLevels;
    private javax.swing.JComboBox<String> comboBoxNrOfVertLevels;
    private javax.swing.JComboBox<String> comboBoxSpan0;
    private javax.swing.JComboBox<String> comboBoxSpan1;
    private javax.swing.JComboBox<String> comboBoxSpan2;
    private javax.swing.JComboBox<String> comboBoxSpan3;
    private javax.swing.JComboBox<String> comboBoxSpan4;
    private javax.swing.JComboBox<String> comboBoxSpan5;
    private javax.swing.JComboBox<String> comboBoxSpan6;
    private javax.swing.JComboBox<String> comboBoxSpan7;
    private javax.swing.JLabel labelDecimals;
    private javax.swing.JLabel labelPTable;
    private javax.swing.JLabel labelPTableSep;
    private javax.swing.JLabel labelRowColVars;
    private javax.swing.JLabel labelSpan0;
    private javax.swing.JLabel labelSpan1;
    private javax.swing.JLabel labelSpan2;
    private javax.swing.JLabel labelSpan3;
    private javax.swing.JLabel labelSpan4;
    private javax.swing.JLabel labelSpan5;
    private javax.swing.JLabel labelSpan6;
    private javax.swing.JLabel labelSpan7;
    private javax.swing.JPanel panelBottomButtons;
    private tauargus.gui.PanelCellDetails panelCellInformation;
    private javax.swing.JPanel panelStatus;
    private javax.swing.JPanel panelSuppress;
    private javax.swing.JRadioButton radioButtonCellKey;
    private javax.swing.JRadioButton radioButtonCta;
    private javax.swing.JRadioButton radioButtonHyperCube;
    private javax.swing.JRadioButton radioButtonMarginal;
    private javax.swing.JRadioButton radioButtonModular;
    private javax.swing.JRadioButton radioButtonNetwork;
    private javax.swing.JRadioButton radioButtonOptimal;
    private javax.swing.JRadioButton radioButtonRounding;
    private javax.swing.JRadioButton radioButtonUwe;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
