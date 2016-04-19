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
package ru.orangesoftware.financisto.activity;

import android.view.View;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.Project;

import java.util.ArrayList;

public class ProjectListActivity extends MyEntityListActivity<Project> {

    public ProjectListActivity() {
        super(Project.class);
    }

    @Override
    protected ArrayList<Project> loadEntities() {
        return em.getAllProjectsList(false);
    }

    @Override
    protected String getContextMenuHeaderTitle(int position) {
        return getString(R.string.project);
    }

    @Override
    protected Class getEditActivityClass() {
        return ProjectActivity.class;
    }

    @Override
    protected Criteria createBlotterCriteria(Project p) {
        return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(p.id));
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        em.deleteProject(id);
        recreateCursor();
    }

}
