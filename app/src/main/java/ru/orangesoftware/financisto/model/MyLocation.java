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

import static ru.orangesoftware.orb.EntityManager.DEF_ID_COL;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

@Entity
@Table(name = "locations")
public class MyLocation extends MyEntity implements SortableEntity {

    public static final int CURRENT_LOCATION_ID = 0;

    public static MyLocation currentLocation() {
        MyLocation location = new MyLocation();
        location.id = CURRENT_LOCATION_ID;
        location.name = "<CURRENT_LOCATION>";
        location.provider = location.resolvedAddress = "?";
        return location;
    }

    @Id
    @Column(name = DEF_ID_COL)
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

    @Column(name = DEF_SORT_COL)
    public long sortOrder;

    @Override
    public long getSortOrder() {
        return sortOrder;
    }
}
