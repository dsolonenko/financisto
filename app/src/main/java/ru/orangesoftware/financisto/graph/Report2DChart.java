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
	protected int chartWidth;
	protected int chartHeight;
	
	protected int currentFilterOrder;
	protected String columnFilter;
	protected List<Long> filterIds;
	protected String noFilterMessage;
	protected Currency currency;
	
	protected Calendar startPeriod;
	protected int periodLength;
	private String[] periodStrings;
	private int[] periods;
	
	protected ReportDataByPeriod data;
	protected List<Report2DPoint> points;
	protected int selectedPoint;
	
	protected MyEntityManager em;
	protected Context context;

	/**
	 * Basic constructor
	 * @param em entity manager to query data from database
	 * @param periodLength The number of months to plot the chart
	 * @param currency The reference currency to filter transactions in same currency
	 */
	public Report2DChart(Context context, MyEntityManager em, int periodLength, Currency currency) {
		setDefaultStartPeriod(periodLength);
		init(context, em, startPeriod, periodLength, currency);
	}
	
	/**
	 * Constructor with a given first number
	 * @param em entity manager to query data from database
	 * @param startPeriod The first month of the period
	 * @param periodLength The number of months to plot the chart
	 * @param currency The reference currency to filter transactions in same currency
	 */
	public Report2DChart(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		init(context, em, startPeriod, periodLength, currency);
	}
	
	/**
	 * Constructor for children charts, identifying the level in the hierarchy (0 = root)
	 * @param em entity manager to query data from database
	 * @param startPeriod The first month of the period 
	 * @param periodLength The number of months to plot the chart
	 * @param currency The reference currency to filter transactions in same currency
	 * @param level The level in the hierarchy (0 = root)
	 */
	public Report2DChart(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency, int level) {
		init(context, em, startPeriod, periodLength, currency);
		this.level = level;
	}
	
	/**
	 * Rebuild data.
	 * @param context The activity context.
	 * @param em entity manager to query data from database
	 * @param startPeriod The first month of the period
	 * @param periodLength The number of months to plot the chart
	 * @param currency The reference currency to filter transactions in same currency
	 */
	public void rebuild(Context context, MyEntityManager em,  Calendar startPeriod, int periodLength, Currency currency) {
		init(context, em, startPeriod, periodLength, currency);
	}

	/**
	 * Sets the first month of the period when the start period is not given.
	 * Consider the chart for the LAST periodLength MONTHS. 
	 * @param periodLength The number of months to plot the chart
	 */
	private void setDefaultStartPeriod(int periodLength) {
		startPeriod = getDefaultStartPeriod(periodLength);
	}
	
	/**
	 * Set the first month of the report period.
	 * @param startPeriod The first month of the report period.
	 */
	public void setStartPeriod(Calendar startPeriod) {
		this.startPeriod = startPeriod;
	}
	
	/**
	 * Get the default start period based on the periodLength, considering the current month the last month of the period.
	 * @param periodLength Number of months to consider previous to the current month.
	 * @return The start period based on the periodLength, assuming as reference the current month. 
	 */
	public static Calendar getDefaultStartPeriod(int periodLength) {
		Calendar now = Calendar.getInstance();
		Calendar dsp = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
		// move to start period (reference month - <periodLength> months)
		dsp.add(Calendar.MONTH, (-1)*periodLength+1);
		return dsp;
	}
	
	/**
	 * Initialize parameters.
	 * @param startPeriod
	 * @param periodLength The number of months to plot the chart
	 * @param currency
	 */
	private void init(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		this.context = context;
		this.em = em;
		this.startPeriod = startPeriod;
		this.periodLength = periodLength;
		this.currency = currency;
		
		periods = new int[22];
    	periodStrings = new String[22];

    	for (int i=3; i<=24; i++) {
    		periods[i-3] = i;
    	}
		
		// classes shall implement to determine query filters
		setFilterIds();
		setColumnFilter();

		if (filterIds!=null && filterIds.size()>0) {
			// get data 
			currentFilterOrder = 0;
			build();
		} // alert message in activity - no filter available 
	}

	/**
	 * Gets the message to display when there is no filter to build chart data.
	 * @param context The activity context.
	 * @return The message to display when there is no filter to build chart data.
	 */
	public abstract String getNoFilterMessage(Context context);
	
	/**
	 * Required step (1) - set the resolution of the chart based on screen available space.
	 * @param height
	 * @param width
	 */
	public void setChartResolution(int height, int width) {
		this.chartHeight = height;
		this.chartWidth = width;
	}
	
	/**
	 * 
	 * @return The list of points to plot.
	 */
	public List<Report2DPoint> getPoints() {
		return points;
	}
	
	/**
	 * 
	 * @param location The order in list of points
	 * @return The given point
	 */
	public Report2DPoint getPoint(int location) {
		if (points!=null && points.size()>0) {
			if (location>=0 && location<points.size()) {
				return points.get(location);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @return
	 */
	public abstract List<Report2DChart> getChildrenCharts();
	
	/**
	 * 
	 * @return 
	 */
	public boolean isRoot() {
		return level==0;
	}
	
	/**
	 * @return
	 */
	public abstract String getFilterName();
	
	/**
	 * Move the cursor to next element of filters list, if not the last element.
	 */
	public boolean nextFilter() {
		if ((currentFilterOrder+1)<filterIds.size()) {
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
		if (currentFilterOrder>0) {
			currentFilterOrder--;
			build();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param periodLength
	 */
	public void changePeriodLength(int periodLength) {
		this.periodLength = periodLength;
		build();
	}
	
	/**
	 * @return The period length
	 */
	public int getPeriodLength() {
		return periodLength;
	}
	
	/**
	 * @return 
	 */
	public String getPeriodLengthString(Context context) {
		return getPeriodString(context, periodLength);
	}
	
	/**
	 * 
	 * @param currency
	 */
	public void changeCurrency(Currency currency) {
		this.currency = currency;
		build();
	}
	
	/**
	 * 
	 * @return The chart currency
	 */
	public Currency getCurrency() {
		return currency;
	}
	
	/**
	 * 
	 * @return The currency symbol
	 */
	public String getCurrencySymbol() {
		return currency.symbol;
	}
	
	/**
	 * Change the period reference by the first month of period given.
	 * @param startPeriod The first month of the chart period.
	 */
	public void changeStartPeriod(Calendar startPeriod) {
		this.startPeriod = startPeriod;
		build();
	}
	
	/**
	 * @return The first month of the chart period.
	 */
	public Calendar getStartPeriod() {
		return startPeriod;
	}
	
	/**
	 * Request data and fill data objects (list of points, max, min, etc.)
	 */
	protected void build() {
		data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, filterIds.get(currentFilterOrder).intValue(),em);
		points = new ArrayList<Report2DPoint>();
		List<PeriodValue> pvs = data.getPeriodValues();

		for (int i=0; i<pvs.size(); i++) {
			points.add(new Report2DPoint(pvs.get(i)));
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
	 * @return The Chart level. 
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * Access to query results, such as min, max, mean and sum.
	 * @return Object on which the data is stored
	 */
	public ReportDataByPeriod getDataBuilder() {
		return data;
	}
	
	/**
	 * Check if there is data to plot.
	 * @return True if there is data to plot or False if there is no points or if all the points have no value different of zero.
	 */
	public boolean hasDataToPlot() {
		if (data.getMaxValue()==data.getMinValue() && data.getMaxValue()==0) {
			//return false;
		}
		//return true;
		if (points!=null && points.size()>0) {
			for (int i=0; i<points.size(); i++) {
				if (points.get(i).getPointData().getValue()!=0) {
					return true;
				}
			}
		} // has no points
		return false;
		
	}
	
	/**
	 * Flag that indicates if is possible to filter data or not.
	 * @return True if the report data has a valid filter, false otherwise.
	 */
	public boolean hasFilter() {
		if (filterIds!=null && filterIds.size()>0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * The array of strings representing the periods.
	 * @param context The activity context.
	 * @return The array of strings representing the periods.
	 */
	public String[] getPeriodStrings(Context context) {
    	for (int i=3; i<=24; i++) {
    		periodStrings[i-3] = getPeriodString(context, i);
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
	 * @param context The activity context.
	 * @param months The number of months of the period.
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
