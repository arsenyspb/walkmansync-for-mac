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
 * FFMpegToolBox.java
 *
 * Created on 4 septembre 2006, 19:51
 *
 */

package org.naurd.media.jsymphonic.toolBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import org.naurd.media.jsymphonic.device.sony.nw.NWGeneric;
import org.naurd.media.jsymphonic.device.sony.nw.NWGenericListener;
import org.naurd.media.jsymphonic.title.Flac;
import org.naurd.media.jsymphonic.title.Mp3;
import org.naurd.media.jsymphonic.title.Title;

/**
 *
 * @author pballeux
 */
public class FFMpegToolBox {
/* FIELDS */
    private boolean ffmpegDetected = false;
    private String ffmpegPath = "";
    private java.io.InputStream processin = null;
    private java.io.OutputStream processout = null;
    private java.io.InputStream filein = null;
    private java.io.OutputStream fileout = null;
    private boolean stopMe = false;
    private boolean convertEnded = false;
    private int lastError = NO_ERROR;
    private NWGeneric nwGeneric;

    //Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.toolBox.FFMpegToolBox");

    // Constants
    public static final int NO_ERROR = 0;
    public static final int FFMPEG_NOT_FOUND = -1;
    public static final int DESTINATION_UNWRITEABLE = -2;
    public static final int DESTINATION_FULL = -3;
    public static final int UNSUPPORTED_FORMAT = -4;
    public static final int BROKEN_PIPE = -5;
    public static final int ERROR = -10;

/*    public enum FileFormat {
        MP3,
        WMA,
        OGG,
        MPC,
        WAV
    }

    public enum FileBitrate{
        RATE64,
        RATE128,
        RATE192,
        RATE256,
        RATE320
    }*/

/* CONSTRUCTORS */
    /** Creates a new instance of FFMpegToolBox */
    public FFMpegToolBox() {
        try{
            java.lang.Process p = Runtime.getRuntime().exec("ffmpeg");
            ffmpegDetected = true;
        } catch(Exception e ){
            // ffmpeg has not been found from the environment variable, let's try on the local folders
            String ffmpegExePath = ".\\ffmpeg.exe";
            File ffmpegExe = new File(ffmpegExePath);

            if(!ffmpegExe.exists()) {
                // ffmpeg has not been found in the base folder of the device, let's try in a JSymphonic folder
                ffmpegExePath= ".\\JSymphonic\\ffmpeg.exe";
                ffmpegExe = new File(ffmpegExePath);
            }

            if(!ffmpegExe.exists()) {
                // ffmpeg has not been found in the base folder of the device, let's try in a ffmpeg-win32 folder
                ffmpegExePath= ".\\ffmpeg-win32\\ffmpeg.exe";
                ffmpegExe = new File(ffmpegExePath);
            }

            if(!ffmpegExe.exists()) {
                // ffmpeg has not been found in the base folder of the device, let's try in a JSymphonic/ffmpeg-win32 folder
                ffmpegExePath= ".\\JSymphonic\\ffmpeg-win32\\ffmpeg.exe";
                ffmpegExe = new File(ffmpegExePath);
            }

            if(!ffmpegExe.exists()) {
                // let's try in /opt/local/bin/ffmpeg
                ffmpegExePath= "/opt/local/bin/ffmpeg";
                ffmpegExe = new File(ffmpegExePath);
            }

            if(ffmpegExe.exists()) {
                // FFMPEG has been found, save the path
                ffmpegPath = ffmpegExePath;
                ffmpegDetected = true;
            }
            else {
                // ffmpeg has not been found
                logger.warning("FFMPEG not detected...");
            }
        }

        /* TODO MAC OS DEBUGGING *
        logger.warning("MAC OS DEBUGGING");
        try{
            byte[] bserr;
            java.lang.Process p = Runtime.getRuntime().exec("ls");
            processin = p.getInputStream();
            bserr = new byte[processin.available()]; // check if something is available

            if (bserr.length>0){
                processin.read(bserr); // read standard error
                String out = new String(bserr); // convert to a string
                logger.warning("ls: "+out);
            }
            else{
                logger.warning("ls: no output");
            }
        }
        catch(Exception e){
            logger.warning("ls IS NOT WORKING");

        }
        try{
            byte[] bserr;
            java.lang.Process p = Runtime.getRuntime().exec("ffmpeg -v");
            processin = p.getInputStream();
            bserr = new byte[processin.available()]; // check if something is available

            if (bserr.length>0){
                processin.read(bserr); // read standard error
                String out = new String(bserr); // convert to a string
                logger.warning("Result of ffmpeg -v: "+out);
            }
            else{
                logger.warning("ffmpeg -v: no output");
            }
        }
        catch(Exception e){
            logger.warning("ffmpeg -v IS NOT WORKING EITHER");

        }
        /* END OF MAC OS DEBUGGING */

    }

/* GET METHODS */
    /**
     * Obtains the error for the last execution of FFMPEG.
     *
     * @return the code of the last error.
     */
    public int getLastError() {
        return lastError;
    }
    
/* METHODS */
    /**
     * Stop the execution of FFMPEG.
     */
    public void stopMe(){
        stopMe=true;
    }

