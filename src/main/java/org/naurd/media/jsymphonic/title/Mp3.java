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
 * Mp3.java
 *
 * Created on October 2, 2006, 10:22 PM
 *
 */

package org.naurd.media.jsymphonic.title;

import java.io.File;
import java.io.FileOutputStream;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.reference.GenreTypes;

/**
 * Describes an MP3 files as it it needed in JSymphonic.
 *
 * @author Pat
 */
public class Mp3 extends Title {
/* FIELDS */
    // Title information
    protected byte mpegVersion = 3; // Mpeg version of the file, 3 stands for MPEG-1, 2 for MPEG-2 and 0 for MPEG-3 (or MPEG-2.5)
    protected byte layer = 1; // Layer of the file, 1 stands for LAYER-III, 2 for LAYER-II and 3 for LAYER-I
    protected long framesNumber; // Number of frames in the MP3 file

/* CONSTRUCTORS */
    /**
     * Create an instance from a string giving the path of the mp3 file.
     *
     * @param mp3Path The path of the mp3 file.
     */
    public Mp3(String mp3Path){
        this(new File(mp3Path));
    }

    /**
     * Create an instance from a file.
     *
     * @param f The file to create the instance from.
     */
    public Mp3(File f){
        // Use the super constructor
        super(f);

        // Set files info
        format = Title.MP3;
        fileSize = f.length();

        // When a file is transcoded, a new empty mp3 object is created, no info should be read since the file doesn't exist yet, so just return
        if(!f.exists()){
            return;
        }

        // Read info from header (using JAudioTagger lib)
        readMP3Info();

        // Read the ID3 tag if set so in the config file
        if(tryReadTagInfo){
            try {
                Tag MP3tag = AudioFileIO.read(f).getTag();
                if(MP3tag.getFirstTitle().length() > 0){ titleName = MP3tag.getFirstTitle();} // Read field is it exist
                if(MP3tag.getFirstArtist().length() > 0){ artistName = MP3tag.getFirstArtist();} // Read field is it exist
                if(MP3tag.getFirstAlbum().length() > 0){ albumName = MP3tag.getFirstAlbum();} // Read field is it exist
                if(MP3tag.getFirstGenre().length() > 0){ genre = MP3tag.getFirstGenre();} // Read field is it exist
                if(MP3tag.getFirstYear().length() > 0){ year = Integer.parseInt(MP3tag.getFirstYear());} // Read field is it exist
                if(MP3tag.getFirstTrack().length() > 0){ // Read field is it exist
                    if(MP3tag.getFirstTrack().contains("/")){
                        titleNumber = Integer.parseInt((MP3tag.getFirstTrack().split("/",2))[0]);
                    }
                    else{
                        titleNumber = Integer.parseInt(MP3tag.getFirstTrack());
                    }
                }
                try{
                    // Genre may be express as a number, try to parse it into an integer
                    Integer genreNumber = Integer.parseInt(genre);
                    // If this works (no exception thrown), convert it to a string
                    GenreTypes GenreList = GenreTypes.getInstanceOf();
                    genre = GenreList.getNameForId(genreNumber);
                }
                catch(Exception e){
                    // If an exception has been thrown, try to see if the tag isn't surrounded by brakets "()" as it could be when tagged with iTunes: "(20)" stands for "Alternative"
                    try{
                        Integer genreNumber = Integer.parseInt(genre.replace("(", "").replace(")", ""));
                        // If this works (no exception thrown), convert it to a string
                        GenreTypes GenreList = GenreTypes.getInstanceOf();
                        genre = GenreList.getNameForId(genreNumber);
                    }
                    catch(Exception ex){
                        // If an exception is thrown again, one assumes that the genre is already given as a string, and it is kept as it is
                    }
                }
            }
            catch (Exception ex) {
                logger.warning("Exception while reading MP3 tag for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
            }
        }
    }

    /**
     * Create an instance from a file and an existing Oma file (used to unwrap title).
     *
     * @param file The file which will contains the unwrapped file.
     * @param title The title to read the info from.
     */
    public Mp3(File file, Oma title){
        sourceFile = file; // This instance is a temporary fake, music data shouldn't be read from this instance
        format = Title.MP3;
        titleName= title.getTitle();
        artistName = title.getArtist();
        albumName = title.getAlbum();
        genre = title.getGenre();
        titleNumber = title.getTitleNumber();
        year = title.getYear();
        length = title.getLength();
        vbr = title.getVbr();
        bitRate = title.getBitRate();
        frequency = title.getFrequency();
        status = title.getStatus();
        mpegVersion = title.getMpegVersion();
        layer = title.getLayer();
        nbOfBytesBeforeMusic = title.getNbOfBytesBeforeMusic();
        nbChannels = title.getNbChannels();
    }

    /**
     * Create an instance from an URL.
     *
     * @param url The URL to instance the object from.
     */
    public Mp3(java.net.URL url){
        super(url);
        format = Title.MP3;

        // Read tag info
        albumName = url.getHost();
        artistName = url.getHost();
        titleName = url.getFile();
    }

/* STATIC METHODS */

    /**
     * Tell if the MP3 title is compatible with the device, i.e. if it can be transfered as it is without transcodage. The answer depends on the configuration chosen by the user.
     *
     * @param title The title to test.
     * @param transcodeAllFiles Should be true is the configuration is set to "always transcode", false otherwise.
     * @param transcodeBitrate Indicates the bitrate set in the configuration to be used to transcode.
     * @param log True to log, false otherwise.
     * @return True if the title is compatible with the device (considering the configuration) and false otherwise.
     */
    public static boolean isCompatible(Title title, Boolean transcodeAllFiles, int transcodeBitrate, int generation, boolean log){
        // Title is compatible if the sampling rate is 44.1kHz and the bitrate is valid (or, if transcodeAllFiles is true, if the bitrate is exactly the one asked)
        if((generation >= 5)){
            // For generation greater or equal to 5, 22.05, 44.1 and 48 kHz are playable
            if((title.getFrequency() != 22.05) && (title.getFrequency() != 44.1) && (title.getFrequency() != 48)) { // Frequence is given in kHz
                // frequency is not good, no need to check the bitrate, file must be converted
                if(log){logger.info("File must be transcoded (frequency "+title.getFrequency()+"):"+title.getSourceFile().getPath());}
                return false;
            }
        }
        else{
            if(title.getFrequency() != 44.1) { // Frequence is given in kHz
                // frequency is not good, no need to check the bitrate, file must be converted
                if(log){logger.info("File must be transcoded (frequency "+title.getFrequency()+"):"+title.getSourceFile().getPath());}
                return false;
            }
        }

        if(transcodeAllFiles) {
            // If all files should be transcoded, the bitrate should be tested, but it only can be if file is not VBR
            if(title.getVbr()){
                if(log){logger.info("File must be transcoded (forced):"+title.getSourceFile().getPath());}
                return false;
            }
            else{
                if(transcodeBitrate == title.getBitRate()){
                    return true;
                }
                else{
                    if(log){logger.info("File must be transcoded (forced):"+title.getSourceFile().getPath());}
                    return false;
                }
            }
        }
        else{
            // transcode need to be in the list, if it is not VBR
            if(title.getVbr()){
                return true;
            }
            else{
                if( ( title.getBitRate() == 48) || ( title.getBitRate() == 64) || ( title.getBitRate() == 96) || ( title.getBitRate() == 128) || ( title.getBitRate() == 160) || ( title.getBitRate() == 192) || ( title.getBitRate() == 256) || ( title.getBitRate() == 320) ){
                    return true;
                }
                else{
                    if(log){logger.info("File must be transcoded (bitrate "+title.getBitRate()+"):"+title.getSourceFile().getPath());}
                    return false;
                }
            }
        }
    }

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "mp3";
    }

/* GET METHODS */
    /**
     * Obtains the Mpeg version
     *
     * @return The mpeg version
     */
    public byte getMpegVersion(){
        return mpegVersion;
    }

