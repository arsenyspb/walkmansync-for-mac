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
 * PrimitiveTypeTools.java
 *
 * Created on 30 mars 2008, 18:15
 *
 */

package org.naurd.media.jsymphonic.openSonyDb.primitiveType;

/**
 *
 * @author neub
 */
public class PrimitiveTypeTools {
    
    public static int fourBytes2int(byte[] bytes) {
        int ret=-1;
        if(bytes.length != 4) return ret;

        //Example to code shifting values <<
        
        return ret;
    }
    
     /*Converts the number represented by some value in an array of bytes to an int. For example, if the array is : ["1";"2";"3";"4"]=[49;50;51;52], the corresponding int will be "1234".
     *
     *@param bytes The array of bytes to convert
     *
     *@return The converted number.
     *
     *@author nicolas_cardoso
     */
    public static int charBytes2int(byte[] bytes){
        int i = bytes.length - 1;
        int ret = 0;
        byte val;
        int pow = 0;
        
        while(i >= 0){
            val = (byte)(bytes[i]-(byte)48);
            if(val < 0 || val > 9) {
                val = -1;
                return 0;
            }

            ret += val*Math.pow(10, pow);
            pow++;
            i--;
        }
        return ret;
    }
    
}
