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
	
	private String[] relayStrings = {"�̵���1","�̵���2","�̵�������"};
	private int relayIndex = 0;
	private void findViews() {
		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());
		tvMessage = (TextView) findViewById(R.id.tv_message);
		tvMessage.setText("�����ּ�ѡ��̵���,#������\n1.�̵���1\n2.�̵���2\n3.�̵�������\n");
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
			// *��

			break;
		case 18:
			// #��
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
		//����Լ���ټ�һλ��mode
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
					Toast.makeText(getApplicationContext(), "CRCУ��δͨ��",
							Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}

		switch (reciveBuf[ProtocolManager.CMDCODE_INDEX]) {

		case ProtocolManager.CmdCode.SELECT_RELAY:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "����"+relayStrings[relayIndex]+"�ɹ�");
				mShowToastThread.start();
				finish();
			} else {
				mShowToastThread = new ShowToastThread(this, "����"+relayStrings[relayIndex]+"ʧ��");
				mShowToastThread.start();
			}

			break;
		default:
			break;
		
		}
	}

}
