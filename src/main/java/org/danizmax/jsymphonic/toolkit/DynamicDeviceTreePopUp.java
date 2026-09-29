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
 * DynamicDeviceTreePopUp.java
 *
 */

package org.danizmax.jsymphonic.toolkit;

import org.danizmax.jsymphonic.gui.device.*;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import org.danizmax.jsymphonic.gui.TrackInfo.TrackInfoWindow;
import org.danizmax.jsymphonic.gui.local.LocalPanel;
import org.naurd.media.jsymphonic.title.Title;


/**
 * This class is a popup that changes its content depending on the object that it handles
 * @author danizmax - Daniel Žalar (danizmax@gmail.com)
 */
public class DynamicDeviceTreePopUp extends JPopupMenu implements ActionListener{

    private static Logger logger = Logger.getLogger("org.danizmax.jsymphonic.gui.device.DynamicDeviceTreePopUp");
    
    public static final int ROOT_ELEMENT_MENU = 0;
    public static final int TRACK_ELEMENT_MENU = 1;
    public static final int ALBUM_ELEMENT_MENU = 2;
    
    private JTree tree= null;
    private JMenuItem titleItem = null;
    private JMenuItem exportItem = null;
    private JMenuItem importItem = null;
    private JMenuItem renameRootItem = null;
    private JMenuItem trackInfoItem = null;
    private DevicePanel devPanel = null;
    private LocalPanel locPanel = null;
    
    /**
     * 
     * @param tree
     */
     public DynamicDeviceTreePopUp(LocalPanel lp, JTree tree){
        initMenu(); 
        setParentLogger(DevicePanel.getLogger()); 
        locPanel = lp;
        this.tree = tree;   
     }
     
     /**
      * 
      * @param dp
      * @param tree
      */
     public DynamicDeviceTreePopUp(DevicePanel dp, JTree tree){
        initMenu();
        setParentLogger(DevicePanel.getLogger()); 
        devPanel = dp;
        this.tree = tree;
     }
      
     private void initMenu(){
        titleItem = new JMenuItem();
        titleItem.setEnabled(false);
        titleItem.setFont(new Font( this.getFont().getFontName(), Font.BOLD, this.getFont().getSize()  ) );
        
        exportItem =  new JMenuItem();
        exportItem.addActionListener(this);
        exportItem.setActionCommand("EXPORT_BUTTON");
        exportItem.setText("Export");
        
        renameRootItem =  new JMenuItem();
        renameRootItem.addActionListener(this);
        renameRootItem.setActionCommand("RENAME_BUTTON");
        renameRootItem.setText("Rename");
        
        importItem =  new JMenuItem();
        importItem.addActionListener(this);
        importItem.setActionCommand("IMPORT_BUTTON");
        importItem.setText("Import");
        
        trackInfoItem = new JMenuItem();
        trackInfoItem.addActionListener(this);
        trackInfoItem.setActionCommand("INFO_BUTTON");
        trackInfoItem.setText("Track Information");
     }
    
    @Override
    public void setVisible(boolean visible){
        if(visible){
            JSymphonicMutableTreeNode tn = (JSymphonicMutableTreeNode)tree.getLastSelectedPathComponent();
            this.removeAll();    
            if(tree.getSelectionPath() != null){
                titleItem.setText(((TreeNode)tree.getSelectionPath().getLastPathComponent()).toString());
                this.add(titleItem);

                if(tree.getLeadSelectionPath().getPath().length == 1){
                    //show only if popup is constructed in devicePanel!
                    if(devPanel != null)   
                         this.add(renameRootItem);
                }else if( tn.getType() == JSymphonicMutableTreeNode.TYPE_TITLE || tn.getType() == JSymphonicMutableTreeNode.TYPE_AUDIOFILE){ // is last item a track?
                    
                    //show only if popup is constructed in devicePanel!
                    if(devPanel != null)      
                        this.add(exportItem);
                    
                    this.addSeparator();
                    this.add(trackInfoItem);
                }else{
                    //show only if popup is constructed in devicePanel!
                    if(devPanel != null){
                        this.add(importItem);
                        this.add(exportItem);
                    }
                }

                SwingUtilities.updateComponentTreeUI(this);
                }
        }
        super.setVisible(visible);   
    }

    public void actionPerformed(ActionEvent arg0) {
        if(arg0.getActionCommand().equals("EXPORT_BUTTON")){
            devPanel.getDeviceManager().scheduleTrackExport();
        }else if(arg0.getActionCommand().equals("INFO_BUTTON")){
           Title selectedTrack = ((JSymphonicMutableTreeNode)tree.getLastSelectedPathComponent()).getAsTitle();
           if(selectedTrack != null){
               TrackInfoWindow ti =  new TrackInfoWindow(selectedTrack);
               TrackInfoWindow.setParentLogger(DevicePanel.getLogger());
               
               if(devPanel != null)
                   ti.setLocationRelativeTo(devPanel.getParent());
               else
                   ti.setLocationRelativeTo(locPanel.getParent());
               
               ti.setVisible(true);
           }else{
               logger.warning("The selected file" + " " + devPanel.getDeviceManager().getLastSelectedNode().toString() + " " + "returned no object (null). There is a bug crawling around!");
           }
        }
    }

    public JTree getTree() {
        return tree;
    }

    public void setTree(JTree tree) {
        this.tree = tree;
    }
    
    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger aLogger) {
        logger = aLogger;
    }
    
    public static void setParentLogger(Logger aLogger) {
        logger.setParent(aLogger);
    }
}
