package ru.orangesoftware.financisto.adapter.async;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.SmsTemplate;

/**
 * @author Mikhail Baturov
 * @since 31/01/18
 */
public class SmsTemplateListAsyncAdapter extends AsyncAdapter<SmsTemplate> {

    public SmsTemplateListAsyncAdapter(int chunkSize,
        ItemSource<SmsTemplate> itemSource, RecyclerView recyclerView) {
        super(chunkSize, itemSource, recyclerView);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        throw new UnsupportedOperationException("#onCreateViewHolder()");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        throw new UnsupportedOperationException("#onBindViewHolder()");
    }


    public class SmsTemplateListViewHolder extends RecyclerView.ViewHolder {
        public TextView lineView;
        public TextView labelView;
        public TextView numberView;
        public TextView amountView;
        public ImageView iconView;

        public SmsTemplateListViewHolder(View view) {
            super(view);

            lineView = (TextView)view.findViewById(R.id.line1);
            labelView = (TextView)view.findViewById(R.id.label);
            numberView = (TextView)view.findViewById(R.id.number);
            amountView = (TextView)view.findViewById(R.id.date);
            iconView = (ImageView) view.findViewById(R.id.icon);
        }

        public void bindView(SmsTemplate item, Integer position) {
            if (item == null) {
                return;
            }
//            title.setText(position + " : " + item.title);
//            content.setText(item.content != null ? item.content : "loading");
        }
    }

}
