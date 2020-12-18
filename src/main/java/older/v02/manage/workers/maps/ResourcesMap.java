package older.v02.manage.workers.maps;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import mystrategy.Constants;
import older.v02.manage.workers.AllEntities;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResourcesMap {
    private int[][] distanceByFoot;
    private int[][] distanceByFootWithObstacles;
    private EntitiesMap entitiesMap;
    private int mapSize;
    private AllEntities allEntities;
    private EnemiesMap enemiesMap;

    public ResourcesMap(PlayerView playerView, EntitiesMap entitiesMap, AllEntities allEntities, EnemiesMap enemiesMap) {
        this.entitiesMap = entitiesMap;
        mapSize = playerView.getMapSize();
        this.allEntities = allEntities;
        this.enemiesMap = enemiesMap;
        distanceByFoot = new int[mapSize][mapSize];
        distanceByFootWithObstacles = new int[mapSize][mapSize];

        List<Coordinate> enemyCoordinates = new ArrayList<>();
        for (Entity resource : allEntities.getResources()) {
            enemyCoordinates.add(new Coordinate(resource.getPosition().getX(), resource.getPosition().getY()));
        }

        fillDistances(distanceByFoot, enemyCoordinates, false);
        fillDistances(distanceByFootWithObstacles, enemyCoordinates, true);
    }


    public Coordinate getMinOfTwoPositions(Coordinate old, Coordinate newPosition, boolean withObstacles) {
        if (newPosition.getX() < 0 || newPosition.getY() < 0 || newPosition.getX() >= mapSize || newPosition.getY() >= mapSize) {
            return old;
        }
        int newDistance = getDistance(newPosition, withObstacles);
        if (newDistance == 0) {
            return old;
        }
        int oldDistance = getDistance(old, withObstacles);
        if (oldDistance == 0 || newDistance < oldDistance) {
            return newPosition;
        }
        return old;
    }

    public int getDistance(Coordinate position, boolean withObstacles) {
        return getDistance(position.getX(), position.getY(), withObstacles);
    }

    public int getDistance(int x, int y, boolean withObstacles) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return withObstacles ? distanceByFootWithObstacles[x][y] : distanceByFoot[x][y];
    }

    private void fillDistances(int[][] distanceMap, List<Coordinate> coordinateList, boolean withObstacles) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            List<Coordinate> coordinateListNext = new ArrayList<>();
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.getX() >= 0 && coordinate.getX() < mapSize
                        && coordinate.getY() >= 0 && coordinate.getY() < mapSize
                        && getDistance(coordinate, withObstacles) == 0
                        && (withObstacles ? isPassableWithObstacles(coordinate) : isPassable(coordinate))) {
                    distanceMap[coordinate.getX()][coordinate.getY()] = i;
                    coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
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
    }

    private boolean isPassable(Coordinate coordinate) {
        return this.entitiesMap.isPassable(coordinate) || this.entitiesMap.getIsResource(coordinate);
    }

    private boolean isPassableWithObstacles(Coordinate coordinate) {
        return this.entitiesMap.isEmpty(coordinate) || this.entitiesMap.getIsResource(coordinate);
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
        Coordinate position = from;
        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition, true);
        }

        if (!Objects.equals(position, from)) {
            return position;
        }

        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition, false);
        }

        return position;
    }
}
