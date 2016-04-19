/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.http;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/17/13
 * Time: 1:58 AM
 */
public class FakeHttpClientWrapper extends HttpClientWrapper {

    public final Map<String, String> responses = new HashMap<String, String>();
    public Exception error;

    public FakeHttpClientWrapper() {
        super(null);
    }

    @Override
    public String getAsString(String url) throws Exception {
        if (error != null) {
            throw error;
        }
        String response = responses.get(url);
        if (response == null) {
            response = responses.get("*");
        }
        return response;
    }

    public void givenResponse(String url, String response) {
        responses.put(url, response);
    }
}