    /**
     * Convert a title to MP3 using ffmpeg. This function copies the tag from the source to the destination (the tag will exist in the Title object, not in the MP3 file !!). The source should be compatible with FFMPEG
     * 
     * @param source The source file to be transcoded.
     * @param destination The destination file to be written.
     * @param bitrate The bitrate to use to transcode (destination is always to MP3 44.1kHz with 2 channels).
     * @param nwGeneric instance of NWGeneric needed to inform the GUI of the progress of the encodage
     * @param sourceOffset An amount of bytes to skip from the source (usefull with Flac files with ID3 tag since FFMPEG can't do that itself)
     * @return 0 is all went OK, -1 otherwise.
     */
    public int convertToMp3(Title source, Mp3 destination, int bitrate, NWGeneric nwGeneric) {
        this.nwGeneric = nwGeneric;
        int sourceOffset = 0;

        // If file format is FLAC, one should check that it doesn't contain ID3, if so, the length of the ID3 tag should be used in the "sourceOffset" variable to be skipped, since FFMPEG is not able to handle FLAC file with ID3 tag
        if(source instanceof Flac) {
            sourceOffset = ((Flac) source).getID3Length();
        }
        
        // Use "convertToMp3" method to convert the file
        convertToMp3(source.getSourceFile(), destination.getSourceFile(), bitrate, sourceOffset);

        if(lastError >= 0) {
            // If transcode went fine,
            // Write the tag in the destination object
            destination.copyTagInfo(source);

            return 0;
        }
        else{
            return -1;
        }
    }
    
