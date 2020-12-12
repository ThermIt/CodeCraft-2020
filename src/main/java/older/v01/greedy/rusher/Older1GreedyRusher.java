package older.v01.greedy.rusher;

import model.*;
import util.DebugInterface;
import util.Strategy;

import java.util.Arrays;

public class Older1GreedyRusher implements Strategy {

    private EntitiesMap entitiesMap;
    private SimCityMap simCityMap;
    private RepairMap repairMap;
    private EnemiesMap enemiesMap;

    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {

        int currentUnits = 0;
        int maxUnits = 0;
        for (Entity otherEntity : playerView.getEntities()) {
            if (otherEntity.getPlayerId() != null && otherEntity.getPlayerId() == playerView.getMyId()) {
                currentUnits += playerView.getEntityProperties().get(otherEntity.getEntityType()).getPopulationUse();
                maxUnits += playerView.getEntityProperties().get(otherEntity.getEntityType()).getPopulationProvide();
            }
        }

        entitiesMap = new EntitiesMap(playerView, debugInterface);
        enemiesMap = new EnemiesMap(playerView, entitiesMap, debugInterface);
        simCityMap = new SimCityMap(playerView, entitiesMap, debugInterface);
        repairMap = new RepairMap(playerView, entitiesMap, debugInterface);
        boolean isFirstBuilder = true;
        Player me = Arrays.stream(playerView.getPlayers()).filter(player -> player.getId() == playerView.getMyId()).findAny().get();

        Action result = new Action(new java.util.HashMap<>());
        int myId = playerView.getMyId();
        for (Entity entity : playerView.getEntities()) {
            if (entity.getPlayerId() == null || entity.getPlayerId() != myId) {
                continue;
            }
            EntityProperties properties = playerView.getEntityProperties().get(entity.getEntityType());

            MoveAction moveAction = null;
            BuildAction buildAction = null;
            RepairAction repairAction = null;
            if (properties.isCanMove()) {
                Coordinate moveTo = enemiesMap.getPositionClosestToEnemy(entity.getPosition());
                if (moveTo == null) {
                    moveTo = new Coordinate(35, 35);
                }
                moveAction =
                        new MoveAction(moveTo,
                                true,
                                false);
            } else if (properties.getBuild() != null) {
                buildAction = getBuildAction(playerView, entity, properties, buildAction);
            }
            EntityType[] validAutoAttackTargets;
            if (entity.getEntityType() == EntityType.BUILDER_UNIT) {
                Integer canRepairThisId = repairMap.canRepairId(entity.getPosition());
                if (canRepairThisId != null) {
                    moveAction = null;
                    repairAction = new RepairAction(canRepairThisId);
                }
                Coordinate buildCoordinates = simCityMap.getBuildCoordinates(entity.getPosition());
                if ((maxUnits - currentUnits) * 100 / maxUnits < 10
                        && me.getResource() >= playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost()
                        && buildCoordinates != null && simCityMap.getDistance(entity.getPosition()) == 2) {
                    buildAction = new BuildAction(EntityType.HOUSE, buildCoordinates);
                    validAutoAttackTargets = new EntityType[0];
                } else {
                    validAutoAttackTargets = new EntityType[]{EntityType.RESOURCE};
                }
                isFirstBuilder = false;
            } else {
                validAutoAttackTargets = new EntityType[0];
            }
            result.getEntityActions().put(entity.getId(), new EntityAction(
                    moveAction,
                    buildAction,
                    new AttackAction(
                            null, new AutoAttack(properties.getSightRange(), validAutoAttackTargets)
                    ),
                    repairAction
            ));
        }
        return result;
    }

    private BuildAction getBuildAction(PlayerView playerView, Entity entity, EntityProperties properties, BuildAction buildAction) {
        EntityType entityType = properties.getBuild().getOptions()[0];
        if (entityType != EntityType.BUILDER_UNIT && playerView.getCurrentTick() < 20) {
            return buildAction;
        }
/*
        int currentUnits = 0;
        for (Entity otherEntity : playerView.getEntities()) {
            if (otherEntity.getPlayerId() != null && otherEntity.getPlayerId() == myId
                    && otherEntity.getEntityType() == entityType) {
                currentUnits++;
            }
        }
*/
//        if ((currentUnits + 1) * playerView.getEntityProperties().get(entityType).getPopulationUse() <= properties.getPopulationProvide()) {
        buildAction = new BuildAction(
                entityType,
                new Coordinate(
                        entity.getPosition().getX() + properties.getSize(),
                        entity.getPosition().getY() + properties.getSize() - 1
                )
        );
//        }
        return buildAction;
    }

    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
        debugInterface.send(new DebugCommand.Clear());
        debugInterface.getState();
    }
}