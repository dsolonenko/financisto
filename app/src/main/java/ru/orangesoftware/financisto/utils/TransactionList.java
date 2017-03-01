/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.TransactionInfo;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 10/23/12 2:27 AM
 */
public class TransactionList {

    public final List<TransactionInfo> transactions;
    public final Total[] totals;

    public TransactionList(List<TransactionInfo> transactions, Total[] totals) {
        this.transactions = transactions;
        this.totals = totals;
    }

}
