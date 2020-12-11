package model;

public enum EntityType {
    INPENETRABLE(-1, false),
    WALL(0, true),
    HOUSE(1, true),
    BUILDER_BASE(2, true),
    BUILDER_UNIT(3, false),
    MELEE_BASE(4, true),
    MELEE_UNIT(5, false),
    RANGED_BASE(6, true),
    RANGED_UNIT(7, false),
    RESOURCE(8, false),
    TURRET(9, true);
    public int tag;
    private boolean isBuilding;

    EntityType(int tag, boolean isBuilding) {
        this.tag = tag;
        this.isBuilding = isBuilding;
    }

    public boolean isBuilding() {
        return isBuilding;
    }
}
