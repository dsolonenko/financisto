package ru.orangesoftware.financisto.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import java.util.Arrays;
import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.model.Total;

/**
 * todo.mb: move to {@link FinancistoService} and call it from here via Intent
 */
public class SmsReceiver extends BroadcastReceiver {

    public static final String SMS_EXTRA_NAME = "pdus";
    public static final String FTAG = "Financisto";
    public static final String ACCOUNT_PATT = "<:A:>";
    public static final String PRICE_PATT = "<:P:>";
    public static final String BALANCE_PATT = "<:B:>";
    public static final String DATE_PATT = "<:D:>";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle smsExtras = intent.getExtras();
        final DatabaseAdapter db = new DatabaseAdapter(context);
        final Total total = db.getAccountsTotalInHomeCurrency();
        Log.d(FTAG, "Totals: " + total.balance);

        Set<String> allowedNumbers = new HashSet<String>(asList("900", "Tinkoff")); // todo.mb: get from Prefs

        Object[] smsArray;
        if (smsExtras != null
            && (smsArray = (Object[]) smsExtras.get(SMS_EXTRA_NAME)) != null) {

            for (final Object one : smsArray) {
                final SmsMessage sms = SmsMessage.createFromPdu((byte[]) one);

                final String addr = sms.getOriginatingAddress();
                String body = sms.getMessageBody();
                if (allowedNumbers.contains(addr)) {
                    List<SmsTemplate> addrTemplates = db.getSmsTemplatesByNumber(addr);
                    for (SmsTemplate t : addrTemplates) {
                        String[] match = findTemplateMatch(t.template, body);
                    }

                    Category category = findCategoryBySmsTemplate(body);
                    Account account = findAccountBySmsTemplate(db, body);
                    if (category != null) {
                        Log.d(FTAG, String.format("Received finance sms, number/body: `%s/%s`", addr, body));
                    }



                } else {
                    Log.d(FTAG, String.format("SMS from `%s` is ignored", addr));
                }

                // Display SMS message
                Toast.makeText(context, String.format("%s:%s", addr, body), Toast.LENGTH_SHORT).show();
            }
        }

        // WARNING!!!
        // If you uncomment the next line then received SMS will not be put to incoming.
        // Be careful!
        // this.abortBroadcast();
    }

    /**
     * ex. ECMC<:A:> <:D:> покупка <:P:> TEREMOK METROPOLIS Баланс: <:B:>р
     */
    public String[] findTemplateMatch(String template, final String sms) {
        // todo.mb: add filter by common prefix and suffix
        String[] results = new String[Placeholder.values().length];
        final int[] phIndexes = findPlaceholderIndexes(template);

        template = template.replaceAll("([\\.\\[\\]\\{\\}\\(\\)\\*\\+\\-\\?\\^\\$\\|])", "\\\\$1");
        for (int i = 0; i < phIndexes.length; i++) {
            if (phIndexes[i] != -1) {
                Placeholder placeholder = Placeholder.values()[i];
                template = template.replace(placeholder.code, placeholder.regexp);
            }
        }

        Matcher matcher = Pattern.compile(template).matcher(sms);
        if (matcher.matches()) {
            for (int i = 0; i < phIndexes.length; i++) {
                final int groupNum = phIndexes[i] + 1;
                if (groupNum > 0) {
                    results[i] = matcher.group(groupNum);
                }
            }
            return results;
        }
        return null;
    }

    int[] findPlaceholderIndexes(String template) {
        int[] result = new int[Placeholder.values().length];
        Arrays.fill(result, -1);
        Map<Integer, Placeholder> sorted = new TreeMap<Integer, Placeholder>();
        for (Placeholder p : Placeholder.values()) {
            int i = template.indexOf(p.code);
            if (i >= 0) sorted.put(i, p);
        }
        int i = 0;
        for (Placeholder p : sorted.values()) {
            result[p.ordinal()] = i++;
        }
        return result;
    }



    private Account findAccountBySmsTemplate(final DatabaseAdapter db, final String smsBody) {

        return null;
    }

    private Category findCategoryBySmsTemplate(final String smsBody) {
        return null;
    }

    enum Placeholder {
        /**
         * Please note that order of constants is very important,
         * and keep it in alphabetical way
         */
        ACCOUNT("<:A:>", "(\\d{4})"),
        BALANCE("<:B:>", "(\\d+[\\.,]?\\d{0,4})"),
        DATE("<:D:>", "(\\d[\\d\\. :]{12,14}\\d)"),
        PRICE("<:P:>", "(\\d+[\\.,]?\\d{0,4})");

        public String code;
        public String regexp;

        Placeholder(String code, String regexp) {
            this.code = code;
            this.regexp = regexp;
        }
    }
}
