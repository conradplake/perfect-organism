import java.util.Collection;
import java.util.List;

import gframe.engine.Model3D;
import gframe.engine.timing.Timed;

public class ModelSwitchSequence implements Timed {

	private List<Collection<Model3D>> modelBucketList;
	private int switchIntervall;
	private boolean switchOn;
	private long timer;
	private int switchCount = 0;

	public ModelSwitchSequence(List<Collection<Model3D>> modelBucketList, int switchIntervall, boolean switchOn) {
		this.modelBucketList = modelBucketList;
		this.switchIntervall = switchIntervall;
		this.switchOn = switchOn;
	}

	@Override
	public boolean done() {
		return switchCount == modelBucketList.size();
	}

	@Override
	public void timePassedInMillis(long timePassed) {
		timer += timePassed;
		if (timer >= switchIntervall) {
			for (Model3D model : modelBucketList.get(switchCount)) {
				model.isVisible = switchOn;
			}
			switchCount++;
			timer -= switchIntervall;
		}
	}

}
