package older.v10.finals.part1.collections;

import model.Coordinate;

import java.util.*;
import java.util.function.Consumer;

public class SingleVisitCoordinateSet implements Iterable<Coordinate> {
    private final int MAP_SIZE = 80;
    boolean[][] beenThere = new boolean[MAP_SIZE][MAP_SIZE];
    List<Coordinate> coordinateList = new ArrayList<>(256);
    List<Coordinate> coordinateListNextStep = new ArrayList<>(256);

    @Override
    public Iterator<Coordinate> iterator() {
        return coordinateList.iterator();
    }

    @Override
    public Spliterator<Coordinate> spliterator() {
        return coordinateList.spliterator();
    }

    @Override
    public void forEach(Consumer<? super Coordinate> action) {
        coordinateList.forEach(action);
    }

    public boolean isEmpty() {
        return coordinateList.isEmpty();
    }

    public int size() {
        return coordinateList.size();
    }

    public boolean add(Coordinate coordinate) {
        if (coordinate.isOutOfBounds() || beenThere[coordinate.getX()][coordinate.getY()]) {
            return false;
        }
        beenThere[coordinate.getX()][coordinate.getY()] = true;
        return coordinateList.add(coordinate);
    }

    public boolean addOnNextStep(Coordinate coordinate) {
        if (coordinate.isOutOfBounds() || beenThere[coordinate.getX()][coordinate.getY()]) {
            return false;
        }
        beenThere[coordinate.getX()][coordinate.getY()] = true;
        return coordinateListNextStep.add(coordinate);
    }

    public boolean addOnNextStepByForce(Coordinate coordinate) {
        beenThere[coordinate.getX()][coordinate.getY()] = true;
        return coordinateListNextStep.add(coordinate);
    }

    public void nextStep() {
        coordinateList = coordinateListNextStep;
        coordinateListNextStep = new ArrayList<>(256);
    }

    public Set<Coordinate> getSet() {
        return new HashSet<>(coordinateList);
    }

    public void addAll(Iterable<Coordinate> coordinates) {
        for (Coordinate pos : coordinates) {
            add(pos);
        }
    }

    public void remove(Coordinate coordinate) {
        if (coordinate.isOutOfBounds()) {
            return;
        }
        if (!beenThere[coordinate.getX()][coordinate.getY()]) {
            beenThere[coordinate.getX()][coordinate.getY()] = true;
            return;
        }
        coordinateList.remove(coordinate);
    }
}
