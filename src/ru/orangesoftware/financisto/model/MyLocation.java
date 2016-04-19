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
package ru.orangesoftware.financisto.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import ru.orangesoftware.financisto.utils.Utils;

@Entity
@Table(name = "LOCATIONS")
public class MyLocation {

	@Id
	@Column(name = "_id")
	public long id = -1;
	
	@Column(name = "name")
	public String name;
	
	@Column(name = "provider")
	public String provider;
	
	@Column(name = "accuracy")
	public float accuracy;
	
	@Column(name = "longitude")
	public double longitude;
	
	@Column(name = "latitude")
	public double latitude;
	
	@Column(name = "is_payee")
	public boolean isPayee;
	
	@Column(name = "resolved_address")
	public String resolvedAddress;

	@Column(name = "datetime")
	public long dateTime;

	@Column(name = "count")
	public int count;
	
	@Column(name = "updated_on")
 	public long updatedOn = System.currentTimeMillis();
 
 	@Column(name = "remote_key")
 	public String remoteKey ;		

	@Override
	public String toString() {
		return Utils.locationToText(name, latitude, longitude, accuracy, resolvedAddress);
	}
	
}
