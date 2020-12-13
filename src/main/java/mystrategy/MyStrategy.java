package mystrategy;

import model.*;
import mystrategy.maps.*;
import util.DebugInterface;
import util.Strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private ResourcesMap resourceMap;

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
        resourceMap = new ResourcesMap(playerView, entitiesMap, allEntities, debugInterface);
        simCityMap = new SimCityMap(playerView, entitiesMap, debugInterface);
        repairMap = new RepairMap(playerView, entitiesMap, debugInterface);

        Action result = new Action(new java.util.HashMap<>());

        // group units (attack/repair/build/harass/reconnaissance/harvest)
        // strategic orders
        // generate movements (attack/retreat/avoid active turrets/retreat to repair)
        // test movements

        // generate attacks+repairs

        // units
        for (Entity unit : allEntities.getMyUnits()) {
            if (/*allEntities.getResources().size() > 0 && */unit.getEntityType() == EntityType.BUILDER_UNIT) {
                MoveAction moveAction = null;
                BuildAction buildAction = null;
                RepairAction repairAction = null;
                AttackAction attackAction = null;

                Coordinate moveTo = resourceMap.getPositionClosestToResource(unit.getPosition());
                if (moveTo == null) {
                    moveTo = new Coordinate(35, 35);
                }
                moveAction =
                        new MoveAction(moveTo,
                                true,
                                false);
                EntityType[] validAutoAttackTargets;
                Integer canBuildId = repairMap.canBuildId(unit.getPosition());
                if (canBuildId != null) {
                    moveAction = null;
                    repairAction = new RepairAction(canBuildId);
                } else {
                    Integer canRepairId = repairMap.canRepairId(unit.getPosition());
                    if (canRepairId != null) {
                        repairAction = new RepairAction(canRepairId);
                    }
                }
                Coordinate buildCoordinates = simCityMap.getBuildCoordinates(unit.getPosition());
                if ((maxUnits == 0 || (maxUnits - currentUnits) * 100 / maxUnits < 33)
                        && me.getResource() >= playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost()
                        && buildCoordinates != null && simCityMap.getDistance(unit.getPosition()) == 2) {
                    buildAction = new BuildAction(EntityType.HOUSE, buildCoordinates);
                    maxUnits += playerView.getEntityProperties().get(EntityType.HOUSE).getPopulationProvide();
                    validAutoAttackTargets = new EntityType[0];
                } else {
//                    if (resourceMap.getDistance(unit.getPosition(), false) == 2) {
                    validAutoAttackTargets = new EntityType[]{EntityType.RESOURCE};
//                    } else {
//                        validAutoAttackTargets = new EntityType[0]; // only after removing autoattack
//                    }
                }
                attackAction = new AttackAction(
                        null, new AutoAttack(1, validAutoAttackTargets)
                );
                result.getEntityActions().put(unit.getId(), new EntityAction(
                        moveAction,
                        buildAction,
                        attackAction,
                        repairAction
                ));

            } else {
                EntityProperties properties = unit.getProperties();
                MoveAction moveAction = null;
                BuildAction buildAction = null;
                RepairAction repairAction = null;
                Coordinate moveTo = enemiesMap.getPositionClosestToEnemy(unit.getPosition());
                if (moveTo == null) {
                    moveTo = new Coordinate(35, 35);
                }
                moveAction = new MoveAction(moveTo, true, true);
                EntityType[] validAutoAttackTargets;
                validAutoAttackTargets = new EntityType[0];
                AttackAction attackAction = new AttackAction(
                        null, new AutoAttack(properties.getSightRange(), validAutoAttackTargets)
                );
                result.getEntityActions().put(unit.getId(), new EntityAction(
                        moveAction,
                        buildAction,
                        attackAction,
                        repairAction
                ));
            }

        }

        // buildings
        handleBuildings(result);
        return result;
    }

    private void handleBuildings(Action result) {
        for (Entity building : allEntities.getMyBuildings()) {
            BuildAction buildAction = null;
            if (building.getProperties().getBuild() != null) {
                buildAction = getBuildingAction(playerView, building, building.getProperties(), null);
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

    private BuildAction getBuildingAction(PlayerView playerView, Entity entity, EntityProperties properties, BuildAction buildAction) {
        EntityType entityType = properties.getBuild().getOptions()[0];
        if (entityType != EntityType.BUILDER_UNIT && playerView.getCurrentTick() < 20) {
            return buildAction;
        }

        System.out.println(allEntities.getMyBuilders().size());

        if (entityType == EntityType.BUILDER_UNIT
                && allEntities.getMyBuilders().size() > (playerView.isFogOfWar() ? 60 : allEntities.getEnemyBuilders().size() + 20)) {
            return buildAction;
        }

        Coordinate defaultBuildPosition = new Coordinate(
                entity.getPosition().getX() + properties.getSize(),
                entity.getPosition().getY() + properties.getSize() - 1
        );
        if (entityType == EntityType.BUILDER_UNIT) {
            Coordinate buildPosition = defaultBuildPosition;
            List<Coordinate> adjustentFreePoints = new ArrayList<>();
            int size = entity.getProperties().getSize();
            for (int i = 0; i < size; i++) {
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() - 1, entity.getPosition().getY() + i));
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() - 1));
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() + size, entity.getPosition().getY() + i));
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() + size));
            }
            adjustentFreePoints = adjustentFreePoints.stream()
                    .filter(point -> !entitiesMap.isOutOfBounds(point))
                    .filter(point -> entitiesMap.isEmpty(point))
                    .collect(Collectors.toList());

            // get any free point
            Optional<Coordinate> any = adjustentFreePoints.stream().findAny();
            if (any.isPresent()) {
                buildPosition = any.get();
            }

            buildPosition = resourceMap.getPositionClosestToResource(buildPosition, adjustentFreePoints);
            buildAction = new BuildAction(
                    EntityType.BUILDER_UNIT,
                    buildPosition
            );
        } else {
            Coordinate buildPosition = defaultBuildPosition;
            List<Coordinate> adjustentFreePoints = new ArrayList<>();
            int size = entity.getProperties().getSize();
            for (int i = 0; i < size; i++) {
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() - 1, entity.getPosition().getY() + i));
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() - 1));
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() + size, entity.getPosition().getY() + i));
                adjustentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() + size));
            }
            adjustentFreePoints = adjustentFreePoints.stream()
                    .filter(point -> !entitiesMap.isOutOfBounds(point))
                    .filter(point -> entitiesMap.isEmpty(point))
                    .collect(Collectors.toList());

            // get any free point
            Optional<Coordinate> any = adjustentFreePoints.stream().findAny();
            if (any.isPresent()) {
                buildPosition = any.get();
            }
            buildPosition = enemiesMap.getPositionClosestToEnemy(buildPosition, adjustentFreePoints);
            buildAction = new BuildAction(
                    entityType,
                    buildPosition
            );
        }
        return buildAction;
    }

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
        debugInterface.send(new DebugCommand.Clear());
        debugInterface.getState();
    }
}