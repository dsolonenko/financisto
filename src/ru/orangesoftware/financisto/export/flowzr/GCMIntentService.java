package ru.orangesoftware.financisto.export.flowzr;


import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
* IntentService responsible for handling GCM messages.
*/

public class GCMIntentService extends IntentService {

	static String TAG="flowzr";
	
	public static final int NOTIFICATION_ID = 1;
    NotificationCompat.Builder builder;

    public GCMIntentService() {
        super("GCMIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        if (!extras.isEmpty()) {
        	if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {	    		
        		if (FlowzrSyncEngine.isRunning) {
	        		Log.i(TAG,"sync already in progess");
        			return;
        		}
        		Log.i(TAG,"starting sync from GCM");
        		new FlowzrSyncTask(getApplicationContext()).execute();
            }
        }
    }
}
	
