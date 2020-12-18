package mystrategy.maps.light;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EntitiesMap;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static util.Initializer.getMyId;

public class BuildOrders {
    private List<Entity> orderList;
    private Entity[][] orderMap;
    private int mapSize;

    public void init(PlayerView playerView) {
        if (mapSize != 0) {
            return;
        }
        mapSize = playerView.getMapSize();
        orderList = new ArrayList<>();

        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 2), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(5, 2), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 5), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(8, 2), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 8), 0, false));

        markActiveOrders();
    }

    private void markActiveOrders() {
        orderMap = new Entity[mapSize][mapSize];
        for (Entity order : getActiveOrders()) {
            List<Coordinate> adjacentCoordinates = order.getAdjacentCoordinates();
            for (Coordinate location :
                    adjacentCoordinates) {
                orderMap[location.getX()][location.getY()] = order;
            }
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

    public List<Entity> getActiveOrders() {
        return orderList.stream().filter(Entity::isActive).collect(Collectors.toList());
    }

    public List<Entity> updateAndGetActiveOrders(AllEntities allEntities, EntitiesMap entitiesMap, Player me) {
        for (Entity building : allEntities.getMyBuildings()) {
            if (!building.isActive() && orderList.stream().allMatch(order -> order.getId() != building.getId())) {
                orderList.add(new Entity(-1, getMyId(), building.getEntityType(), building.getPosition(), 0, false));
            }
        }

        boolean single = true;
        for (Iterator<Entity> iterator = orderList.iterator(); iterator.hasNext(); ) {
            Entity order = iterator.next();
            if (!single) {
                order.setActive(false);
                continue;
            }

            Entity entity = entitiesMap.getEntity(order.getPosition());
            if (entity.isMy(order.getEntityType()) && entity.isActive()) {
                iterator.remove();
            } else if (entity.isMy(order.getEntityType()) && !entity.isActive()) {
                order.setActive(true);
                DebugInterface.print("A+", order.getPosition());
                single = false;
            } else if (!order.isFree(entitiesMap)) {
                order.setActive(false);
            } else if (me.getResource() >= order.getProperties().getInitialCost() - 2
                    && !entity.isMy(order.getEntityType())) {
                order.setActive(true);
                DebugInterface.print("A", order.getPosition());
                single = false;
            } else {
                order.setActive(false);
            }
        }
        markActiveOrders();
        return orderList.stream().filter(Entity::isActive).collect(Collectors.toList());
    }
}
