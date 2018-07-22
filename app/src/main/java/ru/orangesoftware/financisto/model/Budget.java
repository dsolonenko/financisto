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
package ru.orangesoftware.financisto.model;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;

import javax.persistence.*;
import java.util.Map;

import static ru.orangesoftware.financisto.db.DatabaseHelper.BUDGET_TABLE;
import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;
import static ru.orangesoftware.orb.EntityManager.DEF_ID_COL;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

@Entity
@Table(name = BUDGET_TABLE)
public class Budget implements SortableEntity {
	
	@Id
	@Column(name = DEF_ID_COL)
	public long id = -1;

	@Column(name = "title")
	public String title;
	
	@Column(name = "category_id")
	public String categories;
	
	@Column(name = "project_id")
	public String projects;

    @Column(name = "currency_id")
    public long currencyId = -1;

    @JoinColumn(name = "budget_currency_id", required = false)
	public Currency currency;

    @JoinColumn(name = "budget_account_id", required = false)
    public Account account;

    @Column(name = "amount")
	public long amount;
	
	@Column(name = "include_subcategories")
	public boolean includeSubcategories;
	
	@Column(name = "expanded")
	public boolean expanded;

	@Column(name = "include_credit")
	public boolean includeCredit = true;

	@Column(name = "start_date")
	public long startDate;
	
	@Column(name = "end_date")
	public long endDate;
	
	@Column(name = "recur")
	public String recur;
	
	@Column(name = "recur_num")
	public long recurNum;

	@Column(name = "is_current")
	public boolean isCurrent;

	@Column(name = "parent_budget_id")
	public long parentBudgetId;
	
	@Column(name = "updated_on")
	public long updatedOn = System.currentTimeMillis();
	 
	@Column(name = "remote_key")
 	public String remoteKey ;

	@Column(name = DEF_SORT_COL)
	public long sortOrder;

	@Transient
	public String categoriesText = "";

	@Transient
	public String projectsText = "";
	
	@Transient
	public long spent = 0;

	@Transient
	public volatile boolean updated = false;

	public Recur getRecur() {
		return RecurUtils.createFromExtraString(recur);
	}
	
	public static String createWhere(Budget b, Map<Long, Category> categories, Map<Long, Project> projects) {
		StringBuilder sb = new StringBuilder();
		// currency
        if (b.currency != null) {
		    sb.append(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID).append("=").append(b.currency.id);
        } else if (b.account != null) {
            sb.append(BlotterFilter.FROM_ACCOUNT_ID).append("=").append(b.account.id);
        } else {
            sb.append(" 1=1 ");
        }
		// categories & projects
		String categoriesWhere = createCategoriesWhere(b, categories);
		boolean hasCategories = isNotEmpty(categoriesWhere);
		String projectWhere = createProjectsWhere(b, projects);
		boolean hasProjects = isNotEmpty(projectWhere);
		if (hasCategories && hasProjects) {
			sb.append(" AND ((").append(categoriesWhere).append(") ");
			sb.append(b.expanded ? "OR" : "AND");
			sb.append(" (").append(projectWhere).append("))");
		} else if (hasCategories) {
			sb.append(" AND (").append(categoriesWhere).append(")");
		} else if (hasProjects) {
			sb.append(" AND (").append(projectWhere).append(")");
		}
		// start date
		if (b.startDate > 0) {
			sb.append(" AND ").append(BlotterFilter.DATETIME).append(">=").append(b.startDate);
		}
		// end date
		if (b.endDate > 0) {
			sb.append(" AND ").append(BlotterFilter.DATETIME).append("<=").append(b.endDate);
		}
		if (!b.includeCredit) {
			sb.append(" AND from_amount<0");
		}
		return sb.toString();
	}

	private static String createCategoriesWhere(Budget b, Map<Long, Category> categories) {
		long[] ids = MyEntity.splitIds(b.categories);
		if (ids != null) {
			StringBuilder sb = new StringBuilder();
			boolean f = false;
			for (long id : ids) {
				Category c = categories.get(id);
				if (c != null) {
					if (f) {
						sb.append(" OR ");
					}
					if (b.includeSubcategories) {
						sb.append("(").append(BlotterFilter.CATEGORY_LEFT).append(" BETWEEN ").append(c.left).append(" AND ").append(c.right).append(")");
					} else {
						sb.append(BlotterFilter.CATEGORY_ID).append("=").append(c.id);
					}
					f = true;
				}
			}
			if (f) {
				return sb.toString();
			}
		}
		return null;
	}

	private static String createProjectsWhere(Budget b, Map<Long, Project> projects) {
		long[] ids = MyEntity.splitIds(b.projects);
		if (ids != null) {
			StringBuilder sb = new StringBuilder();
			boolean f = false;
			for (long id : ids) {
				Project p = projects.get(id);
				if (p != null) {
					if (f) {
						sb.append(" OR ");
					}
					sb.append(BlotterFilter.PROJECT_ID).append("=").append(p.id);
					f = true;
				}
			}
			if (f) {
				return sb.toString();
			}
		}
		return null;
	}

    public Currency getBudgetCurrency() {
        return currency != null ? currency : (account != null ? account.currency : null);
    }

	@Override
	public long getSortOrder() {
		return sortOrder;
	}
}
