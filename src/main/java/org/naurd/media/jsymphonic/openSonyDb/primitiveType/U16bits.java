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
 * U32bits.java
 *
 * Created on 1 avril 2008, 15:07
 *
 */

package org.naurd.media.jsymphonic.openSonyDb.primitiveType;

/**
 *
 * @author neub
 */
public class U16bits extends UBytes{
    
    /** Creates a new instance of U32bits */
    public U16bits() {
        super(2); //In 16 bits we have 2 bytes (2x8=16)
    }
        
    /**
     * This method transform the 4 bytes writed in big endians to an unsigned int (what we can call a long)
     */
    public long getLong() {
        int tmp=0;
        if(vec.length != 2) return -1;
        tmp += ((int)(vec[1] << 0) & 0x00FF);
        tmp += ((int)(vec[0] << 8) & 0xFF00);
        return (long)(tmp & 0xFFFFFFFF);
    }
    
    public String toString() {
        return ""+getLong();
    }
    
}
