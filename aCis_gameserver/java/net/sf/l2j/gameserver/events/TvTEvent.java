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
package net.sf.l2j.gameserver.events;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Baggos
 */
public class TvTEvent
{
	enum EventState
	{
		INACTIVE,
		INACTIVATING,
		PARTICIPATING,
		STARTING,
		STARTED,
		REWARDING
	}
	
	/** Gives Noblesse to players */
	static L2Skill noblesse = SkillTable.getInstance().getInfo(1323, 1);
	/**
	 * The teams of the TvTEvent<br>
	 */
	public static TvTEventTeams[] _teams = new TvTEventTeams[2]; // event only allow max 2 teams
	private static L2Spawn _npcSpawn = null;
	/**
	 * The state of the TvTEvent<br>
	 */
	public static EventState _state = EventState.INACTIVE;
	
	/**
	 * No instance of this class!<br>
	 */
	private TvTEvent()
	{
	}
	
	/**
	 * Teams initializing<br>
	 */
	public static void init()
	{
		_teams[0] = new TvTEventTeams(Config.TVT_EVENT_TEAM_1_NAME, Config.TVT_EVENT_TEAM_1_COORDINATES);
		_teams[1] = new TvTEventTeams(Config.TVT_EVENT_TEAM_2_NAME, Config.TVT_EVENT_TEAM_2_COORDINATES);
	}
	
	/**
	 * Starts the participation of the TvTEvent<br>
	 * 1. Get NpcTemplate by Config.TVT_EVENT_PARTICIPATION_NPC_ID<br>
	 * 2. Try to spawn a new npc of it<br>
	 * <br>
	 * @return boolean<br>
	 */
	public static boolean startParticipation()
	{
		NpcTemplate tmpl = NpcTable.getInstance().getTemplate(Config.TVT_EVENT_PARTICIPATION_NPC_ID);
		
		if (tmpl == null)
		{
			System.out.println("TvTEventEngine[TvTEvent.startParticipation()]: L2NpcTemplate is a NullPointer -> Invalid npc id in configs?");
			return false;
		}
		
		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			
			_npcSpawn.setLocx(Config.TVT_EVENT_BACK_COORDINATES[0]);
			_npcSpawn.setLocy(Config.TVT_EVENT_BACK_COORDINATES[1]);
			_npcSpawn.setLocz(Config.TVT_EVENT_BACK_COORDINATES[2]);
			_npcSpawn.getAmount();
			_npcSpawn.getHeading();
			_npcSpawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			System.out.println("TvTEventEngine[TvTEvent.startParticipation()]: exception: " + e);
			return false;
		}
		
