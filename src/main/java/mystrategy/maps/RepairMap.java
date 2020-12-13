package mystrategy.maps;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RepairMap {
    private int[][] distanceByFoot;
    private EntitiesMap entitiesMap;
    private int mapSize;
    private int myId;

    public RepairMap(PlayerView playerView, EntitiesMap entitiesMap, DebugInterface debugInterface) {
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
        }
    }

    private boolean isPassable(Coordinate coordinate) {
        return this.entitiesMap.isPassable(coordinate.getX(), coordinate.getY());
    }

    public Integer canRepairId(Coordinate from) {
        List<Coordinate> coordinateList = new ArrayList<>();
        coordinateList.add(new Coordinate(from.getX() - 1, from.getY() + 0));
        coordinateList.add(new Coordinate(from.getX() + 0, from.getY() + 1));
        coordinateList.add(new Coordinate(from.getX() + 0, from.getY() - 1));
        coordinateList.add(new Coordinate(from.getX() + 1, from.getY() + 0));
        coordinateList.add(new Coordinate(from.getX(), from.getY()));

        return coordinateList.stream().map(this::repairRequired).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Integer repairRequired(Coordinate position) {
        Entity entity = entitiesMap.getEntity(position);
        if (entity != null && entity.isPlayer(myId)
                && entity.getHealth() < entity.getProperties().getMaxHealth()) {
            return entity.getId();
        }
        return null;
    }

    public Integer canBuildId(Coordinate position) {
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
