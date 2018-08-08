package example;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.gateway.IdentifyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Arguments: <token> [shardId] [shardCount]");
        }
        String token = args[0];
        int shardId = 0;
        int shardCount = 1;
        if (args.length >= 2) {
            shardId = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            shardCount = Integer.parseInt(args[2]);
        }

        IdentifyOptions options = new IdentifyOptions(shardId, shardCount, null);

        try {
            Path path = Paths.get("resume.dat");
            if (Files.isRegularFile(path)
                && Files.getLastModifiedTime(path).toInstant().plusSeconds(60).isAfter(Instant.now())) {
                for (String line : Files.readAllLines(path)) {
                    String[] tokens = line.split(";", 2);
                    options.setResumeSessionId(tokens[0]);
                    options.setResumeSequence(Integer.valueOf(tokens[1]));
                }
            } else {
                log.debug("Not attempting to resume");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Persist our identify options
            try {
                String sessionId = options.getResumeSessionId();
                Integer sequence = options.getResumeSequence();
                log.debug("Resuming data: {}, {}", sessionId, sequence);
                Path saved = Files.write(Paths.get("resume.dat"),
                    Collections.singletonList(sessionId + ";" + sequence));
                log.info("File saved to {}", saved.toAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        DiscordClient client = new DiscordClientBuilder(token)
                .setIdentifyOptions(options)
                .build();

        BlockingEchoListener commandListener = new BlockingEchoListener(client);
        commandListener.configure();

        LifecycleListener lifecycleListener = new LifecycleListener(client);
        lifecycleListener.configure();

        client.login().block();
    }
}
