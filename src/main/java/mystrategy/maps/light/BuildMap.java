package mystrategy.maps.light;

import model.Coordinate;
import model.Entity;
import model.EntityType;
import model.PlayerView;

import java.util.ArrayList;
import java.util.List;

import static util.Initializer.getMyId;

public class BuildMap {
    private List<Entity> orderList;
    private Entity[][] orderMap;
    private int mapSize;

    public BuildMap(PlayerView playerView) {
        mapSize = playerView.getMapSize();
        orderMap = new Entity[mapSize][mapSize];
        orderList = new ArrayList<>();

        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 2), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(5, 2), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 5), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(8, 2), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 8), 0, false));

        for (Entity order : orderList) {

        }
    }

    public Entity getOrder(Coordinate coordinate) {
        return getOrder(coordinate.getX(), coordinate.getY());
    }

    public Entity getOrder(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return null;
        }
        return orderMap[x][y];
    }

    public boolean isEmpty(Coordinate coordinate) {
        return getOrder(coordinate) == null;
    }
}