    /**
     * Convert a file to MP3 using ffmpeg. This function only handle files, the resulting file has no ID3Tag. The source should be compatible with FFMPEG.
     * There is no code returns, once finished, one should consult the method "getLastError()" to know if execution of FFMPEG went well.
     * 
     * @param source The original file to be transcoded.
     * @param destination The MP3 file to be created.
     * @param bitrate The bitrate to use to transcode (destination is always to MP3 44.1kHz with 2 channels).
     * @param sourceOffset An amount of bytes to skip from the source (usefull with Flac files with ID3 tag since FFMPEG can't do that itself)
     */
    public void convertToMp3(final File source, File destination, int bitrate, final int sourceOffset) {
        String format = "mp3"; // format to encode is always MP3 (for video, this should specify the container (avi, ogm,...) )
        String codec = "libmp3lame"; // codec to encode is always MP3
        String frequency = "44100"; // frequency to encode is always 44100Hz
        java.lang.Process p = null; // the process to run ffmpeg in
        java.io.InputStream processerror = null; // the stream recording the error in the process
        String exec = ""; // string used to set the command line to call ffmpeg
        
        // First thing to do is to initialize the last error string
        lastError = NO_ERROR;

        if(!ffmpegDetected){
            // If ffmpeg has not been detected, method can stop now
            logger.warning("FFMPEG not detected...");
            lastError = FFMPEG_NOT_FOUND;
            return;
        }

        if(!destination.getParentFile().canWrite()){
            // Destination folder is not writeable
            lastError = DESTINATION_UNWRITEABLE;
            return;
        }
        
        //// Define the command line to call ffmpeg:
        // -i -: input is the standard input
        // -ac 2: number of channel is 2
        // -ar f: frequency is f
        // -ab b: bitrate is b
        // -f f: format is f
        // - : output is the standard output
        // Note that input and output are taken as standard input and output to to feed by oursefves, so that you know how the encoding is going
        if(ffmpegPath.length() > 0) {
            // A path is needed to invoke the command:
            exec = "\"" + ffmpegPath + "\"";
        }
        else{
            // Just putting the name is enough
            exec = "ffmpeg";
        }

//        exec += " -i - -y -ac 2 -ar " + frequency + " -ab " + bitrate + "k -f " + format + " -acodec "+ codec + " -"; // the "-acodec" is not mandatory since we are precising the output format. Moreover, the version of FFMPEG in Ubuntu 8.10 repository uses "libmp3lame" to identify the MP3 audio codec whereas compiled version given in the sourceforge Symphonic download page uses "mp3"
        exec += " -i - -y -ac 2 -ar " + frequency + " -ab " + bitrate + "k -f " + format + " -";

        logger.info("ffmpeg is call with command:" + exec);

        try{
            // Create a process with the call to ffmpeg
            p = Runtime.getRuntime().exec(exec);

            // Get standard input, output and error
            processin = p.getInputStream();
            processout = p.getOutputStream();
            processerror = p.getErrorStream();

            // Get stream from the input and output files
            filein = new FileInputStream(source);
            fileout = new FileOutputStream(destination);
        }
        catch(Exception ex){
            // Something went wrong during the execution of FFMPEG, try to know what
            reportException(ex);
            // Close opened streams
            try {
                filein.close(); fileout.close(); processerror.close();
            }
            catch(Exception e){
                logger.warning("An error occured while closing the streams used by FFMPEG.");
                e.printStackTrace();
            }
            return;
        }

        // Compute the total size of the file
        final long totalSize = source.length();

        // Create a new thread, this thread will feed in ffmpeg with the source file to encode
        Thread read = new Thread(){
            @Override
            public void run() {
                int countin = 0; // number of bytes read from source in a loop-iteration
                long totalin = sourceOffset; // number of bytes read from source from the begining (if an offset is skip, count it)
                long currentin  = 0; // current number of bytes read from source in a loop-iteration (to compute the speed)
                float speed = 0; // the speed of the encodage
                long computeTime = 0; // time when the encodage start
                byte[] bsin = new byte[4096]; // buffer for the source
 
                try{
                    // First, skip the offset, if positive
                    if(sourceOffset > 0){
                        long amountToSkip = sourceOffset;
                        long reallyskiped;
                        while(amountToSkip > 0) {
                            reallyskiped = filein.skip(amountToSkip);
                            amountToSkip -= reallyskiped;
                        }
                    }
                    
                    // In a loop, we read data from the source and feed the ffmpeg process
                    while (countin!=-1 && !stopMe){ // While input file has bytes
                        countin = filein.read(bsin); // Read a amount of bytes
                        currentin+=countin; // Count the amount of bytes read since the last speed computation

                        if (System.currentTimeMillis() - computeTime>1000){ // If one second passed
                            speed = currentin / ((System.currentTimeMillis() - computeTime)/1000f) / 1024f; // Compute the speed in ko/s
                            currentin = 0; // Initialize the number of bytes read for the next speed computation
                            computeTime = System.currentTimeMillis(); // Initialize the time for next speed computation
                            logger.fine("ffmpeg is running, speed:" + speed);
                        }

                        if (countin > 0){ // If bytes have been read
                            totalin += countin; // Count the number of bytes read since the beginning
                            if (totalSize!=0){ // Check that the read size is not null
                                nwGeneric.sendFileProgressChanged(NWGenericListener.ENCODING,(totalin*100)/totalSize, speed); // Inform GUI
                            }
                            else {
                                nwGeneric.sendFileProgressChanged(NWGenericListener.ENCODING,0, speed); // Inform GUI
                            }

                            processout.write(bsin, 0, countin); // write in the ffmpeg thread
                        }
                    }
                }
                catch(Exception ex){
                    // Something went wrong during the execution of FFMPEG, try to know what
                    reportException(ex);
                    return;
                }
                finally{
                    try {
                        // Once finished, close the stream
                        filein.close();
                        processout.close();
                    } catch (IOException ex) {
                        logger.severe("Error while closing the streams used by ffmpeg."+ex.getMessage());
                    }
                }
            }
        };

        // Start the read thread
        read.start();

        //Small pause to give time to the reader to start...
        try{
            Thread.sleep(1000);
        }
        catch(Exception ex){
            // Something went wrong during the execution of FFMPEG, try to know what
            logger.severe("An error occured while waiting the reader thread to give data to FFMPEG. The report is: "+ex.getMessage());
        }

        // Create a new thread, this thread will get the output of ffmpeg to fill in the output file
        Thread write = new Thread(){
            @Override
            public void run(){
                int countout = 0; // number of bytes read from the standard output in a loop-iteration
                long totalout = 0; // number of bytes read from the standard output from the begining
                byte[] bsout = new byte[4096]; // buffer for the destination

                // In a loop, we read data from the tandard output and fill in the output file
                try{
                    while (countout != -1){
                        countout = processin.read(bsout); // read the standard output
                        
                        if (countout>0){
                            totalout+=countout; // save the number of bytes written
                            fileout.write(bsout,0,countout); // write in the destination
                        }
                    }
                }
                catch(Exception ex){
                    // Something went wrong during the execution of FFMPEG, try to know what
                    reportException(ex);
                    return;
                }
                finally{
                    try {
                        // Once finished, close the stream
                        filein.close();
                        processout.close();
                    } catch (IOException ex) {
                        logger.severe("Error while closing the streams used by ffmpeg."+ex.getMessage());
                    }
                }
                    
                // Encoded is finished, put convertEnded to true, to warn main thread
                convertEnded=true;
            }
        };
        // Start the second thread
        write.start();

        byte[] bserr = null; // buffer to read the standard error
        // While the encodage is running, watch out the standard error, in case of trouble
        try{
            while(!convertEnded){
                bserr = new byte[processerror.available()]; // check if something is available

                // If something as happen, read it, print it
                if (bserr.length>0){
                    processerror.read(bserr); // read standard error
                    String error = new String(bserr); // convert to a string

                    // If an unsupported codec error occurs
                    if(error.toLowerCase().contains("could not find codec parameters") || error.toLowerCase().contains("unsupported codec")) {
                        // Stop threads (is they are not already stop)
                        convertEnded = true;

                        lastError = UNSUPPORTED_FORMAT;
                        return;
                    }


                    // If the output is not an info message, one assumes that it is an error, and log it
                    // Info message are:
                    //      Task description:
                    // Input #0, ogg, from 'pipe:':  Duration: N/A, start: 0.000000, bitrate: 192 kb/s     Stream #0.0: Audio: vorbis, 44100 Hz, stereo, 192 kb/s Output #0, mp3, to 'pipe:':     Stream #0.0: Audio: libmp3lame, 44100 Hz, stereo, 128 kb/s Stream mapping:   Stream #0.0 -> #0.0
                    //      Process completion:
                    // size=    4837kB time=309.6 bitrate= 128.0kbits/s
                    //      End report:
                    // video:0kB audio:5006kB global headers:0kB muxing overhead 0.000605%
                    // or
                    // FFmpeg version SVN-
                    // or
                    // buffer too small (message passing between LAME and FFMPEG which is not of interest for us)
                    // ts/s (not really know what it means, but it is not an error)
                    if(!error.contains("size=") && !error.contains("Stream mapping") && !error.contains("video:0kB") && !error.contains("Multiple frames in a packet") && !error.contains("FRAME HEADER not here") && !error.contains("output buffer too small") && !error.contains("FFmpeg version SVN-") && !error.contains("ts/s")){
                        logger.warning("Unexpected message while transcoding file \""+source.getAbsolutePath()+"\", could be an error: \n"+error);
                    }
                }
            }

            Thread.sleep(100); // wait a while before checking again
        }
        catch(Exception ex){
            // Something went wrong during the execution of FFMPEG, try to know what
            reportException(ex);
            // Close opened streams
            try {
                filein.close(); fileout.close(); processerror.close();
            }
            catch(Exception e){
                logger.warning("An error occured while closing the streams used by FFMPEG.");
            }
            return;
        }

        // Re put convertEnded for next encodage
        convertEnded=false;

        // Clear references to the streams
        filein=null;
        fileout=null;

        // Wait for the end of the ffmpeg process
        try{
            p.waitFor();
        }
        catch(Exception ex){
            // Something went wrong during the execution of FFMPEG, try to know what
            reportException(ex);
            // Close opened streams
            try {
                filein.close(); fileout.close(); processerror.close();
            }
            catch(Exception e){
                logger.warning("An error occured while closing the streams used by FFMPEG.");
            }
            return;
        }

        // Get the value returned by the ffmpeg process
        // FFMPEG returns 0 if all went right, 1 otherwise
//Nicolas: I commented this part because in Windows version of FFMPEG, LAME may say to FFMPEG that its buffer is too small and then FFMPEG return 1 even thought it is just a warning and not an error, so, checking that FFMPEG returns 1 here makes introduce an error when there is not.
//         if(p.exitValue() != 0 && lastError == NO_ERROR){
//             // If FFMPEG return 1 and no error has already been detected, an unknown error occured
//             lastError = ERROR;
//         }
        
        // Clear references to the process streams
        processin=null;
        processout=null;
        p=null;

        // Return the value returned by the ffmpeg process
        return;
    }

