/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.model;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/27/11 7:24 PM
 */
public enum SymbolFormat {

    RS {
        @Override
        public void appendSymbol(StringBuilder sb, String symbol) {
            sb.append(" ").append(symbol);
        }
    },
    R {
        @Override
        public void appendSymbol(StringBuilder sb, String symbol) {
            sb.append(symbol);
        }
    },
    LS {
        @Override
        public void appendSymbol(StringBuilder sb, String symbol) {
            int offset = getInsertIndex(sb);
            sb.insert(offset, " ").insert(offset, symbol);
        }
    },
    L {
        @Override
        public void appendSymbol(StringBuilder sb, String symbol) {
            sb.insert(getInsertIndex(sb), symbol);
        }
    };

    private static int getInsertIndex(StringBuilder sb) {
        if (sb.length() > 0) {
            char c = sb.charAt(0);
            return c == '+' || c == '-' ? 1 : 0;
        }
        return 0;
    }

    public abstract void appendSymbol(StringBuilder sb, String symbol);

}
