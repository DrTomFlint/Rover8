package levlab.bots.five;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.nxt.*;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.Sound.*;
import lejos.nxt.addon.CompassHTSensor; 
import lejos.nxt.addon.GyroSensor; 
import lejos.nxt.addon.AccelHTSensor; 
import java.util.Date;
import java.lang.System;
import lejos.robotics.Color;
import levlab.bots.eight.Bot8shared;
import levlab.bots.test.TomsPilot;

import java.io.*;

/**  Bot5, Scanbot, redux for Lejos 0.9.1 and new bot configurations.
 * 
 *  Dr Tom Flint, Leverett Laboratory, 2 Feb 2014.
 * 
 * */

public class Bot5
{
	// Variables visible to anything in the class
	public static int ACCEL_MAX = 40;
	public static int ACCEL_FULL = 10;
	public static int ACCEL_LOW = 3;
	public static int TRAVEL_FULL = 10;
	public static int TRAVEL_LOW = 5;
	public static int TRAVEL_MIN = 1;
	public static int TURN_FULL = 20;
	public static int TURN_LOW = 5;
	public static int TURN_MIN = 1;

	public static int BT_ERROR_SLEEP = 4000;

	// This is the "constructor" for the class TomsBot5
	public Bot5()
	{	   
		local = Bot5shared.getInstance();

		// sensors
		local.sonar = new UltrasonicSensor(SensorPort.S2);
		local.lamp = new ColorSensor(SensorPort.S1);

		// add a differential pilot
		double diam = 1.25;		// inches, set experimentally
		double trackwidth = 7.0;	// also in inches, set experimentally not by measurement
		// arguments: (diam, trackwidth, left motor, right motor, reversed)
		local.pilot = new TomsPilot(diam, trackwidth, Motor.B, Motor.A, false);
		local.pilot.setAcceleration(ACCEL_FULL);
		local.pilot.setTravelSpeed(TRAVEL_FULL);
		local.pilot.setRotateSpeed(TURN_FULL);
	}

	// Here is the main method, invoked at startup of the bot.
	public static void main(String[] args)
	{      

		// setup local and the sensor handles
		Bot5 bot = new Bot5();      

		// setup thread for reading sensors
		Bot5monitor monitor = new Bot5monitor();
		monitor.start();

		// setup thread to update LCD
		Bot5lcd lcd = new Bot5lcd();
		lcd.start();

		// setup thread to watch the Bluetooth Comms
		Bot5comms comms = new Bot5comms();
		comms.start();

		// setup thread for reporting status to the servlet
		Bot5reporter reporter = new Bot5reporter();
		reporter.start();

		// Setup the drive motors for stall detection
		Motor.A.setStallThreshold(10,500);
		Motor.B.setStallThreshold(10,500);

		// Start with ultrasonic turned off
		bot.local.sonar.setMode(UltrasonicSensor.MODE_OFF);

		// Startup the main command processing thread
		bot.go();

	}

