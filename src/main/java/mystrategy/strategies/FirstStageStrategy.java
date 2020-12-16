package mystrategy.strategies;

import model.Action;
import model.PlayerView;
import util.DebugInterface;
import util.Strategy;
import util.StrategyTrigger;

public class FirstStageStrategy implements Strategy, StrategyTrigger {

    private boolean done;

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
        return null;
    }

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {
    }
}
