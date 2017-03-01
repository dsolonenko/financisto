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
package ru.orangesoftware.financisto.recur;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.LocalizableEnum;

public enum RecurrenceFrequency implements LocalizableEnum {
	
	NO_RECUR(R.string.recur_interval_no_recur),
	DAILY(R.string.recur_interval_daily),
	WEEKLY(R.string.recur_interval_weekly),		
	MONTHLY(R.string.recur_interval_monthly),
	//SEMI_MONTHLY(R.string.recur_interval_semi_monthly),
	GEEKY(R.string.recur_interval_geeky);
	//YEARLY(R.string.recur_interval_yearly);		
	
	public final int titleId;	
	
	RecurrenceFrequency(int titleId) {
		this.titleId = titleId;
	}

	@Override
	public int getTitleId() {
		return titleId;
	}

}
