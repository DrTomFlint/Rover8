package levlab.bots.test;
import lejos.nxt.*;
import lejos.robotics.navigation.*;
import lejos.nxt.addon.CompassHTSensor; 
import lejos.nxt.addon.GyroSensor; 
import lejos.nxt.addon.AccelHTSensor; 
import lejos.robotics.Color;

/*  This is a top level class for operating a type-8 chassis with differential treads
 * and an HT accelerometer that is used for tilt detection.  The behavior should be to 
 * go forward until the xtilt gets close to tipping the rover over or one of the treads
 * is stalled.  The rover should then back up 20 cm, turn a random amount and go forward
 * again.  Full speed is used if the rover is close to level (green lamp turned on), if
 * tilt in x or y is medium then the rover slows down (blue lamp on). 
 * 
 * TEST 12-8-18 Comment out a couple lines so the rest of project will build.
 * This removes the calls to getStallCount() which rely on an update to NXTRegulatedMotor
 * class of lejos.  Probably breaks this test code.
 * 
 */


public class Test1 {

	static int SPEED_FULL = 10;
	static int SPEED_LOW = 5;
	static int	ACCEL_FULL = 900;
	static int ACCEL_LOW	= 300;
	static int XOFFSET = 26;
	static int YOFFSET = -8;
	
//	public static void maneuver(DifferentialPilot p, int type){
		public static void maneuver(TomsPilot p, int type){
		
		p.setAcceleration(ACCEL_FULL);
    	p.stop();
    	p.setAcceleration(ACCEL_LOW);
    	for(int i=0;i<type;i++){
	    	Sound.playTone(600, 70);
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

		int xtilt;
		int axtilt;		// abs of xtilt
		int ytilt;
		int aytilt;
		long t0, t1, t2;	// time
		int stallCntA, stallCntB;
		
		ColorSensor lamp = new ColorSensor(SensorPort.S3);
		AccelHTSensor accel = new AccelHTSensor(SensorPort.S4);
	    
	    Motor.A.setStallThreshold(10,1000);
	    Motor.B.setStallThreshold(10,1000);
		
//	    DifferentialPilot pilot = new DifferentialPilot(diam, trackwidth, Motor.A, Motor.B, true);
// TF FIX:
// Differential Pilot calls waitComplete() in its stop method.  This will hang up the regulator thread 
// make the motors run continuously and block all other threads.  ^C is only way to recover bot.
// TomsPilot removes that call from the stop() method.	    

	    TomsPilot pilot = new TomsPilot(diam, trackwidth, Motor.A, Motor.B, true);
		pilot.setAcceleration(ACCEL_LOW);
		pilot.setTravelSpeed(SPEED_LOW);
		pilot.setRotateSpeed(30);
		
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
//        LCD.drawString("Tilt",0,4);
        LCD.drawString("StallCnt",0,4);

   		lamp.setFloodlight(Color.GREEN);
   	 	pilot.forward();
//   		Motor.A.forward();
//  		Motor.B.forward();
		while(true){

			// time this loop;
			t1 = t0;
			t0 = System.currentTimeMillis();
			t2 = t0 - t1;
            LCD.clear(2);
	        LCD.drawInt((int)t2,(int)4,0,2);
	        
// TEST 12-8-18
	        //stallCntA = Motor.A.getStallCount();
	        //stallCntB = Motor.B.getStallCount();
	        stallCntA = 0;
	        stallCntB = 0;
	        
	        LCD.drawInt(stallCntA, (int)6, 0, 5);
	        LCD.drawInt(stallCntB, (int)6, 0, 6);
			
            xtilt = accel.getXAccel()+XOFFSET;
            axtilt = Math.abs(xtilt);
            ytilt = accel.getYAccel()+YOFFSET;
            aytilt = Math.abs(ytilt);

//	        LCD.drawInt(xtilt,(int)4,0,5);	     
//	        LCD.drawInt(ytilt,(int)4,0,6);	     

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
		
	}		// end main	
}		// end class
