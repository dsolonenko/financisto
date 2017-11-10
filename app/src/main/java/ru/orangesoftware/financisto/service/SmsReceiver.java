package ru.orangesoftware.financisto.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                    for (SmsTemplate template : addrTemplates) {
                        if (checkSmsMatch(body, template.template)) {
                            Log.i(FTAG, "!!!");
                        }
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
    private boolean checkSmsMatch(final String smsText, final String template) {
        final Pattern regexTpl = getRegexTemplate(template);
        return false;
    }

    private Pattern getRegexTemplate(final String template) {
        template
            .replace(ACCOUNT_PATT, "(\\d{4})")
            .replace(PRICE_PATT, "(\\d+[\\.,]\\d{1,4})")
            .replace(BALANCE_PATT, "(\\d+[\\.,]\\d{1,4})")
            .replace(DATE_PATT, "(\\d[\\d\\. :]{12,14}\\d)");
    }

    private Account findAccountBySmsTemplate(final DatabaseAdapter db, final String smsBody) {

        return null;
    }

    private Category findCategoryBySmsTemplate(final String smsBody) {
        return null;
    }

    public static void main(String[] args) {
        System.out.println("!");
    }
}
