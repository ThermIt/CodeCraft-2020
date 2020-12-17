package mystrategy.maps.light;

import model.Coordinate;
import model.Entity;
import model.PlayerView;
import mystrategy.collections.AllEntities;

import java.util.HashSet;
import java.util.Set;

public class VisibilityMap {

    private boolean isFogOfWar;

    private int[][] visibilityRange;
    private int mapSize;

    public VisibilityMap(
            PlayerView playerView,
            AllEntities allEntities
    ) {
        init(playerView, allEntities);
    }

    public void init(PlayerView playerView, AllEntities allEntities) {
        isFogOfWar = playerView.isFogOfWar();

        // calculate anyway to filter enemies by range

        mapSize = playerView.getMapSize();
        int[][] remainingRange = new int[mapSize][mapSize];
        visibilityRange = new int[mapSize][mapSize];

        Set<Coordinate> adjacentPositions = new HashSet<>();
        for (Entity unit : allEntities.getMyEntities()) {
            for (Coordinate position : unit.getAdjacentCoordinates()) {
                adjacentPositions.add(position);
                remainingRange[position.getX()][position.getY()] =
                        Math.max(
                                remainingRange[position.getX()][position.getY()],
                                unit.getProperties().getSightRange()
                        );
                visibilityRange[position.getX()][position.getY()] = 1;
            }
        }

        Set<Coordinate> coordinateList = adjacentPositions;
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>();
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
        }

/*
        if (DebugInterface.isDebugEnabled()) {
            for (int i = 0; i < mapSize; i++) {
                for (int j = 0; j < mapSize; j++) {
                    if (visibilityRange[i][j] > 0) {
                        DebugInterface.print(Integer.toString(visibilityRange[i][j]), i, j);
                    }
                }
            }
        }
*/
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
            }
            remainingRange[pos.getX()][pos.getY()] = previouslyRemainingRange - 1;
            coordinateListNext.add(pos);
        }
    }
}
