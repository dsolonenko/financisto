package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.filter.WhereFilter;

class FilterState {

    static void updateFilterColor(Context context, WhereFilter filter, ImageButton button) {
        int color = filter.isEmpty() ? ContextCompat.getColor(context, R.color.bottom_bar_tint) : ContextCompat.getColor(context, R.color.holo_blue_dark);
        button.setColorFilter(color);
    }

}
