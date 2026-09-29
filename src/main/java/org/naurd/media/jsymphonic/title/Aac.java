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
 * Aac.java
 *
 * Created on October 2, 2006, 10:22 PM
 *
 */

package org.naurd.media.jsymphonic.title;

import java.io.File;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp4.Mp4AudioHeader;
import org.jaudiotagger.tag.Tag;

/**
 * Aac class describing an AAC title.
 *
 * AAC is a generic name for MPEG4 format and it covers severals formats:
 *  - AAC (LC-AAC) (AAC) Low Complexity Advanced Audio Coding (Apple iTunes files)
 *  - AAC (MAIN) LC profile with backwards prediction
 *  - AAC (LTP) main profile with forward prediction
 *  - AAC (SRS) submitted by Sony and reportedly similar to ATRAC/3
 *  - AAC+ (HE-AAC) (aacPLUS) High Efficiency Advanced Audio Coding
 *  - eAAC+ (HE-AAC-PS) (Enhanced AAC), High Efficiency Advanced Audio Coding with Parametric Stereo
 *  - ALAC (VBR AAC) (AAC with Variable Bit-Rate), Apple’s Lossless Advanced Coding
 *  - ...
 * Most common are LC-AAC (used by iTunes and natively supported by newer Walkmans), HE-AAC and ALAC (not supported by any Walkmans).
 *
 * Possible extensions for AAC files are "mp4" and "aac".
 * Apple introduces others extensions: "m4a" (mpeg 4 audio), "m4b" (mpeg 4 book) has DRM, is used for audio book and has bookmarking capability, "m4p" (mpeg 4 protected) has DRM.
 * "3gp" is a video container (with AAC audio) but this extension is used by SonicStage when ripping to AAC.
 *
 *
 * @author nicolas_cardoso
 */
public class Aac extends Title {
/* FIELDS */
    // Title information
    protected String brand = ""; // The key for the ftyp brand, could be "M4A" (iTunes M4A), "3gp6" (Nero Digital AAC HE v1 or v2, or SonicStage AAC), "mp42" (Nero Digital AAC LC)
    protected String aacFormat = ""; // The actual format, since "m4a" extension could also be used for Apple Lossless format, could be "AAC" or "Apple Lossless"

/* CONSTRUCTORS */
    /** Creates a new instance of Aac */
    public Aac(String aacPath){
        this(new File(aacPath));
    }

