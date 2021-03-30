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

//import argus.model.SpssVariable;
import argus.utils.SystemUtils;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.io.FilenameUtils;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.DataFilePair;
import tauargus.utils.TauArgusUtils;

public class DialogOpenMicrodata extends DialogBase{

    private int returnValue = CANCEL_OPTION;
    private static final Logger logger = Logger.getLogger(DialogOpenMicrodata.class.getName());

    // ***** Dialog Return Values *****
    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;

    public DialogOpenMicrodata(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        textInfo.setVisible(false);
    }
    
    public int showDialog() {
        setLocationRelativeTo(this.getParent());
        setVisible(true);
        return returnValue;
    }

    public DataFilePair getMicrodataFilePair() {
        return new DataFilePair(textFieldMicrodata.getText(), textFieldMetadata.getText());
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
        labelMicrodata = new javax.swing.JLabel();
        textFieldMicrodata = new javax.swing.JTextField();
        buttonMicrodata = new javax.swing.JButton();
        labelMetadata = new javax.swing.JLabel();
        textFieldMetadata = new javax.swing.JTextField();
        buttonMetadata = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        textInfo = new javax.swing.JTextArea();

        fileChooser.setDialogTitle("");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Open Microdata");
        setModal(true);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                DialogClosing(evt);
            }
        });

        labelMicrodata.setLabelFor(textFieldMicrodata);
        labelMicrodata.setText("Microdata:");

        buttonMicrodata.setText("...");
        buttonMicrodata.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMicrodataActionPerformed(evt);
            }
        });

        labelMetadata.setLabelFor(textFieldMetadata);
        labelMetadata.setText("Metadata (optional): ");

        buttonMetadata.setText("...");
        buttonMetadata.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMetadataActionPerformed(evt);
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

        textInfo.setColumns(20);
        textInfo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        textInfo.setForeground(new java.awt.Color(0, 0, 255));
        textInfo.setRows(3);
        textInfo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        textInfo.setOpaque(false);
        jScrollPane1.setViewportView(textInfo);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(labelMicrodata, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelMetadata, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(textFieldMetadata, javax.swing.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
                            .addComponent(textFieldMicrodata))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonMicrodata, javax.swing.GroupLayout.PREFERRED_SIZE, 35, Short.MAX_VALUE)
                            .addComponent(buttonMetadata, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)
                        .addGap(2, 2, 2))))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOK});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonMicrodata, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(labelMicrodata)
                        .addComponent(textFieldMicrodata, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelMetadata)
                    .addComponent(textFieldMetadata, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonMetadata))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(27, 27, 27))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonOK)
                            .addComponent(buttonCancel))
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonMicrodataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMicrodataActionPerformed
        TauArgusUtils.getDataDirFromRegistry(fileChooser);
        fileChooser.setDialogTitle("Open Microdata");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        // filters are shown in order of declaration, setFileFilter sets the default filter
        fileChooser.setFileFilter(new FileNameExtensionFilter("Microdata (*.asc, *.dat, *.csv)", "asc", "dat", "csv"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("SPSS system file (*.sav)", "sav"));
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            String hs = fileChooser.getSelectedFile().toString();
            textFieldMicrodata.setText(fileChooser.getSelectedFile().toString());
            TauArgusUtils.putDataDirInRegistry(hs);
            setMetadataFileNameIfPossible();
        }
        setInfo();
    }//GEN-LAST:event_buttonMicrodataActionPerformed

    private void buttonMetadataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMetadataActionPerformed
        TauArgusUtils.getDataDirFromRegistry(fileChooser);
        fileChooser.setDialogTitle("Open Metadata");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Metadata (*.rda)", "rda"));
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            String hs = fileChooser.getSelectedFile().toString();
            textFieldMetadata.setText(fileChooser.getSelectedFile().toString());
             TauArgusUtils.putDataDirInRegistry(hs);
        }
        setInfo();
    }//GEN-LAST:event_buttonMetadataActionPerformed

    private void setInfo(){
        String hs = "";
        if (!textFieldMicrodata.getText().equals("")){  
            if  (textFieldMicrodata.getText().toUpperCase().trim().endsWith(".SAV")){
                hs = "\n\nBut first the metadata from the SPSS file will be retrieved";   
            }
            textInfo.setVisible(true);     
            if (textFieldMetadata.getText().equals("")){
                textInfo.setText("As no metadata file has been specified\n" +
                                "specify the metadata file too or\n" +
                                "specify the metadata via Specify|Metadata"+hs);
            } else{
                textInfo.setText("For changing/inspecting the metadata go to Specify|Metadata\n"+
                                "For specifying the table(s) go to Specify|Tables"+  hs);
            }  
        }
        else{
            textInfo.setVisible(false);
        }  
    }
    
    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        if (textFieldMicrodata.getText().trim().equals("")) 
        {
            JOptionPane.showMessageDialog(this,"Please specify microdata file.");
            return;
        }
        
        if (!TauArgusUtils.ExistFile(textFieldMicrodata.getText()))
        {
            JOptionPane.showMessageDialog(this,"Microdata file "+textFieldMicrodata.getText()+" does not exist.");
            return;
        }
        
        if (!textFieldMetadata.getText().trim().equals(""))
        {
            if (!TauArgusUtils.ExistFile(textFieldMetadata.getText()))
            {
                JOptionPane.showMessageDialog(this,"Metadata file "+textFieldMetadata.getText()+" does not exist.");
                return;                
            }
        }
        // Check for a SPSS systemfile and retrieve the metadata
        String hs = textFieldMicrodata.getText();
        
        if (textFieldMicrodata.getText().toUpperCase().trim().endsWith(".SAV")) {
            try{
                Application.getSpssUtils().getVariablesFromSpss(hs);
            }
            catch (ArgusException e){
                JOptionPane.showMessageDialog(this, e.getMessage()); 
                return;
            }           
        }
        
        SystemUtils.writeLogbook("Microdata file: "+textFieldMicrodata.getText()+" has been opened");            
        if (!textFieldMetadata.getText().trim().equals(""))SystemUtils.writeLogbook("Metadata file: "+textFieldMetadata.getText()+" has been opened");            
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

    /* 
     * In many situations the metadata filename has the same name as the 
     * microdata filename, only with another extension.
     */
    private void setMetadataFileNameIfPossible() {
        String fileName = textFieldMicrodata.getText();
        int extensionIndex = FilenameUtils.indexOfExtension(fileName);
        String baseFileName;
        if (extensionIndex == -1) {
            baseFileName = fileName;
        }
        else {
            baseFileName = fileName.substring(0, extensionIndex);
        }
        String metadataFileName = baseFileName + ".rda";
        File file = new File(metadataFileName);
        if (file.exists() && file.isFile()) {
            textFieldMetadata.setText(metadataFileName);
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
//            // Anco 1.6
////        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
//        } catch (ClassNotFoundException  ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);}
//          catch (InstantiationException ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);}
//          catch (IllegalAccessException  ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);}
//          catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                DialogOpenMicrodata dialog = new DialogOpenMicrodata(new javax.swing.JFrame(), true);
//                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
//                    @Override
//                    public void windowClosing(java.awt.event.WindowEvent e) {
//                        System.exit(0);
//                    }
//                });
//                dialog.showDialog();
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonMetadata;
    private javax.swing.JButton buttonMicrodata;
    private javax.swing.JButton buttonOK;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelMetadata;
    private javax.swing.JLabel labelMicrodata;
    private javax.swing.JTextField textFieldMetadata;
    private javax.swing.JTextField textFieldMicrodata;
    private javax.swing.JTextArea textInfo;
    // End of variables declaration//GEN-END:variables
}
