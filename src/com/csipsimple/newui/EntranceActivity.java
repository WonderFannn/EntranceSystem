package com.csipsimple.newui;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import org.apache.http.util.EncodingUtils;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.pjsip.PjSipService;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.prefs.AudioTester;
import com.csipsimple.ui.prefs.cupcake.MainPrefs;
import com.csipsimple.ui.prefs.cupcake.PrefsLoaderActivity;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.CallHandlerPlugin.OnLoadListener;
import com.csipsimple.wizards.WizardIface;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class EntranceActivity extends Activity implements OnDataReceiveListener {

	private static final String THIS_FILE = "EntranceActivity";
	private Boolean isDigit = null;
	private PreferencesWrapper prefsWrapper;
	
	private PreferencesProviderWrapper prefProviderWrapper;
	private WizardIface wizard = null;
	protected SipProfile account = null;
	private Context context;

	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			/*
			 * timings.addSplit("Service connected"); if(configurationService !=
			 * null) { timings.dumpToLog(); }
			 */
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}
	};

	private TextView textViewPhoneNumber;
	private char callNumber[] = {' ',' ',' ',' '};
	private int callNuberIndex = 0;
	private TextView textViewCallButton;
	
	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case 7:
		case 8:
		case 9:
		case 10:
		case 11:
		case 12:
		case 13:
		case 14:
		case 15:
		case 16:
			refreshCallNumber(keyCode-7);
			break;
		case 155:
			if (textViewPhoneNumber.getText().toString().equals("9001")) {
				//进入验证用户界面;
				Intent intent = new Intent(this,ModifyActivity.class);
				startActivity(intent);
			}else if (textViewPhoneNumber.getText().toString().equals("9002")) {
				Intent intent = new Intent(this,AdminSettingActivity.class);
				startActivity(intent);
			}else if (textViewPhoneNumber.getText().toString().equals("9999")) {
				Intent intent = new Intent(this,PasswordOpenDoorActivity.class);
				startActivity(intent);
			}
			else {
				placeVideoCall();
				callNuberIndex = 0;
				for (int i = 0; i < callNumber.length; i++) {
					callNumber[i] = ' ';
				}
				textViewPhoneNumber.setText(new String(callNumber));
			}
			break;
		case 18:
			if (callNuberIndex > 0) {
				callNuberIndex --;
				callNumber[callNuberIndex] = ' ';
				textViewPhoneNumber.setText(new String(callNumber));
			}
			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
		
	}
	
    private void refreshCallNumber(int i) {
    	
    	switch (callNuberIndex) {
		case 0:
			if (9>=i && 0<=i) {
				callNumber[callNuberIndex] = String.valueOf(i).charAt(0);
				callNuberIndex ++;
			}
			break;
		case 1:
			if (9>=i && 0<=i) {
				callNumber[callNuberIndex] = String.valueOf(i).charAt(0);
				callNuberIndex ++;
			}
			break;
		case 2:
			if (9>=i && 0<=i) {
				callNumber[callNuberIndex] = String.valueOf(i).charAt(0);
				callNuberIndex ++;
			}
			break;
		case 3:
			if (9>=i && 0<=i) {
				callNumber[callNuberIndex] = String.valueOf(i).charAt(0);
				callNuberIndex ++;
			}
			break;

		default:
			break;
		}
    	textViewPhoneNumber.setText(new String(callNumber));
	}

	@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String str = "KeyCode--->"+keyCode;
                Log.i("KeyCode",str);
