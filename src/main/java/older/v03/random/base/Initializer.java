package older.v03.random.base;

import model.EntityType;
import model.PlayerView;

public class Initializer {
    private PlayerView playerView;
    private static int myId;
    private static int mapSize;

    public Initializer(PlayerView playerView) {
        this.playerView = playerView;
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
