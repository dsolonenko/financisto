/*
 * Copyright (c) 2014 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 1/27/14
 * Time: 10:44 PM
 */
public class ImportExportException extends Exception {

    public final int errorResId;
    public final Exception cause;

    public ImportExportException(int errorResId) {
        this.errorResId = errorResId;
        this.cause = null;
    }

    public ImportExportException(int errorResId, Exception cause) {
        this.errorResId = errorResId;
        this.cause = cause;
    }

}
