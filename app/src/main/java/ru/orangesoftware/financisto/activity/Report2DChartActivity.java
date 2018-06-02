package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.graph.Report2DChart;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.ReportDataByPeriod;
import ru.orangesoftware.financisto.report.AccountByPeriodReport;
import ru.orangesoftware.financisto.report.CategoryByPeriodReport;
import ru.orangesoftware.financisto.report.LocationByPeriodReport;
import ru.orangesoftware.financisto.report.PayeeByPeriodReport;
import ru.orangesoftware.financisto.report.ProjectByPeriodReport;
import ru.orangesoftware.financisto.report.ReportType;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.Report2DChartView;

/**
 * Activity to display 2D Reports.
 *
 * @author Abdsandryk
 */
public class Report2DChartActivity extends Activity {

    // activity result identifier to get results back
    public static final int REPORT_PREFERENCES = 1;

    // Data to display
    private Report2DChart reportData;
    private DatabaseAdapter db;

    private int selectedPeriod;
    private Currency currency;
    private Calendar startPeriod;
    private ReportType reportType;

    // array of string report preferences to identify changes
    String[] initialPrefs;

    // boolean to check if preferred currency is set
    private boolean prefCurNotSet = false;
    // boolean to check if preferred period is set
    private boolean prefPerNotSet = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // initialize activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_2d);

