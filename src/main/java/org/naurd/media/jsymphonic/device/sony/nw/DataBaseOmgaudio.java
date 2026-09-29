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
 * DataBaseOmgaudio.java
 *
 * Created on 20 mai 2007, 10:12
 *
 */

package org.naurd.media.jsymphonic.device.sony.nw;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.toolBox.DataBaseOmgaudioToolBox;

/**
 * An instance of the DataBaseOmgaudio class describes a dataBase on a 3rd generation of Sony devices. This generation has the following particularities:
 * - database is containing in a OMGAUDIO folder
 * - no cover
 * - no intelligent features
 * - protected players
 *
 * Example of 3rd generation players: NW-HD5, NW-E10x,...
 *
 * 
 * 
 * 
 * @author Nicolas Cardoso
 * @author Daniel Žalar - added events to ensure GUI independancy
 * @version 06.06.2007
 */
public class DataBaseOmgaudio extends DataBase{
/* FIELDS */
    private java.io.File omgaudioDir; // OMGAUDIO directory containing all the files for the database
    private boolean gotKey; // True if generation is 0 to 3, false for generation 4 and above
    private boolean gotPlaylist; // True if device can read playlist
    private boolean gotIntelligentFeatures; // True if device has intelligent features
    private boolean gotCovers; // True if device can display covers
    private boolean gotSportFeatures; // True if device has sport features
 
