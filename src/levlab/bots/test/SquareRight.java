package levlab.bots.test;
import lejos.nxt.*;
import lejos.robotics.navigation.*;
import lejos.nxt.addon.CompassHTSensor; 
import lejos.nxt.addon.GyroSensor; 
import lejos.nxt.addon.AccelHTSensor; 
import lejos.robotics.Color;

/* SquareRight is a top level class for operating a type-8 chassis with differential treads.
 * The behavior should be to drive in a square box 50 cm on a side and pause to beep 
 * at each of the 4 corners.  Green led for driving, blue at waypoints.  No collision
 * or stall detection yet.  Forward, Right, Back, Left.  End facing original direction
 */


public class SquareRight {
	
	static int WIDTH = 50;

	static int SPEED_FULL = 10;
	static int SPEED_LOW = 5;
	static int	ACCEL_FULL = 900;
	static int ACCEL_LOW	= 300;
	static int XOFFSET = 26;
	static int YOFFSET = -8;
	
		public static void beep(int times){
			int i;
			for(i=0;i<times;i++){
		    	Sound.playTone(600, 70);
			    Sound.pause(110);
	    	}    
	    	for(i=times;i<4;i++){
	    		Sound.pause(90);
	    	}
			
		}
		
//	public static void maneuver(DifferentialPilot p, int type){
		public static void maneuver(TomsPilot p, int type){
		int i;
		
		p.setAcceleration(ACCEL_FULL);
    	p.stop();
    	p.setAcceleration(ACCEL_LOW);
    	for(i=0;i<type;i++){
	    	Sound.playTone(600, 70);
		    Sound.pause(90);
    	}    
    	for(i=type;i<4;i++){
    		Sound.pause(90);
    	}
		p.setTravelSpeed(SPEED_FULL);
    	p.travel(-20);
		p.rotate(Math.random()*360.0-180.0);
		p.forward();

	}

	public static void main(String [] args) {
		double diam = 3.32;		// in centimeters
		double trackwidth = 33.25;	// also in cm, but set experimentally not by measurement

		float heading;
		int xtilt;
		int axtilt;		// abs of xtilt
		int ytilt;
		int aytilt;
		int ztilt;
		int rawGyro;
		long t0, t1, t2;	// time
		
		ColorSensor lamp = new ColorSensor(SensorPort.S3);
	    CompassHTSensor compass = new CompassHTSensor(SensorPort.S1);
		AccelHTSensor accel = new AccelHTSensor(SensorPort.S4);
//	    GyroSensor gyro = new GyroSensor(SensorPort.S2);
	    
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
		pilot.setRotateSpeed(30);
		
//		Test1Listener listen = new Test1Listener();
//		pilot.addMoveListener(listen);

		Navigator nav = new Navigator(pilot);
		
		// Add a button listener for the ESCAPE key that will abort the 
		// program running on the Bot.
	    Button.ESCAPE.addButtonListener( new ButtonListener() {
	        public void buttonPressed( Button button) {
	            System.exit(2);
	        }
	        public void buttonReleased( Button button) {
	        }
	    }
	    );
		
		t0 = System.currentTimeMillis();
        LCD.drawString("Time",0,1);
        LCD.drawString("Tilt",0,4);

        while(true){
			lamp.setFloodlight(Color.GREEN);
			nav.goTo(WIDTH,0);
			while(nav.isMoving());
			lamp.setFloodlight(Color.BLUE);
			beep((int)1);
			lamp.setFloodlight(Color.GREEN);
			nav.goTo(WIDTH,WIDTH);
			while(nav.isMoving());
			lamp.setFloodlight(Color.BLUE);
			beep((int)2);
			lamp.setFloodlight(Color.GREEN);
			nav.goTo(0,WIDTH);
			while(nav.isMoving());
			lamp.setFloodlight(Color.BLUE);
			beep((int)3);
			lamp.setFloodlight(Color.GREEN);
			nav.goTo(0,0,0);
			while(nav.isMoving());
			lamp.setFloodlight(Color.RED);
			beep((int)4);
			Button.waitForAnyPress();
        }

		
/*   		
		while(true){

			// time this loop;
			t1 = t0;
			t0 = System.currentTimeMillis();
			t2 = t0 - t1;
            LCD.clear(2);
	        LCD.drawInt((int)t2,(int)4,0,2);	     
			
            xtilt = accel.getXAccel()+XOFFSET;
            axtilt = Math.abs(xtilt);
            ytilt = accel.getYAccel()+YOFFSET;
            aytilt = Math.abs(ytilt);

	        LCD.drawInt(xtilt,(int)4,0,5);	     
	        LCD.drawInt(ytilt,(int)4,0,6);	     

	        // Tilt detection
	        if(axtilt>100){
            	// bot angle too high, back off
        		lamp.setFloodlight(Color.RED);
            	maneuver(pilot,1);
            }
        	if((axtilt<40)&&(aytilt<40)){
        		pilot.setTravelSpeed(SPEED_FULL);
        		lamp.setFloodlight(Color.GREEN);
        	}
        	if((axtilt>50)||(aytilt>50)){
        		pilot.setTravelSpeed(SPEED_LOW);
        		lamp.setFloodlight(Color.BLUE);
        	}
   	            		
            // Stall detection
            if(Motor.A.isStalled()){
        		lamp.setFloodlight(Color.RED);
            	maneuver(pilot,2);            	
          	}
            if(Motor.B.isStalled()){
        		lamp.setFloodlight(Color.RED);
            	maneuver(pilot,3);            	
          		lamp.setFloodlight(Color.GREEN);
          	}
                      
		}		// end while
		*/
	}		// end main	
}		// end class