    /**
     * This method tries to guess what happened to FFMPEG to place the "lastError" value correctly.
     *
     * @param ex The exception thrown by FFMPEG.
     */
    private void reportException(Exception ex) {
        // Try to determinate what happened to FFMPEG
        if(ex.getMessage().toLowerCase().contains("broken pipe")){
            lastError = BROKEN_PIPE; // Error is known
        }
        else if(ex.getMessage().toLowerCase().contains("could not find codec parameters")){
            lastError = UNSUPPORTED_FORMAT; // Error is known
        }
        else if(ex.getMessage().toLowerCase().contains("no space left")){
            lastError = DESTINATION_FULL; // Error is known
        }
        else if(ex.getMessage().toLowerCase().contains("output buffer too small")){
            lastError = NO_ERROR; // This message correspond just to a message passing between LAME and FFMPEG, it is not an error
        }
        else {
            // else, error is not known
            lastError = ERROR;
        }


    }
    
    /**
     *Tell if the format is directly encodable, without been decoded before. This depends on the format handled by FFMPEG.
     *
     * @param title The title we want to know if it is encodable.
     * @return true is the title can be directly encoded without been decoded, false otherwise.
     */
    public static boolean isEncodable(Title title){
        int format = title.getFormat();
        
        // If the format of the file is handled by FFMPEG, return true
        if(format == Title.MP3 || format == Title.MPC || format == Title.OGG || format == Title.WMA || format == Title.WAV || format == Title.AAC || format == Title.APE)
            return true;
        else
            return true;
    }
    
    public static boolean isFFMpegPresent() {
        FFMpegToolBox ffmpeg = new FFMpegToolBox();
        return ffmpeg.isFFMpegDetected();
        
    }
    
    private boolean isFFMpegDetected() {
        return ffmpegDetected;
    }

    public static void setParentLogger(Logger aLogger) {
        logger.setParent(aLogger);
    }
}
