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
public class U32bits extends UBytes{
    
    /** Creates a new instance of U32bits */
    public U32bits() {
        super(4); //In 32 bits we have 4 bytes
    }
    
    
    /**
     * This method transform the 4 bytes writed in big endians to an unsigned int (what we can call a long)
     */
    public long getLong() {
        int tmp=0;
        if(vec.length != 4) return -1;
        tmp += ((int)(vec[3] << 0 ) & 0x000000FF);
        tmp += ((int)(vec[2] << 8 ) & 0x0000FF00);
        tmp += ((int)(vec[1] << 16) & 0x00FF0000);
        tmp += ((int)(vec[0] << 24) & 0xFF000000);
        return (long)(tmp & 0xFFFFFFFF);
    }
    
    public String toString() {
        return ""+getLong();
    }
    
}
