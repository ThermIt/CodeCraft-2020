package older.v04.smart.rusher.maps.light;

import model.Coordinate;
import model.Entity;
import model.EntityType;
import model.PlayerView;
import older.v04.smart.rusher.Constants;
import older.v04.smart.rusher.collections.AllEntities;
import older.v04.smart.rusher.maps.EntitiesMap;
import util.DebugInterface;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WarMap {

    private Set<Coordinate> enemyBuildingLocations = new HashSet<>(128);
    private Set<Coordinate> enemyUnitLocations = new HashSet<>(128);
    private int[][] dominanceMap;
    private int[][] delayFuseForCalculation;
    private int mapSize;
    private EntitiesMap entitiesMap;
    private AllEntities allEntities;
    private VisibilityMap visibility;
    private VirtualResources resources;

    public WarMap(VisibilityMap visibility, VirtualResources resources) {
        this.visibility = visibility;
        this.resources = resources;
    }

    public void init(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities
    ) {
        this.entitiesMap = entitiesMap;
        this.allEntities = allEntities;

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

        Iterator<Coordinate> buildingsIterator = enemyBuildingLocations.iterator();
        while (buildingsIterator.hasNext()) {
            Coordinate next = buildingsIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (!entity.isBuilding() || entity.isMy()) {
                    buildingsIterator.remove();
                }
            }
        }
        Iterator<Coordinate> unitsIterator = enemyUnitLocations.iterator();
        while (unitsIterator.hasNext()) {
            Coordinate next = unitsIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (!entity.isUnit() || entity.isMy()) {
                    unitsIterator.remove();
                }
            }
        }

        for (Entity building : allEntities.getEnemyBuildings()) {
            enemyBuildingLocations.add(building.getPosition());
        }

        for (Entity unit : allEntities.getEnemyUnits()) {
            enemyBuildingLocations.add(unit.getPosition());
        }

        dominanceMap = new int[mapSize][mapSize];
        delayFuseForCalculation = new int[mapSize][mapSize];

        Set<Coordinate> enemyDominance = new HashSet<>();
        enemyDominance.addAll(enemyUnitLocations);
        enemyDominance.addAll(enemyBuildingLocations);

        fillDistances(enemyDominance);
    }

    private boolean isPassable(Coordinate coordinate) {
        Entity entity = this.entitiesMap.getEntity(coordinate);
        return !(entity.isBuilding() && entity.isMy()) && !entity.isMy(EntityType.BUILDER_UNIT);
    }

    private void fillDistances(Set<Coordinate> coordinateList) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.getX() >= 0 && coordinate.getX() < mapSize
                        && coordinate.getY() >= 0 && coordinate.getY() < mapSize
                        && dominanceMap[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 2; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        coordinateListNext.add(coordinate);
                    } else {
                        dominanceMap[coordinate.getX()][coordinate.getY()] = i;
                        coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
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
    }

    public Coordinate getMinOfTwoPositions(Coordinate old, Coordinate newPosition) {
        if (newPosition.isOutOfBounds()) {
            return old;
        }
        int newDistance = getDistance(newPosition);
        if (newDistance == 0) {
            return old;
        }
        int oldDistance = getDistance(old);
        if (newDistance < oldDistance) {
            return newPosition;
        }

        return old;
    }

    public int getDistance(Coordinate position) {
        return getDistance(position.getX(), position.getY());
    }

    public int getDistance(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return dominanceMap[x][y];
    }

    public Coordinate getPositionClosestToEnemy(Coordinate from) {
        int radius = 1;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                from = getMinOfTwoPositions(from, new Coordinate(from.getX() + i, from.getY() + j));
            }
        }
        return from;
    }

    public Coordinate getPositionClosestToEnemy(Coordinate from, List<Coordinate> coordinateList) {
        Coordinate position = from;
        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition);
        }
        return position;
    }
}
