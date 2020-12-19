package mystrategy.strategies;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import mystrategy.maps.RepairMap;
import mystrategy.maps.light.*;
import util.StrategyDelegate;
import util.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FirstStageStrategy implements StrategyDelegate {

    private boolean done;
    private EntitiesMap entitiesMap;
    private AllEntities allEntities;
    private WorkerJobsMap jobs;
    private HarvestJobsMap harvestJobs;
    private PlayerView playerView;
    private Player me;
    private BuildOrders buildOrders;
    private VisibilityMap visibility;
    private VirtualResources resources;
    private WarMap warMap; // only to pass wo war state
    private SimCityPlan simCityPlan;
    private RepairMap repairMap;

    public FirstStageStrategy(
            BuildOrders buildOrders,
            VisibilityMap visibility,
            VirtualResources resources,
            WarMap warMap,
            SimCityPlan simCityPlan
    ) {
        this.buildOrders = buildOrders;
        this.visibility = visibility;
        this.resources = resources;
        this.warMap = warMap;
        this.simCityPlan = simCityPlan;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public StrategyDelegate getNextStage() {
        return new DefaultStrategy(buildOrders, visibility, resources, warMap, simCityPlan);
    }

    @Override
    public Action getAction(PlayerView playerView) {
        me = Arrays.stream(playerView.getPlayers()).filter(player -> player.getId() == playerView.getMyId()).findAny().get();
        this.playerView = playerView;
        this.entitiesMap = new EntitiesMap(playerView);
        this.allEntities = new AllEntities(playerView);
        this.visibility.init(playerView, allEntities);
        this.resources.init(playerView, allEntities, entitiesMap);
        this.warMap.init(playerView, entitiesMap, allEntities);
        buildOrders.init(playerView, allEntities);

        EnemiesMap enemiesMap = new EnemiesMap(playerView, entitiesMap);
        this.jobs = new WorkerJobsMap(
                playerView,
                entitiesMap,
                allEntities,
                enemiesMap,
                me,
                buildOrders,
                warMap
        );
        this.harvestJobs = new HarvestJobsMap(
                playerView,
                entitiesMap,
                allEntities,
                enemiesMap,
                me,
                resources
        );
        repairMap = new RepairMap(playerView, entitiesMap);

        for (Entity unit : allEntities.getMyUnits()) {
            MoveAction moveAction;
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                Coordinate moveTo = null;
                if (unit.getTask() == Task.BUILD) {
                    moveTo = null;
                } else if (unit.getTask() == Task.MOVE_TO_BUILD) {
                    moveTo = jobs.getPositionClosestToBuild(unit.getPosition());
                } else if (unit.getTask() != Task.RUN_FOOLS) { // idle workers
                    moveTo = harvestJobs.getPositionClosestToResource(unit.getPosition());
                    if (moveTo == null) {
                        moveTo = new Coordinate(35, 35);
                    }
                }
                if (moveTo != null) {
                    moveAction = new MoveAction(moveTo, true, true);
                    unit.setMoveAction(moveAction);
                }
            }
        }

        for (Entity unit : allEntities.getMyUnits()) {
            BuildAction buildAction = null;
            RepairAction repairAction = null;
            AttackAction attackAction = null;
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                if (unit.getTask() == Task.IDLE) {
                    // assign idle workers to harvest
                    Integer canRepairId = repairMap.canRepairId(unit.getPosition()); // костыль
                    if (canRepairId != null) {
                        repairAction = new RepairAction(canRepairId);
                    } else {
                        Entity resource = harvestJobs.getResource(unit.getPosition());
                        if (resource != null) {
                            attackAction = new AttackAction(resource.getId(), null);
                            resource.increaseDamage(unit.getProperties().getAttack().getDamage());
                        }
                    }
                } else if (unit.getTask() == Task.BUILD) {
//                    DebugInterface.print("B", unit.getPosition());
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

/*
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
*/
/*
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
*/
                unit.setAttackAction(attackAction);
                unit.setBuildAction(buildAction);
                unit.setRepairAction(repairAction);
            }
        }

        allEntities.getMyBuildings().stream()
                .filter(ent -> ent.getEntityType() == EntityType.BUILDER_BASE && ent.isActive())
                .forEach(ent -> ent.setBuildAction(getBuilderBaseBuildingAction(ent)));

/*
        if (DebugInterface.isDebugEnabled()) {
            System.out.println(allEntities.getMyBuilders().size());
        }
*/

        Action result = new Action(new java.util.HashMap<>());
        for (Entity actor : allEntities.getMyActors()) {
            if (actor.hasAction()) {
                result.getEntityActions().put(actor.getId(), new EntityAction(
                        actor.getMoveAction(),
                        actor.getBuildAction(),
                        actor.getAttackAction(),
                        actor.getRepairAction()
                ));
            }
        }

        done = allEntities.getMyBuildings().stream().filter(ent -> ent.getEntityType() == EntityType.HOUSE && ent.isActive())
                .count() >= 3;

        return result;
    }

    private BuildAction getBuilderBaseBuildingAction(Entity entity) {
        Coordinate defaultBuildPosition = new Coordinate(
                entity.getPosition().getX() + entity.getProperties().getSize(),
                entity.getPosition().getY() + entity.getProperties().getSize() - 1
        );
        Coordinate buildPosition = defaultBuildPosition;
        List<Coordinate> adjacentFreePoints = entity.getAdjacentCoordinates();
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
        BuildAction buildAction = new BuildAction(
                EntityType.BUILDER_UNIT,
                buildPosition
        );
        return buildAction;
    }
}
