/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/17/13
 * Time: 1:55 AM
 */
public class HttpClientWrapper {

    private final HttpClient httpClient;

    public HttpClientWrapper(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public JSONObject getAsJson(String url) throws Exception {
        String s = getAsString(url);
        return new JSONObject(s);
    }

    public String getAsString(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.protocol.handle-redirects",false);
        get.setParams(params);
        HttpResponse r = httpClient.execute(get);
        return EntityUtils.toString(r.getEntity());
    }

    public String getAsStringIfOk(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.protocol.handle-redirects",false);
        get.setParams(params);
        HttpResponse r = httpClient.execute(get);
        String s = EntityUtils.toString(r.getEntity());
        if (r.getStatusLine().getStatusCode() == 200) {
            return s;
        } else {
            throw new RuntimeException(s);
        }
    }

}
