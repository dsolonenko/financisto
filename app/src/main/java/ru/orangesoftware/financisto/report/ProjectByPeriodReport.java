package ru.orangesoftware.financisto.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.graph.Report2DChart;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.utils.MyPreferences;

import android.content.Context;

/**
 * 2D Chart Report to display monthly results by Projects.
 *
 * @author Abdsandryk
 */
public class ProjectByPeriodReport extends Report2DChart {

    public ProjectByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
        super(context, em, startPeriod, periodLength, currency);
    }

    /* (non-Javadoc)
     * @see ru.orangesoftware.financisto.graph.ReportGraphic2D#getFilterName()
     */
    @Override
    public String getFilterName() {
        if (filterIds.size() > 0) {
            long projectId = filterIds.get(currentFilterOrder);
            Project project = em.getProject(projectId);
            if (project != null) {
                return project.getTitle();
            } else {
                return context.getString(R.string.no_project);
            }
        } else {
            // no project
            return context.getString(R.string.no_project);
        }
    }

    @Override
    public void setFilterIds() {
        boolean includeNoProject = MyPreferences.includeNoFilterInReport(context);
        filterIds = new ArrayList<Long>();
        currentFilterOrder = 0;
        ArrayList<Project> projects = em.getAllProjectsList(includeNoProject);
        if (projects.size() > 0) {
            Project p;
            for (int i = 0; i < projects.size(); i++) {
                p = projects.get(i);
                filterIds.add(p.getId());
            }
        }
    }

    @Override
    protected void setColumnFilter() {
        columnFilter = TransactionColumns.project_id.name();
    }

    @Override
    public String getNoFilterMessage(Context context) {
        return context.getString(R.string.report_no_project);
    }
}
