package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CommonSwipeRefreshLayout extends SwipeRefreshLayout {
    private View mScrollingView;

    public CommonSwipeRefreshLayout(Context context) {
        super(context);
    }

    public CommonSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        return mScrollingView != null && mScrollingView.canScrollVertically(-1);
    }

    public void setScrollingView(View scrollingView) {
        mScrollingView = scrollingView;
    }
}
