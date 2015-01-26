package team305;
import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	static Random rand = new Random();

	public static void run(RobotController rc) {
		BaseBot myself;

		if (rc.getType() == RobotType.HQ) {
			myself = new HQ(rc);
		} else if (rc.getType() == RobotType.BEAVER) {
			myself = new Beaver(rc);
		} else if (rc.getType() == RobotType.MINERFACTORY) {
			myself = new MinerFactory(rc);
		} else if (rc.getType() == RobotType.MINER) {
			myself = new Miner(rc);
		} else if (rc.getType() == RobotType.BARRACKS) {
			myself = new Barracks(rc);
		} else if (rc.getType() == RobotType.SOLDIER) {
			myself = new Soldier(rc);
		} else if (rc.getType() == RobotType.TOWER) {
			myself = new Tower(rc);
		} else if (rc.getType() == RobotType.SUPPLYDEPOT) {
			myself = new SupplyDepot(rc);
		} else if (rc.getType() == RobotType.TANKFACTORY) {
			myself = new TankFactory(rc);
		} else if (rc.getType() == RobotType.TANK) {
			myself = new Tank(rc);
		} else if (rc.getType() == RobotType.BASHER) {
			myself = new Basher(rc);
		} else {
			myself = new BaseBot(rc);
		}

		while (true) {
			try {
				myself.go();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class BaseBot {
		protected RobotController rc;
		protected MapLocation myHQ, theirHQ, halfway;
		protected Team myTeam, theirTeam;
		protected MapLocation[] theirTowers;

		public BaseBot(RobotController rc) {
			this.rc = rc;
			this.myHQ = rc.senseHQLocation();
			this.theirTowers = rc.senseEnemyTowerLocations();
			this.myTeam = rc.getTeam();
			this.theirTeam = this.myTeam.opponent();
		}

		public Direction intToDir(int integer) {
			if (integer == 0) {
				return Direction.NORTH;
			} else if (integer == 1) {
				return Direction.NORTH_EAST;
			} else if (integer == 2) {
				return Direction.EAST;
			} else if (integer == 3) {
				return Direction.SOUTH_EAST;
			} else if (integer == 4) {
				return Direction.SOUTH;
			} else if (integer == 5) {
				return Direction.SOUTH_WEST;
			} else if (integer == 6) {
				return Direction.WEST;
			} else if (integer == 7) {
				return Direction.NORTH_WEST;
			} else {
				return null;
			}
		}

		public int dirToInt(Direction d) {
			if (d == Direction.NORTH) {
				return 0;
			} else if (d == Direction.NORTH_EAST) {
				return 1;
			} else if (d == Direction.EAST) {
				return 2;
			} else if (d == Direction.SOUTH_EAST) {
				return 3;
			} else if (d == Direction.SOUTH) {
				return 4;
			} else if (d == Direction.SOUTH_WEST) {
				return 5;
			} else if (d == Direction.WEST) {
				return 6;
			} else if (d == Direction.NORTH_WEST) {
				return 7;
			} else {
				return 0;//this case should never happen
			}
		}

		public int dirToIntInverse(Direction d) {
			if (d == Direction.NORTH) {
				return 4;
			} else if (d == Direction.NORTH_EAST) {
				return 5;
			} else if (d == Direction.EAST) {
				return 6;
			} else if (d == Direction.SOUTH_EAST) {
				return 7;
			} else if (d == Direction.SOUTH) {
				return 0;
			} else if (d == Direction.SOUTH_WEST) {
				return 1;
			} else if (d == Direction.WEST) {
				return 2;
			} else if (d == Direction.NORTH_WEST) {
				return 3;
			} else {
				return 0;//this case should never happen
			}
		}

		public Direction[] getDirectionsToward(MapLocation dest) {
			Direction toDest = rc.getLocation().directionTo(dest);
			Direction[] dirs = {toDest,
					toDest.rotateLeft(), toDest.rotateRight(),
					toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};

			return dirs;
		}

		public Direction getMoveDir(MapLocation dest) {
			Direction toDest = rc.getLocation().directionTo(dest);
			Direction[] dirs = {toDest,
					toDest.rotateLeft(), toDest.rotateRight()};
			for (Direction d : dirs) {
				if (rc.canMove(d)) {
					return d;
				}
			}
			return null;
		}

		public Direction getHarassMoveDir(MapLocation dest, MapLocation curLoc) throws GameActionException {
			Direction toDest = curLoc.directionTo(dest);
			Direction[] dirs = {toDest,
					toDest.rotateLeft(), toDest.rotateRight()};
			for (Direction d : dirs) {
				//list of towers & HQ locations
				boolean dangerZone = false;
				for (MapLocation tower : theirTowers){
					if ((curLoc.add(d)).distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) {
						dangerZone = true;
					}
				}
				if ((curLoc.add(d)).distanceSquaredTo(theirHQ) <= RobotType.HQ.attackRadiusSquared) {
					dangerZone = true;
				}
				if (rc.canMove(d) && (dangerZone == false)) {
					return d;
				}
			}
			return null;
		}

		public Direction getLeftHarassMoveDir(MapLocation dest, MapLocation curLoc) throws GameActionException {
			Direction toDest = curLoc.directionTo(dest);
			Direction[] dirs = {toDest,
					toDest.rotateLeft(), toDest.rotateLeft().rotateLeft(),
					toDest.rotateLeft().rotateLeft().rotateLeft(), toDest.rotateLeft().rotateLeft().rotateLeft().rotateLeft()};
			for (Direction d : dirs) {
				//list of towers & HQ locations
				boolean dangerZone = false;
				for (MapLocation tower : theirTowers){
					if ((curLoc.add(d)).distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) {
						dangerZone = true;
					}
				}
				if ((curLoc.add(d)).distanceSquaredTo(theirHQ) <= RobotType.HQ.attackRadiusSquared) {
					dangerZone = true;
				}
				if (rc.canMove(d) && (dangerZone == false)) {
					return d;
				}
			}
			return null;
		}

		public Direction getSpawnDirection(RobotType type) {
			Direction[] dirs = getDirectionsToward(this.theirHQ);
			for (Direction d : dirs) {
				if (rc.canSpawn(d, type)) {
					return d;
				}
			}
			return null;
		}

		public Direction getBeaverSpawnDirection() {
			Direction[] allDirs = Direction.values();
			for (Direction d : allDirs) {
				if (rc.canSpawn(d, RobotType.BEAVER)){
					return d;
				}
			}
			return null;
		}

		public Direction getBuildDirection(RobotType type) {
			Direction[] dirs = Direction.values();//getDirectionsToward(this.theirHQ);
			for (Direction d : dirs) {
//				if (rc.senseTerrainTile(rc.getLocation().add(d)) != TerrainTile.VOID){
//					return d;
//				}
				if (rc.canMove(d)) {
					return d;
				}
			}
			return null;
		}

		public Direction[] directionValues() {
			Direction[] dV = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
					Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
			return dV;
		}

		public static int randInt(int min, int max) {
		    return rand.nextInt((max - min) + 1) + min;
		}

		public Direction getRandomDirection() {
			int firstTry = randInt(0,7);
			for (int i = 0; i < 8; i++) {
				Direction d = directionValues()[(i+firstTry)%8];
				if (rc.canMove(d)){
					return d;
				}
			}
			return null;
		}

		public RobotInfo[] getAllies() {
			RobotInfo[] allies = rc.senseNearbyRobots(Integer.MAX_VALUE, myTeam);
			return allies;
		}

		public RobotInfo[] getEnemiesInAttackingRange(RobotType type) {
			RobotInfo[] enemies = rc.senseNearbyRobots(type.attackRadiusSquared, theirTeam);
			return enemies;
		}

		public void attackLeastHealthEnemy(RobotInfo[] enemies) throws GameActionException {
			if (enemies.length == 0) {
				return;
			}

			double minEnergon = Double.MAX_VALUE;
			MapLocation toAttack = null;
			for (RobotInfo info : enemies) {
				if (info.health < minEnergon) {
					toAttack = info.location;
					minEnergon = info.health;
				}
				if (info.type == RobotType.TOWER || info.type == RobotType.HQ){
					toAttack = info.location;
					break;	// If one of the targets is a tower or HQ, attack this
				}
			}
			if (rc.canAttackLocation(toAttack)){
				rc.attackLocation(toAttack);
			}
		}

		public RobotInfo leastHealthEnemy(RobotInfo[] enemies) throws GameActionException {
			if (enemies.length == 0) {
				return null;
			}
			double minEnergon = Double.MAX_VALUE;
			RobotInfo weakestRobot = null;
			for (RobotInfo info : enemies) {
				if (info.health < minEnergon) {
					weakestRobot = info;
					minEnergon = info.health;
				}
			}
			return weakestRobot;
		}

		public RobotInfo closestEnemy(RobotInfo[] enemies, MapLocation curLoc) throws GameActionException {
			if (enemies.length == 0) {
				return null;
			}
			double minDistance = Double.MAX_VALUE;
			RobotInfo closestRobot = null;
			for (RobotInfo info : enemies) {
				int tempDist = info.location.distanceSquaredTo(curLoc);
				if (tempDist < minDistance){
					closestRobot = info;
					minDistance = tempDist;
				}
			}
			return closestRobot;
		}

		public Direction basherMoveDir(Direction dirToEnemy) throws GameActionException {
			if (rc.canMove(dirToEnemy)){
				return dirToEnemy;
			} else if (rc.canMove(dirToEnemy.rotateLeft())) {
				return dirToEnemy.rotateLeft();
			} else if (rc.canMove(dirToEnemy.rotateRight())){
				return dirToEnemy.rotateRight();
			} else if (rc.canMove(dirToEnemy.rotateLeft().rotateLeft())){
				return dirToEnemy.rotateLeft().rotateLeft();
			} else if (rc.canMove(dirToEnemy.rotateRight().rotateRight())){
				return dirToEnemy.rotateRight().rotateRight();
			} else if (rc.canMove(dirToEnemy.rotateLeft().rotateLeft().rotateLeft())){
				return dirToEnemy.rotateLeft().rotateLeft().rotateLeft();
			} else if (rc.canMove(dirToEnemy.rotateRight().rotateRight().rotateRight())){
				return dirToEnemy.rotateRight().rotateRight().rotateRight();
			} else if (rc.canMove(dirToEnemy.rotateRight().rotateRight().rotateRight().rotateRight())){
				return dirToEnemy.rotateRight().rotateRight().rotateRight().rotateRight();
			} else {
				return null;	//no exit in sight, cannot evade enemy
			}
		}

		public void distributeSupplies() throws GameActionException {
			RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
			double mySupply = rc.getSupplyLevel();
			double lowerSupply = mySupply;
			int transferAmount = 0;
			MapLocation suppliesToThisLocation = null;

			for (RobotInfo ri : nearbyAllies) {
				if (ri.supplyLevel < lowerSupply){
					lowerSupply = ri.supplyLevel;
					transferAmount = (int)((mySupply-lowerSupply)/2);
					suppliesToThisLocation = ri.location;
				}
			}
			if (suppliesToThisLocation != null){
				rc.transferSupplies(transferAmount, suppliesToThisLocation);
			}
		}

		public boolean isSafe(RobotType info, MapLocation myLoc) throws GameActionException {
			RobotInfo[] enemies = rc.senseNearbyRobots(info.sensorRadiusSquared, this.theirTeam);
			return (enemies.length==0);
		}

		public Direction evadeDirection(RobotType info, MapLocation myLoc) throws GameActionException {
			RobotInfo[] enemies = rc.senseNearbyRobots(info.sensorRadiusSquared, this.theirTeam);
			if (enemies.length==0){
				return null;
			} else if (enemies.length==1){
				//Run away from this enemy
				Direction d = myLoc.directionTo(enemies[0].location).opposite();
				if (rc.canMove(d)){
					return d;
				} else if (rc.canMove(d.rotateLeft())) {
					return d.rotateLeft();
				} else if (rc.canMove(d.rotateRight())){
					return d.rotateRight();
				} else if (rc.canMove(d.rotateLeft().rotateLeft())){
					return d.rotateLeft().rotateLeft();
				} else if (rc.canMove(d.rotateRight().rotateRight())){
					return d.rotateRight().rotateRight();
				} else {
					return null;	//no exit in sight, cannot evade enemy
				}
			} else {
				//Run back to base:
				Direction d = myLoc.directionTo(this.myHQ);
				if (rc.canMove(d)){
					return d;
				} else if (rc.canMove(d.rotateLeft())) {
					return d.rotateLeft();
				} else if (rc.canMove(d.rotateRight())){
					return d.rotateRight();
				} else if (rc.canMove(d.rotateLeft().rotateLeft())){
					return d.rotateLeft().rotateLeft();
				} else if (rc.canMove(d.rotateRight().rotateRight())){
					return d.rotateRight().rotateRight();
				} else {
					return null;	//no exit in sight, cannot evade enemy
				}
			}
		}

		public boolean canCrawlTo(MapLocation curLoc, Direction toHere) throws GameActionException {
			RobotInfo robotHere = rc.senseRobotAtLocation(curLoc.add(toHere));
			if (robotHere != null){
				if (robotHere.type.isBuilding){
					return false;
				}
			}
			TerrainTile thisTile = rc.senseTerrainTile(curLoc.add(toHere));
			if ( thisTile.isTraversable() ) {
				// check if an HQ/TOWER can shoot from here
				int siege = rc.readBroadcast(SIEGE);	// if siege == 1, then we should be able to move into HQ/tower range
				for (MapLocation tower : theirTowers){
					if (((curLoc.add(toHere)).distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) && (siege == 0)) {
						return false;
					}
				}
				if (((curLoc.add(toHere)).distanceSquaredTo(theirHQ) <= RobotType.HQ.attackRadiusSquared) && (siege == 0)) {
					return false;
				} else {
					return true;
				}

			} else {
				return false;	// Must be a void, etc.
			}
		}

		public Direction backToBase(MapLocation myLoc) throws GameActionException {
			Direction d = myLoc.directionTo(this.myHQ);
			if (rc.canMove(d)){
				return d;
			} else if (rc.canMove(d.rotateLeft())) {
				return d.rotateLeft();
			} else if (rc.canMove(d.rotateRight())){
				return d.rotateRight();
			} else if (rc.canMove(d.rotateLeft().rotateLeft())){
				return d.rotateLeft().rotateLeft();
			} else if (rc.canMove(d.rotateRight().rotateRight())){
				return d.rotateRight().rotateRight();
			} else {
				return null;	//no exit in sight, cannot evade enemy
			}
		}

		public void beginningOfTurn() {
			if (rc.senseEnemyHQLocation() != null) {
				this.theirHQ = rc.senseEnemyHQLocation();
			}
		}

		public void endOfTurn() {
		}

		public void go() throws GameActionException {
			beginningOfTurn();
			execute();
			endOfTurn();
		}

		public void execute() throws GameActionException {
			rc.yield();
		}

		public void move(Direction dir) throws GameActionException {
			if (dir != null && rc.isCoreReady()) {
				rc.move(dir);
			}
		}

		public void spawn(Direction dir, RobotType type) throws GameActionException {
			if (dir != null && rc.isCoreReady()) {
				rc.spawn(dir, type);
			}
		}

		public void build(Direction dir, RobotType type) throws GameActionException {
			if (dir != null && rc.isCoreReady()) {
				rc.build(dir, type);
			}
		}

		// public MapLocation findSubgroupCentroid(int subgroup) {
		//
		// }
	}

	/*** MESSAGING PROTOCOL ***/
	/*** Names here correspond to variable names used to store msg values ***/
	/*** (Obviously use camelCase where appropriate when using msg vars) ***/
	final static protected int GAME_STAGE = 0; //Used by HQ to determine when certain kinds of units/structures should be built
	final static protected int NUM_MINER_FACTORIES = 1; //Number of COMPLETED miner factories on the map
	final static protected int BUILD_TURN_MINER_FACTORY = 2; //Round # when a MINERFACTORY will be completed. +1 until complete
	final static protected int NUM_BARRACKS = 3; //See above...
	final static protected int BUILD_TURN_BARRACKS = 4;
	final static protected int NUM_SUPPLY_DEPOTS = 5;
	final static protected int BUILD_TURN_SUPPLY_DEPOT = 6;
	final static protected int NUM_TANK_FACTORIES = 7;
	final static protected int BUILD_TURN_TANK_FACTORY = 8;
	final static protected int SIEGE = 10; //"1" if currently on offensive, "0" if on the defensive
	final static protected int SIEGE_TIME = 11; //Counts the number of turns during which the "siege" variable has been equal to "1"
	final static protected int BUILDER_BEAVER = 50; //The beaver that builds stuff (one exists on a given round)
	final static protected int SAVED_ORE = 51;
	final static protected int RALLY_X = 100;
	final static protected int RALLY_Y = 101;
	final static protected int BEAVER_ATTENDANCE = 999;
	final static protected int NUM_BEAVERS = 1000; //Number of beavers alive
	final static protected int CUR_BEAVER = 1001; //Points to beaver in stack. Index (incremented by 2) on the range [1002,1998]
	final static protected int MINER_ATTENDANCE = 1999;
	final static protected int NUM_MINERS = 2000; //see above...
	final static protected int CUR_MINER = 2001;
	final static protected int SOLDIER_ATTENDANCE = 2999;
	final static protected int NUM_SOLDIERS = 3000;
	final static protected int CUR_SOLDIER = 3001;
	final static protected int TANK_ATTENDANCE = 3999;
	final static protected int NUM_TANKS = 4000;
	final static protected int CUR_TANK = 4001;
	final static protected int BASHER_ATTENDANCE = 4999;
	final static protected int NUM_BASHERS = 5000;
	final static protected int CUR_BASHER = 5001;
	final static protected int CENTROID_DIVISOR = 9999; //A counter that's used for computing centroids of subgroups
	final static protected int NUM_SUBGROUPS = 10000; //Number of subgroups. Beginning of subgroup stack
	final static protected int CUR_SUBGROUP = 10001;



	/*** UNIT STACK MECHANICS (e.g. Beavers) ***/
	// 1000	[10] 		<-- Number of beavers
	// 1001 [1007] 	<-- Index of 'current' beaver
	// 1002 [1] 		<-- some state for beaver #1
	// 1003 [0] 		<-- some subgroup for beaver #1
	// 1004 [4] 		<-- some state for beaver #2
	// 1005 [0] 		<-- some subgroup for beaver #2
	// 1006 [1] 		<-- some state for beaver #3
	// 1007 [2] 		<-- some subgroup for beaver #3
	// etc.....


	//----- HQ -----//
	public static class HQ extends BaseBot {

		public HQ(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			distributeSupplies();
			int gameStage = rc.readBroadcast(GAME_STAGE);
			int siege = rc.readBroadcast(SIEGE);
			int siegeTime = rc.readBroadcast(SIEGE_TIME);
			int numBeavers = rc.readBroadcast(BEAVER_ATTENDANCE);
			rc.broadcast(BEAVER_ATTENDANCE, 0); //reset attendance
			rc.broadcast(NUM_BEAVERS, numBeavers);
			int numSoldiers = rc.readBroadcast(SOLDIER_ATTENDANCE);
			rc.broadcast(SOLDIER_ATTENDANCE, 0); //reset attendance
			rc.broadcast(NUM_SOLDIERS, numSoldiers);
			int numTanks = rc.readBroadcast(TANK_ATTENDANCE);
			rc.broadcast(TANK_ATTENDANCE, 0); //reset attendance
			rc.broadcast(NUM_TANKS, numTanks);
			int numMiners = rc.readBroadcast(MINER_ATTENDANCE);
			rc.broadcast(MINER_ATTENDANCE, 0); //reset attendance
			rc.broadcast(NUM_MINERS, numMiners);
			int numBashers = rc.readBroadcast(BASHER_ATTENDANCE);
			rc.broadcast(BASHER_ATTENDANCE, 0); //reset attendance
			rc.broadcast(NUM_BASHERS, numBashers);
			int numMinerFactories = rc.readBroadcast(NUM_MINER_FACTORIES);
			rc.broadcast(NUM_MINER_FACTORIES, 0); //reset attendance
			int numBarracks = rc.readBroadcast(NUM_BARRACKS);
			rc.broadcast(NUM_BARRACKS, 0); //reset attendance
			int numSupplyDepots = rc.readBroadcast(NUM_SUPPLY_DEPOTS);
			rc.broadcast(NUM_SUPPLY_DEPOTS, 0); //reset attendance
			int numTankFactories = rc.readBroadcast(NUM_TANK_FACTORIES);
			rc.broadcast(NUM_TANK_FACTORIES, 0); //reset attendance

			// This is a way of checking how many structures are currently in the process of being built
			if (Clock.getRoundNum() < rc.readBroadcast(BUILD_TURN_MINER_FACTORY)){
				numMinerFactories += 1;
			}
			if (Clock.getRoundNum() < rc.readBroadcast(BUILD_TURN_BARRACKS)){
				numBarracks += 1;
			}
			if (Clock.getRoundNum() < rc.readBroadcast(BUILD_TURN_SUPPLY_DEPOT)){
				numSupplyDepots += 1;
			}
			if (Clock.getRoundNum() < rc.readBroadcast(BUILD_TURN_TANK_FACTORY)){
				numTankFactories += 1;
			}
			int builderBeaver = rc.readBroadcast(BUILDER_BEAVER);

			if (builderBeaver == 0){
				rc.broadcast(BUILDER_BEAVER,NUM_BEAVERS+2);		//Initialize beaverBuilder (if it hasn't been done yet)
				builderBeaver = NUM_BEAVERS+2;
			}

			///////////////////////////////////////////////////////////////////////////////////////////////////
			// GAME STAGE 0 //
			if ((numMinerFactories == 0) && (numBarracks == 0) && (numTankFactories == 0) && (gameStage != 0)){
				gameStage = 0;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			// GAME STAGE 1 //
			if ((numMinerFactories == 1) && (numBarracks == 0) && (numTankFactories == 0) && (gameStage != 1)){
				gameStage = 1;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			// GAME STAGE 2 //
			if ((numMinerFactories == 1) && (numBarracks == 1) && (numTankFactories == 0) && (numMiners >= 1) && (gameStage != 2)){
				gameStage = 2;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			// GAME STAGE 3 //
			if ((numMinerFactories == 1) && (numBarracks == 1) && (numTankFactories == 1) && (numSoldiers >= 8) && (gameStage != 3)){
				gameStage = 3;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			// GAME STAGE 4 //
			if ((numMinerFactories == 1) && (numBarracks == 2) && (numTankFactories == 2) && (numTanks >= 3) && (gameStage != 4)){
				gameStage = 4;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			// GAME STAGE 5 //
			if ((numMinerFactories == 1) && (numBarracks == 2) && (numTankFactories == 3) && (numTanks >= 6) && (gameStage != 5)){
				gameStage = 5;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			///////////////////////////////////////////////////////////////////////////////////////////////////
			//Testing
			rc.setIndicatorString(0, "numSoldiers: "+numSoldiers);
			rc.setIndicatorString(1, "numBeavers: "+numBeavers);
			rc.setIndicatorString(2, "Gamestage: "+gameStage);





			//Build structures

			// BUILD SUPPLY DEPOTS
			if ((numSupplyDepots < Math.round((numBeavers+numMiners+numSoldiers+numTanks)/10)) || (rc.getTeamOre() > 1000)){
				builderBeaver = NUM_BEAVERS + 2*numBeavers;	//first try the closest beaver!
				rc.broadcast(builderBeaver, 5); 	// tell builder beaver to go into "build-supply"-mode (5)
//				savedOre += RobotType.SUPPLYDEPOT.oreCost;
//				rc.broadcast(SAVED_ORE,savedOre);
			}
			// BUILD MINER FACTORIES
			if (gameStage == 0) {
				if (rc.getTeamOre() > RobotType.MINERFACTORY.oreCost){
					builderBeaver = NUM_BEAVERS + 2*numBeavers;	//first try the closest beaver!
					rc.broadcast(builderBeaver, 3); 	// tell first beaver to go into "build-miner"-mode (3)
				}
			}
			// BUILD BARRACKS FACTORIES
			if ((gameStage==1 && numBarracks < 1) || (gameStage==3 && numBarracks < 2)) {
				if (rc.getTeamOre() > RobotType.BARRACKS.oreCost){
					builderBeaver = NUM_BEAVERS + 2*numBeavers;	//first try the closest beaver!
					rc.broadcast(builderBeaver, 4); 	// tell first beaver to go into "build-barracks"-mode (3)
				}
			}
			// BUILD TANK FACTORIES
			if ((gameStage==2 && numTankFactories < 1) || (gameStage==3 && numTankFactories < 2) || (gameStage==4 && numTankFactories < 3)) {
				if (rc.getTeamOre() > RobotType.TANKFACTORY.oreCost){
					builderBeaver = NUM_BEAVERS + 2*numBeavers;	//first try the closest beaver!
					rc.broadcast(builderBeaver, 6); 	// tell first beaver to go into "build-tank-factory"-mode (3)
				}
			}


			//Attack commands
			if ((numSoldiers+numTanks+numBashers > 25) && siege == 0) {
//				rc.broadcast(RALLY_X, this.theirHQ.x);
//				rc.broadcast(RALLY_Y, this.theirHQ.y);
				siege = 1;
				rc.broadcast(SIEGE, siege);	// We are now sieging on enemy base
			}
			if ((siege==1 && numTanks+numBashers+numSoldiers < 10) || (siege==1 && siegeTime >= 100)) {
//				rc.broadcast(RALLY_X, (int)((this.theirHQ.x+2*this.myHQ.x)/3));
//				rc.broadcast(RALLY_Y, (int)((this.theirHQ.y+2*this.myHQ.y)/3));	// Rally in front of home base
				siege = 0;
				rc.broadcast(SIEGE,siege);
				rc.broadcast(SIEGE_TIME, 0);
			}
			if (siege == 1){
				rc.broadcast(SIEGE_TIME, siegeTime+1);
			}


			//Fire at nearby enemies & set rally point
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.HQ.sensorRadiusSquared, this.theirTeam);
			if (enemies.length != 0){
				RobotInfo closestEnemy = closestEnemy(enemies, myHQ);
				rc.broadcast(RALLY_X, closestEnemy.location.x);
				rc.broadcast(RALLY_Y, closestEnemy.location.y);
				if (rc.isWeaponReady()) {
					attackLeastHealthEnemy(getEnemiesInAttackingRange(RobotType.HQ));
				}
			} else if (siege==0) {
				rc.broadcast(RALLY_X, (int)((this.theirHQ.x+2*this.myHQ.x)/3));
				rc.broadcast(RALLY_Y, (int)((this.theirHQ.y+2*this.myHQ.y)/3));	// Rally in front of home base
			} else {
				rc.broadcast(RALLY_X, this.theirHQ.x);
				rc.broadcast(RALLY_Y, this.theirHQ.y);
			}
			if (Clock.getRoundNum() >= 1800) {
				rc.broadcast(RALLY_X, this.theirHQ.x);
				rc.broadcast(RALLY_Y, this.theirHQ.y);
			}

			//Build beavers
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.BEAVER.oreCost)){
				if ((gameStage==0 && numBeavers < 1) || (gameStage == 1 && numBeavers < 4)) {
					Direction newDir = getBeaverSpawnDirection();
					if (newDir != null) {
						rc.spawn(newDir, RobotType.BEAVER);
						if (rc.readBroadcast(NUM_BEAVERS+1)==0){
							rc.broadcast(NUM_BEAVERS+1, NUM_BEAVERS+2);	//Initialize the pointer (if it hasn't been done yet)
						}
						rc.broadcast(NUM_BEAVERS, numBeavers + 1);	//increment numBeavers
					}
				}
			}
			rc.setIndicatorString(1, "numBeavers: "+numBeavers);
//			rc.broadcast(GAME_STAGE, gameStage);
			rc.yield();
		}
	}

	//----- Beaver -----//
	public static class Beaver extends BaseBot {
		public Beaver(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			distributeSupplies();
			rc.broadcast(BEAVER_ATTENDANCE, rc.readBroadcast(BEAVER_ATTENDANCE)+1);	//Take attendance
			int numBeavers = rc.readBroadcast(NUM_BEAVERS);
			int curBeaver = rc.readBroadcast(NUM_BEAVERS+1);
			int curState = rc.readBroadcast(curBeaver);		//Get the current state
			int staticTime = rc.readBroadcast(curBeaver+1);	//Get the time spent stationary
			int builderBeaver = rc.readBroadcast(50);
			MapLocation curLoc = rc.getLocation();
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.BEAVER.sensorRadiusSquared, this.theirTeam);
			Boolean isSafe = (enemies.length==0);
			Boolean stateChanged = false;

			if (!isSafe) {	//There's an enemy robot nearby.  run away!
				curState = 2;
				stateChanged=true;
			}

			//Make sure an ALIVE unit is a builderBeaver
			if (builderBeaver > 2*numBeavers + NUM_BEAVERS) {
				builderBeaver = 2*numBeavers + NUM_BEAVERS;	//Reset builder beaver to most recently spawned beaver
			}

			//Testing
			rc.setIndicatorString(0, "curBeaver: "+curBeaver);
			rc.setIndicatorString(1, "numBeavers: "+numBeavers);

			switch (curState) {
			case 0: // Beaver in "mine" mode (default)
				double oreHere = rc.senseOre(curLoc);
				if (oreHere > 0) {	// Ore here, keep mining!
					if (rc.isCoreReady()) {
						rc.setIndicatorString(1, "Mining "+oreHere+", oh yeah!");
						rc.mine();
					}
				} else {
					Direction dMove = Direction.NONE;
					double largestOre = oreHere;
					for (Direction d : Direction.values()) {
						if (rc.senseOre(curLoc.add(d)) > largestOre && rc.canMove(d)) {
							largestOre = rc.senseOre(curLoc.add(d));
							dMove = d;
						}
					}
					rc.setIndicatorString(1, "oreHere"+oreHere+", largestOre: "+largestOre);
					if ((dMove != Direction.NONE) && rc.isCoreReady()) {
						rc.move(dMove);
					} else if (largestOre < 0.01) { // Go exploring for minerals!
						rc.setIndicatorString(2, "Going exploring");
						if (rc.isCoreReady()){
							Direction d = getRandomDirection();
							if (d!= null){
								rc.move(d);
							}
						}
					}
				}
				break;

			case 1: // Beaver in "explore" mode (goes to "mine" if lots of ore found, or "run" if enemy found)
				if (rc.isCoreReady()) {
					Direction d = getRandomDirection();
					if (d!= null){
						rc.move(d);
						staticTime = 0;
					}
				}
				if (rc.senseOre(curLoc) > 10){
					curState = 0;
					stateChanged=true;
				}
				break;

			case 2:	// Beaver in "return-to-base" mode
				Direction backToBase = backToBase(curLoc);
				int distanceToHQ = curLoc.distanceSquaredTo(this.myHQ);
				rc.setIndicatorString(1, "distanceSquareToHQ: "+distanceToHQ);
				if (distanceToHQ < 25){
					curState = 0;
					stateChanged = true;
				} else {
					if (rc.isCoreReady()){
						rc.move(backToBase);
					}
				}
				break;

			case 3: // Beaver in "build-miner" mode
				Direction newDir = getBuildDirection(RobotType.MINERFACTORY);
				if (newDir != null) {
					if (rc.isCoreReady()){
						if (rc.canBuild(newDir, RobotType.MINERFACTORY)){
							rc.build(newDir, RobotType.MINERFACTORY);
							rc.broadcast(BUILD_TURN_MINER_FACTORY, Clock.getRoundNum()+RobotType.MINERFACTORY.buildTurns+1);	// report time at which MinerFactory should be completed
						}
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) { // no directions available, but core free to use for a move
					Direction d = getRandomDirection();
					if (d!= null){
						rc.move(d);
					}
					curState = 0;
					stateChanged = true;
				}
				// Remove responsibility for building (decrement builderBeaver - because of proximity arguments)
				if (builderBeaver == NUM_BEAVERS+2){
					rc.broadcast(BUILDER_BEAVER, NUM_BEAVERS + 2*numBeavers);
				} else {
					rc.broadcast(BUILDER_BEAVER, builderBeaver - 2);
				}
				break;

			case 4: // Beaver in "build-barracks" mode
				Direction newBarracksDir = getBuildDirection(RobotType.BARRACKS);
				if (newBarracksDir != null) {
					if (rc.isCoreReady()){
						if (rc.canBuild(newBarracksDir, RobotType.BARRACKS)){
							rc.build(newBarracksDir, RobotType.BARRACKS);
							rc.broadcast(BUILD_TURN_BARRACKS, Clock.getRoundNum()+RobotType.BARRACKS.buildTurns+1);	// report time at which Barracks should be completed
						}
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) {
					Direction d = getRandomDirection();
					if (d!= null){
						rc.move(d);
					}
					curState = 0;
					stateChanged = true;
				}
				// Remove responsibility for building (decrement builderBeaver - because of proximity arguments)
				if (builderBeaver == NUM_BEAVERS+2){
					rc.broadcast(BUILDER_BEAVER, NUM_BEAVERS + 2*numBeavers);
				} else {
					rc.broadcast(BUILDER_BEAVER, builderBeaver - 2);
				}
				rc.setIndicatorString(1, "newBarracksDir: "+newBarracksDir);
				rc.setIndicatorString(2, "rc.getCoreDelay(): "+rc.getCoreDelay());
				break;

			case 5: // Beaver in "build-supply-depot" mode
				Direction newSupplyDir = getBuildDirection(RobotType.SUPPLYDEPOT);
				if (newSupplyDir != null) {
					if (rc.isCoreReady()){
						if (rc.canBuild(newSupplyDir, RobotType.SUPPLYDEPOT)){
							rc.build(newSupplyDir, RobotType.SUPPLYDEPOT);
							rc.broadcast(BUILD_TURN_SUPPLY_DEPOT, Clock.getRoundNum()+RobotType.SUPPLYDEPOT.buildTurns+1);	// report time at which SupplyDepot should be completed
						}
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) {
					Direction d = getRandomDirection();
					if (d!= null){
						rc.move(d);
					}
					curState = 0;
					stateChanged = true;
				}
				// Remove responsibility for building (decrement builderBeaver - because of proximity arguments)
				if (builderBeaver == NUM_BEAVERS+2){
					rc.broadcast(BUILDER_BEAVER, NUM_BEAVERS + 2*numBeavers);
				} else {
					rc.broadcast(BUILDER_BEAVER, builderBeaver - 2);
				}
				break;

			case 6: // Beaver in "build-tank-factory" mode
				Direction newTankDir = getBuildDirection(RobotType.TANKFACTORY);
				if (newTankDir != null) {
					if (rc.isCoreReady()){
						if (rc.canBuild(newTankDir, RobotType.TANKFACTORY)){
							rc.build(newTankDir, RobotType.TANKFACTORY);
							rc.broadcast(BUILD_TURN_TANK_FACTORY, Clock.getRoundNum()+RobotType.TANKFACTORY.buildTurns+1);	// report time at which tankFactory should be completed
						}
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) {
					Direction d = getRandomDirection();
					if (d!= null){
						rc.move(d);
					}
					curState = 0;
					stateChanged = true;
				}
				// Remove responsibility for building (decrement builderBeaver - because of proximity arguments)
				if (builderBeaver == NUM_BEAVERS+2){
					rc.broadcast(BUILDER_BEAVER, NUM_BEAVERS + 2*numBeavers);
				} else {
					rc.broadcast(BUILDER_BEAVER, builderBeaver - 2);
				}
				break;
			}

			//System.out.println("Beaver #"+rc.getID()+" at stack position "+curBeaver);

			//Take care of stack
			if (stateChanged) {rc.broadcast(curBeaver,curState);}	//Save changed state, if it was changed
			rc.broadcast(curBeaver+1,staticTime);					//Save changed time
			if (curBeaver >= NUM_BEAVERS+2*numBeavers) {
				rc.broadcast(NUM_BEAVERS+1, NUM_BEAVERS+2);
			} else {
				rc.broadcast(NUM_BEAVERS+1, curBeaver+2);
			}
			rc.yield();
		}
	}

	//----- Miner -----//
	public static class Miner extends BaseBot {
		public Miner(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			distributeSupplies();
			rc.broadcast(MINER_ATTENDANCE, rc.readBroadcast(MINER_ATTENDANCE)+1);	//Take attendance
			int numMiners = rc.readBroadcast(NUM_MINERS);
			int curMiner = rc.readBroadcast(NUM_MINERS+1);
			int curState = rc.readBroadcast(curMiner);		//Get the current state
			int staticTime = rc.readBroadcast(curMiner+1);	//Get the time spent stationary
			MapLocation curLoc = rc.getLocation();
			Boolean isSafe = isSafe(RobotType.MINER, curLoc);
			Boolean stateChanged = false;

			if (!isSafe) {	//There's an enemy robot nearby.  run away!
				curState = 2;
				stateChanged=true;
			}
			
			//Testing
			rc.setIndicatorString(0, "curMiner: "+curMiner);
			rc.setIndicatorString(1, "curState: "+curState);

			switch (curState){
			case 0:	// Miner in "mine" mode
				if (rc.isCoreReady()) {
					double oreHere = rc.senseOre(curLoc);
					if (oreHere > 10 && (staticTime < 8)) {	// Lot's of ore here, keep mining!
						rc.mine();
						staticTime++;
					} else if (oreHere > 0) { // Look for more ore if only getting 0.1 ore per mine
						Boolean stickAround = true;
						int firstTry = randInt(0,7);
						for (int i = 0; i < 8; i++) {
							Direction d = directionValues()[(i+firstTry)%8];
							if (rc.canMove(d) && rc.senseOre(curLoc.add(d)) > oreHere && rc.isCoreReady()){
								rc.move(d);
								stickAround = false;
								staticTime = 0;
							}
						}
						if (stickAround && (staticTime < 8)) { // Couldn't find better ore, so mine here
							rc.mine();
							staticTime++;
						}
					} else {	// Go exploring for minerals!
						curState = 1;
						stateChanged = true;
						Direction d = getRandomDirection();
						if (rc.isCoreReady() && d!= null){
							rc.move(d);
						}
						staticTime = 0;
					}
				}
				break;
			case 1:	// Miner in "explore" mode
				if (rc.isCoreReady()) {
					Direction d = getRandomDirection();
					if (d!= null){
						rc.move(d);
						staticTime=0;
					}
					if (rc.senseOre(curLoc) > 6){
						curState = 0;
						stateChanged=true;
					}
				}
				break;
			case 2:	// Miner in "run" mode
				Direction backToBase = backToBase(curLoc);
				int distanceToHQ = curLoc.distanceSquaredTo(this.myHQ);
				rc.setIndicatorString(1, "distanceSquareToHQ: "+distanceToHQ);
				if (distanceToHQ < 25){
					curState = 0;
					stateChanged = true;
				} else {
					if (rc.isCoreReady() && backToBase != null){
						rc.move(backToBase);
					}
				}
				break;
			}

			//Take care of stack
			if (stateChanged) {rc.broadcast(curMiner,curState);}	//Save changed state, if it was changed
			rc.broadcast(curMiner+1,staticTime);					//Save changed time
			if (curMiner >= NUM_MINERS+2*numMiners) {
				rc.broadcast(NUM_MINERS+1, NUM_MINERS+2);
			} else {
				rc.broadcast(NUM_MINERS+1, curMiner+2);
			}
			rc.yield();
		}
	}

	//----- Barracks -----//
	public static class Barracks extends BaseBot {
		public Barracks(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			rc.broadcast(NUM_BARRACKS, rc.readBroadcast(NUM_BARRACKS)+1);	//Take attendance
			int numSoldiers = rc.readBroadcast(NUM_SOLDIERS);
			int numBashers = rc.readBroadcast(NUM_BASHERS);
			int gameStage = rc.readBroadcast(GAME_STAGE);
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.SOLDIER.oreCost)){
				if (gameStage==2 && numSoldiers < 10) {
					Direction newDir = getSpawnDirection(RobotType.SOLDIER);
					if (newDir != null) {
						rc.spawn(newDir, RobotType.SOLDIER);
						if (rc.readBroadcast(NUM_SOLDIERS+1)==0){
							rc.broadcast(NUM_SOLDIERS+1, NUM_SOLDIERS+2);	//Initialize the pointer (if it hasn't been done yet)
						}
						rc.broadcast(NUM_SOLDIERS, numSoldiers + 1);	//increment numSoldiers
					}
				}
			}
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.TANK.oreCost + RobotType.SOLDIER.oreCost)) {
				if (gameStage>=3) {
					Direction newDir = getSpawnDirection(RobotType.SOLDIER);
					if (newDir != null) {
						rc.spawn(newDir, RobotType.SOLDIER);
						if (rc.readBroadcast(NUM_SOLDIERS+1)==0){
							rc.broadcast(NUM_SOLDIERS+1, NUM_SOLDIERS+2);	//Initialize the pointer (if it hasn't been done yet)
						}
						rc.broadcast(NUM_SOLDIERS, numSoldiers + 1);	//increment numSoldiers
					}
				}
			}
			// if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.BASHER.oreCost)){
			// 	if ((gameStage>=3 && numBashers < 10) || (gameStage >= 4 && numBashers < 15)) {
			// 		Direction newDir = getSpawnDirection(RobotType.BASHER);
			// 		if (newDir != null) {
			// 			rc.spawn(newDir, RobotType.BASHER);
			// 			if (rc.readBroadcast(NUM_BASHERS+1)==0){
			// 				rc.broadcast(NUM_BASHERS+1, NUM_BASHERS+2);	//Initialize the pointer (if it hasn't been done yet)
			// 			}
			// 			rc.broadcast(NUM_BASHERS, numBashers + 1);	//increment numSoldiers
			// 		}
			// 	}
			// }

			rc.yield();
		}
	}

	//----- TankFactory -----//
	public static class TankFactory extends BaseBot {
		public TankFactory(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			rc.broadcast(NUM_TANK_FACTORIES, rc.readBroadcast(NUM_TANK_FACTORIES)+1);	//Take attendance
			int numTanks = rc.readBroadcast(NUM_TANKS);
			int gameStage = rc.readBroadcast(GAME_STAGE);
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.TANK.oreCost)){
				if ((gameStage>=3 && numTanks < 6) || (gameStage >= 4 && numTanks < 25) || (gameStage >= 5 && numTanks < 35)) {
					Direction newDir = getSpawnDirection(RobotType.TANK);
					if (newDir != null) {
						rc.spawn(newDir, RobotType.TANK);
						if (rc.readBroadcast(NUM_TANKS+1)==0){
							rc.broadcast(NUM_TANKS+1, NUM_TANKS+2);	//Initialize the pointer (if it hasn't been done yet)
						}
						rc.broadcast(NUM_TANKS, numTanks + 1);	//increment numTanks
					}
				}
			}

			rc.yield();
		}
	}

	//----- MinerFactory -----//
	public static class MinerFactory extends BaseBot {
		public MinerFactory(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			rc.broadcast(NUM_MINER_FACTORIES, rc.readBroadcast(NUM_MINER_FACTORIES)+1);	//Take attendance
			int gameStage = rc.readBroadcast(GAME_STAGE);
			int numMiners = rc.readBroadcast(NUM_MINERS);
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.MINER.oreCost)){
				if ((gameStage==1 && numMiners < 5) || (gameStage == 2 && numMiners < 15) || (gameStage == 4 && numMiners < 20) || (gameStage == 5 && numMiners < 25)) {
					Direction newDir = getSpawnDirection(RobotType.MINER);
					if (newDir != null) {
						rc.spawn(newDir, RobotType.MINER);
						if (rc.readBroadcast(NUM_MINERS+1)==0){
							rc.broadcast(NUM_MINERS+1, NUM_MINERS+2);	//Initialize the pointer (if it hasn't been done yet)
						}
						rc.broadcast(NUM_MINERS, numMiners + 1);	//increment numMiners
					}
				}
			}

			rc.yield();
		}
	}

	//----- Soldier -----//
	public static class Soldier extends BaseBot {

		public Soldier(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			int oldDistanceToEnemy;
			Direction prevCrawlerDir;

			distributeSupplies();
			rc.setIndicatorString(0, "previous attendance: "+rc.readBroadcast(SOLDIER_ATTENDANCE));
			rc.broadcast(SOLDIER_ATTENDANCE, rc.readBroadcast(SOLDIER_ATTENDANCE)+1);	//Take attendance
			int numSoldiers = rc.readBroadcast(NUM_SOLDIERS);
			int curSoldier = rc.readBroadcast(NUM_SOLDIERS+1);
			int curState = rc.readBroadcast(curSoldier);		//Get the current state
			MapLocation curLoc = rc.getLocation();
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, this.theirTeam);
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, this.theirTeam);
			Boolean isSafe = (enemies.length==0);
			Boolean stateChanged = false;

			if (!isSafe) {	//There's an enemy robot nearby.  Do something and attack!
				if (rc.isWeaponReady() && enemiesInRange.length != 0) {
					attackLeastHealthEnemy(enemiesInRange);
				}
			}

			//Testing
//			rc.setIndicatorString(0, "curLoc.directionTo(theirHQ): "+curLoc.directionTo(theirHQ));
			rc.setIndicatorString(0, "curState: "+curState);
			rc.setIndicatorString(1, " ");
			rc.setIndicatorString(2, " ");

			switch (curState) {
			case 0: // Soldier in "HARASS" mode (default)
				if (rc.isCoreReady() && (enemiesInRange.length == 0)) {
					Direction d = getHarassMoveDir(theirHQ, curLoc);
//					rc.setIndicatorString(2, "harassMoveDir: "+d);
					int distToTheirHQ = curLoc.distanceSquaredTo(theirHQ);
					if (d != null) {
						rc.move(d);
					} else if (distToTheirHQ > 36) {
						// if first 3 move choices are not able to be made, probably stuck.
						stateChanged = true;
						curState = randInt(1,2);	// make it randomly select between states 1&2
//						rc.setIndicatorString(1, "curState: "+curState);
//						rc.setIndicatorString(2, "cannot move forward");
						rc.broadcast(curSoldier+1, distToTheirHQ);	//save how far you are from their HQ
//						below added for wall-crawler method
						rc.broadcast(curSoldier+2, dirToInt(curLoc.directionTo(theirHQ)));
						rc.broadcast(curSoldier + 3, 0);	// initialize # of first tries to zero
					}
				}
				break;

			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			case 1:	// Follow walls & enemy radii through LEFTWISE rotations
				oldDistanceToEnemy = rc.readBroadcast(curSoldier+1);
				int numLeftTries = rc.readBroadcast(curSoldier+3);
				prevCrawlerDir = intToDir(rc.readBroadcast(curSoldier + 2));
				rc.setIndicatorString(1, "prevCrawlerDir: "+prevCrawlerDir);

				// Base case:  if crawling got us closer to the enemy, return to normal movement
				if ((curLoc.distanceSquaredTo(theirHQ) < oldDistanceToEnemy) || (numLeftTries > 4)) {
					rc.setIndicatorString(2, "just hit base case!");
					stateChanged=true;
					curState = 0;
				}

				//Check to see if curLoc.add(prevCrawlerDir) can be moved to (no structures or VOIDS)
				if (canCrawlTo(curLoc, prevCrawlerDir)) {
					rc.setIndicatorString(2, "canCrawl towards "+prevCrawlerDir+" and rc.isCoreReady()="+rc.isCoreReady()+" and rc.canMove="+rc.canMove(prevCrawlerDir));
					if (rc.isCoreReady() && rc.canMove(prevCrawlerDir)) {
						rc.move(prevCrawlerDir);
						Direction savedCrawlerDir = prevCrawlerDir.rotateRight().rotateRight();
						rc.broadcast(curSoldier+2, dirToInt(savedCrawlerDir));
						rc.broadcast(curSoldier + 3,  numLeftTries + 1);
					}
				}

				//Rotate left until we can crawl to a new spot
				Direction[] dirsL = {prevCrawlerDir, prevCrawlerDir.rotateLeft(), prevCrawlerDir.rotateLeft().rotateLeft(),
						prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft(), prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft().rotateLeft(),
						prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft().rotateLeft().rotateLeft()};

				for (int i = 0; i < dirsL.length; i++) {
					if (canCrawlTo(curLoc, dirsL[i].rotateLeft())){
						rc.setIndicatorString(2, "YES! I can crawl towards: "+dirsL[i].rotateLeft());
						if (rc.isCoreReady() && rc.canMove(dirsL[i].rotateLeft())) {
							rc.move(dirsL[i].rotateLeft());
							rc.broadcast(curSoldier+2, dirToInt(dirsL[i]));	// saved the last wall or structure direction
							rc.broadcast(curSoldier + 3,  0);
						} else if (rc.isCoreReady()) {	// Core is ready, can crawl, but a unit is in the way.  Right-rotating units get preference.
							Direction toHQ = curLoc.directionTo(myHQ);
							if (rc.canMove(toHQ)){
								rc.move(toHQ);
								curState = 0;
								stateChanged = true;
							}
						}
						break;
					}
				}
				break;

			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			case 2: // Follow walls & enemy radii through RIGHTWISE rotations
				oldDistanceToEnemy = rc.readBroadcast(curSoldier+1);
				int numRightTries = rc.readBroadcast(curSoldier+3);
				prevCrawlerDir = intToDir(rc.readBroadcast(curSoldier + 2));
				rc.setIndicatorString(1, "prevCrawlerDir: "+prevCrawlerDir);

				// Base case:  if crawling got us closer to the enemy, return to normal movement
				if ((curLoc.distanceSquaredTo(theirHQ) < oldDistanceToEnemy) || (numRightTries > 4)) {
					rc.setIndicatorString(2, "just hit base case!");
					stateChanged=true;
					curState = 0;
				}

				//Check to see if curLoc.add(prevCrawlerDir) can be moved to (no structures or VOIDS)
				if (canCrawlTo(curLoc, prevCrawlerDir)) {
					rc.setIndicatorString(2, "canCrawl towards "+prevCrawlerDir+" and rc.isCoreReady()="+rc.isCoreReady()+" and rc.canMove="+rc.canMove(prevCrawlerDir));
					if (rc.isCoreReady() && rc.canMove(prevCrawlerDir)) {
						rc.move(prevCrawlerDir);
						Direction savedCrawlerDir = prevCrawlerDir.rotateLeft().rotateLeft();
						rc.broadcast(curSoldier+2, dirToInt(savedCrawlerDir));
						rc.broadcast(curSoldier + 3,  numRightTries + 1);
					}
				}

				//Rotate left until we can crawl to a new spot
				Direction[] dirsR = {prevCrawlerDir, prevCrawlerDir.rotateRight(), prevCrawlerDir.rotateRight().rotateRight(),
						prevCrawlerDir.rotateRight().rotateRight().rotateRight(), prevCrawlerDir.rotateRight().rotateRight().rotateRight().rotateRight(),
						prevCrawlerDir.rotateRight().rotateRight().rotateRight().rotateRight().rotateRight()};

				for (int i = 0; i < dirsR.length; i++) {
					if (canCrawlTo(curLoc, dirsR[i].rotateRight())){
						rc.setIndicatorString(2, "YES! I can crawl towards: "+dirsR[i].rotateRight());
						if (rc.isCoreReady() && rc.canMove(dirsR[i].rotateRight())) {
							rc.move(dirsR[i].rotateRight());
							rc.broadcast(curSoldier+2, dirToInt(dirsR[i]));	// saved the last wall or structure direction
							rc.broadcast(curSoldier + 3,  0);
						}
						break;
					}
				}
				break;
			}

			//Take care of stack
			if (stateChanged) {rc.broadcast(curSoldier,curState);}	//Save changed state, if it was changed
			if (curSoldier >= NUM_SOLDIERS+4*numSoldiers-2) {	//Soldiers each occupy FOUR spots on the messaging board
				rc.broadcast(NUM_SOLDIERS+1, NUM_SOLDIERS+2);
			} else {
				rc.broadcast(NUM_SOLDIERS+1, curSoldier+4);
			}
			rc.yield();
		}
	}

	//----- Basher -----//
	public static class Basher extends BaseBot {

		public Basher(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			int oldDistanceToEnemy;
			Direction prevCrawlerDir;
			MapLocation rallyPoint;

			int rallyX = rc.readBroadcast(100);
			int rallyY = rc.readBroadcast(101);
			if ((rallyX == 0) && (rallyY == 0)){
				rallyX = (int)((this.theirHQ.x+2*this.myHQ.x)/3);	// (1/3) the way away from my base (defensively safe, but not too close)
				rallyY = (int)((this.theirHQ.y+2*this.myHQ.y)/3);
			}
			rallyPoint = new MapLocation(rallyX, rallyY);

			distributeSupplies();
			rc.broadcast(BASHER_ATTENDANCE, rc.readBroadcast(BASHER_ATTENDANCE)+1);	//Take attendance
			int numBashers = rc.readBroadcast(NUM_BASHERS);
			int curBasher = rc.readBroadcast(NUM_BASHERS+1);
			int curState = rc.readBroadcast(curBasher);		//Get the current state
			MapLocation curLoc = rc.getLocation();
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.BASHER.sensorRadiusSquared, this.theirTeam);
			//RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotType.BASHER.attackRadiusSquared, this.theirTeam);
			Boolean isSafe = (enemies.length==0);
			Boolean stateChanged = false;

			if (!isSafe) {	//There's an enemy robot nearby.  do something and attack!

				if (rc.isCoreReady()){//Can move, now find a suitable direction to move:
					RobotInfo closestEnemy = closestEnemy(enemies, curLoc);
					if (closestEnemy != null) {
						Direction d = basherMoveDir(curLoc.directionTo(closestEnemy.location));
						if (d != null) {
							rc.move(d);
						}
					}
				}
			}

			//Testing
			rc.setIndicatorString(0, "STATE: "+curState);
			rc.setIndicatorString(1, "Core Delay: "+rc.getCoreDelay());
			rc.setIndicatorString(2, "Weapon Delay: "+rc.getWeaponDelay());

			switch (curState) {
			case 0: // Basher in "rally" mode (default)
				if (rc.isCoreReady()) {
					Direction d = getMoveDir(rallyPoint);
					int distToRally = curLoc.distanceSquaredTo(rallyPoint);
					if (d != null) {
						if (rc.canMove(d)){
							rc.move(d);
						}
					} else if (distToRally > 36) {
						// if first 3 move choices are not able to be made, probably stuck.
						stateChanged = true;
						curState = randInt(1,2);	// make it randomly select between states 1&2
//						rc.setIndicatorString(1, "curState: "+curState);
//						rc.setIndicatorString(2, "cannot move forward");
						rc.broadcast(curBasher+1, distToRally);	//save how far you are from their HQ
//						below added for wall-crawler method
						rc.broadcast(curBasher+2, dirToInt(curLoc.directionTo(rallyPoint)));
						rc.broadcast(curBasher + 3, 0);	// initialize # of first tries to zero
					}
				}
				break;
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			case 1: // Follow walls & enemy radii through LEFTWISE rotations
				oldDistanceToEnemy = rc.readBroadcast(curBasher+1);
				int numLeftTries = rc.readBroadcast(curBasher+3);
				prevCrawlerDir = intToDir(rc.readBroadcast(curBasher + 2));
				rc.setIndicatorString(1, "prevCrawlerDir: "+prevCrawlerDir);

				// Base case:  if crawling got us closer to the enemy, return to normal movement
				if ((curLoc.distanceSquaredTo(rallyPoint) < oldDistanceToEnemy) || (numLeftTries > 4)) {
					rc.setIndicatorString(2, "just hit base case!");
					stateChanged=true;
					curState = 0;
				}

				//Check to see if curLoc.add(prevCrawlerDir) can be moved to (no structures or VOIDS)
				if (canCrawlTo(curLoc, prevCrawlerDir)) {
					rc.setIndicatorString(2, "canCrawl towards "+prevCrawlerDir+" and rc.isCoreReady()="+rc.isCoreReady()+" and rc.canMove="+rc.canMove(prevCrawlerDir));
					if (rc.isCoreReady() && rc.canMove(prevCrawlerDir)) {
						rc.move(prevCrawlerDir);
						Direction savedCrawlerDir = prevCrawlerDir.rotateRight().rotateRight();
						rc.broadcast(curBasher+2, dirToInt(savedCrawlerDir));
						rc.broadcast(curBasher + 3,  numLeftTries + 1);
					}
				}

				//Rotate left until we can crawl to a new spot
				Direction[] dirsL = {prevCrawlerDir, prevCrawlerDir.rotateLeft(), prevCrawlerDir.rotateLeft().rotateLeft(),
						prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft(), prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft().rotateLeft(),
						prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft().rotateLeft().rotateLeft()};

				for (int i = 0; i < dirsL.length; i++) {
					if (canCrawlTo(curLoc, dirsL[i].rotateLeft())){
						rc.setIndicatorString(2, "YES! I can crawl towards: "+dirsL[i].rotateLeft());
						if (rc.isCoreReady() && rc.canMove(dirsL[i].rotateLeft())) {
							rc.move(dirsL[i].rotateLeft());
							rc.broadcast(curBasher+2, dirToInt(dirsL[i]));	// saved the last wall or structure direction
							rc.broadcast(curBasher + 3,  0);
						} else if (rc.isCoreReady()) {	// Core is ready, can crawl, but a unit is in the way.  Right-rotating units get preference.
							Direction toHQ = curLoc.directionTo(myHQ);
							if (rc.canMove(toHQ)){
								rc.move(toHQ);
								curState = 0;
								stateChanged = true;
							}
						}
						break;
					}
				}
				break;
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			case 2: // Follow walls & enemy radii through RIGHTWISE rotations
				oldDistanceToEnemy = rc.readBroadcast(curBasher+1);
				int numRightTries = rc.readBroadcast(curBasher+3);
				prevCrawlerDir = intToDir(rc.readBroadcast(curBasher + 2));
				rc.setIndicatorString(1, "prevCrawlerDir: "+prevCrawlerDir);

				// Base case:  if crawling got us closer to the enemy, return to normal movement
				if ((curLoc.distanceSquaredTo(rallyPoint) < oldDistanceToEnemy) || (numRightTries > 4)) {
					rc.setIndicatorString(2, "just hit base case!");
					stateChanged=true;
					curState = 0;
				}

				//Check to see if curLoc.add(prevCrawlerDir) can be moved to (no structures or VOIDS)
				if (canCrawlTo(curLoc, prevCrawlerDir)) {
					rc.setIndicatorString(2, "canCrawl towards "+prevCrawlerDir+" and rc.isCoreReady()="+rc.isCoreReady()+" and rc.canMove="+rc.canMove(prevCrawlerDir));
					if (rc.isCoreReady() && rc.canMove(prevCrawlerDir)) {
						rc.move(prevCrawlerDir);
						Direction savedCrawlerDir = prevCrawlerDir.rotateLeft().rotateLeft();
						rc.broadcast(curBasher+2, dirToInt(savedCrawlerDir));
						rc.broadcast(curBasher + 3,  numRightTries + 1);
					}
				}

				//Rotate left until we can crawl to a new spot
				Direction[] dirsR = {prevCrawlerDir, prevCrawlerDir.rotateRight(), prevCrawlerDir.rotateRight().rotateRight(),
						prevCrawlerDir.rotateRight().rotateRight().rotateRight(), prevCrawlerDir.rotateRight().rotateRight().rotateRight().rotateRight(),
						prevCrawlerDir.rotateRight().rotateRight().rotateRight().rotateRight().rotateRight()};

				for (int i = 0; i < dirsR.length; i++) {
					if (canCrawlTo(curLoc, dirsR[i].rotateRight())){
						rc.setIndicatorString(2, "YES! I can crawl towards: "+dirsR[i].rotateRight());
						if (rc.isCoreReady() && rc.canMove(dirsR[i].rotateRight())) {
							rc.move(dirsR[i].rotateRight());
							rc.broadcast(curBasher+2, dirToInt(dirsR[i]));	// saved the last wall or structure direction
							rc.broadcast(curBasher + 3,  0);
						}
						break;
					}
				}
				break;
			}

			//Take care of stack
			if (stateChanged) {rc.broadcast(curBasher,curState);}	//Save changed state, if it was changed
			if (curBasher >= NUM_BASHERS+4*numBashers-2) {
				rc.broadcast(NUM_BASHERS+1, NUM_BASHERS+2);
			} else {
				rc.broadcast(NUM_BASHERS+1, curBasher+4);
			}
			rc.yield();
		}
	}


	//----- Tank -----//
	public static class Tank extends BaseBot {

		public Tank(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			int oldDistanceToEnemy;
			Direction prevCrawlerDir;

			distributeSupplies();
			rc.broadcast(TANK_ATTENDANCE, rc.readBroadcast(TANK_ATTENDANCE)+1);	//Take attendance
			int numTanks = rc.readBroadcast(NUM_TANKS);
			int curTank = rc.readBroadcast(NUM_TANKS+1);
			int curState = rc.readBroadcast(curTank);		//Get the current state
			MapLocation curLoc = rc.getLocation();
			//MapLocation curLoc = rc.getLocation();
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.TANK.sensorRadiusSquared, this.theirTeam);
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotType.TANK.attackRadiusSquared, this.theirTeam);
			Boolean isSafe = (enemies.length==0);
			Boolean stateChanged = false;

			int rallyX = rc.readBroadcast(100);
			int rallyY = rc.readBroadcast(101);
			if ((rallyX == 0) && (rallyY == 0)){
				rallyX = (int)((this.theirHQ.x+2*this.myHQ.x)/3);	// (1/3) the way away from my base (defensively safe, but not too close)
				rallyY = (int)((this.theirHQ.y+2*this.myHQ.y)/3);
			}
			MapLocation rallyPoint = new MapLocation(rallyX, rallyY);

			if (!isSafe) {	//There's an enemy robot nearby.  do something and attack!
				if (rc.isWeaponReady() && enemiesInRange.length != 0) {
					attackLeastHealthEnemy(enemiesInRange);
				}
			}

			//Testing
			rc.setIndicatorString(0, "STATE: "+curState);
			rc.setIndicatorString(1, "Core Delay: "+rc.getCoreDelay());
			rc.setIndicatorString(2, "Weapon Delay: "+rc.getWeaponDelay());

			switch (curState) {
			case 0: // Tank in "rally" mode (default)
				if (rc.isCoreReady() && (enemiesInRange.length == 0)) {
					Direction d = getMoveDir(rallyPoint);
					int distToRally = curLoc.distanceSquaredTo(rallyPoint);
					if (d != null) {
						if (rc.canMove(d)){
							rc.move(d);
						}
					} else if (distToRally > 36) {
						// if first 3 move choices are not able to be made, probably stuck.
						stateChanged = true;
						curState = randInt(1,2);	// make it randomly select between states 1&2
						rc.broadcast(curTank+1, distToRally);	//save how far you are from their HQ
//						below added for wall-crawler method
						rc.broadcast(curTank+2, dirToInt(curLoc.directionTo(rallyPoint)));
						rc.broadcast(curTank + 3, 0);	// initialize # of first tries to zero
					}
				}
				break;
				///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			case 1: // Follow walls & enemy radii through LEFTWISE rotations
				oldDistanceToEnemy = rc.readBroadcast(curTank+1);
				int numLeftTries = rc.readBroadcast(curTank+3);
				prevCrawlerDir = intToDir(rc.readBroadcast(curTank + 2));
				rc.setIndicatorString(1, "prevCrawlerDir: "+prevCrawlerDir);

				// Base case:  if crawling got us closer to the enemy, return to normal movement
				if ((curLoc.distanceSquaredTo(rallyPoint) < oldDistanceToEnemy) || (numLeftTries > 4)) {
					rc.setIndicatorString(2, "just hit base case!");
					stateChanged=true;
					curState = 0;
				}

				//Check to see if curLoc.add(prevCrawlerDir) can be moved to (no structures or VOIDS)
				if (canCrawlTo(curLoc, prevCrawlerDir)) {
					rc.setIndicatorString(2, "canCrawl towards "+prevCrawlerDir+" and rc.isCoreReady()="+rc.isCoreReady()+" and rc.canMove="+rc.canMove(prevCrawlerDir));
					if (rc.isCoreReady() && rc.canMove(prevCrawlerDir)) {
						rc.move(prevCrawlerDir);
						Direction savedCrawlerDir = prevCrawlerDir.rotateRight().rotateRight();
						rc.broadcast(curTank+2, dirToInt(savedCrawlerDir));
						rc.broadcast(curTank + 3,  numLeftTries + 1);
					}
				}

				//Rotate left until we can crawl to a new spot
				Direction[] dirsL = {prevCrawlerDir, prevCrawlerDir.rotateLeft(), prevCrawlerDir.rotateLeft().rotateLeft(),
						prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft(), prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft().rotateLeft(),
						prevCrawlerDir.rotateLeft().rotateLeft().rotateLeft().rotateLeft().rotateLeft()};

				for (int i = 0; i < dirsL.length; i++) {
					if (canCrawlTo(curLoc, dirsL[i].rotateLeft())){
						rc.setIndicatorString(2, "YES! I can crawl towards: "+dirsL[i].rotateLeft());
						if (rc.isCoreReady() && rc.canMove(dirsL[i].rotateLeft())) {
							rc.move(dirsL[i].rotateLeft());
							rc.broadcast(curTank+2, dirToInt(dirsL[i]));	// saved the last wall or structure direction
							rc.broadcast(curTank + 3,  0);
						} else if (rc.isCoreReady()) {	// Core is ready, can crawl, but a unit is in the way.  Right-rotating units get preference.
							Direction toHQ = curLoc.directionTo(myHQ);
							if (rc.canMove(toHQ)){
								rc.move(toHQ);
								curState = 0;
								stateChanged = true;
							}
						}
						break;
					}
				}
				break;
				///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			case 2: // Follow walls & enemy radii through RIGHTWISE rotations
				oldDistanceToEnemy = rc.readBroadcast(curTank+1);
				int numRightTries = rc.readBroadcast(curTank+3);
				prevCrawlerDir = intToDir(rc.readBroadcast(curTank + 2));
				rc.setIndicatorString(1, "prevCrawlerDir: "+prevCrawlerDir);

				// Base case:  if crawling got us closer to the enemy, return to normal movement
				if ((curLoc.distanceSquaredTo(rallyPoint) < oldDistanceToEnemy) || (numRightTries > 4)) {
					rc.setIndicatorString(2, "just hit base case!");
					stateChanged=true;
					curState = 0;
				}

				//Check to see if curLoc.add(prevCrawlerDir) can be moved to (no structures or VOIDS)
				if (canCrawlTo(curLoc, prevCrawlerDir)) {
					rc.setIndicatorString(2, "canCrawl towards "+prevCrawlerDir+" and rc.isCoreReady()="+rc.isCoreReady()+" and rc.canMove="+rc.canMove(prevCrawlerDir));
					if (rc.isCoreReady() && rc.canMove(prevCrawlerDir)) {
						rc.move(prevCrawlerDir);
						Direction savedCrawlerDir = prevCrawlerDir.rotateLeft().rotateLeft();
						rc.broadcast(curTank+2, dirToInt(savedCrawlerDir));
						rc.broadcast(curTank + 3,  numRightTries + 1);
					}
				}

				//Rotate left until we can crawl to a new spot
				Direction[] dirsR = {prevCrawlerDir, prevCrawlerDir.rotateRight(), prevCrawlerDir.rotateRight().rotateRight(),
						prevCrawlerDir.rotateRight().rotateRight().rotateRight(), prevCrawlerDir.rotateRight().rotateRight().rotateRight().rotateRight(),
						prevCrawlerDir.rotateRight().rotateRight().rotateRight().rotateRight().rotateRight()};

				for (int i = 0; i < dirsR.length; i++) {
					if (canCrawlTo(curLoc, dirsR[i].rotateRight())){
						rc.setIndicatorString(2, "YES! I can crawl towards: "+dirsR[i].rotateRight());
						if (rc.isCoreReady() && rc.canMove(dirsR[i].rotateRight())) {
							rc.move(dirsR[i].rotateRight());
							rc.broadcast(curTank+2, dirToInt(dirsR[i]));	// saved the last wall or structure direction
							rc.broadcast(curTank + 3,  0);
						}
						break;
					}
				}
				break;
			}

			//Take care of stack
			if (stateChanged) {rc.broadcast(curTank,curState);}	//Save changed state, if it was changed
			if (curTank >= NUM_TANKS+4*numTanks-2) {
				rc.broadcast(NUM_TANKS+1, NUM_TANKS+2);
			} else {
				rc.broadcast(NUM_TANKS+1, curTank+4);
			}
			rc.yield();
		}
	}

	//----- Tower -----//
	public static class Tower extends BaseBot {
		public Tower(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.TOWER.sensorRadiusSquared, this.theirTeam);
			if (enemies.length != 0){
				RobotInfo closestEnemy = closestEnemy(enemies, myHQ);
				rc.broadcast(RALLY_X, closestEnemy.location.x);
				rc.broadcast(RALLY_Y, closestEnemy.location.y);
				if (rc.isWeaponReady()) {
					attackLeastHealthEnemy(getEnemiesInAttackingRange(RobotType.TOWER));
				}
			}

			rc.yield();
		}
	}

	//----- Supply Depot -----//
	public static class SupplyDepot extends BaseBot {
		public SupplyDepot(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			rc.broadcast(NUM_SUPPLY_DEPOTS, rc.readBroadcast(5)+1);	//Take attendance
			rc.yield();
		}
	}
}
