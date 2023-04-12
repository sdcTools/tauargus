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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.apache.commons.lang3.StringUtils;
import tauargus.extern.dataengine.TauArgus;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.RecodeInfo;
import tauargus.model.TableSet;
import tauargus.model.VarCodeProperties;
import tauargus.model.Variable;
import tauargus.service.TableService;
import tauargus.utils.SingleListSelectionModel;
import tauargus.utils.TauArgusUtils;
import tauargus.utils.TreeUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;

public class DialogGlobalRecode extends DialogBase {

    @Override
    protected JRootPane createRootPane() {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                buttonCloseActionPerformed(actionEvent);
            }
        };    
 
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane tmpRootPane = new JRootPane();
        tmpRootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return tmpRootPane;
    }    
    
    private static class CodeInfo {
        private final int codeIndex;
        private final String label;
        private final boolean active;
        private final boolean missing;

        public CodeInfo(int codeIndex, String label, boolean missing, boolean active) {
            this.codeIndex = codeIndex;
            this.label = label;
            this.missing = missing;
            this.active = active;
        }

        public int getCodeIndex() {
            return codeIndex;
        }

        public String getLabel() {
            return label;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isMissing() {
            return missing;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    
    private static class VariableTableModel extends AbstractTableModel {
        private List<Variable> variables;

        VariableTableModel(List<Variable> variables) {
            this.variables = variables;
        }

        public void setData(List<Variable> variables) {
            this.variables = variables;
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return variables.size();
        }

        static String[] columnNames = {"Recoded", "Variable"};//, "Extended variable" };

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return variables.get(rowIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Variable.class;
        }
    }
    
    /**
     * Will be used for listening to changes in missing1, missing2, codelist and recode data
     */
    private static class MyDocumentListener implements DocumentListener {
        private boolean changed = false;

        @Override
        public void insertUpdate(DocumentEvent e) {
            changed = true;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changed = true;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changed = true;
        }

        public void setChanged(boolean changed) {
            this.changed = changed;
        }

        public boolean isChanged() {
            return changed;
        }
    }
        
    private static final Logger LOGGER = Logger.getLogger(DialogGlobalRecode.class.getName());

    private TauArgus tauArgus = Application.getTauArgusDll();
    private TableSet tableSet;
    private Variable variable;
    private boolean fromTree;
    private MyDocumentListener documentListener;

    // Creates new form DialogGlobalRecode
    public DialogGlobalRecode(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        tableVariables.setDefaultRenderer(Variable.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Variable variable = (Variable) value;
                if (column == 0) {
                    if (variable.recoded)
                        value = " R";
                    else if (variable.truncLevels > 0)
                        value = " T";
                    else
                        value = "";
                }
                if (column == 1) {
                    value = variable.name; // VarName(i)
                }
/*                if (column == 2) { // Why is this column here? Equals column 1?
                    value = variable.name; // metadata.Varlist(i).Name
                }*/
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if ((variable.recoded || variable.truncLevels > 0)){// && !isSelected) {
                            component.setForeground(Color.red);
                } else {
                    component.setForeground(isSelected ? Color.white : Color.black);
                }
                return component;
            }
        });

        panelRecodeData.setVisible(false);
        panelTree.setVisible(false);
        treeCode.setVisible(false);
        
        ListSelectionModel selectionModel = new SingleListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (documentListener.isChanged()) {
                    documentListener.setChanged(false);
                    saveRecodeInfo(false);
                }
                int selectedRow = tableVariables.getSelectedRow();
                if (selectedRow != -1) {
                    variable = tableSet.expVar.get(selectedRow);
                    fromTree = variable.hierarchical != Variable.HIER_NONE;
                    panelRecodeData.setVisible(!fromTree);
                    panelMissing.setVisible(!fromTree);
                    panelTree.setVisible(fromTree);
                    treeCode.setVisible(fromTree);
                    buttonUndo.setEnabled(variable.recoded);
                    buttonApply.setEnabled(!variable.recoded);
                    buttonRead.setEnabled(true);
                    if (fromTree) {
                        try{
                            buildTree();
                            if (variable.recoded){
                                labelTreeResult.setText("Tree recode has been applied successfully");
                                labelTreeResult.setToolTipText(null);
                            }
                            else{
                                labelTreeResult.setText("Tree structure for global recode");
                                labelTreeResult.setToolTipText(null);
                            }
                        }
                        catch (Exception ex){
                            JOptionPane.showMessageDialog(DialogGlobalRecode.this, ex.getMessage());        
                        }
                    } else {
                        if (variable.recoded) {
                            try {
                                RecodeInfo recodeInfo = variable.readRecodeFile(variable.currentRecodeFile);
                                textFieldCodelist.setText(variable.currentRecodeCodeListFile);
                                if (recodeInfo != null) {
                                    labelRecodeData.setText("Recode has been applied successfully");
                                    labelRecodeData.setToolTipText(null);
                                    textAreaRecodeData.setText(recodeInfo.getRecodeData());
                                    textFieldMissing1.setText(recodeInfo.getMissing1());
                                    textFieldMissing2.setText(recodeInfo.getMissing2());
                                    textFieldCodelist.setText(recodeInfo.getCodeList());
                                }
                               } catch (ArgusException | IOException  ex) {
                                JOptionPane.showMessageDialog(DialogGlobalRecode.this, ex.getMessage());}
                        }
                        else{
                            labelRecodeData.setText("Edit box for global recode");
                            labelRecodeData.setToolTipText(null);
                            textAreaRecodeData.setText("");
                            textFieldMissing1.setText("");
                            textFieldMissing2.setText("");
                            textFieldCodelist.setText("");
                        }
                        textAreaWarning.setText("");
                    }
                }
                documentListener.setChanged(false);
            }
        });

        documentListener = new MyDocumentListener();
        textAreaRecodeData.getDocument().addDocumentListener(documentListener);
        textFieldMissing1.getDocument().addDocumentListener(documentListener);
        textFieldMissing2.getDocument().addDocumentListener(documentListener);
        textFieldCodelist.getDocument().addDocumentListener(documentListener);
        
        tableVariables.setSelectionModel(selectionModel);
    
        treeCode.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof CodeInfo) {
                    CodeInfo codeInfo = (CodeInfo) userObject;
                    if (!codeInfo.isActive()) {
                        component.setForeground(Color.gray);
                    } else if (codeInfo.isMissing()) {
                        component.setForeground(Color.red);
                    }
                }
                return component;
            }
        });
        
