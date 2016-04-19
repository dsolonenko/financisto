package ru.orangesoftware.financisto.report;

import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/6/11 11:15 PM
 */
public class CategoryReportTest extends AbstractReportTest {

    public void test_should_calculate_correct_report() {
        // A -3400
        //   +250
        // B -1800
        //   +500
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
        assertName(units.get(1), "B");
        assertExpense(units.get(1), -1800);
        assertIncome(units.get(1), 500);
    }

    @Override
    protected Report createReport() {
        return new CategoryReport(getContext(), c1);
    }

}
