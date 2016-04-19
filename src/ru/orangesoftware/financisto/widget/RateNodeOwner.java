/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.widget;

import android.app.Activity;
import ru.orangesoftware.financisto.model.Currency;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/19/12 11:57 PM
 */
public interface RateNodeOwner {

    void onBeforeRateDownload();
    void onAfterRateDownload();
    void onSuccessfulRateDownload();
    void onRateChanged();

    Activity getActivity();

    Currency getCurrencyFrom();
    Currency getCurrencyTo();

}
