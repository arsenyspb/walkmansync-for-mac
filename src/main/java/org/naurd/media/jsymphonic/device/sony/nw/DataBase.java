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
 * DataBase.java
 *
 * Created on 20 mai 2007, 09:15
 *
 */

package org.naurd.media.jsymphonic.device.sony.nw;

import java.util.Map;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.toolBox.JSymphonicMap;

/**
 * This class includes methods for building the dataBase and writing it to the config files in the device.
 *
 * This class is abstract as it implement just things common to all generations (all generation have a list of title for instance...)
 *
 *@version 03.23.2008
 *@author Nicolas Cardoso
 *@author Daniel Žalar - added events to ensure GUI independancy
 */
public abstract class DataBase {
/* FIELDS */
    protected JSymphonicMap titles; // List of titles (associate Title (key) to title ID (value))
    protected JSymphonicMap paths; // List of paths (associates title ID (key) to their original path on the disk (value))
    protected Map covers; // List of paths (associates album (key) to its cover (value))
    
    
/* CONSTRUCTORS */
    /**
     * Allows to create an instance of DataBase from an existing device.
     * The database, once created is empty. Titles list can be filled with the method "addTitle". Before been written to the device, the database should be updated, with "update" method, to fill in the artists, albums,... lists.
     *
     * @author nicolas_cardoso
     */
    public DataBase() {
        titles = new JSymphonicMap();
        paths = new JSymphonicMap();
    }
    
    
/* ABSTRACT METHODS */ 
    /**
     * Write the database to the player.
     * This method is abstract as it depends on the contents of the database...
     *
     *@param genericNw The instance of the Net walkman.
     *
     *@author nicolas_cardoso
     */
    public abstract void write(NWGeneric genericNw);
    
    /**
     * Obtains the number of database files to be written. This method is usefull to inform the user throught the GUI
     * 
     * @return the number of database files.
     */
    public abstract int getNumberOfFiles();

    /**
     * Read the list of path from the database and store them in the "paths" variable.
     */
    public abstract int buildPathList();

    
/* METHODS */ 
    /**
     * Add a title to the database (the title is added to the titles list, other list are not changed) with a given title ID.
     * A title ID is a unique number representing the title in the database (it's also the name of the file in the 10FXX folders).
     *
     * @param title The title to add to the database.
     * @param titleId The title ID which will be associated to the title in the database. A free ID can be obtain using "getFreeTitleId" method.
     *
     * @author nicolas_cardoso
     */
    public void addTitle(Title title, int titleID) {
        // Add the title
        titles.put((Object)title, (Object)titleID);
    }

    /**
     * Add a title to the database.
     * This is a scheduled import, it means that the title should be imported only if doesn't already exist in the database. This is checked by looking at its path. Each file's path can only exists once in the database.
     *
     * @param title The title to be added
     * @return 0 if the title can be and is added, or -1 if the title already exists and hasn't been added.
     */
    public int scheduleImport(Title title){
        String titlePath = title.getSourceFile().getPath();

        // First, check that the title doesn't already exist in the database
        if(paths.containsValue(titlePath)){
            // If path already exist, don't add it again
            return -1;
        }

        // Determine a title ID for the new title
        int titleID = getFreeTitleId();

        // Add the title to the list
        addTitle(title, titleID);

        // Add its path to the list
        paths.put(titleID, titlePath);

        return 0;
    }

    /**
     * Schedule title to export. This methods just changes the title status to "TO_EXPORT".
     *
     * @param title Title to be exported
     */
    public void scheduleExport(Title title){
        ((Title) titles.getKey((Integer) titles.getValue(title))).setStatus(Title.TO_EXPORT);
    }

    /**
     * Schedule title for deletion. This methods changes the title status to "TO_DELETE" if the title is in the database, else, it removes it from the database.
     *
     * @param title Title to be exported
     * @return 0 if the title is on the device, 1 if the title was previously been sheduled for import and -1 if the status of the titles already was "TO_DELETE"
     */
    public int scheduleDeletion(Title title){
        int titleStatus = title.getStatus();

        if(paths.containsKey(titles.getValue(title))){
            // If the path is stored, it can be removed
            paths.remove(titles.getValue(title));
        }

        if(titleStatus == Title.TO_IMPORT || titleStatus == Title.TO_DECODE || titleStatus == Title.TO_ENCODE){
            // If title has previously been sheduled for import, just remove the title from the database
            titles.remove(title);
            return 1;
        }
        else if(titleStatus == Title.ON_DEVICE){
            // If title is on the device, it should be deleted, change its status
            ((Title) titles.getKey((Integer) titles.getValue(title))).setStatus(Title.TO_DELETE);
            return 0;
        }
        else{
            // Else, the title has already been scheduled for deletion, there is nothing more to do
            return -1;
        }
    }
    
