/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.http;

import org.apache.http.HttpResponse;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/19/13
 * Time: 12:55 AM
 */
public class HttpException extends RuntimeException {

    public final HttpResponse response;

    public HttpException(HttpResponse response) {
        this.response = response;
    }

}
