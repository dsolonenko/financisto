package ru.orangesoftware.financisto.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.utils.MyPreferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * @author Abdsandryk
 *
 */
public class CCardStatementClosingDayActivity extends Activity {

	public static final String PERIOD_MONTH = "statement_period_month";
	public static final String PERIOD_YEAR = "statement_period_year";
	public static final String ACCOUNT = "account";
	public static final String REGULAR_CLOSING_DAY = "regular_closing_day";
	public static final String UPDATE_VIEW = "update";
	
	// Period key in database (MMYYYY), where MM = 0 to 11
	private int periodKey;
	
	private DatabaseAdapter dbAdapter;
	
	// Credit Card account id
	private long accountId;
	
	// month (0-11)
	private int month;
	// year
	private int year;
	
	int customClosingDay = 0;
	int regularClosingDay;
	
	RadioButton customCD;
	RadioButton regularCD;
	EditText newClosingDay;
	
	Activity activity;
	Intent intent;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ccard_statement_closing_day);
        
        intent = getIntent();
        activity = this;
		if (intent != null) {
			accountId = intent.getLongExtra(ACCOUNT, 0);

			Calendar cal = Calendar.getInstance();
			month = intent.getIntExtra(PERIOD_MONTH, cal.get(Calendar.MONTH));
			year = intent.getIntExtra(PERIOD_YEAR, cal.get(Calendar.YEAR));
			// verify if exists a custom closing day in database
			periodKey = Integer.parseInt(Integer.toString(month)+Integer.toString(year));
			
			regularClosingDay = intent.getIntExtra(REGULAR_CLOSING_DAY, 0);
		}
		
		initialize();
    }
    
    /**
     * When activity lifecycle ends, release resources
     */
    @Override
    public void onDestroy() {
    	dbAdapter.close();
    	super.onDestroy();
    }
    
    /**
     * Initialize data and GUI elements.
     */
    private void initialize() {
		dbAdapter = new DatabaseAdapter(this);
		dbAdapter.open();
		
    	customCD = (RadioButton)findViewById(R.id.custom_closing_day);
    	regularCD = (RadioButton)findViewById(R.id.regular_closing_day);
    	newClosingDay = (EditText)findViewById(R.id.new_closing_day);
    	
    	customClosingDay = dbAdapter.getCustomClosingDay(accountId, periodKey);
    	if (customClosingDay>0) {
    		// select custom closing day and fill edit text
    		newClosingDay.setText(Integer.toString(customClosingDay));
    		customCD.setChecked(true);
    	} else {
    		// select regular closing day and disable edit text
    		regularCD.setChecked(true);
    		newClosingDay.setVisibility(EditText.GONE);
    	}
    	
		setLabels();
		setListeners();
		
		this.setTitle(R.string.closing_day_title);
		
		if (customClosingDay>0) {
			EditText newCD = (EditText)findViewById(R.id.new_closing_day);
			newCD.setText(Integer.toString(customClosingDay));
			// set custom closing day selected
			regularCD.setChecked(false);
			customCD.setChecked(true);
		} else {
			// set regular closing day selected
			customCD.setChecked(false);
			regularCD.setChecked(true);			
		}
	}

    
	/**
	 * Adjust the title based on the credit card's payment day.
	 */
	private void setLabels() {
		
		Calendar date = new GregorianCalendar(year, month, 1);
		 
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM yyyy");  
		String pd = dateFormat.format(date.getTime()); 
		
		TextView label = (TextView)findViewById(R.id.closing_day_reference_period);
		label.setText(getString(R.string.reference_period)+"\n" + pd);
		
		regularCD.setText(getString(R.string.regular_closing_day)+" ("+regularClosingDay+")");
		
	}
	
	/**
	 * Set listeners for radio buttons
	 */
	private void setListeners() {
		
		// Custom Closing Day radio button
		customCD.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	EditText newClosingDay = (EditText)findViewById(R.id.new_closing_day);
		        // Perform action on clicks, depending on whether it's now checked
		        if (((RadioButton) v).isChecked()) {
		        	newClosingDay.setVisibility(EditText.VISIBLE);
		        } else {
		            
		        }
		    }
		});
		
		// Regular Closing Day radio button
		regularCD.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	EditText newClosingDay = (EditText)findViewById(R.id.new_closing_day);
		        // Perform action on clicks, depending on whether it's now checked
		        if (((RadioButton) v).isChecked()) {
		        	newClosingDay.setVisibility(EditText.GONE);
		        } else {
		            
		        }
		    }
		});
		
		// OK Button
		final Button ok = (Button) findViewById(R.id.closing_day_ok);
		ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click OK
				if (customCD.isChecked()) {
					if (isNewDayValid()) {
						int newCD = Integer.parseInt(newClosingDay.getText().toString());
						if (newCD!=customClosingDay) {
							// store the new value in database
							saveNewClosingDay(newCD);
							Intent resultValue = new Intent();
			                resultValue.putExtra(UPDATE_VIEW, 1);
							activity.setResult(RESULT_OK, resultValue);
							finish();
						} else {
							// same value, no changes
							activity.setResult(RESULT_CANCELED, intent);
							finish();
						}
					} // else - do nothing, alert message to correct value
				} else if (regularCD.isChecked()) {
					if (dbAdapter.getCustomClosingDay(accountId, periodKey)>0) {
						dbAdapter.deleteCustomClosingDay(accountId, periodKey);
						Intent resultValue = new Intent();
		                resultValue.putExtra(UPDATE_VIEW, 1);
						activity.setResult(RESULT_OK, resultValue);
						finish();
					} else {
						// same value, no changes
						activity.setResult(RESULT_CANCELED, intent);
						finish();
					}
				}
				
			}
		});
		
		// Cancel Button
		final Button cancel = (Button) findViewById(R.id.closing_day_cancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				activity.setResult(RESULT_CANCELED, intent);
				finish();
			}
		});
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean isNewDayValid() {
		// check if the value in form is valid
		String text = newClosingDay.getText().toString();
		String alertMsg = "";
		
		if (text!=null && text.length()>0) {
			// Max day of reference month
			Calendar periodCal = new GregorianCalendar(year, month, 1);
			int maxDay = periodCal.getActualMaximum(Calendar.DAY_OF_MONTH);
			
			int newCD = Integer.parseInt(text);
			
			if (newCD<1 || newCD>maxDay) {
				alertMsg = getString(R.string.alert_invalid_closing_day)+" [1-"+maxDay+"].";
			} else if (newCD==regularClosingDay) {
				alertMsg = getString(R.string.alert_regular_closing_day);
			}
			
		} else {
			// text null - alert user
			alertMsg = getString(R.string.alert_null_closing_day);
		}
		if (alertMsg.length()>0) {
			// Alert message
			Log.w("Alert", alertMsg);
			AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
	        dlgAlert.setMessage(alertMsg);
	        dlgAlert.setTitle(R.string.closing_day);
	        dlgAlert.setPositiveButton(R.string.ok, null);
	        dlgAlert.setCancelable(true);
	        dlgAlert.create().show();
	        return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 
	 * @param closingDay
	 */
	private void saveNewClosingDay(int closingDay) {
		if (dbAdapter.getCustomClosingDay(accountId, periodKey)>0) {
			// value exists, update
			dbAdapter.updateCustomClosingDay(accountId, periodKey, closingDay);
		} else {
			// insert new value
			dbAdapter.setCustomClosingDay(accountId, periodKey, closingDay);
		}
	}
}
