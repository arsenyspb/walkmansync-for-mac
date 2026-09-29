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
 * SymphonicFileFilter.java
 *
 * Created on 29 septembre 2007, 00:17
 *
 */

package org.naurd.media.jsymphonic.toolBox;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author skiron
 */
public class JSymphonicIoFileFilter implements FileFilter{
    private String description;
    
    /** Creates a new instance of SymphonicFileFilter */
    public JSymphonicIoFileFilter() {
        description = "FileFilter use in Symphonic";
    }

    public boolean accept(File file) {
        // Use function defined in JSymphonicFileFilter
        return JSymphonicFileFilter.acceptFile(file);
    }   
}
