package maps;

import model.*;
import util.DebugInterface;

public class EntitiesMap {
    private Entity[][] entityAtPosition;
    private EntityType[][] entityType;
    private boolean[][] isEnemy;
    private int mapSize;

    public EntitiesMap(PlayerView playerView, DebugInterface debugInterface) {
        mapSize = playerView.getMapSize();
        entityType = new EntityType[mapSize][mapSize];
        entityAtPosition = new Entity[mapSize][mapSize];
        isEnemy = new boolean[mapSize][mapSize];
        for (Entity entity : playerView.getEntities()) {
            EntityProperties entityProperties = playerView.getEntityProperties().get(entity.getEntityType());
            int maxX = entity.getPosition().getX() + entityProperties.getSize();
            int maxY = entity.getPosition().getY() + entityProperties.getSize();
            boolean isEnemyEntity = entity.getPlayerId() != null && entity.getPlayerId() != playerView.getMyId();
            for (int idX = entity.getPosition().getX(); idX < maxX; idX++) {
                for (int idY = entity.getPosition().getY(); idY < maxY; idY++) {
                    entityAtPosition[idX][idY] = entity;
                    entityType[idX][idY] = entity.getEntityType();
                    isEnemy[idX][idY] = isEnemyEntity;
                }
            }
        }
    }

    public boolean getIsEnemy(Coordinate coordinate) {
        return isEnemy[coordinate.getX()][coordinate.getY()];
    }

    public boolean getIsEnemy(int x, int y) {
        return isEnemy[x][y];
    }

    public EntityType getEntityType(int x, int y) {
        return entityType[x][y];
    }

    public boolean isPassable(Coordinate coordinate) {
        return isPassable(coordinate.getX(), coordinate.getY());
    }

    public boolean isPassable(int x, int y) {
        EntityType entityType = this.getEntityType(x, y);
        if (entityType == null) {
            return true;
        }
        switch (entityType) {
            case WALL:
            case HOUSE:
            case BUILDER_BASE:
            case MELEE_BASE:
            case RANGED_BASE:
            case RESOURCE:
            case TURRET:
                return false;
            case BUILDER_UNIT:
            case MELEE_UNIT:
            case RANGED_UNIT:
                return true;
        }
        return true;
    }

    public Entity getEntity(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return null;
        }
        return entityAtPosition[x][y];
    }
}
