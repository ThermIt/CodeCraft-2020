package mystrategy.maps.light;

import model.Coordinate;
import model.Entity;
import model.EntityType;
import model.PlayerView;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import util.DebugInterface;

import java.util.*;
import java.util.stream.Collectors;

public class WorkerJobsMap {
    private int mapSize;
    private int[][] repair;
    private int[][] harvest;
    private int[][] workers;
    private int[][] build;

    private int[][] distanceByFoot;
    private int[][] distanceByFootWithObstacles;
    private EntitiesMap entitiesMap;
    private AllEntities allEntities;
    private EnemiesMap enemiesMap;
    private DebugInterface debugInterface;

    public WorkerJobsMap(PlayerView playerView, EntitiesMap entitiesMap, AllEntities allEntities, EnemiesMap enemiesMap, DebugInterface debugInterface) {
        this.entitiesMap = entitiesMap;
        this.mapSize = playerView.getMapSize();
        this.allEntities = allEntities;
        this.enemiesMap = enemiesMap;
        this.debugInterface = debugInterface;
        this.distanceByFoot = new int[mapSize][mapSize];
        this.distanceByFootWithObstacles = new int[mapSize][mapSize];

        this.repair = new int[mapSize][mapSize];
        this.harvest = new int[mapSize][mapSize];
        this.workers = new int[mapSize][mapSize];

        Set<Coordinate> resourceCoordinates = new HashSet<>();
        for (Entity resource : allEntities.getResources()) {
            Coordinate location = new Coordinate(resource.getPosition().getX(), resource.getPosition().getY());
            List<Coordinate> adjacentList = location.getAdjacentList();

            int nearWorkersCount = (int) adjacentList.stream().filter(pos -> entitiesMap.getEntity(pos).isMy(EntityType.BUILDER_UNIT)).count();

            if (nearWorkersCount > 0) {
                workers[location.getX()][location.getY()] = nearWorkersCount;
                continue;
            }

            adjacentList.stream().filter(entitiesMap::isPassable)
                    .forEach(loc -> {
                        harvest[loc.getX()][loc.getY()] += resource.getHealth();
                        Entity entity = entitiesMap.getEntity(loc);
                        if (!entity.isMy(EntityType.BUILDER_UNIT)) {
                            resourceCoordinates.add(loc);
                        }
                    });
        }

        List<Coordinate> rapairEntitiesCoordinates = new ArrayList<>();
        for (Entity resource : allEntities.getResources()) {
            resourceCoordinates.add(new Coordinate(resource.getPosition().getX(), resource.getPosition().getY()));
        }

        List<Coordinate> buildCoordinates = new ArrayList<>();
        for (Entity resource : allEntities.getResources()) {
            resourceCoordinates.add(new Coordinate(resource.getPosition().getX(), resource.getPosition().getY()));
        }

        fillDistances(distanceByFoot, resourceCoordinates, false);
        fillDistances(distanceByFootWithObstacles, resourceCoordinates, true);
/*
        if (DebugInterface.isDebugEnabled()) {
            for (int i = 0; i < mapSize; i++) {
                for (int j = 0; j < mapSize; j++) {
                    DebugInterface.print(Integer.toString(distanceByFoot[i][j]), i, j);
                }
            }
        }
*/
    }

    public Entity getResource(Coordinate position) {
        return position
                .getAdjacentList()
                .stream()
                .sorted(Comparator.comparingInt(pos -> workers[pos.getX()][pos.getY()]))
                .map(entitiesMap::getEntity)
                .filter(ent -> ent.getEntityType() == EntityType.RESOURCE)
                .findAny()
                .orElse(null);
    }


    public Coordinate getMinOfTwoPositions(Coordinate old, Coordinate newPosition, boolean withObstacles) {
        if (newPosition.getX() < 0 || newPosition.getY() < 0 || newPosition.getX() >= mapSize || newPosition.getY() >= mapSize) {
            return old;
        }
/*
        if (harvest[old.getX()][old.getY()] != 0) {
            if ()
        }
*/
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

    private void fillDistances(int[][] distanceMap, Set<Coordinate> coordinateList, boolean withObstacles) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>();
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
        return this.entitiesMap.isPassable(coordinate);
    }

    private boolean isPassableWithObstacles(Coordinate coordinate) {
        return this.entitiesMap.isEmpty(coordinate);
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


        Coordinate coordinate = newList.stream().max((Comparator.comparingInt(o -> harvest[o.getX()][o.getY()]))).get();
        if (harvest[coordinate.getX()][coordinate.getY()] > 0) { // go ro max resources
            return coordinate;
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
