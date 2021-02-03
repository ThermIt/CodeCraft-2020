package older.v04.smart.rusher.maps.light;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import older.v04.smart.rusher.Constants;
import older.v04.smart.rusher.collections.AllEntities;
import util.DebugInterface;

import java.util.HashSet;
import java.util.Set;

public class VisibilityMap {

    private boolean isFogOfWar;

    private int[][] visibilityRange;
    private int[][] lastSeen;
    private int mapSize;
    private int tick;

    public int getTick() {
        return tick;
    }

    public void init(PlayerView playerView, AllEntities allEntities) {
        isFogOfWar = playerView.isFogOfWar();
        tick = playerView.getCurrentTick();

        // calculate anyway to filter enemies by range

        mapSize = playerView.getMapSize();
        int[][] remainingRange = new int[mapSize][mapSize];
        visibilityRange = new int[mapSize][mapSize];
        if (lastSeen == null) {
            lastSeen = new int[mapSize][mapSize];
        }

        Set<Coordinate> adjacentPositions = new HashSet<>(128);
        for (Entity unit : allEntities.getMyEntities()) {
            for (Coordinate position : unit.getAdjacentCoordinates()) {
                adjacentPositions.add(position);
                remainingRange[position.getX()][position.getY()] =
                        Math.max(
                                remainingRange[position.getX()][position.getY()],
                                unit.getProperties().getSightRange()
                        );
                visibilityRange[position.getX()][position.getY()] = 1;
                lastSeen[position.getX()][position.getY()] = tick + 1; // more than 0
            }
        }

        Set<Coordinate> coordinateList = adjacentPositions;
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                int previouslyRemainingRange = remainingRange[coordinate.getX()][coordinate.getY()];
                if (coordinate.isInBounds() && previouslyRemainingRange > 0) {
                    addNextCoordinate(remainingRange, i, coordinateListNext, previouslyRemainingRange,
                            new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                    addNextCoordinate(remainingRange, i, coordinateListNext, previouslyRemainingRange,
                            new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                    addNextCoordinate(remainingRange, i, coordinateListNext, previouslyRemainingRange,
                            new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                    addNextCoordinate(remainingRange, i, coordinateListNext, previouslyRemainingRange,
                            new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                }
            }
            coordinateList = coordinateListNext;

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }

    }

    private void addNextCoordinate(
            int[][] remainingRange,
            int i,
            Set<Coordinate> coordinateListNext,
            int previouslyRemainingRange,
            Coordinate pos
    ) {
        if (pos.isInBounds()
                && remainingRange[pos.getX()][pos.getY()] < previouslyRemainingRange - 1
        ) {
            if (visibilityRange[pos.getX()][pos.getY()] == 0) {
                visibilityRange[pos.getX()][pos.getY()] = i + 1;
                lastSeen[pos.getX()][pos.getY()] = tick + 1; // more than 0
            }
            remainingRange[pos.getX()][pos.getY()] = previouslyRemainingRange - 1;
            coordinateListNext.add(pos);
        }
    }

    public boolean isVisible(int x, int y) {
        if (!isFogOfWar) {
            return true;
        }
        return visibilityRange[x][y] > 0;
    }

    public boolean isVisible(Coordinate location) {
        return isVisible(location.getX(), location.getY());
    }

    public void checkTick(PlayerView playerView) {
        if (getTick() != playerView.getCurrentTick()) {
            throw new RuntimeException("visibility is not initialized");
        }
    }
}
