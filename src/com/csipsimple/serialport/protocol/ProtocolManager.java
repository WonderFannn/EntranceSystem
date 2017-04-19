package com.csipsimple.serialport.protocol;


public class ProtocolManager {
	
	public static final int CMDCODE_INDEX = 5;
	public static final int RETURN_STATUS_INDEX = 6;
	public static final int USERMODE_INDEX = 17;
	//校验码用于检验包头包尾
	public static class CheckCode{
		public static final byte PACK_HEAD_1 = (byte) 0x55;
		public static final byte PACK_HEAD_2 = (byte) 0xAA;
		public static final byte PACK_HEAD_3 = (byte) 0x55;
	}
	
	//校验包中的命令码
	public static class CmdCode{
		public static final byte ADD_ADMIN = (byte) 0xF0;
		public static final byte DELETE_ADMIN = (byte) 0xF1;
		public static final byte ADD_USER = (byte) 0xF2;
		public static final byte DELETE_USER = (byte) 0xF3;
		public static final byte VERIFY_ADMIN = (byte) 0xF4;
		public static final byte READ_CARD_NUM = (byte) 0xF5;
		public static final byte VERIFY_USER = (byte) 0xF6;
		public static final byte OPEN_DOOR = (byte) 0xF7;
		public static final byte SET_LOCK_TIME = (byte) 0xF8;
		public static final byte MODIFY = (byte) 0xF9;
		public static final byte CLEAR_USER = (byte) 0xFA;
		public static final byte CLEAR_ADMIN = (byte) 0xFB;
		public static final byte UPLOAD_CARD = (byte) 0xFC;
		public static final byte PASSWORD_OPEN_DOOR = (byte) 0xFD;
	}
	
	public static class ReturnStatus{
		public static final byte SUCCESS = (byte) 0x00;
		public static final byte FAIL = (byte) 0x01;
		public static final byte COVER = (byte) 0x02;
	}
	
	public static class UserMode{
		public static final byte CARD = (byte) 0x01;
		public static final byte PASSWORD = (byte) 0x02;
		public static final byte CARDANDPASSWORD = (byte) 0x03;
	}
	
}
