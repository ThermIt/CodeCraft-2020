package older.v05.strange.maps;

import model.Entity;
import model.PlayerView;
import older.v05.strange.collections.AllEntities;
import util.DebugInterface;

public class MyUnitsMap {
    private Entity[][] myUnits;
    private AllEntities allEntities;
    private int mapSize;
    private DebugInterface debugInterface;

    public MyUnitsMap(PlayerView playerView, AllEntities allEntities, EntitiesMap entitiesMap, DebugInterface debugInterface) {
        this.allEntities = allEntities;
        mapSize = playerView.getMapSize();
        this.debugInterface = debugInterface;
        myUnits = new Entity[mapSize][mapSize];

        for (Entity unit : allEntities.getMyUnits()) {
            if (unit.getMoveAction() != null
                    && entitiesMap.isPassable(unit.getMoveAction().getTarget())
                    && myUnits[unit.getMoveAction().getTarget().getX()][unit.getMoveAction().getTarget().getY()] == null) {
                myUnits[unit.getMoveAction().getTarget().getX()][unit.getMoveAction().getTarget().getY()] = unit;
            } else {

            }
        }

    }
}
