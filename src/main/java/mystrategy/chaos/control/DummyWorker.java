package mystrategy.chaos.control;

import model.Coordinate;
import model.Entity;

public class DummyWorker {
    int score = 0;
    Coordinate position;
    MoveOrder order;
    DummyWorker parent;

    public DummyWorker(Entity worker) {
        position = worker.getPosition();
    }

    public DummyWorker(DummyWorker dummy, Coordinate coordinate, int score, MoveOrder order) {
        this.parent = dummy;
        this.score = score;
        this.position = coordinate;
        this.order = order;
    }

    public int getScore() {
        return score;
    }

    public Coordinate getPosition() {
        return position;
    }

    public MoveOrder getOrder() {
        return order;
    }
}
