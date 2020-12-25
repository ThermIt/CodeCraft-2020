package mystrategy.chaos.control;

import model.Coordinate;
import model.MoveAction;

public enum MoveOrder {
    IDLE("o", true),
    MOVE_LEFT("<", false),
    MOVE_RIGHT(">", false),
    MOVE_UP("^", false),
    MOVE_DOWN("v", false),
    HARVEST_LEFT("<h", true),
    HARVEST_RIGHT(">h", true),
    HARVEST_UP("^h", true),
    HARVEST_DOWN("vh", true),
    REPAIR_LEFT("<b", true),
    REPAIR_RIGHT(">b", true),
    REPAIR_UP("^b", true),
    REPAIR_DOWN("vb", true);

    public static MoveOrder[] ALL_MOVES = {MOVE_DOWN, MOVE_UP, MOVE_LEFT, MOVE_RIGHT};
    private String displaySymbol;
    private boolean isStanding;

    MoveOrder(String displaySymbol, boolean isStanding) {
        this.displaySymbol = displaySymbol;
        this.isStanding = isStanding;
    }

    public boolean isOppositeOf(MoveOrder other) {
        return (this == MOVE_DOWN && other == MOVE_UP)
                || (this == MOVE_UP && other == MOVE_DOWN)
                || (this == MOVE_LEFT && other == MOVE_RIGHT)
                || (this == MOVE_RIGHT && other == MOVE_LEFT);
    }

    public MoveOrder getHarvestOrder() {
        switch (this) {
            case MOVE_LEFT:
                return HARVEST_LEFT;
            case MOVE_RIGHT:
                return HARVEST_RIGHT;
            case MOVE_UP:
                return HARVEST_UP;
            case MOVE_DOWN:
                return HARVEST_DOWN;
        }
        return this;
    }

    public MoveOrder getRepairOrder() {
        switch (this) {
            case MOVE_LEFT:
                return REPAIR_LEFT;
            case MOVE_RIGHT:
                return REPAIR_RIGHT;
            case MOVE_UP:
                return REPAIR_UP;
            case MOVE_DOWN:
                return REPAIR_DOWN;
        }
        return this;
    }

    public Coordinate nextPosition(Coordinate position) {
        switch (this) {
            case IDLE:
                return position;
            case MOVE_LEFT:
                return new Coordinate(position.getX() - 1, position.getY());
            case MOVE_RIGHT:
                return new Coordinate(position.getX() + 1, position.getY());
            case MOVE_UP:
                return new Coordinate(position.getX(), position.getY() + 1);
            case MOVE_DOWN:
                return new Coordinate(position.getX(), position.getY() - 1);
        }
        return position;
    }

    @Override
    public String toString() {
        return displaySymbol;
    }

    public MoveAction getMoveAction(Coordinate position) {
        switch (this) {
            case MOVE_LEFT:
            case HARVEST_LEFT:
                return new MoveAction(new Coordinate(position.getX() - 1, position.getY()), false, true);
            case MOVE_RIGHT:
            case HARVEST_RIGHT:
                return new MoveAction(new Coordinate(position.getX() + 1, position.getY()), false, true);
            case MOVE_UP:
            case HARVEST_UP:
                return new MoveAction(new Coordinate(position.getX(), position.getY() + 1), false, true);
            case MOVE_DOWN:
            case HARVEST_DOWN:
                return new MoveAction(new Coordinate(position.getX(), position.getY() - 1), false, true);
        }
        return null;
    }

    public boolean isStanding() {
        return isStanding;
    }
}
