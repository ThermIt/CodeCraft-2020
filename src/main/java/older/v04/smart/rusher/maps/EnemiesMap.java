package older.v04.smart.rusher.maps;

import model.Coordinate;
import model.EntityProperties;
import model.PlayerView;
import older.v04.smart.rusher.Constants;
import util.DebugInterface;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnemiesMap {
    private int[][] distanceByFoot;
    private int[][] shootDanger;
    private EntitiesMap entitiesMap;
    private int mapSize;

    public EnemiesMap(PlayerView playerView, EntitiesMap entitiesMap) {
        this.entitiesMap = entitiesMap;
        mapSize = playerView.getMapSize();
        distanceByFoot = new int[mapSize][mapSize];
        shootDanger = new int[mapSize][mapSize];

        Set<Coordinate> enemyCoordinates = new HashSet<>(128);
        Set<Coordinate> shoot5Coordinates = new HashSet<>(128);
        Set<Coordinate> shoot1Coordinates = new HashSet<>(128);
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (entitiesMap.getIsEnemy(i, j)) {
                    Coordinate position = new Coordinate(i, j);
                    enemyCoordinates.add(position);

                    EntityProperties properties = entitiesMap.getEntity(i, j).getProperties();
                    if (properties.getAttack() != null
                            && properties.getAttack().getDamage() > 1) {
                        if (properties.getAttack().getAttackRange() >= 5) {
                            shoot5Coordinates.add(position);
                        } else {
                            shoot1Coordinates.add(position);
                        }
                    }
                }
            }
        }

        fillDistances(distanceByFoot, enemyCoordinates);
        fillShootDanger(shootDanger, shoot5Coordinates, shoot1Coordinates);
    }


    public int getDistance(Coordinate position) {
        return getDistance(position.getX(), position.getY());
    }

    public int getDistance(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return distanceByFoot[x][y];
    }

    private void fillShootDanger(int[][] dangerMap, Set<Coordinate> coordinateList5, Set<Coordinate> coordinateList1) {
        Set<Coordinate> coordinateList = coordinateList5;
        for (int i = 7; i > 0; i--) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            if (i == 4) {
                coordinateListNext.addAll(coordinateList1);
            }
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.getX() >= 0 && coordinate.getX() < mapSize
                        && coordinate.getY() >= 0 && coordinate.getY() < mapSize
                        && dangerMap[coordinate.getX()][coordinate.getY()] < i) {
                    dangerMap[coordinate.getX()][coordinate.getY()] = i;
                    coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                }
            }
            coordinateList = coordinateListNext;
        }
    }

    private void fillDistances(int[][] distanceMap, Set<Coordinate> coordinateList) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.getX() >= 0 && coordinate.getX() < mapSize
                        && coordinate.getY() >= 0 && coordinate.getY() < mapSize
                        && getDistance(coordinate) == 0
                        && isPassable(coordinate)) {
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
        return this.entitiesMap.isPassable(coordinate) || this.entitiesMap.getIsEnemy(coordinate);
    }

    public Coordinate getMinOfTwoPositions(Coordinate old, Coordinate newPosition) {
        if (newPosition.getX() < 0 || newPosition.getY() < 0 || newPosition.getX() >= mapSize || newPosition.getY() >= mapSize) {
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

    public Coordinate getPositionClosestToEnemy(Coordinate from) {
        int radius = 4;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                from = getMinOfTwoPositions(from, new Coordinate(from.getX() + i, from.getY() + j));
            }
        }
        return from;
    }

    public int getDangerLevel(Coordinate from) {
        if (from.isOutOfBounds()) {
            return 0;
        }
        return shootDanger[from.getX()][from.getY()];
    }

    public Coordinate getPositionClosestToEnemy(Coordinate from, List<Coordinate> coordinateList) {
        Coordinate position = from;
        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition);
        }
        return position;
    }
}
