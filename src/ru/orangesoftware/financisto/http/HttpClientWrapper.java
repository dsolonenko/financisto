/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.http;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/17/13
 * Time: 1:55 AM
 */
public class HttpClientWrapper {

    private final OkHttpClient client;

    public HttpClientWrapper(OkHttpClient httpClient) {
        this.client = httpClient;
    }

    public JSONObject getAsJson(String url) throws Exception {
        String s = getAsString(url);
        return new JSONObject(s);
    }

    public String getAsString(String url) throws Exception {
        Response response = get(url);
        return response.body().string();
    }

    public String getAsStringIfOk(String url) throws Exception {
        Response response = get(url);
        String s = response.body().string();
        if (response.isSuccessful()) {
            return s;
        } else {
            throw new RuntimeException(s);
        }
    }

    protected Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return client.newCall(request).execute();
    }

}
