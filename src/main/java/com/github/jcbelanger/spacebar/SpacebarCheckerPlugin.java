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
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
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
	private InfoBoxManager infoBoxManager;

	@Inject
	private Notifier notifier;

	private final Pattern allWhitespace = Pattern.compile("^\\s*$");

	private final EnumMap<ChatMessageType, Set<String>> chatRunContributors = new EnumMap<>(ChatMessageType.class);

	private final EnumMap<ChatMessageType, Counter> chatRunCounters = new EnumMap<>(ChatMessageType.class);

	private final EnumSet<ChatMessageType> trackedChats = EnumSet.of(
			PUBLICCHAT,
			FRIENDSCHAT,
			CLAN_CHAT,
			CLAN_GUEST_CHAT
	);

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		final ChatMessageType chatType = chatMessage.getType();
		if (!trackedChats.contains(chatType)) {
			return;
		}

		Set<String> chatContributors = chatRunContributors.computeIfAbsent(chatType, key -> new HashSet<>());
		Counter chatRunCounter =  chatRunCounters.computeIfAbsent(chatType,  key -> {
			final BufferedImage image = ImageUtil.loadImageResource(SpacebarCheckerPlugin.class, "spacebar.png");
			final String text = String.format("%s spacebar check", chatName(chatType));
			Counter counter = new Counter(image, this, 0);
			counter.setTooltip(text);
			return counter;
		});

		boolean isAllWhitespace = allWhitespace.matcher(chatMessage.getMessage()).find();
		boolean isContributorAllowed = config.isDuplicatesAllowed() || !chatContributors.contains(chatMessage.getName());
		boolean isRunContinued = isAllWhitespace && isContributorAllowed;

		if (isRunContinued) {
			chatRunCounter.setCount(chatRunCounter.getCount() + 1);
			chatContributors.add(chatMessage.getName());
		} else {
			boolean isRunStarted = chatRunCounter.getCount() > 0 && chatRunCounter.getCount() >= config.minRunStart();
			if (isRunStarted) {
				String reason;
				if (!isAllWhitespace) {
					reason = ""; // non-spacebar message is the expected reason.
				} else if (!isContributorAllowed) {
					reason = " for already contributing";
				} else {
					reason = " for unknown reasons";
				}
				final String text = String.format(
						"%s ended the %s spacebar check with run of %d%s.",
						chatMessage.getMessageNode().getName(),
						chatName(chatType),
						chatRunCounter.getCount(),
						reason
				);
				client.addChatMessage(GAMEMESSAGE, "", text, null);
			}

			reset(chatType);
		}

		boolean isRunStarted = chatRunCounter.getCount() > 0 && chatRunCounter.getCount() == config.minRunStart();
		if (isRunStarted) {
			if (!infoBoxManager.getInfoBoxes().contains(chatRunCounter)) {
				infoBoxManager.addInfoBox(chatRunCounter);
			}

			if (config.isNotifyOnStart()) {
				final String text = String.format("A new spacebar check started in %s!", chatName(chatType));
				notifier.notify(text);
			}
		}
	}

	protected void reset(ChatMessageType chatType) {
		chatRunContributors.remove(chatType);
		Counter counter = chatRunCounters.remove(chatType);
		counter.setCount(0);
		infoBoxManager.removeInfoBox(counter);
	}

	protected void reset() {
		chatRunCounters.keySet().forEach(this::reset);
	}

	@Override
	protected void startUp() throws Exception {
		reset();
	}

	@Override
	protected void shutDown() throws Exception {
		reset();
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
				return "Chat";
		}
	}

	@Provides
	SpacebarCheckerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SpacebarCheckerConfig.class);
	}
}
