package com.csipsimple.newui;

import java.util.Timer;
import java.util.TimerTask;

import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.protocol.ProtocolManager;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;
import com.csipsimple.serialport.util.LocalNameManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;


public class AdminSettingActivity extends Activity implements OnDataReceiveListener {
	
	private static final String TAG = "AdminSettingActivity";

	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;

	private TextView tvLocalName;
	private TextView tvMessage;
	
	private TextView mTitleTextView;
	private TextView mOffTextView;
	private Handler mOffHandler;
	private Timer mOffTime;
	private Dialog mDialog;
	private Builder mDialogBuilder;
	private boolean isVerify = false;
	
	private void findViews() {
		tvLocalName = (TextView) findViewById(R.id.tv_localname);
		tvLocalName.setText(LocalNameManager.readFile());
		tvMessage = (TextView) findViewById(R.id.tv_message);
		tvMessage.setText("�����ּ�ѡ����,#������\n1.����û�		2.ɾ���û�\n3.��������û�		4.ɾ�������û�\n5.���û���ʱ��\n6.ѡ��̵���\n7.����չ�豸		8.�ر���չ�豸");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_showmessage);
		findViews();
		mSerialPortUtil = SerialPortUtil.getInstance();
		//55AA550012F4FFFFFFFF FFFFFFFFFFFFFF07D00A0000
		byte[] mBuffer = {(byte) 0xF4,//������
							//��֤ģʽ����������ff���
							(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,
							(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,
							(byte) 0x00,//mode
							(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,//�û���,����Ա��
							(byte) 0x00,(byte) 0x00};//��β
		mSerialPortUtil.sendBuffer(mBuffer);
		if (!isVerify) {
			initDialog("����Ա");
		}
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
		mTitleTextView.setText(userName+"��֤");
		mOffTextView = (TextView) mDialogWindow.findViewById(R.id.tv_dialog_message);
		mDialog.setCanceledOnTouchOutside(false);
		mOffHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what > 0) {
					mOffTextView.setText("    ��ˢ" + userName + "����" + msg.what +"    ");
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
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case 8:
			Intent intent1 = new Intent(this,AdminManageUserActivity.class);
			intent1.putExtra("cmdCode", ProtocolManager.CmdCode.ADD_USER);
			startActivity(intent1);
			break;
		case 9:
			Intent intent2 = new Intent(this,AdminManageUserActivity.class);
			intent2.putExtra("cmdCode", ProtocolManager.CmdCode.DELETE_USER);
			startActivity(intent2);
			break;
		case 10:
			Intent intent3 = new Intent(this,AdminManagePasswordUserActivity.class);
			intent3.putExtra("cmdCode", ProtocolManager.CmdCode.ADD_USER);
			startActivity(intent3);
			break;
		case 11:
			Intent intent4 = new Intent(this,AdminManagePasswordUserActivity.class);
			intent4.putExtra("cmdCode", ProtocolManager.CmdCode.DELETE_USER);
			startActivity(intent4);
			break;
		case 12:
			Intent intent5 = new Intent(this,AdminSetLockTimeActivity.class);
			startActivity(intent5);
			break;
		case 13:
			Intent intent6 = new Intent(this,AdminSelectRelayActivity.class);
			startActivity(intent6);
			break;
		case 14:
			byte[] mBufferOpenExtendRelay;
			mBufferOpenExtendRelay = Hex.byteMerger(ProtocolManager.CmdCode.OPEN_EXTEND_RELAY, ProtocolManager.invalidId);//5
			mBufferOpenExtendRelay = Hex.byteMerger(mBufferOpenExtendRelay, ProtocolManager.invalidPassword);//11
			mBufferOpenExtendRelay = Hex.byteMerger(mBufferOpenExtendRelay, ProtocolManager.defaultMode);//12
			mBufferOpenExtendRelay = Hex.byteMerger(mBufferOpenExtendRelay, ProtocolManager.invalidSum);//15
			mBufferOpenExtendRelay = Hex.byteMerger(mBufferOpenExtendRelay, ProtocolManager.defaultFrame);//16
			mBufferOpenExtendRelay = Hex.byteMerger(mBufferOpenExtendRelay, ProtocolManager.defaultCRC);//17
			mSerialPortUtil.sendBuffer(mBufferOpenExtendRelay);
			break;
		case 15:
			byte[] mBufferCloseExtendRelay;
			mBufferCloseExtendRelay = Hex.byteMerger(ProtocolManager.CmdCode.CLOSE_EXTEND_RELAY, ProtocolManager.invalidId);//5
			mBufferCloseExtendRelay = Hex.byteMerger(mBufferCloseExtendRelay, ProtocolManager.invalidPassword);//11
			mBufferCloseExtendRelay = Hex.byteMerger(mBufferCloseExtendRelay, ProtocolManager.defaultMode);//12
			mBufferCloseExtendRelay = Hex.byteMerger(mBufferCloseExtendRelay, ProtocolManager.invalidSum);//15
			mBufferCloseExtendRelay = Hex.byteMerger(mBufferCloseExtendRelay, ProtocolManager.defaultFrame);//16
			mBufferCloseExtendRelay = Hex.byteMerger(mBufferCloseExtendRelay, ProtocolManager.defaultCRC);//17
			mSerialPortUtil.sendBuffer(mBufferCloseExtendRelay);
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
	public void onDataReceive(byte[] buffer, int index, int packlen) {
		byte[] reciveBuf = new byte[packlen];  
        System.arraycopy(buffer, index, reciveBuf, 0, packlen);
        
        if (reciveBuf[reciveBuf.length-1] != CRC8.calcCrc8(reciveBuf, 0, reciveBuf.length-1)) {
        	mShowToastThread = new ShowToastThread(this,"CRCУ��δͨ��");
			mShowToastThread.start();
        	return;
		}
        
        switch (reciveBuf[ProtocolManager.CMDCODE_INDEX]) {
		case ProtocolManager.CmdCode.VERIFY_ADMIN:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				if (isDialogExist()) {
					destroyDialog();
					isVerify = true;
				}
			}else if (ProtocolManager.ReturnStatus.FAIL == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this,"��֤ʧ��");
				mShowToastThread.start();
			}
			break;
		case ProtocolManager.CmdCode.OPEN_EXTEND_RELAY:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this,"����չ�豸�ɹ�");
				mShowToastThread.start();
			}else if (ProtocolManager.ReturnStatus.FAIL == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this,"����չ�豸ʧ��");
				mShowToastThread.start();
			}
			break;
		case ProtocolManager.CmdCode.CLOSE_EXTEND_RELAY:
			if (ProtocolManager.ReturnStatus.SUCCESS == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this,"�ر���չ�豸�ɹ�");
				mShowToastThread.start();
			}else if (ProtocolManager.ReturnStatus.FAIL == reciveBuf[ProtocolManager.RETURN_STATUS_INDEX]) {
				mShowToastThread = new ShowToastThread(this,"�ر���չ�豸ʧ��");
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
	
}
