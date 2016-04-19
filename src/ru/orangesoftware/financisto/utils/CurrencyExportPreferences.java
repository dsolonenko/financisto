/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Spinner;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/10/11 2:01 PM
 */
public class CurrencyExportPreferences {

    public static final String EXPORT_DECIMALS = "EXPORT_DECIMALS";
    public static final String EXPORT_DECIMAL_SEPARATOR = "EXPORT_DECIMAL_SEPARATOR";
    public static final String EXPORT_GROUP_SEPARATOR = "EXPORT_GROUP_SEPARATOR";

    private final String prefix;

    public CurrencyExportPreferences(String prefix) {
        this.prefix = prefix.toUpperCase();
    }

    public static Currency fromIntent(Intent data, String prefix) {
        CurrencyExportPreferences preferences = new CurrencyExportPreferences(prefix);
        return preferences.getCurrencyFromIntent(data);
    }

    public void updateIntentFromUI(Activity activity, Intent data) {
        Spinner decimals = (Spinner)activity.findViewById(R.id.spinnerDecimals);
        Spinner decimalSeparators = (Spinner)activity.findViewById(R.id.spinnerDecimalSeparators);
        Spinner groupSeparators = (Spinner)activity.findViewById(R.id.spinnerGroupSeparators);
        data.putExtra(prefix(EXPORT_DECIMALS), 2-decimals.getSelectedItemPosition());
        data.putExtra(prefix(EXPORT_DECIMAL_SEPARATOR), decimalSeparators.getSelectedItem().toString());
        data.putExtra(prefix(EXPORT_GROUP_SEPARATOR), groupSeparators.getSelectedItem().toString());
    }

    private Currency getCurrencyFromIntent(Intent data) {
        Currency currency = new Currency();
        currency.symbol = "";
        currency.decimals = data.getIntExtra(prefix(EXPORT_DECIMALS), 2);
        currency.decimalSeparator = data.getStringExtra(prefix(EXPORT_DECIMAL_SEPARATOR));
        currency.groupSeparator = data.getStringExtra(prefix(EXPORT_GROUP_SEPARATOR));
        return currency;
    }

    private String prefix(String s) {
        return prefix+"_"+s;
    }

    public void savePreferences(Activity activity, SharedPreferences.Editor editor) {
        Spinner decimals = (Spinner)activity.findViewById(R.id.spinnerDecimals);
        Spinner decimalSeparators = (Spinner)activity.findViewById(R.id.spinnerDecimalSeparators);
        Spinner groupSeparators = (Spinner)activity.findViewById(R.id.spinnerGroupSeparators);
		editor.putInt(prefix(EXPORT_DECIMALS), decimals.getSelectedItemPosition());
		editor.putInt(prefix(EXPORT_DECIMAL_SEPARATOR), decimalSeparators.getSelectedItemPosition());
		editor.putInt(prefix(EXPORT_GROUP_SEPARATOR), groupSeparators.getSelectedItemPosition());
    }

    public void restorePreferences(Activity activity, SharedPreferences preferences) {
        Spinner decimals = (Spinner)activity.findViewById(R.id.spinnerDecimals);
        Spinner decimalSeparators = (Spinner)activity.findViewById(R.id.spinnerDecimalSeparators);
        Spinner groupSeparators = (Spinner)activity.findViewById(R.id.spinnerGroupSeparators);
		decimals.setSelection(preferences.getInt(prefix(EXPORT_DECIMALS), 0));
		decimalSeparators.setSelection(preferences.getInt(prefix(EXPORT_DECIMAL_SEPARATOR), 0));
		groupSeparators.setSelection(preferences.getInt(prefix(EXPORT_GROUP_SEPARATOR), 3));
    }

}
