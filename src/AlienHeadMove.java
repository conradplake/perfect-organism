import gframe.engine.Model3D;
import gframe.engine.timing.Timed;

public class AlienHeadMove implements Timed {

	private Model3D head;
	private long startTime;

	
	private int moveForwardTime = 12000;
	private int headUpTime = 250;
	private float degreePerMillis = 0.04f;
	private float moveForwardPerMillis = 0.05f;

	public AlienHeadMove(Model3D head, long startTime) {
		this.head = head;
		this.startTime = startTime;
	}

	@Override
	public boolean done() {
		return headUpTime <= 0;
	}

	@Override
	public void timePassedInMillis(long timePassed) {
		if (System.currentTimeMillis() < startTime) {
			return;
		}
		
		head.isVisible = true;
		
		if(moveForwardTime > 0) {
			long moveTime = Math.min(moveForwardTime, timePassed);
			head.getOrigin().move(0, 0, moveTime * moveForwardPerMillis);
			moveForwardTime -= timePassed;
		} else if (headUpTime > 0) {
			long moveTime = Math.min(headUpTime, timePassed);
			head.rotate(moveTime * degreePerMillis, 0, 0);
			headUpTime -= timePassed;
		}
	}

}
