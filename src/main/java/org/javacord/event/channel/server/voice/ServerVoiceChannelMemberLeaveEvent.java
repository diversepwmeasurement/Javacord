package org.javacord.event.channel.server.voice;

import org.javacord.entity.channel.ServerVoiceChannel;
import org.javacord.event.user.UserEvent;

import java.util.Optional;

/**
 * A server voice channel member leave event.
 */
public interface ServerVoiceChannelMemberLeaveEvent extends ServerVoiceChannelEvent, UserEvent {

    /**
     * Gets the new channel of the event.
     *
     * @return The new channel of the event.
     */
    Optional<ServerVoiceChannel> getNewChannel();

    /**
     * Gets whether this event is part of a move.
     *
     * @return whether this event is part of a move.
     */
    boolean isMove();

}
