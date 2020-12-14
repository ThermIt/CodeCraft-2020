package older.v03.random.base.maps;

import model.Coordinate;
import model.EntityType;
import model.PlayerView;
import older.v03.random.base.AllEntities;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.List;

public class SimCityMap {

    private int[][] distanceByFoot;
    private Coordinate[][] houseBuildCoordinates;
    private Coordinate[][] rangedBaseBuildCoordinates;
    private EntitiesMap entitiesMap;
    private int mapSize;
    private boolean needBarracks;

    public SimCityMap(PlayerView playerView, EntitiesMap entitiesMap, AllEntities allEntities) {
        this.entitiesMap = entitiesMap;
        mapSize = playerView.getMapSize();
        distanceByFoot = new int[mapSize][mapSize];
        houseBuildCoordinates = new Coordinate[mapSize][mapSize];
        rangedBaseBuildCoordinates = new Coordinate[mapSize][mapSize];
        needBarracks = allEntities.getMyBuildings().stream()
                .noneMatch(ent -> ent.getProperties().getBuild() != null
                        && ent.getProperties().getBuild().getOptions()[0] == EntityType.RANGED_UNIT);

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

                    for (int k = 0; k <= houseSize + 1; k++) { // hack
                        if (i - 1 >= 0)
                        rangedBaseBuildCoordinates[i - 1][j + k] = new Coordinate(i, j);
                        if (j - 1 >= 0)
                        rangedBaseBuildCoordinates[i + k][j  - 1] = new Coordinate(i, j);
                        if (j + houseSize + 2 < mapSize)
                        rangedBaseBuildCoordinates[i + k][j + houseSize + 2] = new Coordinate(i, j);
                        if (i + houseSize + 2 < mapSize)
                        rangedBaseBuildCoordinates[i + houseSize + 2][j + k] = new Coordinate(i, j);
                    }

                    for (int k = 1; k <= houseSize; k++) {
                        houseBuildCoordinates[i + k][j + 0] = new Coordinate(i + 1, j + 1);
                        houseBuildCoordinates[i + 0][j + k] = new Coordinate(i + 1, j + 1);
                        houseBuildCoordinates[i + k][j + houseSize + 1] = new Coordinate(i + 1, j + 1);
                        houseBuildCoordinates[i + houseSize + 1][j + k] = new Coordinate(i + 1, j + 1);
/*
                        for (int l = 1; l <= houseSize; l++) {
                            if (houseBuildCoordinates[i + k][j + l] == null) {
                                houseBuildCoordinates[i + k][j + l] = new Coordinate(i + 1, j + 1);
                            }
                        }
*/
                    }
                }
            }
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (debugInterface.isDebugEnabled() && rangedBaseBuildCoordinates[i][j] != null) {
                    DebugCommand.Add command = new DebugCommand.Add();
                    ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(i, j), new Vec2Float(0, 0), new Color(0, 0, 0, 0.5f));
                    DebugData data = new DebugData.PlacedText(coloredVertex, Objects.toString(rangedBaseBuildCoordinates[i][j]), -1, 12);
                    command.setData(data);
                    debugInterface.send(command);
                }
            }
        }
*/

        fillDistances(distanceByFoot, coordinates);

/*
        List<Coordinate> coordinates = Arrays.stream(playerView.getEntities()).filter(ent -> ent.getPlayerId() != playerView.getMyId())
                .map(box -> new Coordinate(box.getPosition())).collect(Collectors.toList());
*/
    }

    public boolean isNeedBarracks() {
        return needBarracks;
    }

    public void setNeedBarracks(boolean needBarracks) {
        this.needBarracks = needBarracks;
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
/*
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(position.getX(), position.getY()), new Vec2Float(0, 0), new Color(0, 0, 0, 0.5f));
        DebugData data = new DebugData.PlacedText(coloredVertex, Integer.toString(getDistance(position.getX(), position.getY())), -1, 12);
        command.setData(data);
        debugInterface.send(command);
*/
        return getDistance(position.getX(), position.getY());
    }

    public Coordinate getBuildCoordinates(Coordinate position) {
        return houseBuildCoordinates[position.getX()][position.getY()];
    }

    public Coordinate getRangedBaseBuildCoordinates(Coordinate position) {
        return rangedBaseBuildCoordinates[position.getX()][position.getY()];
    }
}