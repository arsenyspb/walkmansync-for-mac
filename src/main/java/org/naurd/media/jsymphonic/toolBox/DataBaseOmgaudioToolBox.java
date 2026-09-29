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
 * DataBaseOmgaudioToolBox.java
 *
 * Created on 27 mars 2008, 19:15
 *
 */

package org.naurd.media.jsymphonic.toolBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.title.Oma;





/**
 * Provides basic function to write the data base on the Sony devices, whatever the generation. This tool box should be replaced by the Sony Library coded in C++ when it will be ready.
 *
 * @author nicolas_cardoso
 */
public class DataBaseOmgaudioToolBox {
    //Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.toolBox.OmaDataBaseToolBox");
    
/* CONSTRUCTORS */
    /** 
     * This class is a tool box, it shouldn't be instanced.
     * 
     * @author nicolas_cardoso
     */
    public DataBaseOmgaudioToolBox() {
    }
    
    
    
/* METHODS */ 
    /**
     * Write 00GRTLST file in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write00GRTLST(File omgaudioDir) throws Exception{
        File table0 = new File(omgaudioDir + "/00gtrlst.dat");
        RandomAccessFile rafTable0;
        
        // Create a new file
        table0.createNewFile();

        // Write data in the file :
        rafTable0 = new RandomAccessFile(table0, "rw"); //Open the file in RAF

        // Header
        WriteTableHeader(rafTable0, "GTLT", 2); //Write table header
        WriteClassDescription(rafTable0, "SYSB", 0x30, 0x70); //Write first class description
        WriteClassDescription(rafTable0, "GTLB", 0xa0, 0xe20); //Write second class description

        //Class 1
        WriteClassHeader(rafTable0, "SYSB", 1, 0x50, 0xd0000000, 0x00000000); //Write first class header
        WriteZeros(rafTable0, 6 * 0x10); //Write element 1

        //Class 2
        WriteClassHeader(rafTable0, "GTLB", 0x2d, 0x50, 0x00000006, 0x04000000); //Write second class header
        WriteGTLBelement(rafTable0, 1, 1, 1, "", "", new byte[0]); //Element 1
        WriteGTLBelement(rafTable0, 2, 3, 1, "TPE1", "", new byte[0]); //Element 2
        WriteGTLBelement(rafTable0, 3, 3, 1, "TALB", "", new byte[0]); //Element 3
        WriteGTLBelement(rafTable0, 4, 3, 1, "TCON", "", new byte[0]); //Element 4
        WriteGTLBelement(rafTable0, 0x22, 2, 0, "", "", new byte[0]); //Element 5
        WriteGTLBelement(rafTable0, 0x2d, 3, 2, "TPE1", "TALB", ("TRNOTTCCTTCC").getBytes()); //Element 6

        for( int i = 5; i <= 44; i++) {
            if( i == 34 ){
                continue;
            }
            WriteGTLBelement(rafTable0, i, 0, 0, "", "", new byte[0]); //Elements 5 to 44, avoiding 34
        }

        rafTable0.close();
    }
    
    
    
    /**
     * Write 01TREE01 and 03GINF01 files in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titles List of the titles in the data base.
     *@param albums List of the albums in the data base.
     *@param titleKeys List of the titleKeys in the data base.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write01TREE01and03GINF01(File omgaudioDir, JSymphonicMap titles) throws Exception{
        File table11 = new File(omgaudioDir + "/01tree01.dat");
        File table31 = new File(omgaudioDir + "/03ginf01.dat");
        RandomAccessFile rafTable11;
        RandomAccessFile rafTable31;
        
        // Create a new file
        table11.createNewFile();
        table31.createNewFile();

        // Write data in the file :
        rafTable11 = new RandomAccessFile(table11, "rw"); //Open the file in RAF
        rafTable31 = new RandomAccessFile(table31, "rw"); //Open the file in RAF

        //Sort the titles in the right order
        List sortedTitles = sortByArtistAlbumTitleNumber(titles);

        // Header 11
        WriteTableHeader(rafTable11, "TREE", 2); //Write table header
        WriteClassDescription(rafTable11, "GPLB", 0x30, 0x4010); //Write first class description
        int class111Length = sortedTitles.size()*0x2 + 0x10; // Calcul class's length
        class111Length += 0x10 - (class111Length % 0x10); // Get an "entire" number
        WriteClassDescription(rafTable11, "TPLB", 0x4040, class111Length); //Write second class description

        // Header 31
        WriteTableHeader(rafTable31, "GPIF", 1); //Write table header
        // ClassDescription can't be written now

        //Get the information needed
        List titlesIdInTPLBlist = new ArrayList();
        List albumsSorted = new ArrayList();
        List artistsSorted = new ArrayList();
        List genresSorted = new ArrayList();
        List titleKeysSorted = new ArrayList();
        List GPLBelements = new ArrayList();
        String albumName, lastAlbumSorted = "";
        Title tempTitle;
        int tempKey = 0;
        int j = 0;

        for( int i = 0; i < sortedTitles.size(); i++ ) {
            tempTitle = (Title) sortedTitles.get(i); // Get the title to add

            // First, check if the title ID is in used
            if( tempTitle == null ) {
                // If no, store that this ID is not used
                continue; // Nothing else to do
            }

            // If ID is in use, store it
            titlesIdInTPLBlist.add(j, titles.getValue(tempTitle));
            j++;
            // Get the name of the corresponding album and add the artist name for NW-E10x players
            albumName = tempTitle.getArtist() + "/" + tempTitle.getAlbum();
            if(albumName.length() == 0) albumName = "unknown album (JS)"; // Check the name

            if( !lastAlbumSorted.equals(albumName) ) { //If current album isn't the same as the last one
                // Sort GPLB elements
                GPLBelements.add(j);
                // Sort album info
                albumsSorted.add(albumName);
                artistsSorted.add(tempTitle.getArtist());
                genresSorted.add(tempTitle.getGenre());
                // Sort the key of the last album, if any
                if(albumsSorted.size() > 0){
                    titleKeysSorted.add(tempKey);
                }
                // Update variable for next loop
                tempKey = tempTitle.getLength();
                lastAlbumSorted = albumName;
            }
            else { //If it's still the same album, increase the key of this album with this title
                tempKey += tempTitle.getLength();
            }
        }
        // If there is at least one album, fill in last key
        if(albumsSorted.size()>0){ 
            titleKeysSorted.add(tempKey);
        }
        //End of 'Get the information needed'

        // Header 31
         int numberOfAlbums = (albumsSorted.size()); // The number of album is given by the size of albumsSorted
         WriteClassDescription(rafTable31, "GPFB", 0x20, numberOfAlbums*0x310 + 0x10); //Write first class description

        //11-Class 1    
        WriteClassHeader(rafTable11, "GPLB", numberOfAlbums, 0x8, albumsSorted.size(), 0); //Write first class header
        //31-Class 1    
        WriteClassHeader(rafTable31, "GPFB", numberOfAlbums, 0x310); //Write first class header

        //Fill in elements in 11-Class 1 & 31-Class 1
        for( int i = 0; i < albumsSorted.size(); i++ ) {
            WriteGPLBelement(rafTable11, i+1, (Integer)GPLBelements.get(i));
            WriteGPFBelement(rafTable31, (Integer)titleKeysSorted.get(i), (String)albumsSorted.get(i), (String)artistsSorted.get(i), (String)genresSorted.get(i));
        }

        WriteZeros(rafTable11, 0x4010 - 0x10 - (0x8*numberOfAlbums)); // Fill in the class with zeros

        //11-Class 2    
        WriteClassHeader(rafTable11, "TPLB", sortedTitles.size(), 0x2, titles.size(), 0); //Write first class header

        //Fill in elements in 11-Class 2
        // First used title ID
        for(int i = 0; i < titlesIdInTPLBlist.size(); i++) {
            rafTable11.write(int2bytes((Integer)titlesIdInTPLBlist.get(i), 2));
        }

        WriteZeros(rafTable11, 0x10 - ((sortedTitles.size()*0x2) % 0x10)); // Fill in the class with zeros

        rafTable11.close();
        rafTable31.close();
    }
    
    
    
    /**
     * Write 01TREE02 and 03GINF02 files in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titles List of the titles in the data base.
     *@param artists List of the artists in the data base.
     *@param titleKeys List of the titleKeys in the data base.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write01TREE02and03GINF02(File omgaudioDir, Map titles) throws Exception{
        File table12 = new File(omgaudioDir + "/01tree02.dat");
        File table32 = new File(omgaudioDir + "/03ginf02.dat");
        RandomAccessFile rafTable12;
        RandomAccessFile rafTable32;
        
        // Create a new file
        table12.createNewFile();
        table32.createNewFile();
        
        // Write data in the file :
        rafTable12 = new RandomAccessFile(table12, "rw"); //Open the file in RAF
        rafTable32 = new RandomAccessFile(table32, "rw"); //Open the file in RAF

        // Header 12
        WriteTableHeader(rafTable12, "TREE", 2); //Write table header
        WriteClassDescription(rafTable12, "GPLB", 0x30, 0x4010); //Write first class description
        int class121Length = titles.size()*0x2 + 0x10; // Calcul class's length
        class121Length += 0x10 - (class121Length % 0x10); // Get an "entire" number
        WriteClassDescription(rafTable12, "TPLB", 0x4040, class121Length); //Write second class description

        //Sort the titles in the right order
        List sortedTitles = sortByArtistTitle(titles);

        //Get the information needed
        List titlesIdInTPLBlist = new ArrayList();
        List artistsSorted = new ArrayList();
        List titleKeysSorted = new ArrayList();
        List GPLBelements = new ArrayList();
        String artistName, lastArtistName = "";
        Title tempTitle;
        int tempKey = 0;

        for( int i = 0; i < sortedTitles.size(); i++ ) { // All the title_ID must be scanned, even if no title are assigned to some of the IDs
            tempTitle = (Title) sortedTitles.get(i); // Get the title to add
            if(tempTitle == null){logger.warning("An invalid title has been found at OmaDataBaseToolBox@write01TREE02and03GINF02.");continue;} // If the title is not valid, just ignore it
            artistName = tempTitle.getArtist(); // Get the name of the artist
            if(artistName.length() == 0) artistName = "unknown artist (JS)"; // Check the name of the artist

            if( !lastArtistName.equals(artistName) ) { //If current artist isn't the same as the last one
                // Store the artist name
                artistsSorted.add(artistName);
                // Store the ID of the artist corresponding to its first title
                GPLBelements.add(i+1);
                // Sort the key of the last album, if any
                if(artistsSorted.size() > 0){
                    titleKeysSorted.add(tempKey);
                }
                // Update variable for next loop
                lastArtistName = artistName;
                tempKey = tempTitle.getLength();
            }
            else { //If it's still the same artist, increase the key of this artist with this title
                tempKey += tempTitle.getLength();
            }

            // Add the title to the list
            titlesIdInTPLBlist.add(titles.get(tempTitle));
        }
        // If there is at least one album, fill in last key
        if(artistsSorted.size()>0){
            titleKeysSorted.add(tempKey);
        }
        //End of 'Get the information needed'

        // Header 32
        WriteTableHeader(rafTable32, "GPIF", 1); //Write table header
        WriteClassDescription(rafTable32, "GPFB", 0x20, (artistsSorted.isEmpty() ? 0 : artistsSorted.size())*0x90 + 0x10); //Write first class description

        //12-Class 1    
        WriteClassHeader(rafTable12, "GPLB", artistsSorted.size(), 0x8, artistsSorted.size(), 0); //Write first class header
        //32-Class 1    
        WriteClassHeader(rafTable32, "GPFB", (artistsSorted.isEmpty() ? 0 : artistsSorted.size()), 0x90); //Write first class header

        //Fill in elements in 12-Class 1 & 32-Class 1
        for( int i = 0; i < artistsSorted.size(); i++ ) {
            WriteGPLBelement(rafTable12, i+1, (Integer)GPLBelements.get(i));
            WriteGPFBelement(rafTable32, (Integer)titleKeysSorted.get(i), (String)artistsSorted.get(i));
        }
        WriteZeros(rafTable12, 0x4010 - 0x10 - (0x8 * artistsSorted.size())); // Fill in the class with zeros

        //12-Class 2    
        WriteClassHeader(rafTable12, "TPLB", titlesIdInTPLBlist.size(), 0x2, sortedTitles.size(), 0); //Write first class header

        //Fill in elements in 12-Class 2
        for( int i = 0; i < titlesIdInTPLBlist.size(); i++ ) {
            rafTable12.write(int2bytes((Integer)titlesIdInTPLBlist.get(i), 2));
        }
        WriteZeros(rafTable12, 0x10 - ((titlesIdInTPLBlist.size()*0x2) % 0x10)); // Fill in the class with zeros

        rafTable12.close();
        rafTable32.close();
    }

    
    
    /**
     * Write 01TREE03 and 03GINF03 files in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titles List of the titles in the data base.
     *@param albums List of the albums in the data base.
     *@param titleKeys List of the titleKeys in the data base.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write01TREE03and03GINF03(File omgaudioDir, Map titles) throws Exception{
        File table13 = new File(omgaudioDir + "/01tree03.dat");
        File table33 = new File(omgaudioDir + "/03ginf03.dat");
        RandomAccessFile rafTable13;
        RandomAccessFile rafTable33;
        
        // Create a new file
        table13.createNewFile();
        table33.createNewFile();
        
        // Write data in the file :
        rafTable13 = new RandomAccessFile(table13, "rw"); //Open the file in RAF
        rafTable33 = new RandomAccessFile(table33, "rw"); //Open the file in RAF

        // Header 13
        WriteTableHeader(rafTable13, "TREE", 2); //Write table header
        WriteClassDescription(rafTable13, "GPLB", 0x30, 0x4010); //Write first class description
        int class131Length = titles.size()*0x2 + 0x10; // Calcul class's length
        class131Length += 0x10 - (class131Length % 0x10); // Get an "entire" number
        WriteClassDescription(rafTable13, "TPLB", 0x4040, class131Length); //Write second class description

        //Sort the titles in the right order
//        List sortedTitles = sortByArtistAlbumTitleNumber(titles);
        List sortedTitles = sortByAlbumTitleNumber(titles);

        //Get the information needed
        List titlesIdInTPLBlist = new ArrayList();
        List albumsSorted = new ArrayList();
        List titleKeysSorted = new ArrayList();
        List GPLBelements = new ArrayList();
        String albumName, lastAlbumName = "";
        Title tempTitle;
        int tempKey = 0;

        for( int i = 0; i < sortedTitles.size(); i++ ) { // All the title_ID must be scanned, even if no title are assigned to some of the IDs
            tempTitle = (Title) sortedTitles.get(i); // Get the title to add
            if(tempTitle == null){logger.warning("An invalid title has been found at OmaDataBaseToolBox@write01TREE03and03GINF03.");continue;} // If the title is not valid, just ignore it
            albumName = tempTitle.getAlbum(); // Get the name of the album
            if(albumName.length() == 0) albumName = "unknown album (JS)"; // Check the name

            if( !lastAlbumName.equals(albumName) ) { //If current artist isn't the same as the last one
                // Store the album name
                albumsSorted.add(albumName);
                // Store the ID of the artist corresponding to its first title
                GPLBelements.add(i+1);
                // Sort the key of the last album, if any
                if(albumsSorted.size() > 0){
                    titleKeysSorted.add(tempKey);
                }
                // Update variable for next loop
                lastAlbumName = albumName;
                tempKey = tempTitle.getLength();
            }
            else { //If it's still the same artist, increase the key of this artist with this title
                tempKey += tempTitle.getLength();
            }

            // Add the title to the list
            titlesIdInTPLBlist.add(titles.get(tempTitle));
        }
        // If there is at least one album, fill in last key
        if(albumsSorted.size()>0){
            titleKeysSorted.add(tempKey);
        }
        //End of 'Get the information needed'

        // Header 33
        WriteTableHeader(rafTable33, "GPIF", 1); //Write table header
        WriteClassDescription(rafTable33, "GPFB", 0x20, (albumsSorted.isEmpty() ? 0 : albumsSorted.size())*0x90 + 0x10); //Write first class description

        //13-Class 1    
        WriteClassHeader(rafTable13, "GPLB", albumsSorted.size(), 0x8, albumsSorted.size(), 0); //Write first class header
        //33-Class 1    
        WriteClassHeader(rafTable33, "GPFB", albumsSorted.size(), 0x90); //Write first class header

        //Fill in elements in 13-Class 1 & 33-Class 1
        for( int i = 0; i < albumsSorted.size(); i++ ) {
            WriteGPLBelement(rafTable13, i+1, (Integer)GPLBelements.get(i));
            WriteGPFBelement(rafTable33, 0, (String)albumsSorted.get(i));
        }
        WriteZeros(rafTable13, 0x4010 - 0x10 - (0x8 * albumsSorted.size())); // Fill in the class with zeros

        //13-Class 2    
        WriteClassHeader(rafTable13, "TPLB", titlesIdInTPLBlist.size(), 0x2, titlesIdInTPLBlist.size(), 0); //Write first class header

        //Fill in elements in 13-Class 2
        for( int i = 0; i < titlesIdInTPLBlist.size(); i++ ) {
            rafTable13.write(int2bytes((Integer)titlesIdInTPLBlist.get(i), 2));
        }
        WriteZeros(rafTable13, 0x10 - ((titlesIdInTPLBlist.size()*0x2) % 0x10)); // Fill in the class with zeros

        rafTable13.close();
        rafTable33.close();
    }
        
        
    /**
     * Write 01TREE04 and 03GINF04 files in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titles List of the titles in the data base.
     *@param genres List of the genres in the data base.
     *@param titleKeys List of the titleKeys in the data base.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write01TREE04and03GINF04(File omgaudioDir, Map titles) throws Exception{
        File table14 = new File(omgaudioDir + "/01tree04.dat");
        File table34 = new File(omgaudioDir + "/03ginf04.dat");
        RandomAccessFile rafTable14;
        RandomAccessFile rafTable34;
        
        // Create a new file
        table14.createNewFile();
        table34.createNewFile();
        
        // Write data in the file :
        rafTable14 = new RandomAccessFile(table14, "rw"); //Open the file in RAF
        rafTable34 = new RandomAccessFile(table34, "rw"); //Open the file in RAF

        // Header 14
        WriteTableHeader(rafTable14, "TREE", 2); //Write table header
        WriteClassDescription(rafTable14, "GPLB", 0x30, 0x4010); //Write first class description
        int class141Length = titles.size()*0x2 + 0x10; // Calcul class's length
        class141Length += 0x10 - (class141Length % 0x10); // Get an "entire" number
        WriteClassDescription(rafTable14, "TPLB", 0x4040, class141Length); //Write second class description

        //Sort the titles in the right order
        List sortedTitles = sortByGenreTitle(titles);

        //Get the information needed
        List titlesIdInTPLBlist = new ArrayList();
        List genresSorted = new ArrayList();
        List titleKeysSorted = new ArrayList();
        List GPLBelements = new ArrayList();
        String genre, lastGenre = "";
        Title tempTitle;
        int tempKey = 0;

        for( int i = 0; i < sortedTitles.size(); i++ ) { // All the title_ID must be scanned, even if no title are assigned to some of the IDs
            tempTitle = (Title) sortedTitles.get(i); // Get the title to add
            if(tempTitle == null){logger.warning("An invalid title has been found at OmaDataBaseToolBox@write01TREE04and03GINF04.");continue;} // If the title is not valid, just ignore it
            genre = tempTitle.getGenre(); // Get the name of the genre
            if(genre.length() == 0) genre = "unknown genre (JS)"; // Check the name

            if( !lastGenre.equals(genre) ) { //If current artist isn't the same as the last one
                // Store the genre
                genresSorted.add(genre);
                // Store the ID of the artist corresponding to its first title
                GPLBelements.add(i+1);
                // Sort the key of the last album, if any
                if(genresSorted.size() > 0){
                    titleKeysSorted.add(tempKey);
                }
                // Update variable for next loop
                lastGenre = genre;
                tempKey = tempTitle.getLength();
            }
            else { //If it's still the same genre, increase the key of this artist with this title
                tempKey += tempTitle.getLength();
            }

            // Add the title to the list
            titlesIdInTPLBlist.add(titles.get(tempTitle));
        }
        // If there is at least one album, fill in last key
        if(genresSorted.size()>0){
            titleKeysSorted.add(tempKey);
        }
        //End of 'Get the information needed'

        // Header 34
        WriteTableHeader(rafTable34, "GPIF", 1); //Write table header
        WriteClassDescription(rafTable34, "GPFB", 0x20, (genresSorted.isEmpty() ? 0 : genresSorted.size())*0x90 + 0x10); //Write first class description

        //14-Class 1    
        WriteClassHeader(rafTable14, "GPLB", genresSorted.size(), 0x8, genresSorted.size(), 0); //Write first class header
        //34-Class 1    
        WriteClassHeader(rafTable34, "GPFB", genresSorted.size(), 0x90); //Write first class header

        //Fill in elements in 14-Class 1 & 34-Class 1
        for( int i = 0; i < genresSorted.size(); i++ ) {
            WriteGPLBelement(rafTable14, i+1, (Integer)GPLBelements.get(i));
            WriteGPFBelement(rafTable34, i+1, (String)genresSorted.get(i));
        }
        WriteZeros(rafTable14, 0x4010 - 0x10 - (0x8 * genresSorted.size())); // Fill in the class with zeros

        //14-Class 2    
        WriteClassHeader(rafTable14, "TPLB", titlesIdInTPLBlist.size(), 0x2, titlesIdInTPLBlist.size(), 0); //Write first class header

        //Fill in elements in 14-Class 2
        for( int i = 0; i < titlesIdInTPLBlist.size(); i++ ) {
            rafTable14.write(int2bytes((Integer)titlesIdInTPLBlist.get(i), 2));
        }
        WriteZeros(rafTable14, 0x10 - ((titlesIdInTPLBlist.size()*0x2) % 0x10)); // Fill in the class with zeros

        rafTable14.close();
        rafTable34.close();
    }

        
    /**
     * Write 01TREE22 and 03GINF22 files in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write01TREE22and03GINF22(File omgaudioDir) throws Exception{
        File table122 = new File(omgaudioDir + "/01tree22.dat");
        File table322 = new File(omgaudioDir + "/03ginf22.dat");
        RandomAccessFile rafTable122;
        RandomAccessFile rafTable322;
        
        //Create a new file
        table122.createNewFile();
        table322.createNewFile();

        // Write data in the file :
        rafTable122 = new RandomAccessFile(table122, "rw"); //Open the file in RAF
        rafTable322 = new RandomAccessFile(table322, "rw"); //Open the file in RAF

        // Header 122
        WriteTableHeader(rafTable122, "TREE", 2); //Write table header
        WriteClassDescription(rafTable122, "GPLB", 0x30, 0x10); //Write first class description
        WriteClassDescription(rafTable122, "TPLB", 0x40, 0x10); //Write second class description

        // Header 322
        WriteTableHeader(rafTable322, "GPIF", 1); //Write table header
        WriteClassDescription(rafTable322, "GPFB", 0x20, 0x10); //Write first class description


        //122-Class 1    
        WriteClassHeader(rafTable122, "GPLB", 0, 0x8, 0, 0); //Write first class header
        //322-Class 1    
        WriteClassHeader(rafTable322, "GPFB", 0, 0x310); //Write first class header

        //122-Class 2    
        WriteClassHeader(rafTable122, "TPLB", 0, 0x2, 0, 0); //Write first class header

        rafTable122.close();
        rafTable322.close();
    }

    
    
    /**
     * Write 01TREE2D and 03GINF2D files in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titles List of the titles in the data base.
     *@param artists List of the artists in the data base.
     *@param albums List of the albums in the data base.
     *@param titleKeys List of the titleKeys in the data base.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write01TREE2Dand03GINF2D(File omgaudioDir, Map titles) throws Exception{
        File table12D = new File(omgaudioDir + "/01tree2D.dat");
        File table32D = new File(omgaudioDir + "/03ginf2D.dat");
        RandomAccessFile rafTable12D;
        RandomAccessFile rafTable32D;
        
        //Create new file
        table12D.createNewFile();
        table32D.createNewFile();

        // Write data in the file :
        rafTable12D = new RandomAccessFile(table12D, "rw"); //Open the file in RAF
        rafTable32D = new RandomAccessFile(table32D, "rw"); //Open the file in RAF

        // Header 12D
        WriteTableHeader(rafTable12D, "TREE", 2); //Write table header
        WriteClassDescription(rafTable12D, "GPLB", 0x30, 0x4010); //Write first class description
        int class12D1Length = titles.size()*0x2 + 0x10; // Calcul class's length
        class12D1Length += 0x10 - (class12D1Length % 0x10); // Get an "entire" number
        WriteClassDescription(rafTable12D, "TPLB", 0x4040, class12D1Length); //Write second class description

        //Sort the titles in the right order
        List sortedTitles = sortByArtistAlbumTitleNumber(titles);

        //Get the information needed
        List titlesIdInTPLBlist = new ArrayList();
        List artistsSorted = new ArrayList();
        List albumsSorted = new ArrayList();
        List titleKeysSorted = new ArrayList();
        List GPLBelements = new ArrayList();
        List albumsCounter = new ArrayList();
        String artistName, lastArtistName="", albumName, lastAlbumName="";
        Title tempTitle;
        int tempKey = 0;
        int albumCounter = 0;
        int j = 2;

        for( int i = 0; i < sortedTitles.size(); i++ ) { // All the title_ID must be scanned, even if no title are assigned to some of the IDs
            tempTitle = (Title) sortedTitles.get(i); // Get the title to add
            if(tempTitle == null){logger.warning("An invalid title has been found at OmaDataBaseToolBox@write01TREE2Dand03GINF2D.");continue;} // If the title is not valid, just ignore it
            artistName = tempTitle.getArtist(); // Get the name of the artist
            albumName = tempTitle.getAlbum(); // Get the name of the album
            if(artistName.length() == 0) artistName = "unknown artist (JS)"; // Check the name
            if(albumName.length() == 0) albumName = "unknown album (JS)"; // Check the name

            if( !lastAlbumName.equals(albumName) ) { //If current album isn't the same as the last one
                if( !lastArtistName.equals(artistName) ) { //If current artist isn't the same as the last one
                    // Store the new artist
                    artistsSorted.add(artistName);
                    // Store the number of albums for the last artist
                    if(albumsSorted.size() > 0){
                        albumsCounter.add(albumCounter);
                    }
                    // Update variable for next loop
                    lastArtistName = artistName;
                    albumCounter = 0;
                    tempKey = tempTitle.getLength();
                }

                // Store the new album
                albumsSorted.add(albumName);
                // Store the ID of the artist corresponding to its first title
                GPLBelements.add(i+1);
                // Sort the key of the last album, if any
                if(albumsSorted.size() > 0){
                    titleKeysSorted.add(tempKey);
                }
                // Update variable for next loop
                lastAlbumName = albumName;
                albumCounter++;
                tempKey = tempTitle.getLength();
            }
            else { //If it's still the same artist and album, increase the key of this album with this title
                tempKey += tempTitle.getLength();
            }

            // Add the title to the list
            titlesIdInTPLBlist.add(titles.get(tempTitle));
        }
        // If there is at least one album, fill in last key and album counter
        if(albumsSorted.size()>0){
            titleKeysSorted.add(tempKey);
            albumsCounter.add(albumCounter);
        }
        //End of 'Get the information needed'

        // Header 32D
        WriteTableHeader(rafTable32D, "GPIF", 1); //Write table header
        WriteClassDescription(rafTable32D, "GPFB", 0x20, (artistsSorted.size() + albumsSorted.size() + 1)*0x110 + 0x10); //Write first class description

        //12D-Class 1
        WriteClassHeader(rafTable12D, "GPLB", artistsSorted.size() + albumsSorted.size() + 1, 0x8, artistsSorted.size() + albumsSorted.size() + 1, 0); //Write first class header
        //32D-Class 1
        WriteClassHeader(rafTable32D, "GPFB", artistsSorted.size() + albumsSorted.size() + 1, 0x110); //Write first class header

        //Fill in elements in 12D-Class 1 & 32D-Class 1
        //First element is empty
        WriteGPLBelement(rafTable12D, 1, 0);
        WriteGPFBelement(rafTable32D, 0, "", "");

        //Next elements
        int albumSortedCounter = 0;
        for( int i = 0; i < artistsSorted.size(); i++ ) {
            // First, write the artist
            WriteGPLBelement(rafTable12D, i + albumSortedCounter + 2, 0);
            WriteGPFBelement(rafTable32D, 0, (String)artistsSorted.get(i), (String)artistsSorted.get(i));

            // Then, add all the albums corresponding to this artist
            for( int k = 0; k < (Integer)albumsCounter.get(i); k++ ) {
                WriteGPLBelement2(rafTable12D,  i + albumSortedCounter + 3, (Integer)GPLBelements.get(albumSortedCounter));
                WriteGPFBelement(rafTable32D, (Integer)titleKeysSorted.get(albumSortedCounter), (String)albumsSorted.get(albumSortedCounter), (String)albumsSorted.get(albumSortedCounter));
                albumSortedCounter++;
            }
        }

        // Fill in the class with zeros
        WriteZeros(rafTable12D, 0x4010 - 0x10 - (0x8*(artistsSorted.size() + albumsSorted.size() + 1))); // Fill in the class with zeros

        //12D-Class 2
        WriteClassHeader(rafTable12D, "TPLB", titlesIdInTPLBlist.size(), 0x2, titlesIdInTPLBlist.size(), 0); //Write first class header

        //Fill in elements in 12D-Class 2
        for( int i = 0; i < titlesIdInTPLBlist.size(); i++ ) {
            rafTable12D.write(int2bytes((Integer)titlesIdInTPLBlist.get(i), 2));
        }
        WriteZeros(rafTable12D, 0x10 - ((titlesIdInTPLBlist.size()*0x2) % 0x10)); // Fill in the class with zeros

        rafTable12D.close();
        rafTable32D.close();
    }
        
        
    /**
     * Write 02TREINF file in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titleKeys List of the titleKeys in the data base.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write02TREINF(File omgaudioDir, Map titles) throws Exception{
        File table2 = new File(omgaudioDir + "/02treinf.dat");
        RandomAccessFile rafTable2;
        
        //Create new file
        table2.createNewFile();
        
        // Write data in the file :
        rafTable2 = new RandomAccessFile(table2, "rw"); //Open the file in RAF

        // Header
        WriteTableHeader(rafTable2, "GTIF", 1); //Write table header
        WriteClassDescription(rafTable2, "GTFB", 0x20, 0x1f00); //Write first class description

        //Class 1
        WriteClassHeader(rafTable2, "GTFB", 0x2d, 0x90); //Write first class header

        //Before writing element, need to know the global key
        Iterator itKey = titles.keySet().iterator();
        int globalKey = 0;
        Title titleTemp;
        while( itKey.hasNext() ) {
            titleTemp = (Title)itKey.next();
            globalKey += titleTemp.getLength();
        }

        // Elements 1 to 4
        for( int i = 1; i <= 4; i++) {
            WriteGTFBelement(rafTable2, globalKey,"");
        }

        // Elements 5 to 0x21
        WriteZeros(rafTable2, (0x21 - 0x5 + 1)*0x90);

        // Element 0x22
        WriteGTFBelement(rafTable2, 0,"");

        // Element 0x23 to 0x2c
        WriteZeros(rafTable2, (0x2c - 0x23 + 1)*0x90);

        // Element 0x2D
        WriteGTFBelement(rafTable2, globalKey,"STD_TPE1");

        // Fill in the class with zeros
        WriteZeros(rafTable2, 0x1f00 - (0x2d*0x90) - 0x10);

        rafTable2.close();
    }
        
    
    
    /**
     * Write 04CNTINF file in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titles
     * @param magicKey The unique key of the walkman
     *@param titleKeys List of the titleKeys in the data base.
     *
     *@throws Exception
     * @author nicolas_cardoso
     */
    public static void write04CNTINF(File omgaudioDir, JSymphonicMap titles, boolean gotKey) throws Exception{
        File table4 = new File(omgaudioDir + "/04cntinf.dat");
        RandomAccessFile rafTable4;
        
        //Create new file
        table4.createNewFile();

        // Write data in the file :
        rafTable4 = new RandomAccessFile(table4, "rw"); //Open the file in RAF

        // Header
        WriteTableHeader(rafTable4, "CNIF", 1); //Write table header
        WriteClassDescription(rafTable4, "CNFB", 0x20, (titles.isEmpty() ? 0 : titles.maxValue())*0x290 + 0x10); //Write first class description
        WriteClassHeader(rafTable4, "CNFB", (titles.isEmpty() ? 0 : titles.maxValue()), 0x290); //Write first class header

        // Write elements
        Title titleTemp;

        for( int i = 1; i <= titles.maxValue(); i++ ) { //04CNTINF must contains an element per title_ID even if no title actually correspond to some of the IDs !! That's the reason why I'm not using an iterator to look into the list
            titleTemp = (Title)titles.getKey(i);
            WriteCNFBelement(rafTable4, titleTemp, gotKey);
       }

        rafTable4.close();
    }
    