//                textViewConsole.setText(str);
                
            }
        });
        return super.dispatchKeyEvent(event);
    }
	/**
	 * Handle button click events<br />
	 * <br />
	 * Auto-created on 2017-03-01 13:28:18 by Android Layout Finder
	 * (http://www.buzzingandroid.com/tools/android-layout-finder)
	 */
	
	 public static String getIPAddress(Context contextThis) {
	        Context context = contextThis;
	        NetworkInfo info = ((ConnectivityManager) context
	                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
	        if (info != null && info.isConnected()) {
	            if (info.getType() == ConnectivityManager.TYPE_ETHERNET||
	            		info.getType() == ConnectivityManager.TYPE_WIFI) {//
	                try {
	                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
	                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
	                        NetworkInterface intf = en.nextElement();
	                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
	                            InetAddress inetAddress = enumIpAddr.nextElement();
	                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
	                                return inetAddress.getHostAddress();
	                            }
	                        }
	                    }
	                } catch (SocketException e) {
	                    e.printStackTrace();
	                }

	            }
	        } else {
	            //当前无网络连接,请在设置中打开网络
	        }
	        return null;
	    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_entrance);
		Log.d("wangfan", THIS_FILE+" start");
		Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
        // Optional, but here we bundle so just ensure we are using csipsimple package
        serviceIntent.setPackage(this.getPackageName());
        this.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        String LocalIPString = getIPAddress(this);
        startSipService();
        if (LocalIPString != null) {
        	createAccount(LocalIPString);
		}
		textViewPhoneNumber = (TextView) findViewById(R.id.textView_PhoneNumber);
//		textViewPhoneNumber.setText("0507");
		textViewPhoneNumber.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		textViewCallButton = (TextView) findViewById(R.id.tv_callbutton);
		textViewCallButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				placeVideoCall();
			}
		});
		
		SharedPreferences.Editor editor = getSharedPreferences("CardData", MODE_PRIVATE).edit();
		editor.putInt("UserNum", 0);
		editor.putInt("AdminNum", 0);
		editor.apply();
		mSerialPortUtil = SerialPortUtil.getInstance();
		mSerialPortUtil.setOnDataReceiveListener(this);
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		onForeground = true;
//		PjSipService.resetCodecs();
//		Intent intent = new Intent(SipManager.ACTION_SIP_REQUEST_RESTART);
//		sendBroadcast(intent);

		// prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT,
		// false);

		// Intent startIntent = new Intent(this, BroadPushServerService.class);
		// startService(startIntent);

	}
	
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		mSerialPortUtil.setOnDataReceiveListener(this);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		// if (id == R.id.action_settings) {
		// return true;
		// }
		return super.onOptionsItemSelected(item);
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
						new ComponentName(EntranceActivity.this,
								EntranceActivity.class));
				startService(serviceIntent);
