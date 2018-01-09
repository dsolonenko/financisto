/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.GenericViewHolder;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.utils.Utils.formatRateDate;

public class ExchangeRatesListActivity extends AbstractListActivity {

    private static final int ADD_RATE = 1;
    private static final int EDIT_RATE = 1;

    private final DecimalFormat nf = new DecimalFormat("0.00000");

    private Spinner fromCurrencySpinner;
    private Spinner toCurrencySpinner;
    private List<Currency> currencies;

    private long lastSelectedCurrencyId;

    public ExchangeRatesListActivity() {
        super(R.layout.exchange_rate_list);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        super.internalOnCreate(savedInstanceState);
        currencies = db.getAllCurrenciesList("name");

        fromCurrencySpinner = findViewById(R.id.spinnerFromCurrency);
        fromCurrencySpinner.setPromptId(R.string.rate_from_currency);
        toCurrencySpinner = findViewById(R.id.spinnerToCurrency);
        toCurrencySpinner.setPromptId(R.string.rate_to_currency);

        if (currencies.size() > 0) {
            toCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    updateAdapter();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            fromCurrencySpinner.setAdapter(createCurrencyAdapter(currencies));
            fromCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                    List<Currency> currencies = getCurrenciesButSelected(id);
                    if (currencies.size() > 0) {
                        int position = findSelectedCurrency(currencies, lastSelectedCurrencyId);
                        toCurrencySpinner.setAdapter(createCurrencyAdapter(currencies));
                        toCurrencySpinner.setSelection(position);
                    }
                    lastSelectedCurrencyId = id;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            fromCurrencySpinner.setSelection(findDefaultCurrency());

            ImageButton bFlip = findViewById(R.id.bFlip);
            bFlip.setOnClickListener(arg0 -> flipCurrencies());

            ImageButton bRefresh = findViewById(R.id.bRefresh);
            bRefresh.setOnClickListener(arg0 -> refreshAllRates());
        }
    }

