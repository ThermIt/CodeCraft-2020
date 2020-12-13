package older.v02.manage.workers.maps;

import model.Coordinate;
import model.EntityProperties;
import model.PlayerView;

import java.util.ArrayList;
import java.util.List;

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

        List<Coordinate> enemyCoordinates = new ArrayList<>();
        List<Coordinate> shoot5Coordinates = new ArrayList<>();
        List<Coordinate> shoot1Coordinates = new ArrayList<>();
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
/*
        List<Coordinate> coordinates = Arrays.stream(playerView.getEntities()).filter(ent -> ent.getPlayerId() != playerView.getMyId())
                .map(box -> new Coordinate(box.getPosition())).collect(Collectors.toList());
*/
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

    public int getDistance(Coordinate position) {
        return getDistance(position.getX(), position.getY());
    }

    public int getDistance(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return distanceByFoot[x][y];
    }

    private void fillShootDanger(int[][] dangerMap, List<Coordinate> coordinateList5, List<Coordinate> coordinateList1) {
        List<Coordinate> coordinateList = coordinateList5;
        for (int i = 7; i > 0; i--) {
            List<Coordinate> coordinateListNext = new ArrayList<>();
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

    private void fillDistances(int[][] distanceMap, List<Coordinate> coordinateList) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            List<Coordinate> coordinateListNext = new ArrayList<>();
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
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled()) {
                    DebugCommand.Add command = new DebugCommand.Add();
                    ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(i, j), new Vec2Float(0, 0), new Color(0, 0, 0, 0.5f));
                    DebugData data = new DebugData.PlacedText(coloredVertex, Integer.toString(distanceByFoot[i][j]), -1, 12);
                    command.setData(data);
                    debugInterface.send(command);
                }
            }
        }
*/

    }

    private boolean isPassable(Coordinate coordinate) {
        return this.entitiesMap.isPassable(coordinate) || this.entitiesMap.getIsEnemy(coordinate);
    }

    public Coordinate getPositionClosestToEnemy(Coordinate from) {
        Coordinate position = from;

        int radius = 5;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                position = getMinOfTwoPositions(position, new Coordinate(from.getX() + i, from.getY() + j));
            }
        }
        return position;
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
