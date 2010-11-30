package isnork.g7;


import isnork.sim.GameConfig;
import isnork.sim.Player;
import isnork.sim.SeaLifePrototype;
import isnork.sim.iSnorkMessage;
import isnork.sim.GameObject.Direction;
import isnork.sim.Observation;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Set;


import org.apache.log4j.Logger;



public class BPConsultant extends Player {

	// A buffer to give you some extra time to go back to boat.
	// Why 6: allows you to make 1 useless diagonal move away from boat. 3 minutes each way.
	private static final int BOAT_TIME_BUFFER = 6;
	
	private static final Logger logger = Logger.getLogger(BPConsultant.class);
	
	private Direction direction;
	Point2D whereIAm = null;
	// having this flag prevents him from wavering between going and not going to boat
	private boolean shouldReturnToBoat = false;
	int boatTimeBufferAdjusted;
	
	Set<SeaLifePrototype> seaLifePossibilities;
	int d = -1;  // parameter "d" == half the board length
	int r = -1;  // parameter "r" == number of cells away you can see
	int n = -1;  // parameter "n" == number of snorkelers
	int penalty;
	int round = 0;
	
	private OurBoard ourBoard;
	private DangerFinder dangerFinder;
	private Point2D myPosition = null;
	private Set<Observation> whatYouSee = null;
	private TaskManager taskManager;
	
	@Override
	public String getName() {
		return "BP Consultant";
	}
	
	@Override
	public String tick(Point2D myPosition, Set<Observation> whatYouSee,
			Set<iSnorkMessage> incomingMessages,Set<Observation> playerLocations) {
		this.myPosition = myPosition;
		this.whatYouSee = whatYouSee;
		taskManager.setPlayerLocations(playerLocations);
		
		String snorkMessage = null;
		whereIAm = myPosition;
		if(n % 10 == 0)
			snorkMessage = "s";
		else
			snorkMessage = null;
		
		round++;
		return snorkMessage;
	}
	

		
	@Override
	public Direction getMove() {	
		// Head back to boat if we are running low on time (shortest time back, plus a buffer)
		if (getRemainingTime() <= NavigateToBoat.getTimeToBoat(whereIAm) + boatTimeBufferAdjusted || shouldReturnToBoat) {
			shouldReturnToBoat = true;
			// If not enough time, ignore all dangerous creatures and return to boat.
			if (getRemainingTime() < NavigateToBoat.getTimeToBoat(whereIAm) + 6) {
				direction = NavigateToBoat.getShortestDirectionToBoat(whereIAm);
			}
			// If there are at least 6 spare minutes, that lets you maneuver around dangerous creatures,
			// even going diagonally away from boat once.
			else {
				Direction preferredDirectionToBoat = NavigateToBoat.getShortestDirectionToBoat(whereIAm);
				direction = dangerFinder.findSafestDirection(myPosition, whatYouSee, preferredDirectionToBoat, true);
			}
			logger.debug("(boat) remaining: " + getRemainingTime() + " whereIAm:"+whereIAm + " (dir "+direction+")");
			return direction;
		} else {
//			logger.debug("(normal) remaining: " + getRemainingTime() + " whereIAm:"+whereIAm + " (dir "+d+")");
			//Direction d = getNewDirection();
			
			logger.trace("in getMove()");
			
			Direction d = dangerFinder.findSafestDirection(myPosition, whatYouSee, direction, false);
//			dangerFinder.printSurroundingDanger();
			
			if (d == null){
				d = getNewDirection();
			}
			
			
			
			Point2D p = new Point2D.Double(whereIAm.getX() + d.dx,
					whereIAm.getY() + d.dy);
			while (Math.abs(p.getX()) > GameConfig.d
					|| Math.abs(p.getY()) > GameConfig.d) {
				d = getNewDirection();
				p = new Point2D.Double(whereIAm.getX() + d.dx,
						whereIAm.getY() + d.dy);
			}
			return d;
		}
	}
	
	@Override
	public void newGame(Set<SeaLifePrototype> seaLifePossibilities, int penalty,
			int d, int r, int n) {
		this.seaLifePossibilities = seaLifePossibilities;
		this.penalty = penalty;
		this.d = d;
		this.r = r;
		this.n = n;
		this.round = 0;
		this.boatTimeBufferAdjusted = NavigateToBoat.getBoatTimeBufferAdjusted(BOAT_TIME_BUFFER, seaLifePossibilities, d);
		this.ourBoard = new OurBoard(d);
		this.dangerFinder = new DangerFinder(ourBoard, seaLifePossibilities, random);
		
		taskManager = new TaskManager(seaLifePossibilities, ourBoard);
	}


	private Direction getNewDirection() {
		int r = random.nextInt(100);
		if(r < 10 || direction == null)
		{
			ArrayList<Direction> directions = Direction.allBut(direction);
			direction = directions.get(random.nextInt(directions.size()));
		}
			return direction;
	}
	
	/**
	 * Get the remaining time out of 480 rounds (8 hours).
	 * The final round will return 0.
	 * GameEngine starts with round 0, and Game Over is before beginning of round 481.
	 * @return
	 */
	private int getRemainingTime() {
		return 480 - round;
	}


}