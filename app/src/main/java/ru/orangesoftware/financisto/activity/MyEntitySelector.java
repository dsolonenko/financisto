package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

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

import static ru.orangesoftware.financisto.activity.AbstractActivity.setVisibility;

public abstract class MyEntitySelector<T extends MyEntity, A extends AbstractActivity> {

    protected final A activity;
    protected final MyEntityManager em;

    private final Class<T> entityClass;
    private final ActivityLayout x;
    private final boolean isShow;
    private final int layoutId, actBtnId, clearBtnId, labelResId, defaultValueResId, showListId, closeFilterId, showFilterId;

    private View node;
    private ActivityLayout.FilterNode filterNode;
    private TextView text;
    private AutoCompleteTextView autoCompleteView;
    private List<T> entities = Collections.emptyList();
    private ListAdapter adapter;
    private boolean isMultiSelect;

    private boolean initAutoComplete = true;
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
            int showListId,
            int closeFilterId,
            int showFilterId) {
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
        this.showListId = showListId;
        this.closeFilterId = closeFilterId;
        this.showFilterId = showFilterId;
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

    protected abstract ArrayAdapter<T> createFilterAdapter();

    public TextView createNode(LinearLayout layout) {
        if (isShow) {
            filterNode = x.addFilterNode(layout, layoutId, isMultiSelect ? -1 : actBtnId, clearBtnId,
                    labelResId, defaultValueResId, showListId, closeFilterId, showFilterId);
            text = filterNode.textView;
            node = filterNode.nodeLayout;
            autoCompleteView = filterNode.autoCompleteTextView;
        }
        return text;
    }

    private void initAutoCompleteFilter() {
        if (initAutoComplete) {
            ArrayAdapter<T> filterAdapter = createFilterAdapter();
            autoCompleteView.setAdapter(filterAdapter);
            autoCompleteView.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    | InputType.TYPE_TEXT_VARIATION_FILTER);
            autoCompleteView.setThreshold(1);
            autoCompleteView.setOnItemClickListener((parent, view, position, id) -> {
                T e = filterAdapter.getItem(position);
                activity.onSelectedId(layoutId, e.id);
            });
            initAutoComplete = false;
        }
    }

    public void onClick(int id) {
        if (id == layoutId) {
            if (isListPick()) {
                pickEntity();
            } else {
                showFilter();
            }
        } else if (id == actBtnId) {
            createEntity();
        } else if (id == showListId) {
            pickEntity();
        } else if (id == clearBtnId) {
            clearSelection();
        } else if (id == showFilterId) {
            showFilter();
        } else if (id == closeFilterId) {
            filterNode.hideFilter();
        }
    }

    private void createEntity() {
        Intent intent = new Intent(activity, getEditActivityClass());
        activity.startActivityForResult(intent, actBtnId);
    }

    private void showFilter() {
        initAutoCompleteFilter();
        filterNode.showFilter();
    }

    protected boolean isListPick() {
        return isListPickConfigured();
    }

    protected abstract boolean isListPickConfigured();

    private void clearSelection() {
        selectedEntityId = 0;
        if (text != null) {
            text.setText(defaultValueResId);
            showHideMinusBtn(false);
        }
        for (MyEntity e : getEntities()) e.setChecked(false);
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
        String selectedEntities = getCheckedTitles();
        if (Utils.isEmpty(selectedEntities)) {
            clearSelection();
        } else {
            text.setText(selectedEntities);
            showHideMinusBtn(true);
        }
        if (filterNode != null) {
            filterNode.hideFilter();
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

    public void selectEntity(T e) {
        if (isShow) {
            if (e == null) {
                clearSelection();
            } else {
                selectedEntityId = e.id;
                if (e.id > 0) {
                    text.setText(e.title);
                    showHideMinusBtn(true);
                } else {
                    text.setText(defaultValueResId);
                    showHideMinusBtn(false);
                }
            }
            if (filterNode != null) {
                filterNode.hideFilter();
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

    void createNewEntity() {
        if (filterNode != null && filterNode.isFilterOn() && selectedEntityId == 0) {
            String filterText = autoCompleteView.getText().toString();
            T e = em.findOrInsertEntityByTitle(entityClass, filterText);
            selectEntity(e);
        }
    }

}
