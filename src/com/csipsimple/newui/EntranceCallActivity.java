package com.csipsimple.newui;

import java.util.Timer;
import java.util.TimerTask;

import org.webrtc.videoengine.ViERenderer;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipCallSession.StatusCode;
import com.csipsimple.newui.view.PowerImageView;
//import com.csipsimple.newui.view.PowerImageView;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.incall.CallProximityManager;
import com.csipsimple.ui.incall.CallProximityManager.ProximityDirector;
import com.csipsimple.ui.incall.IOnCallActionTrigger;
import com.csipsimple.ui.incall.InCallMediaControl;
import com.csipsimple.utils.CallsUtils;
import com.csipsimple.utils.Log;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EntranceCallActivity extends Activity implements
		IOnCallActionTrigger, ProximityDirector {

	private static final String THIS_FILE = "EntranceCallActivity";

	private ViewGroup mainFrame;
	
	private SurfaceView cameraPreview;
	private SurfaceView renderView;

	private Object callMutex = new Object();
	private SipCallSession[] callsInfo = null;

	// Screen wake lock for incoming call
	private WakeLock wakeLock;
	// Screen wake lock for video
	private WakeLock videoWakeLock;

	private Timer quitTimer;
	private static final int QUIT_DELAY = 30000;
	
	private PowerImageView pivCallStatus;
	private TextView tvCallStatus;

	/**
	 * Service binding
	 */
	private boolean serviceConnected = false;
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			try {
				// Log.d(THIS_FILE,
				// "Service started get real call info "+callInfo.getCallId());
				callsInfo = service.getCalls();
				serviceConnected = true;

				runOnUiThread(new UpdateUIFromCallRunnable());
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Can't get back the call", e);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			serviceConnected = false;
			callsInfo = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_entrance_call);
		Log.d("wangfan", THIS_FILE + " start");
		
		mainFrame = (ViewGroup) findViewById(R.id.mainFrame);
		pivCallStatus = (PowerImageView) findViewById(R.id.piv_callstatus);
		tvCallStatus = (TextView) findViewById(R.id.tv_callstatus);
		SipCallSession initialSession = getIntent().getParcelableExtra(SipManager.EXTRA_CALL_INFO);
		synchronized (callMutex) {
			callsInfo = new SipCallSession[1];
			callsInfo[0] = initialSession;
		}
		
		bindService(new Intent(this, SipService.class), connection,
				Context.BIND_AUTO_CREATE);

		if (quitTimer == null) {
            quitTimer = new Timer("Quit-timer");
        }

//		ScreenLocker lockOverlay = (ScreenLocker) findViewById(R.id.lockerOverlay);
//        lockOverlay.setActivity(this);
        
		registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_CALL_CHANGED));
		
//		proximityManager = new CallProximityManager(this, this, lockOverlay);
		attachVideoPreview();
