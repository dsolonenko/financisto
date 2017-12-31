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

public enum AccountType implements EntityEnum {

    CASH(R.string.account_type_cash, R.drawable.account_type_cash, false, false, false, false, false),
    BANK(R.string.account_type_bank, R.drawable.account_type_bank, true, false, false, false, false),
    DEBIT_CARD(R.string.account_type_debit_card, R.drawable.account_type_card, true, true, true, false, false),
    CREDIT_CARD(R.string.account_type_credit_card, R.drawable.account_type_card, true, true, true, true, false),
    ELECTRONIC(R.string.account_type_electronic, R.drawable.account_type_electronic, false, false, false, false, true),
    ASSET(R.string.account_type_asset, R.drawable.account_type_asset, false, false, false, false, false),
    LIABILITY(R.string.account_type_liability, R.drawable.account_type_liability, false, false, false, false, false),
    OTHER(R.string.account_type_other, R.drawable.account_type_other, false, false, false, false, false);

    public final int titleId;
    public final int iconId;
    public final boolean hasIssuer;
    public final boolean hasNumber;
    public final boolean isCard;
    public final boolean isCreditCard;
    public final boolean isElectronic;

    AccountType(int titleId, int iconId,
                boolean hasIssuer, boolean hasNumber, boolean isCard, boolean isCreditCard, boolean isElectronic) {
        this.titleId = titleId;
        this.iconId = iconId;
        this.hasIssuer = hasIssuer;
        this.hasNumber = hasNumber;
        this.isCard = isCard;
        this.isCreditCard = isCreditCard;
        this.isElectronic = isElectronic;
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
