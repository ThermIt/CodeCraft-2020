package mystrategy.maps.light;

import common.Constants;
import common.Decision;
import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.collections.SingleVisitCoordinateSet;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import util.DebugInterface;
import util.Task;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HarvestJobsMap {
    private int mapSize;
    private int[][] harvest;
    private int[][] workers;

    private int[][] resourceDistanceByFoot;
    private int[][] resourceDistanceByFootWithObstacles;
    private int[][] resourceDistanceByFootAllResources;
    private boolean[][] takenSpace;
    private EntitiesMap entitiesMap;
    private EnemiesMap enemiesMap;
    private Player me;
    private PlayerView playerView;
    private WorkerJobsMap otherJobs;

    public HarvestJobsMap(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities,
            EnemiesMap enemiesMap,
            Player me,
            VirtualResources resources,
            WorkerJobsMap jobs) {
        this.playerView = playerView;
        otherJobs = jobs;
        resources.checkTick(playerView);
        this.entitiesMap = entitiesMap;
        this.mapSize = playerView.getMapSize();
        this.enemiesMap = enemiesMap;
        this.me = me;
        this.resourceDistanceByFoot = new int[mapSize][mapSize];
        this.resourceDistanceByFootWithObstacles = new int[mapSize][mapSize];
        this.resourceDistanceByFootAllResources = new int[mapSize][mapSize];

        this.takenSpace = new boolean[mapSize][mapSize];

        this.harvest = new int[mapSize][mapSize];
        this.workers = new int[mapSize][mapSize];

        SingleVisitCoordinateSet restrictedResourceCoordinates = new SingleVisitCoordinateSet();
        SingleVisitCoordinateSet restrictedResourceCoordinatesWithObstacles = new SingleVisitCoordinateSet();
        SingleVisitCoordinateSet allResourceCoordinates = new SingleVisitCoordinateSet();

        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                int resourceCount = resources.getResourceCount(i, j);
                if (resourceCount == 0) {
                    continue;
                }
//                DebugInterface.print(resourceCount, i, j);

                Coordinate location = new Coordinate(i, j);
                List<Coordinate> adjacentList = location.getAdjacentList();

                adjacentList.stream().filter(entitiesMap::isPassable)
                        .forEach(loc -> {
                            harvest[loc.getX()][loc.getY()] += resourceCount;
                            Entity entity = entitiesMap.getEntity(loc);
                            if (!entity.isMy(EntityType.BUILDER_UNIT)) {
                                allResourceCoordinates.add(loc); // without workers on the spot
//                                DebugInterface.print("-", loc);
                            }
                        });

                // next section only for non-mined patches
                int nearWorkersCount = (int) adjacentList.stream().filter(pos -> entitiesMap.getEntity(pos).isMy(EntityType.BUILDER_UNIT)).count();
                if (nearWorkersCount > 0) { // remove patches with workers nearby
                    workers[location.getX()][location.getY()] = nearWorkersCount;
                    continue;
                }

                // only safe untaken spots
                adjacentList.stream().filter(entitiesMap::isPassable)
                        .forEach(loc -> {
                            harvest[loc.getX()][loc.getY()] += resourceCount;
                            Entity entity = entitiesMap.getEntity(loc);
                            if (!entity.isMy(EntityType.BUILDER_UNIT) && enemiesMap.getDangerLevel(loc) == 0) {
                                restrictedResourceCoordinates.add(loc);
                                restrictedResourceCoordinatesWithObstacles.add(loc);
//                                DebugInterface.print("+", loc);
                            }
                        });
            }
        }
/*
        for (Entity resource : allEntities.getResources()) {
        }
*/

        fillDistances(resourceDistanceByFoot, restrictedResourceCoordinates, false);
        fillDistances(resourceDistanceByFootWithObstacles, restrictedResourceCoordinatesWithObstacles, true);
        fillDistances(resourceDistanceByFootAllResources, allResourceCoordinates, false);

/*
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 80; j++) {
                if (resourceDistanceByFoot[i][j] > 0 && resourceDistanceByFoot[i][j] < 50) {
                    DebugInterface.println(resourceDistanceByFoot[i][j], i, j, 0);
                }
                if (resourceDistanceByFootWithObstacles[i][j] > 0 && resourceDistanceByFootWithObstacles[i][j] < 50) {
                    DebugInterface.println(resourceDistanceByFootWithObstacles[i][j], i, j, 1);
                }
                if (resourceDistanceByFootAllResources[i][j] > 0 && resourceDistanceByFootAllResources[i][j] < 50) {
                    DebugInterface.println(resourceDistanceByFootAllResources[i][j], i, j, 2);
                }
            }
        }
*/
    }

