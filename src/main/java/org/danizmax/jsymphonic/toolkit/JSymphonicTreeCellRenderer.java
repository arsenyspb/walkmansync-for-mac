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
 * JSymphonicTreeCellRenderer.java
 * 
 */

package org.danizmax.jsymphonic.toolkit;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import org.naurd.media.jsymphonic.toolBox.JSymphonicMutableTreeNode;

/**
 * This class is a custom tree cell renderer
 * @author danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class JSymphonicTreeCellRenderer implements TreeCellRenderer {

    JPanel renderer;
    JLabel nameLabel;
    
    public JSymphonicTreeCellRenderer(){
        renderer = new JPanel(new GridLayout(0, 1));
        nameLabel = new JLabel(" ");
        nameLabel.setForeground(Color.blue);
        renderer.add(nameLabel);
    }
    
    public Component getTreeCellRendererComponent(JTree arg0, Object file, boolean arg2, boolean arg3, boolean arg4, int arg5, boolean arg6) {
         Component returnValue = null;
    if ((file != null) && (file instanceof JSymphonicMutableTreeNode)) {
      Object userObject = ((JSymphonicMutableTreeNode) file).getUserObject();
   /*   if (userObject instanceof Book) {
        Book book = (Book) userObject;
        titleLabel.setText(book.getTitle());
        authorsLabel.setText(book.getAuthors());
        priceLabel.setText("" + book.getPrice());
        if (selected) {
          renderer.setBackground(backgroundSelectionColor);
        } else {
          renderer.setBackground(backgroundNonSelectionColor);
        }
        renderer.setEnabled(tree.isEnabled());
        returnValue = renderer;
      }
    }
    if (returnValue == null) {
      returnValue = defaultRenderer.getTreeCellRendererComponent(tree,
          value, selected, expanded, leaf, row, hasFocus);*/
    }
         
    return returnValue;
    }

}
