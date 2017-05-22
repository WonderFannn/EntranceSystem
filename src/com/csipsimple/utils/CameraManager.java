package com.csipsimple.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import mobile.ReadFace.YMFace;
import mobile.ReadFace.YMFaceTrack;
//import mobile.ReadFace.YMFaceTrack;

//import mobile.ReadFace.YMFace;
//import mobile.ReadFace.YMFaceTrack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;

public class CameraManager {

	private String TAG = getClass().getSimpleName();
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private YMFaceTrack faceTrack;
    private Context mContext;
    public CameraManager(Context context,Camera camera, SurfaceHolder holder) {
        mCamera = camera;
        mHolder = holder;
        mContext = context;
        faceTrack = new YMFaceTrack();
        
    }

    public Camera getCamera() {
        return mCamera;
    }

    /**
     * �����
     * 
     * @param camera
     *            ���������
     * @param holder
     *            ����ʵʱչʾȡ�������ݵĿؼ�
     * @param tagInfo
     *            ����ͷ��Ϣ����Ϊǰ��/��������ͷ Camera.CameraInfo.CAMERA_FACING_FRONT��ǰ��
     *            Camera.CameraInfo.CAMERA_FACING_BACK������
     * @return �Ƿ�ɹ���ĳ������ͷ
     */
    public boolean openCamera(int tagInfo) {
        // ���Կ�������ͷ
        try {
            mCamera = Camera.open(getCameraId(tagInfo));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
        // ����ǰ��ʧ��
        if (mCamera == null) {
            return false;
        }
        // ������ͷ�е�ͼ��չʾ��holder��
        try {
            // �����myCameraΪ�Ѿ���ʼ����Camera����
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
            // ����������̽��д���ֹͣԤ����Ƭ
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        // ����ɹ���ʼʵʱԤ��
        mCamera.startPreview();
        return true;
    }

    public void releaseCamera() {
    	mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
	}
    /**
     * @return ǰ������ͷ��ID
     */
    public int getFrontCameraId() {
        return getCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    /**
     * @return ��������ͷ��ID
     */
    public int getBackCameraId() {
        return getCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * @param tagInfo
     * @return �õ��ض�camera info��id
     */
    private int getCameraId(int tagInfo) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        // ��ʼ��������ͷ���õ�camera info
        int cameraId, cameraCount;
        for (cameraId = 0, cameraCount = Camera.getNumberOfCameras(); cameraId < cameraCount; cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);

            if (cameraInfo.facing == tagInfo) {
                break;
            }
        }
        return cameraId;
    }

    /**
     * ����ͼƬ�����·����ͼƬ������
     */
    public final static String PHOTO_PATH = "mnt/sdcard/CAMERA_DEMO/Camera/";

    public static String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'LOCK'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }
    
    /**
     * ���ճɹ��ص�
     */
    public class PicCallback implements PictureCallback {
        private String TAG = getClass().getSimpleName();
        private Camera mCamera;

        public PicCallback(Camera camera) {
            // TODO �Զ����ɵĹ��캯�����
            mCamera = camera;
        }

        /* 
         * �����յõ����ֽ�תΪbitmap��Ȼ����ת������д��SD��
         * @param data 
         * @param camera
         */
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // ���õ�����Ƭ����270����ת��ʹ����ֱ
        	
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
//            matrix.preRotate(270);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (faceTrack.initTrack(mContext, YMFaceTrack.FACE_0, YMFaceTrack.RESIZE_WIDTH_640)) {
    			Log.d(TAG, "׷������ʼ���ɹ�");
    			YMFace ymFace = faceTrack.track(data, bitmap.getWidth(), bitmap.getHeight());
    			if (ymFace != null) {
    				float[] rect = ymFace.getRect();
    				Log.d(TAG, "����ʶ��ɹ�!!!");
    				Log.d(TAG, "rect:"+rect[0]+"*"+rect[1]);
    			}
    		}else {
    			Log.d(TAG, "׷������ʼ��ʧ��");
			}
//             ����������ͼƬ�ļ�
            File mFile = new File(PHOTO_PATH);
            if (!mFile.exists()) {
                mFile.mkdirs();
            }
            File pictureFile = new File(PHOTO_PATH, getPhotoFileName());
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                bitmap.recycle();
                fos.close();
                Log.i(TAG, "����ɹ���");
            } catch (Exception error) {
                Log.e(TAG, "����ʧ��");
                error.printStackTrace();
            } finally {
//                mCamera.stopPreview();
//                mCamera.release();
//                mCamera = null;
            }
        }

    }

}