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
 * NWOmgaudio.java
 *
 * Created on 15 mai 2009, 14:31:51
 *
 */

package org.naurd.media.jsymphonic.device.sony.nw;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.naurd.media.jsymphonic.title.Oma;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.toolBox.Java6ToolBox;

/**
 * This class describe a Network Walkman with a database file based on a "OMGAUDIO" folder (generations 3 to 7).
 * It extends the NW generic class.
 *
 * @author skiron
 */
public class NWOmgaudio extends NWGeneric{
/* FIELDS */
    //Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.system.sony.nw.NWOmgaudio");

/* CONSTRUCTORS */
    /**
     * Create an instance of NWOmgaudio class. The object could be a Walkman from generation 3 to 7.
     *
     * @param devicePath The path of the device.
     * @param sourceName idem as super class
     * @param sourceDesc idem as super class
     * @param sourceIcon idem as super class
     * @param generation The generation of the device.
     * @param listener idem as super class
     * @param exportPath idem as super class
     */
    public NWOmgaudio(String devicePath, String sourceName, String sourceDesc, javax.swing.ImageIcon sourceIcon, int generation, NWGenericListener listener, String exportPath){
        // Call the super contructor
        super(sourceName, sourceDesc, sourceIcon, listener, exportPath);

        // Set up the device and database folders
        this.devicePath = devicePath;
        if( !initSourcePath() ) {
            JOptionPane.showMessageDialog(null
                    , java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_in_omgaudio_path")
                    , java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Reloading_tree")
                    , JOptionPane.ERROR_MESSAGE);
            logger.severe("Invalid OMGAUDIO directory.");
        }
        else{
            // Update space
            Java6ToolBox.FileSpaceInfo spaceInfo = Java6ToolBox.getFileSpaceInfo(source);
            usableSpace = spaceInfo.getUsableSpace();
            totalSpace = spaceInfo.getTotalSpace();

            // Set the generation
            this.generation = generation;
            
            // Set up the database according to the generation
            switch(generation){
                case GENERATION_3:
                    this.gotkey = true;
                    dataBase = new DataBaseOmgaudio(source, true, false, false, false, gotkey); // Got playlist
                    break;
                case GENERATION_4:
                    this.gotkey = false;
                    dataBase = new DataBaseOmgaudio(source, true, true, false, false, gotkey); // Got playlist, got intelligent features
                    break;
                case GENERATION_5:
                    this.gotkey = false;
                    dataBase = new DataBaseOmgaudio(source, true, false, false, false, gotkey); // Got playlist
                    break;
                case GENERATION_6:
                    this.gotkey = false;
                    dataBase = new DataBaseOmgaudio(source, true, false, false, true, gotkey); // Got playlist, got sport features
                    break;
                case GENERATION_7:
                    this.gotkey = false;
                    dataBase = new DataBaseOmgaudio(source, true, true, true, false, gotkey); // Got playlist, got intelligent features, got covers
                    break;
                case GENERATION_VAIO:
                    this.gotkey = true;
                    dataBase = new DataBaseOmgaudio(source, true, false, true, false, gotkey); // Got playlist, got covers
                    break;
                default:
                    logger.severe("Wrong generation number, program is existing ! (generation = "+generation+").");
                    System.exit(-1);
            }

            // Load the key file, if needed
            if(gotkey){
                // Load the key
                if(loadKey(devicePath) < 0){
                    // If the DvID.dat file is not found, we should display a warning message
                    JOptionPane.showMessageDialog(null
                            , java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.NoKeyFound")
                            , java.util.ResourceBundle.getBundle("localization/misc").getString("global.Error")
                            , JOptionPane.ERROR_MESSAGE);

                    // Since no keys have been found, all we can do is consider than the walkman doesn't need it...
                    this.gotkey = false;
                }
            }

            // Fill in the title's list
            loadTitlesFromDevice();
        }
    }

/* Abstract Methods implementation */
    /**
     *Export a list of titles from the device to the computer.
     *
     *@param titlesToCopy list of the titles to be exported.
     */
    protected void applyExport() {
        String artist, album, titleName, exportedFilePath, extention; // Title info
        int trackNumber, titleId, wrappedFormat; // Title info
        int titlesExported = 0; // Number of titles correctly exported
        int titlesNotExported = 0; // Number of titles not exported
        int unknownFormat = 0; // Number of titles not exported because the format is unknown
        int alreadyExist = 0; // Number of titles not exported because destination file already exists
        int fileCannotBeCreated = 0; // Number of titles not exported because destination file can't be created
        int copyError = 0; // Number of titles not exported because an error occured during the copy
        int transferStopped = 0; // Number of titles not exported because transfer has been stopped
        File directoryToExport, exportedFile; // The exported file is the new file
        Oma titleToExport; // The title to export is the existing file we want to copy localy

        // Only start step if there are titles
        if(titleVectorsMonitor.size(TitleVectorsMonitor.EXPORT_VECTOR) > 0 && !stopTransfer){
            // Inform GUI
            sendTransferStepStarted(NWGenericListener.EXPORTING);
       }
        else{// Else, there is nothing to do
            return;
        }

        // If the export path is not valid, return
        if( !((new File(getExportPath())).exists()) ) {
            logger.warning("Export path is not valid.");
            sendTransferStepFinished(NWGenericListener.EXPORTING, java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Export_path_not_valid")); // Inform GUI
            return;
        }

        while(!titleVectorsMonitor.isEmpty(TitleVectorsMonitor.EXPORT_VECTOR) && !stopTransfer ) { //for each title
            // Get the title to add
            titleToExport = (Oma)titleVectorsMonitor.firstElement(TitleVectorsMonitor.EXPORT_VECTOR);

            //// Create the file name and his path
            // First, get title information
            artist = titleToExport.getArtist().replace("/","-").replace("?"," ");
            album = titleToExport.getAlbum().replace("/","-").replace("?"," ");
            titleName = titleToExport.getTitle().replace("/","-").replace("?"," ");
            trackNumber = titleToExport.getTitleNumber();

            // Build directory to copy name (artist)
            directoryToExport = new File (getExportPath() + "/" + artist);

            // Create directory to copy is it's not existing
            if( !directoryToExport.exists() ) {
                directoryToExport.mkdir(); // If the directory don't exist, create it (default is uppercase)
            }

            // Build directory to copy name (artist/album)
            directoryToExport = new File (getExportPath() + "/" + artist + "/" + album);

            // Create directory to copy is it's not existing
            if( !directoryToExport.exists() ) {
                directoryToExport.mkdir(); // If the directory don't exist, create it (default is uppercase)
            }

            // Search the extension of the new file
            wrappedFormat = titleToExport.getFormatWrapped();
            switch(wrappedFormat){
                case Title.OMA:
                    extention = "oma";
                    break;
                case Title.MP3:
                    extention = "mp3";
                    break;
                case Title.WMA:
                    extention = "wma";
                    break;
                case Title.AAC:
                    extention = "m4a";
                    break;
                default:
                    // Title format is not as expected
                    logger.severe("Title format is of file "+titleToExport.getSourceFile().getPath()+" not as expected. File cannot be exported.");
                    titlesNotExported++;
                    unknownFormat++;
                    continue;
            }

            // Build file name
            if(trackNumber != 0) {
                if(trackNumber < 10) {
                    exportedFilePath = artist + "/" + album + "/0" + trackNumber + "-" + titleName + "." + extention;
                }
                else {
                    exportedFilePath = artist + "/" + album + "/" + trackNumber + "-" + titleName + "." + extention;
                }
            }
            else {
                exportedFilePath = artist + "/" + album + "/" + titleName + "." + extention;
            }

            // Determine the title ID of the title, it corresponds to the name of the OMA file
            String titleIdString = titleToExport.getSourceFile().getName().toLowerCase();
            titleIdString = titleIdString.replace(".oma",""); // remove the extension
            titleIdString = titleIdString.replaceFirst("1",""); // remove the 1 begining all the .oma file
            titleId = Integer.parseInt(titleIdString, 16); // parse integer from a text in HexaDecimal

            // Create exported file
            exportedFile = new File (getExportPath() + "/" + exportedFilePath);

            // Inform GUI
            sendFileChanged(NWGenericListener.EXPORTING, exportedFilePath);

            // Check if title doesn't already exist at this adress
            if(exportedFile.exists()) {
                // If file exist, it won't be erase, inform the logger
                titlesNotExported++;
                alreadyExist++;
                titleVectorsMonitor.remove(TitleVectorsMonitor.EXPORT_VECTOR, titleToExport);
                logger.log(Level.WARNING,"Can't export the MP3 file '" + exportedFile.getName() + "', it already exists at the adress '" + exportedFile.getAbsolutePath() + "'." );
                continue;
            }
            else {
                // Else, file can be created
                try {
                    exportedFile.createNewFile();
                } catch (IOException ex) {
                    // An error occured
                    titlesNotExported++;
                    fileCannotBeCreated++;
                    titleVectorsMonitor.remove(TitleVectorsMonitor.EXPORT_VECTOR, titleToExport);
                    logger.log(Level.WARNING,"Can't create new file to export the file: '" + exportedFile.getName() + "' at the adress: '" + exportedFile.getAbsolutePath() + "'. The error sent is :'" +ex.getMessage() + "'." );
                    continue;
                }
            }
            try {
                // Data copy
                binaryCopy(NWGenericListener.EXPORTING, titleToExport, titleToExport.unwrapTitle(exportedFile), titleId);
            } catch (Exception ex) {
                // An error occured
                titlesNotExported++;
                copyError++;
                titleVectorsMonitor.remove(TitleVectorsMonitor.EXPORT_VECTOR, titleToExport);
                logger.log(Level.SEVERE,"Error while copying data while exporting title: '"+titleToExport.getSourceFile().getPath() + "'. The error sent is :'" +ex.getMessage() + "'." );
                continue;
            }

            // Count number of exported titles
            titlesExported ++;
            // Update database
            dataBase.updateStatus(titleToExport, Title.ON_DEVICE);
            // Title has been treated, remove it from the vector
            titleVectorsMonitor.remove(TitleVectorsMonitor.EXPORT_VECTOR, titleToExport);
        }

        // If transfer has been stopped, count the number of file not exported
        if(stopTransfer) {
            titlesNotExported += titleVectorsMonitor.size(TitleVectorsMonitor.EXPORT_VECTOR);
            transferStopped = titleVectorsMonitor.size(TitleVectorsMonitor.EXPORT_VECTOR);
        }

        // Exportation is over, even if errors occured, the list should be cleared.
        titleVectorsMonitor.clear(TitleVectorsMonitor.EXPORT_VECTOR);

        if(titlesNotExported > 0){
            // If some titles haven't been exported
            // Write error message
            String errorMessage = "<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Errors_during_exportation") + ":<br>";
            if(unknownFormat > 0){
                errorMessage += unknownFormat + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_export_unknown_format") + "<br>";
            }
            if(alreadyExist > 0){
                errorMessage += alreadyExist + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_export_already_exist") + "<br>";
            }
            if(fileCannotBeCreated > 0){
                errorMessage += fileCannotBeCreated + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_export_cannot_create") + "<br>";
            }
            if(copyError > 0){
                errorMessage += copyError + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_export_copy_error") + "<br>";
            }
            if(transferStopped > 0){
                errorMessage += transferStopped + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_export_transfer_stopped") + "<br>";
            }
            errorMessage += java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.consult_log") + ".";

            // Inform the GUI
            sendTransferStepFinished(NWGenericListener.EXPORTING, errorMessage); // Inform GUI
        }
        else {
            // Else, inform the GUI that all went right
            sendTransferStepFinished(NWGenericListener.EXPORTING, ""); // Inform GUI
        }
        logger.info("Exportation finished.");
    }

