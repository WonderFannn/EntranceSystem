package com.csipsimple.newui;

import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;
import com.csipsimple.serialport.util.LocalNameManager;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class AdminSelectRelayActivity extends Activity implements OnDataReceiveListener {

	private static final String TAG = "AdminSettingActivity";

	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;

	private TextView tvLocalName;
	private TextView tvMessage;
	
	private String[] relayStrings = {"继电器1","继电器2","继电器并用"};
	private int relayIndex = 0;
	private void findViews() {
		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());
		tvMessage = (TextView) findViewById(R.id.tv_message);
		tvMessage.setText("按数字键选择继电器,#键返回\n1.继电器1\n2.继电器2\n3.继电器并用\n");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_showmessage);
		findViews();
		mSerialPortUtil = SerialPortUtil.getInstance();

	}
	@Override
	protected void onResume() {
		super.onResume();
		mSerialPortUtil.setOnDataReceiveListener(this);
	}
	@Override
	protected void onPause() {
		super.onPause();
		mSerialPortUtil.closeReadThread();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();

	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case 8:
		case 9:
		case 10:
			selectRelay(keyCode - 7);
			break;
		case 155:
			// *键

			break;
		case 18:
			// #键
			finish();

			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void selectRelay(int i) {
		relayIndex = i-1;
		byte[] mBuffer;
		mBuffer = Hex.byteMerger(ProtocolManager.CmdCode.SELECT_RELAY,(byte) i);//2
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidId);//6
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidPassword);//12
		//单独约定少加一位的mode
		//mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultMode);//13
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//16 - 1
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//17 -1
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//18 - 1
		mSerialPortUtil.sendBuffer(mBuffer);
	}
	@Override
	public void onDataReceive(byte[] buffer, int index, int packlen) {
		Log.d(TAG, "onDataReceive");
		byte[] reciveBuf = new byte[packlen];
		System.arraycopy(buffer, index, reciveBuf, 0, packlen);

		if (reciveBuf[reciveBuf.length - 1] != CRC8.calcCrc8(reciveBuf, 0,
				reciveBuf.length - 1)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "CRC校验未通过",
							Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}

		switch (reciveBuf[ProtocolManager.CMDCODE_INDEX]) {

		case ProtocolManager.CmdCode.SELECT_RELAY:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "设置"+relayStrings[relayIndex]+"成功");
				mShowToastThread.start();
				finish();
			} else {
				mShowToastThread = new ShowToastThread(this, "设置"+relayStrings[relayIndex]+"失败");
				mShowToastThread.start();
			}

			break;
		default:
			break;
		
		}
	}

}