    /**
     * Write 04PATLST file in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     * Note that this file DOESN'T belongs to the database as SonicStage write it. It is just a file I added to keep a trace of the original path of the audio files in the database.
     * Knowing these paths prevent from added twice the same file, and it eases the handling of playlist.
     *
     * @param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     * @param paths The list of paths for each title in the database
     *
     * @throws Exception
     * @author nicolas_cardoso
     */
    public static void write04PATLST(File omgaudioDir, JSymphonicMap paths) throws Exception{
        File table4 = new File(omgaudioDir + "/04patlst.dat");
        RandomAccessFile rafTable4;

        //Create new file
        table4.createNewFile();

        // Write data in the file :
        rafTable4 = new RandomAccessFile(table4, "rw"); //Open the file in RAF

        // Header
        WriteTableHeader(rafTable4, "PALT", 1); //Write table header
        WriteClassDescription(rafTable4, "PATH", 0x20, (paths.isEmpty() ? 0 : paths.maxKey())*0x290 + 0x10); //Write first class description
        WriteClassHeader(rafTable4, "PATH", (paths.isEmpty() ? 0 : paths.maxKey()), 0x290); //Write first class header

        // Write elements
        String pathTemp;

        for( int i = 1; i <= paths.maxKey(); i++ ) { //04CNTINF must contains an element per title_ID even if no title actually correspond to some of the IDs !! That's the reason why I'm not using an iterator to look into the list
            pathTemp = (String)paths.getValue(i);
            WritePATHelement(rafTable4, pathTemp);
       }

        rafTable4.close();
    }
        
