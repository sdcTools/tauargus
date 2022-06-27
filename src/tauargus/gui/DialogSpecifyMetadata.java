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

import argus.model.SpssVariable;
import argus.utils.SystemUtils;
import argus.view.SpssSelectVariablesView;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.io.FilenameUtils;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.Metadata;
import tauargus.model.SpssUtilsTau;
import tauargus.model.Variable;
import tauargus.service.TableService;
import tauargus.utils.SingleListSelectionModel;
import tauargus.utils.TauArgusUtils;

public class DialogSpecifyMetadata extends DialogBase {

    private Metadata oldMetadata;
    private Metadata metadata;
    private DefaultListModel<Variable> variableListModel; 
    private Variable previousSelectedVariable;
    private static final Logger LOGGER = Logger.getLogger(DialogSpecifyMetadata.class.getName());

    // ***** Dialog Return Values *****
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;
    private int returnValue = CANCEL_OPTION;
    public static boolean SpssSelected = false;

    public DialogSpecifyMetadata(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        listVariables.setSelectionModel(new SingleListSelectionModel());
        listVariables.setCellRenderer(new VariableNameCellRenderer());
    }

    public int showDialog(Metadata metadata) {
        int i;
        this.oldMetadata = metadata;
        try {
            this.metadata = (Metadata)metadata.clone();
        } catch (CloneNotSupportedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        i=0;
        switch (metadata.dataFileType){
            case Metadata.DATA_FILE_TYPE_FIXED:i=0; break; 
            case Metadata.DATA_FILE_TYPE_FREE: i=1; break;
            case Metadata.DATA_FILE_TYPE_SPSS: i=2; break; 
        }
        comboBoxFormat.setSelectedIndex(i);
        textFieldSeparator.setText(metadata.fieldSeparator);
        variableListModel = new DefaultListModel<>();
        previousSelectedVariable = null;
        metadata.variables.forEach((variable) -> {
            variableListModel.addElement(variable);
        });
        listVariables.setModel(variableListModel);

        if (variableListModel.getSize() > 0) {
            listVariables.setSelectedIndex(0);
        }
        
        calculateButtonStates();
        panelEditVariable.setMetadata(metadata);
        panelEditVariable.setDataType(metadata.dataFileType);
        panelEditVariable.setDataOrigin(metadata.dataOrigin);

        // If start with empty rda-file, show minimum needed from panelEditvariable
        // with disabled fields and buttons; only enable the "New"-button
        if (listVariables.getSelectedValue() == null){
            panelEditVariable.panelSetEnabled(false);
        }
        pack();
        setLocationRelativeTo(this.getParent());
        setVisible(true);
        return returnValue;
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
        panelEditVariable = new tauargus.gui.PanelEditVariable();
        panelEdit = new javax.swing.JPanel();
        comboBoxFormat = new javax.swing.JComboBox();
        panelSeparator = new javax.swing.JPanel();
        labelSeparator = new javax.swing.JLabel();
        textFieldSeparator = new javax.swing.JTextField();
        scrollPane = new javax.swing.JScrollPane();
        listVariables = new javax.swing.JList<>();
        buttonNew = new javax.swing.JButton();
        buttonRemove = new javax.swing.JButton();
        buttonMoveUp = new javax.swing.JButton();
        buttonMoveDown = new javax.swing.JButton();
        jButtonGetSPSSMeta = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();

        fileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        fileChooser.setApproveButtonText("");
        fileChooser.setDialogTitle("");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Specify Metadata");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setResizable(false);

        panelEditVariable.setMaximumSize(new java.awt.Dimension(32769, 32769));
        panelEditVariable.setMinimumSize(new java.awt.Dimension(0, 0));
        panelEditVariable.setPreferredSize(null);

        comboBoxFormat.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Fixed format", "Free format", "SPSS" }));
        comboBoxFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxFormatActionPerformed(evt);
            }
        });

        labelSeparator.setLabelFor(textFieldSeparator);
        labelSeparator.setText("Separator:");

        javax.swing.GroupLayout panelSeparatorLayout = new javax.swing.GroupLayout(panelSeparator);
        panelSeparator.setLayout(panelSeparatorLayout);
        panelSeparatorLayout.setHorizontalGroup(
            panelSeparatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSeparatorLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(labelSeparator)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textFieldSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panelSeparatorLayout.setVerticalGroup(
            panelSeparatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSeparatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(textFieldSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelSeparator))
        );

        listVariables.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listVariables.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listVariablesValueChanged(evt);
            }
        });
        scrollPane.setViewportView(listVariables);

        buttonNew.setText("New");
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNewActionPerformed(evt);
            }
        });

        buttonRemove.setText("Remove");
        buttonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveActionPerformed(evt);
            }
        });

        buttonMoveUp.setMnemonic('U');
        buttonMoveUp.setText("Move Up");
        buttonMoveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMoveUpActionPerformed(evt);
            }
        });

        buttonMoveDown.setMnemonic('D');
        buttonMoveDown.setText("Move Down");
        buttonMoveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMoveDownActionPerformed(evt);
            }
        });

        jButtonGetSPSSMeta.setText("SPSS meta");
        jButtonGetSPSSMeta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGetSPSSMetaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelEditLayout = new javax.swing.GroupLayout(panelEdit);
        panelEdit.setLayout(panelEditLayout);
        panelEditLayout.setHorizontalGroup(
            panelEditLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelEditLayout.createSequentialGroup()
                .addGroup(panelEditLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboBoxFormat, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelEditLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(buttonMoveUp, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonMoveDown, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonRemove, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonNew, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonGetSPSSMeta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(8, 8, 8))
        );
        panelEditLayout.setVerticalGroup(
            panelEditLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelEditLayout.createSequentialGroup()
                .addComponent(comboBoxFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelEditLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelEditLayout.createSequentialGroup()
                        .addComponent(buttonNew)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemove)
                        .addGap(18, 18, 18)
                        .addComponent(buttonMoveUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonMoveDown)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonGetSPSSMeta))
                    .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 437, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
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
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(panelEditVariable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonCancel)
                        .addGap(19, 19, 19))))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOK});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelEditVariable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 491, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOK)
                    .addComponent(buttonCancel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        
        if (this.panelEditVariable.getScaleParamsTable().isEditing()){
            if (!this.panelEditVariable.getScaleParamsTable().getCellEditor().stopCellEditing()) return;
        }

        // Trigger a listvalue change so last edited variable data will be stored
        listVariables.clearSelection();
        
        switch (comboBoxFormat.getSelectedIndex()){
            case 0: metadata.dataFileType  = Metadata.DATA_FILE_TYPE_FIXED;
                    break;
            case 1: metadata.dataFileType  = Metadata.DATA_FILE_TYPE_FREE;
                    break;
            case 2: metadata.dataFileType  = Metadata.DATA_FILE_TYPE_SPSS;
                    break;
        }
        
        metadata.fieldSeparator = textFieldSeparator.getText();
        metadata.variables.clear();
        for (int i = 0; i < variableListModel.getSize(); i++) {
            metadata.variables.add(variableListModel.get(i));
        }

        // Always check the validity of the metadata: you may have come from incorrect metadata and did not change correctly
        try {
            metadata.verify(); 
        } 
        catch (ArgusException ex) {
            if (!ex.getMessage().isEmpty()){JOptionPane.showMessageDialog(this, ex.getMessage());}
            listVariables.setSelectedIndex(0);
            panelEditVariable.load(variableListModel.get(0));
            return;
        }
        if (!metadata.equals(oldMetadata)) { // Data has changed
            //It looks a bit weird, but the PanelEditVariable stores the new statusses in oldmetadata.
            //vreemde actie van Robert
            metadata.safeStatus = oldMetadata.safeStatus;
            metadata.unSafeStatus = oldMetadata.unSafeStatus;
            metadata.protectStatus = oldMetadata.protectStatus;
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Metadata has been changed. Save changes to file?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                String hs = SystemUtils.getRegString("general", "datadir", "");
                if (!hs.equals("")){
                  File file = new File(hs); 
                  fileChooser.setCurrentDirectory(file);
                }
                TauArgusUtils.getDataDirFromRegistry(fileChooser);
                fileChooser.setDialogTitle("Save metadata as");
                fileChooser.setSelectedFile(new File(""));
                fileChooser.resetChoosableFileFilters();
                final FileNameExtensionFilter metadataFileFilter = new FileNameExtensionFilter("Metadata (*.rda)", "rda");
                fileChooser.setFileFilter(metadataFileFilter);
                fileChooser.setApproveButtonText("Save");
                if (fileChooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                    String fileName = fileChooser.getSelectedFile().getPath();
                    TauArgusUtils.putDataDirInRegistry(fileName);
                    if (FilenameUtils.indexOfExtension(fileName) == -1) {
                        FileFilter fileFilter = fileChooser.getFileFilter();
                        if (fileFilter instanceof FileNameExtensionFilter) {
                            FileNameExtensionFilter fileNameExtensionFilter = (FileNameExtensionFilter) fileFilter;
                            String[] extensions = fileNameExtensionFilter.getExtensions();
                            fileName = fileName + "." + extensions[0];
                        }
                    }
                    // fileChooser.setSelectedFile(new File(fileName));
                    metadata.write(fileName);
                }
            }
            Application.replaceMetadata(oldMetadata, metadata);
            TableService.clearTables(); // Make sure that the "old" tables are no longer available because the metadata has changed
        }
        returnValue = APPROVE_OPTION;
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        setVisible(false);
        dispose(); // free memory of this dialog
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewActionPerformed
        int selectedIndex = listVariables.getSelectedIndex();
        Variable variable = new Variable(metadata);
        variable.name = "New";
        variable.bPos = 1;
        variable.type = tauargus.model.Type.CATEGORICAL;
        variable.missing = new String[Variable.MAX_NUMBER_OF_MISSINGS];
        for (int i=0; i<Variable.MAX_NUMBER_OF_MISSINGS; i++) {
            variable.missing[i] = "";
        }
        variableListModel.add(selectedIndex + 1, variable);
        listVariables.setSelectedIndex(selectedIndex + 1);
        listVariables.ensureIndexIsVisible(selectedIndex + 1);
    }//GEN-LAST:event_buttonNewActionPerformed
    
    public void SetSpecificElement(Variable variable){
       int index = listVariables.getSelectedIndex(); 
       if (index != -1){ 
        variableListModel.set(index, variable);        
       }
    }
    
    private void buttonRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveActionPerformed
        int selectedIndex = listVariables.getSelectedIndex();
        // set the selection to an item that still exists after deletion
        // if not done before removal the remove button will loose focus
        if (selectedIndex == variableListModel.getSize()- 1) {
            listVariables.setSelectedIndex(selectedIndex - 1);
        }
        else {
            listVariables.setSelectedIndex(selectedIndex + 1);
        }
        variableListModel.remove(selectedIndex);
        listVariables.ensureIndexIsVisible(selectedIndex + 1);
        calculateButtonStates();
    }//GEN-LAST:event_buttonRemoveActionPerformed

    private void buttonMoveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMoveUpActionPerformed
        int selectedIndex = listVariables.getSelectedIndex();
        Variable variable = variableListModel.get(selectedIndex);
        variableListModel.set(selectedIndex, variableListModel.get(selectedIndex - 1));
        variableListModel.set(selectedIndex - 1, variable);
        listVariables.setSelectedIndex(selectedIndex - 1);
    }//GEN-LAST:event_buttonMoveUpActionPerformed

    private void buttonMoveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMoveDownActionPerformed
        int selectedIndex = listVariables.getSelectedIndex();
        Variable variable = variableListModel.get(selectedIndex);
        variableListModel.set(selectedIndex, variableListModel.get(selectedIndex + 1));
        variableListModel.set(selectedIndex + 1, variable);
        listVariables.setSelectedIndex(selectedIndex + 1);
    }//GEN-LAST:event_buttonMoveDownActionPerformed

    private void comboBoxFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxFormatActionPerformed
        if (metadata.dataOrigin == Metadata.DATA_ORIGIN_TABULAR){
           if (comboBoxFormat.getSelectedIndex()!= 1){
               comboBoxFormat.setSelectedIndex(1);               
               JOptionPane.showMessageDialog(null,"For tabular data free format is required");
           }
        }
        panelEditVariable.enableForSPSS(comboBoxFormat.getSelectedIndex()==2);
        SpssSelected = comboBoxFormat.getSelectedIndex()==2;
        switch(comboBoxFormat.getSelectedIndex()){
            case 0:  
              labelSeparator.setVisible(false);
              textFieldSeparator.setVisible(false);
              panelEditVariable.setDataType(Metadata.DATA_FILE_TYPE_FIXED);
              break;
            case 1:
              labelSeparator.setVisible(true);
              textFieldSeparator.setVisible(true);
              panelEditVariable.setDataType(Metadata.DATA_FILE_TYPE_FREE);
                break;
            case 2:
              labelSeparator.setVisible(false);
              textFieldSeparator.setVisible(false);  
              panelEditVariable.setDataType(Metadata.DATA_FILE_TYPE_SPSS); 
        }
       jButtonGetSPSSMeta.setEnabled(comboBoxFormat.getSelectedIndex() == 2);
