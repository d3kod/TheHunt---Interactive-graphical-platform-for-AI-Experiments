package d3kod.thehunt.prey;

import java.text.Bidi;

import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.FloatMath;
import android.util.Log;
import d3kod.d3gles20.D3GLES20;
import d3kod.thehunt.TheHuntRenderer;
import d3kod.thehunt.environment.Environment;
import d3kod.thehunt.environment.EnvironmentData;
import d3kod.thehunt.prey.memory.WorldModel;
import d3kod.thehunt.prey.planner.Planner;
import d3kod.thehunt.prey.sensor.Sensor;

public class Prey {
	
	private static final String TAG = "Prey";
	public static boolean AI = false;
//	public static int BODY_BEND_DELAY = 1;
	public static final int BODY_BENDS_PER_SECOND_MAX = 30;
	public static int BODY_BENDS_PER_SECOND = 12;
	public static int BODY_BEND_DELAY = TheHuntRenderer.TICKS_PER_SECOND/BODY_BENDS_PER_SECOND;
	public static int ACTION_DELAY = 5;
	public static int ACTION_DELAY_MAX = 20;
	
//	public static int SECONDS_PER_REV = 3;
	public static int SMALL_TURNS_PER_SECOND = 10;
//	public static int BODY_ANGLE_COUNTER = PreyData.angleFlop/PreyData.rotateSpeed;
//	public static int rotateSpeed = 360/(TheHuntRenderer.TICKS_PER_SECOND*SECONDS_PER_FULL_ROT); 
	
	private Planner mPlanner;
	private WorldModel mWorldModel;
	private Sensor mSensor;
	private Environment mEnv;
	private PreyData mD;
	private int bodyBendCounter;
	private int actionDelayCounter;
	private float bodyStartAnglePredicted;
	private float bodyBAnglePredicted;
	private float bodyCAnglePredicted;
	private float bodyEndAnglePredicted;
	private int bodyStartAngleRot;
	private int bodyBAngleRot;
	private int bodyCAngleRot;
	private int bodyEndAngleRot;
	private int nextBodyEndAngleTarget;
	private int newBodyStartAngleTarget;
	private int bodyAngleCounter;
	private int backFinTarget;
	private int backFinNextTarget;
	private int backFinAngle;
	private boolean flopBack;
	private int flopBackTargetFirst;
	private int flopBackTargetSecond;
	private boolean floppedFirst;
	private boolean floppedSecond;
	private boolean turningBackFinMotion;
	private int flopBackAngle;
	private TurnAngle turningBackFinAngle;
	private int backAngleToggle;
	public static boolean angleInterpolation = true;
	public static boolean posInterpolation = true;

	public static final float bodyToHeadLength = 0.07f;
	
	int flopBacksPerSecond = 10;
	int flopBackTicks = TheHuntRenderer.TICKS_PER_SECOND/flopBacksPerSecond;
	private int flopBackSpeed;
	
	public void update(float dx, float dy) {
		float[] posTemp = { 0.0f, bodyToHeadLength, 0.0f, 1.0f };
		
		Matrix.setIdentityM(mD.mHeadModelMatrix, 0);
		Matrix.translateM(mD.mHeadModelMatrix, 0, mD.mPosX, mD.mPosY, 0);
		Matrix.rotateM(mD.mHeadModelMatrix, 0, mD.bodyStartAngle, 0, 0, 1);
		Matrix.multiplyMV(posTemp, 0, mD.mHeadModelMatrix, 0, posTemp, 0);
		
		mD.mPosHeadX = posTemp[0]; 
		mD.mPosHeadY = posTemp[1];
		
		mWorldModel.update(mSensor.sense(mD.mPosHeadX, mD.mPosHeadY, mD.mPosX, mD.mPosY));

		if (AI) {
			if (actionDelayCounter == 0) {
				doAction(mPlanner.nextAction(mWorldModel));
				actionDelayCounter = ACTION_DELAY;
			}
			else actionDelayCounter--;
		}
		move(dx, dy);
	}
	
