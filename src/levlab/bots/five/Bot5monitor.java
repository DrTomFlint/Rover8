package levlab.bots.five;
import lejos.nxt.*;
import lejos.robotics.Color;
import lejos.robotics.navigation.Move;
import levlab.bots.eight.Bot8shared;

/* Bot5monitor reads the sensors and places the data into the Bot5shared locations.
 * It also checks the bluetooth connection and the motor status.
 * It will set the output of the lamp to the most recently desired color.
 * There will be a delay "MONITOR_SLEEP" between each pass thru the code.
 * 
 */

public class Bot5monitor extends Thread {

	static final int MONITOR_SLEEP = 20;	// milliseconds 
	Bot5shared local = Bot5shared.getInstance();

	int old_floodLight = 999;
	int old_motorAstate = 999;
	int old_motorBstate = 999;
	int apitch = 0;
	int aroll = 0;
	int tiltState = 0;
	int old_tiltState = 999;
	Move moveCommand = null;
	Move moveScaled = null;

	// updated for use with pilot methods 150+
	double fwdSpeed = 0;
	double[]fwdSpeedArray = { -1.0, -0.6, -0.4, -0.2, -0.1, 0.0, 0.1, 0.2, 0.4, 0.6, 1.0 };

	double turnSpeed = 0;
	double turnRadius = 0;
	double[] turnSpeedArray = { -1.0, -0.6, -0.4, -0.2, -0.1, 0.0, 0.1, 0.2, 0.4, 0.6, 1.0 };
	double[] turnRadiusArray = { 2, 5, 10, 20, 50, 0, -50, -20, -10, -5, -2 };  // arc turns use radius, Left +, Right -

	public Bot5monitor(){
		//
	}

	private void updateSpeed(){
		// Use index and max speed available to set fwdSpeed and turnSpeed, note that arc moves use 
		// radius rather than turn speed
		fwdSpeed = local.pilot.getMaxTravelSpeed() * fwdSpeedArray [ local.fwdSpeedIndex ];
		turnSpeed = local.pilot.getMaxRotateSpeed() * turnSpeedArray [ local.turnSpeedIndex ];
		turnRadius = turnRadiusArray [ local.turnSpeedIndex ];
		
		if((local.fwdSpeedIndex==5)&&(local.turnSpeedIndex==5)){
			// Came to a stop maybe by increment/decrement
			local.pilot.stop();
		}
		if((local.fwdSpeedIndex!=5)&&(local.turnSpeedIndex==5)){
			// Moving forward or back
			if(fwdSpeed>0){
				local.pilot.setTravelSpeed(fwdSpeed);
				local.pilot.forward();
			}
			if(fwdSpeed<0){
				local.pilot.setTravelSpeed(-fwdSpeed);
				local.pilot.backward();
			}
		}
		if((local.fwdSpeedIndex==5)&&(local.turnSpeedIndex!=5)){
			// Pure turning in place
			if(turnSpeed>0){
				local.pilot.setRotateSpeed(turnSpeed);
				local.pilot.rotateRight();
			}else{
				local.pilot.setRotateSpeed(-turnSpeed);
				local.pilot.rotateLeft();
			}		 
		}
		if((local.fwdSpeedIndex!=5)&&(local.turnSpeedIndex!=5)){
			// Arc turns
			if(fwdSpeed>0){
				local.pilot.setTravelSpeed(fwdSpeed);
				local.pilot.arcForward(turnRadius);
			}else{
				local.pilot.setTravelSpeed(-fwdSpeed);
				local.pilot.arcBackward(-turnRadius);
			}
		}
	}

	public void run(){

		while(true){

			// handle updated drive commands
			if(local.drive == 1){
				updateSpeed();
				local.drive = 0;
			}

			// Bluetooth signal
			if(local.btState == Bot5shared.BT_OK){
				local.bluetoothSignal = local.connect.getSignalStrength();
			}else{
				local.bluetoothSignal = 0;
			}

			// Battery
			local.batteryVolts = Battery.getVoltageMilliVolt();

			// Ultrasonic
			if(local.sonar.getMode()==UltrasonicSensor.MODE_OFF){
				local.range = -1;
			}else{
				local.range = local.sonar.getDistance();
			}

			// Lamp, note this is an output!
			// could also read ambient light.
			if (local.floodLight!=old_floodLight){
				local.lamp.setFloodlight(local.floodLight);
				old_floodLight = local.floodLight;
			}

			// Motor positions
			local.motorApos = Motor.A.getTachoCount();
			local.motorBpos = Motor.B.getTachoCount();
			local.motorCpos = Motor.C.getTachoCount();

			// Check the motor states
			if(Motor.A.isMoving()){
				local.motorAstate = Bot5shared.MOVING;
			}else{
				local.motorAstate = Bot5shared.STOPPED;        		   
			}
			if(Motor.A.isStalled()){
				local.motorAstate = Bot5shared.STALLED;
			}

			if(Motor.B.isMoving()){
				local.motorBstate = Bot5shared.MOVING;
			}else{
				local.motorBstate = Bot5shared.STOPPED;        		   
			}
			if(Motor.B.isStalled()){
				local.motorBstate = Bot5shared.STALLED;
			}

			if(Motor.C.isMoving()){
				local.motorCstate = Bot5shared.MOVING;
			}else{
				local.motorCstate = Bot5shared.STOPPED;        		   
			}
			if(Motor.C.isStalled()){
				local.motorCstate = Bot5shared.STALLED;
				Motor.C.flt();	// this is cam motor, so stop when stalled
			}

			// This requires a re-built version of the NXTRegulatedMotor that
			// includes a getPower method
			// TEST 12-8-18 not ready to rebuild the lejos class, so comment these out:
//			local.motorApower = Motor.A.getPower();
//			local.motorBpower = Motor.B.getPower();
//			local.motorCpower = Motor.C.getPower();  
			local.motorApower = 22;
			local.motorBpower = 23;
			local.motorCpower = 24;  


			// Stall detection
			if(local.motorAstate==Bot8shared.STALLED){
				if(old_motorAstate != Bot8shared.STALLED){
					local.pilot.stop();
					Sound.playTone(3000,20);
					local.fwdSpeedIndex = 5;
					local.turnSpeedIndex = 5;
				}
			}
			old_motorAstate = local.motorAstate;

			if(local.motorBstate==Bot8shared.STALLED){
				if(old_motorBstate != Bot8shared.STALLED){
					local.pilot.stop();
					local.fwdSpeedIndex = 5;
					local.turnSpeedIndex = 5;
					Sound.playTone(2000,20);
				}
			}
			old_motorBstate = local.motorBstate;

			// delay before checking again
			try{
				Thread.sleep(MONITOR_SLEEP);
			}catch(InterruptedException e){
			}

		}	// end while(true)
	}	// end run()
}
