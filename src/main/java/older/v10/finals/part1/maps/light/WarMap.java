package older.v10.finals.part1.maps.light;

import common.Constants;
import common.Decision;
import model.*;
import older.v10.finals.part1.collections.AllEntities;
import older.v10.finals.part1.collections.SingleVisitCoordinateSet;
import older.v10.finals.part1.maps.EnemiesMap;
import older.v10.finals.part1.maps.EntitiesMap;
import older.v10.finals.part1.utils.Team;
import util.DebugInterface;

import java.util.*;

public class WarMap {

    public static final int RANGER_RANGE = 5;
    private Set<Coordinate> enemyBuildingLocations = new HashSet<>(128);
    private Set<Coordinate> enemyUnitLocations = new HashSet<>(128);
    private Set<Coordinate> enemyWorkerLocations = new HashSet<>(128);
    private Set<Coordinate> myAttackersLocations = new HashSet<>(128);
    private int[][] enemyDistanceMap;
    private int[][] enemyWorkerDistanceMap;
    private int[][] dominanceMap;
    private boolean[][] takenSpace;
    private int mapSize;
    private PlayerView playerView;
    private EntitiesMap entitiesMap;
    private EnemiesMap enemiesMap;
    private AllEntities allEntities;
    private VisibilityMap visibility;
    private VirtualResources resources;
    private RangedUnitMagnet rangedUnitMagnet;
    private int tick;

    public WarMap(VisibilityMap visibility, VirtualResources resources) {
        this.visibility = visibility;
        this.resources = resources;
    }

    public int getTick() {
        return tick;
    }

    public void checkTick(PlayerView playerView) {
        if (getTick() != playerView.getCurrentTick()) {
            throw new RuntimeException("visibility is not initialized");
        }
    }

    public void init(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities,
            EnemiesMap enemiesMap,
            AllEntities entities
    ) {
        tick = playerView.getCurrentTick();
        this.playerView = playerView;
        this.entitiesMap = entitiesMap;
        this.allEntities = allEntities;
        this.enemiesMap = enemiesMap;

        visibility.checkTick(playerView);
        resources.checkTick(playerView);

        if (mapSize == 0) {
            if (playerView.isOneOnOne()) {
                enemyBuildingLocations.add(new Coordinate(72, 72));
            } else {
                enemyBuildingLocations.add(new Coordinate(72, 72));
                enemyBuildingLocations.add(new Coordinate(7, 72));
                enemyBuildingLocations.add(new Coordinate(72, 7));
            }
            mapSize = playerView.getMapSize();
        }

        myAttackersLocations = new HashSet<>(128);
        for (Entity attacker : allEntities.getMyAttackers()) {
            myAttackersLocations.add(attacker.getPosition());
        }


        Iterator<Coordinate> buildingsIterator = enemyBuildingLocations.iterator();
        while (buildingsIterator.hasNext()) {
            Coordinate next = buildingsIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (!entity.isBuilding() || entity.isMy()) {
                    buildingsIterator.remove(); // clean old instances
                }
            }
        }
        Iterator<Coordinate> unitsIterator = enemyUnitLocations.iterator();
        while (unitsIterator.hasNext()) {
            Coordinate next = unitsIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (!entity.isUnit() || entity.isMy()) { // clean old instances
                    unitsIterator.remove();
                }
            }
        }
        Iterator<Coordinate> workersIterator = enemyWorkerLocations.iterator();
        while (workersIterator.hasNext()) {
            Coordinate next = workersIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (entity.getEntityType() != EntityType.BUILDER_UNIT || entity.isMy()) { // clean old instances
                    workersIterator.remove();
                }
            }
        }

        for (Entity building : allEntities.getEnemyBuildings()) {
            if (building.getEntityType() != EntityType.TURRET) { // little hack
                enemyBuildingLocations.add(building.getPosition());
            }
        }

        for (Entity unit : allEntities.getEnemyUnits()) {
            enemyUnitLocations.add(unit.getPosition());
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                enemyWorkerLocations.add(unit.getPosition());
            }
        }

        SingleVisitCoordinateSet enemyDominance = new SingleVisitCoordinateSet();
        enemyDominance.addAll(enemyUnitLocations);
        enemyDominance.addAll(enemyBuildingLocations);

