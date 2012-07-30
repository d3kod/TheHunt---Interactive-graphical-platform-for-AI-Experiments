package d3kod.thehunt.prey.planner;

import android.util.Log;
import d3kod.d3gles20.D3GLES20;
import d3kod.thehunt.prey.Action;
import d3kod.thehunt.prey.memory.WorldModel;

public class ScavagePlan extends Plan {
	private static final float DISTANCE_ENOUGH = 0.1f;
	private static final String TAG = "SavagePlan";
	private float distanceFromFoodPrev;
	private boolean distanceDecreases;
	Action spinAction = Action.flopLeft;
	private boolean moveForward;
	private boolean distanceFinsDecreases;
	private float fX;
	private float fY;
	
	private float[] targetColor = {1.0f, 0.0f, 0.0f};
	private float targetSize = 0.005f;
	private boolean finishAfterNext;
	public ScavagePlan(float hX, float hY, float bX, float bY, float fX, float fY) {
		super(fX, fY);
		
		this.fX = fX; this.fY = fY;
		if (fX == -1 || fY == -1) {
			Log.v(TAG, "No food location known!");
			return;
		}
//		float fX = foodLocation.getX(), fY = foodLocation.getY(),
//				mX = mWorldModel.getHeadX(), mY = mWorldModel.getHeadY();
//		distanceFromFoodPrev = D3GLES20.distance(hX, hY, fX, fY);
		//addNextAction(spinAction);
//		distanceDecreases = false;
//		moveForward = false;
//		distanceFinsDecreases = false;
	}
	public void update(WorldModel mWorldModel) {
		if (finishAfterNext) {
			finish();
			return;
		}
//		if (mCurrentAction == -1) return;
		float hX = mWorldModel.getHeadX(), hY = mWorldModel.getHeadY(),
				bX = mWorldModel.getBodyX(), bY = mWorldModel.getBodyY();
		float headFromFood = D3GLES20.distance(hX, hY, fX, fY),
				bodyFromFood = D3GLES20.distance(bX, bY, fX, fY);

		if (headFromFood <= DISTANCE_ENOUGH) {
			addNAction(Action.eat);
			finishAfterNext = true;
			return;
		}
		float bhf = D3GLES20.det(bX, bY, hX, hY, fX, fY);
		if (bhf > 0) {
			addNextAction(Action.flopLeft);
		}
		else if (bhf < 0) {
			addNextAction(Action.flopRight);
		}
		else if (headFromFood > bodyFromFood) {
			addNextAction(Action.flopLeft);
		}
		else {
			addNextAction(Action.flopLeft);
			addNextAction(Action.flopRight);
		}
	}
}
