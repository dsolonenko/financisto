package ru.orangesoftware.orb;

import android.content.ContentValues;

public interface Plugin {

    void withContentValues(String tableName, ContentValues values);

}
