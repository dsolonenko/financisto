/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.filter;

import ru.orangesoftware.financisto.blotter.BlotterFilter;

/**
* Created by IntelliJ IDEA.
* User: denis.solonenko
* Date: 12/17/12 9:06 PM
*/
public class SingleCategoryCriteria extends Criteria {

    private final long categoryId;

    public SingleCategoryCriteria(long categoryId) {
        super(BlotterFilter.CATEGORY_ID, WhereFilter.Operation.EQ, String.valueOf(categoryId));
        this.categoryId = categoryId;
    }

    public String toStringExtra() {
        StringBuilder sb = new StringBuilder();
        sb.append(BlotterFilter.CATEGORY_ID).append(",EQ,")
                .append(categoryId);
        return sb.toString();
    }

    public static Criteria fromStringExtra(String extra) {
        String[] a = extra.split(",");
        return new SingleCategoryCriteria(Long.parseLong(a[2]));
    }

    public long getCategoryId() {
        return categoryId;
    }

}