//        addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                if (documentListener.isChanged()) {
//                    documentListener.setChanged(false);
//                    saveRecodeInfo(false);
//                }
//
//                tauArgus.ApplyRecode();
//            }
//        });
//
//        setLocationRelativeTo(parent);
    }

    public void showDialog(TableSet tableSet) {
        this.tableSet = tableSet;

        tableVariables.setModel(new VariableTableModel(tableSet.expVar));
        //TableColumnResizer.adjustColumnPreferredWidths(tableVariables, false);
        tableVariables.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableVariables.getColumnModel().getColumn(0).setMaxWidth(60);
        
        if (!tableSet.expVar.isEmpty()) {
            tableVariables.setRowSelectionInterval(0, 0);
        }
        
        if (!variable.recoded){
            labelRecodeData.setText("Edit box for global recode");
            labelTreeResult.setText("Tree structure for global recode");
        }
        else{
            labelRecodeData.setText("Recode has been applied successfully");
            labelTreeResult.setText("Tree recode has been applied successfully");
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (documentListener.isChanged()) {
                    documentListener.setChanged(false);
                    saveRecodeInfo(false);
                }

                tauArgus.ApplyRecode();
            }
        });

        setLocationRelativeTo(this.getParent());
        
        pack();
        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        scrollPaneVariables = new javax.swing.JScrollPane();
        tableVariables = new javax.swing.JTable();
        panelRecode = new javax.swing.JPanel();
        buttonRead = new javax.swing.JButton();
        buttonApply = new javax.swing.JButton();
        buttonUndo = new javax.swing.JButton();
        panelClose = new javax.swing.JPanel();
        buttonClose = new javax.swing.JButton();
        panelMissing = new javax.swing.JPanel();
        labelMissing1 = new javax.swing.JLabel();
        textFieldMissing1 = new javax.swing.JTextField();
        labelMissing2 = new javax.swing.JLabel();
        textFieldMissing2 = new javax.swing.JTextField();
        panelRecodeData = new javax.swing.JPanel();
        labelRecodeData = new javax.swing.JLabel();
        scrollPaneRecodeData = new javax.swing.JScrollPane();
        textAreaRecodeData = new javax.swing.JTextArea();
        labelCodelist = new javax.swing.JLabel();
        textFieldCodelist = new javax.swing.JTextField();
        buttonCodelist = new javax.swing.JButton();
        labelWarning = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textAreaWarning = new javax.swing.JTextArea();
        panelTree = new javax.swing.JPanel();
        labelMaxLevel = new javax.swing.JLabel();
        comboBoxMaxLevel = new javax.swing.JComboBox<>();
        scrollPaneCodeTree = new javax.swing.JScrollPane();
        treeCode = new javax.swing.JTree();
        labelTreeResult = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        tableVariables.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Recoded", "Variable"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableVariables.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableVariables.getTableHeader().setReorderingAllowed(false);
        scrollPaneVariables.setViewportView(tableVariables);

        panelRecode.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        buttonRead.setText("Read");
        buttonRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReadActionPerformed(evt);
            }
        });

        buttonApply.setText("Apply");
        buttonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonApplyActionPerformed(evt);
            }
        });

        buttonUndo.setText("Undo");
        buttonUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUndoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelRecodeLayout = new javax.swing.GroupLayout(panelRecode);
        panelRecode.setLayout(panelRecodeLayout);
        panelRecodeLayout.setHorizontalGroup(
            panelRecodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRecodeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelRecodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonRead, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonApply, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonUndo, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        panelRecodeLayout.setVerticalGroup(
            panelRecodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRecodeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonRead)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonApply)
                .addGap(18, 18, 18)
                .addComponent(buttonUndo)
                .addContainerGap())
        );

        panelClose.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        buttonClose.setText("Close");
        buttonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCloseLayout = new javax.swing.GroupLayout(panelClose);
        panelClose.setLayout(panelCloseLayout);
        panelCloseLayout.setHorizontalGroup(
            panelCloseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCloseLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelCloseLayout.setVerticalGroup(
            panelCloseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCloseLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonClose)
                .addContainerGap())
        );

        panelMissing.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Missing values"));

        labelMissing1.setText("1");

        labelMissing2.setText("2");

        javax.swing.GroupLayout panelMissingLayout = new javax.swing.GroupLayout(panelMissing);
        panelMissing.setLayout(panelMissingLayout);
        panelMissingLayout.setHorizontalGroup(
            panelMissingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMissingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMissingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelMissing1)
                    .addComponent(labelMissing2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMissingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textFieldMissing1)
                    .addComponent(textFieldMissing2))
                .addContainerGap())
        );
        panelMissingLayout.setVerticalGroup(
            panelMissingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMissingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMissingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelMissing1)
                    .addComponent(textFieldMissing1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMissingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelMissing2)
                    .addComponent(textFieldMissing2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelRecodeData.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        labelRecodeData.setText("<FileName>");

        textAreaRecodeData.setColumns(20);
        textAreaRecodeData.setRows(5);
        scrollPaneRecodeData.setViewportView(textAreaRecodeData);

        labelCodelist.setText("Codelist for recode:");
        labelCodelist.setName(""); // NOI18N

        buttonCodelist.setText("...");
        buttonCodelist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCodelistActionPerformed(evt);
            }
        });

        labelWarning.setText("Warning:");

        jScrollPane1.setToolTipText("");
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        textAreaWarning.setColumns(20);
        textAreaWarning.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        textAreaWarning.setRows(5);
        textAreaWarning.setPreferredSize(new java.awt.Dimension(232, 69));
        jScrollPane1.setViewportView(textAreaWarning);

        javax.swing.GroupLayout panelRecodeDataLayout = new javax.swing.GroupLayout(panelRecodeData);
        panelRecodeData.setLayout(panelRecodeDataLayout);
        panelRecodeDataLayout.setHorizontalGroup(
            panelRecodeDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRecodeDataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelRecodeDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(labelRecodeData, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelCodelist, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelRecodeDataLayout.createSequentialGroup()
                        .addComponent(textFieldCodelist, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCodelist, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(labelWarning, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1)
                    .addComponent(scrollPaneRecodeData))
                .addContainerGap())
        );
        panelRecodeDataLayout.setVerticalGroup(
            panelRecodeDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRecodeDataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelRecodeData)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPaneRecodeData, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelCodelist)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelRecodeDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonCodelist)
                    .addComponent(textFieldCodelist, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelWarning)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        panelTree.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelTree.setPreferredSize(new java.awt.Dimension(250, 427));

        labelMaxLevel.setText("Maximum level:");

        comboBoxMaxLevel.setMaximumRowCount(9);
        comboBoxMaxLevel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        comboBoxMaxLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxMaxLevelActionPerformed(evt);
            }
        });

        treeCode.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        treeCode.setMinimumSize(new java.awt.Dimension(12, 20));
        scrollPaneCodeTree.setViewportView(treeCode);

        labelTreeResult.setMaximumSize(new java.awt.Dimension(66, 16));
        labelTreeResult.setMinimumSize(new java.awt.Dimension(66, 16));
        labelTreeResult.setPreferredSize(new java.awt.Dimension(66, 16));

        javax.swing.GroupLayout panelTreeLayout = new javax.swing.GroupLayout(panelTree);
        panelTree.setLayout(panelTreeLayout);
        panelTreeLayout.setHorizontalGroup(
            panelTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTreeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelTreeLayout.createSequentialGroup()
                        .addComponent(labelMaxLevel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(labelTreeResult, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollPaneCodeTree, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE))
                .addGap(8, 8, 8))
        );
        panelTreeLayout.setVerticalGroup(
            panelTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTreeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelTreeResult, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboBoxMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPaneCodeTree, javax.swing.GroupLayout.PREFERRED_SIZE, 353, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPaneVariables, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelRecode, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelMissing, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelRecodeData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelTree, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelTree, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(panelRecodeData, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(scrollPaneVariables, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(panelRecode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelClose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelMissing, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelRecodeData.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUndoActionPerformed
        int rowIndex = tableVariables.getSelectedRow();
        if (!tauArgus.UndoRecode(variable.index)) {
            JOptionPane.showMessageDialog(this, "Unknown error when undoing recode");
        } else {
            variable.recoded = false;
            variable.truncLevels = 0;
            ((AbstractTableModel)tableVariables.getModel()).fireTableDataChanged();
            buttonUndo.setEnabled(false);
            if (fromTree) {
                buildTree();
            }
            labelTreeResult.setText("Tree structure for global recode");
            labelTreeResult.setForeground(Color.black);
            labelTreeResult.setToolTipText(null);
            labelWarning.setText("");
            labelWarning.setForeground(Color.black);
            LOGGER.info("Recode for var: " + variable.name + " has been reversed\n"); // VarName(CurrentVar)
            SystemUtils.writeLogbook("Recode for var: " + variable.name + " has been reversed");
            buttonApply.setEnabled(true); 
            buttonUndo.setEnabled(false);
            buttonRead.setEnabled(!fromTree);
            textAreaWarning.setText("");
        }
        tableVariables.setRowSelectionInterval(rowIndex, rowIndex);
    }//GEN-LAST:event_buttonUndoActionPerformed

    private void buttonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCloseActionPerformed
        setVisible(false);
        if (documentListener.isChanged()) {
            documentListener.setChanged(false);
            saveRecodeInfo(false);
        }
        
        tauArgus.ApplyRecode();
        for (int i=0;i<TableService.numberOfTables();i++){
            TableSet tmptableSet = TableService.getTable(i);
            tmptableSet.clearHistory();
        }
        dispose();
    }//GEN-LAST:event_buttonCloseActionPerformed

    private void comboBoxMaxLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxMaxLevelActionPerformed
        int selectedIndex = comboBoxMaxLevel.getSelectedIndex(); //int maxDepth = 0;
        if (selectedIndex != -1) {
            //maxDepth = expand(selectedIndex + 1);
            expand(selectedIndex + 1);
        }
    }//GEN-LAST:event_comboBoxMaxLevelActionPerformed

    private void buttonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonApplyActionPerformed
        int rowIndex = tableVariables.getSelectedRow();
        if (fromTree) {
            tauArgus.UndoRecode(variable.index);
            for (int row = 0; row < treeCode.getRowCount(); row++) {
                if (treeCode.isCollapsed(row)) {
                    TreePath treePath = treeCode.getPathForRow(row);
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    if (!treeCode.getModel().isLeaf(treeNode)) {
                        CodeInfo codeInfo = (CodeInfo)treeNode.getUserObject();
                        if (!tauArgus.SetVarCodeActive(variable.index, codeInfo.getCodeIndex(), false)) {
                            JOptionPane.showMessageDialog(this, "Iets fout in de tree");
                        }
                   }
                }
            }
            if (tauArgus.DoActiveRecode(variable.index)) {
                variable.recoded = true;
                buttonUndo.setEnabled(true);
                buttonApply.setEnabled(false);
                labelTreeResult.setText("Tree recode has been applied successfully");
                labelTreeResult.setForeground(Color.black);
                ((AbstractTableModel)tableVariables.getModel()).fireTableDataChanged();
                LOGGER.log(Level.INFO, "Var: {0} has been recoded\n", variable.name); 
                SystemUtils.writeLogbook("Var: " + variable.name + " has been recoded");
                buildTree();
                int i = JOptionPane.showConfirmDialog(this, "Do you want to save the recoding of the tree", "ARGUS-recodefiles", JOptionPane.YES_NO_OPTION);
                if (i == JOptionPane.YES_OPTION) 
                    saveRecodeInfo(true); // true = for treebased recoding
            } else {
                JOptionPane.showMessageDialog(this, "This hierarchical recoding could not be applied");
                labelTreeResult.setText("Tree recode could not be applied");
                labelTreeResult.setForeground(Color.red);
            }
        } 
        else {
            RecodeInfo recodeInfo = new RecodeInfo(
                textAreaRecodeData.getText(), 
                textFieldMissing1.getText(), 
                textFieldMissing2.getText(), 
                textFieldCodelist.getText());
            
            try {
                variable.recode(recodeInfo);
                buttonApply.setEnabled(false);
                buttonUndo.setEnabled(true);
                buttonRead.setEnabled(false);
                textAreaWarning.setText(Variable.RecodeWarning);
                LOGGER.log(Level.INFO, "Var: {0} has been recoded", variable.name);
                ((AbstractTableModel)tableVariables.getModel()).fireTableDataChanged();
//                textAreaRecodeData.requestFocusInWindow();
//                textAreaRecodeData.setCaretPosition(StrUtils.startingPosition(textAreaRecodeData.getText(), errorLine[0], errorPos[0]));
            } catch (ArgusException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        }
        tableVariables.setRowSelectionInterval(rowIndex, rowIndex);
    }//GEN-LAST:event_buttonApplyActionPerformed

    private void buttonCodelistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCodelistActionPerformed
        TauArgusUtils.getDataDirFromRegistry(fileChooser);
        fileChooser.setDialogTitle("Select CodeList files");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CodeList files (*.cdl)", "cdl"));
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            String codeListFile = fileChooser.getSelectedFile().toString();
            TauArgusUtils.putDataDirInRegistry(codeListFile);            
            textFieldCodelist.setText(codeListFile);
            variable.currentRecodeCodeListFile = codeListFile;
        }
    }//GEN-LAST:event_buttonCodelistActionPerformed

    private void buttonReadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReadActionPerformed
