package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.test.SmsTemplateBuilder;

public class SmsTemplateTest extends AbstractDbTest {

    private SmsTemplate template777;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        String template = "*{{a}}. Summa {{P}} RUB. NOVYY PROEKT, MOSCOW. {{D}}. Dostupno {{b}}";
        template777 = SmsTemplateBuilder.withDb(db).title("777").accountId(7).categoryId(8).template(template).create();
    }

    public void testDuplication() throws Exception {
        long dupId = db.duplicate(SmsTemplate.class, template777.id);
        SmsTemplate dup = db.load(SmsTemplate.class, dupId);
        assertNotNull(dup);
        assertEquals(template777.template, dup.template);
        assertEquals(template777.title, dup.title);
        assertEquals(template777.accountId, dup.accountId);
        assertEquals(template777.categoryId, dup.categoryId);
        assertFalse(template777.id == dup.id);
    }

    public void testSorting() throws Exception {
        String template1 = "*{{a}}. Summa {{p}} RUB. {{*}}, MOSCOW. {{d}}. Dostupno {{b}}";
        String template2 = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}}";
        String template3 = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}}";

        SmsTemplateBuilder.withDb(db).title("888").accountId(3).categoryId(8).template(template1).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(6).categoryId(8).template(template1).create();
        SmsTemplateBuilder.withDb(db).title("888").accountId(1).categoryId(88).template(template2).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(4).categoryId(88).template(template2).create();
        SmsTemplateBuilder.withDb(db).title("888").accountId(2).categoryId(89).template(template3).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(5).categoryId(89).template(template3).create();

        try (Cursor c = db.getSmsTemplatesWithFullInfo()) {
            List<SmsTemplate> res = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                SmsTemplate a = SmsTemplate.fromCursor(c);
                res.add(a);
            }

            assertEquals(7, res.get(0).accountId);
            assertEquals(1, res.get(1).accountId);
            assertEquals(2, res.get(2).accountId);
            assertEquals(3, res.get(3).accountId);
            assertEquals(4, res.get(4).accountId);
            assertEquals(5, res.get(5).accountId);
            assertEquals(6, res.get(6).accountId);
        }
    }
}
