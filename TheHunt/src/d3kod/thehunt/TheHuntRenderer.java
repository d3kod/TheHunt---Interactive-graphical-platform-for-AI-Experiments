package d3kod.thehunt;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.SensorEvent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import d3kod.d3gles20.D3GLES20;
import d3kod.d3gles20.TempCircle;
import d3kod.thehunt.environment.Environment;
import d3kod.thehunt.environment.EnvironmentData;
import d3kod.thehunt.prey.Prey;

public class TheHuntRenderer implements GLSurfaceView.Renderer {
	
	private static final String TAG = "TheHuntRenderer";
	private static final int SMOOTHING_SIZE = 30;
	public static boolean LOGGING_TIME = false;
	public static boolean SHOW_TILES = false;
	public static boolean SHOW_CURRENTS = false;
	public static boolean FEED_WITH_TOUCH = false;
	public static boolean NET_WITH_TOUCH = true;

	private static final boolean SHOW_CIRCLE_CONTAINS_CHECKS = false;
	
	
	private float[] mProjMatrix = new float[16]; 
	private float[] mVMatrix = new float[16];
	private long timePreviousNS;
	private Environment mEnv;
	public Prey mPrey;
	private CatchNet mNet;
	
    public static final int TICKS_PER_SECOND = 30;
    private final int MILLISEC_PER_TICK = 1000 / TICKS_PER_SECOND;
    private final int MAX_FRAMESKIP = 5;
	private long next_game_tick;
	private Context mContext;
	private long mslf;
	private long smoothMspf;
	private int smoothingCount;
	private int mCaughtCounter;
	private TempCircle tempCircle;
	public static float[] bgColor = {
			0.8f, 0.8f, 0.8f, 1.0f};

	public void onSensorChanged(SensorEvent event) {
		
	}
	
	public TheHuntRenderer(Context context) {
		mContext = context;
//		next_game_tick = System.currentTimeMillis();
//		mslf = 0;
	    mEnv = null;
	    mPrey = null;
	    mNet = null;
	    mCaughtCounter = 0;
//	    mManuControl.setSize();
		
	}
	
	public void resume() {
		next_game_tick = System.currentTimeMillis();
		mslf = next_game_tick;
		smoothMspf = 0;
		smoothingCount = 0;
		((TheHunt) mContext).runOnUiThread(new Runnable() {
			public void run() {
				((TheHunt) mContext).mCaughtCounter.setText(mCaughtCounter + "");
			}
		});
	}
	
	public void release() {
		// TODO stuff to release	
	}

	/**
	 * The Surface is created/init()
	 */
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glClearColor(bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
		D3GLES20.clean();
	    Matrix.orthoM(mVMatrix, 0, -1, 1, -1, 1, 0.1f, 100f);
	}
	/**
	 * If the surface changes, reset the view
	 */
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		
		if (mEnv == null) mEnv = new Environment(width, height);
	    if (mPrey == null) mPrey = new Prey(EnvironmentData.mScreenWidth, EnvironmentData.mScreenHeight, mEnv);
	    if (mNet == null) mNet = new CatchNet(mEnv);
	    mEnv.initGraphics();
		mPrey.initGraphics();
//		mNet.initGraphics();
		
	    float ratio = (float) width / height;
	    
