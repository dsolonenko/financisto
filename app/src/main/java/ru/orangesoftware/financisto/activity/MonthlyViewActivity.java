package ru.orangesoftware.financisto.activity;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.CreditCardStatementAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.utils.MonthlyViewPlanner;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.TransactionList;
import ru.orangesoftware.financisto.utils.Utils;

/**
 * Display the credit card bill, including scheduled and future transactions for a given period.
 * Display only expenses, ignoring payments (positive values) in Credit Card accounts.
 *
 * @author Abdsandryk
 */
public class MonthlyViewActivity extends ListActivity {

    public static final String ACCOUNT_EXTRA = "account_id";
    public static final String BILL_PREVIEW_EXTRA = "bill_preview";

    private DatabaseAdapter db;

    private long accountId = 0;
    private Account account;
    private Currency currency;
    private boolean isCreditCard = false;
    private boolean isStatementPreview = false;

    private String title;
    private int closingDay = 0;
    private int paymentDay = 0;

    private int month = 0;
    private int year = 0;
    private Calendar closingDate;

    private Utils u;

    private MonthlyPreviewTask currentTask;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monthly_view);

        u = new Utils(this);

        Intent intent = getIntent();
        if (intent != null) {
            accountId = intent.getLongExtra(ACCOUNT_EXTRA, 0);
            isStatementPreview = intent.getBooleanExtra(BILL_PREVIEW_EXTRA, false);
        }
        initialize();
        popupMenu();
    }

    @Override
    public void onDestroy() {
        cancelCurrentTask();
        db.close();
        super.onDestroy();
    }

    private void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
    }

    /**
     * Initialize data and GUI elements.
     */
    private void initialize() {

        // get account data
        db = new DatabaseAdapter(this);
        db.open();

        // set currency based on account
        account = db.getAccount(accountId);

        if (month == 0 && year == 0) {
            // get current month and year in first launch
            Calendar cal = Calendar.getInstance();
            month = cal.get(Calendar.MONTH) + 1;
            year = cal.get(Calendar.YEAR);
        }

        // set part of the title, based on account name: "<CCARD> Bill"
        if (account != null) {

            // get account type
            isCreditCard = AccountType.valueOf(account.type).isCreditCard;

            currency = account.currency;

            if (isCreditCard) {
                if (isStatementPreview) {
                    // assuming that expensesOnly is true only if payment and closing days > 0 [BlotterActivity]
                    title = getString(R.string.ccard_statement_title);
                    String accountTitle = account.title;
                    if (account.title == null || account.title.length() == 0) {
                        accountTitle = account.cardIssuer;
                    }
                    String toReplace = getString(R.string.ccard_par);
                    title = title.replaceAll(toReplace, accountTitle);
                    paymentDay = account.paymentDay;
                    closingDay = account.closingDay;
                    // set activity window title
                    this.setTitle(R.string.ccard_statement);
                    setCCardTitle();
                    setCCardInterval();
                } else {
                    title = (account.title == null || account.title.length() == 0 ? account.cardIssuer : account.title);
                    paymentDay = 1;
                    closingDay = 31;
                    setTitle();
                    setInterval();

                    // set payment date and label on total bar
                    TextView totalLabel = (TextView) findViewById(R.id.monthly_result_label);
                    totalLabel.setText(getResources().getString(R.string.monthly_result));
                }
            } else {
                if (account.title == null || account.title.length() == 0) {
                    if (isCreditCard) {
                        // title = <CARD_ISSUER>
                        title = account.cardIssuer;
                    } else {
                        // title = <ACCOUNT_TYPE_TITLE>
                        AccountType type = AccountType.valueOf(account.type);
                        title = getString(type.titleId);
                    }
                } else {
                    // title = <TITLE>
                    title = account.title;
                }

                paymentDay = 1;
                closingDay = 31;
                setTitle();
                setInterval();

                // set payment date and label on total bar
                TextView totalLabel = (TextView) findViewById(R.id.monthly_result_label);
                totalLabel.setText(getResources().getString(R.string.monthly_result));
            }

            ImageButton bPrevious = (ImageButton) findViewById(R.id.bt_month_previous);
            bPrevious.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    month--;
                    if (month < 1) {
                        month = 12;
                        year--;
                    }
                    if (isCreditCard) {
                        if (isStatementPreview) {
                            setCCardTitle();
                            setCCardInterval();
                        } else {
                            setTitle();
                            setInterval();
                        }
                    } else {
                        setTitle();
                        setInterval();
                    }
                }
            });

            ImageButton bNext = (ImageButton) findViewById(R.id.bt_month_next);
            bNext.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    month++;
                    if (month > 12) {
                        month = 1;
                        year++;
                    }
                    if (isCreditCard) {
                        if (isStatementPreview) {
                            setCCardTitle();
                            setCCardInterval();
                        } else {
                            setTitle();
                            setInterval();
                        }
                    } else {
                        setTitle();
                        setInterval();
                    }
                }
            });

        }
    }


    /**
     * Configure the interval based on the bill period of a credit card.
     * Attention:
     * Calendar.MONTH = 0 to 11
     * integer  month = 1 to 12
     */
    private void setCCardInterval() {

        Calendar close = getClosingDate(month, year);
        Calendar open;

        if (month > 1) {
            // closing date from previous month
            open = getClosingDate(month - 1, year);
        } else {
            open = getClosingDate(12, year - 1);
        }
        // add one day to the closing date of previous month
        open.add(Calendar.DAY_OF_MONTH, +1);

        // adjust time for closing day
        close.set(Calendar.HOUR_OF_DAY, 23);
        close.set(Calendar.MINUTE, 59);
        close.set(Calendar.SECOND, 59);

        this.closingDate = new GregorianCalendar(close.get(Calendar.YEAR),
                close.get(Calendar.MONTH),
                close.get(Calendar.DAY_OF_MONTH));

        // Verify custom closing date
        int periodKey = Integer.parseInt(Integer.toString(close.get(Calendar.MONTH)) +
                Integer.toString(close.get(Calendar.YEAR)));

        int cd = db.getCustomClosingDay(accountId, periodKey);
        if (cd > 0) {
            // use custom closing day
            close.set(Calendar.DAY_OF_MONTH, cd);
        }

        // Verify custom opening date = closing day of previous month + 1
        periodKey = Integer.parseInt(Integer.toString(open.get(Calendar.MONTH)) +
                Integer.toString(open.get(Calendar.YEAR)));

        int od = db.getCustomClosingDay(accountId, periodKey);
        if (od > 0) {
            // use custom closing day
            open.set(Calendar.DAY_OF_MONTH, od);
            open.add(Calendar.DAY_OF_MONTH, +1);
        }

        fillData(open, close);
    }

    private void popupMenu() {
        final ImageButton bMenu = (ImageButton) findViewById(R.id.bt_popup);
        if (isStatementPreview) {
            bMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(MonthlyViewActivity.this, bMenu);
                    MenuInflater inflater = getMenuInflater();
                    inflater.inflate(R.menu.statement_preview_menu, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            onPopupMenuSelected(item.getItemId());
                            return true;
                        }
                    });
                    popupMenu.show();
                }
            });
        } else {
            bMenu.setVisibility(View.GONE);
        }
    }

    public void onPopupMenuSelected(int id) {
        switch (id) {
            case R.id.opt_menu_closing_day:
                // call credit card closing day sending period
                Intent intent = new Intent(this, CCardStatementClosingDayActivity.class);
                int closingDay = getClosingDate(month, year).get(Calendar.DAY_OF_MONTH);
                intent.putExtra(CCardStatementClosingDayActivity.PERIOD_MONTH, closingDate.get(Calendar.MONTH));
                intent.putExtra(CCardStatementClosingDayActivity.PERIOD_YEAR, closingDate.get(Calendar.YEAR));
                intent.putExtra(CCardStatementClosingDayActivity.ACCOUNT, accountId);
                intent.putExtra(CCardStatementClosingDayActivity.REGULAR_CLOSING_DAY, closingDay);
                startActivityForResult(intent, 16);
        }
    }

    /**
     * Configure the interval in a monthly perspective.
     * Attention:
     * Calendar.MONTH = 0 to 11
     * integer  month = 1 to 12
     */
    private void setInterval() {

        Calendar close = new GregorianCalendar(year, month - 1, getLastDayOfMonth(month, year));
        Calendar open = new GregorianCalendar(year, month - 1, 1);

        // adjust time for closing day
        close.set(Calendar.HOUR_OF_DAY, 23);
        close.set(Calendar.MINUTE, 59);
        close.set(Calendar.SECOND, 59);
        close.set(Calendar.MILLISECOND, 999);

        fillData(open, close);
    }

    // Returns the day on which the credit card bill closes for a given month/year.
    private Calendar getClosingDate(int month, int year) {
        int m = month;
        if (closingDay > paymentDay) {
            m--;
        }
        int maxDay = getLastDayOfMonth(m, year);
        int day = closingDay;
        if (closingDay > maxDay) {
            day = maxDay;
        }

        return new GregorianCalendar(year, m - 1, day);
    }

    private int getLastDayOfMonth(int month, int year) {
        Calendar calCurr = GregorianCalendar.getInstance();
        calCurr.set(year, month - 1, 1); // Months are 0 to 11
        return calCurr.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
    }

    private class MonthlyPreviewTask extends AsyncTask<Void, Void, TransactionList> {

        private final Date open;
        private final Date close;
        private final Date now;

        private MonthlyPreviewTask(Date open, Date close, Date now) {
            this.open = open;
            this.close = close;
            this.now = now;
        }

        @Override
        protected void onPreExecute() {
            ((TextView) findViewById(android.R.id.empty)).setText(R.string.calculating);
        }

        @Override
        protected TransactionList doInBackground(Void... voids) {
            MonthlyViewPlanner planner = new MonthlyViewPlanner(db, account, isStatementPreview, open, close, now);
            TransactionList transactions;
            if (isStatementPreview) {
                transactions = planner.getCreditCardStatement();
            } else {
                transactions = planner.getPlannedTransactionsWithTotals();
            }
            return transactions;
        }

        @Override
        protected void onPostExecute(TransactionList monthlyPreviewReport) {
            List<TransactionInfo> transactions = monthlyPreviewReport.transactions;
            long total = monthlyPreviewReport.totals[0].balance;
            if (transactions == null || transactions.isEmpty()) {
                displayNoTransactions();
            } else { // display data

                // Mapping data to view
                CreditCardStatementAdapter expenses = new CreditCardStatementAdapter(MonthlyViewActivity.this, R.layout.credit_card_transaction, transactions, currency, accountId);
                expenses.setStatementPreview(isStatementPreview);
                setListAdapter(expenses);

                // calculate total
                // display total
                TextView totalText = (TextView) findViewById(R.id.monthly_result);
                if (isStatementPreview) {
                    u.setAmountText(totalText, currency, (-1) * total, false);
                    totalText.setTextColor(Color.BLACK);
                } else {
                    if (total < 0) {
                        u.setAmountText(totalText, currency, (-1) * total, false);
                        u.setNegativeColor(totalText);
                    } else {
                        u.setAmountText(totalText, currency, total, false);
                        u.setPositiveColor(totalText);
                    }
                }
            }
        }
    }

    /**
     * Get data for a given period and display the related credit card expenses.
     *
     * @param open  Start of period.
     * @param close End of period.
     */
    private void fillData(Calendar open, Calendar close) {
        cancelCurrentTask();
        currentTask = new MonthlyPreviewTask(open.getTime(), close.getTime(), new Date());
        currentTask.execute();
    }

    private void displayNoTransactions() {
        TextView totalText = (TextView) findViewById(R.id.monthly_result);
        // display total = 0
        u.setAmountText(totalText, currency, 0, false);
        totalText.setTextColor(Color.BLACK);
        // hide list and display empty message
        ((TextView) findViewById(android.R.id.empty)).setText(R.string.no_transactions);
        setListAdapter(null);
    }

    /**
     * Adjust the title based on the credit card's payment day.
     */
    private void setCCardTitle() {

        Calendar date = new GregorianCalendar(year, month - 1, paymentDay);

        String monthStr = Integer.toString(date.get(Calendar.MONTH) + 1);
        String yearStr = Integer.toString(date.get(Calendar.YEAR));

        String paymentDayStr;
        if (paymentDay < 10) {
            paymentDayStr = "0" + paymentDay;
        } else {
            paymentDayStr = Integer.toString(paymentDay);
        }

        if (monthStr.length() < 2) {
            monthStr = "0" + monthStr;
        }

        String pd = paymentDayStr + "/" + monthStr + "/" + yearStr;

        // set payment date and label on title bar
        TextView label = (TextView) findViewById(R.id.monthly_view_title);
        label.setText(title + "\n" + pd);
        // set payment date and label on total bar
        TextView totalLabel = (TextView) findViewById(R.id.monthly_result_label);
        totalLabel.setText(getResources().getString(R.string.bill_on) + " " + pd);
    }

    /**
     * Adjust the title based on the credit card's payment day.
     */
    private void setTitle() {

        Calendar date = new GregorianCalendar(year, month - 1, 1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy");
        String pd = dateFormat.format(date.getTime());

        TextView label = (TextView) findViewById(R.id.monthly_view_title);
        label.setText(title + "\n" + pd);

    }

    // Update view
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                int update = data.getIntExtra(CCardStatementClosingDayActivity.UPDATE_VIEW, 0);
                if (update > 0) {
                    setCCardTitle();
                    setCCardInterval();
                }
                break;
            case RESULT_CANCELED:
                break;
        }
    }

}
