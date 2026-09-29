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
 * NativeStructure.java
 *
 * Created on 30 mars 2008, 18:04
 *
 */

package org.naurd.media.jsymphonic.openSonyDb.structDat;

import java.io.FileInputStream;
import org.naurd.media.jsymphonic.openSonyDb.Debug;

/**
 *
 * @author neub
 *  Abstract class that can not be instantiate
 *
 */
abstract public class NativeStructure {
    
    abstract public void read(FileInputStream f);
    abstract public void write(FileInputStream f);
    
    public static int toCharPosition(long hexaPos) {
        Debug.perr((hexaPos % 10 != 0),"Hexa pos is not a multiple of 10" );
        return (int)(16*hexaPos/10);
    }
    
    
}
