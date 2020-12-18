package mystrategy.maps.light;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import mystrategy.Constants;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EntitiesMap;
import util.DebugInterface;

import java.util.HashSet;
import java.util.Iterator;
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

//        Set<Coordinate> myDominance = allEntities.getMyEntities().stream().map(Entity::getPosition).collect(Collectors.toSet());

        fillDistances(enemyDominance);
    }

    private boolean isPassable(Coordinate coordinate) {
        Entity entity = this.entitiesMap.getEntity(coordinate);
        return !(entity.isBuilding() && entity.isMy());
    }

    private void fillDistances(Set<Coordinate> coordinateList) {
//        private int[][] dominanceMap;
//        private int[][] delayFuseForCalculation;
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
        if (getDistance(newPosition) == 0) {
            return old;
        }
        if (getDistance(newPosition) < getDistance(old)) {
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

}
