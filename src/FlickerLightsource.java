import gframe.engine.Lightsource;
import gframe.engine.timing.Timed;

public class FlickerLightsource implements Timed {

	private Lightsource lightsource;
	private float minIntensity;
	private float maxIntensity;
	private int flickerIntervall;
	private int runTime;

	private float timer;
	private long startTime;

	public FlickerLightsource(Lightsource lightsource, float minIntensity, float maxIntensity, int flickerIntervall,
			int runTime) {
		this.lightsource = lightsource;
		this.minIntensity = minIntensity;
		this.maxIntensity = maxIntensity;
		this.flickerIntervall = flickerIntervall;
		this.runTime = runTime;

		this.startTime = System.currentTimeMillis();
	}

	@Override
	public boolean done() {
		if ((System.currentTimeMillis() - startTime) > runTime) {
			lightsource.setIntensity(minIntensity);
			return true;
		}
		return false;
	}

	@Override
	public void timePassedInMillis(long timePassed) {
		timer += timePassed;
		if (timer >= flickerIntervall) {

			if (Math.random() < 0.5) {
				lightsource.setIntensity(minIntensity);
			} else {
				lightsource.setIntensity(maxIntensity);
			}

			timer -= flickerIntervall;
		}
	}

}
