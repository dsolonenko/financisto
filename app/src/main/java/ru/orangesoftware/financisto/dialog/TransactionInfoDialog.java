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
package ru.orangesoftware.financisto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.BlotterActivity;
import ru.orangesoftware.financisto.activity.BlotterOperations;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionAttributeInfo;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.recur.Recurrence;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.NodeInflater;

import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;

public class TransactionInfoDialog {

    private final Context context;
    private final DatabaseAdapter db;
    private final NodeInflater inflater;
    private final LayoutInflater layoutInflater;
    private final int splitPadding;
    private final Utils u;

    public TransactionInfoDialog(Context context, DatabaseAdapter db, NodeInflater inflater) {
        this.context = context;
        this.db = db;
        this.inflater = inflater;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.splitPadding = context.getResources().getDimensionPixelSize(R.dimen.transaction_icon_padding);
        this.u = new Utils(context);
    }

    public void show(BlotterActivity blotterActivity, long transactionId) {
        TransactionInfo ti = db.getTransactionInfo(transactionId);
        if (ti == null) {
            Toast t = Toast.makeText(blotterActivity, R.string.no_transaction_found, Toast.LENGTH_LONG);
            t.show();
            return;
        }
        if (ti.parentId > 0) {
            ti = db.getTransactionInfo(ti.parentId);
        }
        View v = layoutInflater.inflate(R.layout.info_dialog, null);
        LinearLayout layout = v.findViewById(R.id.list);

        View titleView = createTitleView(ti, layout);
        createMainInfoNodes(ti, layout);
        createAdditionalInfoNodes(ti, layout);

        showDialog(blotterActivity, transactionId, v, titleView);
    }

    private void createMainInfoNodes(TransactionInfo ti, LinearLayout layout) {
        if (ti.toAccount == null) {
            createLayoutForTransaction(ti, layout);
        } else {
            createLayoutForTransfer(ti, layout);
        }
    }

    private void createLayoutForTransaction(TransactionInfo ti, LinearLayout layout) {
        Account fromAccount = ti.fromAccount;
        AccountType formAccountType = AccountType.valueOf(ti.fromAccount.type);
        add(layout, R.string.account, ti.fromAccount.title, formAccountType);
        if (ti.payee != null) {
            add(layout, R.string.payee, ti.payee.title);
        }
        add(layout, R.string.category, ti.category.title);
        if (ti.originalCurrency != null) {
            TextView amount = add(layout, R.string.original_amount, "");
            u.setAmountText(amount, ti.originalCurrency, ti.originalFromAmount, true);
        }
        TextView amount = add(layout, R.string.amount, "");
        u.setAmountText(amount, ti.fromAccount.currency, ti.fromAmount, true);
        if (ti.category.isSplit()) {
            List<Transaction> splits = db.getSplitsForTransaction(ti.id);
            for (Transaction split : splits) {
                addSplitInfo(layout, fromAccount, split);
            }
        }
    }

    private void addSplitInfo(LinearLayout layout, Account fromAccount, Transaction split) {
        if (split.isTransfer()) {
            Account toAccount = db.getAccount(split.toAccountId);
            String title = u.getTransferTitleText(fromAccount, toAccount);
            LinearLayout topLayout = add(layout, title, "");
            TextView amountView = topLayout.findViewById(R.id.data);
            u.setTransferAmountText(amountView, fromAccount.currency, split.fromAmount, toAccount.currency, split.toAmount);
            topLayout.setPadding(splitPadding, 0, 0, 0);
        } else {
            Category c = db.getCategoryWithParent(split.categoryId);
            StringBuilder sb = new StringBuilder();
            if (c != null && c.id > 0) {
                sb.append(c.title);
            }
            if (isNotEmpty(split.note)) {
                sb.append(" (").append(split.note).append(")");
            }
            LinearLayout topLayout = add(layout, sb.toString(), "");
            TextView amountView = topLayout.findViewById(R.id.data);
            u.setAmountText(amountView, fromAccount.currency, split.fromAmount, true);
            topLayout.setPadding(splitPadding, 0, 0, 0);
        }
    }

