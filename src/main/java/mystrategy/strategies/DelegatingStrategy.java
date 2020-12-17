package mystrategy.strategies;

import model.Action;
import model.DebugCommand;
import model.PlayerView;
import mystrategy.maps.light.BuildMap;
import util.*;

public class DelegatingStrategy implements Strategy {

    private StrategyDelegate currentStrategy;

    private StrategyTrigger trigger;

    @Override
    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        new Initializer(playerView, debugInterface).run();
        BuildMap.INSTANCE.init(playerView); // once on start because we don't have settings outside of a round

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
        DefaultStrategy currentStrategy = new DefaultStrategy();
        this.currentStrategy = currentStrategy;
        this.trigger = currentStrategy;
    }

    private void initFogStrategy() {
        FirstStageStrategy currentStrategy = new FirstStageStrategy();
        this.currentStrategy = currentStrategy;
        this.trigger = currentStrategy;
    }

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
        debugInterface.send(new DebugCommand.Clear());
        debugInterface.getState();
    }
}
