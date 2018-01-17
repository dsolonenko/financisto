/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - identifying credit card payments
 ******************************************************************************/
package ru.orangesoftware.financisto.model;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;

import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;

import javax.persistence.*;

import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "transactions")
public class Transaction extends TransactionBase {

    private static final String SPLIT_BLOB = "SPLIT_BLOB";

    @Column(name = "category_id")
    public long categoryId;

    @Column(name = "project_id")
    public long projectId;

    @Column(name = "location_id")
    public long locationId;

    @Column(name = "from_account_id")
    public long fromAccountId;

    @Column(name = "to_account_id")
    public long toAccountId;

    @Column(name = "payee_id")
    public long payeeId;

    @Column(name = "blob_key")
    public String blobKey;

    @Column(name = "original_currency_id")
    public long originalCurrencyId;

    @Transient
    public EnumMap<SystemAttribute, String> systemAttributes;

    @Transient
    public Map<Long, String> categoryAttributes;

    @Transient
    public List<Transaction> splits;

    @Transient
    public long unsplitAmount;

    public ContentValues toValues() {
        ContentValues values = new ContentValues();
        values.put(TransactionColumns.parent_id.name(), parentId);
        values.put(TransactionColumns.category_id.name(), categoryId);
        values.put(TransactionColumns.project_id.name(), projectId);
        values.put(TransactionColumns.datetime.name(), dateTime);
        values.put(TransactionColumns.location_id.name(), locationId);
        values.put(TransactionColumns.provider.name(), provider);
        values.put(TransactionColumns.accuracy.name(), accuracy);
        values.put(TransactionColumns.latitude.name(), latitude);
        values.put(TransactionColumns.longitude.name(), longitude);
        values.put(TransactionColumns.from_account_id.name(), fromAccountId);
        values.put(TransactionColumns.to_account_id.name(), toAccountId);
        values.put(TransactionColumns.payee_id.name(), payeeId);
        values.put(TransactionColumns.note.name(), note);
        values.put(TransactionColumns.from_amount.name(), fromAmount);
        values.put(TransactionColumns.to_amount.name(), toAmount);
        values.put(TransactionColumns.original_currency_id.name(), originalCurrencyId);
        values.put(TransactionColumns.original_from_amount.name(), originalFromAmount);
        values.put(TransactionColumns.is_template.name(), isTemplate);
        values.put(TransactionColumns.template_name.name(), templateName);
        values.put(TransactionColumns.recurrence.name(), recurrence);
        values.put(TransactionColumns.notification_options.name(), notificationOptions);
        values.put(TransactionColumns.status.name(), status.name());
        values.put(TransactionColumns.attached_picture.name(), attachedPicture);
        values.put(TransactionColumns.is_ccard_payment.name(), isCCardPayment);
        values.put(TransactionColumns.last_recurrence.name(), lastRecurrence);
        values.put(TransactionColumns.blob_key.name(), blobKey);
        return values;
    }

    public void toIntentAsSplit(Intent intent) {
        intent.putExtra(SPLIT_BLOB, this);
    }

    public static Transaction fromIntentAsSplit(Intent intent) {
        return (Transaction) intent.getSerializableExtra(SPLIT_BLOB);
    }

    public static Transaction fromBlotterCursor(Cursor c) {
        long id = c.getLong(BlotterColumns._id.ordinal());
        Transaction t = new Transaction();
        t.id = id;
        t.parentId = c.getLong(BlotterColumns.parent_id.ordinal());
        t.fromAccountId = c.getLong(BlotterColumns.from_account_id.ordinal());
        t.toAccountId = c.getLong(BlotterColumns.to_account_id.ordinal());
        t.categoryId = c.getLong(BlotterColumns.category_id.ordinal());
        t.projectId = c.getLong(BlotterColumns.project_id.ordinal());
        t.payeeId = c.getLong(BlotterColumns.payee_id.ordinal());
        t.note = c.getString(BlotterColumns.note.ordinal());
        t.fromAmount = c.getLong(BlotterColumns.from_amount.ordinal());
        t.toAmount = c.getLong(BlotterColumns.to_amount.ordinal());
        t.dateTime = c.getLong(BlotterColumns.datetime.ordinal());
        t.originalCurrencyId = c.getLong(BlotterColumns.original_currency_id.ordinal());
        t.originalFromAmount = c.getLong(BlotterColumns.original_from_amount.ordinal());
        t.locationId = c.getLong(BlotterColumns.location_id.ordinal());
//		t.provider = c.getString(BlotterColumns.provider.ordinal());
//		t.accuracy = c.getFloat(BlotterColumns.accuracy.ordinal());
//		t.latitude = c.getDouble(BlotterColumns.latitude.ordinal());
//		t.longitude = c.getDouble(BlotterColumns.longitude.ordinal());
        t.isTemplate = c.getInt(BlotterColumns.is_template.ordinal());
        t.templateName = c.getString(BlotterColumns.template_name.ordinal());
        t.recurrence = c.getString(BlotterColumns.recurrence.ordinal());
        t.notificationOptions = c.getString(BlotterColumns.notification_options.ordinal());
        t.status = TransactionStatus.valueOf(c.getString(BlotterColumns.status.ordinal()));
        t.attachedPicture = c.getString(BlotterColumns.attached_picture.ordinal());
        t.isCCardPayment = c.getInt(BlotterColumns.is_ccard_payment.ordinal());
        t.lastRecurrence = c.getLong(BlotterColumns.last_recurrence.ordinal());
        return t;
    }

    public boolean isTransfer() {
        return toAccountId > 0;
    }

    public boolean isSplitParent() {
        return categoryId == Category.SPLIT_CATEGORY_ID;
    }

    public String getSystemAttribute(SystemAttribute sa) {
        return systemAttributes != null ? systemAttributes.get(sa) : null;
    }

    @Override
    public Transaction clone() {
        try {
            return (Transaction) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date(dateTime)).append(":");
        sb.append("FA(").append(fromAccountId).append(")->").append(fromAmount).append(",");
        sb.append("TA(").append(toAccountId).append(")->").append(toAmount);
        return sb.toString();
    }

}
