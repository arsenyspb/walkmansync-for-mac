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
 * Title.java
 *
 * Created on October 2, 2006, 10:21 PM
 *
 */

package org.naurd.media.jsymphonic.title;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.naurd.media.jsymphonic.toolBox.FFMpegToolBox;

/**
 *Describes an audio file without regarding the format of the file.
 *
 * @author Pat
 * @author nicolas_cardoso
 */
public class Title {
/* FIELDS */
    // Title tag
    protected String titleName = "unknown title (JStitle)"; // Name of the title
    protected String artistName = "unknown artist (JStitle)"; // Name of the artist
    protected String albumName = "unknown album (JStitle)"; // Name of the album
    protected String genre = "unknown genre (JStitle)"; // Name of the genre
    protected int titleNumber = 0; // Title number of the title
    protected int year = 0000; // Year of the title
    
    // Title information
    protected java.net.URL sourceURL = null; // Source URL of the file (for Podcast)
    protected File sourceFile = null; // The file (for local file)
    protected long fileSize = 0; // Size of the file in octet
    protected long fileSizeAfterTranscodage = 0; // Size of the file in octet after transcodage (if any)
    protected int format = Title.AUDIO;
    protected int bitRate = 128; // Bitrate of the title
    protected double frequency = 44.1; // Frequency of the title, given in kHz
    protected boolean vbr = false; // Indicates if the title uses variable or fix bitrate, true = vbr, false = cbr
    protected int nbChannels = 2; // Number of channel
    protected int length = 210000; // Length of the title in milliseconds
    protected int nbOfBytesBeforeMusic = 0; // Number of bytes to skip to find music (used to skip data to wrap file to oma)
    protected int status = Title.ON_DEVICE;    // Title status defined by the following constant
    
    // Static
    protected static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.title.Title");
    protected static boolean tryReadTagInfo = true;
    protected static String tryReadTagInfoPattern = "";
    
/* CONSTANT */
    // File format constant
    public static final int AUDIO = 0; // Undefined format
    public static final int OMA = 1; // Format with a number between 1 and 9 can be play on all walkman
    public static final int MP3 = 2;
    public static final int AAC = 10; // Format with a number between 10 and 19 can be play on some walkmans and must be transcoded on other walkmans
    public static final int WMA = 11;
    public static final int OGG = 20; // Format with a number greated than 20 must be transcoded on all walkmans
    public static final int MPC = 21;
    public static final int FLAC = 22;
    public static final int WAV = 23;
    public static final int APE = 24;

    // Status the title
    public static final int ON_DEVICE = 0;
    public static final int TO_IMPORT = 1;
    public static final int TO_DELETE = 2;
    public static final int TO_EXPORT = 3;
    public static final int TO_DECODE = 4;
    public static final int TO_ENCODE = 5;
    public static final int TO_ENCODE_AND_DELETE = 6;
    public static final int TO_IMPORT_AND_DELETE = 7;

    
/* CONSTRUCTORS */
    /**
     *Creates a new instance of Title from a given file.
     *
     *@param file the file to create the title from
     */
    public Title(java.io.File file) {
        sourceFile = file;
        fileSize = file.length();
        try{
            readTagInfo();
        }
        catch(Exception ex){
            logger.warning("Error while reading info from files/folders structure. Please check your pattern's configuration.");
        }
    }
    
    /**
     *Creates a new instance of Title from a given URL.
     *
     *@param url the URL to create the title from
     */
    public Title(java.net.URL url) {
        sourceURL = url;
    }
    
    /**
     *Creates an empty instance of Title.
     *
     *This method could lead to not handle exception and should not be used.
     */
    public Title(){
    }
    
    
/* STATIC METHODS */
    /**
     * Tells if the tag (ID3, vorbis,...) should be read from the audio files, or if info should be read from the path name of the file
     *
     * @param newTryReadTagInfo New value: true if the tag should be used, false if info should be read from the path.
     */
    public static void setTryReadTagInfo(boolean newTryReadTagInfo){
        Title.tryReadTagInfo = newTryReadTagInfo;
    }

    /**
     * Tells the pattern to use when tag info are not used (or present)
     *
     * @param newTryReadTagInfoPattern The new value (see the readTagInfo() for more information).
     */
    public static void setTryReadTagInfoPattern(String newTryReadTagInfoPattern){
        Title.tryReadTagInfoPattern = newTryReadTagInfoPattern;
    }

