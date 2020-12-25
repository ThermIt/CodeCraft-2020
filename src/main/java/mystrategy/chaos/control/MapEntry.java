package mystrategy.chaos.control;

import model.Entity;
import model.EntityType;

public class MapEntry {
    Entity entity;
    MoveOrder order;
    int resourceAmount;
    boolean needRecalculation;

    public MapEntry() {
    }

    public MapEntry(Entity entity) {
        this.entity = entity;
        this.order = MoveOrder.IDLE;
        if (entity.getEntityType() == EntityType.RESOURCE) {
            this.resourceAmount = entity.getHealth();
        }
    }

    public MapEntry(MapEntry mapEntry) {
        this.entity = mapEntry.entity;
        this.order = MoveOrder.IDLE;
        this.resourceAmount = mapEntry.resourceAmount;
        this.needRecalculation = mapEntry.needRecalculation;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public boolean isTaken() {
        return entity != null;
    }

    public MoveOrder getOrder() {
        if (order == null) {
            return MoveOrder.IDLE;
        }
        return order;
    }

    public void setOrder(MoveOrder order) {
        this.order = order;
    }

    public int getResourceAmount() {
        return resourceAmount;
    }

    public void setResourceAmount(int resourceAmount) {
        this.resourceAmount = resourceAmount;
    }

    public boolean isNeedRecalculation() {
        return needRecalculation;
    }

    public void setNeedRecalculation(boolean needRecalculation) {
        this.needRecalculation = needRecalculation;
    }

    public boolean isPassableBy(int entityId) {
        return !isTaken() || entity.getId() == entityId;
    }

    @Override
    public String toString() {
        return "Map{" + (isTaken() ? "is taken" : "free") +
                ", " + order +
                ", resourceAmount=" + resourceAmount +
                '}';
    }

    public void clear() {
        entity = null;
        order = MoveOrder.IDLE;
        resourceAmount = 0;
    }
}
