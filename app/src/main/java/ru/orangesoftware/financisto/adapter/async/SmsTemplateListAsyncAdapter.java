package ru.orangesoftware.financisto.adapter.async;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.StringUtil;

public class SmsTemplateListAsyncAdapter extends AsyncAdapter<SmsTemplate, SmsTemplateListAsyncAdapter.SmsTemplateListViewHolder> {

    public SmsTemplateListAsyncAdapter(int chunkSize, SmsTemplateListSource itemSource, RecyclerView recyclerView) {
        super(chunkSize, itemSource, recyclerView);
    }

    @Override
    public SmsTemplateListAsyncAdapter.SmsTemplateListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.generic_list_item, parent,false);
        return new SmsTemplateListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SmsTemplateListViewHolder holder, int position) {
        holder.bindView(listUtil.getItem(position), position);
    }

    public static class SmsTemplateListViewHolder extends RecyclerView.ViewHolder {
        public TextView lineView;
        public TextView labelView;
        public TextView numberView;
        public TextView amountView;
        public ImageView iconView;

        public SmsTemplateListViewHolder(View view) {
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
    }

}