//		onTrigger(START_VIDEO, getActiveCallInfo());
	}

	private void attachVideoPreview() {
		// Video stuff
		if (cameraPreview == null) {
			Log.d(THIS_FILE, "Create Local Renderer");
			cameraPreview = ViERenderer.CreateLocalRenderer(this);
			renderView = ViERenderer.CreateRenderer(this, true);
			mainFrame.addView(renderView,0,new RelativeLayout.LayoutParams(1, 1));
			mainFrame.addView(cameraPreview, 1, 1);
		} else {
			Log.d(THIS_FILE, "NO NEED TO Create Local Renderer");
		}
	}
	
	public SipCallSession getActiveCallInfo() {
		SipCallSession currentCallInfo = null;
		if (callsInfo == null) {
			return null;
		}
		synchronized (callMutex) {
			for (SipCallSession callInfo : callsInfo) {
				currentCallInfo = getPrioritaryCall(callInfo, currentCallInfo);
			}
		}
		return currentCallInfo;
	}
	
	private SipCallSession getPrioritaryCall(SipCallSession call1,
			SipCallSession call2) {
		// We prefer the not null
		if (call1 == null) {
			return call2;
		} else if (call2 == null) {
			return call1;
		}
		// We prefer the one not terminated
		if (call1.isAfterEnded()) {
			return call2;
		} else if (call2.isAfterEnded()) {
			return call1;
		}
		// We prefer the one not held
		if (call1.isLocalHeld()) {
			return call2;
		} else if (call2.isLocalHeld()) {
			return call1;
		}
		// We prefer the older call
		// to keep consistancy on what will be replied if new call arrives
		return (call1.getCallStart() > call2.getCallStart()) ? call2 : call1;
	}

	private class UpdateUIFromCallRunnable implements Runnable {

		@Override
		public void run() {
			// Current call is the call emphasis by the UI.
			SipCallSession mainCallInfo = getActiveCallInfo();

			if (mainCallInfo != null) {
				Log.d(THIS_FILE, "Active call is " + mainCallInfo.getCallId());
				Log.d(THIS_FILE,
						"Update ui from call "
								+ mainCallInfo.getCallId()
								+ " state "
								+ CallsUtils.getStringCallState(mainCallInfo,
										EntranceCallActivity.this));
				int state = mainCallInfo.getCallState();

				// int backgroundResId =
				// R.drawable.bg_in_call_gradient_unidentified;

				// We manage wake lock
				switch (state) {
				case SipCallSession.InvState.INCOMING:
					tvCallStatus.setText("INCOMING");
					break;
				case SipCallSession.InvState.EARLY:
					// tvCallStatus.setText("EARLY");
					delayedQuit();
					break;
				case SipCallSession.InvState.CALLING:
					pivCallStatus.setMovieResource(R.drawable.ic_entrancecallactivity_call, true);
					tvCallStatus.setText("呼叫用户"+"中...");
					break;
				case SipCallSession.InvState.CONNECTING:
					pivCallStatus.setMovieResource(	R.drawable.ic_entrancecallactivity_oncall, true);
					tvCallStatus.setText("正在通话中...");
					onTrigger(START_VIDEO, mainCallInfo);
//					onTrigger(TAKE_CALL, mainCallInfo);
					
					onDisplayVideo(true);
					quitTimer.cancel();
					quitTimer.purge();
					// onTrigger(TAKE_CALL, mainCallInfo);

					Log.d(THIS_FILE, "Acquire wake up lock");
					if (wakeLock != null && !wakeLock.isHeld()) {
						wakeLock.acquire();
					}
					break;
				case SipCallSession.InvState.CONFIRMED:
					break;
				case SipCallSession.InvState.NULL:
				case SipCallSession.InvState.DISCONNECTED:
					tvCallStatus.setText("已挂断...");
					finish();
					Log.d(THIS_FILE,"Active call session is disconnected or null wait for quit...");
					// This will release locks
					// onDisplayVideo(false);
					// delayedQuit();
					return;

				}

				Log.d(THIS_FILE, "we leave the update ui function");
			}

		}

	}


    private class UpdateVideoPreviewRunnable implements Runnable {
        private final boolean show;
        UpdateVideoPreviewRunnable(boolean show){
            this.show = show;
        }
        @Override
        public void run() {
            // Update the camera preview visibility 
            if(cameraPreview != null && renderView != null) {
                cameraPreview.setVisibility(show ? View.VISIBLE : View.GONE);
                renderView.setVisibility(show ? View.VISIBLE : View.GONE);
                if(show) {
                    if(videoWakeLock != null) {
                        videoWakeLock.acquire();
                    }
                    SipService.setVideoWindow(SipCallSession.INVALID_CALL_ID, cameraPreview, true);
                    SipService.setVideoWindow(SipCallSession.INVALID_CALL_ID, renderView, false);
                }else {
                    if(videoWakeLock != null && videoWakeLock.isHeld()) {
                        videoWakeLock.release();
                    }
                    SipService.setVideoWindow(SipCallSession.INVALID_CALL_ID, null, true);
                    SipService.setVideoWindow(SipCallSession.INVALID_CALL_ID, null, false);
                }
            }else {
                Log.w(THIS_FILE, "No camera preview available to be shown");
            }
        }
    }
    
    private synchronized void delayedQuit() {

        if (wakeLock != null && wakeLock.isHeld()) {
            Log.d(THIS_FILE, "Releasing wake up lock");
            wakeLock.release();
        }
        

        Log.d(THIS_FILE, "Start quit timer");
        if (quitTimer != null) {
            quitTimer.schedule(new QuitTimerTask(), QUIT_DELAY);
        } else {
            finish();
        }
    }

    private class QuitTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.d(THIS_FILE, "Run quit timer");
            runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					if (getActiveCallInfo().getCallState() != SipCallSession.InvState.CONNECTING) {
						tvCallStatus.setText("无人接听...");
						pivCallStatus.setMovieResource(R.drawable.ic_entrancecallactivity_noanwser, true);
						onTrigger(TERMINATE_CALL, getActiveCallInfo());						
					}
				}
			});
        }
    };
	private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action.equals(SipManager.ACTION_SIP_CALL_CHANGED)) {
				if (service != null) {
					try {
						synchronized (callMutex) {
							callsInfo = service.getCalls();
							runOnUiThread(new UpdateUIFromCallRunnable());
						}
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Not able to retrieve calls");
					}
				}
			} 
			
		}
	};

	@Override
	public void onTrigger(int whichAction, SipCallSession call) {
		// Sanity check for actions requiring valid call id
		if (whichAction == TAKE_CALL || whichAction == REJECT_CALL
				|| whichAction == DONT_TAKE_CALL
				|| whichAction == TERMINATE_CALL
				|| whichAction == DETAILED_DISPLAY
				|| whichAction == TOGGLE_HOLD || whichAction == START_RECORDING
				|| whichAction == STOP_RECORDING || whichAction == DTMF_DISPLAY
				|| whichAction == XFER_CALL || whichAction == TRANSFER_CALL
				|| whichAction == START_VIDEO || whichAction == STOP_VIDEO) {
			// We check that current call is valid for any actions
			if (call == null) {
				Log.e(THIS_FILE, "Try to do an action on a null call !!!");
				return;
			}
			if (call.getCallId() == SipCallSession.INVALID_CALL_ID) {
				Log.e(THIS_FILE, "Try to do an action on an invalid call !!!");
				return;
			}
		}

		// Reset proximity sensor timer
		// proximityManager.restartTimer();

		try {
			switch (whichAction) {
			case TAKE_CALL: {
				if (service != null) {
					Log.d(THIS_FILE, "Answer call " + call.getCallId());

					boolean shouldHoldOthers = false;
					// Well actually we should be always before confirmed
					if (call.isBeforeConfirmed()) {
						shouldHoldOthers = true;
					}
					service.answer(call.getCallId(),SipCallSession.StatusCode.OK);
					// if it's a ringing call, we assume that user wants to
					// hold other calls
					if (shouldHoldOthers && callsInfo != null) {
						for (SipCallSession callInfo : callsInfo) {
							// For each active and running call
							if (SipCallSession.InvState.CONFIRMED == callInfo
									.getCallState()
									&& !callInfo.isLocalHeld()
									&& callInfo.getCallId() != call.getCallId()) {

								Log.d(THIS_FILE,
										"Hold call " + callInfo.getCallId());
								service.hold(callInfo.getCallId());

							}
						}
					}
				}
				break;
			}
			case DONT_TAKE_CALL: {
				if (service != null) {
					service.hangup(call.getCallId(), StatusCode.BUSY_HERE);
				}
				break;
			}
			case REJECT_CALL:
			case TERMINATE_CALL: {
				if (service != null) {
					service.hangup(call.getCallId(), 0);
				}
				break;
			}
			case MUTE_ON:
			case MUTE_OFF: {
				if (service != null) {
					service.setMicrophoneMute((whichAction == MUTE_ON) ? true
							: false);
				}
				break;
			}
			case SPEAKER_ON:
			case SPEAKER_OFF: {
				if (service != null) {
					Log.d(THIS_FILE, "Manually switch to speaker");
					// useAutoDetectSpeaker = false;
					service.setSpeakerphoneOn((whichAction == SPEAKER_ON) ? true
							: false);
				}
				break;
			}
			case DTMF_DISPLAY: {
				// showDialpad(call.getCallId());
				break;
			}
			case TOGGLE_HOLD: {
				if (service != null) {
					// Log.d(THIS_FILE,
					// "Current state is : "+callInfo.getCallState().name()+" / "+callInfo.getMediaStatus().name());
					if (call.getMediaStatus() == SipCallSession.MediaState.LOCAL_HOLD
							|| call.getMediaStatus() == SipCallSession.MediaState.NONE) {
						service.reinvite(call.getCallId(), true);
					} else {
						service.hold(call.getCallId());
					}
				}
				break;
			}
			case MEDIA_SETTINGS: {
				startActivity(new Intent(this, InCallMediaControl.class));
				break;
			}
			case START_VIDEO:
			case STOP_VIDEO: {
				if (service != null) {
					Bundle opts = new Bundle();
					opts.putBoolean(SipCallSession.OPT_CALL_VIDEO,
							whichAction == START_VIDEO);
					service.updateCallOptions(call.getCallId(), opts);
				}
				break;
			}
			case ZRTP_TRUST: {
				if (service != null) {
					service.zrtpSASVerified(call.getCallId());
				}
				break;
			}
			case ZRTP_REVOKE: {
				if (service != null) {
					service.zrtpSASRevoke(call.getCallId());
				}
				break;
			}
			}
		} catch (RemoteException e) {
			Log.e(THIS_FILE, "Was not able to call service method", e);
		}

	}

	@Override
	public void onDisplayVideo(boolean show) {
		runOnUiThread(new UpdateVideoPreviewRunnable(show));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 18) {
			onTrigger(TERMINATE_CALL, getActiveCallInfo());
			if (service != null) {
				try {
					service.hangup(getActiveCallInfo().getCallId(), 0);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return true;
	}


	@Override
	protected void onDestroy() {
		
		if (quitTimer != null) {
			quitTimer.cancel();
			quitTimer.purge();
			quitTimer = null;
		}
		try {
			unbindService(connection);
		} catch (Exception e) {
			// Just ignore that
		}
		service = null;
		
		try {
			unregisterReceiver(callStateReceiver);
		} catch (IllegalArgumentException e) {
			// That's the case if not registered (early quit)
		}
		super.onDestroy();
	}

	@Override
	public boolean shouldActivateProximity() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onProximityTrackingChanged(boolean acquired) {
		
		
	}
}
