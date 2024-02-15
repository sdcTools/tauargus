/*
* Argus Open Source
* Software to apply Statistical Disclosure Control techniques
* 
* Copyright 2014 Statistics Netherlands
* 
* This program is free software; you can redistribute it and/or 
* modify it under the termcomboBoxOutputDimensions of the European Union Public Licence 
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
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import tauargus.model.APriori;
import tauargus.model.APriori.Mapping;
import tauargus.model.ArgusException;
import tauargus.model.CellStatus;
import tauargus.utils.TableColumnResizer;
import tauargus.utils.TauArgusUtils;


/*!
    brief This dialog allows the user to specify an apriori file, based on a save table (e.g. from a previous year)

    \image html d:\argus\doc\images\screenshot_dialogapriori.jpg

*/
public class DialogAPriori extends DialogBase {
    
    private int maxOutDim = 4;
    
    private APriori apriori = new APriori();
    
    private static class HeaderRenderer implements TableCellRenderer {
        TableCellRenderer renderer;
        int horAlignment;
        public HeaderRenderer(JTable table, int horizontalAlignment) {
            horAlignment = horizontalAlignment;
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            JLabel label = (JLabel)c;
            label.setHorizontalAlignment(horAlignment);
            return label;
        }
    }
    
    private class RadioButtonCellEditorRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer, ActionListener {
        private final JRadioButton radioButton = new JRadioButton();
        {
            radioButton.addActionListener(this);
            radioButton.setOpaque(false);
            radioButton.setHorizontalAlignment(JRadioButton.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            radioButton.setSelected(Boolean.TRUE.equals(value));
            return radioButton;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            radioButton.setSelected(Boolean.TRUE.equals(value));
            return radioButton;
        }

        @Override
        public Object getCellEditorValue() {
            return radioButton.isSelected();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            stopCellEditing();
        }
    }
    
