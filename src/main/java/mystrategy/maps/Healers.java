package mystrategy.maps;

import model.Entity;
import model.PlayerView;
import mystrategy.collections.AllEntities;
import mystrategy.maps.light.WarMap;
import util.DebugInterface;
import util.Task;

public class Healers {
    public static int totalHealed = 0;
    public static int id1 = -1;
    public static int id2 = -1;
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

        Entity healer1 = null;
        Entity healer2 = null;
        for (Entity worker : entities.getMyWorkers()) {
            if (id1 == worker.getId()) {
                healer1 = worker;
            }
            if (id2 == worker.getId()) {
                healer2 = worker;
            }
        }
        for (Entity worker : entities.getMyWorkers()) {
/*
            if (warMap.getDistanceToGoodGuys(worker.getPosition()) == 0) {
                continue;
            }
*/
            if (healer1 == null && id1 != -1) {
                System.out.println("H1 ded " + id1 + "/" + playerView.getCurrentTick());
                totalHealed -= 10;
            }
            if (healer2 == null && id2 != -1) {
                System.out.println("H2 ded " + id2 + "/" + playerView.getCurrentTick());
                totalHealed -= 10;
            }


            if (healer1 == null) {
                healer1 = worker;
            } else if (healer2 == null) {
                healer2 = worker;
            }
        }

        if (healer1 != null) {
            if (healer1.getId() != id1) {
                System.out.println("id1 changed");
            }
            id1 = healer1.getId();
            healer1.setTask(Task.HEAL);
            DebugInterface.println("HEAL1", healer1.getPosition(), 2);
        }
        if (healer2 != null) {
            if (healer1.getId() != id1) {
                System.out.println("i2 changed");
            }
            id2 = healer2.getId();
            healer2.setTask(Task.HEAL);
            DebugInterface.println("HEAL2", healer2.getPosition(), 2);
        }
    }

    public boolean isEnabled() {
        return this.playerView.isFogOfWar() && this.entities.getMyRangedUnits().size() > 0 && this.entities.getMyWorkers().size() > 50;
    }
}
