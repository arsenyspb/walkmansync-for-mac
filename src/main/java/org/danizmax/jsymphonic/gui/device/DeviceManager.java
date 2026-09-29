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
 * DeviceManager.java
 * 
 */

package org.danizmax.jsymphonic.gui.device;

import org.danizmax.jsymphonic.toolkit.*;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.naurd.media.jsymphonic.device.sony.nw.NWEsys;
import org.naurd.media.jsymphonic.device.sony.nw.NWGeneric;
import org.naurd.media.jsymphonic.device.sony.nw.NWGenericListener;
import org.naurd.media.jsymphonic.device.sony.nw.NWOmgaudio;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.toolBox.JSymphonicMap;
import org.naurd.media.jsymphonic.toolBox.JSymphonicTreeCellRenderer;
import org.naurd.media.jsymphonic.toolBox.DataBaseOmgaudioToolBox;

/**
 * It implements the use of  NWGeneric class. It's main idea is to ease use of the JSymphonic API that provides managing the contents of a Sony (c) Walkman (c) device, in a GUI.
 * @author danizmax - Daniel Žalar (danizmax@gmail.com)
 * @author Nicolas Cardoso - refreshTreeDevice, exportSelectedTracks
 */
public class DeviceManager{

    public static final int ARTISTALBUMMODE = 0;
    public static final int ARTISTMODE = 1;
    public static final int ALBUMMODE = 2;
    public static final int GENREMODE = 3;
    
    private NWGeneric genericDevice;
    private int deviceGeneration = 0;
    private String devicePath = null;
    private String tempPath = null;
    private boolean mounted = false;
    private String exportPath = null;
    private String deviceName;
  //  private Map titlesInTree;
    private JTree deviceTree;
    private int TranscodeBitrate=128;
    private boolean devicePathValid = false;
    private boolean AlwaysTranscode = false;
    private boolean inImportState = false;
    private NWGenericListener listener;
    private static Logger logger = Logger.getLogger("org.danizmax.gui.DeviceManager");
    
    public DeviceManager(JTree deviceTree, NWGenericListener listener, String devicePath, int  deviceGeneration, String exportPath, String tempPath){
        this.exportPath = exportPath;
        this.listener = listener;
        this.devicePath = devicePath;
        this.deviceName = determineDeviceName();
        this.deviceGeneration = deviceGeneration;
        this.deviceTree = deviceTree;
        this.tempPath = tempPath;
    }
    
     /**
     * Mount or unmount the device
     * @param mount if true mount the device else unmount
     */
    public void mountDevice(boolean mount) {
        mounted = false;
        if(mount){
            getLogger().info("Mounting the device" + " \""+ deviceName + "\"");
            javax.swing.ImageIcon sourceIcon = new javax.swing.ImageIcon(getClass().getResource("/org/danizmax/jsymphonic/resources/vignette.png"));
            // Create the NWGeneric object according to the generation
            switch(deviceGeneration) {
                case NWGeneric.GENERATION_0:
                    logger.severe("Generation 0 is not implemented for the moment.");
                    break;
                case NWGeneric.GENERATION_1:
                    logger.severe("Generation 1 is not implemented for the moment.");
                    break;
                case NWGeneric.GENERATION_2:
                    genericDevice = new NWEsys(devicePath, deviceName, "", sourceIcon, NWGeneric.GENERATION_2, getListener(),getExportPath());
                    return;
                case NWGeneric.GENERATION_3:
                    genericDevice = new NWOmgaudio(devicePath, deviceName, "", sourceIcon, NWGeneric.GENERATION_3, getListener(),getExportPath());
                    break;
                case NWGeneric.GENERATION_4:
                    genericDevice = new NWOmgaudio(devicePath, deviceName, "", sourceIcon, NWGeneric.GENERATION_4, getListener(),getExportPath());
                    break;
                case NWGeneric.GENERATION_5:
                    genericDevice = new NWOmgaudio(devicePath, deviceName, "", sourceIcon, NWGeneric.GENERATION_5, getListener(),getExportPath());
                    break;
                case NWGeneric.GENERATION_6:
                    genericDevice = new NWOmgaudio(devicePath, deviceName, "", sourceIcon, NWGeneric.GENERATION_6, getListener(),getExportPath());
                    break;
                case NWGeneric.GENERATION_7:
                    genericDevice = new NWOmgaudio(devicePath, deviceName, "", sourceIcon, NWGeneric.GENERATION_7, getListener(),getExportPath());
                    break;
                case NWGeneric.GENERATION_VAIO:
                    genericDevice = new NWOmgaudio(devicePath, deviceName, "", sourceIcon, NWGeneric.GENERATION_VAIO, getListener(),getExportPath());
                    break;
                default:
                    logger.severe("Invalid generation.");
                    return;
            }


            if(genericDevice != null) {
                //pass on the parent logger
                NWGeneric.setParentLogger(logger);
                genericDevice.setAlwaysTranscode(AlwaysTranscode);
                genericDevice.setTranscodeBitrate(TranscodeBitrate);
                genericDevice.setTempPath(tempPath);
                mounted = true;
            }

            devicePathValid = genericDevice.initSourcePath();

            if(!devicePathValid){
                getLogger().warning("The device path is not valid! Please check the configuration!");
            }
        }else{
            genericDevice = null;
            getLogger().fine("Unmounting the device" + "  "+ deviceName);
        }
    }

