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

import ru.orangesoftware.financisto.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MultiNumberPicker extends LinearLayout implements NumberPicker.OnChangedListener {
        
    public interface OnChangedListener {
        void onChanged(MultiNumberPicker picker, int oldVal, int newVal);
    }

    private final NumberPicker[] pickers = new NumberPicker[5];
    private final int maxMultiplier = (int)Math.pow(10, pickers.length-1);  
    
    private OnChangedListener mListener;
    private int mCurrent;
    private int mPrevious;
    
    public MultiNumberPicker(Context context) {
        this(context, null);
    }
    
    public MultiNumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

	public MultiNumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        setOrientation(HORIZONTAL);        
        int pickerWidth = getResources().getDimensionPixelSize(R.dimen.picker_width);
        LinearLayout.LayoutParams lpWrapWrap = new LinearLayout.LayoutParams(pickerWidth, LayoutParams.WRAP_CONTENT);
        lpWrapWrap.weight = 1;
        for (int i=0; i<pickers.length; i++) {
        	pickers[i] = new NumberPicker(context, attrs, defStyle);
       		pickers[i].setRange(0, 9);
        	pickers[i].setOnChangeListener(this);
        	addView(pickers[i], lpWrapWrap);
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
		int m = maxMultiplier;
		int len = pickers.length;
		int v = 0;
		for (int i=0; i<len; i++) {
			v += m*pickers[i].getCurrent();
			m /= 10;
		}
		mPrevious = mCurrent;
		mCurrent = v;		
		notifyChange();
	}
	
	public int getCurrent() {
		return mCurrent;
	}

	public void setCurrent(int newVal) {		
		int v, val = newVal;
		int len = pickers.length;
		for (int i=len-1; i>=0; i--) {
			v = val/10;
			pickers[i].setCurrent(val-v*10);
			val = v;
		}	
		mPrevious = mCurrent;
		mCurrent = newVal;
		notifyChange();
	}
    
}
