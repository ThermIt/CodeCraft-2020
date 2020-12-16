package mystrategy.strategies;

import model.*;
import mystrategy.collections.AllEntities;
import mystrategy.maps.EnemiesMap;
import mystrategy.maps.EntitiesMap;
import mystrategy.maps.light.WorkerJobsMap;
import util.DebugInterface;
import util.Strategy;
import util.StrategyTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FirstStageStrategy implements Strategy, StrategyTrigger {

    private boolean done;
    private EntitiesMap entitiesMap;
    private AllEntities allEntities;
    private WorkerJobsMap jobs;
    private PlayerView playerView;

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public Strategy getNextStage() {
        return new DefaultStrategy();
    }

    @Override
    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        this.playerView = playerView;
        this.entitiesMap = new EntitiesMap(playerView, debugInterface);
        this.allEntities = new AllEntities(playerView, debugInterface);
        this.jobs = new WorkerJobsMap(
                playerView,
                entitiesMap,
                allEntities,
                new EnemiesMap(playerView, entitiesMap, debugInterface),
                debugInterface);

        for (Entity unit : allEntities.getMyUnits()) {
            MoveAction moveAction;
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                Coordinate moveTo = jobs.getPositionClosestToResource(unit.getPosition());
                if (moveTo == null) {
                    moveTo = new Coordinate(35, 35);
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
                Entity resource = jobs.getResource(unit.getPosition());
                if (resource != null) {
                    attackAction = new AttackAction(resource.getId(), null);
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


        done = allEntities.getMyBuildings().stream()
                .anyMatch(ent -> ent.getEntityType() == EntityType.RANGED_BASE && ent.isActive());

        allEntities.getMyBuildings().stream()
                .filter(ent -> ent.getEntityType() == EntityType.BUILDER_BASE && ent.isActive())
                .forEach(ent -> ent.setBuildAction(getBuilderBaseBuildingAction(ent)));

        if (DebugInterface.isDebugEnabled()) {
            System.out.println(allEntities.getMyBuilders().size());
        }

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
        return result;
    }

    private BuildAction getBuilderBaseBuildingAction(Entity entity) {
        Coordinate defaultBuildPosition = new Coordinate(
                entity.getPosition().getX() + entity.getProperties().getSize(),
                entity.getPosition().getY() + entity.getProperties().getSize() - 1
        );
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
                .filter(point -> !point.isOutOfBounds())
                .filter(point -> entitiesMap.isEmpty(point))
                .collect(Collectors.toList());

        // get any free point
        Optional<Coordinate> any = adjustentFreePoints.stream().findAny();
        if (any.isPresent()) {
            buildPosition = any.get();
        }

        buildPosition = jobs.getPositionClosestToResource(buildPosition, adjustentFreePoints);
        BuildAction buildAction = new BuildAction(
                EntityType.BUILDER_UNIT,
                buildPosition
        );
        return buildAction;
    }

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
    }
}
