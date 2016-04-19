/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.utils;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

public class AddressGeocoder {
	
    private final Geocoder geocoder;
    public Exception lastException;

    public AddressGeocoder(Context context) {
    	this.geocoder = new Geocoder(context); 
    }

	public String resolveAddressFromLocation(double latitude, double longitude) {
        try {
        	lastException = null;
            List<Address> results = geocoder.getFromLocation(latitude, longitude, 1);
            if (results.size() > 0) {
            	Address address = results.get(0);
            	return addressToString(address);
            }
        } catch (IOException e) {
        	lastException = e;
            Log.e("Geocoder", "Problem using geocoder", e);            
        }		
        return null;
	}

	public static String addressToString(Address address) {
    	StringBuilder sb = new StringBuilder();
    	int max = address.getMaxAddressLineIndex();
    	for (int i = max; i >= 0; i--) {
    		String line = address.getAddressLine(i);
    		if (i < max) {
    			sb.append(", ");        			
    		}
    		sb.append(line);
    	}
    	return sb.toString();
	}
	
}
