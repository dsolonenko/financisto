/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.flowzr;

import org.apache.http.impl.client.DefaultHttpClient;

import android.content.SharedPreferences;

public class FlowzrSyncOptions {
	
	public static long last_sync_ts=-1; //zero is default server ...
	public static long startTimestamp=-1; //useful only for not pushing what have just been pooled
	public String useCredential;
	public String rootFolderId;
	public 	DefaultHttpClient http_client;
    public static final String PROPERTY_USE_CREDENTIAL = "USE_CREDENTIAL";  	
    public static final String PROPERTY_AUTO_SYNC = "AUTO_SYNC";    
    public static final String PROPERTY_ROOT_FOLDER_ID = "ROOT_FOLDER_ID";  
    public static final String PROPERTY_LAST_SYNC_TIMESTAMP = "LAST_SYNC_LOCAL_TIMESTAMP";   
    public static final String PROPERTY_APP_VERSION = "appVersion";
    
	public static final String FLOWZR_BASE_URL="https://flowzr-hrd.appspot.com";
	public static final String GCM_SENDER_ID = "98966630416";	
	public static final String PROPERTY_REG_ID = "registration_id";
	public static String FLOWZR_API_URL=FLOWZR_BASE_URL + "/financisto3/";
	public String appVersion="";
	
    public FlowzrSyncOptions(String strUseCredential, long lastSyncLocalTimestamp, DefaultHttpClient pHttp_client,String pRootFolderId,String _appVersion) {
        this.last_sync_ts = lastSyncLocalTimestamp;
        this.useCredential=strUseCredential;
        this.http_client=pHttp_client;
        this.rootFolderId=pRootFolderId;
        this.appVersion=_appVersion;
    }

    public static FlowzrSyncOptions fromPrefs(SharedPreferences preferences) {
    	long lastSyncLocalTimestamp=preferences.getLong(PROPERTY_LAST_SYNC_TIMESTAMP,0);    	
        String useCredential=preferences.getString(PROPERTY_USE_CREDENTIAL,"");
        String rootFolderId=preferences.getString(PROPERTY_ROOT_FOLDER_ID,"");
        String appVersion=preferences.getString(PROPERTY_APP_VERSION,"");
        return new FlowzrSyncOptions(useCredential,lastSyncLocalTimestamp,null,rootFolderId,appVersion);            		 		    	
    }          		 		    	
    
    public String getNamespace() {
    	return this.useCredential.split("@")[0];
    }    
}
