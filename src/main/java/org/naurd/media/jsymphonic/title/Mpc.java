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
 * Mpc.java
 *
 */

package org.naurd.media.jsymphonic.title;

/**
 *
 * @author skiron
 */
public class Mpc extends Title {

    /** Creates a new instance of Mpc */
    public Mpc(java.io.File f) {
        super(f);
        format = Title.MPC;
    }

    public Mpc(java.net.URL url){
        super(url);
    }

/* STATIC METHODS */

    /**
     *Obtains the list of the possible file extentions for this format.
     *
     *@return A list of extentions separated with comas.
     */
    public static String getFileExtentionsList() {
        return "mpc";
    }

/* GET METHODS */

    /**
     *Obtains the format of the title as a string.
     *
     *@return the format of the title as a string.
     */
    @Override
    public String getFormatAsString() {
        return "MPC";
    }
}
