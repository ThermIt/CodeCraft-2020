package mystrategy.maps.light;

import common.Constants;
import model.Coordinate;
import model.Entity;
import model.EntityType;
import mystrategy.collections.AllEntities;
import mystrategy.collections.SingleVisitCoordinateSet;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import mystrategy.utils.Team;
import util.DebugInterface;
import util.Initializer;
import util.Task;

import java.util.List;

public class RangedUnitMagnet {
    private VisibilityMap visibility;
    private EntitiesMap entitiesMap;
    private AllEntities entities;
    private VirtualResources resources;
    private EnemiesMap enemiesMap;
    private int[][] distance;
    private int[][] distanceHarass;
    private int[][] distanceAntiHarass;
    private int antiHarassCount;
    private int mapSize = 80;
    private SingleVisitCoordinateSet waveCoordinates;
    private SingleVisitCoordinateSet waveCoordinatesHarass;
    private SingleVisitCoordinateSet waveCoordinatesAntiHarass;
//    private List<Coordinate> coordinates = new ArrayList<>();


    public RangedUnitMagnet(
            VisibilityMap visibility,
            EntitiesMap entitiesMap,
            AllEntities entities,
            VirtualResources resources,
            EnemiesMap enemiesMap
    ) {
        this.visibility = visibility;
        this.entitiesMap = entitiesMap;
        this.entities = entities;
        this.resources = resources;
        this.enemiesMap = enemiesMap;
        this.distance = new int[Initializer.getMapSize()][Initializer.getMapSize()];
        this.distanceHarass = new int[Initializer.getMapSize()][Initializer.getMapSize()];
        this.distanceAntiHarass = new int[Initializer.getMapSize()][Initializer.getMapSize()];
        this.antiHarassCount = 0;

        waveCoordinates = new SingleVisitCoordinateSet();
        waveCoordinatesHarass = new SingleVisitCoordinateSet();
        waveCoordinatesAntiHarass = new SingleVisitCoordinateSet();

        for (Entity enemyUnit : entities.getEnemyRangedUnits()) {
            waveCoordinates.add(enemyUnit.getPosition());
        }

/*
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
*/

        for (Entity enemyUnit : entities.getEnemyWorkers()) {
            waveCoordinates.add(enemyUnit.getPosition());
            waveCoordinatesHarass.add(enemyUnit.getPosition());
        }

        for (Entity enemyUnit : entities.getEnemyMeleeUnits()) {
            waveCoordinates.add(enemyUnit.getPosition());
        }

        boolean onlyTurrets = entities.getEnemyTurrets().size() == entities.getEnemyEntities().size();
        for (Entity enemyBuilding : entities.getEnemyBuildings()) {
            if (onlyTurrets || enemyBuilding.getEntityType() != EntityType.TURRET) {
                for (Coordinate position : enemyBuilding.getInsideCoordinates()) {
                    waveCoordinates.add(position);
                    waveCoordinatesHarass.add(position);
                }
            }
        }

        SingleVisitCoordinateSet waveCoordinatesAntiHarassNegative = new SingleVisitCoordinateSet();
        for (Entity myBuilding : entities.getMyBuildings()) {
            for (Coordinate position : myBuilding.getInsideCoordinates()) {
                waveCoordinatesAntiHarassNegative.add(position);
            }
        }
        for (Entity myWorker : entities.getMyWorkers()) {
            if (myWorker.getTask() != Task.HEAL) {
                waveCoordinatesAntiHarassNegative.add(myWorker.getPosition());
            }/* else {
                System.out.println("healer");
            }*/
        }
        for (int i = 0; i < 10; i++) {
            for (Coordinate coordinate : waveCoordinatesAntiHarassNegative) {
                Entity entity = entitiesMap.getEntity(coordinate);
                if (entity.isEnemy() && entity.getProperties().getAttack() != null && entity.getProperties().getAttack().getDamage() > 1) {
                    waveCoordinatesAntiHarass.add(coordinate);
                    antiHarassCount++;
                    DebugInterface.println("V", coordinate, 0);
                }
                waveCoordinatesAntiHarassNegative.addOnNextStep(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                waveCoordinatesAntiHarassNegative.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                waveCoordinatesAntiHarassNegative.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                waveCoordinatesAntiHarassNegative.addOnNextStep(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
            }
            waveCoordinatesAntiHarassNegative.nextStep();
        }

        fillDistancesAntiHarass();

/*
        for (int i = 40; i < 80; i++) {
            Coordinate coordinate1 = new Coordinate(41, i);
            Coordinate coordinate2 = new Coordinate(i, 41);
            if (entitiesMap.isPassable(coordinate1)) {
                waveCoordinates.add(coordinate1);
                waveCoordinatesHarass.add(coordinate1);
            }
            if (entitiesMap.isPassable(coordinate2)) {
                waveCoordinates.add(coordinate2);
                waveCoordinatesHarass.add(coordinate2);
            }
        }
*/

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

    public void fillDistancesAntiHarass() {
        if (entities.getMyRangedUnits().size() == 0) {
            // TODO: stop when all rangers captured
            return;
        }

        int[][] delayFuseForCalculation = new int[mapSize][mapSize];

        for (int i = 8; !waveCoordinatesAntiHarass.isEmpty(); i++) {
            for (Coordinate coordinate : waveCoordinatesAntiHarass) {
                if (coordinate.isInBounds()
                        && distanceAntiHarass[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        waveCoordinatesAntiHarass.addOnNextStepByForce(coordinate);
                    } else {
                        distanceAntiHarass[coordinate.getX()][coordinate.getY()] = i;
                        prettyprint(i, coordinate);
                        // do not attract to taken places and workers
                        boolean isFrontlineUnit = entitiesMap.getEntity(coordinate).isMy(EntityType.RANGED_UNIT)
                                && ((distance[coordinate.getX()][coordinate.getY()] <= 9
                                && distance[coordinate.getX()][coordinate.getY()] > 0)
                                || enemiesMap.getDangerLevel(coordinate) > 0
                                || coordinate.getX() > 39 || coordinate.getY() > 39);
                        if (!isBuilder(coordinate) && !isFrontlineUnit) {
                            waveCoordinatesAntiHarass.addOnNextStep(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            waveCoordinatesAntiHarass.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            waveCoordinatesAntiHarass.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            waveCoordinatesAntiHarass.addOnNextStep(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            waveCoordinatesAntiHarass.nextStep();

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }

        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled() && distanceAntiHarass[i][j] < 15 && distanceAntiHarass[i][j] > 0) {
                    DebugInterface.println(distanceAntiHarass[i][j], i, j, 0);
                }
            }
        }
    }

    public void fillDistances() {
        if (entities.getMyRangedUnits().size() == 0) {
            // TODO: stop when all rangers captured
            return;
        }

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
                        boolean isFrontlineUnit = entitiesMap.getEntity(coordinate).isMy(EntityType.RANGED_UNIT)
                                && ((distance[coordinate.getX()][coordinate.getY()] <= 9
                                && distance[coordinate.getX()][coordinate.getY()] > 0)
                                || enemiesMap.getDangerLevel(coordinate) > 0
                                || coordinate.getX() > 39 || coordinate.getY() > 39);
                        if (!isBuilder(coordinate) && !isFrontlineUnit) {
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

//        for (int i = 0; i < mapSize; i++) {
//            for (int j = 0; j < mapSize; j++) {
//                if (DebugInterface.isDebugEnabled() && distance[i][j] < 15) {
//                    DebugInterface.println(distance[i][j], i, j, 0);
//                }
//            }
//        }
    }

    public void fillDistancesHarass() {
        if (entities.getMyRangedUnits().size() == 0) {
            // TODO: stop when all rangers captured
            return;
        }
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];

        for (int i = 8; !waveCoordinatesHarass.isEmpty(); i++) {
            for (Coordinate coordinate : waveCoordinatesHarass) {
                if (coordinate.isInBounds()
                        && distanceHarass[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = (resourceCount - 1) / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        waveCoordinatesHarass.addOnNextStepByForce(coordinate);
                    } else {
                        distanceHarass[coordinate.getX()][coordinate.getY()] = i;
                        prettyprint(i, coordinate);
                        // do not attract to taken places and workers
                        boolean isFrontlineUnit = entitiesMap.getEntity(coordinate).isMy(EntityType.RANGED_UNIT)
                                && ((distance[coordinate.getX()][coordinate.getY()] <= 9
                                && distance[coordinate.getX()][coordinate.getY()] > 0)
                                || enemiesMap.getDangerLevel(coordinate) > 0
                                || coordinate.getX() > 39 || coordinate.getY() > 39);
                        if (!isBuilder(coordinate) && !isFrontlineUnit) {
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
                if (DebugInterface.isDebugEnabled() && distance[i][j] < 10) {
                    DebugInterface.println(distanceHarass[i][j], i, j, 1);
                }
            }
        }
*/
    }

    public void prettyprint(int i, Coordinate coordinate) {
        if (i <= 160) {
//            DebugInterface.println(i, coordinate, 1);
        }
    }

    public int getDistanceToEnemy(Coordinate position, Team teamNumber) {
        return getDistanceToEnemy(position.getX(), position.getY(), teamNumber);
    }

    public int getDistanceToEnemy(int x, int y, Team teamNumber) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        if (teamNumber == Team.HARASSERS || teamNumber == Team.HARASSERS2) {
            if (antiHarassCount > 0) {
                int dah = distanceAntiHarass[x][y] == 0 ? distance[x][y] : distanceAntiHarass[x][y];
                int dh = distanceHarass[x][y] == 0 ? distance[x][y] : distanceHarass[x][y];
                if (antiHarassCount >= 20) {
                    return dah;
                } else if (antiHarassCount >= 5) {
                    return Math.min(dah, dh);
                } else if (teamNumber == Team.HARASSERS) {
                    return Math.min(dah, dh);
                }
            }
            return distanceHarass[x][y] == 0 ? distance[x][y] : distanceHarass[x][y];
        } else {
            return distance[x][y];
        }
    }

}
