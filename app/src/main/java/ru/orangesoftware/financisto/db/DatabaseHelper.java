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
package ru.orangesoftware.financisto.db;

import android.content.Context;
import org.androidannotations.annotations.EBean;
import ru.orangesoftware.financisto.utils.EnumUtils;
import static ru.orangesoftware.financisto.utils.EnumUtils.asStringArray;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

@EBean(scope = EBean.Scope.Singleton)
public class DatabaseHelper extends DatabaseSchemaEvolution {

    public DatabaseHelper(Context context) {
        super(context, Database.DATABASE_NAME, null, Database.DATABASE_VERSION);
        setAutoDropViews(true);
    }

    @Override
    protected String getViewNameFromScriptName(String scriptFileName) {
        int x = scriptFileName.indexOf('[');
        int y = scriptFileName.indexOf(']');
        if (x != -1 && y != -1 && y - x > 1) {
            return scriptFileName.substring(x + 1, y);
        }
        return null;
    }

    public static final String TRANSACTION_TABLE = "transactions";
    public static final String ACCOUNT_TABLE = "account";
    public static final String CURRENCY_TABLE = "currency";
    public static final String CATEGORY_TABLE = "category";
    public static final String BUDGET_TABLE = "budget";
    public static final String PROJECT_TABLE = "project";
    public static final String ATTRIBUTES_TABLE = "attributes";
    public static final String SMS_TEMPLATES_TABLE = "sms_template";
    public static final String CATEGORY_ATTRIBUTE_TABLE = "category_attribute";
    public static final String TRANSACTION_ATTRIBUTE_TABLE = "transaction_attribute";
    public static final String LOCATIONS_TABLE = "locations";
    public static final String PAYEE_TABLE = "payee";
    public static final String CCARD_CLOSING_DATE_TABLE = "ccard_closing_date";
    public static final String EXCHANGE_RATES_TABLE = "currency_exchange_rate";

    public static final String V_ALL_TRANSACTIONS = "v_all_transactions";
    public static final String V_BLOTTER = "v_blotter";
    public static final String V_BLOTTER_FLAT_SPLITS = "v_blotter_flatsplits";
    public static final String V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS = "v_blotter_for_account_with_splits";
    public static final String V_CATEGORY = "v_category";
    public static final String V_ATTRIBUTES = "v_attributes";
    public static final String V_REPORT_CATEGORY = "v_report_category";
    public static final String V_REPORT_SUB_CATEGORY = "v_report_sub_category";
    public static final String V_REPORT_PERIOD = "v_report_period";
    public static final String V_REPORT_LOCATIONS = "v_report_location";
    public static final String V_REPORT_PROJECTS = "v_report_project";
    public static final String V_REPORT_PAYEES = "v_report_payee";

    public static enum TransactionColumns {
        _id,
        parent_id,
        from_account_id,
        to_account_id,
        category_id,
        project_id,
        payee_id,
        note,
        from_amount,
        to_amount,
        datetime,
        original_currency_id,
        original_from_amount,
        location_id,
        provider,
        accuracy,
        latitude,
        longitude,
        is_template,
        template_name,
        recurrence,
        notification_options,
        status,
        attached_picture,
        is_ccard_payment,
        last_recurrence,
        blob_key;

        public static String[] NORMAL_PROJECTION = asStringArray(TransactionColumns.values());

    }

    public static enum BlotterColumns {
        _id,
        parent_id,
        from_account_id,
        from_account_title,
        from_account_currency_id,
        to_account_id,
        to_account_title,
        to_account_currency_id,
        category_id,
        category_title,
        category_left,
        category_right,
        category_type,
        project_id,
        project,
        location_id,
        location,
        payee_id,
        payee,
        note,
        from_amount,
        to_amount,
        datetime,
        original_currency_id,
        original_from_amount,
        is_template,
        template_name,
        recurrence,
        notification_options,
        status,
        is_ccard_payment,
        attached_picture,
        last_recurrence,
        from_account_balance,
        to_account_balance,
        is_transfer;

        public static final String[] NORMAL_PROJECTION = asStringArray(BlotterColumns.values());

        public static final String[] BALANCE_PROJECTION = {
                from_account_currency_id.name(),
                "SUM(" + from_amount + ")"
        };

        public static final String BALANCE_GROUP_BY = "FROM_ACCOUNT_CURRENCY_ID";
    }

    public static class AccountColumns {

        public static final String ID = "_id";
        public static final String TITLE = "title";
        public static final String CREATION_DATE = "creation_date";
        public static final String CURRENCY_ID = "currency_id";
        public static final String TYPE = "type";
        public static final String ISSUER = "issuer";
        public static final String NUMBER = "number";
        public static final String TOTAL_AMOUNT = "total_amount";
        public static final String SORT_ORDER = DEF_SORT_COL;
        public static final String LAST_CATEGORY_ID = "last_category_id";
        public static final String LAST_ACCOUNT_ID = "last_account_id";
        public static final String CLOSING_DAY = "closing_day";
        public static final String PAYMENT_DAY = "payment_day";
        public static final String IS_INCLUDE_INTO_TOTALS = "is_include_into_totals";
        public static final String IS_ACTIVE = "is_active";
        public static final String LAST_TRANSACTION_DATE = "last_transaction_date";

        private AccountColumns() {
        }

    }

    public static enum CategoryColumns {
        _id,
        title,
        left,
        right,
        type,
        last_location_id,
        last_project_id,
        sort_order
    }

