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
package ru.orangesoftware.financisto.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.RelativeLayout.LayoutParams;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.ThumbnailUtil;

import java.io.File;

public class NodeInflater {
	
	private final LayoutInflater inflater;
	
	public NodeInflater(LayoutInflater inflater) {
		this.inflater = inflater;
	}

    public View addDivider(LinearLayout layout) {
        View divider = inflater.inflate(R.layout.edit_divider, layout, false);
        layout.addView(divider);
        return divider;
    }

    public class Builder {
		protected final LinearLayout layout;
		protected final View v;
		
		private boolean divider = true; 
		
		public Builder(LinearLayout layout, int layoutId) {
			this.layout = layout;
			this.v = inflater.inflate(layoutId, layout, false); 
		}
		
		public Builder(LinearLayout layout, View v) {
			this.layout = layout;
			this.v = v; 
		}

		public Builder withId(int id, OnClickListener listener) {
			v.setId(id);
			v.setOnClickListener(listener);
			return this;
		}
		
		public Builder withLabel(int labelId) {
			TextView labelView = (TextView)v.findViewById(R.id.label);
			labelView.setText(labelId);		
			return this;
		}

		public Builder withLabel(String label) {
			TextView labelView = (TextView)v.findViewById(R.id.label);
			labelView.setText(label);		
			return this;
		}
	
		public Builder withData(int labelId) {
			TextView labelView = (TextView)v.findViewById(R.id.data);
			labelView.setText(labelId);		
			return this;
		}

		public Builder withData(String label) {
			TextView labelView = (TextView)v.findViewById(R.id.data);
			labelView.setText(label);		
			return this;
		}

		public Builder withIcon(int iconId) {
			ImageView iconView = (ImageView)v.findViewById(R.id.icon);
			iconView.setImageResource(iconId);		
			return this;
		}

		public Builder withNoDivider() {
			divider = false;
			return this;
		}

		public View create() {
			layout.addView(v);
			if (divider) {
                View dividerView = addDivider(layout);
                v.setTag(dividerView);
            }
			return v;
		}

    }
	
	public class EditBuilder extends Builder {

		public EditBuilder(LinearLayout layout, View view) {			
			super(layout, R.layout.select_entry_edit);
			RelativeLayout relativeLayout = (RelativeLayout)v.findViewById(R.id.layout);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.label);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.label);
			relativeLayout.addView(view, layoutParams);
		}
		
	}
	
	public class ListBuilder extends Builder {

		public ListBuilder(LinearLayout layout, int layoutId) {
			super(layout, layoutId);
		}
		
		public ListBuilder withButtonId(int buttonId, OnClickListener listener) {
			ImageView plusImageView = (ImageView)v.findViewById(R.id.plus_minus);
			plusImageView.setId(buttonId);
			plusImageView.setOnClickListener(listener);
			return this;
		}

        public ListBuilder withoutMoreButton() {
            v.findViewById(R.id.more).setVisibility(View.GONE);
            return this;
        }

	}
	
	public class CheckBoxBuilder extends Builder {

		public CheckBoxBuilder(LinearLayout layout) {
			super(layout, R.layout.select_entry_checkbox);
		}
		
		public CheckBoxBuilder withCheckbox(boolean checked) {
			CheckBox checkBox = (CheckBox)v.findViewById(R.id.checkbox);
			checkBox.setChecked(checked);
			return this;
		}
		
	}

	public class PictureBuilder extends ListBuilder {

		public PictureBuilder(LinearLayout layout) {
			super(layout, R.layout.select_entry_picture);
		}
		
		@Override
		public ListBuilder withButtonId(int buttonId, OnClickListener listener) {
			ImageView plusImageView = (ImageView)v.findViewById(R.id.plus_minus);
			plusImageView.setVisibility(View.VISIBLE);
			return super.withButtonId(buttonId, listener);
		}

		public PictureBuilder withPicture(final Context context, Bitmap picture) {
			final ImageView imageView = (ImageView)v.findViewById(R.id.picture);
			imageView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					String pictureFileName = (String)imageView.getTag();
					if (pictureFileName != null) {
						Uri target = Uri.fromFile(new File(ThumbnailUtil.PICTURES_DIR, pictureFileName)); 
						Intent intent = new Intent(Intent.ACTION_VIEW, target); 
						intent.putExtra(Intent.EXTRA_STREAM, target); 
						intent.setDataAndType(target, "image/jpeg"); 
						context.startActivity(intent);
					}
				}
			});
			imageView.setImageBitmap(picture);
			return this;
		}
		
	}

}
