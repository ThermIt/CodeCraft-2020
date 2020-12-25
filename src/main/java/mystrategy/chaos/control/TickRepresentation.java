package mystrategy.chaos.control;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.maps.light.HarvestJobsMap;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static util.Initializer.getMe;

public class TickRepresentation {
    final int MAP_SIZE = 80;
    int tickNumber;
    int foodAmount;
    int resourcesAmount;
    int resourcesSpent;
    int resourcesProduced;
    List<Entity> buildOrders = new ArrayList<>();
    MapEntry[][] map;
    TickRepresentation nextTick;
    TickRepresentation previousTick;
    private HarvestJobsMap harvestersMap;

    public TickRepresentation() {
    }

    public TickRepresentation(TickRepresentation previousTick) {
        this.tickNumber = previousTick.tickNumber + 1;
        this.foodAmount = previousTick.foodAmount;
        this.resourcesAmount = previousTick.resourcesAmount + previousTick.resourcesProduced - previousTick.resourcesSpent;
        this.resourcesSpent = 0;
        this.resourcesProduced = 0;
        this.map = new MapEntry[MAP_SIZE][MAP_SIZE];
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                MapEntry mapEntry = previousTick.map[i][j];
                if (mapEntry != null) {
                    map[i][j] = new MapEntry(mapEntry);
                }
            }
        }
    }

    public void init(PlayerView playerView, HarvestJobsMap harvestersMap) {
        this.tickNumber = playerView.getCurrentTick();
        this.harvestersMap = harvestersMap;
        this.foodAmount = 0;
        this.resourcesSpent = 0;
        this.resourcesProduced = 0;
        this.resourcesAmount = getMe().getResource();
        this.map = new MapEntry[MAP_SIZE][MAP_SIZE];

        AllEntities entities = new AllEntities(playerView);
        for (Entity entity : playerView.getEntities()) {
            int size = entity.getProperties().getSize();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    map[entity.getPosition().getX() + i][entity.getPosition().getY() + j] = new MapEntry(entity);
                }
            }
            if (entity.isMy()) {
                this.foodAmount += entity.getProperties().getPopulationProvide();
                this.foodAmount -= entity.getProperties().getPopulationUse();
            }
        }
        for (Entity worker : entities.getMyWorkers()) {
            fillWorkerOrders(worker);
        }
    }

    public void update(PlayerView playerView, HarvestJobsMap harvestersMap) {
        if (tickNumber != playerView.getCurrentTick()) {
            System.out.println("Tick number mismatch");
            this.tickNumber = playerView.getCurrentTick();
        }
        this.harvestersMap = harvestersMap;
        int foodAmount = 0;
//        this.resourcesSpent = 0;
//        this.resourcesProduced = 0;
//        this.resourcesAmount = getMe().getResource();

        MapEntry[][] oldMap = this.map;
        MapEntry[][] newMap = new MapEntry[MAP_SIZE][MAP_SIZE];

        AllEntities entities = new AllEntities(playerView);
        for (Entity entity : playerView.getEntities()) {
            int size = entity.getProperties().getSize();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    newMap[entity.getPosition().getX() + i][entity.getPosition().getY() + j] = getMapEntry(entity);
                }
            }
            if (entity.isMy()) {
                foodAmount += entity.getProperties().getPopulationProvide();
                foodAmount -= entity.getProperties().getPopulationUse();
            }
        }
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                MapEntry newMapValue = newMap[i][j];
                MapEntry oldMapValue = oldMap[i][j];
                if (newMapValue != null && oldMapValue != null) {
                    // sync order
                    newMapValue.setOrder(oldMapValue.getOrder());
                }
                Integer newMapId = (newMapValue != null && newMapValue.isTaken()) ? newMapValue.getEntity().getId() : null;
                Integer oldMapId = (oldMapValue != null && oldMapValue.isTaken()) ? oldMapValue.getEntity().getId() : null;
                if (!Objects.equals(newMapId, oldMapId)) {
                    DebugInterface.printRed("X", i, j);
                    if (newMapId == null) {
                        clearEntity(new Coordinate(i, j), oldMapId);
                    }
                    if (oldMapId == null) {
                        updateEntity(new Coordinate(i, j), newMapValue);
                    }
                } else if (newMapId != null) {
                    newMapValue.getEntity().setOrderSequence(oldMapValue.getEntity().getOrderSequence());
                    if (newMapValue.getEntity().getHealth() != oldMapValue.getEntity().getHealth()) {
                        updateEntity(new Coordinate(i, j), newMapValue);
                    }
                }
            }
        }
        this.map = map;
        if (this.foodAmount != foodAmount) {
//            System.out.println("Food mismatch, " + this.foodAmount + " -> " + foodAmount);
            this.foodAmount = foodAmount;
        }
        if (this.resourcesAmount != getMe().getResource()) {
//            System.out.println("Resource mismatch, " + this.foodAmount + " -> " + foodAmount);
            this.resourcesAmount = getMe().getResource();
        }
        for (Entity worker : entities.getMyWorkers()) {
            fillWorkerOrders(worker);
        }
    }

    private void  updateEntity(Coordinate coordinate, MapEntry newValue) {
        MapEntry old = map[coordinate.getX()][coordinate.getY()];
        if (old != null && old.isTaken()) {
            clearEntity(coordinate, old.getEntity().getId());
        }
        map[coordinate.getX()][coordinate.getY()] = newValue;
        if (nextTick != null) {
            nextTick.updateEntity(coordinate, new MapEntry(newValue));
        }
    }

    public MapEntry getMapEntry(Entity entity) {
        return new MapEntry(entity);
    }

    private void fillWorkerOrders(Entity worker) {
        if (worker.getOrderSequence() != null && worker.getOrderSequence().size() > 0) {
            MoveOrder moveOrder = worker.getOrderSequence().get(0);
            worker.getOrderSequence().remove(0);

            MoveAction moveAction = moveOrder.getMoveAction(worker.getPosition());
            if (moveAction != null && moveAction.isValid()) {
                worker.setMoveAction(moveAction);
                return;
            }
        }
        DummyWorker printer = getMostValuablePathForWorker(worker);
        if (printer == null) {
            // need recalculation
            return;
        }
        List<MoveOrder> orderSequence = new ArrayList<>();

//        DebugInterface.print("x", printer.position);
        while (printer != null) {
            if (printer.order != null) {
                orderSequence.add(0, printer.order);
            }
            printer = printer.parent;
        }
        MoveOrder moveOrder = orderSequence.get(0);
        updateWorkerLocation(worker, orderSequence);
        worker.setOrderSequence(orderSequence);
        orderSequence.remove(0);
        worker.setMoveAction(moveOrder.getMoveAction(worker.getPosition()));
    }

    private void updateWorkerLocation(Entity worker, List<MoveOrder> orderSequence) {
        Coordinate position = worker.getPosition();
        clearEntity(position, worker.getId());
        TickRepresentation tick = this;
        MapEntry mapEntry;
        for (MoveOrder order : orderSequence) {
            mapEntry = new MapEntry(worker);
            mapEntry.setOrder(order);
            tick.map[position.getX()][position.getY()] = mapEntry;

//            DebugInterface.print(mapEntry.getOrder().toString(), position);
            tick = tick.nextTick;
            Coordinate nextPosition = order.nextPosition(position);
            if (nextPosition.isOutOfBounds()) {
                continue;
            }
            MapEntry nextTickEntry = tick.lookAtTheMap(nextPosition);
            if (!nextTickEntry.isTaken()) {
                position = nextPosition;
            } else {
                if (nextTickEntry.getEntity().getEntityType() != EntityType.RESOURCE) {
                    System.out.println("Harvesting unit is impossible");
                }
            }
        }
//        DebugInterface.print(MoveOrder.IDLE.toString(), position);
        while (tick != null) {
            mapEntry = new MapEntry(worker);
            mapEntry.setOrder(MoveOrder.IDLE);
            tick.map[position.getX()][position.getY()] = mapEntry;
            tick = tick.nextTick;
        }
    }

    private void clearEntity(Coordinate position, int id) {
        MapEntry mapEntry = lookAtTheMap(position);
        if (!mapEntry.isTaken()) {
            return;
        }
        if (mapEntry.getEntity().getId() != id) {
            System.out.println("Wrong entity");
            position.getAdjacentListWithSelf().forEach(pos -> {
                MapEntry backupEntry = lookAtTheMap(pos);
                if (backupEntry.isTaken() && backupEntry.getEntity().getId() == id) {
                    clearEntity(pos, id);
                }
            });
            return;
        }
        MoveOrder order = mapEntry.getOrder();
        mapEntry.clear();
        Coordinate nextPosition = order.nextPosition(position);
        if (nextPosition.isOutOfBounds()) {
            System.out.println("Clearing out of bounds");
        }
        if (nextTick != null && nextPosition.isInBounds()) { // should be in bounds
            nextTick.clearEntity(nextPosition, id);
        }
    }

    private DummyWorker getMostValuablePathForWorker(Entity worker) {
        int PREDICTION_SIZE = 60;
        List<DummyWorker> dummies = new ArrayList<>();
        DummyWorker workerDummy = new DummyWorker(worker);
        dummies.add(workerDummy);
        TickRepresentation currentTick = this;
        TickRepresentation nextTick = currentTick.getNextTick();

        for (int i = 0; i < PREDICTION_SIZE; i++) {
            DummyWorker[][] dummiesMapNext = new DummyWorker[MAP_SIZE][MAP_SIZE];
            List<DummyWorker> nextTickDummies = new ArrayList<>();
            for (DummyWorker dummy : dummies) {

                for (MoveOrder possibleOrder : MoveOrder.ALL_MOVES) {
                    Coordinate nextPosition = possibleOrder.nextPosition(dummy.getPosition());
                    if (nextPosition.isInBounds()) {
                        MapEntry currentMapNextPosition = currentTick.lookAtTheMap(nextPosition);
                        if (!possibleOrder.isOppositeOf(currentMapNextPosition.order)) {
                            MapEntry nextMap = nextTick.lookAtTheMap(nextPosition);

                            if (nextMap.getEntity() != null && nextMap.getEntity().getHealth() < nextMap.getEntity().getProperties().getMaxHealth()) {
//                                replaceDummy(dummiesMapNext, nextTickDummies, dummy, possibleOrder.getRepairOrder(), dummy.getPosition(), dummy.getScore() + 1/* REPAIR */);
                                return new DummyWorker(dummy, nextPosition, dummy.getScore() + 1, possibleOrder.getRepairOrder());
                            } else if (nextMap.getResourceAmount() > 0) {
                                // declare winner?
                                MapEntry currentMapCurrentPosition = currentTick.lookAtTheMap(dummy.getPosition());
                                if (currentMapCurrentPosition.isPassableBy(worker.getId())) {
//                                    replaceDummy(dummiesMapNext, nextTickDummies, dummy, possibleOrder.getHarvestOrder(), dummy.getPosition(), dummy.getScore()/* HARVEST */);
                                    return new DummyWorker(dummy, nextPosition, dummy.getScore() + 1, possibleOrder.getHarvestOrder());
                                }
                            } else if (nextMap.isPassableBy(worker.getId())) {
                                replaceDummy(dummiesMapNext, nextTickDummies, dummy, possibleOrder, nextPosition, dummy.getScore() - 2/* MOVE */);
                            }/* else if (nextMap.getEntity().getEntityType() == EntityType.BUILDER_UNIT && nextMap.getOrder().isStanding()) {
                                replaceDummy(dummiesMapNext, nextTickDummies, dummy, possibleOrder, nextPosition, dummy.getScore() - 5*//* DISPLACE *//*);
                            }*/
                        }
                    }
                }
                // stay still
                Coordinate nextPosition = dummy.getPosition();
                MapEntry nextMap = nextTick.lookAtTheMap(nextPosition);
                if (nextMap.isPassableBy(worker.getId())) {
                    replaceDummy(dummiesMapNext, nextTickDummies, dummy, MoveOrder.IDLE, dummy.getPosition(), dummy.getScore() - 1/* IDLE */);
                }
            }

            currentTick = nextTick;
            nextTick = currentTick.getNextTick();
            dummies = nextTickDummies;
        }
        int maxScore = dummies.stream().map(DummyWorker::getScore).max(Integer::compare).orElse(0);
        return dummies.stream()
                .filter(dum -> dum.getScore() == maxScore)
                .min(Comparator.comparingInt(dum -> harvestersMap.getDistance(dum.getPosition())))
                .orElse(null);
    }

    private void replaceDummy(
            DummyWorker[][] dummiesMapNext,
            List<DummyWorker> nextTickDummies,
            DummyWorker parent,
            MoveOrder possibleOrder,
            Coordinate nextPosition,
            int score
    ) {
        DummyWorker nextTickDummy = dummiesMapNext[nextPosition.getX()][nextPosition.getY()];
        if (nextTickDummy == null || nextTickDummy.getScore() < score) {
            if (nextTickDummy != null) {
                nextTickDummies.remove(nextTickDummy);
            }
            DummyWorker newDummy = new DummyWorker(parent, nextPosition, score, possibleOrder);
            nextTickDummies.add(newDummy);
            dummiesMapNext[nextPosition.getX()][nextPosition.getY()] = newDummy;
        }
    }

    public TickRepresentation getNextTick() {
        if (nextTick == null) {
            nextTick = new TickRepresentation(this);
        }
        return nextTick;
    }

    public MapEntry lookAtTheMap(Coordinate position) {
        return lookAtTheMap(position.getX(), position.getY());
    }

    public MapEntry lookAtTheMap(int x, int y) {
        if (map[x][y] == null) {
            map[x][y] = new MapEntry();
        }
        return map[x][y];
    }
}
