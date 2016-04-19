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

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.Spinner;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.CurrencyExportPreferences;

public class CsvExportActivity extends AbstractExportActivity {
	
	public static final String CSV_EXPORT_FIELD_SEPARATOR = "CSV_EXPORT_FIELD_SEPARATOR";
	public static final String CSV_EXPORT_INCLUDE_HEADER = "CSV_EXPORT_INCLUDE_HEADER";
    public static final String CSV_EXPORT_SPLITS = "CSV_EXPORT_SPLITS";
    public static final String CSV_EXPORT_UPLOAD_TO_DROPBOX = "CSV_EXPORT_UPLOAD_TO_DROPBOX";

    private final CurrencyExportPreferences currencyPreferences = new CurrencyExportPreferences("csv");

    private Spinner fieldSeparators;
    private CheckBox includeHeader;
    private CheckBox exportSplits;
    private CheckBox uploadToDropbox;

    public CsvExportActivity() {
        super(R.layout.csv_export);
    }

    @Override
    protected void internalOnCreate() {
        fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
        includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
        exportSplits = new CheckBox(this); //(CheckBox)findViewById(R.id.checkboxExportSplits);
        includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
        uploadToDropbox = (CheckBox)findViewById(R.id.checkboxUploadToDropbox);
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        currencyPreferences.updateIntentFromUI(this, data);
        data.putExtra(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItem().toString().charAt(1));
        data.putExtra(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
        data.putExtra(CSV_EXPORT_SPLITS, exportSplits.isChecked());
        data.putExtra(CSV_EXPORT_UPLOAD_TO_DROPBOX, uploadToDropbox.isChecked());
    }

	protected void savePreferences() {
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        currencyPreferences.savePreferences(this, editor);
		editor.putInt(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItemPosition());
		editor.putBoolean(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
        editor.putBoolean(CSV_EXPORT_SPLITS, exportSplits.isChecked());
        editor.putBoolean(CSV_EXPORT_UPLOAD_TO_DROPBOX, uploadToDropbox.isChecked());
		editor.commit();
	}

    protected void restorePreferences() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        currencyPreferences.restorePreferences(this, prefs);
        fieldSeparators.setSelection(prefs.getInt(CSV_EXPORT_FIELD_SEPARATOR, 0));
		includeHeader.setChecked(prefs.getBoolean(CSV_EXPORT_INCLUDE_HEADER, true));
        exportSplits.setChecked(prefs.getBoolean(CSV_EXPORT_SPLITS, false));
        uploadToDropbox.setChecked(prefs.getBoolean(CSV_EXPORT_UPLOAD_TO_DROPBOX, false));
	}

}
