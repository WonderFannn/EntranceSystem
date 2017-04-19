package com.csipsimple.newui;

import com.csipsimple.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class HomeActivity extends Activity implements OnClickListener {
	
	private ImageView imageViewSetting;
	
	private TextView testTextView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		imageViewSetting = (ImageView) findViewById(R.id.iv_homeactivity_setting);
		imageViewSetting.setOnClickListener(this);
		
	}
	

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.iv_homeactivity_setting) {
			Intent  i= new Intent(getApplicationContext(),HomeSettingActivity.class);
			startActivity(i);
		}
		
	}

}
