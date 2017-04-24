package com.csipsimple.serialport.util;

import java.io.IOException;


public class Hex {

	/**
	 * ���ڽ���ʮ�������ַ��������Сд�ַ�����
	 */
	private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * ���ڽ���ʮ�������ַ�������Ĵ�д�ַ�����
	 */
	private static final char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * ���ֽ�����ת��Ϊʮ�������ַ�����
	 * 
	 * @param data
	 *            byte[]
	 * @return ʮ������char[]
	 */
	public static char[] encodeHex(byte[] data) {
		return encodeHex(data, true);
	}

	/**
	 * ���ֽ�����ת��Ϊʮ�������ַ�����
	 * 
	 * @param data
	 *            byte[]
	 * @param toLowerCase
	 *            <code>true</code> ������Сд��ʽ �� <code>false</code> �����ɴ�д��ʽ
	 * @return ʮ������char[]
	 */
	public static char[] encodeHex(byte[] data, boolean toLowerCase) {
		return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
	}

	/**
	 * ���ֽ�����ת��Ϊʮ�������ַ�����
	 * 
	 * @param data
	 *            byte[]
	 * @param toDigits
	 *            ���ڿ��������char[]
	 * @return ʮ������char[]
	 */
	protected static char[] encodeHex(byte[] data, char[] toDigits) {
		int l = data.length;
		char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return out;
	}

	/**
	 * ���ֽ�����ת��Ϊʮ�������ַ���
	 * 
	 * @param data
	 *            byte[]
	 * @return ʮ������String
	 */
	public static String encodeHexStr(byte[] data) {
		return encodeHexStr(data, true);
	}

	/**
	 * ���ֽ�����ת��Ϊʮ�������ַ���
	 * 
	 * @param data
	 *            byte[]
	 * @param toLowerCase
	 *            <code>true</code> ������Сд��ʽ �� <code>false</code> �����ɴ�д��ʽ
	 * @return ʮ������String
	 */
	public static String encodeHexStr(byte[] data, boolean toLowerCase) {
		return encodeHexStr(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
	}

	/**
	 * ���ֽ�����ת��Ϊʮ�������ַ���
	 * 
	 * @param data
	 *            byte[]
	 * @param toDigits
	 *            ���ڿ��������char[]
	 * @return ʮ������String
	 */
	protected static String encodeHexStr(byte[] data, char[] toDigits) {
		return new String(encodeHex(data, toDigits));
	}

	/**
	 * ��ʮ�������ַ�����ת��Ϊ�ֽ�����
	 * 
	 * @param data
	 *            ʮ������char[]
	 * @return byte[]
	 * @throws RuntimeException
	 *             ���Դʮ�������ַ�������һ����ֵĳ��ȣ����׳�����ʱ�쳣
	 */
	public static byte[] decodeHex(char[] data) {

		int len = data.length;

		if ((len & 0x01) != 0) {
			throw new RuntimeException("Odd number of characters.");
		}

		byte[] out = new byte[len >> 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; j < len; i++) {
			int f = toDigit(data[j], j) << 4;
			j++;
			f = f | toDigit(data[j], j);
			j++;
			out[i] = (byte) (f & 0xFF);
		}

		return out;
	}

	/**
	 * ��ʮ�������ַ�ת����һ������
	 * 
	 * @param ch
	 *            ʮ������char
	 * @param index
	 *            ʮ�������ַ����ַ������е�λ��
	 * @return һ������
	 * @throws RuntimeException
	 *             ��ch����һ���Ϸ���ʮ�������ַ�ʱ���׳�����ʱ�쳣
	 */
	protected static int toDigit(char ch, int index) {
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new RuntimeException("Illegal hexadecimal character " + ch
					+ " at index " + index);
		}
		return digit;
	}

//	public static void main(String[] args) {
//		String srcStr = "10234560";
//		String encodeStr = encodeHexStr(srcStr.getBytes());
//		String decodeStr = new String(decodeHex(encodeStr.toCharArray()));
//		System.out.println("ת��ǰ��" + srcStr);
//		System.out.println("ת����" + encodeStr);
//		System.out.println("��ԭ��" + decodeStr);
//	}
	
	public static void main(String[] args) {
		byte[] bytes;
		byte[] byte_1 = new byte[100];
		byte[] byte_2 = new byte[1000];
		long then = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			bytes = byteMerger(byte_1, byte_2);
		}
		long now = System.currentTimeMillis();
		System.out.println("time1 :"+ (now - then));
		long then1 = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			bytes = byteMergerByFor(byte_1, byte_2);
		}
		long now1 = System.currentTimeMillis();
		System.out.println("time2 :"+ (now1 - then1));

	}
	
	public static byte[] byteMergerByFor(byte[] byte_1, byte[] byte_2) {
		byte[] byte_3 = new byte[byte_1.length+byte_2.length];
		for (int i = 0; i < byte_3.length; i++) {
			if(i<byte_1.length){
				byte_3[i] = byte_1[i];
			}else {
				byte_3[i] = byte_2[i - byte_1.length];
			}
		}
		return byte_3;
	}
	
	public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){  
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];  
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);  
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);  
        return byte_3;
    }
	
	public static byte[] byteMerger(byte byte_1, byte[] byte_2){  
        byte[] byte_3 = new byte[1+byte_2.length];  
        byte_3[0] = byte_1;
        System.arraycopy(byte_2, 0, byte_3, 1, byte_2.length);  
        return byte_3;
    }
	
	public static byte[] byteMerger(byte[] byte_1, byte byte_2){  
        byte[] byte_3 = new byte[byte_1.length+1];  
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);  
        byte_3[byte_1.length] = byte_2;
        return byte_3;
    }
	
	public static byte[] byteMerger(byte byte_1, byte byte_2){  
        byte[] byte_3 = new byte[2];  
        byte_3[0] = byte_1;
        byte_3[1] = byte_2;
        return byte_3;
    }
	
	public static int byteToInt(byte[] userNumbyte) {
		int sum = 0;
		for (byte b:userNumbyte) {
			sum = (sum << 8) | (b & 0xff);
		}
		return sum;
	}

}
