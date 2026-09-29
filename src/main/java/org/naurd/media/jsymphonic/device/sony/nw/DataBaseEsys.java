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
 * DataBaseEsys.java
 *
 * Created on 25 february 2008, 20:44
 *
 */

package org.naurd.media.jsymphonic.device.sony.nw;

/**
 *
 * @author skiron
 * @author Daniel Žalar - added events to ensure GUI independancy
 */
public class DataBaseEsys extends DataBase{
/* FIELDS */
    private java.io.File esys;
    
/* CONSTRUCTORS */
    /**
     * Creates a new instance of DataBaseEsys
     */
    public DataBaseEsys(java.io.File esys) {
        // Call the super contructor
        super();

        // TODO
        this.esys = esys;
    }
    

/* METHODS */ 
    /**
     * Write the database to the player.
     *
     *@param genNw The instance of the Net walkman.
     *
     *@author nicolas_cardoso
     */
    public void write(NWGeneric genNw) {
        // TODO
    }

    @Override
    public int getNumberOfFiles() {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int buildPathList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
