package older.v02.manage.workers.maps;

import model.Coordinate;
import model.EntityType;
import model.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class SimCityMap {

    private int[][] distanceByFoot;
    private Coordinate[][] buildCoordinates;
    private EntitiesMap entitiesMap;
    private int mapSize;

    public SimCityMap(PlayerView playerView, EntitiesMap entitiesMap) {
        this.entitiesMap = entitiesMap;
        mapSize = playerView.getMapSize();
        distanceByFoot = new int[mapSize][mapSize];
        buildCoordinates = new Coordinate[mapSize][mapSize];

        int houseSize = playerView.getEntityProperties().get(EntityType.HOUSE).getSize();
        int houseSizeWithMargin = houseSize + 2;
        List<Coordinate> coordinates = new ArrayList<>();

        for (int i = 0; i < mapSize - houseSizeWithMargin + 1; i++) {
            for (int j = 0; j < mapSize - houseSizeWithMargin + 1; j++) {
                boolean canBuild = true;
                for (int k = 0; k < houseSize; k++) {
                    if (!canBuild) {
                        break;
                    }
                    for (int l = 0; l < houseSize; l++) {
                        if (!isEmpty(i + 1 + k, j + 1 + l)) {
                            canBuild = false;
                            break;
                        }
                    }
                }
                for (int k = 0; k < houseSizeWithMargin; k++) {
                    if (!canBuild) {
                        break;
                    }
                    for (int l = 0; l < houseSizeWithMargin; l++) {
                        if (!isPassable(i + k, j + l)) {
                            canBuild = false;
                            break;
                        }
                    }
                }
                if (canBuild) {
                    for (int k = 0; k < houseSize; k++) {
                        for (int l = 0; l < houseSize; l++) {
                            int canBuildX = i + 1 + k;
                            int canBuildY = j + 1 + l;
                            coordinates.add(new Coordinate(canBuildX, canBuildY));
/*
                            if (debugInterface.isDebugEnabled()) {
                                DebugCommand.Add command = new DebugCommand.Add();
                                ColoredVertex[] arra = new ColoredVertex[3];
                                arra[0] = new ColoredVertex(new Vec2Float(canBuildX,canBuildY), new Vec2Float(0, 0), new Color(0, 1, 1, 0.5f));
                                arra[1] = new ColoredVertex(new Vec2Float(canBuildX+1,canBuildY), new Vec2Float(0, 0), new Color(0, 1, 1, 0.5f));
                                arra[2] = new ColoredVertex(new Vec2Float(canBuildX,canBuildY+1), new Vec2Float(0, 0), new Color(0, 1, 1, 0.5f));
                                DebugData data = new DebugData.Primitives(arra, PrimitiveType.TRIANGLES);
                                command.setData(data);
                                debugInterface.send(command);
                            }
*/
                        }
                    }
                    for (int k = 0; k < houseSize; k++) {
                        for (int l = 0; l < houseSize; l++) {
                            if (buildCoordinates[i + k][j + l] == null) {
                                buildCoordinates[i + k][j + l] = new Coordinate(i + 1, j + 1);
                            }
                        }
                    }
                }
            }
        }

        fillDistances(distanceByFoot, coordinates);

/*
        List<Coordinate> coordinates = Arrays.stream(playerView.getEntities()).filter(ent -> ent.getPlayerId() != playerView.getMyId())
                .map(box -> new Coordinate(box.getPosition())).collect(Collectors.toList());
*/
    }


    public int getDistance(int x, int y) {
        return distanceByFoot[x][y];
    }

    private void fillDistances(int[][] distanceMap, List<Coordinate> coordinateList) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            List<Coordinate> coordinateListNext = new ArrayList<>();
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.getX() >= 0 && coordinate.getX() < mapSize
                        && coordinate.getY() >= 0 && coordinate.getY() < mapSize
                        && distanceMap[coordinate.getX()][coordinate.getY()] == 0
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
    }

    private boolean isPassable(Coordinate coordinate) {
        return isPassable(coordinate.getX(), coordinate.getY());
    }

    private boolean isPassable(int x, int y) {
        return this.entitiesMap.isPassable(x, y);
    }

    private boolean isEmpty(Coordinate coordinate) {
        return isEmpty(coordinate.getX(), coordinate.getY());
    }

    private boolean isEmpty(int x, int y) {
        EntityType entityType = this.entitiesMap.getEntityType(x, y);
        return entityType == null;
    }

    public int getDistance(Coordinate position) {
        return getDistance(position.getX(), position.getY());
    }

    public Coordinate getBuildCoordinates(Coordinate position) {
        return buildCoordinates[position.getX()][position.getY()];
    }
}
