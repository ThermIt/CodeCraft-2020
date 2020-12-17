package mystrategy.strategies;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import mystrategy.maps.RepairMap;
import mystrategy.maps.SimCityMap;
import mystrategy.maps.light.HarvestJobsMap;
import mystrategy.maps.light.VisibilityMap;
import util.DebugInterface;
import util.StrategyDelegate;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultStrategy implements StrategyDelegate {

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
    //    private ResourcesMap resourceMap;
    private HarvestJobsMap harvestJobs;
    private boolean done;
    private boolean first;
    private boolean second;
    private VisibilityMap visibility;

    public DefaultStrategy(VisibilityMap visibility) {
        this.visibility = visibility;
    }

    /**
     * attack -> build -> repair -> move
     */
    @Override
    public Action getAction(PlayerView playerView) {
        if (!first) {
            first = true;
            if (DebugInterface.isDebugEnabled()) {
                System.out.println(playerView.getCurrentTick() + "SW");
            }
        }
        // определить фронт работ (добыча/постройка/починка/атака/расчистка/разведка/защита)
        // резервирование ресурсов
        // определить кто что делает сейчас // забрать работы
        // определить кто что может делать после пробежки
        // пометить ресурсы как добываемые
        // билд ордер с учётом доступных ресурсов (4 рабочих - барак - 5 рабочих - барак - ...) учитывая рост стоимости
        // определить где что строить
        // выгнать всех с потенциальных построек
        // определит кто куда идёт
        // рабочими идти только к новым патчам
        // (рабочие могу проходить сквозь друг-друга)
        // биться по правилам финала уже.
        // делеать милишников пачками по 5 штук
        // разбавлять лучников рабочими
        // сохранять стоимость юнитов низкой?

        this.playerView = playerView;
        this.debugInterface = debugInterface;
        allEntities = new AllEntities(playerView);
        entitiesMap = new EntitiesMap(playerView);
        me = Arrays.stream(playerView.getPlayers()).filter(player -> player.getId() == playerView.getMyId()).findAny().get();
        this.visibility.init(playerView, allEntities);

        currentUnits = allEntities.getCurrentUnits();
        maxUnits = allEntities.getMaxUnits();

        enemiesMap = new EnemiesMap(playerView, entitiesMap);
//        resourceMap = new ResourcesMap(playerView, entitiesMap, allEntities, enemiesMap, debugInterface);
        harvestJobs = new HarvestJobsMap(playerView, entitiesMap, allEntities, enemiesMap, me);
        simCityMap = new SimCityMap(playerView, entitiesMap, allEntities, debugInterface);
        repairMap = new RepairMap(playerView, entitiesMap, debugInterface);

        boolean barracksNotFinished = allEntities.getMyBuildings().stream()
                .noneMatch(ent -> ent.isMy(EntityType.RANGED_BASE) && ent.isActive());
        if (!second && !barracksNotFinished) {
            second = true;
            if (DebugInterface.isDebugEnabled()) {
                System.out.println(playerView.getCurrentTick() + "BR");
            }
        }

        Action result = new Action(new java.util.HashMap<>());

        // group units (attack/repair/build/harass/reconnaissance/harvest)
        // strategic orders
        // generate movements (attack/retreat/avoid active turrets/retreat to repair)
        // test movements

        // generate attacks+repairs

        // units
        for (Entity unit : allEntities.getMyUnits()) {
            MoveAction moveAction;
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                Coordinate moveTo = harvestJobs.getPositionClosestToResource(unit.getPosition());
                if (moveTo == null) {
                    moveTo = new Coordinate(35, 35);
                }
                moveAction = new MoveAction(moveTo, true, true);
                unit.setMoveAction(moveAction);
            } else {
                Coordinate moveTo = enemiesMap.getPositionClosestToEnemy(unit.getPosition());
                if (moveTo == null || Objects.equals(moveTo, unit.getPosition())) { // hack
                    moveTo = new Coordinate(7, 72);
                }
                moveAction = new MoveAction(moveTo, true, true);
                unit.setMoveAction(moveAction);
            }
        }

        for (Entity unit : allEntities.getMyUnits()) {
            BuildAction buildAction = null;
            RepairAction repairAction = null;
            AttackAction attackAction = null;
            if (/*allEntities.getResources().size() > 0 && */unit.getEntityType() == EntityType.BUILDER_UNIT) {
                Entity resource = entitiesMap.getResource(unit.getPosition());
                if (resource != null) {
                    attackAction = new AttackAction(resource.getId(), null);
                }

                Integer canBuildId = repairMap.canBuildId(unit.getPosition());
                if (canBuildId != null) {
                    attackAction = null;
                    unit.setMoveAction(null); // bugfix this
                    repairAction = new RepairAction(canBuildId);
                } else {
                    Integer canRepairId = repairMap.canRepairId(unit.getPosition());
                    if (canRepairId != null) {
                        repairAction = new RepairAction(canRepairId);
                    }
                }
                Coordinate buildCoordinates = simCityMap.getBuildCoordinates(unit.getPosition());
                Coordinate rbBuildCoordinates = simCityMap.getRangedBaseBuildCoordinates(unit.getPosition());
                if (simCityMap.isNeedBarracks() // hack
                        && me.getResource() >= playerView.getEntityProperties().get(EntityType.RANGED_BASE).getInitialCost()
                        && rbBuildCoordinates != null) {
                    attackAction = null;
                    unit.setMoveAction(null); // bugfix this
                    buildAction = new BuildAction(EntityType.RANGED_BASE, rbBuildCoordinates);
//                    maxUnits += playerView.getEntityProperties().get(EntityType.RANGED_BASE).getPopulationProvide();

//                    simCityMap.setNeedBarracks(false);
                }
                if (needMoreHouses()
                        && !(simCityMap.isNeedBarracks() && maxUnits >= 20)
                        && me.getResource() >= playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost()
                        && buildCoordinates != null) {
                    attackAction = null;
                    unit.setMoveAction(null); // bugfix this
                    buildAction = new BuildAction(EntityType.HOUSE, buildCoordinates);
                    maxUnits += playerView.getEntityProperties().get(EntityType.HOUSE).getPopulationProvide();
                }
                unit.setAttackAction(attackAction);
                unit.setBuildAction(buildAction);
                unit.setRepairAction(repairAction);
            } else {
                EntityProperties properties = unit.getProperties();
                EntityType[] validAutoAttackTargets;
                validAutoAttackTargets = new EntityType[0];
                attackAction = new AttackAction(
                        null, new AutoAttack(properties.getSightRange(), validAutoAttackTargets)
                );
                unit.setAttackAction(attackAction);
                unit.setBuildAction(buildAction);
                unit.setRepairAction(repairAction);
            }

            if (unit.getAttackAction() != null) {
                DebugInterface.print("AK", unit.getPosition());
            } else if (unit.getBuildAction() != null) {
                DebugInterface.print("BD", unit.getPosition());
            } else if (unit.getRepairAction() != null) {
                DebugInterface.print("RR", unit.getPosition());
            } else if (unit.getMoveAction() != null) {
                DebugInterface.print("MV", unit.getPosition());
                DebugInterface.line(unit.getPosition(), unit.getMoveAction().getTarget());

            }


            result.getEntityActions().put(unit.getId(), new EntityAction(
                    unit.getMoveAction(),
                    unit.getBuildAction(),
                    unit.getAttackAction(),
                    unit.getRepairAction()
            ));
        }

        // buildings
        handleBuildings(result);
        return result;
    }

    private boolean needMoreHouses() {
        return maxUnits == 0 || (maxUnits - (currentUnits + me.getResource() / (maxUnits <= 150 ? 10 : 50))) * 100 / maxUnits < 20;
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

/*
        if (DebugInterface.isDebugEnabled()) {
            System.out.println(playerView.getCurrentTick() + ":" + allEntities.getMyBuilders().size());
        }

*/
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
            List<Coordinate> adjacentFreePoints = new ArrayList<>();
            int size = entity.getProperties().getSize();
            for (int i = 0; i < size; i++) {
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() - 1, entity.getPosition().getY() + i));
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() - 1));
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() + size, entity.getPosition().getY() + i));
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() + size));
            }
            adjacentFreePoints = adjacentFreePoints.stream()
                    .filter(point -> !point.isOutOfBounds())
                    .filter(point -> entitiesMap.isEmpty(point))
                    .collect(Collectors.toList());

            // get any free point
            Optional<Coordinate> any = adjacentFreePoints.stream().findAny();
            if (any.isPresent()) {
                buildPosition = any.get();
            }

            buildPosition = harvestJobs.getPositionClosestToResource(buildPosition, adjacentFreePoints);
            buildAction = new BuildAction(
                    EntityType.BUILDER_UNIT,
                    buildPosition
            );
        } else {
            Coordinate buildPosition = defaultBuildPosition;
            List<Coordinate> adjacentFreePoints = new ArrayList<>();
            int size = entity.getProperties().getSize();
            for (int i = 0; i < size; i++) {
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() - 1, entity.getPosition().getY() + i));
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() - 1));
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() + size, entity.getPosition().getY() + i));
                adjacentFreePoints.add(new Coordinate(entity.getPosition().getX() + i, entity.getPosition().getY() + size));
            }
            adjacentFreePoints = adjacentFreePoints.stream()
                    .filter(point -> !point.isOutOfBounds())
                    .filter(point -> entitiesMap.isEmpty(point))
                    .collect(Collectors.toList());

            // get any free point
            Optional<Coordinate> any = adjacentFreePoints.stream().findAny();
            if (any.isPresent()) {
                buildPosition = any.get();
            }
            buildPosition = enemiesMap.getPositionClosestToEnemy(buildPosition, adjacentFreePoints);
            buildAction = new BuildAction(
                    entityType,
                    buildPosition
            );
        }
        return buildAction;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public StrategyDelegate getNextStage() {
        return null;
    }
}