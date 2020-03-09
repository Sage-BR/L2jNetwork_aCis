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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.events.TvTEvent;
import net.sf.l2j.gameserver.events.TvTEventTeleport;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.GMAudit;

/**
 * @author FBIagent The class handles administrator commands for the TvT Engine which was first implemented by FBIagent
 */
public class AdminTvTEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_tvt_add",
		"admin_tvt_remove"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance adminInstance)
	{
		
		GMAudit.auditGMAction(adminInstance.getName(), command, (adminInstance.getTarget() != null ? adminInstance.getTarget().getName() : "no-target"), "");
		
		if (command.equals("admin_tvt_add"))
		{
			L2Object target = adminInstance.getTarget();
			
			if (target == null || !(target instanceof L2PcInstance))
			{
				adminInstance.sendMessage("You should select a player!");
				return true;
			}
			
			add(adminInstance, (L2PcInstance) target);
		}
		else if (command.equals("admin_tvt_remove"))
		{
			L2Object target = adminInstance.getTarget();
			
			if (target == null || !(target instanceof L2PcInstance))
			{
				adminInstance.sendMessage("You should select a player!");
				return true;
			}
			
			remove(adminInstance, (L2PcInstance) target);
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void add(L2PcInstance adminInstance, L2PcInstance playerInstance)
	{
		if (TvTEvent.isPlayerParticipant(playerInstance.getName()))
		{
			adminInstance.sendMessage("Player already participated in the event!");
			return;
		}
		
		if (!TvTEvent.addParticipant(playerInstance))
		{
			adminInstance.sendMessage("Player instance could not be added, it seems to be null!");
			return;
		}
		
		if (TvTEvent.isStarted())
			// we don't need to check return value of TvTEvent.getParticipantTeamCoordinates() for null, TvTEvent.addParticipant() returned true so target is in event
			new TvTEventTeleport(playerInstance, TvTEvent.getParticipantTeamCoordinates(playerInstance.getName()), true, false);
	}
	
	private static void remove(L2PcInstance adminInstance, L2PcInstance playerInstance)
	{
		if (!TvTEvent.removeParticipant(playerInstance.getName()))
		{
			adminInstance.sendMessage("Player is not part of the event!");
			return;
		}
		
		new TvTEventTeleport(playerInstance, Config.TVT_EVENT_BACK_COORDINATES, true, true);
	}
}
