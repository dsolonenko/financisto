/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Scanner;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Schema evolution helper.
 * Put sql files into assets/database directory as following:
 * - All create scripts into /create directory,
 * - All view scripts into /view directory,
 * - All alter scripts in /alter directory.
 * The algorithm is as follows:
 * On initial database create (when SQLiteOpenHelper.onCreate invoked),
 * the helper executes scripts in the following order: /create, /alter, /view.
 * On every database upgrade, the helper executes scripts from /alter which
 * haven't been yet executed, then all scripts from /view.
 *  
 * @author Denis Solonenko
 */
public class DatabaseSchemaEvolution extends SQLiteOpenHelper {

	private static final String TAG = "DatabaseSchemaEvolution";
	
	private static final String ALTERLOG = "alterlog";	
	
	private static final String DATABASE_PATH = "database";
	private static final String CREATE_PATH = DATABASE_PATH + "/create";
	private static final String VIEW_PATH = DATABASE_PATH + "/view";
	private static final String ALTER_PATH = DATABASE_PATH + "/alter";
	
	private final AssetManager assetManager;

	private boolean autoDropViews = false;
	
	public DatabaseSchemaEvolution(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
		super(context, name, factory, version);
		this.assetManager = context.getAssets();
	}
	
	public boolean isAutoDropViews() {
		return autoDropViews;
	}

	public void setAutoDropViews(boolean autoDropViews) {
		this.autoDropViews = autoDropViews;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			Log.i(TAG, "Creating ALTERLOG table");
			db.execSQL("create table "+ALTERLOG+" (script text not null, datetime long not null);");
			db.execSQL("create index "+ALTERLOG+"_script_idx on "+ALTERLOG+" (script);");
			Log.i(TAG, "Running create scripts...");
			runAllScripts(db, CREATE_PATH, false);
			Log.i(TAG, "Running alter scripts...");
			runAllScripts(db, ALTER_PATH, true);
			Log.i(TAG, "Running create view scripts...");
			runAllScripts(db, VIEW_PATH, false);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create database", ex);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		try {
			Log.i(TAG, "Upgrading database from version "+oldVersion+" to version "+newVersion+"...");
			Log.i(TAG, "Running alter scripts...");
			runAllScripts(db, ALTER_PATH, true);
			Log.i(TAG, "Running create view scripts...");
			runAllScripts(db, VIEW_PATH, false);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to upgrade database", ex);
		}		
	}
	
	public void runAlterScript(SQLiteDatabase db, String name) 
		throws IOException {
		runAlterScript(db, ALTER_PATH, name);
	}
	
	private void runAlterScript(SQLiteDatabase db, String path, String name) 
		throws IOException {
		String script = path + "/" + name;
		runScript(db, script);
	}

	private void runAllScripts(SQLiteDatabase db, String path, boolean checkAlterlog) 
		throws IOException {
		String[] scripts = sortScripts(assetManager.list(path));
		for (String scriptFile : scripts) {
			String script = path + "/" + scriptFile;
			if (checkAlterlog) {
				if (alreadyRun(db, script)) {
					Log.d("DatabaseSchema", "Skipping " + script);
					continue;
				}
			}
			if (autoDropViews && VIEW_PATH.equals(path)) {
				String viewName = getViewNameFromScriptName(scriptFile);
				db.execSQL("DROP VIEW IF EXISTS "+viewName);
			}
			Log.i("DatabaseSchema", "Running " + script);
			runScript(db, script);
			if (checkAlterlog) {
				saveScriptToAlterlog(db, script);
			}
		}
	}
	
	private void runScript(SQLiteDatabase db, String script) throws IOException {
		String[] content = readFile(script).split(";");
		for (String s : content) {
			String sql = s.trim();
			if (sql.length() > 1) {
				try {
					db.execSQL(sql);
				} catch (SQLiteException ex) {
					Log.e("DatabaseSchema", "Unable to run sql: "+sql, ex);
					throw ex;
				}
			}
		}
	}

	/**
	 * Sorts array of scripts' names
	 * @param scripts scripts list
	 * @return scripts array sorted with natural order
	 */
	protected String[] sortScripts(String[] scripts) {
		Arrays.sort(scripts);
		return scripts;
	}
		
	protected String getViewNameFromScriptName(String scriptFileName) {
		int i = scriptFileName.indexOf('.');
		return i == -1 ? scriptFileName : scriptFileName.substring(0, i);
	}
	
	private static final String[] projection = {"1"};

	private boolean alreadyRun(SQLiteDatabase db, String script) {
		Cursor c = db.query(ALTERLOG, projection, "script=?", new String[]{script}, null, null, null);
		try {
			return c.moveToFirst();
		} finally {
			c.close();
		}
	}

	private void saveScriptToAlterlog(SQLiteDatabase db, String script) {
		ContentValues values = new ContentValues();
		values.put("script", script);
		values.put("datetime", System.currentTimeMillis());
		db.insert(ALTERLOG, null, values);
	}

	private String readFile(String scriptFile) throws IOException {
		StringBuilder sb = new StringBuilder();
		InputStream is = assetManager.open(scriptFile);
		Scanner scanner = new Scanner(is);
		try {
			while (scanner.hasNextLine()) {
				sb.append(scanner.nextLine().trim()).append(" ");
			}
		} finally {
			scanner.close();
			is.close();
		}
		return sb.toString().trim();
	}

}
