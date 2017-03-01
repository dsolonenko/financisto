package ru.orangesoftware.financisto.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.graph.Report2DChart;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.content.Context;

/**
 * 2D Chart Report to display monthly results by Locations.
 * @author Abdsandryk
 */
public class LocationByPeriodReport extends Report2DChart {
	
	/**
	 * Default constructor.
	 * @param dbAdapter
	 * @param context
	 * @param periodLength
	 * @param currency
	 */
	public LocationByPeriodReport(Context context, MyEntityManager em, int periodLength, Currency currency) {
		super(context, em, periodLength, currency);
	}
	
	/**
	 * Default constructor.
	 * @param context
	 * @param dbAdapter
	 * @param startPeriod
	 * @param periodLength
	 * @param currency
	 */
	public LocationByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	/* (non-Javadoc)
	 * @see ru.orangesoftware.financisto.graph.ReportGraphic2D#getChildrenGraphics()
	 */
	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	/* (non-Javadoc)
	 * @see ru.orangesoftware.financisto.graph.ReportGraphic2D#getFilterName()
	 */
	@Override
	public String getFilterName() {
		if (filterIds.size()>0) {
			long locationId = filterIds.get(currentFilterOrder);
			MyLocation location = em.get(MyLocation.class, locationId);
			if (location != null) {
				return location.name;
			} else {
				return context.getString(R.string.current_location);
			}
		} else {
			// no location
			return context.getString(R.string.current_location);
		}
	}

	/* (non-Javadoc)
	 * @see ru.orangesoftware.financisto.graph.ReportGraphic2D#setFilterIds()
	 */
	@Override
	public void setFilterIds() {
		boolean includeNoLocation = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<Long>();
		currentFilterOrder = 0;
		List<MyLocation> locations = em.getAllLocationsList(includeNoLocation);
		if (locations.size()>0) {
			MyLocation l;
			for (int i=0; i<locations.size(); i++) {
				l = locations.get(i);
				filterIds.add(l.id);
			}
		}
	}

	@Override
	protected void setColumnFilter() {
		columnFilter = TransactionColumns.location_id.name();
	}
	
	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_location);
	}

}
