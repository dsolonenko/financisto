package ru.orangesoftware.financisto.db;

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
}
