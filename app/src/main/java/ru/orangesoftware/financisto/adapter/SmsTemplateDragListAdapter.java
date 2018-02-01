/*
 * Copyright 2014 Magnus Woxblom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.orangesoftware.financisto.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.woxthebox.draglistview.DragItemAdapter;
import java.util.List;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.StringUtil;

public class SmsTemplateDragListAdapter extends DragItemAdapter<SmsTemplate, SmsTemplateDragListAdapter.SmsTemplateListViewHolder> {

    public SmsTemplateDragListAdapter(List<SmsTemplate> list) {
        setItemList(list);
    }

    @Override
    public SmsTemplateListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.generic_draglist_item, parent, false);
        return new SmsTemplateListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SmsTemplateListViewHolder h, int position) {
        super.onBindViewHolder(h, position);

        SmsTemplate item = mItemList.get(position);
        if (item != null) {
            h.itemView.setTag(mItemList.get(position)); // todo.mb: recheck if needed?

            h.lineView.setText(item.title);
            h.numberView.setText(StringUtil.getShortString(item.template, 40));
            h.amountView.setVisibility(View.VISIBLE);
            h.amountView.setText(Category.getTitle(item.categoryName, item.categoryLevel));
        }
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).id;
    }

    class SmsTemplateListViewHolder extends DragItemAdapter.ViewHolder {
        public TextView lineView;
        public TextView labelView;
        public TextView numberView;
        public TextView amountView;
        public ImageView iconView;

        public SmsTemplateListViewHolder(View view) {
            super(view, R.id.icon, false);

            lineView = view.findViewById(R.id.line1);
            labelView = view.findViewById(R.id.label);
            numberView = view.findViewById(R.id.number);
            amountView = view.findViewById(R.id.date);
            iconView = view.findViewById(R.id.icon);
        }

        @Override
        public void onItemClicked(View view) {
            Toast.makeText(view.getContext(), "Item clicked", Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onItemLongClicked(View view) {
            Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}
