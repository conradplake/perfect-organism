import gframe.engine.Camera;
import gframe.engine.timing.Timed;

public class CamMove implements Timed {

	private Camera camera;

	private float speed;

	private int moveTime;

	private long timer;

	public CamMove(Camera camera, float speed, int moveTime) {
		this.camera = camera;
		this.speed = speed;
		this.moveTime = moveTime;
	}

	@Override
	public boolean done() {
		return timer > moveTime;
	}

	@Override
	public void timePassedInMillis(long timePassed) {
		timer += timePassed;
		camera.move(speed * timePassed);
	}

}