    public Aac(File f) {
        // Use the super constructor
        super(f);

        // Set files info
        format = Title.AAC;
        fileSize = f.length();

        // Read info from header (using JAudioTagger lib)
        try {
            Mp4AudioHeader AACinfo = (Mp4AudioHeader) AudioFileIO.read(f).getAudioHeader();
            bitRate = (int) AACinfo.getBitRateAsNumber();
            frequency = AACinfo.getSampleRateAsNumber()/1000.0; // this method returns the frequency in Hz, it should be in kHz
            vbr = AACinfo.isVariableBitRate();
            length = (int) (AACinfo.getPreciseLength() * 1000); // this method returns the tracklength in second, it should be in millisecond
            nbOfBytesBeforeMusic = (int) readNbOfBytesBeforeMusic();
            nbChannels = AACinfo.getChannelNumber();
            brand = AACinfo.getBrand();
            aacFormat = AACinfo.getFormat();
        }
        catch (Exception ex) {
            logger.warning("Exception while reading AAC info for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
        }

        // Read the tag if set so in the config file
        if(tryReadTagInfo){
            try {
                Tag MP4tag = AudioFileIO.read(f).getTag();
                if(MP4tag.getFirstTitle().length() > 0){ titleName = MP4tag.getFirstTitle();} // Read field is it exist
                if(MP4tag.getFirstArtist().length() > 0){ artistName = MP4tag.getFirstArtist();} // Read field is it exist
                if(MP4tag.getFirstAlbum().length() > 0){ albumName = MP4tag.getFirstAlbum();} // Read field is it exist
                if(MP4tag.getFirstGenre().length() > 0){ genre = MP4tag.getFirstGenre();} // Read field is it exist
                if(MP4tag.getFirstYear().length() > 0){ year = Integer.parseInt(MP4tag.getFirstYear());} // Read field is it exist
                if(MP4tag.getFirstTrack().length() > 0){ // Read field is it exist
                    if(MP4tag.getFirstTrack().contains("/")){
                        titleNumber = Integer.parseInt((MP4tag.getFirstTrack().split("/",2))[0]);
                    }
                    else{
                        titleNumber = Integer.parseInt(MP4tag.getFirstTrack());
                    }
                }
            }
            catch (Exception ex) {
                logger.warning("Exception while reading AAC tag for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
            }
        }
    }

    /**
     * Create an instance from a file and an existing Oma file (used to unwrap title).
     *
     * @param file The file which will contains the unwrapped file.
     * @param title The title to read the info from.
     */
    public Aac(File file, Oma title){
        sourceFile = file; // This instance is a temporary fake, music data shouldn't be read from this instance
        format = Title.AAC;
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
        nbChannels = title.getNbChannels();
        nbOfBytesBeforeMusic = title.getNbOfBytesBeforeMusic();
    }
    
    public Aac(java.net.URL url){
        super(url);
    }

/* METHODS */
    /**
     * Compute the number of bytes before music, i.e. the number of bytes before the string "mdat".
     *
     * @return The number of bytes before music.
     */
    public long readNbOfBytesBeforeMusic() {
        java.io.InputStream aacStream = null; // The stream used to read the data
        byte[] buffer1 = new byte[1]; //Buffer (1 byte long)
        byte[] buffer4 = new byte[4]; //Buffer (4 bytes long)
        long offset = 0; // A temp variable to compute the nb of bytes before music
        long maxNumberOfBytes = this.getSourceFile().length(); // get max number of bytes to prevent from infinite loop

        // Search the number of bytes before the string "mdat"
        try{
            // Open the stream
            aacStream = this.getSourceFile().toURI().toURL().openStream();

            // Read the First 4 bytes from the file
            aacStream.read(buffer4);
            offset += 4;
            String bufferString = new String(buffer4, 0, 4); //Convert buffer to string
            while((bufferString.compareTo("mdat") != 0) && (offset < maxNumberOfBytes)){
                 // Read following byte
                aacStream.read(buffer1);
                offset++;

                // Update the buffer
                buffer4[0] = buffer4[1];
                buffer4[1] = buffer4[2];
                buffer4[2] = buffer4[3];
                buffer4[3] = buffer1[0];

                // Compose new string
                bufferString = new String(buffer4, 0, 4);
            }

            // Return the number of bytes read (including the "mdat")
            return offset;
        }
        catch(Exception e){
            logger.warning("An error occured while trying to determinate the number of bytes before music for file: "+this.getSourceFile()+". Error was: "+e.getMessage());
            return 0;
        }
    }

/* STATIC METHODS */

    /**
     * Tell if the AAC title is compatible with the device, i.e. if it can be transfered as it is without transcodage. The answer depends on the configuration chosen by the user.
     *
     * @param title The title to test.
     * @param transcodeAllFiles Should be true is the configuration is set to "always transcode", false otherwise.
     * @param transcodeBitrate Indicates the bitrate set in the configuration to be used to transcode.
     * @param log True to log, false otherwise.
     * @return True if the title is compatible with the device (considering the configuration) and false otherwise.
     */
    public static boolean isCompatible(Aac title, Boolean transcodeAllFiles, int transcodeBitrate, boolean log){
        return false; /* TODO implement AAC support !
        // If the files must be transcoded, only MP3 is allowed.
        if(transcodeAllFiles){
            if(log){logger.info("File must be transcoded (forced):"+title.getSourceFile().getPath());}
            return false;
        }

        // If frequency is different from correct ones return false
        if((title.getFrequency() != 8) && (title.getFrequency() != 11.025) && (title.getFrequency() != 12) && (title.getFrequency() != 16) && (title.getFrequency() != 22.05) && (title.getFrequency() != 24) && (title.getFrequency() != 32) && (title.getFrequency() != 44.1) && (title.getFrequency() != 48) && (title.getFrequency() != 64) && (title.getFrequency() != 88.2) && (title.getFrequency() != 96)){
            if(log){logger.info("File must be transcoded (frequency "+title.getFrequency()+"):"+title.getSourceFile().getPath());}
            return false;
        }

        // If VBR bitrate is not in the range, return false
        if(( title.getBitRate() < 48)  || ( title.getBitRate() > 320)){
            // bitrate is not correct
            if(log){logger.info("File must be transcoded (bitrate "+title.getBitRate()+"):"+title.getSourceFile().getPath());}
            return false;
        }

        // If file is actually encoded with Apple Lossless format, or AAC is not Low Complexity, file must be transcoded
        if((title.aacFormat.compareTo("apple lossless") == 0) || (title.brand.compareTo("3gp6") == 0) ){
            // Format is not correct
            if(log){logger.info("File must be transcoded (format "+title.aacFormat+"-"+title.brand+"):"+title.getSourceFile().getPath());}
            return false;
        }

        // Otherwise, return true
        return true;
*/
    }

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "mp4,m4a,3gp"; // aac is not known in the jaudiotagger lib (as was 3gp before I added it)
    }

/* GET METHODS */
    
    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        return "AAC";
    }
}