	/**
	 * decode incoming messages
	 */
	private void readData()
	{
		String msg = "Startup";

		if(local.btState != Bot5shared.BT_OK){
			// delay before checking again
			try{
				Thread.sleep(BT_ERROR_SLEEP);
			}catch(InterruptedException e){
			}
		}else{
			// Bluetooth is Ok so try to read commands
			try
			{
				// Command and data are integers
				command = local.dataIn.readInt();
				data1 = local.dataIn.readInt();
				data2 = local.dataIn.readInt();
				data3 = local.dataIn.readInt();

			} catch (IOException e)
			{
				// Indicate a bluetooth error, comms thread shoud handle it.
				command = -1;
				local.btState = Bot5shared.BT_ERROR;
			}

			// switch on the command received from the controller
			switch(command){

			case 0:	// halt all
				local.pilot.setAcceleration(ACCEL_MAX);
				local.pilot.stop();
				local.fwdSpeedIndex = 5;
				local.turnSpeedIndex = 5;
				local.pilot.setAcceleration(ACCEL_FULL);
				msg = "Halt All";
				break;

			case 1:	// status only
				msg = "Status";
				break;

			case 7:	// sonar commands
				if(data1==0){
					local.sonar.setMode(UltrasonicSensor.MODE_OFF);
					msg = "Sonar Off";
				}
				if(data1==1){
					local.sonar.setMode(UltrasonicSensor.MODE_PING);	     			 
					msg = "Sonar Ping";
				}
				if(data1==2){
					local.sonar.setMode(UltrasonicSensor.MODE_CONTINUOUS);	     			 
					msg = "Sonar On";
				}
				break;

			case 111:	// move linear arg is distance in 1/100th inch
				local.mode = Bot8shared.MODE_REMOTE;
				local.pilot.setAcceleration(ACCEL_FULL);
				local.pilot.setTravelSpeed(TRAVEL_FULL);
				local.pilot.setRotateSpeed(TURN_FULL);
				local.pilot.travel((double)data1/100,true);
				msg = "Move "+data1;
				break;

			case 121:  // turn linear arg is angle in degrees, left is +
				local.mode = Bot8shared.MODE_REMOTE;
				local.pilot.setAcceleration(ACCEL_FULL);
				local.pilot.setTravelSpeed(TRAVEL_FULL);
				local.pilot.setRotateSpeed(TURN_FULL);
				local.pilot.rotate((double)data1,true);
				msg = "Turn "+data1;
				break;

			case 130:  // Arc, data1 radius in 1/100 inch, data2 is angle in deg
				local.mode = Bot8shared.MODE_REMOTE;
				local.pilot.setAcceleration(ACCEL_MAX);
				local.pilot.setTravelSpeed(TRAVEL_LOW);
				local.pilot.setRotateSpeed(TURN_FULL);
				local.pilot.arc((double)data1/100, (double)data2, true);
				break;

				// Drive 2 Menus:	 
			case 151: // Fwd
				local.mode = Bot8shared.MODE_DRIVE;
				if(local.fwdSpeedIndex<10)local.fwdSpeedIndex++;
				local.drive = 1;
				msg = "D2 "+local.fwdSpeedIndex+", "+local.turnSpeedIndex;
				break;
			case 152: // Back
				local.mode = Bot8shared.MODE_DRIVE;
				if(local.fwdSpeedIndex>0)local.fwdSpeedIndex--;
				local.drive = 1;
				msg = "D2 "+local.fwdSpeedIndex+", "+local.turnSpeedIndex;
				break;
			case 153: // Left
				local.mode = Bot8shared.MODE_DRIVE;
				if(local.turnSpeedIndex>0)local.turnSpeedIndex--;
				local.drive = 1;
				msg = "D2 "+local.fwdSpeedIndex+", "+local.turnSpeedIndex;
				break;
			case 154: // Right
				local.mode = Bot8shared.MODE_DRIVE;
				if(local.turnSpeedIndex<10)local.turnSpeedIndex++;
				local.drive = 1;
				msg = "D2 "+local.fwdSpeedIndex+", "+local.turnSpeedIndex;
				break;
			case 155: // Center
				local.mode = Bot8shared.MODE_DRIVE;
				local.turnSpeedIndex=5;
				local.drive = 1;
				msg = "D2 "+local.fwdSpeedIndex+", "+local.turnSpeedIndex;
				break;

				// command group 200 for the scancam:
			case 200:	// Stop  
				Motor.C.flt();
				msg = "Pan Stop";
				break;
			case 201:	// Re-zero 
				Motor.C.resetTachoCount();
				msg = "ReZero Cam";
				break;
			case 202:	// Pan the cam, data is speed
				if(data1<0){
					Motor.C.setSpeed(-data1);
					Motor.C.backward();
					msg = "Pan Left";
				}else{
					Motor.C.setSpeed(data1);
					Motor.C.forward();
					msg = "Pan Right";
				}
				break;
			case 210:	// Turn To, data1 is PositionC
				// This command is used by AutoScan1 servlet
				Motor.C.setAcceleration(1000);
				Motor.C.setSpeed(700);
				Motor.C.rotateTo(data1,true);
				msg = "Turn To "+data1;
				break;
			case 211:	// Turn By, data1 is delta in PositionC
				Motor.C.setSpeed(700);
				Motor.C.rotate(data1,true);
				msg = "Turn By "+data1;
				break;		         

			case 511:	// Lamp on
				local.floodLight = Color.BLUE;
				msg = "Lamp Blue";
				break;
			case 512:	// Lamp off
				local.floodLight = Color.NONE;
				msg = "Lamp Off";
				break;
			case 513:	// Lamp flash
				local.floodLight = Color.RED;
				try{
					Thread.sleep(data1);
				}catch(InterruptedException e1){
					// do nothing 
				}
				local.floodLight = Color.NONE;
				msg = "Lamp Flash";
				break;		  		 		  		 
			case 514:	// Lamp Toggle
				if(local.floodLight!=Color.GREEN){
					local.floodLight=Color.GREEN;
				}else{
					local.floodLight = Color.NONE;
				}
				break;

				// default to handle unknown commands
			default:
				msg = "Cmd "+command+" ??";
				break;		
			}

			// pass the msg line to LCD thread
			local.msg=msg;

			// All cases 
			if(command!=1){									// "status" doesn't count as a command
				local.lastCommand = command;      				// record this command
				local.lastData = data1; 
			}
		}	         
	}
	private void go()
	{

		// Add a button listener for the ESCAPE key that will abort the 
		// program running on the Bot.
		Button.ESCAPE.addButtonListener( new ButtonListener() {
			public void buttonPressed( Button button) {

				// One sad beep when system shuts down
				Sound.playTone(100, 300);
				Sound.pause(350);

				// Cleanup before exiting
				try {
					local.dataIn.close();
					local.dataOut.close();
				} catch (IOException e) {
					// not much to do if close fails
					e.printStackTrace();
				}
				local.connect.close();
				System.exit(2);
			}
			public void buttonReleased( Button button) {
			}
		}
				);

		// One beep when system is fully up and running
		Sound.playTone(600, 70);
		Sound.pause(90);

		while (true){
			readData();
		}
	}

	// Classes used by Bot5
	Bot5shared local;
	Bot5lcd lcd;
	Bot5reporter reporter;
	Bot5monitor monitor;

	// Internal working vars
	int seqnum = 0;
	int command = -1;
	int data1 = 0;
	int data2 = 0;
	int data3 = 0;
	int tempInt = 0;


}
