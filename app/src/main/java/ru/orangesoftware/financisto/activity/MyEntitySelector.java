package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.InputType;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.utils.ArrUtils;
import ru.orangesoftware.financisto.utils.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ru.orangesoftware.financisto.activity.AbstractActivity.setVisibility;

public abstract class MyEntitySelector<T extends MyEntity, A extends AbstractActivity> {

    protected final A activity;
    protected final MyEntityManager em;

    private final Class<T> entityClass;
    private final ActivityLayout x;
    private final boolean isShow;
    private final int layoutId, actBtnId, clearBtnId, labelResId, defaultValueResId, filterToggleId, showListId;

    private View node;
    private TextView text;
    private AutoCompleteTextView autoCompleteFilter;
    private SimpleCursorAdapter filterAdapter;
    private List<T> entities = Collections.emptyList();
    private ListAdapter adapter;
    private boolean isMultiSelect;

    private long selectedEntityId = 0;

    MyEntitySelector(
            Class<T> entityClass,
            A activity,
            MyEntityManager em,
            ActivityLayout x,
            boolean isShow,
            int layoutId,
            int actBtnId,
            int clearBtnId,
            int labelResId,
            int defaultValueResId,
            int filterToggleId,
            int showListId) {
        this.entityClass = entityClass;
        this.activity = activity;
        this.em = em;
        this.x = x;
        this.isShow = isShow;
        this.layoutId = layoutId;
        this.actBtnId = actBtnId;
        this.clearBtnId = clearBtnId;
        this.labelResId = labelResId;
        this.defaultValueResId = defaultValueResId;
        this.filterToggleId = filterToggleId;
        this.showListId = showListId;
    }

    protected abstract Class getEditActivityClass();

    public List<T> getEntities() {
        return entities;
    }

    public void setEntities(List<T> entities) {
        this.entities = entities;
    }

    public void fetchEntities() {
        entities = fetchEntities(em);
        if (!isMultiSelect) {
            adapter = createAdapter(activity, entities);
        }
    }

    protected abstract List<T> fetchEntities(MyEntityManager em);

    protected abstract ListAdapter createAdapter(Activity activity, List<T> entities);

    protected abstract SimpleCursorAdapter createFilterAdapter();

    public TextView createNode(LinearLayout layout) {
        return createNode(layout, R.layout.select_entry_with_2btn_and_filter);
    }

    public TextView createNode(LinearLayout layout, int nodeLayoutId) {
        if (isShow) {
            final Pair<TextView, AutoCompleteTextView> views = x.addListNodeWithButtonsAndFilter(layout, nodeLayoutId, layoutId, actBtnId, clearBtnId,
                    labelResId, defaultValueResId, filterToggleId, showListId);
            text = views.first;
            autoCompleteFilter = views.second;
            node = (View) text.getTag();
        }
        return text;
    }

