package ru.orangesoftware.financisto.adapter.async;

import android.support.v7.util.AsyncListUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class AsyncAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private final ItemSource<T> itemSource;
    private final RecyclerView recyclerView;
    private final DataCallback dataCallback;
    private final ScrollListener onScrollListener;
    protected final AsyncListUtil<T> listUtil;

    public AsyncAdapter(int chunkSize, ItemSource<T> itemSource, RecyclerView recyclerView) {
        this.itemSource = itemSource;
        this.recyclerView = recyclerView;
        this.dataCallback = new DataCallback();
        this.listUtil = new AsyncListUtil<>(itemSource.clazz(), chunkSize, dataCallback, new ViewCallback());
        this.onScrollListener = new ScrollListener();
    }

    public void onStart(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(onScrollListener);
        listUtil.refresh();
    }

    public void onStop(RecyclerView recyclerView) {
        recyclerView.removeOnScrollListener(onScrollListener);
        dataCallback.close();
    }

    @Override
    public int getItemCount() {
        return listUtil.getItemCount();
    }



    private class DataCallback extends AsyncListUtil.DataCallback<T> implements AutoCloseable {

        @Override
        public int refreshData() {
            return itemSource.getCount();
        }

        @Override
        public void fillData(T[] data, int startPosition, int itemCount) {
            if (data == null) {
                return;
            }
            for (int i = 0; i < itemCount; i++) {
                data[i] = itemSource.getItem(startPosition + i);
            }
        }

        @Override
        public void close(){
            itemSource.close();
        }
    }


    private class ViewCallback extends AsyncListUtil.ViewCallback {

        @Override
        public void getItemRangeInto(int[] outRange) {
            if (outRange == null) {
                return;
            }
            if(recyclerView.getLayoutManager() instanceof LinearLayoutManager){
                LinearLayoutManager llm = (LinearLayoutManager) recyclerView.getLayoutManager();
                outRange[0] = llm.findFirstVisibleItemPosition();
                outRange[1] = llm.findLastVisibleItemPosition();
            }
            if (outRange[0] == -1 && outRange[1] == -1) {
                outRange[0] = 0;
                outRange[1] = 0;
            }
        }

        @Override
        public void onDataRefresh() {
            recyclerView.getAdapter().notifyDataSetChanged();
        }

        @Override
        public void onItemLoaded(int position) {
            recyclerView.getAdapter().notifyItemChanged(position);
        }
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            listUtil.onRangeChanged();
        }
    }
}
