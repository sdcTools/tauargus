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
import argus.utils.SystemUtils;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import static javax.swing.JViewport.SIMPLE_SCROLL_MODE;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import tauargus.model.Application;
import tauargus.model.ArgusException;
import tauargus.model.DataFilePair;
import tauargus.model.LinkedTables;
import tauargus.model.Metadata;
import tauargus.model.SpssUtilsTau;
import tauargus.model.TableSet;
import tauargus.model.batch;
import tauargus.service.TableService;
import tauargus.utils.TauArgusUtils;

public class FrameMain extends javax.swing.JFrame {
    
    private static final Logger LOGGER = Logger.getLogger(FrameMain.class.getName());
    
    private TableSet currentTable = null;
      
    private final Action openMicrodataAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            boolean MetadataError = false;
            DialogOpenMicrodata dialog = new DialogOpenMicrodata(FrameMain.this, true);
            if (dialog.showDialog() == DialogOpenMicrodata.APPROVE_OPTION) {
                DataFilePair dataFilePair = dialog.getMicrodataFilePair();

                panelTable.setVisible(false);
                TableService.clearTables();
                Application.clearMetadatas();

                Metadata metadata = new Metadata(false);
                metadata.dataFile = dataFilePair.getDataFileName();
                metadata.metaFile = dataFilePair.getMetaFileName(); 
                if (metadata.dataFile.toUpperCase().trim().endsWith(".SAV")){
                    metadata.dataFileType = Metadata.DATA_FILE_TYPE_SPSS;                    
                }
                
                //If the datafile is a SAV file, then we assume the file is a SPSS systemfile
                //and so we will first read the SPSS metadata
              
                if (!metadata.metaFile.trim().equals(""))
                {
                    try {
                        metadata.readMicroMetadata();
                    }
                    catch (ArgusException  ex) {
                        if (!ex.getMessage().isEmpty()){
                            JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());
                            MetadataError = true;
                        }
                    }
                    catch ( FileNotFoundException ex) {
                        JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());  
                    } 
 // If SPSS then check the validity of the metadata  
                    if (metadata.dataFileType == Metadata.DATA_FILE_TYPE_SPSS){
                        try{
                            SpssUtilsTau.checkSpssMeta (metadata); 
                        }
                        catch (ArgusException ex){
                            JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage()+ 
                                                                  "\nPlease correct first"); 
                            Application.clearMetadatas();
                            metadata = null;
                        }
                    }    
                }
                
                Application.addMetadata(metadata);
                if (MetadataError){ // then show metadata window
                    for (ActionListener a: menuItemSpecifyMetadata.getActionListeners()){
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) { });
                    }
                }
                organise();
            }
        }
    };

    private final Action openTableAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            DialogOpenTable dialog = new DialogOpenTable(FrameMain.this, true);
            if (dialog.showDialog() == DialogOpenTable.APPROVE_OPTION) {
                DataFilePair dataFilePair = dialog.getTableDataFilePair();

                panelTable.setVisible(false);
                TableService.clearTables();
                Application.clearMetadatas();

                Metadata metadata = new Metadata(true);
                metadata.metaFile = dataFilePair.getMetaFileName();
                metadata.dataFile = dataFilePair.getDataFileName();
                if (!metadata.metaFile.trim().equals(""))
                {
                    try {
                        metadata.readTableMetadata();
// Anco 1.6                        
//                    } catch (ArgusException | FileNotFoundException ex) {
                    } catch (ArgusException ex) {
                        JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());}
                      catch (FileNotFoundException ex) {
                        JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());  
                    }
                }
                Application.addMetadata(metadata);                
                organise();
            }
        }
    };

    private final Action specifyMetadataAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Metadata metadata = Application.getMetadata(0);
            DialogSpecifyMetadata dialog = new DialogSpecifyMetadata(FrameMain.this, true);
            if (dialog.showDialog(metadata) == DialogSpecifyMetadata.APPROVE_OPTION) {
                organise();
            }
        }
    };

    private final Action specifyTablesAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Metadata metadata = Application.getMetadata(0);
            if (metadata.dataOrigin == Metadata.DATA_ORIGIN_MICRO) {
                DialogSpecifyTablesMicro dialog = new DialogSpecifyTablesMicro(FrameMain.this, true);
                if (dialog.showDialog(metadata) == DialogSpecifyTablesMicro.APPROVE_OPTION) {
                    currentTable = TableService.getTable(0);
                    panelTable.setTable(currentTable);
                }
            } else if (metadata.dataOrigin == Metadata.DATA_ORIGIN_TABULAR) {
                DialogSpecifyTablesTabular dialog = new DialogSpecifyTablesTabular(FrameMain.this, true);
                if (dialog.showDialog(metadata) == DialogSpecifyTablesTabular.APPROVE_OPTION) {
                    currentTable = TableService.getTable(0);
                    panelTable.setTable(currentTable);
                }
            }
            organise();
        }
    };

    private final Action selectTableAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            DialogSelectTable dialog = new DialogSelectTable(FrameMain.this, true);
            if (dialog.showDialog() == DialogSelectTable.APPROVE_OPTION) {
                currentTable = dialog.getSelectedTable();
                panelTable.setTable(currentTable);
                organise();
            }
        }
    };

    private final Action LinkedTablesAction = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent actionEvent){
        try {
            if (LinkedTables.TestLinkedPossible()){
                DialogLinkedTables dialog = new DialogLinkedTables(FrameMain.this, true);
                dialog.setLocationRelativeTo(FrameMain.this);
                dialog.setVisible(true);
        
                panelTable.setTable(currentTable);
                organise();
            }
        }
        catch (ArgusException ex){
                 JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());}
        // TODO add your handling code here:
        }
    };
    
    private final Action GenerateAprioriAction = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            DialogAPriori dialog = new DialogAPriori(FrameMain.this, true);
            dialog.setLocationRelativeTo(FrameMain.this);
            dialog.setVisible(true);
        }        
    };
    
    private final Action saveTableAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            DialogSaveTable dialog = new DialogSaveTable(FrameMain.this, true);
            dialog.showDialog(currentTable);
            if (!currentTable.safeFileName.equals("") ){
              int result = JOptionPane.showConfirmDialog(FrameMain.this, "Do you want to see the report?",
                    "", JOptionPane.YES_NO_OPTION);
              if (JOptionPane.YES_OPTION == result) {
                     showReport();
               } 
            }
        }
    };

    private final Action viewReportAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            showReport();            
        }
    };
    
    private final Action OptionsAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            DialogOptions dialog = new DialogOptions(FrameMain.this, true);
            dialog.setLocationRelativeTo(FrameMain.this);
            dialog.setVisible(true);
            if (TableService.numberOfTables() != 0) {panelTable.enableHiddenFeatures(Application.isAnco());}
        }
    };
        
    private void showReport(){

                DialogHtmlViewer dialog = new DialogHtmlViewer(FrameMain.this, true);
                String fileName = StrUtils.replaceExtension(currentTable.safeFileName, ".html");
                if (currentTable.safeFileName.equals("")){
                  JOptionPane.showMessageDialog(FrameMain.this, "The report file is not (yet) available ");  
                  return;
                }
                File file = new File(fileName);
                if (file.exists()){
                  dialog.showDialog("View Report", "file:////"+ fileName);
                } else {
                  JOptionPane.showMessageDialog(FrameMain.this, "The report file is not (yet) available ");
                }
        
    }
    private final JFileChooser fileChooser;

    /**
     * NOTE Fro some silly reason the panelTable is not initialised automatically
     * So I copied it by hand from previous version
     * This concens the two statemensta after initComponents(); 
     * Also the last statement (the declaration of panelTable) disappeared so I added it manually
     * Creates new form FrameMain
     */
    public FrameMain() {
        initComponents();
        panelTable = new tauargus.gui.PanelTable();
        jScrollPane1.setViewportView(panelTable);
        jScrollPane1.getViewport().setScrollMode(SIMPLE_SCROLL_MODE);
        fileChooser = new javax.swing.JFileChooser();
        
        if (TableService.numberOfTables() > 0) {
            currentTable = TableService.getTable(0);
            panelTable.setTable(currentTable);
            panelTable.updateSuppressButtons();
        }

        organise();
    }
    
    private void organise() {
        specifyMetadataAction.setEnabled(Application.numberOfMetadatas() > 0);
        specifyTablesAction.setEnabled((Application.numberOfMetadatas() > 0) && (Application.getMetadatas().get(0).containsExplanatoryVariable()));

        selectTableAction.setEnabled(TableService.numberOfTables() > 1);     
        menuItemLinkedTables.setEnabled(TableService.numberOfTables() > 1);
        LinkedTablesAction.setEnabled(TableService.numberOfTables() > 1);
        
        saveTableAction.setEnabled(TableService.numberOfTables() != 0);
        viewReportAction.setEnabled(TableService.numberOfTables() != 0);      
        menuItemWriteBatchFile.setEnabled(Application.numberOfMetadatas() > 0);  

        menuItemProtectJJFormat.setVisible(Application.isAnco());
        menuItemAncoNews.setVisible(Application.isAnco());
        menuItemSolverOptions.setVisible(Application.isAnco());

        panelTable.setVisible(TableService.numberOfTables() != 0);
        if (TableService.numberOfTables() != 0) {panelTable.enableHiddenFeatures(Application.isAnco());}
        
        if (Application.numberOfMetadatas() > 0) {
            String titlefile;
            if (TableService.numberOfTables() > 0){
                titlefile = TableService.getTable(panelTable.getTableNumber()).metadata.dataFile;
            }
            else{
                titlefile = Application.getMetadata(0).dataFile;
            }
            this.setTitle("TauArgus \t" + titlefile);
        }
    }
    
    public void organiseAnco(){
        organise();
    }

    @Override
    public List<Image> getIconImages() {
        URL url = FrameMain.class.getResource("/tauargus/resources/Tau32.png");
        Image image = Toolkit.getDefaultToolkit().getImage(url);
        ArrayList<Image> imageList = new ArrayList<>();
        imageList.add(image);
        return imageList;
    }

