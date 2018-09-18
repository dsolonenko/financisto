/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.math.BigDecimal;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ReportAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.report.IncomeExpense;
import ru.orangesoftware.financisto.report.PeriodReport;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportData;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.Utils;

public class ReportActivity extends ListActivity implements RefreshSupportedActivity {

    protected static final int FILTER_REQUEST = 1;

    public static final String FILTER_INCOME_EXPENSE = "FILTER_INCOME_EXPENSE";
    
	private DatabaseAdapter db;
	private ImageButton bFilter;
    private ImageButton bToggle;
    private Report currentReport;
    private ReportAsyncTask reportTask;
	
	private WhereFilter filter = WhereFilter.empty();
    private boolean saveFilter = false;
    
    private IncomeExpense incomeExpenseState = IncomeExpense.BOTH;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.report);

        db = new DatabaseAdapter(this);
		db.open();

		bFilter = findViewById(R.id.bFilter);

		bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(ReportActivity.this, ReportFilterActivity.class);
            filter.toIntent(intent);
            startActivityForResult(intent, FILTER_REQUEST);
        });

        bToggle = findViewById(R.id.bToggle);
        bToggle.setOnClickListener(v -> toggleIncomeExpense());

        ImageButton bPieChart = findViewById(R.id.bPieChart);
        bPieChart.setOnClickListener(v -> showPieChart());

		Intent intent = getIntent();
		if (intent != null) {
            currentReport = ReportsListActivity.createReport(this, db, intent.getExtras());
            filter = WhereFilter.fromIntent(intent);
            if (intent.hasExtra(FILTER_INCOME_EXPENSE)) {
                incomeExpenseState = IncomeExpense.valueOf(intent.getStringExtra(FILTER_INCOME_EXPENSE));
            }
            if (filter.isEmpty()) {
                loadPrefsFilter();
            }
			selectReport();
		}

        applyFilter();
        applyIncomeExpense();
        showOrRemoveTotals();
	}

    private SharedPreferences getPreferencesForReport() {
        return getSharedPreferences("ReportActivity_"+currentReport.reportType.name()+"_DEFAULT", 0);
    }

    private void toggleIncomeExpense() {
        IncomeExpense[] values = IncomeExpense.values();
        int nextIndex = incomeExpenseState.ordinal() + 1;
        incomeExpenseState = nextIndex < values.length ? values[nextIndex] : values[0];
        applyIncomeExpense();
        saveFilter();
        selectReport();
    }

    private void applyIncomeExpense() {
        String reportTitle = getString(currentReport.reportType.titleId);
        String incomeExpenseTitle = getString(incomeExpenseState.getTitleId());
        setTitle(reportTitle+" ("+incomeExpenseTitle+")");
        bToggle.setImageDrawable(getResources().getDrawable(incomeExpenseState.getIconId()));
    }

    private void showPieChart() {
        new PieChartGeneratorTask().execute();
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

    private void showOrRemoveTotals() {
        if (!currentReport.shouldDisplayTotal()) {
            findViewById(R.id.total).setVisibility(View.GONE);
        }
    }

    private void applyAnimationToListView() {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(50);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(100);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
        ListView listView = getListView();
        listView.setLayoutAnimation(controller);
    }

    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (currentReport != null) {
			Intent intent = currentReport.createActivityIntent(this, db, WhereFilter.copyOf(filter), id);
			startActivity(intent);
		}
	}

	private void selectReport() {
        cancelCurrentReportTask();
        reportTask = new ReportAsyncTask(currentReport, incomeExpenseState);
        reportTask.execute();
	}

    private void cancelCurrentReportTask() {
        if (reportTask != null) {
            reportTask.cancel(true);
        }
    }

    private void applyFilter() {
        TextView tv = findViewById(R.id.period);
        if (currentReport instanceof PeriodReport) {
            disableFilter();
            tv.setVisibility(View.GONE);
        } else {
            Criteria c = filter.get(ReportColumns.DATETIME);
            if (c != null) {
                tv.setText(DateUtils.formatDateRange(this, c.getLongValue1(), c.getLongValue2(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
            } else {
                tv.setText(R.string.no_filter);
            }
            enableFilter();
            tv.setVisibility(View.VISIBLE);
        }
    }

    protected void disableFilter() {
        bFilter.setEnabled(false);
        //bFilter.setImageResource(R.drawable.ic_menu_filter_off);
    }

    protected void enableFilter() {
        bFilter.setEnabled(true);
        FilterState.updateFilterColor(this, filter, bFilter);
    }

    @Override
	protected void onDestroy() {
        cancelCurrentReportTask();
		db.close();
		super.onDestroy();
	}

	@Override
	public void recreateCursor() {
		selectReport();
	}

    @Override
    public void integrityCheck() {
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILTER_REQUEST) {
			if (resultCode == RESULT_FIRST_USER) {
				filter.clear();
                saveFilter();
                selectReport();
			} else if (resultCode == RESULT_OK) {
                filter = WhereFilter.fromIntent(data);
                saveFilter();
                selectReport();
			}
		}
	}

    private void saveFilter() {
        if (saveFilter) {
            SharedPreferences preferences = getPreferencesForReport();
            filter.toSharedPreferences(preferences);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(FILTER_INCOME_EXPENSE, incomeExpenseState.name());
            editor.apply();
        }
        applyFilter();
    }

    private void loadPrefsFilter() {
        SharedPreferences preferences = getPreferencesForReport();
        filter = WhereFilter.fromSharedPreferences(preferences);
        incomeExpenseState = IncomeExpense.valueOf(preferences.getString(FILTER_INCOME_EXPENSE, IncomeExpense.BOTH.name()));
        saveFilter = true;
    }

    private void displayTotal(Total total) {
        if (currentReport.shouldDisplayTotal()) {
            TextView totalText = findViewById(R.id.total);
            Utils u = new Utils(this);
            u.setTotal(totalText, total);
        }
    }

    private class ReportAsyncTask extends AsyncTask<Void, Void, ReportData> {

        private final Report report;
        private final IncomeExpense incomeExpense;

        private ReportAsyncTask(Report report, IncomeExpense incomeExpense) {
            this.report = report;
            this.incomeExpense = incomeExpense;
        }

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
            ((TextView)findViewById(android.R.id.empty)).setText(R.string.calculating);
        }

        @Override
        protected ReportData doInBackground(Void...voids) {
            report.setIncomeExpense(incomeExpense);
            return report.getReport(db, WhereFilter.copyOf(filter));
        }

        @Override
        protected void onPostExecute(ReportData data) {
            setProgressBarIndeterminateVisibility(false);
            displayTotal(data.total);
            ((TextView) findViewById(android.R.id.empty)).setText(R.string.empty_report);
            ReportAdapter adapter = new ReportAdapter(ReportActivity.this, data.units);
            setListAdapter(adapter);
            applyAnimationToListView();
        }

    }

    private class PieChartGeneratorTask extends AsyncTask<Void, Void, Intent> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Intent doInBackground(Void... voids) {
            return createPieChart();
        }

        private Intent createPieChart() {
            DefaultRenderer renderer = new DefaultRenderer();
            renderer.setLabelsTextSize(getResources().getDimension(R.dimen.report_labels_text_size));
            renderer.setLegendTextSize(getResources().getDimension(R.dimen.report_legend_text_size));
            renderer.setMargins(new int[] { 0, 0, 0, 0 });
            ReportData report = currentReport.getReportForChart(db, WhereFilter.copyOf(filter));
            CategorySeries series = new CategorySeries("AAA");
            long total = Math.abs(report.total.amount)+Math.abs(report.total.balance);
            int[] colors = generateColors(2*report.units.size());
            int i = 0;
            for (GraphUnit unit : report.units) {
                addSeries(series, renderer, unit.name, unit.getIncomeExpense().income, total, colors[i++]);
                addSeries(series, renderer, unit.name, unit.getIncomeExpense().expense, total, colors[i++]);
            }
            renderer.setZoomButtonsVisible(true);
            renderer.setZoomEnabled(true);
            renderer.setChartTitleTextSize(20);
            return ChartFactory.getPieChartIntent(ReportActivity.this, series, renderer, getString(R.string.report));
        }

        public int[] generateColors(int n) {
            int[] colors = new int[n];
            for (int i = 0; i < n; i++) {
                colors[i] = Color.HSVToColor(new float[]{360*(float)i/(float)n, .75f, .85f});
            }
            return colors;
        }

        private void addSeries(CategorySeries series, DefaultRenderer renderer, String name, BigDecimal expense, long total, int color) {
            long amount = expense.longValue();
            if (amount != 0 && total != 0) {
                long percentage = 100*Math.abs(amount)/total;
                series.add((amount > 0 ? "+" : "-") + name + "(" + percentage + "%)", percentage);
                SimpleSeriesRenderer r = new SimpleSeriesRenderer();
                r.setColor(color);
                renderer.addSeriesRenderer(r);
            }
        }

        @Override
        protected void onPostExecute(Intent intent) {
            setProgressBarIndeterminateVisibility(false);
            startActivity(intent);
        }

    }
    
}
