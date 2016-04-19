package ru.orangesoftware.financisto.utils;

import ru.orangesoftware.financisto.model.Transaction;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/23/11 2:02 AM
 */
public class SplitAdjuster {

    public static void adjustEvenly(List<Transaction> splits, long unsplitAmount) {
        if (noSplits(splits)) {
            return;
        }
        int count = splits.size();
        long amount = unsplitAmount/count;
        for (Transaction split : splits) {
            split.fromAmount += amount;
        }
        long extra = unsplitAmount - amount*count;
        if (extra != 0) {
            int sign = extra > 0 ? 1 : -1;
            for (int i=count-1; i >= count-sign*extra; i--) {
                splits.get(i).fromAmount += sign;
            }
        }
    }

    public static void adjustLast(List<Transaction> splits, long unsplitAmount) {
        if (noSplits(splits)) {
            return;
        }
        adjustSplit(splits.get(splits.size()-1), unsplitAmount);
    }

    public static void adjustSplit(Transaction split, long unsplitAmount) {
        split.fromAmount += unsplitAmount;
    }

    private static boolean noSplits(List<Transaction> splits) {
        return splits == null || splits.isEmpty();
    }

}
