import util.DebugInterface;
import util.Strategy;
import util.StreamUtil;

import java.io.*;
import java.net.Socket;

public class Runner {
    private final InputStream inputStream;
    private final OutputStream outputStream;

    Runner(String host, int port, String token) throws IOException {
        Socket socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        inputStream = new BufferedInputStream(socket.getInputStream());
        outputStream = new BufferedOutputStream(socket.getOutputStream());
        StreamUtil.writeString(outputStream, token);
        outputStream.flush();
    }

    private static void runOnce(String host, int port, String token, boolean older) {
        Runnable task = () -> {
            try {
                if (DebugInterface.isDebugEnabled()) {
                    System.out.println("run " + host + ":" + port);
                }
                new Runner(host, port, token).run(older);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    public static void main(String[] args) throws IOException {
        String host = args.length < 1 ? "127.0.0.1" : args[0];
        int port = args.length < 2 ? 31001 : Integer.parseInt(args[1]);
        String token = args.length < 3 ? "0000000000000000" : args[2];
        if (args.length > 4 && "debug".equals(args[4])) {
            DebugInterface.setDebugEnabled();
        }
        if (args.length > 3 && "multiply2".equals(args[3])) {
            runOnce(host, port, token, false);
            runOnce(host, port + 1, token, true);
        } else  {
            new Runner(host, port, token).run();
        }
    }

    void run() throws IOException {
        run(false);
    }

    void run(boolean older) throws IOException {
        try {
            Strategy myStrategy = older ? new MyStrategy() : new MyStrategy();
            DebugInterface debugInterface = new DebugInterface(inputStream, outputStream);
            while (true) {
                model.ServerMessage message = model.ServerMessage.readFrom(inputStream);
                if (message instanceof model.ServerMessage.GetAction) {
                    model.ServerMessage.GetAction getActionMessage = (model.ServerMessage.GetAction) message;
                    new model.ClientMessage.ActionMessage(myStrategy.getAction(getActionMessage.getPlayerView(), getActionMessage.isDebugAvailable() ? debugInterface : null)).writeTo(outputStream);
                    outputStream.flush();
                } else if (message instanceof model.ServerMessage.Finish) {
                    break;
                } else if (debugInterface.isDebugEnabled() && message instanceof model.ServerMessage.DebugUpdate) {
                    model.ServerMessage.DebugUpdate debugUpdateMessage = (model.ServerMessage.DebugUpdate) message;
                    myStrategy.debugUpdate(debugUpdateMessage.getPlayerView(), debugInterface);
                    new model.ClientMessage.DebugUpdateDone().writeTo(outputStream);
                    outputStream.flush();
                } else {
                    throw new IOException("Unexpected server message");
                }
            }
        } catch (IOException e) {
            System.out.println(e.toString());
            e.printStackTrace();
            throw e;
        }
    }
}