    /**
     * Write 05CIDLST file in OMGAUDIO folder. This method should be used while writing the database for generation using a OMGAUDIO folder.
     *
     *@param omgaudioDir Instance of the OMGAUDIO folder where to write the file.
     *@param titles
     *
     *@throws Exception 
     * @author nicolas_cardoso
     */
    public static void write05CIDLST(File omgaudioDir, JSymphonicMap titles) throws Exception{
        File table5 = new File(omgaudioDir + "/05cidlst.dat");
        RandomAccessFile rafTable5;
        
        //Create new file
        table5.createNewFile(); 
        
        // Write data in the file :
        rafTable5 = new RandomAccessFile(table5, "rw"); //Open the file in RAF

        // Header
        WriteTableHeader(rafTable5, "CIDL", 1); //Write table header
        WriteClassDescription(rafTable5, "CILB", 0x20, (titles.isEmpty() ? 0 : titles.maxValue())*0x30 + 0x10); //Write first class description
        WriteClassHeader(rafTable5, "CILB", (titles.isEmpty() ? 0 : titles.maxValue()), 0x30); //Write first class header

        //Sort the titles in the right order
        Iterator it = titles.keySet().iterator();

        // Write elements
        Title titleTemp;
        Boolean hasLSIdrm = false;

        for( int i = 1; i <= titles.maxValue(); i++ ) { //05CIDLST must contains an element per title_ID even if no title actually correspond to some of the IDs !! That's the reason why I'm not using an iterator to look into the list
            titleTemp = (Title)titles.getKey(i);

            if( titleTemp != null ) {
                if(titleTemp instanceof Oma) {
                    Oma oma = (Oma) titleTemp;
                    if(oma.getDrm() == Oma.DRM_LSI){
                        hasLSIdrm = true;
                        rafTable5.write(oma.getDrmKey());
                    }
                }
            }

            if(!hasLSIdrm){
                // If the title is not and OMA LSI DRMed file, fill the element with zeros, even if no title_ID corresponding to this title has been encountered.
                WriteZeros(rafTable5, 0x30);
            }
       }

        rafTable5.close();
    }
        

