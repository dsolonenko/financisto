package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ListAdapter;
import android.widget.Toast;
import java.io.File;
import ru.orangesoftware.financisto.R;
import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermission;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.bus.GreenRobotBus_;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.BackupExportTask;
import ru.orangesoftware.financisto.export.BackupImportTask;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvExportTask;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifExportTask;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportTask;
import ru.orangesoftware.financisto.utils.EntityEnum;
import ru.orangesoftware.financisto.utils.EnumUtils;
import static ru.orangesoftware.financisto.utils.EnumUtils.showPickOneDialog;
import ru.orangesoftware.financisto.utils.ExecutableEntityEnum;
import ru.orangesoftware.financisto.utils.IntegrityFix;

public enum MenuListItem {

    MENU_PREFERENCES(R.string.preferences, android.R.drawable.ic_menu_preferences) {
        @Override
        public void call(Activity activity) {
            activity.startActivityForResult(new Intent(activity, PreferencesActivity.class), ACTIVITY_CHANGE_PREFERENCES);
        }
    },
    MENU_ENTITIES(R.string.entities, R.drawable.menu_entities) {
        @Override
        public void call(final Activity activity) {
            final MenuEntities[] entities = MenuEntities.values();
            ListAdapter adapter = EnumUtils.createEntityEnumAdapter(activity, entities);
            final AlertDialog d = new AlertDialog.Builder(activity)
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            MenuEntities e = entities[which];
                            activity.startActivity(new Intent(activity, e.getActivityClass()));
                        }
                    })
                    .create();
            d.setTitle(R.string.entities);
            d.show();
        }
    },
    MENU_SCHEDULED_TRANSACTIONS(R.string.scheduled_transactions, R.drawable.ic_menu_today) {
        @Override
        public void call(Activity activity) {
            activity.startActivity(new Intent(activity, ScheduledListActivity.class));
        }
    },
    MENU_BACKUP(R.string.backup_database, R.drawable.ic_menu_upload) {
        @Override
        public void call(Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            ProgressDialog d = ProgressDialog.show(activity, null, activity.getString(R.string.backup_database_inprogress), true);
            new BackupExportTask(activity, d, true).execute();
        }
    },
    MENU_RESTORE(R.string.restore_database, 0) {
        @Override
        public void call(final Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            final String[] backupFiles = Backup.listBackups(activity);
            final String[] selectedBackupFile = new String[1];
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.restore_database)
                    .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (selectedBackupFile[0] != null) {
                                ProgressDialog d = ProgressDialog.show(activity, null, activity.getString(R.string.restore_database_inprogress), true);
                                new BackupImportTask(activity, d).execute(selectedBackupFile);
                            }
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (backupFiles != null && which >= 0 && which < backupFiles.length) {
                                selectedBackupFile[0] = backupFiles[which];
                            }
                        }
                    })
                    .show();
        }
    },
    GOOGLE_DRIVE_BACKUP(R.string.backup_database_online_google_drive, R.drawable.ic_menu_back) {
        @Override
        public void call(Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            GreenRobotBus_.getInstance_(activity).post(new MenuListActivity.StartDriveBackup());
        }
    },
    GOOGLE_DRIVE_RESTORE(R.string.restore_database_online_google_drive, R.drawable.ic_menu_forward) {
        @Override
        public void call(Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            GreenRobotBus_.getInstance_(activity).post(new MenuListActivity.StartDriveRestore());
        }
    },
    DROPBOX_BACKUP(R.string.backup_database_online_dropbox, R.drawable.ic_menu_back) {
        @Override
        public void call(Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            GreenRobotBus_.getInstance_(activity).post(new MenuListActivity.StartDropboxBackup());
        }
    },
    DROPBOX_RESTORE(R.string.restore_database_online_dropbox, R.drawable.ic_menu_forward) {
        @Override
        public void call(Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            GreenRobotBus_.getInstance_(activity).post(new MenuListActivity.StartDropboxRestore());
        }
    },
    MENU_BACKUP_TO(R.string.backup_database_to, 0) {
        @Override
        public void call(final Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            ProgressDialog d = ProgressDialog.show(activity, null, activity.getString(R.string.backup_database_inprogress), true);
            final BackupExportTask t = new BackupExportTask(activity, d, false);
            t.setShowResultMessage(false);
            t.setListener(new ImportExportAsyncTaskListener() {
                @Override
                public void onCompleted(Object result) {
                    String backupFileName = t.backupFileName;
                    File file = Export.getBackupFile(activity, backupFileName);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    intent.setType("text/plain");
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.backup_database_to_title)));
                }
            });
            t.execute((String[]) null);
        }
    },
    MENU_IMPORT_EXPORT(R.string.import_export, 0) {
        @Override
        public void call(Activity activity) {
            if (isRequestingPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }
            showPickOneDialog(activity, R.string.import_export, ImportExportEntities.values(), activity);
        }
    },
    MENU_MASS_OP(R.string.mass_operations, R.drawable.ic_menu_agenda) {
        @Override
        public void call(Activity activity) {
            activity.startActivity(new Intent(activity, MassOpActivity.class));
        }
    },
    MENU_PLANNER(R.string.planner, 0) {
        @Override
        public void call(Activity activity) {
            activity.startActivity(new Intent(activity, PlannerActivity.class));
        }
    },
    MENU_INTEGRITY_FIX(R.string.integrity_fix, 0) {
        @Override
        public void call(Activity activity) {
            new IntegrityFixTask(activity).execute();
        }
    },
    MENU_DONATE(R.string.donate, 0) {
        @Override
        public void call(Activity activity) {
            try {
                Intent browserIntent = new Intent("android.intent.action.VIEW",
                        Uri.parse("market://search?q=pname:ru.orangesoftware.financisto.support"));
                activity.startActivity(browserIntent);
            } catch (Exception ex) {
                //eventually market is not available
                Toast.makeText(activity, R.string.donate_error, Toast.LENGTH_LONG).show();
            }
        }

    },
    MENU_ABOUT(R.string.about, 0) {
        @Override
        public void call(Activity activity) {
            activity.startActivity(new Intent(activity, AboutActivity.class));
        }
    };

    public final int textResId;
    public final int iconResId;

    MenuListItem(int textResId, int iconResId) {
        this.textResId = textResId;
        this.iconResId = iconResId;
    }

    public static final int ACTIVITY_CSV_EXPORT = 2;
    public static final int ACTIVITY_QIF_EXPORT = 3;
    public static final int ACTIVITY_CSV_IMPORT = 4;
    public static final int ACTIVITY_QIF_IMPORT = 5;
    public static final int ACTIVITY_CHANGE_PREFERENCES = 6;

    public abstract void call(Activity activity);

    private enum MenuEntities implements EntityEnum {

        CURRENCIES(R.string.currencies, R.drawable.menu_entities_currencies, CurrencyListActivity.class),
        EXCHANGE_RATES(R.string.exchange_rates, R.drawable.menu_entities_exchange_rates, ExchangeRatesListActivity.class),
        CATEGORIES(R.string.categories, R.drawable.menu_entities_categories, CategoryListActivity2.class),
        SMS_TEMPLATES(R.string.sms_templates, R.drawable.menu_entities, SmsTemplateListActivity.class),
        PAYEES(R.string.payees, R.drawable.menu_entities_payees, PayeeListActivity.class),
        PROJECTS(R.string.projects, R.drawable.menu_entities_projects, ProjectListActivity.class),
        LOCATIONS(R.string.locations, R.drawable.menu_entities_locations, LocationsListActivity.class);

        private final int titleId;
        private final int iconId;
        private final Class<?> actitivyClass;

        MenuEntities(int titleId, int iconId, Class<?> activityClass) {
            this.titleId = titleId;
            this.iconId = iconId;
            this.actitivyClass = activityClass;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

        public Class<?> getActivityClass() {
            return actitivyClass;
        }

    }

    private enum ImportExportEntities implements ExecutableEntityEnum<Activity> {

        CSV_EXPORT(R.string.csv_export, R.drawable.ic_menu_back) {
            @Override
            public void execute(Activity mainActivity) {
                Intent intent = new Intent(mainActivity, CsvExportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
            }
        },
        CSV_IMPORT(R.string.csv_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(Activity mainActivity) {
                Intent intent = new Intent(mainActivity, CsvImportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_CSV_IMPORT);
            }
        },
        QIF_EXPORT(R.string.qif_export, R.drawable.ic_menu_back) {
            @Override
            public void execute(Activity mainActivity) {
                Intent intent = new Intent(mainActivity, QifExportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_QIF_EXPORT);
            }
        },
        QIF_IMPORT(R.string.qif_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(Activity mainActivity) {
                Intent intent = new Intent(mainActivity, QifImportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_QIF_IMPORT);
            }
        };

        private final int titleId;
        private final int iconId;

        ImportExportEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

    public static void doCsvExport(Activity activity, CsvExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.csv_export_inprogress), true);
        new CsvExportTask(activity, progressDialog, options).execute();
    }

    public static void doCsvImport(Activity activity, CsvImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.csv_import_inprogress), true);
        new CsvImportTask(activity, progressDialog, options).execute();
    }

    public static void doQifExport(Activity activity, QifExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.qif_export_inprogress), true);
        new QifExportTask(activity, progressDialog, options).execute();
    }

    public static void doQifImport(Activity activity, QifImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.qif_import_inprogress), true);
        new QifImportTask(activity, progressDialog, options).execute();
    }

    private class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

        private final Activity context;
        private ProgressDialog progressDialog;

        IntegrityFixTask(Activity context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, null, context.getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void o) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).refreshCurrentTab();
            }
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... objects) {
            DatabaseAdapter db = new DatabaseAdapter(context);
            new IntegrityFix(db).fix();
            return null;
        }
    }

}