	    if (width > height) Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
	    else Matrix.frustumM(mProjMatrix, 0, -1, 1, -1/ratio, 1/ratio, 1, 10);
	}
	/**
	 * Here we do our drawing
	 */
	public void onDrawFrame(GL10 unused) {

		int loops = 0;
		while (next_game_tick < System.currentTimeMillis() && loops < MAX_FRAMESKIP) {
			updateWorld();
			next_game_tick += MILLISEC_PER_TICK;
			loops++;
		}
		
		mslf = System.currentTimeMillis();
		float interpolation = (System.currentTimeMillis() + MILLISEC_PER_TICK - next_game_tick) / (float) MILLISEC_PER_TICK;
		drawWorld(interpolation);
		long mspf = System.currentTimeMillis() - mslf;
		smoothMspf = (smoothMspf*smoothingCount + mspf)/(smoothingCount+1);
		smoothingCount++;
		if (smoothingCount >= SMOOTHING_SIZE) {
			smoothingCount = 0;
			((TheHunt) mContext).runOnUiThread(new Runnable() {
				public void run() {
//					((TheHunt) mContext).mPreyState.setText(Planner.mState.toString());
					((TheHunt) mContext).mMSperFrame.setText(smoothMspf + "");
				}
			});
		}
	}		

	private void updateWorld() {
		
		//TODO: fix all these static shits
//		((TheHunt) mContext).runOnUiThread(new Runnable() {
//			public void run() {
//				((TheHunt) mContext).mPreyState.setText(Planner.mState.toString());
//			}
//		});
//		
		PointF curDirDelta = mEnv.data.getTileFromPos(mPrey.getPosition()).getDir().getDelta();
		mPrey.update(curDirDelta.x * EnvironmentData.currentStep, 
				curDirDelta.y * EnvironmentData.currentStep);
		
		PointF preyPos = mPrey.getPosition();
//		if (SHOW_CIRCLE_CONTAINS_CHECKS && !mPrey.getCaught() && mNet.isBuilt()) {
//			tempCircle = D3GLES20.newContainsCheckCircle(mNet.getGraphicIndex(), preyPos.x, preyPos.y);
//		}
//		if (!mPrey.getCaught() && mNet.isBuilt() && D3GLES20.contains(mNet.getGraphicIndex(), preyPos.x, preyPos.y)) {
		if (!mPrey.getCaught() && mNet.tryToCatch() && D3GLES20.contains(mNet.getGraphicIndex(), preyPos.x, preyPos.y)) {
			Log.v(TAG, "Prey is caught!");
			mPrey.setCaught(true);
			mNet.caughtPrey(mPrey);
			mCaughtCounter += 1;
			((TheHunt) mContext).runOnUiThread(new Runnable() {
				public void run() {
					((TheHunt) mContext).mCaughtCounter.setText(mCaughtCounter + "");
				}
			});
		}
		
		mNet.update();
	}

	private void drawWorld(float interpolation) {
		int clearMask = GLES20.GL_COLOR_BUFFER_BIT;
		
		if (MultisampleConfigChooser.usesCoverageAa()) {
			final int GL_COVERAGE_BUFFER_BIT_NV = 0x8000;
            clearMask |= GL_COVERAGE_BUFFER_BIT_NV;
		}
		
		GLES20.glClear(clearMask);
		
		D3GLES20.drawAll(mVMatrix, mProjMatrix, interpolation);
		
		mEnv.draw(mVMatrix, mProjMatrix, interpolation);
		mPrey.draw(mVMatrix, mProjMatrix, interpolation);
//		if (mNet.show()) mNet.draw(mVMatrix, mProjMatrix);
		if (SHOW_CIRCLE_CONTAINS_CHECKS) {
			if (tempCircle != null) {
				if (tempCircle.isExpired()) tempCircle = null;
				else tempCircle.draw(mVMatrix, mProjMatrix);
			}
		}
	}

	public void handleTouchDown(float x, float y) {
		x = D3GLES20.fromWorldWidth(x);
		y = D3GLES20.fromWorldHeight(y);
		if (FEED_WITH_TOUCH) {
			mEnv.putFood(x, y);
		}
		else if (NET_WITH_TOUCH) {
			mNet.start(x, y);
		}
	}

	public void handleTouchMove(float x, float y) {
		x = D3GLES20.fromWorldWidth(x);
		y = D3GLES20.fromWorldHeight(y);
		if (NET_WITH_TOUCH) {
			mNet.next(x, y);
		}
	}

	public void handleTouchUp(float x, float y) {
		x = D3GLES20.fromWorldWidth(x);
		y = D3GLES20.fromWorldHeight(y);
		if (NET_WITH_TOUCH) {
			mNet.finish(x, y);
		}
	}
}