package ru.orangesoftware.financisto.persistance

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rxjava3.rxPreferencesDataStore
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.orangesoftware.financisto.export.Export
import ru.orangesoftware.financisto.persistance.PreferencesStore.PreferencesKeys.BACKUP_FOLDER_URI
import ru.orangesoftware.financisto.persistance.PreferencesStore.PreferencesKeys.defaultPath

class PreferencesStore(private val context: Context) {
//    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val Context.dataStoreRx: RxDataStore<Preferences> by rxPreferencesDataStore(name = "settings")

    private object PreferencesKeys {
        val BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val defaultPath = Export.DEFAULT_EXPORT_PATH.absolutePath
    }

//    val backupPreferencesFlow: Flow<BackupPreferences> = context.dataStore.data
//        .catch { exception ->
//            // dataStore.data throws an IOException when an error is encountered when reading data
//            if (exception is IOException) {
//                emit(emptyPreferences())
//            } else {
//                throw exception
//            }
//        }.map { preferences ->
//            // No type safety.
//            val folder = preferences[BACKUP_FOLDER_URI] ?: defaultPath
//            BackupPreferences(Uri.parse(folder))
//        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val backupPreferencesRx: Flowable<BackupPreferences> = context.dataStoreRx.data()
        .map {  preferences ->
        val folder = preferences[BACKUP_FOLDER_URI] ?: defaultPath
            BackupPreferences(Uri.parse(folder))
    }/*Observable.create { emitter ->
        runBlocking {
            backupPreferencesFlow.collect { value ->
                emitter.onNext(value)
            }
            emitter.onComplete()
        }
    }*/

    @OptIn(ExperimentalCoroutinesApi::class)
    fun updateBackupFolderUri(folder: Uri) {
//        context.dataStore.edit { preferences ->
//            preferences[BACKUP_FOLDER_URI] = folder.toString()
//        }
        context.dataStoreRx.updateDataAsync { preferences ->
            val mutablePreferences = preferences.toMutablePreferences()
            mutablePreferences[BACKUP_FOLDER_URI] = folder.toString()
            Single.just(mutablePreferences);
        }
    }
}

data class BackupPreferences(val folder: Uri)
