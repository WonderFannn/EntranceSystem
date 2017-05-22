package com.csipsimple.newui;

import com.csipsimple.R;
import com.csipsimple.utils.CameraManager;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class ShowCameraActivity extends Activity {
	private CameraManager frontCameraManager;
	/**
	 * ����ǰ���йصĲ���
	 */
	private SurfaceView frontSurfaceView;
	private SurfaceHolder frontHolder;
	private boolean isFrontOpened = false;
	private Camera mFrontCamera;

	/**
	 * �Զ��Խ��Ļص���������������Խ��ɹ�/���ɹ�����¼�
	 */
	private AutoFocusCallback mAutoFocus = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO:��ʵ��
			if (success) {
				mFrontCamera.takePicture(null, null,
						frontCameraManager.new PicCallback(mFrontCamera));
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_camera);

		initView();

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		// if (isFrontOpened == false &&
		// frontCameraManager.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT))
		// {
		// mFrontCamera = frontCameraManager.getCamera();
		// // �Զ��Խ�
		// mFrontCamera.autoFocus(mAutoFocus);
		// isFrontOpened = true;
		// // ����
		// }
		super.onResume();
		takePhoto();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		frontCameraManager.releaseCamera();
		super.onPause();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	public void buttonListener(View view) {
		switch (view.getId()) {
		case R.id.openFront_button:
			mFrontCamera.autoFocus(mAutoFocus);
			break;

		default:
			break;
		}
	}

	private void initView() {
		/**
		 * ��ʼ��ǰ���������
		 */
		// ��ʼ��surface view
		frontSurfaceView = (SurfaceView) findViewById(R.id.front_surfaceview);
		// ��ʼ��surface holder
		frontHolder = frontSurfaceView.getHolder();
		frontCameraManager = new CameraManager(this,mFrontCamera, frontHolder);
	}

	/**
	 * @return ����ǰ������ͷ����
	 */
	// private void takeFrontPhoto() {
	// if (isFrontOpened == false
	// && frontCameraManager
	// .openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
	// mFrontCamera = frontCameraManager.getCamera();
	// // �Զ��Խ�
	// mFrontCamera.autoFocus(mAutoFocus);
	// isFrontOpened = true;
	// // ����
	// }
	// mFrontCamera.takePicture(null, null,frontCameraManager.new
	// PicCallback(mFrontCamera));
	// }

	protected void takePhoto() {
		// ����ÿ��߳̽������գ���ΪActivity��δ��ȫ��ʾ��ʱ�����޷��������յģ�SurfaceView��������ʾ
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (mFrontCamera == null) {
					frontCameraManager.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
					try {
						// ��Ϊ��������ͷ��Ҫʱ�䣬�������߳�˯2��
						Thread.sleep(5000);
						mFrontCamera = frontCameraManager.getCamera();
					} catch (InterruptedException e) {
					}
				}else {
					mFrontCamera.takePicture(null, null,
							frontCameraManager.new PicCallback(mFrontCamera));
				}
			}
		}).start();
	}

}
