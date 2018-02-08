package ru.orangesoftware.financisto.adapter.async;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.dragndrop.ItemTouchHelperAdapter;
import ru.orangesoftware.financisto.adapter.dragndrop.ItemTouchHelperViewHolder;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.StringUtil;

public class SmsTemplateListAsyncAdapter extends AsyncAdapter<SmsTemplate, SmsTemplateListAsyncAdapter.LocalViewHolder> implements ItemTouchHelperAdapter {

    public static final String TAG = "777";

    public SmsTemplateListAsyncAdapter(int chunkSize, SmsTemplateListSource itemSource, RecyclerView recyclerView) {
        super(chunkSize, itemSource, recyclerView);
    }

    @Override
    public LocalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.generic_list_item, parent,false);
        return new LocalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LocalViewHolder holder, int position) {
        holder.bindView(listUtil.getItem(position), position);
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

        public LocalViewHolder(View view) {
            super(view);

            lineView = view.findViewById(R.id.line1);
            labelView = view.findViewById(R.id.label);
            numberView = view.findViewById(R.id.number);
            amountView = view.findViewById(R.id.date);
            iconView = view.findViewById(R.id.icon);
        }

        public void bindView(SmsTemplate item, Integer position) {
            if (item != null) {
                lineView.setText(item.title);
                numberView.setText(StringUtil.getShortString(item.template, 40));
                amountView.setVisibility(View.VISIBLE);
                amountView.setText(Category.getTitle(item.categoryName, item.categoryLevel));
            }
        }

        @Override
        public void onItemSelected() {
            Log.i(TAG, String.format("selected: %s", numberView.getText()));
        }

        @Override
        public void onItemClear() {
            Log.i(TAG, String.format("deleted: %s", numberView.getText()));
        }
    }

}
