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

package org.naurd.media.jsymphonic.toolBox;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author skiron
 */
public class JSymphonicMutableTreeNode extends DefaultMutableTreeNode {
    /* Constants */
    public static final int ARTIST = 1;
    public static final int ALBUM = 2;
    public static final int GENRE = 3;
    public static final int PLAYLIST = 4;
    public static final int TITLE = 5;
    public static final int USB = 6;
    public static final int FOLDER = 7;
    public static final int AUDIOFILE = 8;
    
    /* Fields */
    private int type; //Add a new field to handle a different icon per type
    
    /* Constructors */
    /**
     * Creates a new instance of JSymphonicMutableTreeNode
     */
    public JSymphonicMutableTreeNode(int type) {
        super();
        this.type = type; 
    }
    
    public JSymphonicMutableTreeNode(Object userObject, int type) {
        super(userObject);
        this.type = type; 
    }
    
    /* Methods */
    //Add a method to know the type
    public int getType() {
        return type;
    }
    
}
