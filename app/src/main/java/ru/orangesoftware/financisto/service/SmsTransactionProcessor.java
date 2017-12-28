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
import static ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder.ACCOUNT;
import static ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder.ANY;
import static ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder.PRICE;
import ru.orangesoftware.financisto.utils.StringUtil;

public class SmsTransactionProcessor {
    private static final String TAG = SmsTransactionProcessor.class.getSimpleName();

    private final DatabaseAdapter db;

    public SmsTransactionProcessor(DatabaseAdapter db) {
        this.db = db;
    }

    /**
     * Parses sms and adds new transaction if it matches any sms template
     * @return new transaction or null if not matched/parsed
     */
    public Transaction createTransactionBySms(String addr, String fullSmsBody, TransactionStatus status) {
        List<SmsTemplate> addrTemplates = db.getSmsTemplatesByNumber(addr);
        for (final SmsTemplate t : addrTemplates) {
            String[] match = findTemplateMatches(t.template, fullSmsBody);
            if (match != null) {
                Log.d(TAG, format("Found template`%s` with matches `%s`", t, Arrays.toString(match)));

                String parsedPrice = match[PRICE.ordinal()];
                String account = match[ACCOUNT.ordinal()];
                try {
                    double price = Double.parseDouble(parsedPrice);
                    return createNewTransaction(price, account, t, fullSmsBody, status);
                } catch (Exception e) {
                    Log.e(TAG, format("Failed to parse price value: `%s`", parsedPrice), e);
                }
            }
        }
        return null;
    }

    private Transaction createNewTransaction(double price,
        String accountDigits,
        SmsTemplate smsTemplate,
        String note,
        TransactionStatus status) {
        Transaction res = null;
        long accountId = findAccount(accountDigits, smsTemplate.accountId);
        if (price > 0 && accountId > 0) {
            res = new Transaction();
            res.isTemplate = 0;
            res.fromAccountId = accountId;
            res.fromAmount = (smsTemplate.isIncome ? 1 : -1) * (long) Math.abs(price * 100);
            res.note = note; // todo.mb: move to prefs?
            res.categoryId = smsTemplate.categoryId;
            res.status = status;
            long id = db.insertOrUpdate(res);
            res.id = id;

            Log.i(TAG, format("Transaction `%s` was added with id=%s", res, id));
        } else {
            Log.e(TAG, format("Account not found or price wrong for `%s` sms template", smsTemplate));
        }
        return res;
    }

    private long findAccount(String accountLastDigits, long defaultId) {
        long res = defaultId;
        long matchedAccId = findAccountByCardNumber(accountLastDigits);
        if (matchedAccId > 0) {
            res = matchedAccId;
            Log.d(TAG, format("Found account %s by sms match: `%s`", matchedAccId, accountLastDigits));
        }
        return res;
    }

    private long findAccountByCardNumber(String accountEnding) {
        long res = -1;

        if (!StringUtil.isEmpty(accountEnding)) {
            List<Long> accountIds = db.findAccountsByNumber(accountEnding);
            if (accountIds.size() > 0) {
                res = accountIds.get(0);
                if (accountIds.size() > 1) {
                    Log.e(TAG, format("Accounts ending with `%s` - more than one!", accountEnding));
                }
            }
        }
        return res;
    }

    /**
     * Finds template matches or null if none
     * ex. ECMC<:A:> <:D:> покупка <:P:> TEREMOK <::>Баланс: <:B:>р
     */
    public static String[] findTemplateMatches(String template, final String sms) {
        String[] results = null;
        final int[] phIndexes = findPlaceholderIndexes(template);
        if (phIndexes != null) {
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
        }
        return results;
    }

    /**
     * @return null if not found Price placeholder
     */
    static int[] findPlaceholderIndexes(String template) {
        Map<Integer, Placeholder> sorted = new TreeMap<Integer, Placeholder>();
        boolean foundPrice = false;
        for (Placeholder p : Placeholder.values()) {
            int i = template.indexOf(p.code);
            if (i >= 0) {
                if (p == PRICE) {
                    foundPrice = true;
                }
                if (p != ANY) {
                    sorted.put(i, p);
                }
            }
        }
        int[] result = null;
        if (foundPrice) {
            result = new int[Placeholder.values().length];
            Arrays.fill(result, -1);
            int i = 0;
            for (Placeholder p : sorted.values()) {
                result[p.ordinal()] = i++;
            }
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
