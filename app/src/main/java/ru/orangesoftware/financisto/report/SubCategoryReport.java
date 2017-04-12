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
package ru.orangesoftware.financisto.report;

import android.content.Context;
import android.database.Cursor;
import ru.orangesoftware.financisto.activity.BlotterActivity;
import ru.orangesoftware.financisto.activity.SplitsBlotterActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.db.*;
import ru.orangesoftware.financisto.graph.GraphStyle;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.CategoryTree.NodeCreator;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_REPORT_SUB_CATEGORY;

public class SubCategoryReport extends Report {
	
    private final GraphStyle[] styles = new GraphStyle[3];

	public SubCategoryReport(Context context, Currency currency) {
		super(ReportType.BY_CATEGORY, context, currency);
        createStyles(context);
	}

    private void createStyles(Context context) {
        styles[0] = new GraphStyle.Builder(context).dy(2).textDy(5).lineHeight(30).nameTextSize(14).amountTextSize(12).indent(0).build();
        styles[1] = new GraphStyle.Builder(context).dy(2).textDy(5).lineHeight(20).nameTextSize(12).amountTextSize(10).indent(10).build();
        styles[2] = new GraphStyle.Builder(context).dy(2).textDy(5).lineHeight(20).nameTextSize(12).amountTextSize(10).indent(30).build();
    }

    @Override
	public ReportData getReport(final DatabaseAdapter db, WhereFilter filter) {
		filterTransfers(filter);
		Cursor c = db.db().query(V_REPORT_SUB_CATEGORY, DatabaseHelper.SubCategoryReportColumns.NORMAL_PROJECTION,
				filter.getSelection(), filter.getSelectionArgs(), null, null,
                DatabaseHelper.SubCategoryReportColumns.LEFT);
        final ExchangeRateProvider rates = db.getHistoryRates();
        try {
            final int leftColumnIndex = c.getColumnIndex(DatabaseHelper.SubCategoryReportColumns.LEFT);
            CategoryTree<CategoryAmount> amounts = CategoryTree.createFromCursor(c, new NodeCreator<CategoryAmount>(){
                @Override
                public CategoryAmount createNode(Cursor c) {
                    BigDecimal amount;
                    try {
                        amount = TransactionsTotalCalculator.getAmountFromCursor(db, c, currency, rates, c.getColumnIndex(DatabaseHelper.ReportColumns.DATETIME));
                    } catch (UnableToCalculateRateException e) {
                        amount = BigDecimal.ZERO;
                    }
                    return new CategoryAmount(c, leftColumnIndex, amount);
                }
            });

            ArrayList<GraphUnitTree> roots = createTree(amounts, 0);
            ArrayList<GraphUnit> units = new ArrayList<GraphUnit>();
            flattenTree(roots, units);
            Total total = calculateTotal(roots);
            return new ReportData(units, total);
        } finally {
            c.close();
        }
	}

    @Override
    public ReportData getReportForChart(DatabaseAdapter db, WhereFilter filter) {
        ReportData data = super.getReportForChart(db, filter);
        if (data.units.size() > 1) {
            //remove first unit which is parent category
            data.units.remove(0);
        }
        return data;
    }

    private ArrayList<GraphUnitTree> createTree(CategoryTree<CategoryAmount> amounts, int level) {
		ArrayList<GraphUnitTree> roots = new ArrayList<GraphUnitTree>();
		GraphUnitTree u = null;
		long lastId = -1;
		for (CategoryAmount a : amounts) {
			if (u == null || lastId != a.id) {
				u = new GraphUnitTree(a.id, a.title, currency, getStyle(level));
				roots.add(u);
				lastId = a.id;
			}
			u.addAmount(a.amount, skipTransfers && a.isTransfer != 0);
			if (a.hasChildren()) {
				u.setChildren(createTree(a.children, level+1));
				u = null;				
			}
		}
        Iterator<GraphUnitTree> i = roots.iterator();
        while (i.hasNext()) {
            GraphUnitTree root = i.next();
            root.flatten(incomeExpense);
            if (root.size() == 0) {
                i.remove();
            }
        }
		Collections.sort(roots);
		return roots;
	}

	private void flattenTree(List<GraphUnitTree> tree, List<GraphUnit> units) {
		for (GraphUnitTree t : tree) {
			units.add(t);
			if (t.hasChildren()) {
				flattenTree(t.children, units);
				t.setChildren(null);
			}
		}
	}
	
	private GraphStyle getStyle(int level) {
		return styles[Math.min(2, level)];
	}

	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		Category c = db.getCategoryWithParent(id);
		return Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(c.left), String.valueOf(c.right));
	}

    @Override
    protected Class<? extends BlotterActivity> getBlotterActivityClass() {
        return SplitsBlotterActivity.class;
    }

	private static class CategoryAmount extends CategoryEntity<CategoryAmount> {
		
		private final BigDecimal amount;
        private final int isTransfer;

        public CategoryAmount(Cursor c, int leftColumnIndex, BigDecimal amount) {
			this.id = c.getLong(0);
            this.title = c.getString(1);
			this.amount = amount;
            this.left = c.getInt(leftColumnIndex);
            this.right = c.getInt(leftColumnIndex+1);
            this.isTransfer = c.getInt(leftColumnIndex+2);
		}

	}
	
	private static class GraphUnitTree extends GraphUnit {

		public List<GraphUnitTree> children;
		
		public GraphUnitTree(long id, String name, Currency currency, GraphStyle style) {
			super(id, name, currency, style);
		}
		
		public void setChildren(List<GraphUnitTree> children) {
			this.children = children;
		}
		
		public boolean hasChildren() {
			return children != null && children.size() > 0;
		}

        @Override
        public void flatten(IncomeExpense incomeExpense) {
            super.flatten(incomeExpense);
            if (children != null) {
                for (GraphUnitTree child : children) {
                    child.flatten(incomeExpense);
                }
                Collections.sort(children);
            }
        }
    }

}