    /**
     * Write the so called "table header" in the files in the OMGAUDIO folder. This method is used by "write00GRTLST" method for instance.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the header to.
     *@param tableName the name of the table (for instance "TREE" in the "write00GRTLST" method).
     *@param numberOfClasses number of class in this file  (for instance 2 in the "write00GRTLST" method).
     *
     *@author nicolas_cardoso
     */
    public static void WriteTableHeader(RandomAccessFile raf, String tableName, int numberOfClasses) throws java.io.IOException {
        byte[] bytesTableName;
        byte[] constant = {1,1,0,0};
        
        if( tableName.length() != 4 ) { //Control the name of the table (must be 4 characters long)
            logger.severe("Invalid table name while writing config files. Exiting the program.");
            System.exit(-1);
        }

        if( numberOfClasses != 1 && numberOfClasses != 2 ) { //Control the number of classes (must be 1 or 2)
            logger.severe("Invalid number of classes while writing config files. Exiting the program.");
            System.exit(-1);
        }
        
        bytesTableName = tableName.getBytes();
        raf.write(bytesTableName);

        raf.write(constant);
        
        raf.write(numberOfClasses);
        
        WriteZeros(raf, 7);
    }
    
    
    /**
     * Write the so called "table description" in the files in the OMGAUDIO folder. This method is used by "write00GRTLST" method for instance.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the description to.
     *@param className name of the table (for instance "SYSB" in the "write00GRTLST" method).
     *@param startAdress adress where the class starts  (for instance 0x30 in the "write00GRTLST" method).
     *@param length length of the class  (for instance 0x70 in the "write00GRTLST" method).
     *
     *@author nicolas_cardoso
     */
    public static void WriteClassDescription(RandomAccessFile raf, String className, int startAdress, int length) throws java.io.IOException {
        byte[] bytesClassName;
        byte[] bytesLength;
        byte[] bytesStartAdress;
        
        if( className.length() != 4 ) { //Control the name of the class (must be 4 characters long)
            logger.severe("Invalid table name while writing config files. Exiting the program.");
            System.exit(-1);
        }
        
        bytesClassName = className.getBytes();
        raf.write(bytesClassName);
        
        bytesStartAdress = int2bytes(startAdress, 4);
        raf.write(bytesStartAdress);
        
        bytesLength = int2bytes(length, 4);
        raf.write(bytesLength);
        
        WriteZeros(raf, 4);
    }
    
    
    /**
     * Write the so called "class header" in the files in the OMGAUDIO folder. This method is used by "write01TREE01and03GINF01" method for instance.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the header to.
     *@param className name of the table (for instance "GPFB" in the "write01TREE01and03GINF01" method).
     *@param numberOfElement number of element in the class  (for instance the number of album ID in the "write01TREE01and03GINF01" method).
     *@param lengthOfOneElement length of one element of the class  (for instance 0x310 in the "write01TREE01and03GINF01" method).
     *
     *@author nicolas_cardoso*/
    public static void WriteClassHeader(RandomAccessFile raf, String className, int numberOfElement, int lengthOfOneElement) throws java.io.IOException {
        byte[] bytesClassName;
        byte[] bytesNumberOfElement;
        byte[] bytesLengthOfOneElement;
        
        if( className.length() != 4 ) { //Control the name of the class (must be 4 characters long)
            logger.severe("Invalid table name while writing config files. Exiting the program.");
            System.exit(-1);
        }
        
        bytesClassName = className.getBytes();
        raf.write(bytesClassName);

        bytesNumberOfElement = int2bytes(numberOfElement, 2);
        raf.write(bytesNumberOfElement);

        bytesLengthOfOneElement = int2bytes(lengthOfOneElement, 2);
        raf.write(bytesLengthOfOneElement);
        
        WriteZeros(raf, 8);
    }
    
    
    /**
     * Write the so called "class header" in the files in the OMGAUDIO folder. This method is used by "write00GRTLST" method for instance.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the header to.
     *@param className name of the table (for instance "SYSB" in the "write00GRTLST" method).
     *@param numberOfElement number of element in the class  (for instance 0x30 in the "write00GRTLST" method).
     *@param lengthOfOneElement length of one element of the class  (for instance 0x70 in the "write00GRTLST" method).
     *@param classHeaderComplement1 a 4 bytes word to be written in the class header (for instance 0x00000006 in the "write00GRTLST" method).
     *@param classHeaderComplement2 a 4 bytes word to be written in the class header after classHeaderComplement1 (for instance 0x04000000 in the "write00GRTLST" method).
     *
     *@author nicolas_cardoso
     */
    public static void WriteClassHeader(RandomAccessFile raf, String className, int numberOfElement, int lengthOfOneElement, int classHeaderComplement1, int classHeaderComplement2) throws java.io.IOException {
        if( className.length() != 4 ) { //Control the name of the class (must be 4 characters long)
            logger.severe("Invalid table name while writing config files. Exiting the program.");
            System.exit(-1);
        }
        
        raf.write(className.getBytes());
        raf.write(int2bytes(numberOfElement, 2));
        raf.write(int2bytes(lengthOfOneElement, 2));
        raf.write(int2bytes(classHeaderComplement1, 4));
        raf.write(int2bytes(classHeaderComplement2, 4));
    }
    
    
    /**
     * Write a string in a random access file using "UTF-16" encoding.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the string to.
     *@param string the string to be written.
     *@author nicolas_cardoso
     */
    public static void WriteString16(RandomAccessFile raf, String string) throws java.io.IOException {
        if( string.length() == 0 ){
            return;
        }
        
        raf.write(string.getBytes("UTF-16"), 2, string.getBytes("UTF-16").length - 2);
    }

