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
 * JSymphonicMutableTreeNode.java
 *
 * Created on 20 juillet 2007, 19:41
 *
 */

package org.danizmax.jsymphonic.toolkit;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.naurd.media.jsymphonic.device.Device;
import org.naurd.media.jsymphonic.title.Title;

/**
 * This class is a custom tree node
 * @author danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class JSymphonicMutableTreeNode extends DefaultMutableTreeNode implements  Serializable, Cloneable, MutableTreeNode, TreeNode, Comparable {
    /* Constants */
    public static final int TYPE_ARTIST = 1;
    public static final int TYPE_ALBUM = 2;
    public static final int TYPE_GENRE = 3;
    public static final int TYPE_PLAYLIST = 4;
    public static final int TYPE_YEAR = 5;
    public static final int TYPE_TITLE = 6;
    public static final int TYPE_USB = 7;
    public static final int TYPE_FOLDER = 8;
    public static final int TYPE_AUDIOFILE = 9;
    
    public static final int ACTION_LEAVE = Title.ON_DEVICE;
    public static final int ACTION_DELETE = Title.TO_DELETE;
    public static final int ACTION_IMPORT = Title.TO_IMPORT;
    public static final int ACTION_EXPORT = Title.TO_EXPORT;
    
    /* Fields */
    private int type; //Add a new field to handle a different icon per type
    private File f = null;
    private Device device = null;
    private int nodeAction = ACTION_LEAVE;
  //  private int originalTrackAction = ACTION_LEAVE;
    private Title t = null;
    
    /**
     * The node represents a physical object. It can be audiofile, folder or playlist
     * @param file file object representing the file
     * @param type type of file. It can be TYPE_FOLDER, TYPE_AUDIOFILE or TYPE_PLAYLIST
     * @param action action to be executed upon file. Possible actions are ACTION_LEAVE, ACTION_DELETE, ACTION_IMPORT, ACTION_EXPORT
     */
    public JSymphonicMutableTreeNode(File file, int type, int action) {
        nodeAction = action;
        f = file;
        this.type = type;
    }
    
    /**
     * The node represents a physical object. It can be audiofile, folder or playlist
     * @param file file object representing the file
     * @param type type of file. It can be TYPE_FOLDER, TYPE_AUDIOFILE or TYPE_PLAYLIST
     * 
     * The default action is ACTION_LEAVE
     */
    public JSymphonicMutableTreeNode(File file, int type) {
        nodeAction = ACTION_LEAVE;
        f = file;
        this.type = type; 
    }

    /**
     * The node represents a physical device. It is represented by a "device" object
     *
     * @param device The device object representing the device
     *
     * The default action is ACTION_LEAVE
     * The default type is TYPE_USB
     */
    public JSymphonicMutableTreeNode(Device device) {
        nodeAction = ACTION_LEAVE;
        this.device = device;
        this.type = TYPE_USB;
    }
    
    /**
     * The node represents a physical omg object on Sony device. It can be artist, album, genre, year or title
     * @param title titlw object representing the omg file
     * @param type type of omg file. It can be TYPE_ARTIST, TYPE_ALBUM, TYPE_GENRE, TYPE_YEAR or TYPE_TITLE
     * @param action action to be executed upon file. Possible actions are ACTION_LEAVE, ACTION_DELETE, ACTION_IMPORT, ACTION_EXPORT
     * 
     * I the node is of type TYPE_TITLE, it represents a "real" object that represents the omg file on Sony device. 
     * If the node is of type TYPE_ARTIST, TYPE_ALBUM, TYPE_GENRE or TYPE_YEAR, it does not represent a "real" object, but rather only metadata 
     * of a child of type TYPE_TITLE
     */
    public JSymphonicMutableTreeNode(Title title, int type, int action) {
        nodeAction = action;
        t = title;
        this.type = type; 
    }
    
    /**
     * The node represents a physical omg object on Sony device. It can be artist, album, genre, year or title
     * @param title titlw object representing the omg file
     * @param type type of omg file. It can be TYPE_ARTIST, TYPE_ALBUM, TYPE_GENRE, TYPE_YEAR or TYPE_TITLE
     * 
     * I the node is of type TYPE_TITLE, it represents a "real" object that represents the omg file on Sony device. 
     * If the node is of type TYPE_ARTIST, TYPE_ALBUM, TYPE_GENRE or TYPE_YEAR, it does not represent a "real" object, but rather only metadata 
     * of a child of type TYPE_TITLE
     */
    public JSymphonicMutableTreeNode(Title title, int type) {
        nodeAction = ACTION_LEAVE;
        t = title;
        this.type = type; 
    }
    
    /* Methods */
    //Add a method to know the type
    public int getType() {
        return type;
    }

    @Override
    public String toString(){
        try {
            switch (type) {
                case TYPE_TITLE:
                    // If it's a title, try to read the title number to display it
                    int titleNumber = t.getTitleNumber();
                    String titleName = t.getTitle();
                    if(titleNumber != 0){
                        // If the title number is not zero (is coherent), add it to the titlename
                        if(titleNumber < 10){
                            titleName = "0" + titleNumber + "-" + titleName; // Add a leading "0" if < 10
                        }
                        else {
                            titleName = titleNumber + "-" + titleName;
                        }
                    }
                    return titleName;
                case TYPE_ARTIST:
                    return t.getArtist();
                case TYPE_ALBUM:
                    return t.getAlbum();
                case TYPE_GENRE:
                    return t.getGenre();
                case TYPE_YEAR:
                    return String.valueOf(t.getYear());
                case TYPE_FOLDER:
                    return f.getName();
                case TYPE_AUDIOFILE:
                    return f.getName();
                case TYPE_USB:
                    return device.getName();
                case TYPE_PLAYLIST:
                    //TODO playlist not implemented
                    return "playlist not implemented!";
                default:
                    if (f != null) {
                        return f.getCanonicalPath();
                    } else {
                        return t.getSourceFile().getCanonicalPath();
                    }
            }
        } catch (IOException ex) {
            Logger.getLogger(JSymphonicMutableTreeNode.class.getName()).log(Level.SEVERE, null, ex);
            return "Error in node!";
        }

    }

  /*  public void scheduleTrackDeletion(){
        trackAction = ACTION_DELETE;
        //set same state for all children
        if(!this.isLeaf()){ 
               Enumeration children = this.children();
               while(children.hasMoreElements()){
                   ((JSymphonicMutableTreeNode) children.nextElement()).scheduleTrackDeletion();
               } 
        }
    }
    
    public void scheduleTrackImport(){
        trackAction = ACTION_IMPORT;
        //set same state for all children
        if(!this.isLeaf()){ 
               Enumeration children = this.children();
               while(children.hasMoreElements()){
                   ((JSymphonicMutableTreeNode) children.nextElement()).scheduleTrackImport();
               } 
        }
    }
    
    public void scheduleTrackExport(){
        trackAction = ACTION_EXPORT;
        //set same state for all children
        if(!this.isLeaf()){ 
               Enumeration children = this.children();
               while(children.hasMoreElements()){
                   ((JSymphonicMutableTreeNode) children.nextElement()).scheduleTrackExport();
               } 
        }
    }
    
    public void Cancelschedule(){
        trackAction = originalTrackAction;
        //set same state for all children
        if(!this.isLeaf()){ 
               Enumeration children = this.children();
               while(children.hasMoreElements()){
                   ((JSymphonicMutableTreeNode) children.nextElement()).Cancelschedule();
               } 
        }
    }*/
    
    /**
     * Return File object that represents this node.
     * @return File object.
     */
    public File getAsFile() {
        if(f == null)
            return t.getSourceFile();
        else
            return f;
    }
    
    /**
     * Return Title object that represents this node.
     * @return Title object. If node doesn't represent a track null will be returned
     */
    public Title getAsTitle() {
        if(t == null)
            return new Title(f);
        else
            return t;
    }

    public int getNodeAction() {
        return nodeAction;
    }

    /**
     * Overide this method to sort the items when they are inserted into the tree
     */
    @Override
    public void insert(final MutableTreeNode newChild, final int childIndex) {
        super.insert(newChild, childIndex);
        java.util.Collections.sort(this.children);
    }

    /**
     * Overide this method to make tree items comparable
     */
    public int compareTo(Object object) {
        if( (((MutableTreeNode)object).isLeaf() && this.isLeaf()) || (!((MutableTreeNode)object).isLeaf() && !this.isLeaf()) ){
            return this.toString().compareToIgnoreCase(object.toString());
        }
        else if(((MutableTreeNode)object).isLeaf()){
            return this.toString().toUpperCase().compareTo(object.toString().toLowerCase());
        }
        else {
            return this.toString().toLowerCase().compareTo(object.toString().toUpperCase());
        }
    }
}
