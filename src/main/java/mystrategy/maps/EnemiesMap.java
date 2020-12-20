package mystrategy.maps;

import model.Coordinate;
import model.Entity;
import model.EntityProperties;
import model.PlayerView;
import mystrategy.collections.AllEntities;

import java.util.HashSet;
import java.util.Set;

public class EnemiesMap {
    private final EntitiesMap entitiesMap;
    private final AllEntities entities;
    private int[][] shootDangerNextTick;
    private int[][] shootDanger;
    private int mapSize;

    public EnemiesMap(PlayerView playerView, EntitiesMap entitiesMap, AllEntities entities) {
        mapSize = playerView.getMapSize();
        this.entitiesMap = entitiesMap;
        this.entities = entities;
        shootDanger = new int[mapSize][mapSize];

        Set<Coordinate> shoot5Coordinates = new HashSet<>(128);
        Set<Coordinate> shoot1Coordinates = new HashSet<>(128);
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (entitiesMap.getIsEnemy(i, j)) {
                    Coordinate position = new Coordinate(i, j);

                    EntityProperties properties = entitiesMap.getEntity(i, j).getProperties();
                    if (properties.getAttack() != null
                            && properties.getAttack().getDamage() > 1) {
                        if (properties.getAttack().getAttackRange() >= 5) {
                            shoot5Coordinates.add(position);
                        } else {
                            shoot1Coordinates.add(position);
                        }
                    }
                }
            }
        }

        fillShootDanger(shootDanger, shoot5Coordinates, shoot1Coordinates);
/*
        List<Coordinate> coordinates = Arrays.stream(playerView.getEntities()).filter(ent -> ent.getPlayerId() != playerView.getMyId())
                .map(box -> new Coordinate(box.getPosition())).collect(Collectors.toList());
*/

        shootDangerNextTick = new int[mapSize][mapSize];
        for (Entity enemy : entities.getEnemyAttackers()) {
            int size = enemy.getProperties().getSize();
            int attackRange = enemy.getProperties().getAttack().getAttackRange();
            int attackDamage = enemy.getProperties().getAttack().getDamage();
            int x = enemy.getPosition().getX();
            int y = enemy.getPosition().getY();

            if (enemy.isActive() && enemy.getHealth() > 10) { /* fat bonus */
                attackDamage += 5;
            }

            int health;
            if (size == 1) {
                health = calculateNeighbors(attackRange + 2, x, y, size);
            } else {
                health = calculateNeighbors(attackRange + 1, x, y, size);
            }
            if (health >= 60) {
                continue;
            }

            if (size == 1) {
                fillShootDangerForSize1(attackRange + 1 /*mobile bonus*/, attackDamage, x, y);
            } else {
                fillShootDangerForSize2(attackRange, attackDamage, x, y, size);
            }
        }

