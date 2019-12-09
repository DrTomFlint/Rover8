package levlab.bots.nine;

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

/**
 * This is the old Bot4 code, renamed to work with the eclipse project,package,etc.
 * Rework for Bot4 "crazy wheels" started 8 Dec 2010
 * Rework for Bot5 "scanbot" with treads 28 Feb 2011
 * Rework for Bot6 "rover" with 4 treads and suspension, tilt cam, 25 April 2011
 * Update for Bot8 "rover redux, actually getting the tilt cam now, 7 April 2013
 * Update to Lejos 0.9.1beta-3 done 22 Jan 2014
 * Copied from the Bot8 code to jumpstart Bot9, 1 Feb 2014
 * */

public class Bot9
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

	// This is the "constructor" for the class TomsBot9
	public Bot9()
	{	   
		local = Bot9shared.getInstance();

		//local.compass = new CompassHTSensor(SensorPort.S4);
		//local.lamp = new ColorSensor(SensorPort.S1);
		//local.sonar = new UltrasonicSensor(SensorPort.S3);
		//local.accel = new AccelHTSensor(SensorPort.S4);

		double diam = 0.4;		// inches, set experimentally
		double trackwidth = 13.0;	// also in inches, set experimentally not by measurement
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
		Bot9 bot = new Bot9();      

		// setup thread for reading sensors
		Bot9monitor monitor = new Bot9monitor();
		monitor.start();

		// setup thread to update LCD
		Bot9lcd lcd = new Bot9lcd();
		lcd.start();

		// setup thread to watch the Bluetooth Comms
		Bot9comms comms = new Bot9comms();
		comms.start();

		// setup thread for reporting status to the servlet
		Bot9reporter reporter = new Bot9reporter();
		reporter.start();

		// Setup the drive motors for stall detection
		Motor.A.setStallThreshold(10,500);
		Motor.B.setStallThreshold(10,500);

		// Setup motor.c which is the claw
		Motor.C.setStallThreshold(100, 100);
		Motor.C.setAcceleration(ACCEL_LOW);

		// Startup the main command processing thread
		bot.go();

	}
	
	/*
	private void clawSetZero(){
		NXTMotor Claw = new NXTMotor(MotorPort.C);
		int pos = 999;
		Claw.resetTachoCount();
		Claw.setPower(20);
		Claw.forward();
		while(Claw.getTachoCount()!=pos){
			pos=Claw.getTachoCount();
			try{
				Thread.sleep(300);				 
			}catch(InterruptedException e){
				// do nothing
			}
		}
		Claw.flt();
		try{
			Thread.sleep(500);				 
		}catch(InterruptedException e){
			// do nothing
		}
		Claw.resetTachoCount();
		local.claw = Bot9shared.CLAW_ZERO;
		local.clawMax = -150;
		Claw.flt();

	}
	*/

	/**
	 * decode incoming messages
	 */
	private void readData()
	{
		String msg = "Startup";

		if(local.btState != Bot9shared.BT_OK){
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
				local.btState = Bot9shared.BT_ERROR;
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

/*				
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
*/
				
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

				
			case 600: // Stop
				Motor.C.stop(true);
				msg = "Cam Stop";
				break;
				
			case 601: // Up
				Motor.C.setAcceleration(6000);
				Motor.C.setSpeed(10);
				Motor.C.backward();
				msg = "Cam Up";
				break;
				
			case 602: // Down
				Motor.C.setAcceleration(6000);
				Motor.C.setSpeed(10);
				Motor.C.forward();
				msg = "Cam Down";
				break;

			case 603: // Stow
				msg = "Cam Stow";
				Motor.C.resetTachoCount();
				Motor.C.flt(true);
				break;

				// default to handle unknown commands
			default:
				Sound.buzz();
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

	// Classes used by Bot9
	Bot9shared local;
	Bot9lcd lcd;
	Bot9reporter reporter;
	Bot9monitor monitor;

	// Internal working vars
	int seqnum = 0;
	int command = -1;
	int data = -1;
	int data1 = 0;
	int data2 = 0;
	int data3 = 0;
	int tempInt = 0;

}
