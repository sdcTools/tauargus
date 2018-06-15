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

import java.io.File;
import javax.swing.JOptionPane;
import tauargus.model.SaveTable;
import tauargus.model.TableSet;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.io.FilenameUtils;
import tauargus.model.ArgusException;
import argus.utils.StrUtils;
//import tauargus.utils.ExecUtils;
import argus.utils.SystemUtils;
import java.awt.Cursor;
import static javax.swing.JOptionPane.YES_OPTION;
import tauargus.utils.TauArgusUtils;

public class DialogSaveTable extends DialogBase {

    private TableSet tableSet;
    private String[] extensions = {"csv", "csv", "txt","sbs","tab","jj","tab"};
    public DialogSaveTable(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(parent);
    }

    public void showDialog(TableSet tableSet){
        this.tableSet = tableSet;
        this.jRadioCKMFormat.setEnabled(tableSet.ckmProtect);
        setVisible(true);
    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupFormat = new javax.swing.ButtonGroup();
        jFileChooser1 = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jRadioCSVFormat = new javax.swing.JRadioButton();
        jRadioCSVPivot = new javax.swing.JRadioButton();
        jRadioCodeValue = new javax.swing.JRadioButton();
        jRadioSBSFormat = new javax.swing.JRadioButton();
        jRadioIntermediate = new javax.swing.JRadioButton();
        jRadioJJFormat = new javax.swing.JRadioButton();
        jButtonWrite = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JSeparator();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jCheckBoxAddHierarchicalLevels = new javax.swing.JCheckBox();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        checkBoxStatusOnly = new javax.swing.JCheckBox();
        checkBoxAddAuditResults = new javax.swing.JCheckBox();
        checkBoxUseHoldingInfo = new javax.swing.JCheckBox();
        jCheckBoxremoveTrivialLevels = new javax.swing.JCheckBox();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        jCheckBoxAddStatus = new javax.swing.JCheckBox();
        jCheckBoxSuppressEmptyCells = new javax.swing.JCheckBox();
        jCheckBoxVarNamesOnFirstRow = new javax.swing.JCheckBox();
        jCheckBoxEmbedSpanningVarQuotes = new javax.swing.JCheckBox();
        buttonCancel = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JSeparator();
        textFieldSaveFileName = new javax.swing.JTextField();
        jButtonChooseSafeFile = new javax.swing.JButton();
        jRadioCKMFormat = new javax.swing.JRadioButton();
        jCheckBoxAddOrigVal = new javax.swing.JCheckBox();
        jCheckBoxAddDiff = new javax.swing.JCheckBox();
        jCheckBoxAddCellKey = new javax.swing.JCheckBox();
        jSeparator8 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Save table");
        setIconImage(null);
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel1.setName(""); // NOI18N

        buttonGroupFormat.add(jRadioCSVFormat);
        jRadioCSVFormat.setText("CSV format");
        jRadioCSVFormat.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioCSVFormatItemStateChanged(evt);
            }
        });

        buttonGroupFormat.add(jRadioCSVPivot);
        jRadioCSVPivot.setText("CSV for pivot table");
        jRadioCSVPivot.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioCSVPivotItemStateChanged(evt);
            }
        });

        buttonGroupFormat.add(jRadioCodeValue);
        jRadioCodeValue.setSelected(true);
        jRadioCodeValue.setText("Code-value");
        jRadioCodeValue.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioCodeValueItemStateChanged(evt);
            }
        });

        buttonGroupFormat.add(jRadioSBSFormat);
        jRadioSBSFormat.setText("SBS format");
        jRadioSBSFormat.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioSBSFormatItemStateChanged(evt);
            }
        });

        buttonGroupFormat.add(jRadioIntermediate);
        jRadioIntermediate.setText("Intermediate format");
        jRadioIntermediate.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioIntermediateItemStateChanged(evt);
            }
        });

        buttonGroupFormat.add(jRadioJJFormat);
        jRadioJJFormat.setText("JJ format");
        jRadioJJFormat.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioJJFormatItemStateChanged(evt);
            }
        });

        jButtonWrite.setText("Write table");
        jButtonWrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteActionPerformed(evt);
            }
        });

        jCheckBoxAddHierarchicalLevels.setText("Add hierarchical levels");
        jCheckBoxAddHierarchicalLevels.setEnabled(false);

        checkBoxStatusOnly.setText("Status only");
        checkBoxStatusOnly.setEnabled(false);

        checkBoxAddAuditResults.setText("Add audit results");
        checkBoxAddAuditResults.setEnabled(false);

        checkBoxUseHoldingInfo.setText("Use holding info");
        checkBoxUseHoldingInfo.setEnabled(false);

        jCheckBoxremoveTrivialLevels.setText("Remove trivial levels");
        jCheckBoxremoveTrivialLevels.setEnabled(false);

        jLabel1.setText("General options");

        jCheckBoxAddStatus.setText("Add status");

        jCheckBoxSuppressEmptyCells.setText("Suppress empty cells");

        jCheckBoxVarNamesOnFirstRow.setText("Variable names on first row");

        jCheckBoxEmbedSpanningVarQuotes.setSelected(true);
        jCheckBoxEmbedSpanningVarQuotes.setText("Embed spanning variables in quotes");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        jButtonChooseSafeFile.setText("...");
        jButtonChooseSafeFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonChooseSafeFileActionPerformed(evt);
            }
        });

        buttonGroupFormat.add(jRadioCKMFormat);
        jRadioCKMFormat.setText("CKM format");
        jRadioCKMFormat.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioCKMFormatItemStateChanged(evt);
            }
        });

        jCheckBoxAddOrigVal.setText("Add original value");
        jCheckBoxAddOrigVal.setEnabled(false);

        jCheckBoxAddDiff.setText("Add difference");
        jCheckBoxAddDiff.setEnabled(false);

        jCheckBoxAddCellKey.setText("Add cell-key");
        jCheckBoxAddCellKey.setEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator3)
            .addComponent(jSeparator2)
            .addComponent(jSeparator4, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator6, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator5, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator8)
            .addComponent(jSeparator7)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jRadioSBSFormat))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jRadioIntermediate))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jRadioJJFormat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jRadioCKMFormat, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE))))
                .addGap(40, 40, 40)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxEmbedSpanningVarQuotes)
                    .addComponent(jCheckBoxVarNamesOnFirstRow)
                    .addComponent(jCheckBoxSuppressEmptyCells)
                    .addComponent(jCheckBoxAddStatus)
                    .addComponent(jCheckBoxAddCellKey)
                    .addComponent(jCheckBoxAddDiff, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxAddOrigVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCheckBoxremoveTrivialLevels, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxUseHoldingInfo)
                    .addComponent(checkBoxAddAuditResults)
                    .addComponent(checkBoxStatusOnly)
                    .addComponent(jCheckBoxAddHierarchicalLevels))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jRadioCodeValue))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jRadioCSVPivot))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jRadioCSVFormat))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jButtonWrite)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonCancel))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(textFieldSaveFileName, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButtonChooseSafeFile)))))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jRadioCSVFormat)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioCSVPivot)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioCodeValue)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioSBSFormat)
                    .addComponent(jCheckBoxAddHierarchicalLevels))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioIntermediate)
                    .addComponent(checkBoxStatusOnly))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxAddAuditResults)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxUseHoldingInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioJJFormat)
                    .addComponent(jCheckBoxremoveTrivialLevels))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioCKMFormat)
                    .addComponent(jCheckBoxAddOrigVal))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxAddDiff)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxAddCellKey)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jCheckBoxAddStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxSuppressEmptyCells)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxVarNamesOnFirstRow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxEmbedSpanningVarQuotes)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldSaveFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonChooseSafeFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(jButtonWrite))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private boolean testExpandFileName(){
// check if there is a path specified. If not suggest to add the datadir    
        String dataDir = FilenameUtils.getFullPath(SystemUtils.getRegString("general", "datadir", ""));
        String hs = FilenameUtils.getFullPath(tableSet.safeFileName).trim();
        if (hs.equals("")){
          int n = JOptionPane.showConfirmDialog(DialogSaveTable.this,
                  "You need to specify a directory for saving the table\n" + 
                  "Do you want to add \"" + dataDir +"\"","",JOptionPane.YES_NO_OPTION);  
          if (n == YES_OPTION){
            tableSet.safeFileName = dataDir+tableSet.safeFileName;  
            textFieldSaveFileName.setText(tableSet.safeFileName);
            return true;
          }
          else
          {
            return false;  
          }
        }
        return true;
    }
    
    private void jButtonWriteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteActionPerformed
        int selectedFormat = whichFormat();
        tableSet.safeFileName = textFieldSaveFileName.getText();
        if (tableSet.safeFileName.equals("")){
           JOptionPane.showMessageDialog(DialogSaveTable.this, "Please specify a file name first");
           return;
        }
        if (!testExpandFileName()) {return;}
        if (FilenameUtils.getExtension(tableSet.safeFileName).equals("")) {
            tableSet.safeFileName = tableSet.safeFileName + "." + this.extensions[selectedFormat];
        }
        
        File file = new File(tableSet.safeFileName);
        if (file.exists()){
           int result = JOptionPane.showConfirmDialog(DialogSaveTable.this, "This file does already exist.\nDo you want to overwrite?",
                            "", JOptionPane.YES_NO_OPTION);
           if (JOptionPane.NO_OPTION == result) { return;}
        }
        

        SaveTable.writeAddStatus = jCheckBoxAddStatus.isSelected();
        SaveTable.writeSupppressEmpty = jCheckBoxSuppressEmptyCells.isSelected();
        SaveTable.writeVarnamesOnFirstLine = jCheckBoxVarNamesOnFirstRow.isSelected();
        SaveTable.writeEmbedQuotes = jCheckBoxEmbedSpanningVarQuotes.isSelected();
        SaveTable.writeSBSHierarchicalLevels = jCheckBoxAddHierarchicalLevels.isSelected();
        SaveTable.writeIntermediateStatusOnly = checkBoxStatusOnly.isSelected();
        SaveTable.writeIntermediateAddAudit = checkBoxAddAuditResults.isSelected();
        SaveTable.writeIntermediateUseHolding = checkBoxUseHoldingInfo.isSelected();
        SaveTable.writeJJRemoveBogus = jCheckBoxremoveTrivialLevels.isSelected();
        
        SaveTable.writeCKMOriginalValues = jCheckBoxAddOrigVal.isSelected();
        SaveTable.writeCKMDifferences = jCheckBoxAddDiff.isSelected();
        SaveTable.writeCKMCellKeys = jCheckBoxAddCellKey.isSelected();
        
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SaveTable.writeTable(tableSet, selectedFormat);
            setCursor(Cursor.getDefaultCursor());
        } catch (ArgusException ex) {
            setCursor(Cursor.getDefaultCursor());
            tableSet.safeFileName = "";
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
        SaveTable.writeReport(tableSet);

        setVisible(false);
    }//GEN-LAST:event_jButtonWriteActionPerformed

    private int whichFormat(){
        int i = TableSet.FILE_FORMAT_CSV;
        if (jRadioCSVFormat.isSelected())   {i= TableSet.FILE_FORMAT_CSV;}
        if (jRadioCSVPivot.isSelected())    {i= TableSet.FILE_FORMAT_PIVOT_TABLE;}
        if (jRadioCodeValue.isSelected())   {i= TableSet.FILE_FORMAT_CODE_VALUE;}
        if (jRadioSBSFormat.isSelected())   {i= TableSet.FILE_FORMAT_SBS;}
        if (jRadioIntermediate.isSelected()){i= TableSet.FILE_FORMAT_INTERMEDIATE;}
        if (jRadioJJFormat.isSelected())    {i= TableSet.FILE_FORMAT_JJ;}
        if (jRadioCKMFormat.isSelected())   {i= TableSet.FILE_FORMAT_CKM;}
        return i;          
    }
    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        tableSet.safeFileName = "";
        setVisible(false);
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void jButtonChooseSafeFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonChooseSafeFileActionPerformed
      int selectedFormat;  
      selectedFormat = whichFormat();
