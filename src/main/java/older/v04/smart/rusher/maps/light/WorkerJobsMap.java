package older.v04.smart.rusher.maps.light;

import model.*;
import older.v04.smart.rusher.Constants;
import older.v04.smart.rusher.collections.AllEntities;
import older.v04.smart.rusher.maps.EnemiesMap;
import older.v04.smart.rusher.maps.EntitiesMap;
import util.DebugInterface;
import util.Task;

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

        int minWorkers = 3;
        Set<Coordinate> buildCoordinates = new HashSet<>(128);
        for (Entity order : buildOrders.updateAndGetActiveOrders(allEntities, entitiesMap, me)) {
            if (order.getEntityType() == EntityType.RANGED_BASE) {
                minWorkers = 5;
            }
            List<Coordinate> adjacentCoordinates = order.getAdjacentCoordinates();
            buildCoordinates.addAll(adjacentCoordinates);
        }

        fillBuildOrderDistance(buildDistanceByFoot, buildCoordinates, minWorkers);
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
                    if (entity.isMy(EntityType.BUILDER_UNIT)) {
                        if (entity.getTask() == Task.IDLE) {
                            entity.setTask(i == 1 ? Task.BUILD : Task.MOVE_TO_BUILD);
                            workerCount++;
                        }
                        if (i > 2 && workerCount >= minWorkers) {
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
