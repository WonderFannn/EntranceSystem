package android_serialport_api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.csipsimple.serialport.util.CRC8;
import com.csipsimple.serialport.util.Hex;


import android.R.integer;
import android.util.Log;

/**
 * ���ڲ�����
 * 
 * @author
 *
 */
public class SerialPortUtil {
    private String TAG = SerialPortUtil.class.getSimpleName();
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    //������
    //�����ʹ��
//    private String path = "/dev/ttyS3";
    //ʵ������ʹ��
    private String path = "/dev/ttyS7";
    //������
    private int baudrate = 115200;
    private static SerialPortUtil portUtil;
    private OnDataReceiveListener onDataReceiveListener = null;
    private boolean isStop = false;

    public interface OnDataReceiveListener {
        public void onDataReceive(final byte[] buffer,final int index, final int packlen);
    }

    public void setOnDataReceiveListener(
            OnDataReceiveListener dataReceiveListener) {
    	mReadThread = new ReadThread();
    	isStop = false;
    	mReadThread.start();
        onDataReceiveListener = dataReceiveListener;
    }

    public static SerialPortUtil getInstance() {
        if (null == portUtil) {
            portUtil = new SerialPortUtil();
            portUtil.onCreate();
        }
        return portUtil;
    }
    
    public boolean isStop() {
		return isStop;
	}

    /**
     * ��ʼ��������Ϣ 
     */
    public void onCreate() {
        try {
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendBuffer(byte[] mBuffer) {
    	// - ��Ӱ�ͷ,����
    	int bufferlength = mBuffer.length;
    	byte[] buffer = new byte[bufferlength+5];
    	buffer[0] = (byte) 0x55;
    	buffer[1] = (byte) 0xAA;
    	buffer[2] = (byte) 0x55;
    	buffer[3] = (byte) (bufferlength>>8);
    	buffer[4] = (byte) bufferlength;
    	System.arraycopy(mBuffer, 0, buffer, 5, bufferlength);
    	buffer[buffer.length-1] = CRC8.calcCrc8(buffer, 0, buffer.length-1);
        boolean result = true;
        Log.d(TAG,"sendBuffer: "+Hex.encodeHexStr(buffer)+ "size : "+buffer.length);
        try {
            if (mOutputStream != null) {
                mOutputStream.write(buffer);
            } else {
                result = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }  
  
    private class ReadThread extends Thread {
  
        @Override  
        public void run() {
//            super.run();
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);
            // ����һ��������󳤶�  
            int maxLength = 128;
            byte[] buffer = new byte[maxLength];
            // ÿ���յ�ʵ�ʳ���  
            int available = 0;
            // ��ǰ�Ѿ��յ������ܳ��� 
            int currentLength = 0;
            // Э��ͷ����5���ֽڣ���ͷ3������2��  
            int headerLength = 5;
            
            while (!isInterrupted()) {
                try {
                	sleep(100);
                    available = mInputStream.available();
                    if (available > 0) {
                        // ��ֹ����������󳤶ȵ������
                        if (available > maxLength - currentLength) {
                            available = maxLength - currentLength;
                        }
                        mInputStream.read(buffer, currentLength, available);
                        currentLength += available;
                        Log.d(TAG,"FirstRead    : "+Hex.encodeHexStr(buffer).substring(0,currentLength*2)+ "size : "+currentLength);
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                int cursor = 0;  
                // �����ǰ�յ�������ͷ�ĳ��ȣ��������ǰ��  
                while (currentLength >= headerLength) {  
                    // ȡ��ͷ����һ���ֽ�  
                    if ((buffer[cursor] != (byte)0x55)
                    		&& (buffer[cursor+1] != (byte)0xAA) 
                    		&& (buffer[cursor+2] != (byte)0x55)) {
                        --currentLength;
                        ++cursor;
                        continue;
                    }
                    // ���ݵ�4,5λ��������ݳ�
                    int contentLenght = parseLen(buffer, cursor, headerLength);
                    // ������ݰ��ĳ��ȴ���������ݳ��Ȼ���С�ڵ���0����˵������������⣬���� 
                    if (contentLenght <= 0 || contentLenght > maxLength - 5) {
                        currentLength = 0;
                        break;
                    }
                    // �����ǰ��ȡ������С���������ĳ��ȣ�������ѭ���ȴ������������� 
                    int factPackLen = contentLenght + 5;
                    if (currentLength < factPackLen) {
                        break;
                    }
                    // һ��������������  ,����onDataReceive����
                    onDataReceiveListener.onDataReceive(buffer, cursor, factPackLen);
                    Log.d(TAG,"ReceiveBuffer: "+Hex.encodeHexStr(buffer).substring(0,factPackLen*2)+ "size : "+currentLength);
                    currentLength -= factPackLen;
                    cursor += factPackLen;
                }
                // �����ֽ��Ƶ���������  
                if (currentLength > 0 && cursor > 0) {  
                    System.arraycopy(buffer, cursor, buffer, 0, currentLength);  
//                    currentLength = 0;
                }
            }
        }
    }
    
    public int parseLen(byte buffer[], int index, int headerLength) {  
    	  
        byte a = buffer[index + 3];
        byte b = buffer[index + 4];
        int rlt = ((a << 8) | b);
        return rlt;  
    }
    
    
    public void closeReadThread() {
    	if (mReadThread != null) {
    		mReadThread.interrupt();
    	}
    }
    
    /** 
     * �رմ��� 
     */  
    public void closeSerialPort() {
        isStop = true;
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        if (mSerialPort != null) {
            mSerialPort.close();
        }
    }

}