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
 * Wma.java
 *
 * Created on October 2, 2006, 10:22 PM
 *
 */

package org.naurd.media.jsymphonic.title;

import java.io.File;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;

/**
 *
 * @author Pat
 */
public class Wma  extends Title {
/* FIELDS */
    // Extra info for WMA file
    String wmaVersion = "";

/* CONSTRUCTORS */
    /**
     * Create a WMA title form a file. This consctructor read the meta-data to determinate the bitrate, the length,... and the Tag info (if requested)
     *
     * @param f the file to create the instance from.
     */
    public Wma(java.io.File f) {
        super(f);
        format = Title.WMA;
        nbOfBytesBeforeMusic = 0; // Always 0 (the whole WMA file is always copied)

        // Read info from header (using JAudioTagger lib)
        try {
            AudioHeader WMAinfo = AudioFileIO.read(f).getAudioHeader();
            bitRate = (int) WMAinfo.getBitRateAsNumber();
            frequency = WMAinfo.getSampleRateAsNumber()/1000.0; // this method returns the frequency in Hz, it should be in kHz
            vbr = WMAinfo.isVariableBitRate();
            nbChannels = Integer.parseInt(WMAinfo.getChannels());
            length = WMAinfo.getTrackLength()*1000; // this method returns the tracklength in second, it should be in millisecond
            wmaVersion = WMAinfo.getFormat();
        }
        catch (Exception ex) {
            logger.warning("Exception while reading WMA info for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
        }

        // Read the tag if set so in the config file
        if(tryReadTagInfo){
            try {
                Tag WMAtag = AudioFileIO.read(f).getTag();
                if(WMAtag.getFirstTitle().length() > 0){ titleName = WMAtag.getFirstTitle();} // Read field is it exist
                if(WMAtag.getFirstArtist().length() > 0){ artistName = WMAtag.getFirstArtist();} // Read field is it exist
                if(WMAtag.getFirstAlbum().length() > 0){ albumName = WMAtag.getFirstAlbum();} // Read field is it exist
                if(WMAtag.getFirstGenre().length() > 0){ genre = WMAtag.getFirstGenre();} // Read field is it exist
                if(WMAtag.getFirstTrack().length() > 0){ titleNumber = Integer.parseInt(WMAtag.getFirstTrack());} // Read field is it exist
                if(WMAtag.getFirstYear().length() > 0){ year = Integer.parseInt(WMAtag.getFirstYear());} // Read field is it exist
            }
            catch (Exception ex) {
                logger.warning("Exception while reading WMA tag for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
            }
        }
    }
        
    /**
     * Create an instance from a file and an existing Oma file (used to unwrap title).
     *
     * @param file The file which will contains the unwrapped file.
     * @param title The title to read the info from.
     */
    public Wma(File file, Oma title){
        sourceFile = file; // This instance is a temporary fake, music data shouldn't be read from this instance
        format = Title.WMA;
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
        nbOfBytesBeforeMusic = 0;
        nbChannels = title.getNbChannels();
    }

    /**
     * Create an instance from an URL (for podcast).
     *
     * @param url
     */
    public Wma(java.net.URL url){
        super(url);
    }


/* STATIC METHODS */

    /**
     * Tell if the WMA title is compatible with the device, i.e. if it can be transfered as it is without transcodage. The answer depends on the configuration chosen by the user.
     *
     * @param title The title to test.
     * @param transcodeAllFiles Should be true is the configuration is set to "always transcode", false otherwise.
     * @param transcodeBitrate Indicates the bitrate set in the configuration to be used to transcode.
     * @param log True to log, false otherwise.
     * @return True if the title is compatible with the device (considering the configuration) and false otherwise.
     */
    public static boolean isCompatible(Wma title, Boolean transcodeAllFiles, int transcodeBitrate, boolean log){
        // If the files must be transcoded, only MP3 is allowed.
        if(transcodeAllFiles){
            if(log){logger.info("File must be transcoded (forced):"+title.getSourceFile().getPath());}
            return false;
        }

        // If file is encoded with Pro or lossless methods, it can't be read on Sony's Walkman
        if((title.getWmaVersion().toLowerCase().contains("professional")) || (title.getWmaVersion().toLowerCase().contains("lossless"))){
            if(log){logger.info("File must be transcoded (format "+title.getWmaVersion()+"):"+title.getSourceFile().getPath());}
            return false;
        }

        // If frequency is different from "44.1 kHz" return false
        if(title.getFrequency() != 44.1){
            if(log){logger.info("File must be transcoded (frequency "+title.getFrequency()+"):"+title.getSourceFile().getPath());}
            return false;
        }

        // If VBR bitrate is not in the range, return false
        if(( title.getBitRate() < 48)  || ( title.getBitRate() > 320)){
            // bitrate is not good
            if(log){logger.info("File must be transcoded (bitrate "+title.getBitRate()+"):"+title.getSourceFile().getPath());}
            return false;
        }

        // Otherwise, return true
        return true;
    }

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "wma";
    }

/* GET METHODS */
    /**
     *Obtains the version of the WMA file.
     *
     *@return the version, in a string.
     */
    public String getWmaVersion(){
        return wmaVersion;
    }

    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        return "WMA";
    }
}
