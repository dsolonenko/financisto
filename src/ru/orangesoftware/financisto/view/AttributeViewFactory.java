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
package ru.orangesoftware.financisto.view;

import ru.orangesoftware.financisto.model.Attribute;
import android.content.Context;

public class AttributeViewFactory {
	private AttributeViewFactory(){}
	
	public static AttributeView createViewForAttribute(Context context, Attribute attribute) {
		switch (attribute.type) {
		case Attribute.TYPE_TEXT:
			return new TextAttributeView(context, attribute);
		case Attribute.TYPE_NUMBER:
			return new NumberAttributeView(context, attribute);
		case Attribute.TYPE_LIST:
			return new ListAttributeView(context, attribute);
		case Attribute.TYPE_CHECKBOX:
			return new CheckBoxAttributeView(context, attribute);
		}
		return null;
	}
}
