package com.csipsimple.newui;


import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;
import com.csipsimple.serialport.util.LocalNameManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class ModifyPasswordActivity extends Activity implements
		OnDataReceiveListener {

	private static final String TAG = "ModifyPasswordActivity";
	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;

	private TextView[] tvPassword = new TextView[6];
	private int[] textViewID = { R.id.tv_password_1, R.id.tv_password_2,
			R.id.tv_password_3, R.id.tv_password_4, R.id.tv_password_5,
			R.id.tv_password_6 };

	private byte[] userIDByte;
	private byte[] passwordByte;
	private int mode;
	private int[] newPassword = new int[6];
	private int newPasswordIndex = 0;
	
	private TextView tvLocalName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_modify_password);
		
		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());

		Intent passwordIntent = getIntent();
		userIDByte = passwordIntent.getByteArrayExtra("userIDByte");
		passwordByte = passwordIntent.getByteArrayExtra("passwordByte");
		mode = passwordIntent.getIntExtra("mode", 0);
		for (int i = 0; i < textViewID.length; i++) {
			tvPassword[i] = (TextView) findViewById(textViewID[i]);
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mSerialPortUtil = SerialPortUtil.getInstance();
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
			refreshNewPasswordNumber(keyCode-7);
			break;
		case 155:
//			*��
			if (newPasswordIndex == 6) {
				if (newPassword[0] == newPassword[1] && newPassword[0] == newPassword[2]
					&& newPassword[0] == newPassword[3]	&& newPassword[0] == newPassword[4]
					&& newPassword[0] == newPassword[5]) {
					
					mShowToastThread = new ShowToastThread(this, "����������ڼ�");
					mShowToastThread.start();
					
				}else {
					// ������û�IDbyte
					byte[] byteId = userIDByte;
					// ����תΪbyte����
					byte[] bytePassword = new byte[6];
					for (int i = 0; i < newPasswordIndex; i++) {
						bytePassword[i] = (byte) newPassword[i];
					}
					
					byte[] mBuffer;
					mBuffer = Hex.byteMerger(ProtocolManager.CmdCode.MODIFY, byteId);//5
					mBuffer = Hex.byteMerger(mBuffer, bytePassword);//11
					mBuffer = Hex.byteMerger(mBuffer, (byte) mode);//12
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//15
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//16
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//17
					mSerialPortUtil.sendBuffer(mBuffer);
				}
			}else {
				mShowToastThread = new ShowToastThread(this, "��������ȷ������");
				mShowToastThread.start();
			}
			break;
		case 18:
//			#��
			switch (newPasswordIndex) {
			case 0:
				Intent intent = new Intent();
                intent.putExtra("passwordByte", passwordByte);
                setResult(RESULT_OK, intent);
                finish();
				break;
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
				newPasswordIndex --;
				tvPassword[newPasswordIndex].setText("");
			default:
				break;
			}
			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	private void refreshNewPasswordNumber(int i) {
		switch (newPasswordIndex) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			newPassword[newPasswordIndex] = i;
			tvPassword[newPasswordIndex].setText(i+"");
			newPasswordIndex ++;
			break;
		default:
			break;
		}
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
					Toast.makeText(getApplicationContext(), "CRCУ��δͨ��",
							Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}

		switch (reciveBuf[ProtocolManager.CMDCODE_INDEX]) {

		case ProtocolManager.CmdCode.MODIFY:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "��������ɹ�");
				mShowToastThread.start();
				for (int i = 0; i < passwordByte.length; i++) {
					passwordByte[i] = (byte) newPassword[i];
				}
				Intent intent = new Intent();
                intent.putExtra("passwordByte", passwordByte);
                setResult(RESULT_OK, intent);
				finish();
			} else {
				mShowToastThread = new ShowToastThread(this, "��������ʧ��");
				mShowToastThread.start();
			}

			break;
		default:
			break;
		}
	}
}
