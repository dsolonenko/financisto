package ru.orangesoftware.financisto.utils;

import android.test.AndroidTestCase;
import ru.orangesoftware.financisto.model.Transaction;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/23/11 1:53 AM
 */
public class SplitAdjusterTest extends AndroidTestCase {

    List<Transaction> splits;

    public void test_should_adjust_all_splits_evenly() {
        adjustEvenly(0);
        assertAmounts(1000, -500, 420, 100, -100);

        adjustEvenly(500);
        assertAmounts(1100, -400, 520, 200, 0);

        adjustEvenly(-1000);
        assertAmounts(800, -700, 220, -100, -300);

        adjustEvenly(1001);
        assertAmounts(1200, -300, 620, 300, 101);

        adjustEvenly(-1002);
        assertAmounts(800, -700, 220, -101, -301);

        adjustEvenly(1003);
        assertAmounts(1200, -300, 621, 301, 101);

        adjustEvenly(-1004);
        assertAmounts(800, -701, 219, -101, -301);

        adjustEvenly(4);
        assertAmounts(1000, -499, 421, 101, -99);
    }

    public void test_should_adjust_the_last_split() {
        adjustLast(0);
        assertAmounts(1000, -500, 420, 100, -100);

        adjustLast(100);
        assertAmounts(1000, -500, 420, 100, 0);

        adjustLast(-100);
        assertAmounts(1000, -500, 420, 100, -200);
    }

    private void adjustEvenly(long amount) {
        createSplits();
        SplitAdjuster.adjustEvenly(splits, amount);
    }

    private void adjustLast(long amount) {
        createSplits();
        SplitAdjuster.adjustLast(splits, amount);
    }

    private void assertAmounts(long...splitAmounts) {
        assertEquals("Split1", splitAmounts[0], splits.get(0).fromAmount);
        assertEquals("Split2", splitAmounts[1], splits.get(1).fromAmount);
        assertEquals("Split3", splitAmounts[2], splits.get(2).fromAmount);
        assertEquals("Split4", splitAmounts[3], splits.get(3).fromAmount);
        assertEquals("Split5", splitAmounts[4], splits.get(4).fromAmount);
    }

    private void createSplits() {
        splits = Arrays.asList(newSplit(1000), newSplit(-500), newSplit(420), newSplit(100), newSplit(-100));
    }

    private Transaction newSplit(long amount) {
        Transaction t = new Transaction();
        t.fromAmount = amount;
        return t;
    }

}
