/*
 * Copyright (C) 2007, 2008, 2009 Patrick Balleux, Nicolas Cardoso De Castro
 * (nicolas_cardoso@users.sourceforge.net), Daniel Žalar (danizmax@gmail.com)
 *
 * This file is part of JSymphonic program.
 *
 * JSymphonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JSymphonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JSymphonic. If not, see <http://www.gnu.org/licenses/>.
 *
 *****
 * 
 * JSymphonicWindow.java
 *
 * Created on March 25, 2008, 9:59 PM
 * 
 */

package org.danizmax.jsymphonic.gui;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import javax.swing.UnsupportedLookAndFeelException;
import org.danizmax.jsymphonic.gui.device.DevicePanel;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.danizmax.jsymphonic.gui.device.DeviceManager;
import org.danizmax.jsymphonic.gui.local.LocalPanel;
import org.danizmax.jsymphonic.toolkit.ProfileElement;
import org.naurd.media.jsymphonic.title.Title;

/**
 * This Class is the main JSymphonic window, that contains all the command buttons and all the component
 * @author  danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class JSymphonicWindow extends javax.swing.JFrame {

    /**
     * @return the logger
     */
    public static Logger getLogger() {
        return logger;
    }
    
    private LocalPanel localPanel;
    private DevicePanel devicePanel;
    private LocalPanel exportLocalPanel;
    private JTabbedPane localTabbedPane;

    private static Logger logger = Logger.getLogger("org.danizmax.jsymphonic.gui");
    private static String configFile = "JSymphonic.xml";
    private static FileHandler fileLogHandler;
    private static GuiLogHandler glHandler;
    //Log file settings
    private static String logFileName= "JSymphonic.log";
    private static int byteSizeLimit = 1000000; 
    private static int numOfLogFiles = 1;
    private static boolean append = true;
    
    private SettingsHandler sHandler = null;
    //private Log2Gui lgui = null;
    private TreeMap themeMap = null;
    private static TreeMap langMap = null;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

    private boolean profilesAreReady = false; // Since actions performed on the profiles combo box makes the loadNewConfig() method to run, we shouldn't load config when profiles are added to the combo box. This variable is used to know when the loadNewConfig() method should be run or not.
    
    private boolean comboBoxAlreadyClicked = false; // Used to know if the profile should be reloaded

    //configure logging format
    private Formatter logFormatter = new Formatter() {
  	  public String format(LogRecord record) {
  		  String lvlStr = " [" + record.getLevel() + "]"; 
		  while((lvlStr += " ").length() < 11);
		  return  dateFormatter.format(new Date()) + lvlStr
		  	+ record.getSourceClassName() + ":"
		  	+ record.getSourceMethodName() + " : "
		  	+ record.getMessage() + "\n";
	  }
	};
    
    
    /** Creates new form JSymphonicWindow */
    public JSymphonicWindow() {
        logger.setLevel(Level.ALL);
        //config file has to be loaded before anything!!!
        //lgui = new Log2Gui(logTextArea);
        initLang(); //  First thing to do is to init language, because if the config file doesn't exist, we need to know what languages are available to create a default config file
        sHandler = new SettingsHandler(configFile);
        initLogger();
        logger.info("Initializing JSymphonic...");
        initLang(sHandler.getLanguage());

        try {
            initComponents();
            mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.X_AXIS));
            localTabbedPane = new JTabbedPane();
            localPanel = new LocalPanel();
            exportLocalPanel = new LocalPanel();
            devicePanel = new DevicePanel();
            localTabbedPane.add(java.util.ResourceBundle.getBundle("localization/jsymphonicwindow").getString("JSymphonicWindow.local_folder"), localPanel);
            localTabbedPane.add(java.util.ResourceBundle.getBundle("localization/jsymphonicwindow").getString("JSymphonicWindow.export_folder"),exportLocalPanel);
            mainPanel.add(localTabbedPane);
            mainPanel.add(devicePanel);

        }
        catch(Exception e){
            logger.severe("Error while initializaing main window");
            e.printStackTrace();
        }
        
        initThemeMap();
        changeLAF((String)themeMap.get(sHandler.getTheme()));

        //logger.addHandler(lgui);
        SettingsHandler.setParentLogger(logger);
        DevicePanel.setParentLogger(logger);
        DeviceManager.setParentLogger(logger);
        org.naurd.media.jsymphonic.toolBox.FFMpegToolBox.setParentLogger(logger);
        org.naurd.media.jsymphonic.device.sony.nw.NWOmgaudio.setParentLogger(logger);
        org.naurd.media.jsymphonic.device.sony.nw.NWEsys.setParentLogger(logger);
        org.naurd.media.jsymphonic.device.sony.nw.NWGeneric.setParentLogger(logger);
        
        loadProfiles();
        loadNewConfig();
        devicePanel.setWnd(this);
        
        logger.info("JSymphonic is ready!");
    }
    
    /**
     * Read all profiles from the config file and load the default one into the profile combo box
     */
    public void loadProfiles(){
        File localFolderTest; // Folder to test if the local path is valid
        ProfileElement profileTest; // Temporary profile to test if the local path is valid
        Boolean continueLoop = true; // Semaphore to stop the loop (when trying to find a valid local path)

        // Modification are going to be done on the combo box, but we don't want the associated listener to run any action
        profilesAreReady = false;

        // First, empty the combobox
        profilesComboBox.removeAllItems();

        // Read all the profiles and fill in the combobox
        HashMap profiles = sHandler.getProfiles(); // Get the profiles
        Set keys = profiles.keySet(); // Get all the profile names
        Iterator iter = keys.iterator(); // Create an iterator
        while (iter.hasNext()) {
            profilesComboBox.addItem((String)iter.next());
        }

        // Check that the local music folder exist for the default profile
        profileTest = (ProfileElement) profiles.get(sHandler.getSelectedProfile()); // get default profile
        localFolderTest = new File( profileTest.getLocalPath() ); // get local path from the default profile

        // If the local folder doesn't exist, try to find another valid profile
        iter = keys.iterator(); // Re-initialize the iterator
        while(!localFolderTest.exists() && continueLoop){
            if(iter.hasNext()){
                // If there is one other profile
                profileTest = (ProfileElement) profiles.get((String)iter.next()); // get new profile
                localFolderTest = new File( profileTest.getLocalPath() ); // get local path from the new profile
            }
            else{
                // All profiles have been tested, and none are valid, just use the default one
                profileTest = (ProfileElement) profiles.get(sHandler.getSelectedProfile()); // get default profile
                continueLoop = false; // exit the loop
            }
        }
        
        // Set the default profile in the combobox
        profilesComboBox.setSelectedItem(profileTest.getProfileName());

        profilesAreReady = true;
    }
    
    public void initLogger(){
        glHandler = new GuiLogHandler();
        glHandler.setFormatter(getLogFormatter());
        getLogger().addHandler(glHandler);
        if(sHandler.isLogToFile()){
            try {
                fileLogHandler = new FileHandler(logFileName,byteSizeLimit,numOfLogFiles,append);
                fileLogHandler.setFormatter(getLogFormatter());
            } catch (IOException ex) {
                Logger.getLogger(JSymphonicWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(JSymphonicWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            getLogger().addHandler(fileLogHandler);
        }
        
        Level lvl = Level.parse(sHandler.getLogLevel());

        LogManager lm = LogManager.getLogManager();
        // Add logger from jaudiotagger
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.asf.tag"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.flac"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.flac.MetadataBlockDataPicture"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.flac.MetadataBlockDataStreamInfo"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.generic"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.generic"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.mp3"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.mp4"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.mp4.atom"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.ogg"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.audio.ogg.atom"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.tag.datatype"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.tag.id3"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.tag.mp4"));
        lm.addLogger(Logger.getLogger("org.jaudiotagger.tag.vorbiscomment.VorbisCommentReader"));

        Enumeration<String> loggers = lm.getLoggerNames();
        String loggerName = null;
        
        //set levels for all loggers
        while(loggers.hasMoreElements()){
            loggerName = loggers.nextElement();
            if(loggerName.contains("jsymphonic")){ //set log level only for "jsymphonic" classes
                lm.getLogger(loggerName).setLevel(lvl);
            }
            if(loggerName.contains("jaudiotagger")){ //set log level only for "jaudiotagger" classes
                // We don't care about jaudiotagger log, only severe are shown
                lm.getLogger(loggerName).setLevel(Level.SEVERE);
            }
        }

        //set level for all handlers
        Handler[] handlers = Logger.getLogger( "" ).getHandlers();
       // Handler[] handlers = getLogger().getHandlers();
        getLogger().setLevel(lvl);
        for ( int index = 0; index < handlers.length; index++ ) {
            handlers[index].setLevel(lvl);
            handlers[index].setFormatter(getLogFormatter());
        }
    }
    
    public void changeLAF(String laf) {
        try {
            if(laf != null){
                UIManager.setLookAndFeel(laf);
                SwingUtilities.updateComponentTreeUI(this);
                SwingUtilities.updateComponentTreeUI(devicePanel);
                SwingUtilities.updateComponentTreeUI(localPanel);
                SwingUtilities.updateComponentTreeUI(devicePanel.getTreePopUp());
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JSymphonicWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(JSymphonicWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(JSymphonicWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(JSymphonicWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
   }
    
   public void loadNewConfig(){
        HashMap profiles = sHandler.getProfiles();
        ProfileElement profile = (ProfileElement) profiles.get(profilesComboBox.getSelectedItem());

        devicePanel.setAlwaysTranscode(sHandler.isAlwaysTrascode());
        devicePanel.setTranscodeBitrate(sHandler.getBitrate());
        devicePanel.setDevicePath(profile.getDevicePath());
        devicePanel.setDeviceGeneration(profile.getDeviceGeneration());
        devicePanel.setTempPath(profile.getTranscodeTempPath());
        if(((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).isExportPathSameAsLocalPath()){
            devicePanel.setExportPath(((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).getLocalPath());
        }else{
            devicePanel.setExportPath(((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).getExportPath());
        }
        Title.setTryReadTagInfo(sHandler.isReadID3Tags());
        Title.setTryReadTagInfoPattern(sHandler.getReadTagFolderStructure());

        devicePanel.mountDevice(true);
        
        localPanel.loadSupportedFileFormats(profile.getDeviceGeneration());
        localPanel.setLocalPath(((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).getLocalPath());
        localPanel.reloadTree();
        
        //true if export and local path are the same
        if(((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).getLocalPath().equals(((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).getExportPath()) || ((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).isExportPathSameAsLocalPath() == true){
            localTabbedPane.setEnabledAt(1, false);
        }else{
            localTabbedPane.setEnabledAt(1, true);
            exportLocalPanel.loadSupportedFileFormats(profile.getDeviceGeneration());
            exportLocalPanel.setLocalPath(((ProfileElement) profiles.get(profilesComboBox.getSelectedItem())).getExportPath());
            exportLocalPanel.reloadTree();
        }
   }
   
    private void initLang(){
        // Define existing languages
        langMap = new TreeMap();
        getLangMap().put("English (default)", new Locale("en", "GB"));
        getLangMap().put("Chinese", new Locale("zh", "TW"));
        getLangMap().put("Deutsch", new Locale("de", "DE"));
        getLangMap().put("Español", new Locale("es", "ES"));
        getLangMap().put("Français", new Locale("fr", "FR"));
//        getLangMap().put("Italiano", new Locale("it", "IT"));
//        getLangMap().put("Português", new Locale("pt", "PT"));
//        getLangMap().put("Slovenčina ", new Locale("sk", "SK"));
//        getLangMap().put("Slovenščina", new Locale("sl", "SI"));
        getLangMap().put("Svenska", new Locale("sv", "SV"));
        getLangMap().put("Türkçe", new Locale("tr", "TR"));
//        getLangMap().put("Česky", new Locale("cs", "CS"));
        getLangMap().put("Русский", new Locale("ru", "RU"));
    }

    private void initLang(String selectedLanguage){
        // Try to use langMap
        try{
            getLangMap().size();
        }
        catch(Exception e){
            // If an exception is thrown, langMap has not been init, do so:
            initLang();
        }

        // Set selected locale
        if(getLangMap().get(selectedLanguage) != null)
            Locale.setDefault((Locale) getLangMap().get(selectedLanguage));
   }
   
   private void initThemeMap(){
        themeMap = new TreeMap();
        
        // Get LAF info from the system
        UIManager.LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
        
        // Get java version
        int javaVersion = Integer.parseInt(System.getProperty("java.version").substring(2,3)); // Gives an answer like "5" for java 1.5 or "6" for java 1.6
        // If java version is more than 6, Windows and GTK LAf may be available, if they are supported, add them to the list
        if(javaVersion >= 6){
            for(int i = 0; i < lafInfo.length; i++ ) {
                if(lafInfo[i].getName().contains("Windows")) {
                    getThemeMap().put("Windows", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                }
                if(lafInfo[i].getName().contains("GTK+")) {
                    getThemeMap().put("GTK+", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                }
            }
        }
        
        // Then, add all cross-platform styles
        getThemeMap().put("Metal", "javax.swing.plaf.metal.MetalLookAndFeel");
        getThemeMap().put("GTK","com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
       // getThemeMap().put("Windows","com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//        getThemeMap().put("Lipstik", "com.lipstikLF.LipstikLookAndFeel");
//        getThemeMap().put("InfoNode", "net.infonode.gui.laf.InfoNodeLookAndFeel");
//        getThemeMap().put("TinyLaf", "de.muntjak.tinylookandfeel.TinyLookAndFeel");
//        getThemeMap().put("Nimbus", "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
     /*  comented because of substance errors getThemeMap().put("Substance Raven Graphite", "org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel");
        getThemeMap().put("Substance Raven", "org.jvnet.substance.skin.SubstanceRavenLookAndFeel");
        getThemeMap().put("Substance Magma", "org.jvnet.substance.skin.SubstanceMagmaLookAndFeel");
        getThemeMap().put("Substance Emerald Dusk", "org.jvnet.substance.skin.SubstanceEmeraldDuskLookAndFeel");
        getThemeMap().put("Substance Business", "org.jvnet.substance.skin.SubstanceBusinessLookAndFeel");
        getThemeMap().put("Substance Business Blue Steel", "org.jvnet.substance.skin.SubstanceBusinessBlueSteelLookAndFeel");
        getThemeMap().put("Substance Business Black Steel", "org.jvnet.substance.skin.SubstanceBusinessBlackSteelLookAndFeel");
        getThemeMap().put("Substance Creme", "org.jvnet.substance.skin.SubstanceCremeLookAndFeel");
        getThemeMap().put("Substance Creme Coffee", "org.jvnet.substance.skin.SubstanceCremeCoffeeLookAndFeel");
        getThemeMap().put("Substance Sahara", "org.jvnet.substance.skin.SubstanceSaharaLookAndFeel");
        getThemeMap().put("Substance Moderate", "org.jvnet.substance.skin.SubstanceModerateLookAndFeel");
        getThemeMap().put("Substance Office Silver 2007", "org.jvnet.substance.skin.SubstanceOfficeSilver2007LookAndFeel");
        getThemeMap().put("Substance Nebula", "org.jvnet.substance.skin.SubstanceNebulaLookAndFeel");
        getThemeMap().put("Substance Nebula Brick Wall", "org.jvnet.substance.skin.SubstanceNebulaBrickWallLookAndFeel");
        getThemeMap().put("Substance Autumn", "org.jvnet.substance.skin.SubstanceAutumnLookAndFeel");
        getThemeMap().put("Substance Mist Silver", "org.jvnet.substance.skin.SubstanceMistSilverLookAndFeel");
        getThemeMap().put("Substance Mist Aqua", "org.jvnet.substance.skin.SubstanceMistAquaLookAndFeel");         */ 
   }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainToolBar = new javax.swing.JToolBar();
        importButton = new javax.swing.JButton();
        exportButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        applyButton = new javax.swing.JButton();
        revertButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        propertiesButton = new javax.swing.JButton();
        profilesComboBox = new javax.swing.JComboBox();
        mainPanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jsymphonicMenu = new javax.swing.JMenu();
        propertiesMenuItem = new javax.swing.JMenuItem();
        quitMenuItem = new javax.swing.JMenuItem();
        transferMenu = new javax.swing.JMenu();
        importMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        applyMenuItem = new javax.swing.JMenuItem();
        revertMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        logMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("localization/misc"); // NOI18N
        setTitle(bundle.getString("global.version")); // NOI18N
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/js_logo16.png")).getImage());
        setMinimumSize(new java.awt.Dimension(640, 480));
        setName("Form"); // NOI18N

        mainToolBar.setFloatable(false);
        mainToolBar.setRollover(true);
        mainToolBar.setName("mainToolBar"); // NOI18N

        importButton.setFont(new java.awt.Font("Dialog", 0, 12));
        importButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/import32.png"))); // NOI18N
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("localization/jsymphonicwindow"); // NOI18N
        importButton.setToolTipText(bundle1.getString("JSymphonicWindow.importMenuItem.text")); // NOI18N
        importButton.setFocusable(false);
        importButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        importButton.setName("importButton"); // NOI18N
        importButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(importButton);

        exportButton.setFont(new java.awt.Font("Dialog", 0, 12));
        exportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/export32.png"))); // NOI18N
        exportButton.setToolTipText(bundle1.getString("JSymphonicWindow.exportMenuItem.text")); // NOI18N
        exportButton.setFocusable(false);
        exportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exportButton.setName("exportButton"); // NOI18N
        exportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(exportButton);

        deleteButton.setFont(new java.awt.Font("Dialog", 0, 12));
        deleteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/remove32.png"))); // NOI18N
        deleteButton.setToolTipText(bundle.getString("global.Delete")); // NOI18N
        deleteButton.setFocusable(false);
        deleteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteButton.setName("deleteButton"); // NOI18N
        deleteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(deleteButton);

        jSeparator2.setName("jSeparator2"); // NOI18N
        mainToolBar.add(jSeparator2);

        applyButton.setFont(new java.awt.Font("Dialog", 0, 12));
        applyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/save32.png"))); // NOI18N
        applyButton.setToolTipText(bundle1.getString("JSymphonicWindow.Apply_all_changes")); // NOI18N
        applyButton.setFocusable(false);
        applyButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        applyButton.setName("applyButton"); // NOI18N
        applyButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(applyButton);

        revertButton.setFont(new java.awt.Font("Dialog", 0, 12));
        revertButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/revert32.png"))); // NOI18N
        revertButton.setToolTipText(bundle1.getString("JSymphonicWindow.revertMenuItem.text")); // NOI18N
        revertButton.setFocusable(false);
        revertButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        revertButton.setName("revertButton"); // NOI18N
        revertButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        revertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                revertButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(revertButton);

        jSeparator3.setName("jSeparator3"); // NOI18N
        mainToolBar.add(jSeparator3);

        propertiesButton.setFont(new java.awt.Font("Dialog", 0, 12));
        propertiesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/configure32.png"))); // NOI18N
        propertiesButton.setToolTipText(bundle1.getString("JSymphonicWindow.propertiesMenuItem.text")); // NOI18N
        propertiesButton.setFocusable(false);
        propertiesButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        propertiesButton.setName("propertiesButton"); // NOI18N
        propertiesButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        propertiesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                propertiesButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(propertiesButton);

        profilesComboBox.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        profilesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        profilesComboBox.setMaximumSize(new java.awt.Dimension(200, 23));
        profilesComboBox.setMinimumSize(new java.awt.Dimension(200, 23));
        profilesComboBox.setName("profilesComboBox"); // NOI18N
        profilesComboBox.setPreferredSize(new java.awt.Dimension(200, 23));
        profilesComboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                profilesComboBoxMouseClicked(evt);
            }
        });
        profilesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                profilesComboBoxActionPerformed(evt);
            }
        });
        mainToolBar.add(profilesComboBox);

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 742, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 499, Short.MAX_VALUE)
        );

        jMenuBar1.setName("jMenuBar1"); // NOI18N

        jsymphonicMenu.setText(bundle1.getString("JSymphonicWindow.jsymphonicMenu.text")); // NOI18N
        jsymphonicMenu.setActionCommand(bundle1.getString("JSymphonicWindow.jsymphonicMenu.text")); // NOI18N
        jsymphonicMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        jsymphonicMenu.setName("jsymphonicMenu"); // NOI18N

        propertiesMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_MASK));
        propertiesMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        propertiesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/configure.png"))); // NOI18N
        propertiesMenuItem.setText(bundle1.getString("JSymphonicWindow.propertiesMenuItem.text")); // NOI18N
        propertiesMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.propertiesMenuItem.text")); // NOI18N
        propertiesMenuItem.setName("propertiesMenuItem"); // NOI18N
        propertiesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                propertiesMenuItemActionPerformed(evt);
            }
        });
        jsymphonicMenu.add(propertiesMenuItem);

        quitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.ALT_MASK));
        quitMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        quitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/exit.png"))); // NOI18N
        quitMenuItem.setText(bundle1.getString("JSymphonicWindow.quitMenuItem.text")); // NOI18N
        quitMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.quitMenuItem.text")); // NOI18N
        quitMenuItem.setName("quitMenuItem"); // NOI18N
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        jsymphonicMenu.add(quitMenuItem);

        jMenuBar1.add(jsymphonicMenu);

        transferMenu.setText(bundle1.getString("JSymphonicWindow.transferMenu.text")); // NOI18N
        transferMenu.setActionCommand(bundle1.getString("JSymphonicWindow.transferMenu.text")); // NOI18N
        transferMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        transferMenu.setName("transferMenu"); // NOI18N

        importMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.ALT_MASK));
        importMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        importMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/import.png"))); // NOI18N
        importMenuItem.setText(bundle1.getString("JSymphonicWindow.importMenuItem.text")); // NOI18N
        importMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.importMenuItem.text")); // NOI18N
        importMenuItem.setName("importMenuItem"); // NOI18N
        importMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMenuItemActionPerformed(evt);
            }
        });
        transferMenu.add(importMenuItem);

        exportMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.ALT_MASK));
        exportMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        exportMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/export.png"))); // NOI18N
        exportMenuItem.setText(bundle1.getString("JSymphonicWindow.exportMenuItem.text")); // NOI18N
        exportMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.exportMenuItem.text")); // NOI18N
        exportMenuItem.setName("exportMenuItem"); // NOI18N
        exportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMenuItemActionPerformed(evt);
            }
        });
        transferMenu.add(exportMenuItem);

        deleteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.ALT_MASK));
        deleteMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        deleteMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/remove.png"))); // NOI18N
        deleteMenuItem.setText(bundle.getString("global.Delete")); // NOI18N
        deleteMenuItem.setName("deleteMenuItem"); // NOI18N
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItemActionPerformed(evt);
            }
        });
        transferMenu.add(deleteMenuItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        transferMenu.add(jSeparator1);

        applyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.ALT_MASK));
        applyMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        applyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/save.png"))); // NOI18N
        applyMenuItem.setText(bundle1.getString("JSymphonicWindow.Apply_all_changes")); // NOI18N
        applyMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.Apply_all_changes")); // NOI18N
        applyMenuItem.setName("applyMenuItem"); // NOI18N
        applyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyMenuItemActionPerformed(evt);
            }
        });
        transferMenu.add(applyMenuItem);

        revertMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK));
        revertMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        revertMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/revert.png"))); // NOI18N
        revertMenuItem.setText(bundle1.getString("JSymphonicWindow.revertMenuItem.text")); // NOI18N
        revertMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.revertMenuItem.text")); // NOI18N
        revertMenuItem.setName("revertMenuItem"); // NOI18N
        revertMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                revertMenuItemActionPerformed(evt);
            }
        });
        transferMenu.add(revertMenuItem);

        jMenuBar1.add(transferMenu);

        helpMenu.setText(bundle.getString("global.Help")); // NOI18N
        helpMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        helpMenu.setName("helpMenu"); // NOI18N

        helpMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.ALT_MASK));
        helpMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        helpMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/help.png"))); // NOI18N
        helpMenuItem.setText(bundle.getString("global.Help")); // NOI18N
        helpMenuItem.setName("helpMenuItem"); // NOI18N
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpMenuItem);

        logMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_MASK));
        logMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        logMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/run.png"))); // NOI18N
        logMenuItem.setText(bundle1.getString("JSymphonicWindow.logMenuItem.text")); // NOI18N
        logMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.logMenuItem.text")); // NOI18N
        logMenuItem.setName("logMenuItem"); // NOI18N
        logMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(logMenuItem);

        aboutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK));
        aboutMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
        aboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/about.png"))); // NOI18N
        aboutMenuItem.setText(bundle1.getString("JSymphonicWindow.About_JSymphonic")); // NOI18N
        aboutMenuItem.setActionCommand(bundle1.getString("JSymphonicWindow.About_JSymphonic")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        jMenuBar1.add(helpMenu);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainToolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 742, Short.MAX_VALUE)
            .add(mainPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(mainToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(mainPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void propertiesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_propertiesButtonActionPerformed
        JSymphonicProperties jsp = new JSymphonicProperties(sHandler, this);
        jsp.setLocationRelativeTo(this);
        jsp.setVisible(true);
}//GEN-LAST:event_propertiesButtonActionPerformed

private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
    importMenuItemActionPerformed(evt);
}//GEN-LAST:event_importButtonActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
    JSymphonicAbout ab = new JSymphonicAbout(java.util.ResourceBundle.getBundle("localization/jsymphonicwindow").getString("JSymphonicWindow.About_JSymphonic"), java.util.ResourceBundle.getBundle("localization/misc").getString("global.version"),java.util.ResourceBundle.getBundle("localization/jsymphonicwindow").getString("JSymphonicWindow.Thanks_for_using"), java.util.ResourceBundle.getBundle("localization/jsymphonicwindow").getString("JSymphonicWindow.The_JSymphonic_team"));
    ab.setLocationRelativeTo(this);
    ab.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed

private void propertiesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_propertiesMenuItemActionPerformed
    propertiesButtonActionPerformed(evt);
}//GEN-LAST:event_propertiesMenuItemActionPerformed

