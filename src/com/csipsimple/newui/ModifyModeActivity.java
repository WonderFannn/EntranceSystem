package com.csipsimple.newui;

import java.nio.ByteBuffer;

import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_showmessage);

		mSerialPortUtil = SerialPortUtil.getInstance();
		mSerialPortUtil.setOnDataReceiveListener(this);

		Intent passwordIntent = getIntent();
		userIDByte = passwordIntent.getByteArrayExtra("userIDByte");
		passwordByte = passwordIntent.getByteArrayExtra("passwordByte");
		mode = passwordIntent.getIntExtra("mode", 0);

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
		// 发送修改模式命令
		int contentLength = 18;
		byte[] mbuffer = new byte[contentLength];
		// 包头
		byte[] byteHead = { ProtocolManager.CmdCode.MODIFY };
		// 计算出用户IDbyte
		byte[] byteId = userIDByte;
		// 密码转为byte数组
		byte[] bytePassword = passwordByte;
		int mBufferIndex = 0;
		System.arraycopy(byteHead, 0, mbuffer, mBufferIndex, byteHead.length);
		mBufferIndex += byteHead.length;
		System.arraycopy(byteId, 0, mbuffer, mBufferIndex, byteId.length);
		mBufferIndex += byteId.length;
		System.arraycopy(bytePassword, 0, mbuffer, mBufferIndex,
				bytePassword.length);
		mBufferIndex += bytePassword.length;
		mbuffer[mBufferIndex++] = (byte) i;
		
		SharedPreferences cardData = getSharedPreferences("CardData", MODE_PRIVATE);
		int userNum = cardData.getInt("UserNum", 0);
		int adminNum = cardData.getInt("AdminNum", 0);
		byte[] userNumByte = ByteBuffer.allocate(4).putInt(userNum).array();
		System.arraycopy(userNumByte, 2, mbuffer, mBufferIndex, 2);
		mBufferIndex += 2;
		mbuffer[mBufferIndex++] = (byte) adminNum;
		
		while (mBufferIndex < contentLength) {
			mbuffer[mBufferIndex] = (byte) 0x01;
			mBufferIndex++;
		}
		mSerialPortUtil.sendBuffer(mbuffer);
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
