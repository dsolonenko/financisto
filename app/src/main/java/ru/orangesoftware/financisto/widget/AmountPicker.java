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

import java.math.BigDecimal;

import ru.orangesoftware.financisto.R;
import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AmountPicker extends LinearLayout implements NumberPicker.OnChangedListener {
	
    public interface OnChangedListener {
        void onChanged(AmountPicker picker, BigDecimal oldVal, BigDecimal newVal);
    }

    private final NumberPicker[] integers;
    private final NumberPicker[] fractions;
    
    private OnChangedListener mListener;
    private BigDecimal mCurrent;
    private BigDecimal mPrevious;
    
	public AmountPicker(Context context, int decimals) {
        super(context);
        
        integers = new NumberPicker[Math.max(0, 5-decimals)];
        fractions = new NumberPicker[decimals];
        
        int totalWidth = 6*getResources().getDimensionPixelSize(R.dimen.amount_picker_width);
        int total = integers.length + fractions.length;
        int pickerWidth = totalWidth/total;
        int padding = getResources().getDimensionPixelSize(R.dimen.picker_padding);

        setOrientation(HORIZONTAL);
        setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams lpFixedWrap = new LinearLayout.LayoutParams(pickerWidth, LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams lpWrapFill = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        addPickers(context, lpFixedWrap, integers);
        if (decimals > 0) {
        	TextView dotText = new TextView(context);
        	dotText.setText(".");
        	dotText.setGravity(Gravity.BOTTOM);
        	dotText.setTextSize(pickerWidth/2);
        	addView(dotText, lpWrapFill);
        	addPickers(context, lpFixedWrap, fractions);
        }
	}
	
	private void addPickers(Context context, LayoutParams layoutParams,
			NumberPicker[] pickers) {
        for (int i=0; i<pickers.length; i++) {
        	pickers[i] = new NumberPicker(context);
        	pickers[i].setRange(0, 9);
        	pickers[i].setOnChangeListener(this);
        	addView(pickers[i], layoutParams);
        }
	}

	public void setOnChangeListener(OnChangedListener listener) {
        mListener = listener;
    }

    private void notifyChange() {
        if (mListener != null) {
            mListener.onChanged(this, mPrevious, mCurrent);
        }
    }

	@Override
	public void onChanged(NumberPicker picker, int oldVal, int newVal) {
		StringBuilder sb = new StringBuilder();
		appendPickers(sb, integers);
		sb.append(".");
		appendPickers(sb, fractions);
		BigDecimal v = new BigDecimal(sb.toString()); 
		mPrevious = mCurrent;
		mCurrent = v.setScale(2);		
		notifyChange();
	}
	
	private void appendPickers(StringBuilder sb, NumberPicker[] pickers) {
		int len = pickers.length;
		for (NumberPicker picker : pickers) {
			sb.append(picker.getCurrent());
		}
	}

	public BigDecimal getCurrent() {
		return mCurrent != null ? mCurrent : BigDecimal.ZERO;
	}
	
	public void setCurrent(BigDecimal newVal) {
		int unscaled = newVal.abs().unscaledValue().intValue();
		setPickers(integers, unscaled/100);
		setPickers(fractions, unscaled-100*(unscaled/100));
		mPrevious = mCurrent;
		mCurrent = newVal;
		notifyChange();
	}
	
	private int setPickers(NumberPicker[] pickers, int val) {
		int v;		
		int len = pickers.length;
		for (int i=len-1; i>=0; i--) {
			v = val/10;
			pickers[i].setCurrent(val-v*10);
			val = v;
		}	
		return val;
	}
    
}
