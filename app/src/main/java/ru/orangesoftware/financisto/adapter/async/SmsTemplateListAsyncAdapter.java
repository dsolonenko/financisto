package ru.orangesoftware.financisto.adapter.async;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import static android.support.v7.widget.helper.ItemTouchHelper.END;
import static android.support.v7.widget.helper.ItemTouchHelper.START;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.dragndrop.ItemTouchHelperAdapter;
import ru.orangesoftware.financisto.adapter.dragndrop.ItemTouchHelperViewHolder;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.MenuItemInfo;

/**
 * Based on https://github.com/jasonwyatt/AsyncListUtil-Example
 */
public class SmsTemplateListAsyncAdapter extends AsyncAdapter<SmsTemplate, SmsTemplateListAsyncAdapter.LocalViewHolder> implements ItemTouchHelperAdapter {
    public static final String TAG = "Financisto." + SmsTemplateListAsyncAdapter.class.getSimpleName();

    static final int MENU_EDIT = Menu.FIRST + 1;
    static final int MENU_DUPLICATE = Menu.FIRST + 1;

    static final int MENU_DELETE = Menu.FIRST + 3;
    private final DatabaseAdapter db;
    private AtomicLong swippedTargetId = new AtomicLong(0);

    public SmsTemplateListAsyncAdapter(int chunkSize, DatabaseAdapter db, SmsTemplateListSource itemSource, RecyclerView recyclerView) {
        super(chunkSize, itemSource, recyclerView);
        this.db = db;
    }

    @Override
    public LocalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.generic_list_item, parent, false);
        view.setOnClickListener(clickedView -> {
            final PopupMenu popupMenu = new PopupMenu(parent.getContext(), clickedView);
            int i = 0;
            for (MenuItemInfo m : createContextMenus()) {
                if (m.enabled) {
                    popupMenu.getMenu().add(0, m.menuId, i++, m.titleId);
                }
            }
            final Long id = (Long) clickedView.getTag(R.id.sms_tpl_id);
            popupMenu.setOnMenuItemClickListener(item -> onPopupItemSelected(item.getItemId(), clickedView, id));
            popupMenu.show();
        });
        return new LocalViewHolder(view);
    }

    protected boolean onPopupItemSelected(int menuId, View itemView, long id) {
        switch (menuId) {
            case MENU_EDIT: {
                editItem(itemView, id);
                return true;
            }
            case MENU_DELETE: {
                deleteItem(itemView, id);
                return true;
            }
        }
        return false;
    }

    protected List<MenuItemInfo> createContextMenus() {
        List<MenuItemInfo> menus = new ArrayList<>(4);
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
        return menus;
    }

    private void editItem(View itemView, long id) {
        Log.i(TAG, "edit for item id=" + id);
    }

    private void deleteItem(View itemView, long id) {
        Log.i(TAG, "delete for item id=" + id);
    }

    @Override
    public void onBindViewHolder(LocalViewHolder holder, int position) {
        final SmsTemplate item = listUtil.getItem(position);
        holder.bindView(item, position);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        final SmsTemplate itemSrc = listUtil.getItem(fromPosition);
        final SmsTemplate itemTarget = listUtil.getItem(toPosition);
        swippedTargetId.set(itemTarget.getId());
        Log.i(TAG, String.format("dragged %s item to %s item", itemSrc.getId(), itemTarget.getId()));
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position, int dir) {
        Log.i(TAG, String.format("swipped %s pos to %s (%s)",
            position, dir == START ? "left" : dir == END ? "right" : "??", dir));
        notifyItemRemoved(position);
    }

    class LocalViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        public TextView lineView;
        public TextView labelView;
        public TextView numberView;
        public TextView amountView;
        public ImageView iconView;

        public LocalViewHolder(View view) {
            super(view);

            lineView = view.findViewById(R.id.line1);
            labelView = view.findViewById(R.id.label);
            numberView = view.findViewById(R.id.number);
            amountView = view.findViewById(R.id.date);
            iconView = view.findViewById(R.id.icon);
        }

        public void bindView(SmsTemplate item, Integer ignore) {
            if (item != null) {
                itemView.setTag(R.id.sms_tpl_id, item.getId());
                lineView.setText(item.title);
                numberView.setText(item.template);
                amountView.setVisibility(View.VISIBLE);
                amountView.setText(Category.getTitle(item.categoryName, item.categoryLevel));
            }
        }

        @Override
        public void onItemSelected() {
            //numberView.setTextColor(Color.RED);
            Log.i(TAG, String.format("selected: %s", numberView.getText()));
        }

        @Override
        public void onItemClear() {
            //numberView.setTextColor(Color.WHITE);
            long targetId = swippedTargetId.get();
            long srcId = (long) itemView.getTag();
            Log.d(TAG, String.format("`%s` moving to `%s`...", numberView.getText(), targetId));

            new UpdateSortOrderTask().execute(srcId, targetId);
        }
    }


    class UpdateSortOrderTask extends AsyncTask<Long, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Long... ids) {
            return db.swapEntitySortOrders(SmsTemplate.class, ids[0], ids[1]);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            Log.i(TAG, "moved finished: " + res);
        }
    }
}