    /**
     * Write a string in a FileOutputStream using "UTF-16" encoding.
     * This class throws exceptions.
     *
     *@param out FileOutputStream instance of the file to write the string to.
     *@param string the string to be written.
     *@author nicolas_cardoso
     */
    public static void WriteString16(FileOutputStream out, String string) throws java.io.IOException {
        if( string.length() == 0 ){
            return;
        }

        out.write(string.getBytes("UTF-16"), 2, string.getBytes("UTF-16").length - 2);
    }
    
    /**
     * Write a given number of zeros in a FileOutputStream.
     * This class throws exceptions.
     *
     *@param out FileOutputStream instance of the file to write the zeros to.
     *@param numberOfZeros number of zeros to be written.
     *
     *@author nicolas_cardoso
     */
    public static void WriteZeros(FileOutputStream out, int numberOfZeros) throws java.io.IOException {
        for( int i = 0; i < numberOfZeros; i++) {
            out.write(0);
        }
    }
    /**
     * Write a given number of zeros in a random access file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the zeros to.
     *@param numberOfZeros number of zeros to be written.
     *
     *@author nicolas_cardoso
     */
    public static void WriteZeros(RandomAccessFile raf, int numberOfZeros) throws java.io.IOException {
        for( int i = 0; i < numberOfZeros; i++) {
            raf.write(0);
        }
    }
    
    
    /**
     * Write an element of the GTLB class in a random access file. This class is used to describ the database strucure in 00GRTLST file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param fileRef reference of the file represented by the current element.
     *@param unknown1 unknow paramater.
     *@param numberOfTag number of EA3 tag in the file represented by the current element (for instance "TPE1").
     *@param tag1 first tag value.
     *@param tag2 second tag value.
     *@param unknown2 unknow paramater.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGTLBelement(RandomAccessFile raf, int fileRef, int unknown1, int numberOfTag, String tag1, String tag2, byte[] unknown2) throws java.io.IOException {
        byte[] bytesFileRef;
        byte[] bytesUnknown1;
        byte[] bytesNumberOfTag;
        byte[] bytesTag1;
        byte[] bytesTag2;

        bytesFileRef = int2bytes(fileRef, 2);
        raf.write(bytesFileRef);

        bytesUnknown1 = int2bytes(unknown1, 2);
        raf.write(bytesUnknown1);
       
        WriteZeros(raf, 12);
        
        bytesNumberOfTag = int2bytes(numberOfTag, 2);
        raf.write(bytesNumberOfTag);

        WriteZeros(raf, 2);

        if( numberOfTag > 0) {
            bytesTag1 = tag1.getBytes();
            raf.write(bytesTag1);
            
            // Control that the right number of bytes have been written
            if( tag1.length() < 4 ){
                WriteZeros(raf, 4 - tag1.length());
            }
        }
        else {
            WriteZeros(raf, 4);
        }

        if( numberOfTag > 1 ) {
            bytesTag2 = tag2.getBytes();
            raf.write(bytesTag2);
            
            // Control that the right number of bytes have been written
            if( tag2.length() < 4 ){
                WriteZeros(raf, 4 - tag2.length());
            }
        }
        else {
            WriteZeros(raf, 4);
        }
        
        WriteZeros(raf, 20);

        raf.write(unknown2);
        
        //Complete the end of the element with zeros
        WriteZeros(raf, 0x50 - 2*2 - 12 - 2*2 - 4*2 - 20 - unknown2.length); //Complete element with zeros
    }
    
    
    /**
     * Write an element of the GPLB class in a random access file. This method is used to order the title (referenced by their ID) and uses the "1"constant.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param itemIdIn03GINFXX ID of the title in the 03GINFXX files.
     *@param titleIdInTPLBlist ID of the title in the TPLB list.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGPLBelement(RandomAccessFile raf, int itemIdIn03GINFXX, int titleIdInTPLBlist) throws java.io.IOException {
        byte[] constant = {1,0};

        raf.write(int2bytes(itemIdIn03GINFXX, 2));
        raf.write(constant);
        raf.write(int2bytes(titleIdInTPLBlist, 2));
        WriteZeros(raf, 2);
    }
    
    
    /**
     * Write an element of the GPLB class in a random access file. This method is used to order the title (referenced by their ID) and uses the "2"constant.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param itemIdIn03GINFXX ID of the title in the 03GINFXX files.
     *@param titleIdInTPLBlist ID of the title in the TPLB list.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGPLBelement2(RandomAccessFile raf, int itemIdIn03GINFXX, int titleIdInTPLBlist) throws java.io.IOException {
        byte[] constant = {2,0};

        raf.write(int2bytes(itemIdIn03GINFXX, 2));
        raf.write(constant);
        raf.write(int2bytes(titleIdInTPLBlist, 2));
        WriteZeros(raf, 2);
    }

    /**
     * Write an element of the GPLB class in a random access file. This method is used to order the title (referenced by their ID) and uses the "0"constant.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param itemIdIn03GINFXX ID of the title in the 03GINFXX files.
     *@param titleIdInTPLBlist ID of the title in the TPLB list.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGPLBelement0(RandomAccessFile raf, int itemIdIn03GINFXX, int titleIdInTPLBlist) throws java.io.IOException {
        byte[] constant = {0,0};

        raf.write(int2bytes(itemIdIn03GINFXX, 2));
        raf.write(constant);
        raf.write(int2bytes(titleIdInTPLBlist, 2));
        WriteZeros(raf, 2);
    }
    
    
    /**
     * Write an element of the GPFB class in a random access file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param albumKey album key of the title represented by the current element.
     *@param albumName album name of the title represented by the current element.
     *@param artistName artist name of the title represented by the current element.
     *@param genre genre of the title represented by the current element.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGPFBelement(RandomAccessFile raf, int albumKey, String albumName, String artistName, String genre) throws java.io.IOException {
        byte[] constant1 = {0,6,0,-128}; //-128 as a signed byte equals 128 as an unsigned byte equal 0x80 in hex
        byte[] constant2 = {0,2};
        String tag1 = "TIT2";
        String tag2 = "TPE1";
        String tag3 = "TCON";
        String tag4 = "TSOP";
        String tag5 = "PICP";
        String tag6 = "PIC0";
        
        //Element header
        WriteZeros(raf, 8);
        raf.write(int2bytes(albumKey, 4));
        raf.write(constant1);
        
        //Fist sub-element
        raf.write(tag1.getBytes());
        raf.write(constant2);
        WriteString16(raf, albumName);
        WriteZeros(raf, 0x80 - 4 - 2 - (albumName.length()*2) );

        //Second sub-element
        raf.write(tag2.getBytes());
        raf.write(constant2);
        WriteString16(raf, artistName);
        WriteZeros(raf, 0x80 - 4 - 2 - (artistName.length()*2) );
        
        //Third sub-element
        raf.write(tag3.getBytes());
        raf.write(constant2);
        WriteString16(raf, genre);
        WriteZeros(raf, 0x80 - 4 - 2 - (genre.length()*2) );
        
        //Fourth sub-element
        raf.write(tag4.getBytes());
        raf.write(constant2);
        WriteZeros(raf, 0x80 - 4 - 2 );

        //Fifth sub-element
        raf.write(tag5.getBytes());
        raf.write(constant2);
        WriteZeros(raf, 0x80 - 4 - 2 );

        //Sixth sub-element
        raf.write(tag6.getBytes());
        raf.write(constant2);
        WriteZeros(raf, 0x80 - 4 - 2 );
    }
    
    
    /**
     * Write an element of the GPFB class in a random access file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param albumKey album key of the title represented by the current element.
     *@param artistName artist name of the title represented by the current element.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGPFBelement(RandomAccessFile raf, int albumKey, String artistName) throws java.io.IOException {
        byte[] constant1 = {0,1,0,-128}; //-128 as a signed byte equals 128 as an unsigned byte equal 0x80 in hex
        byte[] constant2 = {0,2};
        String tag1 = "TIT2";
        
        //Element header
        WriteZeros(raf, 8);
        raf.write(int2bytes(albumKey, 4));
        raf.write(constant1);
        
        //Fist sub-element
        raf.write(tag1.getBytes());
        raf.write(constant2);
        WriteString16(raf, artistName);
        WriteZeros(raf, 0x80 - 4 - 2 - (artistName.length()*2) );
    }
    
    
    /**
     * Write an element of the GPFB class in a random access file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param elementName1 first element name of the title represented by the current element.
     *@param elementName2 second element name of the title represented by the current element.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGPFBelement(RandomAccessFile raf, int albumKey, String elementName1, String elementName2) throws java.io.IOException {
        byte[] constant1 = {0,2,0,-128}; //-128 as a signed byte equals 128 as an unsigned byte equal 0x80 in hex
        byte[] constant2 = {0,2};
        String tag1 = "TIT2";
        String tag2 = "XSOT";
        
        //Element header
        WriteZeros(raf, 8);
        raf.write(int2bytes(albumKey, 4));
        raf.write(constant1);
        
        //Fist sub-element
        raf.write(tag1.getBytes());
        raf.write(constant2);
        WriteString16(raf, elementName1);
        WriteZeros(raf, 0x80 - 4 - 2 - (elementName1.length()*2) );
        
        //Second sub-element
        raf.write(tag2.getBytes());
        raf.write(constant2);
        WriteString16(raf, elementName2);
        WriteZeros(raf, 0x80 - 4 - 2 - (elementName2.length()*2) );
    }
    
    
    /**
     * Write an element of the GPFB class in a random access file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param key first a key of the title represented by the current element.
     *@param unknown a name of the title represented by the current element.
     *
     *@author nicolas_cardoso
     */
    public static void WriteGTFBelement(RandomAccessFile raf, int key, String unknown) throws java.io.IOException {
        byte[] constant1 = {0,1,0,-128}; //-128 as a signed byte equals 128 as an unsigned byte equal 0x80 in hex
        byte[] constant2 = {0,2};
        String tag = "TIT2";
        
        WriteZeros(raf, 8);
        raf.write(int2bytes(key, 4));
        raf.write(constant1);
        raf.write(tag.getBytes());
        raf.write(constant2);
        WriteString16(raf, unknown);
        
        WriteZeros(raf, 0x80 - 4 - 2 - (unknown.length()*2) );
    }
    
    
    /**
     * Write an element of the CNFB class in a random access file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     *@param magicKey the so called magic key number corresponding to the current walkman.
     *@param titleKey title key of the title represented by the current element.
     *@param titleName title name of the title represented by the current element.
     *@param artistName artist name of the title represented by the current element.
     *@param albumName album name of the title represented by the current element.
     *@param genreName genre of the title represented by the current element.
     *
     *@author nicolas_cardoso
     */
    public static void WriteCNFBelement(RandomAccessFile raf, Title title, boolean gotKey) throws java.io.IOException {
        byte[] constant1 = {0,5,0,-128}; //-128 as a signed byte equals 128 as an unsigned byte equal 0x80 in hex
        byte[] constant2 = {0,2};
        byte[] buffer2;
        String tag1 = "TIT2";
        String tag2 = "TPE1";
        String tag3 = "TALB";
        String tag4 = "TCON";
        String tag5 = "TSOP";

        if( title == null ){
            // title can be null since title ID may not refer to any existing file. In the case, fill in the place with zeros and fake an empty title
            WriteZeros(raf, 12); // Write 12 zeros
            title = new Title(); // Fake a title
            title.setTitleName("");
            title.setArtistName("");
            title.setAlbumName("");
            title.setGenre("");
        }
        else{
            // Title exist, we need to wrap it if it's not already the case
            Oma titleWrapped;
            if(title instanceof Oma){
                titleWrapped = (Oma) title;
            }
            else{
                titleWrapped = new Oma(title);
            }

            //Element header
            WriteZeros(raf, 2); // Write 2 zeros
            // Determine the protection
            if(gotKey){
                if(titleWrapped.getFormatWrapped() == Title.OMA){
                    buffer2 = new byte[]{0,1}; // LSI DRM ATRAC
                }
                else{
                    buffer2 = new byte[]{(byte)0xFF,(byte)0xFE}; // Encrypt MP3
                }
            }
            else{
                buffer2 = new byte[]{(byte)0xFF,(byte)0xFF}; // No protection
            }
            raf.write(buffer2); // Write protection type (2 bytes long)
            raf.write(titleWrapped.getFileProperties()); // Write file properties (4 bytes long)
            raf.write(DataBaseOmgaudioToolBox.int2bytes(title.getLength(), 4)); // Write title_key (4 bytes long)
        }
        raf.write(constant1);
        
        //Fist sub-element
        raf.write(tag1.getBytes());
        raf.write(constant2);
        WriteString16(raf, title.getTitle());
        WriteZeros(raf, 0x80 - 4 - 2 - (title.getTitle().length()*2) );
        
        //Second sub-element
        raf.write(tag2.getBytes());
        raf.write(constant2);
        WriteString16(raf, title.getArtist());
        WriteZeros(raf, 0x80 - 4 - 2 - (title.getArtist().length()*2) );

        //Third sub-element
        raf.write(tag3.getBytes());
        raf.write(constant2);
        WriteString16(raf, title.getAlbum());
        WriteZeros(raf, 0x80 - 4 - 2 - (title.getAlbum().length()*2) );
        
        //Fourth sub-element
        raf.write(tag4.getBytes());
        raf.write(constant2);
        WriteString16(raf, title.getGenre());
        WriteZeros(raf, 0x80 - 4 - 2 - (title.getGenre().length()*2) );
        
        //Fifth sub-element
        raf.write(tag5.getBytes());
        raf.write(constant2);
        WriteZeros(raf, 0x80 - 4 - 2);
    }

