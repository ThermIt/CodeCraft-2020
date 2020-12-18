package mystrategy.maps;

import model.Coordinate;
import model.EntityType;
import model.PlayerView;
import mystrategy.Constants;
import mystrategy.collections.AllEntities;
import mystrategy.maps.light.WarMap;
import util.DebugInterface;

import java.util.HashSet;
import java.util.Set;

public class SimCityMap {

    public static final int MIN_DISTANCE_TO_ENEMY = 15;
    private int[][] distanceByFoot;
    private Coordinate[][] houseBuildCoordinates;
    private Coordinate[][] rangedBaseBuildCoordinates;
    private EntitiesMap entitiesMap;
    private int mapSize;
    private boolean needBarracks;
    private WarMap warMap;

    public SimCityMap(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities,
            WarMap warMap
    ) {
        warMap.checkTick(playerView);
        this.warMap = warMap;
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
        Set<Coordinate> coordinates = new HashSet<>(128);

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
                        if (warMap.getDistanceToEnemy(i + 1 + k, j + 1 + l) < MIN_DISTANCE_TO_ENEMY) {
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

                    boolean canBuildRangedBase = true; // totally hack
                    for (int k = 0; k < houseSizeWithMargin; k++) {
                        if (!canBuildRangedBase) {
                            break;
                        }
                        for (int l = 0; l < houseSizeWithMargin; l++) {
                            if (!isEmpty(i + k, j + l)) {
                                canBuildRangedBase = false;
                                break;
                            }
                            if (warMap.getDistanceToEnemy(i + 1 + k, j + 1 + l) < MIN_DISTANCE_TO_ENEMY) {
                                canBuildRangedBase = false;
                                break;
                            }
                        }
                    }
                    if (canBuildRangedBase) {
                        for (int k = 0; k <= houseSize + 1; k++) { // hack
                            if (i - 1 >= 0) {
                                rangedBaseBuildCoordinates[i - 1][j + k] = new Coordinate(i, j);
                            }
                            if (j - 1 >= 0) {
                                rangedBaseBuildCoordinates[i + k][j - 1] = new Coordinate(i, j);
                            }
                            if (j + houseSize + 2 < mapSize) {
                                rangedBaseBuildCoordinates[i + k][j + houseSize + 2] = new Coordinate(i, j);
                            }
                            if (i + houseSize + 2 < mapSize) {
                                rangedBaseBuildCoordinates[i + houseSize + 2][j + k] = new Coordinate(i, j);
                            }
                        }
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
                if (DebugInterface.isDebugEnabled() && rangedBaseBuildCoordinates[i][j] != null) {
                    DebugInterface.print(rangedBaseBuildCoordinates[i][j].toString(), i, j);
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

    private void fillDistances(int[][] distanceMap, Set<Coordinate> coordinateList) {
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
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

    public int getDistance(model.Coordinate position) {
/*
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(position.getX(), position.getY()), new Vec2Float(0, 0), new Color(0, 0, 0, 0.5f));
        DebugData data = new DebugData.PlacedText(coloredVertex, Integer.toString(getDistance(position.getX(), position.getY())), -1, 12);
        command.setData(data);
        debugInterface.send(command);
*/
        return getDistance(position.getX(), position.getY());
    }

    public Coordinate getBuildCoordinates(model.Coordinate position) {
        return houseBuildCoordinates[position.getX()][position.getY()];
    }

    public Coordinate getRangedBaseBuildCoordinates(model.Coordinate position) {
        return rangedBaseBuildCoordinates[position.getX()][position.getY()];
    }
}
