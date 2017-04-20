package com.csipsimple.newui;

import com.csipsimple.R;
import com.csipsimple.newui.view.ShowToastThread;
import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android_serialport_api.SerialPortUtil;
import android_serialport_api.SerialPortUtil.OnDataReceiveListener;

public class GodModeActivity extends Activity implements OnClickListener, OnDataReceiveListener {

	
	private SerialPortUtil mSerialPortUtil;
	private ShowToastThread mShowToastThread;
	
	private EditText editTextSendBuffer;
	private TextView textViewReceive1;
	private TextView textViewReceive2;
	private TextView textViewReceive3;
	private TextView[] textViews = new TextView[3];
	private int textViewsIndex = 0;
	private Button buttonCmdCode;
	private Button button4F;
	private Button button6F;
	private Button button01;
	private Button button3F;
	private Button buttonSend;
	private Button Button00;
	private Button Button1F;
	private Button Button123456;
	private Button Button3B480CC5;
	private Button Button4BE437C5;
	private Button ButtonClear;
	private Button Button9B59F9C4;
	private Button ButtonDefault1;
	private Button ButtonDefault2;
	private Button ButtonDefault3;
	private Button ButtonDefault4;
	private Button ButtonReturn;

	private AlertDialog.Builder builder;
	private AlertDialog dialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_godmode);
		findViews();
		mSerialPortUtil = SerialPortUtil.getInstance();
		mSerialPortUtil.setOnDataReceiveListener(this);
	}
	/**
	 * Find the Views in the layout<br />
	 * <br />
	 * Auto-created on 2017-04-19 16:21:09 by Android Layout Finder
	 * (http://www.buzzingandroid.com/tools/android-layout-finder)
	 */
	private void findViews() {
		editTextSendBuffer = (EditText)findViewById( R.id.editText_sendBuffer );
		textViewReceive1 = (TextView)findViewById( R.id.textView_receive1 );
		textViewReceive2 = (TextView)findViewById( R.id.textView_receive2 );
		textViewReceive3 = (TextView)findViewById( R.id.textView_receive3 );
		textViews[0] = textViewReceive1;
		textViews[1] = textViewReceive2;
		textViews[2] = textViewReceive3;
		buttonCmdCode = (Button)findViewById( R.id.button_cmdCode );
		button4F = (Button)findViewById( R.id.button_4F );
		button6F = (Button)findViewById( R.id.button_6F );
		button01 = (Button)findViewById( R.id.button_01 );
		button3F = (Button)findViewById( R.id.button_3F );
		buttonSend = (Button)findViewById( R.id.button_send );
		Button00 = (Button)findViewById( R.id.Button_00 );
		Button1F = (Button)findViewById( R.id.Button_1F );
		Button123456 = (Button)findViewById( R.id.Button_123456 );
		Button3B480CC5 = (Button)findViewById( R.id.Button_3B480CC5 );
		Button4BE437C5 = (Button)findViewById( R.id.Button_4BE437C5 );
		ButtonClear = (Button)findViewById( R.id.Button_clear );
		Button9B59F9C4 = (Button)findViewById( R.id.Button_9B59F9C4 );
		ButtonDefault1 = (Button)findViewById( R.id.Button_default1 );
		ButtonDefault2 = (Button)findViewById( R.id.Button_default2 );
		ButtonDefault3 = (Button)findViewById( R.id.Button_default3 );
		ButtonDefault4 = (Button)findViewById( R.id.Button_default4 );
		ButtonReturn = (Button)findViewById( R.id.Button_return );

		buttonCmdCode.setOnClickListener( this );
		button4F.setOnClickListener( this );
		button6F.setOnClickListener( this );
		button01.setOnClickListener( this );
		button3F.setOnClickListener( this );
		buttonSend.setOnClickListener( this );
		Button00.setOnClickListener( this );
		Button1F.setOnClickListener( this );
		Button123456.setOnClickListener( this );
		Button3B480CC5.setOnClickListener( this );
		Button4BE437C5.setOnClickListener( this );
		ButtonClear.setOnClickListener( this );
		Button9B59F9C4.setOnClickListener( this );
		ButtonDefault1.setOnClickListener( this );
		ButtonDefault2.setOnClickListener( this );
		ButtonDefault3.setOnClickListener( this );
		ButtonDefault4.setOnClickListener( this );
		ButtonReturn.setOnClickListener( this );
	}

	/**
	 * Handle button click events<br />
	 * <br />
	 * Auto-created on 2017-04-19 16:21:09 by Android Layout Finder
	 * (http://www.buzzingandroid.com/tools/android-layout-finder)
	 */
	@Override
	public void onClick(View v) {
		if ( v == buttonCmdCode ) {
			// Handle clicks for buttonCmdCode
			showSimpleListDialog(v);
		} else if ( v == button4F ) {
			editTextSendBuffer.append(" FF FF FF FF");
		} else if ( v == button6F ) {
			editTextSendBuffer.append(" FF FF FF FF FF FF");
		} else if ( v == button01 ) {
			editTextSendBuffer.append(" 01");
		} else if ( v == button3F ) {
			editTextSendBuffer.append(" FF FF FF");
		} else if ( v == buttonSend ) {
			String string = editTextSendBuffer.getText().toString();
			
			string = string.replaceAll(" ","");
			
			Log.d("wangfan", string);
			if (string.length()%2 == 0) {
				byte[] mBuffer = Hex.decodeHex(string.toCharArray());
				mSerialPortUtil.sendBuffer(mBuffer);
			}else {
				mShowToastThread = new ShowToastThread(this, "命令不正确");
				mShowToastThread.start();
			}
		} else if ( v == Button00 ) {
			editTextSendBuffer.append(" 00");
		} else if ( v == Button1F ) {
			editTextSendBuffer.append(" FF");
		} else if ( v == Button123456 ) {
			editTextSendBuffer.append(" 01 02 03 04 05 06");
		} else if ( v == Button3B480CC5 ) {
			editTextSendBuffer.append(" 3B 48 0C C5");
		} else if ( v == Button4BE437C5 ) {
			editTextSendBuffer.append(" 4B E4 37 C5");
		} else if ( v == ButtonClear ) {
			editTextSendBuffer.setText("");
		} else if ( v == Button9B59F9C4 ) {
			editTextSendBuffer.append(" 9B 59 F9 C4");
		} else if ( v == ButtonDefault1 ) {
			// Handle clicks for ButtonDefault1
		} else if ( v == ButtonDefault2 ) {
			// Handle clicks for ButtonDefault2
		} else if ( v == ButtonDefault3 ) {
			// Handle clicks for ButtonDefault3
		} else if ( v == ButtonDefault4 ) {
			// Handle clicks for ButtonDefault4
		} else if ( v == ButtonReturn ) {
			finish();
		}
	}
	@Override
	public void onDataReceive(byte[] buffer, int index, int packlen) {

		final byte[] reciveBuf = new byte[packlen];
		System.arraycopy(buffer, index, reciveBuf, 0, packlen);

		if (reciveBuf[reciveBuf.length - 1] != CRC8.calcCrc8(reciveBuf, 0,
				reciveBuf.length - 1)) {
			mShowToastThread = new ShowToastThread(this, "CRC校验未通过");
			mShowToastThread.start();
			return;
		}
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textViews[(textViewsIndex++)%3].setText(Hex.encodeHexStr(reciveBuf,false));
			}
		});
	}
	
	private void showSimpleListDialog(View view) {
        builder=new AlertDialog.Builder(this);
        builder.setTitle("选择命令");

        /**
         * 设置内容区域为简单列表项
         */
        final String[] Items={"F0添加管理员","F1删除管理员","F2添加用户","F3删除用户","F4验证管理员","F5读取用户卡号",
        		"F6验证用户","F7开门","F8设置回锁时间","F9修改密码模式","FA清空用户","FB清空管理员","FD密码开门"};
        builder.setItems(Items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                editTextSendBuffer.append(" "+Items[i].substring(0, 2));
                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        dialog=builder.create();
        dialog.show();
    }

}
