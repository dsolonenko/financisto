/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.filter;

import android.content.Intent;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.utils.ArrUtils;
import ru.orangesoftware.financisto.utils.StringUtil;
import ru.orangesoftware.orb.Expression;
import ru.orangesoftware.orb.Expressions;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 12/17/12 9:06 PM
 */
public class Criteria {

    public static Criteria eq(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.EQ, value);
    }

    public static Criteria neq(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.NEQ, value);
    }

    public static Criteria btw(String column, String... values) {
        if (values.length < 2) throw new IllegalArgumentException("No values for BTW filter!");
        return new Criteria(column, WhereFilter.Operation.BTW, values);
    }

    public static Criteria in(String column, String... values) {
        if (values.length == 0) throw new IllegalArgumentException("No values for IN filter!");
        return new Criteria(column, WhereFilter.Operation.IN, values);
    }

    public static Criteria gt(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.GT, value);
    }

    public static Criteria gte(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.GTE, value);
    }

    public static Criteria lt(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.LT, value);
    }

    public static Criteria lte(String column, String value) {
        return new Criteria(column, WhereFilter.Operation.LTE, value);
    }

    public static Criteria isNull(String column) {
        return new Criteria(column, WhereFilter.Operation.ISNULL);
    }

    public static Criteria raw(String text) {
        return new Criteria("(" + text + ")", WhereFilter.Operation.NOPE);
    }

    public static Criteria or(Criteria a, Criteria b) {
        return new OrCriteria(a, b);
    }
    
    public final String columnName;
    public final WhereFilter.Operation operation;
    private final String[] values;

    public Criteria(String columnName, WhereFilter.Operation operation, String... values) {
        this.columnName = columnName;
        this.operation = operation;
        this.values = values;
    }

    public boolean isNull() {
        return operation == WhereFilter.Operation.ISNULL;
    }
    
    @Deprecated // todo.mb: not used, can be removed
    public Expression toWhereExpression() {
        switch (operation) {
            case EQ:
                return Expressions.eq(columnName, getLongValue1());
            case GT:
                return Expressions.gt(columnName, getLongValue1());
            case GTE:
                return Expressions.gte(columnName, getLongValue1());
            case LT:
                return Expressions.lt(columnName, getLongValue1());
            case LTE:
                return Expressions.lte(columnName, getLongValue1());
            case BTW:
                return Expressions.btw(columnName, getLongValue1(), getLongValue2());
            case LIKE:
                return Expressions.like(columnName, getStringValue());
        }
        throw new IllegalArgumentException();
    }

    public String toStringExtra() {
        StringBuilder sb = new StringBuilder();
        sb.append(columnName).append(",");
        sb.append(operation.name()).append(",");
        String[] values = this.values;
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    public static Criteria fromStringExtra(String extra) {
        final String[] a = extra.split(",");
        final String col = a[0];
        if (BlotterFilter.DATETIME.equals(col)) {
            return DateTimeCriteria.fromStringExtra(extra);
        } else if (BlotterFilter.CATEGORY_ID.equals(col)) {
            return SingleCategoryCriteria.fromStringExtra(extra);
        } else {
            String[] values = new String[a.length - 2];
            System.arraycopy(a, 2, values, 0, values.length);
            return new Criteria(col, WhereFilter.Operation.valueOf(a[1]), values);
        }
    }

    public String[] getValues() {
        return values;
    }

    public String getStringValue() {
        return values.length > 0 ? values[0] : "";
    }

    public int getIntValue() {
        return values.length > 0 ? Integer.parseInt(values[0]) : -1;
    }

    public long getLongValue1() {
        return values.length > 0 ? Long.parseLong(values[0]) : -1;
    }

    public long getLongValue2() {
        return values.length > 1 ? Long.parseLong(values[1]) : -1;
    }

    public String getSelection() {
        String exp = columnName + " " + operation.getOp(getSelectionArgs().length);
        if (operation.getGroupOp() != null && getValues().length > operation.getValsPerGroup()) {
            int groupNum = getValues().length / operation.getValsPerGroup();
            String groupDelim = " " + operation.getGroupOp() + " ";
            return  "(" + StringUtil.generateSeparated(exp, groupDelim, groupNum) + ")";
        }
        return exp;
    }

    public int size() {
        return values != null ? values.length : 0;
    }

    public String[] getSelectionArgs() {
        return values;
    }

    public void toIntent(String title, Intent intent) {
        intent.putExtra(WhereFilter.TITLE_EXTRA, title);
        intent.putExtra(WhereFilter.FILTER_EXTRA, new String[]{toStringExtra()});
    }
    
    static class OrCriteria extends Criteria {
        Criteria a, b;
        
        public OrCriteria(Criteria a, Criteria b) {
            super(a.columnName, a.operation, ArrUtils.joinArrays(a.getValues(), b.getValues()));
            this.a = a;
            this.b = b;
        }

        @Override
        public String getSelection() {
            return "(" + a.getSelection() + " OR " + b.getSelection() + ")";
        }

        @Override
        public String toStringExtra() {
            throw new UnsupportedOperationException();
        }
    }

}
