package older.v09.finals1.maps.light;

import common.Constants;
import model.Coordinate;
import model.Entity;
import model.EntityType;
import older.v09.finals1.collections.AllEntities;
import older.v09.finals1.collections.SingleVisitCoordinateSet;
import older.v09.finals1.utils.Team;
import older.v09.finals1.maps.EntitiesMap;
import util.DebugInterface;
import util.Initializer;

import java.util.List;

public class RangedUnitMagnet {
    private VisibilityMap visibility;
    private EntitiesMap entitiesMap;
    private AllEntities entities;
    private VirtualResources resources;
    private int[][] distance;
    private int[][] distanceHarass;
    private int mapSize = 80;
    private SingleVisitCoordinateSet waveCoordinates;
    private SingleVisitCoordinateSet waveCoordinatesHarass;
//    private List<Coordinate> coordinates = new ArrayList<>();


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
        this.distanceHarass = new int[Initializer.getMapSize()][Initializer.getMapSize()];

        waveCoordinates = new SingleVisitCoordinateSet();
        waveCoordinatesHarass = new SingleVisitCoordinateSet();

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
            waveCoordinatesHarass.add(enemyUnit.getPosition());
        }

        for (Entity enemyUnit : entities.getEnemyMeleeUnits()) {
            waveCoordinates.add(enemyUnit.getPosition());
        }

        for (Entity enemyBuilding : entities.getEnemyBuildings()) {
            if (enemyBuilding.getEntityType() != EntityType.TURRET) {
                for (Coordinate position : enemyBuilding.getInsideCoordinates()) {
                    waveCoordinates.add(position);
                    waveCoordinatesHarass.add(position);
                }
            }
        }

        for (int i = 3; i < 8; i++) { // 5 steps for everyone
            for (Coordinate wave : waveCoordinates) {
                distance[wave.getX()][wave.getY()] = i;
                prettyprint(i, wave);
                List<Coordinate> adjacentList = wave.getAdjacentList();
                for (Coordinate next : adjacentList) {
                    waveCoordinates.addOnNextStep(next);
                }
            }
            waveCoordinates.nextStep();


            for (Coordinate wave : waveCoordinatesHarass) {
                distanceHarass[wave.getX()][wave.getY()] = i;
                prettyprint(i, wave);
                List<Coordinate> adjacentList = wave.getAdjacentList();
                for (Coordinate next : adjacentList) {
                    waveCoordinatesHarass.addOnNextStep(next);
                }
            }
            waveCoordinatesHarass.nextStep();
        }

/*
        for (Coordinate pos : waveCoordinates) {
            if (!entitiesMap.getEntity(pos).isMy()) {
                coordinates.add(pos);
            }
        }
*/

/*
        for (Coordinate pos : coordinates) {
            DebugInterface.println("X", pos, 0);
        }
*/
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

    public void addAllHarass(Iterable<Coordinate> coordinates) {
        for (Coordinate pos : coordinates) {
            waveCoordinatesHarass.add(pos);
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
                    DebugInterface.println(distance[i][j], i, j, 1);
                }
            }
        }
*/
    }

    public void fillDistancesHarass() {
        // TODO: stop when all rangers captured
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];

        for (int i = 8; !waveCoordinatesHarass.isEmpty(); i++) {
            for (Coordinate coordinate : waveCoordinatesHarass) {
                if (coordinate.isInBounds()
                        && distanceHarass[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        waveCoordinatesHarass.addOnNextStepByForce(coordinate);
                    } else {
                        distanceHarass[coordinate.getX()][coordinate.getY()] = i;
                        prettyprint(i, coordinate);
                        // do not attract to taken places and workers
                        if (!isBuilder(coordinate) && !entitiesMap.getEntity(coordinate).isMy(EntityType.RANGED_UNIT)) {
                            waveCoordinatesHarass.addOnNextStep(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            waveCoordinatesHarass.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            waveCoordinatesHarass.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            waveCoordinatesHarass.addOnNextStep(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            waveCoordinatesHarass.nextStep();

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
                    DebugInterface.println(distanceHarass[i][j], i, j, 2);
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

    public int getDistanceToEnemy(Coordinate position, Team teamNumber) {
        return getDistanceToEnemy(position.getX(), position.getY(), teamNumber);
    }

    public int getDistanceToEnemy(int x, int y, Team teamNumber) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        if (teamNumber == Team.HARASSERS || teamNumber == Team.HARASSERS2) {
            return distanceHarass[x][y] == 0 ? distance[x][y] : distanceHarass[x][y];
        } else {
            return distance[x][y];
        }
    }

}
