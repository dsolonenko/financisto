package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.EntityEnum;

public class EntityEnumAdapter<T extends EntityEnum> extends BaseAdapter {

    private final Context context;
    private final T[] values;
    private final boolean tint;

    public EntityEnumAdapter(Context context, T[] values, boolean tint) {
        this.values = values;
        this.context = context;
        this.tint = tint;
    }

    @Override
    public int getCount() {
        return values.length;
    }

    @Override
    public T getItem(int i) {
        return values[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = EntityEnumViewHolder.create(context, convertView, parent);
        EntityEnumViewHolder holder = (EntityEnumViewHolder) view.getTag();
        T v = values[position];
        holder.icon.setImageResource(v.getIconId());
        if (tint) {
            holder.icon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
        }
        holder.title.setText(v.getTitleId());
        return view;
    }

    private static final class EntityEnumViewHolder {

        public final ImageView icon;
        public final TextView title;

        private EntityEnumViewHolder(ImageView icon, TextView title) {
            this.icon = icon;
            this.title = title;
        }

        private static View create(Context context, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.entity_enum_list_item, parent, false);
                view.setTag(create(view));
                return view;
            } else {
                return convertView;
            }
        }

        private static EntityEnumViewHolder create(View convertView) {
            ImageView icon = convertView.findViewById(R.id.icon);
            TextView title = convertView.findViewById(R.id.line1);
            EntityEnumViewHolder holder = new EntityEnumViewHolder(icon, title);
            convertView.setTag(holder);
            return holder;
        }
    }

}