	public void move(float x, float y) {

//		Log.v(TAG, "Debug " + mD.bodyStartAngleTarget + " " + mD.bodyStartAngle + " " + mD.bodyBAngleTarget + " " + mD.bodyBAngle + " " + mD.bodyCAngleTarget + " " + mD.bodyCAngle + " " + mD.bodyEndAngleTarget + mD.bodyEndAngle);
		if (bodyBendCounter == 0) {
//			mD.thrust = Math.abs(mD.bodyCAngleTarget - mD.bodyEndAngleTarget);
			mD.bodyEndAngleTarget = mD.bodyCAngleTarget;
			mD.bodyCAngleTarget = mD.bodyBAngleTarget;
			mD.bodyBAngleTarget = mD.bodyStartAngleTarget;
//			mD.bodyStartAngleTarget = newBodyStartAngleTarget;
//			if (mD.bodyEndAngleTarget == nextBodyEndAngleTarget) nextBodyEndAngleTarget = 0;
//			if (nextBodyEndAngleTarget != 0) mD.bodyEndAngleTarget = nextBodyEndAngleTarget;
			
			bodyBendCounter = BODY_BEND_DELAY-1;
			
//			Log.v(TAG, "Passing spin " + mD.bodyStartAngleTarget + " " + mD.bodyStartAngle + " " + mD.bodyBAngleTarget + " " + mD.bodyBAngle + " " + mD.bodyCAngleTarget + " " + mD.bodyCAngle + " " + mD.bodyEndAngleTarget + mD.bodyEndAngle);
		}
		else {
//			mD.thrust = 0;
			--bodyBendCounter;
		}
		
		if (turningBackFinMotion && !flopBack) {
			if (stoppedTurning()) {
				turningBackFinMotion = false;
			}
			backFinMotion(turningBackFinAngle);
		}
		
		//TODO: 
//		if (bodyAngleCounter == 0) {
//			Log.v(TAG, "DEBUGGING " + mD.bodyStartAngleTarget + " " + mD.bodyStartAngle + " " + mD.rotateSpeed);
			if (mD.bodyStartAngleTarget > mD.bodyStartAngle + mD.rotateSpeedHead) bodyStartAngleRot = mD.rotateSpeedHead;
			else if (mD.bodyStartAngleTarget < mD.bodyStartAngle - mD.rotateSpeedHead) bodyStartAngleRot = -mD.rotateSpeedHead;
			else {
				bodyStartAngleRot = 0;
				mD.bodyStartAngle = mD.bodyStartAngleTarget;
			}
			
			if (mD.bodyBAngleTarget > mD.bodyBAngle + mD.rotateSpeed) bodyBAngleRot = +mD.rotateSpeed;
			else if (mD.bodyBAngleTarget < mD.bodyBAngle - mD.rotateSpeed) bodyBAngleRot = -mD.rotateSpeed;
			else {
				bodyBAngleRot = 0;
				mD.bodyBAngle = mD.bodyBAngleTarget;
			}
			
			if (mD.bodyCAngleTarget > mD.bodyCAngle + mD.rotateSpeed) bodyCAngleRot = +mD.rotateSpeed;
			else if (mD.bodyCAngleTarget < mD.bodyCAngle - mD.rotateSpeed) bodyCAngleRot = -mD.rotateSpeed;
			else {
				bodyCAngleRot = 0;
				mD.bodyCAngle = mD.bodyCAngleTarget;
			}
			
			if (!flopBack) {
				if (mD.bodyEndAngleTarget > mD.bodyEndAngle + mD.rotateSpeed) bodyEndAngleRot = +mD.rotateSpeed;
				else if (mD.bodyEndAngleTarget < mD.bodyEndAngle - mD.rotateSpeed) bodyEndAngleRot = -mD.rotateSpeed;
				else {
					bodyEndAngleRot = 0;
					mD.bodyEndAngle = mD.bodyEndAngleTarget;
				}
			}
			
//			bodyAngleCounter = BODY_ANGLE_COUNTER-1;
//		}
//		else {
//			bodyAngleCounter--;
//		}
		
//			if (backFinTarget != 0) {
//				if (backFinTarget != mD.bodyEndAngleTarget) mD.bodyEndAngleTarget = backFinTarget;
//				else if (mD.bodyEndAngleTarget == mD.bodyEndAngle) backFinTarget = 0;
//			}
//			if (backFinTarget == 0 && backFinNextTarget != 0) {
//				if (backFinNextTarget != mD.bodyEndAngleTarget) mD.bodyEndAngleTarget = backFinNextTarget;
//				else if (mD.bodyEndAngleTarget == mD.bodyEndAngle) backFinNextTarget = 0;
//			}
//			if (backFinTarget == 0 && backFinNextTarget == 0) {
//				mD.bodyEndAngleTarget = mD.bodyCAngleTarget;
//			}
		
//		if (backFinAngle != 0) {
//			if (backFinTarget != -1) {
//				if (backFinTarget == 0) {
//					backFinTarget = mD.bodyEndAngleTarget + backFinAngle;
//				}
//				if (backFinTarget != mD.bodyEndAngleTarget) mD.bodyEndAngleTarget = backFinTarget;
//				else if (backFinTarget == mD.bodyEndAngle) backFinTarget = -1;
//			}
//			if (backFinTarget == -1) {
//				if (backFinNextTarget == 0) {
//					backFinNextTarget = mD.bodyEndAngleTarget - 2*backFinAngle;
//				}
//				if (backFinNextTarget != mD.bodyEndAngleTarget) mD.bodyEndAngleTarget = backFinNextTarget;
//				else if (backFinNextTarget == mD.bodyEndAngle) backFinNextTarget = -1;
//			}
//			if (backFinTarget == -1 && backFinNextTarget == -1) {
//				backFinAngle = 0;
//				backFinTarget = 0;
//				backFinNextTarget = 0;
//				mD.bodyEndAngleTarget = mD.bodyCAngleTarget;
//			}
//			backFinFirstFlop = true;
//		}
//		else {
//			mD.bodyEndAngleTarget = mD.bodyCAngleTarget;
//		}
			
		if (flopBack) {
//			Log.v(TAG, "FlopBack is true");
			doFlopBack();
		}
		
		mD.bodyStartAngle += bodyStartAngleRot;
		mD.bodyBAngle += bodyBAngleRot;
		mD.bodyCAngle += bodyCAngleRot;
		if (!flopBack) mD.bodyEndAngle += bodyEndAngleRot;
		
		//TODO: fix thurst, maybe do it in this method instead of updateSpeed
		updateSpeed(x, y);
		applyFriction();
		mD.mPosX += mD.vx ; mD.mPosY += mD.vy;
	}
	private boolean stoppedTurning() {
		return (mD.bodyStartAngleTarget == mD.bodyBAngleTarget && mD.bodyBAngleTarget == mD.bodyCAngleTarget);
	}