    private void createLayoutForTransfer(TransactionInfo ti, LinearLayout layout) {
        AccountType fromAccountType = AccountType.valueOf(ti.fromAccount.type);
        add(layout, R.string.account_from, ti.fromAccount.title, fromAccountType);
        TextView amountView = add(layout, R.string.amount_from, "");
        u.setAmountText(amountView, ti.fromAccount.currency, ti.fromAmount, true);
        AccountType toAccountType = AccountType.valueOf(ti.toAccount.type);
        add(layout, R.string.account_to, ti.toAccount.title, toAccountType);
        amountView = add(layout, R.string.amount_to, "");
        u.setAmountText(amountView, ti.toAccount.currency, ti.toAmount, true);
        if (MyPreferences.isShowPayeeInTransfers(context)) {
            add(layout, R.string.payee, ti.payee != null ? ti.payee.title : "");
        }
        if (MyPreferences.isShowCategoryInTransferScreen(context)) {
            add(layout, R.string.category, ti.category != null ? ti.category.title : "");
        }
    }

    private void createAdditionalInfoNodes(TransactionInfo ti, LinearLayout layout) {
        List<TransactionAttributeInfo> attributes = db.getAttributesForTransaction(ti.id);
        for (TransactionAttributeInfo tai : attributes) {
            String value = tai.getValue(context);
            if (isNotEmpty(value)) {
                add(layout, tai.name, value);
            }
        }

        Project project = ti.project;
        if (project != null && project.id > 0) {
            add(layout, R.string.project, project.title);
        }

        if (!Utils.isEmpty(ti.note)) {
            add(layout, R.string.note, ti.note);
        }

        MyLocation location = ti.location;
        String locationName;
        if (location != null && location.id > 0) {
            locationName = location.title + (location.resolvedAddress != null ? " (" + location.resolvedAddress + ")" : "");
            add(layout, R.string.location, locationName);
        }
    }

    private View createTitleView(TransactionInfo ti, LinearLayout layout) {
        View titleView = layoutInflater.inflate(R.layout.info_dialog_title, null);
        TextView titleLabel = titleView.findViewById(R.id.label);
        TextView titleData = titleView.findViewById(R.id.data);
        ImageView titleIcon = titleView.findViewById(R.id.icon);
        if (ti.isTemplate()) {
            titleLabel.setText(ti.templateName);
        } else {
            if (ti.isScheduled() && ti.recurrence != null) {
                Recurrence r = Recurrence.parse(ti.recurrence);
                titleLabel.setText(r.toInfoString(context));
            } else {
                int titleId = ti.isSplitParent()
                        ? R.string.split
                        : (ti.toAccount == null ? R.string.transaction : R.string.transfer);
                titleLabel.setText(titleId);
                add(layout, R.string.date, DateUtils.formatDateTime(context, ti.dateTime,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR),
                        ti.attachedPicture);
            }
        }
        TransactionStatus status = ti.status;
        titleData.setText(context.getString(status.titleId));
        titleIcon.setImageResource(status.iconId);
        return titleView;
    }

    private void showDialog(final BlotterActivity blotterActivity, final long transactionId, final View v, View titleView) {
        final Dialog d = new AlertDialog.Builder(blotterActivity)
                .setCustomTitle(titleView)
                .setView(v)
                .create();
        d.setCanceledOnTouchOutside(true);

        Button bEdit = v.findViewById(R.id.bEdit);
        bEdit.setOnClickListener(arg0 -> {
            d.dismiss();
            new BlotterOperations(blotterActivity, db, transactionId).editTransaction();
        });

        Button bClose = v.findViewById(R.id.bClose);
        bClose.setOnClickListener(arg0 -> d.dismiss());

        d.show();
    }

    private void add(LinearLayout layout, int labelId, String data, AccountType accountType) {
        inflater.new Builder(layout, R.layout.select_entry_simple_icon)
                .withIcon(accountType.iconId).withLabel(labelId).withData(data).create();
    }

    private TextView add(LinearLayout layout, int labelId, String data) {
        View v = inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(labelId)
                .withData(data).create();
        return (TextView)v.findViewById(R.id.data);
    }

    private void add(LinearLayout layout, int labelId, String data, String pictureFileName) {
        View v = inflater.new PictureBuilder(layout)
                .withPicture(context, pictureFileName)
                .withLabel(labelId)
                .withData(data)
                .create();
        v.setClickable(false);
        v.setFocusable(false);
        v.setFocusableInTouchMode(false);
        ImageView pictureView = v.findViewById(R.id.picture);
        pictureView.setTag(pictureFileName);
    }

    private LinearLayout add(LinearLayout layout, String label, String data) {
        return (LinearLayout) inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(label)
                .withData(data).create();
    }

}
