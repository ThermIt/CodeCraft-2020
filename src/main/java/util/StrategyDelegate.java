package util;

import model.Action;
import model.PlayerView;

public interface StrategyDelegate extends StrategyTrigger {
    Action getAction(PlayerView playerView);
}
