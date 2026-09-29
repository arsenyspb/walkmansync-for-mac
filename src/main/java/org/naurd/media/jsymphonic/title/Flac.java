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
 * Flac.java
 *
 * Created on 21 mars 2008, 18:20
 *
 */

package org.naurd.media.jsymphonic.title;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;
import org.naurd.media.jsymphonic.toolBox.DataBaseOmgaudioToolBox;

/**
 *
 * @author skiron
 */
public class Flac extends Title{
    
    /** Creates a new instance of Flac */
    public Flac(java.net.URL url) {
        super(url);
    }
    
    public Flac(java.io.File f){
        super(f);
        format = Title.FLAC;

        // Read info from header (using JAudioTagger lib)
        try {
            AudioHeader FLACinfo = AudioFileIO.read(f).getAudioHeader();
            bitRate = (int) FLACinfo.getBitRateAsNumber();
            frequency = FLACinfo.getSampleRateAsNumber()/1000.0; // this method returns the frequency in Hz, it should be in kHz
            vbr = FLACinfo.isVariableBitRate();
            nbChannels = Integer.parseInt(FLACinfo.getChannels());
            length = FLACinfo.getTrackLength()*1000; // this method returns the tracklength in second, it should be in millisecond
        }
        catch (Exception ex) {
            logger.warning("Exception while reading FLAC info for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
        }

        // Read the Vorbis tag if set so in the config file
        if(tryReadTagInfo){
            try {
                Tag FLACtag = AudioFileIO.read(f).getTag();
                if(FLACtag.getFirstTitle().length() > 0){ titleName = FLACtag.getFirstTitle();} // Read field is it exist
                if(FLACtag.getFirstArtist().length() > 0){ artistName = FLACtag.getFirstArtist();} // Read field is it exist
                if(FLACtag.getFirstAlbum().length() > 0){ albumName = FLACtag.getFirstAlbum();} // Read field is it exist
                if(FLACtag.getFirstGenre().length() > 0){ genre = FLACtag.getFirstGenre();} // Read field is it exist
                if(FLACtag.getFirstTrack().length() > 0){ titleNumber = Integer.parseInt(FLACtag.getFirstTrack());} // Read field is it exist
                if(FLACtag.getFirstYear().length() > 0){ year = Integer.parseInt(FLACtag.getFirstYear());} // Read field is it exist
            }
            catch (Exception ex) {
                logger.warning("Exception while reading FLAC tag for file: '" + f.getPath()+ "'. Exception: "+ex.getMessage());
            }
        }
    }

/* GET METHODS */

    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        return "Flac";
    }
    
    /**
     * This method search an ID3 tag into the FLAC file and return the offset of the first "flac" bytes (the flac file itself, including meta-data, not the first byte of music)
     *
     * @return 0 if no ID3 tag is found or the offset to reach the flac file if an ID3 tag is present.
     */
    public int getID3Length() {
        java.io.InputStream flacStream = null; // The stream used to read the data
        byte[] buffer1 = new byte[1]; //Buffer (1 byte long)
        byte[] buffer3 = new byte[3]; //Buffer (3 bytes long)
        byte[] buffer4 = new byte[4]; //Buffer (4 bytes long)
        byte[] buffer16 = new byte[16]; //Buffer (16 bytes long)
        int offset = 0; // The offset before the beginning of the flac file
        int readbytes;

        // First, try to determinate if the file contains an ID3 tag, if so it is at the beginning of the file
        try{
            // Open the stream
            flacStream = this.getSourceFile().toURI().toURL().openStream();

            // Read the First 3 bytes from the file
            flacStream.read(buffer3); //Read the first part of the size
            offset += 3;
            String bufferString = new String(buffer3, 0, 3); //Convert TagId to string

            // If the first bytes are ID3, there is an ID3 tag
            if(bufferString.compareTo("ID3") == 0){
                // Determinate the length of the tag ID3
                // To do so, first, search for the end of the ID3 tag, the end is reach when 16 consecutive zeros are read
                int count = 0;
                flacStream.read(buffer16);
                offset += 16;
                while(!DataBaseOmgaudioToolBox.isZero(buffer16)) {
                    readbytes = flacStream.read(buffer16);
                    while(readbytes != 16){
                        // If the 16 bytes are not read in a row, read the missing bytes
                        byte[] tmp = new byte[16-readbytes]; // create a temporary bytes array to read missing bytes
                         int readbytestmp = flacStream.read(tmp); // read missing bytes
                        // And copy them to the buffer16
                        for(int i = 0; i < readbytestmp; i++){
                            buffer16[(16-tmp.length) + i] = tmp[i];
                        }
                        readbytes += readbytestmp;
                    }
                    count += 16;
                    offset += 16;

                    if(count >= this.getSourceFile().length()){
                        throw new Exception("End of ID3 tag cannot be found in file"+this.getSourceFile().getPath());
                    }
                }

                // Once done, search for the beginning of the flac file (with the "fLaC" string)
                flacStream.read(buffer4);
                offset += 4;
                bufferString = new String(buffer4, 0, 4); //Convert buffer to string
                while(bufferString.compareTo("fLaC") != 0) {
                    count += flacStream.read(buffer1); // Read one bytes
                    offset += 1;
                    // Slip the bytes in the buffer4 and add the last read byte at the end of the buffer4 variable
                    buffer4[0] = buffer4[1];
                    buffer4[1] = buffer4[2];
                    buffer4[2] = buffer4[3];
                    buffer4[3] = buffer1[0];
                    bufferString = new String(buffer4, 0, 4); //Convert buffer to string

                    if(count >= this.getSourceFile().length()){
                        throw new Exception("Start of Flac file cannot be found in file"+this.getSourceFile().getPath());
                    }
                }

                // Once done, the size of the ID3 tag is given by the computed offset - 4 (for the fLaC string)
                return offset-4;
            }
            else{
                // Else, no ID3 tag, return 0
                return 0;
            }
        }
        catch(Exception ex){
            logger.severe("An error occured while determining the size of the ID3 tag in the Flac file: "+this.getSourceFile().getPath()+". The exception is: "+ex.getMessage());
            return 0;
        }
    }

/* STATIC METHODS */

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "fla,flac";
    }
}
