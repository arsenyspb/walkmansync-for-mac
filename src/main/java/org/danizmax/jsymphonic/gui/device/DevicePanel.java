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
 * DevicePanel.java
 *
 * Created on March 25, 2008, 9:50 PM
 * 
 */

package org.danizmax.jsymphonic.gui.device;

import org.danizmax.jsymphonic.toolkit.DynamicDeviceTreePopUp;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import org.danizmax.jsymphonic.gui.JSymphonicWindow;
import org.naurd.media.jsymphonic.device.sony.nw.NWGenericListener;


/**
 * This class is a JPanel component shows the state of the Sony (c) device
 * @author  Daniel Žalar (danizmax@yahoo.com)
 */
public class DevicePanel extends javax.swing.JPanel implements NWGenericListener{
        
    private static Logger logger = Logger.getLogger("org.danizmax.gui.DevicePanel");
    private DeviceManager deviceManager;
    private String devicePath = null;
    private int deviceGeneration = 0;
    private JSymphonicWindow wnd = null;
    private String deviceName = "Walkman";
    private int labelAuthorizedWidth;

    
//    public static String FILTER_ARALT = "Artist/Album/Title";
    public static String ARTIST = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Artist");
    public static String ALBUM = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Album");
    public static String TITLE = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Title");
    public static String GENRE = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Genre");

    public static String FILTER_ARALT = ARTIST+"/"+ALBUM+"/"+TITLE;
    public static String FILTER_ART = ARTIST+"/"+TITLE;
    public static String FILTER_ALT = ALBUM+"/"+TITLE;
    public static String FILTER_GARALT = GENRE+"/"+ARTIST+"/"+ALBUM+"/"+TITLE;

    private int transferState = NO_TRANSFER;
    private int transferProgressValue;
    private int exportProgressValue;
    private int deleteProgressValue;
    private int importProgressValue;
    private int updateProgressValue;
    
    private DynamicDeviceTreePopUp treePopUp;

    public static final int NO_TRANSFER = 0;
    public static final int TRANSFER_IN_PROGRESS = 1;
    public static final int TRANSFER_STOPPED = 2;
    public static final int TRANSFER_OVER = 3;


    /** Creates new instance of DevicePanel */
    public DevicePanel() {
        treePopUp = new DynamicDeviceTreePopUp(this,deviceTree);
        try {
            initComponents();
        }
        catch(Exception e){
            logger.severe("Error while initializaing device panel");
            e.printStackTrace();
        }
        initTransferFrame();
        
        transferPanel.setVisible(false);
        deviceContentPanel.setVisible(true); // Nicolas: I added this line because nothing was shown at startup
        deviceTree.setVisible(false); // Nicolas: hide the device tree, it will be showed when filled
        deviceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
       
        deviceManager  = new DeviceManager(deviceTree, this, "",0, "", "");
        treePopUp.setTree(deviceTree);
        deviceTree.setComponentPopupMenu(treePopUp);
        loadFilterProfiles();
    }
        
    public String getSelectedFilterProfile(){
        return (String) viewComboBox.getSelectedItem();
    }

    private void initTransferFrame() {
        // Hide all progress bar (and the corresponding labels)...
        importLabel.setVisible(false);
        importProgressBar.setVisible(false);
        exportLabel.setVisible(false);
        exportProgressBar.setVisible(false);
        deleteLabel.setVisible(false);
        deleteProgressBar.setVisible(false);
        updateLabel.setVisible(false);
        updateProgressBar.setVisible(false);
        fileLabel.setVisible(false);
        fileProgressBar.setVisible(false);
        decodedFileLabel.setVisible(false);
        decodedFileProgressBar.setVisible(false);
        encodedFileLabel.setVisible(false);
        encodedFileProgressBar.setVisible(false);

        // Show the transfer panel (with all the progress bars)
        transferPanel.setVisible(true);
        // Hide the device content panel
        deviceContentPanel.setVisible(false);
        // Enable the close button (when transfer is not over)
        closeButton.setEnabled(true);
    }
    
