package ru.orangesoftware.financisto.graph;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.PeriodValue;
import ru.orangesoftware.financisto.model.ReportDataByPeriod;

import android.content.Context;

public abstract class Report2DChart {

    public static final String REPORT_TYPE = "report_type";

    protected int level = 0;

    protected int currentFilterOrder = 0;
    protected String columnFilter = "";
    protected List<Long> filterIds = new ArrayList<>();

    protected Currency currency;

    protected Calendar startPeriod;
    protected int periodLength;
    private String[] periodStrings;
    private int[] periods;

    protected ReportDataByPeriod data;
    protected List<Report2DPoint> points;

    protected MyEntityManager em;
    protected Context context;

    /**
     * Basic constructor
     *
     * @param em           entity manager to query data from database
     * @param periodLength The number of months to plot the chart
     * @param currency     The reference currency to filter transactions in same currency
     */
    public Report2DChart(Context context, MyEntityManager em, int periodLength, Currency currency) {
        setDefaultStartPeriod(periodLength);
        init(context, em, startPeriod, periodLength, currency);
    }

    /**
     * Constructor with a given first number
     *
     * @param em           entity manager to query data from database
     * @param startPeriod  The first month of the period
     * @param periodLength The number of months to plot the chart
     * @param currency     The reference currency to filter transactions in same currency
     */
    public Report2DChart(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
        init(context, em, startPeriod, periodLength, currency);
    }

    /**
     * Rebuild data.
     *
     * @param context      The activity context.
     * @param em           entity manager to query data from database
     * @param startPeriod  The first month of the period
     * @param periodLength The number of months to plot the chart
     * @param currency     The reference currency to filter transactions in same currency
     */
    public void rebuild(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
        init(context, em, startPeriod, periodLength, currency);
    }

    /**
     * Sets the first month of the period when the start period is not given.
     * Consider the chart for the LAST periodLength MONTHS.
     *
     * @param periodLength The number of months to plot the chart
     */
    private void setDefaultStartPeriod(int periodLength) {
        startPeriod = getDefaultStartPeriod(periodLength);
    }

    /**
     * Get the default start period based on the periodLength, considering the current month the last month of the period.
     *
     * @param periodLength Number of months to consider previous to the current month.
     * @return The start period based on the periodLength, assuming as reference the current month.
     */
    public static Calendar getDefaultStartPeriod(int periodLength) {
        Calendar now = Calendar.getInstance();
        Calendar dsp = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
        // move to start period (reference month - <periodLength> months)
        dsp.add(Calendar.MONTH, (-1) * periodLength + 1);
        return dsp;
    }

    private void init(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
        this.context = context;
        this.em = em;
        this.startPeriod = startPeriod;
        this.periodLength = periodLength;
        this.currency = currency;

        periods = new int[22];
        periodStrings = new String[22];

        for (int i = 3; i <= 24; i++) {
            periods[i - 3] = i;
        }

        // classes shall implement to determine query filters
        setFilterIds();
        setColumnFilter();

        // get data
        currentFilterOrder = 0;
        build();
    }

    /**
     * Gets the message to display when there is no filter to build chart data.
     *
     * @param context The activity context.
     * @return The message to display when there is no filter to build chart data.
     */
    public abstract String getNoFilterMessage(Context context);

    /**
     * @return The list of points to plot.
     */
    public List<Report2DPoint> getPoints() {
        return points;
    }

    public abstract String getFilterName();

    /**
     * Move the cursor to next element of filters list, if not the last element.
     */
    public boolean nextFilter() {
        if ((currentFilterOrder + 1) < filterIds.size()) {
            currentFilterOrder++;
            build();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Move the pointer to previous element of filters list, if not the first element.
     */
    public boolean previousFilter() {
        if (currentFilterOrder > 0) {
            currentFilterOrder--;
            build();
            return true;
        } else {
            return false;
        }
    }

    public void changePeriodLength(int periodLength) {
        this.periodLength = periodLength;
        build();
    }

    public String getPeriodLengthString(Context context) {
        return getPeriodString(context, periodLength);
    }

    /**
     * @return The chart currency
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Change the period reference by the first month of period given.
     *
     * @param startPeriod The first month of the chart period.
     */
    public void changeStartPeriod(Calendar startPeriod) {
        this.startPeriod = startPeriod;
        build();
    }

    /**
     * Request data and fill data objects (list of points, max, min, etc.)
     */
    protected void build() {
        if (filterIds != null && filterIds.size() > 0) {
            data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, filterIds.get(currentFilterOrder).intValue(), em);
            points = new ArrayList<>();
            List<PeriodValue> pvs = data.getPeriodValues();

            for (int i = 0; i < pvs.size(); i++) {
                points.add(new Report2DPoint(pvs.get(i)));
            }
        }
    }

    /**
     * Set the name of Transaction column to filter on chart
     */
    protected abstract void setColumnFilter();

    /**
     * Fill filterIds with the list of Filter Object ids.
     * Ex.: In Category report, fill with category ids.
     */
    public abstract void setFilterIds();

    /**
     * Required when displaying a chart and its sub-elements.
     * Ex.: Category - level 0 (root). Sub-categories - level 1..n (children charts).
     *
     * @return The Chart level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Access to query results, such as min, max, mean and sum.
     *
     * @return Object on which the data is stored
     */
    public ReportDataByPeriod getDataBuilder() {
        return data;
    }

    /**
     * Check if there is data to plot.
     *
     * @return True if there is data to plot or False if there is no points or if all the points have no value different of zero.
     */
    public boolean hasDataToPlot() {
        if (points != null && points.size() > 0) {
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).getPointData().getValue() != 0) {
                    return true;
                }
            }
        } // has no points
        return false;

    }

    /**
     * Flag that indicates if is possible to filter data or not.
     *
     * @return True if the report data has a valid filter, false otherwise.
     */
    public boolean hasFilter() {
        return filterIds != null && filterIds.size() > 0;
    }

    /**
     * The array of strings representing the periods.
     *
     * @param context The activity context.
     * @return The array of strings representing the periods.
     */
    public String[] getPeriodStrings(Context context) {
        for (int i = 3; i <= 24; i++) {
            periodStrings[i - 3] = getPeriodString(context, i);
        }
        return periodStrings;
    }

    /**
     * @return The array of period options.
     */
    public int[] getPeriodOptions() {
        return periods;
    }

    /**
     * Get the string that represents the periods.
     *
     * @param context The activity context.
     * @param months  The number of months of the period.
     * @return The string representing the given period.
     */
    private String getPeriodString(Context context, int months) {
        switch (months) {
            case ReportDataByPeriod.LAST_QUARTER_PERIOD:
                return context.getString(R.string.report_last_quarter);
            case ReportDataByPeriod.LAST_HALF_YEAR_PERIOD:
                return context.getString(R.string.report_last_half_year);
            case ReportDataByPeriod.LAST_9_MONTHS_PERIOD:
                return context.getString(R.string.report_last_9_months);
            case ReportDataByPeriod.LAST_YEAR_PERIOD:
                return context.getString(R.string.report_last_year);
            default:
                String n = context.getString(R.string.report_n_months_var);
                return context.getString(R.string.report_last_n_months).replace(n, Integer.toString(months));
        }
    }
}