    /**
     * Allows to instanciate a Title using the correct extended class (class Mp3 for MP3 files,...)
     *
     * @param f the file to create the object from.
     *
     * @return the object from the class corresponding to the title type. If no suitable class has been found, a Title object is returned.
     * @throws org.naurd.media.jsymphonic.title.UnknowFileTypeException
     */
    public static Title getTitleFromFile(java.io.File f) throws UnknowFileTypeException {
        // Get the file name
        String filename = f.getName().toLowerCase();
        // Extract the extension
        String[] tokens = filename.split("\\.");
        String extension = tokens[tokens.length-1];

        // If the extension of the file is known, create a object of the corresponding class.
        if (Mp3.getFileExtentionsList().contains(extension)){
            return new Mp3(f);
        } else if (Aac.getFileExtentionsList().contains(extension)){
            return new Aac(f);
        } else if (Wma.getFileExtentionsList().contains(extension)){
            return new Wma(f);
        } else if (Oma.getFileExtentionsList().contains(extension)){
            return new Oma(f);
        } else if (Ogg.getFileExtentionsList().contains(extension)){
            return new Ogg(f);
        } else if (Flac.getFileExtentionsList().contains(extension)){
            return new Flac(f);
        } else if (Mpc.getFileExtentionsList().contains(extension)){
            return new Mpc(f);
        } else if (Ape.getFileExtentionsList().contains(extension)){
            return new Ape(f);
        } else if (Wav.getFileExtentionsList().contains(extension)){
            return new Wav(f);
        } else{
            throw new UnknowFileTypeException(f.getPath());
        }
    }

    /**
     * Creates a Title object from a Podcast.
     *
     *@param podcastItem ??
     *@param host ??
     *@param publisher ??
     *
     *@return the title object.
     */
    public static Title getTitleFromPodcast(org.w3c.dom.Node podcastItem,String host, String publisher){
        Title retValue = null;
        java.net.URL url = null;
        String date = "";
        String title = "";
        String album = "";
        String artist = "";
        long size = 0;
        org.w3c.dom.Node nodeTitle = Title.getChildNode("title",podcastItem);
        
        org.w3c.dom.Node nodeEnclosure = Title.getChildNode("enclosure",podcastItem);
        org.w3c.dom.Node nodeDate = Title.getChildNode("pubDate",podcastItem);
        try{
            date = nodeDate.getTextContent().replaceAll("00:00:00 GMT","");
            title = nodeTitle.getTextContent();
            url = new java.net.URL(nodeEnclosure.getAttributes().getNamedItem("url").getTextContent());
        } catch(Exception e){
            e.printStackTrace();
        }
        try{
            size = new Long(nodeEnclosure.getAttributes().getNamedItem("length").getTextContent()).longValue();
        } catch(Exception e){
            logger.warning("Exception : Could not find title size..." + title);
        }
        
        
        if (url!=null){
            if (url.getFile().toUpperCase().endsWith(".MP3")){
                retValue = new Mp3(url);
            } else if (url.getFile().toUpperCase().endsWith(".WMA")){
                retValue = new Wma(url);
            } else if (url.getFile().toUpperCase().endsWith(".OMA")){
                retValue = null;        //Not supported for now...
            } else if (url.getFile().toUpperCase().endsWith(".OGG")){
                retValue = new Ogg(url);
            } else if (url.getFile().toUpperCase().endsWith(".WAV")){
                retValue = new Wav(url);
            } else{
                retValue = new Mp3(url);
            }
        }
        retValue.setAlbumName(publisher);
        retValue.setArtistName(host);
        retValue.setTitleName(title);
        retValue.fileSize = size;
        return retValue;
    }
    
    /**
     * Get the child from a node.
     *
     *@param name ??
     *@param node ??
     *
     *@return the node.
     */
    private static org.w3c.dom.Node getChildNode(String name,org.w3c.dom.Node node){
        org.w3c.dom.Node retValue = null;
        org.w3c.dom.NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            if(list.item(i).getNodeName().equals(name)){
                retValue = list.item(i);
                break;
            }
        }
        return retValue;
    }

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "audio";
    }
    
