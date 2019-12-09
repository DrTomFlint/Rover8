package levlab.bots.eight;
import lejos.nxt.*;
import lejos.robotics.Color;
import lejos.robotics.navigation.Move;

/* Bot8monitor reads the sensors and places the data into the Bot8shared locations.
 * It also checks the bluetooth connection and the motor status.
 * It will set the output of the lamp to the most recently desired color.
 * There will be a delay "MONITOR_SLEEP" between each pass thru the code.
 * 
 */

public class Bot8monitor extends Thread {

	static final int MONITOR_SLEEP = 20;	// milliseconds 
	static final int TILT_OK = 0;	 
	static final int TILT_CAUTION = 1;	 
	static final int TILT_NO_FWD = 2;	 
	static final int TILT_NO_BACK = 3;	 

	Bot8shared local = Bot8shared.getInstance();

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

	public Bot8monitor(){
		// constructor
	}

	private void updateSpeed(){
		// Check tilt state to ensure not going to tip over
		if(tiltState == TILT_NO_FWD){
			if(local.fwdSpeedIndex>5) local.fwdSpeedIndex = 5;
		}
		if(tiltState == TILT_NO_BACK){
			if(local.fwdSpeedIndex<5) local.fwdSpeedIndex = 5;
		}

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
			if(local.btState == Bot8shared.BT_OK){
				local.bluetoothSignal = local.connect.getSignalStrength();
			}else{
				local.bluetoothSignal = 0;
			}

			// Battery
			local.batteryVolts = Battery.getVoltageMilliVolt();

			// Compass
			local.bearing = local.compass.getDegrees();

			// Motor positions
			local.motorApos = Motor.A.getTachoCount();
			local.motorBpos = Motor.B.getTachoCount();
			local.motorCpos = Motor.C.getTachoCount();

			// Check the motor states
			if(Motor.A.isMoving()){
				local.motorAstate = Bot8shared.MOVING;
			}else{
				local.motorAstate = Bot8shared.STOPPED;        		   
			}
			if(Motor.A.isStalled()){
				local.motorAstate = Bot8shared.STALLED;
			}

			if(Motor.B.isMoving()){
				local.motorBstate = Bot8shared.MOVING;
			}else{
				local.motorBstate = Bot8shared.STOPPED;        		   
			}
			if(Motor.B.isStalled()){
				local.motorBstate = Bot8shared.STALLED;
			}

			if(Motor.C.isMoving()){
				local.motorCstate = Bot8shared.MOVING;
			}else{
				local.motorCstate = Bot8shared.STOPPED;        		   
			}
			if(Motor.C.isStalled()){
				local.motorCstate = Bot8shared.STALLED;
				Motor.C.flt();	// this is cam motor, so stop when stalled
			}
			
			// TEST 12-18-2018
			// If motorC is stopped and the claw is in the full up position
			// then float motor C
			if(local.motorCstate == Bot8shared.STOPPED) {
				if(local.motorCpos<-145) {
					Motor.C.flt(true);
				}
			}
			
			// This requires a re-built version of the NXTRegulatedMotor that
			// includes a getPower method
			local.motorApower = (int)Motor.A.getPower();
			local.motorBpower = (int)Motor.B.getPower();
			local.motorCpower = (int)Motor.C.getPower();  

			// Lamp, note this is an output!
			// could also read ambient light.
			if (local.floodLight!=old_floodLight){
				local.lamp.setFloodlight(local.floodLight);
				old_floodLight = local.floodLight;
			}

			// delay before checking again
			try{
				Thread.sleep(MONITOR_SLEEP);
			}catch(InterruptedException e){
			}

		}	// end while(true)
	}	// end run()
}
