package ru.orangesoftware.financisto.service;

import org.junit.Assert;
import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.SmsTemplateBuilder;

public class SmsTransactionProcessorTest extends AbstractDbTest {

    SmsTransactionProcessor smsProcessor;
    TransactionStatus status =TransactionStatus.PN;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        smsProcessor = new SmsTransactionProcessor(db);
    }

    public void testTemplateWithTextPlaceholder() throws Exception {
        String template = "Покупка. Карта *{{a}}. {{p}} RUB.{{t}}. Доступно {{b}}";
        String sms = "Покупка. Карта *5631. 1477.14 RUB. RNAZK ROSNEF. Доступно 30321.9 RUB";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "30321.9", null, "1477.14", " RNAZK ROSNEF"}, matches);

        SmsTemplateBuilder.withDb(db).title("777").accountId(7).categoryId(8).template(template).create();
        Transaction transaction = smsProcessor.createTransactionBySms("777", sms, status, true);

        assertEquals(7, transaction.fromAccountId);
        assertEquals(8, transaction.categoryId);
        assertEquals(-147714, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }

    public void testTransactionByTinkoffSms() throws Exception {
        String template = "*{{a}}. Summa {{P}} RUB. NOVYY PROEKT, MOSCOW. {{D}}. Dostupno {{b}}";
        String sms = "Pokupka. Karta *5631. Summa 250.77 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34202.82 RUB. Tinkoff.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "34202.82", "02.10.2017 14:19", "250.77", null}, matches);

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(8).template(template).create();
        Transaction transaction = smsProcessor.createTransactionBySms("Tinkoff", sms, status, true);

        assertEquals(7, transaction.fromAccountId);
        assertEquals(8, transaction.categoryId);
        assertEquals(-25077, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }

    public void testDifferentPrices() throws Exception {
        String template = "*{{a}}. Summa {{P}} RUB. NOVYY PROEKT, MOSCOW. {{D}}. Dostupno {{b}}";
        String sms = "Pokupka. Karta *5631. Summa 250,77 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34202.82 RUB. Tinkoff.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "34202.82", "02.10.2017 14:19", "250,77", null}, matches);

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(8).template(template).create();
        Transaction transaction = smsProcessor.createTransactionBySms("Tinkoff", sms, status, true);

        assertEquals(7, transaction.fromAccountId);
        assertEquals(8, transaction.categoryId);
        assertEquals(-25077, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }


    public void testNotFound() throws Exception {
        String smsTpl = "ECMC{{a}} {{d}} покупка {{p}}р TEREMOK METROPOLIS Баланс: {{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK XXX Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertNull(matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).create();
        Transaction transaction = smsProcessor.createTransactionBySms("900", sms, status, true);

        assertNull(transaction);
    }

    public void testTransactionBySberSmsWithAccountLookup() throws Exception {
        String smsTpl = "ECMC{{a}} {{d}} покупка {{p}}р TEREMOK METROPOLIS Баланс: {{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).create();
        Account account = AccountBuilder.withDb(db)
            .currency(CurrencyBuilder.createDefault(db))
            .title("SB")
            .number("1111-2222-3333-5431")
            .create();
        Transaction transaction = smsProcessor.createTransactionBySms("900", sms, status, false);

        assertEquals(account.id, transaction.fromAccountId);
        assertEquals(18, transaction.categoryId);
        assertEquals(-55000, transaction.fromAmount);
        assertEquals("", transaction.note);
    }

    public void testNotFoundAccount() throws Exception {
        String smsTpl = "ECMC{{A}} {{D}} покупка {{P}}р TEREMOK METROPOLIS Баланс: {{B}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").categoryId(18).template(smsTpl).create();
        Transaction transaction = smsProcessor.createTransactionBySms("900", sms, status, true);

        assertNull(transaction);
    }

    public void testWrongPrice() throws Exception {
        String smsTpl = "ECMC{{*}} {{*}} покупка {{p}}р TEREMOK METROPOLIS Баланс: {{*}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 0р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, null, null, "0", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).create();
        Transaction transaction = smsProcessor.createTransactionBySms("900", sms, status, true);

        assertNull(transaction);
    }

    public void testDebitTransactionBySberSms() throws Exception {
        String smsTpl = "ECMC<:A:> <:D:> покупка <:P:>р TEREMOK METROPOLIS Баланс: <:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).income(true).create();
        Transaction transaction = smsProcessor.createTransactionBySms("900", sms, status, true);

        assertEquals(17, transaction.fromAccountId);
        assertEquals(18, transaction.categoryId);
        assertEquals(55000, transaction.fromAmount);
        assertEquals(sms, transaction.note);
    }

    public void testTemplateWithWrongSpaces() throws Exception {
        String smsTpl = "ECMC{{a}}<:D:>покупка{{P}}р TEREMOK METROPOLIS Баланс:{{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    public void testTemplateWithAnyMatch() throws Exception {
        String smsTpl = "ECMC{{A}}{{d}}покупка<:P:>р TEREMOK <::>Баланс:<:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    public void testTemplateWithMultipleAnyMatch() throws Exception {
        String smsTpl = "ECMC<:A:> <:D:> {{*}} <:P:>р TEREMOK<::><:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    public void testTemplateWithMultipleAnyMatchWithoutAccount() throws Exception {
        String smsTpl = "<::> <:D:> {{*}} <:P:>р TEREMOK<::><:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    public void testTemplateWithSpecialChars() throws Exception {
        String smsTpl = "{{*}} {{d}} {{*}} {{p}}р TE{{R}}E{{MOK ME}TROP<:P:OL?$()[]/\\.*IS{{*}}{{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 555р TE{{R}}E{{MOK ME}TROP<:P:OL?$()[]/\\.*IS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, "49820.45", "01.10.17 19:50", "555", null}, matches);
    }

    public void testMultipleAnyMatchWithoutAccountAndDate() throws Exception {
        String smsTpl = "Pokupka{{*}}Summa {{p}} RUB. NOVYY PROEKT, MOSCOW{{*}}Dostupno {{b}} RUB.{{*}}";
        String sms = "Pokupka. Karta *5631. Summa 250.00 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34202.82 RUB. Tinkoff.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, "34202.82", null, "250.00", null}, matches);
    }

    public void testFindingPlaceholderIndexes() throws Exception {
        int[] indexes = SmsTransactionProcessor.findPlaceholderIndexes("Pokupka. Karta *<:A:>. Summa <:P:> RUB. NOVYY PROEKT, MOSCOW. <:D:>. Dostupno <:B:> RUB. Tinkoff.ru");
        Assert.assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        Assert.assertTrue(indexes[Placeholder.PRICE.ordinal()] == 1);
        Assert.assertTrue(indexes[Placeholder.DATE.ordinal()] == 2);
        Assert.assertTrue(indexes[Placeholder.BALANCE.ordinal()] == 3);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:> покупка <:P:> TEREMOK METROPOLIS Баланс: <:B:>р");
        Assert.assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        Assert.assertTrue(indexes[Placeholder.DATE.ordinal()] == 1);
        Assert.assertTrue(indexes[Placeholder.PRICE.ordinal()] == 2);
        Assert.assertTrue(indexes[Placeholder.BALANCE.ordinal()] == 3);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:> покупка <:P:> TEREMOK METROPOLIS Баланс:");
        Assert.assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        Assert.assertTrue(indexes[Placeholder.DATE.ordinal()] == 1);
        Assert.assertTrue(indexes[Placeholder.PRICE.ordinal()] == 2);
        Assert.assertTrue(indexes[Placeholder.BALANCE.ordinal()] == -1);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:><::> <:P:> TEREMOK METROPOLIS Баланс:");
        Assert.assertTrue(indexes[Placeholder.ANY.ordinal()] == -1);
        Assert.assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        Assert.assertTrue(indexes[Placeholder.DATE.ordinal()] == 1);
        Assert.assertTrue(indexes[Placeholder.PRICE.ordinal()] == 2);
        Assert.assertTrue(indexes[Placeholder.BALANCE.ordinal()] == -1);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:><::> TEREMOK METROPOLIS Баланс:");
        Assert.assertNull(indexes);
    }
}