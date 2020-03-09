package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatShout implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		1
	};
	
	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT))
			return;
		
		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		for (Player player : World.getInstance().getPlayers())
		{
			if (!BlockList.isBlocked(player, activeChar) && activeChar.getLevel() >= Config.CHAT_GLOBAL_LEVEL)
				player.sendPacket(cs);
		}
			if (!(activeChar.getLevel() >= Config.CHAT_GLOBAL_LEVEL))
				activeChar.sendMessage("Your level must be more than " + Config.CHAT_GLOBAL_LEVEL + " to use Global Chat!");
			return;
		}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}