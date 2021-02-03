package older.v10.finals.part1.maps;

import model.*;

public class EntitiesMap {
    private static final Entity FREE = new Entity(-1001, null, EntityType.FREE, null, 0, false);
    private Entity[][] entityAtPosition;
    private EntityType[][] entityType;
    private boolean[][] isEnemy;
    private int mapSize;

    public EntitiesMap(PlayerView playerView) {
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

    public Entity getEntity(Coordinate coordinate) {
        return getEntity(coordinate.getX(), coordinate.getY());
    }

    public Entity getEntity(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return FREE;
        }
        Entity entity = entityAtPosition[x][y];
        if (entity == null) {
            return FREE;
        }
        return entity;
    }

    public boolean isResource(Coordinate coordinate) {
        return isResource(coordinate.getX(), coordinate.getY());
    }

    public boolean isResource(int x, int y) {
        return getEntity(x, y).getEntityType() == EntityType.RESOURCE;
    }

    public boolean isEmpty(Coordinate coordinate) {
        return isEmpty(coordinate.getX(), coordinate.getY());
    }

    public boolean isEmpty(int x, int y) {
        return entityAtPosition[x][y] == null;
    }

    public boolean isOrderFree(Entity order) {
        int size = order.getProperties().getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!isEmpty(i + order.getPosition().getX(), j + order.getPosition().getY())) {
                    return false;
                }
            }
        }
        return true;
    }

    public Entity choosePossibleAttackTarget(Entity unit) {
        int attackRange = 5;
        Entity opponent = null;
        int x = unit.getPosition().getX();
        int y = unit.getPosition().getY();
        for (int i = 1; i <= attackRange; i++) {
            int rangeY = attackRange - i;
            opponent = selectBetterOpponent(opponent, x + i, y);
            opponent = selectBetterOpponent(opponent, x - i, y);
            opponent = selectBetterOpponent(opponent, x, y + i);
            opponent = selectBetterOpponent(opponent, x, y - i);
            for (int j = 1; j <= rangeY; j++) {
                opponent = selectBetterOpponent(opponent, x + i, y + j);
                opponent = selectBetterOpponent(opponent, x - i, y + j);
                opponent = selectBetterOpponent(opponent, x + i, y - j);
                opponent = selectBetterOpponent(opponent, x - i, y - j);
            }
        }
        return opponent;
    }

    public Entity selectBetterOpponent(Entity current, int x, int y) {
        if (x < mapSize && y < mapSize && x >= 0 && y >= 0) {
            Entity entity = getEntity(x, y);

            if (entity.getPlayerId() != null && entity.getPlayerId() != null && !entity.isMy()) {
                if (entity.getHealthAfterDamage() > 0) {
                    current = judgeOpponents(current, entity);
                }
            }
        }
        return current;
    }

    private Entity judgeOpponents(Entity current, Entity other) {
        if (current == null) {
            return other;
        }
        if (current.isBuilding() && other.isUnit()) {
            return other;
        }
        if (other.isBuilding() && current.isUnit()) {
            return current;
        }
        if (other.getHealthAfterDamage() <= 5 && other.getHealthAfterDamage() > 0) {
            return other;
        }

//        current.getEntityType();
//        other.getEntityType();
        return current;
    }
}
