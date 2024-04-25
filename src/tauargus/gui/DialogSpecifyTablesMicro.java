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

import argus.utils.SystemUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.Metadata;
import tauargus.model.ProgressSwingWorker;
import tauargus.model.TableSet;
import tauargus.model.Variable;
import tauargus.service.TableService;
import tauargus.utils.DoubleInputVerifier;
import tauargus.utils.IntegerInputVerifier;
import tauargus.utils.MouseJListDoubleClickedListener;
import tauargus.utils.SingleListSelectionModel;
import tauargus.utils.SwingUtils;

public class DialogSpecifyTablesMicro extends DialogBase {

    /*
     * Makes it possible to use <ESC> to close the Dialog (cancel)
     * Different from DialogBase: needs additinoal action in case of Cancel
     */
    @Override
    protected JRootPane createRootPane() {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                buttonCancelActionPerformed(actionEvent);
            }
        };    
 
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane thisrootPane = new JRootPane();
        thisrootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return thisrootPane;
    }
 
    
    // ***** Dialog Return Values *****
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;
    private boolean hasBeenModified = false;

    private static final Logger LOGGER = Logger.getLogger(DialogSpecifyTablesMicro.class.getName());
    
     /*
     Creates new form DialogSpecifyTablesMicro
     */
    public DialogSpecifyTablesMicro(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        createComponentArrays();
        
        labelMSC.setVisible(Application.isAnco());
        textFieldMSC.setVisible(Application.isAnco());
        explanatoryVariableListModel = new DefaultListModel<>();
        listExplanatoryVariables.setModel(explanatoryVariableListModel);
        listExplanatoryVariables.setCellRenderer(new VariableNameCellRenderer());
        listExplanatoryVariables.addMouseListener(new MouseJListDoubleClickedListener<Variable>(listExplanatoryVariables) {
            @Override
            public void itemDoubleClicked(JList<Variable> list, int itemIndex) {
                Variable variable = listExplanatoryVariables.getSelectedValue();
                if (!selectedExplanatoryVariableListModel.contains(variable)) {
                    selectedExplanatoryVariableListModel.addElement(variable);
                    organiseDistanceOption();
                }
            }
        });
        selectedExplanatoryVariableListModel = new DefaultListModel<>();
        listSelectedExplanatoryVariables.setModel(selectedExplanatoryVariableListModel);
        listSelectedExplanatoryVariables.setCellRenderer(new VariableNameCellRenderer());
        listSelectedExplanatoryVariables.addMouseListener(new MouseJListDoubleClickedListener<Variable>(listSelectedExplanatoryVariables) {
            @Override
            public void itemDoubleClicked(JList<Variable> list, int itemIndex) {
                selectedExplanatoryVariableListModel.removeElement(listSelectedExplanatoryVariables.getSelectedValue());
                organiseDistanceOption();
            }
        });
        responseVariableListModel = new DefaultListModel<>();
        listResponseVariables.setModel(responseVariableListModel);
        listResponseVariables.setCellRenderer(new VariableNameCellRenderer());
        listResponseVariables.setSelectionModel(new SingleListSelectionModel());
        listResponseVariables.addMouseListener(new MouseJListDoubleClickedListener<Variable>(listResponseVariables) {
            @Override
            public void itemDoubleClicked(JList<Variable> list, int itemIndex) {
                Variable variable = listResponseVariables.getSelectedValue();
                textFieldResponseVariable.setText(variable.name);
                setResponseVariable(variable);
            }
        });

        // used form adding to list of response variables in showDialog method
        freqVar = Application.getFreqVar();
        tableTables.getTableHeader().setReorderingAllowed(false);        
        tableSetListTableModel = new TableSetListTableModel();
        tableSetListTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                boolean b = tableSetListTableModel.getRowCount() != 0;
                buttonDeleteTable.setEnabled(b);
                buttonComputeTables.setEnabled(b);
            }
        });
        tableTables.setModel(tableSetListTableModel);
        tableTables.setSelectionModel(new SingleListSelectionModel());

        DoubleInputVerifier doubleInputVerifier = new DoubleInputVerifier();
        textFieldLambda.setInputVerifier(doubleInputVerifier);
        textFieldMSC.setInputVerifier(doubleInputVerifier);

        IntegerInputVerifier integerInputVerifier = new IntegerInputVerifier();
        for (int i=0; i<TableSet.MAX_RULE_PAR_SETS; i++) {
            textFieldDomK[i].setInputVerifier(integerInputVerifier);
            textFieldDomN[i].setInputVerifier(integerInputVerifier);
            textFieldPqP[i].setInputVerifier(integerInputVerifier);
            textFieldPqQ[i].setInputVerifier(integerInputVerifier);
            textFieldPqN[i].setInputVerifier(integerInputVerifier);
            textFieldReq[i].setInputVerifier(integerInputVerifier);
        }
        for (int i=0; i<TableSet.MAX_PIEPS; i++) {
            textFieldReqFreq[i].setInputVerifier(integerInputVerifier);
            textFieldReqRange[i].setInputVerifier(integerInputVerifier);
        }
        for (int i=0; i<TableSet.MAX_FREQ_PAR_SETS; i++) {
            textFieldMinFreq[i].setInputVerifier(integerInputVerifier);
            textFieldRange[i].setInputVerifier(integerInputVerifier);
        }
        textFieldlZeroUnsafeRange.setInputVerifier(doubleInputVerifier);
        textFieldManualSafetyRange.setInputVerifier(integerInputVerifier);
    }

    public int showDialog(Metadata metadata) {
        this.metadata = metadata;
        
        // Let table use it's original variables 
        //????????????????????????????????????????????????????????????
        // PWOF 2017-12-08
        // This is only needed in case of "Compute Tables"?
        // In case of "Cancel" this resets the variables incorrectly?
        // But why only in case of more than 1 table?
        //????????????????????????????????????????????????????????????
/*        for (int i = 0; i < TableService.numberOfTables(); i++) {
            TableSet table = TableService.getTable(i);
            for (int j = 0; j < table.expVar.size(); j++) {
                table.expVar.set(j, table.expVar.get(j).originalVariable);
            }
        }
*/        
        if (!Application.isAnco()) {
            checkBoxPqRule.setText("P%-rule");
            tabbedPaneRules.setTitleAt(1, "P%-rule");
            labelPqQ.setVisible(false);
            textFieldPqInd1Q.setVisible(false);
            textFieldPqInd2Q.setVisible(false);
            textFieldPqHold1Q.setVisible(false);
            textFieldPqHold2Q.setVisible(false);
        }

        explanatoryVariableListModel.clear();
        selectedExplanatoryVariableListModel.clear();
        responseVariableListModel.clear();
        setResponseVariable(null);
        setShadowVariable(null);
        setCostVariable(null);

        for (Variable variable : metadata.variables) {
            if (variable.isCategorical()) {
                explanatoryVariableListModel.addElement(variable);
            }
            if (variable.isResponse()) {
                responseVariableListModel.addElement(variable);
            }
        }
        responseVariableListModel.addElement(freqVar);
        tableSetListTableModel.setData();

        if (explanatoryVariableListModel.isEmpty()) {
            listExplanatoryVariables.clearSelection();
        } else {
            listExplanatoryVariables.setSelectedIndex(0);
        }
        if (responseVariableListModel.isEmpty()) {
            listResponseVariables.clearSelection();
        } else {
            listResponseVariables.setSelectedIndex(0);
        }
        if (tableTables.getRowCount() == 0) {
            tableTables.clearSelection();
        } else {
            tableTables.setRowSelectionInterval(0, 0);
        }
        checkBoxRequestRule.setEnabled(metadata.containsRequestVariable());
        checkBoxApplyWeights.setEnabled(metadata.containsWeightVariable());
        checkBoxUseHoldingsInfo.setEnabled(metadata.containsHoldingVariable());
                
        //load(new TableSet(metadata));
        if (TableService.numberOfTables()==0)
        {
            load(new TableSet(metadata));
            loadDefaultsFromRegistry();
        }
        else
        {
            load(TableService.getTable(0));
        }
        organiseAllOptions();

        setLocationRelativeTo(this.getParent());        
        setVisible(true);
        return returnValue;
    }

    /**
     * Load the form fields with the data from the given table set
     */
    private void load(TableSet tableSet) {
        selectedExplanatoryVariableListModel.clear();
        tableSet.expVar.forEach((variable) -> {
            selectedExplanatoryVariableListModel.addElement(variable);
        });

        setResponseVariable(tableSet.respVar);
        setShadowVariable(tableSet.shadowVar);
        textFieldCostVariable.setText("");
        switch (tableSet.costFunc) {
            case TableSet.COST_DIST:
                radioButtonDistanceFunction.setSelected(true);
                break;
            case TableSet.COST_UNITY:
                radioButtonUnity.setSelected(true);
                break;
            case TableSet.COST_FREQ:
                radioButtonFrequency.setSelected(true);
                break;
            default:
                radioButtonVariable.setSelected(true);
                setCostVariable(tableSet.costVar);
                textFieldLambda.setText(Double.toString(tableSet.lambda));
                textFieldMSC.setText(Double.toString(tableSet.maxScaleCost));
                break;
        }

        checkBoxApplyWeights.setSelected(tableSet.weighted);
        checkBoxMissingSafe.setSelected(tableSet.missingIsSafe);
        checkBoxUseHoldingsInfo.setSelected(tableSet.holding);
        checkBoxDominanceRule.setSelected(tableSet.domRule);
        checkBoxPqRule.setSelected(tableSet.pqRule);
        checkBoxRequestRule.setSelected(tableSet.piepRule[0] || tableSet.piepRule[1]);
        checkBoxMinimumFrequency.setSelected(tableSet.frequencyRule);
        checkBoxZeroUnsafe.setSelected(tableSet.zeroUnsafe);

        textFieldlZeroUnsafeRange.setText(Double.toString(tableSet.zeroRange));
        textFieldManualSafetyRange.setText(Integer.toString(tableSet.manualMarge));

        for (int i = 0; i < TableSet.MAX_RULE_PAR_SETS; i++) {
            textFieldDomN[i].setText(Integer.toString(tableSet.domN[i]));
            textFieldDomK[i].setText(Integer.toString(tableSet.domK[i]));

            textFieldPqP[i].setText(Integer.toString(tableSet.pqP[i]));
            textFieldPqQ[i].setText(Integer.toString(tableSet.pqQ[i]));
            textFieldPqN[i].setText(Integer.toString(tableSet.pqN[i]));

            textFieldReq[i].setText(Integer.toString(tableSet.piepPercentage[i]));
        }

        for (int i = 0; i < TableSet.MAX_FREQ_PAR_SETS; i++) {
            textFieldReqRange[i].setText(Integer.toString(tableSet.piepMarge[i]));
            textFieldReqFreq[i].setText(Integer.toString(tableSet.piepMinFreq[i]));
            textFieldRange[i].setText(Integer.toString(tableSet.frequencyMarge[i]));
            if (tableSet.frequencyRule) textFieldMinFreq[i].setText(Integer.toString(tableSet.minFreq[i]));
        }

        organiseAllOptions();

        for (int i = 0; i < tabbedPaneRules.getTabCount(); i++) {
            if (tabbedPaneRules.isEnabledAt(i)) {
                tabbedPaneRules.setSelectedIndex(i);
                break;
            }
        }
    }
    
    private void organiseCostOptions() {
        boolean b = radioButtonVariable.isSelected();
        textFieldCostVariable.setEnabled(b);
        textFieldLambda.setEnabled(b);
        textFieldMSC.setEnabled(b);
    }

    private void organiseDominanceRuleOptions() {
        boolean b = checkBoxDominanceRule.isSelected();
        tabbedPaneRules.setEnabledAt(0, b);
        labelDomN.setEnabled(b);
        labelDomK.setEnabled(b);
        for (int i=0; i<4; i++) {
            if (i == 2) {
                b = b && checkBoxUseHoldingsInfo.isSelected();
            }
            labelDom[i].setEnabled(b);
            textFieldDomN[i].setEnabled(b);
            textFieldDomK[i].setEnabled(b);
        }
    }

    private void organisePqRuleOptions() {
        boolean b = checkBoxPqRule.isSelected();
        tabbedPaneRules.setEnabledAt(1, b);
        labelPqP.setEnabled(b);
        labelPqQ.setEnabled(b);
        labelPqN.setEnabled(b);
        for (int i=0; i<4; i++) {
            if (i==2) {
                b = b && checkBoxUseHoldingsInfo.isSelected();
            }
            labelPq[i].setEnabled(b);
            textFieldPqP[i].setEnabled(b);
            textFieldPqQ[i].setEnabled(b);
            textFieldPqN[i].setEnabled(b);
        }
    }
    
    private void organiseRequestRuleOptions() {
        boolean b = checkBoxRequestRule.isSelected();
        tabbedPaneRules.setEnabledAt(2, b);
        labelReqSafetyRange.setEnabled(b);
        labelReqSafetyRangeHolding.setEnabled(b && checkBoxUseHoldingsInfo.isSelected());
        for (int i=0; i<4; i++) {
            if (i==2) {
                b = b && checkBoxUseHoldingsInfo.isSelected();
            }
            labelReq[i].setEnabled(b);
            textFieldReq[i].setEnabled(b);
        }

        b = checkBoxRequestRule.isSelected();
        for (int i=0; i<2; i++) {
            if (i==1) {
                b = b && checkBoxUseHoldingsInfo.isSelected();
            }
            textFieldReqFreq[i].setEnabled(b);
            labelReqPercentage[i].setEnabled(b);
            textFieldReqRange[i].setEnabled(b);
            labelReqFreq[i].setEnabled(b);
        }
    }

    private void organiseMinimumFrequencyRuleOptions() {
        boolean b = checkBoxMinimumFrequency.isSelected();
        labelFreq.setEnabled(b);
        labelRange.setEnabled(b);
        for (int i=0; i<2; i++) {
            if (i == 1) {
                b = b && checkBoxUseHoldingsInfo.isSelected();
            }
            labelMinFreq[i].setEnabled(b);
            textFieldMinFreq[i].setEnabled(b);
            textFieldRange[i].setEnabled(b);
            labelMinFreqPercentage[i].setEnabled(b);
        }
    }
    
    private void organiseZeroUnsafeRuleOptions() {
        boolean b = checkBoxZeroUnsafe.isSelected();
        labelZeroUnsafeRange.setEnabled(b);
        textFieldlZeroUnsafeRange.setEnabled(b);
    }

    private void organiseDistanceOption() {
        for (Variable variable : Collections.list(selectedExplanatoryVariableListModel.elements())) {
            if (variable.hasDistanceFunction) {
                radioButtonDistanceFunction.setEnabled(true);
                return;
            }
        }
        radioButtonDistanceFunction.setEnabled(false);
        if (radioButtonDistanceFunction.isSelected()) {
            radioButtonVariable.setSelected(true);
            organiseCostOptions();
        }
    }
    
    private void organiseButtonAddTableOption() {
        buttonAddTable.setEnabled(!selectedExplanatoryVariableListModel.isEmpty() && responseVariable != null);
    }
    
    private void organiseAllOptions() {
        organiseCostOptions();
        organiseDominanceRuleOptions();
        organisePqRuleOptions();
        organiseRequestRuleOptions();
        organiseMinimumFrequencyRuleOptions();
        organiseZeroUnsafeRuleOptions();
        organiseButtonAddTableOption();
        organiseDistanceOption();
        SelectPanel();
    }
    
    private void SelectPanel(){
        if (checkBoxDominanceRule.isSelected()){
            tabbedPaneRules.setSelectedComponent(panelDominanceRule);
        }
        else{
            if (checkBoxPqRule.isSelected()){
                tabbedPaneRules.setSelectedComponent(panelPqRule);
            }
            else{
                if (checkBoxRequestRule.isSelected()){
                    tabbedPaneRules.setSelectedComponent(panelRequestRule);
                }
            }
        }        
    }
    
    private void organiseHoldingOptions() {
        organiseDominanceRuleOptions();
        organisePqRuleOptions();
        organiseRequestRuleOptions();
        organiseMinimumFrequencyRuleOptions();
        organiseButtonAddTableOption();
    }
    
    /**
     * Save the form fields in the data from the given table set
     */
    private void save(TableSet tableSet) {
        // Set Variables and cost func...
        List<Variable> variables = Collections.list(selectedExplanatoryVariableListModel.elements());
        tableSet.expVar.addAll(variables);
        tableSet.respVar = responseVariable;
        tableSet.shadowVar = shadowVariable;
        if (radioButtonDistanceFunction.isSelected()) {
            tableSet.costFunc = TableSet.COST_DIST;
        } else if (radioButtonUnity.isSelected()) {
            tableSet.costFunc = TableSet.COST_UNITY;
        } else if (radioButtonFrequency.isSelected()) {
            tableSet.costFunc = TableSet.COST_FREQ;
        } else {
            tableSet.costFunc = TableSet.COST_VAR;
            tableSet.costVar = costVariable;
            tableSet.lambda = Double.parseDouble(textFieldLambda.getText());
            tableSet.maxScaleCost = Double.parseDouble(textFieldMSC.getText());
        }

        // Set booleans...
        tableSet.holding = checkBoxUseHoldingsInfo.isSelected();
        tableSet.weighted = checkBoxApplyWeights.isSelected();
        tableSet.missingIsSafe = checkBoxMissingSafe.isSelected();
        tableSet.domRule = checkBoxDominanceRule.isSelected();
        tableSet.pqRule = checkBoxPqRule.isSelected();
        tableSet.frequencyRule = checkBoxMinimumFrequency.isSelected();
        tableSet.zeroUnsafe = checkBoxZeroUnsafe.isSelected();

        int j = (tableSet.holding && tableSet.metadata.containsHoldingVariable()) ? 4 : 2;
        for (int i=0; i<j; i++) {
               tableSet.domK[i] = Integer.parseInt(textFieldDomK[i].getText());
                tableSet.domN[i] = Integer.parseInt(textFieldDomN[i].getText());
                tableSet.pqP[i] = Integer.parseInt(textFieldPqP[i].getText());
                tableSet.pqQ[i] = Integer.parseInt(textFieldPqQ[i].getText());
                tableSet.pqN[i] = Integer.parseInt(textFieldPqN[i].getText());
                tableSet.piepPercentage[i] = Integer.parseInt(textFieldReq[i].getText());
        }
        for (int i=0; i<2; i++) {
            tableSet.piepRule[i] = checkBoxRequestRule.isSelected() && 
                    ((tableSet.piepPercentage[2*i] != 0) || (tableSet.piepPercentage[2*i+1] != 0));
        }

        // Set request rule (remaining) and frequency rule parameter...
        j = (tableSet.holding && tableSet.metadata.containsHoldingVariable()) ? 2 : 1;
        for (int i=0; i<j; i++) {
            tableSet.piepMarge[i] = Integer.parseInt(textFieldReqRange[i].getText());
            tableSet.piepMinFreq[i] = Integer.parseInt(textFieldReqFreq[i].getText());

            // This if-statement is needed (is not controlled by boolean when calling SetTableSafety-routine) 
            if (tableSet.frequencyRule) {
                tableSet.minFreq[i] = Integer.parseInt(textFieldMinFreq[i].getText());
            } else {
                tableSet.minFreq[i] = 0;
            }
            tableSet.frequencyMarge[i] = Integer.parseInt(textFieldRange[i].getText());
        }
        if (!tableSet.holding) {
            tableSet.minFreq[1] = 0;
        }

        // Other rule parmaters...
        tableSet.zeroRange = Double.parseDouble(textFieldlZeroUnsafeRange.getText());
        tableSet.manualMarge = Integer.parseInt(textFieldManualSafetyRange.getText());
        
        // Other values to be set...
        tableSet.suppressed = TableSet.SUP_NO;
        tableSet.solverUsed = Application.SOLVER_NO;                
        tableSet.singletonUsed = false;
        tableSet.cellkeyVar = metadata.find(tauargus.model.Type.RECORD_KEY);
    }

    private void createComponentArrays() {
        labelDom = new javax.swing.JLabel[] {
            labelDomInd1,
            labelDomInd2,
            labelDomHold1,
            labelDomHold2
        };
        
        textFieldDomN = new javax.swing.JTextField[] {
            textFieldDomInd1N,
            textFieldDomInd2N,
            textFieldDomHold1N,
            textFieldDomHold2N
        };

        textFieldDomK = new javax.swing.JTextField[] {
            textFieldDomInd1K,
            textFieldDomInd2K,
            textFieldDomHold1K,
            textFieldDomHold2K
        };

        labelPq = new javax.swing.JLabel[] {
            labelPqInd1,
            labelPqInd2,
            labelPqHold1,
            labelPqHold2
        };
        
        textFieldPqP = new javax.swing.JTextField[] {
            textFieldPqInd1P,
            textFieldPqInd2P,
            textFieldPqHold1P,
            textFieldPqHold2P
        };

        textFieldPqQ = new javax.swing.JTextField[] {
            textFieldPqInd1Q,
            textFieldPqInd2Q,
            textFieldPqHold1Q,
            textFieldPqHold2Q
        };

        textFieldPqN = new javax.swing.JTextField[] {
            textFieldPqInd1N,
            textFieldPqInd2N,
            textFieldPqHold1N,
            textFieldPqHold2N
        };

        labelReq = new javax.swing.JLabel[] {
            labelReqInd1,
            labelReqInd2,
            labelReqHold1,
            labelReqHold2
        };
        
        textFieldReq = new javax.swing.JTextField[] {
            textFieldReqInd1,
            textFieldReqInd2,
            textFieldReqHold1,
            textFieldReqHold2
        };

        textFieldReqFreq = new javax.swing.JTextField[] {
            textFieldReqIndFreq,
            textFieldReqHoldFreq
        };

        textFieldReqRange = new javax.swing.JTextField[] {
            textFieldReqIndRange,
            textFieldReqHoldRange
        };

        labelReqPercentage = new javax.swing.JLabel[] {
            labelReqIndPercentage,
            labelReqHoldPercentage
        };

        labelReqFreq = new javax.swing.JLabel[] {
            labelReqIndFreq,
            labelReqHoldFreq
        };
        
        labelMinFreq = new javax.swing.JLabel[] {
            labelFreqInd,
            labelFreqHold
        };
        
        textFieldMinFreq = new javax.swing.JTextField[] {
            textFieldIndFreq,
            textFieldHoldFreq
        };

        textFieldRange = new javax.swing.JTextField[] {
            textFieldIndRange,
            textFieldHoldRange
        };
        
        labelMinFreqPercentage = new javax.swing.JLabel[] {
            labelIndPercentage,
            labelHoldPercentage
        };
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupCostVariable = new javax.swing.ButtonGroup();
        panelExplanatoryVariables = new javax.swing.JPanel();
        scrollPaneExplanatoryVariables = new javax.swing.JScrollPane();
        listExplanatoryVariables = new javax.swing.JList<tauargus.model.Variable>();
        buttonExplanatoryAdd = new javax.swing.JButton();
        buttonExplanatoryDelete = new javax.swing.JButton();
        scrollPaneSelectedExplanatoryVariables = new javax.swing.JScrollPane();
        listSelectedExplanatoryVariables = new javax.swing.JList<tauargus.model.Variable>();
        panelCellItems = new javax.swing.JPanel();
        scrollPaneCellItems = new javax.swing.JScrollPane();
        listResponseVariables = new javax.swing.JList<tauargus.model.Variable>();
        buttonResponseAdd = new javax.swing.JButton();
        buttonResponseDelete = new javax.swing.JButton();
        buttonShadowAdd = new javax.swing.JButton();
        buttonShadowDelete = new javax.swing.JButton();
        buttonCostAdd = new javax.swing.JButton();
        buttonCostDelete = new javax.swing.JButton();
        labelResponseVariable = new javax.swing.JLabel();
        textFieldResponseVariable = new javax.swing.JTextField();
        labelShadowVariable = new javax.swing.JLabel();
        textFieldShadowVariable = new javax.swing.JTextField();
        panelCostVariable = new javax.swing.JPanel();
        radioButtonUnity = new javax.swing.JRadioButton();
        radioButtonVariable = new javax.swing.JRadioButton();
        radioButtonFrequency = new javax.swing.JRadioButton();
        radioButtonDistanceFunction = new javax.swing.JRadioButton();
        textFieldCostVariable = new javax.swing.JTextField();
        labelLambda = new javax.swing.JLabel();
        textFieldLambda = new javax.swing.JTextField();
        labelMSC = new javax.swing.JLabel();
        textFieldMSC = new javax.swing.JTextField();
        panelParameters = new javax.swing.JPanel();
        checkBoxDominanceRule = new javax.swing.JCheckBox();
        checkBoxPqRule = new javax.swing.JCheckBox();
        checkBoxRequestRule = new javax.swing.JCheckBox();
        tabbedPaneRules = new javax.swing.JTabbedPane();
        panelDominanceRule = new javax.swing.JPanel();
        labelDomN = new javax.swing.JLabel();
        labelDomK = new javax.swing.JLabel();
        labelDomInd1 = new javax.swing.JLabel();
        textFieldDomInd1N = new javax.swing.JTextField();
        textFieldDomInd1K = new javax.swing.JTextField();
        labelDomInd2 = new javax.swing.JLabel();
        textFieldDomInd2N = new javax.swing.JTextField();
        textFieldDomInd2K = new javax.swing.JTextField();
        separatorDom = new javax.swing.JSeparator();
        labelDomHold1 = new javax.swing.JLabel();
        textFieldDomHold1N = new javax.swing.JTextField();
        textFieldDomHold1K = new javax.swing.JTextField();
        labelDomHold2 = new javax.swing.JLabel();
        textFieldDomHold2N = new javax.swing.JTextField();
        textFieldDomHold2K = new javax.swing.JTextField();
        panelPqRule = new javax.swing.JPanel();
        labelPqP = new javax.swing.JLabel();
        labelPqQ = new javax.swing.JLabel();
        labelPqN = new javax.swing.JLabel();
        labelPqInd1 = new javax.swing.JLabel();
        textFieldPqInd1P = new javax.swing.JTextField();
        textFieldPqInd1Q = new javax.swing.JTextField();
        textFieldPqInd1N = new javax.swing.JTextField();
        labelPqInd2 = new javax.swing.JLabel();
        textFieldPqInd2P = new javax.swing.JTextField();
        textFieldPqInd2Q = new javax.swing.JTextField();
        textFieldPqInd2N = new javax.swing.JTextField();
        labelPqHold1 = new javax.swing.JLabel();
        textFieldPqHold1P = new javax.swing.JTextField();
        textFieldPqHold1N = new javax.swing.JTextField();
        labelPqHold2 = new javax.swing.JLabel();
        textFieldPqHold2P = new javax.swing.JTextField();
        textFieldPqHold2N = new javax.swing.JTextField();
        textFieldPqHold1Q = new javax.swing.JTextField();
        textFieldPqHold2Q = new javax.swing.JTextField();
        separatorPq = new javax.swing.JSeparator();
        panelRequestRule = new javax.swing.JPanel();
        labelReqInd1 = new javax.swing.JLabel();
        textFieldReqInd1 = new javax.swing.JTextField();
        labelReqInd2 = new javax.swing.JLabel();
        textFieldReqInd2 = new javax.swing.JTextField();
        labelReqIndFreq = new javax.swing.JLabel();
        textFieldReqIndFreq = new javax.swing.JTextField();
        labelReqSafetyRange = new javax.swing.JLabel();
        textFieldReqIndRange = new javax.swing.JTextField();
        labelReqIndPercentage = new javax.swing.JLabel();
        separatorReq = new javax.swing.JSeparator();
        labelReqHold1 = new javax.swing.JLabel();
        textFieldReqHold1 = new javax.swing.JTextField();
        labelReqHold2 = new javax.swing.JLabel();
        textFieldReqHold2 = new javax.swing.JTextField();
        labelReqHoldFreq = new javax.swing.JLabel();
        textFieldReqHoldFreq = new javax.swing.JTextField();
        textFieldReqHoldRange = new javax.swing.JTextField();
        labelReqHoldPercentage = new javax.swing.JLabel();
        labelReqSafetyRangeHolding = new javax.swing.JLabel();
        panelMinimumFrequency = new javax.swing.JPanel();
        checkBoxMinimumFrequency = new javax.swing.JCheckBox();
        labelFreq = new javax.swing.JLabel();
        labelRange = new javax.swing.JLabel();
        labelFreqInd = new javax.swing.JLabel();
        textFieldIndFreq = new javax.swing.JTextField();
        textFieldIndRange = new javax.swing.JTextField();
        labelIndPercentage = new javax.swing.JLabel();
        separatorFreq = new javax.swing.JSeparator();
        labelFreqHold = new javax.swing.JLabel();
        textFieldHoldFreq = new javax.swing.JTextField();
        textFieldHoldRange = new javax.swing.JTextField();
        labelHoldPercentage = new javax.swing.JLabel();
        checkBoxApplyWeights = new javax.swing.JCheckBox();
        checkBoxMissingSafe = new javax.swing.JCheckBox();
        checkBoxUseHoldingsInfo = new javax.swing.JCheckBox();
        panelZero = new javax.swing.JPanel();
        checkBoxZeroUnsafe = new javax.swing.JCheckBox();
        labelZeroUnsafeRange = new javax.swing.JLabel();
        textFieldlZeroUnsafeRange = new javax.swing.JTextField();
        labelManualSafetyRange = new javax.swing.JLabel();
        textFieldManualSafetyRange = new javax.swing.JTextField();
        labelManualSafetyRangePercentage = new javax.swing.JLabel();
        buttonDeleteTable = new javax.swing.JButton();
        buttonAddTable = new javax.swing.JButton();
        scrollPaneTables = new javax.swing.JScrollPane();
        tableTables = new javax.swing.JTable();
        buttonComputeTables = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Specify Tables");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                dialogClosing(evt);
            }
        });

        panelExplanatoryVariables.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Explanatory Variables", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.TOP));

        listExplanatoryVariables.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listExplanatoryVariables.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listExplanatoryVariablesValueChanged(evt);
            }
        });
        scrollPaneExplanatoryVariables.setViewportView(listExplanatoryVariables);

        buttonExplanatoryAdd.setText(" >>");
        buttonExplanatoryAdd.setToolTipText("Add explanatory variables");
        buttonExplanatoryAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonExplanatoryAddActionPerformed(evt);
            }
        });

        buttonExplanatoryDelete.setText("<<");
        buttonExplanatoryDelete.setToolTipText("Delete explanatory variables");
        buttonExplanatoryDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonExplanatoryDeleteActionPerformed(evt);
            }
        });

        listSelectedExplanatoryVariables.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listSelectedExplanatoryVariables.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listSelectedExplanatoryVariablesValueChanged(evt);
            }
        });
        scrollPaneSelectedExplanatoryVariables.setViewportView(listSelectedExplanatoryVariables);

        javax.swing.GroupLayout panelExplanatoryVariablesLayout = new javax.swing.GroupLayout(panelExplanatoryVariables);
        panelExplanatoryVariables.setLayout(panelExplanatoryVariablesLayout);
        panelExplanatoryVariablesLayout.setHorizontalGroup(
            panelExplanatoryVariablesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelExplanatoryVariablesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPaneExplanatoryVariables, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelExplanatoryVariablesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(buttonExplanatoryAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonExplanatoryDelete, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPaneSelectedExplanatoryVariables, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelExplanatoryVariablesLayout.setVerticalGroup(
            panelExplanatoryVariablesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelExplanatoryVariablesLayout.createSequentialGroup()
                .addGroup(panelExplanatoryVariablesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(scrollPaneExplanatoryVariables, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPaneSelectedExplanatoryVariables, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelExplanatoryVariablesLayout.createSequentialGroup()
                        .addComponent(buttonExplanatoryAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonExplanatoryDelete)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelCellItems.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Cell items", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.TOP));

        listResponseVariables.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listResponseVariables.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listResponseVariables.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listResponseVariablesValueChanged(evt);
            }
        });
        scrollPaneCellItems.setViewportView(listResponseVariables);

        buttonResponseAdd.setText(">>");
        buttonResponseAdd.setToolTipText("Set response variable");
        buttonResponseAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResponseAddActionPerformed(evt);
            }
        });

        buttonResponseDelete.setText("<<");
        buttonResponseDelete.setToolTipText("Delete response variable");
        buttonResponseDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResponseDeleteActionPerformed(evt);
            }
        });

        buttonShadowAdd.setText(">>");
        buttonShadowAdd.setToolTipText("Set shadow variable");
        buttonShadowAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonShadowAddActionPerformed(evt);
            }
        });

        buttonShadowDelete.setText("<<");
        buttonShadowDelete.setToolTipText("Delete shadow variable");
        buttonShadowDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonShadowDeleteActionPerformed(evt);
            }
        });

        buttonCostAdd.setText(">>");
        buttonCostAdd.setToolTipText("Set cost variable");
        buttonCostAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCostAddActionPerformed(evt);
            }
        });

        buttonCostDelete.setText("<<");
        buttonCostDelete.setToolTipText("Delete cost variable");
        buttonCostDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCostDeleteActionPerformed(evt);
            }
        });

        labelResponseVariable.setLabelFor(textFieldResponseVariable);
        labelResponseVariable.setText("Response variable:");

        textFieldResponseVariable.setEditable(false);
        textFieldResponseVariable.setFocusable(false);

        labelShadowVariable.setLabelFor(textFieldShadowVariable);
        labelShadowVariable.setText("Shadow variable:");

        textFieldShadowVariable.setEditable(false);
        textFieldShadowVariable.setFocusable(false);

        panelCostVariable.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Cost variable", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.TOP));

        buttonGroupCostVariable.add(radioButtonUnity);
        radioButtonUnity.setText("Unity");
        radioButtonUnity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCostActionPerformed(evt);
            }
        });

        buttonGroupCostVariable.add(radioButtonVariable);
        radioButtonVariable.setSelected(true);
        radioButtonVariable.setText("Variable");
        radioButtonVariable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCostActionPerformed(evt);
            }
        });

        buttonGroupCostVariable.add(radioButtonFrequency);
        radioButtonFrequency.setText("Frequency");
        radioButtonFrequency.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCostActionPerformed(evt);
            }
        });

        buttonGroupCostVariable.add(radioButtonDistanceFunction);
        radioButtonDistanceFunction.setText("Distance function");
        radioButtonDistanceFunction.setEnabled(false);
        radioButtonDistanceFunction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCostActionPerformed(evt);
            }
        });

        textFieldCostVariable.setEditable(false);
        textFieldCostVariable.setFocusable(false);

        labelLambda.setLabelFor(textFieldLambda);
        labelLambda.setText("Lambda:");

        labelMSC.setLabelFor(labelMSC);
        labelMSC.setText("MSC:");

        javax.swing.GroupLayout panelCostVariableLayout = new javax.swing.GroupLayout(panelCostVariable);
        panelCostVariable.setLayout(panelCostVariableLayout);
        panelCostVariableLayout.setHorizontalGroup(
            panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCostVariableLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textFieldCostVariable)
                    .addGroup(panelCostVariableLayout.createSequentialGroup()
                        .addGroup(panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(radioButtonUnity, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(radioButtonVariable, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(radioButtonDistanceFunction, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(radioButtonFrequency, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(panelCostVariableLayout.createSequentialGroup()
                        .addComponent(labelLambda)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldLambda, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(labelMSC)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldMSC, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        panelCostVariableLayout.setVerticalGroup(
            panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelCostVariableLayout.createSequentialGroup()
                .addGroup(panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonUnity)
                    .addComponent(radioButtonFrequency))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonVariable)
                    .addComponent(radioButtonDistanceFunction))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textFieldCostVariable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCostVariableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldLambda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelLambda)
                    .addComponent(textFieldMSC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelMSC))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelCellItemsLayout = new javax.swing.GroupLayout(panelCellItems);
        panelCellItems.setLayout(panelCellItemsLayout);
        panelCellItemsLayout.setHorizontalGroup(
            panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCellItemsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPaneCellItems, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonResponseAdd)
                    .addComponent(buttonResponseDelete)
                    .addComponent(buttonShadowAdd)
                    .addComponent(buttonCostAdd)
                    .addComponent(buttonCostDelete)
                    .addComponent(buttonShadowDelete))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelCostVariable, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(textFieldShadowVariable, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelShadowVariable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(labelResponseVariable, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(textFieldResponseVariable))
                .addContainerGap())
        );
        panelCellItemsLayout.setVerticalGroup(
            panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCellItemsLayout.createSequentialGroup()
                .addGroup(panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelCellItemsLayout.createSequentialGroup()
                        .addGroup(panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelResponseVariable)
                            .addComponent(buttonResponseAdd))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(textFieldResponseVariable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonResponseDelete))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelShadowVariable)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(textFieldShadowVariable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonShadowAdd))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelCellItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelCellItemsLayout.createSequentialGroup()
                                .addComponent(buttonShadowDelete)
                                .addGap(27, 27, 27)
                                .addComponent(buttonCostAdd)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonCostDelete)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(panelCostVariable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(scrollPaneCellItems, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        panelParameters.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Parameters", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.TOP));

        checkBoxDominanceRule.setText("<html>Dominance<br>rule<br>");
        checkBoxDominanceRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxDominanceRuleActionPerformed(evt);
            }
        });

        checkBoxPqRule.setText("PQ-rule");
        checkBoxPqRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxPqRuleActionPerformed(evt);
            }
        });

        checkBoxRequestRule.setText("<html>Request<br>rule<br>");
        checkBoxRequestRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxRequestRuleActionPerformed(evt);
            }
        });

        tabbedPaneRules.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        labelDomN.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelDomN.setText("n");

        labelDomK.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelDomK.setText("k");

        labelDomInd1.setText("Ind-1");

        labelDomInd2.setText("Ind-2");

        labelDomHold1.setText("Hold-1");

        labelDomHold2.setText("Hold-2");

        javax.swing.GroupLayout panelDominanceRuleLayout = new javax.swing.GroupLayout(panelDominanceRule);
        panelDominanceRule.setLayout(panelDominanceRuleLayout);
        panelDominanceRuleLayout.setHorizontalGroup(
            panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDominanceRuleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(separatorDom)
                    .addGroup(panelDominanceRuleLayout.createSequentialGroup()
                        .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(labelDomInd2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelDomHold1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelDomHold2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelDomInd1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(labelDomK, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(panelDominanceRuleLayout.createSequentialGroup()
                                    .addComponent(textFieldDomHold2N, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(textFieldDomHold2K, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(panelDominanceRuleLayout.createSequentialGroup()
                                    .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(labelDomN, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(textFieldDomInd2N, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(textFieldDomInd1N, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(textFieldDomInd1K, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(textFieldDomInd2K, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(panelDominanceRuleLayout.createSequentialGroup()
                                .addComponent(textFieldDomHold1N, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(textFieldDomHold1K, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 86, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panelDominanceRuleLayout.setVerticalGroup(
            panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDominanceRuleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelDomN)
                    .addComponent(labelDomK))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelDomInd1)
                    .addComponent(textFieldDomInd1N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textFieldDomInd1K, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldDomInd2N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelDomInd2)
                    .addComponent(textFieldDomInd2K, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separatorDom, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldDomHold1N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelDomHold1)
                    .addComponent(textFieldDomHold1K, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDominanceRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldDomHold2N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelDomHold2)
                    .addComponent(textFieldDomHold2K, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        tabbedPaneRules.addTab("Dom rule", panelDominanceRule);

        labelPqP.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelPqP.setText("P");

        labelPqQ.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelPqQ.setText("Q");

        labelPqN.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelPqN.setText("N");

        labelPqInd1.setText("Ind-1");

        labelPqInd2.setText("Ind-2");

        labelPqHold1.setText("Hold-1");

        labelPqHold2.setText("Hold-2");

        javax.swing.GroupLayout panelPqRuleLayout = new javax.swing.GroupLayout(panelPqRule);
        panelPqRule.setLayout(panelPqRuleLayout);
        panelPqRuleLayout.setHorizontalGroup(
            panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPqRuleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(separatorPq)
                    .addGroup(panelPqRuleLayout.createSequentialGroup()
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(labelPqInd2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelPqHold1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelPqHold2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelPqInd1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(textFieldPqHold2P, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelPqP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(textFieldPqInd1P, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqInd2P, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqHold1P, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(textFieldPqInd1Q, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelPqQ, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(textFieldPqInd2Q, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqHold1Q, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqHold2Q, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelPqN, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqInd1N, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqInd2N, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqHold1N, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqHold2N, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 46, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panelPqRuleLayout.setVerticalGroup(
            panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPqRuleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPqRuleLayout.createSequentialGroup()
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelPqP)
                            .addComponent(labelPqQ))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelPqInd1)
                            .addComponent(textFieldPqInd1P, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(textFieldPqInd2P, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelPqInd2)))
                    .addGroup(panelPqRuleLayout.createSequentialGroup()
                        .addComponent(labelPqN)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(textFieldPqInd1Q, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqInd1N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(textFieldPqInd2Q, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldPqInd2N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separatorPq, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldPqHold1P, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelPqHold1)
                    .addComponent(textFieldPqHold1Q, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textFieldPqHold1N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPqRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldPqHold2P, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelPqHold2)
                    .addComponent(textFieldPqHold2Q, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textFieldPqHold2N, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        tabbedPaneRules.addTab("PQ-rule", panelPqRule);

        labelReqInd1.setLabelFor(textFieldReqInd1);
        labelReqInd1.setText("Ind-1");

        labelReqInd2.setLabelFor(textFieldReqInd2);
        labelReqInd2.setText("Ind-2");

        labelReqIndFreq.setLabelFor(textFieldIndFreq);
        labelReqIndFreq.setText("MinFreq");

        labelReqSafetyRange.setText("Range");
        labelReqSafetyRange.setMaximumSize(new java.awt.Dimension(38, 14));
        labelReqSafetyRange.setMinimumSize(new java.awt.Dimension(38, 14));
        labelReqSafetyRange.setPreferredSize(new java.awt.Dimension(38, 14));

        labelReqIndPercentage.setText("%");

        labelReqHold1.setLabelFor(textFieldReqHold1);
        labelReqHold1.setText("Hold-1");

        labelReqHold2.setLabelFor(textFieldReqHold2);
        labelReqHold2.setText("Hold-2");

        labelReqHoldFreq.setLabelFor(textFieldReqHoldFreq);
        labelReqHoldFreq.setText("MinFreq");

        labelReqHoldPercentage.setText("%");

        labelReqSafetyRangeHolding.setText("Range");
        labelReqSafetyRangeHolding.setMaximumSize(new java.awt.Dimension(38, 14));
        labelReqSafetyRangeHolding.setMinimumSize(new java.awt.Dimension(38, 14));
        labelReqSafetyRangeHolding.setPreferredSize(new java.awt.Dimension(38, 14));

        javax.swing.GroupLayout panelRequestRuleLayout = new javax.swing.GroupLayout(panelRequestRule);
        panelRequestRule.setLayout(panelRequestRuleLayout);
        panelRequestRuleLayout.setHorizontalGroup(
            panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRequestRuleLayout.createSequentialGroup()
                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelRequestRuleLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelRequestRuleLayout.createSequentialGroup()
                                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(labelReqInd1)
                                    .addComponent(labelReqInd2))
                                .addGap(11, 11, 11)
                                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(textFieldReqInd2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(textFieldReqInd1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelRequestRuleLayout.createSequentialGroup()
                                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(labelReqHold2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(labelReqHold1))
                                .addGap(6, 6, 6)
                                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(textFieldReqHold1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(textFieldReqHold2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(labelReqHoldFreq)
                            .addComponent(labelReqSafetyRangeHolding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelReqSafetyRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelReqIndFreq))
                        .addGap(6, 6, 6)
                        .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(textFieldReqHoldFreq, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldReqHoldRange, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldReqIndRange, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldReqIndFreq, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(labelReqHoldPercentage)
                            .addComponent(labelReqIndPercentage, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelRequestRuleLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(separatorReq)))
                .addContainerGap())
        );
        panelRequestRuleLayout.setVerticalGroup(
            panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRequestRuleLayout.createSequentialGroup()
                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRequestRuleLayout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addComponent(labelReqIndPercentage))
                    .addGroup(panelRequestRuleLayout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelReqInd1)
                            .addComponent(textFieldReqInd1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelReqIndFreq)
                            .addComponent(textFieldReqIndFreq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelReqInd2)
                            .addComponent(textFieldReqInd2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelReqSafetyRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textFieldReqIndRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separatorReq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelReqHold1)
                    .addComponent(textFieldReqHold1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textFieldReqHoldFreq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelReqHoldFreq))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelRequestRuleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelReqHold2)
                    .addComponent(textFieldReqHoldRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelReqHoldPercentage)
                    .addComponent(textFieldReqHold2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelReqSafetyRangeHolding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16))
        );

        tabbedPaneRules.addTab("Req. rule", panelRequestRule);

        panelMinimumFrequency.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        checkBoxMinimumFrequency.setText("Minimum frequency");
        checkBoxMinimumFrequency.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxMinimumFrequencyActionPerformed(evt);
            }
        });

        labelFreq.setText("Freq");

        labelRange.setText("Range");

        labelFreqInd.setText("Ind");

        labelIndPercentage.setText("%");

        labelFreqHold.setText("Hold");

        labelHoldPercentage.setText("%");

        javax.swing.GroupLayout panelMinimumFrequencyLayout = new javax.swing.GroupLayout(panelMinimumFrequency);
        panelMinimumFrequency.setLayout(panelMinimumFrequencyLayout);
        panelMinimumFrequencyLayout.setHorizontalGroup(
            panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMinimumFrequencyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(separatorFreq, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelMinimumFrequencyLayout.createSequentialGroup()
                        .addGroup(panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(labelFreqInd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelFreqHold))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelMinimumFrequencyLayout.createSequentialGroup()
                                .addComponent(textFieldIndFreq, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(textFieldIndRange, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelIndPercentage))
                            .addGroup(panelMinimumFrequencyLayout.createSequentialGroup()
                                .addComponent(textFieldHoldFreq, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(textFieldHoldRange, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelHoldPercentage))
                            .addGroup(panelMinimumFrequencyLayout.createSequentialGroup()
                                .addComponent(labelFreq, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelRange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addComponent(checkBoxMinimumFrequency))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelMinimumFrequencyLayout.setVerticalGroup(
            panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMinimumFrequencyLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkBoxMinimumFrequency)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelFreq)
                    .addComponent(labelRange))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelIndPercentage)
                    .addGroup(panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(textFieldIndFreq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelFreqInd)
                        .addComponent(textFieldIndRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separatorFreq, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMinimumFrequencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldHoldFreq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelFreqHold)
                    .addComponent(textFieldHoldRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelHoldPercentage))
                .addContainerGap())
        );

        checkBoxApplyWeights.setText("Apply weights");

        checkBoxMissingSafe.setText("Missing=safe");

        checkBoxUseHoldingsInfo.setText("Use holdings info");
        checkBoxUseHoldingsInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxUseHoldingsInfoActionPerformed(evt);
            }
        });

        panelZero.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        checkBoxZeroUnsafe.setText("Zero unsafe");
        checkBoxZeroUnsafe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxZeroUnsafeActionPerformed(evt);
            }
        });

        labelZeroUnsafeRange.setLabelFor(textFieldlZeroUnsafeRange);
        labelZeroUnsafeRange.setText("Range:");

        javax.swing.GroupLayout panelZeroLayout = new javax.swing.GroupLayout(panelZero);
        panelZero.setLayout(panelZeroLayout);
        panelZeroLayout.setHorizontalGroup(
            panelZeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelZeroLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelZeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelZeroLayout.createSequentialGroup()
                        .addComponent(labelZeroUnsafeRange)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldlZeroUnsafeRange, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(checkBoxZeroUnsafe))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelZeroLayout.setVerticalGroup(
            panelZeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelZeroLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkBoxZeroUnsafe)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelZeroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelZeroUnsafeRange)
                    .addComponent(textFieldlZeroUnsafeRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        labelManualSafetyRange.setLabelFor(textFieldManualSafetyRange);
        labelManualSafetyRange.setText("Manual safety range:");

        labelManualSafetyRangePercentage.setText("%");

        javax.swing.GroupLayout panelParametersLayout = new javax.swing.GroupLayout(panelParameters);
        panelParameters.setLayout(panelParametersLayout);
        panelParametersLayout.setHorizontalGroup(
            panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelParametersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(checkBoxPqRule, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(checkBoxRequestRule)
                    .addComponent(checkBoxDominanceRule, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabbedPaneRules, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelParametersLayout.createSequentialGroup()
                        .addComponent(panelZero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelManualSafetyRange)
                            .addGroup(panelParametersLayout.createSequentialGroup()
                                .addComponent(textFieldManualSafetyRange, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelManualSafetyRangePercentage))))
                    .addGroup(panelParametersLayout.createSequentialGroup()
                        .addComponent(panelMinimumFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(checkBoxMissingSafe, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(checkBoxUseHoldingsInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(checkBoxApplyWeights, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelParametersLayout.setVerticalGroup(
            panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelParametersLayout.createSequentialGroup()
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelParametersLayout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(checkBoxDominanceRule, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxPqRule)
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxRequestRule, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelParametersLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelParametersLayout.createSequentialGroup()
                                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(panelMinimumFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panelParametersLayout.createSequentialGroup()
                                        .addComponent(checkBoxApplyWeights)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(checkBoxMissingSafe)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(checkBoxUseHoldingsInfo)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(panelZero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panelParametersLayout.createSequentialGroup()
                                        .addComponent(labelManualSafetyRange, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(textFieldManualSafetyRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(labelManualSafetyRangePercentage)))))
                            .addComponent(tabbedPaneRules, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        buttonDeleteTable.setText("\u2227");
        buttonDeleteTable.setToolTipText("Delete from tables");
        buttonDeleteTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteTableActionPerformed(evt);
            }
        });

        buttonAddTable.setText("\u2228");
        buttonAddTable.setToolTipText("Add to tables");
        buttonAddTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddTableActionPerformed(evt);
            }
        });

        scrollPaneTables.setPreferredSize(new java.awt.Dimension(772, 402));

        tableTables.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        scrollPaneTables.setViewportView(tableTables);

        buttonComputeTables.setText("Compute tables");
        buttonComputeTables.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonComputeTablesActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPaneTables, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonComputeTables)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(panelParameters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonAddTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonDeleteTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelExplanatoryVariables, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelCellItems, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelCellItems, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelExplanatoryVariables, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(buttonDeleteTable)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonAddTable))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelParameters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollPaneTables, javax.swing.GroupLayout.DEFAULT_SIZE, 102, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(buttonComputeTables, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonCancel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelExplanatoryVariables.getAccessibleContext().setAccessibleName("Explanatory variables");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonComputeTablesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonComputeTablesActionPerformed
        // The GUI thread (EDT) should not be used for long running tasks, 
        // so use a SwingWorker class
        
        // PWOF 2017-12-08
        // Copied from showDialog() because only needed in case of " Compute Tables" ???
        // Let table use it's original variables 
        //????????????????????????????????????????????????????????????
        // PWOF 2017-12-08
        // This is only needed in case of "Compute Tables"?
        // In case of "Cancel" this resets the variables incorrectly?
        //????????????????????????????????????????????????????????????
        for (int i = 0; i < TableService.numberOfTables(); i++) {
            TableSet table = TableService.getTable(i);
            for (int j = 0; j < table.expVar.size(); j++) {
                table.expVar.set(j, table.expVar.get(j).originalVariable);
            }
        }

        saveDefaultsToRegistry();
        final SwingWorker<Void, Void> worker = new ProgressSwingWorker<Void, Void>(ProgressSwingWorker.SINGLE, "Computing tables") {

            // called in a separate thread...
            @Override
            protected Void doInBackground() throws Exception {
                super.doInBackground();
                try{
                    TableService.readMicrodata(getPropertyChangeListener());
                }
                catch (ArgusException ex){
                    TableService.clearTables();
                    throw new ArgusException(ex.getMessage());
                }
                return null;
            }

            // called on the GUI thread
            @Override
            public void done() {
                super.done();
                try {
                    get();
                    returnValue = APPROVE_OPTION;
                    setVisible(false);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                }
                finally { // cleanup
                    dispose(); 
                }
            }
        };
        worker.execute();
    }//GEN-LAST:event_buttonComputeTablesActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        if (hasBeenModified)TableService.clearTables();
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonResponseDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResponseDeleteActionPerformed
        setResponseVariable(null);
    }//GEN-LAST:event_buttonResponseDeleteActionPerformed

    private void buttonShadowDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonShadowDeleteActionPerformed
        setShadowVariable(null);
    }//GEN-LAST:event_buttonShadowDeleteActionPerformed

    private void buttonCostDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCostDeleteActionPerformed
        setCostVariable(null);
    }//GEN-LAST:event_buttonCostDeleteActionPerformed

    private void buttonResponseAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResponseAddActionPerformed
        Variable variable = listResponseVariables.getSelectedValue();
        setResponseVariable(variable);
    }//GEN-LAST:event_buttonResponseAddActionPerformed

    private void buttonShadowAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonShadowAddActionPerformed
        Variable variable = listResponseVariables.getSelectedValue();
        if (variable != freqVar) {
            setShadowVariable(variable);
        }
    }//GEN-LAST:event_buttonShadowAddActionPerformed

    private void buttonCostAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCostAddActionPerformed
        Variable variable = listResponseVariables.getSelectedValue();
        if (variable != freqVar) {
            setCostVariable(variable);
        }
    }//GEN-LAST:event_buttonCostAddActionPerformed

    private void buttonExplanatoryAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonExplanatoryAddActionPerformed
        int selectedIndex = listSelectedExplanatoryVariables.getSelectedIndex();
        if (selectedIndex == -1) {
            selectedIndex = 0;
        }
        for (Variable variable : listExplanatoryVariables.getSelectedValuesList()) {
            if (!selectedExplanatoryVariableListModel.contains(variable)) {
                selectedExplanatoryVariableListModel.addElement(variable);
            }
        }
        listSelectedExplanatoryVariables.setSelectedIndex(selectedIndex);
        organiseDistanceOption();
    }//GEN-LAST:event_buttonExplanatoryAddActionPerformed

    private void buttonExplanatoryDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonExplanatoryDeleteActionPerformed
        // set the selection to an item that still exists after deletion
        // if not done before removal the remove button will loose focus
        List<Variable> selectedValues = listSelectedExplanatoryVariables.getSelectedValuesList();
        int selectedIndex = listSelectedExplanatoryVariables.getMaxSelectionIndex() + 1;
        while (selectedIndex >= 0
                && (selectedIndex >= selectedExplanatoryVariableListModel.getSize() || listSelectedExplanatoryVariables.isSelectedIndex(selectedIndex))) {
            selectedIndex--;
        }
        listSelectedExplanatoryVariables.setSelectedIndex(selectedIndex);
        selectedValues.forEach((variable) -> {
            selectedExplanatoryVariableListModel.removeElement(variable);
        });
        organiseDistanceOption();
    }//GEN-LAST:event_buttonExplanatoryDeleteActionPerformed

    private void checkBoxRequestRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxRequestRuleActionPerformed
        organiseRequestRuleOptions();
        if (checkBoxRequestRule.isSelected()) {
            tabbedPaneRules.setSelectedComponent(panelRequestRule);
        }
        else{
            SelectPanel();
        }
    }//GEN-LAST:event_checkBoxRequestRuleActionPerformed

    private void checkBoxDominanceRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxDominanceRuleActionPerformed
        organiseDominanceRuleOptions();
        if (checkBoxDominanceRule.isSelected()) {
            tabbedPaneRules.setSelectedComponent(panelDominanceRule);
        }
        else{
         SelectPanel();   
        }
    }//GEN-LAST:event_checkBoxDominanceRuleActionPerformed

    private void checkBoxPqRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxPqRuleActionPerformed
        organisePqRuleOptions();
        if (checkBoxPqRule.isSelected()) {
            tabbedPaneRules.setSelectedComponent(panelPqRule);
        }
        else{
            SelectPanel();
        }
    }//GEN-LAST:event_checkBoxPqRuleActionPerformed

    private void buttonAddTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddTableActionPerformed
        if (!checkFields()) {
            return;
        }
        hasBeenModified = true;
        TableSet tableSet = new TableSet(metadata);
        save(tableSet);

        if (!Application.isAnco()
                && TableService.isCopy(tableSet)
                && JOptionPane.showConfirmDialog(this, "There is already a copy of this table\nDo you want an extra copy?" , "Message", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }

        int selectedRow = tableTables.getSelectedRow();
        if (selectedRow == -1) {
            selectedRow = 0;
        }
        TableService.addTable(tableSet);
        tableSetListTableModel.fireTableRowsInserted(TableService.numberOfTables() - 1, TableService.numberOfTables() - 1);
        tableTables.setRowSelectionInterval(selectedRow, selectedRow);
    }//GEN-LAST:event_buttonAddTableActionPerformed

    private void buttonDeleteTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteTableActionPerformed
        int selectedRow = tableTables.getSelectedRow();
        // set the selection to an item that still exists after deletion
        // if not done before removal the remove button will loose focus
        if (selectedRow == tableSetListTableModel.getRowCount() - 1) {
            if (selectedRow == 0) {
                tableTables.clearSelection();
            } else {
                tableTables.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
            }
        }
        else {
            tableTables.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
        }
        hasBeenModified = true;
        TableSet tableSet = TableService.removeTable(selectedRow);
        tableSetListTableModel.fireTableRowsDeleted(selectedRow, selectedRow);
        load(tableSet);
    }//GEN-LAST:event_buttonDeleteTableActionPerformed

    private void radioButtonCostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonCostActionPerformed
        organiseCostOptions();
    }//GEN-LAST:event_radioButtonCostActionPerformed

    private void checkBoxMinimumFrequencyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxMinimumFrequencyActionPerformed
        organiseMinimumFrequencyRuleOptions();
    }//GEN-LAST:event_checkBoxMinimumFrequencyActionPerformed

    private void checkBoxZeroUnsafeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxZeroUnsafeActionPerformed
        organiseZeroUnsafeRuleOptions();
    }//GEN-LAST:event_checkBoxZeroUnsafeActionPerformed

    private void checkBoxUseHoldingsInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxUseHoldingsInfoActionPerformed
        organiseHoldingOptions();
    }//GEN-LAST:event_checkBoxUseHoldingsInfoActionPerformed

    private void listExplanatoryVariablesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listExplanatoryVariablesValueChanged
        buttonExplanatoryAdd.setEnabled(!listExplanatoryVariables.isSelectionEmpty());
    }//GEN-LAST:event_listExplanatoryVariablesValueChanged

    private void listSelectedExplanatoryVariablesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listSelectedExplanatoryVariablesValueChanged
        buttonExplanatoryDelete.setEnabled(!listSelectedExplanatoryVariables.isSelectionEmpty());
        organiseButtonAddTableOption();
    }//GEN-LAST:event_listSelectedExplanatoryVariablesValueChanged

    private void listResponseVariablesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listResponseVariablesValueChanged
        boolean b = !listResponseVariables.isSelectionEmpty();
        buttonResponseAdd.setEnabled(b);
        buttonShadowAdd.setEnabled(b);
        buttonCostAdd.setEnabled(b);
    }//GEN-LAST:event_listResponseVariablesValueChanged

    private void dialogClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_dialogClosing
        if (hasBeenModified) TableService.clearTables();
        setVisible(false);
        dispose();
    }//GEN-LAST:event_dialogClosing

    private void setResponseVariable(Variable variable) {
        responseVariable = variable;
        textFieldResponseVariable.setText(variable != null ? variable.name : "");
        buttonResponseDelete.setEnabled(variable != null);
        organiseButtonAddTableOption();
    }

    private void setShadowVariable(Variable variable) {
        shadowVariable = variable;
        textFieldShadowVariable.setText(variable != null ? variable.name : "");
        buttonShadowDelete.setEnabled(variable != null);
    }

    private void setCostVariable(Variable variable) {
        costVariable = variable;
        textFieldCostVariable.setText(variable != null ? variable.name : "");
        buttonCostDelete.setEnabled(variable != null);
    }
    
    private boolean checkFields() {
        if (!SwingUtils.verifyTextFields(this)) { // Only checks Visible and Enabled TextFields
            return false;
        }
        
        if (tableTables.getRowCount() >= 10) {
            JOptionPane.showMessageDialog(this, "Only 10 tables can be specified in one run");
            return false;
        }
        
       
        // Only freq-rule bij <freq>-tables!!!!
        if (checkBoxDominanceRule.isSelected() || checkBoxPqRule.isSelected() || checkBoxRequestRule.isSelected() || checkBoxZeroUnsafe.isSelected()) {
            if (responseVariable == freqVar) {
                JOptionPane.showMessageDialog(this, "When using response variable <freq>, dominance rule and p%-rule are not allowed");
                return false;
            }
        }

        // TableSet tableSet;
        if (radioButtonVariable.isSelected()) // lambda only needed when Variable used as cost, so only check in that case
        {
            double lambda = Double.parseDouble(textFieldLambda.getText()); // Throws unhandled exception if textField is empty string
            if (lambda < 0) {
                JOptionPane.showMessageDialog(this, "Illegal lambda value;\nshould be non-negative");
                textFieldLambda.requestFocusInWindow();
                return false;
            }
            if (lambda > 2) {
                if (JOptionPane.showConfirmDialog(this, "Are you sure about this lambda value (" + textFieldLambda.getText() + ")?", "Message", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    textFieldLambda.requestFocusInWindow();
                    return false;
                }
            }
        }
        if (checkBoxDominanceRule.isSelected()) {
            int j;
            if (Integer.parseInt(textFieldDomN[0].getText()) == 0 && Integer.parseInt(textFieldDomN[1].getText()) > 0) {
                JOptionPane.showMessageDialog(this, "The first dominance rule has to be specified too");
                tabbedPaneRules.setSelectedIndex(0);
                textFieldDomN[0].requestFocusInWindow();
                return false;
            }
            if (metadata.containsHoldingVariable() && checkBoxUseHoldingsInfo.isSelected()) {
                if (Integer.parseInt(textFieldDomN[2].getText()) == 0 && Integer.parseInt(textFieldDomN[3].getText()) > 0) {
                    JOptionPane.showMessageDialog(this, "illegal value for the first holding dominance rule");
                    tabbedPaneRules.setSelectedIndex(0);
                    textFieldDomN[2].requestFocusInWindow();
                    return false;
                }
                if (Integer.parseInt(textFieldDomN[0].getText()) == 0 && Integer.parseInt(textFieldDomN[2].getText()) == 0) {
                    JOptionPane.showMessageDialog(this, "At least one dom-rule should be specified");
                    tabbedPaneRules.setSelectedIndex(0);
                    textFieldDomN[0].requestFocusInWindow();
                    return false;
                }
                j = 4;
            } else {
                if (Integer.parseInt(textFieldDomN[0].getText()) == 0) {
                    JOptionPane.showMessageDialog(this, "At least one dom-rule should be specified");
                    tabbedPaneRules.setSelectedIndex(0);
                    textFieldDomN[0].requestFocusInWindow();
                    return false;
                }
                j = 2;
            }
            for (int i=0; i<j; i++) {
                double p = Double.parseDouble(textFieldDomN[i].getText());
                double q = Double.parseDouble(textFieldDomK[i].getText());
                if ((p > 0 && (q <= 0 || q >= 100)) || p < 0) {
                    JOptionPane.showMessageDialog(this, "Illegal value for Dom. rule\nN must be > 0 and 0<k<100");
                    tabbedPaneRules.setSelectedIndex(0);
                    textFieldDomN[i].requestFocusInWindow();
                    return false;
                }
            }
        }
        if (checkBoxPqRule.isSelected()) {
            int j = 2;
            if (Integer.parseInt(textFieldPqP[0].getText()) == 0 && Integer.parseInt(textFieldPqP[1].getText()) > 0) {
                JOptionPane.showMessageDialog(this, "The first P-rule should be specified too");
                tabbedPaneRules.setSelectedIndex(1);
                textFieldPqP[0].requestFocusInWindow();
                return false;
            }
            if (metadata.containsHoldingVariable() && checkBoxUseHoldingsInfo.isSelected()) {
                if (Integer.parseInt(textFieldPqP[2].getText()) == 0 && Integer.parseInt(textFieldPqP[3].getText()) > 0) {
                    JOptionPane.showMessageDialog(this, "Illegal value for first P-Holding-rule");
                    tabbedPaneRules.setSelectedIndex(1);
                    textFieldPqP[2].requestFocusInWindow();
                    return false;
                }
                j = 4;
            }
            for (int i = 0; i < j; i++) {
                int p = Integer.parseInt(textFieldPqP[i].getText());
                int q = Integer.parseInt(textFieldPqQ[i].getText());
                int n = Integer.parseInt(textFieldPqN[i].getText());
                if (p > 0) {
                    if (p > q || n < 1 || q > 100) {
                        JOptionPane.showMessageDialog(this, "Illegal value for indiv. PQ-rule" + i + "\n"
                                + "P must be smaller than Q\n"
                                + "Q must be smaller or equal than 100\n"
                                + "N must be greater than 0");
                        tabbedPaneRules.setSelectedIndex(1);
                        textFieldPqP[i].requestFocusInWindow();
                        return false;
                    }
                }
            }
        }
        if (checkBoxMinimumFrequency.isSelected()) {
            int maxi = (metadata.containsHoldingVariable() && checkBoxUseHoldingsInfo.isSelected()) ? 2 : 1;
            for (int i=0; i<maxi; i++) {
                if (Integer.parseInt(textFieldMinFreq[i].getText()) < 0) {
                    JOptionPane.showMessageDialog(this, "Illegal value for min. frequency: " + textFieldMinFreq[i].getText() + "\nMust be positive");
                    textFieldMinFreq[i].requestFocusInWindow();
                    return false;
                } else if (Integer.parseInt(textFieldMinFreq[i].getText()) > 0) {
                    if (Integer.parseInt(textFieldRange[i].getText()) < 0) {
                        JOptionPane.showMessageDialog(this, "Illegal value for min. frequency range: " + textFieldRange[i].getText() + "\nMust be positive");
                        textFieldRange[i].requestFocusInWindow();
                        return false;
                    }
                    if (Double.parseDouble(textFieldRange[i].getText()) == 0) {
                        if (JOptionPane.showConfirmDialog(this, "Zero value for min. frequency range might lead to poor protection\nDo you want to preceed?", "Message", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                            textFieldRange[i].requestFocusInWindow();
                            return false;
                        }
                    }
                }
            }
        }

        if (checkBoxZeroUnsafe.isSelected()) {
            if (Double.parseDouble(textFieldlZeroUnsafeRange.getText()) <= 0) {
                JOptionPane.showMessageDialog(this, "Illegal value for zero margin: " + textFieldlZeroUnsafeRange.getText() + "\nMust be positive");
                textFieldlZeroUnsafeRange.requestFocusInWindow();
                return false;
            }
        }
        
        if (Integer.parseInt(textFieldManualSafetyRange.getText()) < 0) {
            JOptionPane.showMessageDialog(this, "Illegal value for manual safety range: " + textFieldManualSafetyRange.getText() + "\nMust be positive");
            textFieldManualSafetyRange.requestFocusInWindow();
            return false;
        }

        return true;
    }

    private Metadata metadata;
         
    private javax.swing.JLabel[] labelDom;
    private javax.swing.JTextField[] textFieldDomN;
    private javax.swing.JTextField[] textFieldDomK;

    private javax.swing.JLabel[] labelPq;
    private javax.swing.JTextField[] textFieldPqP;
    private javax.swing.JTextField[] textFieldPqQ;
    private javax.swing.JTextField[] textFieldPqN;

    private javax.swing.JLabel[] labelReq;
    private javax.swing.JTextField[] textFieldReq;
    private javax.swing.JTextField[] textFieldReqFreq;
    private javax.swing.JTextField[] textFieldReqRange;
    private javax.swing.JLabel[] labelReqPercentage;
    private javax.swing.JLabel[] labelReqFreq;

    private javax.swing.JLabel[] labelMinFreq;
    private javax.swing.JTextField[] textFieldMinFreq;
    private javax.swing.JTextField[] textFieldRange;
    private javax.swing.JLabel[] labelMinFreqPercentage;
         
    private final DefaultListModel<Variable> explanatoryVariableListModel;
    private DefaultListModel<Variable> selectedExplanatoryVariableListModel;
    private final DefaultListModel<Variable> responseVariableListModel;
    private final Variable freqVar;
    private Variable responseVariable;
    private Variable shadowVariable;
    private Variable costVariable;
    private TableSetListTableModel tableSetListTableModel;
    private int returnValue = CANCEL_OPTION;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddTable;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonComputeTables;
    private javax.swing.JButton buttonCostAdd;
    private javax.swing.JButton buttonCostDelete;
    private javax.swing.JButton buttonDeleteTable;
    private javax.swing.JButton buttonExplanatoryAdd;
    private javax.swing.JButton buttonExplanatoryDelete;
    private javax.swing.ButtonGroup buttonGroupCostVariable;
    private javax.swing.JButton buttonResponseAdd;
    private javax.swing.JButton buttonResponseDelete;
    private javax.swing.JButton buttonShadowAdd;
    private javax.swing.JButton buttonShadowDelete;
    private javax.swing.JCheckBox checkBoxApplyWeights;
    private javax.swing.JCheckBox checkBoxDominanceRule;
    private javax.swing.JCheckBox checkBoxMinimumFrequency;
    private javax.swing.JCheckBox checkBoxMissingSafe;
    private javax.swing.JCheckBox checkBoxPqRule;
    private javax.swing.JCheckBox checkBoxRequestRule;
    private javax.swing.JCheckBox checkBoxUseHoldingsInfo;
    private javax.swing.JCheckBox checkBoxZeroUnsafe;
    private javax.swing.JLabel labelDomHold1;
    private javax.swing.JLabel labelDomHold2;
    private javax.swing.JLabel labelDomInd1;
    private javax.swing.JLabel labelDomInd2;
    private javax.swing.JLabel labelDomK;
    private javax.swing.JLabel labelDomN;
    private javax.swing.JLabel labelFreq;
    private javax.swing.JLabel labelFreqHold;
    private javax.swing.JLabel labelFreqInd;
    private javax.swing.JLabel labelHoldPercentage;
    private javax.swing.JLabel labelIndPercentage;
    private javax.swing.JLabel labelLambda;
    private javax.swing.JLabel labelMSC;
    private javax.swing.JLabel labelManualSafetyRange;
    private javax.swing.JLabel labelManualSafetyRangePercentage;
    private javax.swing.JLabel labelPqHold1;
    private javax.swing.JLabel labelPqHold2;
    private javax.swing.JLabel labelPqInd1;
    private javax.swing.JLabel labelPqInd2;
    private javax.swing.JLabel labelPqN;
    private javax.swing.JLabel labelPqP;
    private javax.swing.JLabel labelPqQ;
    private javax.swing.JLabel labelRange;
    private javax.swing.JLabel labelReqHold1;
    private javax.swing.JLabel labelReqHold2;
    private javax.swing.JLabel labelReqHoldFreq;
    private javax.swing.JLabel labelReqHoldPercentage;
    private javax.swing.JLabel labelReqInd1;
    private javax.swing.JLabel labelReqInd2;
    private javax.swing.JLabel labelReqIndFreq;
    private javax.swing.JLabel labelReqIndPercentage;
    private javax.swing.JLabel labelReqSafetyRange;
    private javax.swing.JLabel labelReqSafetyRangeHolding;
    private javax.swing.JLabel labelResponseVariable;
    private javax.swing.JLabel labelShadowVariable;
    private javax.swing.JLabel labelZeroUnsafeRange;
    private javax.swing.JList<tauargus.model.Variable> listExplanatoryVariables;
    private javax.swing.JList<tauargus.model.Variable> listResponseVariables;
    private javax.swing.JList<tauargus.model.Variable> listSelectedExplanatoryVariables;
    private javax.swing.JPanel panelCellItems;
    private javax.swing.JPanel panelCostVariable;
    private javax.swing.JPanel panelDominanceRule;
    private javax.swing.JPanel panelExplanatoryVariables;
    private javax.swing.JPanel panelMinimumFrequency;
    private javax.swing.JPanel panelParameters;
    private javax.swing.JPanel panelPqRule;
    private javax.swing.JPanel panelRequestRule;
    private javax.swing.JPanel panelZero;
    private javax.swing.JRadioButton radioButtonDistanceFunction;
    private javax.swing.JRadioButton radioButtonFrequency;
    private javax.swing.JRadioButton radioButtonUnity;
    private javax.swing.JRadioButton radioButtonVariable;
    private javax.swing.JScrollPane scrollPaneCellItems;
    private javax.swing.JScrollPane scrollPaneExplanatoryVariables;
    private javax.swing.JScrollPane scrollPaneSelectedExplanatoryVariables;
    private javax.swing.JScrollPane scrollPaneTables;
    private javax.swing.JSeparator separatorDom;
    private javax.swing.JSeparator separatorFreq;
    private javax.swing.JSeparator separatorPq;
    private javax.swing.JSeparator separatorReq;
    private javax.swing.JTabbedPane tabbedPaneRules;
    private javax.swing.JTable tableTables;
    private javax.swing.JTextField textFieldCostVariable;
    private javax.swing.JTextField textFieldDomHold1K;
    private javax.swing.JTextField textFieldDomHold1N;
    private javax.swing.JTextField textFieldDomHold2K;
    private javax.swing.JTextField textFieldDomHold2N;
    private javax.swing.JTextField textFieldDomInd1K;
    private javax.swing.JTextField textFieldDomInd1N;
    private javax.swing.JTextField textFieldDomInd2K;
    private javax.swing.JTextField textFieldDomInd2N;
    private javax.swing.JTextField textFieldHoldFreq;
    private javax.swing.JTextField textFieldHoldRange;
    private javax.swing.JTextField textFieldIndFreq;
    private javax.swing.JTextField textFieldIndRange;
    private javax.swing.JTextField textFieldLambda;
    private javax.swing.JTextField textFieldMSC;
    private javax.swing.JTextField textFieldManualSafetyRange;
    private javax.swing.JTextField textFieldPqHold1N;
    private javax.swing.JTextField textFieldPqHold1P;
    private javax.swing.JTextField textFieldPqHold1Q;
    private javax.swing.JTextField textFieldPqHold2N;
    private javax.swing.JTextField textFieldPqHold2P;
    private javax.swing.JTextField textFieldPqHold2Q;
    private javax.swing.JTextField textFieldPqInd1N;
    private javax.swing.JTextField textFieldPqInd1P;
    private javax.swing.JTextField textFieldPqInd1Q;
    private javax.swing.JTextField textFieldPqInd2N;
    private javax.swing.JTextField textFieldPqInd2P;
    private javax.swing.JTextField textFieldPqInd2Q;
    private javax.swing.JTextField textFieldReqHold1;
    private javax.swing.JTextField textFieldReqHold2;
    private javax.swing.JTextField textFieldReqHoldFreq;
    private javax.swing.JTextField textFieldReqHoldRange;
    private javax.swing.JTextField textFieldReqInd1;
    private javax.swing.JTextField textFieldReqInd2;
    private javax.swing.JTextField textFieldReqIndFreq;
    private javax.swing.JTextField textFieldReqIndRange;
    private javax.swing.JTextField textFieldResponseVariable;
    private javax.swing.JTextField textFieldShadowVariable;
    private javax.swing.JTextField textFieldlZeroUnsafeRange;
    // End of variables declaration//GEN-END:variables

    private void loadDefaultsFromRegistry(){
    
      //P% rule  
      checkBoxPqRule.setSelected(SystemUtils.getRegBoolean("sensitivityrules", "pqrule", false));
      
      textFieldPqInd1P.setText(SystemUtils.getRegString("sensitivityrules", "pqp1", "15"));
      textFieldPqInd1Q.setText(SystemUtils.getRegString("sensitivityrules", "pqq1", "100"));
      textFieldPqInd1N.setText(SystemUtils.getRegString("sensitivityrules", "pqn1", "1"));
      
      textFieldPqInd2P.setText(SystemUtils.getRegString("sensitivityrules", "pqp2", "0"));
      textFieldPqInd2Q.setText(SystemUtils.getRegString("sensitivityrules", "pqq2", "100"));
      textFieldPqInd2N.setText(SystemUtils.getRegString("sensitivityrules", "pqn2", "1"));

      textFieldPqHold1P.setText(SystemUtils.getRegString("sensitivityrules", "pqp3", "0"));
      textFieldPqHold1Q.setText(SystemUtils.getRegString("sensitivityrules", "pqq3", "100"));
      textFieldPqHold1N.setText(SystemUtils.getRegString("sensitivityrules", "pqn3", "1"));
      
      textFieldPqHold2P.setText(SystemUtils.getRegString("sensitivityrules", "pqp4", "0"));
      textFieldPqHold2Q.setText(SystemUtils.getRegString("sensitivityrules", "pqq4", "100"));
      textFieldPqHold2N.setText(SystemUtils.getRegString("sensitivityrules", "pqn4", "1"));
      //dominance rule
      checkBoxDominanceRule.setSelected(SystemUtils.getRegBoolean("sensitivityrules", "domrule", false));
      
      textFieldDomInd1N.setText(SystemUtils.getRegString("sensitivityrules", "domn1", "2"));
      textFieldDomInd1K.setText(SystemUtils.getRegString("sensitivityrules", "dimk1", "75"));
      
      textFieldDomInd2N.setText(SystemUtils.getRegString("sensitivityrules", "domn2", "0"));
      textFieldDomInd2K.setText(SystemUtils.getRegString("sensitivityrules", "dimk2", "0"));

      textFieldDomHold1N.setText(SystemUtils.getRegString("sensitivityrules", "domn3", "0"));
      textFieldDomHold1K.setText(SystemUtils.getRegString("sensitivityrules", "dimk3", "0"));

      textFieldDomHold2N.setText(SystemUtils.getRegString("sensitivityrules", "domn4", "0"));
      textFieldDomHold2K.setText(SystemUtils.getRegString("sensitivityrules", "dimk4", "0"));
      
      //peep rule Only if requestvariable is present in data
      checkBoxRequestRule.setSelected(this.metadata.containsRequestVariable() ? SystemUtils.getRegBoolean("sensitivityrules", "peeprule", false) : false);
      
      textFieldReqInd1.setText(SystemUtils.getRegString("sensitivityrules", "peepindk1", "0"));
      textFieldReqInd2.setText(SystemUtils.getRegString("sensitivityrules", "peepindk2", "0"));
      textFieldReqIndFreq.setText(SystemUtils.getRegString("sensitivityrules", "peepindn1", "0"));
      textFieldReqIndRange.setText(SystemUtils.getRegString("sensitivityrules", "peepindrange1", "0"));

      textFieldReqHold1.setText(SystemUtils.getRegString("sensitivityrules", "peepholdk1", "0"));
      textFieldReqHold2.setText(SystemUtils.getRegString("sensitivityrules", "peepholdk2", "0"));
      textFieldReqHoldFreq.setText(SystemUtils.getRegString("sensitivityrules", "peepholdn1", "0"));
      textFieldReqHoldRange.setText(SystemUtils.getRegString("sensitivityrules", "peepholdrange1", "0"));
      
      //frequency rule
      textFieldIndFreq.setText(SystemUtils.getRegString("sensitivityrules", "indfreq", "3"));
      textFieldHoldFreq.setText(SystemUtils.getRegString("sensitivityrules", "holdfreq", "3"));
      checkBoxMinimumFrequency.setSelected(SystemUtils.getRegBoolean("sensitivityrules", "freqrule", false));
      textFieldIndRange.setText(SystemUtils.getRegString("sensitivityrules", "indfreqrange", "10"));
      textFieldHoldRange.setText(SystemUtils.getRegString("sensitivityrules", "holdreqrange", "10"));
      
      checkBoxZeroUnsafe.setSelected(SystemUtils.getRegBoolean("sensitivityrules", "zerorule", false));
      textFieldlZeroUnsafeRange.setText(SystemUtils.getRegString("sensitivityrules", "zerorulerange", "10"));

      textFieldManualSafetyRange.setText(SystemUtils.getRegString("sensitivityrules", "manualrange", "10"));
    }
    
    private void saveDefaultsToRegistry(){    
     SystemUtils.putRegString("sensitivityrules", "pqp1",textFieldPqInd1P.getText());
     SystemUtils.putRegString("sensitivityrules", "pqq1",textFieldPqInd1Q.getText());
     SystemUtils.putRegString("sensitivityrules", "pqn1",textFieldPqInd1N.getText());
     SystemUtils.putRegBoolean("sensitivityrules", "pqrule",checkBoxPqRule.isSelected());

           //P% rule  
      SystemUtils.putRegBoolean("sensitivityrules", "pqrule", checkBoxPqRule.isSelected());
      
      SystemUtils.putRegString("sensitivityrules", "pqp1", textFieldPqInd1P.getText());
      SystemUtils.putRegString("sensitivityrules", "pqq1", textFieldPqInd1Q.getText());
      SystemUtils.putRegString("sensitivityrules", "pqn1", textFieldPqInd1N.getText());
      
      SystemUtils.putRegString("sensitivityrules", "pqp2", textFieldPqInd2P.getText());
      SystemUtils.putRegString("sensitivityrules", "pqq2", textFieldPqInd2Q.getText());
      SystemUtils.putRegString("sensitivityrules", "pqn2",  textFieldPqInd2N.getText());

      SystemUtils.putRegString("sensitivityrules", "pqp3", textFieldPqHold1P.getText());
      SystemUtils.putRegString("sensitivityrules", "pqq3", textFieldPqHold1Q.getText());
      SystemUtils.putRegString("sensitivityrules", "pqn3", textFieldPqHold1N.getText());
      
      SystemUtils.putRegString("sensitivityrules", "pqp4", textFieldPqHold2P.getText());
      SystemUtils.putRegString("sensitivityrules", "pqq4", textFieldPqHold2Q.getText());
      SystemUtils.putRegString("sensitivityrules", "pqn4", textFieldPqHold2N.getText());
      //dominance rule
      SystemUtils.putRegBoolean("sensitivityrules", "domrule", checkBoxDominanceRule.isSelected());
      
      SystemUtils.putRegString("sensitivityrules", "domn1", textFieldDomInd1N.getText());
      SystemUtils.putRegString("sensitivityrules", "dimk1", textFieldDomInd1K.getText());
      
      SystemUtils.putRegString("sensitivityrules", "domn2", textFieldDomInd2N.getText());
      SystemUtils.putRegString("sensitivityrules", "dimk2", textFieldDomInd2K.getText());

      SystemUtils.putRegString("sensitivityrules", "domn3", textFieldDomHold1N.getText());
      SystemUtils.putRegString("sensitivityrules", "dimk3", textFieldDomHold1K.getText());

      SystemUtils.putRegString("sensitivityrules", "domn4", textFieldDomHold2N.getText());
      SystemUtils.putRegString("sensitivityrules", "dimk4", textFieldDomHold2K.getText());
      
      //peep rule
      SystemUtils.putRegBoolean("sensitivityrules", "peeprule", checkBoxRequestRule.isSelected());
      
      SystemUtils.putRegString("sensitivityrules", "peepindk1", textFieldReqInd1.getText());
      SystemUtils.putRegString("sensitivityrules", "peepindk2", textFieldReqInd2.getText());
      SystemUtils.putRegString("sensitivityrules", "peepindn1", textFieldReqIndFreq.getText());
      SystemUtils.putRegString("sensitivityrules", "peepindrange1", textFieldReqIndRange.getText());

      SystemUtils.putRegString("sensitivityrules", "peepholdk1", textFieldReqHold1.getText());
      SystemUtils.putRegString("sensitivityrules", "peepholdk2", textFieldReqHold2.getText());
      SystemUtils.putRegString("sensitivityrules", "peepholdn1", textFieldReqHoldFreq.getText());
      SystemUtils.putRegString("sensitivityrules", "peepholdrange1", textFieldReqHoldRange.getText());
      
      //frequency rule
      SystemUtils.putRegString("sensitivityrules", "indfreq", textFieldIndFreq.getText());
      SystemUtils.putRegString("sensitivityrules", "holdfreq", textFieldHoldFreq.getText());
      SystemUtils.putRegBoolean("sensitivityrules", "freqrule", checkBoxMinimumFrequency.isSelected());
      SystemUtils.putRegString("sensitivityrules", "indfreqrange", textFieldIndRange.getText());
      SystemUtils.putRegString("sensitivityrules", "holdreqrange", textFieldHoldRange.getText());
      
      SystemUtils.putRegBoolean("sensitivityrules", "zerorule", checkBoxZeroUnsafe.isSelected());
      SystemUtils.putRegString("sensitivityrules", "zerorulerange", textFieldlZeroUnsafeRange.getText());

      SystemUtils.putRegString("sensitivityrules", "manualrange", textFieldManualSafetyRange.getText());
    }

    
    private static class TableSetListTableModel extends AbstractTableModel {

        TableSetListTableModel() {
        }

        public void setData() {
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return TableService.numberOfTables();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }
        static String[] columnNames = {"Expl. vars", "Rule", "Resp. var", "Shadow & cost var"};

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TableSet tableSet = TableService.getTable(rowIndex);
            StringBuilder s = new StringBuilder();
            switch (columnIndex) {
                case 0:
                    for (int i = 0; i < tableSet.expVar.size(); i++) {
                        if (i != 0) {
                            s.append(',');
                        }
                        s.append(tableSet.expVar.get(i).name);
                    }
                    return s.toString();
                case 1:
                    s.append("IND.: ");
                    int K = 0;
                    if (tableSet.domRule) {
                        s.append("n=").append(tableSet.domN[K]).append(", k=").append(tableSet.domK[K]);
                    }
                    if (tableSet.pqRule) {
                        s.append("p=").append(tableSet.pqP[K]).append(", q=").append(tableSet.pqQ[K]).append(", N=").append(tableSet.pqN[K]);
                    }
                    if (!tableSet.domRule && !tableSet.pqRule) {
                        s.append("No rule");
                    }
                    for (int j = 0; j < 2; j++) {
                        if (j == 1) {
                            s.append(" Holding");
                        }
                        if (tableSet.minFreq[j] > 0) {
                            s.append(", MinFreq = ").append(tableSet.minFreq[j]);
                        }
                        if (tableSet.piepRule[j]) {
                            s.append(", RequestRule");
                        }
                        if (!tableSet.holding) {
                            break;
                        }
                    }
                    return s.toString();
                case 2:
                    s.append(tableSet.respVar.name);
//                    if (chkAddTotal.isSelected() && chkAddTotal.isEnabled()) s += ",+";
                    return s.toString();
                case 3:
                    s.append("Shadow=");
                    if (tableSet.shadowVar == null) {
                        s.append("Default,");
                    } else {
                        s.append(tableSet.shadowVar.name).append(",");
                    }
                    if (tableSet.costFunc == TableSet.COST_DIST) {
                        s.append("Dist");
                    } else if (tableSet.costFunc == TableSet.COST_UNITY) {
                        s.append("1");
                    } else if (tableSet.costFunc == TableSet.COST_FREQ) {
                        s.append("freq");
                    } else if (tableSet.costVar == null) {
                        s.append("Cost=Default");
                    } else {
                        s.append("Cost=").append(tableSet.costVar.name);
                    }
                    if (tableSet.weighted) {
                        s.append(", weighted");
                    }
                    if (tableSet.holding) {
                        s.append(", holding");
                    }
                    return s.toString();
                default:
                    return "";
            }
        }
    }
}
