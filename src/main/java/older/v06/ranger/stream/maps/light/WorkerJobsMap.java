package older.v06.ranger.stream.maps.light;

import common.Constants;
import model.*;
import older.v06.ranger.stream.collections.AllEntities;
import older.v06.ranger.stream.maps.EnemiesMap;
import older.v06.ranger.stream.maps.EntitiesMap;
import util.DebugInterface;
import util.Task;

import java.util.*;
import java.util.stream.Collectors;

public class WorkerJobsMap {
    private int mapSize;
    private int[][] repair;
    private int[][] workers;
    private int[][] build;

    private int[][] buildDistanceByFoot;
    private EntitiesMap entitiesMap;
    private AllEntities allEntities;
    private EnemiesMap enemiesMap;
    private Player me;
    private WarMap warMap;

    public WorkerJobsMap(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities,
            EnemiesMap enemiesMap,
            Player me,
            BuildOrders buildOrders,
            WarMap warMap
    ) {
        warMap.checkTick(playerView);
        this.warMap = warMap;
        this.entitiesMap = entitiesMap;
        this.mapSize = playerView.getMapSize();
        this.allEntities = allEntities;
        this.enemiesMap = enemiesMap;
        this.me = me;
        this.buildDistanceByFoot = new int[mapSize][mapSize];

        this.repair = new int[mapSize][mapSize];
        this.workers = new int[mapSize][mapSize];

/*
        Set<Coordinate> repairEntitiesCoordinates = new HashSet<>(128);
        for (Entity resource : allEntities.getMyUnits()) {
//            repairEntitiesCoordinates.add(new Coordinate(resource.getPosition().getX(), resource.getPosition().getY()));
        }
*/

        for (Entity worker : allEntities.getMyWorkers()) {
            if (enemiesMap.getDangerLevel(worker.getPosition()) > 0) {
                markRun(worker);
            }
        }

        int minWorkers = 3;
        Set<Coordinate> buildCoordinates = new HashSet<>(128);
        for (Entity order : buildOrders.updateAndGetActiveOrders(allEntities, entitiesMap, me)) {
            if (order.getEntityType() == EntityType.RANGED_BASE || order.getEntityType() == EntityType.MELEE_BASE) {
                minWorkers = 10;
            }
            List<Coordinate> adjacentCoordinates = order.getAdjacentCoordinates();
            buildCoordinates.addAll(adjacentCoordinates);
        }

        fillBuildOrderDistance(buildDistanceByFoot, buildCoordinates, minWorkers);
/*
        if (DebugInterface.isDebugEnabled()) {
            for (Coordinate pos:buildCoordinates) {
                    DebugInterface.print("X", pos.getX(), pos.getY());
            }
//            for (int i = 0; i < mapSize; i++) {
//                for (int j = 0; j < mapSize; j++) {
//                    DebugInterface.print(Integer.toString(distanceByFoot[i][j]), i, j);
//                }
//            }
        }
*/
    }

    public void markRun(Entity worker) {
        if (worker.getTask() == Task.RUN_FOOLS || !worker.isMy(EntityType.BUILDER_UNIT)) {
            return;
        }
        worker.setTask(Task.RUN_FOOLS);
        Coordinate runTo = getRunDirections(worker.getPosition());
        worker.setMoveAction(new MoveAction(runTo, false, true));
        Entity blockingEntity = entitiesMap.getEntity(runTo);
        if (blockingEntity.isMy(EntityType.BUILDER_UNIT)) {
            markRun(blockingEntity);
        }
    }

