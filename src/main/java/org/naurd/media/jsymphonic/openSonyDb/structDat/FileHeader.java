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
 * FileHeader.java
 *
 * Created on 30 mars 2008, 17:56
 *
 */

package org.naurd.media.jsymphonic.openSonyDb.structDat;

import java.io.FileInputStream;
import java.io.IOException;
import org.naurd.media.jsymphonic.openSonyDb.primitiveType.U32bits;
import org.naurd.media.jsymphonic.openSonyDb.primitiveType.U8bits;
import org.naurd.media.jsymphonic.openSonyDb.primitiveType.UBytes;

/**
 * This class contains 16 bytes
 * @author neub
 */
public class FileHeader extends NativeStructure {
    
    private byte[] buff;
    
    private UBytes magic;      /* 4 bytes : "magic file descriptor" */
    private U32bits cte;       /* 4 bytes : Constant value */
    private U8bits count;      /* 1 byte  : Number of object pointers */
    private UBytes padding;    /* 7 bytes : padding */
    
    private boolean isFilled;
    
    /** Creates a new instance of FileHeader */
    public FileHeader() {
        
        buff= new byte[16];          /* We first read the 16 bytes in an array of char */

        magic   = new UBytes(4);      /* 4 bytes : "magic file descriptor" */
	cte     = new U32bits();     /* 4 bytes : Constant value */
	count   = new U8bits();      /* 1 byte  : Number of object pointers */
	padding = new UBytes(7);     /* 7 bytes : padding (Is not necessary, just to fill the with 0) */
        
        isFilled = false;
    }

    public void read(FileInputStream f) {
              
        //Read the total file Header from the fileReader
        try {
            //Read the magic description
            f.read(buff);    
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        //Initiate the position of each element in the fileHeader
        int pos=0;
        pos = magic.set(buff,pos);
        pos = cte.set(buff,pos);
        pos = count.set(buff,pos);
        pos = padding.set(buff,pos);
        
        isFilled=true;
   
    }
    
    public int nofObjects() {
        return count.getInt();
    }

    public void write(FileInputStream f) {
    }
    
    
    
    
    public String toString() {
        return "FileHeader: magic="+magic+", cte="+cte+", count="+count+", padding="+padding;
    }
    
    
    
    
}
