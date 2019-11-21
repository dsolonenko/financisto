package ru.orangesoftware.financisto.report;

import org.junit.Test;

import java.util.List;

import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import static org.junit.Assert.*;

public class CategoryReportAllTest extends AbstractReportTest {

    @Test
    public void should_calculate_correct_report() {
        // A -3400
        //   +250
        // A2 -2200
        //    +250
        // B -1800
        //   +500
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
        assertName(units.get(2), "B");
        assertExpense(units.get(2), -1800);
        assertIncome(units.get(2), 500);
        assertName(units.get(3), "A1");
        assertExpense(units.get(3), -1100);
    }

    @Override
    protected Report createReport() {
        return new CategoryReportAll(getContext(), c1);
    }

}
