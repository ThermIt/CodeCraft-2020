package older.v04.smart.rusher.strategies;

import model.Action;
import model.DebugCommand;
import model.PlayerView;
import older.v04.smart.rusher.maps.light.BuildOrders;
import older.v04.smart.rusher.maps.light.VirtualResources;
import older.v04.smart.rusher.maps.light.VisibilityMap;
import older.v04.smart.rusher.maps.light.WarMap;
import util.*;

public class Older4SmartRusher implements Strategy {

    private StrategyDelegate currentStrategy;
    private StrategyTrigger trigger;
    private BuildOrders buildOrders = new BuildOrders();
    private VisibilityMap visibility = new VisibilityMap();
    private VirtualResources resources = new VirtualResources(visibility);
    private WarMap warMap = new WarMap(visibility, resources);

    @Override
    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        new Initializer(playerView, debugInterface).run();
        buildOrders.init(playerView); // once on start because we don't have settings outside of a round

        if (currentStrategy == null) {
            if (playerView.isFogOfWar()) {
                initFogStrategy();
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
        DefaultStrategy currentStrategy = new DefaultStrategy(buildOrders, visibility, resources, warMap);
        this.currentStrategy = currentStrategy;
        this.trigger = currentStrategy;
    }

    private void initFogStrategy() {
        FirstStageStrategy currentStrategy = new FirstStageStrategy(buildOrders, visibility, resources, warMap);
        this.currentStrategy = currentStrategy;
        this.trigger = currentStrategy;
    }

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
        debugInterface.send(new DebugCommand.Clear());
        debugInterface.getState();
    }
}
