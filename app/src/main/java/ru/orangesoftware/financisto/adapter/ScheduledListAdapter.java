/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ru.orangesoftware.financisto.utils.TransactionTitleUtils.generateTransactionTitle;

public class ScheduledListAdapter extends BaseAdapter {

    private final StringBuilder sb = new StringBuilder();
    private final Date dt = new Date();
    private final int transferColor;
    private final int scheduledColor;
    private final Drawable icBlotterIncome;
    private final Drawable icBlotterExpense;
    private final Drawable icBlotterTransfer;
    private final Utils u;

    private final Context context;
    private final LayoutInflater inflater;

    private Date now = new Date();
    private List<TransactionInfo> transactions;

    public ScheduledListAdapter(Context context, List<TransactionInfo> transactions) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.transferColor = context.getResources().getColor(R.color.transfer_color);
        this.scheduledColor = context.getResources().getColor(R.color.scheduled);
        this.icBlotterIncome = context.getResources().getDrawable(R.drawable.ic_blotter_income);
        this.icBlotterExpense = context.getResources().getDrawable(R.drawable.ic_blotter_expense);
        this.icBlotterTransfer = context.getResources().getDrawable(R.drawable.ic_blotter_transfer);
        this.u = new Utils(context);
        this.transactions = transactions;
    }

    public void setTransactions(ArrayList<TransactionInfo> transactions) {
        this.now = new Date();
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    @Override
    public TransactionInfo getItem(int position) {
        return transactions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder v;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.blotter_list_item, parent, false);
            v = Holder.create(convertView);
        } else {
            v = (Holder) convertView.getTag();
        }
        TransactionInfo t = getItem(position);
        if (t.nextDateTime != null && t.nextDateTime.after(now)) {
            v.indicator.setBackgroundColor(scheduledColor);
        } else {
            v.indicator.setBackgroundColor(Color.TRANSPARENT);
        }
        TextView noteView = t.isTemplate == 1 ? v.bottomView : v.centerView;
        if (t.toAccount != null) {
            v.topView.setText(R.string.transfer);

            String fromAccountTitle = t.fromAccount.title;
            String toAccountTitle = t.toAccount.title;
            sb.setLength(0);
            sb.append(fromAccountTitle).append(" \u00BB ").append(toAccountTitle);
            noteView.setText(sb.toString());
            noteView.setTextColor(transferColor);

            Currency fromCurrency = t.fromAccount.currency;
            Currency toCurrency = t.toAccount.currency;

            int dateViewColor = v.bottomView.getCurrentTextColor();

            if (fromCurrency.id == toCurrency.id) {
                long amount = Math.abs(t.fromAmount);
                u.setAmountText(v.rightCenterView, fromCurrency, amount, false);
                v.rightCenterView.setTextColor(dateViewColor);
            } else {
                long fromAmount = Math.abs(t.fromAmount);
                long toAmount = t.toAmount;
                sb.setLength(0);
                Utils.amountToString(sb, fromCurrency, fromAmount).append(" \u00BB ");
                Utils.amountToString(sb, toCurrency, toAmount);
                v.rightCenterView.setText(sb.toString());
                v.rightCenterView.setTextColor(dateViewColor);
            }
            v.iconView.setImageDrawable(icBlotterTransfer);
        } else {
            String fromAccountTitle = t.fromAccount.title;
            v.topView.setText(fromAccountTitle);
            String note = t.note;
            String location = "";
            if (t.location != null && t.location.id > 0) {
                location = t.location.title;
            }
            String category = "";
            if (t.category.id > 0) {
                category = t.category.title;
            }
            String payee = t.payee != null ? t.payee.title : null;
            String text = generateTransactionTitle(sb, payee, note, location, t.category.id, category);
            noteView.setText(text);
            noteView.setTextColor(Color.WHITE);

            long amount = t.fromAmount;
            sb.setLength(0);
            u.setAmountText(sb, v.rightCenterView, t.fromAccount.currency, amount, true);
            if (amount > 0) {
                v.iconView.setImageDrawable(icBlotterIncome);
            } else if (amount < 0) {
                v.iconView.setImageDrawable(icBlotterExpense);
            }
        }
        if (t.isTemplate == 1) {
            v.centerView.setText(t.templateName);
        } else {
            String recurrence = t.recurrence;
            if (t.isTemplate == 2 && recurrence != null) {
                if (t.nextDateTime != null) {
                    long nextDateTime = t.nextDateTime.getTime();
                    v.bottomView.setText(DateUtils.formatDateTime(context, nextDateTime,
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
                } else {
                    v.bottomView.setText("?");
                }
                v.bottomView.setTextColor(v.topView.getTextColors().getDefaultColor());
            } else {
                long date = t.dateTime;
                dt.setTime(date);
                v.bottomView.setText(DateUtils.formatDateTime(context, dt.getTime(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
            }
        }
        return convertView;
    }

    private static class Holder {
        RelativeLayout layout;

        TextView indicator;
        TextView topView;
        TextView centerView;
        TextView bottomView;
        TextView rightCenterView;
        ImageView iconView;

        public static Holder create(View view) {
            Holder v = new Holder();
            v.layout = (RelativeLayout) view.findViewById(R.id.layout);
            v.indicator = (TextView) view.findViewById(R.id.indicator);
            v.topView = (TextView) view.findViewById(R.id.top);
            v.centerView = (TextView) view.findViewById(R.id.center);
            v.bottomView = (TextView) view.findViewById(R.id.bottom);
            v.rightCenterView = (TextView) view.findViewById(R.id.right_center);
            v.iconView = (ImageView) view.findViewById(R.id.right_top);
            removeRightView(view, v);
            view.setTag(v);
            return v;
        }

        private static void removeRightView(View view, Holder v) {
            view.findViewById(R.id.right).setVisibility(View.GONE);
        }

    }

}