/**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        toolBar = new javax.swing.JToolBar();
        buttonOpenMicrodata = new javax.swing.JButton();
        buttonOpenTable = new javax.swing.JButton();
        buttonOpenTableSet = new javax.swing.JButton();
        buttonOpenBatchProcess = new javax.swing.JButton();
        separator4 = new javax.swing.JToolBar.Separator();
        buttonSpecifyMetadata = new javax.swing.JButton();
        buttonSpecifyTables = new javax.swing.JButton();
        separator5 = new javax.swing.JToolBar.Separator();
        buttonSelectTable = new javax.swing.JButton();
        buttonLinkedTables = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        buttonSaveTable = new javax.swing.JButton();
        buttonViewReport = new javax.swing.JButton();
        buttonGenerateApriori = new javax.swing.JButton();
        separator7 = new javax.swing.JToolBar.Separator();
        buttonHelp = new javax.swing.JButton();
        buttonOptions = new javax.swing.JButton();
        buttonAbout = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jToolBar1 = new javax.swing.JToolBar();
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuItemOpenMicrodata = new javax.swing.JMenuItem();
        menuItemOpenTable = new javax.swing.JMenuItem();
        menuItemOpenTableSet = new javax.swing.JMenuItem();
        separator1 = new javax.swing.JPopupMenu.Separator();
        menuItemOpenBatchProcess = new javax.swing.JMenuItem();
        separator2 = new javax.swing.JPopupMenu.Separator();
        menuItemExit = new javax.swing.JMenuItem();
        menuSpecify = new javax.swing.JMenu();
        menuItemSpecifyMetadata = new javax.swing.JMenuItem();
        menuItemSpecifyTables = new javax.swing.JMenuItem();
        menuModify = new javax.swing.JMenu();
        menuItemSelectTable = new javax.swing.JMenuItem();
        menuItemLinkedTables = new javax.swing.JMenuItem();
        menuItemProtectJJFormat = new javax.swing.JMenuItem();
        menuOutput = new javax.swing.JMenu();
        menuItemSaveTable = new javax.swing.JMenuItem();
        menuItemViewReport = new javax.swing.JMenuItem();
        menuItemGenerateApriory = new javax.swing.JMenuItem();
        menuItemWriteBatchFile = new javax.swing.JMenuItem();
        menuHelp = new javax.swing.JMenu();
        menuItemContent = new javax.swing.JMenuItem();
        menuItemNews = new javax.swing.JMenuItem();
        menuItemAncoNews = new javax.swing.JMenuItem();
        separator3 = new javax.swing.JPopupMenu.Separator();
        menuItemOptions = new javax.swing.JMenuItem();
        menuItemSolverOptions = new javax.swing.JMenuItem();
        menuItemAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("TauArgus");
        setExtendedState(getExtendedState() | javax.swing.JFrame.MAXIMIZED_BOTH);

        toolBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        toolBar.setRollover(true);
        toolBar.setBorderPainted(false);

        buttonOpenMicrodata.setAction(openMicrodataAction);
        buttonOpenMicrodata.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenMicrodata.png"))); // NOI18N
        buttonOpenMicrodata.setToolTipText("Open Microdata...");
        buttonOpenMicrodata.setBorderPainted(false);
        buttonOpenMicrodata.setFocusable(false);
        buttonOpenMicrodata.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpenMicrodata.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonOpenMicrodata);

        buttonOpenTable.setAction(openTableAction);
        buttonOpenTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenTable.png"))); // NOI18N
        buttonOpenTable.setToolTipText("Open Table...");
        buttonOpenTable.setBorderPainted(false);
        buttonOpenTable.setFocusable(false);
        buttonOpenTable.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpenTable.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonOpenTable);

        buttonOpenTableSet.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenTableSet.png"))); // NOI18N
        buttonOpenTableSet.setToolTipText("Open TableSet...");
        buttonOpenTableSet.setBorderPainted(false);
        buttonOpenTableSet.setFocusable(false);
        buttonOpenTableSet.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpenTableSet.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonOpenTableSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemOpenTableSetActionPerformed(evt);
            }
        });
        toolBar.add(buttonOpenTableSet);

        buttonOpenBatchProcess.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenBatch.png"))); // NOI18N
        buttonOpenBatchProcess.setToolTipText("Open Batch Process...");
        buttonOpenBatchProcess.setBorderPainted(false);
        buttonOpenBatchProcess.setFocusable(false);
        buttonOpenBatchProcess.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpenBatchProcess.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonOpenBatchProcess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemOpenBatchProcessActionPerformed(evt);
            }
        });
        toolBar.add(buttonOpenBatchProcess);
        toolBar.add(separator4);

        buttonSpecifyMetadata.setAction(specifyMetadataAction);
        buttonSpecifyMetadata.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SpecifyMetadata.png"))); // NOI18N
        buttonSpecifyMetadata.setToolTipText("Specify Metadata");
        buttonSpecifyMetadata.setBorderPainted(false);
        buttonSpecifyMetadata.setFocusable(false);
        buttonSpecifyMetadata.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSpecifyMetadata.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonSpecifyMetadata);

        buttonSpecifyTables.setAction(specifyTablesAction);
        buttonSpecifyTables.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SpecifyTables.png"))); // NOI18N
        buttonSpecifyTables.setToolTipText("Specify Tables");
        buttonSpecifyTables.setBorderPainted(false);
        buttonSpecifyTables.setFocusable(false);
        buttonSpecifyTables.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSpecifyTables.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonSpecifyTables);
        toolBar.add(separator5);

        buttonSelectTable.setAction(selectTableAction);
        buttonSelectTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SelectTable.png"))); // NOI18N
        buttonSelectTable.setToolTipText("Select Table");
        buttonSelectTable.setBorderPainted(false);
        buttonSelectTable.setFocusable(false);
        buttonSelectTable.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSelectTable.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonSelectTable);

        buttonLinkedTables.setAction(LinkedTablesAction);
        buttonLinkedTables.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/linked.png"))); // NOI18N
        buttonLinkedTables.setToolTipText("Linked Tables");
        buttonLinkedTables.setBorderPainted(false);
        buttonLinkedTables.setFocusable(false);
        buttonLinkedTables.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonLinkedTables.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonLinkedTables);
        toolBar.add(jSeparator1);

        buttonSaveTable.setAction(saveTableAction);
        buttonSaveTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SaveTable.png"))); // NOI18N
        buttonSaveTable.setToolTipText("Save Table");
        buttonSaveTable.setBorderPainted(false);
        buttonSaveTable.setFocusable(false);
        buttonSaveTable.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSaveTable.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonSaveTable);

        buttonViewReport.setAction(viewReportAction);
        buttonViewReport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/ViewReport.png"))); // NOI18N
        buttonViewReport.setToolTipText("View Report");
        buttonViewReport.setBorderPainted(false);
        buttonViewReport.setFocusable(false);
        buttonViewReport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonViewReport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonViewReport);

        buttonGenerateApriori.setAction(GenerateAprioriAction);
        buttonGenerateApriori.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/apriori.png"))); // NOI18N
        buttonGenerateApriori.setToolTipText("Generate Apriori");
        buttonGenerateApriori.setBorderPainted(false);
        buttonGenerateApriori.setFocusable(false);
        buttonGenerateApriori.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonGenerateApriori.setName(""); // NOI18N
        buttonGenerateApriori.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonGenerateApriori);
        toolBar.add(separator7);

        buttonHelp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/Help.png"))); // NOI18N
        buttonHelp.setToolTipText("Help");
        buttonHelp.setBorderPainted(false);
        buttonHelp.setFocusable(false);
        buttonHelp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonHelp.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemContentActionPerformed(evt);
            }
        });
        toolBar.add(buttonHelp);

        buttonOptions.setAction(OptionsAction);
        buttonOptions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/options.png"))); // NOI18N
        buttonOptions.setToolTipText("Options");
        buttonOptions.setBorderPainted(false);
        buttonOptions.setFocusable(false);
        buttonOptions.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOptions.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(buttonOptions);

        buttonAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/about.png"))); // NOI18N
        buttonAbout.setToolTipText("About");
        buttonAbout.setBorderPainted(false);
        buttonAbout.setFocusable(false);
        buttonAbout.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAbout.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAboutActionPerformed(evt);
            }
        });
        toolBar.add(buttonAbout);

        jScrollPane1.setMaximumSize(new java.awt.Dimension(0, 0));

        jToolBar1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.setRollover(true);
        jScrollPane1.setViewportView(jToolBar1);

        menuFile.setMnemonic('F');
        menuFile.setText("File");

        menuItemOpenMicrodata.setAction(openMicrodataAction);
        menuItemOpenMicrodata.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuItemOpenMicrodata.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenMicrodata.png"))); // NOI18N
        menuItemOpenMicrodata.setMnemonic('M');
        menuItemOpenMicrodata.setText("Open Microdata...");
        menuFile.add(menuItemOpenMicrodata);

        menuItemOpenTable.setAction(openTableAction);
        menuItemOpenTable.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuItemOpenTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenTable.png"))); // NOI18N
        menuItemOpenTable.setMnemonic('T');
        menuItemOpenTable.setText("Open Table...");
        menuFile.add(menuItemOpenTable);

        menuItemOpenTableSet.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenTableSet.png"))); // NOI18N
        menuItemOpenTableSet.setMnemonic('S');
        menuItemOpenTableSet.setText("Open Table Set...");
        menuItemOpenTableSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemOpenTableSetActionPerformed(evt);
            }
        });
        menuFile.add(menuItemOpenTableSet);
        menuFile.add(separator1);

        menuItemOpenBatchProcess.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/OpenBatch.png"))); // NOI18N
        menuItemOpenBatchProcess.setMnemonic('B');
        menuItemOpenBatchProcess.setText("Open Batch Process...");
        menuItemOpenBatchProcess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemOpenBatchProcessActionPerformed(evt);
            }
        });
        menuFile.add(menuItemOpenBatchProcess);
        menuFile.add(separator2);

        menuItemExit.setMnemonic('x');
        menuItemExit.setText("Exit");
        menuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemExitActionPerformed(evt);
            }
        });
        menuFile.add(menuItemExit);

        menuBar.add(menuFile);

        menuSpecify.setMnemonic('S');
        menuSpecify.setText("Specify");

        menuItemSpecifyMetadata.setAction(specifyMetadataAction);
        menuItemSpecifyMetadata.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuItemSpecifyMetadata.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SpecifyMetadata.png"))); // NOI18N
        menuItemSpecifyMetadata.setMnemonic('M');
        menuItemSpecifyMetadata.setText("Metadata...");
        menuSpecify.add(menuItemSpecifyMetadata);

        menuItemSpecifyTables.setAction(specifyTablesAction);
        menuItemSpecifyTables.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SpecifyTables.png"))); // NOI18N
        menuItemSpecifyTables.setMnemonic('T');
        menuItemSpecifyTables.setText("Tables...");
        menuSpecify.add(menuItemSpecifyTables);

        menuBar.add(menuSpecify);

        menuModify.setMnemonic('M');
        menuModify.setText("Modify");

        menuItemSelectTable.setAction(selectTableAction);
        menuItemSelectTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SelectTable.png"))); // NOI18N
        menuItemSelectTable.setMnemonic('S');
        menuItemSelectTable.setText("Select Table...");
        menuModify.add(menuItemSelectTable);

        menuItemLinkedTables.setAction(LinkedTablesAction);
        menuItemLinkedTables.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/linked.png"))); // NOI18N
        menuItemLinkedTables.setMnemonic('L');
        menuItemLinkedTables.setText("Linked Tables...");
        menuItemLinkedTables.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemLinkedTablesActionPerformed(evt);
            }
        });
        menuModify.add(menuItemLinkedTables);

        menuItemProtectJJFormat.setMnemonic('P');
        menuItemProtectJJFormat.setText("ProtectJJFormat...");
        menuItemProtectJJFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemProtectJJFormatActionPerformed(evt);
            }
        });
        menuModify.add(menuItemProtectJJFormat);

        menuBar.add(menuModify);

        menuOutput.setMnemonic('O');
        menuOutput.setText("Output");

        menuItemSaveTable.setAction(saveTableAction);
        menuItemSaveTable.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/SaveTable.png"))); // NOI18N
        menuItemSaveTable.setMnemonic('S');
        menuItemSaveTable.setText("Save Table...");
        menuItemSaveTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemSaveTableActionPerformed(evt);
            }
        });
        menuOutput.add(menuItemSaveTable);

        menuItemViewReport.setAction(viewReportAction);
        menuItemViewReport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/ViewReport.png"))); // NOI18N
        menuItemViewReport.setMnemonic('V');
        menuItemViewReport.setText("View Report");
        menuItemViewReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemViewReportActionPerformed(evt);
            }
        });
        menuOutput.add(menuItemViewReport);

        menuItemGenerateApriory.setAction(GenerateAprioriAction);
        menuItemGenerateApriory.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/apriori.png"))); // NOI18N
        menuItemGenerateApriory.setMnemonic('G');
        menuItemGenerateApriory.setText("Generate Apriory...");
        menuItemGenerateApriory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemGenerateAprioryActionPerformed(evt);
            }
        });
        menuOutput.add(menuItemGenerateApriory);

        menuItemWriteBatchFile.setMnemonic('W');
        menuItemWriteBatchFile.setText("Write Batch File...");
        menuItemWriteBatchFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemWriteBatchFileActionPerformed(evt);
            }
        });
        menuOutput.add(menuItemWriteBatchFile);

        menuBar.add(menuOutput);

        menuHelp.setMnemonic('H');
        menuHelp.setText("Help");

        menuItemContent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/Help.png"))); // NOI18N
        menuItemContent.setMnemonic('C');
        menuItemContent.setText("Content");
        menuItemContent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemContentActionPerformed(evt);
            }
        });
        menuHelp.add(menuItemContent);

        menuItemNews.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/news.png"))); // NOI18N
        menuItemNews.setMnemonic('N');
        menuItemNews.setText("News");
        menuItemNews.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemNewsActionPerformed(evt);
            }
        });
        menuHelp.add(menuItemNews);

        menuItemAncoNews.setMnemonic('A');
        menuItemAncoNews.setText("Anco News");
        menuItemAncoNews.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAncoNewsActionPerformed(evt);
            }
        });
        menuHelp.add(menuItemAncoNews);
        menuHelp.add(separator3);

        menuItemOptions.setAction(OptionsAction);
        menuItemOptions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/options.png"))); // NOI18N
        menuItemOptions.setMnemonic('O');
        menuItemOptions.setText("Options...");
        menuItemOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemOptionsActionPerformed(evt);
            }
        });
        menuHelp.add(menuItemOptions);

        menuItemSolverOptions.setMnemonic('S');
        menuItemSolverOptions.setText("Solver Options...");
        menuItemSolverOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemSolverOptionsActionPerformed(evt);
            }
        });
        menuHelp.add(menuItemSolverOptions);

        menuItemAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tauargus/resources/about.png"))); // NOI18N
        menuItemAbout.setMnemonic('A');
        menuItemAbout.setText("About");
        menuItemAbout.setName(""); // NOI18N
        menuItemAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAboutActionPerformed(evt);
            }
        });
        menuHelp.add(menuItemAbout);

        menuBar.add(menuHelp);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 884, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 884, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 669, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void menuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemAboutActionPerformed
        DialogAbout dialog = new DialogAbout(this, true);
        dialog.setVisible(true);
    }//GEN-LAST:event_menuItemAboutActionPerformed

    private void menuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemExitActionPerformed
        setVisible(false); 
        dispose(); 
        SystemUtils.writeLogbook("End of TauArgus run");
        System.exit(0);
    }//GEN-LAST:event_menuItemExitActionPerformed

    private void menuItemOpenTableSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemOpenTableSetActionPerformed
        DialogOpenTableSet dialog = new DialogOpenTableSet(FrameMain.this, true);
        if (dialog.showDialog() == DialogOpenTableSet.APPROVE_OPTION) {
            List<DataFilePair> list = dialog.getDataFilePairList();
            Application.clearMetadatas();
            try {
                for (DataFilePair dataFilePair : list) {
                    Metadata metadata = new Metadata(true);
                    metadata.metaFile = dataFilePair.getMetaFileName();
                    metadata.dataFile = dataFilePair.getDataFileName();
                    metadata.readTableMetadata();
                    Application.addMetadata(metadata);
                }
                specifyTablesAction.actionPerformed(evt);
            }
// anco 1.6            
//          catch (ArgusException | FileNotFoundException ex) {
            catch (ArgusException ex) {
                JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());
                Application.clearMetadatas();
            }                     
            catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(FrameMain.this, ex.getMessage());
                Application.clearMetadatas();
            }                     
            organise();
        }
    }//GEN-LAST:event_menuItemOpenTableSetActionPerformed

    private void menuItemNewsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemNewsActionPerformed
        String hs="";
        DialogHtmlViewer dialog = new DialogHtmlViewer(FrameMain.this, true);
        try{hs = SystemUtils.getApplicationDirectory(FrameMain.class).getCanonicalPath();}
        catch (Exception ex){}
        hs = hs +"/tauNews.html";
        if (TauArgusUtils.ExistFile(hs)){
        dialog.showDialog("News","file:////"+  hs);
        }else{
           JOptionPane.showMessageDialog(FrameMain.this, "The news file could not be displayed; sorry"); 
        }
    }//GEN-LAST:event_menuItemNewsActionPerformed

    private void menuItemAncoNewsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemAncoNewsActionPerformed
        DialogHtmlViewer dialog = new DialogHtmlViewer(FrameMain.this, true);
        JOptionPane.showMessageDialog(FrameMain.this, "The Anco news file tobe implemented"); 
    }//GEN-LAST:event_menuItemAncoNewsActionPerformed

    private void menuItemSaveTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemSaveTableActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuItemSaveTableActionPerformed

    private void menuItemGenerateAprioryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemGenerateAprioryActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuItemGenerateAprioryActionPerformed

    private void menuItemOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemOptionsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuItemOptionsActionPerformed

    private void menuItemLinkedTablesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemLinkedTablesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuItemLinkedTablesActionPerformed

    private void menuItemOpenBatchProcessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemOpenBatchProcessActionPerformed
        TauArgusUtils.getDataDirFromRegistry(fileChooser);
        fileChooser.setDialogTitle("Open Batch file");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        // filters are shown in order of declaration, setFileFilter sets the default filter
        fileChooser.setFileFilter(new FileNameExtensionFilter("Argus batch filea (*.arb)", "arb"));
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            Application.clearEverythingGui();
            String hs =fileChooser.getSelectedFile().toString();
            TauArgusUtils.putDataDirInRegistry(hs);
            Application.setBatch(Application.BATCH_FROMMENU);
            // start a new thread to allow a window to show progress.               
            final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
                @Override
                protected Void doInBackground()throws ArgusException, Exception{
                    String xs =fileChooser.getSelectedFile().toString();  
                    batch.runBatchProcess(xs); 
                    return null;
                }
                @Override
                protected void done(){
                    try{
                        get();
                        if (TableService.numberOfTables() > 0) {
                            currentTable = TableService.getTable(0);
                            panelTable.setTable(currentTable);
                        }
                        organise();    
                    }
                    catch (InterruptedException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    } catch (ExecutionException ex) {
                        JOptionPane.showMessageDialog(null, ex.getCause().getMessage());
                    }
                }
            };
            worker.execute();    
        }           
    }//GEN-LAST:event_menuItemOpenBatchProcessActionPerformed

    private void menuItemSolverOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemSolverOptionsActionPerformed
        DialogSolverOptions dialog = new DialogSolverOptions(FrameMain.this,true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }//GEN-LAST:event_menuItemSolverOptionsActionPerformed

    private void menuItemWriteBatchFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemWriteBatchFileActionPerformed
        DialogWriteBatchFile dialog = new DialogWriteBatchFile(FrameMain.this,true);
        dialog.setLocationRelativeTo(FrameMain.this);
        dialog.setVisible(true);
    }//GEN-LAST:event_menuItemWriteBatchFileActionPerformed

    private void menuItemViewReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemViewReportActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuItemViewReportActionPerformed

    private void menuItemProtectJJFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemProtectJJFormatActionPerformed
        DialogProtectJJFormat dialog = new DialogProtectJJFormat(FrameMain.this,true);
        dialog.setLocationRelativeTo(FrameMain.this);
        dialog.setVisible(true);       
    }//GEN-LAST:event_menuItemProtectJJFormatActionPerformed

    private void menuItemContentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemContentActionPerformed
        try{
            Application.showHelp("General");
            }
        catch (argus.model.ArgusException ex){JOptionPane.showMessageDialog(null, ex.getMessage());}
    }//GEN-LAST:event_menuItemContentActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAbout;
    private javax.swing.JButton buttonGenerateApriori;
    private javax.swing.JButton buttonHelp;
    private javax.swing.JButton buttonLinkedTables;
    private javax.swing.JButton buttonOpenBatchProcess;
    private javax.swing.JButton buttonOpenMicrodata;
    private javax.swing.JButton buttonOpenTable;
    private javax.swing.JButton buttonOpenTableSet;
    private javax.swing.JButton buttonOptions;
    private javax.swing.JButton buttonSaveTable;
    private javax.swing.JButton buttonSelectTable;
    private javax.swing.JButton buttonSpecifyMetadata;
    private javax.swing.JButton buttonSpecifyTables;
    private javax.swing.JButton buttonViewReport;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenu menuHelp;
    private javax.swing.JMenuItem menuItemAbout;
    private javax.swing.JMenuItem menuItemAncoNews;
    private javax.swing.JMenuItem menuItemContent;
    private javax.swing.JMenuItem menuItemExit;
    private javax.swing.JMenuItem menuItemGenerateApriory;
    private javax.swing.JMenuItem menuItemLinkedTables;
    private javax.swing.JMenuItem menuItemNews;
    private javax.swing.JMenuItem menuItemOpenBatchProcess;
    private javax.swing.JMenuItem menuItemOpenMicrodata;
    private javax.swing.JMenuItem menuItemOpenTable;
    private javax.swing.JMenuItem menuItemOpenTableSet;
    private javax.swing.JMenuItem menuItemOptions;
    private javax.swing.JMenuItem menuItemProtectJJFormat;
    private javax.swing.JMenuItem menuItemSaveTable;
    private javax.swing.JMenuItem menuItemSelectTable;
    private javax.swing.JMenuItem menuItemSolverOptions;
    private javax.swing.JMenuItem menuItemSpecifyMetadata;
    private javax.swing.JMenuItem menuItemSpecifyTables;
    private javax.swing.JMenuItem menuItemViewReport;
    private javax.swing.JMenuItem menuItemWriteBatchFile;
    private javax.swing.JMenu menuModify;
    private javax.swing.JMenu menuOutput;
    private javax.swing.JMenu menuSpecify;
    private javax.swing.JPopupMenu.Separator separator1;
    private javax.swing.JPopupMenu.Separator separator2;
    private javax.swing.JPopupMenu.Separator separator3;
    private javax.swing.JToolBar.Separator separator4;
    private javax.swing.JToolBar.Separator separator5;
    private javax.swing.JToolBar.Separator separator7;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables
    private tauargus.gui.PanelTable panelTable;
}