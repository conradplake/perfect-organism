import java.awt.Color;

import gframe.engine.Model3D;
import gframe.engine.timing.Timed;

public class BlinkLight implements Timed {
	
	private Model3D lightModel;
	private Color onColor;
	private Color offColor;
	private long startTime;
	private int blinkIntervall;
	private int maxBlinkTime;

	private boolean lightIsOff;
	
	private long totalBlinkTime;
	private long timer;

	public BlinkLight(Model3D lightModel, Color onColor, Color offColor, long startTime, int blinkIntervall, int maxBlinkTime) {
		this.lightModel = lightModel;
		this.onColor = onColor;
		this.offColor = offColor;
		this.startTime = startTime;
		this.blinkIntervall = blinkIntervall;
		this.maxBlinkTime = maxBlinkTime;
	}

	@Override
	public boolean done() {
		return totalBlinkTime > maxBlinkTime;
	}

	@Override
	public void timePassedInMillis(long timePassed) {
		if (System.currentTimeMillis() < startTime) {
			return;
		}
		totalBlinkTime += timePassed;
		
		timer += timePassed;		
		if (timer >= blinkIntervall) {

			if (lightIsOff) {
				lightModel.setColor(onColor);
			} else {
				lightModel.setColor(offColor);
			}
			lightIsOff = !lightIsOff;

			timer -= blinkIntervall;
		}
	}

}
