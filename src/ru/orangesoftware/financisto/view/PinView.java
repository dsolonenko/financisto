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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Vibrator;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.Base64Coder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class PinView implements OnClickListener {
	
	private static final int[] buttons = { R.id.b0, R.id.b1, R.id.b2, R.id.b3,
		R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.bHelp,
		R.id.bClear};

	public static interface PinListener {
		void onConfirm(String pinBase64);
		void onSuccess(String pinBase64);
	}

    private final Context context;
	private final PinListener listener;
	private final View v;	
	private final ViewSwitcher switcher;
	private final MessageDigest digest;
    private final Vibrator vibrator;
	
	private TextView result;
	private String pin1;
	private String pin2;
	private boolean confirmPin;

	public PinView(Context context, PinListener listener, int layoutId) {
		this(context, listener, null, layoutId);
	}
	
	public PinView(Context context, PinListener listener, String pin, int layoutId) {
        this.context = context;
		this.listener = listener;
		this.confirmPin = pin == null;
		this.pin1 = pin;
		LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		v = layoutInflater.inflate(layoutId, null);
		for (int id : buttons) {
			v.findViewById(id).setOnClickListener(this);
		}
		result = (TextView)v.findViewById(R.id.result1);		
		switcher = (ViewSwitcher)v.findViewById(R.id.switcher);  
		switcher.setInAnimation(inFromRightAnimation());
		switcher.setOutAnimation(outToLeftAnimation());		
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}
	
	public View getView() {
		return v;
	}
	
	@Override
	public void onClick(View v) {
		Button b = (Button)v;
		char c = b.getText().charAt(0);
        if (vibrator != null && MyPreferences.isPinHapticFeedbackEnabled(context)) {
            vibrator.vibrate(20);
        }
		switch (c) {
		case 'O':
			nextStep();
			break;
		case 'C':
			result.setText("");
			break;
		default:
            String text = result.getText().toString();
			if (text.length() < 7) {
				result.setText(text+String.valueOf(c));
			}
			break;
		}
	}

	private void nextStep() {
		if (confirmPin) {
			pin1 = pinBase64(result.getText().toString());
			result = (TextView)v.findViewById(R.id.result2);
			confirmPin = false;			
			switcher.showNext();		
			listener.onConfirm(pin1);			
		} else {
			pin2 = pinBase64(result.getText().toString());
			if (pin1.equals(pin2)) {
				listener.onSuccess(pin2);				
			} else {
				result.startAnimation(shakeAnimation());
			}
		}
	}
	
	private String pinBase64(String pin) {
		byte[] a = digest.digest(pin.getBytes());
        return new String(Base64Coder.encode(a));
	}

	private Animation inFromRightAnimation() {
		Animation inFromRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromRight.setDuration(300);
		inFromRight.setInterpolator(new AccelerateInterpolator());
		return inFromRight;
	}

	private Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoLeft.setDuration(300);
		outtoLeft.setInterpolator(new AccelerateInterpolator());
		return outtoLeft;
	}
	
	private Animation shakeAnimation() {
		Animation anim = new TranslateAnimation(0.0f, 10.0f, 0.0f, 0.0f);
		anim.setDuration(300);
		anim.setInterpolator(new CycleInterpolator(5));
		return anim;
	}

}
