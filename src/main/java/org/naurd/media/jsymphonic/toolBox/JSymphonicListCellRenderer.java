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
 * JSymphonicListCellRenderer.java
 *
 * Created on 12 octobre 2007, 19:47
 *
 */

package org.naurd.media.jsymphonic.toolBox;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author skiron
 */
public class JSymphonicListCellRenderer extends DefaultListCellRenderer {
    
    /**
     * Creates a new instance of JSymphonicListCellRenderer
     */
    public JSymphonicListCellRenderer() {
    }
   
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
        Component retValue = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        //retValue.setBackground(new Color(255,0,0));
        //setIcon(new ImageIcon(getClass().getResource("/org/naurd/media/symphonic/ressources/usb.png")));
        return retValue;
    }
}
