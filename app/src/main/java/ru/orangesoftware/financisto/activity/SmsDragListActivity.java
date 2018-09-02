package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListAsyncAdapter;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListSource;
import ru.orangesoftware.financisto.adapter.dragndrop.SimpleItemTouchHelperCallback;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.utils.StringUtil;

public class SmsDragListActivity extends AppCompatActivity {

    private static final String TAG = "Financisto." + SmsDragListActivity.class.getSimpleName();
    private static final String LIST_STATE_KEY = "LIST_STATE";

    public static final int NEW_REQUEST_CODE = 1;
    public static final int EDIT_REQUEST_CODE = 2;
    public static final int LIST_CHUNK_SIZE = 100;

    private DatabaseAdapter db;
    private SmsTemplateListSource cursorSource;
    private SmsTemplateListAsyncAdapter adapter;
    private RecyclerView recyclerView;
    private Parcelable listState;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.draglist_bar_layout);

        db = new DatabaseAdapter(this);
        db.open();
        
//        Toolbar menu = findViewById(R.id.tool_bar);
//        setSupportActionBar(menu);

        recyclerView = findViewById(R.id.drag_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        cursorSource = createSource();
        createAdapter(true);
        ((TextView) findViewById(android.R.id.empty)).setVisibility(View.GONE); // todo.mb: handle later
        
        if(state != null) listState = state.getParcelable(LIST_STATE_KEY);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        listState = recyclerView.getLayoutManager().onSaveInstanceState(); // https://stackoverflow.com/a/28262885/365675
        state.putParcelable(LIST_STATE_KEY, listState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (listState != null) recyclerView.getLayoutManager().onRestoreInstanceState(listState);
    }

    @NonNull
    protected SmsTemplateListSource createSource() {
        return new SmsTemplateListSource(db, true);
    }

    private void createAdapter(boolean dragnDrop) {
        adapter = new SmsTemplateListAsyncAdapter(LIST_CHUNK_SIZE, db, cursorSource, recyclerView, this);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        
        if (dragnDrop) {
            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
            ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
            mItemTouchHelper.attachToRecyclerView(recyclerView);
        }
        adapter.onStart(recyclerView);
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            adapter.reloadVisibleItems();
        } else if (resultCode == RESULT_CANCELED) {
            adapter.revertSwipeBack();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.draglist_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            /**
             * Not sense doing via android.widget.Filter as adapter and its data is filtered in async mode 
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                cursorSource.setConstraint(newText);
                adapter.reloadAsyncSource();
                
                if (!StringUtil.isEmpty(newText)) {
                    Log.i(TAG, "filtered by `" + newText + "`");
//                    Toast.makeText(SmsDragListActivity.this, "filtered by '" + newText + "'", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        final MenuItem newItem = menu.findItem(R.id.new_sms_template);
        newItem.setOnMenuItemClickListener(this::addItem);
        return true;
    }

    private boolean addItem(MenuItem menuItem) {
        Intent intent = new Intent(this, SmsTemplateActivity.class);
        startActivityForResult(intent, NEW_REQUEST_CODE);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        adapter.onStop(recyclerView);
        
        super.onDestroy();
    }
}
