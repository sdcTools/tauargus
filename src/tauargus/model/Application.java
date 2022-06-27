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

import argus.model.ArgusException;
import argus.utils.SystemUtils;
import com.ibm.statistics.util.Utility;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.commons.io.FilenameUtils;
import tauargus.extern.dataengine.TauArgus;
import tauargus.extern.tauhitas.HiTaSCtrl;
import tauargus.extern.taurounder.RounderCtrl;
import tauargus.gui.FrameInfo;
import tauargus.gui.FrameMain;
import tauargus.service.TableService;

public class Application {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    // Version info
    public static final int MAJOR = 4;
    public static final int MINOR = 2;
    public static final String REVISION = "3";
    public static final int BUILD = 1;
    
    // Error codes returned by functions in TauArgusJava dll
    public static final int ERR_CODENOTINCODELIST = 1017;
    public static final int ERR_CELLALREADYFILLED = 1022;
    public static final int ERR_TABLENOTADDITIVE = 1024;
    public static final int ERR_CODEDOESNOTEXIST = 1027;
    
    public static final int BATCH_FROMMENU = 1;
    public static final int BATCH_COMMANDLINE = 2;
    public static final int BATCH_NOBATCH = 0;

    public static final int SOLVER_NO = 0;  // not used any more
    public static final int SOLVER_XPRESS = 1;
    public static final int SOLVER_CPLEX = 2;
    public static final int SOLVER_SOPLEX = 3;
    private static final File MANUAL = new File("TauManualV4.1.pdf");
    private static Process helpViewerProcess;

    // for interfacing with C++ dll
    static {
        System.loadLibrary("TauHitas");           
        System.loadLibrary("TauRounder");
        System.loadLibrary("TauArgusJava");                
 }
    private static TauArgus tauArgus = new TauArgus();
    private static HiTaSCtrl tauHitas = new HiTaSCtrl();
    private static RounderCtrl rounder = new RounderCtrl();
    private static ArrayList<Variable> variables = new ArrayList<Variable>();
    private static ArrayList<Metadata> metadatas = new ArrayList<Metadata>();
    private static boolean anco = false;
    private static int batch;
    private static boolean protectCoverTable = false;
    public static int solverSelected;
    public static int generalMaxHitasTime;
    public static boolean SaveDebugHiTaS = false;
    public static FrameInfo windowInfo = new FrameInfo();
    public static boolean windowInfoIsOpen = false;
    private static String manualPath;
    public static String batchDataPath;

    private static Variable freqVar = new Variable(null);
    static {
        freqVar.name = "<freq>";
        freqVar.type = Type.FREQUENCY;
        freqVar.varLen = 4;
        freqVar.bPos = 1;
    }
    
    // On Windows java.io.tmpdir gives the value of the TMP, TEMP, USERPROFILE 
    // or windir environment variable. 
    private static String tempDir;
    static {
        setTempDir(System.getProperty("java.io.tmpdir"));
    }
    
    public static String getFullVersion() {
        return "" + MAJOR + "." + MINOR + "." + REVISION; // + "; build: " + BUILD; build is shown separately in lower left corner
    }
    
    public static String getArgusLibVersion() {
        return "" + argus.model.Application.MAJOR + "." + argus.model.Application.MINOR + "." + 
                                    argus.model.Application.REVISION + " build " + argus.model.Application.BUILD;
    }
    
    public static String getRounderVersion(){
        return rounder.GetVersion();
    }
    
    public static String getHitasVersion(){
        return tauHitas.GetVersion();
    }
    
    public static String getArgusJavaVersion(){
        return tauArgus.GetVersion();
    }
    
    public static String getSolverName( int solver){
        switch (solver) {
          case SOLVER_NO:  return "no solver";
          case SOLVER_XPRESS:  return "XPRESS";
          case SOLVER_CPLEX :  return "CPLEX"; 
          case SOLVER_SOPLEX:  return "SCIP";
          default: return "unknown";    
        }                
    }
    
    private static String getSpssVersion()
    {
        Utility FindSpss = new Utility();
        return FindSpss.getStatisticsLocationLatest();
    }
    
    static{
        try{
            ClassPathHack.addFile(getSpssVersion() + "\\spssjavaplugin.jar");
        }catch (IOException ex){System.out.print(ex.toString());}
    }
        
    private static SpssUtilsTau spssUtils;
    
