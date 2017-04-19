package com.csipsimple.newui;

import java.util.List;
import java.util.UUID;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.WizardIface;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

import android.R.integer;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

public class HomeSettingActivity extends Activity {
	
	private WizardIface wizard = null;
	protected SipProfile account = null;
	private String[] settingInfo = {"0506","0506","192.168.1.101","5060"};
	private Boolean registerState = false; 
	
	private ImageView ivBackButton;
	private ExpandableListView elvSettingMenu;
	private ExpandableAdapter adapter;
    private Context mContext;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_home_setting);
		super.onCreate(savedInstanceState);
		mContext = HomeSettingActivity.this;
		String[] groupName = {"网络","注册/启用配置"};
		String[][] childNames = {{"显示名称","ID","服务器","端口"},{}};
		elvSettingMenu = (ExpandableListView) findViewById(R.id.expandableListView1);
		adapter = new ExpandableAdapter(groupName, childNames, mContext);
		elvSettingMenu.setAdapter(adapter);
		ivBackButton = (ImageView) findViewById(R.id.iv_backbutton);
		ivBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
	}

	private class ExpandableAdapter extends BaseExpandableListAdapter {    
			private String[] groupStrings;
		    private String[][] itermStrings;
		    private Context mContext;
		    public ExpandableAdapter(String []gData,String [][]iData, Context mContext) {
		        this.groupStrings = gData;
		        this.itermStrings = iData;
		        this.mContext = mContext;
		    }
		    @Override
		    public int getGroupCount() {
		        return groupStrings.length;
		    }
		    @Override
		    public int getChildrenCount(int groupPosition) {
		        return itermStrings[groupPosition].length;
		    }
		    @Override
		    public Object getGroup(int groupPosition) {
		        return groupStrings;
		    }
		    @Override
		    public String getChild(int groupPosition, int childPosition) {
		        return itermStrings[groupPosition][childPosition];
		    }
		    @Override
		    public long getGroupId(int groupPosition) {
		        return groupPosition;
		    }
		    @Override
		    public long getChildId(int groupPosition, int childPosition) {
		        return childPosition;
		    }
		    @Override
		    public boolean hasStableIds() {
		        return false;
		    }
		    @Override
		    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		        ViewHolderGroup groupHolder;
		        if (0 == groupPosition) {
		        	if(convertView == null){
		        		convertView = LayoutInflater.from(mContext).inflate(
		        				R.layout.item_homesetting_exlist_group, parent, false);
		        		groupHolder = new ViewHolderGroup();
		        		groupHolder.tv_group_name = (TextView) convertView.findViewById(R.id.tv_group_name);
		        		groupHolder.iv_group_setfold = (ImageView) convertView.findViewById(R.id.iv_group_indicator);
		        		convertView.setTag(groupHolder);
		        	}else{
		        		groupHolder = (ViewHolderGroup) convertView.getTag();
		        	}
		        	groupHolder.tv_group_name.setText(groupStrings[groupPosition]);
		        	groupHolder.iv_group_setfold.setImageResource(!isExpanded ? R.drawable.ic_homesettingactivity_fold:R.drawable.ic_homesettingactivity_unfold);
		        	return convertView;
				}else if (1 == groupPosition) {
					if(convertView == null){
		        		convertView = LayoutInflater.from(mContext).inflate(
		        				R.layout.item_homesetting_exlist_group, parent, false);
		        		groupHolder = new ViewHolderGroup();
		        		groupHolder.tv_group_name = (TextView) convertView.findViewById(R.id.tv_group_name);
		        		groupHolder.iv_group_setfold = (ImageView) convertView.findViewById(R.id.iv_group_indicator);
		        		convertView.setTag(groupHolder);
		        	}else{
		        		groupHolder = (ViewHolderGroup) convertView.getTag();
		        	}
		        	groupHolder.tv_group_name.setText(groupStrings[groupPosition]);
		        	groupHolder.iv_group_setfold.setImageResource(registerState ?R.drawable.ic_homesettingactivity_open :R.drawable.ic_homesettingactivity_close);
		        	groupHolder.iv_group_setfold.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							if (!registerState) {
								registerState = true;
								startSipService();
								((ImageView) v).setImageResource(R.drawable.ic_homesettingactivity_open);
							}else {
								((ImageView) v).setImageResource(R.drawable.ic_homesettingactivity_close);
								stopSipService();
								registerState = false;
							}
						}

					});
		        	return convertView;
				}
		        return convertView;
		    }
		    @Override
		    public View getChildView(int groupPosition,final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		        final ViewHolderItem itemHolder;
		        if(convertView == null){
		            convertView = LayoutInflater.from(mContext).inflate(
		                    R.layout.item_homesetting_exlist_item, parent, false);
		            itemHolder = new ViewHolderItem();
		            itemHolder.et_settingnumber = (EditText) convertView.findViewById(R.id.et_settingnumber);
		            itemHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
		            convertView.setTag(itemHolder);
		        }else{
		            itemHolder = (ViewHolderItem) convertView.getTag();
		        }
		        itemHolder.tv_name.setText(itermStrings[groupPosition][childPosition]);
		        itemHolder.et_settingnumber.setText(settingInfo[childPosition]);
		        itemHolder.et_settingnumber.addTextChangedListener(new TextWatcher() {
		        	@Override
		        	public void onTextChanged(CharSequence s, int start, int before, int count) {
		        	}
		        	@Override
		        	public void beforeTextChanged(CharSequence s, int start, int count,
		        			int after) {
		        	}
		        	@Override
		        	public void afterTextChanged(Editable s) {
							settingInfo[childPosition] = itemHolder.et_settingnumber.getText().toString();
		        	}
		        });
		        return convertView;
		    }
		    //设置子列表是否可选中
		    @Override
		    public boolean isChildSelectable(int groupPosition, int childPosition) {
		        return true;
		    }
		    private class ViewHolderGroup{
		        private TextView tv_group_name;
		        private ImageView iv_group_setfold;
		    }
		    private class ViewHolderItem{

		    	private TextView tv_name;
		        private EditText et_settingnumber;
		    }
		   
    }
	
	private void stopSipService() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(SipManager.ACTION_SIP_CAN_BE_STOPPED);
				sendBroadcast(intent);			
			};
		});
		t.start();
	}
	// Service monitoring stuff
	private void startSipService() {

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
				// Optional, but here we bundle so just ensure we are using
				// csipsimple package
				
				serviceIntent.setPackage("com.csipsimple");
				serviceIntent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY,
						new ComponentName(HomeSettingActivity.this,
								HomeActivity.class));
				startService(serviceIntent);
				createAccount(settingInfo[0],settingInfo[1],settingInfo[2],settingInfo[3]);				
			};
		});
		t.start();

	}

	private void createAccount(String idString,String displayString,String iPString,String portString) {

		WizardInfo wizardInfo = WizardUtils.getWizardClass("BASIC");
		try {
			wizard = (WizardIface) wizardInfo.classObject.newInstance();
		} catch (IllegalAccessException e) {
			return;
		} catch (InstantiationException e) {
			return;
		}

		account = SipProfile.getProfileFromDbId(this, 1,
				DBProvider.ACCOUNT_FULL_PROJECTION);

		boolean needRestart = false;

		PreferencesWrapper prefs = new PreferencesWrapper(getApplicationContext());
		if (account == null) {
			account = new SipProfile();
			account = wizard.buildAccount(account);
		}
		// account.wizard = "BASIC";
//		account.acc_id = "<sip:004@192.168.60.122>";
		account.acc_id = "<sip:"+idString+"@"+iPString+">";
		account.display_name = displayString;
//		account.reg_uri = "sip:192.168.60.122:5060";
		account.reg_uri = "sip:"+iPString+":"+portString;
		// account.wizard = wizardId;
		if (account.id == SipProfile.INVALID_ID) {
			// This account does not exists yet
			prefs.startEditing();
			wizard.setDefaultParams(prefs);
			prefs.endEditing();
			applyNewAccountDefault(account);
			Uri uri = getContentResolver().insert(SipProfile.ACCOUNT_URI,
					account.getDbContentValues());

			// After insert, add filters for this wizard
			account.id = ContentUris.parseId(uri);
			List<Filter> filters = wizard.getDefaultFilters(account);
			if (filters != null) {
				for (Filter filter : filters) {
					// Ensure the correct id if not done by the wizard
					filter.account = (int) account.id;
					getContentResolver().insert(SipManager.FILTER_URI,
							filter.getDbContentValues());
				}
			}
			// Check if we have to restart
			needRestart = wizard.needRestart();

		} else {
			// TODO : should not be done there but if not we should add an
			// option to re-apply default params
			prefs.startEditing();
			wizard.setDefaultParams(prefs);
			prefs.endEditing();
			getContentResolver().update(
					ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE,
							account.id), account.getDbContentValues(), null,
					null);
		}

		// Mainly if global preferences were changed, we have to restart sip
		// stack
		if (needRestart) {
			Intent intent = new Intent(SipManager.ACTION_SIP_REQUEST_RESTART);
			sendBroadcast(intent);
		}

	}

	private void applyNewAccountDefault(SipProfile account) {
		if (account.use_rfc5626) {
			if (TextUtils.isEmpty(account.rfc5626_instance_id)) {
				String autoInstanceId = (UUID.randomUUID()).toString();
				account.rfc5626_instance_id = "<urn:uuid:" + autoInstanceId
						+ ">";
			}
		}
	}
	
}
