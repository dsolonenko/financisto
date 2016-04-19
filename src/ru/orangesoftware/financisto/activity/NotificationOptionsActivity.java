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
package ru.orangesoftware.financisto.activity;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.recur.NotificationOptions;
import ru.orangesoftware.financisto.utils.EnumUtils;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NotificationOptionsActivity extends AbstractActivity {

	public static final String NOTIFICATION_OPTIONS = "options";
	private static final int PICKUP_RINGTONE = 1;
	
	private static final NotificationOptions.LedColor[] colors = NotificationOptions.LedColor.values();
	private static final NotificationOptions.VibrationPattern[] patterns = NotificationOptions.VibrationPattern.values();

	private LinearLayout layout;
	
	private TextView soundText;
	private TextView ledText;
	private TextView vibraText;
	private NotificationOptions options = NotificationOptions.createDefault();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.recurrence);

		layout = (LinearLayout)findViewById(R.id.layout);
		
		Intent intent = getIntent();
		if (intent != null) {
			String options = intent.getStringExtra(NOTIFICATION_OPTIONS);
			if (options != null) {
				try {
					this.options = NotificationOptions.parse(options);
				} catch (Exception e) {
					this.options = NotificationOptions.createDefault();
				}
			}
		}
		
		createNodes();	
		updateOptions();
		
		Button bOK = (Button)findViewById(R.id.bOK);
		bOK.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent data = new Intent();
				data.putExtra(NOTIFICATION_OPTIONS, options.stateToString());
				setResult(RESULT_OK, data);
				finish();
			}
		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}			
		});		
	}

	public void createNodes() {
		layout.removeAllViews();
		soundText = x.addListNode(layout, R.id.notification_sound, R.string.notification_sound, options.getSoundName(this));
		vibraText = x.addListNode(layout, R.id.notification_vibra, R.string.notification_vibra, options.vibration.titleId);
		ledText = x.addListNode(layout, R.id.notification_led, R.string.notification_led, options.ledColor.titleId);
		x.addInfoNodeSingle(layout, R.id.result1, R.string.notification_options_default);			
		x.addInfoNodeSingle(layout, R.id.result2, R.string.notification_options_off);			
	}

	@Override
	protected void onClick(View v, int id) {
		switch (id) {
			case R.id.notification_sound: {
				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				if (options.sound != null) {
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(options.sound));
				}
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
				startActivityForResult(intent, PICKUP_RINGTONE);
			} break;
			case R.id.notification_vibra: {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, patterns);
				x.selectPosition(this, R.id.notification_vibra, R.string.notification_vibra, adapter, options.vibration.ordinal());
			} break;
			case R.id.notification_led:  {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, colors);
				x.selectPosition(this, R.id.notification_led, R.string.notification_led, adapter, options.ledColor.ordinal());
			} break;
			case R.id.result1: {
				options = NotificationOptions.createDefault();
				updateOptions();
			} break;
			case R.id.result2: {
				options = NotificationOptions.createOff();
				updateOptions();
			} break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICKUP_RINGTONE && resultCode == RESULT_OK) {
			Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			options.sound = ringtoneUri != null ? ringtoneUri.toString() : null;
			updateOptions();
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch (id) {
		case R.id.notification_sound:
			updateOptions();
			break;
		case R.id.notification_vibra:
			options.vibration = patterns[selectedPos];
			updateOptions();
			break;
		case R.id.notification_led:
			options.ledColor = colors[selectedPos];
			updateOptions();
			break;
		}
	}

	private void updateOptions() {
		soundText.setText(options.getSoundName(this));
		vibraText.setText(options.vibration.titleId);
		ledText.setText(options.ledColor.titleId);
	}

}