	private void doFlopBack() {
		if (!floppedFirst) {
//			Log.v(TAG, "Flopping first " + flopBackTargetFirst + " " + mD.bodyEndAngle);
//			if (flopBackTargetFirst > mD.bodyEndAngle + mD.rotateSpeed) bodyEndAngleRot = +mD.rotateSpeed;
//			else if (flopBackTargetFirst < mD.bodyEndAngle - mD.rotateSpeed) bodyEndAngleRot = -mD.rotateSpeed;
//			else {
//				bodyEndAngleRot = 0;
//				mD.bodyEndAngle = flopBackTargetFirst;
//				floppedFirst = true;
//				Log.v(TAG, "Flopped first");
//			}
			if (flopBackTargetFirst > flopBackAngle + flopBackSpeed) flopBackAngle += flopBackSpeed;
			else if (flopBackTargetFirst < flopBackAngle - flopBackSpeed) flopBackAngle -= flopBackSpeed;
			else {
//				flopBackFirst = flopBackTargetFirst;
				flopBackAngle = flopBackTargetFirst;
				floppedFirst = true;
			}
			//bodyEndAngleRot = bodyCAngleRot + flopBackAngle;
			mD.bodyEndAngle = mD.bodyCAngle + flopBackAngle;
		}
		else {
//			Log.v(TAG, "Flopping second " + flopBackTargetSecond + " " + mD.bodyEndAngle);
//			if (flopBackTargetSecond > mD.bodyEndAngle + mD.rotateSpeed) bodyEndAngleRot = +mD.rotateSpeed;
//			else if (flopBackTargetSecond < mD.bodyEndAngle - mD.rotateSpeed) bodyEndAngleRot = -mD.rotateSpeed;
//			else {
//				bodyEndAngleRot = 0;
//				mD.bodyEndAngle = flopBackTargetSecond;
//				floppedSecond = true;
//				Log.v(TAG, "Flopped second");
//			}
			
			if (flopBackTargetSecond > flopBackAngle + flopBackSpeed) flopBackAngle += flopBackSpeed;
			else if (flopBackTargetSecond < flopBackAngle - flopBackSpeed) flopBackAngle -= flopBackSpeed;
			else {
				flopBackAngle = flopBackTargetSecond;
				floppedSecond = true;
			}
//			bodyEndAngleRot = bodyCAngleRot + flopBackAngle;
			mD.bodyEndAngle = mD.bodyCAngle + flopBackAngle;
		}
		if (floppedFirst && floppedSecond) {
			flopBack = false;
//			Log.v(TAG, "Flopped both");
		}
	}
	public void turn(TurnAngle angle) {
		int value = angle.getValue();
		mD.bodyStartAngleTarget += value;
		if (value > 0) {
			turningBackFinAngle = TurnAngle.BACK_RIGHT_SMALL;
		}
		else turningBackFinAngle = TurnAngle.BACK_LEFT_SMALL;
		
//		switch (angle) {
//		case LEFT_SMALL: mD.bodyStartAngleTarget += mD.angleFlop; turningBackFinAngle = TurnAngle.BACK_RIGHT_SMALL; break;
//		case LEFT_MEDIUM: mD.bodyStartAngleTarget += 2*mD.angleFlop; turningBackFinAngle = TurnAngle.BACK_RIGHT_SMALL; break;
//		case LEFT_LARGE: mD.bodyStartAngleTarget += 3*mD.angleFlop; turningBackFinAngle = TurnAngle.BACK_RIGHT_SMALL; break;
//		case RIGHT_SMALL: mD.bodyStartAngleTarget -= mD.angleFlop; turningBackFinAngle = TurnAngle.BACK_LEFT_SMALL; break;
//		case RIGHT_MEDIUM: mD.bodyStartAngleTarget -= 2*mD.angleFlop; turningBackFinAngle = TurnAngle.BACK_LEFT_SMALL; break;
//		case RIGHT_LARGE: mD.bodyStartAngleTarget -= 3*mD.angleFlop; turningBackFinAngle = TurnAngle.BACK_LEFT_SMALL; break;
//		}
//		
		turningBackFinMotion = true;
		
	}
	
