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
 * Debug.java
 *
 * Created on 1 avril 2008, 12:39
 *
 */

package org.naurd.media.jsymphonic.openSonyDb;

/**
 *
 * @author neub
 */
public class Debug {
    /*
     * Comment by nicolas_cardoso:
     * I don't know for what you had create this "Debug" class, but the "debug" point of view used in v2.1a is not used anymore. We are now using the "Logger" class. Please contact me if you need more info. 
     */
    
    public static void debug(boolean test) {
        if(!test) 
            System.err.println("JOpenSonyDb Error");
        
    }
    
    public static void perr(boolean test, String msg) {
        if(!test)
            System.err.println("JOpenSonyDb Error > "+msg);
    }
    
}
