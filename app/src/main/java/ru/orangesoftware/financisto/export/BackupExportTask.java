package ru.orangesoftware.financisto.export;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

public class BackupExportTask extends ImportExportAsyncTask {

    public final boolean uploadOnline;

    public volatile String backupFileName;
	
	public BackupExportTask(Activity context, ProgressDialog dialog, boolean uploadOnline) {
		super(context, dialog);
        this.uploadOnline = uploadOnline;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
		DatabaseExport export = new DatabaseExport(context, db.db(), true);
        backupFileName = export.export();
        if (uploadOnline) {
            doUploadToDropbox(context, backupFileName);
			doUploadToGoogleDrive(context, backupFileName);
        }
        return backupFileName;
	}

    @Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}