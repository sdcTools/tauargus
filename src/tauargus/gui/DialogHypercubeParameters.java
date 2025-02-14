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

import tauargus.model.TableSet;
import tauargus.utils.IntegerInputVerifier;
import tauargus.utils.SwingUtils;
/**
 * Organises the control over the specific parameters of the hypercube method
 * Whether or not inferential disclosure protection is required
 * Also the memory model can be specified. 
 * If a table has very long codelists a larger model has to be chosen.
 * But in fact a better contruction of the codelists 
 * (with a few hierarchies) is to be advised.
 * This will lead to a  much better performance
 * Also the singleton protection can be de-activated (
 * By default the singleton protection is active in the hypercube 
 * @author ahnl
 */
public class DialogHypercubeParameters extends DialogBase {

    // ***** Dialog Return Values *****
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;
    
    private int returnValue = CANCEL_OPTION;

    private int ghmMaxSubCode;
    private int ghmMaxSubTab;
    private int ghmSize;
    private TableSet tableset;
 
    // Creates new form DialogHypercubeParameters
    public DialogHypercubeParameters(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        IntegerInputVerifier integerInputVerifier = new IntegerInputVerifier();
        textFieldMaxSubCode.setInputVerifier(integerInputVerifier);
        textFieldMaxSubTable.setInputVerifier(integerInputVerifier);
        organizeAllOptions();
    }

    public int showDialog(TableSet table) {
        checkBoxSingleton.setEnabled(table.hasRealFreq);
        this.tableset = table;
        setLocationRelativeTo(this.getParent());
        setVisible(true);

        return returnValue;
    }
    
    private void organizeAPrioriOptions() {
        boolean b = checkBoxAPriori.isSelected();
        labelAPriori.setEnabled(b);
        textFieldAPriori.setEnabled(b);
    }

    private void organizeGHMiterOptions() {
        boolean B = radioButtonGHSizeManual.isSelected();
        labelMaxSubCode.setEnabled(B);
        textFieldMaxSubCode.setEnabled(B);
        labelMaxSubTable.setEnabled(B);
        textFieldMaxSubTable.setEnabled(B);
        if (!checkBoxSingleton.isEnabled()) {
            checkBoxSingleton.setSelected(false);
        }
    }
    
