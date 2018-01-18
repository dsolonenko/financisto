/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.TransactionAttribute;

public class AttributeBuilder {

    private final DatabaseAdapter db;

    private AttributeBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public static AttributeBuilder withDb(DatabaseAdapter db) {
        return new AttributeBuilder(db);
    }

    public Attribute createTextAttribute(String name) {
        return createAttribute(name, Attribute.TYPE_TEXT);
    }

    public Attribute createNumberAttribute(String name) {
        return createAttribute(name, Attribute.TYPE_NUMBER);
    }

    private Attribute createAttribute(String name, int type) {
        Attribute a = new Attribute();
        a.title = name;
        a.type = type;
        a.id = db.insertOrUpdate(a);
        return a;
    }

    public static TransactionAttribute attributeValue(Attribute a, String value) {
        TransactionAttribute ta = new TransactionAttribute();
        ta.attributeId = a.id;
        ta.value = value;
        return ta;
    }

}