    private SpinnerAdapter createCurrencyAdapter(List<Currency> currencies) {
        ArrayAdapter<Currency> a = new ArrayAdapter<Currency>(this, android.R.layout.simple_spinner_item, currencies) {
            @Override
            public long getItemId(int position) {
                return getItem(position).id;
            }
        };
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private List<Currency> getCurrenciesButSelected(long id) {
        List<Currency> list = new ArrayList<>();
        for (Currency currency : currencies) {
            if (currency.id != id) {
                list.add(currency);
            }
        }
        return list;
    }

    private int findSelectedCurrency(List<Currency> currencies, long id) {
        int i = 0;
        for (Currency currency : currencies) {
            if (currency.id == id) {
                return i;
            }
            ++i;
        }
        return 0;
    }

    private int findDefaultCurrency() {
        int i = 0;
        for (Currency currency : currencies) {
            if (currency.isDefault) {
                return i;
            }
            ++i;
        }
        return 0;
    }

    private void flipCurrencies() {
        Currency toCurrency = (Currency) toCurrencySpinner.getSelectedItem();
        if (toCurrency != null) {
            fromCurrencySpinner.setSelection(findSelectedCurrency(currencies, toCurrency.id));
        }
    }

    private void updateAdapter() {
        Currency fromCurrency = (Currency) fromCurrencySpinner.getSelectedItem();
        Currency toCurrency = (Currency) toCurrencySpinner.getSelectedItem();
        if (fromCurrency != null && toCurrency != null) {
            List<ExchangeRate> rates = db.findRates(fromCurrency, toCurrency);
            ListAdapter adapter = new ExchangeRateListAdapter(this, rates);
            setListAdapter(adapter);
        }
    }

    @Override
    protected void addItem() {
        long fromCurrencyId = fromCurrencySpinner.getSelectedItemId();
        long toCurrencyId = toCurrencySpinner.getSelectedItemId();
        if (fromCurrencyId > 0 && toCurrencyId > 0) {
            Intent intent = new Intent(this, ExchangeRateActivity.class);
            intent.putExtra(ExchangeRateActivity.FROM_CURRENCY_ID, fromCurrencyId);
            intent.putExtra(ExchangeRateActivity.TO_CURRENCY_ID, toCurrencyId);
            startActivityForResult(intent, ADD_RATE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            updateAdapter();
        }
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return null;
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        ExchangeRate rate = (ExchangeRate) getListAdapter().getItem(position);
        db.deleteRate(rate);
        updateAdapter();
    }

    @Override
    protected void editItem(View v, int position, long id) {
        ExchangeRate rate = (ExchangeRate) getListAdapter().getItem(position);
        editRate(rate);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        editItem(v, position, id);
    }

    private void editRate(ExchangeRate rate) {
        Intent intent = new Intent(this, ExchangeRateActivity.class);
        intent.putExtra(ExchangeRateActivity.FROM_CURRENCY_ID, rate.fromCurrencyId);
        intent.putExtra(ExchangeRateActivity.TO_CURRENCY_ID, rate.toCurrencyId);
        intent.putExtra(ExchangeRateActivity.RATE_DATE, rate.date);
        startActivityForResult(intent, EDIT_RATE);
    }

    private void refreshAllRates() {
        new RatesDownloadTask(this).execute();
    }

    private class RatesDownloadTask extends AsyncTask<Void, Void, List<ExchangeRate>> {

        private final Context context;
        private ProgressDialog progressDialog;

        private RatesDownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected List<ExchangeRate> doInBackground(Void... args) {
            List<ExchangeRate> rates = getProvider().getRates(currencies);
            if (isCancelled()) {
                return null;
            } else {
                db.saveDownloadedRates(rates);
                return rates;
            }
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        private void showProgressDialog() {
            String message = context.getString(R.string.downloading_rates, asString(currencies));
            progressDialog = ProgressDialog.show(context, null, message, true, true, dialogInterface -> cancel(true));
        }

        private String asString(List<Currency> currencies) {
            StringBuilder sb = new StringBuilder();
            for (Currency currency : currencies) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(currency.name);
            }
            return sb.toString();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(List<ExchangeRate> result) {
            progressDialog.dismiss();
            if (result != null) {
                showResult(result);
                updateAdapter();
            }
        }

        private void showResult(List<ExchangeRate> result) {
            StringBuilder sb = new StringBuilder();
            for (ExchangeRate rate : result) {
                Currency fromCurrency = CurrencyCache.getCurrency(db, rate.fromCurrencyId);
                Currency toCurrency = CurrencyCache.getCurrency(db, rate.toCurrencyId);
                sb.append(fromCurrency.name).append(" -> ").append(toCurrency.name);
                if (rate.isOk()) {
                    sb.append(" = ").append(nf.format(rate.rate));
                } else {
                    sb.append(" ! ").append(rate.getErrorMessage());
                }
                sb.append(String.format("%n%n"));
            }
            new AlertDialog.Builder(context)
                    .setTitle(R.string.downloading_rates_result)
                    .setMessage(sb.toString())
                    .setNeutralButton(R.string.ok, null)
                    .create().show();
        }

        private ExchangeRateProvider getProvider() {
            return MyPreferences.createExchangeRatesProvider(context);
        }

    }

    private class ExchangeRateListAdapter extends BaseAdapter {

        private final Context context;
        private final LayoutInflater inflater;
        private final List<ExchangeRate> rates;

        private ExchangeRateListAdapter(Context context, List<ExchangeRate> rates) {
            this.context = context;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.rates = rates;
        }

        @Override
        public int getCount() {
            return rates.size();
        }

        @Override
        public ExchangeRate getItem(int i) {
            return rates.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GenericViewHolder v;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.generic_list_item, parent, false);
                v = GenericViewHolder.createAndTag(convertView);
            } else {
                v = (GenericViewHolder) convertView.getTag();
            }
            ExchangeRate rate = getItem(position);
            v.lineView.setText(formatRateDate(context, rate.date));
            v.amountView.setText(nf.format(rate.rate));
            return convertView;
        }
    }

}
