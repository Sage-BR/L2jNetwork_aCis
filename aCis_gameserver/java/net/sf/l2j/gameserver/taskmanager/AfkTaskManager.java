package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Turns off afk status of {@link L2Character} after PERIOD Ms.
 * @author SweeTs
 */
public final class AfkTaskManager implements Runnable
{
	private static final long ALLOWED_AFK_PERIOD = TimeUnit.HOURS.toMillis(1); // 1h
	
	private final Map<L2PcInstance, Long> _players = new ConcurrentHashMap<>();
	
	public static final AfkTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected AfkTaskManager()
	{
		// Run task each second.
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
	}
	
	/**
	 * Adds {@link L2PcInstance} to the AfkTask.
	 * @param player : L2PcInstance to be added and checked.
	 */
	public final void add(L2PcInstance player)
	{
		_players.put(player, System.currentTimeMillis() + ALLOWED_AFK_PERIOD);
	}
	
	/**
	 * Removes {@link L2Character} from the AfkTask.
	 * @param character : {@link L2Character} to be removed.
	 */
	public final void remove(L2Character character)
	{
		_players.remove(character);
	}
	
	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_players.isEmpty())
			return;
		
		// Get current time.
		final long time = System.currentTimeMillis();
		
		// Loop all characters.
		for (Map.Entry<L2PcInstance, Long> entry : _players.entrySet())
		{
			// Time hasn't passed yet, skip.
			if (time < entry.getValue())
				continue;
			
			// Get character.
			final L2PcInstance player = entry.getKey();
			
			player.setAfking(true);
			
			_players.remove(player);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AfkTaskManager _instance = new AfkTaskManager();
	}
}