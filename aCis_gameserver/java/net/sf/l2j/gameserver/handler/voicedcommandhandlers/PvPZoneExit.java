package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Baggos
 */
public class PvPZoneExit implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
		"exit"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equals("exit") && activeChar.isInsideZone(ZoneId.FLAG))
		{
			if (activeChar.isInCombat())
				activeChar.sendMessage("You cannot leave while you are in combat!");
			activeChar.getAI().setIntention(CtrlIntention.IDLE);
			activeChar.doCast(SkillTable.getInstance().getInfo(2100, 1));
			activeChar.sendPacket(new ExShowScreenMessage("You will be teleported in 3 seconds", 3000, 2, true));
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}
