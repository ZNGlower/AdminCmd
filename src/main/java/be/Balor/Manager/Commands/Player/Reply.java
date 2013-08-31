/************************************************************************
 * This file is part of AdminCmd.
 *
 * AdminCmd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminCmd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminCmd.  If not, see <http://www.gnu.org/licenses/>.
 ************************************************************************/
package be.Balor.Manager.Commands.Player;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import be.Balor.Manager.LocaleManager;
import be.Balor.Manager.Commands.CommandArgs;
import be.Balor.Manager.Exceptions.ActionNotPermitedException;
import be.Balor.Manager.Exceptions.PlayerNotFound;
import be.Balor.Player.ACPlayer;
import be.Balor.Tools.Type;
import be.Balor.Tools.CommandUtils.Materials;
import be.Balor.Tools.CommandUtils.Users;
import be.Balor.Tools.Debug.ACLogger;
import be.Balor.bukkit.AdminCmd.ACHelper;
import be.Balor.bukkit.AdminCmd.ConfigEnum;
import be.Balor.bukkit.AdminCmd.LocaleHelper;
import belgium.Balor.Workers.AFKWorker;

/**
 * @author Lathanael (aka Philippe Leipold)
 * 
 */
public class Reply extends PlayerCommand {

	/**
	 *
	 */
	public Reply() {
		permNode = "admincmd.player.reply";
		cmdName = "bal_reply";
	}

	@Override
	public void execute(final CommandSender sender, final CommandArgs args) throws ActionNotPermitedException, PlayerNotFound {
		if (!Users.isPlayer(sender, true)) {
			return;
		}

		if (Users.isPlayer(sender, false) && ACPlayer.getPlayer(((Player) sender)).hasPower(Type.MUTED) && ConfigEnum.MUTEDPM.getBoolean()) {
			LocaleManager.sI18n(sender, "muteEnabled");
			return;
		}
		final Player pSender = (Player) sender;
		final Player buddy = ACHelper.getInstance().getReplyPlayer(pSender);
		if (buddy != null) {
			if (!buddy.isOnline()) {
				LocaleManager.sI18n(sender, "offline", "player", buddy.getDisplayName());
				ACHelper.getInstance().removeReplyPlayer(pSender);
				return;
			}
			if (Users.isInvisibleTo(buddy, sender)) {
				LocaleManager.sI18n(sender, "playerNotFound", "player", args.getString(0));
				return;
			}
			String senderPm = "";
			String msg = "";
			String senderName = "";
			senderName = pSender.getName();
			senderPm = Users.getPlayerName(pSender, buddy) + ChatColor.WHITE + " - ";

			for (final String arg : args) {
				msg += arg + " ";
			}
			msg = msg.trim();
			String parsed = Materials.colorParser(msg);
			if (parsed == null) {
				parsed = msg;
			}
			final HashMap<String, String> replace = new HashMap<String, String>();
			replace.put("sender", senderPm);
			replace.put("receiver", Users.getPlayerName(buddy, sender));
			buddy.sendMessage(LocaleManager.I18n("privateMessageHeader", replace) + parsed);
			ACHelper.getInstance().setReplyPlayer(buddy, pSender);
			if (AFKWorker.getInstance().isAfk(buddy)) {
				AFKWorker.getInstance().sendAfkMessage(sender, buddy);
			} else {
				sender.sendMessage(LocaleManager.I18n("privateMessageHeader", replace) + parsed);
			}
			final String spyMsg = LocaleHelper.SPYMSG_HEADER.getLocale(replace) + parsed;
			for (final Player p : ACHelper.getInstance().getSpyPlayers()) {
				if (p != null && !p.getName().equals(senderName) && !p.getName().equals(buddy.getName())) {
					p.sendMessage(spyMsg);
				}
			}
			if (ConfigEnum.LOG_PM.getBoolean()) {
				ACLogger.info(spyMsg);
			}
		} else {
			LocaleManager.sI18n(sender, "noPlayerToReply");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.Balor.Manager.ACCommands#argsCheck(java.lang.String[])
	 */
	@Override
	public boolean argsCheck(final String... args) {
		return args != null && args.length >= 1;
	}

}