        // get report type
        Intent intent = getIntent();
        if (intent != null) {
            reportType = ReportType.valueOf(intent.getStringExtra(Report2DChart.REPORT_TYPE));
        }
        init();
    }

    /**
     * Initialize activity.
     */
    private void init() {
        // database adapter to query data
        db = new DatabaseAdapter(this);
        db.open();

        // get report preferences to display chart
        // Reference Currency
        currency = getReferenceCurrency();
        // Period of Reference
        int periodLength = getPeriodOfReference();
        selectedPeriod = periodLength - 3;

        // check report preferences for reference month different of current month
        setStartPeriod(periodLength);

        boolean built = false;
        switch (reportType) {
            case BY_ACCOUNT_BY_PERIOD:
                reportData = new AccountByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_CATEGORY_BY_PERIOD:
                reportData = new CategoryByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_PAYEE_BY_PERIOD:
                reportData = new PayeeByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_LOCATION_BY_PERIOD:
                reportData = new LocationByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_PROJECT_BY_PERIOD:
                reportData = new ProjectByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
        }

        if (reportData.hasFilter()) {
            refreshView();
            built = true;
        } else {
            //  There is no <location, project or category> available for filtering data.
            alertNoFilter(reportData.getNoFilterMessage(this));
            adjustLabels();
        }

        if (built && (prefCurNotSet || prefPerNotSet)) {
            alertPreferencesNotSet(prefCurNotSet, prefPerNotSet);
        }

        // previous filter button
        ImageButton bPrevious = (ImageButton) findViewById(R.id.bt_filter_previous);
        bPrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (reportData.previousFilter()) {
                    refreshView();
                }
            }
        });

        // next filter button
        ImageButton bNext = (ImageButton) findViewById(R.id.bt_filter_next);
        bNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (reportData.nextFilter()) {
                    refreshView();
                }
            }
        });

        // prefs
        ImageButton bPrefs = (ImageButton) findViewById(R.id.bt_preferences);
        bPrefs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showPreferences();
            }
        });

        // period length
        findViewById(R.id.report_period).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // pop up options to choose the period
                changePeriodLength(selectedPeriod);
            }
        });
        findViewById(R.id.report_period).setFocusable(true);

    }

    /**
     * Display a message when preferences not set to alert the use of default values.
     *
     * @param isCurrency Inform if currency is not set on report preferences.
     * @param isPeriod   Inform if period is not set on report preferences.
     */
    private void alertPreferencesNotSet(boolean isCurrency, boolean isPeriod) {
        // display message: preferences not set
        String message = "";
        if (isCurrency) {
            if (isPeriod) {
                // neither currency neither period is set
                message = getResources().getString(R.string.report_preferences_not_set);
            } else {
                // only currency not set
                message = getResources().getString(R.string.currency_not_set);
            }
        } else {
            if (isPeriod) {
                // only period not set
                message = getResources().getString(R.string.period_not_set);
            }
        }
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(R.string.reports);
        dlgAlert.setPositiveButton(R.string.ok, null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    /**
     * Alert message to warn that there is no filter available (no category, no project, no account or no location)
     *
     * @param message Message warning the lack of filters by report type.
     */
    private void alertNoFilter(String message) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(R.string.reports);
        dlgAlert.setPositiveButton(R.string.ok, null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    /**
     * Display a list of period length options to redefine period, rebuild data and refresh view.
     *
     * @param previousPeriod The previous selected period to check if data changed, rebuild data and refresh view.
     */
    private void changePeriodLength(final int previousPeriod) {
        final Context context = this;
        new AlertDialog.Builder(this)
                .setTitle(R.string.period)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        processPeriodLengthChange(previousPeriod, true);
                    }
                })
                .setSingleChoiceItems(reportData.getPeriodStrings(context), selectedPeriod, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedPeriod = which;
                    }
                })
                .show();
    }

    /**
     * Process the period length change
     *
     * @param previousPeriod The selected period length before changing
     * @param refresh        True if requires refresh, false if refresh will be processed later
     */
    private void processPeriodLengthChange(int previousPeriod, boolean refresh) {
        if (previousPeriod != selectedPeriod) {
            reportData.changePeriodLength(reportData.getPeriodOptions()[selectedPeriod]);
            setStartPeriod(reportData.getPeriodOptions()[selectedPeriod]);
            reportData.changeStartPeriod(startPeriod);
            if (refresh) refreshView();
        }
    }

    /**
     * Update the view reflecting data changes
     */
    private void refreshView() {
        // set data to plot
        if (reportData.hasDataToPlot()) {
            findViewById(R.id.report_empty).setVisibility(View.GONE);
            Report2DChartView view = ((Report2DChartView) findViewById(R.id.report_2d_chart));
            view.setDataToPlot(reportData.getPoints(),
                    reportData.getDataBuilder().getMaxValue(),
                    reportData.getDataBuilder().getMinValue(),
                    reportData.getDataBuilder().getAbsoluteMaxValue(),
                    reportData.getDataBuilder().getAbsoluteMinValue(),
                    reportData.getDataBuilder().getMeanExcludingNulls());
            ((Report2DChartView) findViewById(R.id.report_2d_chart)).setCurrency(currency);
            findViewById(R.id.report_2d_chart).setVisibility(View.VISIBLE);
            // set labels
            view.refresh();
        } else {
            findViewById(R.id.report_empty).setVisibility(View.VISIBLE);
            findViewById(R.id.report_2d_chart).setVisibility(View.GONE);
        }
        // adjust report 2D user interface elements
        adjustLabels();
        fillStatistics();
    }

    /**
     * Adjust labels after changing report parameters
     */
    private void adjustLabels() {
        // Filter name
        ((TextView) findViewById(R.id.report_filter_name)).setText(reportData.getFilterName());
        // Period
        ((TextView) findViewById(R.id.report_period)).setText(reportData.getPeriodLengthString(this));
    }

    /**
     * Fill statistics panel based on report data
     */
    private void fillStatistics() {
        boolean considerNull = MyPreferences.considerNullResultsInReport(this);
        Double max;
        Double min;
        Double mean;
        Double sum = reportData.getDataBuilder().getSum();
        if (considerNull) {
            max = reportData.getDataBuilder().getMaxValue();
            min = reportData.getDataBuilder().getMinValue();
            mean = reportData.getDataBuilder().getMean();
            if ((min * max >= 0)) {
                // absolute calculation (all points over the x axis)
                max = reportData.getDataBuilder().getAbsoluteMaxValue();
                min = reportData.getDataBuilder().getAbsoluteMinValue();
                mean = Math.abs(mean);
                sum = Math.abs(sum);
            }
        } else {
            // exclude impact of null values in statistics
            max = reportData.getDataBuilder().getMaxExcludingNulls();
            min = reportData.getDataBuilder().getMinExcludingNulls();
            mean = reportData.getDataBuilder().getMeanExcludingNulls();
            if ((min * max >= 0)) {
                // absolute calculation (all points over the x axis)
                max = reportData.getDataBuilder().getAbsoluteMaxExcludingNulls();
                min = reportData.getDataBuilder().getAbsoluteMinExcludingNulls();
                mean = Math.abs(mean);
                sum = Math.abs(sum);
            }
        }
        // chart limits
        ((TextView) findViewById(R.id.report_max_result)).setText(Utils.amountToString(reportData.getCurrency(), max.longValue()));
        ((TextView) findViewById(R.id.report_min_result)).setText(Utils.amountToString(reportData.getCurrency(), min.longValue()));
        // sum and mean
        ((TextView) findViewById(R.id.report_mean_result)).setText(Utils.amountToString(reportData.getCurrency(), mean.longValue()));
        ((TextView) findViewById(R.id.report_mean_result)).setTextColor(Report2DChartView.meanColor);
        ((TextView) findViewById(R.id.report_sum_result)).setText(Utils.amountToString(reportData.getCurrency(), sum.longValue()));
    }

    /**
     * Gets the reference currency registered on preferences or, if not registered, gets the default currency.
     *
     * @return The currency registered as a reference to display chart reports or the default currency if not configured yet.
     */
    private Currency getReferenceCurrency() {
        Currency c = MyPreferences.getReferenceCurrency(this);
        if (c == null) {
            prefCurNotSet = true;
            Collection<Currency> currencies = CurrencyCache.getAllCurrencies();
            if (currencies != null && currencies.size() > 0) {
                for (Currency currency : currencies) {
                    if (currency.isDefault) {
                        c = currency;
                        break;
                    }
                }
                if (c == null) {
                    c = getNewDefaultCurrency();
                }
            } else {
                c = getNewDefaultCurrency();
            }
        }
        return c;
    }

    /**
     * Gets default currency when currency is not set in report preferences.
     *
     * @return Default currency
     */
    private Currency getNewDefaultCurrency() {
        return Currency.defaultCurrency();
    }

    private void showPreferences() {
        // save preferences status before call report preferences activity
        initialPrefs = MyPreferences.getReportPreferences(this);
        // call report preferences activity asking for result when closed
        Intent intent = new Intent(this, ReportPreferencesActivity.class);
        startActivityForResult(intent, REPORT_PREFERENCES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
        if (initialPrefs != null) {
            boolean changed = preferencesChanged(initialPrefs, MyPreferences.getReportPreferences(this));
            if (changed) {
                // rebuild data
                reportData.rebuild(this, db, startPeriod, reportData.getPeriodOptions()[selectedPeriod], currency);
                refreshView();
            }
        }
    }

    /**
     * Check if preferences changed
     *
     * @param initial Preferences status before call Report Preferences Activity.
     * @param actual  Current preferences status.
     * @return True if preferences changed, false otherwise.
     */
    private boolean preferencesChanged(String[] initial, String[] actual) {
        boolean changed = false;
        // general report preferences
        // 0 reference currency
        if (!initial[0].equals(actual[0])) {
            // set reference currency
            currency = getReferenceCurrency();
            changed = true;
        }
        // 1 period of reference
        if (!initial[1].equals(actual[1])) {
            // change period length to the one set in report preferences
            int refPeriodLength = getPeriodOfReference();
            int previousPeriod = selectedPeriod;
            selectedPeriod = refPeriodLength - 3;
            processPeriodLengthChange(previousPeriod, false);
            changed = true;
        }
        // 2 reference month
        if (!initial[2].equals(actual[2])) {
            setStartPeriod(reportData.getPeriodOptions()[selectedPeriod]);
            changed = true;
        }
        // 3 consider nulls in statistics (affects statistics only > recalculate)
        if (!initial[3].equals(actual[3])) {
            // affects statistics only - recalculate
            changed = true;
        }
        // 4 include <no filter> (rebuild will regenerate the filter Ids list)
        if (!initial[4].equals(actual[4])) {
            // the change will be processed in rebuild
            changed = true;
        }

        if (reportType == ReportType.BY_CATEGORY_BY_PERIOD) {
            // include sub categories in list (rebuild will regenerate the filter Ids list)
            if (!initial[5].equals(actual[5])) {
                // the change will be processed in rebuild
                changed = true;
            }
            // add sub categories result to root categories result (affects statistics only > recalculate)
            if (!initial[6].equals(actual[6])) changed = true;
        }
        return changed;
    }

    /**
     * Set the start period based on given period length and reference month registered in report preferences.
     * Start period = Reference Month - periodLength months
     *
     * @param periodLength The number of months to be represented in the 2D report.
     */
    private void setStartPeriod(int periodLength) {
        int refMonth = MyPreferences.getReferenceMonth(this);
        Calendar now = Calendar.getInstance();
        startPeriod = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
        if (refMonth != 0) {
            startPeriod.add(Calendar.MONTH, refMonth);
        }
        // move to start period (reference month - <periodLength> months)
        startPeriod.add(Calendar.MONTH, (-1) * periodLength + 1);
    }

    /**
     * Get the period of reference set in report preferences.
     *
     * @return The number of months to be represented in the 2D report.
     */
    private int getPeriodOfReference() {
        int periodLength = MyPreferences.getPeriodOfReference(this);
        if (periodLength == 0) {
            periodLength = ReportDataByPeriod.DEFAULT_PERIOD;
            prefPerNotSet = true;
        }
        return periodLength;
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
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
}
