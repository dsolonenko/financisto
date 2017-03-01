/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/4/11 10:23 PM
 */
public abstract class AbstractExportTest<T extends Export, O> extends AbstractImportExportTest {

    protected String exportAsString(O options) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        T export = createExport(options);
        export.export(bos);
        String s = new String(bos.toByteArray(), "UTF-8");
        Log.d("Export", s);
        return s;
    }

    protected abstract T createExport(O options);

}
