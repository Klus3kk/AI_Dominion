package put.ai.games.cli;

import org.apache.commons.cli.*;
import put.ai.games.engine.BoardFactory;
import put.ai.games.engine.GameEngine;
import put.ai.games.engine.impl.GameEngineImpl;
import put.ai.games.game.Player;
import put.ai.games.game.exceptions.RuleViolationException;
import put.ai.games.rulesprovider.RulesProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class App {

    public static int DEFAULT_TIMEOUT_MS = 20_000;
    public static int DEFAULT_MEMORY_MB = 2048;

    public static String DEFAULT_PLAYER_HANDLER_PATH = "PlayerHandler/target/PlayerHandler-2.0-SNAPSHOT.jar";

    public static void main(String[] args) {
        new App().entrypoint(args);
        // I think it is not necessary once we have moved to Docker, but better safe than sorry
        System.exit(0);
    }

    private int memoryForOtherProcesses = 500;
    private int minMemoryForPlayer = 500;
    private String playerHandlerPath = DEFAULT_PLAYER_HANDLER_PATH;
    private String dockerImage = "eclipse-temurin:21-jdk";

    private String player1jar = null;
    private String player2jar = null;
    private String game = null;
    private Integer boardSize = null;
    private int timeout = DEFAULT_TIMEOUT_MS;
    private int memory = DEFAULT_MEMORY_MB;

    public String getPlayerHandlerPath() {
        return playerHandlerPath;
    }

    public void setPlayerHandlerPath(String playerHandlerPath) {
        this.playerHandlerPath = playerHandlerPath;
    }

    public int getMinMemoryForPlayer() {
        return minMemoryForPlayer;
    }

    public void setMinMemoryForPlayer(int minMemoryForPlayer) {
        this.minMemoryForPlayer = minMemoryForPlayer;
    }

    public int getMemoryForOtherProcesses() {
        return memoryForOtherProcesses;
    }

    public void setMemoryForOtherProcesses(int memoryForOtherProcesses) {
        this.memoryForOtherProcesses = memoryForOtherProcesses;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }


    public String getPlayer1jar() {
        return player1jar;
    }

    public void setPlayer1jar(String player1jar) {
        this.player1jar = player1jar;
    }

    public String getPlayer2jar() {
        return player2jar;
    }

    public void setPlayer2jar(String player2jar) {
        this.player2jar = player2jar;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public Integer getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(Integer boardSize) {
        this.boardSize = boardSize;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    private static void assertIsFile(File f) {
        if (!(f.exists() && f.isFile() && f.canRead()))
            throw new IllegalArgumentException(String.format("%s is unsuitable", f.getAbsolutePath()));
    }

    private Process createProcess(String path, int timeout, int color, int memory) throws IOException {
        File playerProcessHandler = new File(getPlayerHandlerPath());
        assertIsFile(playerProcessHandler);
        String playerProcessHandlerJarPath = playerProcessHandler.getAbsolutePath();
        File playerJarFile = new File(path);
        assertIsFile(playerJarFile);
        String playerJarPath = playerJarFile.getAbsolutePath();
        String playerJarFileName = playerJarFile.getName();
        String col = Integer.toString(color);
        ProcessBuilder pb = new ProcessBuilder("docker", "run", "-i", "--rm", "--memory=" + Integer.toString(memory) + "m", "--cpuset-cpus=" + col,
                "-v", playerProcessHandlerJarPath + ":/opt/app/PlayerHandler-2.0-SNAPSHOT.jar",
                "-v", playerJarPath + ":/opt/app/" + playerJarFileName, getDockerImage(),
                "java", "-Xmx" + Integer.toString(memory - getMemoryForOtherProcesses()) + "M", "-jar", "opt/app/PlayerHandler-2.0-SNAPSHOT.jar",
                "opt/app/" + playerJarFileName,
                Integer.toString(timeout), col);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        return pb.start();
    }


    boolean configureFromCommandLine(String[] args) {
        final Option optionBoardSize = Option
                .builder("s")
                .longOpt("board-size")
                .desc("The size of the board, the default value is game-dependent")
                .hasArg()
                .type(Integer.class)
                .build();
        final Option optionTimeout = Option
                .builder("t")
                .longOpt("timeout")
                .desc(String.format("Time for a single move, in ms (default: %d)", DEFAULT_TIMEOUT_MS))
                .hasArg()
                .type(Integer.class)
                .build();
        final Option optionMemory = Option
                .builder("m")
                .longOpt("memory")
                .desc(String.format("Memory for the container, in MB (default: %d)", DEFAULT_MEMORY_MB))
                .hasArg()
                .type(Integer.class)
                .build();
        final Option optionHandler = Option
                .builder()
                .longOpt("player-handler")
                .desc(String.format("Path to a JAR with the player handler (default: %s)", DEFAULT_PLAYER_HANDLER_PATH))
                .hasArg()
                .build();
        final Option optionHelp = Option
                .builder("h")
                .longOpt("help")
                .build();
        Options options = new Options();
        options.addOption(optionBoardSize);
        options.addOption(optionTimeout);
        options.addOption(optionMemory);
        options.addOption(optionHandler);
        options.addOption(optionHelp);
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            List<String> rest = commandLine.getArgList();
            if (rest.size() != 3 || commandLine.hasOption(optionHelp)) {
                new HelpFormatter().printHelp("... first-player-jar second-player-jar game [options]", options);
                return false;
            }
            this.player1jar = rest.get(0);
            this.player2jar = rest.get(1);
            this.game = rest.get(2);
            if (commandLine.hasOption(optionBoardSize))
                this.boardSize = commandLine.getParsedOptionValue(optionBoardSize);
            if (commandLine.hasOption(optionTimeout))
                this.timeout = commandLine.getParsedOptionValue(optionTimeout);
            if (commandLine.hasOption(optionMemory))
                this.memory = commandLine.getParsedOptionValue(optionMemory);
            if (commandLine.hasOption(optionHandler))
                this.playerHandlerPath = commandLine.getOptionValue(optionHandler);
            return true;
        } catch (ParseException e) {
            e.printStackTrace();
            new HelpFormatter().printHelp("... first-player-jar second-player-jar game [options]", options);
            return false;
        }
    }

    public void entrypoint(String[] args) {
        PrintStream origOut = System.out;
        System.setOut(System.err);
        if (!configureFromCommandLine(args))
            return;
        BoardFactory boardFactory = RulesProvider.INSTANCE.getRulesByName(args[2]);
        if (boardFactory == null) {
            System.err.printf("Unknown game name `%s'!\n", args[0]);
            return;
        } else {
            System.err.printf("Playing %s\n", boardFactory.getClass().getSimpleName());
        }
        Message message = run(boardFactory, new String[]{args[0], args[1]});
        origOut.println(message);
    }


    public Message run(BoardFactory boardFactory, String[] players) {
        Map<String, Object> config = new HashMap<>();
        if (boardSize != null)
            config.put(BoardFactory.BOARD_SIZE, boardSize);
        boardFactory.configure(config);
        GameEngine g = new GameEngineImpl(boardFactory);
        g.setTimeout(timeout);
        Message message = new Message();
        Process[] processes = new Process[2];
        try {
            for (int i = 0; i < 2; ++i) {
                processes[i] = createProcess(players[i], timeout, i, memory);
                String name = g.addPlayer(processes[i]);
                message.setPlayer(i, name);
            }

            message.winner = g.play((c, b, m) -> System.err.println(b));
        } catch (Exception e) {
            if (e instanceof RuleViolationException)
                message.winner = Player.getOpponent(((RuleViolationException) e).getGuilty());
            message.exception = e;
        } finally {
            //TODO perhaps kill the containers with docker kill
            for (int i = 0; i < 2; ++i) {
                if (processes[i] != null)
                    processes[i].destroy();
            }
        }
        return message;
    }

}
