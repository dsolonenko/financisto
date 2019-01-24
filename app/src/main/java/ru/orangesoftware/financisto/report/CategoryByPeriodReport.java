/**
 *
 */
package ru.orangesoftware.financisto.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.graph.Report2DChart;
import ru.orangesoftware.financisto.graph.Report2DPoint;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.PeriodValue;
import ru.orangesoftware.financisto.model.ReportDataByPeriod;
import ru.orangesoftware.financisto.utils.MyPreferences;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * 2D Chart Report to display monthly results by Categories.
 *
 * @author Abdsandryk
 */
public class CategoryByPeriodReport extends Report2DChart {

    public CategoryByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
        super(context, em, startPeriod, periodLength, currency);
    }

    @Override
    public String getFilterName() {
        if (filterIds.size() > 0) {
            long categoryId = filterIds.get(currentFilterOrder);
            Category category = em.getCategory(categoryId);
            if (category != null) {
                return category.getTitle();
            } else {
                return context.getString(R.string.no_category);
            }
        } else {
            // no category
            return context.getString(R.string.no_category);
        }
    }

    @Override
    public void setFilterIds() {
        boolean includeSubCategories = MyPreferences.includeSubCategoriesInReport(context);
        boolean includeNoCategory = MyPreferences.includeNoFilterInReport(context);
        filterIds = new ArrayList<Long>();
        currentFilterOrder = 0;
        List<Category> categories = em.getAllCategoriesList(includeNoCategory);
        if (categories.size() > 0) {
            Category c;
            for (Category category : categories) {
                if (includeSubCategories) {
                    filterIds.add(category.getId());
                } else {
                    // do not include sub categories
                    if (category.level == 1) {
                        // filter root categories only
                        filterIds.add(category.getId());
                    }
                }
            }
        }
    }

    @Override
    protected void setColumnFilter() {
        columnFilter = TransactionColumns.category_id.name();
    }

    /**
     * Request data and fill data objects (list of points, max, min, etc.)
     */
    @Override
    protected void build() {
        boolean addSubs = MyPreferences.addSubCategoriesToSum(context);
        if (addSubs) {
            SQLiteDatabase db = em.db();
            Cursor cursor = null;
            try {
                long categoryId = filterIds.get(currentFilterOrder);
                Category parent = em.getCategory(categoryId);
                String where = CategoryColumns.left + " BETWEEN ? AND ?";
                String[] pars = new String[]{String.valueOf(parent.left), String.valueOf(parent.right)};
                cursor = db.query(DatabaseHelper.CATEGORY_TABLE, new String[]{CategoryColumns._id.name()}, where, pars, null, null, null);
                int[] categories = new int[cursor.getCount() + 1];
                int i = 0;
                while (cursor.moveToNext()) {
                    categories[i] = (int) cursor.getInt(0);
                    i++;
                }
                categories[i] = filterIds.get(currentFilterOrder).intValue();
                data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, categories, em);
            } finally {
                if (cursor != null) cursor.close();
            }
        } else {
            // only root category
            data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, filterIds.get(currentFilterOrder).intValue(), em);
        }

        points = new ArrayList<Report2DPoint>();
        List<PeriodValue> pvs = data.getPeriodValues();

        for (PeriodValue pv : pvs) {
            points.add(new Report2DPoint(pv));
        }
    }

    @Override
    public String getNoFilterMessage(Context context) {
        return context.getString(R.string.report_no_category);
    }

}
