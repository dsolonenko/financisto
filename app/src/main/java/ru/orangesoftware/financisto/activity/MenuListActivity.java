/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 * Abdsandryk Souza - implementing 2D chart reports
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Status;
import java.util.List;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.SummaryEntityListAdapter;
import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.drive.DoDriveBackup;
import ru.orangesoftware.financisto.export.drive.DoDriveListFiles;
import ru.orangesoftware.financisto.export.drive.DoDriveRestore;
import ru.orangesoftware.financisto.export.drive.DriveBackupError;
import ru.orangesoftware.financisto.export.drive.DriveBackupFailure;
import ru.orangesoftware.financisto.export.drive.DriveBackupSuccess;
import ru.orangesoftware.financisto.export.drive.DriveConnectionFailed;
import ru.orangesoftware.financisto.export.drive.DriveFileInfo;
import ru.orangesoftware.financisto.export.drive.DriveFileList;
import ru.orangesoftware.financisto.export.drive.DriveRestoreSuccess;
import ru.orangesoftware.financisto.export.drive.DropboxFileList;
import ru.orangesoftware.financisto.export.dropbox.DropboxBackupTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxListFilesTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxRestoreTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import ru.orangesoftware.financisto.utils.PinProtection;

import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

@EActivity(R.layout.activity_menu_list)
public class MenuListActivity extends ListActivity {

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;

    @Bean
    GreenRobotBus bus;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @AfterViews
    protected void init() {
        setListAdapter(new SummaryEntityListAdapter(this, MenuListItem.values()));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        MenuListItem.values()[position].call(this);
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CSV_EXPORT)
    public void onCsvExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvExportOptions options = CsvExportOptions.fromIntent(data);
            MenuListItem.doCsvExport(this, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_QIF_EXPORT)
    public void onQifExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifExportOptions options = QifExportOptions.fromIntent(data);
            MenuListItem.doQifExport(this, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CSV_IMPORT)
    public void onCsvImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvImportOptions options = CsvImportOptions.fromIntent(data);
            MenuListItem.doCsvImport(this, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_QIF_IMPORT)
    public void onQifImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifImportOptions options = QifImportOptions.fromIntent(data);
            MenuListItem.doQifImport(this, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CHANGE_PREFERENCES)
    public void onChangePreferences() {
        scheduleNextAutoBackup(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
        bus.unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
        bus.register(this);
    }

    ProgressDialog progressDialog;

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    // google drive

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doGoogleDriveBackup(StartDriveBackup e) {
        progressDialog = ProgressDialog.show(this, null, getString(R.string.backup_database_gdocs_inprogress), true);
        bus.post(new DoDriveBackup());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doGoogleDriveRestore(StartDriveRestore e) {
        progressDialog = ProgressDialog.show(this, null, getString(R.string.google_drive_loading_files), true);
        bus.post(new DoDriveListFiles());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveList(DriveFileList event) {
        dismissProgressDialog();
        final List<DriveFileInfo> files = event.files;
        final String[] fileNames = getFileNames(files);
        final MenuListActivity context = this;
        final DriveFileInfo[] selectedDriveFile = new DriveFileInfo[1];
        new AlertDialog.Builder(context)
                .setTitle(R.string.restore_database_online_google_drive)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    if (selectedDriveFile[0] != null) {
                        progressDialog = ProgressDialog.show(context, null, getString(R.string.google_drive_restore_in_progress), true);
                        bus.post(new DoDriveRestore(selectedDriveFile[0]));
                    }
                })
                .setSingleChoiceItems(fileNames, -1, (dialog, which) -> {
                    if (which >= 0 && which < files.size()) {
                        selectedDriveFile[0] = files.get(which);
                    }
                })
                .show();
    }

    private String[] getFileNames(List<DriveFileInfo> files) {
        String[] names = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            names[i] = files.get(i).title;
        }
        return names;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveConnectionFailed(DriveConnectionFailed event) {
        dismissProgressDialog();
        ConnectionResult connectionResult = event.connectionResult;
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(new DriveBackupError(e.getMessage()));
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveBackupFailed(DriveBackupFailure event) {
        dismissProgressDialog();
        Status status = event.status;
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(new DriveBackupError(e.getMessage()));
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(status.getStatusCode(), this, 0).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveBackupSuccess(DriveBackupSuccess event) {
        dismissProgressDialog();
        Toast.makeText(this, getString(R.string.google_drive_backup_success, event.fileName), Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveRestoreSuccess(DriveRestoreSuccess event) {
        dismissProgressDialog();
        Toast.makeText(this, R.string.restore_database_success, Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveBackupError(DriveBackupError event) {
        dismissProgressDialog();
        Toast.makeText(this, getString(R.string.google_drive_connection_failed, event.message), Toast.LENGTH_LONG).show();
    }

    @OnActivityResult(RESOLVE_CONNECTION_REQUEST_CODE)
    public void onConnectionRequest(int resultCode) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.google_drive_connection_resolved, Toast.LENGTH_LONG).show();
        }
    }

    // dropbox
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doImportFromDropbox(DropboxFileList event) {
        final String[] backupFiles = event.files;
        if (backupFiles != null) {
            final String[] selectedDropboxFile = new String[1];
            new AlertDialog.Builder(this)
                    .setTitle(R.string.restore_database_online_dropbox)
                    .setPositiveButton(R.string.restore, (dialog, which) -> {
                        if (selectedDropboxFile[0] != null) {
                            ProgressDialog d = ProgressDialog.show(MenuListActivity.this, null, getString(R.string.restore_database_inprogress_dropbox), true);
                            new DropboxRestoreTask(MenuListActivity.this, d, selectedDropboxFile[0]).execute();
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, (dialog, which) -> {
                        if (which >= 0 && which < backupFiles.length) {
                            selectedDropboxFile[0] = backupFiles[which];
                        }
                    })
                    .show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxBackup(StartDropboxBackup e) {
        ProgressDialog d = ProgressDialog.show(this, null, this.getString(R.string.backup_database_dropbox_inprogress), true);
        new DropboxBackupTask(this, d).execute();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxRestore(StartDropboxRestore e) {
        ProgressDialog d = ProgressDialog.show(this, null, this.getString(R.string.dropbox_loading_files), true);
        new DropboxListFilesTask(this, d).execute();
    }

    public static class StartDropboxBackup {
    }

    public static class StartDropboxRestore {
    }

    public static class StartDriveBackup {
    }

    public static class StartDriveRestore {
    }

}