	public void backFinMotion(TurnAngle angle) {
		flopBack = true;
		switch(angle) {
		case BACK_SMALL: backFinAngle = 10 * backAngleToggle; backAngleToggle = -backAngleToggle; break;
		case BACK_MEDIUM: backFinAngle = 20 * backAngleToggle; backAngleToggle = -backAngleToggle; break;
		case BACK_LARGE: backFinAngle = 30 * backAngleToggle; backAngleToggle = -backAngleToggle; break;
		case BACK_LEFT_SMALL: backFinAngle = 10; break;
		case BACK_LEFT_MEDIUM: backFinAngle = 20; break;
		case BACK_LEFT_LARGE: backFinAngle = 30; break;
		case BACK_RIGHT_SMALL: backFinAngle = -10; break;
		case BACK_RIGHT_MEDIUM: backFinAngle = -20; break;
		case BACK_RIGHT_LARGE: backFinAngle = -30; break;
		}
		mD.bodyEndAngle = mD.bodyCAngle;
//		flopBackTargetFirst = mD.bodyCAngle + backFinAngle;
//		flopBackTargetSecond = mD.bodyCAngle - backFinAngle;
		flopBackTargetFirst = +backFinAngle;
		flopBackAngle = 0;
		flopBackTargetSecond = -backFinAngle;
		floppedFirst = false;
		floppedSecond = false;
		//mD.thrust = Math.abs(backFinAngle);
		flopBackSpeed = Math.abs(backFinAngle)/flopBackTicks;
		moveForward(Math.abs(backFinAngle));
	}
	
