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

import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import tauargus.model.TableSet;
import tauargus.model.Variable;
import tauargus.utils.SingleListSelectionModel;

public class DialogSelectRowColumn extends javax.swing.JDialog {

    // ***** Dialog Return Values *****
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;

    private static final Logger logger = Logger.getLogger(DialogSelectRowColumn.class.getName());
    
    private int returnValue = CANCEL_OPTION;

    private Variable rowVariable;
    private Variable columnVariable;
    
    public DialogSelectRowColumn(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        listVariables.setSelectionModel(new SingleListSelectionModel());
        listVariables.setCellRenderer(new VariableNameCellRenderer());
        
        setLocationRelativeTo(parent);
    }
    
    public int showDialog(final TableSet table) {
        listVariables.setModel(new AbstractListModel<Variable>() {
            @Override
            public int getSize() {
                return table.expVar.size();
            }

            @Override
            public Variable getElementAt(int index) {
                return table.expVar.get(index);
            }
        });
        listVariables.setSelectedIndex(0);
        
        organise();
        
        setVisible(true);
        
        return returnValue;
    }
    
    public Variable getRowVariable() {
        return rowVariable;
    }

    public Variable getColumnVariable() {
        return columnVariable;
    }
    
    private void organise() {
        textFieldRow.setText(rowVariable == null ? "" : rowVariable.name);
        textFieldColumn.setText(columnVariable == null ? "" : columnVariable.name);
        buttonOk.setEnabled(rowVariable != null && columnVariable != null);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPaneVariables = new javax.swing.JScrollPane();
        listVariables = new javax.swing.JList<Variable>();
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        labelRow = new javax.swing.JLabel();
        labelColumn = new javax.swing.JLabel();
        textFieldColumn = new javax.swing.JTextField();
        textFieldRow = new javax.swing.JTextField();
        buttonToColumn = new javax.swing.JButton();
        buttonFromColumn = new javax.swing.JButton();
        buttonFromRow = new javax.swing.JButton();
        buttonToRow = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select row / column");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                DialogClosing(evt);
            }
        });

        listVariables.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        scrollPaneVariables.setViewportView(listVariables);

        buttonOk.setText("Ok");
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

        labelRow.setText("Row");

        labelColumn.setText("Column");

        textFieldColumn.setEditable(false);

        textFieldRow.setEditable(false);

        buttonToColumn.setText(" >>");
        buttonToColumn.setToolTipText("Add explanatory variables");
        buttonToColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonToColumnActionPerformed(evt);
            }
        });

        buttonFromColumn.setText("<<");
        buttonFromColumn.setToolTipText("Delete explanatory variables");
        buttonFromColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFromColumnActionPerformed(evt);
            }
        });

        buttonFromRow.setText("\u2227");
        buttonFromRow.setToolTipText("Delete from tables");
        buttonFromRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFromRowActionPerformed(evt);
            }
        });

        buttonToRow.setText("\u2228");
        buttonToRow.setToolTipText("Add to tables");
        buttonToRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonToRowActionPerformed(evt);
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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelRow)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(buttonToRow)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonFromRow))
                            .addComponent(textFieldRow, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(scrollPaneVariables, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonFromColumn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonToColumn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelColumn)
                            .addComponent(textFieldColumn, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPaneVariables, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelColumn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldColumn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonToColumn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFromColumn)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonToRow)
                    .addComponent(buttonFromRow))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelRow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textFieldRow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonToColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonToColumnActionPerformed
        Variable selectedVariable = listVariables.getSelectedValue();
        if (selectedVariable != rowVariable) {
            columnVariable = selectedVariable;
        }
        organise();
    }//GEN-LAST:event_buttonToColumnActionPerformed

    private void buttonFromColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFromColumnActionPerformed
        columnVariable = null;
        organise();
    }//GEN-LAST:event_buttonFromColumnActionPerformed

    private void buttonFromRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFromRowActionPerformed
        rowVariable = null;
        organise();
    }//GEN-LAST:event_buttonFromRowActionPerformed

    private void buttonToRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonToRowActionPerformed
        Variable selectedVariable = listVariables.getSelectedValue();
        if (selectedVariable != columnVariable) {
            rowVariable = selectedVariable;
        }
        organise();
    }//GEN-LAST:event_buttonToRowActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        returnValue = APPROVE_OPTION;
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void DialogClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_DialogClosing
        setVisible(false);
        dispose();
    }//GEN-LAST:event_DialogClosing

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
//            java.util.logging.Logger.getLogger(DialogSelectRowColumn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(DialogSelectRowColumn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(DialogSelectRowColumn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(DialogSelectRowColumn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                DialogSelectRowColumn dialog = new DialogSelectRowColumn(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton buttonFromColumn;
    private javax.swing.JButton buttonFromRow;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonToColumn;
    private javax.swing.JButton buttonToRow;
    private javax.swing.JLabel labelColumn;
    private javax.swing.JLabel labelRow;
    private javax.swing.JList<Variable> listVariables;
    private javax.swing.JScrollPane scrollPaneVariables;
    private javax.swing.JTextField textFieldColumn;
    private javax.swing.JTextField textFieldRow;
    // End of variables declaration//GEN-END:variables
}
