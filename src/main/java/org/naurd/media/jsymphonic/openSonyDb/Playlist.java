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
 * Playlist.java
 *
 * Created on 1 avril 2008, 12:33
 *
 */

package org.naurd.media.jsymphonic.openSonyDb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import org.naurd.media.jsymphonic.openSonyDb.structDat.FileHeader;
import org.naurd.media.jsymphonic.openSonyDb.structDat.ObjectHeader;
import org.naurd.media.jsymphonic.openSonyDb.structDat.ObjectPointeur;

/**
 *
 * @author neub
 */
public class Playlist {
    
    String fPathDef = "03GINF22.DAT";
    String fPathTree = "01TREE22";
    
    
    /** Creates a new instance of Playlist */
    public Playlist() {
    }
    
    /*
     * Comment  by nicolas_cardoso:
     * "println" statement will have to be deleted in the final version.
     */
    
    public void read(String omgDir) throws IOException {
        FileInputStream fReader;    
       // File fDef = new File(omgDir+File.separator+fPathDef);
        File fDef = new File("/media/NW-HD5/OLD_OMGAUDIO/03GINF22.DAT");
        Debug.perr(fDef.exists(),"File "+fDef.getAbsoluteFile()+" not found");
        try {
            fReader = new FileInputStream(fDef);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return;
        }
        
        // Read The fileHeader and obtain the number of object
        FileHeader fHeader = new FileHeader();
        fHeader.read(fReader);
        System.out.println(fHeader);
        
        // Construct the number of Object Pointeur present in the FileHeader
        ObjectPointeur[] pObjVec = new ObjectPointeur[fHeader.nofObjects()];
        ObjectHeader[] objVec = new ObjectHeader[fHeader.nofObjects()];
        
        //Browse all the Object's pointer consequitively (IMPORTANT)
        for(int i=0;i<pObjVec.length;i++) {
            pObjVec[i] = new ObjectPointeur();
            pObjVec[i].read(fReader);
            System.out.println(pObjVec[i]);
        }
        
        // Once we know the position of each object we can find their header
        for(int i=0;i<pObjVec.length;i++) {
            objVec[i] = new ObjectHeader();
            fReader.close();
            fReader = new FileInputStream(fDef);
            objVec[i].read(fReader,pObjVec[i]);
            System.out.println(objVec[i]);
        }     
        fReader.close();
        
        
    }

 

    
}
