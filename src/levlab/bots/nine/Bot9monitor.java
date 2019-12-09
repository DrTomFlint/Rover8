package levlab.bots.nine;
import lejos.nxt.*;
import lejos.robotics.Color;
import lejos.robotics.navigation.Move;
import levlab.bots.eight.Bot8shared;

/* Bot9monitor reads the sensors and places the data into the Bot9shared locations.
 * It also checks the bluetooth connection and the motor status.
 * It will set the output of the lamp to the most recently desired color.
 * There will be a delay "MONITOR_SLEEP" between each pass thru the code.
 * 
 */

public class Bot9monitor extends Thread {

	static final int MONITOR_SLEEP = 20;	// milliseconds 

	Bot9shared local = Bot9shared.getInstance();

	int old_floodLight = 999;
	int old_motorAstate = 999;
	int old_motorBstate = 999;
	Move moveCommand = null;
	Move moveScaled = null;

	// updated for use with pilot methods 150+
	double fwdSpeed = 0;
	double[]fwdSpeedArray = { -1.0, -0.6, -0.4, -0.2, -0.1, 0.0, 0.1, 0.2, 0.4, 0.6, 1.0 };

	double turnSpeed = 0;
	double turnRadius = 0;
//	double[] turnSpeedArray = { -1.0, -0.6, -0.4, -0.2, -0.1, 0.0, 0.1, 0.2, 0.4, 0.6, 1.0 };
	double[] turnSpeedArray = { -0.5, -0.25, -0.1, -0.06, -0.03, 0.0, 0.03, 0.06, 0.1, 0.25, 0.5 };
	double[] turnRadiusArray = { 2, 5, 10, 20, 50, 0, -50, -20, -10, -5, -2 };  // arc turns use radius, Left +, Right -


	public Bot9monitor(){
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
			if(local.btState == Bot9shared.BT_OK){
				local.bluetoothSignal = local.connect.getSignalStrength();
			}else{
				local.bluetoothSignal = 0;
			}

			// Battery
			local.batteryVolts = Battery.getVoltageMilliVolt();

			// Motor positions
			local.motorApos = Motor.A.getTachoCount();
			local.motorBpos = Motor.B.getTachoCount();
			local.motorCpos = Motor.C.getTachoCount();

			// Check the motor states
			if(Motor.A.isMoving()){
				local.motorAstate = Bot9shared.MOVING;
			}else{
				local.motorAstate = Bot9shared.STOPPED;        		   
			}
			if(Motor.A.isStalled()){
				local.motorAstate = Bot9shared.STALLED;
			}

			if(Motor.B.isMoving()){
				local.motorBstate = Bot9shared.MOVING;
			}else{
				local.motorBstate = Bot9shared.STOPPED;        		   
			}
			if(Motor.B.isStalled()){
				local.motorBstate = Bot9shared.STALLED;
			}

			if(Motor.C.isMoving()){
				local.motorCstate = Bot9shared.MOVING;
			}else{
				local.motorCstate = Bot9shared.STOPPED;        		   
			}
			if(Motor.C.isStalled()){
				local.motorCstate = Bot9shared.STALLED;
				Motor.C.flt();	// this is cam motor, so stop when stalled
			}

			// This requires a re-built version of the NXTRegulatedMotor that
			// includes a getPower method
			// TEST 12-8-18 not ready to rebuild the lejos class, so comment these out:
			local.motorApower = (int) Motor.A.getPower();
			local.motorBpower = (int) Motor.B.getPower();
			local.motorCpower = (int) Motor.C.getPower();  

			// delay before checking again
			try{
				Thread.sleep(MONITOR_SLEEP);
			}catch(InterruptedException e){
			}

		}	// end while(true)
	}	// end run()
}
