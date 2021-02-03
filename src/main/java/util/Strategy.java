package util;

import model.Action;
import model.PlayerView;

public interface Strategy {

    Action getAction(PlayerView playerView, DebugInterface debugInterface);
    void debugUpdate(PlayerView playerView, DebugInterface debugInterface);
}
