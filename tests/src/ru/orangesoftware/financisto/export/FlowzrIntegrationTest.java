/*
 * Copyright (c) 2011 Denis Solonenko, Emmanuel Florent
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.test.InstrumentationTestCase;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncEngine;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncTask;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.orb.EntityManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;


import static java.lang.String.*;

public class FlowzrIntegrationTest extends InstrumentationTestCase {

    //dev_appserver on this configuration doest not support 127.1 !
    private final static String LAN_IP="10.42.0.32";
    private final static String TEST_ACCOUNT = "colmedis@gmail.com";

    protected Context context;
    public static DefaultHttpClient http_client;
    private static DatabaseAdapter dba;
    public EntityManager em;
    private SQLiteDatabase db;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        FlowzrSyncEngine.FLOWZR_BASE_URL = "http://" + LAN_IP + ":8080/";
        FlowzrSyncEngine.FLOWZR_API_URL = "http://" + LAN_IP + ":8080/financisto3/";

        context = getInstrumentation().getTargetContext();

        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpGet request = getAuthenticateRequest("/");

        this.http_client = new DefaultHttpClient();
        this.dba = new DatabaseAdapter(context);

        FlowzrSyncEngine.dba = this.dba;
        FlowzrSyncEngine.db = this.dba.db();
        FlowzrSyncEngine.em = this.dba.em();

        FlowzrSyncEngine.nsString = TEST_ACCOUNT.replace("@","_");

        FlowzrSyncEngine.http_client = this.http_client;

        FlowzrSyncEngine.last_sync_ts = 1;
        FlowzrSyncEngine.resetLastTime(context);
        FlowzrSyncEngine.isCanceled = false;
        FlowzrSyncEngine.isRunning = false;
        FlowzrSyncEngine.startTimestamp = System.currentTimeMillis();
        db=dba.db();
        em = dba.em();

    }

    public void test_get_android_account() {
        MyPreferences.setFlowzrAccount(context, TEST_ACCOUNT);
        String accountName = MyPreferences.getFlowzrAccount(context);
        assertEquals(TEST_ACCOUNT, accountName);
        android.accounts.Account a = FlowzrSyncTask.getAndroidAccount(context);
        assertEquals(TEST_ACCOUNT, a.name);
    }

    public void test_push_accounts() throws Exception {
        if (dba.em().getAllAccounts().getCount()<1) {
            Currency c = createCurrency("CZK");
            ru.orangesoftware.financisto.model.Account a = new ru.orangesoftware.financisto.model.Account();
            a.title = "My Bank Account";
            a.type = AccountType.BANK.name();
            a.currency = c;
            a.totalAmount = 23450;
            a.sortOrder = 50;
            a.cardIssuer = "card issuer";
            a.closingDay = 30;
            a.creationDate = 0;
            a.isActive = true;
            a.isIncludeIntoTotals = true;
            a.issuer = "issuer";
            a.limitAmount = 1000;
            a.note = "note";
            a.number = "6004300";
            a.paymentDay = 1;
            a.sortOrder = 1;
            a.id = -1;
            dba.em().saveAccount(a);
        }

        FlowzrSyncEngine.fixCreatedEntities();
        FlowzrSyncEngine.pushUpdate("currency", Currency.class);
        FlowzrSyncEngine.pushUpdate("account", ru.orangesoftware.financisto.model.Account.class);

        JSONObject json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "admin_example.com/account/?last_sync_ts=0");
        JSONArray arr = json.getJSONArray("account");

        Cursor cursor=dba.em().getAllAccounts();

        assertEquals(valueOf(cursor.getCount()), valueOf(arr.length()));

        if (cursor.moveToFirst()) {
            do {
                //Log.i("flowzr", "account:" + cursor.getInt(0) + " " + cursor.getString(cursor.getColumnIndex("name")) + " " + cursor.getInt(cursor.getColumnIndex("remote_key")));
                // the view doesn't contains remote_key, the test is unoptimized
                ru.orangesoftware.financisto.model.Account a = dba.em().getAccount(cursor.getInt(cursor.getColumnIndex("_id")));
                //em.load(Attribute.class, cursor.getInt(cursor.getColumnIndex("_id")));
                boolean gotIt = false;
                for (int i = 0; i < arr.length(); i++) {
                    if (a.remoteKey.equals(arr.getJSONObject(i).getString("key"))) {
                        Log.i("flowzr", "got account " + a.title);
                        assertEquals(a.title, arr.getJSONObject(i).getString("name"));
                        assertEquals(a.type,arr.getJSONObject(i).getString("type"));
                        assertEquals(a.currency.name,arr.getJSONObject(i).getString("currency_name"));
                        assertEquals(a.sortOrder,arr.getJSONObject(i).getInt("sort_order"));;
                        if (a.cardIssuer!=null) {
                            assertEquals(a.cardIssuer, arr.getJSONObject(i).getString("card_issuer"));
                        }
                        assertEquals(a.closingDay,arr.getJSONObject(i).getInt("closing_day"));;
                        assertFalse(a.isActive == arr.getJSONObject(i).getBoolean("hidden"));
                        assertEquals(a.isIncludeIntoTotals,arr.getJSONObject(i).getBoolean("is_include_into_totals"));
                        if (a.issuer!=null) {
                            assertEquals(a.issuer, arr.getJSONObject(i).getString("issuer"));
                        }
                        assertEquals(a.limitAmount,arr.getJSONObject(i).getInt("total_limit"));
                        if (a.note!=null) {
                            assertEquals(a.note, arr.getJSONObject(i).getString("description"));
                        }
                        if (a.number!=null) {
                            assertEquals(a.number, arr.getJSONObject(i).getString("code"));
                        }
                        assertEquals(a.paymentDay,arr.getJSONObject(i).getInt("payment_day"));;
                        assertEquals(a.sortOrder,arr.getJSONObject(i).getInt("sort_order"));;
                        gotIt = true;
                    }
                }
                assertTrue(gotIt);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public void test_pull_accounts() throws Exception {


        //expect
        JSONObject json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "test_example.com/account/?last_sync_ts=0");
        JSONArray expect = json.getJSONArray("account");

        FlowzrSyncEngine.pullUpdate("account", ru.orangesoftware.financisto.model.Account.class, 0);

        for (int i = 0; i < expect.length(); i++) {
            Log.i("flowzr" , "checking account " + expect.getJSONObject(i).getString("key"));
            long id= FlowzrSyncEngine.getLocalKey(DatabaseHelper.ACCOUNT_TABLE,expect.getJSONObject(i).getString("key"));
            ru.orangesoftware.financisto.model.Account c=dba.em().getAccount(id);
            assertEquals(expect.getJSONObject(i).getString("name"),c.title );
            assertEquals(expect.getJSONObject(i).getString("type"),c.type);
            assertEquals(c.title, expect.getJSONObject(i).getString("name"));
            assertEquals(c.type,expect.getJSONObject(i).getString("type"));
            assertEquals(c.currency.name,expect.getJSONObject(i).getString("currency_name"));
            assertEquals(c.sortOrder,expect.getJSONObject(i).getInt("sort_order"));
            if (expect.getJSONObject(i).has("card_issuer")) {
                assertEquals(c.cardIssuer, expect.getJSONObject(i).getString("card_issuer"));
            }
            assertEquals(c.closingDay,expect.getJSONObject(i).getInt("closing_day"));;
            assertFalse(c.isActive == expect.getJSONObject(i).getBoolean("hidden"));
            assertEquals(c.isIncludeIntoTotals,expect.getJSONObject(i).getBoolean("is_include_into_totals"));
            if (expect.getJSONObject(i).has("issuer")) {
                assertEquals(c.issuer, expect.getJSONObject(i).getString("issuer"));
            }
            assertEquals(c.limitAmount,expect.getJSONObject(i).getInt("total_limit"));
            if (expect.getJSONObject(i).has("description")) {
                assertEquals(c.note, expect.getJSONObject(i).getString("description"));
            }
            if (expect.getJSONObject(i).has("code")) {
                assertEquals(c.number, expect.getJSONObject(i).getString("code"));
            }
            assertEquals(c.paymentDay,expect.getJSONObject(i).getInt("payment_day"));;
            assertEquals(c.sortOrder,expect.getJSONObject(i).getInt("sort_order"));;
        }
    }


    public void test_push_attributes() throws Exception {

        if (dba.getAllCategories().getCount() == 2) { //only system categories
            Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(dba);
            FlowzrSyncEngine.fixCreatedEntities();
        }

        FlowzrSyncEngine.pushUpdate("attributes", Attribute.class);

        JSONObject json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "admin_example.com/attributes/?last_sync_ts=0");
        JSONArray arr = json.getJSONArray("attributes");
        Cursor cursor = dba.getAllAttributes();
        //minus two system attributes ???
        assertEquals(valueOf(cursor.getCount()), valueOf(arr.length()));

        if (cursor.moveToFirst()) {
            do {
                Log.i("flowzr", "attr:" + cursor.getInt(0) + " " + cursor.getString(cursor.getColumnIndex("name")) + " " + cursor.getInt(cursor.getColumnIndex("remote_key")));
                // the view doesn't contains remote_key, the test is unoptimized
                 Attribute c = dba.getAttribute(cursor.getInt(cursor.getColumnIndex("_id")));
                        //em.load(Attribute.class, cursor.getInt(cursor.getColumnIndex("_id")));
                boolean gotIt = false;
                for (int i = 0; i < arr.length(); i++) {
                    if (c.remoteKey.equals(arr.getJSONObject(i).getString("key"))) {
                        Log.i("flowzr", "got attribute " + c.name);
                        assertEquals(c.name,arr.getJSONObject(i).getString("name"));
                        assertEquals(c.type,arr.getJSONObject(i).getInt("type"));
                        if (c.listValues!=null) {
                            assertEquals(c.listValues, arr.getJSONObject(i).getString("list_values"));
                        }
                        if (c.defaultValue!=null) {
                            assertEquals(c.defaultValue, arr.getJSONObject(i).getString("default_value"));
                        }
                        gotIt = true;
                    }
                }
                //is the object find ?
                assertTrue(gotIt);
            } while (cursor.moveToNext());

        }
        cursor.close();
    }

    public void test_push_category() throws Exception {

        if (dba.getAllCategories().getCount() == 2) { //only system categories
            Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(dba);
            FlowzrSyncEngine.fixCreatedEntities();
        }

        FlowzrSyncEngine.pushUpdate("attributes", Attribute.class);
        FlowzrSyncEngine.pushUpdate("category", Category.class);

        JSONObject json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "admin_example.com/category/?last_sync_ts=0");
        JSONArray result = json.getJSONArray("category");
        Cursor cursor = dba.getCategories(false);
        //minus two system categories
        assertEquals(valueOf(cursor.getCount()), valueOf(result.length()));

        if (cursor.moveToFirst()) {
            do {
                // the view doesn't contains remote_key, the test is unoptimized
                Category expect = em.load(Category.class, cursor.getInt(cursor.getColumnIndex("_id")));
                boolean gotIt = false;
                for (int i = 0; i < result.length(); i++) {
                    if (expect.remoteKey.equals(result.getJSONObject(i).getString("key"))) {

                        assertEquals(expect.getTitle(), result.getJSONObject(i).getString("name"));
                        if (expect.getParentId() > 0) {
                            assertEquals(em.load(Category.class, expect.getParentId()).remoteKey, result.getJSONObject(i).getString("parentCategory"));
                        }
                        assertEquals(expect.type, result.getJSONObject(i).getInt("type"));
                        gotIt = true;
                    }
                }
                assertTrue(expect.title, gotIt);

            } while (cursor.moveToNext());

        }
        cursor.close();

    }

    public void test_pull_category_when_none() throws Exception {

        //expect
        JSONObject json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "test_example.com/category/?last_sync_ts=0");
        JSONArray expect = json.getJSONArray("category");

        FlowzrSyncEngine.pullUpdate("category", Category.class, 0);

        for (int i = 0; i < expect.length(); i++) {
            Log.i("flowzr" , "checking " + expect.getJSONObject(i).getString("key"));
            long id= FlowzrSyncEngine.getLocalKey(DatabaseHelper.CATEGORY_TABLE,expect.getJSONObject(i).getString("key"));
            Category c=dba.getCategory(id);
            if (expect.getJSONObject(i).has("parentCategory")) {
                Category c2=em.load(Category.class, FlowzrSyncEngine.getLocalKey(DatabaseHelper.CATEGORY_TABLE, expect.getJSONObject(i).getString("parentCategory")));
                Category c3=dba.getCategory(c.getParentId());
                assertEquals(c2.id, c3.id);
            }
            if (expect.getJSONObject(i).has("attributes")) {
                String[] attr_arr=expect.getJSONObject(i).getString("attributes").split(";");
                for (int j=0;i<attr_arr.length;j++) {
                    Long attrId=FlowzrSyncEngine.getLocalKey(DatabaseHelper.CATEGORY_TABLE,expect.getJSONObject(i).getString("key"));
                    c.attributes.contains(dba.getAttribute(attrId));
                }

            }
            assertEquals(expect.getJSONObject(i).getString("name"),c.title );
            assertEquals(expect.getJSONObject(i).getInt("type"),c.type);
        }
    }


    public void test_pull_category() throws Exception {
        //expect
        JSONObject json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "test_example.com/category/?last_sync_ts=0");
        JSONArray expect = json.getJSONArray("category");
        //result
        FlowzrSyncEngine.pullUpdate("category", Category.class, 0);

        for (int i = 0; i < expect.length(); i++) {
            Log.i("flowzr" , "checking " + expect.getJSONObject(i).getString("key"));
            long id= FlowzrSyncEngine.getLocalKey(DatabaseHelper.CATEGORY_TABLE,expect.getJSONObject(i).getString("key"));
            Category c=dba.getCategory(id);
            if (expect.getJSONObject(i).has("parentCategory")) {
                Category c2=em.load(Category.class,FlowzrSyncEngine.getLocalKey(DatabaseHelper.CATEGORY_TABLE, expect.getJSONObject(i).getString("parentCategory")));
                Category c3=dba.getCategory(c.getParentId());
                assertEquals(c2.id, c3.id);
            }
            assertEquals(expect.getJSONObject(i).getString("name"),c.title );
            assertEquals(expect.getJSONObject(i).getInt("type"),c.type);
        }
    }


    public void test_push_budgets() throws Exception {
        Budget budgetOne;
        if (dba.em().getAllBudgets(WhereFilter.empty()).size()==0) {
            budgetOne=createBudget();
        } else {
            budgetOne=dba.em().getAllBudgets(WhereFilter.empty()).get(0);
        }
        FlowzrSyncEngine.fixCreatedEntities();
        FlowzrSyncEngine.pushUpdate("currency", Currency.class);
        FlowzrSyncEngine.pushUpdate("account", Account.class);
        FlowzrSyncEngine.pushUpdate("project", Project.class);
        FlowzrSyncEngine.pushUpdate("payee", Payee.class);
        FlowzrSyncEngine.pushUpdate("category", Category.class);

        FlowzrSyncEngine.pushUpdate(DatabaseHelper.BUDGET_TABLE, Budget.class);
        Thread.sleep(2000);
        JSONObject json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "test_example.com/budget/?last_sync_ts=0");
        JSONArray result = json.getJSONArray("budget");

        assertEquals(result.length(),dba.em().getAllBudgets(WhereFilter.empty()).size());


        budgetOne.recur="WEEKLY,startDate=1403582400000,period=EXACTLY_TIMES,periodParam=4,days=TUE";
        dba.em().insertBudget(budgetOne);
        FlowzrSyncEngine.fixCreatedEntities();
        FlowzrSyncEngine.pushDelete();
        Thread.sleep(2000);

        FlowzrSyncEngine.db=this.db;
        FlowzrSyncEngine.pushUpdate(DatabaseHelper.BUDGET_TABLE, Budget.class);

        Thread.sleep(2000);
        json = getJsonResponse(FlowzrSyncEngine.FLOWZR_API_URL + "test_example.com/budget/?last_sync_ts=0");
        result = json.getJSONArray("budget");
        assertEquals(result.length(),dba.em().getAllBudgets(WhereFilter.empty()).size());


    }

    /** Helper functions **/

    private Budget createBudget() {
        Account account;
        Project project;
        if (dba.em().getAllAccountsList().size()==0) {
            account = AccountBuilder.createDefault(dba);
        } else {
            account=dba.em().getAllAccountsList().get(0);
        }

        Map<String,Category> categoriesMap;
        if (dba.em().getAllCategoriesList(true).size()<=2) {
            categoriesMap = CategoryBuilder.createDefaultHierarchy(dba);
        } else {
            categoriesMap = CategoryBuilder.allCategoriesAsMap(dba);
        }

        if ( MyEntity.asMap(dba.em().getAllProjectsList(true)).size()==0) {
            project = new Project();
            project.title = "P1";
            em.saveOrUpdate(project);
        } else {
            project= dba.em().getAllProjectsList(true).get(0);
        }

        Budget budgetOne = new Budget();
        budgetOne.currency = account.currency;
        budgetOne.amount = 1000;
        budgetOne.categories = String.valueOf(categoriesMap.get("A").id);
        budgetOne.projects = String.valueOf(project.id);
        budgetOne.expanded = true;
        budgetOne.includeSubcategories = true;
        budgetOne.startDate = DateTime.date(2011, 4, 1).atMidnight().asLong();
        budgetOne.endDate = DateTime.date(2011, 4, 30).at(23, 59, 59, 999).asLong();
        budgetOne.recur="WEEKLY,startDate=1403582400000,period=EXACTLY_TIMES,periodParam=12,days=TUE";
        dba.em().insertBudget(budgetOne);
        FlowzrSyncEngine.fixCreatedEntities();
        return budgetOne;
    }


    public JSONObject getJsonResponse(String url) throws URISyntaxException, IOException, JSONException {

        HttpGet request = getAuthenticateRequest(url);
        HttpResponse response = http_client.execute(request);
        HttpEntity entity = response.getEntity();
        StringBuilder builder=new StringBuilder();

        if (entity != null) {
            InputStream inputStream = entity.getContent();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            for (String line = null; (line = bufferedReader.readLine()) != null;) {
                builder.append(line).append("\n");
            }
            //Exception getting thrown in below line

            return new JSONObject(builder.toString());

        }
        return null;

    }


    public HttpGet getAuthenticateRequest(String cont) {
        HttpGet h= new HttpGet(FlowzrSyncEngine.FLOWZR_BASE_URL + "_ah/login?&email=test@example.com&action=Login&continue=" + cont);
        h.setHeader("Cookie","dev_appserver_login=test@example.com:False:185804764220139124118");
        return h;
    }

    private Currency createCurrency(String currency) {
        Currency c = CurrencyBuilder.withDb(dba)
                .title("Singapore Dollar")
                .name(currency)
                .separators("''", "'.'")
                .symbol("S$")
                .create();
        return c;
    }

}
