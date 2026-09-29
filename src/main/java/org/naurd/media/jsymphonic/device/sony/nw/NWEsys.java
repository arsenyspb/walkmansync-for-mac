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
 * NWEsys.java
 *
 * Created on 15 mai 2009, 14:31:51
 *
 */

package org.naurd.media.jsymphonic.device.sony.nw;

import java.util.Vector;
import java.util.logging.Logger;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.toolBox.Java6ToolBox;

/**
 * This class describe a Network Walkman with a database file based on a "ESYS" folder (generation 2).
 * It extends the NW generic class.
 *
 * @author skiron
 */
public class NWEsys extends NWGeneric{
/* FIELDS */
    //Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.system.sony.nw.NWEsys");

/* CONSTRUCTORS */
    /**
     * Allows to create an instance of NWOmgaudio from an existing device.
     *
     * TODO to update
     * @param sourceName The name of the source, i.e. the name of the device.
     * @param sourceDesc The description of the source.
     * @param sourceIcon The icon of the source.
     * @param listener an object that implements NWGenericListener or null for no listener
     */
    public NWEsys(String devicePath, String sourceName, String sourceDesc, javax.swing.ImageIcon sourceIcon, int generation, NWGenericListener listener, String exportPath){
        // Call the super contructor
        super(sourceName, sourceDesc, sourceIcon, listener, exportPath);

        // Set up the device and database folders
        this.devicePath = devicePath;
        if( !initSourcePath() ) {
            logger.severe("Invalid OMGAUDIO directory.\nExiting program.");
            System.exit(-1);
        }

        // TODO KEY !?
		this.gotkey = true; // Gen2 devices always have a key !!

        // Update space
        Java6ToolBox.FileSpaceInfo spaceInfo = Java6ToolBox.getFileSpaceInfo(source);
        usableSpace = spaceInfo.getUsableSpace();
        totalSpace = spaceInfo.getTotalSpace();

        // Set up the database
        dataBase = new DataBaseEsys(source);

        // Set the generation
        this.generation = generation;

        /* TODO key ?
        // Load the key
        if(loadKey(sourceDir.getParent()) < 0){
            // If the DvID.dat file is not found, we should display a warning message
            JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("localization/devicepanel").getString("DevicePanel.NoKeyFound"), java.util.ResourceBundle.getBundle("localization/misc").getString("global.Error"), JOptionPane.ERROR_MESSAGE);

            // Since no keys have been found, all we can do is consider than the walkman doesn't need it...
            this.gotkey = false;
        }*/

        // Fill in the title's list
        loadTitlesFromDevice();
    }

/* Abstract Methods implementation */
    @Override
    protected void applyDeletion() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    protected void applyExport() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    protected void applyImport() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    protected void loadTitlesFromDevice() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }
}