    /**
     * Write an element of the PALT class in a random access file.
     * This class throws exceptions.
     *
     *@param raf Random access file instance of the file to write the element to.
     * @param path The string to be written
     * @throws java.io.IOException
     *
     * @author nicolas_cardoso
     */
    public static void WritePATHelement(RandomAccessFile raf, String path) throws java.io.IOException {
        byte[] constant = {0,2};
        String tag1 = "TIT2";

        // Check path
        if(path == null){
            path = new String("");
        }
        raf.write(tag1.getBytes());
        raf.write(constant);
        WriteString16(raf, path);
        WriteZeros(raf, 0x150 - 4 - 2 - (path.length()*2) );
    }

    /**
     * Tells if an array of bytes is full with zeros.
     *
     *@param bytes the array of bytes to test
     *@return true for "yes" and false for "no"
     *
     *@author nicolas_cardoso
     */
    public static boolean isZero(byte[] bytes){
        int i = 0;
        boolean ret = true;
        
        while(i < bytes.length){
            if(bytes[i] == 0){
                ret = ret && true;
            }
            else{
                ret = false;
            }
            i++;
        }
        return ret;
    }
    
    
    /**
     * Converts an array of bytes to an int. For example, if the array is : [1;2;3;4], the corresponding int will be "1234".
     *
     *@param bytes The array of bytes to convert
     *
     *@return The converted number.
     *
     *@author nicolas_cardoso
     */
    public static int bytes2int(byte[] bytes){
        int i = bytes.length - 1;
        int ret = 0;
        
        while(i >= 0){
            int byteValue = bytes[i];
            if(byteValue < 0) {byteValue+= 256;} // Convert it to a positive number if it is not
            ret += byteValue*Math.pow(2, (bytes.length - (i+1))*8);
            i--;
        }
        return ret;
    }
    
    /**
     * Converts an integer into an array of bytes.
     *
     *@param integer The integer to convert.
     *
     *@return The array of bytes representating the integer.
     *
     *@author nicolas_cardoso
     */
    public static byte[] int2bytes(Integer integer){
        int numberOfDigit;
        Integer digit;
        byte[] bytesToReturn;
        
        // Search the number of digit in the integer
        numberOfDigit = (int)((Integer.toBinaryString(integer).length() / 8.0 ) + 0.99); // 0.99 is added to round up the number (can't add 1 beacause in some case, the right number is found, so added 1 make it false althought it's not)
        
        // Create the array of bytes
        bytesToReturn = new byte[numberOfDigit];
        
        // Fill in the array
        for( int i = numberOfDigit - 1; i >= 0; i-- ) {
            digit = integer % 256;
            integer = integer / 256;
            bytesToReturn[i] = (byte) digit.byteValue();
        }
        
        return bytesToReturn;
    }    
    
    
    /**
     * Converts an integer into an array of bytes of a given length.
     *
     *@param integer The integer to convert.
     *@param bytesLength The desired length of the array.
     *
     *@return The array of bytes representating the integer.
     *
     *@author nicolas_cardoso
     */    
    public static byte[] int2bytes(int integer, int bytesLength){
        int numberOfDigit;
        Integer digit;
        byte[] bytesToReturn;
        
        // Search the number of digit in the integer
        if(integer < 0) {integer+= 256;} // Convert it to a positive number if it is not
        numberOfDigit = (int)((Integer.toBinaryString(integer).length() / 8.0 ) + 0.99); // 0.99 is added to round up the number (can't add 1 beacause in some case, the right number is found, so added 1 make it false althought it's not)

        // Create the array of bytes
        bytesToReturn = new byte[bytesLength];
        
        if( numberOfDigit > bytesLength ) {
            logger.severe("Impossible to fit the integer in that bytesLength in method 'int2bytes'. Number is: "+integer+" and the number of bytes wanted is: "+bytesLength+". Existing program.");
            System.exit(-1);
        }
        
        // Fill in the integer in the array
        for( int i = bytesLength - 1; i >= bytesLength - numberOfDigit; i-- ) {
            digit = integer % 256;
            integer = integer / 256;
            bytesToReturn[i] = digit.byteValue();
        }

        // Fill in the rest of the array with zeros
        for( int i = bytesLength - numberOfDigit - 1; i >= 0; i-- ) {
            bytesToReturn[i] = 0;
        }
        
        return bytesToReturn;
    }   
    
    
    /**
     * Converts an long into an array of bytes of a given length.
     *
     *@param longnb The long number to convert.
     *@param bytesLength The desired length of the array.
     *
     *@return The array of bytes representating the long number.
     *
     *@author nicolas_cardoso
     */    
    public static byte[] long2bytes(long longnb, int bytesLength){
        int numberOfDigit;
        Long digit;
        byte[] bytesToReturn;
        
        // Search the number of digit in the integer
        if(longnb < 0) {longnb+= 256;} // Convert it to a positive number if it is not
        numberOfDigit = (int)((Long.toBinaryString(longnb).length() / 8.0 ) + 0.99); // 0.99 is added to round up the number (can't add 1 beacause in some case, the right number is found, so added 1 make it false althought it's not)

        // Create the array of bytes
        bytesToReturn = new byte[bytesLength];
        
        if( numberOfDigit > bytesLength ) {
            logger.severe("Impossible to fit the integer in that bytesLength in method 'long2bytes'. Number is: "+longnb+" and the number of bytes wanted is: "+bytesLength+". Existing program.");
            System.exit(-1);
        }
        
        // Fill in the integer in the array
        for( int i = bytesLength - 1; i >= bytesLength - numberOfDigit; i-- ) {
            digit = longnb % 256;
            longnb = longnb / 256;
            bytesToReturn[i] = digit.byteValue();
        }

        // Fill in the rest of the array with zeros
        for( int i = bytesLength - numberOfDigit - 1; i >= 0; i-- ) {
            bytesToReturn[i] = 0;
        }
        
        return bytesToReturn;
    }       

    /**
     * This method suppress extra "0x0" at the end of an array of bytes.
     *
     * @param bytesArray with extra "0x0"
     * @return the same array, without the extra "0x0"
     */
    public static byte[] rtrimZeros(byte[] bytesArray){
        int lastIndex = 0;

        // Check that the array is valid, if not, return it
        if(bytesArray.length <= 0){
            return bytesArray;
        }

        // Search the last non-zero byte
        for(int i=0; i < bytesArray.length; i++){
            if(bytesArray[i] != 0){
                lastIndex = i;
            }
        }

        // If the last non-zero bytes is not at the end of the array, create a new array without the zeros at the end
        if(lastIndex != bytesArray.length-1){
            byte[] bytesArrayRet = new byte[lastIndex+1]; // Create the new array to be returned
            // Fill in the new arrat
            for(int i=0; i < bytesArrayRet.length; i++){
                bytesArrayRet[i] = bytesArray[i];
            }
            return bytesArrayRet;
        }
        else{
            return bytesArray;
        }
    }


    /**
     * Sort a map of title according to their artist name then title name.
     *
     *@param titles map of the titles to be sorted.
     *
     *@return sorted list of titles.
     *
     *@author nicolas_cardoso
     */
    public static List sortByArtistTitle(Map titles){
        List listToReturn = new ArrayList(titles.keySet());
        Collections.sort(listToReturn, new ArtistTitleComparator());

        return listToReturn;
    }


    /**
     * Sort a map of title according to their genre then title name.
     *
     *@param titles map of the titles to be sorted.
     *
     *@return sorted list of titles.
     *
     *@author nicolas_cardoso
     */
    public static List sortByGenreTitle(Map titles){
        List listToReturn = new ArrayList(titles.keySet());
        Collections.sort(listToReturn, new GenreTitleComparator());

        return listToReturn;
    }