	public void updateSpeed(float dx, float dy) {
		mD.vx += dx; mD.vy += dy;
		//moveForward(mD.thrust);
	}
	
	public void moveForward(float distance) {
		distance *= mD.DISTANCE_TO_ANGLE_RATIO;
//		Log.v(TAG, "Moving the prey forward to a distance of " + distance + " thrust is " + mD.thrust);
		float radAngle = (float)Math.toRadians(mD.bodyStartAngle);
		mD.vx += -FloatMath.sin(radAngle)*distance;
		mD.vy += FloatMath.cos(radAngle)*distance;  
	}
	
	public void applyFriction() {
		mD.vx -= EnvironmentData.frictionCoeff*mD.vx;
		mD.vy -= EnvironmentData.frictionCoeff*mD.vy;
	}
	
	public Prey(float screenWidth, float screenHeight, Environment env) {
		mD = new PreyData();
		
		mD.delayV = new float[mD.delayVLength][2];
		
		mWorldModel = new WorldModel(screenWidth, screenHeight);
		mPlanner = new Planner();
		mEnv = env;
		mSensor = new Sensor(mEnv);
		nextBodyEndAngleTarget = 0;
		newBodyStartAngleTarget = 0;
		
		backAngleToggle = 1;
		
		Matrix.setIdentityM(mD.mModelMatrix, 0);
		mD.mPosX = mD.mPosY = 0;

		for (int i = 0; i < 3; ++i) {
			mD.bodyStart4[i] = mD.bodyStart[i];
		}
		for (int i = 0; i < 3; ++i) {
			mD.bodyB4[i] = mD.bodyB[i];
		}
		for (int i = 0; i < 3; ++i) {
			mD.bodyC4[i] = mD.bodyC[i];
		}
		for (int i = 0; i < 3; ++i) {
			mD.bodyEnd4[i] = mD.bodyEnd[i];
		}
		
		mD.headVerticesData = calcHeadVerticesData();
		mD.leftFinVerticesData = calcLeftFinVerticesData();
		mD.rightFinVerticesData = calcRightFinVerticesData();
		mD.eyeVertexData = D3GLES20.circleVerticesData(mD.eyePosition, mD.eyeSize, mD.eyeDetailsLevel);
		
		mD.finVerticesNum = mD.rightFinVerticesData.length / D3GLES20.COORDS_PER_VERTEX;
		
		
//		AI = false;
	}
	
	private float[] calcRightFinVerticesData() {
		return D3GLES20.quadBezierCurveVertices(
				mD.rightFinStart, mD.rightFinB, mD.rightFinC, mD.rightFinEnd, mD.detailsStep, mD.finSize);
	}

	private float[] calcLeftFinVerticesData() {
		return D3GLES20.quadBezierCurveVertices(
				mD.leftFinStart, mD.leftFinB, mD.leftFinC, mD.leftFinEnd, mD.detailsStep, mD.finSize);
	}

	private float[] calcHeadVerticesData() {
		float[] part1 = D3GLES20.quadBezierCurveVertices(
				mD.headPart1Start, mD.headPart1B, mD.headPart1C, mD.headPart2Start, mD.detailsStep, mD.headSize);
		float[] part2 = D3GLES20.quadBezierCurveVertices(
				mD.headPart2Start, mD.headPart2B, mD.headPart2C, mD.headPart3Start, mD.detailsStep, mD.headSize);
		float[] part3 = D3GLES20.quadBezierCurveVertices(
				mD.headPart3Start, mD.headPart3B, mD.headPart3C, mD.headPart1Start, mD.detailsStep, mD.headSize);
		float[] headVerticesData = new float[part1.length + part2.length + part3.length];
		for (int i = 0; i < part1.length; ++i) {
			headVerticesData[i] = part1[i];
		}
		for (int i = 0; i < part2.length; ++i) {
			headVerticesData[part1.length + i] = part2[i];
		}
		for (int i = 0; i < part3.length; ++i) {
			headVerticesData[part1.length + part2.length + i] = part3[i];
		}
		mD.headVerticesNum = headVerticesData.length / D3GLES20.COORDS_PER_VERTEX;
		return headVerticesData;
	}

