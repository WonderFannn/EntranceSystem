package com.csipsimple.newui.view;

import java.util.Timer;
import java.util.TimerTask;

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
				Toast toast = Toast.makeText(thisActivity, msg, Toast.LENGTH_LONG);
				showMyToast(toast, 1000);
			}
		});
		super.run();
	}
	public void showMyToast(final Toast toast, final int cnt) {  
        final Timer timer = new Timer();  
        timer.schedule(new TimerTask() {  
            @Override  
            public void run() {  
                toast.show();  
            }  
        }, 0, 3000);  
        new Timer().schedule(new TimerTask() {  
            @Override  
            public void run() {  
                toast.cancel();  
                timer.cancel();  
            }  
        }, cnt );  
    }  
}