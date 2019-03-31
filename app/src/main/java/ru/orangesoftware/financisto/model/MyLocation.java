package ru.orangesoftware.financisto.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static ru.orangesoftware.financisto.db.DatabaseHelper.LOCATIONS_TABLE;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

@Entity
@Table(name = LOCATIONS_TABLE)
public class MyLocation extends MyEntity implements SortableEntity {

    public static final int CURRENT_LOCATION_ID = 0;

    public static MyLocation currentLocation() {
        MyLocation location = new MyLocation();
        location.id = CURRENT_LOCATION_ID;
        location.title = "<CURRENT_LOCATION>";
        location.provider = location.resolvedAddress = "?";
        return location;
    }

    @Column(name = "provider")
    public String provider;

    @Column(name = "accuracy")
    public float accuracy;

    @Column(name = "longitude")
    public double longitude;

    @Column(name = "latitude")
    public double latitude;

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
