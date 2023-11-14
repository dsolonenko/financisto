package ru.orangesoftware.financisto.activity;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import ru.orangesoftware.financisto.R;

public class UiUtils {

    public static void applyTheme(Context context, DatePickerDialog d) {
        d.setAccentColor(ContextCompat.getColor(context, R.color.colorPrimary));
        d.setThemeDark(true);
    }

    public static void applyTheme(Context context, TimePickerDialog d) {
        d.setAccentColor(ContextCompat.getColor(context, R.color.colorPrimary));
        d.setThemeDark(true);
    }

}