		setState(EventState.PARTICIPATING);
		return true;
	}
	
	/**
	 * Unspawn event npc.
	 */
	public static void unspawnEventNpc()
	{
		if (_npcSpawn == null || _npcSpawn.getLastSpawn() == null)
			return;
		
		_npcSpawn.getLastSpawn().deleteMe();
		_npcSpawn.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
	}
	
	/**
	 * Starts the TvTEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in configs<br>
	 * 3. Abort if not enought participants(return false)<br>
	 * 4. Set state EventState.STARTED<br>
	 * 5. Teleport all participants to team spot<br>
	 * <br>
	 * @return boolean<br>
	 */
	public static boolean startFight()
	{
		setState(EventState.STARTING);
		
		// not enought participants
		if (_teams[0].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS || _teams[1].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS)
		{
			setState(EventState.INACTIVE);
			unspawnEventNpc();
			_teams[0].cleanMe();
			_teams[1].cleanMe();
			return false;
		}
		
		closeDoors();
		setState(EventState.STARTED); // set state to STARTED here, so TvTEventTeleporter know to teleport to team spot
		
		// teleport all participants to there team spot
		for (TvTEventTeams team : _teams)
		{
			for (String playerName : team.getParticipatedPlayerNames())
			{
				L2PcInstance playerInstance = team.getParticipatedPlayers().get(playerName);
				
				if (playerInstance == null)
					continue;
				
				// leave party
				playerInstance.leaveParty();
				
				// Get Noblesse effect
				noblesse.getEffects(playerInstance, playerInstance);
				
				// implements Runnable and starts itself in constructor
				new TvTEventTeleport(playerInstance, team.getCoordinates(), false, false);
			}
		}
		
		return true;
	}
	
	/**
	 * Calculates the TvTEvent reward<br>
	 * 1. If both teams are at a tie(points equals), send it as system message to all participants, if one of the teams have 0 participants left online abort rewarding<br>
	 * 2. Wait till teams are not at a tie anymore<br>
	 * 3. Set state EvcentState.REWARDING<br>
	 * 4. Reward team with more points<br>
	 * 5. Show win html to wining team participants<br>
	 * <br>
	 * @return String<br>
	 */
	public static String calculateRewards()
	{
		if (_teams[0].getPoints() == _teams[1].getPoints())
		{
			if (_teams[0].getParticipatedPlayerCount() == 0 || _teams[1].getParticipatedPlayerCount() == 0)
			{
				// the fight cannot be completed
				setState(EventState.REWARDING);
				return "TvT Event: Event finish. No team won, cause of inactivity!";
			}
			
			sysMsgToAllParticipants("TvT Event: Both teams are at a tie, next team to get a kill wins!");
		}
		
		while (_teams[0].getPoints() == _teams[1].getPoints())
		{
			waiter(1);
		}
		
		setState(EventState.REWARDING); // after state REWARDING is set, nobody can point anymore
		
		byte teamId = (byte) (_teams[0].getPoints() > _teams[1].getPoints() ? 0 : 1); // which team wins?
		TvTEventTeams team = _teams[teamId];
		
		for (String playerName : team.getParticipatedPlayerNames())
		{
			for (int[] reward : Config.TVT_EVENT_REWARDS)
			{
				if (team.getParticipatedPlayers().get(playerName) == null)
					continue;
				
				PcInventory inv = team.getParticipatedPlayers().get(playerName).getInventory();
				
				if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
					inv.addItem("TvT Event", reward[0], reward[1], team.getParticipatedPlayers().get(playerName), team.getParticipatedPlayers().get(playerName));
				else
				{
					for (int i = 0; i < reward[1]; i++)
						inv.addItem("TvT Event", reward[0], 1, team.getParticipatedPlayers().get(playerName), team.getParticipatedPlayers().get(playerName));
				}
				
				SystemMessage systemMessage = null;
				
				if (reward[1] > 1)
				{
					systemMessage = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
					systemMessage.addItemName(reward[0]);
					systemMessage.addNumber(reward[1]);
				}
				else
				{
					systemMessage = new SystemMessage(SystemMessageId.EARNED_ITEM_S1);
					systemMessage.addItemName(reward[0]);
				}
				
				team.getParticipatedPlayers().get(playerName).sendPacket(systemMessage);
			}
			
			StatusUpdate statusUpdate = new StatusUpdate(team.getParticipatedPlayers().get(playerName));
			
			statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, team.getParticipatedPlayers().get(playerName).getCurrentLoad());
			team.getParticipatedPlayers().get(playerName).sendPacket(statusUpdate);
			
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			
			npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>Your team won the event. Look in your inventory, there should be your reward.</body></html>");
			team.getParticipatedPlayers().get(playerName).sendPacket(npcHtmlMessage);
		}
		
		return "TvT Event: Event finish. Team " + team.getName() + " won with " + team.getPoints() + " kills.";
	}
	
	/**
	 * Stops the TvTEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove tvt npc from world<br>
	 * 3. Open doors specified in configs<br>
	 * 4. Teleport all participants back to participation npc location<br>
	 * 5. Teams cleaning<br>
	 * 6. Set state EventState.INACTIVE<br>
	 */
	public static void stopFight()
	{
		setState(EventState.INACTIVATING);
		openDoors();
		unspawnEventNpc();
		
		for (TvTEventTeams team : _teams)
		{
			for (String playerName : team.getParticipatedPlayerNames())
			{
				L2PcInstance playerInstance = team.getParticipatedPlayers().get(playerName);
				
				if (playerInstance == null)
					continue;
				
				new TvTEventTeleport(playerInstance, Config.TVT_EVENT_BACK_COORDINATES, false, false);
			}
		}
		
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		
		setState(EventState.INACTIVE);
		
	}
	
	/**
	 * Adds a player to a TvTEvent team<br>
	 * 1. Calculate the id of the team in which the player should be added<br>
	 * 2. Add the player to the calculated team
	 * @param playerInstance
	 * @return boolean
	 */
	public static synchronized boolean addParticipant(L2PcInstance playerInstance)
	{
		if (playerInstance == null)
			return false;
		
		byte teamId = 0;
		
		if (_teams[0].getParticipatedPlayerCount() == _teams[1].getParticipatedPlayerCount())
			teamId = (byte) (Rnd.get(2));
		else
			teamId = (byte) (_teams[0].getParticipatedPlayerCount() > _teams[1].getParticipatedPlayerCount() ? 1 : 0);
		
		return _teams[teamId].addPlayer(playerInstance);
	}
	
	/**
	 * Removes a TvTEvent player from it's team<br>
	 * 1. Get team id of the player<br>
	 * 2. Remove player from it's team
	 * @param playerName
	 * @return boolean
	 */
	public static boolean removeParticipant(String playerName)
	{
		byte teamId = getParticipantTeamId(playerName);
		
		if (teamId == -1)
			return false;
		
		_teams[teamId].removePlayer(playerName);
		return true;
	}
	
	/**
	 * Send a SystemMessage to all participated players<br>
	 * 1. Send the message to all players of team number one<br>
	 * 2. Send the message to all players of team number two
	 * @param message
	 */
	public static void sysMsgToAllParticipants(String message)
	{
		for (L2PcInstance playerInstance : _teams[0].getParticipatedPlayers().values())
		{
			if (playerInstance != null)
				playerInstance.sendMessage(message);
		}
		
		for (L2PcInstance playerInstance : _teams[1].getParticipatedPlayers().values())
		{
			if (playerInstance != null)
				playerInstance.sendMessage(message);
		}
	}
	
	/**
	 * Close doors specified in configs
	 */
	public static void closeDoors()
	{
		for (int doorId : Config.TVT_EVENT_DOOR_IDS)
		{
			L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);
			
			if (doorInstance != null)
				doorInstance.closeMe();
		}
	}
	
	/**
	 * Open doors specified in configs
	 */
	public static void openDoors()
	{
		for (int doorId : Config.TVT_EVENT_DOOR_IDS)
		{
			L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);
			
			if (doorInstance != null)
				doorInstance.openMe();
		}
	}
	
	public static void waiter(int seconds)
	{
		try
		{
			Thread.sleep(seconds * 1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Called when a player logs in
	 * @param playerInstance
	 * @param player
	 */
	public static void onLogin(L2PcInstance playerInstance, L2PcInstance player)
	{
		if (playerInstance == null || (!isStarting() && !isStarted()))
			return;
		
		byte teamId = getParticipantTeamId(playerInstance.getName());
		
		if (teamId == -1)
			return;
		
		_teams[teamId].addPlayer(playerInstance);
		new TvTEventTeleport(playerInstance, _teams[teamId].getCoordinates(), true, false);
	}
	
	/**
	 * Called when a player logs out
	 * @param playerInstance
	 * @param player
	 */
	public static void onLogout(L2PcInstance playerInstance, L2PcInstance player)
	{
		if (playerInstance == null || (!isStarting() && !isStarted()))
			return;
		
		removeParticipant(playerInstance.getName());
	}
	
	/**
	 * Called on every bypass by npc of type L2TvTEventNpc<br>
	 * Needs synchronization cause of the max player check
	 * @param command
	 * @param playerInstance
	 */
	public static synchronized void onBypass(String command, L2PcInstance playerInstance)
	{
		if (playerInstance == null || !isParticipating())
			return;
		
		if (command.equals("tvt_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			int playerLevel = playerInstance.getLevel();
			
			if (playerInstance.isCursedWeaponEquipped())
				npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>Cursed weapon owners are not allowed to participate.</body></html>");
			else if (OlympiadManager.getInstance().isRegisteredInComp(playerInstance))
				npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>Olympiad participants can't register.</body></html>");
			else if (playerInstance.getKarma() > 0)
				npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>Chaotic players are not allowed to participate.</body></html>");
			else if (_teams[0].getParticipatedPlayerCount() >= Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS && _teams[1].getParticipatedPlayerCount() >= Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS)
				npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>Sorry the event is full!</body></html>");
			else if (playerLevel < Config.TVT_EVENT_MIN_LVL || playerLevel > Config.TVT_EVENT_MAX_LVL)
				npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>Only players from level " + Config.TVT_EVENT_MIN_LVL + " to level " + Config.TVT_EVENT_MAX_LVL + " are allowed tro participate.</body></html>");
			else if (_teams[0].getParticipatedPlayerCount() > 19 && _teams[1].getParticipatedPlayerCount() > 19)
				npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>The event is full! Maximum of " + Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS + "  player are allowed in one team.</body></html>");
			else if (addParticipant(playerInstance))
				npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>You are on the registration list now.</body></html>");
			else // addParticipant returned false cause playerInstance == null
				return;
			
			playerInstance.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("tvt_event_remove_participation"))
		{
			removeParticipant(playerInstance.getName());
			
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			
			npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>You are not longer on the registration list.</body></html>");
			playerInstance.sendPacket(npcHtmlMessage);
		}
	}
	
	/**
	 * Called on every onAction in L2PcIstance
	 * @param playerName
	 * @param targetPlayerName
	 * @return boolean
	 */
	public static boolean onAction(String playerName, String targetPlayerName)
	{
		if (!isStarted())
			return true;
		
		L2PcInstance playerInstance = L2World.getInstance().getPlayer(playerName);
		
		if (playerInstance == null)
			return false;
		
		if (playerInstance.isGM())
			return true;
		
		byte playerTeamId = getParticipantTeamId(playerName);
		byte targetPlayerTeamId = getParticipantTeamId(targetPlayerName);
		
		if ((playerTeamId != -1 && targetPlayerTeamId == -1) || (playerTeamId == -1 && targetPlayerTeamId != -1))
			return false;
		
		if (playerTeamId != -1 && targetPlayerTeamId != -1 && playerTeamId == targetPlayerTeamId && !Config.TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED)
			return false;
		
		return true;
	}
	
	/**
	 * Called on every summon item use
	 * @param playerName
	 * @return boolean
	 */
	public static boolean onItemSummon(String playerName)
	{
		if (!isStarted())
			return true;
		
		if (isPlayerParticipant(playerName) && !Config.TVT_EVENT_SUMMON_BY_ITEM_ALLOWED)
			return false;
		
		return true;
	}
	
	/**
	 * Is called when a player is killed
	 * @param killerCharacter
	 * @param killedPlayerInstance
	 */
	public static void onKill(L2Character killerCharacter, L2PcInstance killedPlayerInstance)
	{
		if (killerCharacter == null || killedPlayerInstance == null || (!(killerCharacter instanceof L2PcInstance) && !(killerCharacter instanceof L2PetInstance) && !(killerCharacter instanceof L2SummonInstance)) || !isStarted())
			return;
		
		L2PcInstance killerPlayerInstance = null;
		
		if (killerCharacter instanceof L2PetInstance || killerCharacter instanceof L2SummonInstance)
		{
			killerPlayerInstance = ((L2Summon) killerCharacter).getOwner();
			
			if (killerPlayerInstance == null)
				return;
		}
		else
			killerPlayerInstance = (L2PcInstance) killerCharacter;
		
		if (Config.TVT_KILLS_REWARD_ENABLED)
			for (int[] rewardKills : Config.TVT_KILLS_REWARD)
			{
				SystemMessage systemMessage = null;
				// Count the kill
				killerPlayerInstance._tvtkills++;
				switch (killerPlayerInstance._tvtkills)
				{
					case 5: // Reward after 5 kills without die
					case 8: // Reward after 8 kills without die
					case 12: // Reward after 12 kills without die
					case 15: // Reward after 15 kills without die
					case 20: // Reward after 20 kills without die
						
						systemMessage = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						systemMessage.addItemName(rewardKills[0]);
						systemMessage.addNumber(rewardKills[1]);
						
						killerPlayerInstance.getInventory().addItem("TvT Event", rewardKills[0], rewardKills[1], killerPlayerInstance, killerPlayerInstance);
						killerPlayerInstance.sendPacket(new CreatureSay(0, Say2.HERO_VOICE, "Amazing", +killerPlayerInstance._tvtkills + " kills without die. You has been rewarded!"));
						killerPlayerInstance.sendPacket(systemMessage);
						break;
				}
			}
		
		String playerName = killerPlayerInstance.getName();
		byte killerTeamId = getParticipantTeamId(playerName);
		
		playerName = killedPlayerInstance.getName();
		
		byte killedTeamId = getParticipantTeamId(playerName);
		
		if (killerTeamId != -1 && killedTeamId != -1 && killerTeamId != killedTeamId)
			_teams[killerTeamId].increasePoints();
		
		if (killedTeamId != -1)
			new TvTEventTeleport(killedPlayerInstance, _teams[killedTeamId].getCoordinates(), false, false);
	}
	
	/**
	 * Sets the TvTEvent state
	 * @param state
	 */
	private static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}
	
	/**
	 * Is TvTEvent inactive?
	 * @return boolean
	 */
	public static boolean isInactive()
	{
		boolean isInactive;
		
		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}
		
		return isInactive;
	}
	
	/**
	 * Is TvTEvent in inactivating?
	 * @return boolean
	 */
	public static boolean isInactivating()
	{
		boolean isInactivating;
		
		synchronized (_state)
		{
			isInactivating = _state == EventState.INACTIVATING;
		}
		
		return isInactivating;
	}
	
	/**
	 * Is TvTEvent in participation?
	 * @return boolean
	 */
	public static boolean isParticipating()
	{
		boolean isParticipating;
		
		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}
		
		return isParticipating;
	}
	
	/**
	 * Is TvTEvent starting?
	 * @return boolean
	 */
	public static boolean isStarting()
	{
		boolean isStarting;
		
		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}
		
		return isStarting;
	}
	
	/**
	 * Is TvTEvent started?
	 * @return boolean
	 */
	public static boolean isStarted()
	{
		boolean isStarted;
		
		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}
		
		return isStarted;
	}
	
	/**
	 * Is TvTEvent rewarding?
	 * @return boolean
	 */
	public static boolean isRewarding()
	{
		boolean isRewarding;
		
		synchronized (_state)
		{
			isRewarding = _state == EventState.REWARDING;
		}
		
		return isRewarding;
	}
	
	/**
	 * Returns the team id of a player, if player is not participant it returns -1
	 * @param playerName
	 * @return byte
	 */
	public static byte getParticipantTeamId(String playerName)
	{
		return (byte) (_teams[0].containsPlayer(playerName) ? 0 : (_teams[1].containsPlayer(playerName) ? 1 : -1));
	}
	
	/**
	 * Returns the team coordinates in which the player is in, if player is not in a team return null
	 * @param playerName
	 * @return int[]
	 */
	public static int[] getParticipantTeamCoordinates(String playerName)
	{
		return _teams[0].containsPlayer(playerName) ? _teams[0].getCoordinates() : (_teams[1].containsPlayer(playerName) ? _teams[1].getCoordinates() : null);
	}
	
	/**
	 * Is given player participant of the event?
	 * @param playerName
	 * @return boolean
	 */
	public static boolean isPlayerParticipant(String playerName)
	{
		return _teams[0].containsPlayer(playerName) || _teams[1].containsPlayer(playerName);
	}
	
	/**
	 * Returns participated player count<br>
	 * <br>
	 * @return int<br>
	 */
	public static int getParticipatedPlayersCount()
	{
		return _teams[0].getParticipatedPlayerCount() + _teams[1].getParticipatedPlayerCount();
	}
	
	/**
	 * Returns teams names<br>
	 * <br>
	 * @return String[]<br>
	 */
	public static String[] getTeamNames()
	{
		return new String[]
		{
			_teams[0].getName(),
			_teams[1].getName()
		};
	}
	
	/**
	 * Returns player count of both teams<br>
	 * <br>
	 * @return int[]<br>
	 */
	public static int[] getTeamsPlayerCounts()
	{
		return new int[]
		{
			_teams[0].getParticipatedPlayerCount(),
			_teams[1].getParticipatedPlayerCount()
		};
	}
	
	/**
	 * Returns points count of both teams
	 * @return int[]
	 */
	public static int[] getTeamsPoints()
	{
		return new int[]
		{
			_teams[0].getPoints(),
			_teams[1].getPoints()
		};
	}
}