package example;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ResumeEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicLong;

public class BlockingEchoListener {

    private static final Logger log = LoggerFactory.getLogger(BlockingEchoListener.class);

    private final DiscordClient client;
    private final AtomicLong ownerId = new AtomicLong();

    public BlockingEchoListener(DiscordClient client) {
        this.client = client;
    }

    public void configure() {
        Flux.first(client.getEventDispatcher().on(ReadyEvent.class), client.getEventDispatcher().on(ResumeEvent.class))
            .next()
            .flatMap(evt -> client.getApplicationInfo())
            .map(ApplicationInfo::getOwnerId)
            .map(Snowflake::asLong)
            .subscribe(ownerId::set);

        client.getEventDispatcher().on(MessageCreateEvent.class)
            .onErrorContinue()
            .map(event -> {
                Message message = event.getMessage();
                message.getAuthorId()
                    .filter(id -> ownerId.get() == id.asLong()) // only accept bot owner messages
                    .flatMap(id -> message.getContent())
                    .ifPresent(content -> {
                        log.info("Owner sent: {}", content);
                        MessageChannel channel = message.getChannel().block();
                        if (channel != null) {
                            channel.createMessage("Echo: " + content).block();
                        }
                    });
                return event;
            })
            .subscribe();
    }
}
