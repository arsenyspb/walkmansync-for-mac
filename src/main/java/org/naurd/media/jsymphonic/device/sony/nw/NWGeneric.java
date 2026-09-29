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
 * NWGeneric.java
 *
 * Created on 6 juin 2007, 21:17
 *
 */

package org.naurd.media.jsymphonic.device.sony.nw;

import java.util.*;
import java.io.*;
import java.util.logging.Logger;
import org.naurd.media.jsymphonic.title.Mp3;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.title.UnknowFileTypeException;
import org.naurd.media.jsymphonic.toolBox.FFMpegToolBox;
import org.naurd.media.jsymphonic.toolBox.JSymphonicIoFileFilter;
import org.naurd.media.jsymphonic.toolBox.Java6ToolBox;
import org.naurd.media.jsymphonic.toolBox.JSymphonicMap;

/**
 * An instance of the NWGeneric class describes a Sony devices, including a reference to the real device, its name, its description, the list of titles, its dataBase, its icon and a listener.
 * This class is abstract. Implementation depends on the generation of the walkman.
 *
 * Methods on this class are separated into two categories:
 *  - schedule of titles changes (the user asks for a title import or export or deletion, changes are stored)
 *  - apply changes to the device (the user presses the "apply" button titles are actually imported, exported or deleted)
 * 
 * @author nicolas_cardoso
 * @author Daniel Žalar - added events to ensure GUI independancy and logging support
 * @author Pedro Velasco - bugfix in export feature for protected players
 * @version 06.06.2007
 */
public abstract class NWGeneric implements org.naurd.media.jsymphonic.device.Device {
/* FIELDS */
    // Device information
    protected File source = null;         // the database folder of the walkman (ESYS for generation 2, OMGAUDIO for others)
    protected String name = "Walkman";    // name of the device
    protected String description = "";    // description of the device
    protected javax.swing.ImageIcon icon; // icon of the device
    protected DataBase dataBase;       // database of the device
    protected long usableSpace = 0;       // left space on the device in octet
    protected long totalSpace = 0;        // used space on the device in octet
    protected int generation = 0;         // generation of the device
    protected long uintKey;               //The key as an unsigned integer
    protected boolean gotkey = false;     //Do we have a key for the player?
    
    // Configuration information
    private int TranscodeBitrate = 128; // default is 128kbps
    private boolean AlwaysTranscode = false;
    protected String devicePath = null;
    private String tempPath = null;
    private String localPath = null;
    private String exportPath = null;
    
    // Titles on the device
    // titles to add are handeled in the corresponding monitor (titleVectorsMonitor) protected Vector<Title> titlesToAdd = new Vector<Title>();    // list of title to be added in the next transfer operation
    protected long titlesToImportSize = 0; // space used by the titles scheduled for import in octet
    protected long titlesToDeleteSize = 0; // space used by the titles scheduled for deletion in octet

    // Threads
    Thread t1_transfer; // First thread export titles, delete files, import title (if t2 et t3 have finished) and update database
    Thread t2_decode; // Second thread decoded titles and die
    Thread t3_encode; // Third thread encoded titles, it is alive at least as long as t2 lives, then it dies
    
    // Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.system.sony.nw.NWGeneric");
    protected ArrayList listeners;
    protected Boolean stopTransfer = false; // used to know if the transfer should be stopped

    // Generations
    public static final int GENERATION_0 = 0;
    public static final int GENERATION_1 = 1;
    public static final int GENERATION_2 = 2;
    public static final int GENERATION_3 = 3;
    public static final int GENERATION_4 = 4;
    public static final int GENERATION_5 = 5;
    public static final int GENERATION_6 = 6;
    public static final int GENERATION_7 = 7;
    public static final int GENERATION_VAIO = 8;
    protected static Map generationMap = new HashMap(); // used to store the different generation names
    
    // Shared variables for the encode, decode, transfer thread are stored in monitors
    protected ThreadsStateMonitor threadsStateMonitor; // monitor to handle the status of the threads
    protected TitleVectorsMonitor titleVectorsMonitor; // monitor to hanlde the titles passing from a thread to another (only used when apply process has started, schedule import/export/deletion/encoding/decoding are handled in the database)
    
/* INNER MONITOR CLASSES TO MANAGE SHARED MEMORY */
    /**
     * Describes the state of the decode and encode threads. As they are waiting for each other, their status are stored in a monitor to avoid dead-locks.
     *
     *@author nicolas_cardoso
     */
    protected class ThreadsStateMonitor {
        private boolean decodeThreadIsRunning = true;
        private boolean encodeThreadIsRunning = true;


        /**
         *Initialized monitor (in case of several action done on the device (several apply))
         *
         *@author nicolas_cardoso
         */
        public synchronized void initialize() {
            // Put the stat of the thread in the default value
            decodeThreadIsRunning = true;
            encodeThreadIsRunning = true;
    	}