    boolean initSourcePath() {
        return genericDevice.initSourcePath();
    }

    /**
     * Kill all transfer threads in progress.
     */
    void killAllTransferThreads() {
        genericDevice.killAllTransferThreads();
    }

    /**
     * Clear content of the device and reload titles from the device.
     */
    void refreshTitlesFromDevice() {
        genericDevice.reload();
    }

    /**
     * Stop the transfer properly, it stops the importation/exportation and deletion and tries to build the database
     */
    void stopTransfer() {
        genericDevice.stopApplyChanges();
    }
   
    private int refreshDeviceTreeGetAction(int status){
        if((status == Title.TO_IMPORT) || (status == Title.TO_DECODE) || (status == Title.TO_ENCODE) )
            return JSymphonicMutableTreeNode.ACTION_IMPORT;
        else if (status == Title.TO_EXPORT)
            return JSymphonicMutableTreeNode.ACTION_EXPORT;
        else if (status == Title.TO_DELETE)
            return JSymphonicMutableTreeNode.ACTION_DELETE;

        return JSymphonicMutableTreeNode.ACTION_LEAVE;
    }

      /**
      * Load items into the tree
      * @param treeDeviceView selected view of the tree
      */
     public void refreshDeviceTree(int treeDeviceView, String filterString) {
         if (!isMounted()) {
             getLogger().warning("The device is not mounted!");
             return;
         }
         if (genericDevice == null) { //If no instance of the sony device exist, just return
             getLogger().warning("No device instance exist!");
             return;
         }

         getLogger().info("Refreshing the device tree");
         //Local variables
         String titleName, artistName, albumName, genre;
         filterString = filterString.toLowerCase();

         JSymphonicMap titles = genericDevice.getTitlesInMap();
         HashMap artistsList = new HashMap(); //hasmap variables are used to save node while the tree is building
         HashMap albumsList = new HashMap();
         HashMap genresList = new HashMap();
         List sortedTitles;
         Title title;

         // Create root node:
         DefaultMutableTreeNode root = new JSymphonicMutableTreeNode(genericDevice);
         switch (treeDeviceView) { // the data displayed in the tree depend on the chosen view
             case ARTISTMODE:
                 sortedTitles = DataBaseOmgaudioToolBox.sortByArtistTitle(titles); // Sort the title according to the selected mode

                 Iterator itD = sortedTitles.iterator();
                 while (itD.hasNext()) {
                     // Get the current title
                     title = (Title) itD.next();
                     // Get its information
                     int action = refreshDeviceTreeGetAction(title.getStatus());

                     titleName = title.getTitle();
                     artistName = title.getArtist();

                     // added filtering (danizmax)
                     if (titleName.toLowerCase().contains(filterString) || artistName.toLowerCase().contains(filterString)) {
                         if (!(artistsList.containsKey(artistName))) {
                             // If key doesn't exist, it means that the node corresponding to this entry doesn't exist, let's create it
                             JSymphonicMutableTreeNode tempNode = new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_ARTIST, action);
                             artistsList.put(artistName, tempNode);
                             root.add(tempNode);
                         }
                         //Then, add the node
                         //To do so, let's read the node corresponding to the artist in the hasmap
                         JSymphonicMutableTreeNode tempTreeNode = (JSymphonicMutableTreeNode) artistsList.get(artistName);
                         //Add a leaf
                         tempTreeNode.add(new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_TITLE, action)); // Add the leaf
                         //The node with the new leaf is put back in the hasmap
                         artistsList.put(artistName, tempTreeNode);
                     }
                 }
                 break;

