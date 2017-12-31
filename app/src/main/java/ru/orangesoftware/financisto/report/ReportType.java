/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - adding 2D chart reports
 ******************************************************************************/
package ru.orangesoftware.financisto.report;

import ru.orangesoftware.financisto.R;
import android.content.Context;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.SummaryEntityEnum;

public enum ReportType implements SummaryEntityEnum {

	BY_PERIOD(R.string.report_by_period, R.string.report_by_period_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Currency currency) {
			return new PeriodReport(context, currency);
		}
	},
	BY_CATEGORY(R.string.report_by_category, R.string.report_by_category_summary, R.drawable.report_icon_default){
        @Override
        public Report createReport(Context context, Currency currency) {
            return new CategoryReport(context, currency);
        }
	},
	BY_SUB_CATEGORY(R.string.report_by_category, R.string.report_by_category_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Currency currency) {
			return new SubCategoryReport(context, currency);
		}
	},
    BY_PAYEE(R.string.report_by_payee, R.string.report_by_payee_summary, R.drawable.report_icon_default){
        @Override
        public Report createReport(Context context, Currency currency) {
            return new PayeesReport(context, currency);
        }
    },
	BY_LOCATION(R.string.report_by_location, R.string.report_by_location_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Currency currency) {
			return new LocationsReport(context, currency);
		}
	},
	BY_PROJECT(R.string.report_by_project, R.string.report_by_project_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Currency currency) {
			return new ProjectsReport(context, currency);
		}
	}, 
	BY_ACCOUNT_BY_PERIOD(R.string.report_by_account_by_period, R.string.report_by_account_by_period_summary, R.drawable.actionbar_action_line_chart){
		@Override
		public Report createReport(Context context, Currency currency) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	}, 
	BY_CATEGORY_BY_PERIOD(R.string.report_by_category_by_period, R.string.report_by_category_by_period_summary, R.drawable.actionbar_action_line_chart){
		@Override
		public Report createReport(Context context, Currency currency) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	}, 
    BY_PAYEE_BY_PERIOD(R.string.report_by_payee_by_period, R.string.report_by_payee_by_period_summary, R.drawable.actionbar_action_line_chart){
        @Override
        public Report createReport(Context context, Currency currency) {
            return null;
        }

        @Override
        public boolean isConventionalBarReport() {
            return false;
        }
    },
	BY_LOCATION_BY_PERIOD(R.string.report_by_location_by_period, R.string.report_by_location_by_period_summary, R.drawable.actionbar_action_line_chart){
		@Override
		public Report createReport(Context context, Currency currency) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	}, 
	BY_PROJECT_BY_PERIOD(R.string.report_by_project_by_period, R.string.report_by_project_by_period_summary, R.drawable.actionbar_action_line_chart){
		@Override
		public Report createReport(Context context, Currency currency) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	};
	
	public final int titleId;
	public final int summaryId;
	public final int iconId;
	
	ReportType(int titleId, int summaryId, int iconId) {
		this.titleId = titleId;
		this.summaryId = summaryId;
		this.iconId = iconId;
	}

	@Override
	public int getTitleId() {
		return titleId;
	}

	@Override
	public int getSummaryId() {
		return summaryId;
	}

	@Override
	public int getIconId() {
		return iconId;
	}

	public boolean isConventionalBarReport() {
		return true;
	}
	
	public abstract Report createReport(Context context, Currency currency);

}
