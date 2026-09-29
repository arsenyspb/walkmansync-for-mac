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
 * Oma.java
 *
 * Created on October 2, 2006, 10:23 PM
 *
 */

package org.naurd.media.jsymphonic.title;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.naurd.media.jsymphonic.toolBox.DataBaseOmgaudioToolBox;


/**
 *
 * @author Pat
 */
public class Oma  extends Title{
/* FIELDS */
    // Title information
    protected int formatWrapped = Title.AUDIO; // Precise the type of file wrapped into the OMA file
    protected byte mpegVersion = 3; // Mpeg version of the file (only used when wrapping MP3)
    protected byte layer = 1; // Layer of the file (only used when wrapping MP3)
    protected long framesNumber; // Number of frames in the MP3 file (only used when wrapping MP3)
    protected byte[] omaInfo = new byte[3]; // oma info (bitrate in a 3 bytes array) (only used when wrapping OMA)
    protected int version = Oma.ATRAC; // Version of the OMA file (only used when wrapping OMA)
    protected int drm = Oma.NO_DRM; // tell if the file holds DRM (only used when wrapping OMA)
    protected byte[] drmKey = new byte[0x30]; // the key corresponding to the DRM  (only used when wrapping DRMed OMA)

/* CONSTANT */
    // Versions constant
    public static final int ATRAC3 = 0; // ATRAC 3+
    public static final int ATRAC = 1; // ATRAC
    public static final int ATRAC_AL = 3; // ATRAC Advanced Lossless
    public static final int NO_DRM = 0; // File has no DRM
    public static final int DRM_LSI = 1; // File has a DRM, version of the DRM is LSI
    public static final int DRM_ULINF = 2; // File has a DRM, version of the DRM is ULINF



/* CONSTRUCTORS */
    /**
     * Create an instance from a string giving the path of the oma file.
     *
     * @param omaPath The path of the mp3 file.
     */
    public Oma(String omaPath){
        this(new File(omaPath));
    }

    /**
     * Create an instance from a file.
     *
     * @param f The file to create the instance from.
     */
    public Oma(File f){
        // User the super constructor
        super(f);

        // Set files info
        format = Title.OMA;
        fileSize = f.length();

        // Other info are read in the EA3tag
        readEA3TagInfo();
    }

    /**
     * Create an instance from a file and an existing title (used when importing files to device, to wrap an existing title to a new Oma object).
     *
     * @param f The file to create the instance from (will only be used as a source).
     * @param title The title to create the instance from (all data will be read from that title).
     */
    public Oma(File file, Title title){
        // Set files info
        sourceFile = file;
        format = Title.OMA;
        fileSize = 0;

        // Other info are read from the title
        formatWrapped = title.getFormat();
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
        nbOfBytesBeforeMusic = title.getNbOfBytesBeforeMusic();
        nbChannels = title.getNbChannels();

        if(title instanceof Mp3){
            // If wrapped title is an MP3, we should read the layer and the mpegVersion
            Mp3 mp3 = (Mp3) title;
            mpegVersion = mp3.getMpegVersion();
            layer = mp3.getLayer();
            framesNumber = mp3.getFramesNumber();
        }

        if(title instanceof Oma){
            // If title is an OMA, we should read the OMA info (note that we don't know what is the wrapped format, it could be a MP3 wrapped file directly taken from a OMGAUDIO folder)
            Oma oma = (Oma) title;
            formatWrapped = oma.getFormatWrapped();
            omaInfo = oma.getOmaInfo();
            version = oma.getVersion();
            mpegVersion = oma.getMpegVersion();
            layer = oma.getLayer();
            framesNumber = oma.getFramesNumber();
            drm = oma.getDrm();
            drmKey = oma.getDrmKey();
        }
    }

    /**
     * Create an instance from a title. This is usefull when transfering files from computer to the device since all file should be wrapped to OMA files.
     *
     * @param title The title to create the instance from.
     */
    public Oma(Title title) {
        this(title.getSourceFile(), title);
    }

    /**
     * Create an instance from a Oma title. This is usefull when transfering files from computer to the device since all file should be wrapped to OMA files.
     *
     * @param title The title to create the instance from.
     */
    public Oma(Oma title) {
        sourceFile = null; // This instance is a temporary fake, music data shouldn't be read from this instance
        format = Title.OMA;
        formatWrapped = Title.OMA;
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
        nbOfBytesBeforeMusic = title.getNbOfBytesBeforeMusic();
        nbChannels = title.getNbChannels();
        omaInfo = title.getOmaInfo();
        version = title.getVersion();
        drm = title.getDrm();
        drmKey = title.getDrmKey();
    }

    /**
     * Create an instance from a Mp3 title. This is usefull when transfering files from computer to the device since all file should be wrapped to OMA files.
     *
     * @param title The title to create the instance from.
     */
    public Oma(Mp3 title) {
        sourceFile = null; // This instance is a temporary fake, music data shouldn't be read from this instance
        format = Title.OMA;
        formatWrapped = Title.MP3;
        titleName= title.getTitle();
        artistName = title.getArtist();
        albumName = title.getAlbum();
        genre = title.getGenre();
        titleNumber = title.getTitleNumber();
        year = title.getYear();
        length = title.getLength();
        vbr = title.getVbr();
        bitRate = title.getBitRate();
        mpegVersion = title.getMpegVersion();
        layer = title.getLayer();
        framesNumber = title.getFramesNumber();
        frequency = title.getFrequency();
        status = title.getStatus();
        nbChannels = title.getNbChannels();
        nbOfBytesBeforeMusic = title.getNbOfBytesBeforeMusic();
    }

