package ru.orangesoftware.financisto.report;

import org.junit.Test;

import java.util.List;

import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import static org.junit.Assert.*;

public class SubCategoryReportTest extends AbstractReportTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CategoryReport r = new CategoryReport(getContext(), c1);
        filter = r.createFilterForSubCategory(db, WhereFilter.empty(), categories.get("A").id);
    }

    @Test
    public void should_calculate_correct_report_with_one_currency() {
        // A  -3400
        //    +250
        // A2 -2200
        //    +250
        // A1 -1100
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A")).dateTime(DateTime.today()).amount(-100).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A1")).dateTime(DateTime.today()).amount(-100).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A2")).dateTime(DateTime.today()).amount(250).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("B")).dateTime(DateTime.today()).amount(500).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-5000)
                .withSplit(categories.get("A1"), -1000)
                .withSplit(categories.get("A2"), -2200)
                .withSplit(categories.get("B"), -1800)
                .create();
        List<GraphUnit> units = assertReportReturnsData();
        assertName(units.get(0), "A");
        assertExpense(units.get(0), -3400);
        assertIncome(units.get(0), 250);
        assertName(units.get(1), "A2");
        assertExpense(units.get(1), -2200);
        assertIncome(units.get(1), 250);
        assertName(units.get(2), "A1");
        assertExpense(units.get(2), -1100);
    }

    @Test
    public void should_calculate_correct_report_with_one_currency_2() {
        // A       = +6500
        // - A1    +4000 = +5000
        // -- AA1  +1000
        // - A2    +1500
        // B
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A1")).dateTime(DateTime.today()).amount(4000).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("AA1")).dateTime(DateTime.today()).amount(1000).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A2")).dateTime(DateTime.today()).amount(1500).create();
        List<GraphUnit> units = assertReportReturnsData();
        assertUnit(units.get(0), "A", 0, 6500);
        assertUnit(units.get(1), "A1", 0, 5000);
        assertUnit(units.get(2), "AA1", 0, 1000);
        assertUnit(units.get(3), "A2", 0, 1500);
    }

    @Test
    public void should_calculate_correct_report_with_multiple_currencies_1() {
        // A  -120$
        //    -100$$
        RateBuilder.withDb(db).at(DateTime.today()).from(c2).to(c1).rate(0.1f).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A")).dateTime(DateTime.today()).amount(-120).create();
        TransactionBuilder.withDb(db).account(a3).category(categories.get("A")).dateTime(DateTime.today()).amount(-100).create();
        List<GraphUnit> units = assertReportReturnsData();
        assertName(units.get(0), "A");
        assertExpense(units.get(0), -130);
        assertIncome(units.get(0), 0);
    }

    @Test
    public void should_calculate_correct_report_with_multiple_currencies_2() {
        RateBuilder.withDb(db).at(DateTime.today()).from(c2).to(c1).rate(0.1f).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A")).dateTime(DateTime.today()).amount(-100).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A1")).dateTime(DateTime.today()).amount(-100).create();
        TransactionBuilder.withDb(db).account(a3).category(categories.get("A2")).dateTime(DateTime.today()).amount(250).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("B")).dateTime(DateTime.today()).amount(500).create();
        TransactionBuilder.withDb(db).account(a3).dateTime(DateTime.today()).amount(-5000)
                .withSplit(categories.get("A1"), -1000)
                .withSplit(categories.get("A2"), -2200)
                .withSplit(categories.get("B"), -1800)
                .create();
        List<GraphUnit> units = assertReportReturnsData();
        assertEquals(3, units.size());

        assertUnit(units.get(0), "A", -520, 25);
        assertUnit(units.get(1), "A2", -220, 25);
        assertUnit(units.get(2), "A1", -200, 0);
    }

    private void assertUnit(GraphUnit unit, String name, long expense, long income) {
        assertName(unit, name);
        assertExpense(unit, expense);
        assertIncome(unit, income);
    }

    @Override
    protected Report createReport() {
        return new SubCategoryReport(getContext(), c1);
    }

}
