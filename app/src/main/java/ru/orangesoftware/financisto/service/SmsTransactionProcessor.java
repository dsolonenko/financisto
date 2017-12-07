package ru.orangesoftware.financisto.service;

import android.util.Log;
import static java.lang.String.format;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionStatus;
import static ru.orangesoftware.financisto.service.SmsReceiver.FTAG;
import static ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder.ACCOUNT;
import static ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder.ANY;
import static ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder.PRICE;

public class SmsTransactionProcessor {

    private final DatabaseAdapter db;

    public SmsTransactionProcessor(DatabaseAdapter db) {
        this.db = db;
    }

    /**
     * Parses sms and adds new transaction if it matches any sms template
     * @return new transaction or null if not matched/parsed
     */
    public Transaction createTransactionBySms(String addr, String fullSmsBody) {
        List<SmsTemplate> addrTemplates = db.getSmsTemplatesByNumber(addr);
        for (final SmsTemplate t : addrTemplates) {
            String[] match = findTemplateMatches(t.template, fullSmsBody);
            if (match != null) {
                Log.d(FTAG, format("Found template`%s` with matches `%s`", t, Arrays.toString(match)));

                return createTransaction(db, match, fullSmsBody, t);
            }
        }
        return null;
    }

    private Transaction createTransaction(DatabaseAdapter db, String[] match, String fullSmsBody, SmsTemplate smsTemplate) {
        final Transaction t = new Transaction();
        t.isTemplate = 0;
        long accountId = findAccountByCardNumber(match[ACCOUNT.ordinal()]);
        t.fromAccountId = accountId > 0 ? accountId : smsTemplate.accountId;
        double price = Double.parseDouble(match[PRICE.ordinal()]);
        t.fromAmount = (smsTemplate.isIncome ? 1 : -1) * (long) Math.abs(price * 100);
        t.note = fullSmsBody;
        t.categoryId = smsTemplate.categoryId;
        t.status = TransactionStatus.PN; // todo.mb: get this status from Prefs
        long id = db.insertOrUpdate(t);
        t.id = id;

        Log.i(FTAG, format("Transaction `%s` was added with id=%s", t, id));
        return t;
    }

    private long findAccountByCardNumber(String accountEnding) {
        // todo.mb
        return 0;
    }

    /**
     * Finds template matches or null if none
     * ex. ECMC<:A:> <:D:> покупка <:P:> TEREMOK <::>Баланс: <:B:>р
     */
    String[] findTemplateMatches(String template, final String sms) {
        String[] results = null;
        final int[] phIndexes = findPlaceholderIndexes(template);

        template = template.replaceAll("([\\.\\[\\]\\{\\}\\(\\)\\*\\+\\-\\?\\^\\$\\|])", "\\\\$1");
        for (int i = 0; i < phIndexes.length; i++) {
            if (phIndexes[i] != -1) {
                Placeholder placeholder = Placeholder.values()[i];
                template = template.replace(placeholder.code, placeholder.regexp);
            }
        }
        template = template.replace(ANY.code, ANY.regexp);

        Matcher matcher = Pattern.compile(template).matcher(sms);
        if (matcher.matches()) {
            results = new String[Placeholder.values().length];
            for (int i = 0; i < phIndexes.length; i++) {
                final int groupNum = phIndexes[i] + 1;
                if (groupNum > 0) {
                    results[i] = matcher.group(groupNum);
                }
            }
        }
        return results;
    }

    int[] findPlaceholderIndexes(String template) {
        int[] result = new int[Placeholder.values().length];
        Arrays.fill(result, -1);
        Map<Integer, Placeholder> sorted = new TreeMap<Integer, Placeholder>();
        for (Placeholder p : Placeholder.values()) {
            if (p == ANY) continue;

            int i = template.indexOf(p.code);
            if (i >= 0) sorted.put(i, p);
        }
        int i = 0;
        for (Placeholder p : sorted.values()) {
            result[p.ordinal()] = i++;
        }
        return result;
    }


    enum Placeholder {
        /**
         * Please note that order of constants is very important,
         * and keep it in alphabetical way
         */
        ANY("<::>", ".*?"),
        ACCOUNT("<:A:>", "\\s*?(\\d{4})\\s*?"),
        BALANCE("<:B:>", "\\s*?(\\d+[\\.,]?\\d{0,4})\\s*?"),
        DATE("<:D:>", "\\s*?(\\d[\\d\\. :]{12,14}\\d)\\s*?"),
        PRICE("<:P:>", "\\s*?(\\d+[\\.,]?\\d{0,4})\\s*?");

        public String code;
        public String regexp;

        Placeholder(String code, String regexp) {
            this.code = code;
            this.regexp = regexp;
        }
    }
}
