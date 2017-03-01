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
package ru.orangesoftware.financisto.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.webkit.WebView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.Utils;

public class WebViewDialog {

	public static String checkVersionAndShowWhatsNewIfNeeded(Activity activity) {
		try {
			PackageInfo info = Utils.getPackageInfo(activity);
			SharedPreferences preferences = activity.getPreferences(0); 
			int newVersionCode = info.versionCode;
			int oldVersionCode = preferences.getInt("versionCode", -1);
			if (newVersionCode > oldVersionCode) {
				preferences.edit().putInt("versionCode", newVersionCode).commit();
				showWhatsNew(activity);
			}
			return "v. "+info.versionName;
		} catch(Exception ex) { 
			return "Free";
		}
	}
	
	public static void showWhatsNew(Context context) {
		showHTMDialog(context, "whatsnew.htm", R.string.whats_new);
	}

	private static void showHTMDialog(Context context, String fileName, int dialogTitleResId) {
		WebView webView = new WebView(context);
		webView.loadUrl("file:///android_asset/"+fileName);
		new AlertDialog.Builder(context)
			.setView(webView)
			.setTitle(dialogTitleResId)
			.setPositiveButton(R.string.ok, null)
			.show();		
	}

}
