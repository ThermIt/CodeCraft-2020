package util;

import model.EntityType;
import model.Player;
import model.PlayerView;

import java.util.Arrays;

public class Initializer {
    private PlayerView playerView;
    private static DebugInterface debugInterface;
    private static int myId;
    private static Player me;
    private static int mapSize;

    public Initializer(PlayerView playerView, DebugInterface debugInterface) {
        this.playerView = playerView;
        this.debugInterface = debugInterface;
        initStatic();
    }

    public void initStatic() {
        myId = playerView.getMyId();
        me = Arrays.stream(playerView.getPlayers()).filter(player -> player.getId() == playerView.getMyId()).findAny().orElseThrow();
        mapSize = playerView.getMapSize();
        if (playerView.getCurrentTick() == 0) {
            for (EntityType entityType : EntityType.values()) {
                entityType.setProperties(playerView.getEntityProperties().get(entityType));
            }
        }
    }

    public static DebugInterface getDebugInterface() {
        return debugInterface;
    }

    public static int getMyId() {
        return myId;
    }

    public static int getMapSize() {
        return mapSize;
    }

    public static Player getMe() {
        return me;
    }

    public void run() {
        // done;
    }
}