    /**
    *Scans OMGAUDIO folder and fill in the title's list with all titles found. This method DON'T modify config files.
    *
    *@param titlesToRemove The list of the title to remove from the device.
    */
    protected void applyDeletion() {
        int titlesDeleted = 0; // Number of titles deleted
        int titlesNotDeleted = 0; // Number of titles not deleted
        int incorrectSource = 0; // Number of titles not deleted because the source is not an OMA file
        int transferStopped = 0; // Number of titles not deleted because transfer has been stopped
        Title titleToRemove;

        if(titleVectorsMonitor.size(TitleVectorsMonitor.DELETE_VECTOR) > 0 && !stopTransfer){
            // Inform GUI
            sendTransferStepStarted(NWGenericListener.DELETING);
        }
        else{
            // Else, there is nothing to do
            return;
        }

        while(titleVectorsMonitor.size(TitleVectorsMonitor.DELETE_VECTOR) > 0 && !stopTransfer) { //For each title
            // Get the title
            titleToRemove = titleVectorsMonitor.firstElement(TitleVectorsMonitor.DELETE_VECTOR);

            // Inform GUI
            sendFileChanged(NWGenericListener.DELETING, titleToRemove.toString());
            sendFileProgressChanged(NWGenericListener.DELETING, 50, 0) ;

            // Update dataBase
            dataBase.removeTitle(titleToRemove);

            // Check the title to be an .oma
            if( !titleToRemove.getSourceFile().getPath().toLowerCase().endsWith(".oma") ) {
                logger.severe("This error occurs to prevent a file to be erased: "+titleToRemove.getSourceFile().getPath()+". If you see this message, please contact the developers in the forum 'https://sourceforge.net/forum/forum.php?forum_id=747001'.");
                titlesNotDeleted++;
                incorrectSource++;
                titleVectorsMonitor.remove(TitleVectorsMonitor.DELETE_VECTOR, titleToRemove);
                continue;
            }

            // Delete file form device
            logger.fine("Files removed :"+titleToRemove.getSourceFile().getPath() );
            titleToRemove.getSourceFile().delete();

            // Count number of deleted titles
            titlesDeleted ++;
            // Update database
            dataBase.delete(titleToRemove);
            // Title has been treated, remove it from the vector
            titleVectorsMonitor.remove(TitleVectorsMonitor.DELETE_VECTOR, titleToRemove);

            // Update progress bar in GUI
            sendFileProgressChanged(NWGenericListener.DELETING, 100, 0) ;
        }

        // If transfer has been stopped, count the number of file not deleted
        if(stopTransfer) {
            titlesNotDeleted += titleVectorsMonitor.size(TitleVectorsMonitor.DELETE_VECTOR);
            transferStopped = titleVectorsMonitor.size(TitleVectorsMonitor.DELETE_VECTOR);
        }

        // Deletion is over, even if errors occured, the list should be cleared.
        titleVectorsMonitor.clear(TitleVectorsMonitor.DELETE_VECTOR);

        if(titlesNotDeleted > 0){
            // If some titles haven't been deleted
            // Write error message
            String errorMessage = "<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Errors_during_deletion") + ":<br>";
            if(incorrectSource > 0){
                errorMessage += incorrectSource + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_delete_incorrect_source") + "<br>";
            }
            if(transferStopped > 0){
                errorMessage += transferStopped + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_delete_transfer_stopped") + "<br>";
            }
            errorMessage += java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.consult_log") + ".";

            // Inform the GUI
            sendTransferStepFinished(NWGenericListener.DELETING, errorMessage); // Inform GUI
        }
        else {
            // Else, inform the GUI that all went right
            sendTransferStepFinished(NWGenericListener.DELETING, ""); // Inform GUI
        }

