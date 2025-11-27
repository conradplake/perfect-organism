import java.util.Arrays;
import java.util.List;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class AlienFluteMotif implements Runnable {

	static final List<String> NOTES = Arrays.asList("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#",
			"B");

	private MidiChannel channel;
	private int bpm;
	private int baseVolume;
	private boolean finished;

	public AlienFluteMotif(MidiChannel channel, int bpm, int baseVolume) {
		this.channel = channel;
		this.bpm = bpm;
		this.baseVolume = baseVolume;
	}

	@Override
	public void run() {

		int noteDuration = 60000 / bpm;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < 2; i++) {
			// 54
			playNote(channel, "5F#,5D#,4A#", noteDuration * 12, (int) (baseVolume * 0.8));

			// System.out.println(System.currentTimeMillis() - startTime);

			playNote(channel, "6C#,5A#,5F#,4D#", noteDuration, baseVolume);
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "6C#,5A#,5F#,4D#", noteDuration, baseVolume);
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "6C#,5A#,5F#,4D#", noteDuration, baseVolume);
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.8));

			playNote(channel, "5F#,5D#,4A#", noteDuration * 6, (int) (baseVolume * 0.8));

			// 60
			// System.out.println(System.currentTimeMillis() - startTime);

			playNote(channel, "6F#,6C#,5A,4D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "6C,5G,5D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "6F#,6C#,5A,4D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "6C,5G,5D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "6F#,6C#,5A,4D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "6C,5G,5D#", noteDuration, (int) (baseVolume * 0.8));

			// 61
			playNote(channel, "6F#,6C#,5A,4D#", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "6C,5G,5D#", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "6F#,6C#,5A,4D#", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "6C,5G,5D#", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "6F#,6C#,5A,4D#", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "6C,5G,5D#", noteDuration, (int) (baseVolume * 0.7));

			// 62
			playNote(channel, "5F#,5D#,4A#", noteDuration * 6, (int) (baseVolume * 0.8));

			// System.out.println(System.currentTimeMillis() - startTime);

			playNote(channel, "6D,5A#,5G,4D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.6));
			playNote(channel, "6D,5A#,5G,4D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.6));
			playNote(channel, "6D,5A#,5G,4D#", noteDuration, (int) (baseVolume * 0.8));
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.6));

			// 65
			playNote(channel, "6D,5A#,5G", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.5));
			playNote(channel, "6D,5A#,5G", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.5));
			playNote(channel, "6D,5A#,5G", noteDuration, (int) (baseVolume * 0.7));
			playNote(channel, "5A,5E,5C#", noteDuration, (int) (baseVolume * 0.5));

			baseVolume *= 0.9;
		}

		playNote(channel, "5F#,5D#,4A#", noteDuration * 3, (int) (baseVolume * 0.5));
		playNote(channel, "5F#,5D#,4A#", noteDuration * 3, (int) (baseVolume * 0.4));

		finished = true;
	}

	public boolean finished() {
		return finished;
	}

	static void playNote(MidiChannel channel, String notes, int duration, int vol) {
		for (String note : notes.split(",")) {
			channel.noteOn(midiNoteNumber(note), vol);
		}

		rest(duration);

		for (String note : notes.split(",")) {
			channel.noteOff(midiNoteNumber(note));
		}
	}

	static void rest(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the MIDI note number for a given note, e.g. 4C -> 60
	 */
	private static int midiNoteNumber(String note) {
		int octave = Integer.parseInt(note.substring(0, 1));
		return NOTES.indexOf(note.substring(1)) + 12 * octave + 12;
	}

	public static void play(Synthesizer synth) {

		MidiChannel channelA = synth.getChannels()[0];
		MidiChannel channelB = synth.getChannels()[1];
		MidiChannel channelC = synth.getChannels()[2];
		MidiChannel channelD = synth.getChannels()[3];
		Instrument stringSlapInstrument = synth.getAvailableInstruments()[145];
		Instrument fluteInstrument = synth.getAvailableInstruments()[73];
		Instrument glockenspielInstrument = synth.getAvailableInstruments()[9];
		Instrument choirInstrument = synth.getAvailableInstruments()[225];
		Instrument tremoloString = synth.getAvailableInstruments()[44];
		Instrument testInstr = synth.getAvailableInstruments()[91]; // 91 = Space voice

		channelA.programChange(choirInstrument.getPatch().getProgram());
		channelB.programChange(glockenspielInstrument.getPatch().getProgram());
		channelC.programChange(fluteInstrument.getPatch().getProgram());
		channelD.programChange(testInstr.getPatch().getProgram());

		int bpm = 105;

		int baseVolume = 20;
		
		AlienFluteMotif playerA = new AlienFluteMotif(channelA, bpm, baseVolume + 16);
		new Thread(playerA).start();
		//
		AlienFluteMotif playerB = new AlienFluteMotif(channelB, bpm, baseVolume + 13);
		new Thread(playerB).start();
		//
		AlienFluteMotif playerC = new AlienFluteMotif(channelC, bpm, baseVolume + 15);
		new Thread(playerC).start();

		AlienFluteMotif playerD = new AlienFluteMotif(channelD, bpm, baseVolume + 18);
		new Thread(playerD).start();

	}

	public static void main(String[] args) throws MidiUnavailableException {
		Synthesizer synth = MidiSystem.getSynthesizer();
		synth.open();
		play(synth);
	}
}
