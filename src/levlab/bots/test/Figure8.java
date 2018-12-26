package levlab.bots.test;
import lejos.nxt.*;
import lejos.robotics.navigation.*;
import lejos.nxt.addon.CompassHTSensor; 
import lejos.nxt.addon.GyroSensor; 
import lejos.nxt.addon.AccelHTSensor; 
import lejos.robotics.Color;

/* FigureEight is a top level class for operating a type-8 chassis with differential treads.
 * The behavior should be to drive in a figure eight, forward and looping to the right,
 * returning to the zero position, then forward and looping left to return again to the
 * zero position.  Green led for driving, Red when done.  Push Enter to repeat. No collision
 * or stall detection yet. End facing original direction
 * 
 * Tried this using navigator and makes a jumpy bunch of small moves.  Pauses at waypoints.
 * re-do using just the pilot and an arc-move.
 */

public class Figure8{
	
	static float RADIUS = 30;

	static int SPEED_FULL = 10;
	static int SPEED_LOW = 5;
	static int	ACCEL_FULL = 900;
	static int ACCEL_LOW	= 200;
	static int XOFFSET = 26;
	static int YOFFSET = -8;
	
	public static void main(String [] args) {
		double diam = 3.32;		// in centimeters
		double trackwidth = 33.25;	// also in cm, but set experimentally not by measurement

		ColorSensor lamp = new ColorSensor(SensorPort.S3);

		Motor.A.setStallThreshold(10,100);
	    Motor.B.setStallThreshold(10,100);
		
//	    DifferentialPilot pilot = new DifferentialPilot(diam, trackwidth, Motor.A, Motor.B, true);
// TF FIX:
// Differential Pilot calls waitComplete() in its stop method.  This will hang up the regulator thread 
// make the motors run continuously and block all other threads.  ^C is only way to recover bot.
// TomsPilot removes that call from the stop() method.	    
	    TomsPilot pilot = new TomsPilot(diam, trackwidth, Motor.A, Motor.B, true);
		pilot.setAcceleration(ACCEL_LOW);
		pilot.setTravelSpeed(SPEED_FULL);
//		pilot.setMinRadius(RADIUS);
		
//		Navigator nav = new Navigator(pilot);
		
		// Add a button listener for the ESCAPE key that will abort the 
		// program running on the Bot.  This is an in-line definition
		// for the listener.
	    Button.ESCAPE.addButtonListener( new ButtonListener() {
	        public void buttonPressed( Button button) {
	            System.exit(2);
	        }
	        public void buttonReleased( Button button) {
	        }
	    }
	    );
		
        LCD.drawString("Figure8",0,1);
        // Method using pilot only
        
    	Sound.playTone(600, 70);
	    Sound.pause(4000);

        while(true){
			lamp.setFloodlight(Color.GREEN);
	        pilot.arc(RADIUS, (float)(360));		// not return immediate
	        pilot.arc(-RADIUS, (float)(-360));		// not return immediate
			lamp.setFloodlight(Color.RED);
        }
        
        /*
        // Method using navigator
        while(true){
			lamp.setFloodlight(Color.GREEN);
			
			nav.addWaypoint(	RADIUS,		RADIUS,		(float)90);		// 1
			nav.addWaypoint(	(float)0,	2*RADIUS,	(float)180);	// 2
			nav.addWaypoint(	-RADIUS,	RADIUS,		(float)-90);	// 3
			nav.addWaypoint(	(float)0,	(float)0,	(float)0);		// 4

			nav.addWaypoint(	RADIUS,		-RADIUS,	(float)-90);	// 5
			nav.addWaypoint(	(float)0,	-2*RADIUS,	(float)180);	// 6
			nav.addWaypoint(	-RADIUS,	-RADIUS,	(float)90);		// 7
			nav.addWaypoint(	(float)0,	(float)0,	(float)0);		// 8

			nav.followPath();
			while(!nav.pathCompleted());
			lamp.setFloodlight(Color.RED);
			Button.waitForAnyPress();
        }
        */
        
        

	}		// end main	
}		// end class