//        Set<Coordinate> myDominance = allEntities.getMyEntities().stream().map(Entity::getPosition).collect(Collectors.toSet());

        fillEnemyDistances(enemyDominance);
        fillMyAttackersDistances(myAttackersLocations);
        fillEnemyWorkerDistances(enemyWorkerLocations);

        this.rangedUnitMagnet = new RangedUnitMagnet(visibility, entitiesMap, entities, resources);
        rangedUnitMagnet.addAll(enemyUnitLocations);
        rangedUnitMagnet.addAll(enemyBuildingLocations);
        rangedUnitMagnet.addAllHarass(enemyBuildingLocations);
        rangedUnitMagnet.fillDistances();
        rangedUnitMagnet.fillDistancesHarass();
    }

    private boolean isPassable(Coordinate coordinate) {
        Entity entity = this.entitiesMap.getEntity(coordinate);
        return !(entity.isBuilding() && entity.isMy())/* && !entity.isMy(EntityType.BUILDER_UNIT)*/;
    }

    private boolean isBuilder(Coordinate coordinate) {
        return this.entitiesMap.getEntity(coordinate).isMy(EntityType.BUILDER_UNIT);
    }

    private void fillEnemyWorkerDistances(Set<Coordinate> coordinateList) {
        enemyWorkerDistanceMap = new int[mapSize][mapSize];
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && enemyWorkerDistanceMap[coordinate.getX()][coordinate.getY()] == 0
                        && (isPassable(coordinate) || i <= RANGER_RANGE + 1)) {
                    if (i <= RANGER_RANGE + 1) {
                        // skip attack range
                        enemyWorkerDistanceMap[coordinate.getX()][coordinate.getY()] = i;
                        coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        continue;
                    }
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        coordinateListNext.add(coordinate);
                    } else {
                        enemyWorkerDistanceMap[coordinate.getX()][coordinate.getY()] = i;
                        if (!isBuilder(coordinate)) {
                            coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            coordinateList = coordinateListNext;

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled()) {
                    if (enemyWorkerDistanceMap[i][j] == 0) {
                        DebugInterface.print(enemyDistanceMap[i][j], i, j);
                    } else {
                        DebugInterface.print(Math.min(enemyWorkerDistanceMap[i][j], enemyDistanceMap[i][j]), i, j);
                    }
                }
            }
        }