    //Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.system.sony.nw.OmaDataBaseGen3");
      
    
/* CONSTRUCTORS */
    /**
     * Allows to create an instance of DataBaseOmgaudio from an existing device.
     * The database, once created is empty. Titles list can be filled with the method "addTitle". Before been written to the device, the database should be updated, with "update" method, to fill in the artists, albums,... lists.
     *
     *@param omgaudioDir The directory OMGAUDIO from the device.
     *
     * @author nicolas_cardoso
     */
    public DataBaseOmgaudio(java.io.File omgaudioDir, boolean gotPlaylist, boolean gotIntelligentFeatures, boolean gotCovers, boolean gotSportsFeatures, boolean gotKey) {
        // Call the super contructor
        super();
        
        // Save the base directory
        this.omgaudioDir = omgaudioDir;
        // Save the info
        this.gotPlaylist = gotPlaylist;
        this.gotIntelligentFeatures = gotIntelligentFeatures;
        this.gotCovers = gotCovers;
        this.gotSportFeatures = gotSportsFeatures;
        this.gotKey = gotKey;
    }
   

/* METHODS */ 
    /**
     * Update the database, complete all the other list (than the title list).
     *
     *@author nicolas_cardoso
     */
    public void updateTitleKey() {
        List titlesList; // the list of the titles (in a list object)
        Title titleToUpdate; // the title currently scanned
        Iterator it;
               
        // Get all the title in a good order
        titlesList = DataBaseOmgaudioToolBox.sortByArtistAlbumTitleNumber(titles);
        it = titlesList.iterator();
        
        // For each title
        while( it.hasNext() ) {
            // Get the title
            titleToUpdate = (Title)it.next();
            
            // If the title's status is TOENCODE or TODECODE, it means that another similar title exist in the list (the encoded title), so this one should be removed
            if(titleToUpdate.getStatus() == Title.TO_ENCODE || titleToUpdate.getStatus() == Title.TO_DECODE || titleToUpdate.getStatus() == Title.TO_ENCODE_AND_DELETE) {
System.out.println("THERE'S ONE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
System.out.println(titleToUpdate);
                delete(titleToUpdate); // remove the title and its path
                continue; // go to next title 
            }
        }
    }
    
    
    /**
     * Write the database to the player.
     *
     *@param genNw The instance of the Net walkman.
     *
     *@author nicolas_cardoso
     */
    public void write(NWGeneric genNw) {
        String errorMessage = ""; // the error message, if any

        // Inform GUI
        genNw.sendTransferStepStarted(NWGenericListener.UPDATING);
        
        //Backup the database if it is not already done
        File backupDir = new File(omgaudioDir + "/db_backup");
        if(!backupDir.exists()){
            logger.info("Backup of the database has not been found, it is created now.");
            backupDir.mkdir(); // Create the backup directory
            String[] databaseFiles = omgaudioDir.list(); // Search all the files in the database

            for(int i=0; i<databaseFiles.length; i++) { // For each files in the OMGAUDIO folder
                String databaseFileName = databaseFiles[i]; // get the name of the file/folder

                // Do not backup music fodlers (which the name starts with 10F) (also ignore the backup folder)
                if(databaseFileName.toLowerCase().contains("10f") || databaseFileName.toLowerCase().contains("db_backup")){
                    continue;
                }

                File databaseFile = new File(omgaudioDir + "/" + databaseFileName);
                File databaseFileCopy = new File(omgaudioDir + "/db_backup/" + databaseFileName);

                // If current object is a folder, copy the content (only one level deep is enough)
                if(databaseFile.isDirectory()){
                    databaseFileCopy.mkdir(); // Create the destination folder
                    String[] databaseSubFiles = databaseFile.list(); // Search all the files in the folder

                    for(int j=0; j<databaseSubFiles.length; j++) { // For each files in the sub folder
                        String databaseSubFileName = databaseSubFiles[j]; // get the name of the file/folder

                        File databaseSubFile = new File(omgaudioDir + "/" + databaseFileName + "/" + databaseSubFileName);
                        File databaseSubFileCopy = new File(omgaudioDir + "/db_backup/" + databaseFileName + "/" + databaseSubFileName);
                        // Only copy the objecy if it is a file
                        if(databaseSubFile.isFile()){
                            try {
                                copyFile(databaseSubFile, databaseSubFileCopy);
                            } catch (IOException ex) {
                                logger.severe("Error while creating backup of the database. File cannot be copied: "+databaseSubFile.getPath());
                                errorMessage =  java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_copy_old_db") + "<br>";
                            }
                        }
                    }
                }
                else {
                    // Else, if it is a file, copy it
                    try {
                        copyFile(databaseFile, databaseFileCopy);
                    } catch (IOException ex) {
                        logger.severe("Error while creating backup of the database. File cannot be copied: "+databaseFile.getPath());
                        errorMessage =  java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_copy_old_db") + "<br>";
                    }
                }
            }
        }
        
        // Erase all files in the OMGAUDIO folders (which are database files, and the database is going to be re-written) exepct the key file DvID.DAT, ffmpeg and the backup of the db
        String[] databaseFiles = omgaudioDir.list(); // Search all the files in the database
        boolean error = false;
        for(int i=0; i<databaseFiles.length; i++) {
            String databaseFileName = databaseFiles[i];
            // If the file is not the key files or the FFMPEG exe, delete it (file starting with "." are ignore)
            if((databaseFileName.toLowerCase().compareTo("dvid.dat") == 0) || (databaseFileName.toLowerCase().compareTo("ffmpeg.exe") == 0) || (databaseFileName.toLowerCase().compareTo("pthreadGC2.dll") == 0) || (databaseFileName.toLowerCase().compareTo("ffmpeg-win32.zip") ==0 ) || (databaseFileName.toLowerCase().compareTo("ffmpeg-win32") == 0) || (databaseFileName.toLowerCase().compareTo("30grct") == 0) || (databaseFileName.toLowerCase().compareTo("0001001d.dat") == 0) || (databaseFileName.toLowerCase().compareTo("00010021.dat") == 0) || (databaseFileName.toLowerCase().contains("srcidlst"))  || (databaseFileName.compareTo("db_backup") == 0)  || (databaseFileName.toLowerCase().contains("10f")) || (databaseFileName.toLowerCase().contains("jsymphonic")) || (databaseFileName.toLowerCase().startsWith(".")) ){
                continue;
            }

            File databaseFile = new File(omgaudioDir + "/" + databaseFileName);
            try {
                recursifDelete(databaseFile);
            } catch (IOException ex) {
                logger.severe("Error while deleting the old database. File/Folder cannot be deleted: "+databaseFile.getPath());
                error = true;
            }
        }
        
        // Log error for the GUI
        if(error){
            errorMessage +=  java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_delete_old_db") + "<br>";
        }
        // Update titleKey map
        updateTitleKey();

        // Write base info in the database (titles, artist, album, genre)
        errorMessage += writeBaseInfo(genNw);

        // Write playlist (if concerned)
        if(gotPlaylist){
            errorMessage += writePlaylist(genNw);
        }

        // Write intelligent features (if concerned)
        if(gotPlaylist){
            errorMessage += writeIntelligentFeatures(genNw);
        }

        // Write covers (if concerned)
        if(gotPlaylist){
            errorMessage += writeCovers(genNw);
        }

        // Write sport features (if concerned)
        if(gotPlaylist){
            errorMessage += writeSportFeatures(genNw);
        }

        // Database is up to date, inform GUI
        if(errorMessage.length() > 0){
            errorMessage = "<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Errors_during_update") + ":<br>" + errorMessage + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.consult_log") + ".";
            genNw.sendTransferStepFinished(NWGenericListener.UPDATING, errorMessage);
        }
        else {
            genNw.sendTransferStepFinished(NWGenericListener.UPDATING, "");
        }
    }

