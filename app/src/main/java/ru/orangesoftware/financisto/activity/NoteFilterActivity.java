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
package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class NoteFilterActivity extends Activity {

    public static final String NOTE_CONTAINING = "note_containing";

    private EditText edNoteContaining;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.note_filter);

        Intent intent = getIntent();

        edNoteContaining = findViewById(R.id.edNoteContaining);

        Button bOk = findViewById(R.id.bOK);
        bOk.setOnClickListener(v -> {
            Intent data = new Intent();

            String r = edNoteContaining.getText().toString();
            data.putExtra(NOTE_CONTAINING, "%" + r.replace(" ", "%") + "%");
            setResult(RESULT_OK, data);
            finish();
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        Button bNoFilter = findViewById(R.id.bNoFilter);
        bNoFilter.setOnClickListener(v -> {
            setResult(RESULT_FIRST_USER);
            finish();
        });

        if (intent == null) {
            edNoteContaining.setText("");
        } else {
            WhereFilter filter = WhereFilter.fromIntent(intent);
            Criteria c = filter.get(BlotterFilter.NOTE);
            if (c != null) {
                String v = c.getStringValue();
                edNoteContaining.setText(v.substring(1, v.length() - 1).replace("%", " "));
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        final Dialog d = new Dialog(this);
        d.setCancelable(true);
        d.setTitle(R.string.note_text_containing);
        d.setContentView(R.layout.filter_period_select);
        Button bOk = d.findViewById(R.id.bOK);
        bOk.setOnClickListener(v -> d.dismiss());
        Button bCancel = d.findViewById(R.id.bCancel);
        bCancel.setOnClickListener(v -> d.cancel());
        return d;
    }
}
