package ru.orangesoftware.financisto.export;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public abstract class AbstractExportTest<T extends Export, O> extends AbstractImportExportTest {

    String exportAsString(O options) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        T export = createExport(options);
        export.export(bos);
        String s = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        Log.d("Export", s);
        return s;
    }

    protected abstract T createExport(O options);

}