//        String hs = SystemUtils.getRegString("general", "datadir", "");
//        if (!hs.equals("")){
//            File file = new File(hs); 
//            fileChooser.setCurrentDirectory(file);
//        }
        int rowIndex = tableVariables.getSelectedRow();
        TauArgusUtils.getDataDirFromRegistry(fileChooser);        
        fileChooser.setDialogTitle("Recode files");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Recode files (*.grc)", "grc"));
        
        if (fromTree) {
            if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                String recodeListFile = fileChooser.getSelectedFile().toString();
                TauArgusUtils.putDataDirInRegistry(recodeListFile);
                int i, r, j, nc; 
                String xs;
                j = TauArgusUtils.getNumberOfCodes(variable.index);
                int[] Codes = new int[j];
                buildTree();
                //r = treeCode.getRowCount(); 
                nc = 0; 
                xs = "";
                try (BufferedReader reader = new BufferedReader(new FileReader(recodeListFile))){
                    String regel = reader.readLine();
                    if (!StringUtils.equals(regel, "<TREERECODE>")) {
                        throw new ArgusException("First line does not start with \"<TREERECODE>\"");
                    }
                    while ((regel = reader.readLine()) != null) {
                        if (StringUtils.isNotBlank(regel)) {
                            j = TauArgusUtils.getCodeIndex(variable.index, regel);
                            //hs = String.valueOf(j);
                            if (j == -1){ 
                                throw new ArgusException("Code (" + regel + ") not found");
                            }
                            Codes[nc] = j;
                            nc = nc + 1;
                            xs = xs + " Code: " + Integer.toString(nc) +  " waarde " + Integer.toString(j);
//                }                      
                        } 
                    }
// Read all codes. We need the inverse order to close all the necessary nodes
                    for(i=0; i<nc; i++) {
                        for(j=i+1; j<nc; j++){
                            if ( Codes[i] < Codes[j] ) { 
                                r = Codes[j]; 
                                Codes[j] = Codes[i]; 
                                Codes[i] = r;
                            }
                        }
                    }
                    for(i=0; i<nc; i++) {
                        treeCode.collapseRow(Codes[i]);
                    }
              
                    labelTreeResult.setText(recodeListFile);
                    labelTreeResult.setToolTipText(recodeListFile);
//      //      tauArgus.UndoRecode(index);
//            String regel = reader.readLine();
//            if (!StringUtils.equals(regel, "<TREERECODE>")) {
//                throw new ArgusException("First line does not start with \"<TREERECODE>\"");
//            }
//            while ((regel = reader.readLine()) != null) {
//                if (StringUtils.isNotBlank(regel)) {
//                    int codeIndex = TauArgusUtils.getCodeIndex(index, regel);
//                    if (codeIndex == -1) {
//                        throw new ArgusException("Code (" + regel + ") not found");
//                    } 
//                    treeCode.
//                    tauArgus.SetVarCodeActive(index, codeIndex, false);
//                }
//            }
//            tauArgus.DoActiveRecode(index);
                }
                catch (Exception ex) {
//            tauArgus.UndoRecode(index);
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                }
            }
        } else {
            if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                String fileName = fileChooser.getSelectedFile().toString();
                TauArgusUtils.putDataDirInRegistry(fileName);
                if (StringUtils.isNotBlank(fileName)) {
                    try {
                    if (!TauArgusUtils.ExistFile(fileName)){ throw new ArgusException ("Recode file ("+fileName+ ") does not exist");}
                        RecodeInfo recodeInfo = variable.readRecodeFile(fileName);
                        if (recodeInfo != null) {
                            labelRecodeData.setText(fileName);
                            labelRecodeData.setToolTipText(fileName);
                            textAreaRecodeData.setText(recodeInfo.getRecodeData());
                            textFieldMissing1.setText(recodeInfo.getMissing1());
                            textFieldMissing2.setText(recodeInfo.getMissing2());
                            textFieldCodelist.setText(recodeInfo.getCodeList());
                            variable.currentRecodeFile = fileName;
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage());
                    }
                } 
            }
        }
        tableVariables.setRowSelectionInterval(rowIndex, rowIndex);
    }//GEN-LAST:event_buttonReadActionPerformed

    private void saveRecodeInfo(Boolean forTreeRecode) {
        RecodeInfo recodeInfo = new RecodeInfo(textAreaRecodeData.getText(), textFieldMissing1.getText(), textFieldMissing2.getText(), textFieldCodelist.getText());
        int i;
        if (forTreeRecode){ // You only get here when you want to save the tree recode
            i = JOptionPane.YES_OPTION;
        }
        else{
            i = JOptionPane.showConfirmDialog(this, "Recode information has been changed.\nSave recodefile?", "ARGUS-recodefiles", JOptionPane.YES_NO_OPTION);
        }
        if (i == JOptionPane.YES_OPTION) {
//        String hs = SystemUtils.getRegString("general", "datadir", "");
//        if (!hs.equals("")){
//            File file = new File(hs); 
//            fileChooser.setCurrentDirectory(file);
//        }
            TauArgusUtils.getDataDirFromRegistry(fileChooser);
            fileChooser.setDialogTitle("Save global recode file");
            fileChooser.setSelectedFile(new File(variable.currentRecodeFile));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Recode files (*.grc)", "grc"));
            if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                variable.currentRecodeFile = fileChooser.getSelectedFile().toString();
                TauArgusUtils.putDataDirInRegistry(variable.currentRecodeFile);
                if (!forTreeRecode){
                   try {
                       variable.writeRecodeFile(variable.currentRecodeFile, recodeInfo);
                   } catch (Exception ex) {
                       JOptionPane.showMessageDialog(DialogGlobalRecode.this, ex.getMessage());
                   }
                   return;
                }
                else // Save a real tree
                {
                    try{
                       variable.writeRecodeTreeFile(variable.currentRecodeFile); 
                    } catch (Exception ex) {
                       JOptionPane.showMessageDialog(DialogGlobalRecode.this, ex.getMessage());
                    }
                    return;    
                }
            }    
        }
        if(forTreeRecode){return;}
        // we do not need a temp recode file for a hierarchical recode
        String tempDir = Application.getTempDir();
        File f = new File(tempDir, "Argus" + variable.index + ".grc");
        variable.currentRecodeFile = f.getAbsolutePath();
        try {
            variable.writeRecodeFile(variable.currentRecodeFile, recodeInfo);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(DialogGlobalRecode.this, ex.getMessage());
        }
    }

    private int addChildren(DefaultMutableTreeNode parentNode, VarCodeProperties parentProperties, int codeIndex) {
        int nChildren = parentProperties.getnChildren();
        VarCodeProperties properties = new VarCodeProperties();
        for (int i = 0; i < nChildren; i++) {
            codeIndex++;
            properties.setCode(variable.index, codeIndex); 
            CodeInfo codeInfo = new CodeInfo(codeIndex, properties.getCode(), properties.isMissing(), properties.isActive());
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(codeInfo, properties.isParent());
            parentNode.add(childNode);
            codeIndex = addChildren(childNode, properties, codeIndex);
        }
        return codeIndex;
    }
    
    private void addMissing(DefaultMutableTreeNode topNode, int codeIndex) {
        VarCodeProperties properties = new VarCodeProperties();
        for (;;) {
            codeIndex++;
            if (!properties.setCode(variable.index, codeIndex)) {
                break;
            }
            CodeInfo codeInfo = new CodeInfo(codeIndex, properties.getCode(), properties.isMissing(), properties.isActive());
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(codeInfo, properties.isParent());
            topNode.add(childNode);
        }
    }
    
    private void buildTree() {
        int codeIndex = 0; int maxDepth; int VarDepth; int MaxLevelChoice;
        VarCodeProperties properties = new VarCodeProperties(variable.index, codeIndex);
        CodeInfo codeInfo = new CodeInfo(codeIndex, properties.getCode(), properties.isMissing(), properties.isActive());
        DefaultMutableTreeNode topNode = new DefaultMutableTreeNode(codeInfo, properties.isParent());
        codeIndex = addChildren(topNode, properties, codeIndex);
        addMissing(topNode, codeIndex);

        DefaultTreeModel treeModel = new DefaultTreeModel(topNode);
        treeCode.setModel(treeModel);
        maxDepth = expand(Integer.MAX_VALUE);
        //maxDepth = expand(variable.GetDepthOfHierarchicalBoom());
        VarDepth = variable.GetDepthOfHierarchicalBoom(variable.recoded)-1; // here levels are numbered 0, 1, 2, 3, ...
        MaxLevelChoice = comboBoxMaxLevel.getItemCount()-1;
        if (VarDepth - MaxLevelChoice < 0)
        {            
            for (int j=VarDepth;j<MaxLevelChoice;j++)
            {
                comboBoxMaxLevel.removeItemAt(VarDepth+1);
            }
        }
        else
        {
            for (int j=MaxLevelChoice+1;j<=VarDepth;j++)
            {
                comboBoxMaxLevel.addItem(Integer.toString(j+1));
            }
            
        }
        comboBoxMaxLevel.setSelectedIndex(maxDepth);
    }
    
    private int expand(int depth) {
        TreeModel treeModel = treeCode.getModel(); int j;
        TreeUtils.collapse(treeCode, 0);
        int maxDepth = 0;
        for (int row = 0; row < treeCode.getRowCount(); row++) {
            TreePath treePath = treeCode.getPathForRow(row);
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
            j = treeNode.getLevel();
            if (treeNode.getLevel() < depth && treeModel.getChildCount(treeNode) != 0) {
                DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)treeModel.getChild(treeNode, 0);
                CodeInfo codeInfo = (CodeInfo)firstChild.getUserObject();
                if (codeInfo.isActive()) {
                    treeCode.expandRow(row);
                    if (treeNode.getLevel() > maxDepth){maxDepth=treeNode.getLevel(); }
                }
            }
        }   
        return maxDepth;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonApply;
    private javax.swing.JButton buttonClose;
    private javax.swing.JButton buttonCodelist;
    private javax.swing.JButton buttonRead;
    private javax.swing.JButton buttonUndo;
    private javax.swing.JComboBox<String> comboBoxMaxLevel;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelCodelist;
    private javax.swing.JLabel labelMaxLevel;
    private javax.swing.JLabel labelMissing1;
    private javax.swing.JLabel labelMissing2;
    private javax.swing.JLabel labelRecodeData;
    private javax.swing.JLabel labelTreeResult;
    private javax.swing.JLabel labelWarning;
    private javax.swing.JPanel panelClose;
    private javax.swing.JPanel panelMissing;
    private javax.swing.JPanel panelRecode;
    private javax.swing.JPanel panelRecodeData;
    private javax.swing.JPanel panelTree;
    private javax.swing.JScrollPane scrollPaneCodeTree;
    private javax.swing.JScrollPane scrollPaneRecodeData;
    private javax.swing.JScrollPane scrollPaneVariables;
    private javax.swing.JTable tableVariables;
    private javax.swing.JTextArea textAreaRecodeData;
    private javax.swing.JTextArea textAreaWarning;
    private javax.swing.JTextField textFieldCodelist;
    private javax.swing.JTextField textFieldMissing1;
    private javax.swing.JTextField textFieldMissing2;
    private javax.swing.JTree treeCode;
    // End of variables declaration//GEN-END:variables
}