             case ALBUMMODE:
                 sortedTitles = DataBaseOmgaudioToolBox.sortByAlbumTitleNumber(titles); // Sort the title according to the selected mode

                 Iterator itAl = sortedTitles.iterator();
                 while (itAl.hasNext()) {
                     // Get the current title
                     title = (Title) itAl.next();
                     // Get its information
                     int action = refreshDeviceTreeGetAction(title.getStatus());

                     titleName = title.getTitle();
                     albumName = title.getAlbum();

                     // added filtering (danizmax)
                     if (titleName.toLowerCase().contains(filterString) || albumName.toLowerCase().contains(filterString)) {
                         if (!(albumsList.containsKey(albumName))) {
                             // If key doesn't exist, it means that the node corresponding to this entry doesn't exist, let's create it
                             JSymphonicMutableTreeNode tempNode = new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_ALBUM, action);
                             albumsList.put(albumName, tempNode);
                             root.add(tempNode);
                         }
                         //Then, add the node
                         //To do so, let's read the node corresponding to the artist in the hasmap
                         JSymphonicMutableTreeNode tempTreeNode = (JSymphonicMutableTreeNode) albumsList.get(albumName);
                         // Add the leaf
                         tempTreeNode.add(new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_TITLE, action)); // Add the leaf
                         //The node with the new leaf is put back in the hasmap
                         albumsList.put(albumName, tempTreeNode);

                     }
                 }
                 break;

             case GENREMODE:
                 sortedTitles = DataBaseOmgaudioToolBox.sortByGenreArtistAlbumTitle(titles);

                 Iterator itG = sortedTitles.iterator();
                 while (itG.hasNext()) {

                     // Get the current title
                     title = (Title) itG.next();
                     // Get its information
                     int action = refreshDeviceTreeGetAction(title.getStatus());

                     titleName = title.getTitle();
                     artistName = title.getArtist();
                     albumName = title.getAlbum();
                     genre = title.getGenre();

                     // added filtering (danizmax)
                     if (titleName.toLowerCase().contains(filterString) || albumName.toLowerCase().contains(filterString) || artistName.toLowerCase().contains(filterString)) {
                         if (!(genresList.containsKey(genre))) {
                             // If key doesn't exist, it means that the node corresponding to this entry doesn't exist, let's create it
                             JSymphonicMutableTreeNode tempNode = new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_GENRE, action);
                             genresList.put(genre, tempNode);
                             root.add(tempNode);
                         }

                         //  /!\ since one artist could have several genre, the artist name in the artistsList use genre+albumName

                         //Then, add the node artist if it doesn't exist
                         if (!(artistsList.containsKey(genre+artistName))) {
                             // If key doesn't exist, it means that the node corresponding to this entry doesn't exist, let's create it
                             //To do so, let's read the node corresponding to the genre in the hasmap
                             JSymphonicMutableTreeNode tempTreeNode = (JSymphonicMutableTreeNode) genresList.get(genre);

                             JSymphonicMutableTreeNode tempNode = new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_ARTIST, action);
                             tempTreeNode.add(tempNode);// Add the node

                             artistsList.put(genre+artistName, tempNode); // Update the hashmap
                         }

                         //  /!\ since several artist could have the same album name, the albums name in the albumsList use genre+artistName+albumName

                         //Then, add the node album if it doesn't exist
                         if (!(albumsList.containsKey(genre+artistName+albumName))) {
                             // If key doesn't exist, it means that the node corresponding to this entry doesn't exist, let's create it
                             //To do so, let's read the node corresponding to the artist in the hasmap
                             JSymphonicMutableTreeNode tempTreeNode = (JSymphonicMutableTreeNode) artistsList.get(genre+artistName);

                             JSymphonicMutableTreeNode tempNode = new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_ALBUM, action);
                             tempTreeNode.add(tempNode);// Add the node

                             albumsList.put(genre+artistName+albumName, tempNode); // Update the hashmap
                         }

                         //Then, add the leaf
                         //To do so, let's read the node corresponding to the album in the hasmap
                         JSymphonicMutableTreeNode tempTreeNode = (JSymphonicMutableTreeNode) albumsList.get(genre+artistName+albumName);
                         tempTreeNode.add(new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_TITLE, action)); // Add the node
                         //The node with the new leaf is put back in the hasmap
                         albumsList.put(genre+artistName+albumName, tempTreeNode);
                     }
                 }
                 break;
             default:
                 sortedTitles = DataBaseOmgaudioToolBox.sortByArtistAlbumTitleNumber(titles);

                 Iterator itAA = sortedTitles.iterator();
                 while (itAA.hasNext()) {
                     // Get the current title
                     title = (Title) itAA.next();
                     // Get its information
                     int action = refreshDeviceTreeGetAction(title.getStatus());
                     titleName = title.getTitle();
                     artistName = title.getArtist();
                     albumName = title.getAlbum();

                     // added filtering
                     if (titleName.toLowerCase().contains(filterString) || albumName.toLowerCase().contains(filterString) || artistName.toLowerCase().contains(filterString)) {
                         if (!(artistsList.containsKey(artistName))) {
                             // If key doesn't exist, it means that the node corresponding to this entry doesn't exist, let's create it
                             JSymphonicMutableTreeNode tempNode = new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_ARTIST, action);
                             artistsList.put(artistName, tempNode);
                             root.add(tempNode);
                         }

                         //  /!\ since several artist could have the same album name, the albums name in the albumsList use artistName+albumName

                         //Then, add the node album if it doesn't exist
                         if (!(albumsList.containsKey(artistName+albumName))) {
                             // If key doesn't exist, it means that the node corresponding to this entry doesn't exist, let's create it
                             //To do so, let's read the node corresponding to the artist in the hasmap
                             JSymphonicMutableTreeNode tempTreeNode = (JSymphonicMutableTreeNode) artistsList.get(artistName);

                             JSymphonicMutableTreeNode tempNode = new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_ALBUM, action);
                             tempTreeNode.add(tempNode);// Add the node

                             albumsList.put(artistName+albumName, tempNode); // Update the hashmap
                         }

                         //Then, add the leaf
                         //To do so, let's read the node corresponding to the album in the hasmap
                         JSymphonicMutableTreeNode tempTreeNode = (JSymphonicMutableTreeNode) albumsList.get(artistName+albumName);
                         tempTreeNode.add(new JSymphonicMutableTreeNode(title, JSymphonicMutableTreeNode.TYPE_TITLE, action)); // Add the node
                         //The node with the new leaf is put back in the hasmap
                         albumsList.put(artistName+albumName, tempTreeNode);
                     }
                 }
          }

         // Associate the tree model to the model and display it
         DefaultTreeModel model = (DefaultTreeModel) deviceTree.getModel();
         model.setRoot(root);
         //model.reload(root);

         // Put some style
         // Build default renderer
         deviceTree.setCellRenderer(new JSymphonicTreeCellRenderer());
    }
      
     
     
     
     /**
      * Schedule selected tracks fot deletion.
      */
     public void scheduleTrackDeletion(){
          if(isMounted()){
                try{  
                    //TreePath currentPath;
                    // Get the paths of the title to remove
                  //  TreePath[] pathsToRemove = deviceTree.getSelectionPaths();
                    Title[] tl = getSelectedTitles();
                    // Get all the existing paths
              //      Set pathsSet = titlesInTree.keySet();

                    // For each path, remove the titles
                    for(int i = 0; i < tl.length; i++){
                        //currentPath = pathsToRemove[i];
                        //JSymphonicMutableTreeNode tn = (JSymphonicMutableTreeNode) pathsToRemove[i].getLastPathComponent();
                         //logger.fine(pathsToRemove[i].toString());
                       // tn.scheduleTrackDeletion();
                        logger.fine(tl[i].toString());
                        genericDevice.scheduleDeletion(tl[i]);
                        deviceTree.repaint();
                        // As the current path can design a part of a real path (i.e. it can design an entire album, and not only a title), we need to search in all the path the ones which have to be deleted
                     //   Iterator it = pathsSet.iterator();

                    /*    while(it.hasNext()) { // For each existing saved path, if the current path is include in this path, the current path represent a title to delete 
                            String pathInSavedPathsList = (String)it.next();
                            if(pathInSavedPathsList.startsWith(currentPath.toString().replace("]", ""))) { // If the current title contains in its path the current path, it sould be deleted
                                    // Get the title from the global HashMap
                                    Title titleToRemove = (Title)titlesInTree.get(pathInSavedPathsList);                    

                                    // Remove title
                                    genericDevice.removeTitles(titleToRemove);  
                            }
                        }*/
                    }

                } catch (NullPointerException ex) {
                    logger.warning("Nothing to delete");
                }
            }
     }
     
     /**
      * This method applies all changes made to the device. This includes exports, imports and deletion.
      */
     public void applyChanges(){
         if(isMounted()){
            try{ 
                genericDevice.applyChanges(); //here actually adds the tracks!
            } catch (NullPointerException ex) { 
                logger.warning("Sony device not connected!");
            }
        }
     }
    
     /**
      * Schedule selected tracks for import 
      * @param files field of selected files 
      * @return >0 some files have been scheduled, 0 if no errors occurs or ni files scheduled, -1 if a file's extension is not recognized and -2 the device is full
      */
     public int scheduleTrackImport(File files[]){
        int res = 0;
         if(isMounted()){
            res = genericDevice.scheduleImport(files, true);
        }
        
        if(res > 0)
            inImportState = true;
        
        return res;
     }
     /**
      * Schedule selected tracks for export. The tracks are taken from DevicePanels JTree.
      * Tracks will be exported to the export path that can be set with method setExportPath(String exportPath).
      */
     public void scheduleTrackExport(){
        if(isMounted()){
            try{
                //TreePath currentPath;
                // Get the paths of the title to export
                //TreePath[] pathsToExport = deviceTree.getSelectionPaths();
                Title[] tl = getSelectedTitles();
                // Get all the existing paths
               /// Set pathsSet = titlesInTree.keySet();

                // For each path, export the titles
                for(int i = 0; i < tl.length; i++){
                  //  currentPath = pathsToExport[i];

                    
                     //   tn.scheduleTrackExport();
                        logger.fine(tl[i].toString());
                        genericDevice.scheduleExport(tl[i]);
                        deviceTree.repaint();
                    // As the current path can design a part of a real path (i.e. it can design an entire album, and not only a title), we need to search in all the path the ones which have to be exported
                 ///   Iterator it = pathsSet.iterator();
/*
                    while(it.hasNext()) { // For each existing saved path, if the current path is include in this path, the current path represent a title to export 
                        String pathInSavedPathsList = (String)it.next();
                        if(pathInSavedPathsList.startsWith(currentPath.toString().replace("]", ""))) { // If the current title contains in its path the current path, it sould be exported
                                // Get the title from the global HashMap
                                Title titleToExport = (Title)titlesInTree.get(pathInSavedPathsList);                    

                                // Export title
                                genericDevice.exportTitle(titleToExport);  
                        }
                    }*/
                }
            } catch (NullPointerException ex) {
               logger.warning("Nothing to export");
            }
        }
     }
     
     /**
      * Cancels all changes from the device
      */
     public void cancelChanges(){
         if(isMounted()){
            // Cancel changes in the devices
            inImportState = false; 
            genericDevice.reload();
         }
     }
      
     /**
     * This method loads all nodes into the HashMap
     * @param currPath path to be scanned
     * @param filter filter string to match file names
     * @param selectedFileList root of currently scanned path
     * @return all selected nodes in the HashMap
     */
    private HashMap getNodesFromSelection(JSymphonicMutableTreeNode currPath, String filter, HashMap selectedFileList){
        Enumeration children =  currPath.children();
        while(children.hasMoreElements()){ //iterate through childs
            JSymphonicMutableTreeNode ch = (JSymphonicMutableTreeNode) children.nextElement();
            if(ch.getAsTitle().getTitle().toLowerCase().contains(filter.toLowerCase())){
                if(ch.isLeaf()){
                    selectedFileList.put(ch, null); 
                }else{ //add a leaf element
                    selectedFileList.putAll(getNodesFromSelection(ch, filter, selectedFileList));
                }    
            }
        }
        return selectedFileList;
    }
    
    public String getDeviceName(){
        return deviceName;
    }
     
    public String getDevicePath() {
        return devicePath;
    }
     
    public void setDevicePath(String devicePath) {
        this.devicePath = devicePath;
        this.deviceName = determineDeviceName();
    }

    public int getDeviceGeneration() {
        return deviceGeneration;
    }

    public void setDeviceGeneration(int deviceGeneration) {
        this.deviceGeneration = deviceGeneration;
    }

  /*  public Map getTitlesInTree() {
        return titlesInTree;
    }*/
    
    public String getSpaceLeftInText(){
        return genericDevice.getSpaceLeftInText();
    }
    
    public int getSpaceLeftInRatio(){
        return genericDevice.getSpaceLeftInRatio();
    }

    /**
    * Returns selected Title objects
    * @return selected Title objects
    */    
    public Title[] getSelectedTitles(){
        TreePath[] tp = deviceTree.getSelectionPaths();
        HashMap pathList = new HashMap();
        
         //add all nodes and subnodes (childs) to pathList hashmap
         for(int i=0; i<tp.length;i++){
             JSymphonicMutableTreeNode tn = (JSymphonicMutableTreeNode)tp[i].getLastPathComponent();
            if( tn.isLeaf() ){
                pathList.put(tn, null);
            }else{
                pathList.putAll(getNodesFromSelection(tn, "", pathList));
            }
            
         }
        
        Title [] selectedTitles = new Title[pathList.size()];
        
        Set s = pathList.keySet();
        Iterator it = s.iterator();
        int i = 0;
        while(it.hasNext()){
            selectedTitles[i] = ((JSymphonicMutableTreeNode) it.next()).getAsTitle();
            //logger.fine(selectedTitles[i].toString());
            i++;
        }
        
        return selectedTitles;
    }
    
    public JSymphonicMutableTreeNode getLastSelectedNode(){
         return (JSymphonicMutableTreeNode)deviceTree.getLastSelectedPathComponent();
    }
    
    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public boolean isDevicePathValid() {
        return devicePathValid;
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

    public boolean isAlwaysTranscode() {
        return AlwaysTranscode;
    }

    public void setAlwaysTranscode(boolean AlwaysTranscode) {
        this.AlwaysTranscode = AlwaysTranscode;
        if(genericDevice != null)
            genericDevice.setAlwaysTranscode(AlwaysTranscode);
    }

    public int getTranscodeBitrate() {
        return TranscodeBitrate;
    }

    public void setTranscodeBitrate(int TranscodeBitrate) {
        this.TranscodeBitrate = TranscodeBitrate;
        if(genericDevice != null)
           genericDevice.setTranscodeBitrate(TranscodeBitrate);
    }

    public boolean isMounted() {
        return mounted;
    }

    public NWGenericListener getListener() {
        return listener;
    }

    public void setListener(NWGenericListener listener) {
        this.listener = listener;
    }

    public boolean isInImportState() {
        return inImportState;
    }

    private String determineDeviceName(){
        String deviceNameGuessed;

        // Try to determine device name from the device path
        File deviceDir = new File(devicePath);
        deviceNameGuessed = deviceDir.getName();

        // Check validity
        if((deviceNameGuessed.compareTo("") == 0) || (deviceNameGuessed.compareTo(".") == 0) || (deviceNameGuessed.compareTo("..") == 0) || (deviceNameGuessed.compareTo("../..") == 0) || ((deviceNameGuessed.length() == 2) && (deviceNameGuessed.contains(":"))) ){
            deviceNameGuessed = "Walkman";
        }

        return deviceNameGuessed;
    }
}