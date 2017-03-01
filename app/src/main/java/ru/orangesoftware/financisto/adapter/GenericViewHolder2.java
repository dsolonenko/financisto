/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.adapter;

import ru.orangesoftware.financisto.R;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GenericViewHolder2 {
	public ImageView iconView;
	public ImageView iconOverView;
	public TextView topView;
	public TextView centerView;
	public TextView bottomView;
	public TextView rightView;
	public TextView rightCenterView;
	public ProgressBar progressBar;
	public TextView progressText;
	
	public static View create(View view) {
		GenericViewHolder2 v = new GenericViewHolder2();
		v.iconView = (ImageView)view.findViewById(R.id.icon);
		v.iconOverView = (ImageView)view.findViewById(R.id.active_icon);
		v.topView = (TextView)view.findViewById(R.id.top);
		v.centerView = (TextView)view.findViewById(R.id.center);		
		v.bottomView = (TextView)view.findViewById(R.id.bottom);
		v.rightView = (TextView)view.findViewById(R.id.right);
		v.rightCenterView = (TextView)view.findViewById(R.id.right_center);
		v.rightCenterView.setVisibility(View.GONE);
		v.progressBar = (ProgressBar)view.findViewById(R.id.progress);
		v.progressBar.setVisibility(View.GONE);
		v.progressText = (TextView)view.findViewById(R.id.progress_text);
		v.progressText.setVisibility(View.GONE);
		view.setTag(v);
		return view;
	}
	
}
