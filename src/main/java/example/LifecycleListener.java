package example;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.*;

public class LifecycleListener {

    private final DiscordClient client;

    public LifecycleListener(DiscordClient client) {
        this.client = client;
    }

    public void configure() {
        client.getEventDispatcher().on(ConnectEvent.class).subscribe();
        client.getEventDispatcher().on(DisconnectEvent.class).subscribe();
        client.getEventDispatcher().on(ReconnectStartEvent.class).subscribe();
        client.getEventDispatcher().on(ReconnectEvent.class).subscribe();
        client.getEventDispatcher().on(ReconnectFailEvent.class).subscribe();
    }
}