*/
    }

    private void fillEnemyDistances(SingleVisitCoordinateSet coordinateList) {
        enemyDistanceMap = new int[mapSize][mapSize];
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && enemyDistanceMap[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        coordinateList.addOnNextStepByForce(coordinate);
                    } else {
                        enemyDistanceMap[coordinate.getX()][coordinate.getY()] = i;
                        if (!isBuilder(coordinate)) {
                            coordinateList.addOnNextStep(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            coordinateList.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            coordinateList.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            coordinateList.addOnNextStep(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            coordinateList.nextStep();

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled()) {
                    DebugInterface.print(enemyDistanceMap[i][j], i, j);
                }
            }
        }
*/
    }

    private void fillMyAttackersDistances(Set<Coordinate> coordinateList) {
        dominanceMap = new int[mapSize][mapSize];
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && dominanceMap[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        coordinateListNext.add(coordinate);
                    } else {
                        dominanceMap[coordinate.getX()][coordinate.getY()] = i;
                        if (!isBuilder(coordinate)) {
                            coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            coordinateList = coordinateListNext;

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled()) {
                    DebugInterface.print(dominanceMap[i][j], i, j);
                }
            }
        }
*/
    }

    public Coordinate getMinOfTwoPositions(Coordinate old, Coordinate newPosition) {
        if (newPosition.isOutOfBounds()) {
            return old;
        }
        int newDistance = getDistanceToEnemy(newPosition);
        if (newDistance == 0) {
            return old;
        }
        int oldDistance = getDistanceToEnemy(old);
        if (newDistance < oldDistance) {
            return newPosition;
        }

        if (newDistance == oldDistance && entitiesMap.isEmpty(newPosition)) { // scatters a bit
            return newPosition;
        }
        return old;
    }

    public Coordinate getMinOfTwoPositionsForRangedUnit(Coordinate old, Coordinate newPosition, Team teamNumber) {
        if (newPosition.isOutOfBounds()) {
            return old;
        }
        if (old == null) {
            return newPosition;
        }
        int newDistance = rangedUnitMagnet.getDistanceToEnemy(newPosition, teamNumber);
        if (newDistance == 0) {
            return old;
        }
        int oldDistance = rangedUnitMagnet.getDistanceToEnemy(old, teamNumber);
        if (newDistance < oldDistance) {
            return newPosition;
        }

/*
        if (newDistance == oldDistance && !entitiesMap.isEmpty(old)) { // scatters a bit RANGED
            return newPosition;
        }
*/
        if (teamNumber == Team.MAIN && newDistance == oldDistance
                && Math.abs(newPosition.getY() - newPosition.getX()) < Math.abs(old.getY() - old.getX())) { // stream in the middle
            return newPosition;
        }
        if (teamNumber == Team.HARASSERS && newDistance == oldDistance
                && newPosition.getY() > old.getY()) { // top
            return newPosition;
        }
        if (teamNumber == Team.HARASSERS2 && newDistance == oldDistance
                && newPosition.getX() > old.getX()) { // bottom
            return newPosition;
        }

        return old;
    }

    public int getDistanceToEnemy(Coordinate position) {
        return getDistanceToEnemy(position.getX(), position.getY());
    }

    public int getDistanceToEnemy(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return enemyDistanceMap[x][y];
    }

    public int getDistanceToEnemyWorker(Coordinate position) {
        return getDistanceToEnemyWorker(position.getX(), position.getY());
    }

    public int getDistanceToEnemyWorker(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return enemyWorkerDistanceMap[x][y];
    }

    public int getDistanceToGoodGuys(Coordinate position) {
        return getDistanceToGoodGuys(position.getX(), position.getY());
    }

    public int getDistanceToGoodGuys(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return dominanceMap[x][y];
    }

    public Coordinate getPositionClosestToEnemy(Entity fromUnit) {
        Coordinate fromPosition = fromUnit.getPosition();
        List<Coordinate> possibleMoves = fromPosition.getAdjacentListWithSelf();
        for (Coordinate newPosition : possibleMoves) {
            if (newPosition.isInBounds()) {
                fromPosition = getMinOfTwoPositions(fromPosition, newPosition);
            }
        }
/*
        int radius = 1;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                from = getMinOfTwoPositions(from, new Coordinate(from.getX() + i, from.getY() + j));
            }
        }
*/
        return fromPosition;
    }

    public Coordinate getPositionClosestToForRangedUnit(Entity fromUnit) {
        Team teamNumber;
//        teamNumber = fromUnit.getId() % 41 > 20 ? Team.HARASSERS : Team.MAIN;
        switch ((fromUnit.getId() % 41) % 3) {
            case 1:
                teamNumber = Team.HARASSERS;
                break;
            case 2:
                teamNumber = Team.HARASSERS2;
                break;
            default:
                teamNumber = Team.MAIN;
                break;
        }
        if (fromUnit.getEntityType() == EntityType.BUILDER_UNIT) {
            teamNumber = Team.HARASSERS;
        }

        Coordinate closestCandidate = null;
        List<Coordinate> possibleMoves = fromUnit.getPosition().getAdjacentListWithSelf();
        for (Coordinate newPosition : possibleMoves) {
            if (newPosition.isInBounds()
                    && (enemiesMap.getDamageOnNextTick(newPosition) < fromUnit.getHealth()/* || fromUnit.getHealth() <= 5*/)
                    && !takenSpace[newPosition.getX()][newPosition.getY()]) {
                closestCandidate = getMinOfTwoPositionsForRangedUnit(closestCandidate, newPosition, teamNumber);
            }
        }
        return closestCandidate == null ? fromUnit.getPosition() : closestCandidate;
    }

    public Coordinate getPositionClosestToEnemy(Coordinate from, List<Coordinate> coordinateList) {
        Coordinate position = from;
        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition);
        }
        return position;
    }

    public void updateFreeSpaceMaskForRangedUnits() {
        takenSpace = new boolean[mapSize][mapSize];
        for (Entity entity : playerView.getEntities()) {
            if (entity.isMy(EntityType.RANGED_UNIT)) {
                continue;
            }
            if (entity.getEntityType() == EntityType.RESOURCE) {
//                DebugInterface.print("-", entity.getPosition().getX(), entity.getPosition().getY());
                continue;
            }
            if (entity.getMoveAction() != null && entity.getAttackAction() == null && entity.getRepairAction() == null && entity.getBuildAction() == null) {
                takenSpace[entity.getMoveAction().getTarget().getX()][entity.getMoveAction().getTarget().getY()] = true;
//                DebugInterface.print("X", entity.getMoveAction().getTarget().getX(), entity.getMoveAction().getTarget().getY());
            } else {
                int size = entity.getProperties().getSize();
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        takenSpace[entity.getPosition().getX() + i][entity.getPosition().getY() + j] = true;
//                        DebugInterface.print("X", entity.getPosition().getX() + i, entity.getPosition().getY() + j);
                    }
                }
                if (entity.getBuildAction() != null) {
                    // ignore for now, does not matter on the field of battle
                }
            }
        }
    }

    public void decideMoveForRangedUnit(Entity unit) {
        if (unit.getAttackAction() != null) {
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
// *           DebugInterface.print("0 - attacking", unit.getPosition().getX(), unit.getPosition().getY());
            unit.setMoveAction(null);
            unit.setMoveDecision(Decision.DECIDED);
            return;
        }
        if (unit.getMoveDecision() == Decision.DECIDED) {
            return;
        }
        unit.setMoveDecision(Decision.DECIDING);
        Coordinate moveTo = getPositionClosestToForRangedUnit(unit);

        Entity otherUnit = entitiesMap.getEntity(moveTo.getX(), moveTo.getY());
        if (!Objects.equals(moveTo, unit.getPosition()) // not self
                && otherUnit.isMy(EntityType.RANGED_UNIT)
                && otherUnit.getMoveDecision() != Decision.DECIDED // DECIDING marks his place as taken
        ) {
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
            decideMoveForRangedUnit(otherUnit);
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = false;
            if (takenSpace[moveTo.getX()][moveTo.getY()]) {
                takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true; // so it tries to move anyway
                decideMoveForRangedUnit(unit);
                takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = false;
                return;
            }
        }

        if (!Objects.equals(moveTo, unit.getPosition()) && otherUnit.getEntityType() != EntityType.RESOURCE) {
            takenSpace[moveTo.getX()][moveTo.getY()] = true;
// *            DebugInterface.print("0 - move", moveTo.getX(), moveTo.getY());
            MoveAction moveAction = new MoveAction(moveTo, true, true);
            unit.setMoveAction(moveAction);
        } else if (otherUnit.getEntityType() == EntityType.RESOURCE) {
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
// *            DebugInterface.print("0 - cleaning", unit.getPosition().getX(), unit.getPosition().getY());
            MoveAction moveAction = new MoveAction(moveTo, true, true);
            unit.setMoveAction(moveAction);
        } else {
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
// *            DebugInterface.print("0 - not moving", unit.getPosition().getX(), unit.getPosition().getY());
            unit.setMoveAction(null);
        }
        unit.setMoveDecision(Decision.DECIDED);
    }
}
