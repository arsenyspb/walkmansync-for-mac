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
 * JSymphonicMap.java
 *
 * Created on 26 juillet 2007, 16:44
 *
 */

package org.naurd.media.jsymphonic.toolBox;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author skiron
 */
public class JSymphonicMap extends HashMap {
    //Other
    private static Logger logger = Logger.getLogger("org.naurd.media.jsymphonic.toolBox.JSymphonicMap");
    
    /** Constructors */
    public JSymphonicMap() {
        super();
    }

    public JSymphonicMap(int initialCapacity) {
        super(initialCapacity);
    }
    
    public JSymphonicMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }
    
    /** Overwritten methods */
    @Override
    public Object put(Object key, Object value) {
        if( this.containsValue(value) ) {
//            logger.warning("This value is already associated in this map. No changes are made to the map.");
            return null;
        }
        else {
            return super.put((Object)key, (Object)value);
        }
    }
    
    @Override
    public void putAll(Map m){
        logger.severe("This method isn't implemented in this class.");
    }
    
    /** New methods */
    public Object getKey(Integer value) {
        if( !this.containsValue(value) ) {
            return null;
        }
        
        Object valueInTheMap, key;
        Iterator it = this.keySet().iterator();
        while( it.hasNext() ) {
            key = it.next();
            valueInTheMap = this.get(key);
            if( value.equals(valueInTheMap)) {
                return key;
            }
        }
        
        return null;
    }
    
    public Object getValue(Object key) {
        if( !this.containsKey(key) ) {
//            logger.warning("This key isn't in this map. 0 is returned.");
            return null;
        }
        
        return super.get(key);
    }
    
    public Integer maxValue() {
        Integer valueToReturn, currentValue;
        Collection values = this.values();
        Iterator it = values.iterator();
        
        if( it.hasNext() ) {
           valueToReturn = (Integer)it.next();
        }
        else{
           valueToReturn = 0;
        }
        
        while( it.hasNext() ) {
            currentValue = (Integer)it.next();
            
            if( currentValue.compareTo(valueToReturn) > 0 ) {
                valueToReturn = currentValue;
            }
        }
        
        return valueToReturn;
    }

    public Integer maxKey() {
        Integer keyToReturn, currentKey;
        Set keys = this.keySet();
        Iterator it = keys.iterator();

        if( it.hasNext() ) {
           keyToReturn = (Integer)it.next();
        }
        else{
           keyToReturn = 0;
        }

        while( it.hasNext() ) {
            currentKey = (Integer)it.next();

            if( currentKey.compareTo(keyToReturn) > 0 ) {
                keyToReturn = currentKey;
            }
        }

        return keyToReturn;
    }
}
