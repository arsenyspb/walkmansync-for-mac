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
 * SettingsHandler.java
 *
 */

package org.danizmax.jsymphonic.gui;

import org.danizmax.jsymphonic.toolkit.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * It handles operations such as setting, getting, saving and loading of settings values.
 * @author  danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class SettingsHandler extends DefaultHandler{
    private static Logger logger = Logger.getLogger("org.danizmax.jsymphonic.SettingsHandler");   
    private File configFile;
    String currQualifiedName = null;
    
    //window settings
    private int windowPositionX = 100;
    private int windowPositionY = 100;
    private int windowWidth = 640;
    private int windowHeight = 480;
    
    //other
    private HashMap profiles = new HashMap();
    private String language = "English (default)";
    private String theme = "Windows";
    private String selectedProfile = "Default";
    private String logLevel = "ALL";
    private boolean LogToFile = false;
    private String devicePath = "";
    private int generation = 0;
    
    //transfer
    private int bitrate = 128;
    private boolean alwaysTrascode = false;
    private boolean readID3Tags = true;
    private String readTagFolderStructure = "";
    
    public SettingsHandler(String configFile){
        this.configFile = new File(configFile);
        if(this.configFile.exists()){
            readXMLFile();
        }else{
            // Config file doesn't exist, create one
            getLogger().warning("File" + " " + configFile + " " + "does not exist!, Creating new default config file");

            // Create a default config
            createDefaultConfig();
            // and save it
            writeXMLFile();
        }
    }

    /**
     * Set the default values for all variables
     */
    public void createDefaultConfig(){
        // Profile name
        selectedProfile = "Default";
        
        // Default window size and position
        windowPositionX = 100;
        windowPositionY = 100;
        windowWidth = 640;
        windowHeight = 480;

        // First, guess the language
        // Default language is english
        language = "English (default)";
        // Try to load the language corresponding to the one of the user
        Map langMap = JSymphonicWindow.getLangMap(); // Get available languages
        if(langMap.containsValue(Locale.getDefault())){
            // If user's language is available, use it
            // Problem is that we don't know what key corresponds to the wanted locale, so
            // Get all keys in a set
            Set langName = langMap.keySet();
            // Search throught the keys the one which give the correct locale
            Iterator it = langName.iterator();
            while(it.hasNext()){
                String tempLanguage = (String)it.next();
                if( ((Locale)langMap.get(tempLanguage)).toString().compareTo(( Locale.getDefault().toString())) == 0){
                    // If current key is associated whit the user's language, use it as default language
                    language = tempLanguage;
                }
            }
        }

        // Default scheme for reading info from file/folders is "Artist/Album/Title"
        ResourceBundle jspropBundle = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties");
        readTagFolderStructure = "<"+jspropBundle.getString("JSymphonicProperties.Artist")+">/<"+jspropBundle.getString("JSymphonicProperties.Album")+">/<"+jspropBundle.getString("JSymphonicProperties.Title")+">";

        // Default theme is "Metal". If "Windows" theme is available, choose it.
        theme = "Metal";
        // Get LAF info from the system
        UIManager.LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
        // Get java version
        int javaVersion = Integer.parseInt(System.getProperty("java.version").substring(2,3)); // Gives an answer like "5" for java 1.5 or "6" for java 1.6
        // If java version is more than 6, Windows may be available, if yes, choose it.
        if(javaVersion >= 6){
            for(int i = 0; i < lafInfo.length; i++ ) {
                if(lafInfo[i].getName().contains("Windows")) {
                    theme = "Windows";
                }
            }
        }
        
        // Search for the device path
        // Search through a list of paths
        List devicePathsList = new ArrayList();
        // Relative Paths:
        devicePathsList.add(".");
        devicePathsList.add("..");
        devicePathsList.add("../..");
        // Linux and Mac possible Paths:
        if(System.getProperty("os.name").toLowerCase().contains("linux") || System.getProperty("os.name").toLowerCase().contains("mac")){
            devicePathsList.add("/media/Walkman");
            devicePathsList.add("/media/walkman");
            devicePathsList.add("/media/WALKMAN");
            devicePathsList.add("/media/disk");
            devicePathsList.add("/mnt/Walkman");
            devicePathsList.add("/mnt/walkman");
            devicePathsList.add("/mnt/WALKMAN");
            devicePathsList.add("/mnt/disk");
        }
        // Windows possible Paths:
        if(System.getProperty("os.name").toLowerCase().contains("windows")){
            devicePathsList.add("D:");
            devicePathsList.add("E:");
            devicePathsList.add("F:");
            devicePathsList.add("G:");
            devicePathsList.add("H:");
            devicePathsList.add("I:");
            devicePathsList.add("J:");
            devicePathsList.add("K:");
            devicePathsList.add("L:");
        }
        // Search through all these paths using an iterator
        Iterator itDevicePath = devicePathsList.iterator();

        while(devicePath.length() == 0 && itDevicePath.hasNext()){
            String testPathString = (String)itDevicePath.next();
            File omgaudioPathUpper = new File(testPathString + "/OMGAUDIO");
            File omgaudioPathLower = new File(testPathString + "/omgaudio");
            File esysPathUpper = new File(testPathString + "/ESYS");
            File esysPathLower = new File(testPathString + "/esys");
            if(omgaudioPathUpper.exists() || omgaudioPathLower.exists() || esysPathUpper.exists() || esysPathLower.exists()){
                // If something is found, use it
                devicePath = testPathString;
            }
        }

        // Finally, if nothing has been found, set to a default unknown value
        if(devicePath.length() == 0){
            devicePath = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.invalid_path");
        }

        // Search for local and export path
        // Search through a list of paths
        List localPathsList = new ArrayList();
        // Get user Home path
        String userHome = System.getProperty("user.home");
        // Define a list of paths
        // ~/My documents/My music
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.my_documents") + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.my_music"));
        // ~/My documents/Music
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.my_documents") + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.music"));
        // ~/Documents/My music
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.documents") + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.my_music"));
        // ~/Documents/Music
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.documents") + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.music"));
        // ~/My music
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.my_music"));
        // ~/Music
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.music"));
        // ~/My documents
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.my_documents"));
        // ~/Documents
        localPathsList.add(userHome + "/" + java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.documents"));
        // Search through all these paths using an iterator
        Iterator itLocalPath = localPathsList.iterator();
        String localPath = "";

        while(itLocalPath.hasNext() && localPath.length() == 0){
            String testPathString = (String)itLocalPath.next();
            File testPath = new File(testPathString);
            if(testPath.exists()){
                // If something is found, use it
                localPath = testPathString;
            }
        }

        // Finally, if nothing has been found, set default to the home folder
        if(localPath.length() == 0){
            localPath = userHome;
        }

        // Then ask the user the generation and device
        JSymphonicFirstConfig device_config = new JSymphonicFirstConfig(this, devicePath);
        device_config.setVisible(true);
        try {
            synchronized (device_config) {
                // Wait for user's answer
                device_config.wait();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(SettingsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Export folder is taken the same as local folder
        // User home folder is taken as temporary folder to not use space on the device while transcoding

        // Set default profile with all default values
        profiles.put(selectedProfile, new ProfileElement(selectedProfile, generation, localPath, localPath, devicePath, userHome, false, false));
    }
    
        /** This method writes settings in XML format to a file. */
    public void writeXMLFile() {
        try {

            getLogger().info("Writing config file" + getSettingsFile().getCanonicalPath() + "... ");
            Document document;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {

                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.newDocument(); // Create from whole cloth
                //create root element
                Element JSymphonicSetting = (Element) document.createElement("JSymphonicSetting");
                document.appendChild(JSymphonicSetting);

                //Window Settings
                Element WindowSettings = (Element) document.createElement("WindowSettings");

                Element positionEl = (Element) document.createElement("Position");
                positionEl.setAttribute("PositionX", String.valueOf(getWindowPositionX()));
                positionEl.setAttribute("PositionY", String.valueOf(getWindowPositionY()));
                WindowSettings.appendChild(positionEl);

                Element sizeEl = (Element) document.createElement("Size");
                sizeEl.setAttribute("Width", String.valueOf(getWindowWidth()));
                sizeEl.setAttribute("Height", String.valueOf(getWindowHeight()));
                WindowSettings.appendChild(sizeEl);
                JSymphonicSetting.appendChild(WindowSettings);
                
                Element guiEl = (Element) document.createElement("GuiSettings");
                guiEl.setAttribute("Language", getLanguage());
                guiEl.setAttribute("Theme",getTheme()) ;
                JSymphonicSetting.appendChild(guiEl);
                
                Element levelEl = (Element) document.createElement("Logging");
                levelEl.setAttribute("Level", getLogLevel());
                levelEl.setAttribute("LogToFile",String.valueOf(isLogToFile())) ;
                JSymphonicSetting.appendChild(levelEl);
                
                Element transferEl = (Element) document.createElement("Transfer");
                transferEl.setAttribute("Bitrate", String.valueOf(getBitrate()));
                transferEl.setAttribute("AlwaysTranscode", String.valueOf(isAlwaysTrascode())) ;
                transferEl.setAttribute("ReadID3Tags", String.valueOf(isReadID3Tags())) ;
                transferEl.setAttribute("readTagFolderStructure", String.valueOf(getReadTagFolderStructure())) ;
                JSymphonicSetting.appendChild(transferEl);
                
                //profiles
                Element profilesEl = (Element) document.createElement("Profiles");
                profilesEl.setAttribute("Selected", getSelectedProfile());
                Set keys = profiles.keySet(); // The set of keys in the map.
                Iterator iter = keys.iterator();

                while (iter.hasNext()) {
                    Object key = iter.next();
                    Object val = profiles.get(key); 
                    Element profileEl = (Element) document.createElement("Profile");
                    profileEl.setAttribute("Name", (String) ((ProfileElement)val).getProfileName());
                    profileEl.setAttribute("DeviceGeneration", Integer.toString(((ProfileElement)val).getDeviceGeneration()));
                    profileEl.setAttribute("DevicePath", (String) ((ProfileElement)val).getDevicePath());
                    profileEl.setAttribute("LocalPath", (String) ((ProfileElement)val).getLocalPath());
                    profileEl.setAttribute("ExportPath", (String) ((ProfileElement)val).getExportPath());
                    profileEl.setAttribute("TranscodeTempPath", (String) ((ProfileElement)val).getTranscodeTempPath());
                    profileEl.setAttribute("TTPathIsSameAsDPath", (String) String.valueOf(((ProfileElement)val).isTempPathSameAsDevicePath()));
                    profileEl.setAttribute("ExPathIsSameAsLPath", (String) String.valueOf(((ProfileElement)val).isExportPathSameAsLocalPath()));
                    profilesEl.appendChild(profileEl);
                }

                JSymphonicSetting.appendChild(profilesEl);
                
                Source src = new DOMSource(document);
                StreamResult result = new StreamResult(configFile);
                Transformer xformer = TransformerFactory.newInstance().newTransformer();
                xformer.setOutputProperty(OutputKeys.INDENT, "yes");
                xformer.transform(src, result);
            } catch (ParserConfigurationException pce) {
                // Parser with specified options can't be built
                pce.printStackTrace();
            } catch (TransformerConfigurationException e) {
            } catch (TransformerException e) {
            }
            getLogger().info("Done writing file");
        } catch (IOException ex) {
            Logger.getLogger(SettingsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** This method reads settings from a XML file. */
    public void readXMLFile() {
        Document document;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        if (configFile.exists()) {
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(configFile);

                DefaultHandler handler = this;

                // Use the default (non-validating) parser
                SAXParserFactory saxFactory = SAXParserFactory.newInstance();
                try {
                    // Parse the input
                    SAXParser saxParser = saxFactory.newSAXParser();
                    saxParser.parse(configFile, handler);
                } catch (Throwable e) {
                    String msg = "Error parsing file:" + " "+ configFile.getCanonicalPath() + "! "+"\nProblem in node \"" + currQualifiedName + "\". Correct or delete the file and try again.";
                    getLogger().warning(msg);
                    JOptionPane.showMessageDialog(null, msg, "Error parsing file...",JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            } catch (SAXException e) {
                getLogger().warning(e.getMessage());
            } catch (ParserConfigurationException e) {
                // Parser with specified options can't be built
                getLogger().warning("Parser with specified options can't be built");
            } catch (IOException e) {
                // I/O error
                getLogger().warning("I/O error");
            }
        } else {
            try {
                getLogger().warning("File " + " " + configFile.getCanonicalPath() + " " + "does not exist!");
            } catch (IOException ex) {
                Logger.getLogger(SettingsHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void startDocument() throws SAXException {
        try {
            profiles = new HashMap();
            getLogger().info("Reading file" + " " + configFile.getCanonicalPath() + "...!");
        } catch (IOException ex) {
            Logger.getLogger(SettingsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        getLogger().info("Done reading file");
    }

    @Override
    public void startElement(String namespaceURI, String simpleName, String qualifiedName, Attributes attrs) throws SAXException {
        currQualifiedName = qualifiedName; //in case of error this variable will tell us where is the problem
        if (qualifiedName.equals("Position")) {
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    
 // Local name return an empty string   String atr = attrs.getLocalName(i);
                    String atr = attrs.getQName(i);
                    String val = attrs.getValue(i);

                    if (atr.equals("PositionX")) {
                        windowPositionX = Integer.valueOf(val);
                    } else if (atr.equals("PositionY")) {
                        windowPositionY = Integer.valueOf(val);
                    }
                }
            }
        }else if (qualifiedName.equals("Size")) {
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    
 // Local name return an empty string   String atr = attrs.getLocalName(i);
                    String atr = attrs.getQName(i);
                    String val = attrs.getValue(i);

                    if (atr.equals("Width")) {
                        windowWidth = Integer.valueOf(val);
                    } else if (atr.equals("Height")) {
                        windowHeight = Integer.valueOf(val);
                    }
                }
            }
        }else if (qualifiedName.equals("GuiSettings")) {
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    
 // Local name return an empty string   String atr = attrs.getLocalName(i);
                    String atr = attrs.getQName(i);
                    String val = attrs.getValue(i);

                    if (atr.equals("Language")) {
                        setLanguage(val);
                    } else if (atr.equals("Theme")) {
                        setTheme(val);
                    } 
                }
            }
        }else if (qualifiedName.equals("Logging")) {
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    
 // Local name return an empty string   String atr = attrs.getLocalName(i);
                    String atr = attrs.getQName(i);
                    String val = attrs.getValue(i);

                    if (atr.equals("Level")) {
                        setLogLevel(val);
                    }else if (atr.equals("LogToFile")) {
                        setLogToFile(Boolean.valueOf(val));
                    }
                }
            }
        }else if (qualifiedName.equals("Profiles")) {
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    
 // Local name return an empty string   String atr = attrs.getLocalName(i);
                    String atr = attrs.getQName(i);
                    String val = attrs.getValue(i); //TODO check that the value is well loaded (maybe a SAX bug?)

                    if (atr.equals("Selected")) {
                        setSelectedProfile(val);
                    }
                }
            }
        }else if (qualifiedName.equals("Transfer")) {
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    
 // Local name return an empty string   String atr = attrs.getLocalName(i);
                    String atr = attrs.getQName(i);
                    String val = attrs.getValue(i);

                    if (atr.equals("Bitrate")) {
                        setBitrate(Integer.valueOf(val));
                    } else if (atr.equals("AlwaysTranscode")) {
                        setAlwaysTrascode(Boolean.parseBoolean(val));
                    } else if (atr.equals("ReadID3Tags")) {
                        setReadID3Tags(Boolean.parseBoolean(val));
                    } else if (atr.equals("readTagFolderStructure")) {
                        setReadTagFolderStructure(val);
                    }
                    
                }
            }
        }else if (qualifiedName.equals("Profile")) {
            if (attrs != null) {
                String devicePath = null;
                int deviceGeneration = 0;
                String localPath = null;
                String exportPath = null;
                String profileName = null;
                String TranscodeTempPath = null;
                String  TTPathIsSameAsDPath = null;
                String  ExPathIsSameAsLPath = null;
                
                for (int i = 0; i < attrs.getLength(); i++) {

 // Local name return an empty string   String atr = attrs.getLocalName(i);
                    String atr = attrs.getQName(i);
                    String val = attrs.getValue(i);

                    if (atr.equals("Name")) {
                        profileName = val;
                    } else if (atr.equals("DevicePath")) {
                        devicePath = val;
                    } else if (atr.equals("LocalPath")) {
                        localPath = val;
                    }else if (atr.equals("ExportPath")) {
                        exportPath = val;
                    }else if (atr.equals("DeviceGeneration")) {
                        deviceGeneration = Integer.parseInt(val);
                    }else if (atr.equals("TranscodeTempPath")) {
                        TranscodeTempPath = val;
                    }else if (atr.equals("TTPathIsSameAsDPath")) {
                        TTPathIsSameAsDPath = val;
                    }else if (atr.equals("ExPathIsSameAsLPath")) {
                        ExPathIsSameAsLPath = val;
                    }

                    if ((devicePath != null) && (localPath != null) && (deviceGeneration != 0) && (profileName != null) && (TranscodeTempPath != null) && (TTPathIsSameAsDPath != null)) {
                        profiles.put(profileName, new ProfileElement(profileName, deviceGeneration, localPath, exportPath, devicePath, TranscodeTempPath, Boolean.parseBoolean(TTPathIsSameAsDPath), Boolean.parseBoolean(ExPathIsSameAsLPath)));
                        devicePath = null;
                        deviceGeneration = 0;
                        localPath = null;
                        exportPath = null;
                        profileName = null;
                        TranscodeTempPath = null;
                    }
                }
            }
        }else if(qualifiedName.equals("JSymphonicSetting") || qualifiedName.equals("WindowSettings") ){
             //getLogger().warning("Found root node: " + qualifiedName);
        }else{
            getLogger().warning("Unknown option: " + qualifiedName);
        }
    }

    @Override
    public void endElement(String namespaceURI, String sName, // simple name
            String qName) throws SAXException {
    //  emit("</"+sName+">");
    }    
    
    public void addProfile(ProfileElement profile){
        profiles.put(profile.getProfileName(), profile);
    }
      
    public void removeProfile(String profileName){
        profiles.remove(profileName);
    }
    
    public void clearProfiles(){
        profiles.clear();
    }
 
    public File getSettingsFile() {
        return configFile;
    }

    public void setSettingsFile(File settingsFile) {
        this.configFile = settingsFile;
    }

    public int getWindowPositionX() {
        return windowPositionX;
    }

    public void setWindowPositionX(int windowPositionX) {
        this.windowPositionX = windowPositionX;
    }

    public int getWindowPositionY() {
        return windowPositionY;
    }

    public void setWindowPositionY(int windowPositionY) {
        this.windowPositionY = windowPositionY;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public HashMap getProfiles() {
        return profiles;
    }

    public void setProfiles(HashMap profiles) {
        this.profiles = profiles;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public boolean isAlwaysTrascode() {
        return alwaysTrascode;
    }

    public void setAlwaysTrascode(boolean alwaysTrascode) {
        this.alwaysTrascode = alwaysTrascode;
    }
    
     public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger aLogger) {
        logger = aLogger;
    }
    
    public static void setParentLogger(Logger aLogger) {
        logger.setParent(aLogger);
    }

    public boolean isReadID3Tags() {
        return readID3Tags;
    }

    public void setReadID3Tags(boolean readID3Tags) {
        this.readID3Tags = readID3Tags;
    }

    public String getReadTagFolderStructure() {
        return readTagFolderStructure;
    }

    public void setReadTagFolderStructure(String readTagFolderStructure) {
        this.readTagFolderStructure = readTagFolderStructure;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isLogToFile() {
        return LogToFile;
    }

    public void setLogToFile(boolean LogToFile) {
        this.LogToFile = LogToFile;
    }

    public void setDevicePath(String devicePath) {
        this.devicePath = devicePath;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