        /**
         * Wait that decode thread has decode a new track or has finished
         *
         *@return true if decode thread is still runing (and still has work to do) or false if decode thread is over (and the thread calling this method shouldn't wait this thread anymore)
         *
         *@author nicolas_cardoso
         */
        public synchronized boolean waitForDecodeThread() {
            if(decodeThreadIsRunning){
                // If decode thread is still running, we should wait
                try {
                    wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                return true;
            }
            else{
                return false;
            }
    	}

    	/**
         * Wait that encode thread has encode a new track or has finished
         *
         *@return true if encode thread is still runing (and still has work to do) or false if encode thread is over (and the thread calling this method shouldn't wait this thread anymore)
         *
         *@author nicolas_cardoso
         */
        public synchronized boolean waitForEncodeThread() {
            if(encodeThreadIsRunning){
                // If decode thread is still running, we should wait
                try {
                    wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                // encode thread is still running, return true
                return true;
            }
            else {
                return false;
            }
    	}

        /**
         * When decode thread has decode a title or when it has finished, it should notify encode thread through this method
         *
         *@param hasFinished true if decodeThread has finished or false if it has just decode one title
         *
         *@author nicolas_cardoso
         */
    	public synchronized void notifyEncodeThread(boolean hasFinished) {
            if(hasFinished){
                decodeThreadIsRunning = false;
            }

            // Notify all the treads that this thread's state has changed
            notifyAll();
        }

        /**
         * When encode thread has encode a title or when it has finished, it should notify transfer thread through this method
         *
         *@param hasFinished true if encodeThread has finished or false if it has just encode one title
         *
         *@author nicolas_cardoso
         */
        public synchronized void notifyTransferThread(boolean hasFinished) {
            if(hasFinished){
                encodeThreadIsRunning = false;
            }

            // Notify all the treads that this thread's state has changed
            notifyAll();
        }
    }

    /**
     * Describes the lists of titles passing through the different threads. It's shared memory, they are stored in a monitor to avoid trouble.
     *
     *@author nicolas_cardoso
     */
    protected class TitleVectorsMonitor {
        // List of titles, separated into the ones to export, to delete, to decode, to encode and to transfer
        private Vector<Title> titlesToExport = new Vector<Title>();
        private Vector<Title> titlesToDelete = new Vector<Title>();
        private Vector<Title> titlesToDecode = new Vector<Title>();
        private Vector<Title> titlesToEncode = new Vector<Title>();
        private Vector<Title> titlesToImport = new Vector<Title>();

        // Constant
        protected static final int ALL = 0;
        protected static final int EXPORT_VECTOR = 1;
        protected static final int DELETE_VECTOR = 2;
        protected static final int DECODE_VECTOR = 3;
        protected static final int ENCODE_VECTOR = 4;
        protected static final int IMPORT_VECTOR = 5;

        /**
         * Initialize the titles in each vector from the titles in the database.
         * Titles to export are added to the EXPORT vector and so on...
         *
         * @param titles All the titles from the database.
         */
        public synchronized void initialize(JSymphonicMap titles) {
            // Clear all vector
            titlesToExport.clear();
            titlesToDelete.clear();
            titlesToDecode.clear();
            titlesToEncode.clear();
            titlesToImport.clear();

            Set<Title> titlesInSet = titles.keySet(); // Get all the titles to a set
            Iterator it = titlesInSet.iterator(); // Create an iterator on the set

            while(it.hasNext()){ // For each title in the database
                Title titleTmp = (Title) it.next(); // Read the title status
                switch(titleTmp.getStatus()){ // Add the title into the correct vector
                    case Title.TO_EXPORT:
                        add(EXPORT_VECTOR, titleTmp);
                        break;
                    case Title.TO_DELETE:
                        add(DELETE_VECTOR, titleTmp);
                        break;
                    case Title.TO_DECODE:
                        add(DECODE_VECTOR, titleTmp);
                        break;
                    case Title.TO_ENCODE:
                        add(ENCODE_VECTOR, titleTmp);
                        break;
                    case Title.TO_IMPORT:
                        add(IMPORT_VECTOR, titleTmp);
                        break;
                    default:
                        // Do nothing if the title is already on the device
                }
            }
        }

        /**
         * Add a title to a vector.
         *
         *@param vector The vector to add the title in.
         *@param title The title to add.
         *
         *@author nicolas_cardoso
         */
        public synchronized void add(int vector, Title title) {
            switch(vector){
                case EXPORT_VECTOR:
                    titlesToExport.add(title);
                    break;
                case DELETE_VECTOR:
                    titlesToDelete.add(title);
                    break;
                case DECODE_VECTOR:
                    titlesToDecode.add(title);
                    break;
                case ENCODE_VECTOR:
                    titlesToEncode.add(title);
                    break;
                case IMPORT_VECTOR:
                    titlesToImport.add(title);
                    break;
            }
        }

        /**
         * Remove a title from a vector.
         *
         *@param vector The vector to remove the title from.
         *@param title The title to remove.
         *
         *@author nicolas_cardoso
         */
        public synchronized void remove(int vector, Title title) {
            switch(vector){
                case EXPORT_VECTOR:
                    titlesToExport.remove(title);
                    break;
                case DELETE_VECTOR:
                    titlesToDelete.remove(title);
                    break;
                case DECODE_VECTOR:
                    titlesToDecode.remove(title);
                    break;
                case ENCODE_VECTOR:
                    titlesToEncode.remove(title);
                    break;
                case IMPORT_VECTOR:
                    titlesToImport.remove(title);
                    break;
            }
        }

        /**
         * Clear a vector.
         *
         *@param vector The vector to clear.
         *
         *@author nicolas_cardoso
         */
        public synchronized void clear(int vector) {
            switch(vector){
                case EXPORT_VECTOR:
                    titlesToExport.clear();
                    break;
                case DELETE_VECTOR:
                    titlesToDelete.clear();
                    break;
                case DECODE_VECTOR:
                    titlesToDecode.clear();
                    break;
                case ENCODE_VECTOR:
                    titlesToEncode.clear();
                    break;
                case IMPORT_VECTOR:
                    titlesToImport.clear();
                    break;
                case ALL:
                    titlesToExport.clear();
                    titlesToDelete.clear();
                    titlesToDecode.clear();
                    titlesToEncode.clear();
                    titlesToImport.clear();
                    break;
            }
        }

        /**
         * Get from element from a vector.
         *
         *@param vector The vector to get the first element from.
         *@return The first title in the vector.
         *
         *@author nicolas_cardoso
         */
        public synchronized Title firstElement(int vector) {
            switch(vector){
                case EXPORT_VECTOR:
                    return titlesToExport.firstElement();
                case DELETE_VECTOR:
                    return titlesToDelete.firstElement();
                case DECODE_VECTOR:
                    return titlesToDecode.firstElement();
                case ENCODE_VECTOR:
                    return titlesToEncode.firstElement();
                case IMPORT_VECTOR:
                    return titlesToImport.firstElement();
            }
            return null;
        }

        /**
         * Tell is a vector is empty.
         *
         *@param vector The vector to get the info from.
         *@return True if vector is empty, false otherwise.
         *
         *@author nicolas_cardoso
         */
        public synchronized boolean isEmpty(int vector) {
            switch(vector){
                case EXPORT_VECTOR:
                    return titlesToExport.isEmpty();
                case DELETE_VECTOR:
                    return titlesToDelete.isEmpty();
                case DECODE_VECTOR:
                    return titlesToDecode.isEmpty();
                case ENCODE_VECTOR:
                    return titlesToEncode.isEmpty();
                case IMPORT_VECTOR:
                    return titlesToImport.isEmpty();
            }
            return true;
        }

        /**
         * Get the number of element in a vector.
         *
         *@param vector The vector to get the info from.
         *@return The number of elements in the vector.
         *
         *@author nicolas_cardoso
         */
        public synchronized int size(int vector) {
            switch(vector){
                case EXPORT_VECTOR:
                    return titlesToExport.size();
                case DELETE_VECTOR:
                    return titlesToDelete.size();
                case DECODE_VECTOR:
                    return titlesToDecode.size();
                case ENCODE_VECTOR:
                    return titlesToEncode.size();
                case IMPORT_VECTOR:
                    return titlesToImport.size();
            }
            return 0;
        }
    }

    
/* CONSTRUCTORS */
    /**
     * Creates an instance of NWGeneric class. Since this class is abstract, the instance will be an instance from NWOmgaudio or NWEsys classes.
     *
     * @param sourceName The name of the device.
     * @param sourceDesc The description of the device.
     * @param sourceIcon The icon of the device.
     * @param listener The listener on the device.
     * @param exportPath The path of the export folder.
     */
    public NWGeneric(String sourceName, String sourceDesc, javax.swing.ImageIcon sourceIcon, NWGenericListener listener, String exportPath){
        // Save the information
        name = sourceName; // the name of the device
        description = sourceDesc; // the description of the device
        icon = sourceIcon; // the icon of the device
        this.exportPath = exportPath;
        // Set up the listeners
        listeners = new ArrayList();
        if(listener != null){
            addGenericNWListener(listener);
        }
        
        // Instance monitor inner classes to handle shared variables
        threadsStateMonitor = new ThreadsStateMonitor();
        titleVectorsMonitor = new TitleVectorsMonitor();

        // Clean the vector for the first transfer
        titleVectorsMonitor.clear(TitleVectorsMonitor.ALL);
    }
     /*
     *This constructor is  needed to be able to instanciate the class
     *from a call Class.forName()
     */
    public NWGeneric() {
        // Create the listener list
        listeners = new ArrayList();
    }

/* ABSTRACT METHODS */
    protected abstract void applyDeletion();
    protected abstract void applyExport();
    protected abstract void applyImport();
    protected abstract void loadTitlesFromDevice();

/* SCHEDULE METHODS */
    /**
     *Adds title to the list of title to add to the device. Changes are only applied when "writeTitles()" is called.
     *
     *@param t The title to add.
     *@param transcodeAllFiles should be true is the configuration is set to "always transcode", false otherwise
     *@param transcodeBitrate indicates the bitrate set in the configuration to be used to transcode.
     *
     *@return 0 if all is OK, -1 if the device is full
     */
    public int scheduleImport(Title title){
        double titleSize;

        // Determine size of the title when it will be on the device
        if(Title.isCompatible(title, AlwaysTranscode, TranscodeBitrate, generation, false)) {
           // if file is simply copied, we just have to get its size
            titleSize = title.size();
        }
        else {
            // if file must be trancoded, its size should be computed from its lenght and bitrate
            titleSize = (title.getLength()/1000.0)*(getTranscodeBitrate()*(1000.0/8.0)) + 2000.0; // title length is given in milliseconds, divide it by 1000 to have it in seconds; bitrate is in kpps, multiply it by (1000/8) to have it in octet per second, and the +2000 is for the lenght of the tag
            title.setFileSizeAfterTranscodage((long) titleSize); // Store the new size
            logger.fine("Computed title size (in Mo):"+titleSize/(1024*1024));
        }

        // Before adding the title, space left should be checked
        if(titlesToImportSize + titleSize - titlesToDeleteSize >= usableSpace) {
            // If the device is full, return -1
            logger.fine("Device is full.");
            return -1;
        }
        else {
            // Else, determine if the file has to be DECODED, ENCODED or just IMPORTED and put it in the correct vector
            if(Title.isCompatible(title, AlwaysTranscode, TranscodeBitrate, generation, true)){
                // File is compatible, it can be transfered without been transcoded
                title.setStatus(Title.TO_IMPORT); // Change status file
            }
            else if(FFMpegToolBox.isEncodable(title)) {
                // File is compatible with FFMPEG and only need to be encoded
                title.setStatus(Title.TO_ENCODE); // Change status file
            }
            else{
                // File need to be decoded before been encoded
                title.setStatus(Title.TO_DECODE); // Change status file
            }

            // Add the file to the database
            if(dataBase.scheduleImport(title) < 0){
                // If title already exists, it is not added twice to the database
                logger.fine("File already in the database, it is ignored: "+title);
            }
            else {
                // Else, update space
                titlesToImportSize += titleSize;
                logger.fine("File scheduled for import: "+title);
            }
            return 0;
        }
    }

    /**
     * Define a recursive function to scan folders, this method instance a new title according to its extension (MP3, OGG,...) and check if the title should be converted.
     * @author Nicolas Cardoso
     * @author Daniel Žalar - added logging functionality and ported to this class
     * @param file The file to add to the list of title to import to the Sony device, or (if it's a folder) the folder to scan to look for titles to add.
     * @param init A boolean value to tell if the GUI needs to be initialized or not  (true if so, false otherwise)
     *
     * @return 0 if no errors occurs, -1 if a file's extension is not recognized and -2 the device is full
     */
    public int scheduleImport(File[] files, boolean init) {
        int ret = 0; // Store return result
        int progressBarValue = 0; // Used to inform the GUI about the number of files already scanned

        if(init){
            // GUI needs to be initialized. Count the number of files to scan, in current list of folder and also subfolders
            int numberOfFiles = 0;

            // Create an array to store the folders
            ArrayList <File> filesArray = new ArrayList <File>();
            int j = 0; // j is the index of the next empty space in the array

            // First, scan all files from the given list
            for(int i = 0; i < files.length; i++) {
                if (files[i].isFile()){
                    // Files are just counted
                    numberOfFiles++;
                }
                else{
                    // Folders are added to the array
                    filesArray.add(j,files[i]);
                    j++;
                }
            }

            // Then, scan every folders from the array
            for(int i = 0; i < filesArray.size(); i++) {
                File folder = filesArray.get(i);
                if (folder.isFile()){
                    // If it is a file, count it
                    numberOfFiles++;
                }
                else{
                    // If it is a folder, add all the sub files/folders to the array, they will be scanned later in the loop
                    File[] fileList = folder.listFiles(new JSymphonicIoFileFilter());
                    for(int k = 0; k < fileList.length; k++) {
                        filesArray.add(j,fileList[i]);
                        j++;
                    }
                }
            }

            // Once done, the total number of files to add is known, tell the GUI
            sendLoadingInitialization(numberOfFiles);
        }

        for(int i = 0; i < files.length; i++) {
            if (files[i].isFile()){
                // We have a file, we should instance a new Title, check if it should be transcode and add it to the list of titles to add.
                Title newTitle; // the title currently scanned

                try {
                    newTitle = Title.getTitleFromFile(files[i]);
                } catch (UnknowFileTypeException ex) {
                    // If the file can't be instance to a title, warn the logger
                    logger.warning("File with unknown format scanned: " + ex.getMessage());
                    // store the return result
                    ret = -1;

                    // Inform the GUI
                    progressBarValue++;
                    sendLoadingProgresChange(progressBarValue);

                    // and skip the file
                    continue;
                }

                // Add the title and check that return is OK
                if(scheduleImport(newTitle) < 0 ) {
                    // If something went wrong, it's because player is full, show corresponding message
                    sendShowPlayerFullMsg(); // This method also send Loading Progress Change

                    return -2;
                }

                // Inform the GUI
                progressBarValue++;
                sendLoadingProgresChange(progressBarValue);
            }
            else {
                File[] fileList = files[i].listFiles(new JSymphonicIoFileFilter());
                if(scheduleImport(fileList, false) == -2){return -2;} // scan the folder, if player is full, stop the method.
            }
        }
        return ret;
    }

    /**
     * Add a title to the list of title to be exported from the device to the computer. Changes are only applied when "writeTitles()" is called.
     *
     *@param t The title to export.
     */
    public void scheduleExport(Title title){
        // Schedule export in the database
        dataBase.scheduleExport(title);
    }

    /**
    *Deletes track from the device. Changes are only applied when "writeTitles()" is called.
    *
    *@param t The title to remove.
    */
    public void scheduleDeletion(Title title){
        // Schedule deletion in the database
        int scheduled = dataBase.scheduleDeletion(title);
        if( scheduled > 0){
            // If title was scheduled for import, update space used
            titlesToImportSize -= title.getFileSizeAfterTranscodage();
        }
        else if(scheduled == 0){
            // If title is on the device and hasn't already been schedule for deletion, update space used
            titlesToDeleteSize += title.getFileSizeAfterTranscodage();
        }
    }

    /**
    *Ignores tracks changes in the GUI and refreshes the GUI with the content of the device.
    */
    public void reload(){
        // Purge and clear the database
        dataBase.clear();

        // Update space
        Java6ToolBox.FileSpaceInfo spaceInfo = Java6ToolBox.getFileSpaceInfo(source.getParentFile());
        usableSpace = spaceInfo.getUsableSpace();
        totalSpace = spaceInfo.getTotalSpace();
        titlesToImportSize = 0;
        titlesToDeleteSize = 0;

        loadTitlesFromDevice(); // Fill in the title's list
    }

/* APPLY METHODS */
    /**
     *Applies tracklist changes from the GUI to the device (export, delete, transfer tracks and update database).
     *This method should be used to start a new thread.
     */
    public void applyChanges(){
        // Initialize variable
        stopTransfer = false;

        // Initialize components
        titleVectorsMonitor.initialize(dataBase.getTitles()); // titles monitor
        threadsStateMonitor.initialize(); // threads monitor
        sendTransferInitialization(titleVectorsMonitor.size(TitleVectorsMonitor.EXPORT_VECTOR), titleVectorsMonitor.size(TitleVectorsMonitor.DELETE_VECTOR), titleVectorsMonitor.size(TitleVectorsMonitor.DECODE_VECTOR), titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR), titleVectorsMonitor.size(TitleVectorsMonitor.IMPORT_VECTOR), dataBase.getNumberOfFiles()); // GUI

        // Create and launch a thread to execute "applyChangesInThread" method
        t1_transfer = new Thread(){
            @Override
            public void run(){
                try{
                    applyChangesInThread();
                } catch(Exception e){
                    logger.warning("Error in applyChanges method: "+e.getStackTrace()+" - "+e.getMessage());
                }
            }
        };
        t1_transfer.setPriority(Thread.NORM_PRIORITY);
        t1_transfer.start(); // Start the thread

        // Create and launch a thread to decode titles (which is running "decodeTitlesInThread" method)
        t2_decode = new Thread(){
            @Override
            public void run(){
                try{
                    decodeTitlesInThread();
                } catch(Exception e){}
            }
        };
        t2_decode.setPriority(Thread.NORM_PRIORITY);
        logger.info("decodeThread has started");
        t2_decode.start();

        // Create and launch a thread to encode titles (which is running "encodeTitlesInThread" method)
        t3_encode = new Thread(){
            @Override
            public void run(){
                try{
                    encodeTitlesInThread();
                } catch(Exception e){}
            }
        };
        t3_encode.setPriority(Thread.NORM_PRIORITY);
        t3_encode.start();
        logger.info("encodeThread has started");
    }

