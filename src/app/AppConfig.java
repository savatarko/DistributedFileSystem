package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * This class contains all the global application configuration stuff.
 * @author bmilojkovic
 *
 */
public class AppConfig {

	/**
	 * Convenience access for this servent's information
	 */
	public static ServentInfo myServentInfo;
	
	/**
	 * Print a message to stdout with a timestamp
	 * @param message message to print
	 */
	public static void timestampedStandardPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.out.println(timeFormat.format(now) + " - " + message);
	}
	
	/**
	 * Print a message to stderr with a timestamp
	 * @param message message to print
	 */
	public static void timestampedErrorPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.err.println(timeFormat.format(now) + " - " + message);
	}
	
	public static boolean INITIALIZED = false;
	public static int BOOTSTRAP_PORT;
	public static int SERVENT_COUNT;
	
	public static ChordState chordState;
	public static String rootDirectory;
	public static Long weakLimit;
	public static long strongLimit;
	public static final Object tokenLock = new Object();
	public static HeartbeatManager heartbeatManager;
	public static boolean connection = false;
	
	/**
	 * Reads a config file. Should be called once at start of app.
	 * The config file should be of the following format:
	 * <br/>
	 * <code><br/>
	 * servent_count=3 			- number of servents in the system <br/>
	 * chord_size=64			- maximum value for Chord keys <br/>
	 * bs.port=2000				- bootstrap server listener port <br/>
	 * servent0.port=1100 		- listener ports for each servent <br/>
	 * servent1.port=1200 <br/>
	 * servent2.port=1300 <br/>
	 * 
	 * </code>
	 * <br/>
	 * So in this case, we would have three servents, listening on ports:
	 * 1100, 1200, and 1300. A bootstrap server listening on port 2000, and Chord system with
	 * max 64 keys and 64 nodes.<br/>
	 * 
	 * @param configName name of configuration file
	 * @param serventId id of the servent, as used in the configuration file
	 */
	public static void readConfig(String configName, int serventId){
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));
			
		} catch (IOException e) {
			timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}
		try {
			BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bs.port"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading bootstrap_port. Exiting...");
			System.exit(0);
		}
		if(configName.contains("servent_list.properties")) {
			try {
				SERVENT_COUNT = Integer.parseInt(properties.getProperty("servent_count"));
			} catch (NumberFormatException e) {
				timestampedErrorPrint("Problem reading servent_count. Exiting...");
				System.exit(0);
			}
			return;
		}
		
//		try {
//			int chordSize = Integer.parseInt(properties.getProperty("chord_size"));
//
//			ChordState.CHORD_SIZE = chordSize;
//			chordState = new ChordState();
//
//		} catch (NumberFormatException e) {
//			timestampedErrorPrint("Problem reading chord_size. Must be a number that is a power of 2. Exiting...");
//			System.exit(0);
//		}
		
		String portProperty = "port";
		
		int serventPort = -1;
		
		try {
			serventPort = Integer.parseInt(properties.getProperty(portProperty));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
			System.exit(0);
		}

		try{
			rootDirectory = properties.getProperty("root");
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading root directory. Exiting...");
			System.exit(0);
		}

		try {
			deleteDirectory(new File(rootDirectory));
		} catch (Exception e) {
			timestampedErrorPrint("Problem deleting root directory. Exiting...");
			System.exit(0);
		}

		try{
			weakLimit = Long.parseLong(properties.getProperty("weak_failure_limit"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading weak limit. Exiting...");
			System.exit(0);
		}
		try{
			strongLimit = Long.parseLong(properties.getProperty("strong_failure_limit"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading strong limit. Exiting...");
			System.exit(0);
		}

		String ipAddress = properties.getProperty("ip");

		myServentInfo = new ServentInfo(ipAddress, serventPort);

		chordState = new ChordState();

		heartbeatManager = new HeartbeatManager();

		Thread hearthBeatSenderThread = new Thread(new HeartbeatSender());
		hearthBeatSenderThread.start();

		Thread hearthBeatManagerThread = new Thread(AppConfig.heartbeatManager);
		hearthBeatManagerThread.start();
	}

	private static void deleteDirectory(File file) {
		// store all the paths of files and folders present
		// inside directory
		for (File subfile : file.listFiles()) {

			// if it is a subfolder,e.g Rohan and Ritik,
			//  recursively call function to empty subfolder
			if (subfile.isDirectory()) {
				deleteDirectory(subfile);
			}

			// delete files and empty subfolders
			subfile.delete();
		}
	}
	
}
