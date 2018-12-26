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

			// Accelerometer
			local.pitch = (local.accel.getXAccel()-local.pitchOffset);  // nose up is +, nose down is -
			local.roll = -(local.accel.getYAccel()+local.rollOffset);	// right side high is +, left side high is -
			// TF since Z axis acceleration is not used don't read it.
			//local.ztilt = local.accel.getZAccel();

			// Compass
			local.bearing = local.compass.getDegrees();

			// Ultrasonic
			if(local.sonar.getMode()==UltrasonicSensor.MODE_OFF){
				local.range = -1;
			}else{
				local.range = local.sonar.getDistance();
			}

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
			local.motorApower = Motor.A.getPower();
			local.motorBpower = Motor.B.getPower();
			local.motorCpower = Motor.C.getPower();  

			// Tilt detection
			apitch = Math.abs(local.pitch);
			aroll = Math.abs(local.roll);

			/*
			if((apitch>100)||(aroll>100)){
				if((tiltState == TILT_OK) || (tiltState == TILT_CAUTION)){
					// If just entered danger then command a stop immediately
					local.pilot.setAcceleration(Bot8.ACCEL_MAX);
					local.pilot.stop();
					local.fwdSpeedIndex = 5;
					local.turnSpeedIndex = 5;
					// set state to indicate how rover entered danger
					if(local.pilot.getMovement().getDistanceTraveled()>0){
						tiltState = TILT_NO_FWD;
					}else{
						tiltState = TILT_NO_BACK;
					}
				}else{	         	    
					// Already in danger, reset acceleration, don't alter state till escape danger
					local.pilot.setAcceleration(Bot8.ACCEL_FULL);
				}
				local.floodLight = Color.RED;
			}else{
				if((apitch<40)&&(aroll<40)){
					tiltState = TILT_OK;
					local.floodLight = Color.NONE;
				}
				if((apitch>50)||(aroll>50)){
					tiltState = TILT_CAUTION;
					local.floodLight = Color.BLUE;
					if(local.mode == Bot8shared.MODE_DRIVE){
						// if driving, limit forward speed
						if(local.fwdSpeedIndex>7){
							local.fwdSpeedIndex=7;
							updateSpeed();
						}
						if(local.fwdSpeedIndex<3){
							local.fwdSpeedIndex=3;
							updateSpeed();
						}
					}
				}
			}
			old_tiltState = tiltState;
		*/

/* Disabled stall detect 12-26-2018
 * 			
 
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
*/
			
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
