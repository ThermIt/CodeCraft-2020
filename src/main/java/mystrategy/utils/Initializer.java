package mystrategy.utils;

import model.EntityType;
import model.PlayerView;
import util.DebugInterface;

public class Initializer {
    private PlayerView playerView;
    private DebugInterface debugInterface;
    private static int myId;
    private static int mapSize;

    public Initializer(PlayerView playerView, DebugInterface debugInterface) {
        this.playerView = playerView;
        this.debugInterface = debugInterface;
    }

    public void initStatic() {
        myId = playerView.getMyId();
        mapSize = playerView.getMapSize();
        if (playerView.getCurrentTick() == 0) {
            for (EntityType entityType : EntityType.values()) {
                entityType.setProperties(playerView.getEntityProperties().get(entityType));
            }
        }
    }

    public static int getMyId() {
        return myId;
    }

    public static int getMapSize() {
        return mapSize;
    }
}