    /**
     * Create an instance from a Wma title. This is usefull when transfering files from computer to the device since all file should be wrapped to OMA files.
     *
     * @param title The title to create the instance from.
     */
    public Oma(Wma title) {
        sourceFile = null; // This instance is a temporary fake, music data shouldn't be read from this instance
        format = Title.OMA;
        formatWrapped = Title.MP3;
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
        // layer, mpegVersion and framesNumber are useless here since the Wrapped format is not MP3
    }

    /**
     * Create an instance from a Aac title. This is usefull when transfering files from computer to the device since all file should be wrapped to OMA files.
     *
     * @param title The title to create the instance from.
     */
    public Oma(Aac title) {
        sourceFile = null; // This instance is a temporary fake, music data shouldn't be read from this instance
        format = Title.OMA;
        formatWrapped = Title.MP3;
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
        // layer, mpegVersion and framesNumber are useless here since the Wrapped format is not MP3
    }

/* GET METHODS */
    /**
     *Obtains the number of bytes before music.
     *
     *@return the number of bytes to skip to get the music.
     */
    @Override
    public int getNbOfBytesBeforeMusic() {
        if(drm == DRM_LSI){
            // OMA files with LSI DRM are entirely copied, nothing is skipped.
            return 0;
        }
        else{
            return super.getNbOfBytesBeforeMusic();
        }
    }

    /**
     * Obtains the format of the file wrapped into the Oma file.
     *
     * @return the format.
     */
    public int getFormatWrapped() {
        return formatWrapped;
    }

    /**
     * Obtains the mpegversion of the title (make only a sense when an mp3 is wrapped).
     * @return The mpeg version
     */
    public byte getMpegVersion(){
        return mpegVersion;
    }

    /**
     * Obtains the layer of the title (make only a sense when an mp3 is wrapped).
     * @return The layer.
     */
    public byte getLayer(){
        return layer;
    }

    /**
     * Obtains the number of frames of the title (make only a sense when an mp3 is wrapped).
     * @return The number of frames.
     */
    public long getFramesNumber(){
        return framesNumber;
    }

    /**
     * Obtains the OMA info (make only a sense when an OMA is wrapped).
     * @return The OMA info.
     */
    public byte[] getOmaInfo(){
        return omaInfo;
    }

    /**
     * Obtains the DRM info (make only a sense when an OMA is wrapped).
     * @return The DRM info. Result can be compared with Oma.NO_DRM to know if the file has DRM.
     */
    public int getDrm(){
        return drm;
    }

    /**
     * Obtains the DRM key bits sequence (make only a sense when an OMA is wrapped with LSI DRM).
     * @return The sequence of bits corresponding to the LSI DRM.
     */
    public byte[] getDrmKey(){
        return drmKey;
    }

    /**
     * Obtains the version of the OMA file (ATRAC or ATRAC3+) (make only a sense when an OMA is wrapped).
     * @return The version.
     */
    public int getVersion(){
        return version;
    }

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "oma";
    }

    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        switch(formatWrapped){
            case Title.WMA:
                return "WMA";
            case Title.MP3:
                return "MP3";
            case Title.AAC:
                return "AAC";
            default:
                return "Atrac";
        }
    }

    public byte[] getFileProperties(){
        byte[] fileProperties = new byte[4]; // value to return
        byte[] buffer1 = new byte[1]; // temp variable
        byte[] buffer2 = new byte[2]; // temp variable

        // Info depends on the format wrapped
        switch(formatWrapped){
            case Title.OMA:
                // First specify the format
                    if(version == Oma.ATRAC3){
                        fileProperties[0] = 0x0; // 0 stands for OMA (atrac3)
                    }
                    else {
                        fileProperties[0] = 0x1; // 1 stands for OMA (atrac)
                    }

                // Next 3 bytes, the bitrate
                fileProperties[1] = omaInfo[0];
                fileProperties[2] = omaInfo[1];
                fileProperties[3] = omaInfo[2];

                break;
            case Title.AAC:
                // First specify the format
                fileProperties[0] = 0x2; // 2 stands for AAC (mp4)

                // Then, compute the bitrate (which is added to 0x5000, I don't really know why...)
                buffer2 = DataBaseOmgaudioToolBox.int2bytes(bitRate + 0x5000, 2);
                fileProperties[1] = buffer2[0];
                fileProperties[2] = buffer2[1];

                // Then, the number of channels and the sampling rate
                // Compute the index code corresponding to the sampling rate
                int indexFrequency;
                switch((int)(frequency*1000)){
                    case 8000:
                        indexFrequency = 0xB;
                        break;
                    case 11025:
                        indexFrequency = 0xA;
                        break;
                    case 12000:
                        indexFrequency = 0x9;
                        break;
                    case 16000:
                        indexFrequency = 0x8;
                        break;
                    case 22050:
                        indexFrequency = 0x7;
                        break;
                    case 24000:
                        indexFrequency = 0x6;
                        break;
                    case 32000:
                        indexFrequency = 0x5;
                        break;
                    case 44100:
                        indexFrequency = 0x4;
                        break;
                    case 48000:
                        indexFrequency = 0x3;
                        break;
                    case 64000:
                        indexFrequency = 0x2;
                        break;
                    case 88200:
                        indexFrequency = 0x1;
                        break;
                    case 96000:
                        indexFrequency = 0x0;
                        break;
                    default:
                        indexFrequency = 0x4;
                }
                // Compute nb of channels and sampling rate
                buffer1 = DataBaseOmgaudioToolBox.int2bytes(nbChannels*0x10 + indexFrequency, 1);
                fileProperties[3] = buffer1[0];

                break;
            case Title.MP3:
                // First specify the format
                fileProperties[0] = 0x3; // 3 stands for MP3

                // Next byte tells if the title is VBR
                if(vbr) {
                    buffer1 = DataBaseOmgaudioToolBox.int2bytes(144, 1); // 144 = 0x90 = vbr
                }
                else {
                    buffer1 = DataBaseOmgaudioToolBox.int2bytes(128, 1); // 128 = 0x80 = cbr
                }
                fileProperties[1] = buffer1[0];

                // Next byte tells the bitrate, layer, mpeg version
                int indexBitrate;
                try{
                    indexBitrate = DataBaseOmgaudioToolBox.humanBitRateToIndex(bitRate, layer, mpegVersion);
                }
                catch(Exception e){
                    // If any error occured while getting the index bitrate, assume a default one (128kbps)
                    indexBitrate = 9;
                }
                buffer1 = DataBaseOmgaudioToolBox.int2bytes(indexBitrate + layer*16 + mpegVersion*64, 1); // mpeg version(2bits), layer version(2bits), bitrate(4bits)
                fileProperties[2] = buffer1[0];

                // Then get the number of channels: 0x10 for Stereo or Joint Stereo, 0x20 for Dual Channels and 0x30 for Mono
                if(nbChannels == 2) {
                    buffer1 = DataBaseOmgaudioToolBox.int2bytes(0x10, 1);
                }
                else {
                    buffer1 = DataBaseOmgaudioToolBox.int2bytes(0x30, 1);
                }
                fileProperties[3] = buffer1[0];

                break;
             case Title.WMA:
                // First specify the format
                fileProperties[0] = 0x5; // 5 stands for WMA

                // Get the bitrate, using 2 bytes
                buffer2 = DataBaseOmgaudioToolBox.int2bytes(0x4000 + bitRate, 2);
                fileProperties[1] = buffer2[0];
                fileProperties[2] = buffer2[1];

                // VBR flag
                if(vbr){
                    buffer1 = DataBaseOmgaudioToolBox.int2bytes(0x45, 1);
                }
                else {
                    buffer1 = DataBaseOmgaudioToolBox.int2bytes(0x44, 1);
                }
                fileProperties[3] = buffer1[0];

                break;
        }
        return fileProperties;
    }

