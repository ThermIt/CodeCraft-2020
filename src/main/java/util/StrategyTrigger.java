package util;

public interface StrategyTrigger {
    boolean isDone();
    StrategyDelegate getNextStage();
}
