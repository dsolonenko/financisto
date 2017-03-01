package ru.orangesoftware.financisto.graph;

import java.util.Calendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.PeriodValue;
import android.content.Context;

/**
 * Point data to plot in Report 2D charts.
 * @author Abdsandryk
 */
public class Report2DPoint {
	
	protected PeriodValue pointData;
	
	// Point coordinates
	protected int x = 0;
	protected int y = 0;

	/**
	 * Default constructor.
	 * @param month The month of reference.
	 * @param value The result value in the reference month.
	 */
	public Report2DPoint(Calendar month, double value) {
		this.pointData = new PeriodValue(month, value);
	}
	
	/**
	 * Default constructor.
	 * @param pv PeriodValue object.
	 */
	public Report2DPoint(PeriodValue pv) {
		this.pointData = pv;
	}
	
	/**
	 * Gets the complete string representing the month.
	 * Ex.: August
	 * @param context The activity context.
	 * @return The complete string representing the month.
	 */
	public String getMonthLongString(Context context) {
		String[] monthName = {context.getResources().getString(R.string.month_x_jan),
							  context.getResources().getString(R.string.month_x_feb),
							  context.getResources().getString(R.string.month_x_mar),
							  context.getResources().getString(R.string.month_x_apr),
							  context.getResources().getString(R.string.month_x_mai),
							  context.getResources().getString(R.string.month_x_jun),
							  context.getResources().getString(R.string.month_x_jul),
							  context.getResources().getString(R.string.month_x_aug),
							  context.getResources().getString(R.string.month_x_sep),
							  context.getResources().getString(R.string.month_x_oct),
							  context.getResources().getString(R.string.month_x_nov),
							  context.getResources().getString(R.string.month_x_dec)};
		return monthName[pointData.getMonth().get(Calendar.MONTH)];
	}
	
	/**
	 * Gets the short string representing the month.
	 * Ex.: AUG
	 * @param context The activity context.
	 * @return The short string representing the month.
	 */
	public String getMonthShortString(Context context) {
		String[] monthName = {context.getResources().getString(R.string.month_jan),
							  context.getResources().getString(R.string.month_feb),
							  context.getResources().getString(R.string.month_mar),
							  context.getResources().getString(R.string.month_apr),
							  context.getResources().getString(R.string.month_mai),
							  context.getResources().getString(R.string.month_jun),
							  context.getResources().getString(R.string.month_jul),
							  context.getResources().getString(R.string.month_aug),
							  context.getResources().getString(R.string.month_sep),
							  context.getResources().getString(R.string.month_oct),
							  context.getResources().getString(R.string.month_nov),
							  context.getResources().getString(R.string.month_dec)};
		return monthName[pointData.getMonth().get(Calendar.MONTH)];
	}
	
	/**
	 * @return The string representing the year.
	 */
	public String getYearString() {
		return Integer.toString(pointData.getMonth().get(Calendar.YEAR));
	}

	/**
	 * @return The PeriodValue point data.
	 */
	public PeriodValue getPointData() {
		return pointData;
	}
	
	/**
	 * @return The horizontal position of the point in the screen.
	 */
	public float getX() {
		return x;
	}

	/**
	 * Sets the horizontal position of the point in the screen
	 * @param x
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * @return The vertical position of the point in the screen.
	 */
	public float getY() {
		return y;
	}

	/**
	 * Sets the vertical position of the point in the screen.
	 * @param y
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * @return True if the number is negative, false otherwise.
	 */
	public boolean isNegative() {
		if (pointData.getValue()<0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @return The result value in modulus.
	 */
	public double getAbsoluteValue() {
		return Math.abs(pointData.getValue());
	}
	
}