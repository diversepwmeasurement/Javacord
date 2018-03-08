package org.javacord.entity.channel.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.javacord.DiscordApi;
import org.javacord.ImplDiscordApi;
import org.javacord.entity.DiscordEntity;
import org.javacord.entity.channel.InternalChannel;
import org.javacord.entity.channel.InternalTextChannel;
import org.javacord.entity.channel.InternalVoiceChannel;
import org.javacord.entity.channel.PrivateChannel;
import org.javacord.entity.user.User;
import org.javacord.entity.user.impl.ImplUser;
import org.javacord.listener.ChannelAttachableListener;
import org.javacord.listener.ObjectAttachableListener;
import org.javacord.listener.TextChannelAttachableListener;
import org.javacord.listener.VoiceChannelAttachableListener;
import org.javacord.listener.channel.user.PrivateChannelAttachableListener;
import org.javacord.listener.channel.user.PrivateChannelDeleteListener;
import org.javacord.util.ClassHelper;
import org.javacord.util.Cleanupable;
import org.javacord.util.cache.ImplMessageCache;
import org.javacord.util.cache.MessageCache;
import org.javacord.util.event.ListenerManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The implementation of {@link PrivateChannel}.
 */
public class ImplPrivateChannel
        implements PrivateChannel, Cleanupable, InternalChannel, InternalTextChannel, InternalVoiceChannel {

    /**
     * The discord api instance.
     */
    private final ImplDiscordApi api;

    /**
     * The id of the channel.
     */
    private final long id;

    /**
     * The recipient of the private channel.
     */
    private final ImplUser recipient;

    /**
     * The message cache of the private channel.
     */
    private final ImplMessageCache messageCache;

    /**
     * Creates a new private channel.
     *
     * @param api The discord api instance.
     * @param data The json data of the channel.
     */
    public ImplPrivateChannel(ImplDiscordApi api, JsonNode data) {
        this.api = api;
        this.recipient = (ImplUser) api.getOrCreateUser(data.get("recipients").get(0));
        this.messageCache = new ImplMessageCache(
                api, api.getDefaultMessageCacheCapacity(), api.getDefaultMessageCacheStorageTimeInSeconds());

        id = Long.parseLong(data.get("id").asText());
        recipient.setChannel(this);
    }

    @Override
    public DiscordApi getApi() {
        return api;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public User getRecipient() {
        return recipient;
    }

    @Override
    public ListenerManager<PrivateChannelDeleteListener> addPrivateChannelDeleteListener(
            PrivateChannelDeleteListener listener) {
        return ((ImplDiscordApi) getApi()).addObjectListener(
                PrivateChannel.class, getId(), PrivateChannelDeleteListener.class, listener);
    }

    @Override
    public List<PrivateChannelDeleteListener> getPrivateChannelDeleteListeners() {
        return ((ImplDiscordApi) getApi()).getObjectListeners(
                PrivateChannel.class, getId(), PrivateChannelDeleteListener.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PrivateChannelAttachableListener & ObjectAttachableListener>
    Collection<ListenerManager<? extends PrivateChannelAttachableListener>> addPrivateChannelAttachableListener(
            T listener) {
        return ClassHelper.getInterfacesAsStream(listener.getClass())
                .filter(PrivateChannelAttachableListener.class::isAssignableFrom)
                .filter(ObjectAttachableListener.class::isAssignableFrom)
                .map(listenerClass -> (Class<T>) listenerClass)
                .flatMap(listenerClass -> {
                    if (ChannelAttachableListener.class.isAssignableFrom(listenerClass)) {
                        return addChannelAttachableListener(
                                (ChannelAttachableListener & ObjectAttachableListener) listener).stream();
                    } else if (TextChannelAttachableListener.class.isAssignableFrom(listenerClass)) {
                        return addTextChannelAttachableListener(
                                (TextChannelAttachableListener & ObjectAttachableListener) listener).stream();
                    } else if (VoiceChannelAttachableListener.class.isAssignableFrom(listenerClass)) {
                        return addVoiceChannelAttachableListener(
                                (VoiceChannelAttachableListener & ObjectAttachableListener) listener).stream();
                    } else {
                        return Stream.of(((ImplDiscordApi) getApi()).addObjectListener(PrivateChannel.class, getId(),
                                                                                       listenerClass, listener));
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PrivateChannelAttachableListener & ObjectAttachableListener> void
    removePrivateChannelAttachableListener(T listener) {
        ClassHelper.getInterfacesAsStream(listener.getClass())
                .filter(PrivateChannelAttachableListener.class::isAssignableFrom)
                .filter(ObjectAttachableListener.class::isAssignableFrom)
                .map(listenerClass -> (Class<T>) listenerClass)
                .forEach(listenerClass -> {
                    if (ChannelAttachableListener.class.isAssignableFrom(listenerClass)) {
                        removeChannelAttachableListener(
                                (ChannelAttachableListener & ObjectAttachableListener) listener);
                    } else if (TextChannelAttachableListener.class.isAssignableFrom(listenerClass)) {
                        removeTextChannelAttachableListener(
                                (TextChannelAttachableListener & ObjectAttachableListener) listener);
                    } else if (VoiceChannelAttachableListener.class.isAssignableFrom(listenerClass)) {
                        removeVoiceChannelAttachableListener(
                                (VoiceChannelAttachableListener & ObjectAttachableListener) listener);
                    } else {
                        ((ImplDiscordApi) getApi()).removeObjectListener(PrivateChannel.class, getId(),
                                                                         listenerClass, listener);
                    }
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PrivateChannelAttachableListener & ObjectAttachableListener> Map<T, List<Class<T>>>
    getPrivateChannelAttachableListeners() {
        Map<T, List<Class<T>>> privateChannelListeners =
                ((ImplDiscordApi) getApi()).getObjectListeners(PrivateChannel.class, getId());
        getTextChannelAttachableListeners().forEach((listener, listenerClasses) -> privateChannelListeners
                .merge((T) listener,
                       (List<Class<T>>) (Object) listenerClasses,
                       (listenerClasses1, listenerClasses2) -> {
                           listenerClasses1.addAll(listenerClasses2);
                           return listenerClasses1;
                       }));
        getVoiceChannelAttachableListeners().forEach((listener, listenerClasses) -> privateChannelListeners
                .merge((T) listener,
                       (List<Class<T>>) (Object) listenerClasses,
                       (listenerClasses1, listenerClasses2) -> {
                           listenerClasses1.addAll(listenerClasses2);
                           return listenerClasses1;
                       }));
        getChannelAttachableListeners().forEach((listener, listenerClasses) -> privateChannelListeners
                .merge((T) listener,
                       (List<Class<T>>) (Object) listenerClasses,
                       (listenerClasses1, listenerClasses2) -> {
                           listenerClasses1.addAll(listenerClasses2);
                           return listenerClasses1;
                       }));
        return privateChannelListeners;
    }

    @Override
    public <T extends PrivateChannelAttachableListener & ObjectAttachableListener> void removeListener(
            Class<T> listenerClass, T listener) {
        ((ImplDiscordApi) getApi()).removeObjectListener(PrivateChannel.class, getId(), listenerClass, listener);
    }

    @Override
    public MessageCache getMessageCache() {
        return messageCache;
    }

    @Override
    public void cleanup() {
        messageCache.cleanup();
    }

    @Override
    public boolean equals(Object o) {
        return (this == o)
               || !((o == null)
                    || (getClass() != o.getClass())
                    || (getId() != ((DiscordEntity) o).getId()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return String.format("PrivateChannel (id: %s, recipient: %s)", getIdAsString(), getRecipient());
    }

}
