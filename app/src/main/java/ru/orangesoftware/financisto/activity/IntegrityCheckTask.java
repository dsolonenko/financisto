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
import ru.orangesoftware.financisto.utils.IntegrityCheck;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 8/21/12 10:28 PM
 */
public class IntegrityCheckTask extends AsyncTask<IntegrityCheck, Void, IntegrityCheck.Result> {

    private final Activity activity;

    public IntegrityCheckTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected IntegrityCheck.Result doInBackground(IntegrityCheck... objects) {
        View textView = getResultView();
        if (textView != null) {
            return objects[0].check();
        }
        return IntegrityCheck.Result.OK;
    }

    @Override
    protected void onPostExecute(IntegrityCheck.Result result) {
        TextView textView = getResultView();
        if (textView != null) {
            if (result.level == IntegrityCheck.Level.OK) {
                textView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(View.VISIBLE);
                textView.setBackgroundColor(activity.getResources().getColor(colorForLevel(result.level)));
                textView.setText(activity.getString(R.string.integrity_error_message, result.message));
            }
        }
    }

    private int colorForLevel(IntegrityCheck.Level level) {
        switch (level) {
            case INFO:
                return R.color.holo_green_dark;
            case WARN:
                return R.color.holo_orange_dark;
            default:
                return R.color.holo_red_dark;
        }
    }

    private TextView getResultView() {
        return activity.findViewById(R.id.integrity_error);
    }

}
