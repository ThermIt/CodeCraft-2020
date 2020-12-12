package model;

public enum EntityType {
    INPENETRABLE(-1, false, false),
    WALL(0, true, false),
    HOUSE(1, true, false),
    BUILDER_BASE(2, true, false),
    BUILDER_UNIT(3, false, true),
    MELEE_BASE(4, true, false),
    MELEE_UNIT(5, false, true),
    RANGED_BASE(6, true, false),
    RANGED_UNIT(7, false, true),
    RESOURCE(8, false, false),
    TURRET(9, true, false);
    public int tag;
    private boolean building;
    private boolean unit;
    EntityProperties properties;

    EntityType(int tag, boolean building, boolean unit) {
        this.tag = tag;
        this.building = building;
        this.unit = unit;
    }

    public boolean isBuilding() {
        return building;
    }

    public boolean isUnit() {
        return unit;
    }

    public EntityProperties getProperties() {
        return properties;
    }

    public void setProperties(EntityProperties properties) {
        this.properties = properties;
    }
}
