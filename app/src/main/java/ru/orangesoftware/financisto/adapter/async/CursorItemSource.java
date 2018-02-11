package ru.orangesoftware.financisto.adapter.async;

import android.database.Cursor;

abstract public class CursorItemSource<T> implements ItemSource<T>, AutoCloseable {
    protected Cursor cursor;

    @Override
    public int getCount() {
        prepareCursor();
        return cursor.getCount();
    }

    @Override
    public T getItem(int position) {
        prepareCursor();
        if(cursor.moveToPosition(position)){
            return loadItem();
        }
        return itemOnError();

    }

    public void prepareCursor() {
        if (cursor == null || cursor.isClosed()) {
            cursor = initCursor();
        }
    }

    protected T itemOnError() {
        return null;
    }

    protected abstract T loadItem();

    public abstract Cursor initCursor();

    @Override
    public void close() {
        if (cursor != null) cursor.close();
    }
}
