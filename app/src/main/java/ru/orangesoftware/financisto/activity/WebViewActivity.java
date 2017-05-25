/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 3/24/11 10:22 PM
 */
public class WebViewActivity extends Activity {

    public static final String FILENAME = "filename";
    public static final String URL = "url";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        String url = getIntent().getStringExtra(URL);
        String fileName = getIntent().getStringExtra(FILENAME);
        if (fileName != null) {
            url = "file:///android_asset/" + fileName + ".htm";

        }
        webView.loadUrl(url);
    }

}