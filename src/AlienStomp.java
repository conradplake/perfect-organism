import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class AlienStomp implements Runnable {

	private MidiChannel channel;
	private int bpm;
	private int baseVolume;

	public AlienStomp(MidiChannel channel, int bpm, int baseVolume) {
		this.channel = channel;
		this.bpm = bpm;
		this.baseVolume = baseVolume;
	}

	@Override
	public void run() {
		int noteDuration = 60000 / bpm;

		AlienFluteMotif.rest(4000);

		for (int i = 0; i < 3; i++) {
			AlienFluteMotif.playNote(channel, "2C#,1C#,0C#", noteDuration * 2, baseVolume);
			AlienFluteMotif.playNote(channel, "2C#,1C#,0C#", noteDuration * 2, baseVolume);
			baseVolume *= 0.8;
		}
	}

	public static void play(Synthesizer synth) {
		int bpm = 65;

		int baseVolume = 50;

		MidiChannel channelA = synth.getChannels()[0];
		MidiChannel channelB = synth.getChannels()[1];

		channelA.programChange(synth.getAvailableInstruments()[83].getPatch().getProgram());
		channelB.programChange(synth.getAvailableInstruments()[116].getPatch().getProgram());

		AlienStomp playerA = new AlienStomp(channelA, bpm, baseVolume + 16);
		new Thread(playerA).start();

		AlienStomp playerB = new AlienStomp(channelB, bpm, baseVolume + 16);
		new Thread(playerB).start();
	}

	public static void main(String[] args) throws MidiUnavailableException {
		Synthesizer synth = MidiSystem.getSynthesizer();
		synth.open();
		play(synth);
	}
}
