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
	 * 定义前置有关的参数
	 */
	private SurfaceView frontSurfaceView;
	private SurfaceHolder frontHolder;
	private boolean isFrontOpened = false;
	private Camera mFrontCamera;

	/**
	 * 自动对焦的回调方法，用来处理对焦成功/不成功后的事件
	 */
	private AutoFocusCallback mAutoFocus = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO:空实现
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
		// // 自动对焦
		// mFrontCamera.autoFocus(mAutoFocus);
		// isFrontOpened = true;
		// // 拍照
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
		 * 初始化前置相机参数
		 */
		// 初始化surface view
		frontSurfaceView = (SurfaceView) findViewById(R.id.front_surfaceview);
		// 初始化surface holder
		frontHolder = frontSurfaceView.getHolder();
		frontCameraManager = new CameraManager(this,mFrontCamera, frontHolder);
	}

	/**
	 * @return 开启前置摄像头照相
	 */
	// private void takeFrontPhoto() {
	// if (isFrontOpened == false
	// && frontCameraManager
	// .openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
	// mFrontCamera = frontCameraManager.getCamera();
	// // 自动对焦
	// mFrontCamera.autoFocus(mAutoFocus);
	// isFrontOpened = true;
	// // 拍照
	// }
	// mFrontCamera.takePicture(null, null,frontCameraManager.new
	// PicCallback(mFrontCamera));
	// }

	protected void takePhoto() {
		// 这里得开线程进行拍照，因为Activity还未完全显示的时候，是无法进行拍照的，SurfaceView必须先显示
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (mFrontCamera == null) {
					frontCameraManager.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
					try {
						// 因为开启摄像头需要时间，这里让线程睡2秒
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
