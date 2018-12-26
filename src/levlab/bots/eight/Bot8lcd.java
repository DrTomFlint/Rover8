package levlab.bots.eight;

import lejos.nxt.*;

/* This class just reads data from the shared singleton and presents it on the LCD.
 * 
 */

public class Bot8lcd extends Thread {
       
	   public static int LCD_SLEEP = 400;
	   
       public Bot8lcd(){
           // constructor
       }
       
       public void run(){

    	   Bot8shared local = Bot8shared.getInstance();
    	   
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

               // Line 2
               LCD.drawString("Pitch",0,2);
               LCD.drawInt(local.pitch,(int)4,6,2);
               
               // Line 3
               LCD.drawString("Roll",0,3);
               LCD.drawInt(local.roll,(int)4,6,3);
               
               // Line 4
               LCD.drawString("Range",0,4);
               LCD.drawInt((int)(local.range), (int)4,6,4);
               
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
