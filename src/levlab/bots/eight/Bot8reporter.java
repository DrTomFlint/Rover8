package levlab.bots.eight;

import java.io.IOException;

/* This class is revised so that it just reads values from Bot8shared singleton
 * and reports them out the bluetooth connection.  At the other end, the run() 
 * method of Bot8cmd (part of the TestOne servlet) is constantly reading the 
 * data and putting it into the Shared singleton in the tomcat server.
 * 
 * Dr Tom Flint, Leverett Laboratory, 23 Jan 2014.
 * 
 */
   public class Bot8reporter extends Thread {

	   static final int REPORTER_SLEEP = 20; 
       
       public Bot8reporter(){
    	   // constructor
       }
       
       public void run(){

    	   Bot8shared local = Bot8shared.getInstance();
    	   int reportNum = 0;
           while(true){
        	   if(local.btState==Bot8shared.BT_OK){
        		   try{
	        		   // Write out data
			           local.dataOut.writeInt(reportNum++);
			           local.dataOut.writeInt(local.bluetoothSignal);
			           local.dataOut.writeInt(local.batteryVolts);
			           local.dataOut.writeInt(local.motorApos);
			           local.dataOut.writeInt(local.motorBpos);
			           local.dataOut.writeInt(local.motorCpos);
			           local.dataOut.writeInt(local.motorApower);
			           local.dataOut.writeInt(local.motorBpower);
			           local.dataOut.writeInt(local.motorCpower);
			           local.dataOut.writeInt(local.motorAstate);
			           local.dataOut.writeInt(local.motorBstate);
			           /* note that speed indices are adjusted to read +/-5, with 0 center (stop) */
			           local.dataOut.writeInt(local.fwdSpeedIndex-5);
			           local.dataOut.writeInt(local.turnSpeedIndex-5);
			           
			           local.dataOut.writeInt(local.claw);
			           local.dataOut.writeInt((int)(local.bearing));
			           local.dataOut.writeInt(local.floodLight);
			           
			           local.dataOut.writeInt(local.lastCommand);
			           local.dataOut.writeInt(local.lastData);
			           
			           			         
			           local.dataOut.flush();
			           
		              }catch(IOException e2){   
		            	   // This may occur when bluetooth link is lost
		            	   // perhaps so re-try behavior?
		              }
	              	  // small delay before next status update is sent
	                   try{
	                	   Thread.sleep(REPORTER_SLEEP);
	                   }catch(InterruptedException e1){
	                	   // catch for the tread sleep
	                   }
	              
        	   }else{
        		   // btState is not BT_OK, make a longer delay before retry
                   try{
                	   Thread.sleep(1000);
	               }catch(InterruptedException e1){
	            	   // catch for the tread sleep
	               }
        	   }
           }
       }
   }