    /**
     *Applies tracklist changes from the GUI to the device (export, delete, transfer tracks and update database).
     *This method really apply the changes as it is run from a new thread created by "writeTitles" method.
     */
    private void applyChangesInThread() {
        try{
            //Export files fisrt
            logger.info("Exportation started.");
            applyExport();

            //Delete files
            logger.info("Deleting files started.");
            applyDeletion();

            //Import files
            logger.info("Import files started.");
            applyImport();

            // Once all the title has been tranfered, end the transfer by writing the database and updating the GUI
            logger.info("Writing database started.");
            dataBase.write(this);
            logger.info("Writing database finished.");

            //The transfert is completed
            reload(); //reload everything...
            sendTransferTermination();
            logger.info("transferThread has finished");

        }catch(Exception e){
            logger.warning(e.getMessage().toString());
        }
    }


/* METHODS */
    /**
     * Obtains the space left on the device on a human readable text form.
     *
     *@return A string gining the space left on the device.
     *
     *@author nicolas_cardoso
     */
    public String getSpaceLeftInText() {
        long tmpTotalSpace = totalSpace;
        long usedSpace = totalSpace - usableSpace + titlesToImportSize - titlesToDeleteSize; // Calculate used space
        int n=0;
        String text;
        
        
        while((usedSpace/(1024) > 1024)) { // Convert used space in a suitable unit
            usedSpace = usedSpace/1024;
            n++;
        }
        
        if(n==0) {
            text = usedSpace/1024 + " KB" ; // Memorised the used space
        }
        else {
            text = usedSpace/1024 + "."+ (usedSpace%1024)/10 + " " ; // Memorised the used space
            if(n == 1) {text += "MB";} // Put the right unit
            if(n == 2) {text += "GB";}
            if(n == 3) {text += "TB";}
        }
        text += " / " ;
        
        n=0;
        while((tmpTotalSpace/(1024) > 1024)) { // Convert used space in a suitable unit
            tmpTotalSpace = tmpTotalSpace/1024;
            n++;
        }
        
        if(n==0) {
            text += tmpTotalSpace/1024 + " KB" ; // Memorised the used space
        }
        else {
            text += tmpTotalSpace/1024 + "."+ (tmpTotalSpace%1024)/10 + " " ; // Memorised the used space
            if(n == 1) {text += "MB";} // Put the right unit
            if(n == 2) {text += "GB";}
            if(n == 3) {text += "TB";}
        }
        
        return text;
    }    

