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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.LinkedTables;
import tauargus.model.TableSet;
import tauargus.model.Variable;
import tauargus.service.TableService;

/**
 * Controls the linked tables procedure, both the modular version as well as the hypercube version.
 * 
 * 
 * @author ahnl
 */
public class DialogLinkedTables extends DialogBase {
    @Override
    protected JRootPane createRootPane() {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                buttonReadyActionPerformed(actionEvent);
            }
        };    
 
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane MyrootPane = new JRootPane();
        MyrootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return MyrootPane;
    }    
    
    TableSet tableSet;
    private JFrame getParentFrame() {
        Container container = this;
        while (!(container instanceof JFrame)) {
            container = container.getParent();
        }
        return (JFrame)container;
    }

    // Creates new form DialogLinkedTables
    public DialogLinkedTables(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        initLists();
    }

    private void initLists(){
        int i, n; Variable variable;
        TableSet tmptableSet;

        n = TableService.numberOfTables();
        labelTable1.setVisible(n>=1);
        labelTable2.setVisible(n>=2);
        labelTable3.setVisible(n>=3);
        labelTable4.setVisible(n>=4);
        labelTable5.setVisible(n>=5);
        labelTable6.setVisible(n>=6);
        labelTable7.setVisible(n>=7);
        listTable1.setVisible(n>=1);
        listTable2.setVisible(n>=2);
        listTable3.setVisible(n>=3);
        listTable4.setVisible(n>=4);
        listTable5.setVisible(n>=5);
        listTable6.setVisible(n>=6);
        listTable7.setVisible(n>=7);

        if (n>=1){tmptableSet=TableService.getTable(0);
        DefaultListModel<String> hulpModel1 = new DefaultListModel<>();
        hulpModel1.clear();          
        for (i=0;i<tmptableSet.expVar.size();i++){
           variable = tmptableSet.expVar.get(i);
           hulpModel1.addElement(variable.name); 
          }
          listTable1.setModel(hulpModel1);}

        if (n>=2){tmptableSet=TableService.getTable(1);
        DefaultListModel<String> hulpModel2 = new DefaultListModel<>();
        hulpModel2.clear();          
        for (i=0;i<tmptableSet.expVar.size();i++){
           variable = tmptableSet.expVar.get(i);
           hulpModel2.addElement(variable.name); 
          }
          listTable2.setModel(hulpModel2);}
        
        if (n>=3){tmptableSet=TableService.getTable(2);
        DefaultListModel<String> hulpModel3 = new DefaultListModel<>();
        hulpModel3.clear();          
        for (i=0;i<tmptableSet.expVar.size();i++){
           variable = tmptableSet.expVar.get(i);
           hulpModel3.addElement(variable.name); 
          }
          listTable3.setModel(hulpModel3);}

        if (n>=4){tmptableSet=TableService.getTable(3);
        DefaultListModel<String> hulpModel4 = new DefaultListModel<>();
        hulpModel4.clear();          
        for (i=0;i<tmptableSet.expVar.size();i++){
           variable = tmptableSet.expVar.get(i);
           hulpModel4.addElement(variable.name); 
          }
          listTable4.setModel(hulpModel4);}

       if (n>=5){tmptableSet=TableService.getTable(4);
        DefaultListModel<String> hulpModel5 = new DefaultListModel<>();
        hulpModel5.clear();          
        for (i=0;i<tmptableSet.expVar.size();i++){
           variable = tmptableSet.expVar.get(i);
           hulpModel5.addElement(variable.name); 
          }
          listTable5.setModel(hulpModel5);}

       if (n>=6){tmptableSet=TableService.getTable(5);
        DefaultListModel<String> hulpModel6 = new DefaultListModel<>();
        hulpModel6.clear();          
        for (i=0;i<tmptableSet.expVar.size();i++){
           variable = tmptableSet.expVar.get(i);
           hulpModel6.addElement(variable.name); 
          }
          listTable6.setModel(hulpModel6);}

        if (n>=7){tmptableSet=TableService.getTable(6);
        DefaultListModel<String> hulpModel7 = new DefaultListModel<>();
        hulpModel7.clear();          
        for (i=0;i<tmptableSet.expVar.size();i++){
           variable = tmptableSet.expVar.get(i);
           hulpModel7.addElement(variable.name); 
          }
          listTable7.setModel(hulpModel7);}

        LinkedTables.buildCoverTable();       
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        listTable1 = new javax.swing.JList<>();
        labelTable1 = new javax.swing.JLabel();
        buttonSuppressHypercube = new javax.swing.JButton();
        buttonSuppressModular = new javax.swing.JButton();
        buttonReady = new javax.swing.JButton();
        labelTable2 = new javax.swing.JLabel();
        labelTable3 = new javax.swing.JLabel();
        labelTable4 = new javax.swing.JLabel();
        labelTable5 = new javax.swing.JLabel();
        labelTable6 = new javax.swing.JLabel();
        labelTable7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        listTable2 = new javax.swing.JList<>();
        jScrollPane3 = new javax.swing.JScrollPane();
        listTable7 = new javax.swing.JList<>();
        jScrollPane4 = new javax.swing.JScrollPane();
        listTable3 = new javax.swing.JList<>();
        jScrollPane5 = new javax.swing.JScrollPane();
        listTable4 = new javax.swing.JList<>();
        jScrollPane6 = new javax.swing.JScrollPane();
        listTable5 = new javax.swing.JList<>();
        jScrollPane7 = new javax.swing.JScrollPane();
        listTable6 = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Linked tables");
        setModal(true);
        setResizable(false);

        listTable1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Var1", "Var2" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        listTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listTable1.setToolTipText("");
        listTable1.setFocusable(false);
        jScrollPane1.setViewportView(listTable1);

        labelTable1.setText("Table 1");

        buttonSuppressHypercube.setText("Suppress via hypercube");
        buttonSuppressHypercube.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSuppressHypercubeActionPerformed(evt);
            }
        });

        buttonSuppressModular.setText("Suppress via modular");
        buttonSuppressModular.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSuppressModularActionPerformed(evt);
            }
        });

        buttonReady.setText("Ready");
        buttonReady.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReadyActionPerformed(evt);
            }
        });

        labelTable2.setText(" Table 2");

        labelTable3.setText("Table 3");

        labelTable4.setText("Table 4");

        labelTable5.setText("Table 5");

        labelTable6.setText("Table 6");

        labelTable7.setText("Table 7");

        listTable2.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Var1", "Var2" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(listTable2);

        listTable7.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Var1", "Var2" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(listTable7);

        listTable3.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Var1", "Var2" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane4.setViewportView(listTable3);

        listTable4.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Var1", "Var2" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane5.setViewportView(listTable4);

        listTable5.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Var1", "Var2" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane6.setViewportView(listTable5);

        listTable6.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Var1", "Var2" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane7.setViewportView(listTable6);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelTable1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(labelTable3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(labelTable4, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(9, 9, 9)
                                .addComponent(labelTable5, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(labelTable2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelTable6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(labelTable7, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(buttonSuppressModular, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSuppressHypercube, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonReady))
                .addGap(182, 182, 182))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelTable1)
                    .addComponent(labelTable2)
                    .addComponent(labelTable3)
                    .addComponent(labelTable4)
                    .addComponent(labelTable5)
                    .addComponent(labelTable6)
                    .addComponent(labelTable7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(38, 38, 38)
                .addComponent(buttonSuppressModular)
                .addGap(18, 18, 18)
                .addComponent(buttonSuppressHypercube)
                .addGap(18, 18, 18)
                .addComponent(buttonReady)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * 
     * @param evt 
     */
    private void buttonSuppressModularActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSuppressModularActionPerformed
      String hs = "", hs1;  
      Boolean oke;
      JFrame parentFrame = getParentFrame();
      tableSet = TableService.getTable(0);
      DialogModularParameters paramsG = new DialogModularParameters(parentFrame, tableSet, false, true);
 
      if (paramsG.showDialog() == DialogModularParameters.APPROVE_OPTION) {
         try {
            LinkedTables.runLinkedModular(this,tableSet.maxScaleCost,tableSet.GetLowerMarg(),tableSet.GetUpperMarg());
            try{
                BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("Inconsistent.cnt")));
                hs = in.readLine();
                if (hs.trim().equals("0")) {hs = "";}
                else {
                hs = hs + " inconsistencies found in the cover table\n" + 
                    "Please check the file: "+ Application.getTempFile("Inconsistent.txt") + "/n";
                }  
            }
            //catch (FileNotFoundException ex){}
            catch (IOException ex) {}
            for (int i=0;i<TableService.numberOfTables();i++){
                tableSet = TableService.getTable(i);  
                hs = hs + tableSet.CountSecondaries()+ " suppressions in table "+(i+1) + "\n";
            }  
            JOptionPane.showMessageDialog(this, "Modular has finished the linked tables protection\n"+  hs + 
                      "Processing time: "+ StrUtils.timeToString(tableSet.processingTime));  
         }     
         catch (ArgusException ex){ 
            hs = ex.getMessage();
            try{
                BufferedReader in = new BufferedReader(new FileReader(Application.getTempFile("Inconsistent.cnt")));
                hs1 = in.readLine();
                oke = hs1.trim().equals("0");
                if (!oke){
                    DialogInfo Info = new DialogInfo(getParentFrame(), true);
                    Info.addLabel("Overview of the inconsistency errors");
                    try{
                        Info.addTextFile(Application.getTempFile("inconsistent.txt"));
                    }
                    catch (ArgusException ex1){}
                    Info.setVisible(true);  
                    hs = hs + "\nSolve the inconsistencies first";
                }
            }
            //catch (FileNotFoundException ex1){}
            catch (IOException ex1) {}
            JOptionPane.showMessageDialog(this, hs);
         }
      }             
    }//GEN-LAST:event_buttonSuppressModularActionPerformed

    private void buttonReadyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReadyActionPerformed
        // TODO add your handling code here:
      setVisible(false);
      listTable1.setLayout(null);
      listTable1.setSize(new Dimension(300,300));
      dispose();
    }//GEN-LAST:event_buttonReadyActionPerformed
