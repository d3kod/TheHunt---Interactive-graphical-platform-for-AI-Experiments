package d3kod.thehunt.prey.sensor;

import java.util.ArrayList;

import d3kod.thehunt.environment.Environment;
import d3kod.thehunt.environment.EnvironmentData;

public class Sensor {
	
	enum Sensors {
		CURRENT_SENSOR, FOOD_SENSOR, ALGAE_SENSOR;
	}
	private ArrayList<Sensors> mSensors;
	private Environment mEnv;
	
	public Sensor(Environment env) {
		mEnv = env;
		mSensors = new ArrayList<Sensor.Sensors>();
		mSensors.add(Sensors.CURRENT_SENSOR);
		mSensors.add(Sensors.FOOD_SENSOR);
		mSensors.add(Sensors.ALGAE_SENSOR);
	}
	public ArrayList<Event> sense(float hX, float hY, float bX, float bY) {
		ArrayList<Event> sensedEvents = new ArrayList<Event>();
		sensedEvents.add(new EventAt(hX, hY, bX, bY));
		for (Sensors sensor: mSensors) {
			switch(sensor) {
			case CURRENT_SENSOR: sensedEvents.add(mEnv.senseCurrent(bX, bY)); break;
			case FOOD_SENSOR:  sensedEvents.add(mEnv.senseFood(hX, hY)); break;
			case ALGAE_SENSOR: sensedEvents.add(mEnv.senseAlgae()); break;
			}
		}
		return sensedEvents;
	}

}