    /**
     *Obtain the space left in the device in per cent.
     *
     *@return the space left in per cent.
     *
     *@author nicolas_cardoso
     */
    public int getSpaceLeftInRatio() {
        long usedSpace = totalSpace - usableSpace + titlesToImportSize - titlesToDeleteSize; // Calculate used space
        return (int)((usedSpace)*100.0/totalSpace);
    }
    
    
    /**
     *Obtain the generation of the device.
     *
     *@return the generation.
     *
     *@author nicolas_cardoso
     */
    public int getGeneration() {
        return generation;
    }
    
    
    /**
     *Obtains the tracklist into the device (more exactly, the titles currently registered in the database).
     *
     *@return The tracklist.
     *
     *@author nicolas_cardoso
     */
    public Title[] getTitles(){
        //Get the titles in a JSymphonic Map
        JSymphonicMap titles = dataBase.getTitles();
        
        //Get the number of titles
        int count = titles.size();
        //Create the vector to return
        Title[] titlesToReturn = new org.naurd.media.jsymphonic.title.Title[count];
        
        // Get all the title IDs from titles (= all the titles)
        Set tmpTitles = titles.keySet();
        
        //Convert the set to an array
        Iterator it = tmpTitles.iterator();
        int i = 0;
        while( it.hasNext() ) {
            titlesToReturn[i] = (Title)it.next();
            i++;
        }

        return titlesToReturn;    
    }
    
