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

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 *
 * @author pwof
 */
public class DialogProgress extends DialogBase implements PropertyChangeListener{
    
    public static final int SINGLE = 0;
    public static final int DOUBLE = 1;
    public static final int VALUES = 2;
    
    /**
     * Creates new form MyDialogProgress
     * @param parent
     * @param modal
     */
    public DialogProgress(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }
    
    public void showDialog(int version) {
        for (Component comp : getContentPane().getComponents()) {
            if ((comp instanceof JPanel)) {
                Container cont = (Container) comp;
                for (Component comp2 : cont.getComponents()) {
                    comp2.setVisible(false);
                }
            } else {
                comp.setVisible(false);
            }
        }
        switch (version) {
            case SINGLE:
                labelActivityMain.setVisible(true);
                progressBarMain.setVisible(true);
                break;
            case DOUBLE:
                labelActivityMain.setVisible(true);
                progressBarMain.setVisible(true);
                labelActivityDetail.setVisible(true);
                progressBarDetail.setVisible(true);
                break;
            case VALUES:
                label1.setVisible(true);
                value1.setVisible(true);
                label2.setVisible(true);
                value2.setVisible(true);
                label3.setVisible(true);
                value3.setVisible(true);
                label4.setVisible(true);
                value4.setVisible(true);
                jLabel7.setVisible(true);
        }
        pack();
        setLocationRelativeTo(this.getParent());
        setVisible(true);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "progressMain":
                progressBarMain.setValue(((Integer) evt.getNewValue()));
                break;
            case "activityMain":
                labelActivityMain.setText((String) evt.getNewValue());
                break;
            case "progressDetail":
                progressBarDetail.setValue(((Integer) evt.getNewValue()));
                break;
            case "activityDetail":
                labelActivityDetail.setText((String) evt.getNewValue());
                break;
            case "label1":
                label1.setText((String) evt.getNewValue());
                break;
            case "label2":
                label2.setText((String) evt.getNewValue());
                break;
            case "label3":
                label3.setText((String) evt.getNewValue());
                break;
            case "label4":
                label4.setText((String) evt.getNewValue());
                break;
            case "value1":
                value1.setText(Integer.toString(((Integer) evt.getNewValue())));
                break;
            case "value2":
                value2.setText(Integer.toString(((Integer) evt.getNewValue())));
                break;
            case "value3":
                value3.setText(String.format("%9.5f", ((Double) evt.getNewValue())));
                jLabel7.setText("%");
                break;
            case "value4":
                value4.setText(CalculateHHMMSS(((Integer) evt.getNewValue())));
        }
    }

    public void setIndeterminate(boolean newValue){
        progressBarMain.setIndeterminate(newValue);
    }
    
    private static String CalculateHHMMSS(int seconds){
        int hours = seconds/3600;
        int remainder = seconds%3600;
        int minutes = remainder/60;
        int sec = remainder%60;
        return ((hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":" + (sec < 10 ? "0" : "") + sec);
    }
            
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progressBarMain = new javax.swing.JProgressBar();
        labelActivityMain = new javax.swing.JLabel();
        labelActivityDetail = new javax.swing.JLabel();
        progressBarDetail = new javax.swing.JProgressBar();
        jPanel1 = new javax.swing.JPanel();
        label1 = new javax.swing.JLabel();
        label2 = new javax.swing.JLabel();
        label3 = new javax.swing.JLabel();
        label4 = new javax.swing.JLabel();
        value1 = new javax.swing.JLabel();
        value2 = new javax.swing.JLabel();
        value3 = new javax.swing.JLabel();
        value4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        progressBarMain.setPreferredSize(new java.awt.Dimension(256, 14));

        labelActivityMain.setText("labelActivityMain");

        labelActivityDetail.setText("labelActivityDetail");

        label1.setText("Upper Bound:");

        label2.setText("Lower Bound:");

        label3.setText("Discrepancy:");
        label3.setToolTipText("");

        label4.setText("Time used:");

        value1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        value1.setMaximumSize(new java.awt.Dimension(0, 14));
        value1.setMinimumSize(new java.awt.Dimension(0, 14));
        value1.setPreferredSize(new java.awt.Dimension(0, 14));

        value2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        value2.setMaximumSize(new java.awt.Dimension(0, 14));
        value2.setMinimumSize(new java.awt.Dimension(0, 14));
        value2.setPreferredSize(new java.awt.Dimension(0, 14));

        value3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        value3.setMaximumSize(new java.awt.Dimension(0, 14));
        value3.setMinimumSize(new java.awt.Dimension(0, 14));
        value3.setOpaque(true);
        value3.setPreferredSize(new java.awt.Dimension(0, 14));

        value4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        value4.setMaximumSize(new java.awt.Dimension(0, 14));
        value4.setMinimumSize(new java.awt.Dimension(0, 14));
        value4.setPreferredSize(new java.awt.Dimension(0, 14));

        jLabel7.setText("   ");
        jLabel7.setToolTipText("");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(label1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(value1, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(label2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(value2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(label3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(label4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(value3, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                    .addComponent(value4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0)
                .addComponent(jLabel7)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(value3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(label1)
                        .addComponent(label3)
                        .addComponent(value1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(label2)
                    .addComponent(label4)
                    .addComponent(value2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(value4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(progressBarDetail, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(progressBarMain, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(labelActivityMain)
                    .addComponent(labelActivityDetail))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(labelActivityMain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBarMain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(labelActivityDetail)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBarDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel label1;
    private javax.swing.JLabel label2;
    private javax.swing.JLabel label3;
    private javax.swing.JLabel label4;
    private javax.swing.JLabel labelActivityDetail;
    private javax.swing.JLabel labelActivityMain;
    private javax.swing.JProgressBar progressBarDetail;
    private javax.swing.JProgressBar progressBarMain;
    private javax.swing.JLabel value1;
    private javax.swing.JLabel value2;
    private javax.swing.JLabel value3;
    private javax.swing.JLabel value4;
    // End of variables declaration//GEN-END:variables
}
