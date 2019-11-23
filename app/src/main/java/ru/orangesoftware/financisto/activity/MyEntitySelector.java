package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
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
    private final int layoutId, actBtnId, clearBtnId, labelResId, defaultValueResId, showListId;

    private View node;
    private TextView text;
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
            if (isListPick()) {
                if (isMultiSelect) {
                    text = x.addFilterNodeMinus(layout, layoutId, clearBtnId, labelResId, defaultValueResId);
                    node = (View) text.getTag();
                } else {
                    text = x.addListNodePlus(layout, layoutId, actBtnId, labelResId, defaultValueResId);
                    node = (View) text.getTag();
                }
            } else {
                AutoCompleteTextView autoCompleteView = x.addListNodeWithButtonsAndFilter(layout, nodeLayoutId, layoutId, actBtnId, clearBtnId,
                        labelResId, defaultValueResId, showListId);
                text = autoCompleteView;
                node = (View) autoCompleteView.getTag();
                initAutoCompleteFilter(autoCompleteView);
            }
        }
        return text;
    }

    private void initAutoCompleteFilter(AutoCompleteTextView autoCompleteView) {
        filterAdapter = createFilterAdapter();
        autoCompleteView.setAdapter(filterAdapter);
        autoCompleteView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_FILTER);
        autoCompleteView.setThreshold(1);
        autoCompleteView.setOnItemClickListener((parent, view, position, id) -> activity.onSelectedId(layoutId, id));
    }

    public void onClick(int id) {
        if (id == layoutId) {
            if (/*isMultiSelect() || */isListPick()) {
                pickEntity();
            }
        } else if (id == actBtnId) {
            Intent intent = new Intent(activity, getEditActivityClass());
            activity.startActivityForResult(intent, actBtnId);
        } else if (id == showListId) {
            pickEntity();
        } else if (id == clearBtnId) {
            clearSelection();
        }
    }

    protected boolean isListPick() {
        return isMultiSelect || isListPickConfigured();
    }

    protected abstract boolean isListPickConfigured();

    private void clearSelection() {
        selectedEntityId = 0;
        text.setText("");
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
        String selectedEntities = getCheckedTitles();
        if (Utils.isEmpty(selectedEntities)) {
            clearSelection();
        } else {
            text.setText(selectedEntities);
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
                selectedEntityId = e.id;
                if (e.id > 0) {
                    if (isListPick()) {
                        text.setText(e.title);
                    } else {
                        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)text;
                        autoCompleteTextView.setText(e.title, false);
                    }
                    showHideMinusBtn(true);
                } else {
                    text.setText("");
                    showHideMinusBtn(false);
                }
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

    void createNewEntity() {
        if (text != null && selectedEntityId == 0) {
            T e = em.findOrInsertEntityByTitle(entityClass, filterText());
            selectEntity(e);
        }
    }

    private String filterText() {
        if (text == null) return "";
        return text.getText().toString();
    }

}
