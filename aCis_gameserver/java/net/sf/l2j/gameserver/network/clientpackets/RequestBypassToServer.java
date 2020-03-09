package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.AioMenu;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Menu;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Repair;
import net.sf.l2j.gameserver.instancemanager.BotsPreventionManager;
import net.sf.l2j.gameserver.instancemanager.StartupManager;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.MultiNpc;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (_command.isEmpty())
			return;
		
		if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS))
			return;
		
		final Player player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (_command.startsWith("admin_"))
		{
			String command = _command.split(" ")[0];
			
			final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
			if (ach == null)
			{
				if (player.isGM())
					player.sendMessage("The command " + command.substring(6) + " doesn't exist.");
				LOGGER.warn("No handler registered for admin command '{}'.", command);
				return;
			}
			
			if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
			{
				player.sendMessage("You don't have the access rights to use this command.");
				LOGGER.warn("{} tried to use admin command '{}' without proper Access Level.", player.getName(), command);
				return;
			}
			
			if (Config.GMAUDIT)
				GMAUDIT_LOG.info(player.getName() + " [" + player.getObjectId() + "] used '" + _command + "' command on: " + ((player.getTarget() != null) ? player.getTarget().getName() : "none"));
			
			ach.useAdminCommand(_command, player);
		}
		else if (_command.startsWith("player_help "))
		{
			final String path = _command.substring(12);
			if (path.indexOf("..") != -1)
				return;
			
			final StringTokenizer st = new StringTokenizer(path);
			final String[] cmd = st.nextToken().split("#");
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/help/" + cmd[0]);
			if (cmd.length > 1)
				html.setItemId(Integer.parseInt(cmd[1]));
			html.disableValidation();
			player.sendPacket(html);
		}
		if (_command.startsWith("voiced_"))
		{
			String command = _command.split(" ")[0];
			
			IVoicedCommandHandler ach = VoicedCommandHandler.getInstance().getHandler(_command.substring(7));
			
			if (ach == null)
			{
				player.sendMessage("The command " + command.substring(7) + " does not exist!");
				LOGGER.warn("No handler registered for command '" + _command + "'");
				return;
			}
			
			ach.useVoicedCommand(_command.substring(7), player, null);
		}
		else if (_command.startsWith("voice_"))
		{
			String params = "";
			String command;
			if (_command.indexOf(" ") != -1)
			{
				command = _command.substring(6, _command.indexOf(" "));
				params = _command.substring(_command.indexOf(" ") + 1);
			}
			else
			{
				command = _command.substring(6);
			}
			
			IVoicedCommandHandler vc = VoicedCommandHandler.getInstance().getHandler(command);
			
			if (vc == null)
			{
				return;
			}
			vc.useVoicedCommand(command, player, params);
		}
		else if (_command.startsWith("aiopanel"))
		{
			String value = _command.substring(8);
			StringTokenizer st = new StringTokenizer(value);
			String command = st.nextToken();
			
			AioMenu.bypass(player, command, st);
		}
		else if (_command.startsWith("repairchar "))
		{
			String value = _command.substring(11);
			StringTokenizer st = new StringTokenizer(value);
			String repairChar = null;
			
			try
			{
				if (st.hasMoreTokens())
					repairChar = st.nextToken();
			}
			catch (Exception e)
			{
				player.sendMessage("You can't put empty box.");
				return;
			}
			
			if (repairChar == null || repairChar.equals(""))
				return;
			
			if (Repair.checkAcc(player, repairChar))
			{
				if (Repair.checkChar(player, repairChar))
				{
					String htmContent = HtmCache.getInstance().getHtm("data/html/mods/repair/repair-self.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
					npcHtmlMessage.setHtml(htmContent);
					player.sendPacket(npcHtmlMessage);
					return;
				}
				else if (Repair.checkPunish(player, repairChar))
				{
					String htmContent = HtmCache.getInstance().getHtm("data/html/mods/repair/repair-jail.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
					npcHtmlMessage.setHtml(htmContent);
					player.sendPacket(npcHtmlMessage);
					return;
				}
				else if (Repair.checkKarma(player, repairChar))
				{
					player.sendMessage("Selected Char has Karma,Cannot be repaired!");
					return;
				}
				else
				{
					Repair.repairBadCharacter(repairChar);
					String htmContent = HtmCache.getInstance().getHtm("data/html/mods/repair/repair-done.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
					npcHtmlMessage.setHtml(htmContent);
					player.sendPacket(npcHtmlMessage);
					return;
				}
			}
			
			String htmContent = HtmCache.getInstance().getHtm("data/html/mods/repair/repair-error.htm");
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);
			npcHtmlMessage.setHtml(htmContent);
			npcHtmlMessage.replace("%acc_chars%", Repair.getCharList(player));
			player.sendPacket(npcHtmlMessage);
		}
		
		if (OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
			return;
		}
		else if (_command.startsWith("submitpin"))
		{
			try
			{
				String value = _command.substring(9);
				StringTokenizer s = new StringTokenizer(value, " ");
				int _pin = player.getPin();
				
				try
				{
					if (player.getPincheck())
					{
						_pin = Integer.parseInt(s.nextToken());
						if (Integer.toString(_pin).length() < 5)
						{
							player.sendMessage("You should type more than 5 numbers. ");
							return;
						}
						
						try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE characters SET pin=? WHERE obj_id=?");)
						{
							statement.setInt(1, _pin);
							statement.setInt(2, player.getObjectId());
							statement.execute();
							statement.close();
							player.setPincheck(false);
							player.updatePincheck();
							player.sendMessage("You successfully secure your character. Your code is: " + _pin);
						}
						catch (Exception e)
						{
							e.printStackTrace();
							LOGGER.warn("could not set char first login:" + e);
						}
					}
				}
				catch (Exception e)
				{
					player.sendMessage("Your code must be more than 5 numbers.");
				}
			}
			catch (Exception e)
			{
				player.sendMessage("Your code must be more than 5 numbers.");
			}
			
		}
		else if (_command.startsWith("removepin"))
		{
			try
			{
				String value = _command.substring(9);
				StringTokenizer s = new StringTokenizer(value, " ");
				int dapin = 0;
				int pin = 0;
				dapin = Integer.parseInt(s.nextToken());
				
				PreparedStatement statement = null;
				
				try (Connection con = L2DatabaseFactory.getInstance().getConnection())
				{
					statement = con.prepareStatement("SELECT pin FROM characters WHERE obj_Id=?");
					statement.setInt(1, player.getObjectId());
					ResultSet rset = statement.executeQuery();
					
					while (rset.next())
					{
						pin = rset.getInt("pin");
					}
					
					if (pin == dapin)
					{
						player.setPincheck(true);
						player.setPin(0);
						player.updatePincheck();
						player.sendMessage("You successfully remove your pin.");
					}
					else
						player.sendMessage("Code is wrong..");
				}
				catch (Exception e)
				{
					
				}
			}
			catch (Exception e)
			{
				// e.printStackTrace();
				player.sendMessage("Your code must be more than 5 numbers.");
			}
		}
		else if (_command.startsWith("enterpin"))
		{
			try
			{
				String value = _command.substring(8);
				StringTokenizer s = new StringTokenizer(value, " ");
				int dapin = 0;
				int pin = 0;
				dapin = Integer.parseInt(s.nextToken());
				
				PreparedStatement statement = null;
				
				try (Connection con = L2DatabaseFactory.getInstance().getConnection())
				{
					statement = con.prepareStatement("SELECT pin FROM characters WHERE obj_Id=?");
					statement.setInt(1, player.getObjectId());
					ResultSet rset = statement.executeQuery();
					
					while (rset.next())
					{
						pin = rset.getInt("pin");
					}
					
					if (pin == dapin)
					{
						player.sendMessage("Code Authenticated!");
						player.setIsImmobilized(false);
						player.setIsSubmitingPin(false);
					}
					else
					{
						player.sendMessage("Code is wrong.. You will now get disconnected!");
						ThreadPool.schedule(() -> player.logout(false), 3000);
					}
				}
				catch (Exception e)
				{
				}
			}
			catch (Exception e)
			{
				// e.printStackTrace();
				player.sendMessage("Your code must be more than 5 numbers.");
			}
		}
		else if (_command.startsWith("page1"))
			Menu.mainHtml(player);
		else if (_command.startsWith("buffprot"))
		{
			if (player.isBuffProtected())
			{
				player.setIsBuffProtected(false);
				player.sendMessage("Buff protection is disabled.");
				Menu.mainHtml(player);
			}
			else
			{
				player.setIsBuffProtected(true);
				player.sendMessage("Buff protection is enabled.");
				Menu.mainHtml(player);
			}
		}
		else if (_command.startsWith("mageclass"))
			StartupManager.getInstance().MageClasses(_command, player);
		else if (_command.startsWith("fighterclass"))
			StartupManager.getInstance().FighterClasses(_command, player);
		else if (_command.startsWith("lightclass"))
			StartupManager.getInstance().LightClasses(_command, player);
		else if (_command.startsWith("class"))
			StartupManager.getInstance().Classes(_command, player);
		else if (_command.startsWith("base"))
			MultiNpc.Classes(_command, player);
		else if (_command.startsWith("report"))
			BotsPreventionManager.getInstance().AnalyseBypass(_command, player);
		else if (_command.startsWith("npc_"))
		{
			if (!player.validateBypass(_command))
				return;
			
			int endOfId = _command.indexOf('_', 5);
			String id;
			if (endOfId > 0)
				id = _command.substring(4, endOfId);
			else
				id = _command.substring(4);
			
			try
			{
				final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
				
				if (object != null && object instanceof Npc && endOfId > 0 && ((Npc) object).canInteract(player))
					((Npc) object).onBypassFeedback(player, _command.substring(endOfId + 1));
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			catch (NumberFormatException nfe)
			{
			}
		}
		// Navigate throught Manor windows
		else if (_command.startsWith("manor_menu_select?"))
		{
			WorldObject object = player.getTarget();
			if (object instanceof Npc)
				((Npc) object).onBypassFeedback(player, _command);
		}
		else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
		{
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		}
		else if (_command.startsWith("Quest "))
		{
			if (!player.validateBypass(_command))
				return;
			
			String[] str = _command.substring(6).trim().split(" ", 2);
			if (str.length == 1)
				player.processQuestEvent(str[0], "");
			else
				player.processQuestEvent(str[0], str[1]);
		}
		else if (_command.startsWith("_match"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = Hero.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				Hero.getInstance().showHeroFights(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("_diary"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = Hero.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				Hero.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("arenachange")) // change
		{
			final boolean isManager = player.getCurrentFolk() instanceof OlympiadManagerNpc;
			if (!isManager)
			{
				// Without npc, command can be used only in observer mode on arena
				if (!player.inObserverMode() || player.isInOlympiadMode() || player.getOlympiadGameId() < 0)
					return;
			}
			
			final int arenaId = Integer.parseInt(_command.substring(12).trim());
			player.enterOlympiadObserverMode(arenaId);
		}
	}
}