//        String hs = SystemUtils.getRegString("general", "datadir", "");
//        if (!hs.equals("")){
//            File file = new File(hs); 
//            jFileChooser1.setCurrentDirectory(file);
//        }
        TauArgusUtils.getDataDirFromRegistry(jFileChooser1);
        // TODO add your handling code here:
        jFileChooser1.setDialogTitle("Safe file name");
        jFileChooser1.setSelectedFile(new File(""));
        jFileChooser1.resetChoosableFileFilters();
//        jFileChooser1.setCurrentDirectory("");
        // filters are shown in order of declaration, setFileFilter sets the default filter
        jFileChooser1.setFileFilter(new FileNameExtensionFilter("Safe file name (*."+ 
                this.extensions[selectedFormat] +")", this.extensions[selectedFormat]));
        if (jFileChooser1.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            tableSet.safeFileName=jFileChooser1.getSelectedFile().toString();
            textFieldSaveFileName.setText(tableSet.safeFileName);
            TauArgusUtils.putDataDirInRegistry(tableSet.safeFileName);
        }
    }//GEN-LAST:event_jButtonChooseSafeFileActionPerformed

    private void jRadioCSVPivotItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioCSVPivotItemStateChanged
        // TODO add your handling code here:
        jCheckBoxVarNamesOnFirstRow.setSelected(jRadioCSVPivot.isSelected());
        jCheckBoxEmbedSpanningVarQuotes.setSelected(jRadioCSVPivot.isSelected());
        enableGeneralOptions();
    }//GEN-LAST:event_jRadioCSVPivotItemStateChanged

    private void jRadioCodeValueItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioCodeValueItemStateChanged
        // TODO add your handling code here:
        jCheckBoxEmbedSpanningVarQuotes.setSelected(jRadioCodeValue.isSelected());
        enableGeneralOptions();
    }//GEN-LAST:event_jRadioCodeValueItemStateChanged

    private void jRadioCSVFormatItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioCSVFormatItemStateChanged
        // TODO add your handling code here:
        jCheckBoxEmbedSpanningVarQuotes.setSelected(jRadioCSVFormat.isSelected());
        enableGeneralOptions();
    }//GEN-LAST:event_jRadioCSVFormatItemStateChanged

    private void jRadioSBSFormatItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioSBSFormatItemStateChanged
        // TODO add your handling code here:
        jCheckBoxAddHierarchicalLevels.setEnabled(jRadioSBSFormat.isSelected());
        jCheckBoxEmbedSpanningVarQuotes.setSelected(jRadioSBSFormat.isSelected());
        enableGeneralOptions();
    }//GEN-LAST:event_jRadioSBSFormatItemStateChanged

    private void jRadioIntermediateItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioIntermediateItemStateChanged
        // TODO add your handling code here:
        checkBoxStatusOnly.setEnabled(jRadioIntermediate.isSelected());
        checkBoxAddAuditResults.setEnabled(jRadioIntermediate.isSelected());
        checkBoxUseHoldingInfo.setEnabled(jRadioIntermediate.isSelected());
        jCheckBoxEmbedSpanningVarQuotes.setSelected(jRadioIntermediate.isSelected());
        enableGeneralOptions();
    }//GEN-LAST:event_jRadioIntermediateItemStateChanged

    private void jRadioJJFormatItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioJJFormatItemStateChanged
        // TODO add your handling code here:
        jCheckBoxremoveTrivialLevels.setEnabled(jRadioJJFormat.isSelected());
        jCheckBoxEmbedSpanningVarQuotes.setSelected(false);
        enableGeneralOptions();
    }//GEN-LAST:event_jRadioJJFormatItemStateChanged

    private void jRadioCKMFormatItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioCKMFormatItemStateChanged
        // TODO add your handling code here:
        jCheckBoxAddOrigVal.setEnabled(jRadioCKMFormat.isSelected());
        jCheckBoxAddDiff.setEnabled(jRadioCKMFormat.isSelected());
        jCheckBoxAddCellKey.setEnabled(jRadioCKMFormat.isSelected());
        enableGeneralOptions();
    }//GEN-LAST:event_jRadioCKMFormatItemStateChanged
    
    private  void enableGeneralOptions(){
        jCheckBoxAddStatus.setEnabled(jRadioCSVPivot.isSelected() ||
                                      jRadioCodeValue.isSelected()  );
        jCheckBoxEmbedSpanningVarQuotes.setEnabled(jRadioCSVPivot.isSelected() ||
                                                   jRadioCodeValue.isSelected()||
                                                   jRadioIntermediate.isSelected() ||
                                                   jRadioSBSFormat.isSelected() ||
                                                   jRadioCSVFormat.isSelected() ||
                                                   jRadioCKMFormat.isSelected() );
        jCheckBoxSuppressEmptyCells.setEnabled(jRadioCSVPivot.isSelected() ||
                                                jRadioCodeValue.isSelected()||
                                                jRadioIntermediate.isSelected() ||
                                                jRadioSBSFormat.isSelected() ||
                                                jRadioCKMFormat.isSelected() );
        jCheckBoxVarNamesOnFirstRow.setEnabled(//jRadioCSVPivot.isSelected() ||
                                                jRadioCodeValue.isSelected() //||
                                                //jRadioIntermediate.isSelected() ||
                                                //jRadioSBSFormat.isSelected()
                                                );
//jCheckBoxVarNamesOnFirstRow.setSelected(jRadioCSVPivot.isSelected());
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DialogSaveTable.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DialogSaveTable.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DialogSaveTable.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DialogSaveTable.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DialogSaveTable dialog = new DialogSaveTable(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroupFormat;
    private javax.swing.JCheckBox checkBoxAddAuditResults;
    private javax.swing.JCheckBox checkBoxStatusOnly;
    private javax.swing.JCheckBox checkBoxUseHoldingInfo;
    private javax.swing.JButton jButtonChooseSafeFile;
    private javax.swing.JButton jButtonWrite;
    private javax.swing.JCheckBox jCheckBoxAddCellKey;
    private javax.swing.JCheckBox jCheckBoxAddDiff;
    private javax.swing.JCheckBox jCheckBoxAddHierarchicalLevels;
    private javax.swing.JCheckBox jCheckBoxAddOrigVal;
    private javax.swing.JCheckBox jCheckBoxAddStatus;
    private javax.swing.JCheckBox jCheckBoxEmbedSpanningVarQuotes;
    private javax.swing.JCheckBox jCheckBoxSuppressEmptyCells;
    private javax.swing.JCheckBox jCheckBoxVarNamesOnFirstRow;
    private javax.swing.JCheckBox jCheckBoxremoveTrivialLevels;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton jRadioCKMFormat;
    private javax.swing.JRadioButton jRadioCSVFormat;
    private javax.swing.JRadioButton jRadioCSVPivot;
    private javax.swing.JRadioButton jRadioCodeValue;
    private javax.swing.JRadioButton jRadioIntermediate;
    private javax.swing.JRadioButton jRadioJJFormat;
    private javax.swing.JRadioButton jRadioSBSFormat;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JTextField textFieldSaveFileName;
    // End of variables declaration//GEN-END:variables
}
