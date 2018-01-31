package ru.orangesoftware.financisto.adapter.async;

import android.database.Cursor;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.SmsTemplate;

public class SmsTemplateListSource extends CursorItemSource<SmsTemplate> {
    private final DatabaseAdapter db;

    public SmsTemplateListSource(DatabaseAdapter db) {
        this.db = db;
    }

    @Override
    public Cursor init() {
        return db.getSmsTemplatesWithFullInfo();
    }

    @Override
    protected SmsTemplate loadItem() {
        return SmsTemplate.fromCursor(cursor);
    }

    @Override
    public Class<SmsTemplate> clazz() {
        return SmsTemplate.class;
    }
}