	public void initGraphics() {
		mD.leftFinVertexBuffer = D3GLES20.newFloatBuffer(mD.leftFinVerticesData);
		mD.rightFinVertexBuffer = D3GLES20.newFloatBuffer(mD.rightFinVerticesData);
		mD.headVertexBuffer = D3GLES20.newFloatBuffer(mD.headVerticesData);
		mD.eyeVertexBuffer = D3GLES20.newFloatBuffer(mD.eyeVertexData);
		
		int vertexShaderHandle = D3GLES20.loadShader(GLES20.GL_VERTEX_SHADER, mD.vertexShaderCode);
        int fragmentShaderHandle = D3GLES20.loadShader(GLES20.GL_FRAGMENT_SHADER, mD.fragmentShaderCode);
        
        mD.mProgram = D3GLES20.createProgram(vertexShaderHandle, fragmentShaderHandle);
        
        mD.mMVPMatrixHandle = GLES20.glGetUniformLocation(mD.mProgram, "u_MVPMatrix"); 
        mD.mPositionHandle = GLES20.glGetAttribLocation(mD.mProgram, "a_Position");
        mD.mColorHandle = GLES20.glGetUniformLocation(mD.mProgram, "u_Color");
        
		if (Planner.SHOW_TARGET) {
			mPlanner.makeTarget();
		}
	}
	
	public void draw(float[] mVMatrix, float[] mProjMatrix, float interpolation) {
//		GLES20.glLineWidth(2f);
		
        // Interpolate
		if (angleInterpolation) {
			bodyStartAnglePredicted = mD.bodyStartAngle + bodyStartAngleRot * interpolation;
			bodyBAnglePredicted = mD.bodyBAngle + bodyBAngleRot * interpolation;
			bodyCAnglePredicted = mD.bodyCAngle + bodyCAngleRot * interpolation;
			bodyEndAnglePredicted = mD.bodyEndAngle + bodyEndAngleRot * interpolation;
		}
		else {
			bodyStartAnglePredicted = mD.bodyStartAngle;
			bodyBAnglePredicted = mD.bodyBAngle;
			bodyCAnglePredicted = mD.bodyCAngle;
			bodyEndAnglePredicted = mD.bodyEndAngle;
		}
        Matrix.setIdentityM(mD.mBodyStartRMatrix, 0);
        Matrix.setIdentityM(mD.mBodyBRMatrix, 0);
        Matrix.setIdentityM(mD.mBodyCRMatrix, 0);
        Matrix.setIdentityM(mD.mBodyEndRMatrix, 0);
        Matrix.rotateM(mD.mBodyStartRMatrix, 0, bodyStartAnglePredicted, 0, 0, 1);
        Matrix.rotateM(mD.mBodyBRMatrix, 0, bodyBAnglePredicted, 0, 0, 1);
        Matrix.rotateM(mD.mBodyCRMatrix, 0, bodyCAnglePredicted, 0, 0, 1);
        Matrix.rotateM(mD.mBodyEndRMatrix, 0, bodyEndAnglePredicted, 0, 0, 1);
		
        if (posInterpolation) {
        	mD.mPredictedPosX = mD.mPosX + mD.vx*interpolation; 
        	mD.mPredictedPosY = mD.mPosY + mD.vy*interpolation;
        }
        else {
        	mD.mPredictedPosX = mD.mPosX; mD.mPredictedPosY = mD.mPosY;
        }
        
        // Rotate the body vertices
        
        updateBodyVertexBuffer();
        
        // Start Drawing
        
		GLES20.glUseProgram(mD.mProgram);
		
        GLES20.glUniform4fv(mD.mColorHandle, 1, mD.preyColor , 0);
        GLES20.glEnableVertexAttribArray(mD.mColorHandle);
        
		// Body
        
        Matrix.setIdentityM(mD.mModelMatrix, 0);
        Matrix.translateM(mD.mModelMatrix , 0, mD.mPredictedPosX, mD.mPredictedPosY, 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mVMatrix, 0, mD.mModelMatrix, 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mProjMatrix, 0, mD.mMVPMatrix, 0);
        
        GLES20.glVertexAttribPointer(mD.mPositionHandle, D3GLES20.COORDS_PER_VERTEX, 
        		GLES20.GL_FLOAT, false, mD.STRIDE_BYTES, mD.bodyVertexBuffer);
        GLES20.glEnableVertexAttribArray(mD.mPositionHandle);
        
        GLES20.glUniformMatrix4fv(mD.mMVPMatrixHandle, 1, false, mD.mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mD.bodyVerticesNum);
        
        // Feet
        
        Matrix.rotateM(mD.mFeetModelMatrix, 0, mD.mModelMatrix, 0, bodyEndAnglePredicted, 0, 0, 1);
        Matrix.translateM(mD.mFeetModelMatrix, 0, 
        		mD.leftFootPosition[0], mD.leftFootPosition[1], 0);
        Matrix.rotateM(mD.mFeetModelMatrix, 0, mD.mLeftFootAngle, 0, 0, 1);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mVMatrix, 0, mD.mFeetModelMatrix, 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mProjMatrix, 0, mD.mMVPMatrix, 0);
        
