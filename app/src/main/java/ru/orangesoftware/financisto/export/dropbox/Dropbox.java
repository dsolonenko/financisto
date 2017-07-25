/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.dropbox;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class Dropbox {

    private static final String APP_KEY = "INSERT_APP_KEY_HERE";

    private final Context context;

    private boolean startedAuth = false;
    private DbxClientV2 dropboxClient;

    public Dropbox(Context context) {
        this.context = context;
    }

    public void startAuth() {
        startedAuth = true;
        Auth.startOAuth2Authentication(context, APP_KEY);
    }

    public void completeAuth() {
        try {
            String authToken = Auth.getOAuth2Token();
            if (startedAuth && authToken != null) {
                try {
                    MyPreferences.storeDropboxKeys(context, authToken);
                } catch (IllegalStateException e) {
                    Log.i("Financisto", "Error authenticating Dropbox", e);
                }
            }
        } finally {
            startedAuth = false;
        }
    }

    public void deAuth() {
        MyPreferences.removeDropboxKeys(context);
        if (dropboxClient != null) {
            try {
                dropboxClient.auth().tokenRevoke();
            } catch (DbxException e) {
                Log.e("Financisto", "Unable to unlink Dropbox", e);
            }
        }
    }

    private boolean authSession() {
        String accessToken = MyPreferences.getDropboxAuthToken(context);
        if (accessToken != null) {
            if (dropboxClient == null) {
                DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("financisto")
                        .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                        .build();
                dropboxClient = new DbxClientV2(requestConfig, accessToken);
            }
            return true;
        }
        return false;
    }

    public FileMetadata uploadFile(File file) throws Exception {
        if (authSession()) {
            try {
                InputStream is = new FileInputStream(file);
                try {
                    FileMetadata fileMetadata = dropboxClient.files().uploadBuilder("/" + file.getName()).withMode(WriteMode.ADD).uploadAndFinish(is);
                    Log.i("Financisto", "Dropbox: The uploaded file's rev is: " + fileMetadata.getRev());
                    return fileMetadata;
                } finally {
                    IOUtil.closeInput(is);
                }
            } catch (Exception e) {
                Log.e("Financisto", "Dropbox: Something wrong", e);
                throw new ImportExportException(R.string.dropbox_error, e);
            }
        } else {
            throw new ImportExportException(R.string.dropbox_auth_error);
        }
    }

    List<String> listFiles() throws Exception {
        if (authSession()) {
            try {
                List<String> files = new ArrayList<String>();
                ListFolderResult listFolderResult = dropboxClient.files().listFolder("");
                for (Metadata metadata : listFolderResult.getEntries()) {
                    String name = metadata.getName();
                    if (name.endsWith(".backup")) {
                        files.add(name);
                    }
                }
                Collections.sort(files, new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s2.compareTo(s1);
                    }
                });
                return files;
            } catch (Exception e) {
                Log.e("Financisto", "Dropbox: Something wrong", e);
                throw new ImportExportException(R.string.dropbox_error, e);
            }
        } else {
            throw new ImportExportException(R.string.dropbox_auth_error);
        }
    }

    public InputStream getFileAsStream(String backupFile) throws Exception {
        if (authSession()) {
            try {
                return dropboxClient.files().downloadBuilder("/" + backupFile).start().getInputStream();
            } catch (Exception e) {
                Log.e("Financisto", "Dropbox: Something wrong", e);
                throw new ImportExportException(R.string.dropbox_error, e);
            }
        } else {
            throw new ImportExportException(R.string.dropbox_auth_error);
        }
    }
}
