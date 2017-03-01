/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.flowzr;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.export.docs.GoogleDriveClient;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

public class PictureDriveTask extends AsyncTask<String, String, Object> {

    private String rootFolderId;
	private Uri fileUri;
	private long trDate;
	private String remote_key;
	private Context context;
	private DefaultHttpClient http_client;
	private DatabaseAdapter dba;

	public PictureDriveTask(Context context, DefaultHttpClient http_client,Uri _fileUri,long l,String _remote_key) {
			this.http_client=http_client; //-(FlowzrSyncEngine) context)
			this.context=context;
		    this.fileUri = _fileUri;
		    this.trDate = l;
		    this.remote_key=_remote_key;
	        dba = new DatabaseAdapter(context);
			dba.open();		    
	}


    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        try {
            String folder = MyPreferences.getGoogleDriveFolder(context);
            // check the backup folder registered on preferences
            if (folder == null || folder.equals("")) {
                throw new ImportExportException(R.string.gdocs_folder_not_configured);
            }
            String googleDriveAccount = MyPreferences.getFlowzrAccount(context);
            Drive drive = GoogleDriveClient.create(context,googleDriveAccount);
            runUpload(drive);
            return true;
        } catch (ImportExportException e) {
            throw e;
        } catch (GoogleAuthException e) {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        } catch (IOException e) {
        	e.printStackTrace();        	
            throw new ImportExportException(R.string.gdocs_io_error);
        } catch (Exception e) {
        	e.printStackTrace();
            throw new ImportExportException(R.string.gdocs_service_error, e);
        }
    }

    protected boolean runUpload (Drive driveService) throws IOException {
		String targetFolderId=null;


			String ROOT_FOLDER=MyPreferences.getGoogleDriveFolder(context);
			// ensure to have the app root folder in drive ...
			if (rootFolderId==null) {
				//search root folder ...
				FileList folders=driveService.files().list().setQ("mimeType='application/vnd.google-apps.folder'").execute();
				for(File fl: folders.getItems()){
					if (fl.getTitle().equals(ROOT_FOLDER)) {
						rootFolderId=fl.getId();
					}
				}
				//if not found create it
				if (rootFolderId==null) {
					File body = new File();
					body.setTitle(ROOT_FOLDER);
					body.setMimeType("application/vnd.google-apps.folder");
					File file = driveService.files().insert(body).execute();
					rootFolderId=file.getId();
				}
			} 
			//search for the target folder (depending of the date)    		      
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(trDate));	    		      
			int month=cal.get(Calendar.MONTH) + 1;
			String targetFolder=String.valueOf(cal.get(Calendar.YEAR)) + "-"  + (month<10?("0"+month):(month));

			FileList subfolders=driveService.files().list().setQ("mimeType='application/vnd.google-apps.folder' and '" + rootFolderId + "' in parents").execute();
			for(File fl: subfolders.getItems()){
				if (fl.getTitle().equals(targetFolder)) {
					targetFolderId=fl.getId();
				}
			}
			//create the target folder if not exist
			if (targetFolderId==null) {
				//create folder
				File body = new File();
				body.setTitle(targetFolder);
				ArrayList<ParentReference> pList=new ArrayList<ParentReference>();
				pList.add(new ParentReference().setId(rootFolderId)) ;   		        	  
				body.setParents(pList);
				body.setMimeType("application/vnd.google-apps.folder");
				File file = driveService.files().insert(body).execute();
				targetFolder=file.getId();
			}
			// File's binary content
			java.io.File fileContent = new java.io.File(fileUri.getPath());
			InputStreamContent mediaContent = new InputStreamContent("image/jpeg", new   BufferedInputStream(
					new FileInputStream(fileContent)));   		        	
			mediaContent.setLength(fileContent.length());    		          
			// File's metadata.
			File body = new File();
			body.setTitle(fileContent.getName());
			body.setMimeType("image/jpeg");    
			body.setFileSize(fileContent.length());
			ArrayList<ParentReference> pList2=new ArrayList<ParentReference>();
			pList2.add(new ParentReference().setId(targetFolderId)) ;   		        		          
			body.setParents(pList2);
			File file = driveService.files().insert(body, mediaContent).execute();   		              		           		          
 		

		   Thread thread=  new Thread(){
	       @Override
	       public void run(){
	           try {
		               synchronized(this){
		                   wait(3000);
		               }
		           }
		           catch(InterruptedException ex){                    
		           }        
		       }
		   };
		   thread.start(); 
		
			String uploadedId=null;
			FileList files;
			
				files = driveService.files().list().setQ("mimeType='image/jpeg' and '" + targetFolderId + "' in parents").execute();
				String file_url="";
				String thumbnail_url="";
				for(File fl: files.getItems()){
					if (fl.getTitle().equals(fileUri.getLastPathSegment())) {
						uploadedId=fl.getId();	    
						try {
							file_url=fl.getAlternateLink();
							thumbnail_url=fl.getIconLink();
						} catch (Exception e) {
							file_url="https://drive.google.com/#folders/" + targetFolderId +"/";
						}
					}
				}  
				if (!uploadedId.equals("null")) {
					String sql="update transactions set blob_key='" + uploadedId + "' where remote_key='" + remote_key+"'";
					dba.db().execSQL(sql);	   				
					sql="select from_account_id,attached_picture from " + DatabaseHelper.TRANSACTION_TABLE +  " where remote_key='" + remote_key+"'";	
					Cursor c=dba.db().rawQuery(sql, null);	   			
					if (c.moveToFirst()) {	   					   				
						String account_key=FlowzrSyncEngine.getRemoteKey(DatabaseHelper.ACCOUNT_TABLE, String.valueOf(c.getLong(0)));
						String file_type="image/jpeg";
						String file_name=c.getString(1);
						if (file_url==null) {
							file_url="";
						}
						if (thumbnail_url==null) {
							thumbnail_url="";
						}	   					
						if (http_client!=null) {
							//make html link beetwen Flowzr.com & Drive
							String url=FlowzrSyncEngine.FLOWZR_API_URL +"/clear/blob/?url=" +  URLEncoder.encode(file_url, "UTF-8") + "&thumbnail_url=" + URLEncoder.encode(thumbnail_url, "UTF-8") + "&account="+account_key+"&crebit="+ remote_key + "&name="+ file_name + "&blob_key=" + uploadedId + "type=" + file_type;
					    	try {         
					    		HttpGet httpGet = new HttpGet(url);
					    		http_client.execute(httpGet);
					    		Log.i("flowzr","linked to :" + file_url);
					    	} catch (Exception e) {
					    		e.printStackTrace();
					    	} 
						}
					}
				}
   	
			return true;
    }
    
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }


	@Override
	protected Object doInBackground(String... arg0) {
		DatabaseAdapter db = new DatabaseAdapter(context);
		db.open();
		try {
			return work(context, db);
		} catch(Exception ex){
			Log.e("Financisto", "Unable to do import/export", ex);
			return ex;
		} finally {
			db.close();
		}
	}

}