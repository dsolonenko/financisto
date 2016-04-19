/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.activity;



import static ru.orangesoftware.financisto.utils.NetworkUtils.isOnline;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncEngine;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncOptions;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncTask;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;


public class FlowzrSyncActivity extends Activity  {
	
	public static final String PROPERTY_REG_ID = "registration_id";	
    public static final int CLASS_ACCOUNT = 1;
    public static final int CLASS_CATEGORY = 2;
    public static final int CLASS_TRANSACTION = 3;
    public static final int CLASS_PAYEE= 4;
    public static final int CLASS_PROJECT = 5;
    public static final int FLOWZR_SYNC_REQUEST_CODE = 6;    
 
    private long lastSyncLocalTimestamp=0;
    public Account useCredential;
	DefaultHttpClient http_client ;
	public Button bOk;    
	
	public String TAG="flowzr";

	public NotificationCompat.Builder mNotifyBuilder;
	public static final int NOTIFICATION_ID=0;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;	
	public NotificationManager nm;	
	
	public static boolean isRunning=false;
	
	public String regid="";
	GoogleCloudMessaging gcm; 
		
	public String FLOWZR_BASE_URL="https://flowzr-hrd.appspot.com";
	public static FlowzrSyncTask flowzrSyncTask;
	static FlowzrSyncEngine flowzrSyncEngine;	
	
	private static FlowzrSyncActivity me=null;

	
	public void setRunning() {
		  runOnUiThread(new Runnable() {
		  public void run() {
			bOk.setEnabled(false);
			bOk.setText(R.string.flowzr_sync_inprogress);
			setProgressBarIndeterminateVisibility(true);		
				    }
			});
	}
	
	public void setIsFinished() {
		setReady();
		if (MainActivity.activity !=null) {
			  runOnUiThread(new Runnable() {
				     public void run() {
				    	 ((MainActivity)MainActivity.activity).refreshCurrentTab();
				    }
				});		
					
		}
	}
	
	public void setReady() {
		  runOnUiThread(new Runnable() {
			     public void run() {
			        TextView tv = (TextView) findViewById(R.id.sync_was);
					//SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());		
					//lastSyncLocalTimestamp=preferences.getLong(FlowzrSyncOptions.PROPERTY_LAST_SYNC_TIMESTAMP,0);
					lastSyncLocalTimestamp=MyPreferences.getFlowzrLastSync(FlowzrSyncActivity.this)+ 1400236028;

			        tv.setText(getString(R.string.flowzr_sync_was) + " " + new Date(lastSyncLocalTimestamp).toLocaleString());

			    	bOk.setText(R.string.ok);
			 		bOk.setEnabled(true);	
			        CheckBox chk=(CheckBox)findViewById(R.id.chk_sync_from_zero);
			        chk.setChecked(false);			 		
					setProgressBarIndeterminateVisibility(false);	
			    }
			});
	}
	  
	public void renderLastTime(long lastTime) {
	    TextView tv = (TextView) findViewById(R.id.sync_was);
	    tv.setText(getString(R.string.flowzr_sync_was) + " " + new Date(lastTime).toLocaleString());			
	}	
	

	
    public static FlowzrSyncActivity getMySelf() {
        return FlowzrSyncActivity.me;
    }
	


    public void initProgressDialog() {
    	Intent notificationIntent = new Intent(getApplicationContext(),FlowzrSyncActivity.class);
    	PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
    	        0, notificationIntent,
    	        PendingIntent.FLAG_CANCEL_CURRENT);			    	
    	
