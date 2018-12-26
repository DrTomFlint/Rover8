package levlab.bots.five;

import lejos.nxt.Sound;
import lejos.nxt.comm.Bluetooth;

/* This class runs a bluetooth server waiting for a connection from the PC host
 * If any other class marks the shared.btState as an error, this class will try
 * to get a new connection.
 */
public class Bot5comms extends Thread {
	   
	   static final int COMMS_SLEEP = 3000; // mSec
    
	   public Bot5comms(){
    }
    
    public void run(){
    	// get the local storage singleton
    	Bot5shared local = Bot5shared.getInstance();

 	   while(true){
 		   
 		   // BT_NOT
 		   if(local.btState == Bot5shared.BT_NOT){
 			  local.connect = null;
 			  local.dataIn = null;
 			  local.dataOut = null;
 		      local.connect = Bluetooth.waitForConnection(); // waits forever?
 		      if(local.connect!=null){
 		    	  local.dataIn = local.connect.openDataInputStream();
 		    	  if(local.dataIn!=null){
 		    		  local.dataOut = local.connect.openDataOutputStream();
 		    		  if(local.dataOut!=null){
 		    			  local.btState = Bot5shared.BT_OK;
 		    		      // Two beeps for a new bluetooth connection
	 		 			    Sound.playTone(600, 70);
	 		 			    Sound.pause(90);
	 		 			    Sound.playTone(600, 70);
	 		 			    Sound.pause(90);
 		    		  }
 		    	  }
 		      }
 		   }
 		   
 		   // BT_OK
 		   if(local.btState == Bot5shared.BT_OK){

 		   }
 		   
 		   // BT_ERROR
 		   if(local.btState == Bot5shared.BT_ERROR){
 			   local.dataIn = null;
 			   local.dataIn = null;
 			   local.connect = null; 	
 			   local.btState = Bot5shared.BT_NOT;
  		      // Long low beep for a bluetooth error
	 			    Sound.playTone(300, 300);
	 			    Sound.pause(350);
 		   }
	 	   
 		   // delay before checking again
           try{
               Thread.sleep(COMMS_SLEEP);
           }catch(InterruptedException e){
           }
 		   
 	   }
    }
}

