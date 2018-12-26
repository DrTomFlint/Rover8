package levlab.bots.five;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import lejos.nxt.ColorSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.AccelHTSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.nxt.addon.GyroSensor;
import lejos.nxt.comm.BTConnection;
import lejos.robotics.Color;
import levlab.bots.test.TomsPilot;

public class Bot5shared {
	
		public static final int MOVING = 1;
		public static final int STOPPED = 2;
		public static final int STALLED = 3;
		
		public static final int BT_NOT = 0;
		public static final int BT_OK = 1;
		public static final int BT_ERROR = 2;

		public static final int MODE_NONE = 0;
		public static final int MODE_INIT = 1;
		public static final int MODE_REMOTE = 2;
		public static final int MODE_DRIVE = 3;

		// Protect constructor so no other class can call it
		private Bot5shared() {
		}
		
		// Create only 1 instance, save to a private static var
		private static Bot5shared instance = new Bot5shared();
		
		// Make the static instance available publicly thru method
		public static Bot5shared getInstance() { return instance; }
		
		// Bluetooth connection
		BTConnection connect;
		DataInputStream dataIn = null;
		DataOutputStream dataOut = null;
		int btState = BT_NOT;
		int lastCommand = 0;
		int lastData = 0;
		int mode = MODE_NONE;
		int fwdSpeedIndex = 5;
		int turnSpeedIndex = 5;
		int drive = 0;
		
		String msg = "Startup";
		
		// Handles for the 4 sensors, (ports defined in Bot5.java)
		ColorSensor lamp;
	    UltrasonicSensor sonar;
	    TomsPilot pilot;

		// Sensor outputs
		int range = 0;
				
		// Status
		int floodLight = Color.NONE;
		int batteryVolts = 0;
		int bluetoothSignal = 0;
		
		int motorApos = 0;
		int motorBpos = 0;
		int motorCpos = 0;
		
		int motorAstate = 0;
		int motorBstate = 0;
		int motorCstate = 0;

		int motorApower = 0;
		int motorBpower = 0;
		int motorCpower = 0;
				
		// Settings based on the current state and status
		int motorSpdMax = 0;

		
		
	}

