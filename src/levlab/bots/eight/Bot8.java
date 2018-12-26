package levlab.bots.eight;

import java.io.IOException;

import lejos.nxt.*;
import lejos.nxt.addon.CompassHTSensor; 
import lejos.nxt.addon.AccelHTSensor; 
import java.lang.System;
import lejos.robotics.Color;
import levlab.bots.eight.Bot8shared;
import levlab.bots.test.TomsPilot;


/**
 * This is the old Bot4 code, renamed to work with the eclipse project,package,etc.
 * Rework for Bot4 "crazy wheels" started 8 Dec 2010
 * Rework for Bot5 "scanbot" with treads 28 Feb 2011
 * Rework for Bot6 "rover" with 4 treads and suspension, tilt cam, 25 April 2011
 * Update for Bot8 "rover redux, actually getting the tilt cam now, 7 April 2013
 * Update to Lejos 0.9.1beta-3 done 22 Jan 2014
 * Switch to using a pilot instead of direct drive of motors, 2 Mar 2014
 * 
 * */

public class Bot8
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

	// This is the "constructor" for the class TomsBot8
	public Bot8()
	{	   
		local = Bot8shared.getInstance();

		local.compass = new CompassHTSensor(SensorPort.S4);
		local.lamp = new ColorSensor(SensorPort.S1);
		local.sonar = new UltrasonicSensor(SensorPort.S2);
		local.accel = new AccelHTSensor(SensorPort.S3);

		// add a differential pilot
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
		Bot8 bot = new Bot8();      

		// setup thread for reading sensors
		Bot8monitor monitor = new Bot8monitor();
		monitor.start();

		// setup thread to update LCD
		Bot8lcd lcd = new Bot8lcd();
		lcd.start();

		// setup thread to watch the Bluetooth Comms
		Bot8comms comms = new Bot8comms();
		comms.start();

		// setup thread for reporting status to the servlet
		Bot8reporter reporter = new Bot8reporter();
		reporter.start();

		// Setup the drive motors for stall detection
		Motor.A.setStallThreshold(10,500);
		Motor.B.setStallThreshold(10,500);

		// Setup motor.c which is the claw
		Motor.C.setStallThreshold(100, 100);
		Motor.C.setAcceleration(ACCEL_LOW);

		// Start with ultrasonic turned off
		bot.local.sonar.setMode(UltrasonicSensor.MODE_OFF);

		// Startup the main command processing thread
		bot.go();

	}
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
		local.claw = Bot8shared.CLAW_ZERO;
		local.clawMax = -150;
		Claw.flt();

	}


	/**
	 * decode incoming messages
	 */
	private void readData()
	{
		String msg = "Startup";
		int i = 0;

		if(local.btState != Bot8shared.BT_OK){
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
				local.btState = Bot8shared.BT_ERROR;
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

			case 5:	// initialize the accelerometer offset
				int ptemp = 0;
				int rtemp = 0;
				// zero out offsets
				local.pitchOffset = 0;
				local.rollOffset = 0;
				// delay long enough to ensure next read is done
				// with zero offsets, read is in monitor thread
				try{
					Thread.sleep(100);
				}catch(InterruptedException e){
				}
				for(i=0;i<50;i++){
					ptemp += local.pitch;
					rtemp += local.roll;
					// delay before checking again
					try{
						Thread.sleep(50);
					}catch(InterruptedException e){
					}

				}
				local.pitchOffset = ptemp/50;
				local.rollOffset = rtemp/50;
				msg = "Init";
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

				// 400 is the manipulator group
			case 401:	// Start Init
				// For the moment this will not be interruptable
				clawSetZero();
				break;
			case 402:	// Halt Init
				break;
			case 403:	// Claw close
				if(local.claw>0){;
				Motor.C.setAcceleration(200);
				Motor.C.setSpeed(200);
				Motor.C.rotateTo(local.clawMax,true);
				}
				break;
			case 404: // Claw open
				if(local.claw>0){
					Motor.C.setAcceleration(200);
					Motor.C.setSpeed(200);
					Motor.C.rotateTo(0,true);
				}
				break;
			case 405:  // Claw stop
				Motor.C.stop(true);
				Motor.C.waitComplete();
				Thread.yield();
				Motor.C.flt();
				break;
			case 406:	// Claw up
				if(local.claw>0){
					Motor.C.setAcceleration(200);
					Motor.C.setSpeed(200);
					if( (Motor.C.getTachoCount()-10) > local.clawMax){
						Motor.C.rotate(-10,true);
					}else{
						Motor.C.rotateTo(local.clawMax,true);
					}
				}
				break;
			case 407: // Claw down
				if(local.claw>0){
					Motor.C.setAcceleration(200);
					Motor.C.setSpeed(200);
					if( (Motor.C.getTachoCount()+10) < 0){
						Motor.C.rotate(10,true);
					}else{
						Motor.C.rotateTo(0,true);
					}
				}
				break;

			case 510: // color lamp 
				local.floodLight = data1;
				msg ="Lamp Color "+data1;
				break;
			case 512:	// Lamp off
				local.floodLight = Color.NONE;
				msg = "Lamp Off";
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

	// Classes used by Bot8
	Bot8shared local;
	Bot8lcd lcd;
	Bot8reporter reporter;
	Bot8monitor monitor;

	// Internal working vars
	int seqnum = 0;
	int command = -1;
	int data = -1;
	int data1 = 0;
	int data2 = 0;
	int data3 = 0;
	int tempInt = 0;

}
