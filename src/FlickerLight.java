import gframe.engine.Model3D;
import gframe.engine.timing.Timed;

public class FlickerLight implements Timed {

	private boolean lightIsOff;
	private Model3D lightModel;
	private Model3D reflectionModel;
	private long startTime;
	private int flickerIntervall;

	private long timer;

	public FlickerLight(Model3D lightModel, Model3D reflectionModel, long startTime, int flickerIntervall) {
		this.lightModel = lightModel;
		this.reflectionModel = reflectionModel;
		this.startTime = startTime;
		this.flickerIntervall = flickerIntervall;
	}

	public void switchOff() {
		lightIsOff = true;
	}

	@Override
	public boolean done() {
		return lightIsOff;
	}

	@Override
	public void timePassedInMillis(long timePassed) {
		if (System.currentTimeMillis() < startTime) {
			return;
		}
		timer += timePassed;
		if (timer >= flickerIntervall) {

			if (Math.random() < 0.5) {
				lightModel.isVisible = true;
				if (reflectionModel != null) {
					reflectionModel.isVisible = true;
				}
			} else {
				lightModel.isVisible = false;
				if (reflectionModel != null) {
					reflectionModel.isVisible = false;
				}
			}

			timer -= flickerIntervall;
		}
	}

}