/* METHODS */
    /**
     * Returns a title of the correct instance according to the format of the title wrapped in the current instance.
     *
     * @param file The new source file for the unwrapped file
     * @return A title in a correct instance according to the format of the title wrapped in the current instance.
     */
    public Title unwrapTitle(java.io.File file){
        switch(formatWrapped){
            case Title.OMA:
                return new Oma(file, this);
            case Title.MP3:
                return new Mp3(file, this);
            case Title.WMA:
                return new Wma(file, this);
            case Title.AAC:
                return new Aac(file, this);
            default:
                return this;
        }
    }

    /**
     * Convert the omaInfo (3 bytes from OMA file) to human readable bitrate
     *
     * @param omaInfo The 3 bytes omaInfo
     * @return The bitrate
     */
    private int omaInfoToBitRate(byte[] omaInfo) {
        /*         AL = Advanced Lossless
               48kbps:   0x 00 28 22
               64kbps:   0x 00 28 2E
             AL64kbps:   0x 00 28 2E
               96kbps:   0x 00 28 45
              128kbps:   0x 00 28 5C
            AL128kbps:   0x 00 28 5C
              132kbps:   0x 00 20 30
            AL132kbps:   0x 00 20 30
              160kbps:   0x 00 28 74
              192kbps:   0x 00 28 8B
              256kbps:   0x 00 28 B9
            AL256kbps:   0x 00 28 B9
              320kbps:   0x 00 28 E8
              352kbps:   0x 00 28 FF
            AL352kbps:   0x 00 28 FF
         */
        if(omaInfo[1] == 0x20){
            // ATRAC 3+
            return 132;
        }
        else{
            // ATRAC 3
            int bitrateIndex = omaInfo[2];
            if(bitrateIndex < 0) {bitrateIndex+= 256;} // Convert it to a positive number !
            switch(bitrateIndex){
                case 0x22:
                    return 48;
                case 0x2E:
                    return 64;
                case 0x45:
                    return 96;
                case 0x5C:
                    return 128;
                case 0x74:
                    return 160;
                case 0x8B:
                    return 192;
                case 0xB9:
                    return 256;
                case 0xE8:
                    return 320;
                case 0xFF:
                    return 352;
                default:
                    // Unknown bitrate
                    return 0;
            }
        }
    }

   /**
     * Try to read the tag info from the EA3 tag
     */
    private void readEA3TagInfo() {
        java.io.InputStream omaFileStream = null; // The stream used to read the data
        byte[] tagId = new byte[4]; //Used to read the name of the tag (like 'TIT2' or 'TLEN')
        byte[] tagLength = new byte[4]; //Used to read the number of bytes in the tag
        byte[] tagValue; //Used to read the value of the tag (length is defined later)
        byte[] buffer1 = new byte[1]; //Buffer (1 byte long)
        byte[] buffer2 = new byte[2]; //Buffer (2 bytes long)
        byte[] buffer4 = new byte[4]; //Buffer (4 bytes long)
        String stringTagValue;
        String stringTagId;
        String logInfo; // A string to save all the info when reading a title
        int intTagLength;
        int intTagValue;
        int infiniteLoop = 0; // This variable is used to detect and stop infinite loop, in case of...
        int sizePart1, sizePart2, size = 0; // Size of the tag ea3

        logInfo = "Scanning file "+sourceFile.getName();

        if(sourceFile.length() <= 0) {
            logger.info(logInfo);
            logger.warning("ERROR: File is empty.");
            return;
        }

        try{
            //Opens the file in stream mode (must be in try-catch)
            omaFileStream = sourceFile.toURI().toURL().openStream();

            // Read the size of the tag ea3 (first part of the tag, containing the tag info, second part, called EA3 contains file properties)
            omaFileStream.skip(4); //Skips 4 bytes (which are "e", "a", "3", 0x03)
            omaFileStream.read(buffer4); //Read the first part of the size
            sizePart1 = DataBaseOmgaudioToolBox.bytes2int(buffer4); // Convert it to int
            omaFileStream.read(buffer1); //Reads the second part of the size
            sizePart2 = buffer1[0]; // Convert it to int
            if(sizePart2 < 0) {sizePart2+= 256;} // Convert it to a positive number !
            size = 0x4000*sizePart1 + 0x80*sizePart2 + 0x80;
            omaFileStream.skip(1); //Skips one more byte [0x76]

            //Reads the first tagId
            omaFileStream.read(tagId); 

            // Loop to read each frame of the tag
            while(!DataBaseOmgaudioToolBox.isZero(tagId)) {
                stringTagId = new String(tagId, 0, 4); //Convert TagId to string
                logInfo += "; "+stringTagId + ":"; // Log

                omaFileStream.read(tagLength); //Reads the length of the tag
                intTagLength = DataBaseOmgaudioToolBox.bytes2int(tagLength) - 1; //and convert to integer (-1 is for the skip of character encodage 2)

                omaFileStream.skip(3); //Skips 3 bytes (which are [0 0 2])

                if(stringTagId.compareTo("GEOB") == 0){
                    // GEOB could be a cover, a key (for DRM files)... try to figure out what it is
                    // If it's a KEYRING tag, bytes 9 to 26 gives the type of key
                    omaFileStream.skip(9); // skip "binary000"
                    tagValue = new byte[18];
                    omaFileStream.read(tagValue);
                    stringTagValue = new String(tagValue,"UTF16");
                    stringTagValue = stringTagValue.replace(""+(char)0,""); //Remove empty characters from the TagValue (these characters are due to 16-bits encodage in Ea3Tag)

                    if(stringTagValue.contains("OMG_LSI")){
                        // file has a LSI DRM
                        drm = Oma.DRM_LSI;
                        logInfo += " LSI DRM found";

                        // Read the DRM info needed
                        // Skip to the first part of the info
                        long amountToSkip = 314;
                        long reallyskiped;
                        while(amountToSkip > 0) {
                            reallyskiped = omaFileStream.skip(amountToSkip);
                            amountToSkip -= reallyskiped;
                        }
                        // Read the first part
                        byte[] drmKeyPart1 = new byte[0x20];
                        omaFileStream.read(drmKeyPart1);
                        // Skip to second part
                        omaFileStream.skip(16);
                        // Read the second part
                        byte[] drmKeyPart2 = new byte[0x10];
                        omaFileStream.read(drmKeyPart2);
                        // skip to the end of the GEOB tag
                        amountToSkip = 48;
                        while(amountToSkip > 0) {
                            reallyskiped = omaFileStream.skip(amountToSkip);
                            amountToSkip -= reallyskiped;
                        }

                        // Store the drm Key
                        for(int i=0; i < 0x20; i++){
                            drmKey[i] = drmKeyPart1[i];
                        }
                        for(int i=0; i < 0x10; i++){
                            drmKey[i+0x20] = drmKeyPart2[i];
                        }

                        // Read next tag for next iteration
                        omaFileStream.read(tagId); //Reads the next tagId
                        continue;
                    }

                    if(stringTagValue.contains("OMG_ULINF")){
                        // file has a ULINF DRM
                        drm = Oma.DRM_ULINF;
                        logInfo += " ULINF DRM found";
                    }
                    else{
                        logInfo += " skip a GEOB tag";
                    }

                    // Skip the GEOB tag
                    // Note that skip method is not very efficient and reads data it skips, and it may not skip all that was wanted, so it's nessecary to check what was really skiped...
                    long amountToSkip = intTagLength - 9 - 18; // 9 bytes have already been skipped and 18 have been read
                    long reallyskiped;
                    while(amountToSkip > 0) {
                        reallyskiped = omaFileStream.skip(amountToSkip);
                        amountToSkip -= reallyskiped;
                    }

                    // Read next tag for next iteration
                    omaFileStream.read(tagId); //Reads the next tagId
                    continue; // Rest of the loop isn't to do
                }

                if(intTagLength > 3200) { // If the length to be read is wrong, stop the loop
                    logger.info(logInfo);
                    logger.warning("Error of tag length while scanning "+ sourceFile.getName() + ".\nSkip title.");
                    return;
                }

                // If it's not a picture:
                tagValue = new byte[intTagLength];
                omaFileStream.read(tagValue); //Reads the value of the tag
                //這裡有問題，並沒有把讀出的字串用UTF8編碼來處理 Michael
                // The tagValue may have extra "0x0" at the end, because some Sony's software (like Vaio music transfer) uses fix tag length, so extra space is filled with "0x0". But these extra zeros cause problem when the tag is turn to string, so, they are removed with the method "rtrimZeros".
                stringTagValue = new String(DataBaseOmgaudioToolBox.rtrimZeros(tagValue),"UTF16"); // Change it for fix Unicode problem by Michael Chen 2008/3/23
                stringTagValue = stringTagValue.replace(""+(char)0,""); //Remove empty characters from the TagValue (these characters are due to 16-bits encodage in Ea3Tag)
                intTagValue = DataBaseOmgaudioToolBox.charBytes2int(tagValue); //also convert to integer if it is a number

                if(stringTagId.compareTo("TIT2") == 0){
                    titleName = stringTagValue;
                    logInfo += " titleName:" +stringTagValue;
                }
                if(stringTagId.compareTo("TPE1") == 0){
                    artistName = stringTagValue;
                    logInfo += " artistName:" +stringTagValue;
                }
                if(stringTagId.compareTo("TALB") == 0){
                    albumName = stringTagValue;
                    logInfo += " albumName:" +stringTagValue;
                }
                if(stringTagId.compareTo("TCON") == 0){
                    genre = stringTagValue;
                    if(genre.length() == 0){
                        genre = "unknown genre (JSoma)"; // if an empty genre has been read, we put a default value
                    }
                    logInfo += " genre:" +stringTagValue;
                }
                if(stringTagId.compareTo("TXXX") == 0 && stringTagValue.contains("OMG_TRACK")){
                    stringTagValue = stringTagValue.replace("OMG_TRACK","");
                    stringTagValue = stringTagValue.replace(" ","");
                    titleNumber = Integer.parseInt(stringTagValue);
                    logInfo += " trackNumber:" +titleNumber;
                }
                if(stringTagId.compareTo("TXXX") == 0 && stringTagValue.contains("OMG_TRLDA")){
                    stringTagValue = stringTagValue.replace("OMG_TRLDA","");
                    String[] splitenString = stringTagValue.split("/"); // Date can be on the form "2004/01/01" or "04/01/01"
                    stringTagValue = splitenString[0]; // Only the first value is interesting, the year
                    stringTagValue = stringTagValue.replace(" ","");
                    if(stringTagValue.length() == 2 && Integer.parseInt(stringTagValue) < 50) {
                        // If the year is written with two number less than 50 (eg 07) wue suppose that the rigth date is 2007
                        stringTagValue = "20" + stringTagValue;
                    }
                    if(stringTagValue.length() == 2 && Integer.parseInt(stringTagValue) >= 50) {
                        // If the year is written with two number greater than 50 (eg 94) wue suppose that the rigth date is 1994
                        stringTagValue = "19" + stringTagValue;
                    }
                    year = Integer.parseInt(stringTagValue);
                    logInfo += " year:" +year;
                }
                if(stringTagId.compareTo("TYER") == 0){
                    year = intTagValue;
                    logInfo += " year:" +year;
                }
                if(stringTagId.compareTo("TLEN") == 0){
                    length = intTagValue;
                    logInfo += " length:" +length;
                }
                omaFileStream.read(tagId); //Reads the next tagId

                // Before next iteration, check if we are in an infinite loop
                infiniteLoop++;
                if(infiniteLoop > 1000) {
                    // an infinite loop has been detected, log info and stop the loop
                    logger.info(logInfo);
                    logger.severe("An infinite loop has been detected and stop while reading EA3 tag from file " + sourceFile.getName() + ".\nPlease contact the developers if you are reading this error.");
                    return;
                }
            }
            // Log info:
            logger.info(logInfo);
        } catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try {
                // always close the stream
                omaFileStream.close();
            } catch (IOException ex) {}
        }

        // Now read the file properties (the EA3 part)
        try{
            // Re-open the file
            omaFileStream = sourceFile.toURI().toURL().openStream();
            // Skip the first part
            long bytesToskip = size + 32; // amont of bytes to be skipped = the ea3 tag plus 32 useless bytes
            long bytesSkipped = 0; // count the amount of bytes really skipped
            // call skip method until the correct number of bytes have been skipped
            while((bytesSkipped = omaFileStream.skip(bytesToskip)) > 0) { // This turns out to be necessary since the skip method is not able to skip the correct amount of bytes once when tag holds a cover
                bytesToskip = bytesToskip - bytesSkipped; // substract bytes correctly skipped
            }

            // Read the type of wrapped format
            omaFileStream.read(buffer1);
            int formatCode = DataBaseOmgaudioToolBox.bytes2int(buffer1);
            switch(formatCode){
                case 0x0:
                case 0x1:
                    formatWrapped = Title.OMA;

                    // OMA file could be encoded with ATRAC or ATRAC 3, it depends on the formatCode
                    if(formatCode == 0x0){
                        version = Oma.ATRAC3;
                    }
                    else {
                        version = Oma.ATRAC;
                    }

                    // Next 3 bytes give the bitrate
                    omaFileStream.read(omaInfo);
                    bitRate = omaInfoToBitRate(omaInfo);

                    // I don't really know if ATRAC could be anything else than CBR, stereo at 44.1kHz... and since I don't know how to get these info from the files themselves, I always assume so:
                    vbr = false;
                    nbChannels = 2;
                    frequency = 44.1;

                    break;
                case 0x2:
                    formatWrapped = Title.AAC;

                    // Read bitrate
                    omaFileStream.read(buffer2);
                    bitRate = DataBaseOmgaudioToolBox.bytes2int(buffer2)-0x5000;

                    // Read number of channels and frequency
                    omaFileStream.read(buffer1); // buffer1 is on the form 0x24 : "2" means 2 channels and 4 is a code giving the frequecy
                    nbChannels = DataBaseOmgaudioToolBox.bytes2int(buffer1)/0x10;
                    int indexFrequency = DataBaseOmgaudioToolBox.bytes2int(buffer1)%0x10;
                    // Get sampling rate in human readable form (44.1kHz instead of code 0x04)
                    switch(indexFrequency){
                        case 0xB:
                            frequency = 8;
                            break;
                        case 0xA:
                            frequency = 11.025;
                            break;
                        case 0x9:
                            frequency = 12;
                            break;
                        case 0x8:
                            frequency = 16;
                            break;
                        case 0x7:
                            frequency = 22.05;
                            break;
                        case 0x6:
                            frequency = 24;
                            break;
                        case 0x5:
                            frequency = 32;
                            break;
                        case 0x4:
                            frequency = 44.1;
                            break;
                        case 0x3:
                            frequency = 48;
                            break;
                        case 0x2:
                            frequency = 64;
                            break;
                        case 0x1:
                            frequency = 88.2;
                            break;
                        case 0x0:
                            frequency = 96;
                            break;
                        default:
                            frequency = 44.1;
                    }

                    break;
                case 0x3:
                    formatWrapped = Title.MP3;

                    // Read if the file wrapped is vbr or not
                    omaFileStream.read(buffer1);
                    if(DataBaseOmgaudioToolBox.bytes2int(buffer1) == 0x80){
                        vbr = false;
                    }
                    else{
                        vbr = true;
                    }

                    // Read bitrate, mpeg version and layer
                    omaFileStream.read(buffer1);
                    // Convert it to a number
                    int info = DataBaseOmgaudioToolBox.bytes2int(buffer1);
                    // mpeg version is given by the 2 "heavy" bits
                    mpegVersion = (byte) (info/64);
                    // layer is given by the next 2 bits
                    layer = (byte) ((info - mpegVersion*64)/16);
                    // bitrate is given by the last 4 bits, but it is expressed in "index" form
                    int bitrateIndex = info - mpegVersion*64 - layer*16;
                    // Get bitrate in human form
                    bitRate = DataBaseOmgaudioToolBox.indexBitRateToHuman(bitrateIndex, layer, mpegVersion);

                    // Read number of channel
                    omaFileStream.read(buffer1); // This byte is 0x10 for Stereo or Joint Stereo, 0x20 for Dual Channels and 0x30 for Mono
                    int nbChannelsIndex = DataBaseOmgaudioToolBox.bytes2int(buffer1);
                    if(nbChannelsIndex == 0x10 || nbChannelsIndex == 0x20) {
                        nbChannels = 2;
                    }
                    else {
                        nbChannels = 1;
                    }

                    // Then, there is the length (4 bytes) but it is already known, so it is skipped
                    omaFileStream.skip(4);

                    // Then, the number of frames
                    omaFileStream.read(buffer4);
                    framesNumber = DataBaseOmgaudioToolBox.bytes2int(buffer4); // Convert it to a number

                    // Since Walkmans are only able to play MP3 files at 44.1kHz, the frequency is known
                    frequency = 44.1;
                    break;
                case 0x5:
                    formatWrapped = Title.WMA;

                    // Read the bitrate
                    omaFileStream.read(buffer2);
                    bitRate = DataBaseOmgaudioToolBox.bytes2int(buffer2) - 0x4000;

                    // Read VBR flag
                    omaFileStream.read(buffer1);
                    if(DataBaseOmgaudioToolBox.bytes2int(buffer1) == 0x45){
                        vbr = true;
                    }
                    else{
                        vbr = false;
                    }

                    // Then, title length and another unknown field

                    break;
                default:
                    // Either the format is not known, or file is an OMA file encoded with ATRAC Advanced Lossless
                    // To know if it is an ATRAC AL, try to read the format code 15 bytes after
                    omaFileStream.skip(15);
                    omaFileStream.read(buffer1);
                    formatCode = DataBaseOmgaudioToolBox.bytes2int(buffer1);
                    if(formatCode == 0x00 || formatCode == 0x01){
                        formatWrapped = Title.OMA;

                        // OMA file is encoded into ATRAC Advanced Lossless
                        version = Oma.ATRAC_AL;

                        // Next 3 bytes give the bitrate
                        omaFileStream.read(omaInfo);
                        bitRate = omaInfoToBitRate(omaInfo);
                        // I don't really know if ATRAC could be anything else than CBR, stereo at 44.1kHz... and since I don't know how to get these info from the files themselves, I always assume so:
                        vbr = false;
                        nbChannels = 2;
                        frequency = 44.1;
                    }
                    else{
                        // Wrapped file format is unknown
                        formatWrapped = Title.AUDIO;
                        logger.warning("An unknown type of file has been found: "+sourceFile.getPath());
                    }

                    // Close opened stream
                    omaFileStream.close();
            }

            // Finally, search for the total size of the OMA header
            // The rest of the tag is full of zeros, so finding a non-zero bytes should indicates the start of the music. But, in some cases, the EA3 tag is longer and includes some non zero bytes (this case is only for Atrac files, so I shouldn't care). In some MP3 case, in the music part 4 bytes are non-zero, the 4 following ARE zeros, but one the ones after.
            // OK, well, I don't care about special cases, for the moment, I'm just going to do something that works in many cases, let's use a fixed EA3 length:
            nbOfBytesBeforeMusic = size + 0x60;
        }
        catch(Exception e){ }

        // Check validity of track number
        if(titleNumber > 10000){
            // If title number is greater than 10000, it has been guessed from a OMA file names within OMGAUDIO folder (on the form 1000000x.OMA). Re-initialize it to 0
            titleNumber = 0;
        }
    }

    /**
     * Write the EA3Tag in a file stream from the info in the current object.
     *
     * @param stream the stream of the file to write the tag to.
     * @param gotKey True if the player mounted is protected, False otherwise (this arg is only used when importing a file to the device).
     * @throws java.lang.Exception
     */
    @Override public void writeTagInfoToFile(FileOutputStream stream, boolean gotKey) throws Exception{
        // Only write a tag if the OMA file is not wrappin an OMA file with LSI DRM
        if(drm == DRM_LSI){
            // Do nothing
            return;
        }

        int headerlength = 0; // count the amount of bytes writen. Since the length of the tag should be determined, this variable will be used to fill with zero the space left
        byte[] encodageCode = {0,0,2}; // used to announce the encodage of the info writen in the tag
        byte partOneSizeA; // First of the two bytes describing the size of the first part of the EA3 tag
        byte partOneSizeB; // Second of the two bytes describing the size of the first part of the EA3 tag

        //// Add first part of the tag (the one called "ea3" with title info (title name, album name...))
        //Header: tag label (ea3) and size
        byte[] bytes4 = {(byte)0x65,(byte)0x61,(byte)0x33,(byte)0x03}; // "e","a","3",0x3
        stream.write(bytes4); headerlength += 4; // Write label

        // Compute the size of the first part
        // If no cover is writen, SonicStage always uses a given lenght: 0xC00 bytes counted as follows: 0x17*0x80 + 0x80. Here, one byte is enough to code the lenght (0x17) but 5 bytes are reserved for that, see what follows.
        // When a cover is writen, if the cover is large (around 0x8000), the length of the tag may be coded over 2 bytes, as follows 0x0A*0x4000 + 0x0F*0x80 + 0x80 (the two bytes here are 0A 0F)
//NOT IMPLEMENTED YET        if(!COVER){ TODO
        partOneSizeA = (byte)0x0;
        partOneSizeB = (byte)0x17;
//NOT IMPLEMENTED YET        }
//NOT IMPLEMENTED YET        else { // Compute and write the size of the first part of the tag
//NOT IMPLEMENTED YET            partOneSizeA = 0x0;
//NOT IMPLEMENTED YET            partOneSizeB = 0x0;
//NOT IMPLEMENTED YET        }
        byte[] bytes6 = {0,0,0,partOneSizeA,partOneSizeB,(byte)0x76}; // The size of the first part of the tag is "0x17", "0X76" is an unknown constant
        stream.write(bytes6); headerlength += 6;

        //Title name
        String label = "TIT2";
        stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
        if(titleName.length() > 60) {titleName = (String)titleName.subSequence(0,59);} // We can write more than 60 char in a EA3 tag field
        stream.write(DataBaseOmgaudioToolBox.int2bytes(titleName.length()*2 + 1, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
        DataBaseOmgaudioToolBox.WriteString16(stream, titleName); headerlength += titleName.length()*2; // Write the info

        //Artist name
        label = "TPE1";
        stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
        if(artistName.length() > 60) {artistName = (String)artistName.subSequence(0,59);} // We can write more than 60 char in a EA3 tag field
        stream.write(DataBaseOmgaudioToolBox.int2bytes(artistName.length()*2 + 1, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
        DataBaseOmgaudioToolBox.WriteString16(stream, artistName); headerlength += artistName.length()*2; // Write the info

        //Album name
        label = "TALB";
        stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
        if(albumName.length() > 60) {albumName = (String)albumName.subSequence(0,59);} // We can write more than 60 char in a EA3 tag field
        stream.write(DataBaseOmgaudioToolBox.int2bytes(albumName.length()*2 + 1, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
        DataBaseOmgaudioToolBox.WriteString16(stream, albumName); headerlength += albumName.length()*2; // Write the info

        //Genre
        label = "TCON";
        stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
        if(genre.length() > 60) {genre = (String)genre.subSequence(0,59);} // We can write more than 60 char in a EA3 tag field
        stream.write(DataBaseOmgaudioToolBox.int2bytes(genre.length()*2 + 1, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
        DataBaseOmgaudioToolBox.WriteString16(stream, genre); headerlength += genre.length()*2; // Write the info

        //Track number
        label = "TXXX";
        stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
        titleNumber = titleNumber % 100; // Track number is between 1 and 99
        if(titleNumber < 10) { // need to know the number of digit
            stream.write(DataBaseOmgaudioToolBox.int2bytes(23, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        }
        else {
            stream.write(DataBaseOmgaudioToolBox.int2bytes(25, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        }
        stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
        DataBaseOmgaudioToolBox.WriteString16(stream, "OMG_TRACK"); headerlength += 8*2; // Write a OMG text to wrapp track number
        DataBaseOmgaudioToolBox.WriteZeros(stream, 2); headerlength += 2; // Two zeros between the text and the actual number
        DataBaseOmgaudioToolBox.WriteString16(stream, Integer.toString(titleNumber)); if(titleNumber < 10) {headerlength += 2*2;}else{headerlength += 3*2;} // Write the info

        //Year
        label = "TYER";
        stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
        stream.write(DataBaseOmgaudioToolBox.int2bytes(4*2 + 1, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
        year = year%10000; // year should be less than 10000
        if(year < 1000) { // Check date validity
            DataBaseOmgaudioToolBox.WriteString16(stream, "0000"); headerlength += 4*2; // Write the info
        }
        else {
            DataBaseOmgaudioToolBox.WriteString16(stream, Integer.toString(year)); headerlength += 4*2; // Write the info
        }

        //Year again (there are several way to put the year... This is maybe the date where the title was transfered...)
        label = "TXXX";
        stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
        stream.write(DataBaseOmgaudioToolBox.int2bytes(59, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
        stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
        DataBaseOmgaudioToolBox.WriteString16(stream, "OMG_TRLDA "); headerlength += 10*2; // Write a OMG text to wrapp year
        if(year < 1000) {
            DataBaseOmgaudioToolBox.WriteString16(stream, "0000"); headerlength += 4*2; // Write the info
        }
        else {
            DataBaseOmgaudioToolBox.WriteString16(stream, Integer.toString(year)); headerlength += 4*2; // Write the info
        }
        DataBaseOmgaudioToolBox.WriteString16(stream, "/01/01 00:00:00"); headerlength += 15*2; // Write the whole date and time...

        // Track length (only writen if valid)
        if(length > 0) {
            label = "TLEN";
            stream.write(label.getBytes()); headerlength += 4; // Write the label to announce the upcoming info
            String lengthString = Long.toString(length); // Get the length in a string
            stream.write(DataBaseOmgaudioToolBox.int2bytes(lengthString.length()*2 + 1, 4)); headerlength += 4; // Write the length to announce the size of the upcoming info
            stream.write(encodageCode); headerlength += 3; // Write the encodage code for the size of the upcoming info
            DataBaseOmgaudioToolBox.WriteString16(stream, lengthString); headerlength += lengthString.length()*2; // Write the info
        }

        //Fill in with zeros the first part of the tag (ea3) to reach the length we had writen at the beginning of the tag
        DataBaseOmgaudioToolBox.WriteZeros(stream, partOneSizeA*0x4000 + partOneSizeB*0x80 + 0x80 - headerlength);


        //// Add second part of the tag (the one called "EA3" with title properties)
        // 1st line and 2nd line (a line is 0x10 bytes)
        label = "EA3";
        stream.write(label.getBytes()); // Write label
        byte[] bytes3 = new byte[]{(byte)0x02,(byte)0x00, (byte)0x60};
        stream.write(bytes3); // Write a constant
        // Then, write file protection
        byte[] bytes2;
        if(gotKey) { // Protection is 0x0001 when file is OMA with LSI DRM, 0xFFFE when file is encrypt MP3 and 0xFFFF in any other cases
            bytes2 = new byte[]{(byte)0xFF,(byte)0xFE};
        }
        else {
            bytes2 = new byte[]{(byte)0xFF,(byte)0xFF};
        }
        stream.write(bytes2); // Write protection

        // Next 24 bytes are only used when file has LSI DRM, not the case here, write zeros
        DataBaseOmgaudioToolBox.WriteZeros(stream, 24);

        // 3rd line contains properties of the file
        // Get the file common properties (format, bitrate and sampling rate) (4 bytes long)
        stream.write(getFileProperties());
        // Then, other info depend on the format wrapped
        switch(formatWrapped){
            case Title.OMA:
                // 12 zeros to finish the 3rd line
                DataBaseOmgaudioToolBox.WriteZeros(stream, 12);

                break;
            case Title.AAC:
                // Write the track length in milliseconds
                stream.write(DataBaseOmgaudioToolBox.int2bytes(length, 4));

                // Finally write a misterious number related to the track length and the sampling rate
                stream.write(DataBaseOmgaudioToolBox.int2bytes((int)(frequency*1000*33120/length), 4)); // *1000 is to have the frequency in Hz, 33120 is a magic number to have a correct result

                // 4 zeros to finish the 3rd line
                DataBaseOmgaudioToolBox.WriteZeros(stream, 4);

                break;
            case Title.MP3:
                // Then write the track length
                stream.write(DataBaseOmgaudioToolBox.int2bytes(length, 4));

                // And finally the number of frames. If the number of frame is not available, it can be computed. But here, we have a lib that already does the job
                if(framesNumber == 0){
                    // Only compute the number of frame is it's zero
                    // To have if need to make some computation
                        // CODE FROM ML_SONY
                        //int  SAMPLING_RATES[] = {11025, 12000, 8000, 0, 0, 0, 22050, 24000, 16000, 44100, 48000, 32000};

                        //sample per frame 0=reserved
                        //          MPG2.5 res        MPG2   MPG1
                        //reserved  0      0          0      0
                        //Layer III 576    0          576    1152
                        //Layer II  1152   0          1152   1152
                        //Layer I   384    0          384    384

                        //int samplingRate = SAMPLING_RATES[(mpegVersion * 3) + samplingRateIndex];

                    int  SAMPLE_PER_FRAME[] = {0,576,1152,384,0,0,0,0,0,576,1152,384,0,1152,1152,384};

                    double samplingRate = frequency;
                    int samplePerFrame = SAMPLE_PER_FRAME[(mpegVersion * 4) + layer]; // Compute the sample per frame
                    // Compute the number of frames
                    framesNumber = (int)((length * samplingRate) / samplePerFrame); // frequency is in kHz, don't really know why, but it should be kept as it is to have a correct result
                }
                // Write the number of frames
                stream.write(DataBaseOmgaudioToolBox.int2bytes((int)framesNumber, 4));

                // 4 zeros to finish the 3rd line
                DataBaseOmgaudioToolBox.WriteZeros(stream, 4);

                break;
            case Title.WMA:
                // Write the title length
                stream.write(DataBaseOmgaudioToolBox.int2bytes(length, 4));

                // Then write the a number (well, I don't really know what this bytes stands for... but it seems to be related with the size and the bitrate, the computation given here gives value around the ones computed by SonicStage - around 10% )
                stream.write(DataBaseOmgaudioToolBox.int2bytes((int)(fileSize/(frequency*bitRate)), 4));

                // 4 zeros to finish the 3rd line
                DataBaseOmgaudioToolBox.WriteZeros(stream, 4);

                break;
           default:
                logger.severe("Unsupported format to wrap in an OMA file !!");
                return ;
        }

        //Fill in the second part of the tag with zeros
        DataBaseOmgaudioToolBox.WriteZeros(stream, 3*0x10);
    }
}
