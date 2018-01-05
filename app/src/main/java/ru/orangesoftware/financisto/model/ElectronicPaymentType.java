/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - parameters for bill filtering
 ******************************************************************************/
package ru.orangesoftware.financisto.model;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.EntityEnum;

public enum ElectronicPaymentType implements EntityEnum {

	PAYPAL(R.string.electronic_type_paypal, R.drawable.electronic_type_paypal),
	AMAZON(R.string.electronic_type_amazon, R.drawable.electronic_type_amazon),
	EBAY(R.string.electronic_type_ebay, R.drawable.electronic_type_ebay),
	GOOGLE_WALLET(R.string.electronic_type_google_wallet, R.drawable.electronic_type_google_wallet),
    BITCOIN(R.string.electronic_type_bitcoin, R.drawable.electronic_type_bitcoin),
	ALIPAY(R.string.electronic_type_alipay, R.drawable.electronic_type_alipay),
	WEB_MONEY(R.string.electronic_type_web_money, R.drawable.electronic_type_webmoney),
    YANDEX_MONEY(R.string.electronic_type_yandex_money, R.drawable.electronic_type_yandex_money);

	public final int titleId;
	public final int iconId;

	ElectronicPaymentType(int titleId, int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@Override
	public int getTitleId() {
		return titleId;
	}
	
	@Override
	public int getIconId() {
		return iconId;
	}

}