    /**
     * Sort a map of title according to their genre then artist name then album name.
     *
     *@param titles map of the titles to be sorted.
     *
     *@return sorted list of titles.
     *
     *@author nicolas_cardoso
     */
    public static List sortByGenreArtistAlbumTitle(Map titles){
        List listToReturn = new ArrayList(titles.keySet());
        Collections.sort(listToReturn, new GenreArtistAlbumTitleComparator());

        return listToReturn;
    }


    /**
     * Sort a map of title according to their album name then title number.
     *
     *@param titles map of the titles to be sorted.
     *
     *@return sorted list of titles.
     *
     *@author nicolas_cardoso
     */
    public static List sortByAlbumTitleNumber(Map titles){
        List listToReturn = new ArrayList(titles.keySet());
        Collections.sort(listToReturn, new AlbumTitleNumberComparator());
        
        return listToReturn;
    }


    /**
     * Sort a map of title according to their artist name then album name then title number.
     *
     *@param titles map of the titles to be sorted.
     *
     *@return sorted list of titles.
     *
     *@author nicolas_cardoso
     */
    public static List sortByArtistAlbumTitleNumber(Map titles){
        List listToReturn = new ArrayList(titles.keySet());
        Collections.sort(listToReturn, new ArtistAlbumTitleNumberComparator());
        
        return listToReturn;
    }

    
    /**
     * Sort a map of title according to their title ID.
     *
     *@param titles map of the titles to be sorted.
     *
     *@return sorted list of titles.
     *
     *@author nicolas_cardoso
     */
    public static List sortByTitleId(Map titles){
        List listToReturn = new ArrayList(titles.keySet());
        Collections.sort(listToReturn, new TitleIdComparator(titles));

        return listToReturn;
    }
  
    /**
     *Converts the number represented by some value in an array of bytes to an int. For example, if the array is : ["1";"2";"3";"4"]=[49;50;51;52], the corresponding int will be "1234".
     *
     *@param bytes The array of bytes to convert
     *
     *@return The converted number.
     *
     *@author nicolas_cardoso
     */
    public static int charBytes2int(byte[] bytes){
        int i = bytes.length - 1;
        int ret = 0;
        byte val;
        int pow = 0;
        
        while(i >= 0){
            switch(bytes[i]){
                case 48:
                    val = 0;
                    break;
                case 49:
                    val = 1;
                    break;
                case 50:
                    val = 2;
                    break;
                case 51:
                    val = 3;
                    break;
                case 52:
                    val = 4;
                    break;
                case 53:
                    val = 5;
                    break;
                case 54:
                    val = 6;
                    break;
                case 55:
                    val = 7;
                    break;
                case 56:
                    val = 8;
                    break;
                case 57:
                    val = 9;
                    break;
                default:
                    val = -1;
            }

            if(val >= 0){
                ret += val*Math.pow(10, pow);
                pow++;
            }
            i--;
        }
        return ret;
    }

