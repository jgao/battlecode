package eddie;

import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	public static void run(RobotController rc) {
		BaseBot myself;

		if (rc.getType() == RobotType.HQ) {
			myself = new HQ(rc);
		} else if (rc.getType() == RobotType.BEAVER) {
			myself = new Beaver(rc);
		} else if (rc.getType() == RobotType.BARRACKS) {
			myself = new Barracks(rc);
		} else if (rc.getType() == RobotType.SOLDIER) {
			myself = new Soldier(rc);
		} else if (rc.getType() == RobotType.TOWER) {
			myself = new Tower(rc);
		} else if (rc.getType() == RobotType.MINERFACTORY) {
			myself = new MinerFactory(rc);
		} else if (rc.getType() == RobotType.MINER) {
			myself = new Miner(rc);
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
		protected MapLocation myHQ, theirHQ;
		protected Team myTeam, theirTeam;
		protected int bStratPhase;
		protected int bMinerFactories;
		protected int bBarracks;
		protected int bSoldiers;
		protected int bTankFactories;
		protected int bTanks;

		protected int beaversMax = 4;
		protected int minerFactoriesMax = 2;
		protected int minersMax = 10;
		protected int barracksMax = 2;
		protected int oreMax = 500;
		protected int soldiersMax = 25;
		protected int tankFactoriesMax = 3;
		protected int tanksMax = 8;
		protected Random rand = new Random();

		public BaseBot(RobotController rc) {
			this.rc = rc;
			this.myHQ = rc.senseHQLocation();
			this.theirHQ = rc.senseEnemyHQLocation();
			this.myTeam = rc.getTeam();
			this.theirTeam = this.myTeam.opponent();
		}

		public Direction[] getDirectionsToward(MapLocation dest) {
			Direction toDest = rc.getLocation().directionTo(dest);
			Direction[] dirs = {toDest,
				toDest.rotateLeft(), toDest.rotateRight(),
				toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight(),
				toDest.rotateRight().rotateRight().rotateRight(),
				toDest.rotateLeft().rotateLeft().rotateLeft(),
				toDest.rotateRight().rotateRight().rotateRight().rotateRight()};

			return dirs;
		}

		public Direction[] getDirectionsAway(MapLocation dest) {
			Direction toDest = rc.getLocation().directionTo(dest);
			Direction[] dirs = {toDest.rotateRight().rotateRight().rotateRight().rotateRight(),
				toDest.rotateRight().rotateRight().rotateRight(),
				toDest.rotateLeft().rotateLeft().rotateLeft(),
				toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight(),
				toDest.rotateLeft(), toDest.rotateRight(), toDest};

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

		public Direction getMoveAwayDir(MapLocation dest) {
			Direction[] dirs = getDirectionsAway(dest);
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

		public Direction getSpawnAwayDirection(RobotType type) {
			Direction[] dirs = getDirectionsAway(this.theirHQ);
			for (Direction d : dirs) {
				if (rc.canSpawn(d, type)) {
					return d;
				}
			}
			return null;
		}

		public Direction getBuildDirection(RobotType type) {
			Direction[] dirs = getDirectionsToward(this.theirHQ);
			for (Direction d : dirs) {
				if (rc.canBuild(d, type)) {
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
			}

			rc.attackLocation(toAttack);
		}

		public void beginningOfTurn() throws GameActionException {
			if (rc.senseEnemyHQLocation() != null) {
				this.theirHQ = rc.senseEnemyHQLocation();
			}
			if (rc.isCoreReady()){
				this.bStratPhase = rc.readBroadcast(0);
				this.bMinerFactories = rc.readBroadcast(1); //get miner factories
				this.bBarracks = rc.readBroadcast(2); //barracks
				this.bSoldiers = rc.readBroadcast(3); //soldiers
				this.bTankFactories = rc.readBroadcast(4); //tank factories
				this.bTanks = rc.readBroadcast(5); //tanks
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

		public int randInt(int min, int max) {
			int randomNum = this.rand.nextInt((max - min) + 1) + min;
			return randomNum;
		}
	}

	public static class HQ extends BaseBot {
		protected int beavers;

		public HQ(RobotController rc) {
			super(rc);
			this.beavers = 0;
		}

		public void execute() throws GameActionException {

			//Build phase 0-2 beavers
			if (this.bStratPhase >= 0) {
				if (rc.isCoreReady()){
					if (this.beavers < this.beaversMax && rc.getTeamOre() >= 100) {
						rc.spawn(this.getSpawnDirection(RobotType.BEAVER), RobotType.BEAVER);
						this.beavers++;
					}
				}
				if (rc.getTeamOre() >= this.oreMax && this.bStratPhase == 0) {
					rc.broadcast(0, 1); //Go to phase 1
				}
			}

			if (this.bStratPhase >= 3) {
				this.beaversMax = 6;
			}

			//Go to phase 2
			if (this.bMinerFactories >= this.minerFactoriesMax && this.bStratPhase == 1) {
				rc.broadcast(0, 2);
			}

			//Go to phase 3
			if (this.bBarracks >= this.barracksMax && this.bStratPhase == 2) {
				rc.broadcast(0, 3);
			}

			//Go to phase 4
			if (this.bSoldiers >= this.soldiersMax && this.bTanks >= this.tanksMax && this.bStratPhase == 3) {
				rc.broadcast(0, 4);
			}

			//rc.broadcast(this.stratPhase)
			rc.yield();
		}
	}

	public static class Beaver extends BaseBot {
		protected int maxDistToHQ;
		protected int minDistToHQ;

		public Beaver(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			MapLocation myLoc = rc.getLocation();

			//Phase 0 radii
			if (this.bStratPhase == 0){
				this.maxDistToHQ = 49;
				this.minDistToHQ = 16;
			} else if (this.bStratPhase >= 1) {
				//spread out
				this.minDistToHQ =25;
				this.maxDistToHQ = 100;
			}

			Direction bd;

			//Build phase 1 miner factories
			if (this.bStratPhase == 1){
				if (rc.isCoreReady()){
					if (this.bMinerFactories < this.minerFactoriesMax &&
					rc.getTeamOre() > 500){
						bd = this.getBuildDirection(RobotType.MINERFACTORY);
						if (bd != null) {
							rc.build(bd, RobotType.MINERFACTORY);
							rc.broadcast(1, this.bMinerFactories + 1);
						}
					}
				}
			} else if (this.bStratPhase == 2) { //Build phase 2 barracks
				if (rc.isCoreReady()) {
					if (this.bBarracks < this.barracksMax && rc.getTeamOre() > 300) {
						bd = this.getBuildDirection(RobotType.BARRACKS);

						if (bd != null) {
							rc.build(bd, RobotType.BARRACKS);
							rc.broadcast(2, this.bBarracks + 1);
						}

					}
				}
			} else if (this.bStratPhase == 3) { //Build phase 3 tank factories
				if (rc.isCoreReady()) {
					if (this.bTankFactories < this.tankFactoriesMax && rc.getTeamOre() > 500) {
						bd = this.getBuildDirection(RobotType.TANKFACTORY);
						if (bd != null) {
							rc.build(bd, RobotType.TANKFACTORY);
							rc.broadcast(4, this.bTankFactories + 1);
						}

					}
				}
			}

			//mining logic
			if (rc.isCoreReady()){
				if (rc.senseOre(myLoc) > 0 && myLoc.distanceSquaredTo(this.myHQ) > this.minDistToHQ){
					rc.mine();
				}
			}

			//move logic
			Direction md;
			if (rc.isCoreReady()) {
				if (myLoc.distanceSquaredTo(this.myHQ) <= myLoc.distanceSquaredTo(this.theirHQ)){
					md = this.getMoveAwayDir(this.myHQ);
					if (md != null){
						rc.move(md);
					}
				} else {
					if (myLoc.distanceSquaredTo(this.myHQ) > this.maxDistToHQ){
						md = this.getMoveDir(this.myHQ);
						if (md != null){
							rc.move(md);
						}

					} else {
						md = this.getMoveAwayDir(this.theirHQ);
						if (md != null){
							rc.move(md);
						}
					}
				}
			}

			rc.yield();
		}
	}

	public static class Barracks extends BaseBot {
		public Barracks(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {

			if (this.bStratPhase >= 4) {
				this.soldiersMax = 35;
			}

			Direction sd;

			if (this.bStratPhase >= 2) {
				if (rc.isCoreReady()) {
					if (rc.getTeamOre() > 50 && this.bSoldiers < this.soldiersMax) {
						sd = this.getSpawnDirection(RobotType.SOLDIER);
						if (sd != null){
							rc.spawn(this.getSpawnDirection(RobotType.SOLDIER), RobotType.SOLDIER);
							rc.broadcast(3, this.bSoldiers + 1);
						}
					}
				}
			}

			rc.yield();
		}
	}

	public static class Soldier extends BaseBot {
		public Soldier(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {

			RobotInfo[] enemies = this.getEnemiesInAttackingRange(RobotType.SOLDIER);

			if (rc.isCoreReady()) {
				if (rc.isWeaponReady()) {
					this.attackLeastHealthEnemy(enemies);
				}
			}

			if (enemies.length == 0) {
				Direction md;
				if (this.bStratPhase >= 4) {
					if (rc.isCoreReady()) {
						md = this.getMoveDir(this.theirHQ);
						if (md != null){
							rc.move(md);
						}
					}
				}
				if (this.bStratPhase >= 2) {
					if (rc.isCoreReady()) {
						int whichTowerToRally = this.randInt(0,5);
						md = this.getMoveDir(rc.senseTowerLocations()[whichTowerToRally]);
						if (md != null){
							rc.move(md);
						}
					}
				}
			}

			rc.yield();
		}
	}

	public static class Tower extends BaseBot {
		public Tower(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {

			if (rc.isCoreReady()) {
				if (rc.isWeaponReady()) {
					this.attackLeastHealthEnemy(this.getEnemiesInAttackingRange(RobotType.TOWER));
				}
			}

			rc.yield();
		}
	}

	public static class MinerFactory extends BaseBot {
		protected int miners;

		public MinerFactory(RobotController rc) {
			super(rc);
			this.miners = 0;
		}

		public void execute() throws GameActionException {

			if (this.bStratPhase >= 3) {
				this.minersMax = 35;
			}

			if (this.bStratPhase >= 2) {
				if (rc.isCoreReady() && rc.getTeamOre() > 50 && this.miners < this.minersMax) {
					rc.spawn(this.getSpawnAwayDirection(RobotType.MINER), RobotType.MINER);
					this.miners++;
				}
			}

			rc.yield();
		}
	}

	public static class Miner extends BaseBot {
		protected int maxDistToHQ;
		protected int minDistToHQ;

		public Miner(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
			MapLocation myLoc = rc.getLocation();

			if (this.bStratPhase <= 3) {
				this.maxDistToHQ = 500;
			} else if (this.bStratPhase >= 4) {
				this.maxDistToHQ = 9999;
			}

			//mining logic
			if (rc.isCoreReady()){
				if (rc.senseOre(myLoc) > 3 && myLoc.distanceSquaredTo(this.myHQ) > this.minDistToHQ){
					rc.mine();
				}
			}

			//move logic
			Direction md;
			if (rc.isCoreReady()) {
				int choice = this.randInt(0, 3);
				if (choice == 0){
					md = this.getMoveAwayDir(this.myHQ);
				} else {
					md = this.getMoveAwayDir(this.theirHQ);
				}

				if (md != null){
					rc.move(md);
				}				
			}

			rc.yield();
		}
	}

	public static class TankFactory extends BaseBot {
		public TankFactory(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {

			Direction sd;

			if (this.bStratPhase >= 3) {
				if (rc.isCoreReady()) {
					if (rc.getTeamOre() > 250) {
						sd = this.getSpawnDirection(RobotType.TANK);
						if (sd != null){
							rc.spawn(this.getSpawnDirection(RobotType.TANK), RobotType.TANK);
							rc.broadcast(5, this.bTanks + 1);
						}
					}
				}
			}

			rc.yield();
		}
	}

	public static class Tank extends BaseBot {
		public Tank(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {

			RobotInfo[] enemies = this.getEnemiesInAttackingRange(RobotType.TANK);

			if (rc.isCoreReady()) {
				if (rc.isWeaponReady()) {
					this.attackLeastHealthEnemy(enemies);
				}
			}

			if (enemies.length == 0) {
				Direction md;
				if (this.bStratPhase >= 4) {
					if (rc.isCoreReady()) {
						md = this.getMoveDir(this.theirHQ);
						if (md != null){
							rc.move(md);
						}
					}
				}
				if (this.bStratPhase >= 2) {
					if (rc.isCoreReady()) {
						int whichTowerToRally = this.randInt(0,5);
						md = this.getMoveDir(rc.senseTowerLocations()[whichTowerToRally]);
						if (md != null){
							rc.move(md);
						}
					}
				}
			}
			rc.yield();
		}
	}
}
