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

import android.database.Cursor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import static ru.orangesoftware.financisto.db.DatabaseHelper.SMS_TEMPLATES_TABLE;
import ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns;

@Entity
@Table(name = SMS_TEMPLATES_TABLE)
public class SmsTemplate extends MyEntity {

    @Column(name = "template")
    public String template;

    @Column(name = "category_id")
    public long categoryId;

    @Column(name = "account_id")
    public long accountId;

    public static SmsTemplate fromCursor(Cursor c) {
        SmsTemplate t = new SmsTemplate();
        t.id = c.getLong(SmsTemplateColumns.Indicies.ID);
        t.title = c.getString(SmsTemplateColumns.Indicies.NUMBER);
        t.template = c.getString(SmsTemplateColumns.Indicies.TEMPLATE);
        t.categoryId = c.getLong(SmsTemplateColumns.Indicies.CATEGORY_ID);
        t.accountId = c.getLong(SmsTemplateColumns.Indicies.ACCOUNT_ID);
        return t;
    }
}
