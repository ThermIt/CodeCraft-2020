import mystrategy.strategies.DefaultStrategy;
import mystrategy.strategies.DelegatingStrategy;
import older.v06.ranger.stream.strategies.Older6RangerStream;
import older.v09.finals1.strategies.Older9Finals1;
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

    private static void runOnceMultithreaded(String host, int port, String token, Strategy myStrategy) {
        Runnable task = () -> {
            runOnceSameThread(host, port, token, myStrategy);
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private static void runOnceSameThread(String host, int port, String token, Strategy myStrategy) {
        try {
            if (DebugInterface.isDebugEnabled()) {
                System.out.println("run " + host + ":" + port);
            }
            new Runner(host, port, token).run(myStrategy);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String host = args.length < 1 ? "127.0.0.1" : args[0];
        int port = args.length < 2 ? 31001 : Integer.parseInt(args[1]);
        String token = args.length < 3 ? "0000000000000000" : args[2];
        if (args.length > 4 && "debug".equals(args[4])) {
            DebugInterface.setDebugEnabled();
        }
        if (args.length > 3 && "multiply2".equals(args[3])) {
            runOnceMultithreaded(host, port, token, new DelegatingStrategy());
            runOnceMultithreaded(host, port + 1, token, new Older9Finals1());
        } else if (args.length > 3 && "multiply3".equals(args[3])) {
            runOnceMultithreaded(host, port, token, new DelegatingStrategy());
            runOnceMultithreaded(host, port + 1, token, new Older6RangerStream());
            runOnceMultithreaded(host, port + 2, token, new Older6RangerStream());
        } else if (args.length > 3 && "multiply4".equals(args[3])) {
            runOnceMultithreaded(host, port, token, new DelegatingStrategy());
            runOnceMultithreaded(host, port + 1, token, new Older6RangerStream());
            runOnceMultithreaded(host, port + 2, token, new Older6RangerStream());
            runOnceMultithreaded(host, port + 3, token, new Older6RangerStream());
        } else {
            runOnceSameThread(host, port, token, new DelegatingStrategy());
        }
    }

    void run(Strategy myStrategy) throws IOException {
        try {
            DebugInterface debugInterface = new DebugInterface(inputStream, outputStream);
            while (true) {
                if (syncRun(myStrategy, debugInterface)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(e.toString());
            e.printStackTrace();
            throw e;
        }
    }

    private synchronized boolean syncRun(Strategy myStrategy, DebugInterface debugInterface) throws IOException {
        model.ServerMessage message = model.ServerMessage.readFrom(inputStream);
        if (message instanceof model.ServerMessage.GetAction) {
            model.ServerMessage.GetAction getActionMessage = (model.ServerMessage.GetAction) message;
            new model.ClientMessage.ActionMessage(myStrategy.getAction(getActionMessage.getPlayerView(), getActionMessage.isDebugAvailable() ? debugInterface : null)).writeTo(outputStream);
            outputStream.flush();
        } else if (message instanceof model.ServerMessage.Finish) {
            return true;
        } else if (message instanceof model.ServerMessage.DebugUpdate) {
            if (!(myStrategy instanceof DefaultStrategy)) {
                model.ServerMessage.DebugUpdate debugUpdateMessage = (model.ServerMessage.DebugUpdate) message;
                myStrategy.debugUpdate(debugUpdateMessage.getPlayerView(), debugInterface);
            }
            new model.ClientMessage.DebugUpdateDone().writeTo(outputStream);
            outputStream.flush();
        } else {
            throw new IOException("Unexpected server message");
        }
        return false;
    }
}