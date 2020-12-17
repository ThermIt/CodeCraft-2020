package mystrategy.maps.light;

import model.*;
import mystrategy.Constants;
import mystrategy.Task;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import util.DebugInterface;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public WorkerJobsMap(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities,
            EnemiesMap enemiesMap,
            Player me,
            BuildOrders buildOrders
    ) {
        this.entitiesMap = entitiesMap;
        this.mapSize = playerView.getMapSize();
        this.allEntities = allEntities;
        this.enemiesMap = enemiesMap;
        this.me = me;
        this.buildDistanceByFoot = new int[mapSize][mapSize];

        this.repair = new int[mapSize][mapSize];
        this.workers = new int[mapSize][mapSize];

        Set<Coordinate> repairEntitiesCoordinates = new HashSet<>();
        for (Entity resource : allEntities.getMyUnits()) {
//            repairEntitiesCoordinates.add(new Coordinate(resource.getPosition().getX(), resource.getPosition().getY()));
        }

        Set<Coordinate> buildCoordinates = new HashSet<>();
        for (Entity order : buildOrders.updateAndGetActiveOrders(entitiesMap, me)) {
            List<Coordinate> adjacentCoordinates = order.getAdjacentCoordinates();
            buildCoordinates.addAll(adjacentCoordinates);
        }

        fillBuildOrderDistance(buildDistanceByFoot, buildCoordinates);
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

    public int getDistanceUnsafe(int[][] distanceMap, Coordinate position) {
        return distanceMap[position.getX()][position.getY()];
    }

    private void fillBuildOrderDistance(int[][] distanceMap, Set<Coordinate> coordinateList) {
        int workerCount = 0;
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>();
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && getDistanceUnsafe(distanceMap, coordinate) == 0
                        && isPassable(coordinate)) {
                    Entity entity = entitiesMap.getEntity(coordinate);
//                    DebugInterface.print(Integer.toString(i), coordinate); // build distance
                    if (entity.isMy(EntityType.BUILDER_UNIT)) {
                        if (entity.getTask() == Task.IDLE) {
                            entity.setTask(i == 1 ? Task.BUILD : Task.MOVE_TO_BUILD);
                            workerCount++;
                        }
                        if (i > 2 && workerCount >= 3) {
                            return;
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
