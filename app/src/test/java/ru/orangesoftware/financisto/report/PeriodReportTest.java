package ru.orangesoftware.financisto.report;

import android.preference.PreferenceManager;

import org.junit.Test;

import java.util.List;

import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import static org.junit.Assert.*;

public class PeriodReportTest extends AbstractReportTest {

    // important that report is re-created after include_transfers_into_reports preference is set

    @Test
    public void should_calculate_correct_report_for_today_without_transfers() {
        //given
        givenTransfersAreExcludedFromReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), 0);
        assertExpense(units.get(0), -3500);
    }

    @Test
    public void should_calculate_correct_report_for_today_with_transfers() {
        //given
        givenTransfersAreIncludedIntoReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        assertIncome(units.get(0), 250);
        assertExpense(units.get(0), -4700);
    }

    @Test
    public void should_calculate_correct_report_for_today_with_splits_without_transfers() {
        //given
        givenTransfersAreExcludedFromReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-10000)
                .withSplit(categories.get("A1"), -8000)
                .withTransferSplit(a2, -2000, 2000)
                .create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), 0);
        assertExpense(units.get(0), -8000);
    }

    @Test
    public void should_calculate_correct_report_for_today_with_splits_with_transfers() {
        //given
        givenTransfersAreIncludedIntoReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-10000)
                .withSplit(categories.get("A1"), -8000)
                .withTransferSplit(a2, -2000, 2000)
                .create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), 2000);
        assertExpense(units.get(0), -10000);
    }

    @Test
    public void should_calculate_report_in_home_currency_without_transfers() {
        //given
        givenTransfersAreExcludedFromReports();
        RateBuilder.withDb(db).at(DateTime.today()).from(c2).to(c1).rate(0.1f).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransactionBuilder.withDb(db).account(a3).dateTime(DateTime.today()).amount(-1500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a3).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), 0);
        assertExpense(units.get(0), -3650);
    }

    @Test
    public void should_calculate_report_in_home_currency_with_transfers() {
        //given
        givenTransfersAreIncludedIntoReports();
        RateBuilder.withDb(db).at(DateTime.today()).from(c2).to(c1).rate(0.1f).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransactionBuilder.withDb(db).account(a3).dateTime(DateTime.today()).amount(-1500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a3).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), 1200);
        assertExpense(units.get(0), -4850);
    }

    @Test
    public void should_calculate_report_in_home_currency_with_transfers_with_selected_income_expense() {
        //given
        givenTransfersAreIncludedIntoReports();
        RateBuilder.withDb(db).at(DateTime.today()).from(c2).to(c1).rate(0.1f).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransactionBuilder.withDb(db).account(a3).dateTime(DateTime.today()).amount(-1500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a3).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData(IncomeExpense.BOTH);
        //then
        assertIncome(units.get(0), 1200);
        assertExpense(units.get(0), -4850);
        //when
        report = createReport();
        units = assertReportReturnsData(IncomeExpense.INCOME);
        //then
        assertIncome(units.get(0), 1200);
        assertExpense(units.get(0), 0);
        //when
        report = createReport();
        units = assertReportReturnsData(IncomeExpense.EXPENSE);
        //then
        assertIncome(units.get(0), 0);
        assertExpense(units.get(0), -4850);
        //when
        report = createReport();
        units = assertReportReturnsData(IncomeExpense.SUMMARY);
        //then
        assertIncome(units.get(0), -3650);
        assertExpense(units.get(0), 0);
    }

    private void givenTransfersAreExcludedFromReports() {
        assertTrue(PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("include_transfers_into_reports", false).commit());
    }

    private void givenTransfersAreIncludedIntoReports() {
        assertTrue(PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("include_transfers_into_reports", true).commit());
    }

    @Override
    protected Report createReport() {
        return new PeriodReport(getContext(), c1);
    }

}
