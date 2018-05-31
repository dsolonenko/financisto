/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - implementing 2D chart reports
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.SummaryEntityListAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.graph.Report2DChart;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportType;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ReportsListActivity extends ListActivity {
	
	public static final String EXTRA_REPORT_TYPE = "reportType";
	
	private ReportType[] reports;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		reports = getReportsList();

		setContentView(R.layout.reports_list);		
		setListAdapter(new SummaryEntityListAdapter(this, reports));
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
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (reports[position].isConventionalBarReport()) {
			// Conventional Bars reports
			Intent intent = new Intent(this, ReportActivity.class);
			intent.putExtra(EXTRA_REPORT_TYPE, reports[position].name());
			startActivity(intent);
		} else {
			// 2D Chart reports
			Intent intent = new Intent(this, Report2DChartActivity.class);
			intent.putExtra(Report2DChart.REPORT_TYPE, reports[position].name());
			startActivity(intent);
		}
	}

	public static Report createReport(Context context, MyEntityManager em, Bundle extras) {
		String reportTypeName = extras.getString(EXTRA_REPORT_TYPE);
		ReportType reportType = ReportType.valueOf(reportTypeName);
        Currency c = em.getHomeCurrency();
		return reportType.createReport(context, c);
	}

	private ReportType[] getReportsList() {
		ArrayList<ReportType> reports = new ArrayList<>();

		reports.add(ReportType.BY_PERIOD);
		reports.add(ReportType.BY_CATEGORY);

		if (MyPreferences.isShowPayee(getBaseContext())) {
			reports.add(ReportType.BY_PAYEE);
		}

		if (MyPreferences.isShowLocation(getBaseContext())) {
			reports.add(ReportType.BY_LOCATION);
		}

		if (MyPreferences.isShowProject(getBaseContext())) {
			reports.add(ReportType.BY_PROJECT);
		}

		reports.add(ReportType.BY_ACCOUNT_BY_PERIOD);
		reports.add(ReportType.BY_CATEGORY_BY_PERIOD);

		if (MyPreferences.isShowPayee(getBaseContext())) {
			reports.add(ReportType.BY_PAYEE_BY_PERIOD);
		}

		if (MyPreferences.isShowLocation(getBaseContext())) {
			reports.add(ReportType.BY_LOCATION_BY_PERIOD);
		}

		if (MyPreferences.isShowProject(getBaseContext())) {
			reports.add(ReportType.BY_PROJECT_BY_PERIOD);
		}

		return reports.toArray(new ReportType[reports.size()]);
	}
}
