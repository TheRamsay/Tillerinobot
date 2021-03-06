package tillerino.tillerinobot.handlers;

import java.io.IOException;
import java.sql.SQLException;

import javax.inject.Inject;

import org.slf4j.MDC;
import org.tillerino.osuApiModel.Mods;
import org.tillerino.osuApiModel.OsuApiUser;

import lombok.RequiredArgsConstructor;
import tillerino.tillerinobot.BeatmapMeta;
import tillerino.tillerinobot.BotBackend;
import tillerino.tillerinobot.CommandHandler;
import tillerino.tillerinobot.IRCBot;
import tillerino.tillerinobot.UserDataManager.UserData;
import tillerino.tillerinobot.UserDataManager.UserData.BeatmapWithMods;
import tillerino.tillerinobot.UserException;
import tillerino.tillerinobot.UserException.RareUserException;
import tillerino.tillerinobot.lang.Language;
import tillerino.tillerinobot.websocket.LiveActivityEndpoint;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class WithHandler implements CommandHandler {
	private final BotBackend backend;

	private final LiveActivityEndpoint live;

	@Override
	public Response handle(String originalMessage, OsuApiUser apiUser,
			UserData userData) throws UserException,
			IOException, SQLException, InterruptedException {
		if (!originalMessage.toLowerCase().startsWith("with")) {
			return null;
		}

		MDC.put(IRCBot.MDC_HANDLER, "with");
		
		Language lang = userData.getLanguage();
		
		BeatmapWithMods lastSongInfo = userData.getLastSongInfo();
		if(lastSongInfo == null) {
			throw new UserException(lang.noLastSongInfo());
		}
		String message = originalMessage.substring(4).trim();
		
		Long mods = Mods.fromShortNamesContinuous(message);
		if (mods == null || mods == 0) {
			throw new UserException(lang.malformattedMods(message));
		}
		BeatmapMeta beatmap = backend.loadBeatmap(lastSongInfo.getBeatmap(), mods, lang);
		if (beatmap == null) {
			throw new RareUserException(lang.excuseForError());
		}
		if (beatmap.getMods() == 0) {
			throw new UserException(lang.noInformationForMods());
		}
		
		live.propagateMessageDetails(IRCBot.getEventId(), "!" + originalMessage);

		return new Message(beatmap.formInfoMessage(false, null,
				userData.getHearts(), null, null, null)).thenRun(
				() -> userData.setLastSongInfo(new BeatmapWithMods(beatmap
						.getBeatmap().getBeatmapId(), beatmap.getMods())))
				.then(lang.optionalCommentOnWith(apiUser, beatmap));
	}

}