    /**
     *Obtains the tracklist into the device (more exactly, the titles currently registered in the database).
     *
     *@return The tracklist.
     *
     *@author nicolas_cardoso
     */
    public JSymphonicMap getTitlesInMap(){
        // Get the titles from the actual database
        JSymphonicMap titles = dataBase.getTitles();
        
        // Adding new titles - titles to decode
        Iterator itDecodedTitles = titleVectorsMonitor.titlesToDecode.iterator();
        int indexValue;
        if( titles.size() > 0) {
            indexValue = titles.maxValue(); // To keep JSymphonicMap structure, we must add an index value associated with each titles, as these value won't be used anymore, we just add values from the highest to be sur to not have twice the same
        }
        else {
            indexValue = 1;
        }
        while(itDecodedTitles.hasNext()) {
            titles.put(itDecodedTitles.next(),++indexValue);
        }

        // Adding new titles - titles to encode
        Iterator itEncodedTitles = titleVectorsMonitor.titlesToEncode.iterator();
        if( titles.size() > 0) {
            indexValue = titles.maxValue(); // To keep JSymphonicMap structure, we must add an index value associated with each titles, as these value won't be used anymore, we just add values from the highest to be sur to not have twice the same
        }
        else {
            indexValue = 1;
        }
        while(itEncodedTitles.hasNext()) {
            titles.put(itEncodedTitles.next(),++indexValue);
        }
        
        // Adding new titles - titles to transfer
        Iterator itTransferedTitles = titleVectorsMonitor.titlesToImport.iterator();
        if( titles.size() > 0) {
            indexValue = titles.maxValue(); // To keep JSymphonicMap structure, we must add an index value associated with each titles, as these value won't be used anymore, we just add values from the highest to be sur to not have twice the same
        }
        else {
            indexValue = 1;
        }
        while(itTransferedTitles.hasNext()) {
            titles.put(itTransferedTitles.next(),++indexValue);
        }

        return titles;
    }
            
    
    /**
     * This method should be call in a new thread, dealing just with the decoding of titles in WAV. This method read the title to be decoded in the variable titlesToDecode. Once decoded, this method put the titles in the variable titlesToEncode.
     *
     *@author nicolas_cardoso
     */
    private void decodeTitlesInThread() {
        Title titleToDecode; // the title which is currently decoded
        int format; // format of the current title
        int temporaryCounter = 0; // a counter for the temporary decoded files (to create unique filename)
        int titlesDecoded = 0; // Number of titles correctly decoded
        int titlesNotDecoded = 0; // Number of titles not decoded
        int unsupportedFormat = 0; // Number of titles not decoded because format is not supported
        int transferStopped = 0; // Number of titles not decoded because transfer has been stopped
        File temporaryFolder = new File(getTempPath()); // get the temp folder

        if(titleVectorsMonitor.size(TitleVectorsMonitor.DECODE_VECTOR) > 0 && !stopTransfer){
            // Inform GUI
            sendTransferStepStarted(NWGenericListener.DECODING);
        }
        else{
            // Else, there is nothing to do
            logger.info("decodeThread has finished");
            threadsStateMonitor.notifyEncodeThread(true); // true to indicate that thread has finished
            return;
        }

        // Check that ffmpeg is available
        if(!FFMpegToolBox.isFFMpegPresent()){
            // if ffmpeg is not present
            // Display en error
            logger.warning("ERROR: ffmpeg can't be found, titles that need to be transcoded can't be transfered, please check you installation of ffmpeg.");

            // Empty the list of title to decode since they won't be encoded, there is no need to decode them !!
            titleVectorsMonitor.clear(TitleVectorsMonitor.DECODE_VECTOR);
        }

        // Check that temp folder is valid
        if(!temporaryFolder.exists() || temporaryFolder.isFile()){
            logger.severe("Temporay folder cannot be found. Check the preferences.");
            threadsStateMonitor.notifyTransferThread(true); // true because this thread is over

            // Empty the list of title to encode
            titleVectorsMonitor.clear(TitleVectorsMonitor.ENCODE_VECTOR);

            return;
        }

        // If program crashed (or was exited) while an encodage task, unusued temporary file should be deleted
        File[] temporaryFiles = temporaryFolder.listFiles();
        for(int i = 0; i < temporaryFiles.length; i++){
            // Delete all found files
            if(temporaryFiles[i].getName().contains("JStmpFileDec")){
                logger.fine("Temporary file left from a previous execution to delete:'" + temporaryFiles[i].getPath());
                temporaryFiles[i].delete();
            }
        }

        // Check the files supported for decoding by ffmpeg and give them to the encode thread
        while(!titleVectorsMonitor.isEmpty(TitleVectorsMonitor.DECODE_VECTOR) && !stopTransfer) { // While there are title to be decoded
            // get the first element form the list of the title to decode
            titleToDecode = titleVectorsMonitor.firstElement(TitleVectorsMonitor.DECODE_VECTOR);

            // Inform GUI
            sendFileChanged(NWGenericListener.DECODING, titleToDecode.toString());
            
            // Determine the format of the title        
            format = titleToDecode.getFormat();
            switch(format) {
/*
                case Title.FLAC:
                // I used to use a FLAC decoder to decode flac because I experienced problems with several FLAC files and FFMPEG. But I then discovered that flac files not recognized by FFMPEG were files with an ID3 tag, so I implemented a function to skip this Tag and now FLAC can be transcoded only with FFMPEG.
                // However, I leave this code because if one day an extra codec is added in the decode thread, the code could be inspirated by this one
                    // Create a flac decoder object
                    Decoder flacDecoder = new Decoder();

                    // Create temporary output file
                    if(!temporaryFolder.exists()) {
                        // If the temporary folders don't exist, create them
                        temporaryFolder.mkdirs();
                    }
                    File newWAVFile = new File(temporaryFolder + "/JStmpFileDec" + temporaryCounter + ".wav");
                    temporaryCounter++;

                    // Decode the file
                    try {
                        flacDecoder.decode(titleToDecode.getSourceFile().getPath(), newWAVFile.getPath(), this);
                    } catch (IOException ex) {
                        logger.severe("ERROR in flac decoder while decoding file:"+titleToDecode.getSourceFile().getPath());
                        ex.printStackTrace();

                        // Saved changes
                        titlesNotDecoded++;
                    }

                    // Create a new Title object from the decoded title
                    Title titleToEncode;
                    try {
                        titleToEncode = Title.getTitleFromFile(newWAVFile);
                    } catch (UnknowFileTypeException ex) {
                        logger.severe("Wave file is not valid: "+ex.getMessage());
                        // Saved changes
                        titlesNotDecoded++;
                        // Inform GUI
                        sendFileProgressChanged(NWGenericListener.DECODING,100, 0) ;
                        // remove it from the decode list
                        titleVectorsMonitor.remove(TitleVectorsMonitor.DECODE_VECTOR, titleToDecode);
                        // Skip the file
                        continue;
                    }
                    // change its status
                    titleToEncode.setStatus(Title.TO_ENCODE_AND_DELETE);
 // should use "replaceTitle" in the database and "updateStatus" to change its status
                    // copy tag info from the title to decode to the title to encode since decoding loose tag info
                    titleToEncode.copyTagInfo(titleToDecode);
                    // give the title to encode thread
                    titleVectorsMonitor.add(TitleVectorsMonitor.ENCODE_VECTOR, titleToEncode);

                    // Saved changes
                    titlesDecoded++;

                    break;
*/
                default:
                    logger.severe("File format non supported for this file:"+titleToDecode.getSourceFile().getPath());
                    // Saved changes
                    titlesNotDecoded++;
                    unsupportedFormat++;
                    // Inform GUI
                    sendFileProgressChanged(NWGenericListener.DECODING,100, 0) ;
            }
            
            // remove it from the decode list                
            titleVectorsMonitor.remove(TitleVectorsMonitor.DECODE_VECTOR, titleToDecode);
        }
        
        
        // At the end of the thread, we update the state of the thread in the monitor and we wake up other threads
        logger.info("decodeThread has finished");
        threadsStateMonitor.notifyEncodeThread(true); // true to indicate that thread has finished
        
        // If transfer has been stopped, count the number of file not decoded
        if(stopTransfer) {
            // Store the number of files not transfered
            titlesNotDecoded += titleVectorsMonitor.size(TitleVectorsMonitor.DECODE_VECTOR);
            transferStopped = titleVectorsMonitor.size(TitleVectorsMonitor.DECODE_VECTOR);
            // Delete files not decoded from the database
            while(!titleVectorsMonitor.isEmpty(TitleVectorsMonitor.DECODE_VECTOR)){
                titleToDecode = titleVectorsMonitor.firstElement(TitleVectorsMonitor.DECODE_VECTOR);
                dataBase.delete(titleToDecode);
                titleVectorsMonitor.remove(TitleVectorsMonitor.DECODE_VECTOR, titleToDecode);
            }
        }

        // Task is over, even if errors occured, the list should be cleared.
        titleVectorsMonitor.clear(TitleVectorsMonitor.DECODE_VECTOR);

        if(titlesNotDecoded > 0){
            // If some titles haven't been decoded
            // Write error message
            String errorMessage = "<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Errors_during_decoding") + ":<br>";
            if(unsupportedFormat > 0){
                errorMessage += unsupportedFormat + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_decode_unsupported_format") + "<br>";
            }
            if(transferStopped > 0){
                errorMessage += transferStopped + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_decode_transfer_stopped") + "<br>";
            }
            errorMessage += java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.consult_log") + ".";

            // Inform the GUI
            sendTransferStepFinished(NWGenericListener.DECODING, errorMessage); // Inform GUI
        }
        else {
            // Else, inform the GUI that all went right
            sendTransferStepFinished(NWGenericListener.DECODING, ""); // Inform GUI
        }
    }

