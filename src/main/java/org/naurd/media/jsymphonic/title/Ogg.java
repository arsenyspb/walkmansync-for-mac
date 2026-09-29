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
 * Ogg.java
 *
 * Created on October 15, 2006, 11:00 AM
 *
 */

package org.naurd.media.jsymphonic.title;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;

/**
 *
 * @author Pat
 */
public class Ogg extends Title{
    
    /** Creates a new instance of Ogg */
    public Ogg(java.net.URL url) {
        super(url);
    }
    public Ogg(java.io.File f){
        super(f);
        format = Title.OGG;

        // Read info from header (using JAudioTagger lib)
        try {
            AudioHeader OGGinfo = AudioFileIO.read(f).getAudioHeader();
            bitRate = (int) OGGinfo.getBitRateAsNumber();
            frequency = OGGinfo.getSampleRateAsNumber()/1000.0; // this method returns the frequency in Hz, it should be in kHz
            vbr = OGGinfo.isVariableBitRate();
            nbChannels = Integer.parseInt(OGGinfo.getChannels());
            length = OGGinfo.getTrackLength()*1000; // this method returns the tracklength in second, it should be in millisecond
        }
        catch (Exception ex) {
            logger.warning("Exception while reading OGG info for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
        }

        // Read the Vorbis tag if set so in the config file
        if(tryReadTagInfo){
            try {
                Tag OGGtag = AudioFileIO.read(f).getTag();
                if(OGGtag.getFirstTitle().length() > 0){ titleName = OGGtag.getFirstTitle();} // Read field is it exist
                if(OGGtag.getFirstArtist().length() > 0){ artistName = OGGtag.getFirstArtist();} // Read field is it exist
                if(OGGtag.getFirstAlbum().length() > 0){ albumName = OGGtag.getFirstAlbum();} // Read field is it exist
                if(OGGtag.getFirstGenre().length() > 0){ genre = OGGtag.getFirstGenre();} // Read field is it exist
                if(OGGtag.getFirstTrack().length() > 0){ titleNumber = Integer.parseInt(OGGtag.getFirstTrack());} // Read field is it exist
                if(OGGtag.getFirstYear().length() > 0){ year = Integer.parseInt(OGGtag.getFirstYear());} // Read field is it exist
            }
            catch (Exception ex) {
                logger.warning("Exception while reading OGG tag for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
            }
        }
    }

/* STATIC METHODS */

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "ogg";
    }

/* GET METHODS */

    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        return "OGG";
    }
}
