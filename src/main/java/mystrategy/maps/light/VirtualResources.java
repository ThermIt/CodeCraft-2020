package mystrategy.maps.light;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EntitiesMap;

public class VirtualResources {
    private int mapSize;
    private VisibilityMap visibility;
    private int playersCount;
    private PlayerView playerView;
    private AllEntities allEntities;
    private boolean[][] processedAlready;
    private boolean[][] clearedFromResource;
    private boolean[][] possibleResource;

    public VirtualResources(VisibilityMap visibility) {
        this.visibility = visibility;
    }

    public void init(PlayerView playerView, AllEntities allEntities, EntitiesMap entitiesMap) {
        if (visibility.getTick() != playerView.getCurrentTick()) {
            throw new RuntimeException("visibility is not initialized");
        }
        this.playersCount = playerView.getPlayers().length;
        this.playerView = playerView;
        this.allEntities = allEntities;
        this.mapSize = playerView.getMapSize();
        if (this.processedAlready == null) {
            this.processedAlready = new boolean[mapSize][mapSize];
            this.possibleResource = new boolean[mapSize][mapSize];
            this.clearedFromResource = new boolean[mapSize][mapSize];
        }

        for (Entity resource : allEntities.getResources()) {
            Coordinate pos = resource.getPosition();
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
            }
        }

/*
        if (DebugInterface.isDebugEnabled()) {
            for (int i = 0; i < mapSize; i++) {
                for (int j = 0; j < mapSize; j++) {
                    if (possibleResource[i][j]) {
                        DebugInterface.print("X", i, j);
                    }
                }
            }
        }
*/
    }

    public void markProcessed(Coordinate pos) {
        processedAlready[pos.getX()][pos.getY()] = true;
        possibleResource[pos.getX()][pos.getY()] = !clearedFromResource[pos.getX()][pos.getY()];
    }
}
