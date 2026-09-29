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
 * Device.java
 *
 * Created on October 7, 2006, 12:07 PM
 *
 */

package org.naurd.media.jsymphonic.device;
import javax.swing.ImageIcon;
import org.naurd.media.jsymphonic.title.Title;
/**
 *
 * @author Pat
 */
public interface Device {
    public Title[] getTitles();
    
    public int scheduleImport(Title t);
    public void scheduleExport(Title t);
    public void scheduleDeletion(Title t);
    public void reload();
    public void applyChanges() throws java.io.IOException;
    public void stopApplyChanges();

    public Object getSource();
    public void setSource(String path);
    public String getName();
    public void setName(String n);
    public String getDescription();
    public void setDescription(String d);
    public long getTotalSpace();
    public long getUsableSpace();
    public ImageIcon getIcon();
}
