import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class InputTest {
	
	private static SourceDataLine sdl;
	private static DataLine.Info dli;
	private static AudioFormat af;
	
	public static void main(String[] args){
//		long time = System.currentTimeMillis();
//		double d;
//		for(int i=0; i<44100; i++){
//			d = Math.sin(i * Math.PI) * Math.cos(i * 2d) / 44100;
//		}
//		System.out.println(System.currentTimeMillis() - time);
//		System.exit(0);
		PThread pt = new PThread();
		pt.start();
		Scanner scan = new Scanner(System.in);
		while(pt.cont){
			String in = scan.nextLine();
			if(in.equalsIgnoreCase("start"))pt.playSound(true);
			if(in.equalsIgnoreCase("stop"))pt.playSound(false);
			if(in.equalsIgnoreCase("first"))pt.setData(440);
			if(in.equalsIgnoreCase("major"))pt.setData(550);
			if(in.equalsIgnoreCase("fifth"))pt.setData(660);
			if(in.equalsIgnoreCase("octave"))pt.setData(880);
			if(in.equalsIgnoreCase("quit"))pt.cont = false;
		}
		scan.close();
	}

	public static AudioFormat getAudioFormat() {
		return new AudioFormat(44100, 8, 1, true, false);
	}

	static class PThread extends Thread {

		private static final int bsize = 1764;
		
		private byte[] tbuffer = new byte[bsize];
		private boolean playNow = true;
		private int freq = 440;
		public boolean cont = true;
		
		public void playSound(boolean t){
			playNow = t;
		}

		public void setData(int f) {
			System.out.println("setting");
			freq = f;
		}

		public void run() {
			play();
		}

		private void play() {
			try {
				af = getAudioFormat();
				dli = new DataLine.Info(SourceDataLine.class, af);
				sdl = (SourceDataLine) AudioSystem.getLine(dli);
				sdl.open(af, bsize);
				sdl.start();
				int index=0;
				while (cont) {
					for(int i=0; i<tbuffer.length; i++){
						if(playNow){
							double d = (Math.sin(2d * Math.PI * (double)index * (double)freq / 44100d))/2d;
							tbuffer[i] = (byte)((short)(d * 255) & 0xff);
//							System.out.println(d);
						}else tbuffer[i] = 0;
						index++;
						if(index % (44100d/freq) == 0)index=0;
					}
					sdl.write(tbuffer, 0, tbuffer.length);
//					b[0] = 0;
//					if(playNow){
//						double d = (Math.sin(2 * Math.PI * index * 440 / 44100))/2d;
////						if(d==0)if(++count >= 2){
////							index = 0;
////							count = 0;
////						}
//						b[0] = (byte)((short)(d*255));
//					}
//					sdl.write(b, 0, 1);
				}
				sdl.stop();
				sdl.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
