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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String fileName = getIntent().getStringExtra(FILENAME);
        WebView webView = new WebView(this);
        setContentView(webView);
        webView.loadUrl("file:///android_asset/"+fileName+".htm");
    }

}