//       if (Metadata.dataOrigin = Metadata.DATA_ORIGIN_TABULAR)
// Hier controle op data origin en free format       
       calculateButtonStates();       
    }//GEN-LAST:event_comboBoxFormatActionPerformed

    private void listVariablesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listVariablesValueChanged
        Variable selectedVariable = listVariables.getSelectedValue();
        if (selectedVariable != previousSelectedVariable) {
            if (previousSelectedVariable != null) {
                // check if previous selected variable is not deleted
                int index = variableListModel.indexOf(previousSelectedVariable);
                if (index != -1) {
                    Variable variable = new Variable(metadata);
                    try {
                        panelEditVariable.save(variable);
                        variableListModel.set(index, variable);
                    } catch (ArgusException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                        return;
                    }
                }
            }
            if (selectedVariable != null) {
                panelEditVariable.load(selectedVariable);
            }
        }

        previousSelectedVariable = selectedVariable;
        calculateButtonStates();
        panelEditVariable.enableForSPSS(comboBoxFormat.getSelectedIndex()==2);
        pack();
    }//GEN-LAST:event_listVariablesValueChanged

    private void jButtonGetSPSSMetaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGetSPSSMetaActionPerformed
     int j;
     SpssSelectVariablesView selectView = new SpssSelectVariablesView(null, true);
     selectView.showVariables(SpssUtilsTau.spssVariables);
     selectView.setVisible(true);
  //   Fill the meta with the SPSS variables
     metadata.variables.clear();
     int startPos = 1; 
     for (SpssVariable v : SpssUtilsTau.spssVariables) {
       if (v.isSelected()) {
       Variable var = new Variable(metadata);
       var.name = v.getName();
       var.bPos = startPos;
       var.varLen = v.getVariableLength();
       startPos = startPos + var.varLen;
       if (v.isCategorical()){var.type = tauargus.model.Type.CATEGORICAL;}
       if (v.isNumeric())    {
           var.type = tauargus.model.Type.RESPONSE;
           var.nDecimals = v.getNumberOfDecimals();
           }
       var.missing = new String[Variable.MAX_NUMBER_OF_MISSINGS];
       for (int i=0; i<Variable.MAX_NUMBER_OF_MISSINGS; i++) {var.missing[i] = "";}
       if (v.getStringMissings() != null){
         j = v.getStringMissings().length;
         for (int i=0;i<v.getStringMissings().length;i++){
            var.missing[i] = v.getStringMissings()[i];
         }
       }

       metadata.variables.add(var);
       }
     }
     variableListModel.clear();
     metadata.variables.forEach((variable) -> {
         variableListModel.addElement(variable);
        });
   
    }//GEN-LAST:event_jButtonGetSPSSMetaActionPerformed

    private void calculateButtonStates() {
        int selectedIndex = listVariables.getSelectedIndex();
        if (comboBoxFormat.getSelectedIndex() == 2){ //SPSS
            buttonRemove.setEnabled(false);
            buttonNew.setEnabled(false);
            buttonMoveUp.setEnabled(false);
            buttonMoveDown.setEnabled(false);                      
        } else
        {    
            buttonRemove.setEnabled(selectedIndex != -1);
            buttonNew.setEnabled(true);
            buttonMoveUp.setEnabled(selectedIndex > 0);
            buttonMoveDown.setEnabled((selectedIndex != -1) && (selectedIndex < variableListModel.getSize() - 1));
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonMoveDown;
    private javax.swing.JButton buttonMoveUp;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonRemove;
    private javax.swing.JComboBox comboBoxFormat;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JButton jButtonGetSPSSMeta;
    private javax.swing.JLabel labelSeparator;
    private javax.swing.JList<tauargus.model.Variable> listVariables;
    private javax.swing.JPanel panelEdit;
    private tauargus.gui.PanelEditVariable panelEditVariable;
    private javax.swing.JPanel panelSeparator;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTextField textFieldSeparator;
    // End of variables declaration//GEN-END:variables
}
