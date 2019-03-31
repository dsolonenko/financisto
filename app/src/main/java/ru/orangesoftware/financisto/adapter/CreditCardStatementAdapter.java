package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static ru.orangesoftware.financisto.utils.MonthlyViewPlanner.*;

public class CreditCardStatementAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final int layout;
    private final List<TransactionInfo> transactions;

    private final Utils u;
    private final Currency currency;
    private final long account;

    private final int scheduledStyle = Typeface.ITALIC;
    private final int scheduledColor;
    private final int futureColor;
    private final int negativeColor;
    private final int normalStyle = Typeface.NORMAL;
    private final int normalColor = Color.LTGRAY;
    private final LayoutInflater inflater;

    private boolean isStatementPreview = false;

    /**
     * Create an adapter to display the expenses list of a credit card bill.
     *
     * @param context The context.
     * @param layout  The layout id.
     * @param cur     The credit card base currency.
     */
    public CreditCardStatementAdapter(Context context, int layout, List<TransactionInfo> transactions, Currency cur, long account) {
        this.context = context;
        this.layout = layout;
        this.transactions = transactions;
        this.u = new Utils(context);
        this.currency = cur;
        this.account = account;
        this.futureColor = context.getResources().getColor(R.color.future_color);
        this.scheduledColor = context.getResources().getColor(R.color.scheduled);
        this.negativeColor = context.getResources().getColor(R.color.negative_amount);
        this.inflater = LayoutInflater.from(context);
    }

    public boolean isStatementPreview() {
        return isStatementPreview;
    }

    public void setStatementPreview(boolean isStatementPreview) {
        this.isStatementPreview = isStatementPreview;
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    @Override
    public TransactionInfo getItem(int i) {
        return transactions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).id;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = newView(viewGroup);
        }
        Holder h = (Holder) view.getTag();
        updateListItem(h, i);
        return view;
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    public View newView(ViewGroup parent) {
        View v = inflater.inflate(layout, parent, false);
        Holder h = new Holder(v);
        v.setTag(h);
        return v;
    }

    private void updateListItem(Holder h, int i) {
        TransactionInfo t = getItem(i);

        if (isHeader(t)) {
            drawGroupTitle(context.getResources().getString(getHeaderTitle(t)), h);
            return;
        }

        // get amount of expense
        long value = t.fromAmount;
        // to consider correct value from transfers
        if (t.isTransfer() && t.toAccount.id == account) {
            value = t.toAmount;
        }

        // is scheduled?
        boolean isScheduled = t.isScheduled();

        // get columns values or needed parameters
        long date = t.dateTime;
        String note = t.note;
        String desc = "";
        boolean future = date > Calendar.getInstance().getTimeInMillis();

        /*
               * Set description:
               * a) if location is set, format description considering location
               *    - "Location (Note)"
               * b) otherwise, show description as note
               *    - "Note"
               */
        if (t.location != null && t.location.id > 0) {
            if (note != null && note.length() > 0) {
                desc = t.location.title + " (" + note + ")";
            } else {
                desc = t.location.title;
            }
        } else {
            desc = note;
        }

        // set expenses date, description and value to the respective columns
        TextView dateText = h.dateText;
        TextView descText = h.descText;
        TextView valueText = h.valueText;

        dateText.setText(getDate(date) + " ");
        descText.setText(desc);
        if (isStatementPreview) {
            u.setAmountText(valueText, currency, (-1) * value, false);
        } else {
            u.setAmountText(valueText, currency, value, false);
        }

        // set style
        if (isScheduled) {
            dateText.setTypeface(Typeface.defaultFromStyle(scheduledStyle), scheduledStyle);
            descText.setTypeface(Typeface.defaultFromStyle(scheduledStyle), scheduledStyle);
            valueText.setTypeface(Typeface.defaultFromStyle(scheduledStyle), scheduledStyle);
            dateText.setTextColor(scheduledColor);
            descText.setTextColor(scheduledColor);
            valueText.setTextColor(scheduledColor);
        } else {
            dateText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);
            descText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);
            valueText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);

            // set color
            if (future) {
                // future expenses
                dateText.setTextColor(futureColor);
                descText.setTextColor(futureColor);
                valueText.setTextColor(futureColor);
            } else {
                // normal
                dateText.setTextColor(normalColor);
                descText.setTextColor(normalColor);
                // display colored negative values in month preview, but not in bill preview
                if (value < 0 && !isStatementPreview) valueText.setTextColor(negativeColor);
                else valueText.setTextColor(normalColor);
            }
        }
    }

    private int getHeaderTitle(TransactionInfo t) {
        if (t == CREDITS_HEADER) {
            return R.string.header_credits;
        } else if (t == EXPENSES_HEADER) {
            return R.string.header_expenses;
        } else {
            return R.string.header_payments;
        }
    }

    private boolean isHeader(TransactionInfo t) {
        return t == CREDITS_HEADER || t == EXPENSES_HEADER || t == PAYMENTS_HEADER;
    }

    private void drawGroupTitle(String title, Holder h) {
        TextView dateText = h.dateText;
        TextView descText = h.descText;
        TextView valueText = h.valueText;
        dateText.setText("");
        descText.setText(title);
        valueText.setText("");
        dateText.setBackgroundColor(Color.DKGRAY);
        descText.setBackgroundColor(Color.DKGRAY);
        valueText.setBackgroundColor(Color.DKGRAY);
        descText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);
        descText.setTextColor(normalColor);
    }

    /**
     * TODO denis.solonenko: use locale specific DateFormat
     * Return the string for date in the following format: dd/MM/yy.
     *
     * @param date Time in milliseconds.
     * @return The string representing the given time in the format dd/MM/yy.
     */
    private String getDate(long date) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(date);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        int m = cal.get(Calendar.MONTH) + 1;
        int y = cal.get(Calendar.YEAR);
        return (d < 10 ? "0" + d : d) + "/" + (m < 10 ? "0" + m : m) + "/" + (y - 2000);
    }

    private class Holder {
        private final TextView dateText;
        private final TextView descText;
        private final TextView valueText;

        public Holder(View v) {
            dateText = (TextView) v.findViewById(R.id.list_date);
            descText = (TextView) v.findViewById(R.id.list_note);
            valueText = (TextView) v.findViewById(R.id.list_value);
        }
    }
}
