package levlab.bots.test;
import lejos.nxt.*;
import lejos.robotics.navigation.*;

public class Test1Listener implements MoveListener {
   
   public void moveStarted(Move move, MoveProvider mp) {}

   public void moveStopped(Move move, MoveProvider mp) {
	   
	   int dist;
	   
	   dist =  (int)move.getDistanceTraveled();
	   LCD.drawInt(dist,(int)4,0,7);	     
   }
}
