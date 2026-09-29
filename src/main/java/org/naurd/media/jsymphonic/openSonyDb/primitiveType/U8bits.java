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
 * U8bits.java
 *
 * Created on 1 avril 2008, 12:13
 *
 */

package org.naurd.media.jsymphonic.openSonyDb.primitiveType;

/**
 * This class let manage the unsigned type (8bits).
 * @author neub
 */
public class U8bits {
    
    byte realbyte;
    
    /**
     * Creates a new instance of U8bits
     */
    public U8bits() {
        realbyte = 0;
    }
    
    public U8bits(int val) {
        if(0 <= val && val < 256) {
            realbyte=(byte)val;
        }     
    }
    
    /** Set with int value */
    public void set(int val) {
        if(0 <= val && val < 256) {
            realbyte=(byte)val;
        }  
    }
    
     /** Set with byte[] value */
    public int set(byte[] vals, int pos) {
        realbyte=vals[pos];
        return pos+1;
    }
    
    
    
    public int getInt() {
        return (int)(realbyte & 0xff);
    }
    
    public long getLong() {
        return (long)(realbyte & 0xFF);
    }
    
    public String toString() {
        return ""+getInt();
    }
    
    
}
