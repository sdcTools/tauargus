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

//import tauargus.model.APriori;

import java.awt.Cursor;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import tauargus.model.APriori;
import tauargus.model.ArgusException;
import tauargus.model.TableSet;
import tauargus.service.TableService;
import tauargus.utils.TauArgusUtils;

/**
 *
 * @author whcg
 */
public class DialogReadApriori extends DialogBase {

    
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;
    private int apTableNumber = 0;
    APriori _Apriori;

    
    /**
     * Creates new form ReadApriori
     */
    public DialogReadApriori(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        jTable1.setVisible(false);
        chkExpandTrivialLevels.setVisible(true);
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
        jLabel1 = new javax.swing.JLabel();
        txtAprioriFilename = new javax.swing.JTextField();
        btnSelectAprioriFilename = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        txtSeparator = new javax.swing.JTextField();
        chkIgnoreIcorrectLines = new javax.swing.JCheckBox();
        chkExpandTrivialLevels = new javax.swing.JCheckBox();
        buttonOK = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("APriori filename");

        txtAprioriFilename.setMinimumSize(new java.awt.Dimension(50, 20));

        btnSelectAprioriFilename.setText("jButton1");
        btnSelectAprioriFilename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectAprioriFilenameActionPerformed(evt);
            }
        });

        jLabel2.setText("Separator");

        txtSeparator.setText(",");
        txtSeparator.setToolTipText("");

        chkIgnoreIcorrectLines.setText("Ignore incorrect lines");

        chkExpandTrivialLevels.setText("Expand for trivial levels");

        buttonOK.setText("Apply");
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

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Type", "Correct", "Incorrect"
            }
        ));
        jTable1.setFillsViewportHeight(true);
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addGap(18, 18, 18)
                            .addComponent(txtSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jLabel1)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(chkIgnoreIcorrectLines, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(chkExpandTrivialLevels, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                                .addComponent(txtAprioriFilename, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnSelectAprioriFilename, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtAprioriFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSelectAprioriFilename))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkIgnoreIcorrectLines)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkExpandTrivialLevels)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOK)
                    .addComponent(buttonCancel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        TableSet currentTable = TableService.getTable(apTableNumber); // is 0 altijd de current table ???
        int[][] aPrioryStatus = new int[10][2];///check: is OK, 1e index = 5 waardes, 2e index slechts 2

        try{
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            TableSet.processAprioryFile(txtAprioriFilename.getText(), 
                                    apTableNumber,  txtSeparator.getText(), 
                                    chkIgnoreIcorrectLines.isSelected(),
                                    chkExpandTrivialLevels.isSelected(), 
                                    true, aPrioryStatus);
            jTable1.setVisible(true);
            int line=0;
            for(int type=0; type<5; type++)
                { if (type!=3){           
                    jTable1.getModel().setValueAt(APriori.getStatus(type), line, 0 );
                    jTable1.getModel().setValueAt(aPrioryStatus[type][0], line, 1 );
                    jTable1.getModel().setValueAt(aPrioryStatus[type][1], line, 2 );
                    line++;
                    }  
                }   
            setCursor(Cursor.getDefaultCursor());
        }
        catch (ArgusException ex){
                setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(this,ex.getMessage());
                if(! (aPrioryStatus == null) ){
                    TableSet.CloseAprioriFiles(chkExpandTrivialLevels.isSelected(), aPrioryStatus);}
        }
        returnValue = APPROVE_OPTION;
    }//GEN-LAST:event_buttonOKActionPerformed

    
    public void ShowDialog()
    {
        setLocationRelativeTo(this.getParent());
        setVisible(true);
    }
    
    public void SetAprioyTable(int tabNo)
    {
        apTableNumber = tabNo;
    }
    
    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void btnSelectAprioriFilenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectAprioriFilenameActionPerformed
        TauArgusUtils.getDataDirFromRegistry(fileChooser);
        fileChooser.setDialogTitle("Select Apriori file");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new FileNameExtensionFilter("APriori file (*.hst)", "hst"));
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            String aprioriFile = fileChooser.getSelectedFile().toString();
            txtAprioriFilename.setText(aprioriFile);
            
            _Apriori = new APriori(aprioriFile);
            
            String hs = fileChooser.getSelectedFile().getPath();
            TauArgusUtils.putDataDirInRegistry(hs);
        }
    }//GEN-LAST:event_btnSelectAprioriFilenameActionPerformed

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
//            java.util.logging.Logger.getLogger(DialogReadApriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(DialogReadApriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(DialogReadApriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(DialogReadApriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                DialogReadApriori dialog = new DialogReadApriori(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton btnSelectAprioriFilename;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private javax.swing.JCheckBox chkExpandTrivialLevels;
    private javax.swing.JCheckBox chkIgnoreIcorrectLines;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtAprioriFilename;
    private javax.swing.JTextField txtSeparator;
    // End of variables declaration//GEN-END:variables


    private int returnValue = CANCEL_OPTION;

}
