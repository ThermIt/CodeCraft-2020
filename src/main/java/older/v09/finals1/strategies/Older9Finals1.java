package older.v09.finals1.strategies;

import model.Action;
import model.DebugCommand;
import model.PlayerView;
import older.v09.finals1.maps.light.*;
import util.*;

public class Older9Finals1 implements Strategy {

    private StrategyDelegate currentStrategy;
    private StrategyTrigger trigger;
    private BuildOrders buildOrders = new BuildOrders();
    private VisibilityMap visibility = new VisibilityMap();
    private VirtualResources resources = new VirtualResources(visibility);
    private WarMap warMap = new WarMap(visibility, resources);
    private SimCityPlan simCityPlan = new SimCityPlan();

    @Override
    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        // once on start because we don't have settings outside of a round
        new Initializer(playerView, debugInterface).run();

        if (currentStrategy == null) {
            if (playerView.isFogOfWar()) {
//                initFogStrategy();
                initDefaultStrategy();
            } else {
                initDefaultStrategy();
            }
        } else {
            if (trigger.isDone()) {
                StrategyDelegate nextStage = trigger.getNextStage();
                currentStrategy = nextStage;
                trigger = (StrategyTrigger) nextStage;
            }
        }
        return currentStrategy.getAction(playerView);
    }

    private void initDefaultStrategy() {
        DefaultStrategy currentStrategy = new DefaultStrategy(buildOrders, visibility, resources, warMap, simCityPlan);
        this.currentStrategy = currentStrategy;
        this.trigger = currentStrategy;
    }

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
        debugInterface.send(new DebugCommand.Clear());
        debugInterface.getState();
    }
}