        GLES20.glUniformMatrix4fv(mD.mMVPMatrixHandle, 1, false, mD.mMVPMatrix, 0);
        
        GLES20.glVertexAttribPointer(mD.mPositionHandle, D3GLES20.COORDS_PER_VERTEX, 
        		GLES20.GL_FLOAT, false, mD.STRIDE_BYTES, mD.leftFinVertexBuffer);
        GLES20.glEnableVertexAttribArray(mD.mPositionHandle);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mD.finVerticesNum);
        
        Matrix.rotateM(mD.mFeetModelMatrix, 0, mD.mModelMatrix, 0, bodyEndAnglePredicted, 0, 0, 1);
        Matrix.translateM(mD.mFeetModelMatrix, 0, 
        		-mD.leftFootPosition[0], mD.leftFootPosition[1], 0);
        Matrix.rotateM(mD.mFeetModelMatrix, 0, -mD.mRightFootAngle, 0, 0, 1);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mVMatrix, 0, mD.mFeetModelMatrix, 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mProjMatrix, 0, mD.mMVPMatrix, 0);
        
        GLES20.glUniformMatrix4fv(mD.mMVPMatrixHandle, 1, false, mD.mMVPMatrix, 0);
        
        GLES20.glVertexAttribPointer(mD.mPositionHandle, D3GLES20.COORDS_PER_VERTEX, 
        		GLES20.GL_FLOAT, false, mD.STRIDE_BYTES, mD.rightFinVertexBuffer);
        GLES20.glEnableVertexAttribArray(mD.mPositionHandle);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mD.finVerticesNum);
        
        // Head
        Matrix.rotateM(mD.mHeadModelMatrix, 0, mD.mModelMatrix, 0, bodyStartAnglePredicted, 0, 0, 1);
        Matrix.translateM(mD.mHeadModelMatrix , 0, 
        		mD.headPosition[0], mD.headPosition[1], 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mVMatrix, 0, mD.mHeadModelMatrix, 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mProjMatrix, 0, mD.mMVPMatrix, 0);
        
        GLES20.glUniformMatrix4fv(mD.mMVPMatrixHandle, 1, false, mD.mMVPMatrix, 0);
        
        GLES20.glVertexAttribPointer(mD.mPositionHandle, D3GLES20.COORDS_PER_VERTEX, 
        		GLES20.GL_FLOAT, false, mD.STRIDE_BYTES, mD.headVertexBuffer);
        GLES20.glEnableVertexAttribArray(mD.mPositionHandle);
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mD.headVerticesNum );
		
		//EYE
