/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.service;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.widget.Toast;
import android_serialport_api.SerialPortUtil;

import com.csipsimple.R;
import com.csipsimple.api.IRecongService;
import com.csipsimple.api.ISipConfiguration;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipManager.PresenceStatus;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipUri;
import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;
import com.csipsimple.newui.EntranceActivity;
import com.csipsimple.pjsip.PjSipCalls;
import com.csipsimple.pjsip.PjSipService;
import com.csipsimple.pjsip.UAStateReceiver;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.Hex;
import com.csipsimple.service.SipService.FinalizeDestroyRunnable;
import com.csipsimple.service.receiver.DynamicReceiver4;
import com.csipsimple.service.receiver.DynamicReceiver5;
import com.csipsimple.ui.incall.InCallMediaControl;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.ExtraPlugins;
import com.csipsimple.utils.ExtraPlugins.DynActivityPlugin;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecongService extends Service {

	
	// static boolean creating = false;
	private static final String THIS_FILE = "SIP SRV";

	private SipWakeLock sipWakeLock;
	private boolean autoAcceptCurrent = false;
	public boolean supportMultipleCalls = false;
	
	// For video testing -- TODO : remove
	private static RecongService singleton = null;
	// Implement public interface for the service
	private final IRecongService.Stub binder = new IRecongService.Stub() {
		
		

		@Override
		public int openLock() throws RemoteException {
			// TODO Auto-generated method stub
			SerialPortUtil mSerialPortUtil = SerialPortUtil.getInstance();
			byte[] mBuffer;
			mBuffer = Hex.byteMerger(ProtocolManager.CmdCode.OPEN_DOOR, ProtocolManager.invalidId);//5
			mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidPassword);//11
			mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultMode);//12
			mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//15
			mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//16
			mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//17
			mSerialPortUtil.sendBuffer(mBuffer);
			return 0;
		}
	};





	@Override
	public IBinder onBind(Intent intent) {

		String serviceName = intent.getAction();
		Log.d(THIS_FILE, "Action is " + serviceName);

		return binder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
//        android.os.Debug.waitForDebugger();
		singleton = this;



	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(THIS_FILE, "Destroying SIP Service");
	}
}