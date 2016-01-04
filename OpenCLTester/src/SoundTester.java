import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;


public class SoundTester {

	private static final double phi = 1.05946309436;
	
	private SourceDataLine sdl;
	private DataLine.Info dli;
	private AudioFormat af;
	private AudioInputStream ais;
	private byte[] samples;
	private int duration;
	
	public static void main(String[] args){
		SoundTester st = new SoundTester();
	}
	
	public SoundTester(){
		duration = 5;
		samples = new byte[duration * 2 * 44100];
		generateSamps();
		PThread pt = new PThread();
		pt.setData(samples);
		pt.start();
	}
	
	private void generateSamps(){
		double[] samps = new double[duration * 44100];
		for(int i=0; i<samps.length; i++){
			double a = Math.sin(2d * Math.PI * 440d * i / 44100d);
			double b = Math.sin(2d * Math.PI * 440d * (3d/2d) * i / 44100d);
			samps[i] = (a + b) / 2d;
		}
		for(int i=0; i<samps.length; i++){
			int effI = (int)(i + 50 - 50 * Math.sin(6 * Math.PI * i / 44100d));
			if(effI >= samps.length)effI = samps.length-1;
			short samp = (short)(samps[effI] * 32767);
			samples[i * 2] = (byte)((samp & 0xff00) >>> 8);
			samples[i * 2 + 1] = (byte)(samp & 0xff);
		}
	}
	
	public AudioFormat getAudioFormat(){
		return new AudioFormat(44100, 16, 1, true, true);
	}
	
	class PThread extends Thread{
		
		private byte[] data;
		private byte[] tbuffer = new byte[10000];
		
		public void setData(byte[] d){
			data = d;
		}
		
		public void run(){
			play();
		}
		
		private void play(){
			try{
				InputStream bais = new ByteArrayInputStream(data);
				af = getAudioFormat();
				ais = new AudioInputStream(bais, af, data.length / af.getFrameSize());
				dli = new DataLine.Info(SourceDataLine.class, af);
				sdl = (SourceDataLine) AudioSystem.getLine(dli);
				sdl.open(af);
				sdl.start();
				int c;
				while((c = ais.read(tbuffer, 0, tbuffer.length)) != -1){
					if(c > 0)sdl.write(tbuffer, 0, c);
				}
				sdl.stop();
				sdl.close();
				bais.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}
	
}
