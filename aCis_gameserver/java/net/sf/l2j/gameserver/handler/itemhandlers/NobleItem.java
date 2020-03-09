package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

/**
 * @author Baggos
 */
public class NobleItem implements IItemHandler
{
	
	public NobleItem()
	{
	}
	
	@Override
	public void useItem(L2Playable playable, ItemInstance item, boolean forceUse)
	{
		if (Config.NOBLE_ITEM)
		{
			if (!(playable instanceof L2PcInstance))
				return;
			L2PcInstance activeChar = (L2PcInstance) playable;
			if (activeChar.isNoble())
			{
				activeChar.sendMessage("You Are Already A Noblesse!.");
			}
			else
			{
				activeChar.broadcastPacket(new SocialAction(activeChar, 16));
				activeChar.setNoble(true, true);
				activeChar.sendMessage("You Are Now a Noble,You Are Granted With Noblesse Status , And Noblesse Skills.");
				activeChar.broadcastUserInfo();
				playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
			}
		}
	}
	
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int ITEM_IDS[] =
	{
		Config.NOBLE_ITEM_ID
	};
	
}