    /**
     * Write base info to the device. These info concern every generations.
     * Base info are "title, artist, album and genre".
     * Concerned files are "00GRTLST, 01TREE01, 01TREE02, 01TREE03, 01TREE04, 01TREE22, 01TREE2D, 02TREINF, 03GINF01, 03GINF02, 03GINF03, 03GINF04, 03GINF22, 03GINF2D, 04CNTINF, 05CIDLST"
     *
     * @param genNw instance of the generic device, used to inform the GUI of the progress of the task
     * @return Error messages written in a String. If no error occured, the string is empty. This String is meant to be displayed in the GUI to warn the user at the end of the task.
     */
    private String writeBaseInfo(NWGeneric genNw){
        String errorMessage = ""; // Store errors that may occured

        // Write 00GRTLST file (info about the database)
        try {
            DataBaseOmgaudioToolBox.write00GRTLST(omgaudioDir);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 00GRTLST file: " + ex.getMessage());
            errorMessage =  "00GRTLST" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "OOGRTLST");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);

        // Write 01TREE01 and 03GINF01 files (info about the titles)
        try {
            DataBaseOmgaudioToolBox.write01TREE01and03GINF01(omgaudioDir, titles);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 01TREE01 and 03GINF01 files: " + ex.getMessage());
            errorMessage +=  "01TREE01" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
            errorMessage +=  "03GINF01" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "01TREE01");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);
        genNw.sendFileChanged(NWGenericListener.UPDATING, "03GINF01");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);


        // Write 01TREE02 and 03GINF02 files (info about the artists)
        try {
            DataBaseOmgaudioToolBox.write01TREE02and03GINF02(omgaudioDir, titles);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 01TREE02 and 03GINF02 files: " + ex.getMessage());
            errorMessage +=  "01TREE02" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
            errorMessage +=  "03GINF02" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "01TREE02");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);
        genNw.sendFileChanged(NWGenericListener.UPDATING, "03GINF02");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);

        // Write 01TREE03 and 03GINF03 files (info about the albums)
        try {
            DataBaseOmgaudioToolBox.write01TREE03and03GINF03(omgaudioDir, super.titles);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 01TREE03 and 03GINF03 files: " + ex.getMessage());
            errorMessage +=  "01TREE03" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
            errorMessage +=  "03GINF03" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "01TREE03");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);
        genNw.sendFileChanged(NWGenericListener.UPDATING, "03GINF03");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);

        // Write 01TREE04 and 03GINF04 files (info about the genres)
        try {
            DataBaseOmgaudioToolBox.write01TREE04and03GINF04(omgaudioDir, titles);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 01TREE04 and 03GINF04 files: " + ex.getMessage());
            errorMessage +=  "01TREE04" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
            errorMessage +=  "03GINF04" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "01TREE04");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);
        genNw.sendFileChanged(NWGenericListener.UPDATING, "03GINF04");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);

        // Write 01TREE22 and 03GINF22 files
        try {
            DataBaseOmgaudioToolBox.write01TREE22and03GINF22(omgaudioDir);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 01TREE22 and 03GINF22 files: " + ex.getMessage());
            errorMessage +=  "01TREE22" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
            errorMessage +=  "03GINF22" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "01TREE22");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);
        genNw.sendFileChanged(NWGenericListener.UPDATING, "03GINF22");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);

        // Write 01TREE2D and 03GINF2D files
        try {
            DataBaseOmgaudioToolBox.write01TREE2Dand03GINF2D(omgaudioDir, titles);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 01TREE2D and 03GINF2D files: " + ex.getMessage());
            errorMessage +=  "01TREE2D" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
            errorMessage +=  "03GINF2D" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "01TREE2D");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);
        genNw.sendFileChanged(NWGenericListener.UPDATING, "03GINF2D");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);

        // Write 02TREINF file (list of the keys)
        try {
            DataBaseOmgaudioToolBox.write02TREINF(omgaudioDir, titles);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 02TREINF file: " + ex.getMessage());
            errorMessage +=  "02TREINF" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "02TREINF");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);


        // Write 04CNTINF file (list of the titles)
        try {
            DataBaseOmgaudioToolBox.write04CNTINF(omgaudioDir, titles, gotKey);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 04CNTINF file: " + ex.getMessage());
            errorMessage +=  "04CNTINF" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "04CNTINF");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);


        // Write 04PATLST file (list of the paths)
        try {
            DataBaseOmgaudioToolBox.write04PATLST(omgaudioDir, paths);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 04PATLST file: " + ex.getMessage());
            errorMessage +=  "04PATLST" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "04PATLST");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);


        // Write 05CIDLST file
        try {
            DataBaseOmgaudioToolBox.write05CIDLST(omgaudioDir, titles);
        } catch (Exception ex) {
            logger.severe("ERROR: while writting 05CIDLST file: " + ex.getMessage());
            errorMessage +=  "05CIDLST" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_update_file_error") + "<br>";
        }

        // Update progress bar in GUI
        genNw.sendFileChanged(NWGenericListener.UPDATING, "05CIDLST");
        genNw.sendFileProgressChanged(NWGenericListener.UPDATING, 100, 0);

        return errorMessage;
    }

    /**
     * Write playlist info to the device.
     * Concerned files are "TODO"
     *
     * @param genNw instance of the generic device, used to inform the GUI of the progress of the task
     * @return Error messages written in a String. If no error occured, the string is empty. This String is meant to be displayed in the GUI to warn the user at the end of the task.
     */
    private String writePlaylist(NWGeneric genNw){
        // TODO
        return "";
    }

    /**
     * Write intelligent features info to the device.
     * Concerned files are "TODO"
     *
     * @param genNw instance of the generic device, used to inform the GUI of the progress of the task
     * @return Error messages written in a String. If no error occured, the string is empty. This String is meant to be displayed in the GUI to warn the user at the end of the task.
     */
    private String writeIntelligentFeatures(NWGeneric genNw){
        // TODO
        return "";
    }

    /**
     * Write covers info to the device.
     * Concerned files are "TODO"
     *
     * @param genNw instance of the generic device, used to inform the GUI of the progress of the task
     * @return Error messages written in a String. If no error occured, the string is empty. This String is meant to be displayed in the GUI to warn the user at the end of the task.
     */
    private String writeCovers(NWGeneric genNw){
        // TODO
        return "";
    }

    /**
     * Write sport features info to the device.
     * Concerned files are "TODO"
     *
     * @param genNw instance of the generic device, used to inform the GUI of the progress of the task
     * @return Error messages written in a String. If no error occured, the string is empty. This String is meant to be displayed in the GUI to warn the user at the end of the task.
     */
    private String writeSportFeatures(NWGeneric genNw){
        // TODO
        return "";
    }

    @Override
    public int getNumberOfFiles() {
        // todo Update this method taking into account the fact that covers, intelligent features... has to be written
        return 17;
    }

   /**
    * Copy a file from a source to a destination. This method concerns files only.
    *
    * @param src The source file.
    * @param dest The destination file.
    * @throws java.io.IOException
    */
   public static void copyFile(File src, File dest) throws IOException {
      if (!src.exists()) throw new IOException(
         "File not found '" + src.getAbsolutePath() + "'");
      BufferedOutputStream out = new BufferedOutputStream(
         new FileOutputStream(dest));
      BufferedInputStream in = new BufferedInputStream(
         new FileInputStream(src));
         
      byte[] read = new byte[128];
      int len = 128;
      while ((len = in.read(read)) > 0)
         out.write(read, 0, len);
      
      out.flush();
      out.close();
      in.close();
   }

   /*
    * Delete file or folder (and it sub files and sub folders)
    *
    * @param path The file or folder to delete.
    */
    public static void recursifDelete(File path) throws IOException {
        if (!path.exists()) throw new IOException("File not found '" + path.getAbsolutePath() + "'");
        if (path.isDirectory()) {
            File[] children = path.listFiles();
            for (int i=0; children != null && i<children.length; i++)
                recursifDelete(children[i]);
            if (!path.delete()) throw new IOException("No delete path '" + path.getAbsolutePath() + "'");
        }
        else if (!path.delete()) throw new IOException("No delete file '" + path.getAbsolutePath() + "'");
    }

    @Override
    public int buildPathList() {
        java.io.InputStream fileStream = null; // The stream used to read the data
        byte[] buffer2 = new byte[2];
        byte[] buffer150 = new byte[0x150 - 6];
        File sourceFile = new File(omgaudioDir.getPath() + File.separatorChar + "04PATLST.DAT");
        int numberOfTitles;
        String path;

        // Empty the list of paths
        paths.clear();
        
        // Search if file exist
        if(!sourceFile.exists()){
            // If file doesn't exist, try in lowercase
            sourceFile = new File(omgaudioDir.getPath() + File.separatorChar + "04patlst.dat");
            if(!sourceFile.exists()){
                // No file found, display a warning message
                logger.warning("04PATLST.DAT file cannot be found. Please regenerate the database.");
                return -1;
            }
        }

        try{
            //Opens the file in stream mode (must be in try-catch)
            fileStream = sourceFile.toURI().toURL().openStream();

            // Skip 0x24 bytes
            fileStream.skip(0x10);
            fileStream.skip(0x10);
            fileStream.skip(0x4);

            // Read the number of elements in the class
            fileStream.read(buffer2);
            numberOfTitles = DataBaseOmgaudioToolBox.bytes2int(buffer2); // Convert it to int

            // Skip bytes to first element
            fileStream.skip(0x0A);

            // Read each element
            for(int i=1; i <= numberOfTitles; i++){
                fileStream.skip(6); // Skip "TIT2" and the 3 bytes giving the text-encoding
                fileStream.read(buffer150);
                path = new String(DataBaseOmgaudioToolBox.rtrimZeros(buffer150),"UTF16"); // Convert to string
                path = path.replace(""+(char)0,""); //Remove empty characters from the TagValue (these characters are due to 16-bits encodage in Ea3Tag)

                if(path.compareTo("") != 0){
                    // If path is path, add it to the list
                    paths.put(i, path);
                }
                else{
                }
            }

        }
        catch(Exception e){

        }
        return 0;
    }
}
