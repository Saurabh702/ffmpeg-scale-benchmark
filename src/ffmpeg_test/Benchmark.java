package ffmpeg_test;

import java.io.IOException;
//import java.util.Scanner;

public class Benchmark {

	/*	Help
	 *  ====
	 *  
	 *  java Benchmark <input-file> <output-file> <new-resolution> <processing-method>
	 *  	input-file : file path for input video
	 *  	output-file : file path for output video
	 *  	new-resolution : a resolution for the output video which is divisible by 2
	 *  	processing-method : w (process whole video) or s (split video into 2 segments and process each part) 
	 */

	/*	Batch Run
	 *	=========
	 *
	 *	To run this program over multiple files in Windows CMD, do the following
	 * 
	 *	->	Create a Runnable jar ( command to run the jar file : java -jar test.jar <input> <output> <resolution> <w/s> )
	 * 
	 *	->	To process all videos in a directory as a whole :
	 * 		
	 *		FOR %A IN ("\path\to\directory\*") DO java -jar <jar-name>.jar %~fA \path\to\output\%~nA_W%~xA <resolution> w
	 * 
	 *	->	To process all videos in a directory as segments :
	 * 		
	 *		FOR %A IN ("\path\to\directory\*") DO java -jar <jar-name>.jar %~fA \path\to\output\%~nA_S%~xA <resolution> s
	 * 
	 *  References
	 *  ==========
	 *  
	 *  ->	https://ss64.com/ 
	 *  
	 *  ->	https://superuser.com/a/489242
	 */

	public static float timer, timerT1, timerT2;

	public static void main(String[] args) throws IOException {

		String command,input_filepath, output_filepath, duration;
		String resolution[] = new String[2];
		int new_resolution;
		//Scanner reader = new Scanner(System.in);

		//System.out.print("Enter the Input file path : ");
		input_filepath = args[0];//reader.nextLine();

		/*
		 * Get Duration : ffprobe -i input.mp4 -show_entries format=duration -v quiet -of csv="p=0"
		 * https://stackoverflow.com/a/22243834
		 * 
		 * Get Resolution : ffprobe -i input.mp4 -show_entries stream=width,height -v quiet -of csv="p=0"
		 * https://superuser.com/a/841379
		 */

		command = "ffprobe -i "+ input_filepath + " -show_entries format=duration -v quiet -of csv=\"p=0\"";
		duration = Processes.run(new String[] {"cmd.exe","/c",command}).trim();

		command = "ffprobe -i " + input_filepath + " -show_entries stream=width,height -v quiet -of csv=\"p=0\"";
		resolution =  Processes.run(new String[] {"cmd.exe","/c",command}).split(",");

		System.out.println("Input file info : Path = " + input_filepath + " , Duration = " + duration + " seconds , Resolution = " + resolution[1].trim() + "p");

		//System.out.print("Enter new resolution (144,240,360,480,720...) : ");
		new_resolution = Integer.parseInt(args[2]);//reader.nextInt();

		//reader.nextLine();

		//System.out.print("Enter the Output file path : ");
		output_filepath = args[1];//reader.nextLine();

		//System.out.print("Process video as a whole (1) or Process as segments (2) ? ");

		/*
		 * To avoid width not divisible by 2 error in ffmpeg : https://stackoverflow.com/a/29582287
		 * Note: New resolution(height) should be divisible by 2
		 */

		int choice = args[3].equals("w")? 1 : args[3].equals("s")? 2 : 0;

		switch(choice) //reader.nextInt()
		{
		case 1: 
			System.out.println("\nProcessing...");
			command = "ffmpeg -nostats -i " + input_filepath + " -vf scale=-2:" + new_resolution + " " + output_filepath + " -benchmark 2>&1 | findstr rtime";
			System.out.println("\nBenchmark: " + Processes.run(new String[] {"cmd.exe","/c",command}).split("=")[3].replace("s","").trim() + "s");
			break;
		case 2: 
			System.out.println("\nSplitting video into 2 approx. equal segments...");

			Integer temp = (int) ((new Float(duration))/2);

			timer = timerT1 = timerT2 = 0.000f;

			// Running two tasks simultaneously using threads : https://stackoverflow.com/a/12191090

			Thread T1 = new Thread(new Runnable() {
				public void run() {
					try {

						System.out.println("\nThread-1, Re-encoding first video segment using ultrafast preset");

						String command = "ffmpeg -nostats -ss 00:00:00.000 -i " + input_filepath + " -t " + temp.toString() + " -c:v libx264 -preset ultrafast -c:a copy temp1.mp4 -benchmark 2>&1 | findstr rtime";
						timerT1 += new Float(Processes.run(new String[] {"cmd.exe","/c",command}).split("=")[3].replace("s",""));

						System.out.println("\nThread-1, Scaling first video segment");

						command = "ffmpeg -nostats -i temp1.mp4 -vf scale=-2:" + new_resolution + " output1.mp4 -benchmark 2>&1 | findstr rtime && echo file 'output1.mp4' > list.txt";
						timerT1 += new Float(Processes.run(new String[] {"cmd.exe","/c",command}).split("=")[3].replace("s",""));

						//System.out.println("Thread1 : " + timerT1);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			Thread T2 = new Thread(new Runnable() {
				public void run() {
					try {

						System.out.println("\nThread-2, Re-encoding second video segment using ultrafast preset");

						String command = "ffmpeg -nostats -i " + input_filepath + " -ss " + temp.toString() + " -c:v libx264 -preset ultrafast -c:a copy temp2.mp4 -benchmark 2>&1 | findstr rtime";
						timerT2 += new Float(Processes.run(new String[] {"cmd.exe","/c",command}).split("=")[3].replace("s",""));

						System.out.println("\nThread-2, Scaling second video segment");

						command = "ffmpeg -nostats -i temp2.mp4 -vf scale=-2:" + new_resolution + " output2.mp4 -benchmark 2>&1 | findstr rtime && echo file 'output2.mp4' >> list.txt";
						timerT2 += new Float(Processes.run(new String[] {"cmd.exe","/c",command}).split("=")[3].replace("s",""));

						//System.out.println("Thread2 : " + timerT2);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			try {
				// Start threads and block until all threads finish
				T1.start();
				T2.start();
				T1.join();
				T2.join();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("\nMerging segments...");

			command = "ffmpeg -nostats -f concat -i list.txt -c copy " + output_filepath + " -benchmark 2>&1 | findstr rtime";	        	
			timer += new Float(Processes.run(new String[] {"cmd.exe","/c",command}).split("=")[3].replace("s",""));

			// Computing time taken by threads (select greater of 2)
			timer += (timerT1 > timerT2)? timerT1 : timerT2;

			System.out.println("\nBenchmark: " + timer + "s");
			break;
		default: System.out.println("\nWrong Choice !!");
		}	

		//TODO : cleanup code
		command = "rm temp1.mp4 temp2.mp4 list.txt output1.mp4 output2.mp4";
		Processes.run(new String[] {"cmd.exe","/c",command});

		//reader.close();	
	}
}