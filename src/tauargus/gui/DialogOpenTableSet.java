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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import tauargus.model.DataFilePair;
import tauargus.utils.TauArgusUtils;

public class DialogOpenTableSet extends DialogBase {

    private static final Logger logger = Logger.getLogger(DialogOpenTableSet.class.getName());

    // ***** Dialog Return Values *****
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;

    public DialogOpenTableSet(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    public int showDialog() {
        setLocationRelativeTo(this.getParent());
        setVisible(true);
        return returnValue;
    }
    
    public List<DataFilePair> getDataFilePairList() {
        return dataFilePairList;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelSelectTable = new tauargus.gui.PanelSelectTableFiles(false);
        labelTables = new javax.swing.JLabel();
        scrollPane = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonAdd = new javax.swing.JButton();
        buttonRemove = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Open TableSet");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                DialogClosing(evt);
            }
        });

        labelTables.setLabelFor(panelSelectTable);
        labelTables.setText("Tables:");

        table.setModel(dataFilePairListTableModel);
        table.getTableHeader().setReorderingAllowed(false);
        scrollPane.setViewportView(table);

        buttonAdd.setText("Add");
        buttonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddActionPerformed(evt);
            }
        });

        buttonRemove.setText("Remove");
        buttonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveActionPerformed(evt);
            }
        });

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
                    .addComponent(panelSelectTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(labelTables)
                                .addGap(0, 429, Short.MAX_VALUE))
                            .addComponent(scrollPane))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonRemove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOK});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelSelectTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelTables)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonOK)
                            .addComponent(buttonCancel)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemove)
                        .addGap(0, 320, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        returnValue = APPROVE_OPTION;
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void DialogClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_DialogClosing
        setVisible(false);
        dispose();
    }//GEN-LAST:event_DialogClosing

    private void buttonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddActionPerformed
        if (panelSelectTable.getTableDataFileName().trim().equals("") || panelSelectTable.getTableMetaFileName().trim().equals("")) 
        {
            JOptionPane.showMessageDialog(this,"Please specify tabledata file and tablemeta file.");
            return;
        }
        
        if (!TauArgusUtils.ExistFile(panelSelectTable.getTableDataFileName()))
        {
            JOptionPane.showMessageDialog(this,"Tabledata file "+panelSelectTable.getTableDataFileName()+" does not exist.");
            return;
        }
        
        if (!TauArgusUtils.ExistFile(panelSelectTable.getTableMetaFileName()))
        {
            JOptionPane.showMessageDialog(this,"Metadata file "+panelSelectTable.getTableMetaFileName()+" does not exist.");
            return;                
        }
        
        dataFilePairList.add(panelSelectTable.getDataFilePair());
        dataFilePairListTableModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonAddActionPerformed

    private void buttonRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveActionPerformed
        int[] selectedRows = table.getSelectedRows();
        // iterate from higher to lower values (selectedRows is sorted), otherwise row indices change
        for (int i=selectedRows.length-1; i>=0; i--) {
            dataFilePairList.remove(selectedRows[i]);
        }
        dataFilePairListTableModel.fireTableDataChanged();
    }//GEN-LAST:event_buttonRemoveActionPerformed

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
//            logger.log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                DialogOpenTableSet dialog = new DialogOpenTableSet(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton buttonAdd;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonRemove;
    private javax.swing.JLabel labelTables;
    private tauargus.gui.PanelSelectTableFiles panelSelectTable;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables

// Anco 1.6    
//    private List<DataFilePair> dataFilePairList = new ArrayList<>();
    private List<DataFilePair> dataFilePairList = new ArrayList<DataFilePair>();
    private DataFilePairListTableModel dataFilePairListTableModel = new DataFilePairListTableModel(dataFilePairList);
    private int returnValue = CANCEL_OPTION;
    
    private static class DataFilePairListTableModel extends AbstractTableModel {

        DataFilePairListTableModel(List<DataFilePair> dataFilePairList) {
            this.dataFilePairList = dataFilePairList;
        }
                
        @Override
        public int getRowCount() {
            return dataFilePairList.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Data";
            }
            else {
                return "Metadata";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return dataFilePairList.get(rowIndex).getDataFileName();
            }
            else {
                return dataFilePairList.get(rowIndex).getMetaFileName();
            }
        }

        private List<DataFilePair> dataFilePairList;
    }
}
