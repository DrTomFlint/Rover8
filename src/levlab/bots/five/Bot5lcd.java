package levlab.bots.five;

import lejos.nxt.*;

/* This class just reads data from the shared singleton and presents it on the LCD.
 * 
 */

public class Bot5lcd extends Thread {
       
	   public static int LCD_SLEEP = 400;
	   
       public Bot5lcd(){
           // constructor
       }
       
       public void run(){

    	   Bot5shared local = Bot5shared.getInstance();
    	   
           while(true){
        	   // clear old data
               LCD.clear();
               // Line 0 
               LCD.drawString(" Sig     Bat    ", 0, 0);
               LCD.drawInt(local.bluetoothSignal,(int)4,  4, 0);
               LCD.drawInt(local.batteryVolts,(int)4,  12, 0);

               // Line 2
               LCD.drawString("Range",0,2);
               LCD.drawInt(local.range,(int)4,6,2);
               
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
