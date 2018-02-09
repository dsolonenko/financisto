package ru.orangesoftware.financisto.adapter.async;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
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
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.dragndrop.ItemTouchHelperAdapter;
import ru.orangesoftware.financisto.adapter.dragndrop.ItemTouchHelperViewHolder;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.StringUtil;

public class SmsTemplateListAsyncAdapter extends AsyncAdapter<SmsTemplate, SmsTemplateListAsyncAdapter.LocalViewHolder> implements ItemTouchHelperAdapter {

    static final int MENU_EDIT = Menu.FIRST + 1;
    static final int MENU_DUPLICATE = Menu.FIRST + 1;
    static final int MENU_DELETE = Menu.FIRST + 3;

    public static final String TAG = "777";

    public SmsTemplateListAsyncAdapter(int chunkSize, SmsTemplateListSource itemSource, RecyclerView recyclerView) {
        super(chunkSize, itemSource, recyclerView);
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
//        String prev = mItems.remove(fromPosition);
//        mItems.add(toPosition > fromPosition ? toPosition - 1 : toPosition, prev);
        Log.i(TAG, String.format("moved from %s to %s", fromPosition, toPosition));
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
//        mItems.remove(position);
        Log.i(TAG, String.format("deleted %s pos", position));
        notifyItemRemoved(position);
    }

    static class LocalViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        public TextView lineView;
        public TextView labelView;
        public TextView numberView;
        public TextView amountView;
        public ImageView iconView;

        private final int COLOR_SELECTED = Color.RED;
        private int lineColor, numberColor;

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
                numberView.setText(StringUtil.getShortString(item.template, 40));
                amountView.setVisibility(View.VISIBLE);
                amountView.setText(Category.getTitle(item.categoryName, item.categoryLevel));
            }
        }

        @Override
        public void onItemSelected() {
            numberColor = numberView.getCurrentTextColor();
            lineColor = lineView.getCurrentTextColor();

            lineView.setTextColor(COLOR_SELECTED);
            numberView.setTextColor(COLOR_SELECTED);

            Log.i(TAG, String.format("selected: %s", numberView.getText()));
        }

        @Override
        public void onItemClear() {
            lineView.setTextColor(lineColor);
            numberView.setTextColor(numberColor);
            Log.i(TAG, String.format("move completed: %s", numberView.getText()));
        }
    }

}
