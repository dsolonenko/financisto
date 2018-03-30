/*
  Copyright 2014 Magnus Woxblom
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package ru.orangesoftware.financisto.activity;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListAsyncAdapter;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListSource;
import ru.orangesoftware.financisto.adapter.dragndrop.SimpleItemTouchHelperCallback;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

public class SmsDragListFragment extends Fragment implements RefreshSupportedActivity {

    private static final String TAG = SmsDragListFragment.class.getSimpleName();
    private static final String LIST_STATE_KEY = "LIST_STATE";
    
    public static final int NEW_REQUEST_CODE = 1;
    public static final int EDIT_REQUEST_CODE = 2;
    
    private DatabaseAdapter db;
    private SmsTemplateListSource cursorSource;

    protected ImageButton bAdd;
    private EditText filterTxt;

    private RecyclerView recyclerView;
    private Parcelable listState;
    private SmsTemplateListAsyncAdapter adapter;

    public static SmsDragListFragment newInstance() {
        return new SmsDragListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.draglist_layout, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        db = new DatabaseAdapter(context);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.drag_list_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        cursorSource = createSource();
        recreateAdapter();

        bAdd = view.findViewById(R.id.bAdd);
        bAdd.setOnClickListener(this::addItem);
        filterTxt = view.findViewById(R.id.sms_tpl_filter); // todo.mb: finish
        filterTxt.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ("1".equals(s)) {
                    adapter.filter("77");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void recreateAdapter() {
        adapter = new SmsTemplateListAsyncAdapter(100, db, cursorSource, recyclerView, this);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @NonNull
    protected SmsTemplateListSource createSource() {
        return new SmsTemplateListSource(db, true);
    }

    private void addItem(View v) {
        Intent intent = new Intent(v.getContext(), SmsTemplateActivity.class);
        startActivityForResult(intent, NEW_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            adapter.reloadVisibleItems();
        } else if (resultCode == RESULT_CANCELED) {
            adapter.revertSwipeBack();
        }
    }

    @Override
    public void recreateCursor() { // todo.mb: not needed so far
//        Log.i(TAG, "Recreating source...");
        
//        listState = recyclerView.getLayoutManager().onSaveInstanceState();
//        try {
//            if (cursorSource != null) cursorSource.close();
//            cursorSource = createSource();
//            recreateAdapter();
//        } finally {
//            recyclerView.getLayoutManager().onRestoreInstanceState(listState);
//        }
    }

    @Override
    public void integrityCheck() {
        // ignore
    }

    // service methods >>

    @Override
    public void onActivityCreated(@Nullable Bundle state) {
        super.onActivityCreated(state);

        if(state != null) listState = state.getParcelable(LIST_STATE_KEY);

        adapter.onStart(recyclerView);
    }



    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);

        listState = recyclerView.getLayoutManager().onSaveInstanceState(); // https://stackoverflow.com/a/28262885/365675
        state.putParcelable(LIST_STATE_KEY, listState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listState != null) recyclerView.getLayoutManager().onRestoreInstanceState(listState);
    }

    @Override
    public void onDestroy() {
        if (db != null) db.close();
        adapter.onStop(recyclerView);

        super.onDestroy();
    }
}
