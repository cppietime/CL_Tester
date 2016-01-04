import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

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
	private float duration;
	private String sourceFile;
	
	private int rate;
	private int depth;
	private int channels;
	private int dataSize;
	private boolean signed;
	private boolean bigEnd;
	
	public static void main(String[] args){
		Scanner scan = new Scanner(System.in);
		SoundTester st = new SoundTester(scan.nextLine());
		scan.close();
	}
	
	public SoundTester(String path){
		rate = 44100;
		depth = 16;
		channels = 1;
		bigEnd = false;
		signed = true;
		sourceFile = path;
		generateFromFile();
		duration = dataSize * 8/ (rate * channels * depth);
		resample(0.5f);
		PThread pt = new PThread();
		pt.setData(samples);
		pt.start();
	}
	
	public SoundTester(){
		rate = 44100;
		depth = 16;
		channels = 1;
		bigEnd = true;
		signed = true;
		duration = 5;
		samples = new byte[(int)(duration * 2 * rate)];
		generateSamps();
		PThread pt = new PThread();
		pt.setData(samples);
		pt.start();
	}
	
	private short bytesToShort(byte[] b){
		if(b.length < 2)return 0;
		return (short) ((b[0] & 0xff) | ((b[1] & 0xff) << 8));
	}
	
	private int bytesToInt(byte[] b){
		if(b.length < 4)return 0;
		return (b[0] & 0xff) | ((b[1] & 0xff) << 8) | ((b[2] & 0xff) << 16) | ((b[3] & 0xff) << 24);
	}
	
	private void generateFromFile(){
		try {
			File f = new File(sourceFile);
			InputStream in = new FileInputStream(f);
			byte[] temp = new byte[4];
			in.skip(22);
			in.read(temp, 0, 2);
			channels = bytesToShort(temp);
			in.read(temp, 0, 4);
			rate = bytesToInt(temp);
			in.skip(6);
			in.read(temp, 0, 2);
			depth = bytesToShort(temp);
			if(depth == 8)signed = false;
			in.skip(4);
			in.read(temp, 0, 4);
			dataSize = bytesToInt(temp);
//			if(dataSize != f.length() - 44){
//				in.close();
//				throw new Exception("File size discrepency!");
//			}
			samples = new byte[dataSize];
			in.read(samples, 0, dataSize);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void generateSamps(){
		double[] samps = new double[(int)(duration * rate)];
		for(int i=0; i<samps.length; i++){
			double a = Math.sin(2d * Math.PI * 440d * i / (double)rate);
			double b = Math.sin(2d * Math.PI * 440d * (3d/2d) * i / (double)rate);
			samps[i] = (a + b) / 2d;
		}
		for(int i=0; i<samps.length; i++){
			int effI = (int)(i + 50 - 50 * Math.sin(6 * Math.PI * i / (double)rate));
			if(effI >= samps.length)effI = samps.length-1;
			short samp = (short)(samps[effI] * 32767);
			samples[i * 2] = (byte)((samp & 0xff00) >>> 8);
			samples[i * 2 + 1] = (byte)(samp & 0xff);
		}
	}
	
	private void resample(float factor){
		float newDur = duration * factor;
		short[] tempA = new short[(int)(duration * rate * channels)];
		for(int i=0; i<tempA.length; i++){
			tempA[i] = (short) ((samples[i*2] & 0xff) | ((samples[i*2+1] & 0xff) << 8));
		}
		short[] tempB = new short[(int)(newDur * rate * channels)];
		samples = new byte[(int)(newDur * rate * channels * depth / 8)];
		for(int i=0; i<tempB.length/channels; i++){
			for(int c=0; c<channels; c++){
				int index = i * channels + c;
				tempB[index] = tempA[(int)((float)index / factor)];
				samples[index*2] = (byte) (tempB[index] & 0xff);
				samples[index*2+1] = (byte)((tempB[index] & 0xff00) >>> 8);
			}
		}
		duration = newDur;
	}
	
	public AudioFormat getAudioFormat(){
		return new AudioFormat(rate, depth, channels, signed, bigEnd);
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
