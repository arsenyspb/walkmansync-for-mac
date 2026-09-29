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
 * JSymphonicProfilesDialog.java
 *
 * Created on June 8, 2008, 4:47 PM
 * 
 */

package org.danizmax.jsymphonic.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import org.danizmax.jsymphonic.toolkit.ProfileElement;
import org.naurd.media.jsymphonic.device.sony.nw.NWGeneric;

/**
 * This class is used to create new profiles, it creates a dialog for editing the JSymphonic profiles.
 * @author  danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class JSymphonicProfilesDialog extends javax.swing.JDialog {

    private HashMap generationMap = null;
    private HashMap profiles = null;
    private boolean updating = false;
    private boolean ok = false;
    
    /** Creates new form JSymphonicProfilesDialog */
    public JSymphonicProfilesDialog(java.awt.Frame parent, HashMap generationMap) {
        super(parent, true);
        this.generationMap = generationMap;
        initComponents();
        initConstantValues();
    }
    
     /***
     * This method populates comboboxes that do not change and creates Hasmaps for usage
     */
    private void initConstantValues(){
        // Remove any item
        generationComboBox.removeAllItems();

        // Add an entry for each generation available
        List<Integer> generationList = new ArrayList(generationMap.values());
        Collections.sort(generationList); // Sort element to display generation in ascending order
        Iterator it = generationList.iterator();
        while(it.hasNext()){
            generationComboBox.addItem(it.next());
        }
    }


    /**
     * create new profile element
     * @param profiles
     * @return new profile element
     */
    public ProfileElement createNewProfileElement(HashMap profiles){
        if(profiles != null){
            updating = false;
            this.profiles = profiles;
            nameTextField.setEnabled(true);
            this.setVisible(true);
            if(ok){
                return new ProfileElement(nameTextField.getText(), NWGeneric.getKeyfromGenerationCombo((String)generationComboBox.getSelectedItem()), localPathTextField.getText(), exportPathTextField.getText(), devicePathTextField.getText(), tempTextField.getText(), tempCheckBox.isSelected(), exportCheckBox.isSelected());
            }else{
                return null;
            }
        }
        return null;
    }
    /**
     * Update existing profile element
     * @param profiles
     * @param selectedProfile
     * @return true if anything has been updated alse false
     */
    public boolean updateExistingProfileElement(HashMap profiles, String selectedProfile){
        if(profiles != null){
            this.profiles = profiles;
            if(profiles.containsKey(selectedProfile)){ //if true update old entry
                fillFields(profiles, selectedProfile);
                nameTextField.setEnabled(false);
                updating = true;
                this.setVisible(true);
                
                if(ok){
                   ProfileElement pEl = (ProfileElement) profiles.get(selectedProfile);

                   pEl.setDeviceGeneration(NWGeneric.getKeyfromGenerationCombo((String)generationComboBox.getSelectedItem()));
                   pEl.setDevicePath(devicePathTextField.getText());
                   pEl.setLocalPath(localPathTextField.getText());
                   pEl.setExportPath(exportPathTextField.getText());
                   pEl.setTempPathSameAsDevicePath(tempCheckBox.isSelected());
                   pEl.setExportPathSameAsLocalPath(exportCheckBox.isSelected());
                   pEl.setTranscodeTempPath(tempTextField.getText());
                   return true;

                }
            }else{
                     JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.error01.ProfileWithName") + " " + selectedProfile + " " + java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.error01.does_not_exist"), java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.error01.Non_existing_profile_name..."), JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }
    
    /**
     * fills profile fields
     * @param profiles 
     * @param selectedProfile the name of the selected profile
     */
    private void fillFields(HashMap profiles, String selectedProfile){
        if(profiles.containsKey(selectedProfile)){ 
            if(profiles != null && selectedProfile != null){
                ProfileElement sel = (ProfileElement) profiles.get(selectedProfile);
                if(sel != null){
                    nameTextField.setText(sel.getProfileName());
                    localPathTextField.setText(sel.getLocalPath());
                    exportCheckBox.setSelected(sel.isExportPathSameAsLocalPath());
                    exportPathTextField.setEnabled(!sel.isExportPathSameAsLocalPath());
                    exportPathTextField.setText(sel.getExportPath());
                    devicePathTextField.setText(sel.getDevicePath());
                    generationComboBox.setSelectedItem(generationMap.get(sel.getDeviceGeneration()));
                    tempBrowsButton.setEnabled(!sel.isTempPathSameAsDevicePath());
                    tempCheckBox.setSelected(sel.isTempPathSameAsDevicePath());
                    tempTextField.setEnabled(!sel.isTempPathSameAsDevicePath());
                    tempTextField.setText(sel.getTranscodeTempPath());
                }
            }
        }
    }
    
    /**
     * Opens a choose dialog for folders
     * @param dialogText the text you want to appera in the dialog
     * @param tf the JTextField you want to enter path into
     */
    protected boolean OpenChooserDialog(String dialogText, JTextField tf){
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(dialogText);
        fc.setCurrentDirectory(new File(tf.getText()));
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setVisible(true);
        
        if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            tf.setText(fc.getSelectedFile().getAbsolutePath());
            return true;
        }
        
        return false;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        nameTextField = new javax.swing.JTextField();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        deviceBrowseButton = new javax.swing.JButton();
        devicePathTextField = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        localBrowsButton = new javax.swing.JButton();
        localPathTextField = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        exportBrowsButton = new javax.swing.JButton();
        exportPathTextField = new javax.swing.JTextField();
        exportCheckBox = new javax.swing.JCheckBox();
        jPanelContainer4 = new javax.swing.JPanel();
        tempTextField = new javax.swing.JTextField();
        tempBrowsButton = new javax.swing.JButton();
        tempCheckBox = new javax.swing.JCheckBox();
        jPanel7 = new javax.swing.JPanel();
        generationComboBox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setName("Form"); // NOI18N

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog"); // NOI18N
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("JSymphonicProfilesDialog.profile_name"))); // NOI18N
        jPanel2.setName("jPanel2"); // NOI18N

        nameTextField.setName("nameTextField"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(nameTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(nameTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        okButton.setFont(new java.awt.Font("Dialog", 0, 12));
        okButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/ok.png"))); // NOI18N
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("localization/misc"); // NOI18N
        okButton.setText(bundle1.getString("global.OK")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setFont(new java.awt.Font("Dialog", 0, 12));
        cancelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/cancel.png"))); // NOI18N
        cancelButton.setText(bundle1.getString("global.Cancel")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.setName("jPanel1"); // NOI18N

        java.util.ResourceBundle bundle2 = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties"); // NOI18N
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle2.getString("JSymphonicProperties.devPathLabel.text"))); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N

        deviceBrowseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/folder.png"))); // NOI18N
        deviceBrowseButton.setName("deviceBrowseButton"); // NOI18N
        deviceBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceBrowseButtonActionPerformed(evt);
            }
        });

        devicePathTextField.setName("devicePathTextField"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(devicePathTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 423, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(deviceBrowseButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(deviceBrowseButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(devicePathTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle2.getString("JSymphonicProperties.localPathLabel.text"))); // NOI18N
        jPanel5.setName("jPanel5"); // NOI18N

        localBrowsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/folder.png"))); // NOI18N
        localBrowsButton.setName("localBrowsButton"); // NOI18N
        localBrowsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                localBrowsButtonActionPerformed(evt);
            }
        });

        localPathTextField.setName("localPathTextField"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(localPathTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 423, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(localBrowsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(localBrowsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(localPathTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle2.getString("JSymphonicProperties.exportPathLabel.text"))); // NOI18N
        jPanel6.setName("jPanel6"); // NOI18N

        exportBrowsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/folder.png"))); // NOI18N
        exportBrowsButton.setName("exportBrowsButton"); // NOI18N
        exportBrowsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportBrowsButtonActionPerformed(evt);
            }
        });

        exportPathTextField.setName("exportPathTextField"); // NOI18N

        exportCheckBox.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        exportCheckBox.setText(bundle.getString("JSymphonicProfilesDialog.exportCheckBox.text")); // NOI18N
        exportCheckBox.setName("exportCheckBox"); // NOI18N
        exportCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportCheckBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(exportCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(exportPathTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 423, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(exportBrowsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(exportBrowsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(exportPathTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(exportCheckBox)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelContainer4.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle2.getString("JSymphonicProperties.tempPathLabel.text"))); // NOI18N
        jPanelContainer4.setName("jPanelContainer4"); // NOI18N

        tempTextField.setName("tempTextField"); // NOI18N

        tempBrowsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/folder.png"))); // NOI18N
        tempBrowsButton.setName("tempBrowsButton"); // NOI18N
        tempBrowsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tempBrowsButtonActionPerformed(evt);
            }
        });

        tempCheckBox.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        tempCheckBox.setText(bundle.getString("JSymphonicProfilesDialog.tempCheckBox.text")); // NOI18N
        tempCheckBox.setName("tempCheckBox"); // NOI18N
        tempCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tempCheckBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanelContainer4Layout = new org.jdesktop.layout.GroupLayout(jPanelContainer4);
        jPanelContainer4.setLayout(jPanelContainer4Layout);
        jPanelContainer4Layout.setHorizontalGroup(
            jPanelContainer4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanelContainer4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanelContainer4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanelContainer4Layout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(tempCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanelContainer4Layout.createSequentialGroup()
                        .add(tempTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 423, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(tempBrowsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanelContainer4Layout.setVerticalGroup(
            jPanelContainer4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanelContainer4Layout.createSequentialGroup()
                .add(jPanelContainer4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tempBrowsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(tempTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tempCheckBox))
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle2.getString("JSymphonicProperties.genPathLabel.text"))); // NOI18N
        jPanel7.setName("jPanel7"); // NOI18N

        generationComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        generationComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        generationComboBox.setName("generationComboBox"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(generationComboBox, 0, 457, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(generationComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanelContainer4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanelContainer4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(29, Short.MAX_VALUE))
        );

        java.util.ResourceBundle bundle3 = java.util.ResourceBundle.getBundle("localization/jsymphonicwindow"); // NOI18N
        jPanel7.getAccessibleContext().setAccessibleName(bundle3.getString("JSymphonicWindow.Device_Generation")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 103, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton)))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {cancelButton, okButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 52, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelButton)
                    .add(okButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void deviceBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceBrowseButtonActionPerformed
    OpenChooserDialog(java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.Select_device_path"), devicePathTextField);
}//GEN-LAST:event_deviceBrowseButtonActionPerformed

private void localBrowsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_localBrowsButtonActionPerformed
    OpenChooserDialog(java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.Select_local_path"), localPathTextField);
}//GEN-LAST:event_localBrowsButtonActionPerformed

private void tempBrowsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tempBrowsButtonActionPerformed
    OpenChooserDialog(java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.Select_temp_path"), tempTextField);
}//GEN-LAST:event_tempBrowsButtonActionPerformed

private void tempCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tempCheckBoxActionPerformed
if (tempCheckBox.isSelected() ) {
        tempTextField.setEnabled(false);
        tempBrowsButton.setEnabled(false);
    } else {
        tempTextField.setEnabled(true);
        tempBrowsButton.setEnabled(true);
    }
}//GEN-LAST:event_tempCheckBoxActionPerformed

private void exportBrowsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportBrowsButtonActionPerformed
    OpenChooserDialog(java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.Select_export_path"), exportPathTextField);
}//GEN-LAST:event_exportBrowsButtonActionPerformed

private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    if( (profiles.containsKey(nameTextField.getText()) && updating) || (!profiles.containsKey(nameTextField.getText()) && !updating)){
        ok = true;
        this.dispose();
    }else{
        JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.error02.Profile_with_name") + " " + nameTextField.getText() + " " + java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.error02.already_exist"), java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.error02.Existing_profile_name"), JOptionPane.ERROR_MESSAGE);
    }
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    this.dispose();
}//GEN-LAST:event_cancelButtonActionPerformed

private void exportCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportCheckBoxActionPerformed
    if (exportCheckBox.isSelected() ) {
        exportPathTextField.setEnabled(false);
        exportBrowsButton.setEnabled(false);
    } else {
        exportPathTextField.setEnabled(true);
        exportBrowsButton.setEnabled(true);
    }
}//GEN-LAST:event_exportCheckBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton deviceBrowseButton;
    private javax.swing.JTextField devicePathTextField;
    private javax.swing.JButton exportBrowsButton;
    private javax.swing.JCheckBox exportCheckBox;
    private javax.swing.JTextField exportPathTextField;
    private javax.swing.JComboBox generationComboBox;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanelContainer4;
    private javax.swing.JButton localBrowsButton;
    private javax.swing.JTextField localPathTextField;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JButton okButton;
    private javax.swing.JButton tempBrowsButton;
    private javax.swing.JCheckBox tempCheckBox;
    private javax.swing.JTextField tempTextField;
    // End of variables declaration//GEN-END:variables

}
