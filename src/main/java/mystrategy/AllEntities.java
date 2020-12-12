package mystrategy;

import model.Entity;
import model.EntityType;
import model.PlayerView;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.List;

public class AllEntities {

    private int myId;

    private List<Entity> resources = new ArrayList<>();

    private List<Entity> myUnits = new ArrayList<>();
    private List<Entity> myBuildings = new ArrayList<>();

    private List<Entity> enemyUnits = new ArrayList<>();
    private List<Entity> enemyBuildings = new ArrayList<>();

    public AllEntities(PlayerView playerView, DebugInterface debugInterface) {
        myId = playerView.getMyId();

        for (Entity entity : playerView.getEntities()) {
            if (entity.getEntityType() == EntityType.RESOURCE) {
                resources.add(entity);
                continue;
            }
            if (entity.getPlayerId() == myId) {
                if (entity.getEntityType().isBuilding()) {
                    myBuildings.add(entity);
                }
                if (entity.getEntityType().isUnit()) {
                    myUnits.add(entity);
                }
            } else {
                if (entity.getEntityType().isBuilding()) {
                    enemyBuildings.add(entity);
                }
                if (entity.getEntityType().isUnit()) {
                    enemyUnits.add(entity);
                }
            }
        }
    }

    public int getMyId() {
        return myId;
    }

    public List<Entity> getResources() {
        return resources;
    }

    public List<Entity> getEnemyUnits() {
        return enemyUnits;
    }

    public List<Entity> getMyUnits() {
        return myUnits;
    }

    public List<Entity> getEnemyBuildings() {
        return enemyBuildings;
    }

    public List<Entity> getMyBuildings() {
        return myBuildings;
    }
}
