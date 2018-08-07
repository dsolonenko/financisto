/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.TransactionUtils;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/2/12 9:25 PM
 */
public class LocationSelector extends MyEntitySelector<MyLocation> {

    public LocationSelector(Activity activity, MyEntityManager em, ActivityLayout x) {
        super(activity, em, x, MyPreferences.isShowLocation(activity),
                R.id.location, R.id.location_add, R.string.location, R.string.current_location, R.id.location_filter_toggle);
    }

    @Override
    protected Class getEditActivityClass() {
        return LocationActivity.class;
    }

    @Override
    protected List<MyLocation> fetchEntities(MyEntityManager em) {
        return em.getAllLocationsList(true);
    }

    @Override
    protected ListAdapter createAdapter(Activity activity, List<MyLocation> entities) {
        return TransactionUtils.createLocationAdapter(activity, entities);
    }

    @Override
    protected SimpleCursorAdapter createFilterAdapter() {
        return TransactionUtils.createLocationAutoCompleteAdapter(activity, em);
    }

}
