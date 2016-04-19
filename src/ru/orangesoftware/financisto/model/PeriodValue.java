package ru.orangesoftware.financisto.model;

import java.util.Calendar;

/**
 * Data that represents a result in a month.
 * @author Rodrigo Sousa
 */
public class PeriodValue { 
	/**
	 * The reference month.
	 */
	private Calendar month;
	
	/**
	 * The result value of the corresponding month.
	 */
	private double value;
	
	/**
	 * Default constructor.
	 * @param month The month of reference.
	 * @param value The result value in the given month.
	 */
	public PeriodValue(Calendar month, double value) {
		this.month = month;
		this.value = value;		
	}
	

	/**
	 * @return The reference month. 
	 */
	public Calendar getMonth() {
		return month;
	}


	/**
	 * @return The reference month in time milliseconds.
	 */
	public long getMonthTimeInMillis() {
		return month.getTimeInMillis();
	}


	/**
	 * @return The monthly result value.
	 */
	public double getValue() {
		return value;
	}

}
