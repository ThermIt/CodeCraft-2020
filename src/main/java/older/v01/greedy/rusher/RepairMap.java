package older.v01.greedy.rusher;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import mystrategy.Constants;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.List;

public class RepairMap {
    private int[][] distanceByFoot;
    private EntitiesMap entitiesMap;
    private int mapSize;
    private int myId;

    public RepairMap(PlayerView playerView, EntitiesMap entitiesMap) {
        this.entitiesMap = entitiesMap;
        myId = playerView.getMyId();
        mapSize = playerView.getMapSize();
        distanceByFoot = new int[mapSize][mapSize];

/*
        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                Entity entity = entitiesMap.getEntity(i, j);
                if (entity != null
                        && entity.isPlayer(playerView.getMyId())
                        && entity.isBuilding()
                        && !entity.isActive()) {
                    coordinates.add(new Coordinate(i, j));
                }
            }
        }
*/

//        fillDistances(distanceByFoot, coordinates);
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
        return this.entitiesMap.isPassable(coordinate.getX(), coordinate.getY());
    }

    public Integer canRepairId(Coordinate position) {
        int x = position.getX();
        int y = position.getY();
        Integer entity = buildingRequired(x-1, y);
        if (entity == null) {
            entity = buildingRequired(x, y-1);
        }
        if (entity == null) {
            entity = buildingRequired(x, y+1);
        }
        if (entity == null) {
            entity = buildingRequired(x+1, y);
        }
        return entity;
    }

    private Integer buildingRequired(int x, int y) {
        Entity entity = entitiesMap.getEntity(x, y);
        if (entity != null && entity.isPlayer(myId)
                && entity.isBuilding()
                && !entity.isActive()) {
            return entity.getId();
        }
        return null;
    }
}
