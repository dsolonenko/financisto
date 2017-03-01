/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.datetime;

/**
* Created by IntelliJ IDEA.
* User: denis.solonenko
* Date: 12/17/12 9:07 PM
*/
public class Period {

    public PeriodType type;
    public long start;
    public long end;

    public Period(PeriodType type, long start, long end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }

    public boolean isSame(long start, long end) {
        return this.start == start && this.end == end;
    }

    public boolean isCustom() {
        return type == PeriodType.CUSTOM;
    }

}