/**
 * The hypercube has build in facilitities for linked tables.
 * So all the tables are saved and prepared for the hypercube
 * in the standard way.
 * Then the additional files must be prepared
 * @param evt 
 */
    private void buttonSuppressHypercubeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSuppressHypercubeActionPerformed
    String hs;
    JFrame parentFrame = getParentFrame();
    tableSet = TableService.getTable(0);
    hs = "";
    DialogHypercubeParameters paramsG = new DialogHypercubeParameters(parentFrame, true);
        if (paramsG.showDialog(tableSet) == DialogHypercubeParameters.APPROVE_OPTION) {
            try {
                LinkedTables.runLinkedGHMiter();
                for (int i=0;i<TableService.numberOfTables();i++){
                    tableSet = TableService.getTable(i);
                    hs = hs + tableSet.CountSecondaries()+ " suppressions in table "+(i+1) + "\n";  
                    if (!TableService.getTable(i).ghMiterMessage.equals("")){
                        hs = hs + TableService.getTable(i).ghMiterMessage +" for table "+ (i+1) + "\n";
                    }  
                }
                JOptionPane.showMessageDialog(this, "The Hypercube has finished the linked tables protection\n"+ hs+
                      "Processing time: "+StrUtils.timeToString(tableSet.processingTime));
                if (argus.utils.TauArgusUtils.ExistFile(Application.getTempFile("frozen.txt"))){
                DialogInfo Info = new DialogInfo(getParentFrame(), true);
                Info.addLabel("Overview of the frozen cells");
                try{
                    Info.addTextFile(Application.getTempFile("frozen.txt"));
                }
                catch (ArgusException ex1){}
                    Info.setVisible(true);
                }
            }
            catch (ArgusException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        } 
        // Ask GhMiterParameters for table 1
    }//GEN-LAST:event_buttonSuppressHypercubeActionPerformed


    
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
//            java.util.logging.Logger.getLogger(DialogLinkedTables.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(DialogLinkedTables.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(DialogLinkedTables.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(DialogLinkedTables.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                DialogLinkedTables dialog = new DialogLinkedTables(new javax.swing.JFrame(), true);
//                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
//                    @Override
//                    public void windowClosing(java.awt.event.WindowEvent e) {
//                        System.exit(0);
//                    }
//                });
//                dialog.setVisible(true);
//
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonReady;
    private javax.swing.JButton buttonSuppressHypercube;
    private javax.swing.JButton buttonSuppressModular;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JLabel labelTable1;
    private javax.swing.JLabel labelTable2;
    private javax.swing.JLabel labelTable3;
    private javax.swing.JLabel labelTable4;
    private javax.swing.JLabel labelTable5;
    private javax.swing.JLabel labelTable6;
    private javax.swing.JLabel labelTable7;
    private javax.swing.JList<String> listTable1;
    private javax.swing.JList<String> listTable2;
    private javax.swing.JList<String> listTable3;
    private javax.swing.JList<String> listTable4;
    private javax.swing.JList<String> listTable5;
    private javax.swing.JList<String> listTable6;
    private javax.swing.JList<String> listTable7;
    // End of variables declaration//GEN-END:variables
}
