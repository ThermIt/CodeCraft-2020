package mystrategy.maps.light;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.collections.SingleVisitCoordinateSet;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuildOrders {
    private List<Entity> buildQueue;
    private List<Entity> repairQueue;
    private Entity[][] orderMap;
    private int mapSize;
    private PlayerView playerView;
    private AllEntities entities;
    private EntitiesMap entitiesMap;

    private int repairs;
    private int builds;
    private int completed;
    private int active;
    private int inactive;

    public void init(PlayerView playerView, AllEntities entities, EntitiesMap entitiesMap) {
        this.playerView = playerView;
        this.entities = entities;
        this.entitiesMap = entitiesMap;
        if (mapSize != 0) {
            return;
        }
        mapSize = playerView.getMapSize();
        buildQueue = new ArrayList<>();
        repairQueue = new ArrayList<>();

        if (playerView.isRound1()) {
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 2), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(5, 2), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 5), 0, false));
        } else {
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 2), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(5, 2), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 5), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(8, 2), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 8), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(11, 2), 0, false));
//            buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(2, 11), 0, false));


/*
            int o=0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j <= i; j++) {
                    buildQueue.add(new Entity(-1, getMyId(), EntityType.HOUSE, new Coordinate(i*4, j*4), 0, false));
                    DebugInterface.println("X"+o++, j*4, (i-j)*4,2);
                }
            }
*/
        }
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
//        return true;
        if (playerView.isRound1()) {
            return true;
        } else if (playerView.isRound2()) {
            return (entities.getMyHouses().size() >= 4 && entities.getMyHouses().size() <= 6)
                    || entities.getMyRangedBases().size() > 0 && entities.getMyRangedBases().get(0).getHealth() > 10;
        }
        return (entities.getMyHouses().size() >= 4 && entities.getMyHouses().size() <= 8)
                || (entities.getMyRangedBases().size() > 0 && entities.getMyRangedBases().get(0).getHealth() > 10);
    }

    public List<Entity> updateAndGetActiveOrders(
            AllEntities allEntities,
            EntitiesMap entitiesMap,
            Player me,
            boolean needBarracks,
            boolean needHouses,
            SimCityPlan simCityPlan,
            EnemiesMap enemiesMap
    ) {
        repairQueue = new ArrayList<>();
        for (Entity building : allEntities.getMyBuildings()) { // add built outside
            if (!building.isActive()) {
                repairQueue.add(building);
            }
        }

        if (buildQueue.stream().anyMatch(order -> order.isMy(EntityType.RANGED_BASE))) {
            needBarracks = false;
        }
        if (needBarracks) {

        }

        int maxUnits = allEntities.getMaxUnits();
        int currentUnits = allEntities.getCurrentUnits();
        int resources = me.getResource();

        maxUnits += buildQueue.stream().collect(Collectors.summarizingInt(order -> order.getProperties().getPopulationProvide())).getSum();

        int housesNeeded = (int) Math.ceil(((currentUnits * 1.2 + resources / (maxUnits <= 50 ? 20.0 : 50.0) - maxUnits)) / 5);
        System.out.println(housesNeeded);
        System.out.println(maxUnits == 0 || (maxUnits - (currentUnits + resources / (maxUnits <= 150 ? 10 : 50))) * 100 / maxUnits < 20);

/*
        allEntities.getMyBuildings().stream()
                .noneMatch(ent -> ent.getProperties().getBuild() != null
                        && ent.getProperties().getBuild().getOptions()[0] == EntityType.RANGED_UNIT);
*/
        boolean needBarracks2 = allEntities.getMyBuildings().stream()
                .noneMatch(ent -> ent.getProperties().getBuild() != null
                        && ent.getProperties().getBuild().getOptions()[0] == EntityType.RANGED_UNIT
                        && ent.getHealth() > 30);

        if (housesNeeded > 0 && !(needBarracks2 && maxUnits > 30) && resources >= 49) {
            int[][] heat = new int[80][80];

            for (Entity worker : allEntities.getMyWorkers()) {
                SingleVisitCoordinateSet set = new SingleVisitCoordinateSet();
                set.add(worker.getPosition());
                int MAX_STEPS = 15;
                for (int i = 0; i < MAX_STEPS && set.size() > 0; i++) {
                    for (Coordinate loc : set) {
                        if (entitiesMap.isPassable(loc)) {
                            heat[loc.getX()][loc.getY()] += MAX_STEPS - i;
                            set.addOnNextStep(new Coordinate(loc.getX() + 1, loc.getY()));
                            set.addOnNextStep(new Coordinate(loc.getX() - 1, loc.getY()));
                            set.addOnNextStep(new Coordinate(loc.getX(), loc.getY() + 1));
                            set.addOnNextStep(new Coordinate(loc.getX(), loc.getY() - 1));
                        }
                    }
                    set.nextStep();
                }
            }

            Coordinate pos = null;
            int maxHeat = -1;
            for (int i = 0; i < 77; i++) {
                for (int j = 0; j < 77; j++) {

                    boolean enoughSpace;
                    if (i > 0 && j > 0) {
                        enoughSpace = simCityPlan.getEmptySize(i, j) >= 3 && (simCityPlan.getPassableSize(i - 1, j - 1) >= 5 || (i <= 8 && j <= 8));
                    } else {
                        enoughSpace = simCityPlan.getEmptySize(i, j) >= 3 && simCityPlan.getPassableSize(i, j) >= 4;
                    }
                    int cheat = Math.max(Math.max(heat[i][j], heat[i + 1][j + 1]), heat[i + 2][j + 2]);
                    if (cheat > 0 && enoughSpace) {
                        DebugInterface.println(cheat, i, j, 0);
                        if (cheat > maxHeat) {
                            maxHeat = cheat;
                            pos = new Coordinate(i, j);
                        }
                    }
                }
            }
            if (maxHeat > 0) {
                buildQueue.add(new Entity(-1, me.getId(), EntityType.HOUSE, pos, 0, false));
            }
/*
            for (int i = 0; i < 80; i++) {
                for (int j = 0; j < 80; j++) {
                    if (heat[i][j] > 0) {
                        DebugInterface.println(heat[i][j], i, j, 1);
                    }
                }
            }
*/
        }

        if (needBarracks && resources >= 490) {
            int[][] heat = new int[80][80];

            for (Entity worker : allEntities.getMyWorkers()) {
                SingleVisitCoordinateSet set = new SingleVisitCoordinateSet();
                set.add(worker.getPosition());
                int MAX_STEPS = 10;
                for (int i = 0; i < MAX_STEPS && set.size() > 0; i++) {
                    for (Coordinate loc : set) {
                        if (entitiesMap.isPassable(loc)) {
                            heat[loc.getX()][loc.getY()] += MAX_STEPS - i;
                            set.addOnNextStep(new Coordinate(loc.getX() + 1, loc.getY()));
                            set.addOnNextStep(new Coordinate(loc.getX() - 1, loc.getY()));
                            set.addOnNextStep(new Coordinate(loc.getX(), loc.getY() + 1));
                            set.addOnNextStep(new Coordinate(loc.getX(), loc.getY() - 1));
                        }
                    }
                    set.nextStep();
                }
            }

            Coordinate pos = null;
            int maxHeat = -1;
            int maxQ = -1;
            for (int i = 0; i < 75; i++) {
                for (int j = 0; j < 75; j++) {
                    int cheat = Math.max(Math.max(Math.max(heat[i][j], heat[i + 1][j + 1]), Math.max(heat[i + 2][j + 2], heat[i + 3][j + 3])), heat[i + 4][j + 4]);
                    if (cheat > 0 && simCityPlan.getEmptySize(i, j) >= 5) {
                        DebugInterface.println(cheat, i, j, 0);
                        int q = Math.min(i, j);
                        if (cheat > 2 && q > maxQ) {
                            maxHeat = cheat;
                            maxQ = q;
                            pos = new Coordinate(i, j);
                        }
                    }
                }
            }
            if (maxHeat > 0) {
                buildQueue.add(new Entity(-1, me.getId(), EntityType.RANGED_BASE, pos, 0, false));
            }
            for (int i = 0; i < 80; i++) {
                for (int j = 0; j < 80; j++) {
                    if (heat[i][j] > 0) {
                        DebugInterface.println(heat[i][j], i, j, 1);
                    }
                }
            }
        }
        boolean single = true;
        for (Iterator<Entity> iterator = buildQueue.iterator(); iterator.hasNext(); ) {
            Entity order = iterator.next();

            Entity entity = entitiesMap.getEntity(order.getPosition());
            if (simCityPlan.getPassableSize(order.getPosition().getX(), order.getPosition().getY()) < order.getProperties().getSize()/*entity.isMy(order.getEntityType())*/) {
                iterator.remove();
                continue;
            }

            if (!single) {
                order.setActive(false);
                continue;
            }

            if (entity.isMy(order.getEntityType()) && entity.getHealth() != entity.getProperties().getMaxHealth()) {
                order.setActive(true);
//                DebugInterface.print("A+", order.getPosition());
//                single = false;
            } else if (!entitiesMap.isOrderFree(order)) {
                order.setActive(false);
            } else if (me.getResource() >= order.getProperties().getInitialCost()
                    && !entity.isMy(order.getEntityType())) {
                order.setActive(true);
//                DebugInterface.print("A", order.getPosition());
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
//                DebugInterface.print("RX", location);
            }
        }
        return activeOrders;
    }
}
