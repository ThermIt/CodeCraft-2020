package older.v09.finals1.maps.light;

import model.Coordinate;
import model.Entity;
import model.EntityType;
import model.PlayerView;
import older.v09.finals1.collections.AllEntities;
import older.v09.finals1.maps.EntitiesMap;

public class VirtualResources {
    public static final int VIRTUAL_PATCH_SIZE = 10;
    private int mapSize;
    private VisibilityMap visibility;
    private int playersCount;
    private PlayerView playerView;
    private AllEntities allEntities;
    private boolean[][] processedAlready;
    private boolean[][] clearedFromResource;
    private boolean[][] possibleResource;
    private int[][] resourceCount;
    private int totalResourceCount;
    private int tick;

    public VirtualResources(VisibilityMap visibility) {
        this.visibility = visibility;
    }

    public void init(PlayerView playerView, AllEntities allEntities, EntitiesMap entitiesMap) {
        visibility.checkTick(playerView);
        tick = playerView.getCurrentTick();
        this.playersCount = playerView.getPlayers().length;
        this.playerView = playerView;
        this.allEntities = allEntities;
        this.mapSize = playerView.getMapSize();
        if (this.processedAlready == null) {
            this.processedAlready = new boolean[mapSize][mapSize];
            this.possibleResource = new boolean[mapSize][mapSize];
            this.clearedFromResource = new boolean[mapSize][mapSize];
            this.resourceCount = new int[mapSize][mapSize];
        }

        for (Entity resource : allEntities.getResources()) {
            Coordinate pos = resource.getPosition();
            resourceCount[pos.getX()][pos.getY()] = resource.getHealth();
            if (!processedAlready[pos.getX()][pos.getY()]) {
                markProcessed(pos);
                if (playersCount > 2) {
                    Coordinate pos2 = new Coordinate(mapSize - pos.getY() - 1, pos.getX());
                    Coordinate pos4 = new Coordinate(pos.getY(), mapSize - pos.getX() - 1);
                    markProcessed(pos2);
                    markProcessed(pos4);
                }
                Coordinate pos3 = new Coordinate(mapSize - pos.getX() - 1, mapSize - pos.getY() - 1);
                markProcessed(pos3);
            }
        }

        totalResourceCount = 0;
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (!clearedFromResource[i][j]) {
                    if (visibility.isVisible(i, j)) {
                        if (!entitiesMap.isResource(i, j)) {
                            possibleResource[i][j] = false;
                            clearedFromResource[i][j] = true;
                        }
                    }
                }
                totalResourceCount += getResourceCount(i, j);
            }
        }

/*
        if (DebugInterface.isDebugEnabled()) {
            for (int i = 0; i < mapSize; i++) {
                for (int j = 0; j < mapSize; j++) {
                    int resourceCount = getResourceCount(i, j);
                    if (resourceCount > 0) {
                        DebugInterface.print(resourceCount, i, j);
                    }
                }
            }
        }
*/
    }

    public int getResourceCount(Coordinate location) {
        if (location.isOutOfBounds()) {
            return 0;
        }
        return getResourceCount(location.getX(), location.getY());
    }

    public int getResourceCount(int x, int y) {
        if (clearedFromResource[x][y]) {
            return 0;
        } else if (resourceCount[x][y] == 0/* &&

                ((x >= 40 - VIRTUAL_PATCH_SIZE && x < 40 + VIRTUAL_PATCH_SIZE &&
                        y >= 40 - VIRTUAL_PATCH_SIZE && y < 40 + VIRTUAL_PATCH_SIZE)
                        || (playerView.isFinials() && (x == 0 || y == 0)))*/
        ) {
            return 1;
        }

        return resourceCount[x][y];
    }

    public void markProcessed(Coordinate pos) {
        processedAlready[pos.getX()][pos.getY()] = true;
        boolean resourcePossible = !clearedFromResource[pos.getX()][pos.getY()];
        possibleResource[pos.getX()][pos.getY()] = resourcePossible;
        if (resourceCount[pos.getX()][pos.getY()] == 0 && resourcePossible) {
            resourceCount[pos.getX()][pos.getY()] = playerView.getEntityProperties().get(EntityType.RESOURCE).getMaxHealth();
        }
    }

    public int getTick() {
        return tick;
    }

    public void checkTick(PlayerView playerView) {
        if (getTick() != playerView.getCurrentTick()) {
            throw new RuntimeException("visibility is not initialized");
        }
    }

    public int getTotalResourceCount() {
        return totalResourceCount;
    }
}