    /**
     * This method should be call in a new thread, dealing just with the encoding of titles in MP3. This method read the title to be encoded in the variable titlesToEncode. Once encoded, this method put the titles in the variable titlesToImport.
     *
     *@author nicolas_cardoso
     */
    private void encodeTitlesInThread() {
        Title titleToEncode;
        boolean weShouldRun = true;
        int titlesEncoded = 0; // Number of titles correctly encoded
        int titlesNotEncoded = 0; // Number of titles not encoded
        int noFfmpeg = 0; // Number of titles not encoded because FFMPEG is missing
        int destUnwriteable = 0; // Number of titles not encoded because destination is unwriteable
        int destFull = 0; // Number of titles not encoded because destination is full
        int unsupportedFormat = 0; // Number of titles not encoded because format is not supported
        int brokenPipe = 0; // Number of titles not encoded because FFMPEG suddenly closed (because of a broken pipe)
        int unknownError = 0; // Number of titles not encoded because of an unknown error
        int transferStopped = 0; // Number of titles not encoded because transfer has been stopped
        int temporaryCounter = 0; // a counter to determine different name for all temporary files
        // An instance of ffmpeg is needed
        FFMpegToolBox ffMpegToolBox = new FFMpegToolBox();
        // Get the temp folder
        File temporaryFolder = new File(getTempPath()); // default is the device directoy

        // Check if there is anything to encode
        if((titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR) > 0) || (threadsStateMonitor.waitForDecodeThread())) {
            // Inform GUI
            sendTransferStepStarted(NWGenericListener.ENCODING);
        }
        else{
            // Else, there is nothing to do
            logger.info("encodeThread has finished");
            threadsStateMonitor.notifyTransferThread(true); // true because this thread is over
            return;
        }

        // Check that ffmpeg is available
        if(!FFMpegToolBox.isFFMpegPresent()){
            // if ffmpeg is not present
            // Display en error
            logger.warning("ERROR: ffmpeg can't be found, titles that need to be transcoded can't be transfered, please check you installation of ffmpeg.");

            // Empty the list of title to encode
            titleVectorsMonitor.clear(TitleVectorsMonitor.ENCODE_VECTOR);
        }

        // Check that temp folder is valid
        if(!temporaryFolder.exists() || temporaryFolder.isFile()){
            logger.severe("Temporay folder cannot be found. Check the preferences.");
            threadsStateMonitor.notifyTransferThread(true); // true because this thread is over

            // Empty the list of title to encode
            titleVectorsMonitor.clear(TitleVectorsMonitor.ENCODE_VECTOR);

            return;
        }

        // If program crashed (or was exited) while an encodage task, unusued temporary file should be deleted
        File[] temporaryFiles = temporaryFolder.listFiles();
        for(int i = 0; i < temporaryFiles.length; i++){
            // Delete all found files
            if(temporaryFiles[i].getName().contains("JStmpFileEnc")){
                logger.fine("Temporary file left from a previous execution to delete:'" + temporaryFiles[i].getPath());
                temporaryFiles[i].delete();
            }
        }

        while(weShouldRun && !stopTransfer) {
            if(titleVectorsMonitor.isEmpty(TitleVectorsMonitor.ENCODE_VECTOR)) {
                // There is no title in the vector, so either decodage is finished (decode thread is over) or we should wait
                // Wait is done in threadsStateMonitor
                weShouldRun = threadsStateMonitor.waitForDecodeThread();
                // Force another loop to re-test the state if of the encode vector or to leave the loop if all the work is done
                continue;
            }
            else {
                // There are titles to encode, we take the first one
                titleToEncode = titleVectorsMonitor.firstElement(TitleVectorsMonitor.ENCODE_VECTOR);
                
                // Inform GUI
                sendFileChanged(NWGenericListener.ENCODING, titleToEncode.toString());

                // Create temporary output file
                if(!temporaryFolder.exists()) {
                    // If the temporary folders don't exist, create them
                    temporaryFolder.mkdirs();
                }
                File newMP3File = new File(temporaryFolder + "/JStmpFileEnc" + temporaryCounter + ".mp3");
                temporaryCounter++; // Increment the counter

                // Create a new Title instance for the conversion
                Mp3 titleEncoded = new Mp3(newMP3File.getAbsolutePath());
                
                // We transcode it
                if(ffMpegToolBox.convertToMp3(titleToEncode, titleEncoded, getTranscodeBitrate(), this) < 0){
                    // Save the number of file not encoded
                    titlesNotEncoded++;

                    // Try to know what was the error
                    switch(ffMpegToolBox.getLastError()) {
                        case FFMpegToolBox.FFMPEG_NOT_FOUND:
                            // No other title can be encoded
                            // Store the number of title not encoded
                            titlesNotEncoded += titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
                            noFfmpeg = titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
                            logger.warning("FFMPEG was not found. Titles cannot be encoded.");

                            // Clear the list
                            titleVectorsMonitor.clear(TitleVectorsMonitor.ENCODE_VECTOR);
                            break;
                        case FFMpegToolBox.DESTINATION_UNWRITEABLE:
                            // No other title can be encoded
                            // Store the number of title not encoded
                            titlesNotEncoded += titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
                            destUnwriteable = titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
                            logger.warning("Cannot write into temporary folder. Files cannot be encoded.");

                            // Clear the list
                            titleVectorsMonitor.clear(TitleVectorsMonitor.ENCODE_VECTOR);
                            break;
                        case FFMpegToolBox.DESTINATION_FULL:
                            // No other title can be encoded
                            // Store the number of title not encoded
                            titlesNotEncoded += titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
                            destFull= titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
                            logger.warning("Temporary folder is full! No more files can be encoded.");

                            // Clear the list
                            titleVectorsMonitor.clear(TitleVectorsMonitor.ENCODE_VECTOR);
                            break;
                        case FFMpegToolBox.UNSUPPORTED_FORMAT:
                            // Store the number of title not encoded
                            titlesNotEncoded++;
                            unsupportedFormat++;
                            logger.warning("FFMPEG cannot convert the following file: "+ titleToEncode.getSourceFile().getPath() +", because its format is not supported by FFMPEG.");
                            break;
                        case FFMpegToolBox.BROKEN_PIPE:
                            // Store the number of title not encoded
                            titlesNotEncoded++;
                            logger.warning("FFMPEG cannot convert the following file: "+ titleToEncode.getSourceFile().getPath() +", it suddenly closes.");
                            brokenPipe++;
                            break;
                        default:
                            // Store the number of title not encoded
                            titlesNotEncoded++;
                            logger.warning("Unknown error while converting the following file with FFMPEG: "+ titleToEncode.getSourceFile().getPath() +".");
                            unknownError++;
                    }
                }
                else {
                    // Read the MP3 file info on the new created MP3 file
                    titleEncoded.readMP3Info();

                    // Save the number of file encoded
                    titlesEncoded++;

                    // Update the database
                    dataBase.replaceTitle(titleToEncode, titleEncoded);
                    // Tell to the transfer thread to erase the title once it's transfered (this encoded file is temporary)
                    dataBase.updateStatus(titleEncoded, Title.TO_IMPORT_AND_DELETE);

                    // Add the encoded title to the list of title to transfer
                    titleVectorsMonitor.add(TitleVectorsMonitor.IMPORT_VECTOR, titleEncoded);
                }
                    

                // If the title has been decoded, the temporary wave file sould be erased
                if(titleToEncode.getStatus() == Title.TO_ENCODE_AND_DELETE) {
                    // Check that the filename has been generated by JSymphonic, to not deleted a non temporary file
                    if(titleToEncode.getSourceFile().getName().contains("JStmpFile")){
                        logger.fine("Temporary WAV file to delete:'" + titleToEncode.getSourceFile().getPath());
                        titleToEncode.getSourceFile().delete();
                    }
                    else{
                        logger.severe("A non temporary file has been avoided to be deleted because declared as a WAV temporary file !!! This is not normal, please report the bug to the developpers!!" +titleToEncode.getSourceFile().getPath() );
                    }
                }
                
                // Remove the original title from the encode vector
                titleVectorsMonitor.remove(TitleVectorsMonitor.ENCODE_VECTOR, titleToEncode);
            }
        }
        