//        mHeadModelMatrix = mModelMatrix.clone();
       
        Matrix.translateM(mD.mHeadModelMatrix , 0, 
        		mD.eyePosition[0], mD.eyePosition[1], 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mVMatrix, 0, mD.mHeadModelMatrix, 0);
        Matrix.multiplyMM(mD.mMVPMatrix, 0, mProjMatrix, 0, mD.mMVPMatrix, 0);
        
        GLES20.glUniformMatrix4fv(mD.mMVPMatrixHandle, 1, false, mD.mMVPMatrix, 0);
        
		GLES20.glVertexAttribPointer(mD.mPositionHandle, D3GLES20.COORDS_PER_VERTEX, 
        		GLES20.GL_FLOAT, false, mD.STRIDE_BYTES, mD.eyeVertexBuffer);
		GLES20.glEnableVertexAttribArray(mD.mPositionHandle);
		GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, mD.eyeDetailsLevel);
		
		if (Planner.SHOW_TARGET) {
			D3GLES20.draw(mPlanner.getTarget(), mPlanner.getTargetMMatrix(), mVMatrix, mProjMatrix);
		}
	}
	
	private void updateBodyVertexBuffer() {
		Matrix.multiplyMV(mD.bodyStartRot, 0, mD.mBodyStartRMatrix, 0, mD.bodyStart4, 0);
		Matrix.multiplyMV(mD.bodyBRot, 0, mD.mBodyBRMatrix, 0, mD.bodyB4, 0);
		Matrix.multiplyMV(mD.bodyCRot, 0, mD.mBodyCRMatrix, 0, mD.bodyC4, 0);
		Matrix.multiplyMV(mD.bodyEndRot, 0, mD.mBodyEndRMatrix, 0, mD.bodyEnd4, 0);
		float[] bodyVerticesData = D3GLES20.quadBezierCurveVertices(mD.bodyStartRot, mD.bodyBRot, mD.bodyCRot, mD.bodyEndRot, mD.detailsStep, mD.bodyLength);
		mD.bodyVerticesNum = bodyVerticesData.length/D3GLES20.COORDS_PER_VERTEX;
		mD.bodyVertexBuffer = D3GLES20.newFloatBuffer(bodyVerticesData);
	}

	public PointF getWorldPosition() {
		return new PointF(D3GLES20.toWorldWidth(mD.mPosX), D3GLES20.toWorldHeight(mD.mPosY));
	}

	public PointF getPosition() {
		return new PointF(mD.mPosX, mD.mPosY);
	}

	private void doAction(Action nextAction) {
		if (nextAction == null) return;
		switch(nextAction) {
		case TURN_LEFT_SMALL: turn(TurnAngle.LEFT_SMALL);break;//flopLeft(); break;
		case TURN_LEFT_MEDIUM: turn(TurnAngle.LEFT_MEDIUM);break;
		case TURN_LEFT_LARGE: turn(TurnAngle.LEFT_LARGE);break;
		case TURN_RIGHT_SMALL: turn(TurnAngle.RIGHT_SMALL);break;//flopRight(); break;
		case TURN_RIGHT_MEDIUM: turn(TurnAngle.RIGHT_MEDIUM);break;
		case TURN_RIGHT_LARGE: turn(TurnAngle.RIGHT_LARGE);break;
		case FORWARD_SMALL: backFinMotion(TurnAngle.BACK_SMALL); break;
		case FORWARD_MEDIUM: backFinMotion(TurnAngle.BACK_MEDIUM); break;
		case FORWARD_LARGE: backFinMotion(TurnAngle.BACK_LARGE); break;
		case eat: eat(); break; 
		case none: break;
		default: Log.v(TAG, "Could not process action!");
		}
	}

	private void eat() {
		Log.v(TAG, "Eating food at " + mD.mPosHeadX + " " + mD.mPosHeadY);
		mEnv.eatFood(mD.mPosHeadX, mD.mPosHeadY);
		mWorldModel.eatFood(mD.mPosHeadX, mD.mPosHeadY);
	}
}
