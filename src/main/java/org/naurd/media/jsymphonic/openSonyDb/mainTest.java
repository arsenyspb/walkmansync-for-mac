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
 * mainTest.java
 *
 * Created on 1 avril 2008, 12:27
 *
 */

package org.naurd.media.jsymphonic.openSonyDb;


/**
 *
 * @author neub
 */
public class mainTest {
    
    public static void main(String args[]) {
        loadPlaylist();
    }
    
    
    public static void loadPlaylist() {
// SETTINGS have changed in version 0.3.0a      Settings settings = new Settings();
// SETTINGS have changed in version 0.3.0a       String omgaudioPath = settings.getValue("OMGAUDIOpath","omgaudio");
        Playlist pl = new Playlist();
//comment to be able to compile        pl.read(omgaudioPath);
        
    }
    
    
}
