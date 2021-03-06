package mystrategy.maps;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import util.DebugInterface;

import java.util.Objects;

public class RepairMap {
    private int[][] distanceByFoot;
    private PlayerView playerView;
    private EntitiesMap entitiesMap;
    private int mapSize;
    private EnemiesMap enemiesMap;
    private int myId;

    public RepairMap(PlayerView playerView, EntitiesMap entitiesMap, EnemiesMap enemiesMap) {
        this.playerView = playerView;
        this.entitiesMap = entitiesMap;
        myId = playerView.getMyId();
        mapSize = playerView.getMapSize();
        this.enemiesMap = enemiesMap;
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

    public Integer canRepairId(Coordinate from) {
        return from.getAdjacentList().stream().map(this::repairRequired)
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Integer repairRequired(Coordinate position) {
        Entity entity = entitiesMap.getEntity(position);
        if (entity.isMy() && entity.getHealth() < entity.getProperties().getMaxHealth()) {
            return entity.getId();
        } else if (entity.isMy() && enemiesMap.getDangerLevel(entity.getPosition()) > 1) {
            DebugInterface.println("HHH", position, 0);
            Healers.totalHealed+=5;
            System.out.println("heal " + playerView.getCurrentTick() + "/" + Healers.totalHealed);
            return entity.getId();
        }
        return null;
    }

    public Integer canBuildId(Coordinate position) {
        int x = position.getX();
        int y = position.getY();
        Integer entity = buildingRequired(x - 1, y);
        if (entity == null) {
            entity = buildingRequired(x, y - 1);
        }
        if (entity == null) {
            entity = buildingRequired(x, y + 1);
        }
        if (entity == null) {
            entity = buildingRequired(x + 1, y);
        }
        return entity;
    }

    private Integer buildingRequired(int x, int y) {
        Entity entity = entitiesMap.getEntity(x, y);
        if (entity.isMy() && entity.isBuilding() && !entity.isActive()) {
            return entity.getId();
        }
        return null;
    }
}