private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuItemActionPerformed
    System.exit(0);
}//GEN-LAST:event_quitMenuItemActionPerformed

private void importMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importMenuItemActionPerformed
       devicePanel.scheduleTrackImport(localPanel.getSelectedTracks());
}//GEN-LAST:event_importMenuItemActionPerformed

private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMenuItemActionPerformed
       devicePanel.scheduleTrackExport();
       devicePanel.reloadTree();
}//GEN-LAST:event_exportMenuItemActionPerformed

private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
    exportMenuItemActionPerformed(evt);
}//GEN-LAST:event_exportButtonActionPerformed

private void revertButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_revertButtonActionPerformed
    revertMenuItemActionPerformed(evt);
}//GEN-LAST:event_revertButtonActionPerformed

private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonActionPerformed
    applyMenuItemActionPerformed(evt);
}//GEN-LAST:event_applyButtonActionPerformed

private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
    deleteMenuItemActionPerformed(evt);
}//GEN-LAST:event_deleteButtonActionPerformed

private void deleteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteMenuItemActionPerformed
    devicePanel.deleteSelectedTracks();
    devicePanel.reloadTree();
}//GEN-LAST:event_deleteMenuItemActionPerformed

private void applyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyMenuItemActionPerformed
    if(JOptionPane.showConfirmDialog(this, java.util.ResourceBundle.getBundle("localization/jsymphonicwindow").getString("JSymphonicWindow.Do_you_want_to_apply_all_changes"), java.util.ResourceBundle.getBundle("localization/jsymphonicwindow").getString("JSymphonicWindow.Apply_all_changes"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
        devicePanel.applyChanges();
    }
}//GEN-LAST:event_applyMenuItemActionPerformed

private void revertMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_revertMenuItemActionPerformed
    devicePanel.cancelChanges();
}//GEN-LAST:event_revertMenuItemActionPerformed

