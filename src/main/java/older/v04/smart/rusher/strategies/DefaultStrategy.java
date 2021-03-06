package older.v04.smart.rusher.strategies;

import model.*;
import older.v04.smart.rusher.collections.AllEntities;
import older.v04.smart.rusher.maps.EnemiesMap;
import older.v04.smart.rusher.maps.EntitiesMap;
import older.v04.smart.rusher.maps.RepairMap;
import older.v04.smart.rusher.maps.SimCityMap;
import older.v04.smart.rusher.maps.light.*;
import util.StrategyDelegate;
import util.Task;

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
    private HarvestJobsMap harvestJobs;
    private boolean done;
    private boolean first;
    private boolean second;
    private boolean third;
    private BuildOrders buildOrders;
    private VisibilityMap visibility;
    private VirtualResources resources;
    private WarMap warMap;
    private WorkerJobsMap jobs;

    public DefaultStrategy(BuildOrders buildOrders, VisibilityMap visibility, VirtualResources resources, WarMap warMap) {
        this.buildOrders = buildOrders;
        this.visibility = visibility;
        this.resources = resources;
        this.warMap = warMap;
    }

    /**
     * attack -> build -> repair -> move
     */
    @Override
    public Action getAction(PlayerView playerView) {
        if (!first) {
            first = true;
        }

        this.playerView = playerView;
        allEntities = new AllEntities(playerView);
        entitiesMap = new EntitiesMap(playerView);
        me = Arrays.stream(playerView.getPlayers()).filter(player -> player.getId() == playerView.getMyId()).findAny().get();
        this.visibility.init(playerView, allEntities);
        this.resources.init(playerView, allEntities, entitiesMap);
        this.warMap.init(playerView, entitiesMap, allEntities);
        currentUnits = allEntities.getCurrentUnits();
        maxUnits = allEntities.getMaxUnits();
        enemiesMap = new EnemiesMap(playerView, entitiesMap);

        this.jobs = new WorkerJobsMap(
                playerView,
                entitiesMap,
                allEntities,
                enemiesMap,
                me,
                buildOrders
        );

        harvestJobs = new HarvestJobsMap(playerView, entitiesMap, allEntities, enemiesMap, me);
        simCityMap = new SimCityMap(playerView, entitiesMap, allEntities);
        repairMap = new RepairMap(playerView, entitiesMap);

        if (!second && allEntities.getMyBuildings().stream()
                .anyMatch(ent1 -> ent1.isMy(EntityType.RANGED_BASE) && !ent1.isActive())) {
            second = true;
        }
        if (!third && allEntities.getMyBuildings().stream()
                .anyMatch(ent -> ent.isMy(EntityType.RANGED_BASE) && ent.isActive())) {
            third = true;
        }

        Action result = new Action(new HashMap<>());

        for (Entity unit : allEntities.getMyUnits()) {
            MoveAction moveAction;
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                Coordinate moveTo;
                if (unit.getTask() == Task.BUILD) {
                    moveTo = null;
                } else if (unit.getTask() == Task.MOVE_TO_BUILD) {
                    moveTo = jobs.getPositionClosestToBuild(unit.getPosition());
                } else { // idle workers
                    moveTo = harvestJobs.getPositionClosestToResource(unit.getPosition());
                    if (moveTo == null) {
                        moveTo = new Coordinate(35, 35);
                    }
                }
                if (moveTo != null) {
                    moveAction = new MoveAction(moveTo, true, true);
                    unit.setMoveAction(moveAction);
                }
            } else {
                Coordinate moveTo = warMap.getPositionClosestToEnemy(unit.getPosition());
                if (moveTo == null || Objects.equals(moveTo, unit.getPosition())) { // hack
                    if (playerView.isOneOnOne()) {
                        moveTo = new Coordinate(72, 72);
                    } else {
                        moveTo = new Coordinate(7, 72);
                    }
                }
                moveAction = new MoveAction(moveTo, true, true);
                unit.setMoveAction(moveAction);
            }
        }

        for (Entity unit : allEntities.getMyUnits()) {
            BuildAction buildAction = null;
            RepairAction repairAction = null;
            AttackAction attackAction = null;
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {

                if (unit.getTask() == Task.IDLE) {

                    // assign idle workers to harvest
                    Entity resource = harvestJobs.getResource(unit.getPosition());
                    Integer canBuildId = repairMap.canBuildId(unit.getPosition());
                    if (canBuildId != null) {
                        attackAction = null;
                        unit.setMoveAction(null); // bugfix this
                        repairAction = new RepairAction(canBuildId);
                    } else {
                        Integer canRepairId = repairMap.canRepairId(unit.getPosition());
                        if (canRepairId != null) {
                            repairAction = new RepairAction(canRepairId);
                        } else if (resource != null) {
                            attackAction = new AttackAction(resource.getId(), null);
                            resource.increaseDamage(unit.getProperties().getAttack().getDamage());
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
                } else if (unit.getTask() == Task.BUILD) {
                    Entity order = buildOrders.getOrder(unit.getPosition());
                    if (order != null) {
                        Entity entity = entitiesMap.getEntity(order.getPosition());
                        if (entity.getEntityType() == order.getEntityType()) {
                            repairAction = new RepairAction(entity.getId());
                        } else {
                            buildAction = new BuildAction(order.getEntityType(), order.getPosition());
                        }
                    }
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

        int buildersLimit = allEntities.getEnemyBuilders().size() + 20;
        if (playerView.isRound2())
            buildersLimit = 650;

        if (playerView.isFinials())
            buildersLimit = 1000;

        if (entityType == EntityType.BUILDER_UNIT
                && allEntities.getMyBuilders().size() > buildersLimit) {
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
            buildPosition = warMap.getPositionClosestToEnemy(buildPosition, adjacentFreePoints);
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