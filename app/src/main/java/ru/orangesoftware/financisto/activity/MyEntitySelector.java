/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.MyEntity;

import java.util.List;

import static ru.orangesoftware.financisto.activity.AbstractActivity.setVisibility;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/2/12 9:25 PM
 */
public abstract class MyEntitySelector<T extends MyEntity> {

    private final Activity activity;
    private final MyEntityManager em;
    private final ActivityLayout x;
    private final boolean isShow;
    private final int layoutId;
    private final int layoutPlusId;
    private final int labelResId;
    private final int defaultValueResId;

    private View node;
    private TextView text;
    private List<T> entities;
    private ListAdapter adapter;

    private long selectedEntityId = 0;

    public MyEntitySelector(Activity activity, MyEntityManager em, ActivityLayout x, boolean isShow,
                            int layoutId, int layoutPlusId, int labelResId, int defaultValueResId) {
        this.activity = activity;
        this.em = em;
        this.x = x;
        this.isShow = isShow;
        this.layoutId = layoutId;
        this.layoutPlusId = layoutPlusId;
        this.labelResId = labelResId;
        this.defaultValueResId = defaultValueResId;
    }

    protected abstract Class getEditActivityClass();

    public void fetchEntities() {
        entities = fetchEntities(em);
        adapter = createAdapter(activity, entities);
    }

    protected abstract List<T> fetchEntities(MyEntityManager em);

    protected abstract ListAdapter createAdapter(Activity activity, List<T> entities);

    public void createNode(LinearLayout layout) {
        if (isShow) {
            text = x.addListNodePlusWithFilter(layout, layoutId, layoutPlusId, labelResId, defaultValueResId);
            node = (View) text.getTag();
        }
    }

    public void onClick(int id) {
        if (id == layoutId) {
            pickEntity();
        } else if (id == layoutPlusId) {
            Intent intent = new Intent(activity, getEditActivityClass());
            activity.startActivityForResult(intent, layoutPlusId);
        }
    }

    private void pickEntity() {
        int selectedEntityPos = MyEntity.indexOf(entities, selectedEntityId);
        x.selectPosition(activity, layoutId, labelResId, adapter, selectedEntityPos);
    }

    public void onSelectedPos(int id, int selectedPos) {
        if (id == layoutId) onEntitySelected(selectedPos);
    }

    private void onEntitySelected(int selectedPos) {
        T e = entities.get(selectedPos);
        selectEntity(e);
    }

    public void selectEntity(long entityId) {
        if (isShow) {
            T e = MyEntity.find(entities, entityId);
            selectEntity(e);
        }
    }

    private void selectEntity(T e) {
        if (isShow && e != null) {
            text.setText(e.title);
            selectedEntityId = e.id;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == layoutPlusId) {
            onNewEntity(data);
        }
    }

    private void onNewEntity(Intent data) {
        fetchEntities();
        long entityId = data.getLongExtra(DatabaseHelper.EntityColumns.ID, -1);
        if (entityId != -1) {
            selectEntity(entityId);
        }
    }

    public void setNodeVisible(boolean visible) {
        if (isShow) {
            setVisibility(node, visible ? View.VISIBLE : View.GONE);
        }
    }

    public long getSelectedEntityId() {
        return node == null || node.getVisibility() == View.GONE ? 0 : selectedEntityId;
    }

}
