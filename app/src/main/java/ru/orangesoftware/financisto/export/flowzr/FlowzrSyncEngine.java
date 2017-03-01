package ru.orangesoftware.financisto.export.flowzr;
/*

 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */


import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.api.client.json.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.activity.FlowzrSyncActivity;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncTask.GetAuthTokenCallback;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionAttribute;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.IntegrityFix;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class FlowzrSyncEngine  {
	private static String TAG="flowzr";
	private final static String FLOWZR_MSG_NET_ERROR="FLOWZR_MSG_NET_ERROR";
    public static HttpContext ctx;
    public static SQLiteDatabase db;
    public static DatabaseAdapter dba;
    
    public static MyEntityManager em;
    static InputStream isHttpcontent = null;
    static JSONObject jObj = null;
    static String json = "";
    private final static long KEY_CREATE=-1;

    private static Context context;
 
    private static String[] tableNames= {"attributes","currency","project","payee","account","LOCATIONS","category","transactions",DatabaseHelper.BUDGET_TABLE, "currency_exchange_rate"};
	private static Class[] clazzArray = {Attribute.class,Currency.class,Project.class,Payee.class,Account.class,MyLocation.class,Category.class,Transaction.class,Budget.class,ExchangeRate.class};
	
	private static int MAX_PULL_SIZE=50;
	private static int MAX_PUSH_SIZE=20;
	static JsonReader reader = null;
	static InputStream is = null;
	static final int REQUEST_AUTHORIZATION = 2;

	public static String rootFolderId=null;	
	static final int REQUEST_ACCOUNT_PICKER = 8;

	public static final java.io.File PICTURES_DIR = new java.io.File(Environment.getExternalStorageDirectory(), "financisto/pictures");
	public static boolean isCanceled=false;
	public static boolean isRunning=false;
	
	public static NotificationManager mNotificationManager;		
	public static  NotificationCompat.Builder mNotifyBuilder;
	public static final int NOTIFICATION_ID=666;
	public static final int NOTIFICATION_ID2=667;
	public static  DefaultHttpClient  http_client;
	public static long last_sync_ts;
	public static long startTimestamp;
	public static String nsString; // used to identify a book on Flowzr
	
	public static String FLOWZR_BASE_URL="https://flowzr-hrd.appspot.com/";
	public static String FLOWZR_API_URL="https://flowzr-hrd.appspot.com/financisto3/";	
	
	public static MainActivity currentActivity = null;
	
	  public synchronized static void setUpdatable(MainActivity updatable) {
		    currentActivity = updatable;
	  }
	
	public static String create(Context p_context,DatabaseAdapter p_dba, DefaultHttpClient p_http) {
    	startTimestamp=System.currentTimeMillis(); 
	
    	if (isRunning==true) {
    		isCanceled=true;
    		isRunning=false;
    	}
    	isRunning=true;    
    	boolean recordSyncTime=true;
		
		dba=p_dba;
		db=dba.db();
		em=dba.em();
		http_client=p_http;
		context=p_context;

		last_sync_ts=MyPreferences.getFlowzrLastSync(context);
		FLOWZR_BASE_URL="https://" + MyPreferences.getSyncApiUrl(context);
		FLOWZR_API_URL=FLOWZR_BASE_URL + "/financisto3/";
		
		nsString=MyPreferences.getFlowzrAccount(context).replace("@", "_"); //urlsafe
        
		Log.i(TAG,"init sync engine, last sync was " + new Date(last_sync_ts).toLocaleString());   	
		//if (true) {
		if (!checkSubscriptionFromWeb()) {
			Intent notificationIntent = new Intent(context,
			FlowzrSyncActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
            notificationIntent, 0);

            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.icon)
                    .setTicker(context.getString(R.string.flowzr_subscription_required))
                    .setContentTitle(context.getString(R.string.flowzr_sync_error))
                    .setContentText(context.getString(R.string.flowzr_subscription_required, MyPreferences.getFlowzrAccount(context)))
                    .setContentIntent(pendingIntent).setAutoCancel(true).build();
            notificationManager.notify(0, notification);
			
			Log.w("flowzr","subscription rejected from web");
			isCanceled=true;
			MyPreferences.unsetAutoSync(context);
			return null;
		} else {
	        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	        // Sets an ID for the notification, so it can be updated

	        mNotifyBuilder = new NotificationCompat.Builder(context)
	        	.setAutoCancel(true)
	            .setContentTitle(context.getString(R.string.flowzr_sync))
	            .setContentText(context.getString(R.string.flowzr_sync_inprogress))
	            .setSmallIcon(R.drawable.icon);			
		}
		
        if (!isCanceled) {
        	notifyUser("fix created entities",5);
	    	fixCreatedEntities();
        }
        /**
         * pull delete
         */ 
        if (!isCanceled) {
	        notifyUser(context.getString(R.string.flowzr_sync_receiving) + " ...",10);
	        try {
				pullDelete(last_sync_ts);				
			} catch (Exception e) {
				sendBackTrace(e);
				recordSyncTime=false;
			}
	      }
        /**
         * push delete
         */        
        if (!isCanceled) {
	        notifyUser(context.getString(R.string.flowzr_sync_sending) + " ...",15);
	        try {
				pushDelete();
			} catch (Exception e) {
				sendBackTrace(e);
				recordSyncTime=false;				
			}
        }        
        /**
         * pull update
         */
        if (!isCanceled && last_sync_ts==0) {		
	        notifyUser(context.getString(R.string.flowzr_sync_receiving) + " ...",20);
				try {
					pullUpdate();
				} catch (IOException e) {
					sendBackTrace(e);
					recordSyncTime=false;					
				} catch (JSONException e) {
					sendBackTrace(e);
					recordSyncTime=false;					
				} catch (Exception e) {
					sendBackTrace(e);
					recordSyncTime=false;					
				}				
        }
        /**
         * push update
         */
        if (!isCanceled) {
	        notifyUser(context.getString(R.string.flowzr_sync_sending) + " ...",35);
	        try {
				pushUpdate();
			} catch (ClientProtocolException e) {
                e.printStackTrace();
				sendBackTrace(e);
				recordSyncTime=false;
			} catch (IOException e) {
                e.printStackTrace();
				sendBackTrace(e);
				recordSyncTime=false;
			} catch (JSONException e) {
                e.printStackTrace();
				sendBackTrace(e);
				recordSyncTime=false;
			} catch (Exception e) {
                e.printStackTrace();
				recordSyncTime=false;
			}      
        }
        /**
         * pull update
         */
        if (!isCanceled && last_sync_ts>0) {		
	        notifyUser(context.getString(R.string.flowzr_sync_receiving) + " ...",20);
				try {
					pullUpdate();
				} catch (IOException e) {
					sendBackTrace(e);
					recordSyncTime=false;					
				} catch (JSONException e) {
					sendBackTrace(e);
					recordSyncTime=false;					
				} catch (Exception e) {
					sendBackTrace(e);
					recordSyncTime=false;					
				}				
        }

        /**
         * send account balances boundaries
         */
        if (!isCanceled) {        
        //if (true) { //will generate a Cloud Messaging request if prev. aborted        
        	notifyUser(context.getString(R.string.flowzr_sync_sending) + "..." ,80);
        	//nm.notify(NOTIFICATION_ID, mNotifyBuilder.build()); 
        	ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        	nameValuePairs.add(new BasicNameValuePair("action","balancesRecalc"));
        	nameValuePairs.add(new BasicNameValuePair("last_sync_ts",String.valueOf(last_sync_ts)));        
        	try {
				httpPush(nameValuePairs,"balances");
			} catch (Exception e) {
				sendBackTrace(e);
			}    
        }	    	
        notifyUser(context.getString(R.string.integrity_fix),85);
        new IntegrityFix(dba).fix();
        
        notifyUser("Widgets ...",90);     
        AccountWidget.updateWidgets(context);             


        Handler refresh = new Handler(Looper.getMainLooper());
        refresh.post(new Runnable() {
            public void run()
            {
            	if (currentActivity !=null) {
            		//currentActivity.refreshCurrentTab();
            	}
            }
        });
        
        
        if (!isCanceled && MyPreferences.doGoogleDriveUpload(context)) {
            notifyUser(context.getString(R.string.flowzr_sync_sending) + " Google Drive",95);  
        	pushAllBlobs();
        }  else {
        	Log.i("flowzr","picture upload desactivated in prefs");
        }
        notifyUser(context.getString(R.string.flowzr_sync_success),100);
        if (isCanceled==false) {
            if (recordSyncTime==true) {
            	last_sync_ts=System.currentTimeMillis();         	
	        	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
	        	editor.putLong("PROPERTY_LAST_SYNC_TIMESTAMP", last_sync_ts);
	        	editor.commit();        
           }        	
        }        
        //
        mNotificationManager.cancel(NOTIFICATION_ID);
        isRunning=false;
        isCanceled=false;
        if (context instanceof FlowzrSyncActivity) {
        	((FlowzrSyncActivity)context).setIsFinished();   
        }
		return FLOWZR_BASE_URL;        
    
    }

	public static void resetLastTime (Context ctx) {
    	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
    	if (editor!=null) {
    		editor.putLong("PROPERTY_LAST_SYNC_TIMESTAMP", 0);
    		editor.commit();    
    	}
	}
    
    public static void notifyUser(final String msg, final int pct) {
		Intent notificationIntent = new Intent(context,
		FlowzrSyncActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
        notificationIntent, 0);  
		if (mNotifyBuilder==null) {
	        mNotifyBuilder = new NotificationCompat.Builder(context)
        	.setAutoCancel(true)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.flowzr_sync_inprogress))
            .setSmallIcon(R.drawable.icon);		        
	        
	        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	        // Sets an ID for the notification, so it can be updated			
		}
    	mNotifyBuilder.setContentText(msg);
    	mNotifyBuilder.setContentIntent(pendingIntent);
    	mNotifyBuilder.setAutoCancel(true).build();
    	if (pct!=0) {
    		mNotifyBuilder.setProgress(100, pct,false);
    	}
    	mNotificationManager.notify(
            NOTIFICATION_ID,
            mNotifyBuilder.build());     	    	
    }
	
    static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
	
    public static void sendBackTrace(Exception result) {
     	final String msg=getStackTrace((Exception)result);
     	((Exception)result).printStackTrace();
     	         	
     	Thread trd = new Thread(new Runnable(){
     		  @Override
     		  public void run(){
     				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
     				nameValuePairs.add(new BasicNameValuePair("action","error"));
     				nameValuePairs.add(new BasicNameValuePair("stack",msg));					
     		        HttpPost httppost = new HttpPost(FLOWZR_API_URL + nsString + "/error/");
     		        try {
     					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
     				} catch (UnsupportedEncodingException e) {
     					e.printStackTrace();
     				}     		        
     		        try {
     					http_client.execute(httppost);
     				} catch (ClientProtocolException e1) {
     					e1.printStackTrace();
     				} catch (IOException e1) {
     					e1.printStackTrace();
     				} catch (Exception e) {
     					e.printStackTrace();
     				}
     		  }
     		});
     	trd.start();     	
     	return;
    }
    
    /*
     * Push job
     */    
    private static void pushUpdate() throws ClientProtocolException, IOException, JSONException, Exception {
    	int i=0;
    	for (String t : tableNames) {     
        	notifyUser("pushing " + t, 0);
    		pushUpdate(t,clazzArray[i]);
    		i++;
        }      
    }
    
	public static <T extends Object> void pushUpdate(String tableName,Class<T> clazz) throws ClientProtocolException, IOException, JSONException, Exception  {
		SQLiteDatabase db2=dba.db();
		Cursor cursorCursor;
		String sql;
		long total;

		sql="select count(*) from " + tableName  + " where updated_on<=0 or remote_key is null or (updated_on > " + last_sync_ts  ;
        if (!tableName.equals(DatabaseHelper.BUDGET_TABLE)) {
            sql=sql+ " and updated_on<" + startTimestamp + ")" ;
        } else {
            sql=sql+ ")";
        }

		cursorCursor=db.rawQuery(sql, null);
		cursorCursor.moveToFirst();
		total=cursorCursor.getLong(0);


        sql="select * from " + tableName +  " where updated_on<=0 or remote_key is null or (updated_on > " + last_sync_ts ;
        if (!tableName.equals(DatabaseHelper.BUDGET_TABLE)) {
            sql=sql+ " and updated_on<" + startTimestamp + ")" ;
        } else {
            sql=sql+ ")";
        }

		if (tableName.equals(DatabaseHelper.TRANSACTION_TABLE)) {
			sql+= " order by  parent_id asc,_id asc";	
		} else 	if (tableName.equals(DatabaseHelper.BUDGET_TABLE)) {
			sql+= " order by  parent_budget_id asc";	
		} else 	if (!tableName.equals("currency_exchange_rate")) {
			sql+= " order by  _id asc";	
		} 
		
		cursorCursor=db2.rawQuery(sql, null);
		JSONArray resultSet 	= new JSONArray();
		
		int i=0;
		if (cursorCursor.moveToFirst() && isCanceled!=true) {
            Log.i("flowzr","pushing "  + tableName);
			do {								 	
				if (i%10==0) {					
					//notifyUser(context.getString(R.string.flowzr_sync_sending) + " " + tableName, (int)(Math.round(i*100/total)));
				}				
				resultSet.put(cursorToDict(tableName,cursorCursor));
				i++;
				if (i%MAX_PUSH_SIZE==0) {
					String resp=makeRequest(tableName, resultSet.toString());
					resultSet 	= new JSONArray();
					if (resp.equals(FLOWZR_MSG_NET_ERROR)) {
						isCanceled=true;
					}
					if (isCanceled) {
						return ;
					}
				}
			} while (cursorCursor.moveToNext());						
		}	
		cursorCursor.close();
		if (i%MAX_PUSH_SIZE!=0) {
			String resp=makeRequest(tableName, resultSet.toString());
			if (resp.equals(FLOWZR_MSG_NET_ERROR)) {
				isCanceled=true;
                Log.e("flowzr",resp);
			}
			if (isCanceled) {
                Log.i("flowzr","sync canceled!");
				return ;
			}
		}
	}
		
	public static String makeRequest(String tableName, String json) throws ClientProtocolException, IOException, JSONException,Exception {
		if (isCanceled) {
			return FLOWZR_MSG_NET_ERROR;
		}

		String uri=FLOWZR_API_URL +  nsString + "/" + tableName + "/";
		String strResponse;

	        HttpPost httpPost = new HttpPost(uri);

	        httpPost.setEntity(new StringEntity(json,HTTP.UTF_8));
            httpPost.addHeader("Cookie","dev_appserver_login=test@example.com:False:18580476422013912411");

	        HttpResponse response =http_client.execute(httpPost);
		    HttpEntity entity = response.getEntity();
	        int code = response.getStatusLine().getStatusCode();
		    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			strResponse = reader.readLine();
			if (!tableName.equals("currency_exchange_rate")) {
				JSONArray arr=new JSONArray();			
				arr = new JSONArray(strResponse);
				 for(int i = 0; i < arr.length(); i++){
			            JSONObject o = arr.getJSONObject(i);        
			            String key=o.getString("key");
			            int id=o.getInt("id");
						ContentValues args = new ContentValues();
						args.put("remote_key", key);					
						db.update(tableName, args, String.format("%s = ?", "_id"),
						           new String[]{String.valueOf(id)});		
				 }						
			}	    
		    entity.consumeContent();	    
		    if (code!=200) {
		       throw new Exception(Html.fromHtml(strResponse).toString());
		    }
		return strResponse;    	 
	}
	
	
	private static JSONObject cursorToDict(String tableName,Cursor c) {
   	    int totalColumn = c.getColumnCount();
   	    JSONObject rowObject = new JSONObject();
   	    if (c.getColumnIndex("_id")!=-1) {
   	    	try {
				rowObject.put("_id" ,  c.getInt(c.getColumnIndex("_id")));
			} catch (JSONException e) {
				e.printStackTrace();
			}   	    	
   	    }
   	    for( int i=0 ;  i< totalColumn ; i++ ) {
   	    	if( c.getColumnName(i) != null ) {
   	    		String colName=c.getColumnName(i);
   	    		try {
       	    		if( c.getString(i) != null ) {
       	    			if (colName.endsWith("_id") || colName.equals("parent")) {
       	    				if (tableName.equals(DatabaseHelper.BUDGET_TABLE)) {
       	    					if (colName.equals("parent_budget_id")) {
	       	    					rowObject.put(colName ,  c.getInt(i));    
       	    					} else if (!colName.equals("_id")) {
	       	    					String[] entities=c.getString(c.getColumnIndex(colName)).split(",");	
	    							String keys="";
	    							for (String entity_id2: entities) {
	    								keys+=getRemoteKey(getTableForColName(colName),entity_id2) + ",";	
	    							}
	    							if (keys.endsWith(",")) {
	    								keys=keys.substring(0,keys.length()-1);
	    							}
	           	    				rowObject.put(colName ,  keys );       	    					
       	    					}
       	    				} else {
	       	    				if (!colName.equals("_id")) {
	       	    					String k=getRemoteKey(getTableForColName(colName),c.getString(i));
		       	    				if (k!=null) {
		       	    					rowObject.put(colName ,  k);	       	    				
		       	    				} else {
		       	    					rowObject.put(colName ,  c.getInt(i));
		       	    				}
	       	    				}
       	    				}
       	    			} else {
       	    				rowObject.put(colName , c.getString(c.getColumnIndex(colName)));
       	    			}
	       	    		/****/
	       	    		if (tableName.equals(DatabaseHelper.ACCOUNT_TABLE)) {
	       	 				String sql="select max(dateTime) as maxDate, min(dateTime) as minDate from " + DatabaseHelper.TRANSACTION_TABLE + " where from_account_id=" + c.getInt(c.getColumnIndex("_id")) ;		
	       	 				Cursor c2=db.rawQuery(sql, null);
	       	 				c2.moveToFirst();	
	       	 				rowObject.put("dateOfFirstTransaction" , c2.getString(1)); 
	       	 				rowObject.put("dateOfLastTransaction" , c2.getString(0)); 
	       	 				//each account can have a timezone so you can have a balance at closing day					
	       	 				rowObject.put("tz" , String.valueOf(TimeZone.getDefault().getRawOffset())); 
	       	 			} else if (tableName.equals(DatabaseHelper.CATEGORY_TABLE)) {
	       	 				//load parent id
	       	 				Category cat=dba.getCategory(c.getInt(0)); // sql build/load parentId	
	       	 				if (cat.getParentId()>0) {
                                    Category pcat = em.load(Category.class, cat.getParentId());
                                    rowObject.put("parent", pcat.remoteKey);
                                    rowObject.put("parent_id", pcat.id);

	       	 				}
	       	 				String attrPushString="";
	       	 				
	       	 				for (Attribute attr: dba.getAttributesForCategory(c.getInt(0))) {						
	       	 					attrPushString=attrPushString + attr.remoteKey + ";";
	       	 				}
	       	 				if (attrPushString!="") {	
	       	 					rowObject.put( "attributes" ,  attrPushString );       	 					
	       	 				}
	       	 			} else if (tableName.equals(DatabaseHelper.TRANSACTION_TABLE)) {
	       	 	            Map<Long, String> attributesMap = dba.getAllAttributesForTransaction(c.getInt(0));
	       	 	            String transaction_attribute="";
	       	 	            for (long attributeId : attributesMap.keySet()) {
	       	 	                transaction_attribute+= dba.getAttribute(attributeId).remoteKey + "=" + attributesMap.get(attributeId) +";";
	       	 	            } 
	       	 	            rowObject.put( "transaction_attribute" ,  transaction_attribute );        	 				
	       	 			}        	    			
	       	    		/****/       	    		
       	    		} else {
           	    		rowObject.put( colName ,  "" ); 
           	    	}
   	    		} catch( JSONException e ) {
   	    			Log.d(TAG, e.getMessage()  );
   	    		}
   	    	}
   	    }
		return rowObject;		
	}
    
    public static String getRemoteKey(String tableName,String localKey) {
    	if (localKey.equals("-1") || tableName==null) {
    		return null;
    	}
		Cursor c = db.query(tableName, new String[] { "remote_key" }, "_id = ?",
		          new String[]{ localKey }, null, null, null, null);	
		if (c.moveToFirst()) {
			String k = c.getString(0);
			c.close();
			return k;
		} else  {
			return localKey;
		}
    }
  
    
    
    private static String httpPush (ArrayList<NameValuePair> nameValuePairs,String action) throws Exception {
    	HttpPost httppost = new HttpPost(FLOWZR_API_URL + nsString + "/" + action + "/");
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
        HttpResponse response;
        String strResponse;
		response = http_client.execute(httppost);
        HttpEntity entity = response.getEntity();
        int code = response.getStatusLine().getStatusCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
		strResponse = reader.readLine(); 				 			
        entity.consumeContent();
        if (code!=200) {
        	 throw new Exception(Html.fromHtml(strResponse).toString());
        }
		return strResponse;    	    	
    }
 
    /**
     * According to :
     * http://sqlite.1065341.n5.nabble.com/Can-t-insert-timestamp-field-with-value-CURRENT-TIME-td42729.html
     * it is not possible to make alter table add column updated with a default timestamp at current_timestamp
     * so default is set as zero and this pre-sync function make all 0 at last_sync_ts + 1
     */    
    public static void fixCreatedEntities() {
    	long ctime=last_sync_ts + 1;
    	for (String t : tableNames) {         		
    		db.execSQL("update "  + t + " set updated_on=" + ctime + " where updated_on=0");
    	}
    }	
	

	private static String getTableForColName(String colName) {
		if (colName.equals("currency_id")) {
			return DatabaseHelper.CURRENCY_TABLE;
		}		
		if (colName.equals("from_currency_id")) {
			return DatabaseHelper.CURRENCY_TABLE;
		}
		if (colName.equals("to_currency_id")) {
			return DatabaseHelper.CURRENCY_TABLE;
		}
		if (colName.equals("last_location_id")) {
			return DatabaseHelper.LOCATIONS_TABLE;
		}
		if (colName.equals("last_project_id")) {
			return DatabaseHelper.PROJECT_TABLE;
		}
		if (colName.equals("last_category_id")) {
			return DatabaseHelper.CATEGORY_TABLE;
		}
		if (colName.equals("last_account_id")) {
			return DatabaseHelper.ACCOUNT_TABLE;
		}
		if (colName.equals("from_account_id")) {
			return DatabaseHelper.ACCOUNT_TABLE;
		}
		if (colName.equals("to_account_id")) {
			return DatabaseHelper.ACCOUNT_TABLE;
		}
		if (colName.equals("category_id")) {
			return DatabaseHelper.CATEGORY_TABLE;
		}
		if (colName.equals("project_id")) {
			return DatabaseHelper.PROJECT_TABLE;
		}
		if (colName.equals("payee_id")) {
			return DatabaseHelper.PAYEE_TABLE;
		}		
		if (colName.equals("location_id")) {
			return DatabaseHelper.LOCATIONS_TABLE;
		}
		if (colName.equals("parent_id")) {
			return DatabaseHelper.TRANSACTION_TABLE;
		}
		if (colName.equals("original_currency_id")) {
			return DatabaseHelper.CURRENCY_TABLE;
		}
		if (colName.equals("parent_budget_id")) {
			return DatabaseHelper.BUDGET_TABLE;
		}
		if (colName.equals("budget_account_id")) {
			return DatabaseHelper.ACCOUNT_TABLE;
		}
		if (colName.equals("budget_currency_id")) {
			return DatabaseHelper.CURRENCY_TABLE;
		}
		if (colName.equals("") || colName==null) {
			return null;
		}
		Log.e(TAG,"no parent table found for "+ colName);
		return null;
	}
        
	private Class<?> getClassForColName(String colName) {
		if (colName.equals("category_id")) {
			return Category.class;
		}
		if (colName.equals("project_id")) {
			return Project.class;
		}		
		if (colName.equals("payee_id")) {
			return Payee.class;
		}
		if (colName.equals("currency_id")) {
			return Currency.class;
		}
		if (colName.equals("from_currency_id")) {
			return Currency.class;
		}
		if (colName.equals("to_currency_id")) {
			return Currency.class;
		}		
		if (colName.equals("original_currency_id")) {
			return Currency.class;
		}	
		if (colName.equals("account_id")) {
			return Account.class;
		}		
		if (colName.equals("from_account_id")) {
			return Account.class;
		}
		if (colName.equals("to_account_id")) {
			return Account.class;
		}
		if (colName.equals("transaction_id")) {
			return Transaction.class;
		}	
		if (colName.equals("parent_id")) {
			return Transaction.class;
		}
		if (colName.equals("location_id")) {
			return MyLocation.class;
		}
		if (colName.equals("parent_budget_id")) {
			return Budget.class;
		}		
		if (colName.equals("budget_id")) {
			return Budget.class;
		}		
		if (colName.equals("budget_account_id")) {
			return Account.class;
		}
		if (colName.equals("budget_currency_id")) {
			return Currency.class;
		}		
		return null;
	}
    
    public static Object pushDelete() throws Exception {
		String sql="select count(*) from " + DatabaseHelper.DELETE_LOG_TABLE ;		
		Cursor cursorCursor=db.rawQuery(sql, null);
		cursorCursor.moveToFirst();
		long total=cursorCursor.getLong(0);
    	cursorCursor.close();
    	Cursor cursor=db.rawQuery("select table_name,remote_key from delete_log",null);
    	int i=0;
    	String del_list="";
    	if (cursor.moveToFirst()) {
    		do {
				//notifyUser("push delete",(int)(Math.round(i*100/total)));
				del_list+=cursor.getString(1) + ";";
    			i++;
    		} while (cursor.moveToNext());
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("action","pushDelete"));
			nameValuePairs.add(new BasicNameValuePair("remoteKey",del_list));
    		httpPush(nameValuePairs,"delete");    		
    	}    	
    	cursor.close();
    	return null;
    }
    
    public Object finishDelete() {
    	db.execSQL("delete from " + DatabaseHelper.DELETE_LOG_TABLE);
    	return null;
    }
    
    /**
     * Pull Job
     */
	private static Object saveOrUpdateAttributeFromJSON(long localKey,
			JSONObject jsonObjectEntity) {
		if (!jsonObjectEntity.has("name")) {
			return null;
		}
		
		Attribute tEntity=dba.getAttribute(localKey);
		
		try {			
			tEntity.remoteKey=jsonObjectEntity.getString("key");
			tEntity.name=jsonObjectEntity.getString("name");
			if (jsonObjectEntity.has("type")) {
				tEntity.type=jsonObjectEntity.getInt("type");
			}
			if (jsonObjectEntity.has("default_value")) {
				tEntity.defaultValue=jsonObjectEntity.getString("default_value");
			}
			if (jsonObjectEntity.has("list_values")) {			
				tEntity.listValues=jsonObjectEntity.getString("list_values");
			}
			dba.insertOrUpdate(tEntity);
			return tEntity;
		} catch (Exception e) {
			e.printStackTrace();
			return e;
		}	
	}

	public static <T> Object saveOrUpdateEntityFromJSON(Class<T> clazz,long id,JSONObject jsonObjectEntity) {					
		if (!jsonObjectEntity.has("name")) {
			return null;
		}
		MyEntity tEntity=(MyEntity) em.get(clazz, id);
		if (tEntity==null) {
			if (clazz==Project.class) {
				tEntity= new Project();
			} else if (clazz==Payee.class) {
				tEntity=new Payee();
			} 
			tEntity.id=KEY_CREATE; 			
		}
		//---
		try {			
			tEntity.remoteKey=jsonObjectEntity.getString("key");
			((MyEntity)tEntity).title=jsonObjectEntity.getString("name");
			if ((clazz)==Project.class) {
				if (jsonObjectEntity.has("is_active")) {
					if (jsonObjectEntity.getBoolean("is_active")) {
						((Project)tEntity).isActive=true;
					} else {
						((Project)tEntity).isActive=false;						
					}
				}
			}
			em.saveOrUpdate((MyEntity)tEntity);
			return tEntity;
		} catch (Exception e) {
			e.printStackTrace();
			return e;
		}		 		
	}

	public static <T> Object saveOrUpdateCategoryFromJSON(long id,JSONObject jsonObjectEntity) {

		if (!jsonObjectEntity.has("name")) {
			return null;
		}
		Category tEntity=new Category(KEY_CREATE);
		if (id != KEY_CREATE && id!=-2) {
			tEntity = dba.getCategory(id);				
		}
		//---
		try {			
			tEntity.remoteKey=jsonObjectEntity.getString("key");
			tEntity.title=jsonObjectEntity.getString("name");
			if (jsonObjectEntity.has("parentCategory") ) {
				try {			
					int l=(int) getLocalKey(DatabaseHelper.CATEGORY_TABLE, jsonObjectEntity.getString("parentCategory"));
					Category cParent=dba.getCategory(l);
					if (l!=KEY_CREATE) {
						tEntity.parent=cParent;
					}
				} catch (Exception e) {			
					Log.e(TAG,"Error setting parent to :" + jsonObjectEntity.getString("parentCategory"));					
					e.printStackTrace();
				}
			}

            if (jsonObjectEntity.has("type") ) {
                tEntity.type=jsonObjectEntity.getInt("type");
            }

                ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			if (jsonObjectEntity.has("attributes") ) {				
				for (String attr_key: jsonObjectEntity.getString("attributes").split(";")) {
						int l=(int) getLocalKey(DatabaseHelper.ATTRIBUTES_TABLE, attr_key);
						if (l>0) {						
							Attribute attr=dba.getAttribute(l);
							attributes.add(attr);
						}
				}				
			}

            String r=tEntity.remoteKey;

			//left, right
			dba.insertOrUpdate(tEntity, attributes);
            String sql="update category set remote_key='"+ r + "' where _id="+ tEntity.id ;
            db.execSQL(sql);




			
			return tEntity;
		} catch (Exception e) {
			e.printStackTrace();
			return e;
		}		 		
	}
	
	public static Object saveOrUpdateCurrencyRateFromJSON(JSONObject jsonObjectEntity) {		

		if (!jsonObjectEntity.has("effective_date")) {
			//return null;
		}
		try {
			long toCurrencyId= getLocalKey(DatabaseHelper.CURRENCY_TABLE, jsonObjectEntity.getString("to_currency"));
			long fromCurrencyId= getLocalKey(DatabaseHelper.CURRENCY_TABLE, jsonObjectEntity.getString("from_currency"));
			if (toCurrencyId>-1 && fromCurrencyId>-1) {
				Currency toCurrency=em.load(Currency.class,toCurrencyId);
				Currency fromCurrency=em.load(Currency.class, fromCurrencyId);
				long effective_date=jsonObjectEntity.getLong("effective_date")*1000;
				double rate=jsonObjectEntity.getDouble("rate");
				ExchangeRate exRate= new ExchangeRate();				
				exRate.toCurrencyId=toCurrency.id;
				exRate.fromCurrencyId=fromCurrency.id;
				exRate.rate=rate;
				exRate.date=effective_date;
				dba.saveRate(exRate);
			}
		} catch (Exception e) {
			Log.e(TAG,"unable to load a currency rate from server...");
			e.printStackTrace();
			
		}	
		return null;
	}
	
	public static Object saveOrUpdateBudgetFromJSON(long id,JSONObject jsonObjectEntity) throws JSONException {
		Budget tEntity=em.get(Budget.class, id);
		if (tEntity==null) {
			tEntity = new Budget();
			tEntity.id=KEY_CREATE; 									
		}			
		try {
			tEntity.remoteKey=jsonObjectEntity.getString("key");
		} catch (JSONException e2) {
			e2.printStackTrace();
		} 
		if (jsonObjectEntity.has("title")) {	
			try {
				tEntity.title=jsonObjectEntity.getString("title");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		if (jsonObjectEntity.has("categories")) {	
			try {
				String[] strArrCategories=jsonObjectEntity.getString("categories").split(",");
				tEntity.categories="";
				for (String key: strArrCategories) {
					tEntity.categories+=getLocalKey(DatabaseHelper.CATEGORY_TABLE, key)+",";
				}	
				if (tEntity.categories.endsWith(",")) {
					tEntity.categories=tEntity.categories.substring(0, tEntity.categories.length()-1);
				}
			} catch (Exception e) {
				Log.e(TAG,"Error parsing Budget categories");
				e.printStackTrace();
			}
		}
		if (jsonObjectEntity.has("projects")) {	
			try {
				String[] strArrProjects=jsonObjectEntity.getString("projects").split(",");
				tEntity.projects="";
				for (String key: strArrProjects) {
					tEntity.projects+=getLocalKey(DatabaseHelper.PROJECT_TABLE, key)+",";
				}				
				if (tEntity.projects.endsWith(",")) {
					tEntity.projects=tEntity.projects.substring(0, tEntity.projects.length()-1);
				}									
			} catch (Exception e) {
				Log.e(TAG,"Error parsing Budget.project_id ");				
				e.printStackTrace();
			}
		}
//		if (jsonObjectEntity.has("currency")) {				
//			try {
//				tEntity.currencyId=getLocalKey(DatabaseHelper.CURRENCY_TABLE, jsonObjectEntity.getString("currency"));
//			} catch (Exception e) {
//				Log.e(TAG,"Error parsing Budget.currency ");				
//				e.printStackTrace();
//			}
//		}
		if (jsonObjectEntity.has("budget_account_id")) {				
			try {
				tEntity.account=em.load(Account.class,getLocalKey(DatabaseHelper.ACCOUNT_TABLE, jsonObjectEntity.getString("budget_account_id")));
			} catch (Exception e) {
				Log.e(TAG,"Error parsing Budget.budget_account_id ");				
				e.printStackTrace();
			}
		} else {
			if (jsonObjectEntity.has("budget_currency_id")) {				
				try {
					tEntity.currency=em.load(Currency.class,getLocalKey(DatabaseHelper.CURRENCY_TABLE, jsonObjectEntity.getString("budget_currency_id")));
					tEntity.currencyId=getLocalKey(DatabaseHelper.CURRENCY_TABLE, jsonObjectEntity.getString("budget_currency_id"));
				} catch (Exception e) {
					Log.e(TAG,"Error parsing Budget.budget_currency_id ");				
					e.printStackTrace();
				}
			}
		}		
		if (jsonObjectEntity.has("amount2")) {			
			try {
				tEntity.amount=jsonObjectEntity.getInt("amount2");
			} catch (Exception e) {
				Log.e(TAG,"Error parsing Budget.amount");								
				e.printStackTrace();
			}
		}
		if (jsonObjectEntity.has("includeSubcategories")) {
			tEntity.includeSubcategories=jsonObjectEntity.getBoolean("includeSubcategories");
		} 		
		if (jsonObjectEntity.has("expanded")) {
			tEntity.expanded=jsonObjectEntity.getBoolean("expanded");
		} 
		if (jsonObjectEntity.has("includeCredit")) {
				tEntity.includeCredit=jsonObjectEntity.getBoolean("includeCredit");
		} 			
		if (jsonObjectEntity.has("startDate")) {
			try {
				tEntity.startDate = jsonObjectEntity.getLong("startDate")*1000;
			} catch (Exception e1) {					
				Log.e(TAG,"Error parsing Budget.startDate");
				e1.printStackTrace();
			}
		}
		if (jsonObjectEntity.has("endDate")) {			
			try {
				tEntity.endDate = jsonObjectEntity.getLong("endDate")*1000;
			} catch (Exception e1) {					
				Log.e(TAG,"Error parsing Budget.endDate");
				e1.printStackTrace();					
			}
		}

		if (jsonObjectEntity.has("recurNum")) {
			try {
				tEntity.recurNum=jsonObjectEntity.getInt("recurNum");
			} catch (JSONException e) {
				
				e.printStackTrace();
			}
		}
		if (jsonObjectEntity.has("isCurrent")) {
			try {
				tEntity.isCurrent=jsonObjectEntity.getBoolean("isCurrent");
			} catch (JSONException e) {
				
				e.printStackTrace();
			}
		}
		if (jsonObjectEntity.has("parentBudgetId")) {
			try {
				tEntity.parentBudgetId=getLocalKey(DatabaseHelper.BUDGET_TABLE, jsonObjectEntity.getString("parentBudgetId"));
			} catch (Exception e) {					
				Log.e(TAG,"Error parsing Budget.parentBudgetId ");				
				e.printStackTrace();
			}
		} 
		if (jsonObjectEntity.has("recur")) {
				try {
					tEntity.recur=jsonObjectEntity.getString("recur");
				} catch (JSONException e) {
					
					e.printStackTrace();
				}
		}			
		em.saveOrUpdate(tEntity);			
		return tEntity;	 						
	}	
	
	public static Object saveOrUpdateLocationFromJSON(long id,JSONObject jsonObjectEntity) {				
		MyLocation tEntity=em.get(MyLocation.class, id); 
		if (tEntity==null) {
			tEntity=new MyLocation();			
			tEntity.id=KEY_CREATE; 			
		}		
		try {
			tEntity.remoteKey=jsonObjectEntity.getString("key"); 			
			if (jsonObjectEntity.has("name")) {
				tEntity.name=jsonObjectEntity.getString("name"); 			
			} else {
				tEntity.name="---";
			}
			if (jsonObjectEntity.has("provider")) {
				tEntity.provider=jsonObjectEntity.getString("provider"); 
			}
			if (jsonObjectEntity.has("accuracy")) {
				try {
					tEntity.accuracy=Float.valueOf(jsonObjectEntity.getString("accuracy")); 	   
				} catch (Exception e) {
					Log.e(TAG,"Error parsing MyLocation.accuracy with : " + jsonObjectEntity.getString("accuracy"));				
				}
			}
			if (jsonObjectEntity.has("lon")) {
				tEntity.longitude=jsonObjectEntity.getDouble("lon");		
			}
			if (jsonObjectEntity.has("lat")) {
				tEntity.latitude=jsonObjectEntity.getDouble("lat");
			}	
			if (jsonObjectEntity.has("is_payee")) {
				if (jsonObjectEntity.getBoolean("is_payee")) {
					tEntity.isPayee=true;
				} else {
					tEntity.isPayee=false;				
				}
			}
			if (jsonObjectEntity.has("resolved_adress")) {
				tEntity.resolvedAddress=jsonObjectEntity.getString("resolved_adress");
			}
			if (jsonObjectEntity.has("dateOfEmission")) {
				try {
					tEntity.dateTime = jsonObjectEntity.getLong("dateOfEmission");
		 		} catch (Exception e1) {					
					Log.e(TAG,"Error parsing MyLocation.dateTime with : " + jsonObjectEntity.getString("dateOfEmission"));
				}
			}
			if (jsonObjectEntity.has("count")) {
				tEntity.count=jsonObjectEntity.getInt("count");						
			}
			if (jsonObjectEntity.has("dateTime")) {
				tEntity.dateTime=jsonObjectEntity.getLong("dateTime");						
			}
			em.saveOrUpdate(tEntity);					
			return tEntity;
		} catch (Exception e) {
			e.printStackTrace();
			return e;
		}		 		
	}
	
	
	public static Object saveOrUpdateCurrencyFromJSON(long id,JSONObject jsonObjectEntity) {
		Currency tEntity=em.get(Currency.class, id);
		if (tEntity==null) {
			tEntity = Currency.EMPTY;
			tEntity.id=KEY_CREATE; 
		}					
		try {	
			tEntity.remoteKey=jsonObjectEntity.getString("key"); 
			if (jsonObjectEntity.has("title")) {
				tEntity.title=jsonObjectEntity.getString("title");	
			}
			if (jsonObjectEntity.has("name")) {
				tEntity.name=jsonObjectEntity.getString("name");	
			}
			//deduplicate if server already have the currency
			String sql="select _id from " + DatabaseHelper.CURRENCY_TABLE + " where name= '" + tEntity.name + "';";
			Cursor c=db.rawQuery(sql, null);

			if (c.moveToFirst()) {
				tEntity.id=c.getLong(0); 	
				c.close();
			} else {
				c.close();	
			}			
			if (jsonObjectEntity.has("symbol")) {
				try {
					tEntity.symbol=jsonObjectEntity.getString("symbol");						
				} catch (Exception e) {
					Log.e(TAG,"Error pulling Currency.symbol");					
					e.printStackTrace();
				}
			}
			tEntity.isDefault=false;		
			if (jsonObjectEntity.has("isDefault")) {
				if (jsonObjectEntity.getBoolean("isDefault")) {
					tEntity.isDefault=jsonObjectEntity.getBoolean("isDefault");
				} 
			}
			if (jsonObjectEntity.has("decimals")) {
				try {
					tEntity.decimals=jsonObjectEntity.getInt("decimals");			
				}  catch (Exception e) {
					Log.e(TAG,"Error pulling Currency.decimals");					
					e.printStackTrace();
				}
			}
			try {
				tEntity.decimalSeparator=jsonObjectEntity.getString("decimalSeparator");
			} catch (Exception e) {
				Log.e(TAG,"Error pulling Currency.symbol");					
			}
			try {
				tEntity.groupSeparator=jsonObjectEntity.getString("groupSeparator");
			} catch (Exception e) {
				Log.e(TAG,"Error pulling Currency.symbol");					
			}
			em.saveOrUpdate(tEntity); 				
			return tEntity;
		} catch (Exception e) {			
			e.printStackTrace();
			return e;
		}		 						
	}
	
	public static Object saveOrUpdateAccountFromJSON(long id,JSONObject jsonObjectAccount) {

		Account tEntity=em.get(Account.class, id);

		if (tEntity==null) {
			tEntity = new Account();
			tEntity.id=KEY_CREATE; 									
		}		
  						
		try {			
			//title
			try {
			tEntity.title=jsonObjectAccount.getString("name");
			} catch (Exception e) {
				tEntity.title="---";
				Log.e(TAG,"Error parsing Account.name with");
			} 
			tEntity.remoteKey=jsonObjectAccount.getString("key");	
			//creation_date
			try {
				tEntity.creationDate =jsonObjectAccount.getLong("created_on");
			} catch (Exception e) {
				Log.e(TAG,"Error parsing Account.creationDate");
			}
			//last_transaction_date
			if (jsonObjectAccount.has("dateOfLastTransaction")) {
				try {
					tEntity.lastTransactionDate = jsonObjectAccount.getLong("dateOfLastTransaction");
				} catch (Exception e) {
					Log.e(TAG,"Error parsing Account.dateOfLastTransaction with : " + jsonObjectAccount.getString("dateOfLastTransaction"));
				}			
			}
			//currency, currency_name, currency_symbol
			Currency c=null;			
			Collection<Currency> currencies=CurrencyCache.getAllCurrencies();			
			if (jsonObjectAccount.has("currency_id")) {			
				try {
					c=em.load(Currency.class,getLocalKey(DatabaseHelper.CURRENCY_TABLE, jsonObjectAccount.getString("currency_id")));				
					tEntity.currency=c;
				} catch (Exception e) {
					Log.e(TAG,"unable to load currency for account "  + tEntity.title + " with " +  jsonObjectAccount.getString("currency_id"));
				}
			//server created account don't have a currency_id but all the properties to build one.
			} 
			if (tEntity.currency==null && jsonObjectAccount.has("currency")) {
				//try if provided currency is in user's currency
				for (Currency currency: currencies) {
					if (currency.name.equals(jsonObjectAccount.getString("currency"))) {
						tEntity.currency=currency;						
					}
				}
				//load from data server if any
				if (tEntity.currency==null) {
					c=Currency.EMPTY;	
					c.name=jsonObjectAccount.getString("currency");
					if (jsonObjectAccount.has("currency_name")) {					
						c.title=jsonObjectAccount.getString("currency_name");
					}
					if (jsonObjectAccount.has("currency_symbol")) {							
						c.symbol=jsonObjectAccount.getString("currency_symbol");
					}	
					tEntity.currency=c;
					c.id=-1; //db put!
					em.saveOrUpdate(c);							
				}
			} else if  (tEntity.currency==null) {
				//no currency provided use default
				for (Currency currency: currencies) {
					if (currency.isDefault) {
						tEntity.currency=currency;						
					}
				}				
				//still nothing : default set to empty
				if (tEntity.currency==null) {
					c=Currency.EMPTY;	
					c.isDefault=true;
					tEntity.currency=c;	
					//c.id=-1; //db put!
					//em.saveOrUpdate(c);							
				}
			}
			CurrencyCache.initialize(em);			
			//card_issuer
		 	if (jsonObjectAccount.has("card_issuer")) {
		 		tEntity.cardIssuer=jsonObjectAccount.getString("card_issuer");
		 	} 
		 	//issuer
		 	if (jsonObjectAccount.has(DatabaseHelper.AccountColumns.ISSUER)) {			 	
		 		tEntity.issuer=jsonObjectAccount.getString(DatabaseHelper.AccountColumns.ISSUER);				
		 	}
		 	//number
		 	if (jsonObjectAccount.has("code")) {
		 		tEntity.number=jsonObjectAccount.getString("code");
		 	}
            //limit
            if (jsonObjectAccount.has("total_limit")) {
                tEntity.limitAmount=jsonObjectAccount.getLong("total_limit");
            }
		 	//is_active
		 	if (jsonObjectAccount.has("closed")) {			 	
		 		if (jsonObjectAccount.getBoolean("closed")) {
		 			tEntity.isActive=false;
		 		} else {
		 			tEntity.isActive=true;
		 		}
		 	}
			//is_include_into_totals
		 	if (jsonObjectAccount.has(DatabaseHelper.AccountColumns.IS_INCLUDE_INTO_TOTALS)) {			 	
				if (jsonObjectAccount.getBoolean(DatabaseHelper.AccountColumns.IS_INCLUDE_INTO_TOTALS)) {
					tEntity.isIncludeIntoTotals=true;
				} else  {
					tEntity.isIncludeIntoTotals=false;
				}			
		 	}
			//closing_day
			try {
				tEntity.closingDay=jsonObjectAccount.getInt(DatabaseHelper.AccountColumns.CLOSING_DAY);
			}	catch (Exception e) {}
			//payment_day
			try {
				tEntity.paymentDay=jsonObjectAccount.getInt(DatabaseHelper.AccountColumns.PAYMENT_DAY);    
			}	catch (Exception e) {}
			//note
		 	if (jsonObjectAccount.has("description")) {	
		 		tEntity.note=jsonObjectAccount.getString("description");
		 	}
		 	//sort_order
		 	if (jsonObjectAccount.has(DatabaseHelper.AccountColumns.SORT_ORDER)) {
		 		tEntity.sortOrder=jsonObjectAccount.getInt(DatabaseHelper.AccountColumns.SORT_ORDER);
		 	}
		 	if (jsonObjectAccount.has(DatabaseHelper.AccountColumns.TYPE)) {
		 		tEntity.type=jsonObjectAccount.getString(DatabaseHelper.AccountColumns.TYPE);
		 	}
	 	
			em.saveOrUpdate(tEntity);						
			return tEntity;			
		} catch (Exception e1) {					
			e1.printStackTrace();
			return e1;				
		} 	
	}

	public static Object saveOrUpdateTransactionFromJSON(long id,JSONObject jsonObjectResponse) throws JSONException,Exception {
		Transaction tEntity=em.get(Transaction.class, id);		
		if (tEntity==null) {
			tEntity= new Transaction();
			tEntity.id=KEY_CREATE; 			
		}			
		//from_account_id,       			
		try {
			tEntity.fromAccountId=getLocalKey(DatabaseHelper.ACCOUNT_TABLE, jsonObjectResponse.getString("account"));
		} catch (Exception e1) {					
			Log.e("flowzr","Error parsing Transaction.fromAccount");
			return null; //REQUIRED				
		} 				
		if (jsonObjectResponse.has("dateTime")) {					
			tEntity.dateTime=jsonObjectResponse.getLong("dateTime")*1000;
		} else {
			return null; //REQUIRED
		}		
		//parent_tr		
		if (jsonObjectResponse.has("parent_tr")) {		
				long pid=getLocalKey(DatabaseHelper.TRANSACTION_TABLE, jsonObjectResponse.getString("parent_tr"));
				if (pid>KEY_CREATE) {
					tEntity.parentId=pid;
					Transaction parent_tr=em.load(Transaction.class, tEntity.parentId);
					if (parent_tr.categoryId!=Category.SPLIT_CATEGORY_ID) {
						parent_tr.categoryId=Category.SPLIT_CATEGORY_ID;
						em.saveOrUpdate(parent_tr);					
					}
				} else {
					try {
						String key=jsonObjectResponse.getString("parent_tr");
						requery(DatabaseHelper.TRANSACTION_TABLE,Transaction.class,key);
						long pid2=getLocalKey(DatabaseHelper.TRANSACTION_TABLE, jsonObjectResponse.getString("parent_tr"));
	    				tEntity.parentId=pid2;
	    				Transaction parent_tr=em.load(Transaction.class, tEntity.parentId);
	    				if (parent_tr.categoryId!=Category.SPLIT_CATEGORY_ID) {
	    						parent_tr.categoryId=Category.SPLIT_CATEGORY_ID;
	    						em.saveOrUpdate(parent_tr);					
	    				}
					} catch (Exception e) {
						//add to delete log ?
						e.printStackTrace();
						//throw new Exception("Got key " + jsonObjectResponse.getString("parent_tr") + " but couldn't find related parent tr");						
					}
					
				}
		}				
		//to_account_id,
		if (jsonObjectResponse.has("to_account")) {       			
			try {
				tEntity.toAccountId=getLocalKey(DatabaseHelper.ACCOUNT_TABLE, jsonObjectResponse.getString("to_account")); 						
			} catch (Exception e1) {											 						
				Log.e("flowzr","Error parsing Transaction.toAccount with : " + jsonObjectResponse.getString("to_account"));					
			} 					
		}
		if (jsonObjectResponse.has("key")) {
			tEntity.remoteKey=jsonObjectResponse.getString("key");					
		}   			       									
		if (jsonObjectResponse.has("amount")) {
				tEntity.fromAmount=jsonObjectResponse.getLong("amount");
		}
		if (jsonObjectResponse.has("to_amount")) {
			tEntity.toAmount=jsonObjectResponse.getLong("to_amount");
		}
		if (jsonObjectResponse.has("original_currency_id")) {
			try {					
				tEntity.originalCurrencyId=getLocalKey(DatabaseHelper.CURRENCY_TABLE, jsonObjectResponse.getString("original_currency_id"));
			} catch (Exception e) {
				Log.e("flowzr","Error parsing Transaction.original_currency_id with : " + jsonObjectResponse.getString("original_currency_id"));						
			}					
		}
		if (jsonObjectResponse.has("original_from_amount")) {
			tEntity.originalFromAmount=(long)jsonObjectResponse.getDouble("original_from_amount");       				
		}								
		
		if (jsonObjectResponse.has("description")) {
			tEntity.note=jsonObjectResponse.getString("description");				
		}						
		//category_id,       			
		if (jsonObjectResponse.has("cat")) {       		
			try {
				long l = getLocalKey(DatabaseHelper.CATEGORY_TABLE, jsonObjectResponse.getString("cat"));
				if (l>=KEY_CREATE) {
					tEntity.categoryId=l;	
				} //else do nothing ...			
			} catch (Exception e1) {					
				tEntity.categoryId=Category.NO_CATEGORY_ID;
				e1.printStackTrace();
				Log.e("flowzr","Error parsing Transaction.categoryId with : " + jsonObjectResponse.getString("cat"));			
			} 						
		} else {
			tEntity.categoryId=Category.NO_CATEGORY_ID;					
		}
		//project_id,
		if (jsonObjectResponse.has("project")) {
			try {
				tEntity.projectId=getLocalKey(DatabaseHelper.PROJECT_TABLE, jsonObjectResponse.getString("project"));
			} catch (Exception e1) {					
				Log.e("flowzr","Error parsing Transaction.ProjectId with : " + jsonObjectResponse.getString("project"));		
			} 					
		}
		//payee_id,
		if (jsonObjectResponse.has("payee_id")) {
			try {
				tEntity.payeeId=getLocalKey(DatabaseHelper.PAYEE_TABLE, jsonObjectResponse.getString("payee_id"));  
			} catch (Exception e1) {					
				Log.e("flowzr","Error parsing Transaction.PayeeId with : " + jsonObjectResponse.getString("payee_id"));					
			} 						     				
		}       			 
		//location_id
		if (jsonObjectResponse.has("location_id")) {
			try {
				long lid=getLocalKey(DatabaseHelper.LOCATIONS_TABLE, jsonObjectResponse.getString("location_id"));
				if (lid>0) {
					tEntity.locationId=lid;
				}
			} catch (Exception e1) {					
				Log.e("flowzr","Error parsing Transaction.location_id with : " + jsonObjectResponse.getString("location_id"));					
			} 						
		}
		//accuracy,provider,latitude,longitude
		if (jsonObjectResponse.has("provider")) {				
			tEntity.provider=jsonObjectResponse.getString("provider");
		}
		if (jsonObjectResponse.has("accuracy")) {	
			try {
				tEntity.accuracy=jsonObjectResponse.getLong("accuracy");
			} catch (Exception e) {
				Log.e("flowzr","Error getting accuracy value for transaction with:" + jsonObjectResponse.getString("accuracy"));
			}
		}
		if (jsonObjectResponse.has("lat") && jsonObjectResponse.has("lon")) {
			try {
				tEntity.latitude=jsonObjectResponse.getDouble("lat");
				tEntity.longitude=jsonObjectResponse.getDouble("lon");
			}	catch (Exception e) {
				Log.e("flowzr","Error getting geo_point value for transaction with:" + jsonObjectResponse.getString("lat") + " " + jsonObjectResponse.getDouble("lon"));
			}
		}				
		tEntity.status=TransactionStatus.UR;
		if (jsonObjectResponse.has("status")) {	
			if (jsonObjectResponse.getString("status").equals("UR")) {
				tEntity.status=TransactionStatus.UR;
			} else if (jsonObjectResponse.getString("status").equals("RC")) {
				tEntity.status=TransactionStatus.RC;
			} else if (jsonObjectResponse.getString("status").equals("CL")) {
				tEntity.status=TransactionStatus.CL;
			}	else if (jsonObjectResponse.getString("status").equals("PN")) {
				tEntity.status=TransactionStatus.PN;
			} else if (jsonObjectResponse.getString("status").equals("RS")) {
				tEntity.status=TransactionStatus.RS;
			} else {
				tEntity.status=TransactionStatus.UR;
			}		
		}
		//is_ccard_payment,
		if (jsonObjectResponse.has("is_ccard_payment")) {				
				tEntity.isCCardPayment=jsonObjectResponse.getInt("is_ccard_payment");
		}
		List<TransactionAttribute> attributes = null;
		if (jsonObjectResponse.has("transaction_attribute")) {
			attributes = new LinkedList<TransactionAttribute>();
	    	for (String pair : jsonObjectResponse.getString("transaction_attribute").split(";")) {
	    		String [] strArrAttr=pair.split("=");
	    		if (strArrAttr.length==2) {
	    			TransactionAttribute a = new TransactionAttribute();
	    			a.value=strArrAttr[1];
	    			a.attributeId=getLocalKey(DatabaseHelper.ATTRIBUTES_TABLE, strArrAttr[0]);
	    			a.transactionId=tEntity.id;
	    			attributes.add(a);
	    		} 		
	    	}
		}
		id=em.saveOrUpdate(tEntity);			
		if (attributes!=null) {
            dba.db().delete(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, DatabaseHelper.TransactionAttributeColumns.TRANSACTION_ID+"=?",
                    new String[]{String.valueOf(id)});
    		for (TransactionAttribute a : attributes) {
    			a.transactionId=id;
    			ContentValues values = a.toValues();
				dba.db().insert(DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE, null, values);
			}
		}	
		return tEntity;
	}

	public static void requery(String tableName,Class clazz,String key) throws ClientProtocolException, IOException, JSONException,Exception {
		Log.i(TAG,"Got key " + key + " but couldn't find related parent tr, requerying ...");
		String url=FLOWZR_API_URL + nsString + "/key/?tableName=" + DatabaseHelper.TRANSACTION_TABLE + "&key=" + key;   				
		StringBuilder builder = new StringBuilder();		
		DefaultHttpClient http_client2= new DefaultHttpClient();
		http_client2.setCookieStore(http_client.getCookieStore());
		HttpGet httpGet = new HttpGet(url);
    	HttpResponse httpResponse = http_client2.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        InputStream content = httpEntity.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        String line;
        while ((line = reader.readLine()) != null) {
          builder.append(line);
        }
		JSONObject o = new JSONObject(builder.toString()).getJSONArray(DatabaseHelper.TRANSACTION_TABLE).getJSONObject(0);
		saveEntityFromJson(o, tableName, clazz,1);
	}
	
    public static long getLocalKey(String tableName,String remoteKey) {
		Cursor c = db.query(tableName, new String[] { "_id" }, "remote_key = ?",
		          new String[]{ remoteKey }, null, null, null, null);		
		if (c.moveToFirst()) {
			long l = c.getLong(0);
			c.close();
			return l;
		} else {
			c.close();
			if (tableName.equals(DatabaseHelper.CATEGORY_TABLE)) {
				return -2;
			} else {
				return KEY_CREATE;
			}
		}
    }

    private static void pullUpdate() throws IOException, JSONException, Exception {
    	int i=0;
    	for (String tableName : tableNames) {      	   		
    		Log.i("flowzr",  context.getString(R.string.flowzr_sync_receiving) + " " + tableName );
   
    		if (tableName.equals(DatabaseHelper.TRANSACTION_TABLE)) {
				notifyUser(context.getString(R.string.flowzr_sync_receiving) + " " + tableName + ". " + context.getString(R.string.hint_run_background), (int)(Math.round(i*100/tableNames.length)));
			} else {
				notifyUser(context.getString(R.string.flowzr_sync_receiving) + " " + tableName, (int)(Math.round(i*100/tableNames.length)));
			}
      		if (!isCanceled) {
      			pullUpdate(tableName,clazzArray[i],last_sync_ts); 
      		}
        	i++;
        }
    } 
    
    public static <T> void pullUpdate(String tableName,Class<T> clazz,long  last_sync_ts) throws IOException, JSONException, Exception {
		
		if (tableName.equals(DatabaseHelper.TRANSACTION_TABLE)) {			
    		//pull all remote accounts, accounts by accounts
    		String sql="select remote_key from account";		
    		Cursor c=db.rawQuery(sql, null);        		
    		if (c.moveToFirst()) {
        		do {     			
        			String account_key=c.getString(c.getColumnIndex("remote_key"));
        			if (account_key!=null) {
        				String url=FLOWZR_API_URL + nsString + "/" + tableName + "/?last_sync_ts=" + last_sync_ts + "&account=" +  account_key;        			
        				getJSONFromUrl2(url,tableName,c.getString(c.getColumnIndex("remote_key")),clazz,last_sync_ts);        				        				
        			}       			
        		} while (c.moveToNext() && !isCanceled); //      		    		
    		}
    		c.close();
		} else {
			String url=FLOWZR_API_URL + nsString + "/" + tableName + "/?last_sync_ts=" + last_sync_ts; 			
			getJSONFromUrl(url,tableName,clazz,last_sync_ts);   
		}
    }    
    
    public static <T> int getJSONFromUrl(String url,String tableName, Class<T> clazz,long last_sync_ts) throws IOException, JSONException, Exception {
    	if (url==null) {
    		return 0;
    	}

        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Cookie","dev_appserver_login=test@example.com:False:185804764220139124118");
    	HttpResponse httpResponse = http_client.execute(httpGet);

        HttpEntity httpEntity = httpResponse.getEntity();

        // All the work is done for you here :)
        String jsonContent = EntityUtils.toString(httpEntity);

        // Create a Reader from String
        Reader stringReader = new StringReader(jsonContent);

        //is = httpEntity.getContent();

        reader = new JsonReader(stringReader);
        reader.setLenient(true);

        reader.beginObject();

        int i = readMessage(reader, tableName, clazz, last_sync_ts);
        httpEntity.consumeContent();
        return i;


    }

    public static <T> void getJSONFromUrl2(String url,String tableName,String account_key, Class<T> clazz,long last_sync_ts) throws IOException, JSONException, Exception {
		int i=MAX_PULL_SIZE;
        while (i!=0) {
        	i=getJSONFromUrl(url, tableName,clazz,last_sync_ts);
        }    
    }
    
    public static <T> int readMessage(JsonReader reader,String tableName,Class<T> clazz,long last_sync_ts) throws IOException, JSONException,Exception {    
		String n = null;
		int i=0;

   		while (reader.hasNext()) {
   			JsonToken peek=reader.peek();

   			String v = null;
   			if (peek==JsonToken.BEGIN_OBJECT) {
   				reader.beginObject();
   			} else if (peek==JsonToken.NAME) {
   				n=reader.nextName();
   			} else if (peek==JsonToken.BEGIN_ARRAY) {   				
   				if (n.equals(tableName)) {
   					i=readJsnArr(reader,tableName,clazz);
   					
   				} else {
   					if (n.equals("params")) {
   						reader.beginArray();
   						if (reader.hasNext()) {
   						reader.beginObject();
   						if (reader.hasNext()) {
	   						n=reader.nextName();
	   						v=reader.nextString();
   						}
   						reader.endObject();
   						}
   						reader.endArray();
   					} else {
   						reader.skipValue();
   					}
   				}
   			} else if (peek==JsonToken.END_OBJECT) {
   				reader.endObject();
   			} else if (peek==JsonToken.END_ARRAY) {
   				reader.endArray();
   			} else if (peek==JsonToken.STRING) {
                reader.skipValue();
            } else {
                reader.skipValue();
            }
   		}
   		return i;      
    }
    
	public static <T> int readJsnArr(JsonReader reader, String tableName, Class<T> clazz) throws IOException, JSONException,Exception {
		JSONObject o = new JSONObject();
		JsonToken peek = reader.peek();
		String n = null;
		reader.beginArray();
		int j=0;
		int i=0;		
		while (reader.hasNext()) {
			peek = reader.peek();
			if (reader.peek()==JsonToken.BEGIN_OBJECT) {
				reader.beginObject();
			} else if (reader.peek()==JsonToken.END_OBJECT) {
				reader.endObject();
			}
			o = new JSONObject();
			while (reader.hasNext()) {
				peek = reader.peek();
				if (peek == JsonToken.NAME) {
					n = reader.nextName();
				} else if (peek==JsonToken.BEGIN_OBJECT) {
					reader.beginObject();
				} else if (peek==JsonToken.END_OBJECT) {
					reader.endObject();
				} else if (peek == JsonToken.BOOLEAN) {
					try {
						o.put(n, reader.nextBoolean());
					} catch (JSONException e) {
						
						e.printStackTrace();
					}
				} else if (peek == JsonToken.STRING) {
					try {
						o.put(n, reader.nextString());

					} catch (JSONException e) {
						
						e.printStackTrace();
					}
				} else if (peek == JsonToken.NUMBER) {
					try {
						o.put(n, reader.nextDouble());

					} catch (JSONException e) {
						
						e.printStackTrace();
					}
				} 
			}
			reader.endObject();
			if (o.has("key")) {
				i=i+1;
				j=j+1;
				if (j%100==0) {
					j=2;
				}
				saveEntityFromJson(o, tableName, clazz,i);
				if (i%10==0) {
					//notifyUser(context.getString(R.string.flowzr_sync_receiving) + " " + tableName + ". " + context.getString(R.string.hint_run_background), (int)(Math.round(j)));
				}
			}
		}
		reader.endArray();
		return i;
	}
			
	public static <T> void saveEntityFromJson(JSONObject o, String tableName, Class<T> clazz, int i) throws JSONException,Exception {
		String remoteKey = o.getString("key");
		if (clazz==Transaction.class) {    							
			saveOrUpdateTransactionFromJSON(getLocalKey(tableName,remoteKey),o);
		} else if (clazz==Account.class) {
			saveOrUpdateAccountFromJSON(getLocalKey(tableName,remoteKey),o);						
		} else if (clazz==Currency.class) {
			saveOrUpdateCurrencyFromJSON(getLocalKey(tableName,remoteKey),o);					
		} else if (clazz==Budget.class) {
			saveOrUpdateBudgetFromJSON(getLocalKey(tableName,remoteKey),o);					
		} else if (clazz==MyLocation.class) {
			saveOrUpdateLocationFromJSON(getLocalKey(tableName,remoteKey),o);					
		} else if (tableName.equals("currency_exchange_rate"))  {
			saveOrUpdateCurrencyRateFromJSON(o);
		} else if (clazz==Category.class)  {
			saveOrUpdateCategoryFromJSON(getLocalKey(tableName,remoteKey),o);						
		}else if (clazz==Attribute.class)  {
			saveOrUpdateAttributeFromJSON(getLocalKey(tableName,remoteKey),o);						
		}  else  {
			saveOrUpdateEntityFromJSON(clazz,getLocalKey(tableName,remoteKey),o);										
		} 

	}
     
    public static void pullDelete(long last_sync_ts)  throws Exception {
    	String url=FLOWZR_API_URL + nsString + "/delete/?last_sync_ts=" + last_sync_ts ;
		HttpGet httpGet = new HttpGet(url);
    	HttpResponse httpResponse = http_client.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        is = httpEntity.getContent(); 
        reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
        reader.beginObject();
        readDelete(reader);
        httpEntity.consumeContent();
    }
	
    public static void readDelete(JsonReader reader) throws IOException {
    	reader.nextName();
    	reader.beginArray();
    	while (reader.hasNext()) {
    		reader.beginObject();
    		reader.nextName(); //tablename
    		String t=reader.nextString();
    	    reader.nextName(); //key    				
    		execDelete(t,reader.nextString());
    		reader.endObject();
    	}
    	reader.endArray();
    }
    
    public static void execDelete(String tableName,String remoteKey) {
    	long id=getLocalKey(tableName,remoteKey);  			
		
		if (id>0) {
			if (tableName.equals(DatabaseHelper.ACCOUNT_TABLE)) {    
				dba.deleteAccount(id);				
			} else if (tableName.equals(DatabaseHelper.TRANSACTION_TABLE)) {
				dba.deleteTransaction(id);								
			} else if (tableName.equals(DatabaseHelper.CURRENCY_TABLE)) {
				em.deleteCurrency(id);					
			} else if (tableName.equals(DatabaseHelper.BUDGET_TABLE)) {
				em.deleteBudget(id);
			} else if (tableName.equals(DatabaseHelper.LOCATIONS_TABLE)) {
				em.deleteLocation(id);
			} else if (tableName.equals(DatabaseHelper.PROJECT_TABLE)) {
				em.deleteProject(id);							
			} else if (tableName.equals(DatabaseHelper.PAYEE_TABLE)) {
				em.delete(Payee.class,id);								
			} else  if (tableName.equals(DatabaseHelper.CATEGORY_TABLE)) {
				dba.deleteCategory(id);
			}
		}
    }
   
   public static void pushAllBlobs() {
	   if (db==null) {
		   if (context==null) {
			   context=MainActivity.activity;
		   }
		   db=new DatabaseAdapter(context).db();
	   }
	   String sql="select attached_picture,datetime,remote_key,blob_key " +
	   		"from transactions " +
	   		"where attached_picture is not null " +
	   		"and blob_key is null"; 

	   Cursor cursorCursor=db.rawQuery(sql, null);
	   int i=0;
	   if (cursorCursor.moveToFirst()) {			
			do {
				i=i+10;
				notifyUser(cursorCursor.getString(0) + " >> Google Drive. " +  context.getString(R.string.hint_run_background),i);
				if (i==100) {
					i=10;
				}					
				saveFileToDrive(cursorCursor.getString(0),cursorCursor.getLong(1),cursorCursor.getString(2));				
			} while (cursorCursor.moveToNext());	
		}
		cursorCursor.close();
		notifyUser(context.getString(R.string.googledrive_upload) + " " +  context.getString(R.string.ok), 100);
   }
   
   
   public static void saveFileToDrive(String pictureFileName,long l,String remoteKey) {
	   java.io.File pictureFile = new java.io.File(PICTURES_DIR, pictureFileName);
	   Uri fileUri=Uri.fromFile(pictureFile);	   
	   new PictureDriveTask(context, http_client, fileUri, l, remoteKey).execute();	  
   }

   
   
    public static boolean checkSubscriptionFromWeb() {
    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	String registrationId = prefs.getString(FlowzrSyncOptions.PROPERTY_REG_ID, "");
    	if (registrationId=="") {
    		Log.i(TAG, "Registration not found.");
    	}

    	String url=FLOWZR_API_URL +  "?action=checkSubscription&regid=" + registrationId;

    	try {            
    		HttpGet httpGet = new HttpGet(url);
    		HttpResponse httpResponse = http_client.execute(httpGet);               
    		int code = httpResponse.getStatusLine().getStatusCode();
    		Log.i("flowzr","Subscription status code is : " + String.valueOf(code));
    		if (code==402) {
    			httpResponse.getEntity().consumeContent();
    			return false;
    		}
    		httpResponse.getEntity().consumeContent();
    	} catch (Exception e) {
    		e.printStackTrace();
    	} 
    	return true;
    }
}