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
 * GenericDevice.java
 *
 * Created on October 15, 2006, 10:02 AM
 *
 */

package org.naurd.media.jsymphonic.device;
import java.io.File;
import org.naurd.media.jsymphonic.title.Title;
import org.naurd.media.jsymphonic.title.UnknowFileTypeException;
import org.naurd.media.jsymphonic.toolBox.Java6ToolBox;

/**
 *
 * @author Pat
 */
public class GenericDevice implements org.naurd.media.jsymphonic.device.Device {
    private File source = null;
    private String name = "default";
    private String description = "";
    private java.util.Vector<Title> titles = new java.util.Vector<Title>();
    private java.util.Vector<Title> titlesToAdd = new java.util.Vector<Title>();
    private java.util.Vector<Title> titlesToRemove = new java.util.Vector<Title>();
    /** Creates a new instance of GenericDevice */
    public GenericDevice(java.io.File dir,String sourceName,String sourceDesc){
        name = sourceName;
        description = sourceDesc;
        source = dir;
        loadTitlesFromDir(dir);
    }
    public GenericDevice(){
    }
    
    public javax.swing.ImageIcon getIcon(){
        return null;
    }
    public Title[] getTitles(){
        int count = titles.size() + titlesToAdd.size() + titlesToRemove.size();
        org.naurd.media.jsymphonic.title.Title[] ts = new org.naurd.media.jsymphonic.title.Title[count];
        int index = 0;
        for (int i=0;i<titles.size();i++){
            ts[index++] = (org.naurd.media.jsymphonic.title.Title)titles.get(i);
        }
        for (int i=0;i<titlesToAdd.size();i++){
            ts[index++] = (org.naurd.media.jsymphonic.title.Title)titlesToAdd.get(i);
        }
        for (int i=0;i<titlesToRemove.size();i++){
            ts[index++] = (org.naurd.media.jsymphonic.title.Title)titlesToRemove.get(i);
        }
        return ts;
    }
    
    public void reload(){
        titles.clear();
        titlesToAdd.clear();
        titlesToRemove.clear();
        loadTitlesFromDir(source);
    }

    private void loadTitlesFromDir(java.io.File dir){
        java.io.File[] fs = dir.listFiles();
        for (int i = 0;dir.exists() && i<fs.length;i++){
            if (fs[i].isDirectory()){
                loadTitlesFromDir(fs[i]);
            } else{
                //Validate that the format is a title...
                Title t;
                try {
                    t = Title.getTitleFromFile(fs[i]);
                } catch (UnknowFileTypeException ex) {
                    return;
                }
                titles.add(t);
            }
        }
    }

    public void scheduleDeletion(Title t){
        t.setStatus(Title.TO_DELETE);
        titlesToRemove.add(t);
    }

    public int scheduleImport(Title t){
        t.setStatus(Title.TO_IMPORT);
        titlesToAdd.add(t);

        return 0;
    }

    public String getName(){
        return name;
    }
    public void setName(String n){
        name=n;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String d){
        description = d;
    }
    public java.net.URL getSourceURL(){
        try{
            return source.toURI().toURL();
        } catch(Exception e){
            return null;
        }
    }
    
    public File getSource(){
        return source;
    }
    
    public void setSource(String source){
        this.source = new java.io.File(source);
    }
    
    public void applyChanges(){
        Thread t = new Thread(){
            @Override
            public void run(){
                try{
                    applyChangesInTread();
                } catch(Exception e){}
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        t = null;
    }

    private void applyChangesInTread() throws java.io.IOException{
        //Deleting files first...
        Title currentTitle = null;
        java.io.File f = null;
        for (int i = 0;i<titlesToRemove.size();i++){
            currentTitle = titlesToRemove.get(i);
            f = currentTitle.getSourceFile();
            if (f!=null){
                f.delete();
            }
        }
        
        //Adding new titles...
        for (int i = 0;i<titlesToAdd.size();i++){
            currentTitle = titlesToAdd.get(i);
            java.io.File artistDir = new java.io.File(source,currentTitle.getArtist());
            java.io.File albumDir = new java.io.File(artistDir,currentTitle.getAlbum());
            String filename = currentTitle.getTitle();
            java.io.File titleFile = new java.io.File(albumDir,filename);
            //Check if artist dir is there...
            if (!artistDir.exists()){
                artistDir.mkdir();
            }
            //Check if ablum dir is there
            if (!albumDir.exists()){
                albumDir.mkdir();
            }
            //Delete the file if it is already there...
            if (titleFile.exists()){
                titleFile.delete();
            }
            //Copy the file...
            java.io.InputStream in = currentTitle.getInputStream();
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(titleFile,"rw");
            byte[] buffer = new byte[4096];
            int count = 0;
            long totalCount = 0;
            while (count>=0){
                count = in.read(buffer);
                if (count>0){
                    totalCount+=count;
                    raf.write(buffer,0,count);
                }
            }
            in.close();
            raf.close();
        }
        currentTitle=null;
        //Once completed, reload everything...
        reload();
    }
    
    public long getTotalSpace(){
        Java6ToolBox.FileSpaceInfo spaceInfo = Java6ToolBox.getFileSpaceInfo(source);
        return spaceInfo.getTotalSpace();
    }
    public long getUsableSpace(){
        Java6ToolBox.FileSpaceInfo spaceInfo = Java6ToolBox.getFileSpaceInfo(source);
        return spaceInfo.getUsableSpace();
    }
    @Override
    public String toString(){
        return name;
    }

    public void scheduleExport(Title t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stopApplyChanges() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
