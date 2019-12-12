package levlab.bots.one;
import lejos.nxt.*;
import lejos.robotics.Color;
import lejos.robotics.navigation.Move;
import levlab.bots.eight.Bot8shared;

/* Bot1monitor reads the sensors and places the data into the Bot1shared locations.
 * It also checks the bluetooth connection and the motor status.
 * It will set the output of the lamp to the most recently desired color.
 * There will be a delay "MONITOR_SLEEP" between each pass thru the code.
 * 
 */

public class Bot1monitor extends Thread {

	static final int MONITOR_SLEEP = 20;	// milliseconds 

	Bot1shared local = Bot1shared.getInstance();

	int old_floodLight = 999;
	int old_motorAstate = 999;
	int old_motorBstate = 999;
	Move moveCommand = null;
	Move moveScaled = null;

	// updated for use with pilot methods 150+
	int fwdSpeed = 0;
	int[]fwdSpeedArray = { -500, -400, -300, -200, -100, 0, 100, 200, 300, 400, 500 };

	int turnSpeed = 0;
	int[] turnSpeedArray = { 150, 120, 90, 60, 30, 0, -30, -60, -90, -120, -150 };


	public Bot1monitor(){
		//
	}
	private void updateSpeed(){

		// Use index and max speed available to set fwdSpeed and turnSpeed, note that arc moves use 
		// radius rather than turn speed
		fwdSpeed = fwdSpeedArray [ local.fwdSpeedIndex ];
		turnSpeed = turnSpeedArray [ local.turnSpeedIndex ];
		
		// control power to rear wheels
		if(fwdSpeed==0) Motor.A.flt(true);
		if(fwdSpeed>0) {
			Motor.A.setSpeed( (int)(fwdSpeed));
			Motor.A.backward();
		}
		if(fwdSpeed<0) {
			Motor.A.setSpeed( (int)(-fwdSpeed));
			Motor.A.forward();
		}

		// control the steering
		Motor.B.rotateTo(turnSpeed,true);
	}

	public void run(){


		while(true){

			// handle updated drive commands
			if(local.drive == 1){
				updateSpeed();
				local.drive = 0;
			}

			// Bluetooth signal
			if(local.btState == Bot1shared.BT_OK){
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
				local.motorAstate = Bot1shared.MOVING;
			}else{
				local.motorAstate = Bot1shared.STOPPED;        		   
			}
			if(Motor.A.isStalled()){
				local.motorAstate = Bot1shared.STALLED;
			}

			if(Motor.B.isMoving()){
				local.motorBstate = Bot1shared.MOVING;
			}else{
				local.motorBstate = Bot1shared.STOPPED;        		   
			}
			if(Motor.B.isStalled()){
				local.motorBstate = Bot1shared.STALLED;
			}

			if(Motor.C.isMoving()){
				local.motorCstate = Bot1shared.MOVING;
			}else{
				local.motorCstate = Bot1shared.STOPPED;        		   
			}
			if(Motor.C.isStalled()){
				local.motorCstate = Bot1shared.STALLED;
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
