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
package ru.orangesoftware.financisto.activity;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;

public abstract class FilterAbstractActivity extends AbstractActivity {

	protected void showMinusButton(TextView textView) {
		ImageView v = findMinusButton(textView);
		v.setVisibility(View.VISIBLE);
	}

	protected void hideMinusButton(TextView textView) {
		ImageView v = findMinusButton(textView);
		v.setVisibility(View.GONE);
	}

	protected ImageView findMinusButton(TextView textView) {
		return (ImageView) textView.getTag(R.id.bMinus);
	}

}
