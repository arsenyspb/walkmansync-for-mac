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
 * UBytes.java
 *
 * Created on 1 avril 2008, 14:51
 *
 */

package org.naurd.media.jsymphonic.openSonyDb.primitiveType;

//import java.util.ArrayList;
//import javax.lang.model.type.PrimitiveType;

/**
 *
 * @author neub
 */
public class UBytes {
    
    byte[] vec;
    /** Creates a new instance of UBytes
     * @param length 
     */
    public UBytes(int length) {
        vec = new byte[length];
    }  
    
    public int set(byte[] buff, int pos) {
         //Check if the buffer is not to small
        int nextPos = pos+vec.length;
        if(nextPos > buff.length) return -1;
        
        int tmp=0;
        for(int i=pos;i<nextPos;i++) {
            vec[tmp++]=buff[i];
        }
        return nextPos;
    }
    
    
    public int length() {
        return vec.length;
    }
    
    public char[] toChar() {
        char[] ret = new char[vec.length];
        for(int i=0;i<ret.length;i++) {
            ret[i]=(char)(vec[i] & 0xFF);
        }
        return ret;
    }
    
    
    @Override
    public String toString() {
        return new String(toChar());
    }
    
    public void debug() {
        /*
         * Comment by nicolas_cardoso:
         * same comment as in the Debug class, you should now use logger...
         */
        for(int i=0;i<vec.length;i++) {
            System.out.println(vec[i]);
        }
    }
    
}
