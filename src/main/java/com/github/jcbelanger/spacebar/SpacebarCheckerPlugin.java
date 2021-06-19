package com.github.jcbelanger.spacebar;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;
import java.util.regex.Pattern;

import static net.runelite.api.ChatMessageType.*;

@Slf4j
@PluginDescriptor(
	name = "Spacebar Checker",
	description = "Counts consecutive spacebar messages in chats."
)
public class SpacebarCheckerPlugin extends Plugin {

	@Inject
	private Client client;

	@Inject
	private SpacebarCheckerConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Notifier notifier;

	private final Pattern allWhitespace = Pattern.compile("^\\s*$");

	private final EnumMap<ChatMessageType, Integer> chatRunCounts = new EnumMap<>(ChatMessageType.class);

	private final EnumMap<ChatMessageType, Set<String>> chatRunContributors = new EnumMap<>(ChatMessageType.class);

	private final EnumSet<ChatMessageType> trackedChats = EnumSet.of(
		PUBLICCHAT,
		FRIENDSCHAT,
		CLAN_CHAT,
		CLAN_GUEST_CHAT
	);

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		ChatMessageType chatType = chatMessage.getType();
		if (!trackedChats.contains(chatType)) {
			return;
		}

		int chatRunCount = chatRunCounts.getOrDefault(chatType, 0);
		Set<String> chatContributors = chatRunContributors.computeIfAbsent(chatType, key -> new HashSet<>());

		boolean isAllWhitespace = allWhitespace.matcher(chatMessage.getMessage()).find();
		boolean isContributorAllowed = config.isDuplicatesAllowed() || !chatContributors.contains(chatMessage.getName());
		boolean isRunContinued = isAllWhitespace && isContributorAllowed;

		if (isRunContinued) {
			chatRunCount++;
			chatContributors.add(chatMessage.getName());
		} else {
			if(chatRunCount > 0 && chatRunCount >= config.minRunStart()) {
				String reason;
				if (!isAllWhitespace) {
					reason = "a non-spacebar message";
				} else if (!isContributorAllowed) {
					reason = "already contributing";
				} else {
					reason = "unknown reasons";
				}
				final String text = String.format("%s ended the spacebar check with run of %d in %s for %s.", chatMessage.getMessageNode().getName(), chatRunCount, chatName(chatType), reason);
				client.addChatMessage(GAMEMESSAGE, "", text, null);
			}
			chatRunCount = 0;
			chatContributors.clear();
		}
		chatRunCounts.put(chatType, chatRunCount);

		if (config.isNotifyOnStart()) {
			if (chatRunCount > 0 && chatRunCount == config.minRunStart()) {
				final String text = String.format("A new spacebar check started in %s!", chatName(chatType));
				notifier.notify(text);
			}
		}

	}

	protected String chatName(ChatMessageType chatType) {
		switch (chatType) {
			case PUBLICCHAT:
				return "Public Chat";
			case FRIENDSCHAT:
				return "Friends Chat";
			case CLAN_CHAT:
				return "Clan Chat";
			case CLAN_GUEST_CHAT:
				return "Clan Guest Chat";
			default:
				return "Other Chat";
		}
	}

	@Provides
	SpacebarCheckerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SpacebarCheckerConfig.class);
	}
}

