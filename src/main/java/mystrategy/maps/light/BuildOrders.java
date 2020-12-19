package mystrategy.maps.light;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EntitiesMap;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static util.Initializer.getMyId;

public class BuildOrders {
    private List<Entity> buildQueue;
    private List<Entity> repairQueue;
    private Entity[][] orderMap;
    private int mapSize;
    private PlayerView playerView;
    private AllEntities entities;

    private int repairs;
    private int builds;
    private int completed;
    private int active;
    private int inactive;

    public void init(PlayerView playerView, AllEntities entities) {
        this.playerView = playerView;
        this.entities = entities;
        if (mapSize != 0) {
            return;
        }
        mapSize = playerView.getMapSize();
        buildQueue = new ArrayList<>();
        repairQueue = new ArrayList<>();

        buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 2), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(5, 2), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 5), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(8, 2), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 8), 0, false));
/*
        buildQueue.add(new Entity(-1, getMyId(), EntityType.TURRET, new Coordinate(10, 5), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.TURRET, new Coordinate(5, 10), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.TURRET, new Coordinate(10, 10), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.TURRET, new Coordinate(15, 15), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.TURRET, new Coordinate(20, 20), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.TURRET, new Coordinate(25, 25), 0, false));
        buildQueue.add(new Entity(-1, getMyId(), EntityType.TURRET, new Coordinate(30, 30), 0, false));
*/
/*
//        orderList.add(new Entity(-1, getMyId(), EntityType.RANGED_BASE, new Coordinate(5, 11), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(11, 0), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(0, 12), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(14, 0), 0, false));
        orderList.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(0, 15), 0, false));
*/
    }

    public Entity getOrder(Coordinate coordinate) {
        if (coordinate.isOutOfBounds()) {
            return null;
        }
        return orderMap[coordinate.getX()][coordinate.getY()];
    }

    public boolean isEmpty() {
        return buildQueue.isEmpty() && repairQueue.isEmpty();
    }

    public boolean isFreeToAdd() {
        return entities.getMyRangedBases().size() > 0 && entities.getMyRangedBases().get(0).getHealth() > 10;
    }

    public List<Entity> updateAndGetActiveOrders(AllEntities allEntities, EntitiesMap entitiesMap, Player me) {
        repairQueue = new ArrayList<>();
        for (Entity building : allEntities.getMyBuildings()) { // add built outside
            if (!building.isActive()) {
                repairQueue.add(building);
            }
        }

        boolean single = true;
        for (Iterator<Entity> iterator = buildQueue.iterator(); iterator.hasNext(); ) {
            Entity order = iterator.next();

            Entity entity = entitiesMap.getEntity(order.getPosition());
            if (entity.isMy(order.getEntityType()) && entity.isActive()) {
                iterator.remove();
                continue;
            }

            if (!single) {
                order.setActive(false);
                continue;
            }

            if (entity.isMy(order.getEntityType()) && entity.getHealth() != entity.getProperties().getMaxHealth()) {
                order.setActive(true);
                DebugInterface.print("A+", order.getPosition());
//                single = false;
            } else if (!entitiesMap.isOrderFree(order)) {
                order.setActive(false);
            } else if (me.getResource() >= order.getProperties().getInitialCost()
                    && !entity.isMy(order.getEntityType())) {
                order.setActive(true);
                DebugInterface.print("A", order.getPosition());
                single = false;
                if (order.getEntityType() == EntityType.RANGED_BASE) {
                    single = false;
                }
            } else {
                order.setActive(false);
            }
        }
        // mark active on map
        List<Entity> activeOrders = Stream.concat(buildQueue.stream().filter(Entity::isActive), repairQueue.stream())
                .collect(Collectors.toList());
        orderMap = new Entity[mapSize][mapSize];
        for (Entity order : activeOrders) {
            List<Coordinate> adjacentCoordinates = order.getAdjacentCoordinates();
            for (Coordinate location : adjacentCoordinates) {
                orderMap[location.getX()][location.getY()] = order;
                DebugInterface.print("RX", location);
            }
        }
        return activeOrders;
    }

    public void placeBarracks(Coordinate rbBuildCoordinates) {
        buildQueue.add(new Entity(-1, getMyId(), EntityType.RANGED_BASE, rbBuildCoordinates, 0, true));
    }
}
