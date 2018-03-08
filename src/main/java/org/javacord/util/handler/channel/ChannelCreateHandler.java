package org.javacord.util.handler.channel;

import com.fasterxml.jackson.databind.JsonNode;
import org.javacord.DiscordApi;
import org.javacord.entity.channel.ChannelCategory;
import org.javacord.entity.channel.GroupChannel;
import org.javacord.entity.channel.PrivateChannel;
import org.javacord.entity.channel.ServerTextChannel;
import org.javacord.entity.channel.ServerVoiceChannel;
import org.javacord.entity.channel.impl.ImplGroupChannel;
import org.javacord.entity.server.impl.ImplServer;
import org.javacord.entity.user.User;
import org.javacord.entity.user.impl.ImplUser;
import org.javacord.event.channel.group.GroupChannelCreateEvent;
import org.javacord.event.channel.group.impl.ImplGroupChannelCreateEvent;
import org.javacord.event.channel.server.ServerChannelCreateEvent;
import org.javacord.event.channel.server.impl.ImplServerChannelCreateEvent;
import org.javacord.event.channel.user.PrivateChannelCreateEvent;
import org.javacord.event.channel.user.impl.ImplPrivateChannelCreateEvent;
import org.javacord.listener.channel.group.GroupChannelCreateListener;
import org.javacord.listener.channel.server.ServerChannelCreateListener;
import org.javacord.listener.channel.user.PrivateChannelCreateListener;
import org.javacord.util.gateway.PacketHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the channel create packet.
 */
public class ChannelCreateHandler extends PacketHandler {

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public ChannelCreateHandler(DiscordApi api) {
        super(api, true, "CHANNEL_CREATE");
    }

    @Override
    public void handle(JsonNode packet) {
        int type = packet.get("type").asInt();
        switch (type) {
            case 0:
                handleServerTextChannel(packet);
                break;
            case 1:
                handlePrivateChannel(packet);
                break;
            case 2:
                handleServerVoiceChannel(packet);
                break;
            case 3:
                handleGroupChannel(packet);
                break;
            case 4:
                handleChannelCategory(packet);
                break;
        }
    }

    /**
     * Handles channel category creation.
     *
     * @param channel The channel data.
     */
    private void handleChannelCategory(JsonNode channel) {
        long serverId = channel.get("guild_id").asLong();
        api.getServerById(serverId).ifPresent(server -> {
            ChannelCategory channelCategory = ((ImplServer) server).getOrCreateChannelCategory(channel);
            ServerChannelCreateEvent event = new ImplServerChannelCreateEvent(channelCategory);

            List<ServerChannelCreateListener> listeners = new ArrayList<>();
            listeners.addAll(server.getServerChannelCreateListeners());
            listeners.addAll(api.getServerChannelCreateListeners());

            api.getEventDispatcher().dispatchEvent(server, listeners, listener -> listener.onServerChannelCreate(event));
        });
    }

    /**
     * Handles server text channel creation.
     *
     * @param channel The channel data.
     */
    private void handleServerTextChannel(JsonNode channel) {
        long serverId = channel.get("guild_id").asLong();
        api.getServerById(serverId).ifPresent(server -> {
            ServerTextChannel textChannel = ((ImplServer) server).getOrCreateServerTextChannel(channel);
            ServerChannelCreateEvent event = new ImplServerChannelCreateEvent(textChannel);

            List<ServerChannelCreateListener> listeners = new ArrayList<>();
            listeners.addAll(server.getServerChannelCreateListeners());
            listeners.addAll(api.getServerChannelCreateListeners());

            api.getEventDispatcher().dispatchEvent(server, listeners, listener -> listener.onServerChannelCreate(event));
        });
    }

    /**
     * Handles server voice channel creation.
     *
     * @param channel The channel data.
     */
    private void handleServerVoiceChannel(JsonNode channel) {
        long serverId = channel.get("guild_id").asLong();
        api.getServerById(serverId).ifPresent(server -> {
            ServerVoiceChannel voiceChannel = ((ImplServer) server).getOrCreateServerVoiceChannel(channel);
            ServerChannelCreateEvent event = new ImplServerChannelCreateEvent(voiceChannel);

            List<ServerChannelCreateListener> listeners = new ArrayList<>();
            listeners.addAll(server.getServerChannelCreateListeners());
            listeners.addAll(api.getServerChannelCreateListeners());

            api.getEventDispatcher().dispatchEvent(server, listeners, listener -> listener.onServerChannelCreate(event));
        });
    }

    /**
     * Handles a private channel creation.
     *
     * @param channel The channel data.
     */
    private void handlePrivateChannel(JsonNode channel) {
        // A CHANNEL_CREATE packet is sent every time a bot account receives a message, see
        // https://github.com/hammerandchisel/discord-api-docs/issues/184
        ImplUser recipient = (ImplUser) api.getOrCreateUser(channel.get("recipients").get(0));
        if (!recipient.getPrivateChannel().isPresent()) {
            PrivateChannel privateChannel = recipient.getOrCreateChannel(channel);
            PrivateChannelCreateEvent event = new ImplPrivateChannelCreateEvent(privateChannel);

            List<PrivateChannelCreateListener> listeners = new ArrayList<>();
            listeners.addAll(recipient.getPrivateChannelCreateListeners());
            listeners.addAll(api.getPrivateChannelCreateListeners());

            api.getEventDispatcher().dispatchEvent(api, listeners, listener -> listener.onPrivateChannelCreate(event));
        }
    }

    /**
     * Handles a group channel creation.
     *
     * @param channel The channel data.
     */
    private void handleGroupChannel(JsonNode channel) {
        long channelId = channel.get("id").asLong();
        if (!api.getGroupChannelById(channelId).isPresent()) {
            GroupChannel groupChannel = new ImplGroupChannel(api, channel);
            GroupChannelCreateEvent event = new ImplGroupChannelCreateEvent(groupChannel);

            List<GroupChannelCreateListener> listeners = new ArrayList<>();
            groupChannel.getMembers().stream()
                    .map(User::getGroupChannelCreateListeners)
                    .forEach(listeners::addAll);
            listeners.addAll(api.getGroupChannelCreateListeners());

            api.getEventDispatcher().dispatchEvent(api, listeners, listener -> listener.onGroupChannelCreate(event));
        }
    }

}