/*
        if (DebugInterface.isDebugEnabled()) {
            for (int i = 0; i < mapSize; i++) {
                for (int j = 0; j < mapSize; j++) {
                    if (shootDangerNextTick[i][j] > 0) {
                        DebugInterface.print(Integer.toString(shootDangerNextTick[i][j]), i, j);
                    }
                }
            }
        }
*/
    }

    public void addDamage(int[][] map, int attackDamage, int x, int y) {
        if (x < mapSize && y < mapSize && x >= 0 && y >= 0) {
            map[x][y] += attackDamage;
        }
    }

    public void fillShootDangerForSize1(int attackRange, int attackDamage, int x, int y) {
        for (int i = 1; i <= attackRange; i++) {
            int rangeY = attackRange - i;
            addDamage(shootDangerNextTick, attackDamage, x + i, y);
            addDamage(shootDangerNextTick, attackDamage, x - i, y);
            addDamage(shootDangerNextTick, attackDamage, x, y + i);
            addDamage(shootDangerNextTick, attackDamage, x, y - i);
            for (int j = 1; j <= rangeY; j++) {
                addDamage(shootDangerNextTick, attackDamage, x + i, y + j);
                addDamage(shootDangerNextTick, attackDamage, x - i, y + j);
                addDamage(shootDangerNextTick, attackDamage, x + i, y - j);
                addDamage(shootDangerNextTick, attackDamage, x - i, y - j);
            }
        }
    }

    public void fillShootDangerForSize2(int attackRange, int attackDamage, int x, int y, int size) {
        int SIZE_DELTA = size - 1;
        for (int i = 1; i <= attackRange; i++) {
            int rangeY = attackRange - i;
            for (int j = 0; j < size; j++) {
                addDamage(shootDangerNextTick, attackDamage, x + i + SIZE_DELTA, y + j);
                addDamage(shootDangerNextTick, attackDamage, x - i, y + j);
                addDamage(shootDangerNextTick, attackDamage, x + j, y + i + SIZE_DELTA);
                addDamage(shootDangerNextTick, attackDamage, x + j, y - i);
            }
            for (int j = 1; j <= rangeY; j++) {
                addDamage(shootDangerNextTick, attackDamage, x + i + SIZE_DELTA, y + j + SIZE_DELTA);
                addDamage(shootDangerNextTick, attackDamage, x - i, y + j + SIZE_DELTA);
                addDamage(shootDangerNextTick, attackDamage, x + i + SIZE_DELTA, y - j);
                addDamage(shootDangerNextTick, attackDamage, x - i, y - j);
            }
        }
    }

    public int calculateNeighbors(int attackRange, int x, int y, int size) {
        int SIZE_DELTA = size - 1;
        int result = 0;
        for (int i = 1; i <= attackRange; i++) {
            int rangeY = attackRange - i;
            for (int j = 0; j < size; j++) {
                result += getHealthOfMyUnit(i + j, x + i + SIZE_DELTA, y + j);
                result += getHealthOfMyUnit(i + j, x - i, y + j);
                result += getHealthOfMyUnit(i + j, x + j, y + i + SIZE_DELTA);
                result += getHealthOfMyUnit(i + j, x + j, y - i);
            }
            for (int j = 1; j <= rangeY; j++) {
                result += getHealthOfMyUnit(i + j, x + i + SIZE_DELTA, y + j + SIZE_DELTA);
                result += getHealthOfMyUnit(i + j, x - i, y + j + SIZE_DELTA);
                result += getHealthOfMyUnit(i + j, x + i + SIZE_DELTA, y - j);
                result += getHealthOfMyUnit(i + j, x - i, y - j);
            }
        }
        return result;
    }

    private int getHealthOfMyUnit(int distance, int x, int y) {
        Entity entity = entitiesMap.getEntity(x, y);
        if (entity.isUnit() && entity.isMy()) {
            return entity.getHealth();
        }
        if (entity.isBuilding() && entity.isMy() && distance <= 5) { // protect buildings
            return 100;
        }
        return 0;
    }


    private void fillShootDanger(
            int[][] dangerMap, Set<Coordinate> coordinateList5,
            Set<Coordinate> coordinateList1
    ) {
        int delta = 1;
        Set<Coordinate> coordinateList = coordinateList5;
        for (int i = 6 + delta; i > 0; i--) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            if (i == 3 + delta) {
                coordinateListNext.addAll(coordinateList1);
            }
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds() && dangerMap[coordinate.getX()][coordinate.getY()] < i) {
                    dangerMap[coordinate.getX()][coordinate.getY()] = i;
                    coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                    coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                }
            }
            coordinateList = coordinateListNext;
        }

//        if (DebugInterface.isDebugEnabled()) {
//            for (int i = 0; i < mapSize; i++) {
//                for (int j = 0; j < mapSize; j++) {
//                    if (shootDanger[i][j] > 0) {
//                        DebugInterface.print(Integer.toString(shootDanger[i][j]), i, j);
//                    }
//                }
//            }
//        }
    }

    public int getDangerLevel(Coordinate from) {
        if (from.isOutOfBounds()) {
            return 0;
        }
        return shootDanger[from.getX()][from.getY()];
    }

    public int getDamageOnNextTick(Coordinate newPosition) {
        if (newPosition.isOutOfBounds()) {
            return 0;
        }
        return shootDangerNextTick[newPosition.getX()][newPosition.getY()];
    }
}