/*
    public void updateFreeSpaceMaskForHarvesters() {
        takenSpace = new boolean[mapSize][mapSize];
        for (Entity entity : playerView.getEntities()) {
            if (entity.isMy(EntityType.BUILDER_UNIT)) {
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
*/


    public Entity getResource(Coordinate position) {
        return position
                .getAdjacentList()
                .stream()
                .sorted(Comparator.comparingInt(pos -> workers[pos.getX()][pos.getY()]))
                .map(entitiesMap::getEntity)
                .filter(ent -> ent.getEntityType() == EntityType.RESOURCE && ent.getHealthAfterDamage() > 0)
                .findAny()
                .orElse(null);
    }


    public Coordinate getMinOfTwoPositions(Coordinate old, Coordinate newPosition, int[][] distanceMap) {
        if (newPosition.isOutOfBounds()) {
            return old;
        }
        int newDistance = getDistance(newPosition, distanceMap);
        if (newDistance == 0) {
            return old;
        }
        int oldDistance = getDistance(old, distanceMap);
        if (oldDistance == 0 || newDistance < oldDistance) {
            return newPosition;
        }
        if (newDistance == oldDistance) {
            if (takenSpace[newPosition.getX()][newPosition.getY()]) {
                return old;
            }
            if (entitiesMap.isEmpty(newPosition)) {
                return newPosition;
            }
        }
        return old;
    }

    public int getDistance(Coordinate position, int[][] distanceMap) {
        if (position.isOutOfBounds()) {
            return 0;
        }

        return distanceMap[position.getX()][position.getY()];
    }

    private void fillDistances(int[][] distanceMap, SingleVisitCoordinateSet coordinateList, boolean withObstacles) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && getDistance(coordinate, distanceMap) == 0
                        && (withObstacles ? isPassableWithObstacles(coordinate) : isPassable(coordinate))) {
                    distanceMap[coordinate.getX()][coordinate.getY()] = i;
                    coordinateList.addOnNextStep(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                    coordinateList.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                    coordinateList.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                    coordinateList.addOnNextStep(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
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
    }

    private boolean isPassable(Coordinate coordinate) {
        return this.entitiesMap.isPassable(coordinate);
    }

    private boolean isPassableWithObstacles(Coordinate coordinate) {
        return this.entitiesMap.isEmpty(coordinate) || this.entitiesMap.getEntity(coordinate).getEntityType() == EntityType.BUILDER_UNIT;
    }

    public Coordinate getPositionClosestToResource(Coordinate from) {
        return getPositionClosestToResource(from, from.getAdjacentList());
    }

    public Coordinate getPositionClosestToResource(Coordinate from, List<Coordinate> coordinateList) {
        List<Coordinate> newList = coordinateList.stream()
                .filter(pos -> !pos.isOutOfBounds())
                .filter(pos -> enemiesMap.getDangerLevel(pos) == 0)
                .collect(Collectors.toList());

        if (!newList.isEmpty()) {
            coordinateList = newList;
        }
        coordinateList = coordinateList.stream()
                .filter(pos -> !pos.isOutOfBounds())
                .filter(pos -> !takenSpace[pos.getX()][pos.getY()])
                .collect(Collectors.toList());

        // TODO: sort by enemiesMap.getDangerLevel(pos)

/*
        Coordinate coordinate = newList.stream()
                .filter(o -> entitiesMap.isEmpty(o))
                .max(Comparator.comparingInt(o -> harvest[o.getX()][o.getY()]))
                .orElse(from);
        if (harvest[coordinate.getX()][coordinate.getY()] > 0) { // go to max resources
            return coordinate;
        }
*/

        Coordinate position = from;
        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition, resourceDistanceByFootWithObstacles);
        }

        if (!Objects.equals(position, from)) {
            return position;
        }

        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition, resourceDistanceByFoot);
        }

        if (!Objects.equals(position, from)) {
            return position;
        }

        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition, resourceDistanceByFootAllResources);
        }

        return position;
    }

    public void decideMoveForBuilderUnit(Entity unit) {
        if (unit.getTask() == Task.RUN_FOOLS) {
            unit.setBuildAction(null); // drop all legacy tasks and run
            unit.setAttackAction(null);
            unit.setRepairAction(null);
        }
        if (unit.getBuildAction() != null || unit.getRepairAction() != null || unit.getAttackAction() != null) {
//            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
// *           DebugInterface.print("0 - attacking", unit.getPosition().getX(), unit.getPosition().getY());
            unit.setMoveAction(null);
            unit.setMoveDecision(Decision.DECIDED);
            return;
        }
        if (unit.getMoveDecision() == Decision.DECIDED) {
            return;
        }
        unit.setMoveDecision(Decision.DECIDING);
        Coordinate moveTo = getMoveTo(unit);
        DebugInterface.println("moveTo", moveTo, 0);

        // untie knots
        Entity otherUnit = entitiesMap.getEntity(moveTo.getX(), moveTo.getY());
        if (!Objects.equals(moveTo, unit.getPosition()) // not self
                && otherUnit.isMy(EntityType.BUILDER_UNIT)
                && otherUnit.getMoveDecision() != Decision.DECIDED // DECIDING marks his place as taken while calculating other units
        ) {
            if (unit.getTask() == Task.RUN_FOOLS) {
                otherUnit.setTask(Task.RUN_FOOLS);
            }
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
            decideMoveForBuilderUnit(otherUnit);
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = false;
            if (takenSpace[moveTo.getX()][moveTo.getY()]) {
                takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true; // so it tries to move anyway
                decideMoveForBuilderUnit(unit);
                takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = false;
                return;
            }
        }

        // force push
        if (!Objects.equals(moveTo, unit.getPosition()) // not self
                && otherUnit.isMy(EntityType.BUILDER_UNIT)
                && (otherUnit.getBuildAction() != null || otherUnit.getRepairAction() != null || otherUnit.getAttackAction() != null)
        ) {
            otherUnit.setBuildAction(null);
            otherUnit.setAttackAction(null);
            otherUnit.setRepairAction(null);
            otherUnit.setTask(Task.IDLE);
            otherUnit.setMoveDecision(Decision.DECIDING);
//            DebugInterface.print("X", moveTo);
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true; // so it tries to move anyway
            takenSpace[otherUnit.getPosition().getX()][unit.getPosition().getY()] = true; // so it tries to move anyway
            decideMoveForBuilderUnit(otherUnit);
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = false;
            takenSpace[otherUnit.getPosition().getX()][unit.getPosition().getY()] = false;
        }

        if (!Objects.equals(moveTo, unit.getPosition()) && otherUnit.getEntityType() != EntityType.RESOURCE) { // move
            takenSpace[moveTo.getX()][moveTo.getY()] = true;
// *            DebugInterface.print("0 - move", moveTo.getX(), moveTo.getY());
            MoveAction moveAction = new MoveAction(moveTo, true, true);
            unit.setMoveAction(moveAction);
        } else if (otherUnit.getEntityType() == EntityType.RESOURCE) { // take self owned place in case of resource
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
// *            DebugInterface.print("0 - cleaning", unit.getPosition().getX(), unit.getPosition().getY());
            MoveAction moveAction = new MoveAction(moveTo, true, true);
            unit.setMoveAction(moveAction);
        } else { // not move
            takenSpace[unit.getPosition().getX()][unit.getPosition().getY()] = true;
// *            DebugInterface.print("0 - not moving", unit.getPosition().getX(), unit.getPosition().getY());
            unit.setMoveAction(null);
        }
        unit.setMoveDecision(Decision.DECIDED);
    }

    public Coordinate getMoveTo(Entity unit) {
        Coordinate moveTo = unit.getPosition();
        if (unit.getTask() == Task.BUILD) {
            moveTo = unit.getPosition();
        } else if (unit.getTask() == Task.MOVE_TO_BUILD) {
            moveTo = otherJobs.getPositionClosestToBuild(unit.getPosition(), takenSpace);
        } else if (unit.getTask() != Task.RUN_FOOLS) { // idle workers
            moveTo = getPositionClosestToResource(unit.getPosition());
        } else if (unit.getTask() == Task.RUN_FOOLS) {
            moveTo = otherJobs.getRunDirections(unit.getPosition(), takenSpace); // костыль же
        }
        return moveTo;
    }

    public void printTakenMap() {
/*
        if (DebugInterface.isDebugEnabled()) {
            for (int i = 0; i < 80; i++) {
                for (int j = 0; j < 80; j++) {
                    if (takenSpace[i][j]) {
                        DebugInterface.println("X", i, j,0);
                    }
                }
            }
        }
*/
    }
}
