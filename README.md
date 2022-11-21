# Financisto

[![Build Status](https://app.bitrise.io/app/a4284a64a52e1063/status.svg?token=-JUe6I0K_79mxYjxLGp9BA&branch=master)](https://app.bitrise.io/app/a4284a64a52e1063)

## About

Financisto is an open-source personal finance tracker for Android platform. This is a forked version as the original one is not maintained for a long time. I already picked some PRs in the oroginal financisto and could work on new PRs here. However, I have no resouce to fix known issues below or fulfill new feature requests due to my perosnal affair for now.

For those who love this app, you can find my re-published app here: https://play.google.com/store/apps/details?id=com.bluecatsoftware.financisto

## Restore from the original finicisto

The backup file is compabitlbe with this financisto+, but financisto+ cannot read the backup file directly due to new Android app policy, i.e. an App cannot read files from other apps.

There are two tricks you can use to read the backup file from the original financisto.

* Via ADB
  * Trigger backup from financisto+, it should generate a backup file, i.e. /sdcard/Documents/financisto+/XXXX.backup
  * Use adb to push copy your financisto backup file, YYYY.backup, to the same location
  * Execute "cp YYYY.backup XXXX.backup" to override it (Must use cp, but not mv).
  * Now you can restore your old data from this overrided file.

## Known issues
* Google Driver linking does not work because Google Driver APIs are changed and old ones used by financisto are deprecated.
* ~~Dropbxo linking can only be maintained for one day due to unknown reasons, i.e. you need to re-login again and everyday backup upload won't work.~~ Dropbox does not work anymore.
* SMS function is suspended because Google Store does not grant SMS permission to this application.
* Fingerprint function does not work on Android 13.

## Features

- Multiple accounts, multiple currencies 
- Home currency and exchange rates
- Transfers with downloadable rates
- Scheduled & recurring transactions
- Split transactions
- Hierarchical categories with custom attributes
- Recurring budgets
- Projects and payees
- Filtering and reporting
- Cloud backup (Dropbox, Google Drive)
- Automatic daily backups
- QIF/CSV import/export

## License

See [License](license.txt)
