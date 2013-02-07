package d3kod.thehunt.agent.prey.planner.plans;

import android.util.Log;
import d3kod.thehunt.agent.Agent;
import d3kod.thehunt.agent.prey.Action;
import d3kod.thehunt.agent.prey.memory.MoodLevel;
import d3kod.thehunt.agent.prey.memory.StressLevel;
import d3kod.thehunt.agent.prey.memory.WorldModel;
import d3kod.thehunt.world.events.Event;

public class GoToAndEatPlan extends GoToPlan {

	private static final String TAG = "GoToAndEatPlan";
	private boolean ate;
	
	public GoToAndEatPlan(float hX, float hY, float bX, float bY, Event target) {
		super(hX, hY, bX, bY, target);
		ate = false;
	}

	@Override
	public void update(WorldModel mWorldModel) {
		if (ate) return;
		if (mWorldModel.getStressLevel() == StressLevel.PLOK_CLOSE && mWorldModel.getMoodLevel() != MoodLevel.DESPAIR) {
			Log.v(TAG, "Plok close!");
			finish();
		}
		else super.update(mWorldModel);
		if (arrived) {
			finish();
			addParallelAction(Action.eat);
			ate = true;
			return;
		}
	}
}