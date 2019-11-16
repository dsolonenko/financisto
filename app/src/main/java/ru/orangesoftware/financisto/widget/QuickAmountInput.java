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
package ru.orangesoftware.financisto.widget;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;

import java.math.BigDecimal;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;

@EFragment
public class QuickAmountInput extends DialogFragment {

    @FragmentArg
    protected long currencyId;
    @FragmentArg
    protected long amount;

    private AmountPicker picker;
    private AmountListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Activity activity = getActivity();
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(ContextCompat.getColor(activity, R.color.calculator_background));

        LinearLayout.LayoutParams lpWrapWrap = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpWrapWrap.weight = 1;

        // picker
        Currency currency = CurrencyCache.getCurrencyOrEmpty(currencyId);
        picker = new AmountPicker(activity, currency.decimals);
        layout.addView(picker, lpWrapWrap);
        picker.setCurrent(new BigDecimal(amount));
        picker.setOnChangeListener((picker, oldVal, newVal) -> setTitle());

        // buttons
        LinearLayout buttonsLayout = new LinearLayout(new ContextThemeWrapper(activity, R.style.ButtonBar), null, R.style.ButtonBar);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button bOK = new Button(activity);
        bOK.setText(R.string.ok);
        bOK.setOnClickListener(arg0 -> {
            listener.onAmountChanged(picker.getCurrent().toPlainString());
            dismiss();
        });
        buttonsLayout.addView(bOK, lpWrapWrap);

        Button bClear = new Button(activity);
        bClear.setText(R.string.reset);
        bClear.setOnClickListener(arg0 -> picker.setCurrent(BigDecimal.ZERO));
        buttonsLayout.addView(bClear, lpWrapWrap);

        Button bCancel = new Button(activity);
        bCancel.setText(R.string.cancel);
        bCancel.setOnClickListener(arg0 -> dismiss());

        buttonsLayout.addView(bCancel, lpWrapWrap);
        layout.addView(buttonsLayout, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return layout;
    }

    private void setTitle() {
        //setTitle(Utils.amountToString(currency, picker.getCurrent().multiply(Utils.HUNDRED)));
    }

    public void setListener(AmountListener listener) {
        this.listener = listener;
    }

}