        // If transfer has been stopped, count the number of file not encoded
        if(stopTransfer) {
            titlesNotEncoded += titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
            transferStopped = titleVectorsMonitor.size(TitleVectorsMonitor.ENCODE_VECTOR);
            // Delete files not encoded from the database
            while(!titleVectorsMonitor.isEmpty(TitleVectorsMonitor.ENCODE_VECTOR)){
                titleToEncode = titleVectorsMonitor.firstElement(TitleVectorsMonitor.ENCODE_VECTOR);
                dataBase.delete(titleToEncode);
                titleVectorsMonitor.remove(TitleVectorsMonitor.ENCODE_VECTOR, titleToEncode);
            }
        }

        // Task is over, even if errors occured, the list should be cleared.
        titleVectorsMonitor.clear(TitleVectorsMonitor.ENCODE_VECTOR);

        if(titlesNotEncoded > 0){
            // If some titles haven't been encoded
            // Write error message
            String errorMessage = "<html>" + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.Errors_during_encoding") + ":<br>";
            if(noFfmpeg > 0){
                errorMessage += java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_encode_no_ffmpeg") + "<br>";
            }
            if(destUnwriteable > 0){
                errorMessage += destUnwriteable + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_encode_dest_unwriteable") + "<br>";
            }
            if(destFull > 0){
                errorMessage += destFull + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_encode_dest_full") + "<br>";
            }
            if(unsupportedFormat > 0){
                errorMessage += unsupportedFormat + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_encode_unsupported_format") + "<br>";
            }
            if(brokenPipe > 0){
                errorMessage += brokenPipe + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_encode_broken_pipe") + "<br>";
            }
            if(unknownError > 0){
                errorMessage += unknownError + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_encode_unknown_error") + "<br>";
            }
            if(transferStopped > 0){
                errorMessage += transferStopped + " " + java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.err_encode_transfer_stopped") + "<br>";
            }
            errorMessage += java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.consult_log") + ".</html>";

            // Inform the GUI
            sendTransferStepFinished(NWGenericListener.ENCODING, errorMessage); // Inform GUI
        }
        else {
            // Else, inform the GUI that all went right
            sendTransferStepFinished(NWGenericListener.ENCODING, ""); // Inform GUI
        }
        
