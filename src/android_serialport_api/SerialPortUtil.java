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
 * 串口操作类
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
    //串口名
    //虚拟机使用
//    private String path = "/dev/ttyS3";
    //实机测试使用
    private String path = "/dev/ttyS7";
    //波特率
    private int baudrate = 115200;
//    private int baudrate = 9600;
    private static SerialPortUtil portUtil;
    private OnDataReceiveListener onDataReceiveListener = null;
    private boolean isStop = false;

    public interface OnDataReceiveListener {
        public void onDataReceive(final byte[] buffer,final int index, final int packlen);
    }

    public void setOnDataReceiveListener(
            OnDataReceiveListener dataReceiveListener) {
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
     * 初始化串口信息 
     */
    public void onCreate() {
        try {
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            mReadThread = new ReadThread();
            isStop = false;
            mReadThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendBuffer(byte[] mBuffer) {
    	// - 添加包头,长度
    	int bufferlength = mBuffer.length;
    	byte[] buffer = new byte[bufferlength+5];
    	buffer[0] = (byte) 0x55;
    	buffer[1] = (byte) 0xAA;
    	buffer[2] = (byte) 0x55;
    	buffer[3] = (byte) 0x00;
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
            super.run();
            // 定义一个包的最大长度  
            int maxLength = 128;
            byte[] buffer = new byte[maxLength];
            // 每次收到实际长度  
            int available = 0;
            // 当前已经收到包的总长度 
            int currentLength = 0;
            // 协议头长度5个字节（包头3，长度2）  
            int headerLength = 5;
            
            while (!isInterrupted()) {
                try {
                    available = mInputStream.available();
                    if (available > 0) {
                        // 防止超出数组最大长度导致溢出
                        if (available > maxLength - currentLength) {
                            available = maxLength - currentLength;
                        }
                        mInputStream.read(buffer, currentLength, available);
                        currentLength += available;
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                int cursor = 0;  
                // 如果当前收到包大于头的长度，则解析当前包  
                while (currentLength >= headerLength) {  
                    // 取到头部第一个字节  
                    if ((buffer[cursor] != (byte)0x55)
                    		&& (buffer[cursor+1] != (byte)0xAA) 
                    		&& (buffer[cursor+2] != (byte)0x55)) {
                        --currentLength;
                        ++cursor;
                        continue;
                    }
                    int contentLenght = parseLen(buffer, cursor, headerLength);
                    // 如果内容包的长度大于最大内容长度或者小于等于0，则说明这个包有问题，丢弃 
                    if (contentLenght <= 0 || contentLenght > maxLength - 5) {
                        currentLength = 0;
                        break;
                    }
                    // 如果当前获取到长度小于整个包的长度，则跳出循环等待继续接收数据 
                    int factPackLen = contentLenght + 5;
                    if (currentLength < factPackLen) {
                        break;
                    }
                    // 一个完整包即产生  ,传给onDataReceive处理
                    onDataReceiveListener.onDataReceive(buffer, cursor, factPackLen);
                    Log.d(TAG,"ReceiveBuffer: "+Hex.encodeHexStr(buffer).substring(0,currentLength)+ "size : "+currentLength);
                    currentLength -= factPackLen;
                    cursor += factPackLen;
                }
                // 残留字节移到缓冲区首  
                if (currentLength > 0 && cursor > 0) {  
                    System.arraycopy(buffer, cursor, buffer, 0, currentLength);  
                    currentLength = 0;
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
    
    /** 
     * 关闭串口 
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