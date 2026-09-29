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
 * JSymphonicFileFilter.java
 *
 * Created on 29 septembre 2007, 00:17
 *
 */

package org.naurd.media.jsymphonic.toolBox;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author skiron
 */
public class JSymphonicFileFilter extends FileFilter{
    private String description;
    
    /**
     * Creates a new instance of JSymphonicFileFilter
     */
    public JSymphonicFileFilter() {
        description = "FLAC, MP2, MP3, OGG, OMA files";
    }

    public boolean accept(File file) {
        return acceptFile(file);
    }

    public static boolean acceptFile(File file){
        String fileName = file.getName();
        
        // Hidden files or directory
        if( fileName.startsWith(".")) { return false; }
        
        // Directory
        if( file.isDirectory() ) { return true; }
        
        // AAC files
        if( fileName.endsWith(".aac")) { return true; }
        if( fileName.endsWith(".aaC")) { return true; }
        if( fileName.endsWith(".aAc")) { return true; }
        if( fileName.endsWith(".aAC")) { return true; }
        if( fileName.endsWith(".AaC")) { return true; }
        if( fileName.endsWith(".AaC")) { return true; }
        if( fileName.endsWith(".AAc")) { return true; }
        if( fileName.endsWith(".AAC")) { return true; }
        
        // FLAC files
        if( fileName.endsWith(".flac")) { return true; }
        if( fileName.endsWith(".flaC")) { return true; }
        if( fileName.endsWith(".flAc")) { return true; }
        if( fileName.endsWith(".flAC")) { return true; }
        if( fileName.endsWith(".fLac")) { return true; }
        if( fileName.endsWith(".fLaC")) { return true; }
        if( fileName.endsWith(".fLAc")) { return true; }
        if( fileName.endsWith(".fLAC")) { return true; }
        if( fileName.endsWith(".Flac")) { return true; }
        if( fileName.endsWith(".FlaC")) { return true; }
        if( fileName.endsWith(".FlAc")) { return true; }
        if( fileName.endsWith(".FlAC")) { return true; }
        if( fileName.endsWith(".FLac")) { return true; }
        if( fileName.endsWith(".FLaC")) { return true; }
        if( fileName.endsWith(".FLAc")) { return true; }
        if( fileName.endsWith(".FLAC")) { return true; }
        
        // MP2 files
        if( fileName.endsWith(".mp2")) { return true; }
        if( fileName.endsWith(".mP2")) { return true; }
        if( fileName.endsWith(".Mp2")) { return true; }
        if( fileName.endsWith(".MP2")) { return true; }

        // MP3 files
        if( fileName.endsWith(".mp3")) { return true; }
        if( fileName.endsWith(".mP3")) { return true; }
        if( fileName.endsWith(".Mp3")) { return true; }
        if( fileName.endsWith(".MP3")) { return true; }
        
        // MPC files
        if( fileName.endsWith(".mpc")) { return true; }
        if( fileName.endsWith(".mpC")) { return true; }
        if( fileName.endsWith(".mPc")) { return true; }
        if( fileName.endsWith(".mPC")) { return true; }
        if( fileName.endsWith(".Mpc")) { return true; }
        if( fileName.endsWith(".MpC")) { return true; }
        if( fileName.endsWith(".MPc")) { return true; }
        if( fileName.endsWith(".MPC")) { return true; }
        
        // OGG files
        if( fileName.endsWith(".ogg")) { return true; }
        if( fileName.endsWith(".ogG")) { return true; }
        if( fileName.endsWith(".oGg")) { return true; }
        if( fileName.endsWith(".oGG")) { return true; }
        if( fileName.endsWith(".Ogg")) { return true; }
        if( fileName.endsWith(".OgG")) { return true; }
        if( fileName.endsWith(".OGg")) { return true; }
        if( fileName.endsWith(".0GG")) { return true; }
        
        // OMA files
        if( fileName.endsWith(".oma")) { return true; }
        if( fileName.endsWith(".omA")) { return true; }
        if( fileName.endsWith(".oMa")) { return true; }
        if( fileName.endsWith(".oMA")) { return true; }
        if( fileName.endsWith(".Oma")) { return true; }
        if( fileName.endsWith(".OmA")) { return true; }
        if( fileName.endsWith(".OMa")) { return true; }
        if( fileName.endsWith(".OMA")) { return true; }
         
        // WAVE files
        if( fileName.endsWith(".wav")) { return true; }
        if( fileName.endsWith(".waV")) { return true; }
        if( fileName.endsWith(".wAv")) { return true; }
        if( fileName.endsWith(".wAV")) { return true; }
        if( fileName.endsWith(".Wav")) { return true; }
        if( fileName.endsWith(".WaV")) { return true; }
        if( fileName.endsWith(".WAv")) { return true; }
        if( fileName.endsWith(".WAV")) { return true; }
        if( fileName.endsWith(".wave")) { return true; }
        if( fileName.endsWith(".wavE")) { return true; }
        if( fileName.endsWith(".waVe")) { return true; }
        if( fileName.endsWith(".waVE")) { return true; }
        if( fileName.endsWith(".wAve")) { return true; }
        if( fileName.endsWith(".wAvE")) { return true; }
        if( fileName.endsWith(".wAVe")) { return true; }
        if( fileName.endsWith(".wAVE")) { return true; }
        if( fileName.endsWith(".Wave")) { return true; }
        if( fileName.endsWith(".WavE")) { return true; }
        if( fileName.endsWith(".WaVe")) { return true; }
        if( fileName.endsWith(".WaVE")) { return true; }
        if( fileName.endsWith(".WAve")) { return true; }
        if( fileName.endsWith(".WAvE")) { return true; }
        if( fileName.endsWith(".WAVe")) { return true; }
        if( fileName.endsWith(".WAVE")) { return true; }
        
        // WMA files
        if( fileName.endsWith(".wma")) { return true; }
        if( fileName.endsWith(".wmA")) { return true; }
        if( fileName.endsWith(".wMa")) { return true; }
        if( fileName.endsWith(".wMA")) { return true; }
        if( fileName.endsWith(".Wma")) { return true; }
        if( fileName.endsWith(".WmA")) { return true; }
        if( fileName.endsWith(".WMa")) { return true; }
        if( fileName.endsWith(".WMA")) { return true; }
        
        // Other files
        return false;
    }

    public String getDescription() {
        return description;
    }
    
}
