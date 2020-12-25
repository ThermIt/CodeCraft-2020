package util;

import model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static util.Initializer.getDebugInterface;

public class DebugInterface {

    public static final int TEXT_SIZE = 30;
    private static final DebugState EMPTY_STATE = new DebugState();
    private static boolean debugEnabled = false;
    private InputStream inputStream;
    private OutputStream outputStream;

    public DebugInterface(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled() {
        debugEnabled = true;
    }

    public static void print(String test, Coordinate pos) {
        print(test, pos.getX(), pos.getY());
    }

    public static void print(Integer test, Coordinate pos) {
        print(test, pos.getX(), pos.getY());
    }

    public static void println(String test, Coordinate pos, int line) {
        println(test, pos.getX(), pos.getY(), line);
    }

    public static void println(Integer test, Coordinate pos, int line) {
        if (test != null) {
            println(test.toString(), pos.getX(), pos.getY(), line);
        }
    }

    public static void print(Integer test, int x, int y) {
        print(test.toString(), x, y);
    }

    public static void println(Integer test, int x, int y, int line) {
        println(test.toString(), x, y, line);
    }

    public static void print(String test, int x, int y) {
        if (!isDebugEnabled()) {
            return;
        }
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(x, y), new Vec2Float(0, 0), new Color(0, 0, 0, 0.5f));
        DebugData data = new DebugData.PlacedText(coloredVertex, test, 0, TEXT_SIZE);
        command.setData(data);
        getDebugInterface().send(command);
    }

    public static void printRed(String test, int x, int y) {
        if (!isDebugEnabled()) {
            return;
        }
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(x, y), new Vec2Float(0, 0), new Color(1, 0, 0, 0.7f));
        DebugData data = new DebugData.PlacedText(coloredVertex, test, 0, TEXT_SIZE);
        command.setData(data);
        getDebugInterface().send(command);
    }

    public static void println(String test, int x, int y, int line) {
        if (!isDebugEnabled()) {
            return;
        }
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(x, y), new Vec2Float(0, line * TEXT_SIZE + 1), new Color(0, 0, 0, 0.5f));
        DebugData data = new DebugData.PlacedText(coloredVertex, test, 0, TEXT_SIZE);
        command.setData(data);
        getDebugInterface().send(command);
    }

    public static void line(Coordinate pos, Coordinate tgt) {
        if (!isDebugEnabled()) {
            return;
        }
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex1 = new ColoredVertex(new Vec2Float(pos.getX() + 0.5f, pos.getY() + 0.5f), new Vec2Float(0, 0), new Color(1, 1, 1, 0.7f));
        ColoredVertex coloredVertex2 = new ColoredVertex(new Vec2Float((pos.getX() + tgt.getX()) / 2.0f + 0.5f, (pos.getY() + tgt.getY()) / 2.0f + 0.5f), new Vec2Float(0, 0), new Color(0, 0, 0, 0.7f));
        ColoredVertex[] vertices = {coloredVertex1, coloredVertex2};
        DebugData data = new DebugData.Primitives(vertices, PrimitiveType.LINES);
        command.setData(data);
        getDebugInterface().send(command);

/*
                            if (debugInterface.isDebugEnabled()) {
                                DebugCommand.Add command = new DebugCommand.Add();
                                ColoredVertex[] arra = new ColoredVertex[3];
                                arra[0] = new ColoredVertex(new Vec2Float(canBuildX,canBuildY), new Vec2Float(0, 0), new Color(0, 1, 1, 0.5f));
                                arra[1] = new ColoredVertex(new Vec2Float(canBuildX+1,canBuildY), new Vec2Float(0, 0), new Color(0, 1, 1, 0.5f));
                                arra[2] = new ColoredVertex(new Vec2Float(canBuildX,canBuildY+1), new Vec2Float(0, 0), new Color(0, 1, 1, 0.5f));
                                DebugData data = new DebugData.Primitives(arra, PrimitiveType.TRIANGLES);
                                command.setData(data);
                                debugInterface.send(command);
                            }
*/
    }

    public void send(model.DebugCommand command) {
        if (!debugEnabled) {
            return;
        }
        try {
            new model.ClientMessage.DebugMessage(command).writeTo(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public model.DebugState getState() {
        if (!debugEnabled) {
            return EMPTY_STATE;
        }
        try {
            new model.ClientMessage.RequestDebugState().writeTo(outputStream);
            outputStream.flush();
            return model.DebugState.readFrom(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}