package ru.orangesoftware.financisto.adapter.async;

public interface ItemSource<T> {

    Class<T> clazz();

    int getCount();

    T getItem(int position);

    void close();
    
    void setConstraint(CharSequence constraint);
}
