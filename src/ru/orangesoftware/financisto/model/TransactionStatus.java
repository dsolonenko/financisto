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
package ru.orangesoftware.financisto.model;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.LocalizableEnum;

public enum TransactionStatus implements LocalizableEnum {
	RS(R.string.transaction_status_restored, R.drawable.transaction_status_restored_2, R.color.restored_transaction_color),
	PN(R.string.transaction_status_pending, R.drawable.transaction_status_pending_2, R.color.pending_transaction_color),
	UR(R.string.transaction_status_unreconciled, R.drawable.transaction_status_unreconciled_2, R.color.unreconciled_transaction_color),
	CL(R.string.transaction_status_cleared, R.drawable.transaction_status_cleared_2, R.color.cleared_transaction_color),
	RC(R.string.transaction_status_reconciled, R.drawable.transaction_status_reconciled_2, R.color.reconciled_transaction_color);
	
	public final int titleId;
	public final int iconId;
	public final int colorId;
	
	private TransactionStatus(int titleId, int iconId, int colorId) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.colorId = colorId;
	}

	@Override
	public int getTitleId() {
		return titleId;
	}

}