/* GET METHODS */
    /**
     *Obtains the album name corresponding to the title.
     *
     *@return the album name, no more than 60 char long.
     */
    public String getAlbum(){
        if(albumName.length() > 60) {albumName = (String)albumName.subSequence(0,59);}
        return albumName;
    }
    
    /**
     *Obtains the artist name corresponding to the title.
     *
     *@return the artist name, no more than 60 char long.
     */
    public String getArtist(){
        if(artistName.length() > 60) {artistName = (String)artistName.subSequence(0,59);}
        return artistName;
    }
    
    /**
     *Obtains the title name corresponding to the title.
     *
     *@return the title name, no more than 60 char long.
     */
    public String getTitle(){
        if(titleName.length() > 60) {titleName = (String)titleName.subSequence(0,59);}
        return titleName;
    }
        
    /**
     *Obtains the genre corresponding to the title.
     *
     *@return the genre, no more than 60 char long.
     */
    public String getGenre(){
        if(genre.length() > 60) {genre = (String)genre.subSequence(0,59);}
        return genre;
    }
    
    /**
     *Obtains the track number corresponding to the title.
     *
     *@return the track number.
     */
    public int getTitleNumber(){
        return titleNumber;
    }
    
    /**
     *Obtains the year corresponding to the title.
     *
     *@return the year.
     */
    public int getYear(){
        return year;
    }
    
    /**
     *Obtains encoding method (variable or constant bitrate) corresponding to the title.
     *
     *@return true if VBR and false if CBR.
     */
    public boolean getVbr(){
        return vbr;
    }
    
    /**
     *Obtains frequency used to encode the title.
     *
     *@return the frequency in kHz.
     */
    public double getFrequency(){
        return frequency;
    }

    /**
     *Obtains the number of channel.
     *
     *@return the number of channels.
     */
    public int getNbChannels(){
        return nbChannels;
    }
    
    /**
     *Obtains a stream to read the title from.
     *
     *@return the stream.
     */    
    public java.io.InputStream getInputStream() throws java.io.IOException{
        java.io.InputStream in = null;
        if (sourceURL!=null){
            in = sourceURL.openStream();
        }
        return in;
    }
    
    /**
     *Obtains the file described by this title.
     *
     *@return the corresponding file.
     */
    public File getSourceFile(){
        return sourceFile;
    }
    
    /**
     *Obtains the bitrate this title.
     *
     *@return the bitrate.
     */
    public int getBitRate(){
        return bitRate;
    }
    
    /**
     *Obtains the length of the title in millisecond.
     *
     *@return the length of the title.
     */
    public int getLength() {
        return length;
    }
    
    /**
     *Obtains the format of the title (OMA, MP3,...).
     *Constant are defined in the class "Title" to associated format to number. For instance Title.MP3 = 2.
     *
     *@return the format of the title as an int. 
     */
    public int getFormat() {
        return format;
    }

    /**
     *Obtains the format of the title (OMA, MP3,...) as a string.
     *
     *@return the format of the title as a string.
     */
    public String getFormatAsString() {
        return "Audio";
    }

    /**
     *Obtains the file size.
     *
     *@return the size of the file, in octets.
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     *Obtains the status of the title (NONE, TOIMPORT,...).
     *Constant are defined in the class "Title" to associated status to number. For instance Title.NONE = 0.
     *
     *@return the format of the title as an int. 
     */
    public int getStatus() {
        return status;
    }
    
    /**
     *Obtains the number of bytes before music.
     *
     *@return the number of bytes to skip to get the music.
     */
    public int getNbOfBytesBeforeMusic() {
        return nbOfBytesBeforeMusic;
    }

    /**
     *Obtains the size of the file after transcodage.
     *
     *@return The size of the file after transcodage if a transcodage is planned, the current size otherwise.
     */
    public long getFileSizeAfterTranscodage() {
        if(fileSizeAfterTranscodage == 0){
            return fileSize;
        }
        else{
            return fileSizeAfterTranscodage;
        }
    }

    /**
     * Obtains the source URL of the title.
     *
     *@return the URL.
     *
    public java.net.URL getSourceURL(){
        return this.sourceURL;
    } temporary commented, could be uncomment when podcast will be implemented*/
    
    
    
