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
package ru.orangesoftware.orb;

public class JoinEntity {

	public final FieldInfo field;
	public final EntityDefinition entity;
	public final int index;
	public final int parentIndex;
	public final boolean required;
	
	public JoinEntity(FieldInfo field, EntityDefinition entity, 
			int index, int parentIndex, boolean required) {
		this.field = field;
		this.entity = entity;
		this.index = index;
		this.parentIndex = parentIndex;
		this.required = required;
	}
		
}
