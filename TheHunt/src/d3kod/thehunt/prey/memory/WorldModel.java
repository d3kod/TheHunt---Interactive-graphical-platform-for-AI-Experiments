package d3kod.thehunt.prey.memory;

import java.util.ArrayList;

import android.util.Log;
import d3kod.d3gles20.D3Maths;
import d3kod.thehunt.TheHuntRenderer;
import d3kod.thehunt.environment.Dir;
import d3kod.thehunt.environment.FloatingObject;
import d3kod.thehunt.events.Event;
import d3kod.thehunt.events.Event.EventType;
import d3kod.thehunt.events.EventAlgae;
import d3kod.thehunt.events.EventAt;
import d3kod.thehunt.events.EventCurrent;
import d3kod.thehunt.events.EventFood;
import d3kod.thehunt.events.EventLight;
import d3kod.thehunt.events.EventNoise;

public class WorldModel {
	private static final String TAG = "WorldModel";
	private static final float INF = 100;
	private static final float LOUD_NOISE = 1f;
	private int hiddenForSafe = 0;
	private static final int HIDDEN_FOR_SAFE_ADJ = 20;
	private static final int HIDDEN_FOR_SAFE_MAX = 100;
	
	private static final int ENERGY_DEPLETE_SPEED = 3;
//	private static final int ONE_FOOD_ENERGY = 30;
	
	private static final int DESPAIR_ENERGY = 30;
	private static final int RISK_ENERGY = 60;
	private static final int MAX_ENERGY = 100;
	
	private static final int SECONDS_FOR_ENERGY_LOSS = 1;
	private static final int ENERGY_DEPLETE_TICKS = TheHuntRenderer.TICKS_PER_SECOND*SECONDS_FOR_ENERGY_LOSS;
	
	private static final int SECONDS_FOR_INCR_RISK = 2;
	private static final int INCR_RISK_TICKS = TheHuntRenderer.TICKS_PER_SECOND*SECONDS_FOR_INCR_RISK;
	
	//MemoryGraph mNodes;
	private float mHeadX;
	private float mHeadY;
	private float mBodyY;
	private float mBodyX;
	private int mLightLevel;
	
	private ArrayList<Event> mEventMemory = new ArrayList<Event>();
	private EventFood mNearestFood;
	private EventAlgae mNearestAlgae;
	private Dir mCurrentDir;
	private float mHeadAngle;
	private StressLevel mStressLevel;
	private int mHiddenFor;
	private int mEnergy;
	private int energyDepleteCounter;
//	private boolean mPanic;
	private int incrRiskCounter;
	private MoodLevel mMoodLevel;
	
	
	public WorldModel(float screenWidth, float screenHeight) {
//		mNodes = new MemoryGraph(screenWidth, screenHeight);
//		mNearestFoodX = mNearestFoodY = -1;
		mNearestFood = new EventFood(0, 0, 0); //TODO: I don't like this. Better make it null
		mNearestAlgae = null;
		mStressLevel = StressLevel.CALM;
		mHiddenFor = 0;
		mEnergy = MAX_ENERGY;
		energyDepleteCounter = 0;
		incrRiskCounter = 0;
	}
	public void update(ArrayList<Event> sensorEvents) {
//		mLoudNoiseHeard = false;
//		Log.v(TAG, "Updating world model");
//		mPanic = false;
		for (Event e: sensorEvents) {
			processEvent(e);
		}
		if (knowAlgaeLocation()) {
			mNearestAlgae.set(recallNearestAlgae());
		}
		else {
			mNearestAlgae = recallNearestAlgae();
		}
		
		if (energyDepleteCounter >= ENERGY_DEPLETE_TICKS) {
			energyDepleteCounter = 0;
			mEnergy -= ENERGY_DEPLETE_SPEED;
			if (mEnergy < 0) mEnergy = 0;
		}
		else {
			energyDepleteCounter++;
		}
		
		if (mEnergy <= DESPAIR_ENERGY) {
			mMoodLevel = MoodLevel.DESPAIR;
		}
		else if (mEnergy <= RISK_ENERGY) {
			mMoodLevel = MoodLevel.RISK;
			incrRiskCounter++;
			if (incrRiskCounter >= INCR_RISK_TICKS) {
				incrRiskCounter = 0;
//				hiddenForSafe -= HIDDEN_FOR_SAFE_ADJ;
//				if (hiddenForSafe < 0) hiddenForSafe = 0;
//				Log.v(TAG, "decr hiddenForSafe is now " + hiddenForSafe);
				increaseRisk();
			}
		}
		else {
			mMoodLevel = MoodLevel.NEUTRAL;
		}
	}
	private void processEvent(Event e) {
		if (mEventMemory.contains(e)) {
			//TODO: fix dirty, works only with moving food for now
			//TODO: doing it to work with algae as well now
			if (knowFoodLocation() && mNearestFood.equals(e)) {
				mNearestFood.set((EventFood) e);
			}
			else if (mNearestAlgae != null && mNearestAlgae.equals(e)) {
				mNearestAlgae.set((EventAlgae) e);
			}
			mEventMemory.remove(e);
			mEventMemory.add(e);
			return;
		}
		switch(e.type()) { 
		case AT: 
			EventAt eAt = (EventAt) e;
			mHeadX = eAt.getHeadX(); mHeadY = eAt.getHeadY();
			mBodyX = eAt.getBodyX(); mBodyY = eAt.getBodyY();
			mHeadAngle = eAt.getHeadAngle();
			break;
		case FOOD: 
			EventFood food = (EventFood)e;
			float foodX = food.getX();
			float foodY = food.getY();
			if (!knowFoodLocation() ||
					D3Maths.distance(mHeadX, mHeadY, mNearestFood.getX(), mNearestFood.getY()) > 
					D3Maths.distance(mHeadX, mHeadY, foodX, foodY)) {
				mNearestFood.set(food);
				//TODO cache current food distance
			}
			break;
		case ALGAE:
			break;
		case LIGHT:
			mLightLevel = ((EventLight) e).getLightLevel();
			if (mLightLevel == 0) {
				mHiddenFor++;
			}
			else {
				mHiddenFor = 0;
			}
			if (mStressLevel == StressLevel.PLOK_CLOSE && mHiddenFor > hiddenForSafe) {
				Log.v(TAG, "Feeling safe, be cautios now");
				mStressLevel = StressLevel.CAUTIOS;
			}
			break;
		case CURRENT:
			mCurrentDir = ((EventCurrent) e).getDir();
			break;
		case NOISE:
			EventNoise noise = (EventNoise) e;
			if (mMoodLevel.compareTo(MoodLevel.DESPAIR) < 0 && noise.getLoudness() >= LOUD_NOISE) {
				Log.v(TAG, "Loud noise heard, panic!");
				mStressLevel = StressLevel.PLOK_CLOSE;
				mHiddenFor = 0;
//				if (hiddenForSafe < HIDDEN_FOR_SAFE_MAX) hiddenForSafe += HIDDEN_FOR_SAFE_ADJ;
//				Log.v(TAG, "incr hiddenForSafe is now " + hiddenForSafe);
				decreaseRisk();
//				mPanic = true;
			}
		}
		if (e.type() == EventType.FOOD || e.type() == EventType.ALGAE) {
			rememberEvent(e);
		}
	}

