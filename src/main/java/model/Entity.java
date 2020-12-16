package model;

import util.StreamUtil;

import static util.Initializer.getMyId;

public class Entity {
    private int id;
    private Integer playerId;
    private model.EntityType entityType;
    private Coordinate position;
    private int health;
    private int accumulatedDamage;
    private boolean active;

    // attack -> build -> repair -> move
    private AttackAction attackAction = null;
    private BuildAction buildAction = null;
    private RepairAction repairAction = null;
    private MoveAction moveAction = null;

    public Entity() {
    }

    public Entity(int id, Integer playerId, model.EntityType entityType, Coordinate position, int health, boolean active) {
        this.id = id;
        this.playerId = playerId;
        this.entityType = entityType;
        this.position = position;
        this.health = health;
        this.active = active;
    }

    public static Entity readFrom(java.io.InputStream stream) throws java.io.IOException {
        Entity result = new Entity();
        result.id = StreamUtil.readInt(stream);
        if (StreamUtil.readBoolean(stream)) {
            result.playerId = StreamUtil.readInt(stream);
        } else {
            result.playerId = null;
        }
        switch (StreamUtil.readInt(stream)) {
            case 0:
                result.entityType = model.EntityType.WALL;
                break;
            case 1:
                result.entityType = model.EntityType.HOUSE;
                break;
            case 2:
                result.entityType = model.EntityType.BUILDER_BASE;
                break;
            case 3:
                result.entityType = model.EntityType.BUILDER_UNIT;
                break;
            case 4:
                result.entityType = model.EntityType.MELEE_BASE;
                break;
            case 5:
                result.entityType = model.EntityType.MELEE_UNIT;
                break;
            case 6:
                result.entityType = model.EntityType.RANGED_BASE;
                break;
            case 7:
                result.entityType = model.EntityType.RANGED_UNIT;
                break;
            case 8:
                result.entityType = model.EntityType.RESOURCE;
                break;
            case 9:
                result.entityType = model.EntityType.TURRET;
                break;
            default:
                throw new java.io.IOException("Unexpected tag value");
        }
        result.position = Coordinate.readFrom(stream);
        result.health = StreamUtil.readInt(stream);
        result.active = StreamUtil.readBoolean(stream);
        return result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public model.EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(model.EntityType entityType) {
        this.entityType = entityType;
    }

    public Coordinate getPosition() {
        return position;
    }

    public void setPosition(Coordinate position) {
        this.position = position;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        StreamUtil.writeInt(stream, id);
        if (playerId == null) {
            StreamUtil.writeBoolean(stream, false);
        } else {
            StreamUtil.writeBoolean(stream, true);
            StreamUtil.writeInt(stream, playerId);
        }
        StreamUtil.writeInt(stream, entityType.tag);
        position.writeTo(stream);
        StreamUtil.writeInt(stream, health);
        StreamUtil.writeBoolean(stream, active);
    }

    public boolean isPlayer(int playerId) {
        return this.playerId != null && this.playerId == playerId;
    }

    public boolean isMy() {
        return this.playerId != null && this.playerId == getMyId();
    }

    public boolean isMy(EntityType entityType) {
        return this.playerId != null && this.playerId == getMyId() && getEntityType() == entityType;
    }

    public boolean isEnemy() {
        return this.playerId != null && this.playerId != getMyId();
    }

    public boolean isBuilding() {
        return getEntityType().isBuilding();
    }

    public EntityProperties getProperties() {
        return getEntityType().getProperties();
    }

    public MoveAction getMoveAction() {
        return moveAction;
    }

    public void setMoveAction(MoveAction moveAction) {
        this.moveAction = moveAction;
    }

    public BuildAction getBuildAction() {
        return buildAction;
    }

    public void setBuildAction(BuildAction buildAction) {
        this.buildAction = buildAction;
    }

    public RepairAction getRepairAction() {
        return repairAction;
    }

    public void setRepairAction(RepairAction repairAction) {
        this.repairAction = repairAction;
    }

    public AttackAction getAttackAction() {
        return attackAction;
    }

    public void setAttackAction(AttackAction attackAction) {
        this.attackAction = attackAction;
    }

    public boolean hasAction() {
        return attackAction != null || repairAction != null || moveAction != null || buildAction != null;
    }

    public int getHealthAfterDamage() {
        return health - accumulatedDamage;
    }

    public void increaseDamage(int damage) {
        accumulatedDamage += damage;
    }
}