    private void organizeAllOptions() {
        organizeAPrioriOptions();
        organizeGHMiterOptions();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupGHSize = new javax.swing.ButtonGroup();
        labelInfo = new javax.swing.JLabel();
        checkBoxAPriori = new javax.swing.JCheckBox();
        textFieldAPriori = new javax.swing.JTextField();
        labelAPriori = new javax.swing.JLabel();
        checkBoxSingleton = new javax.swing.JCheckBox();
        panelMemoryModel = new javax.swing.JPanel();
        radioButtonGHSizeNormal = new javax.swing.JRadioButton();
        radioButtonGHSizeLarge = new javax.swing.JRadioButton();
        radioButtonGHSizeManual = new javax.swing.JRadioButton();
        labelMaxSubCode = new javax.swing.JLabel();
        labelMaxSubTable = new javax.swing.JLabel();
        textFieldMaxSubCode = new javax.swing.JTextField();
        textFieldMaxSubTable = new javax.swing.JTextField();
        panelCommand = new javax.swing.JPanel();
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("GHMiter specifications");
        setIconImage(null);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                DialogClosing(evt);
            }
        });

        labelInfo.setText("Additional parameters for the use of GHMiter:");

        checkBoxAPriori.setSelected(true);
        checkBoxAPriori.setText("Protection against inferential disclosure required");
        checkBoxAPriori.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxAPrioriActionPerformed(evt);
            }
        });

        textFieldAPriori.setText("100");

        labelAPriori.setText("% external a priori bounds on the cell values");

        checkBoxSingleton.setSelected(true);
        checkBoxSingleton.setText("Apply singleton protection");

        panelMemoryModel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Memory model", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.TOP));

        buttonGroupGHSize.add(radioButtonGHSizeNormal);
        radioButtonGHSizeNormal.setSelected(true);
        radioButtonGHSizeNormal.setText("Normal size");
        radioButtonGHSizeNormal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonGroupGHSizeActionPerformed(evt);
            }
        });

        buttonGroupGHSize.add(radioButtonGHSizeLarge);
        radioButtonGHSizeLarge.setText("Large size");
        radioButtonGHSizeLarge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonGroupGHSizeActionPerformed(evt);
            }
        });

        buttonGroupGHSize.add(radioButtonGHSizeManual);
        radioButtonGHSizeManual.setText("Manual");
        radioButtonGHSizeManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonGroupGHSizeActionPerformed(evt);
            }
        });

        labelMaxSubCode.setLabelFor(textFieldMaxSubCode);
        labelMaxSubCode.setText("Max sub-codelist size");

        labelMaxSubTable.setLabelFor(textFieldMaxSubTable);
        labelMaxSubTable.setText("Max sub-table size");

        javax.swing.GroupLayout panelMemoryModelLayout = new javax.swing.GroupLayout(panelMemoryModel);
        panelMemoryModel.setLayout(panelMemoryModelLayout);
        panelMemoryModelLayout.setHorizontalGroup(
            panelMemoryModelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMemoryModelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMemoryModelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMemoryModelLayout.createSequentialGroup()
                        .addGroup(panelMemoryModelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelMaxSubCode)
                            .addComponent(labelMaxSubTable))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelMemoryModelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(textFieldMaxSubCode, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                            .addComponent(textFieldMaxSubTable)))
                    .addComponent(radioButtonGHSizeNormal)
                    .addComponent(radioButtonGHSizeLarge)
                    .addComponent(radioButtonGHSizeManual))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelMemoryModelLayout.setVerticalGroup(
            panelMemoryModelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMemoryModelLayout.createSequentialGroup()
                .addComponent(radioButtonGHSizeNormal)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonGHSizeLarge)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonGHSizeManual)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMemoryModelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelMaxSubCode)
                    .addComponent(textFieldMaxSubCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMemoryModelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelMaxSubTable)
                    .addComponent(textFieldMaxSubTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        buttonOk.setText("OK");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCommandLayout = new javax.swing.GroupLayout(panelCommand);
        panelCommand.setLayout(panelCommandLayout);
        panelCommandLayout.setHorizontalGroup(
            panelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCommandLayout.createSequentialGroup()
                .addComponent(buttonCancel)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(buttonOk, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelCommandLayout.setVerticalGroup(
            panelCommandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCommandLayout.createSequentialGroup()
                .addComponent(buttonOk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCancel))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelInfo)
                            .addComponent(checkBoxAPriori))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                        .addComponent(panelCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxSingleton)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(textFieldAPriori, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelAPriori))
                            .addComponent(panelMemoryModel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelInfo)
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxAPriori))
                    .addComponent(panelCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldAPriori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelAPriori))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxSingleton)
                .addGap(18, 18, 18)
                .addComponent(panelMemoryModel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void checkBoxAPrioriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxAPrioriActionPerformed
        organizeAPrioriOptions();
    }//GEN-LAST:event_checkBoxAPrioriActionPerformed

    private void buttonGroupGHSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonGroupGHSizeActionPerformed
        organizeGHMiterOptions();
    }//GEN-LAST:event_buttonGroupGHSizeActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void DialogClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_DialogClosing
        setVisible(false);
        dispose();
    }//GEN-LAST:event_DialogClosing
    
    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        if (SwingUtils.verifyTextFields(this)) {
            storeGHMiterParameters();
            setVisible(false);
            returnValue = APPROVE_OPTION;    
            dispose();
        }
    }//GEN-LAST:event_buttonOkActionPerformed
   private void storeGHMiterParameters(){
       tableset.ghMiterApplySingleton = checkBoxSingleton.isSelected();
       tableset.ghMiterApriory = checkBoxAPriori.isSelected();
       tableset.ghMiterAprioryPercentage = Integer.parseInt(textFieldAPriori.getText());
       if (radioButtonGHSizeNormal.isSelected()) tableset.ghMiterSize = 0;
       if (radioButtonGHSizeLarge.isSelected()) tableset.ghMiterSize = 1;
       if (radioButtonGHSizeManual.isSelected()){
          tableset.ghMiterSize = 2;
          tableset.ghMiterSubtable=Integer.parseInt(textFieldMaxSubTable.getText());
          tableset.ghMiterSubcode=Integer.parseInt(textFieldMaxSubCode.getText());       
      }
   }
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(DialogHypercubeParameters.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(DialogHypercubeParameters.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(DialogHypercubeParameters.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(DialogHypercubeParameters.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                DialogHypercubeParameters dialog = new DialogHypercubeParameters(new javax.swing.JFrame(), true);
//                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
//                    @Override
//                    public void windowClosing(java.awt.event.WindowEvent e) {
//                        System.exit(0);
//                    }
//                });
//                dialog.setVisible(true);
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroupGHSize;
    private javax.swing.JButton buttonOk;
    private javax.swing.JCheckBox checkBoxAPriori;
    private javax.swing.JCheckBox checkBoxSingleton;
    private javax.swing.JLabel labelAPriori;
    private javax.swing.JLabel labelInfo;
    private javax.swing.JLabel labelMaxSubCode;
    private javax.swing.JLabel labelMaxSubTable;
    private javax.swing.JPanel panelCommand;
    private javax.swing.JPanel panelMemoryModel;
    private javax.swing.JRadioButton radioButtonGHSizeLarge;
    private javax.swing.JRadioButton radioButtonGHSizeManual;
    private javax.swing.JRadioButton radioButtonGHSizeNormal;
    private javax.swing.JTextField textFieldAPriori;
    private javax.swing.JTextField textFieldMaxSubCode;
    private javax.swing.JTextField textFieldMaxSubTable;
    // End of variables declaration//GEN-END:variables
}
