/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;

import static ru.orangesoftware.financisto.utils.AndroidUtils.isInstalledOnSdCard;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 10/12/12 12:01 AM
 */
public class InstalledOnSdCardCheckTask extends AsyncTask<Void, Void, Boolean> {

    private final Activity activity;

    public InstalledOnSdCardCheckTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Void... objects) {
        View textView = getResultView();
        return textView != null && isInstalledOnSdCard(activity);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        TextView textView = getResultView();
        if (textView != null) {
            textView.setText(R.string.installed_on_sd_card_warning);
            textView.setVisibility(result != null && result ? View.VISIBLE : View.GONE);
        }
    }

    private TextView getResultView() {
        return (TextView) activity.findViewById(R.id.integrity_error);
    }

}
