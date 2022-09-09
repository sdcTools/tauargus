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
//import tauargus.model.Application;
//import tauargus.utils.ExecUtils;
import argus.utils.SystemUtils;

public class ArgusException extends Exception {
    
    private boolean critical;
    public String ArgusExceptionMessage;
    
    public ArgusException(boolean critical) {
        this.critical = critical;
    }

    public ArgusException(String message, boolean critical) {
        super(message);
        this.critical = critical;
        if (tauargus.model.Application.isBatch()) {SystemUtils.writeLogbook(message);}
    }

    public ArgusException(String message) {
        this(message, true);
        ArgusExceptionMessage = message;
    }

    public ArgusException(String message, Throwable cause, boolean critical) {
        super(message, cause);
        this.critical = critical;
    }

    public ArgusException(Throwable cause, boolean critical) {
        super(cause);
        this.critical = critical;
    }

    public ArgusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, boolean critical) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.critical = critical;
    }

    public boolean getCritical() {
        return critical;
    }
}
