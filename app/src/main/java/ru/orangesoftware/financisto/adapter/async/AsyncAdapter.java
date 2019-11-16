package ru.orangesoftware.financisto.adapter.async;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Based on <a href=https://github.com/jasonwyatt/AsyncListUtil-Example>AsyncListUtil-Example</a> and 
 * <a href=https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf>drag-and-swipe-with-recyclerview</a>
 */
public abstract class AsyncAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> { 

    private final ItemSource<T> itemSource;
    private final RecyclerView recyclerView;
    private final ScrollListener onScrollListener;
    private final int chunkSize;
    protected volatile AsyncListUtil<T> listUtil;

    public AsyncAdapter(int chunkSize, ItemSource<T> itemSource, RecyclerView recyclerView) {
        this.chunkSize = chunkSize;
        this.itemSource = itemSource;
        this.recyclerView = recyclerView;
        this.listUtil = initListUtil();
        this.onScrollListener = new ScrollListener();
    }

    @NonNull
    private AsyncListUtil<T> initListUtil() {
        return new AsyncListUtil<>(itemSource.clazz(), chunkSize, new DataCallback(), new ViewCallback());
    }

    public void onStart(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(onScrollListener);
        listUtil.refresh();
    }

    public void onStop(RecyclerView recyclerView) {
        recyclerView.removeOnScrollListener(onScrollListener);
        itemSource.close();
    }

    public void reloadAsyncSource() {
        itemSource.close();
        listUtil = initListUtil();
        notifyDataSetChanged();
    }
    
    /**
     * Reloads all visible items from DB
     */
    public void reloadVisibleItems() {
        itemSource.close();
        listUtil.refresh(); // it'll cause reload items from DB and so cursor re-init
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
//            Log.d(SmsTemplateListAsyncAdapter.TAG, "777 Refreshed");
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
