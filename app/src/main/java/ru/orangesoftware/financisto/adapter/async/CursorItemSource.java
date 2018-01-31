package ru.orangesoftware.financisto.adapter.async;

import android.database.Cursor;

abstract public class CursorItemSource<T> implements ItemSource<T>, AutoCloseable {
    protected Cursor cursor;

    public CursorItemSource() {
        cursor = init();
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public T getItem(int position) {
        if (cursor == null || cursor.isClosed()) {
            cursor = init();
        }
        if(cursor.moveToPosition(position)){
            return loadItem();
        }
        return itemOnError();

    }

    protected T itemOnError() {
        return null;
    }

    protected abstract T loadItem();

    public abstract Cursor init();

    @Override
    public void close() {
        cursor.close();
    }
}