    private void initAutoCompleteFilter(final AutoCompleteTextView filterText) {
        filterAdapter = createFilterAdapter();
        filterText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_FILTER);
        filterText.setThreshold(1);
        filterText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                filterText.setAdapter(filterAdapter);
                filterText.selectAll();
            }
        });
        filterText.setOnItemClickListener((parent, view, position, id) -> {
            activity.onSelectedId(layoutId, id);
            closeFilter();
        });
    }

    public void onClick(int id) {
        if (id == layoutId) {
            if (/*isMultiSelect() || */isListPick()) {
                pickEntity();
            } else {
                openFilter();
            }
        } else if (id == actBtnId) {
            Intent intent = new Intent(activity, getEditActivityClass());
            activity.startActivityForResult(intent, actBtnId);
        } else if (id == filterToggleId) {
            closeFilter();
        } else if (id == showListId) {
            closeFilter();
            pickEntity();
        } else if (id == clearBtnId) {
            clearSelection();
        }
    }

    private void openFilter() {
        initFilterAdapter();
        setFilterVisibility(VISIBLE);
        if (isMultiSelect || selectedEntityId <= 0) {
            autoCompleteFilter.setText("");
        }
        Utils.openSoftKeyboard(autoCompleteFilter, activity);
    }

    private void closeFilter() {
        setFilterVisibility(GONE);
        Utils.closeSoftKeyboard(autoCompleteFilter, activity);
    }

    private void setFilterVisibility(int visibility) {
        autoCompleteFilter.setVisibility(visibility);
        setViewVisibility(R.id.filterToggle, visibility);
        setViewVisibility(R.id.list, visibility);
        node.findViewById(R.id.list_node_row).setVisibility(visibility == VISIBLE ? GONE : VISIBLE);
    }

    private void setViewVisibility(int id, int visibility) {
        ((View) autoCompleteFilter.getTag(id)).setVisibility(visibility);
    }

    protected abstract boolean isListPick();

    private void initFilterAdapter() {
        if (filterAdapter == null) initAutoCompleteFilter(autoCompleteFilter);
    }

    private void clearSelection() {
        text.setText(defaultValueResId);
        selectedEntityId = 0;
        autoCompleteFilter.setText("");
        for (MyEntity e : getEntities()) e.setChecked(false);
        showHideMinusBtn(false);
    }

    private void showHideMinusBtn(boolean show) {
        ImageView minusBtn = (ImageView) text.getTag(R.id.bMinus);
        if (minusBtn != null) minusBtn.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void pickEntity() {
        if (isMultiSelect) {
            x.selectMultiChoice(activity, layoutId, labelResId, entities);
        } else {
            int selectedEntityPos = MyEntity.indexOf(entities, selectedEntityId);
            x.selectPosition(activity, layoutId, labelResId, adapter, selectedEntityPos);
        }
    }

    public void onSelectedPos(int id, int selectedPos) {
        if (id == layoutId) onEntitySelected(selectedPos);
    }

    public void onSelectedId(int id, long selectedId) {
        if (id != layoutId) return;

        selectEntity(selectedId);
    }

    public void onSelected(int id, List<? extends MultiChoiceItem> ignore) {
        if (id == layoutId) fillCheckedEntitiesInUI();
    }

    public void fillCheckedEntitiesInUI() {
        String selectedProjects = getCheckedTitles();
        if (Utils.isEmpty(selectedProjects)) {
            clearSelection();
        } else {
            text.setText(selectedProjects);
            showHideMinusBtn(true);
        }
    }


    public String getCheckedTitles() {
        return getCheckedTitles(entities);
    }

    public static String getCheckedTitles(List<? extends MyEntity> list) {
        StringBuilder sb = new StringBuilder();
        for (MyEntity e : list) {
            if (e.checked) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(e.title);
            }
        }
        return sb.toString();
    }

    public String[] getCheckedIds() {
        return getCheckedIds(entities);
    }

    public static String[] getCheckedIds(List<? extends MyEntity> list) {
        List<String> res = new LinkedList<>();
        for (MyEntity e : list) {
            if (e.checked) {
                res.add(String.valueOf(e.id));
            }
        }
        return ArrUtils.strListToArr(res);
    }

    public String getCheckedIdsAsStr() {
        return getCheckedIdsAsStr(this.entities);
    }

    public static String getCheckedIdsAsStr(List<? extends MyEntity> list) {
        StringBuilder sb = new StringBuilder();
        for (MyEntity e : list) {
            if (e.checked) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(e.id);
            }
        }
        return sb.toString();
    }

    private void onEntitySelected(int selectedPos) {
        T e = entities.get(selectedPos);
        selectEntity(e);
    }

    public void selectEntity(long entityId) {
        if (isShow) {
            if (isMultiSelect) {
                updateCheckedEntities("" + entityId);
                fillCheckedEntitiesInUI();
            } else {
                T e = MyEntity.find(entities, entityId);
                selectEntity(e);
            }
        }
    }

    private void selectEntity(T e) {
        if (isShow) {
            if (e == null) {
                clearSelection();
            } else {
                autoCompleteFilter.setText(e.title);
                text.setText(e.title);
                selectedEntityId = e.id;
                showHideMinusBtn(e.id > 0);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == actBtnId) {
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

    public boolean isShow() {
        return isShow;
    }

    public void initMultiSelect() {
        this.isMultiSelect = true;
        fetchEntities();
    }

    public void updateCheckedEntities(String checkedCommaIds) {
        updateCheckedEntities(this.entities, checkedCommaIds);
    }

    public static void updateCheckedEntities(List<? extends MyEntity> list, String checkedCommaIds) {
        if (!Utils.isEmpty(checkedCommaIds)) {
            updateCheckedEntities(list, checkedCommaIds.split(","));
        }

    }

    public void updateCheckedEntities(String[] checkedIds) {
        updateCheckedEntities(this.entities, checkedIds);
    }

    public static void updateCheckedEntities(List<? extends MyEntity> list, String[] checkedIds) {
        for (String s : checkedIds) {
            long id = Long.parseLong(s);
            for (MyEntity e : list) {
                if (e.id == id) {
                    e.checked = true;
                    break;
                }
            }
        }
    }

    public void onDestroy() {
        if (filterAdapter != null) {
            filterAdapter.changeCursor(null);
            filterAdapter = null;
        }
    }


    public boolean isTyping() {
        if (isShow() && !isListPick()) {
            ToggleButton toggleBtn = (ToggleButton) autoCompleteFilter.getTag();
            return toggleBtn.isChecked();
        }
        return false;
    }

    public boolean askToCreateIfTyping() {
        if (isTyping()) {
            Utils.closeSoftKeyboard(autoCompleteFilter, activity);
            String entityTypeName = getEntityTypeName();
            new AlertDialog.Builder(activity)
                    .setTitle(entityTypeName)
                    .setMessage(activity.getString(R.string.create_new_entity_with_title, entityTypeName, filterText()))
                    .setPositiveButton(R.string.create, (dialog, which) -> createNewEntity())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                    .show();
            return true;
        }
        return false;
    }

    private void createNewEntity() {
        T e = em.findOrInsertEntityByTitle(entityClass, filterText());
        selectEntity(e);
        closeFilter();
    }

    @NonNull
    private String filterText() {
        return autoCompleteFilter.getText().toString();
    }

    protected abstract String getEntityTypeName();
}
