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

    public static void print(String test, int x, int y) {
        if (!isDebugEnabled()) {
            return;
        }
        DebugCommand.Add command = new DebugCommand.Add();
        ColoredVertex coloredVertex = new ColoredVertex(new Vec2Float(x, y), new Vec2Float(0, 0), new Color(0, 0, 0, 0.5f));
        DebugData data = new DebugData.PlacedText(coloredVertex, test, -1, 12);
        command.setData(data);
        getDebugInterface().send(command);
    }
}