package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.utils.Utils.text;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public abstract class AbstractSplitActivity extends AbstractActivity {

    protected EditText noteText;
    protected TextView unsplitAmountText;

    protected Account fromAccount;
    protected Currency originalCurrency;
    protected Utils utils;
    protected Transaction split;

    protected ProjectSelector<AbstractSplitActivity> projectSelector;

    private final int layoutId;

    protected AbstractSplitActivity(int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(layoutId);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_dialog_currency);

        fetchData();
        // todo.mb: check selector here
        projectSelector = new ProjectSelector<>(this, db, x);
        projectSelector.fetchEntities();

        utils  = new Utils(this);
        split = Transaction.fromIntentAsSplit(getIntent());
        if (split.fromAccountId > 0) {
            fromAccount = db.getAccount(split.fromAccountId);
        }
        if (split.originalCurrencyId > 0) {
            originalCurrency = CurrencyCache.getCurrency(db, split.originalCurrencyId);
        }

        LinearLayout layout = findViewById(R.id.list);

        createUI(layout);
        createCommonUI(layout);
        updateUI();
    }

    private void createCommonUI(LinearLayout layout) {
        unsplitAmountText = x.addInfoNode(layout, R.id.add_split, R.string.unsplit_amount, "0");

        noteText = new EditText(this);
        x.addEditNode(layout, R.string.note, noteText);

        projectSelector.createNode(layout);

        Button bSave = findViewById(R.id.bSave);
		bSave.setOnClickListener(arg0 -> saveAndFinish());

        Button bCancel = findViewById(R.id.bCancel);
		bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    protected abstract void fetchData();

    protected abstract void createUI(LinearLayout layout);

    @Override
    protected void onClick(View v, int id) {
        projectSelector.onClick(id);
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        projectSelector.onSelectedPos(id, selectedPos);
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        projectSelector.onSelectedId(id, selectedId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        projectSelector.onActivityResult(requestCode, resultCode, data);
    }

    private void saveAndFinish() {
        Intent data = new Intent();
        if (updateFromUI()) {
            split.toIntentAsSplit(data);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }

    protected boolean updateFromUI() {
        split.note = text(noteText);
        split.projectId = projectSelector.getSelectedEntityId();
        return true;
    }

    protected void updateUI() {
        projectSelector.selectEntity(split.projectId);
        setNote(split.note);
    }

    private void setNote(String note) {
        noteText.setText(note);
    }

    protected void setUnsplitAmount(long amount) {
        Currency currency = getCurrency();
        utils.setAmountText(unsplitAmountText, currency, amount, false);
    }

    protected Currency getCurrency() {
        return originalCurrency != null ? originalCurrency : (fromAccount != null ? fromAccount.currency : Currency.defaultCurrency());
    }

    @Override
    protected boolean shouldLock() {
        return MyPreferences.isPinProtectedNewTransaction(this);
    }

    @Override
    protected void onDestroy() {
        if (projectSelector != null) projectSelector.onDestroy();
        super.onDestroy();
    }
}