    /**
     * Convert a bitrate given as an integer corresponding to the table given at "http://javamusictag.sourceforge.net/api/index.html" (Class MP3File) into a 10-based integer (human understandable) (like 128, 256, 320...).
     *
     * @param BitRateHuman in "Index" form.
     * @param layer Layer of the file, 1 stands for LAYER-III, 2 for LAYER-II and 3 for LAYER-I
     * @param mpegVersion Mpeg version of the file, 3 stands for MPEG-1, 2 for MPEG-2 and 0 for MPEG-3 (or MPEG-2.5). Here, MPEG-3 (or MPEG-2.5) is not handled.
     * @return Bitrate in "Human" form.
     */
    public static int indexBitRateToHuman(int bitRateIndex, byte layer, byte mpegVersion) {
        // First, fill in the HashMap
        int bitRateCorrespondance[][][] = { // [mpeg][layer][bitrate]
            {// mpeg2
                {// mpeg2 - layer3
                    0,// Invalid value
                    8,// 8kpbs
                    16,// 16kpbs
                    24,// 24kpbs
                    32,// 32kpbs
                    64,// 64kpbs
                    80,// 80kpbs
                    56,// 56kpbs
                    64,// 64kpbs
                    128,// 128kpbs
                    160,// 160kpbs
                    112,// 112kpbs
                    128,// 128kpbs
                    256,// 256kpbs
                    320,// 320kpbs
                    0,// Invalid value
                },
                {// mpeg2 - layer2
                    0,// Invalid value
                    32,// 32kpbs
                    48,// 48kpbs
                    56,// 56kpbs
                    64,// 60kpbs
                    80,// 80kpbs
                    96,// 96kpbs
                    112,// 112kpbs
                    128,// 128kpbs
                    160,// 160kpbs
                    192,// 192kpbs
                    224,// 224kpbs
                    256,// 256kpbs
                    320,// 320kpbs
                    384,// 384kpbs
                    0,// Invalid value
                },
                {// mpeg2 - layer1
                    0,// Invalid value
                    32,// 32kpbs
                    64,// 64kpbs
                    96,// 96kpbs
                    128,// 128kpbs
                    160,// 160kpbs
                    192,// 192kpbs
                    224,// 224kpbs
                    256,// 256kpbs
                    288,// 288kpbs
                    320,// 320kpbs
                    352,// 352kpbs
                    384,// 384kpbs
                    416,// 416kpbs
                    448,// 448kpbs
                    0,// Invalid value
                }
            },
            {// mpeg1
                {// mpeg1 - layer3
                    0,// Invalid value
                    32,// 32kpbs
                    40,// 40kpbs
                    48,// 48kpbs
                    56,// 56kpbs
                    64,// 64kpbs
                    80,// 80kpbs
                    96,// 96kpbs
                    112,// 112kpbs
                    128,// 128kpbs
                    160,// 160kpbs
                    192,// 192kpbs
                    224,// 224kpbs
                    256,// 256kpbs
                    320,// 320kpbs
                    0,// Invalid value
                },
                {// mpeg1 - layer2
                    0,// Invalid value
                    32,// 32kpbs
                    48,// 48kpbs
                    56,// 56kpbs
                    64,// 64kpbs
                    80,// 80kpbs
                    96,// 96kpbs
                    112,// 112kpbs
                    128,// 128kpbs
                    160,// 160kpbs
                    192,// 192kpbs
                    224,// 224kpbs
                    256,// 256kpbs
                    320,// 320kpbs
                    384,// 384kpbs
                    0,// Invalid value
                },
                {// mpeg1 - layer1
                    0,// Invalid value
                    32,// 32kpbs
                    64,// 64kpbs
                    96,// 96kpbs
                    128,// 128kpbs
                    160,// 160kpbs
                    192,// 192kpbs
                    224,// 224kpbs
                    256,// 256kpbs
                    288,// 288kpbs
                    320,// 320kpbs
                    352,// 352kpbs
                    384,// 384kpbs
                    416,// 416kpbs
                    448,// 448kpbs
                    0// Invalid value
                }
            }};

        // From the index, the layer and the mpegVersion, we can look into the table the int we want
        //mpegVersion is 3 for version 1 and 2 for version 2, so we substract 2 to this value to have 1 for version 1 or 0 for version 2
        //layer is 3 for version 1, 2 for version 2 and 1 for version 3 so we substract 1 to this value to have 2 for version 1, 1 for version 2 or 0 for version 3
        return bitRateCorrespondance[mpegVersion -2][layer - 1][bitRateIndex];
    }
    /**
     * Convert a bitrate given in a 10-based integer (human understandable) (like 128, 256, 320...) in an integer corresponding to the table given at "http://javamusictag.sourceforge.net/api/index.html" (Class MP3File)
     *
     * @param BitRateHuman in "Human" form.
     * @param layer Layer of the file, 1 stands for LAYER-III, 2 for LAYER-II and 3 for LAYER-I
     * @param mpegVersion Mpeg version of the file, 3 stands for MPEG-1, 2 for MPEG-2 and 0 for MPEG-3 (or MPEG-2.5). Here, MPEG-3 (or MPEG-2.5) is not handled.
     * @return Bitrate in "Index" form.
     */
    public static int humanBitRateToIndex(int bitRateHuman, byte layer, byte mpegVersion) {
        // First, fill in the HashMap
        HashMap bitRateTable = new HashMap();
        bitRateTable.put(8, 0);
        bitRateTable.put(16, 1);
        bitRateTable.put(24, 2);
        bitRateTable.put(32, 3);
        bitRateTable.put(40, 4);
        bitRateTable.put(48, 5);
        bitRateTable.put(56, 6);
        bitRateTable.put(64, 7);
        bitRateTable.put(80, 8);
        bitRateTable.put(96, 9);
        bitRateTable.put(112, 10);
        bitRateTable.put(128, 11);
        bitRateTable.put(144, 12);
        bitRateTable.put(160, 13);
        bitRateTable.put(176, 14);
        bitRateTable.put(192, 15);
        bitRateTable.put(224, 16);
        bitRateTable.put(256, 17);
        bitRateTable.put(288, 18);
        bitRateTable.put(320, 19);
        bitRateTable.put(352, 20);
        bitRateTable.put(384, 21);
        bitRateTable.put(416, 22);
        bitRateTable.put(448, 23);

        // Check if the bitrate given exists in the table (VBR file could have any bitrate)
        if(!bitRateTable.containsKey(bitRateHuman)){
            // If the bitrate is not found, use the closer bitrate
            if(bitRateHuman < 12){ bitRateHuman = 8;}
            else if(bitRateHuman < 20){ bitRateHuman = 16;}
            else if(bitRateHuman < 28){ bitRateHuman = 24;}
            else if(bitRateHuman < 36){ bitRateHuman = 32;}
            else if(bitRateHuman < 44){ bitRateHuman = 40;}
            else if(bitRateHuman < 52){ bitRateHuman = 48;}
            else if(bitRateHuman < 60){ bitRateHuman = 56;}
            else if(bitRateHuman < 72){ bitRateHuman = 64;}
            else if(bitRateHuman < 88){ bitRateHuman = 80;}
            else if(bitRateHuman < 104){ bitRateHuman = 96;}
            else if(bitRateHuman < 120){ bitRateHuman = 112;}
            else if(bitRateHuman < 136){ bitRateHuman = 128;}
            else if(bitRateHuman < 152){ bitRateHuman = 144;}
            else if(bitRateHuman < 168){ bitRateHuman = 160;}
            else if(bitRateHuman < 184){ bitRateHuman = 176;}
            else if(bitRateHuman < 200){ bitRateHuman = 192;}
            else if(bitRateHuman < 240){ bitRateHuman = 224;}
            else if(bitRateHuman < 272){ bitRateHuman = 256;}
            else if(bitRateHuman < 304){ bitRateHuman = 288;}
            else if(bitRateHuman < 336){ bitRateHuman = 320;}
            else if(bitRateHuman < 368){ bitRateHuman = 352;}
            else if(bitRateHuman < 400){ bitRateHuman = 384;}
            else if(bitRateHuman < 432){ bitRateHuman = 416;}
            else{ bitRateHuman = 448;}
        }

        // Define then the table of correspondance,
        int bitRateCorrespondance[][][] = { // [mpeg][layer][bitrate]
            {// mpeg2
                {// mpeg2 - layer3
                    1,// 8kpbs
                    2,// 16kpbs
                    3,// 24kpbs
                    4,// 32kpbs
                    5,// 40kpbs
                    6,// 48kpbs
                    7,// 56kpbs
                    8,// 64kpbs
                    9,// 80kpbs
                    10,// 96kpbs
                    11,// 112kpbs
                    12,// 128kpbs
                    13,// 144kpbs
                    14,// 160kpbs
                    0,// 176kpbs INVALID for this mpeg version and layer
                    0,// 192kpbs INVALID for this mpeg version and layer
                    0,// 224kpbs INVALID for this mpeg version and layer
                    0,// 256kpbs INVALID for this mpeg version and layer
                    0,// 288kpbs INVALID for this mpeg version and layer
                    0,// 320kpbs INVALID for this mpeg version and layer
                    0,// 352kpbs INVALID for this mpeg version and layer
                    0,// 384kpbs INVALID for this mpeg version and layer
                    0,// 416kpbs INVALID for this mpeg version and layer
                    0// 448kpbs INVALID for this mpeg version and layer
                },
                {// mpeg2 - layer2
                    1,// 8kpbs
                    2,// 16kpbs
                    3,// 24kpbs
                    4,// 32kpbs
                    5,// 40kpbs
                    6,// 48kpbs
                    7,// 56kpbs
                    8,// 64kpbs
                    9,// 80kpbs
                    10,// 96kpbs
                    11,// 112kpbs
                    12,// 128kpbs
                    13,// 144kpbs
                    14,// 160kpbs
                    0,// 176kpbs INVALID for this mpeg version and layer
                    0,// 192kpbs INVALID for this mpeg version and layer
                    0,// 224kpbs INVALID for this mpeg version and layer
                    0,// 256kpbs INVALID for this mpeg version and layer
                    0,// 288kpbs INVALID for this mpeg version and layer
                    0,// 320kpbs INVALID for this mpeg version and layer
                    0,// 352kpbs INVALID for this mpeg version and layer
                    0,// 384kpbs INVALID for this mpeg version and layer
                    0,// 416kpbs INVALID for this mpeg version and layer
                    0// 448kpbs INVALID for this mpeg version and layer
                },
                {// mpeg2 - layer1
                    0,// 8kpbs INVALID for this mpeg version and layer
                    0,// 16kpbs INVALID for this mpeg version and layer
                    0,// 24kpbs INVALID for this mpeg version and layer
                    1,// 32kpbs
                    0,// 40kpbs INVALID for this mpeg version and layer
                    2,// 48kpbs
                    3,// 56kpbs
                    4,// 64kpbs
                    5,// 80kpbs
                    6,// 96kpbs
                    7,// 112kpbs
                    8,// 128kpbs
                    9,// 144kpbs
                    10,// 160kpbs
                    11,// 176kpbs
                    12,// 192kpbs
                    13,// 224kpbs
                    14,// 256kpbs
                    0,// 288kpbs INVALID for this mpeg version and layer
                    0,// 320kpbs INVALID for this mpeg version and layer
                    0,// 352kpbs INVALID for this mpeg version and layer
                    0,// 384kpbs INVALID for this mpeg version and layer
                    0,// 416kpbs INVALID for this mpeg version and layer
                    0// 448kpbs INVALID for this mpeg version and layer
                }
            },
            {// mpeg1
                {// mpeg1 - layer3
                    0,// 8kpbs INVALID for this mpeg version and layer
                    0,// 16kpbs INVALID for this mpeg version and layer
                    0,// 24kpbs INVALID for this mpeg version and layer
                    1,// 32kpbs
                    2,// 40kpbs
                    3,// 48kpbs
                    4,// 56kpbs
                    5,// 64kpbs
                    6,// 80kpbs
                    7,// 96kpbs
                    8,// 112kpbs
                    9,// 128kpbs
                    0,// 144kpbs INVALID for this mpeg version and layer
                    10,// 160kpbs
                    0,// 176kpbs INVALID for this mpeg version and layer
                    11,// 192kpbs
                    12,// 224kpbs
                    13,// 256kpbs
                    0,// 288kpbs INVALID for this mpeg version and layer
                    14,// 320kpbs
                    0,// 352kpbs INVALID for this mpeg version and layer
                    0,// 384kpbs INVALID for this mpeg version and layer
                    0,// 416kpbs INVALID for this mpeg version and layer
                    0// 448kpbs INVALID for this mpeg version and layer
                },
                {// mpeg1 - layer2
                    0,// 8kpbs INVALID for this mpeg version and layer
                    0,// 16kpbs INVALID for this mpeg version and layer
                    0,// 24kpbs INVALID for this mpeg version and layer
                    1,// 32kpbs
                    0,// 40kpbs INVALID for this mpeg version and layer
                    2,// 48kpbs
                    3,// 56kpbs
                    4,// 64kpbs
                    5,// 80kpbs
                    6,// 96kpbs
                    7,// 112kpbs
                    8,// 128kpbs
                    0,// 144kpbs INVALID for this mpeg version and layer
                    9,// 160kpbs
                    0,// 176kpbs INVALID for this mpeg version and layer
                    10,// 192kpbs
                    11,// 224kpbs
                    12,// 256kpbs
                    0,// 288kpbs INVALID for this mpeg version and layer
                    13,// 320kpbs
                    0,// 352kpbs INVALID for this mpeg version and layer
                    14,// 384kpbs
                    0,// 416kpbs INVALID for this mpeg version and layer
                    0// 448kpbs INVALID for this mpeg version and layer
                },
                {// mpeg1 - layer1
                    0,// 8kpbs INVALID for this mpeg version and layer
                    0,// 16kpbs INVALID for this mpeg version and layer
                    0,// 24kpbs INVALID for this mpeg version and layer
                    1,// 32kpbs
                    0,// 40kpbs INVALID for this mpeg version and layer
                    0,// 48kpbs INVALID for this mpeg version and layer
                    0,// 56kpbs INVALID for this mpeg version and layer
                    2,// 64kpbs
                    0,// 80kpbs INVALID for this mpeg version and layer
                    3,// 96kpbs
                    0,// 112kpbs INVALID for this mpeg version and layer
                    4,// 128kpbs
                    0,// 144kpbs INVALID for this mpeg version and layer
                    5,// 160kpbs
                    0,// 176kpbs INVALID for this mpeg version and layer
                    6,// 192kpbs
                    7,// 224kpbs
                    8,// 256kpbs
                    9,// 288kpbs
                    10,// 320kpbs
                    11,// 352kpbs
                    12,// 384kpbs
                    13,// 416kpbs
                    14// 448kpbs
                }
            }};

        // Then, we get the index of the bit rate from the first table
        Integer index = (Integer)bitRateTable.get(bitRateHuman);
        // And, from the index, the layer and the mpegVersion, we can look into the second table the int we want
        //mpegVersion is 3 for version 1 and 2 for version 2, so we substract 2 to this value to have 1 for version 1 or 0 for version 2
        //layer is 3 for version 1, 2 for version 2 and 1 for version 3 so we substract 1 to this value to have 2 for version 1, 1 for version 2 or 0 for version 3
        return bitRateCorrespondance[mpegVersion -2][layer - 1][index];
    }
}
/* OTHER CLASSES */
class ArtistTitleComparator implements Comparator<Title> {
    public int compare(Title arg0, Title arg1) {
        String a = arg0.getArtist();
        String b = arg1.getArtist();
        if(a.equalsIgnoreCase(b)) {
            a = arg0.getTitle();
            b = arg1.getTitle();
        }
        return a.compareToIgnoreCase(b);
    }
}

class GenreTitleComparator implements Comparator<Title> {

    public int compare(Title arg0, Title arg1) {
        String a = arg0.getGenre();
        String b = arg1.getGenre();
        if(a.equalsIgnoreCase(b)) {
            a = arg0.getTitle();
            b = arg1.getTitle();
        }
        return a.compareToIgnoreCase(b);
    }
}

class GenreArtistAlbumTitleComparator implements Comparator<Title> {

    public int compare(Title arg0, Title arg1) {
        String a = arg0.getGenre();
        String b = arg1.getGenre();
        if(a.equalsIgnoreCase(b)) {
            a = arg0.getArtist();
            b = arg1.getArtist();
            if(a.equalsIgnoreCase(b)) {
                a = arg0.getAlbum();
                b = arg1.getAlbum();
                if(a.equalsIgnoreCase(b)) {
                    a = arg0.getTitle();
                    b = arg1.getTitle();
                }
            }
        }
        return a.compareToIgnoreCase(b);
    }
}

class AlbumTitleNumberComparator implements Comparator<Title> {

    public int compare(Title arg0, Title arg1) {
        String a = arg0.getAlbum();
        String b = arg1.getAlbum();
        if(a.equalsIgnoreCase(b)) {
            int t = arg1.getTitleNumber() - arg0.getTitleNumber();
            if(t != 0) return t < 0 ? 1 : -1;

            a = arg0.getTitle();
            b = arg1.getTitle();
        }
        return a.compareToIgnoreCase(b);
    }
}

class ArtistAlbumTitleNumberComparator implements Comparator<Title> {

    public int compare(Title arg0, Title arg1) {
        String a = arg0.getArtist();
        String b = arg1.getArtist();
        if(a.equalsIgnoreCase(b)) {
            a = arg0.getAlbum();
            b = arg1.getAlbum();
            if(a.equalsIgnoreCase(b)) {
                int t = arg1.getTitleNumber() - arg0.getTitleNumber();
                if(t != 0)return t < 0 ? 1 : -1;

                a = arg0.getTitle();
                b = arg1.getTitle();
            }
        }
        return a.compareToIgnoreCase(b);
    }
}

class TitleIdComparator implements Comparator<Object> {
    private Map<Title, Integer> titles;

    public TitleIdComparator(Map<Title, Integer> map){
        this.titles = map;
    }

    public int compare(Object t1, Object t2) {
        //récupérer les personnes du Map par leur identifiant
        Integer ID1 = titles.get(t1);
        Integer ID2 = titles.get(t2);

//        return (Integer)arg0 < (Integer)arg1 ? -1 : ((Integer)arg0 > (Integer)arg1 ? 1 : 0);
        return ID1 - ID2;
    }
}
