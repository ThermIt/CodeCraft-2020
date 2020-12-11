package model;

import util.StreamUtil;

public class Coordinate {
    private int x;
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    private int y;
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public Coordinate() {}
    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public static Coordinate readFrom(java.io.InputStream stream) throws java.io.IOException {
        Coordinate result = new Coordinate();
        result.x = StreamUtil.readInt(stream);
        result.y = StreamUtil.readInt(stream);
        return result;
    }
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        StreamUtil.writeInt(stream, x);
        StreamUtil.writeInt(stream, y);
    }
}