    public static enum CategoryViewColumns {
        _id,
        title,
        level,
        left,
        right,
        type,
        last_location_id,
        last_project_id,
        sort_order;

        public static String[] NORMAL_PROJECTION = asStringArray(CategoryViewColumns.values());
    }

    public static enum ExchangeRateColumns {
        from_currency_id,
        to_currency_id,
        rate_date,
        rate;

        public static String[] NORMAL_PROJECTION = asStringArray(ExchangeRateColumns.values());
        public static String[] LATEST_RATE_PROJECTION = new String[]{
                from_currency_id.name(),
                to_currency_id.name(),
                "max(" + rate_date + ")",
                rate.name()
        };
        public static String DELETE_CLAUSE = from_currency_id + "=? and " + to_currency_id + "=? and " + rate_date + "=?";
        public static String LATEST_RATE_GROUP_BY = from_currency_id + "," + to_currency_id;
        public static String NORMAL_PROJECTION_WHERE = from_currency_id + "=? and " + to_currency_id + "=? and " + rate_date + "=?";
    }

    public static class EntityColumns {

        public static final String ID = "_id";
        public static final String TITLE = "title";

    }

    public static class AttributeColumns {

        public static final String ID = "_id";
        public static final String TITLE = "title";
        public static final String TYPE = "type";
        public static final String LIST_VALUES = "list_values";
        public static final String DEFAULT_VALUE = "default_value";

        public static final String[] NORMAL_PROJECTION = {
                ID,
                TITLE,
                TYPE,
                LIST_VALUES,
                DEFAULT_VALUE
        };

        public static class Indicies {
            public static final int ID = 0;
            public static final int TITLE = 1;
            public static final int TYPE = 2;
            public static final int LIST_VALUES = 3;
            public static final int DEFAULT_VALUE = 4;
        }

    }

    public static class AttributeViewColumns {

        public static final String TITLE = "title";
        public static final String CATEGORY_ID = "category_id";
        public static final String CATEGORY_LEFT = "category_left";
        public static final String CATEGORY_RIGHT = "category_right";

        public static final String[] NORMAL_PROJECTION = {
                CATEGORY_ID,
                TITLE
        };

        public static class Indicies {
            public static final int CATEGORY_ID = 0;
            public static final int NAME = 1;
        }
    }

    public static class CategoryAttributeColumns {
        public static final String ATTRIBUTE_ID = "attribute_id";
        public static final String CATEGORY_ID = "category_id";
    }

    public enum SmsTemplateColumns {
        _id,
        title,
        template,
        category_id,
        account_id,
        is_income,
        sort_order;

        public static final String[] NORMAL_PROJECTION = EnumUtils.asStringArray(SmsTemplateColumns.values());
    }

    public enum SmsTemplateListColumns {
        cat_name,
        cat_level
    }

    public static class TransactionAttributeColumns {
        public static final String ATTRIBUTE_ID = "attribute_id";
        public static final String TRANSACTION_ID = "transaction_id";
        public static final String VALUE = "value";

        public static final String[] NORMAL_PROJECTION = {
                ATTRIBUTE_ID,
                TRANSACTION_ID,
                VALUE
        };

        public static class Indicies {
            public static final int ATTRIBUTE_ID = 0;
            public static final int TRANSACTION_ID = 1;
            public static final int VALUE = 2;
        }

    }

    public static class ReportColumns {

        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String DATETIME = "datetime";
        public static final String FROM_ACCOUNT_CURRENCY_ID = "from_account_currency_id";
        public static final String FROM_AMOUNT = "from_amount";
        public static final String TO_ACCOUNT_CURRENCY_ID = "to_account_currency_id";
        public static final String TO_AMOUNT = "to_amount";
        public static final String ORIGINAL_CURRENCY_ID = "original_currency_id";
        public static final String ORIGINAL_FROM_AMOUNT = "original_from_amount";
        public static final String IS_TRANSFER = "is_transfer";

        public static String[] NORMAL_PROJECTION = {ID, NAME, DATETIME, FROM_ACCOUNT_CURRENCY_ID, FROM_AMOUNT, TO_ACCOUNT_CURRENCY_ID, TO_AMOUNT, ORIGINAL_CURRENCY_ID, ORIGINAL_FROM_AMOUNT, IS_TRANSFER};

    }

    public static class SubCategoryReportColumns extends ReportColumns {

        public static final String LEFT = "left";
        public static final String RIGHT = "right";

        public static String[] NORMAL_PROJECTION = {ID, NAME, DATETIME, FROM_ACCOUNT_CURRENCY_ID, FROM_AMOUNT, TO_ACCOUNT_CURRENCY_ID, TO_AMOUNT, ORIGINAL_CURRENCY_ID, ORIGINAL_FROM_AMOUNT, LEFT, RIGHT, IS_TRANSFER};
    }

    public static class LocationColumns {
        public static final String ID = "_id";
        public static final String TITLE = "title";
        public static final String DATETIME = "datetime";
        public static final String PROVIDER = "provider";
        public static final String ACCURACY = "accuracy";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String IS_PAYEE = "is_payee";
        public static final String RESOLVED_ADDRESS = "resolved_address";
    }


    public static class CreditCardClosingDateColumns {

        public static final String ACCOUNT_ID = "account_id";
        // Period key in database (MMYYYY), where MM = 0 to 11
        public static final String PERIOD = "period";
        public static final String CLOSING_DAY = "closing_day";

    }

    public static class deleteLogColumns {
        public static final String TABLE_NAME = "table_name";
        public static final String DELETED_ON = "deleted_on";
    }

}
