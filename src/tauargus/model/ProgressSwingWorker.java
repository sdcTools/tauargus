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

package tauargus.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.SwingWorker;
import tauargus.gui.DialogProgress;
import tauargus.gui.DialogRounderProgress;

public abstract class ProgressSwingWorker<T, V> extends SwingWorker<T, V> {
    
    public static final int SINGLE = 0;
    public static final int DOUBLE = 1;
    public static final int VALUES = 2;
    public static final int ROUNDER = 3;

    private final int progressType;
    private final DialogProgress dialogProgress;
    private final DialogRounderProgress dialogRounderProgress;
    private final PropertyChangeListener propertyChangeListener;

    public ProgressSwingWorker(int progressType, String title) {
        super();
        
        this.progressType = progressType;

        propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
            }
        };

        dialogRounderProgress = new DialogRounderProgress(Application.windowInfo, true);
        dialogProgress = new DialogProgress(Application.windowInfo, true);
        
        if (progressType == ROUNDER)
        {
            dialogRounderProgress.setTitle(title);
            addPropertyChangeListener(dialogRounderProgress);            
        }
        else
        {
            dialogProgress.setTitle(title);
            addPropertyChangeListener(dialogProgress);
        }
    }

    @Override
    protected T doInBackground() throws Exception {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (progressType == ROUNDER)
                    dialogRounderProgress.showDialog();
                else
                    dialogProgress.showDialog(progressType);
            }
        });
        
        return null;
    }

    @Override
    protected void done() { // cleanup dialogs
        if (progressType == ROUNDER){
            dialogRounderProgress.setVisible(false);
            dialogRounderProgress.dispose();
            dialogProgress.dispose();
        }
        else{
            dialogProgress.setVisible(false);
            dialogProgress.dispose();
            dialogRounderProgress.dispose();
        }
    }

    public PropertyChangeListener getPropertyChangeListener() {
        return propertyChangeListener;
    }
}
