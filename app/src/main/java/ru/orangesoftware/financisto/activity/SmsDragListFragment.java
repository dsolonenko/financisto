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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageButton;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListAsyncAdapter;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListSource;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

@Deprecated // todo.mb: remove then
public class SmsDragListFragment extends Fragment implements RefreshSupportedActivity {

    private static final String TAG = SmsDragListFragment.class.getSimpleName();
    
    
    private DatabaseAdapter db;
    private SmsTemplateListSource cursorSource;

    protected ImageButton bAdd;
    private EditText filterTxt;

    private RecyclerView recyclerView;
    private SmsTemplateListAsyncAdapter adapter;
    
    public static SmsDragListFragment newInstance() {
        return new SmsDragListFragment();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.draglist_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
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

        bAdd = view.findViewById(R.id.bAdd);
        
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

    

    

    

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        
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

        
    }



    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);

        
    }

    @Override
    public void onResume() {
        super.onResume();
        
    }

    @Override
    public void onDestroy() {
        

        super.onDestroy();
    }
}
