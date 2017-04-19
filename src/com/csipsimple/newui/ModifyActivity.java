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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class ModifyActivity extends Activity implements OnDataReceiveListener {

	private static final String TAG = "ModifyActivity";
	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;

	private TextView mTitleTextView;
	private TextView mOffTextView;
	private Handler mOffHandler;
	private Builder mDialogBuilder;
	private Timer mOffTime;
	private Dialog mDialog;
	private boolean isVerify = false;

	private TextView textViewUserId;
	private TextView textViewUserPassword;
	private TextView textViewUserMode;

	private String userId ;
	private byte[] IDbyte = {(byte)0x07,(byte)0x5b,(byte) 0xcd,(byte)0x15};
	private String password ;
	private byte[] passwordByte = {1,2,3,4,5,6};
	private int mode = 0;
	private String[] modeStrings = {"未获取到模式","卡号","密码","卡号+密码"};

	private void findViews() {
		textViewUserId = (TextView) findViewById(R.id.TextView_user_ID);
		textViewUserPassword = (TextView) findViewById(R.id.TextView_user_password);
		textViewUserMode = (TextView) findViewById(R.id.TextView_user_mode);
		textViewUserId.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_modify);
		mSerialPortUtil = SerialPortUtil.getInstance();
		mSerialPortUtil.setOnDataReceiveListener(this);
		byte[] mBuffer = {
				ProtocolManager.CmdCode.VERIFY_USER,// 命令字
				// 验证模式卡号密码用ff填充
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,	(byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, 
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF,// 用户数,管理员数
				(byte) 0x00, (byte) 0x00 };// 包尾
		mSerialPortUtil.sendBuffer(mBuffer);
		findViews();
		if (!isVerify) {
			initDialog("用户");
		}
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
		mTitleTextView = (TextView) mDialogWindow.findViewById(R.id.tv_dialog_title);
		mTitleTextView.setText(userName+"验证");
		mOffTextView = (TextView) mDialogWindow.findViewById(R.id.tv_dialog_message);
		mDialog.setCanceledOnTouchOutside(false);
		mOffHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what > 0) {
					mOffTextView.setText("    请刷" + userName + "卡：" + msg.what +"    ");
				} else {
					if (mDialog != null) {
						mDialog.dismiss();
						if (!isVerify) {
							finish();
						}
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case 8:
			//进入修改密码界面
			Intent passwordIntent = new Intent(this,ModifyPasswordActivity.class);
			passwordIntent.putExtra("userIDByte", IDbyte);
			passwordIntent.putExtra("passwordByte", passwordByte);
			passwordIntent.putExtra("mode",	mode);
			startActivityForResult(passwordIntent, 0);
			break;
		case 9:
			Intent modeIntent = new Intent(this,ModifyModeActivity.class);
			modeIntent.putExtra("userIDByte", IDbyte);
			modeIntent.putExtra("passwordByte", passwordByte);
			modeIntent.putExtra("mode",	mode);
			startActivityForResult(modeIntent, 1);
			//进入修改模式界面
			break;
		case 18:
			finish();
			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case 0:
			passwordByte = data.getByteArrayExtra("passwordByte");
			password = "";
			if (passwordByte != null) {
				for (int i = 0; i < passwordByte.length; i++) {
					password += (int) passwordByte[i];
				}
			}
			textViewUserPassword.setText(password);
			break;
		case 1:
			mode = data.getIntExtra("mode", 0);
			textViewUserMode.setText(modeStrings[mode]);
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
		
		if (reciveBuf[reciveBuf.length-1] != CRC8.calcCrc8(reciveBuf, 0, reciveBuf.length-1)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "CRC校验未通过", Toast.LENGTH_SHORT).show();
				}
			});
        	return;
		}
		
		switch (reciveBuf[ProtocolManager.CMDCODE_INDEX]) {
		case ProtocolManager.CmdCode.VERIFY_USER:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				if (isDialogExist()) {
					destroyDialog();
					isVerify = true;
					// 计算卡号
					int IDindex = ProtocolManager.RETURN_STATUS_INDEX + 1;
					IDbyte = new byte[4];
					System.arraycopy(reciveBuf, IDindex, IDbyte, 0, 4);
					userId = Long.valueOf(Hex.encodeHexStr(IDbyte), 16)
							.toString();
					while (userId.length() < 10) {
						userId = "0" + userId;
					}
					// 获取密码
					password = "";
					int passwordIndex = ProtocolManager.RETURN_STATUS_INDEX + 5;
					passwordByte = new byte[6];
					System.arraycopy(reciveBuf, passwordIndex, passwordByte, 0,
							6);
					for (int i = 0; i < passwordByte.length; i++) {
						password += (int) passwordByte[i];
					}
					// 获取模式
					int modeIndex = ProtocolManager.RETURN_STATUS_INDEX + 11;
					mode = reciveBuf[modeIndex] % 4;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							textViewUserId.setText(userId);
							textViewUserPassword.setText(password);
							textViewUserMode.setText(modeStrings[mode]);
						}
					});
				}

			} else if (ProtocolManager.ReturnStatus.FAIL == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this, "验证失败");
				mShowToastThread.start();
			}
			break;
		default:
			break;
		}
	}


}