        // At the end of the thread, we update the state of the thread in the monitor and we wake up other threads
        logger.info("encodeThread has finished");
        threadsStateMonitor.notifyTransferThread(true); // true because this thread is over
    }
    
    /**
    *Obtains the name of the source.
    *
    *@return The name of the source.
    */    
    public String getName(){
        return name;
    }
    
    /**
    *Sets the name of the source.
    *
    *@param n The name of the source.
    */    
    public void setName(String n){
        name=n;
    }
    
    /**
    *Obtains the description of the source.
    *
    *@return The description of the source.
    */
    public String getDescription(){
        return description;
    }
    
    /**
    *Sets the description of the source.
    *
    *@param d The description of the source.
    */    
    public void setDescription(String d){
        description = d;
    }
    
    /**
    *Obtains the source (in URL).
    *
    *@return The source.
    */    
    public java.net.URL getSourceURL(){
        try{
            return source.toURI().toURL();
        } catch(Exception e){
            return null;
        }
    }
    
    /**
    *Obtains the icon related to the device.
    *
    *@return The icon.
    */
    public javax.swing.ImageIcon getIcon(){
        return icon;
    }
    
    /**
     *Get the capacity of the device.
     *
     *@return the capacity.
     */
    public long getTotalSpace(){
        Java6ToolBox.FileSpaceInfo spaceInfo = Java6ToolBox.getFileSpaceInfo(source.getParentFile());
        return spaceInfo.getTotalSpace();
    }
    
    /**
     *Get the available space on the device.
     *
     *@return the available space.
     */
    public long getUsableSpace(){
        Java6ToolBox.FileSpaceInfo spaceInfo = Java6ToolBox.getFileSpaceInfo(source.getParentFile());
        return spaceInfo.getUsableSpace();
    }
    
    /**
     *Set the value of the config parameter "always transcode".
     *
     *@param AlwaysTranscode true if the "always transcode" option is set, false otherwise.
     */
    public void setAlwaysTranscode(boolean AlwaysTranscode) {
        this.AlwaysTranscode = AlwaysTranscode;
    }

    /**
     *This methods allows classes to register for your events
     */
    public void addGenericNWListener(NWGenericListener listener) {
        listeners.add(listener);
    }

    /**
     *This methods allows classes to unregister for you events
     */
    public void removeGenericNWListener(NWGenericListener listener) {
      listeners.remove(listener);
    }

    protected void sendTransferInitialization(int numberOfExportFiles, int numberOfDeleteFiles, int numberOfDecodeFiles, int numberOfEncodeFiles, int numberOfTransferFiles, int numberOfDbFiles){
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.transferInitialization(numberOfExportFiles, numberOfDeleteFiles, numberOfDecodeFiles, numberOfEncodeFiles, numberOfTransferFiles, numberOfDbFiles);
            }
        }
    }
    
    protected void sendTransferTermination(){
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.transferTermination();
            }
        }
    }
    
    protected void sendTransferStepStarted(int step){
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.transferStepStarted(step);
            }
        }
    }
    
    protected void sendTransferStepFinished(int step, String errorMessage){
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.transferStepFinished(step, errorMessage);
            }
        }
    }        
        
    protected void sendFileChanged(int step, String name){
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.fileChanged(step, name);
            }
        }
    }
        
    public void sendFileProgressChanged(int step, double value, double speed) {
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.fileProgressChanged(step, value, speed);
            }
        }
    }

    protected void sendLoadingInitialization(int i) {
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.loadingInitialization(i);
            }
        }
    }

    protected void sendLoadingProgresChange(double i) {
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.loadingProgresChanged(i);
            }
        }
    }

    protected void sendShowPlayerFullMsg(){
        for ( int j = 0; j < listeners.size(); j++ ) {
            NWGenericListener ev = (NWGenericListener) listeners.get(j);
            if ( ev != null ) {
                ev.showPlayerFullMsg();
            }
        }
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

    /**
     * Fill in the generation map with the correct values.
     */
    private static void initializeGenerationMap() {
// Generation 0, 1 and 2 are not supported
//        generationMap.put(GENERATION_0, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation0"));
//        generationMap.put(GENERATION_1, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation1"));
//        generationMap.put(GENERATION_2, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation2"));
        generationMap.put(GENERATION_3, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation3"));
        generationMap.put(GENERATION_4, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation4"));
        generationMap.put(GENERATION_5, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation5"));
        generationMap.put(GENERATION_6, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation6"));
        generationMap.put(GENERATION_7, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generation7"));
        generationMap.put(GENERATION_VAIO, java.util.ResourceBundle.getBundle("localization/jsymphonicprofilesdialog").getString("JSymphonicProfilesDialog.generationVaio"));
    }

    /**
     * Obtain a map with all the generations, their ID (NWGeneric.GENERATION_X) are associated to their names in the default languages.
     *
     * @return The map with the generation names (keys are the ID "NWGeneric.GENERATION_X") and values are the names (Strings)
     */
    public static HashMap getGenerationMap(){
        if(generationMap.isEmpty()){
            initializeGenerationMap();
        }

        return (HashMap) generationMap;
    }

     /**
     * Get the key for for item that is stored in the generation map as value.
      *
     * @param item The name of the generation we want the index.
     * @return The number of the generation wanted (NW_GENERATION _X).
     */
    public static int getKeyfromGenerationCombo(String item){
        // If the map is empty, fill in it
        if(generationMap.isEmpty()){
            initializeGenerationMap();
        }

        // Find generation key
       Set keys = generationMap.keySet();
       Iterator iter = keys.iterator();

       while (iter.hasNext()){
            Object key = iter.next();
            Object val = generationMap.get(key);
            if(val.equals(item)){
                return (Integer)key;
            }
       }
       return 0;
    }

    /**
     * This method copy audio file from a given source to a destination. The tag from the source can be skip (i.e. not copied to the destination). A given tag can be written in the destination.
     *
     * @param step The step the copy occurs in to inform GUI.
     * @param source The source file to copy from.
     * @param destination The destination to copy to.
     * @param titleId The ID of the file currently copied. This is only used when data is encrypted, i.e. for protected players, generation 3 and before.
     * @throws java.lang.Exception
     */
	protected void binaryCopy(int step, Title source, Title destination, int titleId) throws Exception {
        long sourceOffset; // The offset to be skipped in the source file.
        short[] ucharOmaKey = {0,0,0,0}; // Used to store the OmaKey for encryption

		// Create file streaml to copy data
        FileInputStream in = new FileInputStream(source.getSourceFile());
        FileOutputStream out = new FileOutputStream(destination.getSourceFile());

        // Skip the amount of bytes necessary to get to the music part of the file
        sourceOffset = source.getNbOfBytesBeforeMusic();
        in.skip(sourceOffset);

        // Before beginning the copy, write a tag to the destination
        destination.writeTagInfoToFile(out, gotkey);

        // If title should be encrypt, the omaKey should be generated
        if(gotkey) {
            // Protected players encrypt the files. Compute the decryption key corresponding to the current file index
            long omaKey; // Key for the current OMA file
            ucharOmaKey = new short[4]; // Same key in a char

            // Compute the key from Sony's secret recipe, uses the Key of the player, the ID of the OMA file and some numbers from nowhere...
            omaKey = (0x2465 + titleId * 0x5296E435L) ^ uintKey;

            // Save the omaKey into the char
            ucharOmaKey[3] = (byte)(omaKey & 0x0FFL);
            ucharOmaKey[2] = (byte)((omaKey >> 8) & 0x0FFL);
            ucharOmaKey[1] = (byte)((omaKey >> 16) & 0x0FFL);
            ucharOmaKey[0] = (byte)((omaKey >> 24) & 0x0FFL);
        }


        // Actual copy of audio data from source to destination
        byte [] buffer = new byte[4096]; // The buffer used to copy data
        long countIn = 0; // Count the number of bytes read from in when filling the buffer
        long totalIn = 0; // Count the number of bytes read from in from the beginning, initialized to the offset already skipped
        int currentIn = 0; // Count the number of bytes read from last speed computed
        long startTime = 0; // The time the last speed computation was started (initialized to zero to force the speed computation at the first iteration of the copy)
        float speed = 0; // Used to tell the speed to the GUI
        long totalSize = source.size(); // Compute the total size of the file
        int bytesCounter = 0; // Only used for encryption/decryption, count byte per byte to know how to encrypt/decrypt

        while (countIn!=-1){ // While input file has bytes
            countIn = in.read(buffer); // Read a amount of bytes
            currentIn+=countIn; // Count the amount of bytes read since the last speed computation

            if (System.currentTimeMillis() - startTime>10){ // If 0.1 second passed
                speed = currentIn / ((System.currentTimeMillis() - startTime)/100f) / 1024f; // Compute the speed in ko/s
                currentIn = 0; // Initialize the number of bytes read for the next speed computation
                startTime=System.currentTimeMillis(); // Initialize the time for next speed computation
            }

            if (countIn > 0){ // If bytes have been read
                totalIn+=countIn; // Count the number of bytes read since the beginning

                if(gotkey) {
                    // If the player is protected, data should be encrypted/decrypted:
                    for (int i = 0 ; i < countIn; i++) {
                        buffer[i] ^= ucharOmaKey[bytesCounter%4];
                        bytesCounter++;
                    }
                }

                // Write data to the destination
                out.write(buffer,0,(int)countIn);

                // Inform the GUI if the step is valid and the read size is not null
                if((totalSize!=0 && step >= 0) && (step > 0)){
                    sendFileProgressChanged(step, (double)(((totalIn+sourceOffset)*100)/totalSize), (double)speed); // Inform GUI
                }
            }
        }

        // Close every streams
        out.close();
        in.close();
	}

    /**
     * Set the "stopTransfer" to true to stop exportation/deletion/decoding/encoding/importation and try to build a correct database.
     */
    public void stopApplyChanges() {
        stopTransfer = true;
    }

    /**
     * Stops all transfer threads. This method uses the deprecated "stop" method form the Thread class. When using this method, the user is warned that its action may cause damage to the device or the program.
     */
    public void killAllTransferThreads() {
        if(t1_transfer.isAlive())
            t1_transfer.stop();
        if(t2_decode.isAlive())
            t2_decode.stop();
        if(t3_encode.isAlive())
            t3_encode.stop();
    }


   public int getTranscodeBitrate() {
        return TranscodeBitrate;
    }

    public void setTranscodeBitrate(int TranscodeBitrate) {
        this.TranscodeBitrate = TranscodeBitrate;
    }

    public String getDevicePath() {
        return devicePath;
    }

    public void setDevicePath(String devicePath) {
        this.devicePath = devicePath;
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(String devicePath) {
        this.devicePath = devicePath;
        initSourcePath();
    }
    
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    /**
    * Initialize the OMG path
    * @return true if omg path is valid
    */
    public boolean initSourcePath(){
        String databaseFolderName; // The name to be used as database folder name

        getLogger().info("Initializing database path");
        if(getDevicePath() != null){
            // The database dir is ESYS for generation 2 and OMGAUDIO for others
            if(generation == 2){
                databaseFolderName = "ESYS";
            }
            else{
                databaseFolderName = "OMGAUDIO";
            }

            // Build database folder name
            source = new File(devicePath + File.separatorChar + databaseFolderName);

            // Try to see if database folder exist
            if (!source.exists()) {
                // If not, try in lowercase
                source = new File(devicePath + File.separatorChar +  databaseFolderName.toLowerCase().replace("ı", "i"));
                if (!source.exists()) {
                    logger.warning("The device path " + source.getAbsolutePath() + " does not exist!");
                    return false;
                }
            }
            try {
                getLogger().info("Selected database path is " + source.getCanonicalPath());
            } catch (IOException ex) {
                getLogger().info(ex.getMessage());
            }
            return true;
        }
        else{
            getLogger().warning("The device path has not been set!");
            return false;
        }
    }
}

//TODO: (PEDRO)
//-FIX THE LOADKEY STUFF (TO SOMETHING MORE CLEAN THAN JUST TRYING UPP/LOWERCASE COMBINATIONS)
