package mystrategy.maps.light;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;

import java.util.*;
import java.util.stream.Collectors;

public class HarvestJobsMap {
    private int mapSize;
    private int[][] harvest;
    private int[][] workers;

    private int[][] resourceDistanceByFoot;
    private int[][] resourceDistanceByFootWithObstacles;
    private int[][] resourceDistanceByFootAllResources;
    private EntitiesMap entitiesMap;
    private EnemiesMap enemiesMap;
    private Player me;

    public HarvestJobsMap(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities,
            EnemiesMap enemiesMap,
            Player me
    ) {
        this.entitiesMap = entitiesMap;
        this.mapSize = playerView.getMapSize();
        this.enemiesMap = enemiesMap;
        this.me = me;
        this.resourceDistanceByFoot = new int[mapSize][mapSize];
        this.resourceDistanceByFootWithObstacles = new int[mapSize][mapSize];
        this.resourceDistanceByFootAllResources = new int[mapSize][mapSize];

        this.harvest = new int[mapSize][mapSize];
        this.workers = new int[mapSize][mapSize];

        Set<Coordinate> restrictedResourceCoordinates = new HashSet<>();
        Set<Coordinate> allResourceCoordinates = new HashSet<>();
        for (Entity resource : allEntities.getResources()) {
            Coordinate location = new Coordinate(resource.getPosition().getX(), resource.getPosition().getY());
            List<Coordinate> adjacentList = location.getAdjacentList();

            adjacentList.stream().filter(entitiesMap::isPassable)
                    .forEach(loc -> {
                        harvest[loc.getX()][loc.getY()] += resource.getHealth();
                        Entity entity = entitiesMap.getEntity(loc);
                        if (!entity.isMy(EntityType.BUILDER_UNIT)) {
                            allResourceCoordinates.add(loc); // without workers on the spot
                        }
                    });

            // next section only for non-mined patches
            int nearWorkersCount = (int) adjacentList.stream().filter(pos -> entitiesMap.getEntity(pos).isMy(EntityType.BUILDER_UNIT)).count();
            if (nearWorkersCount > 0) { // remove patches with workers nearby
                workers[location.getX()][location.getY()] = nearWorkersCount;
                continue;
            }

            adjacentList.stream().filter(entitiesMap::isPassable)
                    .forEach(loc -> {
                        harvest[loc.getX()][loc.getY()] += resource.getHealth();
                        Entity entity = entitiesMap.getEntity(loc);
                        if (!entity.isMy(EntityType.BUILDER_UNIT)) {
                            restrictedResourceCoordinates.add(loc);
                        }
                    });
        }

        fillDistances(resourceDistanceByFoot, restrictedResourceCoordinates, false);
        fillDistances(resourceDistanceByFootWithObstacles, restrictedResourceCoordinates, true);
        fillDistances(resourceDistanceByFootAllResources, restrictedResourceCoordinates, false);
    }

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
        return old;
    }

    public int getDistance(Coordinate position, int[][] distanceMap) {
        if (position.isOutOfBounds()) {
            return 0;
        }

        return distanceMap[position.getX()][position.getY()];
    }

    private void fillDistances(int[][] distanceMap, Set<Coordinate> coordinateList, boolean withObstacles) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>();
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.getX() >= 0 && coordinate.getX() < mapSize
                        && coordinate.getY() >= 0 && coordinate.getY() < mapSize
                        && getDistance(coordinate, distanceMap) == 0
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

        Coordinate coordinate = newList.stream()
                .filter(o -> entitiesMap.isEmpty(o))
                .max(Comparator.comparingInt(o -> harvest[o.getX()][o.getY()]))
                .orElse(from);
        if (harvest[coordinate.getX()][coordinate.getY()] > 0) { // go to max resources
            return coordinate;
        }

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
}
