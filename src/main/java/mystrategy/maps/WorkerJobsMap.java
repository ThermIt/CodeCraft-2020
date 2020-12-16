package mystrategy.maps;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import mystrategy.collections.AllEntities;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorkerJobsMap {
    private int[][] distanceByFoot;
    private int[][] distanceByFootWithObstacles;
    private EntitiesMap entitiesMap;
    private int mapSize;
    private AllEntities allEntities;
    private EnemiesMap enemiesMap;
    private DebugInterface debugInterface;

    public WorkerJobsMap(PlayerView playerView, EntitiesMap entitiesMap, AllEntities allEntities, EnemiesMap enemiesMap, DebugInterface debugInterface) {
        this.entitiesMap = entitiesMap;
        mapSize = playerView.getMapSize();
        this.allEntities = allEntities;
        this.enemiesMap = enemiesMap;
        this.debugInterface = debugInterface;
        distanceByFoot = new int[mapSize][mapSize];
        distanceByFootWithObstacles = new int[mapSize][mapSize];

        List<Coordinate> resourceCoordinates = new ArrayList<>();
        for (Entity resource : allEntities.getResources()) {
            resourceCoordinates.add(new Coordinate(resource.getPosition().getX(), resource.getPosition().getY()));
        }

        fillDistances(distanceByFoot, resourceCoordinates, false);
        fillDistances(distanceByFootWithObstacles, resourceCoordinates, true);
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
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled()) {
                    DebugCommand.Add command = new DebugCommand.Add();

                    ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(i, j), new Vec2Float(0, withObstacles ? 14 : 0), new Color(0, 0, 0, 0.5f));
                    DebugData data = new DebugData.PlacedText(coloredVertex, Integer.toString(getDistance(i, j, withObstacles)), -1, 12);
                    command.setData(data);
                    debugInterface.send(command);
                }
            }
        }
*/

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
