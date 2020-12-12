package mystrategy;

import model.EntityType;
import model.PlayerView;
import util.DebugInterface;

public class Initializer {
    private PlayerView playerView;
    private DebugInterface debugInterface;

    public Initializer(PlayerView playerView, DebugInterface debugInterface) {
        this.playerView = playerView;
        this.debugInterface = debugInterface;
    }

    public void initStatic() {
        if (playerView.getCurrentTick() == 0) {
            for (EntityType entityType : EntityType.values()) {
                entityType.setProperties(playerView.getEntityProperties().get(entityType));
            }
        }
    }
}
