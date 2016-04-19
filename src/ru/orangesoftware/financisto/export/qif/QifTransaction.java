package ru.orangesoftware.financisto.export.qif;

import android.database.Cursor;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import static ru.orangesoftware.financisto.export.qif.QifUtils.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 12:52 AM
 */
public class QifTransaction {

    public long id;
    public Date date;
    public long amount;
    public String payee;
    public String memo;
    public String category;
    public String categoryClass;
    public String toAccount;
    public String project;

    public boolean isSplit = false;
    public List<QifTransaction> splits;

    public static QifTransaction fromBlotterCursor(Cursor c, Map<Long, Category> categoriesMap) {
        QifTransaction t = new QifTransaction();
        t.id = c.getLong(BlotterColumns._id.ordinal());
        t.date = new Date(c.getLong(BlotterColumns.datetime.ordinal()));
        t.amount = c.getLong(BlotterColumns.from_amount.ordinal());
        t.payee = c.getString(BlotterColumns.payee.ordinal());
        t.memo = c.getString(BlotterColumns.note.ordinal());
        long projectId = c.getLong(BlotterColumns.project_id.ordinal());
        if (projectId > 0) {
            t.project = c.getString(BlotterColumns.project.ordinal());
        }
        Category category = getCategoryFromCursor(c, categoriesMap);
        if (category != null) {
            QifCategory qifCategory = QifCategory.fromCategory(category);
            t.category = qifCategory.name;
        }
        t.isSplit = categoryIsSplit(c);
        t.toAccount = c.getString(BlotterColumns.to_account_title.ordinal());
        return t;
    }

    private static Category getCategoryFromCursor(Cursor c, Map<Long, Category> categoriesMap) {
        long categoryId = c.getLong(BlotterColumns.category_id.ordinal());
        return categoriesMap.get(categoryId);
    }

    private static boolean categoryIsSplit(Cursor c) {
        long categoryId = c.getLong(BlotterColumns.category_id.ordinal());
        return categoryId == Category.SPLIT_CATEGORY_ID;
    }

    public void writeTo(QifBufferedWriter qifWriter, QifExportOptions options)
            throws IOException {
        qifWriter.write("D").write(options.dateFormat.format(date)).newLine();
        qifWriter.write("T").write(Utils.amountToString(options.currency, amount)).newLine();
        if (toAccount != null) {
            qifWriter.write("L[").write(toAccount).write("]").newLine();
        } else if (category != null && project != null) {
            qifWriter.write("L").write(category).write("/").write(project).newLine();
        } else if (category != null) {
            qifWriter.write("L").write(category).newLine();
        } else if (project != null) {
            qifWriter.write("L/").write(project).newLine();
        }
        if (Utils.isNotEmpty(payee)) {
            qifWriter.write("P").write(payee).newLine();
        }
        if (Utils.isNotEmpty(memo)) {
            qifWriter.write("M").write(memo).newLine();
        }
        if (isSplit()) {
            for (QifTransaction split : splits) {
                writeSplit(qifWriter, options, split);
            }
        }
        qifWriter.end();
    }

    private void writeSplit(QifBufferedWriter qifWriter, QifExportOptions options, QifTransaction split) throws IOException {
        if (split.toAccount != null) {
            qifWriter.write("S[").write(split.toAccount).write("]").newLine();
        } else {
            if (split.category != null) {
                qifWriter.write("S").write(split.category).newLine();
            } else {
                qifWriter.write("S<NO_CATEGORY>").newLine();
            }
        }
        qifWriter.write("$").write(Utils.amountToString(options.currency, split.amount)).newLine();
        if (Utils.isNotEmpty(split.memo)) {
            qifWriter.write("E").write(split.memo).newLine();
        }
    }

    public boolean isSplit() {
        return isSplit;
    }

    public void setSplits(List<QifTransaction> splits) {
        this.splits = splits;
    }

    public void readFrom(QifBufferedReader r, QifDateFormat dateFormat) throws IOException {
        QifTransaction split = null;
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("^")) {
                break;
            }
            if (line.startsWith("D")) {
                this.date = parseDate(trimFirstChar(line), dateFormat);
            } else if (line.startsWith("T")) {
                this.amount = parseMoney(trimFirstChar(line));
            } else if (line.startsWith("P")) {
                this.payee = trimFirstChar(line);
            } else if (line.startsWith("M")) {
                this.memo = trimFirstChar(line);
            } else if (line.startsWith("L")) {
                parseCategory(this, line);
            } else if (line.startsWith("S")) {
                addSplit(split);
                split = new QifTransaction();
                parseCategory(split, line);
            } else if (line.startsWith("$")) {
                if (split != null) {
                    split.amount = parseMoney(trimFirstChar(line));
                }
            } else if (line.startsWith("E")) {
                if (split != null) {
                    split.memo = trimFirstChar(line);
                }
            }
        }
        addSplit(split);
        adjustSplitsDatetime();
    }

    private void adjustSplitsDatetime() {
        if (splits != null) {
            for (QifTransaction split : splits) {
                split.date = this.date;
            }
        }
    }

    private void parseCategory(QifTransaction t, String line) {
        String category = trimFirstChar(line);
        int i = category.indexOf('/');
        if (i != -1) {
            t.categoryClass = category.substring(i+1);
            category = category.substring(0, i);
        }
        if (isTransferCategory(category)) {
            t.toAccount = category.substring(1, category.length()-1);
        } else {
            t.category = category;
        }
    }

    private void addSplit(QifTransaction split) {
        if (split == null) {
            return;
        }
        if (splits == null) {
            splits = new ArrayList<QifTransaction>();
        }
        splits.add(split);
    }

    public static QifTransaction fromTransaction(Transaction transaction, Map<Long, Category> categoriesMap, Map<Long, Account> accountsMap) {
        QifTransaction qifTransaction = new QifTransaction();
        qifTransaction.amount = transaction.fromAmount;
        qifTransaction.memo = transaction.note;
        if (transaction.toAccountId > 0) {
            Account toAccount = accountsMap.get(transaction.toAccountId);
            qifTransaction.toAccount = toAccount.title;
            //TODO: test if from and to accounts have different currencies
        }
        Category category = categoriesMap.get(transaction.categoryId);
        if (category != null) {
            QifCategory qifCategory = QifCategory.fromCategory(category);
            qifTransaction.category = qifCategory.name;
        }
        qifTransaction.isSplit = transaction.isSplitParent();
        return qifTransaction;
    }

    public Transaction toTransaction() {
        Transaction t = new Transaction();
        t.id = -1;
        t.dateTime = date.getTime();
        t.fromAmount = amount;
        t.note = memo;
        return t;
    }

    public boolean isTransfer() {
        return toAccount != null;
    }

}
