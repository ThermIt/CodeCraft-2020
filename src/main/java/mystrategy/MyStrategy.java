package mystrategy;

import model.*;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import mystrategy.maps.RepairMap;
import mystrategy.maps.SimCityMap;
import util.DebugInterface;
import util.Strategy;

import java.util.Arrays;

public class MyStrategy implements Strategy {

    private EntitiesMap entitiesMap;
    private SimCityMap simCityMap;
    private RepairMap repairMap;
    private EnemiesMap enemiesMap;
    private int currentUnits;
    private int maxUnits;
    private AllEntities allEntities;
    private Player me;
    private PlayerView playerView;
    private DebugInterface debugInterface;

    @Override
    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        this.playerView = playerView;
        this.debugInterface = debugInterface;
        new Initializer(playerView, debugInterface).initStatic();
        allEntities = new AllEntities(playerView, debugInterface);
        entitiesMap = new EntitiesMap(playerView, debugInterface);
        me = Arrays.stream(playerView.getPlayers()).filter(player -> player.getId() == playerView.getMyId()).findAny().get();

        currentUnits = allEntities.getCurrentUnits();
        maxUnits = allEntities.getMaxUnits();

        enemiesMap = new EnemiesMap(playerView, entitiesMap, debugInterface);
        simCityMap = new SimCityMap(playerView, entitiesMap, debugInterface);
        repairMap = new RepairMap(playerView, entitiesMap, debugInterface);

        Action result = new Action(new java.util.HashMap<>());

        // buildings
        handleBuildings(result);

        // units
        for (Entity unit : allEntities.getMyUnits()) {
            EntityProperties properties = unit.getProperties();

            MoveAction moveAction = null;
            BuildAction buildAction = null;
            RepairAction repairAction = null;
            if (properties.isCanMove()) {
                Coordinate moveTo = enemiesMap.getPositionClosestToEnemy(unit.getPosition());
                if (moveTo == null) {
                    moveTo = new Coordinate(35, 35);
                }
                moveAction =
                        new MoveAction(moveTo,
                                true,
                                false);
            }
            EntityType[] validAutoAttackTargets;
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                Integer canRepairThisId = repairMap.canRepairId(unit.getPosition());
                if (canRepairThisId != null) {
                    moveAction = null;
                    repairAction = new RepairAction(canRepairThisId);
                }
                Coordinate buildCoordinates = simCityMap.getBuildCoordinates(unit.getPosition());
                if ((maxUnits - currentUnits) * 100 / maxUnits < 33
                        && me.getResource() >= playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost()
                        && buildCoordinates != null && simCityMap.getDistance(unit.getPosition()) == 2) {
                    buildAction = new BuildAction(EntityType.HOUSE, buildCoordinates);
                    maxUnits += playerView.getEntityProperties().get(EntityType.HOUSE).getPopulationProvide();
                    validAutoAttackTargets = new EntityType[0];
                } else {
                    validAutoAttackTargets = new EntityType[]{EntityType.RESOURCE};
                }
            } else {
                validAutoAttackTargets = new EntityType[0];
            }
            result.getEntityActions().put(unit.getId(), new EntityAction(
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

    private void handleBuildings(Action result) {
        for (Entity building : allEntities.getMyBuildings()) {
            BuildAction buildAction = null;
            if (building.getProperties().getBuild() != null) {
                buildAction = getBuildAction(playerView, building, building.getProperties(), null);
            }
            result.getEntityActions().put(building.getId(), new EntityAction(
                    null,
                    buildAction,
                    new AttackAction(
                            null, new AutoAttack(building.getProperties().getSightRange(), new EntityType[0])
                    ),
                    null
            ));

        }
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

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
        debugInterface.send(new DebugCommand.Clear());
        debugInterface.getState();
    }
}