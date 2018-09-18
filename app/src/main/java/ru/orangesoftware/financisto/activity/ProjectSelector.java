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
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.TransactionUtils;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/2/12 9:25 PM
 */
public class ProjectSelector<A extends AbstractActivity> extends MyEntitySelector<Project, A> {

    public ProjectSelector(A activity, DatabaseAdapter db, ActivityLayout x) {
        this(activity, db, x, R.id.project_add, R.id.project_clear, R.string.no_project);
    }
    
    public ProjectSelector(A activity, DatabaseAdapter db, ActivityLayout x, int actBtnId, int clearBtnId, int emptyId) {
        super(activity, db, x, MyPreferences.isShowProject(activity), 
                R.id.project, actBtnId, clearBtnId, R.string.project, emptyId, R.id.project_filter_toggle);
    }

    @Override
    protected Class getEditActivityClass() {
        return ProjectActivity.class;
    }

    @Override
    protected List<Project> fetchEntities(MyEntityManager em) {
        return em.getActiveProjectsList(!isMultiSelect());
    }

    @Override
    protected ListAdapter createAdapter(Activity activity, List<Project> entities) {
        return TransactionUtils.createProjectAdapter(activity, entities);
    }

    @Override
    protected SimpleCursorAdapter createFilterAdapter() {
        return TransactionUtils.createProjectAutoCompleteAdapter(activity, em);
    }

}
