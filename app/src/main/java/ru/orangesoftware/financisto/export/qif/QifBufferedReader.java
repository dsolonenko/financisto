/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.qif;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 9/26/11 7:50 PM
 */
public class QifBufferedReader {

    private final BufferedReader r;

    public QifBufferedReader(BufferedReader r) {
        this.r = r;
    }

    public String readLine() throws IOException {
        while (true) {
            String line = r.readLine();
            if (line == null) {
                return null;
            }
            line = line.trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
    }

    public String peekLine() throws IOException {
        r.mark(256);
        String peek = readLine();
        r.reset();
        return peek;
    }

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }

}
