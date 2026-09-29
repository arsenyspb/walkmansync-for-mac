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
 * JSymphonicProperties.java
 *
 * Created on April 24, 2008, 6:41 PM
 * 
 */

package org.danizmax.jsymphonic.gui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.danizmax.jsymphonic.toolkit.ProfileElement;
import org.danizmax.jsymphonic.gui.device.DeviceManager;
import org.naurd.media.jsymphonic.device.sony.nw.NWGeneric;
import org.naurd.media.jsymphonic.toolBox.FFMpegToolBox;

/**
 * This class is a JFrame for changing JSymphonic settings
 * @author  danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class JSymphonicProperties extends javax.swing.JFrame {
    
    private SettingsHandler sHandler = null;
    private TreeMap themeMap = null;
    private HashMap profiles = null;
    private JSymphonicWindow mainWindow = null;
    private HashMap generationMap;
    
        
    
    /** Creates new form JSymphonicProperties */
    public JSymphonicProperties(SettingsHandler sh, JSymphonicWindow mainWindow) {
        mainWindow.setEnabled(false);
        //device generations
        generationMap = NWGeneric.getGenerationMap();

        sHandler = sh;
        this.mainWindow = mainWindow;
        initComponents();
        initConstantValues();
        //load profiles
        profiles = sHandler.getProfiles();
        loadProfiles();
     //   updateSelectedProfileData(profiles, sHandler.getSelectedProfile());
        profileComboBox.setSelectedItem(sHandler.getSelectedProfile());
        
        //gui settings
        themeComboBox.setSelectedItem(sHandler.getTheme());
        languageComboBox.setSelectedItem(sHandler.getLanguage());
        logLevelComboBox.setSelectedItem(sHandler.getLogLevel());
        logPanelCheckBox.setSelected(sHandler.isLogToFile());
        
        //transfer
        bitrateComboBox.setSelectedItem(sHandler.getBitrate() + " kbps");
        alwaysTranscodeRadioButton.setSelected(sHandler.isAlwaysTrascode());
        alwaysTranscodeRadioButton.setEnabled(FFMpegToolBox.isFFMpegPresent());
        onlyNeededTranscodeRadioButton.setSelected(!sHandler.isAlwaysTrascode());
        readTagsRadioButton.setSelected(sHandler.isReadID3Tags());
        neverReadTagsRadioButton.setSelected(!sHandler.isReadID3Tags());
        // Manage the tag read patterns combo box
        String tryReadTagInfoPattern = sHandler.getReadTagFolderStructure(); // get the saved value
        tagReadStructureComboBox.removeItem(tryReadTagInfoPattern); // remove it from the combo box (if it doesn't exist, nothing will be done, by deleting it before adding, it prevent from appearing twice)
        tagReadStructureComboBox.addItem(tryReadTagInfoPattern); // add it
        tagReadStructureComboBox.setSelectedItem(tryReadTagInfoPattern); // select it

        // Write text in the pattern explanation text area
        patternExplanationTextArea.append("\n - " +
                java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.pattern_explanation_rule1")+
                "\n - " +
                java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.pattern_explanation_rule2")+
                "\n - " +
                java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.pattern_explanation_rule3")+
                "\n - " +
                java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.pattern_explanation_rule4")
                );
    }

    private void ShowSelectedProfileData(HashMap profiles, String selectedProfile) {
        if(profiles.containsKey(selectedProfile)){ 
            if(profiles != null && selectedProfile != null){
                ProfileElement sel = (ProfileElement) profiles.get(selectedProfile);
                if(sel != null){
                    localPathLabelData.setText(sel.getLocalPath());
                    if(sel.isExportPathSameAsLocalPath()){
                        exportPathLabelData.setText(sel.getLocalPath());
                    }else{
                        exportPathLabelData.setText(sel.getExportPath());
                    }
                    
                    devPathLabelData.setText(sel.getDevicePath());
                    genPathLabelData.setText((String) generationMap.get(sel.getDeviceGeneration()));
                    if(sel.isTempPathSameAsDevicePath()){
                        tempPathLabelData.setText(sel.getDevicePath());
                    }else{
                        tempPathLabelData.setText(sel.getTranscodeTempPath());
                    }
                }
            }
        }
    }

    
    /**
     * Loasd profiles into profiles combobox
     */
    private void loadProfiles(){
        Set keys = profiles.keySet();
        Iterator iter = keys.iterator();
        profileComboBox.removeAllItems();
        
        while (iter.hasNext()) {
            profileComboBox.addItem((String)iter.next());
        }
        profileComboBox.setSelectedItem(((ProfileElement) profiles.get(sHandler.getSelectedProfile())).getProfileName());
    }
          
    /***
     * This method populates comboboxes that do not change and creates Hasmaps for usage
     */
    private void initConstantValues(){
        themeComboBox.removeAllItems();
        languageComboBox.removeAllItems();
        
        //language
        TreeMap langs = JSymphonicWindow.getLangMap();
        Set keys = langs.keySet();
        Iterator iter = keys.iterator();
        languageComboBox.removeAllItems();
        while (iter.hasNext()){
            languageComboBox.addItem(iter.next());
        }
        
        //themes
        themeMap = mainWindow.getThemeMap();
        keys = themeMap.keySet();
        iter = keys.iterator();
           
        while (iter.hasNext()){
            themeComboBox.addItem(iter.next());
        }

        // Patterns to read tag info from folders and file
        ResourceBundle jspropBundle = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties");
        // <artist>/<year>-<album>/<trackNumber>-<title>
        tagReadStructureComboBox.addItem("<"+jspropBundle.getString("JSymphonicProperties.Artist")+">/<"+jspropBundle.getString("JSymphonicProperties.Year")+">-<"+jspropBundle.getString("JSymphonicProperties.Album")+">/<"+jspropBundle.getString("JSymphonicProperties.TrackNumber")+">-<"+jspropBundle.getString("JSymphonicProperties.Title")+">");
        // <artist> - <album>/<trackNumber>.<title>
        tagReadStructureComboBox.addItem("<"+jspropBundle.getString("JSymphonicProperties.Artist")+"> - <"+jspropBundle.getString("JSymphonicProperties.Album")+">/<"+jspropBundle.getString("JSymphonicProperties.TrackNumber")+">.<"+jspropBundle.getString("JSymphonicProperties.Title")+">");
        // <genre>/<artist>-<album> [<year>]/<title>
        tagReadStructureComboBox.addItem("<"+jspropBundle.getString("JSymphonicProperties.Genre")+">/<"+jspropBundle.getString("JSymphonicProperties.Artist")+">-<"+jspropBundle.getString("JSymphonicProperties.Album")+"> [<"+jspropBundle.getString("JSymphonicProperties.Year")+">]/<"+jspropBundle.getString("JSymphonicProperties.Title")+">");
    }

    @Override
    public void dispose() {
        mainWindow.setEnabled(true);
        super.dispose();
    }


    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        transcodebuttonGroup = new javax.swing.ButtonGroup();
        tagUtilGroup = new javax.swing.ButtonGroup();
        cancelButton = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        pathPanel = new javax.swing.JPanel();
        profileComboBox = new javax.swing.JComboBox();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        profileDataPanel = new javax.swing.JPanel();
        devPathLabel = new javax.swing.JLabel();
        localPathLabel = new javax.swing.JLabel();
        exportPathLabel = new javax.swing.JLabel();
        tempPathLabel = new javax.swing.JLabel();
        genPathLabel = new javax.swing.JLabel();
        genPathLabelData = new javax.swing.JLabel();
        tempPathLabelData = new javax.swing.JLabel();
        exportPathLabelData = new javax.swing.JLabel();
        localPathLabelData = new javax.swing.JLabel();
        devPathLabelData = new javax.swing.JLabel();
        editButton = new javax.swing.JButton();
        noteProfileLabel = new javax.swing.JLabel();
        interfacePanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        languageComboBox = new javax.swing.JComboBox();
        noteLangLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        themeComboBox = new javax.swing.JComboBox();
        logLevelPanel = new javax.swing.JPanel();
        logLevelComboBox = new javax.swing.JComboBox();
        logPanelCheckBox = new javax.swing.JCheckBox();
        transferPanel = new javax.swing.JPanel();
        mp3OptionsPanel = new javax.swing.JPanel();
        jPanelSpacer8 = new javax.swing.JPanel();
        bitrateLabel = new javax.swing.JLabel();
        bitrateComboBox = new javax.swing.JComboBox();
        onlyNeededTranscodeRadioButton = new javax.swing.JRadioButton();
        alwaysTranscodeRadioButton = new javax.swing.JRadioButton();
        tagutilPanel = new javax.swing.JPanel();
        readTagsRadioButton = new javax.swing.JRadioButton();
        neverReadTagsRadioButton = new javax.swing.JRadioButton();
        usePatternLabel = new javax.swing.JLabel();
        tagReadStructureComboBox = new javax.swing.JComboBox();
        patternExplanationTextArea = new javax.swing.JTextArea();
        applyButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties"); // NOI18N
        setTitle(bundle.getString("JSymphonicProperties.Properties")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        cancelButton.setFont(new java.awt.Font("Dialog", 0, 12));
        cancelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/cancel.png"))); // NOI18N
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("localization/misc"); // NOI18N
        cancelButton.setText(bundle1.getString("global.Cancel")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setFont(new java.awt.Font("Dialog", 0, 12));
        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        pathPanel.setName("pathPanel"); // NOI18N

        profileComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        profileComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        profileComboBox.setName("profileComboBox"); // NOI18N
        profileComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                profileComboBoxActionPerformed(evt);
            }
        });

        addButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/add.png"))); // NOI18N
        addButton.setText(bundle.getString("JSymphonicProperties.addButton.text")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        removeButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/remove.png"))); // NOI18N
        removeButton.setText(bundle.getString("JSymphonicProperties.removeButton.text")); // NOI18N
        removeButton.setName("removeButton"); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        profileDataPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        profileDataPanel.setName("profileDataPanel"); // NOI18N

        devPathLabel.setText(bundle.getString("JSymphonicProperties.devPathLabel.text")); // NOI18N
        devPathLabel.setName("devPathLabel"); // NOI18N

        localPathLabel.setText(bundle.getString("JSymphonicProperties.localPathLabel.text")); // NOI18N
        localPathLabel.setName("localPathLabel"); // NOI18N

        exportPathLabel.setText(bundle.getString("JSymphonicProperties.exportPathLabel.text")); // NOI18N
        exportPathLabel.setName("exportPathLabel"); // NOI18N

        tempPathLabel.setText(bundle.getString("JSymphonicProperties.tempPathLabel.text")); // NOI18N
        tempPathLabel.setName("tempPathLabel"); // NOI18N

        genPathLabel.setText(bundle.getString("JSymphonicProperties.genPathLabel.text")); // NOI18N
        genPathLabel.setName("genPathLabel"); // NOI18N

        genPathLabelData.setFont(new java.awt.Font("Dialog", 0, 12));
        genPathLabelData.setText(bundle.getString("JSymphonicProperties.none")); // NOI18N
        genPathLabelData.setName("genPathLabelData"); // NOI18N

        tempPathLabelData.setFont(new java.awt.Font("Dialog", 0, 12));
        tempPathLabelData.setText(bundle.getString("JSymphonicProperties.none")); // NOI18N
        tempPathLabelData.setName("tempPathLabelData"); // NOI18N

        exportPathLabelData.setFont(new java.awt.Font("Dialog", 0, 12));
        exportPathLabelData.setText(bundle.getString("JSymphonicProperties.none")); // NOI18N
        exportPathLabelData.setName("exportPathLabelData"); // NOI18N

        localPathLabelData.setFont(new java.awt.Font("Dialog", 0, 12));
        localPathLabelData.setText(bundle.getString("JSymphonicProperties.none")); // NOI18N
        localPathLabelData.setName("localPathLabelData"); // NOI18N

        devPathLabelData.setFont(new java.awt.Font("Dialog", 0, 12));
        devPathLabelData.setText(bundle.getString("JSymphonicProperties.none")); // NOI18N
        devPathLabelData.setName("devPathLabelData"); // NOI18N

        org.jdesktop.layout.GroupLayout profileDataPanelLayout = new org.jdesktop.layout.GroupLayout(profileDataPanel);
        profileDataPanel.setLayout(profileDataPanelLayout);
        profileDataPanelLayout.setHorizontalGroup(
            profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(profileDataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, genPathLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, tempPathLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, exportPathLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, localPathLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, devPathLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(localPathLabelData, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE)
                    .add(exportPathLabelData, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE)
                    .add(genPathLabelData, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, tempPathLabelData, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE)
                    .add(devPathLabelData, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        profileDataPanelLayout.setVerticalGroup(
            profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(profileDataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(devPathLabel)
                    .add(devPathLabelData))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(localPathLabel)
                    .add(localPathLabelData))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(exportPathLabel)
                    .add(exportPathLabelData))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tempPathLabel)
                    .add(tempPathLabelData))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(profileDataPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(genPathLabel)
                    .add(genPathLabelData))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        editButton.setFont(new java.awt.Font("Dialog", 0, 12));
        editButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/edit.png"))); // NOI18N
        editButton.setText(bundle.getString("JSymphonicProperties.editButton.text")); // NOI18N
        editButton.setName("editButton"); // NOI18N
        editButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editButtonActionPerformed(evt);
            }
        });

        noteProfileLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        noteProfileLabel.setText(bundle.getString("JSymphonicProperties.noteProfileLabel.text")); // NOI18N
        noteProfileLabel.setName("noteProfileLabel"); // NOI18N

        org.jdesktop.layout.GroupLayout pathPanelLayout = new org.jdesktop.layout.GroupLayout(pathPanel);
        pathPanel.setLayout(pathPanelLayout);
        pathPanelLayout.setHorizontalGroup(
            pathPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pathPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(pathPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(profileComboBox, 0, 625, Short.MAX_VALUE)
                    .add(noteProfileLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 471, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, pathPanelLayout.createSequentialGroup()
                        .add(removeButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 121, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(addButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editButton))
                    .add(profileDataPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE))
                .addContainerGap())
        );

        pathPanelLayout.linkSize(new java.awt.Component[] {addButton, editButton, removeButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        pathPanelLayout.setVerticalGroup(
            pathPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pathPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(profileComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(profileDataPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pathPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(editButton)
                    .add(addButton)
                    .add(removeButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 201, Short.MAX_VALUE)
                .add(noteProfileLabel)
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("JSymphonicProperties.pathPanel.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/device.png")), pathPanel); // NOI18N

        interfacePanel.setName("interfacePanel"); // NOI18N

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("JSymphonicProperties.language"))); // NOI18N
        jPanel2.setName("jPanel2"); // NOI18N

        languageComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        languageComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        languageComboBox.setName("languageComboBox"); // NOI18N

        noteLangLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        noteLangLabel.setText(bundle.getString("JSymphonicProperties.noteLangLabel.text")); // NOI18N
        noteLangLabel.setName("noteLangLabel"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(languageComboBox, 0, 591, Short.MAX_VALUE)
                    .add(noteLangLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 561, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(languageComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(noteLangLabel)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("JSymphonicProperties.theme"))); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N

        themeComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        themeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        themeComboBox.setName("themeComboBox"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(themeComboBox, 0, 591, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(themeComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        logLevelPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("JSymphonicProperties.logLevelPanel.border.title"))); // NOI18N
        logLevelPanel.setName("logLevelPanel"); // NOI18N

        logLevelComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        logLevelComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST ", "ALL" }));
        logLevelComboBox.setName("logLevelComboBox"); // NOI18N

        logPanelCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        logPanelCheckBox.setText(bundle.getString("JSymphonicProperties.log_to_file")); // NOI18N
        logPanelCheckBox.setName("logPanelCheckBox"); // NOI18N
        logPanelCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logPanelCheckBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout logLevelPanelLayout = new org.jdesktop.layout.GroupLayout(logLevelPanel);
        logLevelPanel.setLayout(logLevelPanelLayout);
        logLevelPanelLayout.setHorizontalGroup(
            logLevelPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(logLevelPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(logLevelPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(logLevelPanelLayout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(logPanelCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE))
                    .add(logLevelComboBox, 0, 591, Short.MAX_VALUE))
                .addContainerGap())
        );
        logLevelPanelLayout.setVerticalGroup(
            logLevelPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(logLevelPanelLayout.createSequentialGroup()
                .add(logLevelComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(logPanelCheckBox)
                .addContainerGap(17, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout interfacePanelLayout = new org.jdesktop.layout.GroupLayout(interfacePanel);
        interfacePanel.setLayout(interfacePanelLayout);
        interfacePanelLayout.setHorizontalGroup(
            interfacePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(interfacePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(interfacePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, logLevelPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        interfacePanelLayout.setVerticalGroup(
            interfacePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(interfacePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(logLevelPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(155, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("JSymphonicProperties.interfacePanel.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/gui.png")), interfacePanel); // NOI18N

        transferPanel.setName("transferPanel"); // NOI18N

        mp3OptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("JSymphonicProperties.mp3OptionsPanel.border.title"))); // NOI18N
        mp3OptionsPanel.setMaximumSize(new java.awt.Dimension(410, 420));
        mp3OptionsPanel.setMinimumSize(new java.awt.Dimension(410, 420));
        mp3OptionsPanel.setName("mp3OptionsPanel"); // NOI18N

        jPanelSpacer8.setMaximumSize(new java.awt.Dimension(10, 10));
        jPanelSpacer8.setName("jPanelSpacer8"); // NOI18N
        jPanelSpacer8.setLayout(new javax.swing.BoxLayout(jPanelSpacer8, javax.swing.BoxLayout.LINE_AXIS));

        bitrateLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        bitrateLabel.setText(bundle.getString("JSymphonicProperties.bitrateLabel.text")); // NOI18N
        bitrateLabel.setName("bitrateLabel"); // NOI18N

        bitrateComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        bitrateComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "32 kbps", "48 kbps", "64 kbps", "96 kbps", "128 kbps", "160 kbps", "192 kbps", "256 kbps", "320 kbps" }));
        bitrateComboBox.setName("bitrateComboBox"); // NOI18N
        bitrateComboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                bitrateComboBoxMouseClicked(evt);
            }
        });
        bitrateComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bitrateComboBoxActionPerformed(evt);
            }
        });

        transcodebuttonGroup.add(onlyNeededTranscodeRadioButton);
        onlyNeededTranscodeRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        onlyNeededTranscodeRadioButton.setSelected(true);
        onlyNeededTranscodeRadioButton.setText(bundle.getString("JSymphonicProperties.onlyNeededTranscodeRadioButton.text")); // NOI18N
        onlyNeededTranscodeRadioButton.setName("onlyNeededTranscodeRadioButton"); // NOI18N

        transcodebuttonGroup.add(alwaysTranscodeRadioButton);
        alwaysTranscodeRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        alwaysTranscodeRadioButton.setText(bundle.getString("JSymphonicProperties.alwaysTranscodeRadioButton.text")); // NOI18N
        alwaysTranscodeRadioButton.setName("alwaysTranscodeRadioButton"); // NOI18N

        org.jdesktop.layout.GroupLayout mp3OptionsPanelLayout = new org.jdesktop.layout.GroupLayout(mp3OptionsPanel);
        mp3OptionsPanel.setLayout(mp3OptionsPanelLayout);
        mp3OptionsPanelLayout.setHorizontalGroup(
            mp3OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mp3OptionsPanelLayout.createSequentialGroup()
                .add(jPanelSpacer8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(mp3OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(mp3OptionsPanelLayout.createSequentialGroup()
                        .add(bitrateLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(bitrateComboBox, 0, 537, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, mp3OptionsPanelLayout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(mp3OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(alwaysTranscodeRadioButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
                            .add(onlyNeededTranscodeRadioButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE))))
                .addContainerGap())
        );
        mp3OptionsPanelLayout.setVerticalGroup(
            mp3OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mp3OptionsPanelLayout.createSequentialGroup()
                .add(mp3OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanelSpacer8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(mp3OptionsPanelLayout.createSequentialGroup()
                        .add(4, 4, 4)
                        .add(mp3OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(bitrateLabel)
                            .add(bitrateComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(onlyNeededTranscodeRadioButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(alwaysTranscodeRadioButton)))
                .addContainerGap(54, Short.MAX_VALUE))
        );

        tagutilPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("JSymphonicProperties.tagutilPanel.border.title"))); // NOI18N
        tagutilPanel.setName("tagutilPanel"); // NOI18N

        tagUtilGroup.add(readTagsRadioButton);
        readTagsRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        readTagsRadioButton.setSelected(true);
        readTagsRadioButton.setText(bundle.getString("JSymphonicProperties.readTagsRadioButton.text")); // NOI18N
        readTagsRadioButton.setName("readTagsRadioButton"); // NOI18N
        readTagsRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readTagsRadioButtonActionPerformed(evt);
            }
        });

        tagUtilGroup.add(neverReadTagsRadioButton);
        neverReadTagsRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        neverReadTagsRadioButton.setText(bundle.getString("JSymphonicProperties.neverReadTagsRadioButton.text")); // NOI18N
        neverReadTagsRadioButton.setName("neverReadTagsRadioButton"); // NOI18N
        neverReadTagsRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                neverReadTagsRadioButtonActionPerformed(evt);
            }
        });

        usePatternLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        usePatternLabel.setText(bundle.getString("JSymphonicProperties.use_following_pattern")); // NOI18N
        usePatternLabel.setName("usePatternLabel"); // NOI18N

        tagReadStructureComboBox.setEditable(true);
        tagReadStructureComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        tagReadStructureComboBox.setName("tagReadStructureComboBox"); // NOI18N

        patternExplanationTextArea.setBackground(getBackground());
        patternExplanationTextArea.setColumns(90);
        patternExplanationTextArea.setEditable(false);
        patternExplanationTextArea.setFont(patternExplanationTextArea.getFont().deriveFont((patternExplanationTextArea.getFont().getStyle() & ~java.awt.Font.ITALIC) & ~java.awt.Font.BOLD, patternExplanationTextArea.getFont().getSize()-1));
        patternExplanationTextArea.setLineWrap(true);
        patternExplanationTextArea.setRows(9);
        patternExplanationTextArea.setText(bundle.getString("JSymphonicProperties.pattern_explanation_header")); // NOI18N
        patternExplanationTextArea.setWrapStyleWord(true);
        patternExplanationTextArea.setName("patternExplanationTextArea"); // NOI18N

        org.jdesktop.layout.GroupLayout tagutilPanelLayout = new org.jdesktop.layout.GroupLayout(tagutilPanel);
        tagutilPanel.setLayout(tagutilPanelLayout);
        tagutilPanelLayout.setHorizontalGroup(
            tagutilPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, tagutilPanelLayout.createSequentialGroup()
                .add(16, 16, 16)
                .add(tagutilPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, patternExplanationTextArea, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 591, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, readTagsRadioButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 591, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, usePatternLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 591, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, neverReadTagsRadioButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 591, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, tagReadStructureComboBox, 0, 591, Short.MAX_VALUE))
                .addContainerGap())
        );
        tagutilPanelLayout.setVerticalGroup(
            tagutilPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tagutilPanelLayout.createSequentialGroup()
                .add(readTagsRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(neverReadTagsRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(usePatternLabel)
                .add(8, 8, 8)
                .add(tagReadStructureComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(patternExplanationTextArea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 109, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout transferPanelLayout = new org.jdesktop.layout.GroupLayout(transferPanel);
        transferPanel.setLayout(transferPanelLayout);
        transferPanelLayout.setHorizontalGroup(
            transferPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(transferPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(transferPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, tagutilPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, mp3OptionsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        transferPanelLayout.setVerticalGroup(
            transferPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(transferPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mp3OptionsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 151, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tagutilPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("JSymphonicProperties.transferPanel.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/transfer.png")), transferPanel); // NOI18N

        applyButton.setFont(new java.awt.Font("Dialog", 0, 12));
        applyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/ok.png"))); // NOI18N
        applyButton.setText(bundle1.getString("global.Apply")); // NOI18N
        applyButton.setName("applyButton"); // NOI18N
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 654, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(applyButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 104, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton)))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {applyButton, cancelButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelButton)
                    .add(applyButton))
                .addContainerGap())
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed

        JSymphonicProfilesDialog pd = new JSymphonicProfilesDialog(this, generationMap);
        pd.setLocationRelativeTo(this);
        ProfileElement pe = pd.createNewProfileElement(profiles);
        if(pe != null){
          profiles.put(pe.getProfileName(), pe);
          sHandler.setProfiles(profiles);
          sHandler.writeXMLFile();
          loadProfiles();
          mainWindow.loadProfiles();
        }
            

    }//GEN-LAST:event_addButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.dispose();
}//GEN-LAST:event_cancelButtonActionPerformed

    private void profileComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profileComboBoxActionPerformed
      ShowSelectedProfileData(profiles, (String)profileComboBox.getSelectedItem());
    }//GEN-LAST:event_profileComboBoxActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        if(profileComboBox.getEditor().getItem().equals("Default")){
            JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.default_profile_cannot_be_removed"));
        }else{
            if(JOptionPane.showConfirmDialog(this, java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.msg01.Do_you_want_to_delete_profile_named") + " " +profileComboBox.getSelectedItem() +" " + "?" , java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.msg01.Deleting_profile..."), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                if(profiles.containsKey(profileComboBox.getSelectedItem())){
                    profiles.remove(profileComboBox.getSelectedItem());
                    sHandler.setProfiles(profiles);
                    sHandler.writeXMLFile();
                    loadProfiles();
                    mainWindow.loadProfiles();
                }
            }
        } 
    }//GEN-LAST:event_removeButtonActionPerformed

    private void bitrateComboBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bitrateComboBoxMouseClicked

}//GEN-LAST:event_bitrateComboBoxMouseClicked

    private void bitrateComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bitrateComboBoxActionPerformed

}//GEN-LAST:event_bitrateComboBoxActionPerformed

    private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonActionPerformed
        sHandler.setLanguage((String)languageComboBox.getSelectedItem());
        sHandler.setProfiles(profiles);
        sHandler.setLogLevel((String) logLevelComboBox.getSelectedItem());
        sHandler.setLogToFile(logPanelCheckBox.isSelected());
        String brt = (String)bitrateComboBox.getSelectedItem();
        sHandler.setBitrate( Integer.valueOf(brt.substring(0, brt.length()-5)));
        sHandler.setAlwaysTrascode(alwaysTranscodeRadioButton.isSelected());
        sHandler.setAlwaysTrascode(alwaysTranscodeRadioButton.isSelected());
        sHandler.setReadTagFolderStructure((String)tagReadStructureComboBox.getSelectedItem());
        if(profiles.containsKey((String)profileComboBox.getSelectedItem())){ //check if the selected profile has been saved
            sHandler.setSelectedProfile((String)profileComboBox.getSelectedItem());
        }
        sHandler.setTheme((String)themeComboBox.getSelectedItem());
        mainWindow.changeLAF((String)themeMap.get(sHandler.getTheme()));
        SwingUtilities.updateComponentTreeUI(this);
        sHandler.writeXMLFile();

        // Start a new thread to load the config since scanning the device may take time
        Thread loadNewConfigThread = new Thread(){
            @Override
            public void run(){
                try{
                    mainWindow.loadNewConfig();
                } catch(Exception e){}
            }
        };
        loadNewConfigThread.setPriority(Thread.NORM_PRIORITY);
        loadNewConfigThread.start();
        loadNewConfigThread = null;
        this.dispose();
    }//GEN-LAST:event_applyButtonActionPerformed

