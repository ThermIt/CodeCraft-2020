package mystrategy.collections;

import model.Entity;
import model.EntityType;
import model.PlayerView;
import util.DebugInterface;

import java.util.ArrayList;
import java.util.List;

public class AllEntities {

    private static List<Entity> myRangedUnitsOld = new ArrayList<>();
    private static List<Entity> myHalfRangedUnitsOld = new ArrayList<>();
    private static int deadTotal = 0;
    private static int hdeadTotal = 0;
    private int myId;
    private List<Entity> resources = new ArrayList<>();
    private List<Entity> myEntities = new ArrayList<>();
    private List<Entity> myUnits = new ArrayList<>();
    private List<Entity> myAttackers = new ArrayList<>();
    private List<Entity> myBuildings = new ArrayList<>();
    private List<Entity> myActors = new ArrayList<>();
    private List<Entity> myBuilders = new ArrayList<>();
    private List<Entity> myRangedBases = new ArrayList<>();
    private List<Entity> myHouses = new ArrayList<>();
    private List<Entity> myMeleeBases = new ArrayList<>();
    private List<Entity> enemyAttackers = new ArrayList<>();
    private List<Entity> enemyUnits = new ArrayList<>();
    private List<Entity> enemyEntities = new ArrayList<>();
    private List<Entity> enemyTurrets = new ArrayList<>();
    private List<Entity> enemyBuildings = new ArrayList<>();
    private List<Entity> enemyBuilders = new ArrayList<>();
    private List<Entity> enemyRangedUnits = new ArrayList<>();
    private List<Entity> enemyMeleeUnits = new ArrayList<>();
    private List<Entity> myMeleeUnits = new ArrayList<>();
    private List<Entity> myRangedUnits = new ArrayList<>();
    private int currentUnits;
    private int maxUnits;

    public List<Entity> getEnemyEntities() {
        return enemyEntities;
    }

    public List<Entity> getEnemyTurrets() {
        return enemyTurrets;
    }

    public List<Entity> getEnemyBuilders() {
        return enemyBuilders;
    }

    public AllEntities(PlayerView playerView) {
        myId = playerView.getMyId();

        currentUnits = 0;
        maxUnits = 0;
        for (Entity otherEntity : playerView.getEntities()) {
            if (otherEntity.getPlayerId() != null && otherEntity.getPlayerId() == playerView.getMyId()) {
                currentUnits += otherEntity.getProperties().getPopulationUse();
                maxUnits += otherEntity.getProperties().getPopulationProvide();
            }
        }

        for (Entity entity : playerView.getEntities()) {
            if (entity.getEntityType() == EntityType.RESOURCE) { // neutral
                resources.add(entity);
                continue;
            }
            if (entity.getPlayerId() == myId) { //my
                myEntities.add(entity);
                if (entity.getEntityType().isBuilding()) {
                    myBuildings.add(entity);
                    if (entity.getEntityType() != EntityType.HOUSE) {
                        myActors.add(entity);
                        if (entity.getEntityType() == EntityType.RANGED_BASE) {
                            myRangedBases.add(entity);
                        } else if (entity.getEntityType() == EntityType.MELEE_BASE) {
                            myMeleeBases.add(entity);
                        }
                    } else {
                        myHouses.add(entity);
                    }
                } else if (entity.getEntityType().isUnit()) {
                    if (entity.getEntityType() == EntityType.BUILDER_UNIT) {
                        myBuilders.add(entity);
                    } else if (entity.getEntityType() == EntityType.RANGED_UNIT) {
                        myRangedUnits.add(entity);
                    } else if (entity.getEntityType() == EntityType.MELEE_UNIT) {
                        myMeleeUnits.add(entity);
                    }
                    myUnits.add(entity);
                    myActors.add(entity);
                }
                if (entity.getProperties().getAttack() != null && entity.getProperties().getAttack().getDamage() > 1) {
                    myAttackers.add(entity);
                }

            } else if (entity.getPlayerId() != null) { // enemy
                enemyEntities.add(entity);
                if (entity.getProperties().getAttack() != null) {
                    enemyAttackers.add(entity);
                }
                if (entity.getEntityType().isBuilding()) {
                    enemyBuildings.add(entity);
                    if (entity.getEntityType() == EntityType.TURRET) {
                        enemyTurrets.add(entity);
                    }
                }
                if (entity.getEntityType().isUnit()) {
                    if (entity.getEntityType() == EntityType.BUILDER_UNIT) {
                        enemyBuilders.add(entity);
                    } else if (entity.getEntityType() == EntityType.RANGED_UNIT) {
                        enemyRangedUnits.add(entity);
                    } else {
                        enemyMeleeUnits.add(entity);
                    }
                    enemyUnits.add(entity);
                }
            }
        }

        if (DebugInterface.isDebugEnabled()) {
            int ded = 0;
            int hded = 0;
            for (Entity ranger : myRangedUnitsOld) {
                if (myRangedUnits.stream().noneMatch(ent -> ent.getId() == ranger.getId())) {
                    ded++;
                }
            }
            for (Entity ranger : myHalfRangedUnitsOld) {
                if (myRangedUnits.stream().noneMatch(ent -> ent.getId() == ranger.getId())) {
                    hded++;
                }
            }
            myRangedUnitsOld = new ArrayList<>();
            myHalfRangedUnitsOld = new ArrayList<>();
            for (Entity ranger : myRangedUnits) {
                if (ranger.getHealth() < 10) {
                    myHalfRangedUnitsOld.add(ranger);
                } else {
                    myRangedUnitsOld.add(ranger);
                }
            }
            if (ded > 0 || hded > 0) {
                hdeadTotal += hded;
                deadTotal += ded;
                System.out.println("ded:" + ded + ";hded:" + hded + ";dt:" + deadTotal + ";hdt:" + hdeadTotal + ";%" + 100 * deadTotal / (deadTotal + hdeadTotal));

            }
        }
    }

    public int getMyId() {
        return myId;
    }

    public List<Entity> getResources() {
        return resources;
    }

    public List<Entity> getEnemyUnits() {
        return enemyUnits;
    }

    public List<Entity> getEnemyAttackers() {
        return enemyAttackers;
    }

    public List<Entity> getEnemyWorkers() {
        return enemyBuilders;
    }

    public List<Entity> getMyWorkers() {
        return myBuilders;
    }

    public List<Entity> getMyUnits() {
        return myUnits;
    }

    public List<Entity> getMyAttackers() {
        return myAttackers;
    }

    public List<Entity> getEnemyBuildings() {
        return enemyBuildings;
    }

    public List<Entity> getMyBuildings() {
        return myBuildings;
    }

    public int getCurrentUnits() {
        return currentUnits;
    }

    public int getMaxUnits() {
        return maxUnits;
    }

    public List<Entity> getMyActors() {
        return myActors;
    }

    public List<Entity> getMyEntities() {
        return myEntities;
    }

    public List<Entity> getMyRangedBases() {
        return myRangedBases;
    }

    public List<Entity> getMyHouses() {
        return myHouses;
    }

    public List<Entity> getMyMeleeBases() {
        return myMeleeBases;
    }

    public List<Entity> getMyBuilders() {
        return myBuilders;
    }

    public List<Entity> getEnemyRangedUnits() {
        return enemyRangedUnits;
    }

    public List<Entity> getEnemyMeleeUnits() {
        return enemyMeleeUnits;
    }

    public List<Entity> getMyMeleeUnits() {
        return myMeleeUnits;
    }

    public List<Entity> getMyRangedUnits() {
        return myRangedUnits;
    }
}
