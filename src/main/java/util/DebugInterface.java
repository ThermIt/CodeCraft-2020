package util;

import model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static util.Initializer.getDebugInterface;

public class DebugInterface {

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

    public static void print(Integer test, int x, int y) {
        print(test.toString(), x, y);
    }

    public static void print(String test, int x, int y) {
        if (!isDebugEnabled()) {
            return;
        }
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(x, y), new Vec2Float(0, 0), new Color(0, 0, 0, 0.5f));
        DebugData data = new DebugData.PlacedText(coloredVertex, test, -1, 24);
        command.setData(data);
        getDebugInterface().send(command);
    }

    public static void line(Coordinate pos, Coordinate tgt) {
        if (!isDebugEnabled()) {
            return;
        }
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex1 = new ColoredVertex(new Vec2Float(pos.getX()+0.5f, pos.getY()+0.5f), new Vec2Float(0, 0), new Color(1, 1, 1, 0.7f));
        ColoredVertex coloredVertex2 = new ColoredVertex(new Vec2Float(tgt.getX()+0.5f, tgt.getY()+0.5f), new Vec2Float(0, 0), new Color(0, 0, 0, 0.7f));
        ColoredVertex[] vertices = {coloredVertex1,coloredVertex2};
        DebugData data = new DebugData.Primitives(vertices, PrimitiveType.LINES);
        command.setData(data);
        getDebugInterface().send(command);
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