	private void rememberEvent(Event e) {
		mEventMemory.add(e);
	}
	
	private void increaseRisk() {
		hiddenForSafe -= HIDDEN_FOR_SAFE_ADJ;
		if (hiddenForSafe < 0) hiddenForSafe = 0;
		Log.v(TAG, "decr hiddenForSafe is now " + hiddenForSafe);
	}
	private void decreaseRisk() {
		if (hiddenForSafe < HIDDEN_FOR_SAFE_MAX) hiddenForSafe += HIDDEN_FOR_SAFE_ADJ;
		Log.v(TAG, "incr hiddenForSafe is now " + hiddenForSafe);
	}
	public int getLightLevel() {
		return mLightLevel;
	}
	public float getNearestFoodX() {
		return mNearestFood.getX();
	}
	public float getNearestFoodY() {
		return mNearestFood.getY();
	}
	public EventFood getNearestFood() {
		return mNearestFood;
	}
	public EventAlgae getNearestAlgae() {
		return mNearestAlgae;
	}
	public float getNearestAlgaeX() {
		return mNearestAlgae.getX();
	}
	public float getNearestAlgaeY() {
		return mNearestAlgae.getY();
	}
	public float getHeadX() {
		return mHeadX;
	}
	public float getHeadY() {
		return mHeadY;
	}
	public void eatFood(int energy) {
		//TODO: the food removed is not always the nearest food #BUG
		mEnergy += energy;
		if (mEnergy > MAX_ENERGY) {
			mEnergy = MAX_ENERGY;
		}
		mEventMemory.remove(mNearestFood);
		mNearestFood.set(recallNearestFood());
	}
	private EventFood recallNearestFood() {
		float closestX = INF, closestY = INF;
		EventFood closestFood = new EventFood(0, 0, 0);
		
		for (Event e: mEventMemory) {
			if (e.type() == EventType.FOOD) {
				EventFood ef = (EventFood)e;
				float foodX = ef.getX();
				float foodY = ef.getY();
				if (D3Maths.distance(mHeadX, mHeadY, foodX, foodY) <
						D3Maths.distance(mHeadX, mHeadY, closestX, closestY)) {
					closestFood = ef;
					closestX = foodX; 
					closestY = foodY;
				}
			}
		}
		
		return closestFood;
	}
	
	private EventAlgae recallNearestAlgae() {
		float closestX = INF, closestY = INF;
		EventAlgae closestAlgae = null;
		
		for (Event e: mEventMemory) {
			if (e.type() == EventType.ALGAE) {
				EventAlgae ea = (EventAlgae)e;
				float foodX = ea.getX();
				float foodY = ea.getY();
				if (D3Maths.distance(mHeadX, mHeadY, foodX, foodY) <
						D3Maths.distance(mHeadX, mHeadY, closestX, closestY)) {
					closestAlgae = ea;
					closestX = foodX; 
					closestY = foodY;
				}
			}
		}
		
		return closestAlgae;	
	}
	
	public float getBodyX() {
		return mBodyX;
	}
	public float getBodyY() {
		return mBodyY;
	}
	public boolean knowFoodLocation() {
		return (mNearestFood.getNutri() > 0);
	}
	public boolean knowAlgaeLocation() {
		return mNearestAlgae != null;
	}
	public void recalcNearestFood() {
		mNearestFood.set(recallNearestFood());
	}
	public Dir getCurrentDir() {
		return mCurrentDir;
	}
	
	public float getHeadAngle() {
		return mHeadAngle;
	}
	
	public StressLevel getStressLevel() {
		return mStressLevel;
	}
	
	public int getEnergy() {
		return mEnergy;
	}
	
	public MoodLevel getMoodLevel() {
		return mMoodLevel;
	}
	public void refillEnergy() {
		mEnergy = MAX_ENERGY;
	}
}
