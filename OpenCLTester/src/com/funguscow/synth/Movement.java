package com.funguscow.synth;

public class Movement {

	private int sampleRate; //Samples per second
	
	private PartLoader[] parts; //"Tracks", instruments
	private int samplesPerBeat; //Samples per "beat"(whole note / timeBottom)
	private int timeTop; //Time signature top: beats per measure
	private int timeBottom; //Time signature bottom: beats per whole note
	private int measures; //Number of measures(or beats * timeTop) in movement
	private int duration; //Number of samples in movement: samplesPerBeat * measures * timeTop
	private int updateRate;
	private int lfoSampleRate;
	
	private float globalVol;
	
	public int getSPB(){return samplesPerBeat;}
	public int getTop(){return timeTop;}
	public int getBottom(){return timeBottom;}
	public int getMeasures(){return measures;}
	public int getDuration(){return duration;}
	public int getUR(){return updateRate;}
	public int getLFO(){return lfoSampleRate;}
	
}
