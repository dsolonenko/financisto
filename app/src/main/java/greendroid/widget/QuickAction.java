/*
 * Copyright (C) 2010 Cyril Mottier (https://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greendroid.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;

/**
 * A QuickAction implements an item in a {@link greendroid.widget.QuickActionWidget}. A
 * QuickAction represents a single action and may contain a text and an icon.
 * 
 * @author Benjamin Fellous
 * @author Cyril Mottier
 */
public class QuickAction {

    public Drawable mDrawable;
    public CharSequence mTitle;

    /* package */WeakReference<View> mView;

    public QuickAction(Drawable d, CharSequence title) {
        mDrawable = d;
        mTitle = title;
    }

    public QuickAction(Context ctx, int drawableId, CharSequence title) {
        mDrawable = ContextCompat.getDrawable(ctx, drawableId);
        mTitle = title;
    }

    public QuickAction(Context ctx, Drawable d, int titleId) {
        mDrawable = d;
        mTitle = ctx.getResources().getString(titleId);
    }

    public QuickAction(Context ctx, int drawableId, int titleId) {
        mDrawable = ContextCompat.getDrawable(ctx, drawableId);
        mTitle = ctx.getResources().getString(titleId);
    }

}