    private Coordinate getRunDirections(Coordinate from) {
        Coordinate result = from;

        List<Coordinate> possibleRunLocations = from.getAdjacentList().stream()
                .filter(pos -> entitiesMap.isPassable(pos))
                .filter(pos -> {
                    MoveAction otherMoveAction = entitiesMap.getEntity(pos).getMoveAction();
                    return otherMoveAction == null || !Objects.equals(otherMoveAction.getTarget(), from);
                })
                .collect(Collectors.toList());
        int minDanger = possibleRunLocations.stream()
                .map(pos -> enemiesMap.getDangerLevel(pos))
                .min(Integer::compareTo)
                .orElse(enemiesMap.getDangerLevel(from));
        List<Coordinate> lowestDangerPassablePoints = possibleRunLocations.stream()
                .filter(pos -> enemiesMap.getDangerLevel(pos) == minDanger)
                .collect(Collectors.toList());

        if (lowestDangerPassablePoints.size() > 0 && !lowestDangerPassablePoints.contains(result)) {
            result = lowestDangerPassablePoints.get(0);
        }

        int maxDominance = lowestDangerPassablePoints.stream()
                .map(pos -> warMap.getDistanceToGoodGuys(pos))
                .min(Integer::compareTo)
                .orElse(warMap.getDistanceToGoodGuys(from));
        List<Coordinate> lowestDangerMaxDominancePassablePoints = possibleRunLocations.stream()
                .filter(pos -> warMap.getDistanceToGoodGuys(pos) == maxDominance)
                .collect(Collectors.toList());

        if (lowestDangerMaxDominancePassablePoints.size() > 0 && !lowestDangerMaxDominancePassablePoints.contains(result)) {
            result = lowestDangerMaxDominancePassablePoints.get(0);
        }

        int maxDistance = lowestDangerMaxDominancePassablePoints.stream()
                .map(pos -> warMap.getDistanceToEnemy(pos))
                .max(Integer::compareTo)
                .orElse(warMap.getDistanceToEnemy(from));
        List<Coordinate> lowestDangerMaxDistancePassablePoints = possibleRunLocations.stream()
                .filter(pos -> warMap.getDistanceToEnemy(pos) == maxDistance)
                .collect(Collectors.toList());

        if (lowestDangerMaxDistancePassablePoints.size() > 0 && !lowestDangerMaxDistancePassablePoints.contains(result)) {
            result = lowestDangerMaxDistancePassablePoints.get(0);
        }

        List<Coordinate> lowestDangerMaxDistanceEmptyPoints = lowestDangerMaxDistancePassablePoints.stream()
                .filter(entitiesMap::isEmpty)
                .collect(Collectors.toList());

        if (lowestDangerMaxDistanceEmptyPoints.size() > 0 && !lowestDangerMaxDistanceEmptyPoints.contains(result)) {
            result = lowestDangerMaxDistanceEmptyPoints.get(0);
        }

        return result;
    }

    public int getDistanceUnsafe(int[][] distanceMap, Coordinate position) {
        return distanceMap[position.getX()][position.getY()];
    }

    private void fillBuildOrderDistance(int[][] distanceMap, Set<Coordinate> coordinateList, int minWorkers) {
        int workerCount = 0;
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && getDistanceUnsafe(distanceMap, coordinate) == 0
                        && isPassable(coordinate)) {
                    Entity entity = entitiesMap.getEntity(coordinate);
//                    DebugInterface.print(Integer.toString(i), coordinate); // build distance
                    if (entity.isMy(EntityType.BUILDER_UNIT)) {
                        if (i > 2 && workerCount >= minWorkers) {
                            return;
                        }
                        if (entity.getTask() == Task.IDLE) {
                            entity.setTask(i == 1 ? Task.BUILD : Task.MOVE_TO_BUILD);
                            workerCount++;
                        }
                        if (entity.getTask() == Task.BUILD) {
                            continue;
                        }
                    }
                    distanceMap[coordinate.getX()][coordinate.getY()] = i;
                    coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                }
            }

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }

            coordinateList = coordinateListNext;
        }
    }

    private boolean isPassable(Coordinate coordinate) {
        return this.entitiesMap.isPassable(coordinate);
    }

    private boolean isPassableWithObstacles(Coordinate coordinate) {
        return this.entitiesMap.isEmpty(coordinate);
    }

    public Coordinate getPositionClosestToBuild(Coordinate position) {
        return position.getAdjacentListWithSelf().stream()
                .filter(pos -> enemiesMap.getDangerLevel(pos) == 0
                        && buildDistanceByFoot[pos.getX()][pos.getY()] != 0)
                .min(Comparator.comparingInt(o -> buildDistanceByFoot[o.getX()][o.getY()])).orElse(null);
    }
}
