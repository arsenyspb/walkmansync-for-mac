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
 * GenericPodcast.java
 *
 * Created on 21 février 2007, 19:32
 *
 */

package org.naurd.media.jsymphonic.podcasts;
import org.naurd.media.jsymphonic.title.Title;

/**
 *
 * @author pballeux
 */
public class GenericPodcast {/* TODO implements org.naurd.media.jsymphonic.device.Device {
    private String name = "default";
    private String description = "";
    private String source = "";
    private java.util.Vector<Title> titles = new java.util.Vector<Title>();
    private javax.swing.ImageIcon icon = null;
    /** Creates a new instance of GenericPodcast *
    public GenericPodcast() {
    }
    
    public Title[] getTitles(){
        int count = titles.size();
        org.naurd.media.jsymphonic.title.Title[] ts = new org.naurd.media.jsymphonic.title.Title[count];
        int index = 0;
        for (int i=0;i<titles.size();i++){
            ts[index++] = (org.naurd.media.jsymphonic.title.Title)titles.get(i);
        }
        return ts;
    }
    
    public void removeTitles(Title t){
    }
    public int addTitle(Title t){
        return 0;
    }
    public void replaceTitle(Title oldTitle,Title newTitle){
    }
    public String getSourceName(){
        return name;
    }
    public void setSourceName(String n){
        name=n;
    }
    public String getSourceDescription(){
        return description;
    }
    public void setSourceDescription(String d){
        description = d;
    }
    public java.net.URL getSourceURL(){
        java.net.URL url = null;
        try{
            url = new java.net.URL(source);
        } catch(Exception e){}
        return url;
    }
    public String getSource(){
        return source;
    }
    public void setSource(String url){
        source = url;
    }
    public void writeTitles() throws java.io.IOException{
    }
    public void refreshTitles(){
        if (source.length()>0){
            try{
                java.net.URL url = new java.net.URL(source);
                org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openStream());
                org.w3c.dom.NodeList nodes = doc.getElementsByTagName("item");
                org.w3c.dom.Node channel = doc.getElementsByTagName("channel").item(0);
                org.w3c.dom.NodeList urls = doc.getElementsByTagName("url");
                String title = "";
                //Finding the icon for that podcast...
                for (int i = 0;i<urls.getLength();i++){
                    if (urls.item(i).getParentNode().getNodeName().equals("image")){
                        java.net.URL logourl = new java.net.URL(urls.item(i).getTextContent());
                        icon = new javax.swing.ImageIcon(logourl);
                        break;
                    }
                }
                //Finding the title for that podcast as the description...
                for (int i = 0;i<channel.getChildNodes().getLength();i++){
                    org.w3c.dom.Node node = channel.getChildNodes().item(i);
                    if (node.getNodeName().equals("description")){
                        description = node.getTextContent();
                        break;
                    }
                }
                //Finding the host...
                for (int i = 0;i<channel.getChildNodes().getLength();i++){
                    org.w3c.dom.Node node = channel.getChildNodes().item(i);
                    if (node.getNodeName().equals("title")){
                        title = node.getTextContent();
                        break;
                    }
                }
                
                //Finding the publisher...
                for (int i = 0;i<channel.getChildNodes().getLength();i++){
                    org.w3c.dom.Node node = channel.getChildNodes().item(i);
                    if (node.getNodeName().equals("description")){
                        description = node.getTextContent();
                        break;
                    }
                }
                //Finding each title...
                for(int i = 0;i<nodes.getLength();i++){
                    //Each title in this podcast...
                    Title t = Title.getTitleFromPodcast(nodes.item(i),url.getHost(),title);
                    titles.add(t);
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        
    }
    public javax.swing.ImageIcon getIcon(){
        return icon;
    }
    public long getTotalSpace(){
        return -1;
    }
    public long getUsableSpace(){
        return -1;
    }

    @Override public String toString(){
        return name;
    }*/
}