    /**
     * Gets the instance of SpssUtilsTau.
     *
     * @return SpssUtils, or null if the Spss plugin cannot be loaded
     */
    public static SpssUtilsTau getSpssUtils() {
        try {
            if (spssUtils == null) {
                spssUtils = new SpssUtilsTau();
            }
            return spssUtils;
        } catch (NoClassDefFoundError err) {
            JOptionPane.showMessageDialog(null,"Not possible to read SPSS-files, because no SPSS version 21+ found.","Information",JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
    }
    
    public static TauArgus getTauArgusDll() {
        return tauArgus;
    }

    public static HiTaSCtrl getTauHitasDll() {
        return tauHitas;
    }
    
    public static RounderCtrl getRounder() {
        return rounder;
    }

    public static List<Metadata> getMetadatas() {
        return metadatas;
    }
    
    public static void addVariable(Variable variable) {
        variable.index = variables.size();
        variables.add(variable);
    }
    
    public static void clearEverythingGui(){
        TableService.clearTables();
        clearMetadatas();
        clearVariables();
    }
  
    public static void clearVariables() {
        variables.clear();
    }

    public static int numberOfVariables() {
        return variables.size();
    }
    
    public static Variable getVariable(int index) {
        return variables.get(index);
    }
    
    public static int indexOfVariable(Variable variable) {
        return variable == null ? -1 : variable.index;
    }
    
    public static void setVariables() {
        clearVariables();
        for (Metadata metadata : metadatas) {
            for (Variable variable : metadata.variables) {
                addVariable(variable);
                if (metadata.dataOrigin == Metadata.DATA_ORIGIN_MICRO && metadata.dataFileType == Metadata.DATA_FILE_TYPE_FREE) {
                    variable.bPos = variable.index + 1;
                }
            }
        }
    }
    
    public static void showHelp(String namedDest) throws ArgusException {
        ArrayList<String> args = new ArrayList<>();
        args.add("-loadfile");
        args.add(MANUAL.getAbsolutePath());
        if (namedDest != null) {
            args.add("-nameddest");
            args.add(namedDest);
        }

        try {
            execClass(
                    "org.icepdf.ri.viewer.Main",
                    "lib/ICEpdf.jar",
                    args);
        }
        catch (IOException | InterruptedException ex) {
 //               throw new ArgusException("Error trying to display help file");
        }
    }

    public static void execClass(String className, String classPath, List<String> arguments) throws IOException,
                                               InterruptedException {
        if (helpViewerProcess != null) {
            helpViewerProcess.destroy();
            helpViewerProcess = null;
        }
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        //String classpath = System.getProperty("java.class.path");
        //String className = klass.getCanonicalName();
        arguments.add(0, javaBin);
        arguments.add(1, "-cp");
        arguments.add(2, classPath);
        arguments.add(3, className);
        ProcessBuilder builder = new ProcessBuilder( arguments );

        helpViewerProcess = builder.start();
    }
            
   
    public static void openInfoWindow(boolean openen){
        if (openen) {
         windowInfo.setVisible(true);
         windowInfo.setLocationRelativeTo(null);
         windowInfoIsOpen = true; 
        } else { 
         windowInfo.setVisible(false);
         windowInfoIsOpen = false;
       }
    }

    public static int numberOfMetadatas() {
        return metadatas.size();
    }
    
    public static Metadata getMetadata(int index) {
        return metadatas.get(index);
    }
    
    public static void addMetadata(Metadata metadata) {
        metadata.index = metadatas.size();
        metadatas.add(metadata);
    }

/* Never used?????
    public static Metadata removeMetadata(int index) {
        Metadata metadata = metadatas.get(index);
        metadatas.remove(index);
        for (int i = index; i < metadatas.size(); i++) {
            metadatas.get(i).index--;
        }
        return metadata;
    }
*/
    public static void replaceMetadata(Metadata oldMetadata, Metadata metadata) {
        metadatas.set(oldMetadata.index, metadata);
        metadata.index = oldMetadata.index;
    }

    public static void clearMetadatas() {
        metadatas.clear();
    }
    
    public static boolean isBatch() {
        return batch != BATCH_NOBATCH;
    }

    public static int batchType() {
        return batch;
    }

    public static void setBatch(int b) {
        ArgusException.setForLogbook(b!=BATCH_NOBATCH);
        batch = b;
    }

    public static boolean isAnco() {
        return anco;
    }

    public static void setAnco(boolean anco) {
        Application.anco = anco;
        SystemUtils.putRegBoolean("general", "anco" , anco);
    }
    
    public static void getAnco() {
        Application.anco = SystemUtils.getRegBoolean("general", "anco" , false);
    }

    public static boolean isProtectCoverTable() {
        return protectCoverTable;
    }

    public static void setProtectCoverTable(boolean protectCoverTable) {
        Application.protectCoverTable = protectCoverTable;
    }

    public static Variable getFreqVar() {
        return freqVar;
    }

    public static String getTempDir() {
        return tempDir;
    }

    public static void setTempDir(String tempDir) {
        Application.tempDir = FilenameUtils.normalizeNoEndSeparator(tempDir);
    }
    
    public static String getTempFile(String fileName) {
        return FilenameUtils.concat(tempDir, fileName);
    }
    
    private static void sleepThread(int milliSecs) {
        try {
            Thread.sleep(milliSecs);
        }
        catch (InterruptedException ex) {
            // Do something, if there is a exception
            System.out.println(ex.toString());
        }
    }

    public static void showBuildInfoInSplashScreen() {
       final SplashScreen splash = SplashScreen.getSplashScreen();
       if (splash == null) {
           System.out.println("SplashScreen.getSplashScreen() returned null");
       } else {
           Graphics2D g = splash.createGraphics();
           if (g == null) {
               System.out.println("g is null");
           } else {
               g.setPaintMode();
               g.setColor(new Color(0, 0, 255));

               g.setFont(new Font("Arial",Font.BOLD,20));
               g.drawString("Version " + Application.getFullVersion(), 260, 340);
               
               g.setFont(new Font("Arial",Font.PLAIN,16));
               g.drawString("Build " + Application.BUILD, 39, 439);
               splash.update();
               // Sleep, so people can see it
               sleepThread(1000); 
           }
       }
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                    
                }
            }
// Anco 1.6            
//        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
        } catch (ClassNotFoundException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);}
          catch (InstantiationException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);}
          catch (IllegalAccessException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);}
          catch (javax.swing.UnsupportedLookAndFeelException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        //Locale.setDefault(Locale.US);
        //try{manualPath = SystemUtils.getApplicationDirectory(Application.class).getCanonicalPath();}
        //catch (IOException ex){};
        //manualPath = manualPath +"/tauManual.pdf";
        try{manualPath = MANUAL.getPath();}
        catch (Exception ex){}
        
        SystemUtils.setRegistryRoot("tauargus/");
        batch = BATCH_NOBATCH;
        getAnco();
        SystemUtils.setLogbook(SystemUtils.getRegString("general", "logbook", getTempFile("TauLogbook.txt")));
        SystemUtils.writeLogbook(" ");         
        SystemUtils.writeLogbook("--------------------------");
        SystemUtils.writeLogbook("Start of TauArgus run");
        SystemUtils.writeLogbook("TauArgus version " + Application.getFullVersion() + " build " + Application.BUILD);
        SystemUtils.writeLogbook("ArgusLib.jar version " + getArgusLibVersion());
        SystemUtils.writeLogbook("TauRounder.dll version " + rounder.GetVersion());
        SystemUtils.writeLogbook("TauHitas.dll version " + tauHitas.GetVersion());
        SystemUtils.writeLogbook("TauArgusJava.dll version " + tauArgus.GetVersion());
        SystemUtils.writeLogbook("--------------------------");
        solverSelected = SystemUtils.getRegInteger("optimal", "solverused", SOLVER_SOPLEX);
        generalMaxHitasTime = SystemUtils.getRegInteger("optimal", "maxhitastime", 1);
        anco = SystemUtils.getRegBoolean("general", "anco", false);
        batchDataPath = "";
        if (args.length > 0 && !args[0].equalsIgnoreCase("X")) {
            //an "X" temporarily disables the command-line parameters set in the IDE
            // Batch processing...
            setBatch(BATCH_COMMANDLINE);
            
            //TODO Declare global
            //the "/v" parameter is only used if the batch is called for linked tables
            //tau will then show some progress info
            for (String arg : args) {
                if (arg.equals("/v")) {  
                    setBatch(BATCH_FROMMENU);
                }
            }
            if (args.length > 1) {
              if(!args[1].equals("/v") && !args[1].equals("-")){SystemUtils.setLogbook(args[1]);}
            }

            if (args.length > 2) {
                if(!args[2].equals("/v") && !args[2].equals("-")){setTempDir(args[2]);}
            }

            if (args.length > 3) {
                if(!args[3].equals("/v")){tauargus.model.batch.setBatchDataPath(args[3]);}
                //Checking on the validity of this path willbe donewhen invokingthe batch process
            }
            boolean interActive;
            int exitCode;
            
            // Do a lot of stuff...
            exitCode = tauargus.model.batch.runBatchProcess(args[0]);
            interActive = (exitCode == 1); 
            if (!interActive) {
                SystemUtils.writeLogbook("End of TauArgus run");
                System.exit(exitCode);
            }
        }
        else {
        // Interactive mode...
        showBuildInfoInSplashScreen();
        }
        
        // Showing the GUI will remove the splashscreen (see file manifest.mf)        
        /* Create and display the form */
        Application.setBatch(BATCH_NOBATCH);
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FrameMain().setVisible(true);
            }
        });
    }
}
