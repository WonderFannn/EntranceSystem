package com.csipsimple.newui;

import java.util.Arrays;

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
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class PasswordOpenDoorActivity extends Activity implements
		OnDataReceiveListener {
	
	private static final String TAG = "PasswordOpenDoorActivity";
	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;

	private TextView tvMessage;
	private TextView[] tvPassword = new TextView[6];
	private int[] textViewID = { R.id.tv_password_1, R.id.tv_password_2,
			R.id.tv_password_3, R.id.tv_password_4, R.id.tv_password_5,
			R.id.tv_password_6 };

	private byte[] verifyPassword = new byte[6];
	
	private int[] newPassword = new int[6];
	private int newPasswordIndex = 0;
	
	private TextView tvLocalName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_modify_password);

		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());
		Intent intent = getIntent();
		verifyPassword = intent.getByteArrayExtra("password");
		
		tvMessage = (TextView) findViewById(R.id.tv_message);
		tvMessage.setText("������6λ����\n*������.\n#��ɾ������򷵻��ϼ�\n");
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
			refreshNewPasswordNumber(keyCode - 7);
			break;
		case 155:
			// *��
			if (newPasswordIndex == 6) {
				
				byte[] passwordByte = new byte[6];
				for (int i = 0; i < passwordByte.length; i++) {
					passwordByte[i] = (byte) newPassword[i];
				}
				
				if (verifyPassword == null) {
					// �������뿪������
					byte[] mBuffer;
					mBuffer = Hex.byteMerger(ProtocolManager.CmdCode.PASSWORD_OPEN_DOOR, ProtocolManager.invalidId);//5
					mBuffer = Hex.byteMerger(mBuffer, passwordByte);//11
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.UserMode.PASSWORD);//12
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//15
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//16
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//17
					mSerialPortUtil.sendBuffer(mBuffer);
				}else {
					if (Arrays.equals(verifyPassword,passwordByte)) {
						//����ֱ�ӿ�������
					    //F7,FFFFFFFF,FFFFFFFFFFFF,FF,07D00A,0001
						byte[] mBuffer;
						mBuffer = Hex.byteMerger(ProtocolManager.CmdCode.OPEN_DOOR, ProtocolManager.invalidId);//5
						mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidPassword);//11
						mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultMode);//12
						mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//15
						mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//16
						mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//17
						mSerialPortUtil.sendBuffer(mBuffer);
						mShowToastThread = new ShowToastThread(this, "�ɹ�����");
						mShowToastThread.start();
						finish();
						
					}else {
						//�������,���������������
						mShowToastThread = new ShowToastThread(this, "�������,����������");
						mShowToastThread.start();
						newPasswordIndex = 0;
						for (int i = 0; i < 6; i++) {
							tvPassword[i].setText("");
						}
					}
				}
			} else {
				mShowToastThread = new ShowToastThread(this, "������6λ������");
				mShowToastThread.start();
			}
			break;
		case 18:
			// #��
			switch (newPasswordIndex) {
			case 0:
				finish();
				break;
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
				newPasswordIndex--;
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
			tvPassword[newPasswordIndex].setText(i + "");
			newPasswordIndex++;
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

		if (reciveBuf[reciveBuf.length-1] != CRC8.calcCrc8(reciveBuf, 0, reciveBuf.length-1)){
			mShowToastThread = new ShowToastThread(this, "CRCУ��δͨ��");
			mShowToastThread.start();
			return;
		}

		switch (reciveBuf[ProtocolManager.CMDCODE_INDEX]) {

		case ProtocolManager.CmdCode.PASSWORD_OPEN_DOOR:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "�ɹ�����");
				mShowToastThread.start();
				finish();
			} else {
				mShowToastThread = new ShowToastThread(this, "�������");
				mShowToastThread.start();
			}

			break;
		default:
			break;
		}
	}
}