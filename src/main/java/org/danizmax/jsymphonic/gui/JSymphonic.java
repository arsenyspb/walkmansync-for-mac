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
 * JSymphonicWindow.java
 *
 */

package org.danizmax.jsymphonic.gui;


/**
 * This is the main JSymphonic class that is used for starting the application
 * 
 * @author danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class JSymphonic {

        /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JSymphonicWindow jsw = new JSymphonicWindow();
        jsw.setLocationByPlatform(true);
        jsw.setVisible(true);
    }
}
