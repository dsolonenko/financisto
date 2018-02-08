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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListAsyncAdapter;
import ru.orangesoftware.financisto.adapter.async.SmsTemplateListSource;
import ru.orangesoftware.financisto.adapter.dragndrop.SimpleItemTouchHelperCallback;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

public class SmsDragListFragment extends Fragment {

    private DatabaseAdapter db;
    private ItemTouchHelper mItemTouchHelper;
    private SmsTemplateListSource listSource;

    public SmsDragListFragment() {

    }



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

        this.db = new DatabaseAdapter(this.getContext());
        this.listSource =  new SmsTemplateListSource(db);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final RecyclerView recyclerView = view.findViewById(R.id.drag_list_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        SmsTemplateListAsyncAdapter adapter = new SmsTemplateListAsyncAdapter(100, listSource, recyclerView);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);

        recyclerView.setAdapter(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }
}
