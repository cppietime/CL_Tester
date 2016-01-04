package com.funguscow.synth;

import java.util.ArrayList;
import java.util.List;

public class PartLoader {

	private static final int TRACKS = 8;

	private int sampleRate; // Samples per second
	private int samplesPerBeat; // Samples per "beat"(whole note / timeBottom)
	private int timeTop; // Time signature top: beats per measure
	private int timeBottom; // Time signature bottom: beats per whole note
	private int measures; // Number of measures(or beats * timeTop) in movement
	private int duration; // Number of samples in movement: samplesPerBeat *
							// measures * timeTop

	private int refFreq; // A4 frequency
	private int updateRate; // Number of "states" per second
	private int lfoSampleRate; // Samples per second of LFOs, probably roughly
								// equal to updateRate

	private short[][] pBends; // List of pitch bend considerations,
								// [track][frame]
	private int[][] bendTimes; // List of updates up to which to apply bends

	private byte[][] octaves; // List of octaves at each update
	private byte[][] pitches; // List of notes at each update
	private byte[][] vols; // Volume per track at update

	private short[] detune; // Detune of instrument in cents, [frame]

	private byte[] attack, decay, sustain, release; // Envelope of each point

	private float[][] lfoFreqs; // Frequencies of LFO at each update
	private short[][] lfoSamples; // Samples of LFO per frame

	private short[] lowPass;
	private short[] highPass;
	private float[][] clip;
	private byte[][] doClip; // bitflags for weather to use each clip(hard,
								// norm, soft), [3][updates/8+1]
	private byte[] partVol; // Relative volume of this part

	private Part part;

	public PartLoader(int sr) {
		sampleRate = sr;
	}

	public void loadMovementData(Movement m) {
		samplesPerBeat = m.getSPB();
		timeTop = m.getTop();
		timeBottom = m.getBottom();
		measures = m.getMeasures();
		duration = m.getDuration();
		updateRate = m.getUR();
		lfoSampleRate = m.getLFO();
	}

	public void loadA4(int f) {
		refFreq = f;
	}

	public void init() {
		int updates = updateRate * duration / sampleRate;
		pBends = new short[TRACKS][];
		bendTimes = new int[TRACKS][];
		octaves = new byte[TRACKS][updates];
		pitches = new byte[TRACKS][updates];
		vols = new byte[TRACKS][updates];
		detune = new short[updates];
		attack = new byte[updates];
		decay = new byte[updates];
		sustain = new byte[updates];
		release = new byte[updates];
		lfoFreqs = new float[2][updates];
		lfoSamples = new short[2][duration * lfoSampleRate / sampleRate];
		lowPass = new short[updates];
		highPass = new short[updates];
		clip = new float[3][updates];
		doClip = new byte[3][updates / 8 + 1];
	}

	public void load(String data) {
		// Split into tokens
		String[] commands = data.split(";");
		String[][] tokens = new String[commands.length][];
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = commands[i].split(",");
		}

		// Get Pitch Bends and Bend Times
		List<List<Short>> bends = new ArrayList<List<Short>>();
		List<List<Integer>> times = new ArrayList<List<Integer>>();
		for (int i = 0; i < TRACKS; i++) {
			bends.add(new ArrayList<Short>());
			times.add(new ArrayList<Integer>());
		}
		long time = 0;
		long lastTime = 0;
		for (int index = 0; index < tokens.length; index++) {
			int timeAdd = Integer.valueOf(tokens[index][0]);
			time += timeAdd;
			if (tokens[index][1].equalsIgnoreCase("be")) {
				bends.get(Integer.valueOf(tokens[index][2])).add(
						Short.valueOf(tokens[index][3]));
				times.get(Integer.valueOf(tokens[index][2])).add(
						(int) (time - lastTime));
				lastTime = time;
			}
		}
		for (int i = 0; i < TRACKS; i++) {
			pBends[i] = new short[bends.get(i).size()];
			bendTimes[i] = new int[bends.get(i).size()];
			for (int j = 0; j < pBends[i].length; j++) {
				pBends[i][j] = bends.get(i).get(j);
				bendTimes[i][j] = times.get(j).get(j);
			}
		}

		// Setup Pitches
		time = 0;
		int ttn = 0;
		byte cvol[] = new byte[TRACKS];
		byte cpit[] = new byte[TRACKS];
		int cnote[] = new int[TRACKS];
		int remTime[] = new int[TRACKS];
		for (int i = 0; i < TRACKS; i++) {
			cvol[i] = (byte) 0xff;
			attack[i] = 0;
			decay[i] = 0;
			sustain[i] = (byte) 0xff;
			release[i] = 0;
		}
		int i = 0;
		for (time = 0; time < duration * updateRate / sampleRate; time++) {
			for(int t=0; t<TRACKS; t++){
				if(remTime[t] == 0)continue;
				pitches[t][(int)time] = cpit[t];
				double fraction = (double)(cnote[t] - remTime[t])/(double)cnote[t];
				if(fraction < (double)attack[(int)time]/255d){
					vols[t][(int)time] = (byte) ((cnote[t] - remTime[t]) * cvol[t] * 255d / (attack[(int)time] * cnote[t]));
				}else if(fraction < (double)(decay[(int)time] + attack[(int)time])/255d){
					vols[t][(int)time] = (byte)(255 - (cnote[t] - remTime[t] - (attack[(int)time] * cnote[t] / 255d)) * 255d / (decay[(int)time] * cnote[t]));
				}
			}
			while (ttn == 0) {
				ttn = Integer.valueOf(tokens[i][0]);
				if (tokens[i][1].equalsIgnoreCase("n")) {
					int def = 0;
					int curT = remTime[0];
					for (int j = 1; j < TRACKS; j++) {
						if (remTime[j] < curT)
							def = j;
					}
					cpit[def] = Byte.valueOf(tokens[i][2]);
					remTime[def] = getCorTime(tokens[i][3].charAt(0));
					cnote[def] = remTime[def];

				}
				i++;
			}
		}

	}

	private int getCorTime(char id) {
		if (id == 'w')
			return samplesPerBeat * timeBottom;
		if (id == 'h')
			return samplesPerBeat * timeBottom / 2;
		if (id == 'q')
			return samplesPerBeat * timeBottom / 4;
		if (id == 'e')
			return samplesPerBeat * timeBottom / 8;
		if (id == 's')
			return samplesPerBeat * timeBottom / 16;
		if (id == 't')
			return samplesPerBeat * timeBottom / 32;
		if (id == 'x')
			return samplesPerBeat * timeBottom / 64;
		if (id == '3')
			return samplesPerBeat * timeBottom / 12;
		else
			return samplesPerBeat;
	}

}