private void readTagsRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readTagsRadioButtonActionPerformed
    sHandler.setReadID3Tags(true);
}//GEN-LAST:event_readTagsRadioButtonActionPerformed

private void neverReadTagsRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_neverReadTagsRadioButtonActionPerformed
    sHandler.setReadID3Tags(false);
}//GEN-LAST:event_neverReadTagsRadioButtonActionPerformed

private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
    JSymphonicProfilesDialog pd = new JSymphonicProfilesDialog(this, generationMap);
    pd.setLocationRelativeTo(this);
    if(pd.updateExistingProfileElement(profiles, (String)profileComboBox.getSelectedItem())){
        sHandler.setProfiles(profiles);
        sHandler.writeXMLFile();
        loadProfiles();
        mainWindow.loadProfiles();
    }
}//GEN-LAST:event_editButtonActionPerformed

private void logPanelCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logPanelCheckBoxActionPerformed
    sHandler.setLogToFile(logPanelCheckBox.isSelected());
}//GEN-LAST:event_logPanelCheckBoxActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JRadioButton alwaysTranscodeRadioButton;
    private javax.swing.JButton applyButton;
    private javax.swing.JComboBox bitrateComboBox;
    private javax.swing.JLabel bitrateLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel devPathLabel;
    private javax.swing.JLabel devPathLabelData;
    private javax.swing.JButton editButton;
    private javax.swing.JLabel exportPathLabel;
    private javax.swing.JLabel exportPathLabelData;
    private javax.swing.JLabel genPathLabel;
    private javax.swing.JLabel genPathLabelData;
    private javax.swing.JPanel interfacePanel;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelSpacer8;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox languageComboBox;
    private javax.swing.JLabel localPathLabel;
    private javax.swing.JLabel localPathLabelData;
    private javax.swing.JComboBox logLevelComboBox;
    private javax.swing.JPanel logLevelPanel;
    private javax.swing.JCheckBox logPanelCheckBox;
    private javax.swing.JPanel mp3OptionsPanel;
    private javax.swing.JRadioButton neverReadTagsRadioButton;
    private javax.swing.JLabel noteLangLabel;
    private javax.swing.JLabel noteProfileLabel;
    private javax.swing.JRadioButton onlyNeededTranscodeRadioButton;
    private javax.swing.JPanel pathPanel;
    private javax.swing.JTextArea patternExplanationTextArea;
    private javax.swing.JComboBox profileComboBox;
    private javax.swing.JPanel profileDataPanel;
    private javax.swing.JRadioButton readTagsRadioButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JComboBox tagReadStructureComboBox;
    private javax.swing.ButtonGroup tagUtilGroup;
    private javax.swing.JPanel tagutilPanel;
    private javax.swing.JLabel tempPathLabel;
    private javax.swing.JLabel tempPathLabelData;
    private javax.swing.JComboBox themeComboBox;
    private javax.swing.ButtonGroup transcodebuttonGroup;
    private javax.swing.JPanel transferPanel;
    private javax.swing.JLabel usePatternLabel;
    // End of variables declaration//GEN-END:variables
    
}
