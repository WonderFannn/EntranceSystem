package com.csipsimple.newui.view;

import android.app.Activity;
import android.widget.Toast;

public class ShowToastThread extends Thread {
	String msg;
	Activity thisActivity;

	public ShowToastThread(Activity activity,String showMsg) {
		thisActivity = activity;
		msg = showMsg;
	}
	@Override
	public void run() {
		thisActivity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(thisActivity, msg, Toast.LENGTH_SHORT).show();
			}
		});
		super.run();
	}
}