    private static final String[] columnNames = new String[]{ "Status", "Omit", "Safe", "Unsafe", "Protect", "Cost", "Cost value" };
    
    
    /*
     * Creates new form DialogAPriori
     */
    public DialogAPriori(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
 
        table.setModel(new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return CellStatus.size() - 1;
            }

            
            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 5:
                        // JTable uses checkboxes for editing if class is Boolean
                        return Boolean.class;
                    case 6:
                        return Double.class;
                    default:
                        return super.getColumnClass(columnIndex);
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex != 0;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return CellStatus.findByValue(rowIndex + 1).getDescription();
                } else {
                    Mapping m = apriori.Mappings.get(rowIndex);
                    
                            
                    if (columnIndex >=1 && columnIndex <= 4) {
                        return m.newstat.getIndex()== (columnIndex - 1);
                    } else if (columnIndex == 5) {
                        return m.useCost;
                    } else {
                        if( m.useCost)
                            return m.costValue;
                        else
                            return null;                            
                    }
                }
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                if (columnIndex != 0) {
                    Mapping m = apriori.Mappings.get(rowIndex);
                    if (columnIndex >=1 && columnIndex <= 4) {
                        m.newstat =  APriori.ChangeStatus.findByValue(columnIndex - 1);
                        // necessary for updating columns 1 to 4
                        fireTableRowsUpdated(rowIndex, rowIndex);
                    } else if (columnIndex == 5) {
                        m.useCost = (boolean)value;
                        fireTableRowsUpdated(rowIndex, rowIndex);
                    } else {
                        if( value!=null && value!="")
                            m.costValue = (Double)value;
                    }
                }
            }
        });

        for (int columnIndex = 0; columnIndex < 7; columnIndex++) {
            TableColumn column = table.getColumnModel().getColumn(columnIndex);
            if (columnIndex >= 1 && columnIndex <= 4) {
                column.setCellEditor(new RadioButtonCellEditorRenderer());
                column.setCellRenderer(new RadioButtonCellEditorRenderer());
            } 
        }
        
        int[] alignments = new int[] {JLabel.LEFT,JLabel.CENTER,JLabel.CENTER,JLabel.CENTER,JLabel.CENTER,JLabel.CENTER,JLabel.CENTER};
        for (int i = 0 ; i < table.getColumnCount(); i++){
                table.getTableHeader().getColumnModel().getColumn(i).setHeaderRenderer(new HeaderRenderer(table, alignments[i]));
        }
        
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);        
        TableColumnResizer.adjustColumnPreferredWidths(table, false);
        panelTable.add(table.getTableHeader(), java.awt.BorderLayout.PAGE_START);
        //pack();
     
        ChangeOkButtonState();
        apriori.InitColumnMap(4);
        apriori.ReadMappingsFromRegistry();
        ShowDimensionSelection(0,0);
    }

    /**
     * organises the activity of the buttons etc
     */
    private void ChangeOkButtonState()
    {
        boolean enabled = true;
        
        if( apriori == null )
            enabled = false;
        
        if( textFieldAprioryFile.getText().trim().equals(""))
            enabled = false;
        
        List<Integer> selectedvars = new ArrayList<>();
        
        if( comboBoxOutVar1.isVisible() ) selectedvars.add(comboBoxOutVar1.getSelectedIndex());
        if( comboBoxOutVar2.isVisible() ) selectedvars.add(comboBoxOutVar2.getSelectedIndex());
        if( comboBoxOutVar3.isVisible() ) selectedvars.add(comboBoxOutVar3.getSelectedIndex());
        if( comboBoxOutVar4.isVisible() ) selectedvars.add(comboBoxOutVar4.getSelectedIndex());
       
        int SelectedVarCount =0;
        boolean Doubles = false;
        
        for(int i=0; i<selectedvars.size(); i++){
            if(selectedvars.get(i) > 0){
                SelectedVarCount++;
            
                if( i<selectedvars.size()-1 )
                    for(int j=i+1; j<selectedvars.size(); j++)
                        if( selectedvars.get(i).equals(selectedvars.get(j)))
                              Doubles = true;
            }
        }    

        if( selectedvars.size()==1 && selectedvars.get(0)==0 )
            enabled = false;

        if( Doubles)
           JOptionPane.showMessageDialog(null, "All selected variables must be distinct or Other");
       
        if( SelectedVarCount==0 || Doubles)
            enabled = false;
       
        buttonOk.setEnabled(enabled);       
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
        textFieldSafeFile = new javax.swing.JTextField();
        labelSafeFile = new javax.swing.JLabel();
        labelAprioryFile = new javax.swing.JLabel();
        textFieldAprioryFile = new javax.swing.JTextField();
        buttonSafeFile = new javax.swing.JButton();
        buttonAprioryFile = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        labelOutputDimension = new javax.swing.JLabel();
        comboBoxOutputDimension = new javax.swing.JComboBox<>();
        comboBoxOutVar1 = new javax.swing.JComboBox<>();
        labelOutVar1 = new javax.swing.JLabel();
        labelOutVar2 = new javax.swing.JLabel();
        comboBoxOutVar2 = new javax.swing.JComboBox<>();
        labelOutVar3 = new javax.swing.JLabel();
        comboBoxOutVar3 = new javax.swing.JComboBox<>();
        labelOutVar4 = new javax.swing.JLabel();
        comboBoxOutVar4 = new javax.swing.JComboBox<>();
        labelSeparator = new javax.swing.JLabel();
        textFieldSeparator = new javax.swing.JTextField();
        panelTable = new javax.swing.JPanel();
        table = new javax.swing.JTable();
        labelMapping = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Make apriory file");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                dialogClosing(evt);
            }
        });

        labelSafeFile.setText("Safe file name:");

        labelAprioryFile.setText("A priori file name:");

        buttonSafeFile.setText("...");
        buttonSafeFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSafeFileActionPerformed(evt);
            }
        });

        buttonAprioryFile.setText("...");
        buttonAprioryFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAprioryFileActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOk.setText("OK");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        labelOutputDimension.setText("Output dimension:");

        comboBoxOutputDimension.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4" }));
        comboBoxOutputDimension.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comboBoxOutputDimensionItemStateChanged(evt);
            }
        });

        comboBoxOutVar1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item1", "Item 2", "Item 3", "Item 4" }));
        comboBoxOutVar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxOutVar1ActionPerformed(evt);
            }
        });

        labelOutVar1.setText("1");

        labelOutVar2.setText("2");

        comboBoxOutVar2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxOutVar2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxOutVar2ActionPerformed(evt);
            }
        });

        labelOutVar3.setText("3");

        comboBoxOutVar3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxOutVar3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxOutVar3ActionPerformed(evt);
            }
        });

        labelOutVar4.setText("4");

        comboBoxOutVar4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxOutVar4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxOutVar4ActionPerformed(evt);
            }
        });

        labelSeparator.setText("Separator:");

        textFieldSeparator.setText(",");
        textFieldSeparator.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textFieldSeparatorKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                textFieldSeparatorKeyTyped(evt);
            }
        });

        panelTable.setLayout(new java.awt.BorderLayout());

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Status", "Omit", "Safe", "Unsafe", "Protect", "Cost", "Cost value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setRowSelectionAllowed(false);
        table.setShowVerticalLines(false);
        panelTable.add(table, java.awt.BorderLayout.CENTER);

        labelMapping.setText("Mapping:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelSafeFile)
                            .addComponent(labelSeparator)
                            .addComponent(labelOutputDimension)
                            .addComponent(labelAprioryFile))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(textFieldSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(comboBoxOutputDimension, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(labelOutVar2)
                                            .addComponent(labelOutVar1)
                                            .addComponent(labelOutVar3)))
                                    .addComponent(labelOutVar4, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(comboBoxOutVar1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(comboBoxOutVar2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(comboBoxOutVar3, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(comboBoxOutVar4, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(0, 304, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(textFieldAprioryFile)
                                    .addComponent(textFieldSafeFile))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(buttonSafeFile, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(buttonAprioryFile, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)
                        .addGap(10, 10, 10))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelTable, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelMapping)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldSafeFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSafeFile)
                    .addComponent(labelSafeFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textFieldSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelSeparator))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelAprioryFile)
                    .addComponent(textFieldAprioryFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonAprioryFile))
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelOutputDimension)
                    .addComponent(comboBoxOutputDimension, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboBoxOutVar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelOutVar1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelOutVar2)
                    .addComponent(comboBoxOutVar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelOutVar3)
                    .addComponent(comboBoxOutVar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelOutVar4)
                    .addComponent(comboBoxOutVar4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(labelMapping)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOk))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        apriori.WriteMappingsToRegistry();
        apriori.WriteAprioriFile(textFieldAprioryFile.getText(), textFieldSeparator.getText(), comboBoxOutputDimension.getSelectedIndex()+1);
        setCursor(Cursor.getDefaultCursor());
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void dialogClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_dialogClosing
        setVisible(false);
        dispose();
    }//GEN-LAST:event_dialogClosing

    
    
    private void ShowDimension(JLabel label, JComboBox<String> comboBox, boolean visible, int MaxNumber)
    {
        label.setVisible((visible));
        comboBox.setVisible((visible));
        
        if( visible)
        {
            comboBox.removeAllItems();
            comboBox.addItem("Other");
            for(Integer i=0; i<MaxNumber; i++)
                comboBox.addItem("ExpVar"+ Integer.toString(i+1));
            
            comboBox.setSelectedIndex(0);
        }
    }
    
    private void ShowDimensionSelection(int MaxNumber, int SelectedNumber)
    {   //This should not be here, but at a general level
        comboBoxOutputDimension.removeAllItems();
        
        int outDim = comboBoxOutputDimension.getSelectedIndex()+1;
        ShowDimension(labelOutVar1, comboBoxOutVar1, false, outDim);//MaxNumber);
        ShowDimension(labelOutVar2, comboBoxOutVar2, false, outDim);//MaxNumber);
        ShowDimension(labelOutVar3, comboBoxOutVar3, false, outDim);//MaxNumber);
        ShowDimension(labelOutVar4, comboBoxOutVar4, false, outDim);//MaxNumber);
            
        if( MaxNumber>0){                
            programmatically = true;  // to shield of the state changed listener
            for (int i=0;i<maxOutDim;i++){comboBoxOutputDimension.addItem(Integer.toString(i+1));}
                
            for(int i=0; i<maxOutDim; i++){
                switch(i){
                    //case 0: ShowDimension(labelOutVar1, comboBoxOutVar1, i<=SelectedNumber, MaxNumber); break;
                    case 0: ShowDimension(labelOutVar1, comboBoxOutVar1, i<=SelectedNumber, MaxNumber); break;
                    case 1: ShowDimension(labelOutVar2, comboBoxOutVar2, i<=SelectedNumber, MaxNumber); break;
                    case 2: ShowDimension(labelOutVar3, comboBoxOutVar3, i<=SelectedNumber, MaxNumber); break;
                    case 3: ShowDimension(labelOutVar4, comboBoxOutVar4, i<=SelectedNumber, MaxNumber); break;
                }
            }
                
            comboBoxOutputDimension.setSelectedIndex(SelectedNumber);
            programmatically = false;
        }
    }
    
    private void InputFileSpecsChanged() {
        if( !textFieldSafeFile.getText().trim().equals("") && 
            !textFieldSeparator.getText().trim().equals("")     )
        {
            try
            {
                apriori.ReadSafeFile(textFieldSafeFile.getText(), textFieldSeparator.getText());  
                
                if( !apriori.AllCellsHaveAStatus())
                    throw new ArgusException("Not all cells have a status defined.");
            }
            catch(ArgusException e)
            {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
            ShowDimensionSelection(apriori.getDimension(), apriori.getDimension()-1);
            ChangeOkButtonState();
        }
    }
    
    private void buttonSafeFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSafeFileActionPerformed
        TauArgusUtils.getDataDirFromRegistry(fileChooser);
        fileChooser.setDialogTitle("Select Safe file");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Safe file (*.txt)", "txt"));
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String safeFile = fileChooser.getSelectedFile().toString();
            if(!safeFile.contains(".")) safeFile+=".txt";
             
            textFieldSafeFile.setText(safeFile);
            
            InputFileSpecsChanged();
            TauArgusUtils.putDataDirInRegistry(safeFile);
            setCursor(Cursor.getDefaultCursor());
        }
    }//GEN-LAST:event_buttonSafeFileActionPerformed

    private void buttonAprioryFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAprioryFileActionPerformed
        TauArgusUtils.getDataDirFromRegistry(fileChooser);
        fileChooser.setDialogTitle("Select APriory file");
        fileChooser.setSelectedFile(new File(""));
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new FileNameExtensionFilter("APRiori file (*.hst)", "hst"));
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String aprioriFile = fileChooser.getSelectedFile().toString();
            
            if( !aprioriFile.contains(".") )
                aprioriFile+=".hst";

            textFieldAprioryFile.setText(aprioriFile);
            ChangeOkButtonState();
            setCursor(Cursor.getDefaultCursor());
            TauArgusUtils.putDataDirInRegistry(aprioriFile);
        }
    }//GEN-LAST:event_buttonAprioryFileActionPerformed

    private Boolean programmatically = false;
    private void comboBoxOutputDimensionItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comboBoxOutputDimensionItemStateChanged
        if( evt.getStateChange() == ItemEvent.SELECTED )
            if( !programmatically )
            {
                apriori.SelectedColumnsCount = comboBoxOutputDimension.getSelectedIndex()+1;
                ShowDimensionSelection(apriori.getDimension(), comboBoxOutputDimension.getSelectedIndex());
            }
    }//GEN-LAST:event_comboBoxOutputDimensionItemStateChanged
    
    private void comboBoxOutVar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxOutVar1ActionPerformed
        if( !programmatically)
        {
            ChangeOkButtonState();        
            apriori.ColumnMap.put(1, comboBoxOutVar1.getSelectedIndex());
        }
    }//GEN-LAST:event_comboBoxOutVar1ActionPerformed

    private void comboBoxOutVar2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxOutVar2ActionPerformed
        if( !programmatically)
        {
            ChangeOkButtonState();        
            apriori.ColumnMap.put(2, comboBoxOutVar2.getSelectedIndex());
        }
    }//GEN-LAST:event_comboBoxOutVar2ActionPerformed

    private void comboBoxOutVar3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxOutVar3ActionPerformed
        if( !programmatically)
        {
            ChangeOkButtonState();        
            apriori.ColumnMap.put(3, comboBoxOutVar3.getSelectedIndex());
        }
    }//GEN-LAST:event_comboBoxOutVar3ActionPerformed

    private void comboBoxOutVar4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxOutVar4ActionPerformed
        if( !programmatically)
        {
            ChangeOkButtonState();        
            apriori.ColumnMap.put(4, comboBoxOutVar4.getSelectedIndex());
        }
    }//GEN-LAST:event_comboBoxOutVar4ActionPerformed

    private void textFieldSeparatorKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFieldSeparatorKeyTyped
                   
    }//GEN-LAST:event_textFieldSeparatorKeyTyped

    private void textFieldSeparatorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFieldSeparatorKeyReleased
           InputFileSpecsChanged(); 
    }//GEN-LAST:event_textFieldSeparatorKeyReleased

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
//            java.util.logging.Logger.getLogger(DialogAPriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(DialogAPriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(DialogAPriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(DialogAPriori.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                DialogAPriori dialog = new DialogAPriori(new javax.swing.JFrame(), true);
//                dialog.setVisible(true);
//                System.exit(0);
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAprioryFile;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonSafeFile;
    private javax.swing.JComboBox<String> comboBoxOutVar1;
    private javax.swing.JComboBox<String> comboBoxOutVar2;
    private javax.swing.JComboBox<String> comboBoxOutVar3;
    private javax.swing.JComboBox<String> comboBoxOutVar4;
    private javax.swing.JComboBox<String> comboBoxOutputDimension;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JLabel labelAprioryFile;
    private javax.swing.JLabel labelMapping;
    private javax.swing.JLabel labelOutVar1;
    private javax.swing.JLabel labelOutVar2;
    private javax.swing.JLabel labelOutVar3;
    private javax.swing.JLabel labelOutVar4;
    private javax.swing.JLabel labelOutputDimension;
    private javax.swing.JLabel labelSafeFile;
    private javax.swing.JLabel labelSeparator;
    private javax.swing.JPanel panelTable;
    private javax.swing.JTable table;
    private javax.swing.JTextField textFieldAprioryFile;
    private javax.swing.JTextField textFieldSafeFile;
    private javax.swing.JTextField textFieldSeparator;
    // End of variables declaration//GEN-END:variables
}