private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
    HelpFrame hlpF;
    File localREADME = new File("./README_v0.3.0b.html");

    // Try to find the local file
    if(localREADME.exists()){
        // If it exist, use it
        hlpF = new HelpFrame("file:"+File.separator+File.separator+File.separator+localREADME.getAbsolutePath().replace(File.separator+".", ""));    }
    else{
        // Else, use the online doc
        hlpF = new HelpFrame("http://symphonic.sourceforge.net/e107_themes/images/documentation/help.html");
    }
    hlpF.setLocationRelativeTo(this);
    hlpF.setVisible(true);
}//GEN-LAST:event_helpMenuItemActionPerformed

private void logMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logMenuItemActionPerformed
   LogFrame lf = new LogFrame(System.getProperty("user.dir") + System.getProperty("file.separator") + logFileName, glHandler);
   if(lf.isSourceValid()){
       lf.setLocationRelativeTo(this);
       lf.setVisible(true);
   }else{
       lf = null;
   }
}//GEN-LAST:event_logMenuItemActionPerformed

private void profilesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profilesComboBoxActionPerformed
    if(profilesAreReady){
        loadNewConfig();
    }
    comboBoxAlreadyClicked = false;
}//GEN-LAST:event_profilesComboBoxActionPerformed

private void profilesComboBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_profilesComboBoxMouseClicked
//    if(comboBoxAlreadyClicked){
    //    if(profilesAreReady){
      //      loadNewConfig();
        //}
  //      comboBoxAlreadyClicked = false;
  //  }
    //else{
      //  comboBoxAlreadyClicked = true;
