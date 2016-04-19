/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.export.csv.Csv;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 6/20/11 6:40 PM
 */
public class CurrencySelector {

    public static interface OnCurrencyCreatedListener {
        void onCreated(long currencyId);
    }

    private final Context context;
    private final MyEntityManager em;
    private final List<List<String>> currencies;
    private final OnCurrencyCreatedListener listener;

    private int selectedCurrency = 0;

    public CurrencySelector(Context context, MyEntityManager em, OnCurrencyCreatedListener listener) {
        this.context = context;
        this.em = em;
        this.listener = listener;
        this.currencies = readCurrenciesFromAsset();
    }

    public void show() {
        String[] items = createItemsList(currencies);
        new AlertDialog.Builder(context)
                .setTitle(R.string.currencies)
                .setIcon(R.drawable.ic_dialog_currency)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addSelectedCurrency(selectedCurrency);
                        dialogInterface.dismiss();
                    }
                })
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedCurrency = i;
                    }
                })
                .show();
    }

    public void addSelectedCurrency(int selectedCurrency) {
        if (selectedCurrency > 0 && selectedCurrency <= currencies.size()) {
            List<String> c = currencies.get(selectedCurrency-1);
            addSelectedCurrency(c);
        } else {
            listener.onCreated(0);
        }
    }

    private void addSelectedCurrency(List<String> list) {
        Currency c = new Currency();
        c.name = list.get(0);
        c.title = list.get(1);
        c.symbol = list.get(2);
        c.decimals = Math.max(0, Math.min(2, Integer.parseInt(list.get(3))));
        c.decimalSeparator = decodeSeparator(list.get(4));
        c.groupSeparator = decodeSeparator(list.get(5));
        c.isDefault = isTheFirstCurrencyAdded();
        em.saveOrUpdate(c);
        CurrencyCache.initialize(em);
        listener.onCreated(c.id);
    }

    private boolean isTheFirstCurrencyAdded() {
        return em.getAllCurrenciesList().isEmpty();
    }

    private String decodeSeparator(String s) {
        if ("COMMA".equals(s)) {
            return "','";
        } else if ("PERIOD".equals(s)) {
            return "'.'";
        } else if ("SPACE".endsWith(s)) {
            return "' '";
        } else {
            return "''";
        }
    }

    private List<List<String>> readCurrenciesFromAsset() {
        try {
            InputStreamReader r = new InputStreamReader(context.getAssets().open("currencies.csv"), "UTF-8");
            try {
                Csv.Reader csv = new Csv.Reader(r).delimiter(',').ignoreComments(true).ignoreEmptyLines(true);
                List<List<String>> allLines = new ArrayList<List<String>>();
                List<String> line;
                while ((line = csv.readLine()) != null) {
                    if (line.size() == 6) {
                        allLines.add(line);
                    }
                }
                return allLines;
            } finally {
                r.close();
            }
        } catch (IOException e) {
            Log.e("Financisto", "IO error while reading currencies", e);
            Toast.makeText(context, e.getClass() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return Collections.emptyList();
    }

    private String[] createItemsList(List<List<String>> currencies) {
        int size = currencies.size();
        String[] items = new String[size+1];
        items[0] = context.getString(R.string.new_currency);
        for (int i=0; i<size; i++) {
            List<String> c = currencies.get(i);
            items[i+1] = c.get(0)+" ("+c.get(1)+")";
        }
        return items;
    }


}
