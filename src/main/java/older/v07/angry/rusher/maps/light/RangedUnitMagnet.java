package older.v07.angry.rusher.maps.light;

import common.Constants;
import model.Coordinate;
import model.Entity;
import model.EntityType;
import mystrategy.collections.AllEntities;
import mystrategy.collections.SingleVisitCoordinateSet;
import mystrategy.maps.EntitiesMap;
import mystrategy.maps.light.VirtualResources;
import mystrategy.maps.light.VisibilityMap;
import util.DebugInterface;
import util.Initializer;

import java.util.ArrayList;
import java.util.List;

public class RangedUnitMagnet {
    private mystrategy.maps.light.VisibilityMap visibility;
    private EntitiesMap entitiesMap;
    private AllEntities entities;
    private VirtualResources resources;
    private int[][] distance;
    private int mapSize = 80;
    private SingleVisitCoordinateSet waveCoordinates;
    private List<Coordinate> coordinates = new ArrayList<>();


    public RangedUnitMagnet(
            VisibilityMap visibility,
            EntitiesMap entitiesMap,
            AllEntities entities,
            VirtualResources resources
    ) {
        this.visibility = visibility;
        this.entitiesMap = entitiesMap;
        this.entities = entities;
        this.resources = resources;
        this.distance = new int[Initializer.getMapSize()][Initializer.getMapSize()];

        waveCoordinates = new SingleVisitCoordinateSet();

        if (entities.getEnemyWorkers().size() == 0)
        for (Entity enemyUnit : entities.getEnemyRangedUnits()) {
            waveCoordinates.add(enemyUnit.getPosition());
        }

        for (int i = 1; i < 3; i++) { // 2 initial steps for rangers
            for (Coordinate wave : waveCoordinates) {
                distance[wave.getX()][wave.getY()] = i;
                prettyprint(i, wave);
                List<Coordinate> adjacentList = wave.getAdjacentList();
                for (Coordinate next : adjacentList) {
                    waveCoordinates.addOnNextStep(next);
                }
            }
            waveCoordinates.nextStep();
        }

        for (Entity enemyUnit : entities.getEnemyWorkers()) {
            waveCoordinates.add(enemyUnit.getPosition());
        }

        if (entities.getEnemyWorkers().size() == 0)
        for (Entity enemyUnit : entities.getEnemyMeleeUnits()) {
            waveCoordinates.add(enemyUnit.getPosition());
        }

        if (entities.getEnemyWorkers().size() == 0)
            for (Entity enemyBuilding : entities.getEnemyBuildings()) {
            if (enemyBuilding.getEntityType() != EntityType.TURRET) {
                for (Coordinate position : enemyBuilding.getInsideCoordinates()) {
                    waveCoordinates.add(position);
                }
            }
        }

        for (int i = 3; i < 8; i++) { // 5 steps for rangers
            for (Coordinate wave : waveCoordinates) {
                distance[wave.getX()][wave.getY()] = i;
                prettyprint(i, wave);
                List<Coordinate> adjacentList = wave.getAdjacentList();
                for (Coordinate next : adjacentList) {
                    waveCoordinates.addOnNextStep(next);
                }
            }
            waveCoordinates.nextStep();
        }

        for (Coordinate pos : waveCoordinates) {
            if (!entitiesMap.getEntity(pos).isMy()) {
                coordinates.add(pos);
            }
        }

        for (Coordinate pos : coordinates) {
            DebugInterface.println("X", pos, 0);
        }
    }

/*
    public Iterable<Coordinate> getCoordinates() {
        return coordinates;
    }
*/

    public void addAll(Iterable<Coordinate> coordinates) {
        for (Coordinate pos : coordinates) {
            waveCoordinates.add(pos);
        }
    }

    private boolean isPassable(Coordinate coordinate) {
        Entity entity = this.entitiesMap.getEntity(coordinate);
        return !(entity.isBuilding() && entity.isMy())/* && !entity.isMy(EntityType.BUILDER_UNIT)*/;
    }

    private boolean isBuilder(Coordinate coordinate) {
        return this.entitiesMap.getEntity(coordinate).isMy(EntityType.BUILDER_UNIT);
    }

    public void fillDistances() {
        // TODO: stop when all rangers captured
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];

        for (int i = 8; !waveCoordinates.isEmpty(); i++) {
            for (Coordinate coordinate : waveCoordinates) {
                if (coordinate.isInBounds()
                        && distance[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        waveCoordinates.addOnNextStepByForce(coordinate);
                    } else {
                        distance[coordinate.getX()][coordinate.getY()] = i;
                        prettyprint(i, coordinate);
                        // do not attract to taken places and workers
                        if (!isBuilder(coordinate) && !entitiesMap.getEntity(coordinate).isMy(EntityType.RANGED_UNIT)) {
                            waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            waveCoordinates.nextStep();

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
                    DebugInterface.print(enemyDistanceMap[i][j], i, j);
                }
            }
        }
*/
    }

    public void prettyprint(int i, Coordinate coordinate) {
/*
        if (i <= 16) {
            DebugInterface.println(i, coordinate, 1);
        }
*/
    }

    public int getDistanceToEnemy(Coordinate position) {
        return getDistanceToEnemy(position.getX(), position.getY());
    }

    public int getDistanceToEnemy(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return distance[x][y];
    }

}
