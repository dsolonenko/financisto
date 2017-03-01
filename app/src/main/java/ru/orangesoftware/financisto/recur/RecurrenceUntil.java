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

public enum RecurrenceUntil implements LocalizableEnum {

	INDEFINETELY(R.string.recur_indefinitely),
	EXACTLY_TIMES(R.string.recur_exactly_n_times),
	STOPS_ON_DATE(R.string.recur_stops_on_date);

	public final int titleId;
	
	private RecurrenceUntil(int titleId) {
		this.titleId = titleId;
	}

	@Override
	public int getTitleId() {
		return titleId;
	}

}
