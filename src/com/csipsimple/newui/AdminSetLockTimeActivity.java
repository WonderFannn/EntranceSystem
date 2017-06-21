package com.csipsimple.newui;


import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.LocalNameManager;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class AdminSetLockTimeActivity extends Activity implements
		OnDataReceiveListener {

	private static final String TAG = "AdminSetLockTimeActivity";

	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;
	
	private TextView tvShowEditNum;
	private TextView tvMessage;
	private TextView tvLocalName;
	
	private String lockTime = "";

	private void findViews() {
		
		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());
		tvShowEditNum = (TextView) findViewById(R.id.tv_show_edit_num);
		tvMessage = (TextView) findViewById(R.id.tv_message);
		
		tvMessage.setText("请输入0-255秒范围内回锁时间\n设置0s为不可开门\n设置255s为单元门常开\n*键确认.\n#键删除或返回上级\n");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_admin_setting_base);
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
			refreshLockTime(keyCode-7);
			break;
		case 155:
			if (isNumeric(lockTime) && 255 >= Integer.parseInt(lockTime)) {
				byte lockTimeByte = (byte) Integer.parseInt(lockTime);
				byte[] mBuffer = {(byte) 0xF8,					//命令字
						//验证模式卡号密码用ff填充
						(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,
						(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,
						(byte) 0x01,//模式
						(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,//用户数,管理员数
						(byte) 0x00,
						lockTimeByte,
						(byte) 0x00};//包尾
				mSerialPortUtil.sendBuffer(mBuffer);
			}else {
				mShowToastThread = new ShowToastThread(this,"无效的时间值");
				mShowToastThread.start();
			}
			break;
		case 18:
			if (lockTime.length() > 0) {
				lockTime = lockTime.substring(0, lockTime.length()-1);
				tvShowEditNum.setText(lockTime);
			}else {
				finish();
			}
			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}


	private void refreshLockTime(int i) {
		String lockTimeString = lockTime + i;
		if (Integer.valueOf(lockTimeString) >= 0 && Integer.valueOf(lockTimeString) <= 255) {
			lockTime = lockTimeString;
			tvShowEditNum.setText(lockTime);
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
		case ProtocolManager.CmdCode.SET_LOCK_TIME:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]){
				mShowToastThread = new ShowToastThread(this, "设置回锁时间"+lockTime+"s成功");
				mShowToastThread.start();
				finish();
			}else {
				mShowToastThread = new ShowToastThread(this, "设置回锁时间失败");
				mShowToastThread.start();
				
			}
			break;
		
		default:
			break;
		}
	}
	
	private boolean isNumeric(String str){
		for(int i=str.length();--i>=0;){
			int chr=str.charAt(i);
			if(chr<48 || chr>57)
				return false;
		}
		return true;
	}

}
