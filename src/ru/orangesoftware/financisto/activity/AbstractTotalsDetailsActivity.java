/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public abstract class AbstractTotalsDetailsActivity extends AbstractActivity {

    private LinearLayout layout;
    private View calculatingNode;
    private Utils u;
    protected boolean shouldShowHomeCurrencyTotal = true;

    private final int titleNodeResId;

    protected AbstractTotalsDetailsActivity(int titleNodeResId) {
        this.titleNodeResId = titleNodeResId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.totals_details);

        u = new Utils(this);
        layout = (LinearLayout)findViewById(R.id.list);
        calculatingNode = x.addTitleNodeNoDivider(layout, R.string.calculating);

        Button bOk = (Button)findViewById(R.id.bOK);
        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        internalOnCreate();
        calculateTotals();
    }

    protected void internalOnCreate() {}

    private void calculateTotals() {
        CalculateAccountsTotalsTask task = new CalculateAccountsTotalsTask();
        task.execute();
    }

    @Override
    protected void onClick(View v, int id) {
    }
    
    private class CalculateAccountsTotalsTask extends AsyncTask<Void, Void, TotalsInfo> {

        @Override
        protected TotalsInfo doInBackground(Void... voids) {
            prepareInBackground();
            Total[] totals = getTotals();
            Total totalInHomeCurrency = getTotalInHomeCurrency();
            Currency homeCurrency = totalInHomeCurrency.currency;
            ExchangeRateProvider rates = db.getLatestRates();
            List<TotalInfo> result = new ArrayList<TotalInfo>();
            for (Total total : totals) {
                ExchangeRate rate = rates.getRate(total.currency, homeCurrency);
                TotalInfo info = new TotalInfo(total, rate);
                result.add(info);
            }
            Collections.sort(result, new Comparator<TotalInfo>() {
                @Override
                public int compare(TotalInfo thisTotalInfo, TotalInfo thatTotalInfo) {
                    String thisName = thisTotalInfo.total.currency.name;
                    String thatName = thatTotalInfo.total.currency.name;
                    return thisName.compareTo(thatName);
                }
            });
            return new TotalsInfo(result, totalInHomeCurrency);
        }

        @Override
        protected void onPostExecute(TotalsInfo totals) {
            calculatingNode.setVisibility(View.GONE);
            for (TotalInfo total : totals.totals) {
                String title = getString(titleNodeResId, total.total.currency.name);
                addAmountNode(total.total, title);
            }
            if (shouldShowHomeCurrencyTotal) {
                addAmountNode(totals.totalInHomeCurrency, getString(R.string.home_currency_total));
            }
        }

        private void addAmountNode(Total total, String title) {
            x.addTitleNodeNoDivider(layout, title);
            if (total.isError()) {
                addAmountAndErrorNode(total);
            } else {
                addSingleAmountNode(total);
            }
        }

        private void addAmountAndErrorNode(Total total) {
            TextView data = x.addInfoNode(layout, -1, R.string.not_available, "");
            Drawable dr = getResources().getDrawable(R.drawable.total_error);
            dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
            if (total.currency == Currency.EMPTY) {
                data.setText(R.string.currency_make_default_warning);
            } else {
                data.setText(total.getError(AbstractTotalsDetailsActivity.this));
            }
            data.setError("Error!", dr);
        }

        private void addSingleAmountNode(Total total) {
            TextView label = x.addInfoNodeSingle(layout, -1, "");
            u.setAmountText(label, total);
        }

    }

    protected abstract Total getTotalInHomeCurrency();

    protected abstract Total[] getTotals();

    protected void prepareInBackground() { }

    private static class TotalInfo {

        public final Total total;
        public final ExchangeRate rate;

        public TotalInfo(Total total, ExchangeRate rate) {
            this.total = total;
            this.rate = rate;
        }
    }
    
    private static class TotalsInfo {
        
        public final List<TotalInfo> totals;
        public final Total totalInHomeCurrency;

        public TotalsInfo(List<TotalInfo> totals, Total totalInHomeCurrency) {
            this.totals = totals;
            this.totalInHomeCurrency = totalInHomeCurrency;
        }

    }
    

}
