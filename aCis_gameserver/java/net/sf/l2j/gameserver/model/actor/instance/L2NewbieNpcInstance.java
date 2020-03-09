package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Baggos
 */
public class L2NewbieNpcInstance extends L2NpcInstance
{
	public L2NewbieNpcInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		
		if (player == null)
			return;
		
		if (!Config.ALLOW_CLASS_MASTERS)
			return;
		
		if (command.equalsIgnoreCase("change"))
		{
			String filename = "data/html/mods/NewbieNpc/changeclass.htm";
			
			if (Config.ALLOW_CLASS_MASTERS)
				filename = "data/html/mods/NewbieNpc/changeclass.htm";
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		if (command.startsWith("1stClass"))
			L2ClassMasterInstance.showHtmlMenu(player, getObjectId(), 1);
		else if (command.startsWith("2ndClass"))
			L2ClassMasterInstance.showHtmlMenu(player, getObjectId(), 2);
		else if (command.startsWith("3rdClass"))
			L2ClassMasterInstance.showHtmlMenu(player, getObjectId(), 3);
		else if (command.startsWith("change_class"))
		{
			int val = Integer.parseInt(command.substring(13));
			
			if (L2ClassMasterInstance.checkAndChangeClass(player, val))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/classmaster/ok.htm");
				html.replace("%name%", CharTemplateTable.getInstance().getClassNameById(val));
				player.sendPacket(html);
			}
		}
		else if (command.equalsIgnoreCase("LevelUp") && player.getLevel() == 1)
			player.addExpAndSp(Experience.LEVEL[Config.NEWBIE_LVL], 0);
		
		else if (command.equalsIgnoreCase("items"))
		{
			final ClassId currentClassId = player.getClassId();
			if (currentClassId.level() < 3)
			{
				player.sendMessage("First complete your Third Class!");
				return;
			}
			if (player.getSp() >= 1)
			{
				player.sendMessage("You already took Items!");
				return;
			}
			ClassId classes = player.getClassId();
			switch (classes)
			{
				case adventurer:
				case sagittarius:
				case duelist:
				case titan:
				case grandKhauatari:
				case phoenixKnight:
				case moonlightSentinel:
				case fortuneSeeker:
				case maestro:
				case dreadnought:
				case hellKnight:
				case evaTemplar:
				case swordMuse:
				case windRider:
				case shillienTemplar:
				case spectralDancer:
				case ghostHunter:
				case ghostSentinel:
				case soultaker:
				case mysticMuse:
				case archmage:
				case arcanaLord:
				case elementalMaster:
				case cardinal:
				case stormScreamer:
				case spectralMaster:
				case shillienSaint:
				case dominator:
				case doomcryer:
					NewbiesItems(player);
					player.addExpAndSp(Experience.LEVEL[0], 1);
					break;
			}
		}
		
		else if (command.equalsIgnoreCase("buffs"))
		{
			for (int id : (player.isMageClass() || player.getClassId() == ClassId.dominator || player.getClassId() == ClassId.doomcryer) ? Config.NEWBIE_MAGE_BUFFS : Config.NEWBIE_FIGHTER_BUFFS)
			{
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentMp(player.getMaxMp());
				L2Skill buff = SkillTable.getInstance().getInfo(id, SkillTable.getInstance().getMaxLevel(id));
				buff.getEffects(player, player);
				player.broadcastPacket(new MagicSkillUse(player, player, id, buff.getLevel(), 0, 0));
			}
		}
		else if (command.equalsIgnoreCase("teleport"))
		{
			final ClassId currentClassId = player.getClassId();
			if (currentClassId.level() < 3)
			{
				player.sendMessage("You Can't Leave! Your Character Isin't Complete!");
				return;
			}
			player.teleToLocation(Config.SETX, Config.SETY, Config.SETZ, 0);
			player.sendPacket(new ExShowScreenMessage("Your character is ready for our world!", 4000, 2, true));
		}
	}
	
	/**
	 * @param player
	 */
	private static void NewbiesItems(L2PcInstance player)
	{
		final int[] DaggerArmors =
		{
			6590,
			6379,
			6380,
			6381,
			6382,
			920,
			893,
			858,
			862,
			889
		};
		final int[] ArcherArmors =
		{
			7577,
			6379,
			6380,
			6381,
			6382,
			920,
			893,
			858,
			862,
			889
		};
		final int[] MageArmors =
		{
			6608,
			2407,
			5767,
			5779,
			512,
			920,
			893,
			858,
			862,
			889
		};
		final int[] DuelistArmor =
		{
			6580,
			6373,
			6374,
			6375,
			6376,
			6378,
			920,
			893,
			858,
			862,
			889
		};
		final int[] TitanArmor =
		{
			6605,
			6373,
			6374,
			6375,
			6376,
			6378,
			920,
			893,
			858,
			862,
			889
		};
		final int[] GrandKhaArmors =
		{
			6604,
			6379,
			6380,
			6381,
			6382,
			920,
			893,
			858,
			862,
			889
		};
		final int[] TankArmors =
		{
			6581,
			6373,
			6374,
			6375,
			6376,
			6377,
			6378,
			920,
			893,
			858,
			862,
			889
		};
		final int[] DwarfArmors =
		{
			6585,
			6373,
			6374,
			6375,
			6376,
			6377,
			6378,
			920,
			893,
			858,
			862,
			889
		};
		final int[] DreadArmors =
		{
			6601,
			6373,
			6374,
			6375,
			6376,
			6378,
			920,
			893,
			858,
			862,
			889
		};
		final int[] DancerArmors =
		{
			6580,
			6379,
			6380,
			6381,
			6382,
			920,
			893,
			858,
			862,
			889
		};
		ItemInstance items = null;
		ClassId classes = player.getClassId();
		switch (classes)
		{
			case adventurer:
			case windRider:
			case ghostHunter:
				for (int id : DaggerArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case sagittarius:
			case ghostSentinel:
			case moonlightSentinel:
				for (int id : ArcherArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case soultaker:
			case mysticMuse:
			case archmage:
			case arcanaLord:
			case elementalMaster:
			case cardinal:
			case stormScreamer:
			case spectralMaster:
			case shillienSaint:
			case dominator:
			case doomcryer:
				for (int id : MageArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case duelist:
				for (int id : DuelistArmor)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case titan:
				for (int id : TitanArmor)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case grandKhauatari:
				for (int id : GrandKhaArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case phoenixKnight:
			case hellKnight:
			case evaTemplar:
			case shillienTemplar:
				for (int id : TankArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case fortuneSeeker:
			case maestro:
				for (int id : DwarfArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case dreadnought:
				for (int id : DreadArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
			case spectralDancer:
			case swordMuse:
				for (int id : DancerArmors)
				{
					player.getInventory().addItem("Armors", id, 1, player, null);
					items = player.getInventory().getItemByItemId(id);
					player.getInventory().equipItemAndRecord(items);
					player.getInventory().reloadEquippedItems();
					player.broadcastCharInfo();
					new InventoryUpdate();
				}
				break;
		}
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/mods/newbieNpc/" + filename + ".htm";
	}
}