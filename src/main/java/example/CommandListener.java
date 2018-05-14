package example;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ResumeEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Message;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicLong;

public class CommandListener {

    private static final Logger log = LoggerFactory.getLogger(CommandListener.class);

    private final DiscordClient client;
    private final AtomicLong ownerId = new AtomicLong();

    public CommandListener(DiscordClient client) {
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
                .doOnNext(event -> {
                    Message message = event.getMessage();

                    message.getAuthorId()
                            .filter(id -> ownerId.get() == id.asLong()) // only accept bot owner messages
                            .flatMap(id -> message.getContent())
                            .ifPresent(content -> {
                                if ("!close".equals(content)) {
                                    client.logout();
                                } else if ("!retry".equals(content)) {
                                    client.reconnect();
                                } else if ("!online".equals(content)) {
                                    client.updatePresence(Presence.online()).subscribe();
                                } else if ("!dnd".equals(content)) {
                                    client.updatePresence(Presence.doNotDisturb()).subscribe();
                                } else if ("!raise".equals(content)) {
                                    // exception if DM
                                    Snowflake guildId = message.getGuild().block().getId();
                                    log.info("Message came from guild: {}", guildId);
                                }
                            });
                })
                .retry() // retry if above block throws
                .subscribe();
    }
}
