package model;

import util.StreamUtil;

public class BuildAction {
    private model.EntityType entityType;
    public model.EntityType getEntityType() { return entityType; }
    public void setEntityType(model.EntityType entityType) { this.entityType = entityType; }
    private Coordinate position;
    public Coordinate getPosition() { return position; }
    public void setPosition(Coordinate position) { this.position = position; }
    public BuildAction() {}
    public BuildAction(model.EntityType entityType, Coordinate position) {
        this.entityType = entityType;
        this.position = position;
    }
    public static BuildAction readFrom(java.io.InputStream stream) throws java.io.IOException {
        BuildAction result = new BuildAction();
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
        return result;
    }
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        StreamUtil.writeInt(stream, entityType.tag);
        position.writeTo(stream);
    }
}
