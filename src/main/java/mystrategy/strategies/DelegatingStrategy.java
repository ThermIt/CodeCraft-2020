package mystrategy.strategies;

import model.Action;
import model.DebugCommand;
import model.PlayerView;
import mystrategy.utils.Initializer;
import util.StrategyTrigger;
import util.DebugInterface;
import util.Strategy;

public class DelegatingStrategy implements Strategy {

    private Strategy currentStrategy;

    private StrategyTrigger trigger;

    @Override
    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        new Initializer(playerView, debugInterface).initStatic();
        if (currentStrategy == null) {
            initDefaultStrategy();
/*
            if (playerView.isFogOfWar()) {
                initFogStrategy();
            } else {
                initDefaultStrategy();
            }
*/
        } else {
            if (trigger.isDone()) {
                currentStrategy = trigger.getNextStage();
            }
        }
        return currentStrategy.getAction(playerView, debugInterface);
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
