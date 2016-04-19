/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 5/31/12 7:27 PM
 */
public interface ProgressListener {

    void onProgress(int percentage);

}