    private void loadFilterProfiles(){
        viewComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { FILTER_ARALT, FILTER_ART, FILTER_ALT, FILTER_GARALT }));
    }
    
    /**
     * Set the GUI state as if mounted
     * @param isMounted if true set the GUI state as if mounted
     */
    private void setMountedGuiState(boolean isMounted){
        if(!isMounted){
                deviceActionProgressBar.setString(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Device_is_not_mounted"));
                deviceActionProgressBar.setValue(0);
                ((DefaultTreeModel)deviceTree.getModel()).setRoot(null);
        }
        
        mountToggleButton.setSelected(isMounted);
    }
    
    /**
     * Mounts or unmounts the device
     * @param mount if true mount the device else unmount
     */
    public void mountDevice(final boolean mount){
        Thread mountDeviceThread = new Thread(){
            @Override
            public void run(){
                try{
                    mountDeviceInThread(mount);
                } catch(Exception e){}
            }
        };
        mountDeviceThread.setPriority(Thread.NORM_PRIORITY);
        mountDeviceThread.start();
        mountDeviceThread = null;
    }
    
    /**
     * Start a thread to mount the device
     */
    public void mountDeviceInThread(boolean mount){
        if(getManager() != null){
            if(!getManager().isInImportState()){ //we must not let unmount while in import state
                setMountedGuiState(false);
                    if(mount){
                        getManager().mountDevice(true);
                        reloadTree();
                    }else{
                        getManager().mountDevice(false);
                    }
                    setMountedGuiState(getManager().isMounted());
            }else{
                JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Cannot_unmount"), java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Mounting_error"), JOptionPane.INFORMATION_MESSAGE);
                setMountedGuiState(getManager().isMounted());}
        }
    }
    
    /**
     * 
     * @param files files for import
     */
    public void scheduleTrackImport(final File files[]){
        // A thread must be create here, because the object is used in this method (to inform about how many files are imported)
        Thread t = new Thread(){
            @Override
            public void run(){
                getManager().scheduleTrackImport(files);
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        t = null;
    }
    
    public void scheduleTrackExport(){
        getManager().scheduleTrackExport();
    }
    
    public void deleteSelectedTracks(){
        getManager().scheduleTrackDeletion();
    }
    
    public void applyChanges(){
      //  transferStatusFrame.setVisible(true);
        getManager().applyChanges();
    }
    
    public void cancelChanges(){
        getManager().cancelChanges();
        reloadTree();
    }
    
     /** Load data into the tree  */
     public void reloadTree(){
       if(getManager().isDevicePathValid()){
           if(getDeviceManager().isMounted()){
                //If genericDevice folder isn't correct, display configuration window 
                if( !deviceManager.initSourcePath()) {
                    JOptionPane.showMessageDialog(this,java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_in_device_path_part1") + "" + devicePath + "" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_in_device_path_part2"));
                }
                else { // Else, we can save the value and load the music from the genericDevice            
                    // Create a new thread to load the info of the genericDevice
                    enableGUI(false);
                    getManager().refreshDeviceTree(viewComboBox.getSelectedIndex(), filterTextField.getText());
                    displayDeviceSpace();
                    //mountDevice(true);  
                   /* Thread t = new Thread(){
                        @Override
                        public void run(){
                            try{
                                //load the tree
                                getManager().refreshDeviceTree(viewComboBox.getSelectedIndex(), filterTextField.getText());
                                displayDeviceSpace();
                            } catch(Exception e){
                                logger.warning(e.getMessage());
                            }
                        }
                    };
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
                    t = null;*/
                    enableGUI(true);        
                }
           }else{
               getLogger().warning("The device is not mounted!");
           }
       }else{
            logger.warning("Cannot find OMG path. The device path is probably wrong!");
       }
     }
     
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        deviceContentPanel = new javax.swing.JPanel();
        deviceActionProgressBar = new javax.swing.JProgressBar();
        clearButton = new javax.swing.JButton();
        viewComboBox = new javax.swing.JComboBox();
        filterTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        deviceTree = new javax.swing.JTree();
        filterButton = new javax.swing.JButton();
        mountToggleButton = new javax.swing.JToggleButton();
        transferPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        exportLabel = new javax.swing.JLabel();
        exportProgressBar = new javax.swing.JProgressBar();
        deleteLabel = new javax.swing.JLabel();
        deleteProgressBar = new javax.swing.JProgressBar();
        importLabel = new javax.swing.JLabel();
        importProgressBar = new javax.swing.JProgressBar();
        updateLabel = new javax.swing.JLabel();
        updateProgressBar = new javax.swing.JProgressBar();
        spacerPanel = new javax.swing.JPanel();
        fileLabel = new javax.swing.JLabel();
        fileProgressBar = new javax.swing.JProgressBar();
        decodedFileLabel = new javax.swing.JLabel();
        decodedFileProgressBar = new javax.swing.JProgressBar();
        encodedFileLabel = new javax.swing.JLabel();
        encodedFileProgressBar = new javax.swing.JProgressBar();
        currentFileLabel = new javax.swing.JLabel();
        transferProgressBar = new javax.swing.JProgressBar();
        closeButton = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setName("Form"); // NOI18N

        deviceContentPanel.setName("deviceContentPanel"); // NOI18N

        deviceActionProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        deviceActionProgressBar.setName("deviceActionProgressBar"); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("localization/devicepanel"); // NOI18N
        deviceActionProgressBar.setString(bundle.getString("DevicePanel.Device_is_not_mounted")); // NOI18N
        deviceActionProgressBar.setStringPainted(true);

        clearButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/clear_right.png"))); // NOI18N
        clearButton.setName("clearButton"); // NOI18N
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        viewComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        viewComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Artist/Album/Title", "Artists/Titles", "Albums/Titles", "Genres/Artists/Albums/Titles" }));
        viewComboBox.setName("viewComboBox"); // NOI18N
        viewComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewComboBoxActionPerformed(evt);
            }
        });

        filterTextField.setMinimumSize(new java.awt.Dimension(4, 28));
        filterTextField.setName("filterTextField"); // NOI18N
        filterTextField.setPreferredSize(new java.awt.Dimension(4, 20));
        filterTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                filterTextFieldKeyPressed(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        deviceTree.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        deviceTree.setName("deviceTree"); // NOI18N
        deviceTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                deviceTreeMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                deviceTreeMouseReleased(evt);
            }
        });
        deviceTree.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                deviceTreeComponentShown(evt);
            }
        });
        deviceTree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                deviceTreeMouseDragged(evt);
            }
        });
        jScrollPane1.setViewportView(deviceTree);

        filterButton.setFont(new java.awt.Font("Dialog", 0, 12));
        filterButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/filter.png"))); // NOI18N
        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("localization/misc"); // NOI18N
        filterButton.setText(bundle1.getString("global.Filter")); // NOI18N
        filterButton.setMaximumSize(new java.awt.Dimension(90, 26));
        filterButton.setMinimumSize(new java.awt.Dimension(90, 28));
        filterButton.setName("filterButton"); // NOI18N
        filterButton.setPreferredSize(new java.awt.Dimension(90, 28));
        filterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterButtonActionPerformed(evt);
            }
        });

        mountToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/device.png"))); // NOI18N
        mountToggleButton.setToolTipText(bundle.getString("DevicePanel.mountToggleButton.toolTipText")); // NOI18N
        mountToggleButton.setName("mountToggleButton"); // NOI18N
        mountToggleButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/device_mounted.png"))); // NOI18N
        mountToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mountToggleButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout deviceContentPanelLayout = new org.jdesktop.layout.GroupLayout(deviceContentPanel);
        deviceContentPanel.setLayout(deviceContentPanelLayout);
        deviceContentPanelLayout.setHorizontalGroup(
            deviceContentPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, deviceContentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(deviceContentPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
                    .add(deviceContentPanelLayout.createSequentialGroup()
                        .add(deviceActionProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(mountToggleButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, deviceContentPanelLayout.createSequentialGroup()
                        .add(clearButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(filterTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(filterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, viewComboBox, 0, 293, Short.MAX_VALUE))
                .addContainerGap())
        );
        deviceContentPanelLayout.setVerticalGroup(
            deviceContentPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(deviceContentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(deviceContentPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(deviceContentPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(filterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(filterTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(clearButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(viewComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1152, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(deviceContentPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(mountToggleButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(deviceActionProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        deviceContentPanelLayout.linkSize(new java.awt.Component[] {filterButton, filterTextField}, org.jdesktop.layout.GroupLayout.VERTICAL);

        transferPanel.setName("transferPanel"); // NOI18N

        jPanel1.setAlignmentX(0.06410257F);
        jPanel1.setName("jPanel1"); // NOI18N

        exportLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        exportLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/export.png"))); // NOI18N
        exportLabel.setText(bundle.getString("DevicePanel.Exporting")); // NOI18N
        exportLabel.setName("exportLabel"); // NOI18N

        exportProgressBar.setName("exportProgressBar"); // NOI18N

        deleteLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        deleteLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/remove.png"))); // NOI18N
        deleteLabel.setText(bundle.getString("DevicePanel.Deleting")); // NOI18N
        deleteLabel.setName("deleteLabel"); // NOI18N

        deleteProgressBar.setName("deleteProgressBar"); // NOI18N

        importLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        importLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/import.png"))); // NOI18N
        importLabel.setText(bundle.getString("DevicePanel.Importing")); // NOI18N
        importLabel.setName("importLabel"); // NOI18N

        importProgressBar.setName("importProgressBar"); // NOI18N

        updateLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        updateLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/load.png"))); // NOI18N
        updateLabel.setText(bundle.getString("DevicePanel.Updating_database")); // NOI18N
        updateLabel.setName("updateLabel"); // NOI18N

        updateProgressBar.setName("updateProgressBar"); // NOI18N

        spacerPanel.setName("spacerPanel"); // NOI18N

        org.jdesktop.layout.GroupLayout spacerPanelLayout = new org.jdesktop.layout.GroupLayout(spacerPanel);
        spacerPanel.setLayout(spacerPanelLayout);
        spacerPanelLayout.setHorizontalGroup(
            spacerPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 257, Short.MAX_VALUE)
        );
        spacerPanelLayout.setVerticalGroup(
            spacerPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 665, Short.MAX_VALUE)
        );

        fileLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        fileLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/file.png"))); // NOI18N
        fileLabel.setText(bundle.getString("DevicePanel.File_in_progress")); // NOI18N
        fileLabel.setName("fileLabel"); // NOI18N

        fileProgressBar.setName("fileProgressBar"); // NOI18N

        decodedFileLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        decodedFileLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/run.png"))); // NOI18N
        decodedFileLabel.setText(bundle.getString("DevicePanel.File_currently_decoded")); // NOI18N
        decodedFileLabel.setName("decodedFileLabel"); // NOI18N

        decodedFileProgressBar.setName("decodedFileProgressBar"); // NOI18N

        encodedFileLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        encodedFileLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/file.png"))); // NOI18N
        encodedFileLabel.setText(bundle.getString("DevicePanel.File_currently_encoded")); // NOI18N
        encodedFileLabel.setName("encodedFileLabel"); // NOI18N

        encodedFileProgressBar.setName("encodedFileProgressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spacerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(exportLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(exportProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(deleteLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(deleteProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(importLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(importProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(updateLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(updateProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(fileLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(fileProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(decodedFileLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(decodedFileProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(encodedFileLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .add(encodedFileProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(exportLabel)
                .add(7, 7, 7)
                .add(exportProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(deleteLabel)
                .add(7, 7, 7)
                .add(deleteProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(importLabel)
                .add(7, 7, 7)
                .add(importProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(updateLabel)
                .add(7, 7, 7)
                .add(updateProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileLabel)
                .add(7, 7, 7)
                .add(fileProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(decodedFileLabel)
                .add(7, 7, 7)
                .add(decodedFileProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(encodedFileLabel)
                .add(7, 7, 7)
                .add(encodedFileProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(92, 92, 92)
                .add(spacerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        currentFileLabel.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        currentFileLabel.setText(bundle.getString("DevicePanel.total_progress")); // NOI18N
        currentFileLabel.setName("currentFileLabel"); // NOI18N

        transferProgressBar.setName("transferProgressBar"); // NOI18N

        closeButton.setFont(new java.awt.Font("Dialog", 0, 12));
        closeButton.setText(bundle1.getString("global.Close")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout transferPanelLayout = new org.jdesktop.layout.GroupLayout(transferPanel);
        transferPanel.setLayout(transferPanelLayout);
        transferPanelLayout.setHorizontalGroup(
            transferPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, transferPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(transferPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, currentFileLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, transferProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                    .add(closeButton))
                .addContainerGap())
        );
        transferPanelLayout.setVerticalGroup(
            transferPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(transferPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(currentFileLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(transferProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(closeButton)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(deviceContentPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(transferPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(transferPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(deviceContentPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void filterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterButtonActionPerformed
        reloadTree();
    }//GEN-LAST:event_filterButtonActionPerformed

    private void viewComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewComboBoxActionPerformed
        getManager().refreshDeviceTree(viewComboBox.getSelectedIndex(), filterTextField.getText());
    }//GEN-LAST:event_viewComboBoxActionPerformed

    private void deviceTreeComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_deviceTreeComponentShown
        
    }//GEN-LAST:event_deviceTreeComponentShown

    private void deviceTreeMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deviceTreeMouseDragged
        
    }//GEN-LAST:event_deviceTreeMouseDragged

    private void filterTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filterTextFieldKeyPressed
        if(evt.getKeyCode() == 10){
           filterButton.doClick();
        }
    }//GEN-LAST:event_filterTextFieldKeyPressed

    private void deviceTreeMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deviceTreeMouseReleased
    //   deviceTreeMousePressed(evt); //for cross-platform compatibility
    }//GEN-LAST:event_deviceTreeMouseReleased

    private void deviceTreeMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deviceTreeMousePressed
       // deviceTree.setSelectionPath(deviceTree.getPathForLocation(evt.getX(), evt.getY()));
        if ( evt.isPopupTrigger()) {
            getTreePopUp().setLocation(evt.getX(), evt.getY());
            getTreePopUp().setVisible(true);
        }else{
            getTreePopUp().setVisible(false);
        }
    }//GEN-LAST:event_deviceTreeMousePressed

private void mountToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mountToggleButtonActionPerformed
   mountDevice(mountToggleButton.isSelected());
}//GEN-LAST:event_mountToggleButtonActionPerformed

private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
    filterTextField.setText("");
    reloadTree();
}//GEN-LAST:event_clearButtonActionPerformed

private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    // Action to perform when this button is press depends on the state of the transfer
    switch(transferState){
        case TRANSFER_IN_PROGRESS:
            transferState = TRANSFER_STOPPED; // Change transfer state
            // If transfer is in progress, the deletion/copy of titles should be stopped, the database should be build
            deviceManager.stopTransfer();
            // Change the text on the button
            closeButton.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Stop_now"));
            break;
        case TRANSFER_STOPPED:
            // If transfer was stopped (it means that something goes wrong in stopping deletion/copy threads or building the database), all threads should be killed and the device should be left on the current state
            // This action uses deprecated methods and leave the device to an undetermined stated, so a confirmation is asked to the user
            if(JOptionPane.showConfirmDialog(this, java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Are_you_sure_to_interrupt"), java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Are_you_sure"), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION){
                return;
            }
            // Continue if user want so, kill all thread
            deviceManager.killAllTransferThreads();
            // Refresh content
            deviceManager.refreshTitlesFromDevice();
            // Enable the GUI back
            if(wnd != null){
                wnd.setTransferState(false);
            }
            // No breaks here, after the threads have been killed, the GUI should come back to an initial state, as if "Close" button was pressed after an over transfer
        case TRANSFER_OVER:
            transferState = NO_TRANSFER; // Change transfer state
            // If transfer is over, the transfer panel should be hidden, the device tree should be reloaded and shown
            transferPanel.setVisible(false);
            deviceContentPanel.setVisible(true);
            this.reloadTree();
            break;
        default:
            logger.severe("Transfer state is not as expected!");
    }
}//GEN-LAST:event_closeButtonActionPerformed
    
    public void displayDeviceSpace() {
        if(getManager() != null){
            deviceActionProgressBar.setString(getManager().getSpaceLeftInText());
            deviceActionProgressBar.setValue(getManager().getSpaceLeftInRatio());
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel currentFileLabel;
    private javax.swing.JLabel decodedFileLabel;
    private javax.swing.JProgressBar decodedFileProgressBar;
    private javax.swing.JLabel deleteLabel;
    private javax.swing.JProgressBar deleteProgressBar;
    private javax.swing.JProgressBar deviceActionProgressBar;
    private javax.swing.JPanel deviceContentPanel;
    private javax.swing.JTree deviceTree;
    private javax.swing.JLabel encodedFileLabel;
    private javax.swing.JProgressBar encodedFileProgressBar;
    private javax.swing.JLabel exportLabel;
    private javax.swing.JProgressBar exportProgressBar;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JProgressBar fileProgressBar;
    private javax.swing.JButton filterButton;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JLabel importLabel;
    private javax.swing.JProgressBar importProgressBar;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToggleButton mountToggleButton;
    private javax.swing.JPanel spacerPanel;
    private javax.swing.JPanel transferPanel;
    private javax.swing.JProgressBar transferProgressBar;
    private javax.swing.JLabel updateLabel;
    private javax.swing.JProgressBar updateProgressBar;
    private javax.swing.JComboBox viewComboBox;
    // End of variables declaration//GEN-END:variables

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger aLogger) {
        logger = aLogger;
    }
    
    public static void setParentLogger(Logger aLogger) {
        logger.setParent(aLogger);
    }
    
    public String getDevicePath() {
        return devicePath;
    }

    public void setDevicePath(String devicePath) {
        this.devicePath = devicePath;
        getManager().setDevicePath(devicePath);
        setMountedGuiState(getManager().isDevicePathValid());
    }
    
    public int getDeviceGeneration() {
        return deviceGeneration;
    }

    public void setDeviceGeneration(int deviceGen) {
        this.deviceGeneration = deviceGen;
        getManager().setDeviceGeneration(deviceGen);
    }
        
    public String getDeviceName() {
        return deviceName;
    }
    
    public String getExportPath() {
        return getDeviceManager().getExportPath();
    }

    public void setExportPath(String exportPath) {
        getDeviceManager().setExportPath(exportPath);
    }

    public void setTempPath(String tempPath) {
        getDeviceManager().setTempPath(tempPath);
    }

    public boolean isAlwaysTranscode() {
        return getDeviceManager().isAlwaysTranscode();
    }

    public void setAlwaysTranscode(boolean AlwaysTranscode) {
        getDeviceManager().setAlwaysTranscode(AlwaysTranscode);
    }

    public int getTranscodeBitrate() {
        return getDeviceManager().getTranscodeBitrate();
    }

    public void setTranscodeBitrate(int TranscodeBitrate) {
        getDeviceManager().setTranscodeBitrate(TranscodeBitrate);
    }
    
    /**
     * Enable or disable GUI
     * @param enabled if true the GUI is enabled else disabled
     */
    public void enableGUI(boolean enabled){
            this.setEnabled(enabled);
    }

    public DynamicDeviceTreePopUp getTreePopUp() {
        return treePopUp;
    }

    public DeviceManager getManager() {
        return getDeviceManager();
    }
    
    public javax.swing.JProgressBar getDeviceActionProgressBar() {
        return deviceActionProgressBar;
    }

    public boolean Ismounted() {
        return getDeviceManager().isMounted();
    }
    
    // Javadoc is given in the implemented interface "NWGenericListener"
    public void transferInitialization(int numberOfExportFiles, int numberOfDeleteFiles, int numberOfDecodeFiles, int numberOfEncodeFiles, int numberOfTransferFiles, int numberOfDbFiles) {
        // Store the new state
        transferState = TRANSFER_IN_PROGRESS;

        // Initialize progress bar values
        transferProgressValue = 0;
        exportProgressValue = 0;
        deleteProgressValue = 0;
        importProgressValue = 0;
        updateProgressValue = 0;

        // Initialize progress bars
        transferProgressBar.setValue(0);
        exportProgressBar.setValue(0);
        deleteProgressBar.setValue(0);
        importProgressBar.setValue(0);
        updateProgressBar.setValue(0);
        fileProgressBar.setValue(0);

        // Initialize label
        currentFileLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.total_progress")+": ");
        exportLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Exporting")+": ");
        deleteLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Deleting")+": ");
        importLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Importing")+": ");
        updateLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Updating_database")+": ");
        closeButton.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Stop_after_current_action"));

        // Initialized the width of the label
        labelAuthorizedWidth = deviceContentPanel.getWidth() - 115;
/* nicolas: I tried to solve the oscillation panel problem... it seems OK when the window is not maximized, but when maximized, there are still problem...
//        labelAuthorizedWidth = deviceContentPanel.getWidth() - 120;
        currentFileLabel.setMaximumSize(new Dimension(labelAuthorizedWidth+5, currentFileLabel.getHeight()));
        currentFileLabel.setMinimumSize(new Dimension(labelAuthorizedWidth+5, currentFileLabel.getHeight()));
        exportLabel.setMaximumSize(new Dimension(labelAuthorizedWidth+5, exportLabel.getHeight()));
        exportLabel.setMinimumSize(new Dimension(labelAuthorizedWidth+5, exportLabel.getHeight()));
        deleteLabel.setMaximumSize(new Dimension(labelAuthorizedWidth+5, deleteLabel.getHeight()));
        deleteLabel.setMinimumSize(new Dimension(labelAuthorizedWidth+5, deleteLabel.getHeight()));
        importLabel.setMaximumSize(new Dimension(labelAuthorizedWidth+5, importLabel.getHeight()));
        importLabel.setMinimumSize(new Dimension(labelAuthorizedWidth+5, importLabel.getHeight()));
        decodeLabel.setMaximumSize(new Dimension(labelAuthorizedWidth+5, decodeLabel.getHeight()));
        decodeLabel.setMinimumSize(new Dimension(labelAuthorizedWidth+5, decodeLabel.getHeight()));
        encodeLabel.setMaximumSize(new Dimension(labelAuthorizedWidth+5, encodeLabel.getHeight()));
        encodeLabel.setMinimumSize(new Dimension(labelAuthorizedWidth+5, encodeLabel.getHeight()));
        updateLabel.setMaximumSize(new Dimension(labelAuthorizedWidth+5, updateLabel.getHeight()));
        updateLabel.setMinimumSize(new Dimension(labelAuthorizedWidth+5, updateLabel.getHeight()));
 */

        // Set the max values of the progress bars
        exportProgressBar.setMaximum(numberOfExportFiles + 1); // "+1" is used to not have a full progress bar when the last file is in progress
        deleteProgressBar.setMaximum(numberOfDeleteFiles + 1);
        importProgressBar.setMaximum(numberOfDecodeFiles*3 + numberOfEncodeFiles*2 + numberOfTransferFiles + 1); // Files to decode counts three times: once to decode, once to encode and once to transfer
        updateProgressBar.setMaximum(numberOfDbFiles + 1);
        transferProgressBar.setMaximum(numberOfExportFiles + numberOfDeleteFiles + importProgressBar.getMaximum()); // there is not "+1" here since there is one in the "import progress bar"
        fileProgressBar.setMaximum(100);

        // Show and hide component for startup
        initTransferFrame();
        if(wnd != null)
            wnd.setTransferState(true);
    }

    // Javadoc is given in the implemented interface "NWGenericListener"
    public void transferTermination() {
        closeButton.setText(java.util.ResourceBundle.getBundle("localization/misc").getString("global.Close"));
        transferProgressBar.setValue(transferProgressBar.getMaximum());
        currentFileLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.All_finished"));
        transferState = TRANSFER_OVER;
        if(wnd != null)
            wnd.setTransferState(false);
    }

    // Javadoc is given in the implemented interface "NWGenericListener"
    public void transferStepStarted(int step) {
        switch(step){
            case NWGenericListener.EXPORTING:
                exportLabel.setVisible(true);
                exportProgressBar.setVisible(true);
                break;
            case NWGenericListener.IMPORTING:
                importLabel.setVisible(true);
                importProgressBar.setVisible(true);
                break;
            case NWGenericListener.DELETING:
                deleteLabel.setVisible(true);
                deleteProgressBar.setVisible(true);
                break;
            case NWGenericListener.UPDATING:
                updateLabel.setVisible(true);
                updateProgressBar.setVisible(true);
                break;
        }
    }
    
    // Javadoc is given in the implemented interface "NWGenericListener"
    public void transferStepFinished(int step, String errorMessage) {
        switch(step){
            case NWGenericListener.EXPORTING:
                exportProgressBar.setValue(exportProgressBar.getMaximum());
                exportProgressBar.setVisible(false);
                if(errorMessage.length() <= 0){
                    exportLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Export_finished"));
                    exportLabel.setVisible(false);
                }
                else{
                    exportLabel.setText(errorMessage);
                }
                break;
            case NWGenericListener.IMPORTING:
                importProgressBar.setValue(importProgressBar.getMaximum());
                importProgressBar.setVisible(false);
                if(errorMessage.length() <= 0){
                    importLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Import_finished"));
                    importLabel.setVisible(false);
                }
                else{
                    // If decoding/encoding error are written, keep them, else, overwrite the text
                    if(importLabel.getText().compareTo(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Importing")+": ") == 0){
                        importLabel.setText(errorMessage);
                    }
                    else{
                        importLabel.setText(importLabel.getText().replace("</html>", "") + errorMessage.replace("<html>", "<br>"));
                    }
                }
                break;
            case NWGenericListener.DELETING:
                deleteProgressBar.setValue(deleteProgressBar.getMaximum());
                deleteProgressBar.setVisible(false);
                if(errorMessage.length() <= 0){
                    deleteLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Deleting_finished"));
                    deleteLabel.setVisible(false);
                }
                else{
                    deleteLabel.setText(errorMessage);
                }
                break;
            case NWGenericListener.ENCODING:
                encodedFileLabel.setVisible(false);
                encodedFileProgressBar.setVisible(false);
            case NWGenericListener.DECODING:
                decodedFileLabel.setVisible(false);
                decodedFileProgressBar.setVisible(false);
                if(errorMessage.length() > 0){
                    // If something was written, keep it, else, overwrite the text
                    if(importLabel.getText().compareTo(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Importing")+": ") == 0){
                        importLabel.setText(errorMessage);
                    }
                    else{
                        importLabel.setText(importLabel.getText() + errorMessage);
                    }
                    importLabel.setVisible(true);
                }
                break;
            case NWGenericListener.UPDATING:
                updateProgressBar.setValue(updateProgressBar.getMaximum());
                updateProgressBar.setVisible(false);
                fileLabel.setVisible(false);
                if(errorMessage.length() <= 0){
                    updateLabel.setText(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Database_is_up_to_date"));
                    updateLabel.setVisible(false);
                }
                else{
                    updateLabel.setText(errorMessage);
                }
                break;
        }

    }

    /**
    * This method shortens text to wisth of the control
    * @param text
    * @param label
    * @return shortened text
    */
    private String wrapText(String text, JLabel label){
        Boolean hasBeenChanged = false;

        while (labelAuthorizedWidth < label.getFontMetrics(label.getFont()).stringWidth(text + "...")){
            if(text.length()>10){
                text = (String) text.subSequence(0, text.length()-5);
                hasBeenChanged = true;
            }else{
                break;
            }
        }
        
        if (hasBeenChanged)
            return text + "...";
        else 
            return text;
    }

    // Javadoc is given in the implemented interface "NWGenericListener"
    public void fileChanged(int step, String name) {
        // Update the overall progress bar (except in update stage)
        if(step != NWGenericListener.UPDATING) {
            transferProgressBar.setValue(transferProgressValue);
            transferProgressValue++; // Increase value for next file
        }

        // Update file in progress field (this step is different for decode and encode step since they have their own progress bars)
        switch(step){
            case NWGenericListener.EXPORTING:
            case NWGenericListener.DELETING:
            case NWGenericListener.IMPORTING:
            case NWGenericListener.UPDATING:
                // Update the file name
                fileLabel.setText("<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.File_in_progress") + ":<br>" + wrapText(name,fileLabel));
                // Reset the file progress bar
                fileProgressBar.setValue(0);
                // Show label and progress bar
                fileLabel.setVisible(true);
                fileProgressBar.setVisible(true);
                break;
            case NWGenericListener.DECODING:
                // Update the file name
                decodedFileLabel.setText("<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.File_currently_decoded") + ":<br>"+ wrapText(name,decodedFileLabel));
                // Reset the file progress bar
                decodedFileProgressBar.setValue(0);
                // Show label and progress bar
                decodedFileLabel.setVisible(true);
                decodedFileProgressBar.setVisible(true);
                break;
            case NWGenericListener.ENCODING:
                // Update the file name
                encodedFileLabel.setText("<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.File_currently_encoded") + ":<br>"+ wrapText(name,encodedFileLabel));
                // Reset the file progress bar
                encodedFileProgressBar.setValue(0);
                // Show label and progress bar
                encodedFileLabel.setVisible(true);
                encodedFileProgressBar.setVisible(true);
                break;
        }

        // Search for the particular bar to be increased
        switch(step){
            case NWGenericListener.EXPORTING:
                exportProgressBar.setValue(exportProgressValue);
                exportProgressValue++;
                break;
            case NWGenericListener.DELETING:
                deleteProgressBar.setValue(deleteProgressValue);
                deleteProgressValue++;
                break;
            case NWGenericListener.IMPORTING:
                importProgressBar.setValue(importProgressValue);
                importProgressValue++;
                break;
            case NWGenericListener.DECODING:
                importProgressBar.setValue(importProgressValue);
                importProgressValue++;
                break;
            case NWGenericListener.ENCODING:
                importProgressBar.setValue(importProgressValue);
                importProgressValue++;
                break;
            case NWGenericListener.UPDATING:
                updateProgressBar.setValue(updateProgressValue);
                updateProgressValue++;
                break;
        }
    }

    // Javadoc is given in the implemented interface "NWGenericListener"
    public void fileProgressChanged(int step, double value, double speed) {
        // The progress bar to be updated depends on the step
        switch(step){
            case NWGenericListener.EXPORTING:
            case NWGenericListener.DELETING:
            case NWGenericListener.IMPORTING:
            case NWGenericListener.UPDATING:
                // Only update the progress bar if the task is not finished (<99.9%)
                if(value < 99.9) {
                    // Update the value of the progress bar
                    fileProgressBar.setValue((int) value);
                    // Update the speed of the progress bar
                    if(speed > 0) { fileProgressBar.setString(((int) speed) + " kb/s"); }
                    else { fileProgressBar.setString(" "); }
                }
                else {
                    // Else, hide the progress bar and the corresponding label
                    fileLabel.setVisible(false);
                    fileProgressBar.setVisible(false);
                }
                break;
            case NWGenericListener.DECODING:
                // Only update the progress bar if the task is not finished (<99.9%)
                if(value < 99.9) {
                    // Update the value of the progress bar
                    decodedFileProgressBar.setValue((int) value);
                    // Update the speed of the progress bar
                    if(speed > 0) {decodedFileProgressBar.setString(((int) speed) + " kb/s");}
                    else { decodedFileProgressBar.setString(" ");}
                }
                else {
                    // Else, hide the progress bar and the corresponding label
                    decodedFileLabel.setVisible(false);
                    decodedFileProgressBar.setVisible(false);
                }
                break;
            case NWGenericListener.ENCODING:
                // Only update the progress bar if the task is not finished (<99.9%)
                if(value < 99.9) {
                    // Update the value of the progress bar
                    encodedFileProgressBar.setValue((int) value);
                    // Update the speed of the progress bar
                    if(speed > 0) { encodedFileProgressBar.setString(((int) speed) + " kb/s");}
                    else {encodedFileProgressBar.setString(" ");}
                }
                else {
                    // Else, hide the progress bar and the corresponding label
                    encodedFileLabel.setVisible(false);
                    encodedFileProgressBar.setVisible(false);
                }
                break;
        }
    }

    // Javadoc is given in the implemented interface "NWGenericListener"
    public void loadingProgresChanged(double value) {
        deviceActionProgressBar.setValue((int) value);
        
        // If the loading is over, show the tree
        if(value >= deviceActionProgressBar.getMaximum()) {
            // Show the device content
            deviceTree.setVisible(true);
            // The progress bar, used to show the number of files managed should be now used to display the space left on the device
            deviceActionProgressBar.setMaximum(100);
            if(Ismounted()){
                displayDeviceSpace();
                reloadTree();
            }
        }
    }

    // Javadoc is given in the implemented interface "NWGenericListener"
    public void loadingInitialization(int numberOfFile) {
        // Hide the device tree
        deviceTree.setVisible(false);

        // Set the maximum of the bar
        if(numberOfFile == 0) {
            // If there is no file to load, forced termination process
            deviceActionProgressBar.setMaximum(100);
            loadingProgresChanged(100);
        }
        else {
            // Else, configure the bar
            deviceActionProgressBar.setMaximum(numberOfFile);
            deviceActionProgressBar.setValue(0);
        }
        // Change it's text
        deviceActionProgressBar.setString(java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Loading_items"));
    }

    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    /**
     * @param wnd the wnd to set
     */
    public void setWnd(JSymphonicWindow wnd) {
        this.wnd = wnd;
    }

    public void showPlayerFullMsg() {
        // Show a warning message
        JOptionPane.showMessageDialog(this,java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.PlayerFull"), java.util.ResourceBundle.getBundle("localization/misc").getString("global.warning"), JOptionPane.WARNING_MESSAGE);
        // Force the end of the loading process
        loadingProgresChanged(deviceActionProgressBar.getMaximum());
    }
}