/* SET METHODS */
    /**
     * Change the name of the title.
     *
     *@param newTitleName the new title name.
     */
    public void setTitleName(String newTitleName){
        titleName = newTitleName;
    }

    /**
     * Change the artist of the title.
     *
     *@param newArtistName the new artist name.
     */
    public void setArtistName(String newArtistName){
        artistName = newArtistName;
    }

    /**
     * Change the album of the title.
     *
     *@param newAlbumName the new album name.
     */
    public void setAlbumName(String newAlbumName){
        albumName = newAlbumName;
    }
    
    /**
     * Change the genre of the title.
     *
     *@param newGenre the new genre.
     */
    public void setGenre(String newGenre){
        genre = newGenre;
    }

    /**
     * Change the format of the title.
     * Constant are defined in the class "Title" to associated format to number. For instance Title.MP3 = 2.
     *
     *@param newFormat the new format to be assigned to the title.
     */
    public void setFormat(int newFormat) {
        format = newFormat;
    }

    /**
     * Change the status of the title.
     *Constant are defined in the class "Title" to associated status to number. For instance Title.NONE = 0.
     *
     *@param newStatus the new status to be assigned to the title.
     */
    public void setStatus(int newStatus) {
        status = newStatus;
    }
    
    /**
     * Change the size of the file after transcodage.
     *
     *@param newFileSizeAfterTranscodage The new size to be assigned to the title.
     */
    public void setFileSizeAfterTranscodage(long newFileSizeAfterTranscodage) {
        fileSizeAfterTranscodage = newFileSizeAfterTranscodage;
    }
    
/* STATIC METHODS */

    /**
     * Tell if the title is compatible, i.e. if it can be transfered as it is without transcodage. The answer depends on the configuration chosen by the user. For instance, if a title is a MP3@128kbps-44,1Hz (playable by all Sony devices after generation 1) and if in the configuration "transcode all" is set with a bitrate of 64kbps, answer will be FALSE. With the same title, if the configuration is on "transcode all files" with a bitrate of 128kbps, the answer will be TRUE,... ...
     *
     * @param title The title to test.
     * @param transcodeAllFiles Should be true is the configuration is set to "always transcode", false otherwise.
     * @param transcodeBitrate Indicates the bitrate set in the configuration to be used to transcode.
     * @param generation The generation of the device.
     * @param log True to log, false otherwise.
     * @return True if the title is compatible with the device (considering the configuration) and false otherwise.
     */
    public static boolean isCompatible(Title title, Boolean transcodeAllFiles, int transcodeBitrate, int generation, boolean log){
        //In general case, all files should be transcoded, except
        //  - OMA
        //  - valid MP3 for generation 1 and above
        //  - valid WMA for generation 4 and above
        //  - valid AAC for generation 6 and above
        // If transcode all files is on, only OMA and MP3 valid files can not be transcoded
        if(title.getFormat() == Title.OMA){
            return true;
        }
        else if((title.getFormat() == Title.MP3) && (generation >= 1)){
            return Mp3.isCompatible(title, transcodeAllFiles, transcodeBitrate, generation, log);
        }
        else if((title.getFormat() == Title.WMA) && (generation >= 4)){
            return Wma.isCompatible((Wma)title, transcodeAllFiles, transcodeBitrate, log);
        }
        else if((title.getFormat() == Title.AAC) && (generation >= 6)){
            return Aac.isCompatible((Aac)title, transcodeAllFiles, transcodeBitrate, log);
        }
        else{
            if(log){logger.info("File must be transcoded (format: "+title.getFormat()+"):"+title.getSourceFile().getPath());}
            return false;
        }
    }

    public static Map GetSupportedFileFormats(int deviceGeneration){
        // Store the file formats supported in an associative Map (with their names in the correct language and their extentions)
        Map fileExtentionMap = new TreeMap();
        // Files always supported, whatever the generation or FFMPEG presence:
        fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.All_Files"), "");
        fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Oma_Files"), Oma.getFileExtentionsList());
        fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Mp3_Files"), Mp3.getFileExtentionsList());

        if(FFMpegToolBox.isFFMpegPresent()) {
            // Set file extensions available when FFMPEG is present
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Aac_Files"), Aac.getFileExtentionsList());
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Ape_Files"), Ape.getFileExtentionsList());
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Flac_Files"), Flac.getFileExtentionsList());
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Mpc_Files"), Mpc.getFileExtentionsList());
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Ogg_Files"), Ogg.getFileExtentionsList());
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Wav_Files"), Wav.getFileExtentionsList());
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Wma_Files"), Wma.getFileExtentionsList());
            fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.All_Music_Files"), Oma.getFileExtentionsList()+","+Mp3.getFileExtentionsList()+","+Aac.getFileExtentionsList()+","+Ape.getFileExtentionsList()+","+Flac.getFileExtentionsList()+","+Mpc.getFileExtentionsList()+","+Ogg.getFileExtentionsList()+","+Wav.getFileExtentionsList()+","+Wma.getFileExtentionsList());
        }
        else {
            // Set files extension available when FFMPEG is not present, this depends on the generation
            if(deviceGeneration < 4) {
                fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.All_Music_Files"), Oma.getFileExtentionsList()+","+Mp3.getFileExtentionsList());
            }
            else if(deviceGeneration < 6) {
                // Wma is natively supported
                fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Wma_Files"), Wma.getFileExtentionsList());
                fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.All_Music_Files"), Oma.getFileExtentionsList()+","+Mp3.getFileExtentionsList()+","+Wma.getFileExtentionsList());
            }
            else{
                // Wma and Aac is natively supported for generation >= 6
                fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Aac_Files"), Aac.getFileExtentionsList());
                fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.Wma_Files"), Wma.getFileExtentionsList());
                fileExtentionMap.put(java.util.ResourceBundle.getBundle("localization/localpanel").getString("LocalPanel.All_Music_Files"), Oma.getFileExtentionsList()+","+Mp3.getFileExtentionsList()+","+Aac.getFileExtentionsList()+","+Wma.getFileExtentionsList());
            }
        }

        // Return the map
        return fileExtentionMap;
    }

