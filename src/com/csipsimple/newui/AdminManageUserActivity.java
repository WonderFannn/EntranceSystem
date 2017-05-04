package com.csipsimple.newui;

import java.util.Timer;
import java.util.TimerTask;

import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class AdminManageUserActivity extends Activity implements
		OnDataReceiveListener {

	private static final String TAG = "AdminManageUserActivity";

	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;
	
	private TextView tvShowEditNum;
	private TextView tvMessage;
	
	private String userID = "";
	
	private byte cmdCode;

	private TextView mTitleTextView;
	private TextView mOffTextView;
	private Handler mOffHandler;
	private Timer mOffTime;
	private Dialog mDialog;
	private Builder mDialogBuilder;

	private void findViews() {
		tvShowEditNum = (TextView) findViewById(R.id.tv_show_edit_num);
		tvMessage = (TextView) findViewById(R.id.tv_message);
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
		tvMessage.setText("请输入用户卡号\n*键确认"+message+"或读卡.\n#键清空卡号或返回上级\n");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_admin_setting_base);
		Intent intent = getIntent();
		cmdCode = intent.getByteExtra("cmdCode", ProtocolManager.CmdCode.ADD_USER);
		findViews();
		mSerialPortUtil = SerialPortUtil.getInstance();
		mSerialPortUtil.setOnDataReceiveListener(this);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isDialogExist()) {
			destroyDialog();
		}
	}

	void initDialog(final String userName) {

		mDialogBuilder = new AlertDialog.Builder(this);

		mDialog = mDialogBuilder.create();
		mDialog.show();
		Window mDialogWindow = mDialog.getWindow();
		mDialogWindow.setContentView(R.layout.alert_dialog_verify_info);
		mTitleTextView = (TextView) mDialogWindow
				.findViewById(R.id.tv_dialog_title);
		mTitleTextView.setText(userName + "验证");
		mOffTextView = (TextView) mDialogWindow
				.findViewById(R.id.tv_dialog_message);
		mDialog.setCanceledOnTouchOutside(false);
		mOffHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what > 0) {
					mOffTextView.setText("    请刷" + userName + "卡：" + msg.what
							+ "    ");
				} else {
					if (mDialog != null) {
						mDialog.dismiss();
					}
					mOffTime.cancel();
				}
				super.handleMessage(msg);
			}
		};
		mOffTime = new Timer(true);
		TimerTask tt = new TimerTask() {
			int countTime = 15;

			public void run() {
				if (countTime > 0) {
					countTime--;
				}
				Message msg = new Message();
				msg.what = countTime;
				mOffHandler.sendMessage(msg);
			}
		};
		mOffTime.schedule(tt, 1000, 1000);
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
			if (userID == null || userID.equals("")) {
				byte[] mBuffer = {(byte) 0xF5,//命令字
						//验证模式卡号密码用ff填充
						(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,
						(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,
						(byte) 0x00,(byte) 0x05,(byte) 0x0A,//用户数,管理员数
						(byte) 0x00,(byte) 0x00};//包尾
				mSerialPortUtil.sendBuffer(mBuffer);
				initDialog("用户");
			}else {
				long maxValue = 4294967295L;//16进制表示为0xff ff ff ff
				if (isNumeric(userID) && maxValue >= Long.parseLong(userID)) {
					String id = Long.toHexString(Long.parseLong(userID));
					while (id.length() < 8) {
						id = "0"+id;
					}
					Log.d(TAG,"id:----"+id+"-----");
					byte[] byteId = Hex.decodeHex(id.toCharArray());
					byte[] mBuffer;
					mBuffer = Hex.byteMerger(cmdCode, byteId);//5
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultPassword);//11
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultMode);//12
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//15
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//16
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//17
					mSerialPortUtil.sendBuffer(mBuffer);
				}else {
					mShowToastThread = new ShowToastThread(this,"无效的用户号");
					mShowToastThread.start();
				}
			}
			break;
		case 18:
			if(userID.length() > 0){
				userID = userID.substring(0, userID.length()-1);
				tvShowEditNum.setText(userID);
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
		if (userID.length() < 10) {
			userID += i;
			tvShowEditNum.setText(userID);
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
		case ProtocolManager.CmdCode.READ_CARD_NUM:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				if (isDialogExist()) {
					destroyDialog();
					byte[] readId = new byte[4];
					for (int i = 0; i < readId.length; i++) {
						readId[i] = reciveBuf[ProtocolManager.RETURN_STATUS_INDEX+1+i];
					}
					byte[] mBuffer;
					mBuffer = Hex.byteMerger(cmdCode, readId);//5
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultPassword);//11
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultMode);//12
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.invalidSum);//15
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultFrame);//16
					mBuffer = Hex.byteMerger(mBuffer, ProtocolManager.defaultCRC);//17
					
					mSerialPortUtil.sendBuffer(mBuffer);
				}
			} else if (ProtocolManager.ReturnStatus.FAIL == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "读取卡号失败");
				mShowToastThread.start();
			}
			break;
		case ProtocolManager.CmdCode.ADD_USER:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]){
				mShowToastThread = new ShowToastThread(this, "添加用户成功");
				mShowToastThread.start();
				finish();
			}else {
				mShowToastThread = new ShowToastThread(this, "添加用户失败");
				mShowToastThread.start();
			}
			break;
		case ProtocolManager.CmdCode.DELETE_USER:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]){
				mShowToastThread = new ShowToastThread(this, "删除用户成功");
				mShowToastThread.start();
				finish();
			}else {
				mShowToastThread = new ShowToastThread(this, "删除用户失败");
				mShowToastThread.start();
			}
			break;
		default:
			break;
		}
	}

	private boolean isDialogExist() {
		return mDialog != null && mOffTime != null;
	}

	private void destroyDialog() {
		mDialog.dismiss();
		mDialog = null;
		mOffTime.cancel();
		mOffTime.purge();
		mOffTime = null;
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