    /**
     * Obtains the layer
     *
     * @return The layer
     */
    public byte getLayer(){
        return layer;
    }

    /**
     * Obtains the number of frames
     *
     * @return The number of frames
     */
    public long getFramesNumber(){
        return framesNumber;
    }

    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        return "MP3";
    }

/* SET METHODS */

/* METHODS */
    /**
     * Write the ID3Tag in a file stream from the info in the current object.
     *
     * @param stream the stream of the file to write the tag to.
     * @param gotKey True if the player mounted is protected, False otherwise (this arg is only used when importing a file to the device).
     * @throws java.lang.Exception
     */
    @Override public void writeTagInfoToFile(FileOutputStream stream, boolean gotKey) {
        
// TODO
    }

    /**
     * Read info from header (using JAudioTagger lib) to determine the bitrate, the frequency, if vbr, the length, the number of frames, the mpeg version, the layer the number of bytes before music and the number of channels.
     * This method is called when a new instance of "Mp3" class is created.
     * This method can be called again when a file has been transcoded to MP3 (When the instance of the Mp3 file has been created, no actual MP3 file was existing, and these infos were left to default, once the title has been transcoded to MP3, the MP3 file is then containing the actual music data, and this method is run again to determine the file infos).
     */
    public void readMP3Info(){
        try {
            MP3AudioHeader MP3info = (MP3AudioHeader) AudioFileIO.read(sourceFile).getAudioHeader();
            bitRate = (int) MP3info.getBitRateAsNumber();
            frequency = MP3info.getSampleRateAsNumber()/1000.0; // this method returns the frequency in Hz, it should be in kHz
            vbr = MP3info.isVariableBitRate();
            length = MP3info.getTrackLength()*1000; // this method returns the tracklength in second, it should be in millisecond
            framesNumber = MP3info.getNumberOfFrames();
            if(MP3info.getMpegVersion().contains("1")){
                mpegVersion = 3;
            }
            else if(MP3info.getMpegVersion().contains("3") || MP3info.getMpegVersion().contains("2.5")){
                mpegVersion = 0;
            }
            else{
                mpegVersion = 2;
            }
            if(MP3info.getMpegLayer().contains("3") || MP3info.getMpegLayer().contains("III")){
                layer = 1;
            }
            else if(MP3info.getMpegLayer().contains("2") || MP3info.getMpegLayer().contains("II")){
                layer = 2;
            }
            else{
                layer = 3;
            }
            nbOfBytesBeforeMusic = (int) MP3info.getMp3StartByte();
            if(MP3info.getChannels().toLowerCase().contains("stereo") || MP3info.getChannels().toLowerCase().contains("dual")){ // Channels could be "Stereo", "Joint Stereo", "Dual channel" or "Mono"
                nbChannels = 2;
            }
            else{
                nbChannels = 1;
            }
        }
        catch (Exception ex) {
            logger.warning("Exception while reading MP3 info for file: '" + sourceFile.getPath()+ "'. Exception: "+ex.getMessage());
        }
    }

}