        logger.info("Deleting files finished.");
        return;
    }

    /**
     * Add the list of files to the device. This method "turn" the titles to ".OMA" files. This method DON'T modify database files.
     */
    protected void applyImport() {
        int titleId; // titleID used to make a new file with a non-existing filename on the device
        String fileName; // name of the new file on the device
        int dirNumber; // number of directory on the device used to make the new title
        String dirName; // directory name on the device used to make the new title
        File omaFile, directory; // new file and its directory
        Title titleToImport; // title to be transfered
        boolean weShouldRun = true; // allow to stop the transfer
        int titlesImported = 0; // Number of titles correctly imported
        int titlesNotImported = 0; // Number of titles not imported
        int databaseError = 0; // Number of titles not transfered because of a database error
        int copyError = 0; // Number of titles not transfered because of a copy error
        int noSpaceLeft = 0; // Number of titles not transfered because of the device is full
        int unsupportedFormat = 0; // Number of titles not transfered because format is not supported
        int transferStopped = 0; // Number of titles not transfered because transfer has been stopped
        int noLsiDrmKeyFiles = 0; // Number of titles not transfered because LSI DRM key files are missing.


        // Check if there is anything to transfer
        if((titleVectorsMonitor.size(TitleVectorsMonitor.IMPORT_VECTOR) > 0) || (threadsStateMonitor.waitForEncodeThread())){
            // Inform GUI
            sendTransferStepStarted(NWGenericListener.IMPORTING);
        }
        else{
            // Else, there is nothing to do
            return;
        }

        while(weShouldRun && !stopTransfer) {
            if(titleVectorsMonitor.isEmpty(TitleVectorsMonitor.IMPORT_VECTOR)) {
                // There is no title in the vector, so either encodage is finished (encode thread is over) or we should wait
                // Wait is done in threadsStateMonitor
                weShouldRun = threadsStateMonitor.waitForEncodeThread();
                // Force another loop to re-test the state if of the transfer vector or to leave the loop if all the work is done
                logger.info("Transfer thread is waiting for encode thread");
                continue;
            }
            // There are titles to transfer, we take the first one, and remove it from the vector
            titleToImport = titleVectorsMonitor.firstElement(TitleVectorsMonitor.IMPORT_VECTOR);
            titleVectorsMonitor.remove(TitleVectorsMonitor.IMPORT_VECTOR, titleToImport);

            // Inform GUI
            sendFileChanged(NWGenericListener.IMPORTING, titleToImport.toString());

            // Check format
            if(titleToImport instanceof Oma){
                Oma oma = new Oma(titleToImport);
                if((oma.getVersion() == Oma.ATRAC_AL) || ((generation > 3) && (oma.getDrm() != Oma.NO_DRM))){
                    if(oma.getVersion() == Oma.ATRAC_AL){
                        // ATRAC Advanced Lossless codec is not supported
                        logger.warning("ATRAC Advanced Lossless codec is not supported.");
                    }
                    else{
                        // ATRAC files with DRM cannot be transfered to models with generation > 3
                        logger.warning("ATRAC files with DRM are not supported.");
                    }
                    titlesNotImported++;
                    unsupportedFormat++;
                    // Remove the path from database
                    dataBase.removePath(titleToImport);
                    continue;
                }

                if(generation <= 3){
                    if(oma.getDrm() != Oma.DRM_LSI){
                        logger.warning("ATRAC files without LSI DRM are not supported.");
                        titlesNotImported++;
                        unsupportedFormat++;
                        // Remove the path from database
                        dataBase.removePath(titleToImport);
                        continue;
                    }

                    // Check that the LSI DRM key files are present (0001001D.DAT, 00010021.DAT, SRCIDLST.DAT, SRCIDLST.BAK, 30GRCT folder)
                    boolean drmKeyFilesPresent = true;
                    List<String> drmKeyFilesList = new ArrayList();
                    drmKeyFilesList.add("0001001D.DAT");
                    drmKeyFilesList.add("00010021.DAT");
                    drmKeyFilesList.add("SRCIDLST.DAT");
                    drmKeyFilesList.add("SRCIDLST.BAK");

                    // First the files
                    for(int i=0; i < drmKeyFilesList.size(); i++){
                        String drmKeyFileName = drmKeyFilesList.get(i);
                        File drmKeyFile = new File(source.getPath() + File.separatorChar + drmKeyFileName);
                        if(!drmKeyFile.exists()){
                            // If file is missing, try to find it in the backup
                            drmKeyFile = new File(source.getPath() + File.separatorChar + "db_backup" + File.separatorChar + drmKeyFileName);
                            if(drmKeyFile.exists()){
                               // if file exist, move it to the root
                                drmKeyFile.renameTo(new File(source.getPath() + File.separatorChar + drmKeyFileName));
                            }
                            else{
                                // else, file is missing
                                drmKeyFilesPresent = false;
                            }
                        }
                    }
                    // Then the folder
                    File drmKeyFolder = new File(source.getPath() + File.separatorChar + "30GRCT");
                    if(!drmKeyFolder.exists()){
                        // If file is missing, try to find it in the backup
                        File drmKeyFolder_bu = new File(source.getPath() + File.separatorChar + "db_backup File.separatorChar 30GRCT");
                        if(drmKeyFolder_bu.exists()){
                            // if file exist, move its content to the root
                            drmKeyFolder.mkdir(); // create the folder
                            String[] drmKeyFiles = drmKeyFolder_bu.list();
                            for(int i=0; i < drmKeyFiles.length; i++){
                                File drmKeyFile_bu = new File(source.getPath() + File.separatorChar + "db_backup" + File.separatorChar + "30GRCT" + File.separatorChar+ drmKeyFiles[i]);
                                File drmKeyFile = new File(source.getPath() + File.separatorChar + "30GRCT" + File.separatorChar + drmKeyFiles[i]);
                                drmKeyFile_bu.renameTo(drmKeyFile);
                            }
                        }
                        else{
                            // else, file is missing
                            drmKeyFilesPresent = false;
                        }
                    }

                    if(!drmKeyFilesPresent){
                        // If at least one drm key file is missing, title won't be playable, don't transfer them
                        logger.warning("No LSI DRM key files found. OMA files cannot be played.");
                        titlesNotImported++;
                        noLsiDrmKeyFiles++;
                        // Remove the path from database
                        dataBase.removePath(titleToImport);
                        continue;
                    }
                }
            }

            // Create the name of the file on the device and the folder in which it will be stored
            titleId = dataBase.getTitleId(titleToImport); // Get a free title ID

            dirNumber = (titleId / 256); // Build the name of the directory
            dirName = Integer.toHexString(dirNumber);
            if( dirNumber < 0x10 ) { //If dirNumber is less than 10, a zero must be added to the name of the directory
                dirName = "0" + dirName;
            }
            dirName = "10F" + dirName;

            // create the directory
            directory = new File (source.getPath() + File.separatorChar + dirName);
            if( !directory.exists() ) {
                File directoryLower = new File (source.getPath() + File.separatorChar + dirName.toLowerCase()); // Test directory in lower case for linux

                if( !directoryLower.exists() ) {
                    directory.mkdir(); // If the directory don't exist both in lower and upper case, create it (default is uppercase)
                }
                else { // If the directory exist in lower, change the variable dirName to lower
                    dirName = dirName.toLowerCase();
                }
            }

            // Create the file
            fileName = Integer.toHexString(titleId).toUpperCase(); // Build the name of the file
            while( fileName.length() < 7 ) {
                fileName = "0" + fileName; //Add zeros to have 7 characters
            }
            String index = "1" + fileName; // Save the index
            fileName = index + ".OMA"; //Add prefix and suffix

            // New path built mustn't represent an existing file
            omaFile = new File(source.getPath() + File.separatorChar + dirName + File.separatorChar + fileName);
            if( omaFile.exists() ) {
                logger.warning("Meet an non-free file while adding oma files. Skip this file.");
                titlesNotImported++;
                databaseError++;
                // Remove the path from database
                dataBase.removePath(titleToImport);
                continue;
            }

            // Create the destination file
            File newOmaFile = new File(source.getPath() + File.separatorChar + dirName + File.separatorChar + fileName);
            Title newOma = new Oma(newOmaFile, titleToImport);

            // Copy the data
            try {
                binaryCopy(NWGenericListener.IMPORTING, titleToImport, newOma, titleId);
                logger.info("Add file, source:'" + titleToImport.getSourceFile().getPath() + "', destination:'"+source.getPath() + "/" + dirName + "/" + fileName+"'.");
            } catch (Exception ex) {
                logger.severe("Error while copying data: "+ex.getMessage());
                titlesNotImported++;
                if(ex.getMessage().toLowerCase().contains("no space left")){
                    noSpaceLeft++; // A copy error may be caused if device is full, if said so in the exception message, update the "noSpaceLeft" variable
                }
                else {
                    copyError++; // Else, the error is not specificly known
                }
                // Remove the path from database
                dataBase.removePath(titleToImport);
                continue;
            }

            // If the title was a temporary title, it should be deleted
            if(titleToImport.getStatus() == Title.TO_IMPORT_AND_DELETE || titleToImport.getStatus() == Title.TO_ENCODE_AND_DELETE) {
                // Check that the filename has been generated by JSymphonic, to not deleted a non temporary file
                if(titleToImport.getSourceFile().getName().contains("JStmpFile")){
                    logger.fine("Temporary file to delete:'" + titleToImport.getSourceFile().getPath());
                    titleToImport.getSourceFile().delete();
                }
                else{
                    logger.severe("A non temporary file has been avoided to be deleted because declared as a temporary file !!! This is not normal, please report the bug to the developpers!!" +titleToImport.getSourceFile().getPath() );
                }
            }

            // Count number of imported titles
            titlesImported++;
            // Update database
            dataBase.updateStatus(titleToImport, Title.ON_DEVICE);
        }

        // If transfer has been stopped, count the number of file not transfered
        if(stopTransfer) {
            titlesNotImported += titleVectorsMonitor.size(TitleVectorsMonitor.IMPORT_VECTOR);
            transferStopped = titleVectorsMonitor.size(TitleVectorsMonitor.IMPORT_VECTOR);
            // Delete files not imported from the database
            while(!titleVectorsMonitor.isEmpty(TitleVectorsMonitor.IMPORT_VECTOR)){
                titleToImport = titleVectorsMonitor.firstElement(TitleVectorsMonitor.IMPORT_VECTOR);
                dataBase.delete(titleToImport);
                titleVectorsMonitor.remove(TitleVectorsMonitor.IMPORT_VECTOR, titleToImport);
            }
        }

        // Task is over, even if errors occured, the list should be cleared.
        titleVectorsMonitor.clear(TitleVectorsMonitor.IMPORT_VECTOR);

        // Delete temporary files which could be left and not deleted because not transfered (because device is full or stop button pressed mostly)
        File temporaryFolder = new File(getTempPath()); // get the temp folder
        File[] temporaryFiles = temporaryFolder.listFiles();
        for(int i = 0; i < temporaryFiles.length; i++){
            // Delete all found files
            if(temporaryFiles[i].getName().contains("JStmpFile")){
                logger.fine("Temporary file to be deleted:'" + temporaryFiles[i].getPath());
                temporaryFiles[i].delete();
            }
        }

        if(titlesNotImported > 0){
            // If some titles haven't been encoded
            // Write error message
            String errorMessage = "<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Errors_during_transfer") + ":<br>";
            if(databaseError > 0){
                errorMessage += databaseError + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_transfer_db_error") + "<br>";
            }
            if(copyError > 0){
                errorMessage += copyError + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_transfer_copy_error") + "<br>";
            }
            if(noSpaceLeft > 0){
                errorMessage += noSpaceLeft + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_transfer_no_space_left") + "<br>";
            }
            if(unsupportedFormat > 0){
                errorMessage += unsupportedFormat + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_transfer_unsupported_format") + "<br>";
            }
            if(transferStopped > 0){
                errorMessage += transferStopped + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_transfer_transfer_stopped") + "<br>";
            }
            if(noLsiDrmKeyFiles > 0){
                errorMessage += noLsiDrmKeyFiles + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_transfer_no_drmkeyfiles_found") + "<br>";
            }
            errorMessage += java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.consult_log") + ".";

            // Inform the GUI
            sendTransferStepFinished(NWGenericListener.IMPORTING, errorMessage); // Inform GUI
        }
        else {
            // Else, inform the GUI that all went right
            sendTransferStepFinished(NWGenericListener.IMPORTING, ""); // Inform GUI
        }
        logger.info("Import files finished.");
        return;
    }

    /**
    * Scans OMGAUDIO folder and fill in the title's list with all titles found. (this method is only valid for Generation 3 to 7)
    */
    protected void loadTitlesFromDevice(){
        int titleId = 0;
        int numberOfTitles = 0, pathListValid;
        Set totalTitlesList = new HashSet();

        // Build the path list in the database
        pathListValid = dataBase.buildPathList();

        //Create vector of directories in OMGAUDIO
        java.io.File[] dirList = source.listFiles();

        //First, search all the directories contening oma files
        for (int i = 0; i < dirList.length; i++){
            // Skip hidden file
            if(dirList[i].getName().startsWith(".")){
                continue;
            }

            //Is it's a directory starting with "10F", it contains titles
            if (dirList[i].isDirectory() && (dirList[i].getName().toLowerCase().startsWith("10f") || dirList[i].getName().toLowerCase().startsWith("10F"))){
                java.io.File[] titlesList = dirList[i].listFiles(); //Create vector of files in the directory

                numberOfTitles += titlesList.length;
                totalTitlesList.add(titlesList);
            }
        }

        // Calculate the increment to use in loading progress bar in GUI
        double progressBarValue = 0;
        sendLoadingInitialization(numberOfTitles);

        // Add the titles to the database
        Iterator it = totalTitlesList.iterator();
        while (it.hasNext()) {
            java.io.File[] titlesList = (File[])it.next();

            //For each file
            for(int j = 0; j < titlesList.length; j++){
                //Create a title from the file

                Title t = new Oma(titlesList[j]);

                // If file is empty, do not use it and delete it from the source device (an error certainly occured in a previous execution)
                if(t.getFileSize() < 5){ // 5 octets, because an empty file could contains some id octets
                    logger.info("Meet an empty file, delete it: " + titlesList[j].getPath() + "");
                    titlesList[j].delete();
                    progressBarValue++;
                    continue;
                }

                //Determine the title ID to use in the database
                try{// An error may occur if the name of the title is not as expected.
                    titleId = Integer.parseInt(titlesList[j].getName().toLowerCase().replaceAll("10*(.*)\\.oma","$1"), 16);
                }
                catch(Exception e){
                    logger.warning("ERROR: A file in the 10FXX folder is not named as expected, file is skipped:"+titlesList[j].getPath());
                    progressBarValue++;
                    continue;
                }

                //Add the title and the titleId in the database if not null
                if ( t != null && titleId != 0 ){
                    t.setStatus(Title.ON_DEVICE); // Change its status
                    dataBase.addTitle(t, titleId);
                    if(pathListValid < 0){
                        dataBase.addPath(titleId, "unknown");
                    }

                    // Update loading progress bar
                    progressBarValue++;
                    sendLoadingProgresChange(progressBarValue);
                }
            }
        }
    }

	/*
	 * Trys to locate the DvID.dat file and load the key.
     *
     * @return 0 is a key was loaded, -1 otherwise
	 */
	protected int loadKey(String playerPath){
        int returnValue = -1;

		try
		{
			testKeyFileName(playerPath + "/DvID.dat");
			testKeyFileName(playerPath + "/DvID.DAT");
			testKeyFileName(playerPath + "/OMGAUDIO/DvID.dat");
			testKeyFileName(playerPath + "/OMGAUDIO/DvID.DAT");
			testKeyFileName(playerPath + "/omgaudio/DvID.dat");
			testKeyFileName(playerPath + "/omgaudio/DvID.DAT");
			testKeyFileName(playerPath + "/MP3FM/DvID.dat");
			testKeyFileName(playerPath + "/MP3FM/DvID.DAT");
			testKeyFileName(playerPath + "/mp3fm/DvID.dat");
			testKeyFileName(playerPath + "/mp3fm/DvID.DAT");
			testKeyFileName(playerPath + "/JSYMPHONIC/DvID.DAT");
			testKeyFileName(playerPath + "/JSYMPHONIC/DvID.dat");
			testKeyFileName(playerPath + "/jsymphonic/DvID.DAT");
			testKeyFileName(playerPath + "/jsymphonic/DvID.dat");
		}
		catch (KeyFileFoundException e)
		{
			//A key file has bee found, now read the key
			try
			{
                byte[] bytesKey = new byte[4]; // Tab of bytes to read the key from the file

				// Open file in stream mode
				// Is this really necessary (toURI.toURL)?
				InputStream dvid_datStream = new File(e.getMessage()).toURI().toURL().openStream();

				// Skip the first useless bytes
				dvid_datStream.skip(10);
				// Read the key
				// Save for compatibility with unmodified methods
				dvid_datStream.read(bytesKey);

				//Extract the DvID key into a uint:
				this.uintKey = (0x0FFL & ((int)bytesKey[3]));
				this.uintKey += 0x0100L * (0x0FFL & ((int)bytesKey[2]));
				this.uintKey += 0x010000L * (0x0FFL & ((int)bytesKey[1]));
				this.uintKey += 0x01000000L * (0x0FFL & ((int)bytesKey[0]));

                // Key is found, returned value should be 0
                returnValue = 0;

				// Debug info
                logger.info("A key has been read: "+ uintKey);

				// Close the stream
				dvid_datStream.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
        return returnValue;
	}


	/*
	 * Exception to use when a key file is found
	 */
	private class KeyFileFoundException extends Exception
	{
		KeyFileFoundException(String s)
		{
			super(s);
		}
	}

	/*
	 * Check for the file and throw an exception if it exists
	 */
	public void testKeyFileName(String keyFilePath)
		throws KeyFileFoundException
	{
		File testFile = new File(keyFilePath);
		if(testFile.exists())
			throw new KeyFileFoundException(keyFilePath);
	}
}
