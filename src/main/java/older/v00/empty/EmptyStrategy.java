package older.v00.empty;

import model.Action;
import model.PlayerView;
import util.DebugInterface;
import util.Strategy;

public class EmptyStrategy implements Strategy {
    @Override
    public Action getAction(PlayerView playerView, DebugInterface debugInterface) {
        return new Action(new java.util.HashMap<>());
    }

    @Override
    public void debugUpdate(PlayerView playerView, DebugInterface debugInterface) {

    }
}
