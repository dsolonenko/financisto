/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.flowzr;


import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.FlowzrSyncActivity;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

public class FlowzrSyncTask extends AsyncTask<String, String, Object> {

	protected final Context context;
	public static final String TAG = "flowzr";
	public static  DefaultHttpClient  http_client;
	private static DatabaseAdapter dba;
	
    public FlowzrSyncTask(Context context) {
    	this.context=context;
    	
    	BasicHttpParams params = new BasicHttpParams();
    	SchemeRegistry schemeRegistry = new SchemeRegistry();
    	schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    	final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
    	schemeRegistry.register(new Scheme("https", (SocketFactory) sslSocketFactory, 443));
    	ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
    	this.http_client = new DefaultHttpClient(cm, params);
    	this.dba=new DatabaseAdapter(context);
    }

    public static android.accounts.Account getAndroidAccount(Context context) {
        String accountName=MyPreferences.getFlowzrAccount(context);
        AccountManager accountManager = AccountManager.get(context);
        android.accounts.Account[] accounts = accountManager.getAccountsByType("com.google");
        Account useCredential = null;
        for (int i = 0; i < accounts.length; i++) {
            if (accountName.equals(((android.accounts.Account) accounts[i]).name)) {
                return accounts[i];
            }
        }
        return null;
    }

    protected Object work(Context context, DatabaseAdapter dba, String... params) throws ImportExportException {    	

    	AccountManager accountManager = AccountManager.get(context);
		android.accounts.Account[] accounts = accountManager.getAccountsByType("com.google");
		
	    String accountName=MyPreferences.getFlowzrAccount(context);
        if (accountName == null) {
			NotificationManager nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);

			Intent notificationIntent = new Intent(context,
					FlowzrSyncActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
					notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			Builder mNotifyBuilder = new NotificationCompat.Builder(context);
			mNotifyBuilder
					.setContentIntent(contentIntent)
					.setSmallIcon(R.drawable.icon)
					.setWhen(System.currentTimeMillis())
					.setAutoCancel(true)
					.setContentTitle(context.getString(R.string.flowzr_sync))
					.setContentText(
							context.getString(R.string.flowzr_choose_account));
			nm.notify(0, mNotifyBuilder.build());		
			Log.i("Financisto","account name is null");
            throw new ImportExportException(R.string.flowzr_choose_account);
        }
		Account useCredential = null;
		for (int i = 0; i < accounts.length; i++) {
	    	 if (accountName.equals(((android.accounts.Account) accounts[i]).name)) {
	    		 useCredential=accounts[i];
	    	 }
	     }	    	
		accountManager.getAuthToken(useCredential, "ah", false, new GetAuthTokenCallback(), null);    	
    	return null;
    }

    
    @Override
	protected Object doInBackground(String... params) {

    	DatabaseAdapter db = new DatabaseAdapter(context);
		db.open();
		try {
			try {
				return work(context, db, params);
			} catch (ImportExportException e) {
				e.printStackTrace();
				return e;
			}	
		} finally {
			db.close();
		}			
		
	}

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }


    
	@Override
	protected void onPostExecute(Object result) {		
			if (!(result instanceof Exception)) {
				
				
			}
	}


public class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
		public void run(AccountManagerFuture<Bundle> result) {
			Bundle bundle;	        
			try {
				bundle = result.getResult();
				Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
				if(intent != null) {
					// User input required
					context.startActivity(intent);
				} else {
	            	AccountManager.get(context).invalidateAuthToken(bundle.getString(AccountManager.KEY_ACCOUNT_TYPE), bundle.getString(AccountManager.KEY_AUTHTOKEN));
	            	AccountManager.get(context).invalidateAuthToken("ah", bundle.getString(AccountManager.KEY_AUTHTOKEN));
	            	onGetAuthToken(bundle);
				}
			} catch (OperationCanceledException e) {
				//notifyUser(context.getString(R.string.flowzr_sync_error_no_network), 100);
				//showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);
				//context.setReady();
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				//notifyUser(context.getString(R.string.flowzr_sync_error_no_network), 100);			
				//flowzrSyncActivity.setReady();				
				e.printStackTrace();
			} catch (IOException e) {
				//notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_error_no_network), 100);		
				//flowzrSyncActivity.setReady();				
				e.printStackTrace();
			}
		}
	}

	protected void onGetAuthToken(Bundle bundle) {
		String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
		new GetCookieTask().execute(auth_token);
	}
 
	private class GetCookieTask extends AsyncTask<String, Void, Boolean> {
		protected Boolean doInBackground(String... tokens) {
			//notifyUser(context.getString(R.string.flowzr_sync_auth_inprogress), 15);
			try {								
				http_client.getParams().setParameter("http.protocol.content-charset","UTF-8");
				// Don't follow redirects
				http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
				HttpGet http_get = new HttpGet(FlowzrSyncEngine.FLOWZR_BASE_URL + "/_ah/login?continue=" 
												+ FlowzrSyncEngine.FLOWZR_BASE_URL +"/&auth=" + tokens[0]);
				HttpResponse response;
				response = http_client.execute(http_get);
				response.getEntity().consumeContent();
				if(response.getStatusLine().getStatusCode() != 302) {
					// Response should be a redirect
					return false;
				}
				for(Cookie cookie : http_client.getCookieStore().getCookies()) {
					if(cookie.getName().equals("ACSID")) {					
						return true;
					}
				}
			} catch (ClientProtocolException e) {
				Log.e("flowzr",e.getMessage());				
				return false;
			} catch (IOException e) {  				
				Log.e("flowzr",e.getMessage());
				return false;
			} finally {
				http_client.getParams().setParameter("http.protocol.content-charset","UTF-8");				
				http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);				
			}
			return false;
		}

		protected void onPostExecute(Boolean result) {

        	Thread myThread = new Thread(new Runnable(){
        	    @Override
        	    public void run()
        	    {
        	    	FlowzrSyncEngine.create(context,dba,http_client);
        	    }
        	});

        	myThread.start();
		
		}
	}
}