package util;

public interface StrategyTrigger {
    boolean isDone();
    Strategy getNextStage();
}