    /**
     * Add a playlist to the device.
     */
    public void addPlaylist() {
        //TODO
    }

    /**
     * Add a path to a title ID in the database. This method should only be used when the 04PATLST.dat file has not been found, to fill in the paths list manually.
     *
     * @param titleId The Title ID to add a path to.
     * @param path The path to add.
     */
    public void addPath(int titleId, String path){
        paths.put(titleId, path);
    }

    /**
     * Remove a title from the database (the title is removed from the titles list, other list are not changed)
     *
     *@param titleToRemove The title to remove.
     *
     *@author nicolas_cardoso
     */
    public void removeTitle(Title titleToRemove) {
        titles.remove(titleToRemove);
    }

    /**
     * Remove a path from the database (the path is removed from the paths list, other list are not changed)
     *
     *@param title The title which the path is to remove.
     *
     *@author nicolas_cardoso
     */
    public void removePath(Title title) {
        // Remove the path if it exists
        if(paths.containsValue(getTitlePath(title))){
            paths.remove(getTitleId(title));
        }
    }
    
    /**
     * Obtain the list of the titles currently saved in the database.
     * 
     *@return The list of the titles.
     *
     *@author nicolas_cardoso
     */
    public JSymphonicMap getTitles() {
        return (JSymphonicMap) titles.clone();
    }
    
    /**
     * Obtain the title ID associated to a title.
     *
     *@param title The title which the title ID is wanted.
     *@return The title ID is title is in the database, "0" otherwise.
     *
     *@author nicolas_cardoso
     */   
    public int getTitleId(Title title) {
        if(titles.containsKey(title)){
            return (Integer)titles.getValue(title);
        }
        return 0;
    }

    /**
     * Replace a title in the database by another. This method should be used to replace a title by a itself when it has been transcoded.
     *
     * @param oldTitle The existing title in the database which should be replaced.
     * @param newTitle The new title.
     */
    public void replaceTitle(Title oldTitle, Title newTitle){
        // Search the title and the original path of the title to replace
        int titleId = (Integer)titles.getValue(oldTitle);
        // Note that the path should stay as it is
        // TODO check if there is anything to do with covers

        // Remove old title
        titles.remove(oldTitle);

        // Add new title
        addTitle(newTitle, titleId);
    }

    /**
     * Obtain the path corresponding to a title from the database.
     *
     * @param title The title which the path is wanted.
     * @return The path of the title (the original path of the title, from the computer, before it was transfered).
     */
    public String getTitlePath(Title title) {
        return (String)paths.getValue(getTitleId(title));
    }
    
    /**
     * Obtain the next free ID in the title list.
     *
     *@return A free ID for a title in the title list
     *
     *@author nicolas_cardoso
     */
    public int getFreeTitleId() {
        int i = 1;
        
        while(titles.containsValue(i)) { //search the first ID unused
            i++;
        }
        
        return i;
    }
    
    /**
     * Clear the database. This methods empty all the lists.
     *
     *@author nicolas_cardoso
     */
    public void clear() {
        // Empty all the lists
        titles.clear();
        paths.clear();
        //covers.clear(); TODO
    }

    /**
     * Remove a title from the database.
     * This method is used by the "applyDeletion" method, when it correctly delete a file from the device. It is also used by other methods to delete files not decoded,encode,transfered beacause the transfered has been interrupted.
     *
     * @param title
     */
    public void delete(Title title) {
        // First check that the title's path has been deleted from the path list, if not, do so
        if(paths.containsValue(getTitlePath(title))){
            paths.remove(getTitleId(title));
        }

        // TODO remove the cover

        // Remove the title from the list
        titles.remove(title);
    }

    /**
     * Update the status of a file in the database.
     * This method is used by the "apply" methods, when a file has been copied to the device, its status must be updated from "TO_IMPORT" (or "TO_DECODE", "TO_ENCODE", "TO_EXPORT") to "ON_DEVICE".
     *
     * @param title The title to update.
     * @param newStatus The new status of the title.
     */
    public void updateStatus(Title title, int newStatus) {
        ((Title) titles.getKey((Integer) titles.getValue(title))).setStatus(newStatus);
    }

    /**
     * Remove from the database the title which status is not ON_DEVICE and the corresponding paths.
     */
    public void flush() {
        for(int i=0; i < titles.size(); i++){
            // For each title
            // Get the title
            Title title = (Title) titles.get(i);
            if(title.getStatus() != Title.ON_DEVICE){
                // If the status is not ON_DEVICE, remove its path and the title
                delete(title);
            }
        }
    }
}
