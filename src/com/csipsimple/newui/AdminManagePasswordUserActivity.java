package com.csipsimple.newui;


import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;
import com.csipsimple.serialport.util.LocalNameManager;

import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class AdminManagePasswordUserActivity extends Activity implements OnDataReceiveListener {
	private static final String TAG = "AdminAddPasswordUserActivity";

	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;
	
	private TextView tvShowEditNum;
	private TextView tvMessage;
	private TextView tvLocalName;
	
	private String userIDAndPassword = "";
	
	private byte cmdCode;

	private void findViews() {
		tvShowEditNum = (TextView) findViewById(R.id.tv_show_edit_num);
		tvMessage = (TextView) findViewById(R.id.tv_message);
		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());
		String message = "";
		switch (cmdCode) {
		case ProtocolManager.CmdCode.ADD_USER:
			message = "添加用户";
			break;
		case ProtocolManager.CmdCode.DELETE_USER:
			message = "删除用户";
			break;

		default:
			break;
		}
		tvMessage.setText("请输入房间号和6位密码如0506123456\n*键确认"+message+".\n#键修改输入或返回上级\n");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_admin_setting_base);
		Intent intent = getIntent();
		cmdCode = intent.getByteExtra("cmdCode", ProtocolManager.CmdCode.ADD_USER);
		findViews();
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
	protected void onDestroy() {
		super.onDestroy();
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
			refreshIDNumber(keyCode-7);
			break;
		case 155:
			// long maxValue = 4294967295L;//16进制表示为0xff ff ff ff
			if (userIDAndPassword.length() == 10) {
				String id = Long.toHexString(Long.parseLong("80000"+userIDAndPassword.substring(0, 4)));
				while (id.length() < 8) {
					id = "0" + id;
				}
				Log.d(TAG, "id:----" + id + "-----");
				byte[] byteId = Hex.decodeHex(id.toCharArray());
				String password = userIDAndPassword.substring(4, 10);
				byte[] bytePassword = new byte[6];
				for (int i = 0; i < bytePassword.length; i++) {
					bytePassword[i] =  (byte) Integer.parseInt(""+password.charAt(i));
				}
				byte[] mBuffer;
				mBuffer = Hex.byteMerger(cmdCode, byteId);// 5
				mBuffer = Hex.byteMerger(mBuffer, bytePassword);// 11
				mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.UserMode.PASSWORD);// 12
				mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);// 15
				mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);// 16
				mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);// 17
				mSerialPortUtil.sendBuffer(mBuffer);
			} else {
				mShowToastThread = new ShowToastThread(this, "请输入4位房间号加6位密码");
				mShowToastThread.start();
			}
			
			break;
		case 18:
			if(userIDAndPassword.length() > 0){
				userIDAndPassword = userIDAndPassword.substring(0, userIDAndPassword.length()-1);
				tvShowEditNum.setText(userIDAndPassword);
			}else {
				finish();
			}
			
			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void refreshIDNumber(int i) {
		if (userIDAndPassword.length() < 10) {
			userIDAndPassword += i;
			tvShowEditNum.setText(userIDAndPassword);
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
		case ProtocolManager.CmdCode.ADD_USER:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]){
				mShowToastThread = new ShowToastThread(this, "添加密码用户成功");
				mShowToastThread.start();
				finish();
			}else if (ProtocolManager.ReturnStatus.FAIL == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "添加密码用户失败");
				mShowToastThread.start();
			}else {
				mShowToastThread = new ShowToastThread(this, "已覆盖存在的用户");
				mShowToastThread.start();
				finish();
			}
			break;
		case ProtocolManager.CmdCode.DELETE_USER:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]){
				mShowToastThread = new ShowToastThread(this, "删除密码用户成功");
				mShowToastThread.start();
				finish();
			}else {
				mShowToastThread = new ShowToastThread(this, "删除密码用户失败");
				mShowToastThread.start();
			}
			break;
		default:
			break;
		}
	}
	

}
