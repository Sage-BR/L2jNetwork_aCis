/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.scripts.custom;

/**
 * @author Baggos
 *
 */
import java.sql.Connection;
import java.sql.PreparedStatement;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Services extends Quest
{
	private static final int NPC_ID = 50058;
	
	public Services()
	{
		super(-1, "custom");
		
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
			st = newQuestState(player);
		
		return OlympiadManager.getInstance().isRegistered(player) ? "Services-Blocked.htm" : "Services.htm";
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmlText = event;
		QuestState st = player.getQuestState(getName());
		
		if (event.equals("setNoble"))
		{
			if (!player.isNoble())
			{
				if (st.getQuestItemsCount(Config.NOBL_ID) >= Config.NOBL_AMOUNT)
				{
					st.takeItems(Config.NOBL_ID, Config.NOBL_AMOUNT);
					player.setNoble(true, true);
					player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 1, 0));
					htmlText = "NoblesseServices-Success.htm";
				}
				else
					htmlText = "NoblesseServices-NoItems.htm";
			}
			else
				htmlText = "NoblesseServices-AlredyNoble.htm";
		}
		else if (event.equals("changeGender"))
		{
			if (st.getQuestItemsCount(Config.GENDER_ID) >= Config.GENDER_AMOUNT)
			{
				st.takeItems(Config.GENDER_ID, Config.GENDER_AMOUNT);
				player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
				player.getAppearance().setSex(player.getAppearance().getSex());
				player.sendMessage("Your gender has been changed.");
				player.broadcastUserInfo();
				player.decayMe();
				player.spawnMe();
				player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 1, 0));
				htmlText = "ChangeGender-Success.htm";
			}
			else
				htmlText = "ChangeGender-NoItems.htm";
		}
		else if (event.startsWith("changeName"))
		{
			try
			{
				String newName = event.substring(11);
				
				if (st.getQuestItemsCount(Config.NAME_ID) >= Config.NAME_AMOUNT)
				{
					if (newName == null)
					{
						return "ChangeName.htm";
					}
					if (!newName.matches("^[a-zA-Z0-9]+$"))
					{
						player.sendMessage("Incorrect name. Please try again.");
						return "ChangeName.htm";
					}
					else if (newName.equals(player.getName()))
					{
						player.sendMessage("Please, choose a different name.");
						return "ChangeName.htm";
					}
					else if (CharNameTable.doesCharNameExist(newName))
					{
						player.sendMessage("The name " + newName + " already exists.");
						return "ChangeName.htm";
					}
					else
					{
						st.takeItems(Config.NAME_ID, Config.NAME_AMOUNT);
						player.setName(newName);
						player.store();
						player.sendMessage("Your new character name is " + newName);
						player.broadcastUserInfo();
						player.getClan().broadcastClanStatus();
						
						return "ChangeName-Success.htm";
					}
				}
				return "ChangeName-NoItems.htm";
			}
			catch (Exception e)
			{
				player.sendMessage("Please, insert a correct name.");
				return "ChangeName.htm";
			}
		}
		else if (event.startsWith("reducePks"))
		{
			try
			{
				final int amount = Integer.parseInt(event.substring(10));
				
				if (amount <= 0 || amount > player.getPkKills())
				{
					player.sendMessage("Incorrect value. Please try again.");
					return null;
				}
				
				if (st.getQuestItemsCount(Config.PK_ID) >= Config.PK_AMOUNT * amount)
				{
					st.takeItems(Config.PK_ID, Config.PK_AMOUNT * amount);
					player.setPkKills(player.getPkKills() - amount);
					player.sendMessage("You have successfuly cleaned " + amount + " PKs.");
					player.broadcastUserInfo();
					htmlText = "PkServices-Success.htm";
				}
				else
					htmlText = "PkServices-NoItems.htm";
			}
			catch (Exception e)
			{
				player.sendMessage("Incorrect value. Please try again.");
			}
		}
		else if (event.startsWith("changeClanName"))
		{
			if (player.isClanLeader())
			{
				try
				{
					String clanName = event.substring(15);
					
					if (st.getQuestItemsCount(Config.CLAN_NAME_ID) >= Config.CLAN_NAME_AMOUNT)
					{
						if (!StringUtil.isAlphaNumeric(clanName))
						{
							player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
							return null;
						}
						else if (clanName.length() < 2 || clanName.length() > 16)
						{
							player.sendPacket(SystemMessageId.CLAN_NAME_LENGTH_INCORRECT);
							return null;
						}
						else if (ClanTable.getInstance().getClanByName(clanName) != null)
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_EXISTS).addString(clanName));
							return null;
						}
						
						st.takeItems(Config.CLAN_NAME_ID, Config.CLAN_NAME_AMOUNT);
						player.getClan().setName(clanName);
						
						try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET clan_name=? WHERE clan_id=?"))
						{
							statement.setString(1, clanName);
							statement.setInt(2, player.getClan().getClanId());
							statement.execute();
						}
						catch (Exception e)
						{
							_log.info("Error updating clan name for player " + player.getName() + ". Error: " + e);
						}
						
						player.getClan().broadcastClanStatus();
						htmlText = "ChangeClanName-Success.htm";
					}
					else
						htmlText = "ChangeClanName-NoItems.htm";
				}
				catch (Exception e)
				{
					player.sendPacket(SystemMessageId.CLAN_NAME_INVALID);
				}
			}
			else
				htmlText = "ChangeClanName-NoLeader.htm";
		}
		
		return htmlText;
	}
}
