package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import greendroid.widget.QuickAction;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/25/11 9:56 PM
 */
public class MyQuickAction extends QuickAction {

    private static final ColorFilter BLACK_CF = new LightingColorFilter(Color.BLACK, Color.BLACK);

    public MyQuickAction(Context ctx, int drawableId, int titleId) {
        super(ctx, buildDrawable(ctx, drawableId), titleId);
    }

    private static Drawable buildDrawable(Context ctx, int drawableId) {
        Drawable d = ctx.getResources().getDrawable(drawableId).mutate();
        d.setColorFilter(BLACK_CF);
        return d;
    }

}
