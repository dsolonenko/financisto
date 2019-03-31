package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.TransactionUtils;

public class LocationSelector<A extends AbstractActivity> extends MyEntitySelector<MyLocation, A> {

    public LocationSelector(A activity, DatabaseAdapter db, ActivityLayout x) {
        this(activity, db, x, R.id.location_add, R.id.location_clear, R.string.current_location);
    }

    public LocationSelector(A activity, DatabaseAdapter db, ActivityLayout x, int actBtnId, int clearBtnId, int emptyId) {
        super(MyLocation.class, activity, db, x, MyPreferences.isShowLocation(activity),
                R.id.location, actBtnId, clearBtnId, R.string.location, emptyId, R.id.location_filter_toggle, R.id.location_show_list);
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

    @Override
    protected boolean isListPick() {
        return MyPreferences.isLocationSelectorList(activity);
    }

    @Override
    protected String getEntityTypeName() {
        return activity.getString(R.string.location);
    }

}