    	mNotifyBuilder.setContentIntent(contentIntent)
    	            .setSmallIcon(R.drawable.icon)
    	            .setWhen(System.currentTimeMillis())
    	            .setAutoCancel(true)
    	            .setContentTitle(getApplicationContext().getString(R.string.flowzr_sync))
    	            .setContentText(getApplicationContext().getString(R.string.flowzr_sync_auth_inprogress));
    	nm.notify(NOTIFICATION_ID, mNotifyBuilder.build()); 
    }
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlowzrSyncActivity.me=this;
        
    	mNotifyBuilder = new NotificationCompat.Builder(getApplicationContext());        
    	nm = (NotificationManager) getApplicationContext()
    	        .getSystemService(Context.NOTIFICATION_SERVICE);
    	setContentView(R.layout.flowzr_sync);
        restoreUIFromPref();      
        		
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        final Account[] accounts = accountManager.getAccountsByType("com.google");
		if (accounts.length<1) {
			new AlertDialog.Builder(this)
			.setTitle(getString(R.string.flowzr_sync_error))
			.setMessage(R.string.account_required)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			})
			.show();
		}
        //checkbox
        CheckBox chk=(CheckBox)findViewById(R.id.chk_sync_from_zero);
        OnClickListener chk_listener = new OnClickListener() {
            public void onClick(View v) {
                lastSyncLocalTimestamp=0;
                renderLastTime(0);
                flowzrSyncEngine.resetLastTime(getApplicationContext());
            }
        };
        chk.setOnClickListener(chk_listener);
        //radio crendentials
		RadioGroup radioGroupCredentials = (RadioGroup)findViewById(R.id.radioCredentials);
		OnClickListener radio_listener = new OnClickListener() {
		    public void onClick(View v) {
		    	RadioButton radioButtonSelected = (RadioButton)findViewById(v.getId());
	    	for (Account account: accounts) {
		    		if (account.name==radioButtonSelected.getText()) {
                        lastSyncLocalTimestamp=0;
                        renderLastTime(0);
                        flowzrSyncEngine.resetLastTime(getApplicationContext());
                        useCredential=account;
		    		}
		    	}		    			    	
		    }
		};
        radioGroupCredentials.setOnClickListener(radio_listener);
		//initialize value
	     for (int i = 0; i < accounts.length; i++) {
	    	 	RadioButton rb = new RadioButton(this);
	            radioGroupCredentials.addView(rb); //, 0, lp); 
	    	 	rb.setOnClickListener(radio_listener);
	        	rb.setText(((Account) accounts[i]).name);
	        	String prefAccount=MyPreferences.getFlowzrAccount(this);
	    		if (prefAccount!=null) {	        	
		        	if ( accounts[i].name.equals(prefAccount)) {
		        		useCredential=accounts[i];
		                rb.toggle(); //.setChecked(true);
		        	} 
	    		}
		}
	            	
        bOk = (Button) findViewById(R.id.bOK);
        bOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	startSync();
            	
            }
        });

        Button bCancel = (Button) findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	if (flowzrSyncEngine!=null) {
            		flowzrSyncEngine.isCanceled=true;
            	}
            	isRunning=false;
                setResult(RESULT_CANCELED);    
                setReady();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));                
                //finish();
            }
        });    
           
        Button textViewAbout = (Button) findViewById(R.id.buySubscription);
        textViewAbout.setOnClickListener(new View.OnClickListener() {        		
        	public void onClick(View v) {
	        		if (isOnline(FlowzrSyncActivity.this)) {
                        visitFlowzr(useCredential);
	        		} else {         			
	    				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);            			           			
	        		}        			
        		}
		});

        Button textViewAboutAnon = (Button) findViewById(R.id.visitFlowzr);
        textViewAboutAnon.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
	        		if (isOnline(FlowzrSyncActivity.this)) {
                        visitFlowzr(null);
	        		} else {
	    				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);
	        		}
        	}
		});

        TextView textViewNotes = (TextView) findViewById(R.id.flowzrPleaseNote);
        textViewNotes.setMovementMethod(LinkMovementMethod.getInstance());
        textViewNotes.setText(Html.fromHtml(getString(R.string.flowzr_terms_of_use)));
        if (MyPreferences.isAutoSync(this)) {
	        if (checkPlayServices()) {
	            gcm = GoogleCloudMessaging.getInstance(this);
	            regid = getRegistrationId(getApplicationContext());
	
	            if (regid.equals("")) {
	                registerInBackground();
	            }
	            Log.i(TAG,"Google Cloud Messaging registered as :" + regid);
	        } else {
	            Log.i(TAG, "No valid Google Play Services APK found.");
	        }
        }
	}

	
	public void startSync () {
    	if (FlowzrSyncEngine.isRunning) {
    		Toast.makeText(this, R.string.flowzr_sync_inprogress, Toast.LENGTH_SHORT).show();   
    		return;
    	}
	    String accountName=useCredential.name;
	    MyPreferences.setFlowzrAccount(getApplicationContext(), accountName);
        if (accountName == null) {     	
        	Toast.makeText(this, R.string.flowzr_choose_account, Toast.LENGTH_SHORT).show();         	
        	return;
        }
		if (isOnline(FlowzrSyncActivity.this)) {
	        //checkPlayServices();    
		} else {         			
			showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);            			           			
			return;
		}   
  	
		if (FlowzrSyncEngine.isRunning) {
			showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_auth_inprogress);  				       
			return;
		}
		
    	Thread myThread = new Thread(new Runnable(){
    	    @Override
    	    public void run()
    	    {
    	    	Object o = new FlowzrSyncTask(FlowzrSyncActivity.this).execute();
    	    }
    	});
    	myThread.start();
        Toast.makeText(this, R.string.flowzr_sync_inprogress, Toast.LENGTH_SHORT).show();    	
    	finish();
    }
	
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId=="") {
	        Log.i(TAG, "GCM Registration not found in prefs.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    String registeredVersion = prefs.getString(FlowzrSyncOptions.PROPERTY_APP_VERSION, "");
	    String currentVersion;
		try {
			currentVersion = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
		    if (!registeredVersion.equals(currentVersion)) {
		        Log.i(TAG, "App version changed.");
		        return "";
		    }
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return registrationId;
	}

	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	@SuppressWarnings("unchecked")
	private void registerInBackground() {
	    AsyncTask execute = new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				 String msg = "";
		            try {
		                if (gcm == null) {
		                    gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
		                }
				        Log.i(TAG, "Registering GCM in background ...");		                
		                regid = gcm.register(FlowzrSyncOptions.GCM_SENDER_ID);
		                msg = "Device registered, registration ID=" + regid;

		                // You should send the registration ID to your server over HTTP,
		                // so it can use GCM/HTTP or CCS to send messages to your app.
		                // The request to your server should be authenticated if your app
		                // is using accounts.
		                sendRegistrationIdToBackend();

		                // For this demo: we don't need to send it because the device
		                // will send upstream messages to a server that echo back the
		                // message using the 'from' address in the message.

		                // Persist the regID - no need to register again.
		                storeRegistrationId(getApplicationContext(), regid);
		            } catch (IOException ex) {
		                msg = "Error :" + ex.getMessage();
		                // If there is an error, don't just keep trying to register.
		                // Require the user to click a button again, or perform
		                // exponential back-off.
				        Log.i(TAG, msg);		                
		            }
		            return msg;
				
				
			}
	    }.execute(null, null, null);
	   
	}
    private void sendRegistrationIdToBackend() {
    	//@TODO Sending GCM registration key to server ...
        Log.i(TAG, "Sending GCM registration key to server ...");	
      }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appVersion;
		try {
			appVersion = context.getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
	        Log.i(TAG, "Saving regId on app version " + appVersion);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(PROPERTY_REG_ID, regId);
	        editor.putString(FlowzrSyncOptions.PROPERTY_APP_VERSION, appVersion);
	        editor.commit();
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
    }    
    
    
    private void visitFlowzr(Account useCredential) {
        String url=FLOWZR_BASE_URL + "/paywall/";
        if (useCredential !=null) {
            url=url + useCredential.name;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
	protected void onResume() {
		super.onResume();
		if (this.isRunning) {
			setRunning();

		} else {
			setReady();
		}
        restoreUIFromPref();
        checkPlayServices();        
	}



	private void showErrorPopup(Context context, int message) {
		new AlertDialog.Builder(context)
		.setMessage(message)
		.setTitle(R.string.error)
		.setPositiveButton(R.string.ok, null)
		.setCancelable(true)
		.create().show();
	}	
   

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FLOWZR_SYNC_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {                
                        saveOptionsFromUI();
            }
        }
    }    



	protected void saveOptionsFromUI() {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putString(FlowzrSyncOptions.PROPERTY_USE_CREDENTIAL, useCredential.name);  
        CheckBox chk=(CheckBox)findViewById(R.id.chk_sync_from_zero);
        if (chk.isChecked()) {
    		editor.putLong(FlowzrSyncOptions.PROPERTY_LAST_SYNC_TIMESTAMP, 0);  
    		lastSyncLocalTimestamp=0;
        }
        editor.commit();          
	}

    protected void restoreUIFromPref() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());		
		lastSyncLocalTimestamp=MyPreferences.getFlowzrLastSync(getApplicationContext());
				preferences.getLong(FlowzrSyncOptions.PROPERTY_LAST_SYNC_TIMESTAMP,0);
        AccountManager accountManager = AccountManager.get(getApplicationContext());
		Account[] accounts = accountManager.getAccountsByType("com.google");
        for (Account account : accounts) {
            if (preferences.getString(FlowzrSyncOptions.PROPERTY_USE_CREDENTIAL, "").equals(account.name)) {
                useCredential = account;
            }
        }
        TextView tv = (TextView) findViewById(R.id.sync_was);
        tv.setText(getString(R.string.flowzr_sync_was) + " " + new Date(lastSyncLocalTimestamp).toLocaleString());

    }
    	
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            Log.w(TAG, "This device is does not support Google Play Services.");
	            finish();
	        }
	        return false;
	    }
	    return true;
	}
}