//    }
}//GEN-LAST:event_profilesComboBoxMouseClicked
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton applyButton;
    private javax.swing.JMenuItem applyMenuItem;
    private javax.swing.JButton deleteButton;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JButton exportButton;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JButton importButton;
    private javax.swing.JMenuItem importMenuItem;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JMenu jsymphonicMenu;
    private javax.swing.JMenuItem logMenuItem;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JToolBar mainToolBar;
    private javax.swing.JComboBox profilesComboBox;
    private javax.swing.JButton propertiesButton;
    private javax.swing.JMenuItem propertiesMenuItem;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JButton revertButton;
    private javax.swing.JMenuItem revertMenuItem;
    private javax.swing.JMenu transferMenu;
    // End of variables declaration//GEN-END:variables

    public TreeMap getThemeMap() {
        return themeMap;
    }

    public static TreeMap getLangMap() {
        return langMap;
    }

    public void setTransferState(boolean transfering){
            mainToolBar.setEnabled(!transfering);
            importButton.setEnabled(!transfering);
            exportButton.setEnabled(!transfering);
            deleteButton.setEnabled(!transfering);
            applyButton.setEnabled(!transfering);
            revertButton.setEnabled(!transfering);
            propertiesButton.setEnabled(!transfering);
            profilesComboBox.setEnabled(!transfering);
            jMenuBar1.setEnabled(!transfering);
            jsymphonicMenu.setEnabled(!transfering);
            transferMenu.setEnabled(!transfering);
            if(!transfering)
                exportLocalPanel.reloadTree();
    }

    /**
     * @return the logFormatter
     */
    public Formatter getLogFormatter() {
        return logFormatter;
    }
    
}
