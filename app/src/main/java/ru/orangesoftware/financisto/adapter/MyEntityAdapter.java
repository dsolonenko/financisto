package ru.orangesoftware.financisto.adapter;

import java.util.List;

import ru.orangesoftware.financisto.model.MyEntity;
import android.content.Context;
import android.widget.ArrayAdapter;

public class MyEntityAdapter<T extends MyEntity> extends ArrayAdapter<T> {

	public MyEntityAdapter(Context context, int resource,
			int textViewResourceId, List<T> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int resource,
			int textViewResourceId, T[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public MyEntityAdapter(Context context, int textViewResourceId,
			List<T> objects) {
		super(context, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int textViewResourceId, T[] objects) {
		super(context, textViewResourceId, objects);
	}

	public MyEntityAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}

}
