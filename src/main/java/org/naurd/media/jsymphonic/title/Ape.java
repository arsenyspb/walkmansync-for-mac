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
 * Ape.java
 *
 * Created on March 06, 2008, 11:00 AM
 *
 */

package org.naurd.media.jsymphonic.title;

//import javazoom.jlgui.player.amp.tag.APEInfo;

import davaguine.jmac.info.APEInfo;
import davaguine.jmac.info.APETag;


/**
 *
 * @author Pat
 */
public class Ape extends Title{

    /** Creates a new instance of Ape */
    public Ape(java.net.URL url) {
        super(url);
    }
    public Ape(java.io.File f){
        super(f);
        format = Title.APE;
        APEInfo apeInfo;

        try {
            // Read info from header (using Jmac lib)
            apeInfo = new APEInfo(f);
            bitRate = apeInfo.getApeInfoAverageBitrate();
            frequency = apeInfo.getApeInfoSampleRate()/1000.0; // this method returns the frequency in Hz, it should be in kHz
            vbr = true;
            nbChannels = apeInfo.getApeInfoChannels();
            length = (int) apeInfo.getApeInfoLengthMs();
        }
        catch (Exception ex) {
            logger.warning("Exception while reading APE info for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
        }

        if(tryReadTagInfo){
            try {
                // Read the tag if set so in the config file
                APETag apeTag = new APEInfo(f).getApeInfoTag();

                // Temporary string and int
                String titleApe = "", albumApe = "", artistApe = "", genreApe = "";
                int trackApe = 0, yearApe = 0;

                // Determine the version of the tag (ID3 or APE)
                if(apeTag.GetHasID3Tag()){
                    logger.warning("ID3 tag in APE files is not yet supported and won't be if anyone show interest. If you would like JSymphonic to support ID3 tag in APE files, please, contact the the developpers.");
                }
                if(apeTag.GetHasAPETag()){
                    // If an APE tag is present, read the fields
                    // Title
                    if(apeTag.GetTagField(APETag.APE_TAG_FIELD_TITLE).GetIsUTF8Text()) {
                        // If field is UTF8, set the String type
                        titleApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_TITLE).GetFieldValue(), "UTF8");
                    }
                    else {
                        // Else, pray God
                        titleApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_TITLE).GetFieldValue());
                    }
                    // Artist
                    if(apeTag.GetTagField(APETag.APE_TAG_FIELD_ARTIST).GetIsUTF8Text()) {
                        // If field is UTF8, set the String type
                        artistApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_ARTIST).GetFieldValue(), "UTF8");
                    }
                    else {
                        // Else, pray God
                        artistApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_ARTIST).GetFieldValue());
                    }
                    // Album
                    if(apeTag.GetTagField(APETag.APE_TAG_FIELD_ALBUM).GetIsUTF8Text()) {
                        // If field is UTF8, set the String type
                        albumApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_ALBUM).GetFieldValue(), "UTF8");
                    }
                    else {
                        // Else, pray God
                        albumApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_ALBUM).GetFieldValue());
                    }
                    // Genre
                    if(apeTag.GetTagField(APETag.APE_TAG_FIELD_GENRE).GetIsUTF8Text()) {
                        // If field is UTF8, set the String type
                        genreApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_GENRE).GetFieldValue(), "UTF8");
                    }
                    else {
                        // Else, pray God
                        genreApe = new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_GENRE).GetFieldValue());
                    }
                    // Track
                    if(apeTag.GetTagField(APETag.APE_TAG_FIELD_TRACK).GetIsUTF8Text()) {
                        // If field is UTF8, set the String type
                        trackApe = Integer.parseInt(new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_TRACK).GetFieldValue(), "UTF8"));
                    }
                    else {
                        // Else, pray God
                        trackApe = Integer.parseInt(new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_TRACK).GetFieldValue()));
                    }
                    // Year
                    if(apeTag.GetTagField(APETag.APE_TAG_FIELD_YEAR).GetIsUTF8Text()) {
                        // If field is UTF8, set the String type
                        yearApe = Integer.parseInt(new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_YEAR).GetFieldValue(), "UTF8"));
                    }
                    else {
                        // Else, pray God
                        yearApe = Integer.parseInt(new String(apeTag.GetTagField(APETag.APE_TAG_FIELD_YEAR).GetFieldValue()));
                    }
                }

                // Then, set the field is they are correct
                if(titleApe.length() > 0){ titleName = titleApe; } // Set field is it has been correctly read
                if(artistApe.length() > 0){ artistName = artistApe; } // Set field is it has been correctly read
                if(albumApe.length() > 0){ albumName = albumApe; } // Set field is it has been correctly read
                if(genreApe.length() > 0){ genre = genreApe; } // Set field is it has been correctly read
                if(trackApe > 0){ titleNumber = trackApe;} // Set field is it has been correctly read
                if(yearApe > 0){ year = yearApe;} // Set field is it has been correctly read
            }
            catch (Exception ex) {
                logger.warning("Exception while reading APE tag for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
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
        return "ape";
    }
    
/* GET METHODS */
    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        return "Monkey's Audio (APE)";
    }
}
