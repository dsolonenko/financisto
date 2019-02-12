package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.MyEntity;

public class EntityListAdapter<T extends MyEntity> extends BaseAdapter {

    private final Context context;
    private List<T> entities;

    public EntityListAdapter(Context context, List<T> entities) {
        this.context = context;
        this.entities = entities;
    }

    public void setEntities(List<T> entities) {
        this.entities = entities;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return entities.size();
    }

    @Override
    public T getItem(int i) {
        return entities.get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = EntityEnumViewHolder.create(context, convertView, parent);
        EntityEnumViewHolder holder = (EntityEnumViewHolder) view.getTag();
        MyEntity e = getItem(position);
        holder.title.setText(e.title);
        holder.icon.setImageResource(e.isActive ? R.drawable.entity_active_icon : R.drawable.entity_inactive_icon);
        return view;
    }

    private static class EntityEnumViewHolder {

        public final ImageView icon;
        public final TextView title;

        private EntityEnumViewHolder(ImageView icon, TextView title) {
            this.icon = icon;
            this.title = title;
        }

        public static View create(Context context, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.entity_list_item, parent, false);
                view.setTag(EntityEnumViewHolder.create(view));
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
