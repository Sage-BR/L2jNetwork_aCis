package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author Baggos
 */
public class ManaPotion implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		Player activeChar = (Player) playable;
		
		long MaxMp = activeChar.getMaxMp();
		long CurrentMp = (int) activeChar.getCurrentMp();
		
		if (MaxMp == CurrentMp)
		{
			activeChar.sendMessage("Your MP is full.");
			return;
		}
		
		if (activeChar.isAfraid() || activeChar.isAlikeDead() || activeChar.isInOlympiadMode())
			return;
		
		int itemId = item.getItemId();
		if (itemId == 728)
		{
			if (!Config.INFINITY_MANAPOT)
			{
				activeChar.getInventory().destroyItemByItemId("ManaPot", itemId, 1, activeChar, null);
				activeChar.getInventory().updateDatabase();
				activeChar.sendPacket(new ItemList(activeChar, false));
			}
			
			MagicSkillUse u = new MagicSkillUse(activeChar, activeChar, 2005, 1, 500, 1000);
			Broadcast.toSelfAndKnownPlayersInRadius(activeChar, u, 1350);
			
			if (MaxMp - CurrentMp > Config.MAX_MP)
			{
				activeChar.setCurrentMp(CurrentMp + Config.MAX_MP);
				activeChar.broadcastStatusUpdate();
				CharInfo info1 = new CharInfo(activeChar);
				activeChar.broadcastPacket(info1);
				UserInfo info2 = new UserInfo(activeChar);
				activeChar.sendPacket(info2);
				activeChar.sendMessage("MP recovered by " + Config.MAX_MP + ".");
			}
			else
			{
				activeChar.setCurrentMp(MaxMp);
				activeChar.broadcastStatusUpdate();
				CharInfo info1 = new CharInfo(activeChar);
				activeChar.broadcastPacket(info1);
				UserInfo info2 = new UserInfo(activeChar);
				activeChar.sendPacket(info2);
				activeChar.sendMessage("MP recovered by " + (MaxMp - CurrentMp) + ".");
			}
		}
	}
	
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int[] ITEM_IDS =
	{
		728
	};
}
