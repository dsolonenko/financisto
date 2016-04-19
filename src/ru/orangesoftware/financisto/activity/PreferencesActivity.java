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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.dialog.FolderBrowser;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.rates.ExchangeRateProviderFactory;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

public class PreferencesActivity extends PreferenceActivity {

    private static final String[] ACCOUNT_TYPE = new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    private static final int SELECT_DATABASE_FOLDER = 100;
    private static final int CHOOSE_ACCOUNT = 101;

    Preference pOpenExchangeRatesAppId;
    GoogleAccountManager googleAccountManager;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);   
		addPreferencesFromResource(R.xml.preferences);

        googleAccountManager = new GoogleAccountManager(this);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference pLocale = preferenceScreen.findPreference("ui_language");
        pLocale.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String locale = (String) newValue;
                MyPreferences.switchLocale(PreferencesActivity.this, locale);
				return true;
			}
		});
		Preference pNewTransactionShortcut = preferenceScreen.findPreference("shortcut_new_transaction");
		pNewTransactionShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                addShortcut(".activity.TransactionActivity", R.string.transaction, R.drawable.icon_transaction);
                return true;
            }

        });
		Preference pNewTransferShortcut = preferenceScreen.findPreference("shortcut_new_transfer");
		pNewTransferShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				addShortcut(".activity.TransferActivity", R.string.transfer, R.drawable.icon_transfer);
				return true;
			}
		});
        Preference pDatabaseBackupFolder = preferenceScreen.findPreference("database_backup_folder");
        pDatabaseBackupFolder.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                selectDatabaseBackupFolder();
                return true;
            }
        });
        Preference pAuthDropbox = preferenceScreen.findPreference("dropbox_authorize");
        pAuthDropbox.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                authDropbox();
                return true;
            }
        });
        Preference pDeauthDropbox = preferenceScreen.findPreference("dropbox_unlink");
        pDeauthDropbox.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                deAuthDropbox();
                return true;
            }
        });
        Preference pExchangeProvider = preferenceScreen.findPreference("exchange_rate_provider");
        pOpenExchangeRatesAppId = preferenceScreen.findPreference("openexchangerates_app_id");
        pExchangeProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                pOpenExchangeRatesAppId.setEnabled(isOpenExchangeRatesProvider((String) newValue));
                return true;
            }

            private boolean isOpenExchangeRatesProvider(String provider) {
                return ExchangeRateProviderFactory.openexchangerates.name().equals(provider);
            }
        });
        Preference pDriveAccount = preferenceScreen.findPreference("google_drive_backup_account");
        pDriveAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                chooseAccount();
                return true;
            }
        });
        linkToDropbox();
        setCurrentDatabaseBackupFolder();
        enableOpenExchangeApp();
        selectAccount();
	}

    private void chooseAccount() {
        try {
            Account selectedAccount = getSelectedAccount();
            Intent intent = AccountPicker.newChooseAccountIntent(selectedAccount, null, ACCOUNT_TYPE, true,
                    null, null, null, null);
            startActivityForResult(intent, CHOOSE_ACCOUNT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.google_drive_account_select_error, Toast.LENGTH_LONG).show();
        }
    }

    private Account getSelectedAccount() {
        Account selectedAccount = null;
        String account = MyPreferences.getGoogleDriveAccount(this);
        if (account != null) {
            selectedAccount = googleAccountManager.getAccountByName(account);
        }
        return selectedAccount;
    }

    private void linkToDropbox() {
        boolean dropboxAuthorized = MyPreferences.isDropboxAuthorized(this);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.findPreference("dropbox_unlink").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_backup").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_autobackup").setEnabled(dropboxAuthorized);
    }

    private void selectDatabaseBackupFolder() {
        Intent intent = new Intent(this, FolderBrowser.class);
        intent.putExtra(FolderBrowser.PATH, getDatabaseBackupFolder());
        startActivityForResult(intent, SELECT_DATABASE_FOLDER);
    }

    private void enableOpenExchangeApp() {
        pOpenExchangeRatesAppId.setEnabled(MyPreferences.isOpenExchangeRatesProviderSelected(this));
    }

    private String getDatabaseBackupFolder() {
        return Export.getBackupFolder(this).getAbsolutePath();
    }

    private void setCurrentDatabaseBackupFolder() {
        Preference pDatabaseBackupFolder = getPreferenceScreen().findPreference("database_backup_folder");
        String summary = getString(R.string.database_backup_folder_summary, getDatabaseBackupFolder());
        pDatabaseBackupFolder.setSummary(summary);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_DATABASE_FOLDER:
                    String databaseBackupFolder = data.getStringExtra(FolderBrowser.PATH);
                    MyPreferences.setDatabaseBackupFolder(this, databaseBackupFolder);
                    setCurrentDatabaseBackupFolder();
                    break;
                case CHOOSE_ACCOUNT:
                    if (data != null) {
                        Bundle b = data.getExtras();
                        String accountName = b.getString(AccountManager.KEY_ACCOUNT_NAME);
                        Log.d("Preferences", "Selected account: " + accountName);
                        if (accountName != null && accountName.length() > 0) {
                            Account account = googleAccountManager.getAccountByName(accountName);
                            MyPreferences.setGoogleDriveAccount(this, account.name);
                            selectAccount();
                        }
                    }
                    break;
            }
        }
    }

    private void selectAccount() {
        Preference pDriveAccount = getPreferenceScreen().findPreference("google_drive_backup_account");
        Account account = getSelectedAccount();
        if (account != null) {
            pDriveAccount.setSummary(account.name);
        }
    }

    private void addShortcut(String activity, int nameId, int iconId) {
		Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(this, iconId), 
				"com.android.launcher.action.INSTALL_SHORTCUT");
		sendBroadcast(intent);
	}

	private Intent createShortcutIntent(String activity, String shortcutName, ShortcutIconResource shortcutIcon, String action) {
		Intent shortcutIntent = new Intent();
		shortcutIntent.setComponent(new ComponentName(this.getPackageName(), activity));
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon);
		intent.setAction(action);
		return intent;
	}

    Dropbox dropbox = new Dropbox(this);

    private void authDropbox() {
        dropbox.startAuth();
    }

    private void deAuthDropbox() {
        dropbox.deAuth();
        linkToDropbox();
    }

    @Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
        dropbox.completeAuth();
        linkToDropbox();
    }

}
