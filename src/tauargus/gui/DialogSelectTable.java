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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import tauargus.model.TableSet;
import tauargus.model.Variable;
import tauargus.service.TableService;
import tauargus.utils.SingleListSelectionModel;

/**
 *
 * @author ambargus
 */
public class DialogSelectTable extends DialogBase {

    // ***** Dialog Return Values *****
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;

    private final TableModel tableModel = new AbstractTableModel() {

        @Override
        public int getRowCount() {
            return TableService.numberOfTables();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Explanatory variables";
            } else {
                return "Resp. var";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TableSet table = TableService.getTable(rowIndex);
            if (columnIndex == 0) {
                String s = "";
                for (int i = 0; i < table.expVar.size(); i++) {
                    Variable variable = table.expVar.get(i);
                    if (i != 0) {
                        s += ", ";
                    }
                    s += variable.name;
                    if (variable.recoded) s+= " (R)";
                }
                return s;
            } else {
                return table.respVar.name;
            }
        }
    };
    
    private int returnValue = CANCEL_OPTION;
    
    public DialogSelectTable(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        tableTables.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    returnValue = APPROVE_OPTION;
                    setVisible(false);
                }
            }
        });
    }
    
    public int showDialog() {
        setLocationRelativeTo(this.getParent());
        setVisible(true);
        return returnValue;
    }
    
    public TableSet getSelectedTable() {
        int tableIndex = tableTables.getSelectedRow();
        return TableService.getTable(tableIndex);
    }
    
    public void setSelectedTable(int TableIndex) {
        tableTables.setRowSelectionInterval(TableIndex, TableIndex);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonCancel = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        scrollPaneTables = new javax.swing.JScrollPane();
        tableTables = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select table");
        setModal(true);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                DialogClosing(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOk.setText("Ok");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        tableTables.setModel(tableModel);
        tableTables.setSelectionModel(        new SingleListSelectionModel());
        tableTables.getTableHeader().setReorderingAllowed(false);
        scrollPaneTables.setViewportView(tableTables);
        tableTables.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addComponent(scrollPaneTables, javax.swing.GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPaneTables, javax.swing.GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        returnValue = APPROVE_OPTION;
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void DialogClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_DialogClosing
        setVisible(false);
        dispose();
    }//GEN-LAST:event_DialogClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOk;
    private javax.swing.JScrollPane scrollPaneTables;
    private javax.swing.JTable tableTables;
    // End of variables declaration//GEN-END:variables
}
