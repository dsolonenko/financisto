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
package ru.orangesoftware.financisto.model;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.TransactionActivity;
import ru.orangesoftware.financisto.activity.TransferActivity;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

@Entity
@Table(name = "transactions")
public class TransactionInfo extends TransactionBase {
	
	@JoinColumn(name = "from_account_id")
	public Account fromAccount;

	@JoinColumn(name = "to_account_id", required = false)
	public Account toAccount;

	@JoinColumn(name = "category_id")
	public Category category;

	@JoinColumn(name = "project_id", required = false)
	public Project project;

	@JoinColumn(name = "location_id", required = false)
	public MyLocation location;

    @JoinColumn(name = "original_currency_id", required = false)
    public Currency originalCurrency;

    @JoinColumn(name = "payee_id", required = false)
    public Payee payee;

    @Transient
	public Date nextDateTime;
	
	public boolean isTransfer() {
		return toAccount != null;
	}

	public Class<? extends Activity> getActivity() {
		return isTransfer() ? TransferActivity.class : TransactionActivity.class;
	}
	
	public int getNotificationIcon() {
		return isTransfer() ?
            R.drawable.notification_icon_transfer :
            fromAmount > 0 ?
                R.drawable.notification_icon_transaction : R.drawable.ic_btn_round_minus;
	}
	
	public String getNotificationTickerText(Context context) {
		return context.getString(isTransfer() ? R.string.new_scheduled_transfer_text : R.string.new_scheduled_transaction_text);
	}

	public String getNotificationContentTitle(Context context) {
		return context.getString(isTransfer() ? R.string.new_scheduled_transfer_title : R.string.new_scheduled_transaction_title);
	}
	
	public String getNotificationContentText(Context context) {
		if (toAccount != null) {
			if (fromAccount.currency.id == toAccount.currency.id) {
				return context.getString(R.string.new_scheduled_transfer_notification_same_currency, 
						Utils.amountToString(fromAccount.currency, Math.abs(fromAmount)),
						fromAccount.title, toAccount.title);				
			} else {
				return context.getString(R.string.new_scheduled_transfer_notification_differ_currency, 
						Utils.amountToString(fromAccount.currency, Math.abs(fromAmount)),
						Utils.amountToString(toAccount.currency, Math.abs(toAmount)),
						fromAccount.title, toAccount.title);								
			}
		} else {
			return context.getString(R.string.new_scheduled_transaction_notification,
					Utils.amountToString(fromAccount.currency, Math.abs(fromAmount)),
					context.getString(fromAmount > 0 ? R.string.new_scheduled_transaction_debit : R.string.new_scheduled_transaction_credit),
					fromAccount.title);
		}		
	}

    @Override
    public TransactionInfo clone() {
        try {
            return (TransactionInfo)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSplitParent() {
        return category.isSplit();
    }

    public static TransactionInfo fromBlotterCursor(Cursor c) {
        long id = c.getLong(DatabaseHelper.BlotterColumns._id.ordinal());
        TransactionInfo t = new TransactionInfo();
        t.id = id;
        t.parentId = c.getLong(DatabaseHelper.BlotterColumns.parent_id.ordinal());

        t.dateTime = c.getLong(DatabaseHelper.BlotterColumns.datetime.ordinal());
        t.note = c.getString(DatabaseHelper.BlotterColumns.note.ordinal());
        t.fromAmount = c.getLong(DatabaseHelper.BlotterColumns.from_amount.ordinal());
        t.toAmount = c.getLong(DatabaseHelper.BlotterColumns.to_amount.ordinal());

        t.originalCurrency = CurrencyCache.getCurrencyOrEmpty(c.getLong(DatabaseHelper.BlotterColumns.original_currency_id.ordinal()));
        t.originalFromAmount = c.getLong(DatabaseHelper.BlotterColumns.original_from_amount.ordinal());

        Account fromAccount = new Account();
        fromAccount.id = c.getLong(DatabaseHelper.BlotterColumns.from_account_id.ordinal());
        fromAccount.title = c.getString(DatabaseHelper.BlotterColumns.from_account_title.ordinal());
        fromAccount.currency = CurrencyCache.getCurrencyOrEmpty(c.getLong(DatabaseHelper.BlotterColumns.from_account_currency_id.ordinal()));
        t.fromAccount = fromAccount;

        long toAccountId = c.getLong(DatabaseHelper.BlotterColumns.to_account_id.ordinal());
        if (toAccountId > 0) {
            Account toAccount = new Account();
            toAccount.id = toAccountId;
            toAccount.title = c.getString(DatabaseHelper.BlotterColumns.to_account_title.ordinal());
            toAccount.currency = CurrencyCache.getCurrencyOrEmpty(c.getLong(DatabaseHelper.BlotterColumns.to_account_currency_id.ordinal()));
            t.toAccount = toAccount;
        }

        Category category = new Category();
        category.id = c.getLong(DatabaseHelper.BlotterColumns.category_id.ordinal());
        category.title = c.getString(DatabaseHelper.BlotterColumns.category_title.ordinal());
        t.category = category;

        Project project = new Project();
        project.id = c.getLong(DatabaseHelper.BlotterColumns.project_id.ordinal());
        project.title = c.getString(DatabaseHelper.BlotterColumns.project.ordinal());
        t.project = project;

        Payee payee = new Payee();
        payee.id = c.getLong(DatabaseHelper.BlotterColumns.payee_id.ordinal());
        payee.title = c.getString(DatabaseHelper.BlotterColumns.payee.ordinal());
        t.payee = payee;

        MyLocation location = new MyLocation();
        location.id = c.getLong(DatabaseHelper.BlotterColumns.location_id.ordinal());
        location.title = c.getString(DatabaseHelper.BlotterColumns.location.ordinal());
        t.location = location;

        t.isTemplate = c.getInt(DatabaseHelper.BlotterColumns.is_template.ordinal());
        t.templateName = c.getString(DatabaseHelper.BlotterColumns.template_name.ordinal());
        t.recurrence = c.getString(DatabaseHelper.BlotterColumns.recurrence.ordinal());
        t.notificationOptions = c.getString(DatabaseHelper.BlotterColumns.notification_options.ordinal());
        t.status = TransactionStatus.valueOf(c.getString(DatabaseHelper.BlotterColumns.status.ordinal()));
        t.attachedPicture = c.getString(DatabaseHelper.BlotterColumns.attached_picture.ordinal());
        t.isCCardPayment = c.getInt(DatabaseHelper.BlotterColumns.is_ccard_payment.ordinal());
        t.lastRecurrence = c.getLong(DatabaseHelper.BlotterColumns.last_recurrence.ordinal());

        return t;
    }

}
