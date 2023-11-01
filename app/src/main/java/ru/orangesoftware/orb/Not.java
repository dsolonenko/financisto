/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.orb;


public class Not implements Expression {
	
	private final Expression e;
	
	public Not(Expression e) {
		this.e = e;
	}

	@Override
	public Selection toSelection(EntityDefinition ed) {
		StringBuilder sb = new StringBuilder();
		sb.append("NOT (");
		Selection s = e.toSelection(ed); 
		sb.append(s.selection);
		sb.append(")");
		return new Selection(sb.toString(), s.selectionArgs);
	}

}
