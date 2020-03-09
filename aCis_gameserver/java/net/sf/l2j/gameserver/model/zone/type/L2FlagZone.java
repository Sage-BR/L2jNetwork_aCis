package net.sf.l2j.gameserver.model.zone.type;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author SweeTs
 */
public class L2FlagZone extends L2ZoneType
{
	L2Skill noblesse = SkillTable.getInstance().getInfo(1323, 1);
	
	public L2FlagZone(int id)
	{
		super(id);
		loadConfigs();
	}
	
	static int radius, respawn;
	static int[][] spawn_loc;
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) character;
			
			character.setInsideZone(ZoneId.FLAG, true);
			noblesse.getEffects(player, player);
			player.updatePvPFlag(1);
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) character;
			
			character.setInsideZone(ZoneId.FLAG, false);
			player.updatePvPFlag(0);
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			final L2PcInstance activeChar = ((L2PcInstance) character);
			activeChar.sendPacket(new ExShowScreenMessage(+ respawn + " seconds until auto respawn", 4000, 2, true));
			
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					activeChar.doRevive();
					int[] loc = spawn_loc[Rnd.get(spawn_loc.length)];
					activeChar.teleToLocation(loc[0] + Rnd.get(-radius, radius), loc[1] + Rnd.get(-radius, radius), loc[2], 0);
				}
			}, 1000 * respawn);
		}
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) character;
			noblesse.getEffects(player, player);
			
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentMp(player.getMaxMp());
		}
	}
	
	private static void loadConfigs()
	{
		try
		{
			Properties prop = new Properties();
			prop.load(new FileInputStream(new File("./config/customs/flagzone.properties")));
			spawn_loc = parseItemsList(prop.getProperty("SpawnLoc", "82273,148068,-3469"));
			radius = Integer.parseInt(prop.getProperty("RespawnRadius", "500"));
			respawn = Integer.parseInt(prop.getProperty("RespawnDelay", "5"));
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static int[][] parseItemsList(String line)
	{
		final String[] propertySplit = line.split(";");
		if (propertySplit.length == 0)
			return null;
		
		int i = 0;
		String[] valueSplit;
		final int[][] result = new int[propertySplit.length][];
		for (String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 3)
				return null;
			
			result[i] = new int[3];
			try
			{
				result[i][0] = Integer.parseInt(valueSplit[0]);
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			
			try
			{
				result[i][1] = Integer.parseInt(valueSplit[1]);
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			
			try
			{
				result[i][2] = Integer.parseInt(valueSplit[2]);
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			i++;
		}
		return result;
	}
}