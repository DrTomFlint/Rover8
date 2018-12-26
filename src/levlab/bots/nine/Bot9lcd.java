package levlab.bots.nine;

import lejos.nxt.*;

/* This class just reads data from the shared singleton and presents it on the LCD.
 * 
 */

public class Bot9lcd extends Thread {
       
	   public static int LCD_SLEEP = 400;
	   
       public Bot9lcd(){
           // constructor
       }
       
       public void run(){

    	   Bot9shared local = Bot9shared.getInstance();
    	   
           while(true){
        	   // clear old data
               LCD.clear();
               // Line 0 
               LCD.drawString(" Sig     Bat    ", 0, 0);
               LCD.drawInt(local.bluetoothSignal,(int)4,  4, 0);
               LCD.drawInt(local.batteryVolts,(int)4,  12, 0);

               // Line 1
               LCD.drawString("Head",0,1);
               LCD.drawInt((int)(local.bearing), (int)4,6,1);

               // Line 5
               LCD.drawString("Cmd",0,5);
               LCD.drawInt((int)(local.lastCommand), (int)4,6,5);
               
               // Line 6
               LCD.drawString("Data",0,6);
               LCD.drawInt((int)(local.lastData), (int)4,6,6);
               
               // Line 7
               LCD.drawString(local.msg,0,7);
               LCD.refresh();

               // delay
               try{
                   Thread.sleep(LCD_SLEEP);
               }catch(InterruptedException e){
               }
           }
       }
   }
