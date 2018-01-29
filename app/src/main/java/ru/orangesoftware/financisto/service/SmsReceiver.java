package ru.orangesoftware.financisto.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import static java.lang.String.format;
import java.util.Set;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import static ru.orangesoftware.financisto.service.FinancistoService.ACTION_NEW_TRANSACTION_SMS;

public class SmsReceiver extends BroadcastReceiver {

    public static final String PDUS_NAME = "pdus";
    public static final String FTAG = "Financisto";
    public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    public static final String SMS_TRANSACTION_NUMBER = "SMS_TRANSACTION_NUMBER";
    public static final String SMS_TRANSACTION_BODY = "SMS_TRANSACTION_BODY";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if(!SMS_RECEIVED_ACTION.equals(intent.getAction())) return;

        Bundle pdusObj = intent.getExtras();
        final DatabaseAdapter db = new DatabaseAdapter(context);
        Set<String> smsNumbers = db.findAllSmsTemplateNumbers();
        Log.d(FTAG, "All sms numbers: " + smsNumbers);

        Object[] msgs;
        if (pdusObj != null && (msgs = (Object[]) pdusObj.get(PDUS_NAME)) != null && msgs.length > 0) {
            Log.d(FTAG, format("pdus: %s", msgs.length));

            SmsMessage msg = null;
            String addr = null;
            final StringBuilder body = new StringBuilder();

            for (final Object one : msgs) {
                msg = SmsMessage.createFromPdu((byte[]) one);
                addr = msg.getOriginatingAddress();
                if (smsNumbers.contains(addr)) {
                    body.append(msg.getDisplayMessageBody());
                }
            }

            final String fullSmsBody = body.toString();
            if (!fullSmsBody.isEmpty()) {
                Log.d(FTAG, format("%s sms from %s: `%s`", msg.getTimestampMillis(), addr, fullSmsBody));

                Intent serviceIntent = new Intent(ACTION_NEW_TRANSACTION_SMS, null, context, FinancistoService.class);
                serviceIntent.putExtra(SMS_TRANSACTION_NUMBER, addr);
                serviceIntent.putExtra(SMS_TRANSACTION_BODY, fullSmsBody);
                FinancistoService.enqueueWork(context, serviceIntent);
            }
                // Display SMS message
                //                Toast.makeText(context, String.format("%s:%s", addr, body), Toast.LENGTH_SHORT).show();
        }

        // WARNING!!!
        // If you uncomment the next line then received SMS will not be put to incoming.
        // Be careful!
        // this.abortBroadcast();
    }
}
