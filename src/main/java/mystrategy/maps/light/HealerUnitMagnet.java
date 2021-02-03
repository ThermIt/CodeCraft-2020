package mystrategy.maps.light;

import common.Constants;
import model.Coordinate;
import model.PlayerView;
import mystrategy.collections.AllEntities;
import mystrategy.collections.SingleVisitCoordinateSet;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import mystrategy.maps.Healers;
import util.DebugInterface;
import util.Initializer;

import java.util.Comparator;

public class HealerUnitMagnet {
    private final PlayerView playerView;
    private final WarMap warMap;
    private VisibilityMap visibility;
    private EntitiesMap entitiesMap;
    private AllEntities entities;
    private VirtualResources resources;
    private AllEntities allEntities;
    private int[][] distance;
    private int mapSize = 80;
    private SingleVisitCoordinateSet waveCoordinates;
    private Healers healers;
    private EnemiesMap enemiesMap;


    public HealerUnitMagnet(
            PlayerView playerView,
            VisibilityMap visibility,
            EntitiesMap entitiesMap,
            AllEntities entities,
            VirtualResources resources,
            WarMap warMap,
            Healers healers,
            EnemiesMap enemiesMap
    ) {
        this.enemiesMap = enemiesMap;
        warMap.checkTick(playerView);
        this.playerView = playerView;
        this.warMap = warMap;
        this.healers = healers;
        warMap.checkTick(playerView);
        this.visibility = visibility;
        this.entitiesMap = entitiesMap;
        this.entities = entities;
        this.resources = resources;
        this.allEntities = allEntities;
        this.distance = new int[Initializer.getMapSize()][Initializer.getMapSize()];

        waveCoordinates = new SingleVisitCoordinateSet();

        entities.getMyRangedUnits().stream()
                .filter(unit -> warMap.getDistanceToEnemy(unit.getPosition()) != 0 || unit.getHealth() < unit.getProperties().getMaxHealth())
                .sorted(Comparator.comparingInt(unit -> warMap.getDistanceToEnemy(unit.getPosition()) + 2 * unit.getHealth()))
                .limit(10)
                .forEach(unit -> waveCoordinates.add(unit.getPosition()));
        fillDistances();
    }

    private boolean isPassable(Coordinate coordinate) {
        return entitiesMap.isPassable(coordinate) && enemiesMap.getDangerLevel(coordinate) <= 1;
    }

    public void fillDistances() {
        if (!healers.isEnabled()) {
            return;
        }

        for (int i = 1; !waveCoordinates.isEmpty(); i++) {
            for (Coordinate coordinate : waveCoordinates) {
                if (coordinate.isInBounds()
                        && distance[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    distance[coordinate.getX()][coordinate.getY()] = i;
                    prettyprint(i, coordinate);
                    // do not attract to taken places
                    waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                    waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                    waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                    waveCoordinates.addOnNextStep(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                }
            }
            waveCoordinates.nextStep();

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled()) {
                    DebugInterface.println(distance[i][j], i, j, 1);
                }
            }
        }
*/
    }


    public void prettyprint(int i, Coordinate coordinate) {
/*
        if (i <= 16) {
            DebugInterface.println(i, coordinate, 1);
        }
*/
    }

    public int getDistance(Coordinate position) {
        return getDistance(position.getX(), position.getY());
    }

    public int getDistance(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return distance[x][y];
    }
}
