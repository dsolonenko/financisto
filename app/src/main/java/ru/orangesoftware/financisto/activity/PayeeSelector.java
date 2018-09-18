/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.TransactionUtils;

public class PayeeSelector<A extends AbstractActivity> extends MyEntitySelector<Payee, A> {

    public PayeeSelector(A activity, MyEntityManager em, ActivityLayout x) {
        this(activity, em, x, R.id.payee_add, R.id.payee_clear, R.string.no_payee);
    }

    public PayeeSelector(A activity, MyEntityManager em, ActivityLayout x, int actBtnId, int clearBtnId, int emptyId) {
        super(activity, em, x, MyPreferences.isShowPayee(activity),
                R.id.payee, actBtnId, clearBtnId, R.string.payee, emptyId, R.id.payee_filter_toggle);
    }

    @Override
    protected Class getEditActivityClass() {
        return PayeeActivity.class;
    }

    @Override
    protected List<Payee> fetchEntities(MyEntityManager em) {
        return em.getAllPayeeList();
    }

    @Override
    protected ListAdapter createAdapter(Activity activity, List<Payee> entities) {
        return TransactionUtils.createPayeeAdapter(activity, entities);
    }

    @Override
    protected SimpleCursorAdapter createFilterAdapter() {
        return TransactionUtils.createPayeeAutoCompleteAdapter(activity, em);
    }

}
