package ru.orangesoftware.financisto.db;

import android.database.Cursor;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.test.SmsTemplateBuilder;

import static org.junit.Assert.*;

public class SmsTemplateTest extends AbstractDbTest {

    private SmsTemplate template777;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        String template = "*{{a}}. Summa {{P}} RUB. NOVYY PROEKT, MOSCOW. {{D}}. Dostupno {{b}}";
        template777 = SmsTemplateBuilder.withDb(db).title("777").accountId(7).categoryId(8).template(template).create();
    }

    @Test
    public void duplication() throws Exception {
        long dupId = db.duplicate(SmsTemplate.class, template777.id);
        assertEquals(1, template777.getSortOrder());

        SmsTemplate dup = db.load(SmsTemplate.class, dupId);

        assertEquals(template777.template, dup.template);
        assertEquals(template777.title, dup.title);
        assertEquals(template777.accountId, dup.accountId);
        assertEquals(template777.categoryId, dup.categoryId);
        assertEquals(2, dup.getSortOrder());
        assertFalse(template777.id == dup.id);

        SmsTemplate t2 = SmsTemplateBuilder.withDb(db).title("2").accountId(2).categoryId(8).template("second").create();
        SmsTemplate t3 = SmsTemplateBuilder.withDb(db).title("3").accountId(3).categoryId(8).template("third").create();
        SmsTemplate t4 = SmsTemplateBuilder.withDb(db).title("4").accountId(4).categoryId(8).template("4th").create();
        SmsTemplate t5 = SmsTemplateBuilder.withDb(db).title("5").accountId(5).categoryId(8).template("5th").create();
        SmsTemplate t6 = SmsTemplateBuilder.withDb(db).title("6").accountId(6).categoryId(8).template("6th").create();

        long dupAfterThirdId = db.duplicateSmsTemplateBelowOriginal(t3.id);
        long dupAfter5thId = db.duplicateSmsTemplateBelowOriginal(t5.id);
        long dupAfterLastId = db.duplicateSmsTemplateBelowOriginal(t6.id);

        assertEquals(1, db.load(SmsTemplate.class, template777.id).getSortOrder());
        assertEquals(2, db.load(SmsTemplate.class, dupId).getSortOrder());
        assertEquals(3, db.load(SmsTemplate.class, t2.id).getSortOrder());
        assertEquals(4, db.load(SmsTemplate.class, t3.id).getSortOrder());
        assertEquals(5, db.load(SmsTemplate.class, dupAfterThirdId).getSortOrder());
        assertEquals(6, db.load(SmsTemplate.class, t4.id).getSortOrder());
        assertEquals(7, db.load(SmsTemplate.class, t5.id).getSortOrder());
        assertEquals(8, db.load(SmsTemplate.class, dupAfter5thId).getSortOrder());
        assertEquals(9, db.load(SmsTemplate.class, t6.id).getSortOrder());
        assertEquals(10, db.load(SmsTemplate.class, dupAfterLastId).getSortOrder());

    }

    @Test
    public void sorting() throws Exception {
        String template1 = "*{{a}}. Summa {{p}} RUB. {{*}}, MOSCOW. {{d}}. Dostupno {{b}}";
        String template2 = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}}";
        String template3 = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}}";

        SmsTemplateBuilder.withDb(db).title("888").accountId(3).categoryId(8).template(template1).sortOrder(4).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(6).categoryId(8).template(template1).sortOrder(7).create();
        SmsTemplateBuilder.withDb(db).title("888").accountId(1).categoryId(88).template(template2).sortOrder(2).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(4).categoryId(88).template(template2).sortOrder(4).create();
        SmsTemplateBuilder.withDb(db).title("888").accountId(2).categoryId(89).template(template3).sortOrder(3).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(5).categoryId(89).template(template3).sortOrder(6).create();

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

        List<SmsTemplate> res = db.getSmsTemplatesByNumber("888");

        assertEquals("Number Query Sort Order mismatch: ", 1, res.get(0).accountId);
        assertEquals("Number Query Sort Order mismatch: ", 2, res.get(1).accountId);
        assertEquals("Number Query Sort Order mismatch: ", 3, res.get(2).accountId);
    }

    @Test
    public void checking_order() throws Exception {
        SmsTemplate t2 = SmsTemplateBuilder.withDb(db).title("2").accountId(2).categoryId(8).template("first").create();
        SmsTemplate t3 = SmsTemplateBuilder.withDb(db).title("3").accountId(3).categoryId(8).template("second").create();
        SmsTemplate t4 = SmsTemplateBuilder.withDb(db).title("4").accountId(4).categoryId(8).template("third").create();
        long t5Id = db.duplicate(SmsTemplate.class, t4.id);
        SmsTemplate t5 = db.load(SmsTemplate.class, t5Id);
        t5.title = "5";
        t5.template = "4th";
        //SmsTemplateBuilder.withDb(db).title("5").accountId(5).categoryId(8).template("4th").create();
        SmsTemplate t6 = SmsTemplateBuilder.withDb(db).title("6").accountId(6).categoryId(8).template("5th").create();
        SmsTemplate t7 = SmsTemplateBuilder.withDb(db).title("7").accountId(7).categoryId(8).template("6th").create();
        SmsTemplate t8 = SmsTemplateBuilder.withDb(db).title("8").accountId(8).categoryId(8).template("7th").create();

        assertEquals(2, t2.getSortOrder());
        assertEquals(3, t3.getSortOrder());
        assertEquals(4, t4.getSortOrder());
        assertEquals(5, t5.getSortOrder());
        assertEquals(6, t6.getSortOrder());
        assertEquals(7, t7.getSortOrder());
        assertEquals(8, t8.getSortOrder());


        assertEquals(-1, db.getNextByOrder(SmsTemplate.class, 0));
        assertEquals(t2.id, db.getNextByOrder(SmsTemplate.class, template777.id));
        assertEquals(t4.id, db.getNextByOrder(SmsTemplate.class, t3.id));
        assertEquals(t5.id, db.getNextByOrder(SmsTemplate.class, t4.id));
        assertEquals(t8.id, db.getNextByOrder(SmsTemplate.class, t7.id));
        assertEquals(-1, db.getNextByOrder(SmsTemplate.class, t8.id));
    }

    @Test
    public void changing_sorting() throws Exception {
        SmsTemplate t2 = SmsTemplateBuilder.withDb(db).title("2").accountId(2).categoryId(8).template("first").create();
        SmsTemplate t3 = SmsTemplateBuilder.withDb(db).title("3").accountId(3).categoryId(8).template("second").create();
        SmsTemplate t4 = SmsTemplateBuilder.withDb(db).title("4").accountId(4).categoryId(8).template("third").create();
        SmsTemplate t5 = SmsTemplateBuilder.withDb(db).title("5").accountId(5).categoryId(8).template("4th").create();
        SmsTemplate t6 = SmsTemplateBuilder.withDb(db).title("6").accountId(6).categoryId(8).template("5th").create();
        SmsTemplate t7 = SmsTemplateBuilder.withDb(db).title("7").accountId(7).categoryId(8).template("6th").create();
        SmsTemplate t8 = SmsTemplateBuilder.withDb(db).title("8").accountId(8).categoryId(8).template("7th").create();

        // move middle item down 
        Assert.assertTrue(db.moveItemByChangingOrder(SmsTemplate.class, t3.id, t6.id));

        assertEquals(1, db.load(SmsTemplate.class, template777.id).getSortOrder());
        assertEquals(2, db.load(SmsTemplate.class, t2.id).getSortOrder());
        assertEquals(3, db.load(SmsTemplate.class, t4.id).getSortOrder());
        assertEquals(4, db.load(SmsTemplate.class, t5.id).getSortOrder());
        assertEquals(5, db.load(SmsTemplate.class, t6.id).getSortOrder());
        assertEquals(t3.id, db.getNextByOrder(SmsTemplate.class, t6.id));
        assertEquals(6, db.load(SmsTemplate.class, t3.id).getSortOrder());

        // back
        Assert.assertTrue(db.moveItemByChangingOrder(SmsTemplate.class, t3.id, t4.id));

        assertEquals(2, db.load(SmsTemplate.class, t2.id).getSortOrder());
        assertEquals(3, db.load(SmsTemplate.class, t3.id).getSortOrder());
        assertEquals(4, db.load(SmsTemplate.class, t4.id).getSortOrder());
        assertEquals(5, db.load(SmsTemplate.class, t5.id).getSortOrder());
        assertEquals(6, db.load(SmsTemplate.class, t6.id).getSortOrder());

        // move middle item up to the top
        Assert.assertTrue(db.moveItemByChangingOrder(SmsTemplate.class, t5.id, template777.id));

        assertEquals(1, db.load(SmsTemplate.class, t5.id).getSortOrder());
        assertEquals(2, db.load(SmsTemplate.class, template777.id).getSortOrder());
        assertEquals(3, db.load(SmsTemplate.class, t2.id).getSortOrder());
        assertEquals(4, db.load(SmsTemplate.class, t3.id).getSortOrder());
        assertEquals(5, db.load(SmsTemplate.class, t4.id).getSortOrder());
        assertEquals(6, db.load(SmsTemplate.class, t6.id).getSortOrder());

        // move middle item down to the bottom
        Assert.assertTrue(db.moveItemByChangingOrder(SmsTemplate.class, t4.id, t6.id));

        assertEquals(1, db.load(SmsTemplate.class, t5.id).getSortOrder());
        assertEquals(2, db.load(SmsTemplate.class, template777.id).getSortOrder());
        assertEquals(3, db.load(SmsTemplate.class, t2.id).getSortOrder());
        assertEquals(4, db.load(SmsTemplate.class, t3.id).getSortOrder());
        assertEquals(5, db.load(SmsTemplate.class, t6.id).getSortOrder());
        assertEquals(6, db.load(SmsTemplate.class, t4.id).getSortOrder());

        // move item down to the next one
        Assert.assertTrue(db.moveItemByChangingOrder(SmsTemplate.class, t2.id, t3.id));

        assertEquals(1, db.load(SmsTemplate.class, t5.id).getSortOrder());
        assertEquals(2, db.load(SmsTemplate.class, template777.id).getSortOrder());
        assertEquals(3, db.load(SmsTemplate.class, t3.id).getSortOrder());
        assertEquals(4, db.load(SmsTemplate.class, t2.id).getSortOrder());
        assertEquals(5, db.load(SmsTemplate.class, t6.id).getSortOrder());
        assertEquals(6, db.load(SmsTemplate.class, t4.id).getSortOrder());

        // move item up to the next one
        Assert.assertTrue(db.moveItemByChangingOrder(SmsTemplate.class, t4.id, t6.id));

        assertEquals(1, db.load(SmsTemplate.class, t5.id).getSortOrder());
        assertEquals(2, db.load(SmsTemplate.class, template777.id).getSortOrder());
        assertEquals(3, db.load(SmsTemplate.class, t3.id).getSortOrder());
        assertEquals(4, db.load(SmsTemplate.class, t2.id).getSortOrder());
        assertEquals(5, db.load(SmsTemplate.class, t4.id).getSortOrder());
        assertEquals(6, db.load(SmsTemplate.class, t6.id).getSortOrder());

        Assert.assertFalse(db.moveItemByChangingOrder(SmsTemplate.class, t2.id, t2.id));
        Assert.assertFalse(db.moveItemByChangingOrder(SmsTemplate.class, t6.id, 0));
    }


}
