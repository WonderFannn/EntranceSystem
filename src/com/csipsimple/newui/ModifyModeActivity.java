package com.csipsimple.newui;

import java.nio.ByteBuffer;

import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;
import com.csipsimple.serialport.util.LocalNameManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class ModifyModeActivity extends Activity implements
		OnDataReceiveListener {

	private static final String TAG = "ModifyModeActivity";
	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;

	private byte[] userIDByte;
	private byte[] passwordByte;
	private int mode;
	private int newMode;
	
	private TextView tvLocalName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_showmessage);

		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());
		mSerialPortUtil = SerialPortUtil.getInstance();

		Intent passwordIntent = getIntent();
		userIDByte = passwordIntent.getByteArrayExtra("userIDByte");
		passwordByte = passwordIntent.getByteArrayExtra("passwordByte");
		mode = passwordIntent.getIntExtra("mode", 0);

	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mSerialPortUtil.setOnDataReceiveListener(this);
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mSerialPortUtil.closeReadThread();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case 8:
		case 9:
		case 10:
			changeMode(keyCode - 7);
			break;
		case 155:
			// *键

			break;
		case 18:
			// #键
			Intent intent = new Intent();
			intent.putExtra("mode", mode);
			setResult(RESULT_OK, intent);
			finish();

			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void changeMode(int i) {
		newMode = i;
		// 计算出用户IDbyte
		byte[] byteId = userIDByte;
		// 密码转为byte数组
		byte[] bytePassword = passwordByte;
		byte[] mBuffer;
		mBuffer = Hex.byteMerger(ProtocolManager.CmdCode.MODIFY, byteId);//5
		mBuffer = Hex.byteMerger(mBuffer, bytePassword);//11
		mBuffer = Hex.byteMerger(mBuffer, (byte) newMode);//12
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//15
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//16
		mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//17
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

		case ProtocolManager.CmdCode.MODIFY:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "设置模式成功");
				mShowToastThread.start();
				mode = newMode;
				Intent intent = new Intent();
				intent.putExtra("mode", mode);
				setResult(RESULT_OK, intent);
				finish();
			} else {
				mShowToastThread = new ShowToastThread(this, "设置模式失败");
				mShowToastThread.start();
			}

			break;
		default:
			break;
		}
	}
}
