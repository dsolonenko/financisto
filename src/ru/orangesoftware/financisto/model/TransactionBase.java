/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.model;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

public abstract class TransactionBase implements Serializable, Cloneable {

	@Id
	@Column(name = "_id")
	public long id = -1;

    @Column(name = "parent_id")
    public long parentId;

	@Column(name = "datetime")
	public long dateTime = System.currentTimeMillis();
	
	@Column(name = "provider")
	public String provider;

	@Column(name = "accuracy")
	public float accuracy;

	@Column(name = "longitude")
	public double longitude;

	@Column(name = "latitude")
	public double latitude;

	@Column(name = "note")
	public String note;

    @Column(name = "original_from_amount")
    public long originalFromAmount;

    @Column(name = "from_amount")
	public long fromAmount;
	
	@Column(name = "to_amount")
	public long toAmount;
	
	@Column(name = "is_template")
	public int isTemplate;
	
	@Column(name = "template_name")
	public String templateName;

	@Column(name = "recurrence")
	public String recurrence;	
	
	@Column(name = "notification_options")
	public String notificationOptions;		
	
	@Column(name = "status")
	public TransactionStatus status = TransactionStatus.UR;	
	
	@Column(name = "attached_picture")
	public String attachedPicture;
	
	@Column(name = "is_ccard_payment")
	public int isCCardPayment;

	@Column(name = "last_recurrence")
	public long lastRecurrence;
	
	@Column(name = "updated_on")
	public long updatedOn = System.currentTimeMillis();
	 
	@Column(name = "remote_key")
	public String remoteKey ;		

	public boolean isTemplate() {
		return isTemplate == 1;
	}

    public void setAsTemplate() {
        this.isTemplate = 1;
    }

    public boolean isScheduled() {
		return isTemplate == 2;
	}

    public void setAsScheduled() {
        this.isTemplate = 2;
    }

	public boolean isTemplateLike() {
		return isTemplate > 0;
	}

	public boolean isNotTemplateLike() {
		return isTemplate == 0;
	}

    public boolean isCreatedFromTemlate() {
        return !isTemplate() && templateName != null && templateName.length() > 0;
    }

	public boolean isCreditCardPayment() {
		return isCCardPayment == 1;
	}

    public boolean isSplitChild() {
        return parentId > 0;
    }

}