/* METHODS */
    
    /**
     *Obtains the size of the title in octets (bytes).
     *
     *@return the size of the title.
     */
    public long size(){
        return fileSize;
    }
    
    /**
     *Obtains a "string" view of the title to be printed.
     *
     *@return a string describing the title.
     */
    @Override
    public String toString(){
        return artistName + " - " + titleName;
    }

    /**
     * Read the info from the name of the title and the parent folders to guess the tag info (title, title number, artist, album,...)
     * Pattern format is composed with tag fields (e.g. <title>), folder hierarchy and any other separotors. Rules are:
     *  - "/" is used to separate folders
     *  - "<tagfield>" is used to identify a tag field
     *  - possible tag fields are "artist", "album", "genre", "title", "track", "year"
     *  - all other characters are supposed to be part of the scheme ("<" and ">" can be used)
     *  - each tag field can appear once
     * Examples of valid patterns: "[<genre>] <artist>/<year> - <album>/<track>. <title>" or "<artist>/<album> <<year>>-<track>. <title>" or "<artist>/<year>-<album>/<track>-<title>"
     */
    private void readTagInfo() {
        List<String> tagFields = new ArrayList(); // List of possible tag fields
        Map<Integer, String> tagIndexes; // Position of the tag fiels into a pattern's level associate with th tag field name
        List<Integer> tagIndexesSorted; // Position of the tag fiels into a pattern's level sorted
        String currentTag = ""; // Name of the current tag field scanned
        int currentTagIndex; // Position of the current tag field scanned
        int nextTagIndex; // Position of the next tag field scanned
        String patternLevelToScan; // The rest of the pattern level to be scanned
        Boolean tagFieldLeft = true; // Semaphore to loop into pattern's levels searching for tag field
        File folderToScan = sourceFile; // Folder currently scanned to get the info
        String folderToScanName; // Name of the folder currently scanned to get the info
        String separator; // String corresponding to the separator between the current tag field and the next one
        int separatorLength; // Length of the separator (this might be different from separator.length() since escape chars might be added to "separator" for special chars)
        // Iterators
        Iterator tagFieldsIt;
        Iterator tagIndexesSortedIt;

        // Initialize variables, in the default language
        ResourceBundle jspropBundle = java.util.ResourceBundle.getBundle("localization/jsymphonicproperties");
        tagFields.add("<"+jspropBundle.getString("JSymphonicProperties.Artist")+">"); // Artist
        tagFields.add("<"+jspropBundle.getString("JSymphonicProperties.Album")+">"); // Album
        tagFields.add("<"+jspropBundle.getString("JSymphonicProperties.Genre")+">"); // Genre
        tagFields.add("<"+jspropBundle.getString("JSymphonicProperties.Title")+">"); // Title
        tagFields.add("<"+jspropBundle.getString("JSymphonicProperties.TrackNumber")+">"); // TrackNumber
        tagFields.add("<"+jspropBundle.getString("JSymphonicProperties.Year")+">"); // Year
        tagFields.add("<"+jspropBundle.getString("JSymphonicProperties.Ignore")+">"); // Ignore

        // First of all,  split the different pattern's levels
        String[] patternLevels = tryReadTagInfoPattern.split("/") ;

        // And scan each level (first one is the file name, then the parent folder, then the grandparent folder and so on)
        for(int i=patternLevels.length - 1; i >= 0; i--){
            // If this is the first pattern level, read the file name and suppress its extension
            if(i == patternLevels.length - 1){
                // Get the name of the folder currently scanned
                folderToScanName = folderToScan.getName();

                // Remove extension, split the fileName using "." as separator, without limit, last token found is the extension
                String[] tokens = folderToScanName.split("\\.") ;
                folderToScanName = folderToScanName.replace("." + tokens[tokens.length - 1], "");
            }
            else {
                // Else, get the folder parent and read its name
                try{
                    folderToScan = folderToScan.getParentFile();
                    folderToScanName = folderToScan.getName();
                }
                catch(Exception e){
                    // If an exception is thrown here, it means that there is no parent folder, in this case, just stop reading info
                    return;
                }
            }
            
            // Init the patternLevelToScan variable
            patternLevelToScan = patternLevels[i];
            tagFieldLeft= true;
            while(tagFieldLeft) {
                // Check that there are characters left to scan
                if(folderToScanName.length() <= 0){
                    // If not, just stop here
                    tagFieldLeft = false;
                    continue;
                }

                // Get first and second tag fields (lowest non-zero index and the following)
                // First get each tag field position in this level
                tagFieldsIt = tagFields.iterator();
                tagIndexes = new HashMap();
                while(tagFieldsIt.hasNext()){
                    String tagFieldTemp = (String)tagFieldsIt.next();
                    tagIndexes.put(patternLevelToScan.indexOf(tagFieldTemp), tagFieldTemp); // -1 is returned if tag field is not present
                }
                // Then sort the indexes and get the first and second ones not negative
                currentTagIndex = -1; // Init
                nextTagIndex = -1; // Init
                tagIndexesSorted = new ArrayList(tagIndexes.keySet());
                Collections.sort(tagIndexesSorted); // Sort the indexes
                tagIndexesSortedIt = tagIndexesSorted.iterator();
                while(tagIndexesSortedIt.hasNext()){
                    int tagIndexTmp = (Integer)tagIndexesSortedIt.next();
                    if((currentTagIndex == -1) && (tagIndexTmp >= 0)){
                        currentTagIndex = tagIndexTmp; // Set the first not negative index as the current one
                        currentTag = tagIndexes.get(currentTagIndex);
                    }
                    else if((nextTagIndex == -1) && (currentTagIndex >= 0)){
                        nextTagIndex = tagIndexTmp; // Set the following index as the next one
                    }
                }

                if(currentTagIndex < 0){
                    // If currentTagIndex value has not changed, there is no tag field left, exit the loop
                    tagFieldLeft = false;
                    continue;
                }

                // Skip extra char before the tag field
                folderToScanName = folderToScanName.substring(currentTagIndex);
                patternLevelToScan = patternLevelToScan.substring(currentTagIndex);

                // If there is no next tag field, fake it
                if(nextTagIndex < 0){
                    nextTagIndex = patternLevelToScan.length();
                }

                // Get separator between current tag field and the next one
                separator = patternLevelToScan.substring(currentTag.length(), nextTagIndex);
                separatorLength = separator.length();
                // If separator contains special char, escape them (special chars for regexp)
                if(separator.contains(".")) separator = separator.replace(".", "\\.");
                if(separator.contains("(")) separator = separator.replace("(", "\\(");
                if(separator.contains(")")) separator = separator.replace(")", "\\)");
                if(separator.contains("[")) separator = separator.replace("[", "\\[");
                if(separator.contains("]")) separator = separator.replace("]", "\\]");
                if(separator.contains("{")) separator = separator.replace("{", "\\{");
                if(separator.contains("}")) separator = separator.replace("}", "\\}");
                if(separator.contains("^")) separator = separator.replace("^", "\\^");
                if(separator.contains("$")) separator = separator.replace("$", "\\$");
                if(separator.contains("?")) separator = separator.replace("?", "\\?");
                if(separator.contains("*")) separator = separator.replace("*", "\\*");
                if(separator.contains("+")) separator = separator.replace("+", "\\+");

                // Split the folderToScanName variable according to the separator if it is not empty, otherwise, do not split the variable and take all chars
                String[] tokens;
                if(separator.length() > 0){
                    tokens = folderToScanName.split(separator);
                }
                else {
                    tokens = new String[1];
                    tokens[0] = folderToScanName;
                }

                if(tokens.length == 0) {
                    // If nothing has been returned by the split method, an error occured, stop here
                    tagFieldLeft = false;
                    continue;
                }

                // Value of the tag field is given by the first token
                if(currentTag.compareTo("<"+java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Artist")+">") == 0) artistName = tokens[0];
                if(currentTag.compareTo("<"+java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Album")+">") == 0) albumName = tokens[0];
                if(currentTag.compareTo("<"+java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Genre")+">") == 0) genre = tokens[0];
                if(currentTag.compareTo("<"+java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Title")+">") == 0) titleName = tokens[0];
                if(currentTag.compareTo("<"+java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.TrackNumber")+">") == 0){
                    try {
                        titleNumber = Integer.parseInt(tokens[0]);
                    }
                    catch(Exception e){} // If there is an error while reading, just ignore it
                }
                if(currentTag.compareTo("<"+java.util.ResourceBundle.getBundle("localization/jsymphonicproperties").getString("JSymphonicProperties.Year")+">") == 0){
                    try {
                        year = Integer.parseInt(tokens[0]);
                    }
                    catch(Exception e){} // If there is an error while reading, just ignore it
                }

                // Prepare variable for next loop
                if(folderToScanName.length() <= tokens[0].length()+separator.length()){
                    // If more char than existing have been read, just leave the variable to an empty string
                    folderToScanName = "";
                }
                else{
                    // Else, suppress what have been read
                    folderToScanName = folderToScanName.substring(tokens[0].length()+separatorLength);
                }
                patternLevelToScan = patternLevelToScan.substring(nextTagIndex);
            }
        }

        // When all if finished, check if something has been guessed for the artist and title, if not, force the use of file name (without extension) and parent folder name
        if(titleName.compareTo("unknown title (JStitle)") == 0){
            String fileName = sourceFile.getName();
            String[] tokens = fileName.split("\\.") ;
            titleName = fileName.replace("." + tokens[tokens.length - 1], "");
        }
        if(artistName.compareTo("unknown artist (JStitle)") == 0) artistName = sourceFile.getParentFile().getName();
    }

    /**
     * Write the tag info to the file.
     */

    /**
     * Write the tag in a file stream from the info in the current object.
     *
     * @param stream the stream of the file to write the tag to.
     * @param gotKey True if the player mounted is protected, False otherwise (this arg is only used when importing a file to the device).
     * @throws java.lang.Exception
     */
    public void writeTagInfoToFile(FileOutputStream stream, boolean gotKey) throws Exception {
        // Nothing to do here.
    }

    /**
     * Copy the tag info (title, number, artist, album, genre and year) from the title given in argument to the current title instance.
     * 
     * @param titleToDecode The title to read the tag info from.
     */
    public void copyTagInfo(Title titleWithInfo) {
        titleName = titleWithInfo.getTitle();
        titleNumber = titleWithInfo.getTitleNumber();
        artistName = titleWithInfo.getArtist();
        albumName = titleWithInfo.getAlbum();
        year = titleWithInfo.getYear();
        genre = titleWithInfo.getGenre();
    }
}