//				createAccount();
				// applyPrefs();
				// // postStartSipService();
			};
		});
		t.start();

	}

	private void createAccount(String IPString) {

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
		account.acc_id = "<sip:001@"+IPString+">";
		account.display_name = "001";
		account.reg_uri = "sip:"+IPString+":5060";
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

	// private void postStartSipService() {
	// // If we have never set fast settings
	// if (CustomDistribution.showFirstSettingScreen()) {
	// if
	// (!prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP,
	// false)) {
	// Intent prefsIntent = new Intent(SipManager.ACTION_UI_PREFS_FAST);
	// prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// startActivity(prefsIntent);
	// return;
	// }
	// } else {
	// boolean doFirstParams =
	// !prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP,
	// false);
	// prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP,
	// true);
	// if (doFirstParams) {
	// prefProviderWrapper.resetAllDefaultValues();
	// }
	// }
	//
	// // If we have no account yet, open account panel,
	// // if (!hasTriedOnceActivateAcc) {
	// //
	// // Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new
	// String[] {
	// // SipProfile.FIELD_ID
	// // }, null, null, null);
	// // int accountCount = 0;
	// // if (c != null) {
	// // try {
	// // accountCount = c.getCount();
	// // } catch (Exception e) {
	// // Log.e(THIS_FILE, "Something went wrong while retrieving the account",
	// e);
	// // } finally {
	// // c.close();
	// // }
	// // }
	// //
	// // if (accountCount == 0) {
	// // Intent accountIntent = null;
	// // WizardInfo distribWizard =
	// CustomDistribution.getCustomDistributionWizard();
	// // if (distribWizard != null) {
	// // accountIntent = new Intent(this, BasePrefsWizard.class);
	// // accountIntent.putExtra(SipProfile.FIELD_WIZARD, distribWizard.id);
	// // } else {
	// // accountIntent = new Intent(this, AccountsEditList.class);
	// // }
	// //
	// // if (accountIntent != null) {
	// // accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// // startActivity(accountIntent);
	// // hasTriedOnceActivateAcc = true;
	// // return;
	// // }
	// // }
	// // hasTriedOnceActivateAcc = true;
	// // }
	// }

	private boolean onForeground = false;

	// /**
	// * A placeholder fragment containing a simple view.
	// */
	// public static class PlaceholderFragment extends Fragment {
	//
	// public PlaceholderFragment() {
	// }
	//
	// @Override
	// public View onCreateView(LayoutInflater inflater, ViewGroup container,
	// Bundle savedInstanceState) {
	// View rootView = inflater.inflate(R.layout.fragment_main, container,
	// false);
	// return rootView;
	// }
	// }

	private PowerManager.WakeLock sCpuWakeLock;

	private void acquireCpuWakeLock(Context context) {
		if (sCpuWakeLock != null) {
			return;
		}
		Log.v("power", "acquireCpuWakeLock");
		sCpuWakeLock = createPartialWakeLock(context);
		sCpuWakeLock.acquire();
	}

	private PowerManager.WakeLock createPartialWakeLock(Context context) {
		Log.v("power", "createPartialWakeLock");
		String flag = "log";
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		return pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, flag);
	}

	public void releaseCpuLock() {
		if (sCpuWakeLock != null) {
			Log.v("power", "releaseCpuLock");
			sCpuWakeLock.release();
			sCpuWakeLock = null;
		}
	}

	public void placeCall() {
		placeCallWithOption(null);
	}

	public void placeVideoCall() {
		Bundle b = new Bundle();
		b.putBoolean(SipCallSession.OPT_CALL_VIDEO, true);
		placeCallWithOption(b);
	}

	private void placeCallWithOption(Bundle b) {
		if (service == null) {
			return;
		}
		String toCall = "";
		Long accountToUse = SipProfile.INVALID_ID;
		// Find account to use
		SipProfile acc = account;
		if (acc == null) {
			return;
		}

		accountToUse = acc.id;
		// Find number to dial
		toCall = textViewPhoneNumber.getText().toString();
//		if (true) {
//			toCall = PhoneNumberUtils.stripSeparators(toCall);
//		}
//
//		toCall = rewriteNumber(toCall);

		if (TextUtils.isEmpty(toCall)) {
			return;
		}

		// -- MAKE THE CALL --//
		if (accountToUse >= 0) {
			// It is a SIP account, try to call service for that
			try {
				service.makeCallWithOptions(toCall, accountToUse.intValue(), b);
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Service can't be called to make the call");
			}
		} else if (accountToUse != SipProfile.INVALID_ID) {
//			// It's an external account, find correct external account
//			CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
//			ch.loadFrom(accountToUse, toCall, new OnLoadListener() {
//				@Override
//				public void onLoad(CallHandlerPlugin ch) {
//					placePluginCall(ch);
//				}
//			});
		}
	}

	@Override
	public void onDataReceive(byte[] buffer, int index, int packlen) {
		byte[] reciveBuf = new byte[packlen];
		System.arraycopy(buffer, index, reciveBuf, 0, packlen);

		if (reciveBuf[reciveBuf.length - 1] != CRC8.calcCrc8(reciveBuf, 0,
				reciveBuf.length - 1)) {
			mShowToastThread = new ShowToastThread(this, "CRC校验未通过");
			mShowToastThread.start();
			return;
		}
		
		switch (reciveBuf[ProtocolManager.CMDCODE_INDEX]) {
		case ProtocolManager.CmdCode.UPLOAD_CARD:
			if (ProtocolManager.UserMode.CARDANDPASSWORD == reciveBuf[ProtocolManager.USERMODE_INDEX]){
				//读取密码进入下个界面
				byte[] password = new byte[6];
				System.arraycopy(reciveBuf, ProtocolManager.CMDCODE_INDEX + 5, password, 0, password.length);
				Intent passwordIntent = new Intent(this,PasswordOpenDoorActivity.class);
				passwordIntent.putExtra("password", password);
				startActivity(passwordIntent);
			}
			break;
		
		default:
			break;
		}
	}

}
