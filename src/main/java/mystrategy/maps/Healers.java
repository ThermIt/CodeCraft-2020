package mystrategy.maps;

import model.Entity;
import model.PlayerView;
import mystrategy.collections.AllEntities;
import mystrategy.maps.light.WarMap;
import util.DebugInterface;
import util.Task;

import java.util.ArrayList;
import java.util.List;

public class Healers {
    public static final int NUM_HEALERS = 5;
    public static int totalHealed = 0;
    public static int id1 = -1;
    public static int id2 = -1;
    public static List<Integer> ids = new ArrayList<>();
    public static List<Entity> healers = new ArrayList<>();
    private PlayerView playerView;
    private EntitiesMap map;
    private AllEntities entities;

    public Healers(PlayerView playerView,
                   EntitiesMap map,
                   AllEntities entities,
                   WarMap warMap
    ) {
        this.playerView = playerView;
        this.map = map;
        this.entities = entities;
        if (!isEnabled()) {
            return;
        }
        healers = new ArrayList<>();

        for (Entity worker : entities.getMyWorkers()) {
            if (ids.contains(worker.getId())) {
                healers.add(worker);
            }
        }
        if (healers.size() < ids.size()) {
            System.out.println("H deds " + (ids.size() - healers.size()) + "/" + playerView.getCurrentTick());
            totalHealed -= 10 * (ids.size() - healers.size());
        }
        for (Entity worker : entities.getMyWorkers()) {
            if (healers.size() < NUM_HEALERS) {
                healers.add(worker);
            } else {
                break;
            }
        }

        for (Entity healer : healers) {
            healer.setTask(Task.HEAL);
            DebugInterface.println("HEAL", healer.getPosition(), 2);
        }
    }

    public boolean isEnabled() {
        return this.playerView.isFogOfWar() && this.entities.getMyRangedUnits().size() > 0 && this.entities.getMyWorkers().size() > 50;
    }
}
