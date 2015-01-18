package team305;
import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	static Random rand;

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

		public Direction[] getDirectionsToward(MapLocation dest) {
			Direction toDest = rc.getLocation().directionTo(dest);
			Direction[] dirs = {toDest,
					toDest.rotateLeft(), toDest.rotateRight(),
					toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};

			return dirs;
		}

		public Direction getMoveDir(MapLocation dest) {
			Direction[] dirs = getDirectionsToward(dest);
			for (Direction d : dirs) {
				if (rc.canMove(d)) {
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

		public Direction getRandomDirection() {
			int firstTry = rc.getID()%9;
			for (int i = 0; i < 9; i++) {
				Direction d = Direction.values()[(i+firstTry)%9];
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
			int seige = rc.readBroadcast(SIEGE);
			int seigeTime = rc.readBroadcast(SIEGE_TIME);
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
			if ((numMinerFactories == 1) && (numBarracks == 1) && (numTankFactories == 1) && (numSoldiers >= 1) && (gameStage != 3)){
				gameStage = 3;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			// GAME STAGE 4 //
			if ((numMinerFactories == 1) && (numBarracks == 2) && (numTankFactories == 2) && (numTanks >= 1) && (gameStage != 4)){
				gameStage = 4;
				rc.broadcast(GAME_STAGE, gameStage);
			}
			///////////////////////////////////////////////////////////////////////////////////////////////////
			//Testing
			rc.setIndicatorString(0, "numSoldiers: "+numSoldiers);
			rc.setIndicatorString(1, "numBeavers: "+numBeavers);
			rc.setIndicatorString(2, "Gamestage: "+gameStage);

			
			
			

			//Build structures

			// BUILD SUPPLY DEPOTS
			if (numSupplyDepots < Math.round((numBeavers+numMiners+numSoldiers+numTanks)/10)){
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
			if ((gameStage==2 && numTankFactories < 1) || (gameStage==3 && numTankFactories < 2)) {
				if (rc.getTeamOre() > RobotType.TANKFACTORY.oreCost){
					builderBeaver = NUM_BEAVERS + 2*numBeavers;	//first try the closest beaver!
					rc.broadcast(builderBeaver, 6); 	// tell first beaver to go into "build-tank-factory"-mode (3)
				}
			}


			//Attack commands
			if ((numSoldiers+numTanks > 20) && seige == 0) {
				rc.broadcast(100, this.theirHQ.x);
				rc.broadcast(101, this.theirHQ.y);
				rc.broadcast(10, 1);	// We are now seiging on enemy base
			}
			if ((seige==1 && numTanks < 4) || (seige==1 && seigeTime >= 100)) {
				rc.broadcast(100, (int)((this.theirHQ.x+2*this.myHQ.x)/3));
				rc.broadcast(101, (int)((this.theirHQ.y+2*this.myHQ.y)/3));	// Rally around home base
				seige = 0;
				rc.broadcast(10,seige);
				rc.broadcast(11, 0);
			}
			if (seige == 1){
				rc.broadcast(11, seigeTime+1);
			}


			//Fire at nearby enemies
			if (rc.isWeaponReady()) {
				attackLeastHealthEnemy(getEnemiesInAttackingRange(RobotType.TOWER));
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
			rc.setIndicatorString(0, "curState: "+curState);

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
							rc.move(getRandomDirection());
						}
					}
				}
				break;

			case 1: // Beaver in "explore" mode (goes to "mine" if lots of ore found, or "run" if enemy found)
				if (rc.isCoreReady()) {
					rc.move(getRandomDirection());	// Currently set to move in random direction,  will FIX later
					staticTime = 0;
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
						rc.build(newDir, RobotType.MINERFACTORY);
//						rc.broadcast(SAVED_ORE, rc.readBroadcast(51)-RobotType.MINERFACTORY.oreCost); // stop saving this amount of money
						rc.broadcast(BUILD_TURN_MINER_FACTORY, Clock.getRoundNum()+RobotType.MINERFACTORY.buildTurns+1);	// report time at which MinerFactory should be completed
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) {
					rc.move(getRandomDirection());	// no directions available, but core free to use for a move
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
						rc.build(newBarracksDir, RobotType.BARRACKS);
//						rc.broadcast(SAVED_ORE, rc.readBroadcast(51)-RobotType.BARRACKS.oreCost); // stop saving this amount of money
						rc.broadcast(BUILD_TURN_BARRACKS, Clock.getRoundNum()+RobotType.BARRACKS.buildTurns+1);	// report time at which Barracks should be completed
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) {
					rc.move(getRandomDirection());	// no directions available, but core free to use for a move
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
						rc.build(newSupplyDir, RobotType.SUPPLYDEPOT);
//						rc.broadcast(SAVED_ORE, rc.readBroadcast(51)-RobotType.SUPPLYDEPOT.oreCost); // stop saving this amount of money
						rc.broadcast(BUILD_TURN_SUPPLY_DEPOT, Clock.getRoundNum()+RobotType.SUPPLYDEPOT.buildTurns+1);	// report time at which SupplyDepot should be completed
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) {
					rc.move(getRandomDirection());	// no directions available, but core free to use for a move
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
						rc.build(newTankDir, RobotType.TANKFACTORY);
//						rc.broadcast(SAVED_ORE, rc.readBroadcast(51)-RobotType.TANKFACTORY.oreCost); // stop saving this amount of money
						rc.broadcast(BUILD_TURN_TANK_FACTORY, Clock.getRoundNum()+RobotType.TANKFACTORY.buildTurns+1);	// report time at which tankFactory should be completed
						curState = 0;		// completed building, change state
						stateChanged = true;
					} else {
						rc.broadcast(BUILDER_BEAVER, builderBeaver+2); //increment so this will still be builderBeaver next turn (just waiting on core)
					}
				} else if (rc.isCoreReady()) {
					rc.move(getRandomDirection());	// no directions available, but core free to use for a move
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

			switch (curState){
			case 0:	// Miner in "mine" mode
				if (rc.isCoreReady()) {
					double oreHere = rc.senseOre(curLoc);
					if (oreHere > 10 && (staticTime < 8)) {	// Lot's of ore here, keep mining!
						rc.mine();
						staticTime++;
					} else if (oreHere > 0) { // Look for more ore if only getting 0.1 ore per mine
						Boolean stickAround = true;
						for (Direction d : Direction.values()) {
							if (rc.senseOre(curLoc.add(d)) > oreHere && rc.canMove(d) && rc.isCoreReady()) {
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
					rc.move(getRandomDirection());	// Currently set to move in random direction,  will FIX later
					staticTime = 0;
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
			int numTanks = rc.readBroadcast(NUM_TANKS);
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.SOLDIER.oreCost) && numSoldiers < 35){
				Direction newDir = getSpawnDirection(RobotType.SOLDIER);
				if (newDir != null) {
					rc.spawn(newDir, RobotType.SOLDIER);
					if (rc.readBroadcast(NUM_SOLDIERS+1)==0){
						rc.broadcast(NUM_SOLDIERS+1, NUM_SOLDIERS+2);	//Initialize the pointer (if it hasn't been done yet)
					}
					rc.broadcast(NUM_SOLDIERS, numSoldiers + 1);	//increment numSoldiers
				}
			}

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
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.TANK.oreCost)){	//prioritize tank building - don't save
				Direction newDir = getSpawnDirection(RobotType.TANK);
				if (newDir != null) {
					rc.spawn(newDir, RobotType.TANK);
					if (rc.readBroadcast(NUM_TANKS+1)==0){
						rc.broadcast(NUM_TANKS+1, NUM_TANKS+2);	//Initialize the pointer (if it hasn't been done yet)
					}
					rc.broadcast(NUM_TANKS, numTanks + 1);	//increment numTanks
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
			int numMiners = rc.readBroadcast(NUM_MINERS);
			if (rc.isCoreReady() && rc.getTeamOre() > (RobotType.MINER.oreCost) && numMiners < 20){
				Direction newDir = getSpawnDirection(RobotType.MINER);
				if (newDir != null) {
					rc.spawn(newDir, RobotType.MINER);
					if (rc.readBroadcast(NUM_MINERS+1)==0){
						rc.broadcast(NUM_MINERS+1, NUM_MINERS+2);	//Initialize the pointer (if it hasn't been done yet)
					}
					rc.broadcast(NUM_MINERS, numMiners + 1);	//increment numMiners
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
			distributeSupplies();
			rc.broadcast(SOLDIER_ATTENDANCE, rc.readBroadcast(SOLDIER_ATTENDANCE)+1);	//Take attendance
			int numSoldiers = rc.readBroadcast(NUM_SOLDIERS);
			int curSoldier = rc.readBroadcast(NUM_SOLDIERS+1);
			int curState = rc.readBroadcast(curSoldier);		//Get the current state
			MapLocation curLoc = rc.getLocation();
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, this.theirTeam);
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, this.theirTeam);
			Boolean isSafe = (enemies.length==0);
			Boolean stateChanged = false;

			if (!isSafe) {	//There's an enemy robot nearby.  do something and attack!
				//        		curState = 2;
				//        		stateChanged=true;
				if (rc.isWeaponReady() && enemiesInRange.length != 0) {
					attackLeastHealthEnemy(enemiesInRange);
				} //else if (rc.isCoreReady()) {
				//			Direction d = getMoveDir(leastHealthEnemy(enemies).location);
				//			if (rc.isCoreReady() && d != null){
				//				rc.move(d);
				//			}
				//		}
			}

			//Testing
			rc.setIndicatorString(0, "STATE: "+curState);
			rc.setIndicatorString(1, "Core Delay: "+rc.getCoreDelay());
			rc.setIndicatorString(2, "Weapon Delay: "+rc.getWeaponDelay());

			switch (curState) {
			case 0: // Soldier in "rally" mode (default)
				if (rc.isCoreReady() && (enemiesInRange.length == 0)) {
					int rallyX = rc.readBroadcast(100);
					int rallyY = rc.readBroadcast(101);
					MapLocation rallyPoint;
					if ((rallyX == 0) && (rallyY == 0)){
						rallyX = (int)((this.theirHQ.x+2*this.myHQ.x)/3);	// (1/3) the way away from my base (defensively safe, but not too close)
						rallyY = (int)((this.theirHQ.y+2*this.myHQ.y)/3);
					}
					rallyPoint = new MapLocation(rallyX, rallyY);
					Direction d = getMoveDir(rallyPoint);
					if (rc.canMove(d) && (d != null)) {
						rc.move(d);
					}
				}
				break;
			}

			//Take care of stack
			if (stateChanged) {rc.broadcast(curSoldier,curState);}	//Save changed state, if it was changed
			if (curSoldier >= NUM_SOLDIERS+2*numSoldiers) {
				rc.broadcast(NUM_SOLDIERS+1, NUM_SOLDIERS+2);
			} else {
				rc.broadcast(NUM_SOLDIERS+1, curSoldier+2);
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
			distributeSupplies();
			rc.broadcast(TANK_ATTENDANCE, rc.readBroadcast(TANK_ATTENDANCE)+1);	//Take attendance
			int numTanks = rc.readBroadcast(NUM_TANKS);
			int curTank = rc.readBroadcast(NUM_TANKS+1);
			int curState = rc.readBroadcast(curTank);		//Get the current state
			//MapLocation curLoc = rc.getLocation();
			RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.TANK.sensorRadiusSquared, this.theirTeam);
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotType.TANK.attackRadiusSquared, this.theirTeam);
			Boolean isSafe = (enemies.length==0);
			Boolean stateChanged = false;

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
					int rallyX = rc.readBroadcast(100);
					int rallyY = rc.readBroadcast(101);
					MapLocation rallyPoint;
					if ((rallyX == 0) && (rallyY == 0)){
						rallyX = (int)((this.theirHQ.x+2*this.myHQ.x)/3);	// (1/3) the way away from my base (defensively safe, but not too close)
						rallyY = (int)((this.theirHQ.y+2*this.myHQ.y)/3);
					}
					rallyPoint = new MapLocation(rallyX, rallyY);
					Direction d = getMoveDir(rallyPoint);
					if (rc.canMove(d) && (d != null)) {
						rc.move(d);
					}
				}
				break;
			}

			//Take care of stack
			if (stateChanged) {rc.broadcast(curTank,curState);}	//Save changed state, if it was changed
			if (curTank >= NUM_TANKS+2*numTanks) {
				rc.broadcast(NUM_TANKS+1, NUM_TANKS+2);
			} else {
				rc.broadcast(NUM_TANKS+1, curTank+2);
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
			if (rc.isWeaponReady()) {
				attackLeastHealthEnemy(getEnemiesInAttackingRange(RobotType.TOWER));
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