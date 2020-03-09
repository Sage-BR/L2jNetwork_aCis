package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Menu implements IVoicedCommandHandler
{
	private final String[] _voicedCommands =
	{
		"menu"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		mainHtml(activeChar);
		return true;
	}
	
	public static void mainHtml(Player activeChar)
	{
		NpcHtmlMessage nhm = new NpcHtmlMessage(5);
		final StringBuilder tb = new StringBuilder();
		
		tb.append("<html><head><title>Personal Menu</title></head><body>");
		tb.append("<center>");
		tb.append("<br>");
		tb.append("<td valign=\"top\">Players online <font color=\"00FF00\"> " + (World.getInstance().getPlayers().size()) + "</font>");
		tb.append("<table border=\"1\" width=\"100\" height=\"12\" bgcolor=\"\">");
		tb.append("<tr>");
		tb.append("<td width=\"52\">ON</td>");
		tb.append("<td width=\"16\"><button width=16 height=12 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		tb.append("<td><button width=32 height=12 back=\"L2UI_CH3.tutorial_pointer1\" fore=\"L2UI_CH3.tutorial_pointer1\"></td>");
		tb.append("</tr>");
		tb.append("<tr>");
		tb.append("<td width=\"52\">OFF</td>");
		tb.append("<td width=\"16\"><button width=16 height=12 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("<td><button width=32 height=12 back=\"L2UI_CH3.tutorial_pointer1\" fore=\"L2UI_CH3.tutorial_pointer1\"></td>");
		tb.append("</tr>");
		tb.append("</table><br>");
		tb.append("<table border=\"1\" width=\"280\" height=\"20\" bgcolor=\"\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\" width=\"52\">Buff Protection System</td>");
		if (activeChar.isBuffProtected())
			tb.append("<td width=\"16\"><button action=\"bypass -h buffprot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.isBuffProtected())
			tb.append("<td width=\"16\"><button action=\"bypass -h buffprot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<table border=\"1\" width=\"280\" height=\"20\" bgcolor=\"\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\" width=\"52\">Personal Message Refusal</td>");
		if (activeChar.isInRefusalMode())
			tb.append("<td width=\"16\"><button action=\"bypass -h pmref\" width=30 height=18 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.isInRefusalMode())
			tb.append("<td width=\"16\"><button action=\"bypass -h pmref\" width=30 height=18 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<table border=\"1\" width=\"280\" height=\"20\" bgcolor=\"\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\" width=\"52\">Trade Request Protection</td>");
		if (activeChar.getTradeRefusal())
			tb.append("<td width=\"16\"><button action=\"bypass -h tradeprot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.getTradeRefusal())
			tb.append("<td width=\"16\"><button action=\"bypass -h tradeprot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<table border=\"1\" width=\"280\" height=\"20\" bgcolor=\"\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\" width=\"52\">Soulshot/Spiritshot Effect</td>");
		if (activeChar.isSSDisabled())
			tb.append("<td width=\"16\"><button action=\"bypass -h ssprot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.isSSDisabled())
			tb.append("<td width=\"16\"><button action=\"bypass -h ssprot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("</tr>");
		tb.append("<table border=\"1\" width=\"280\" height=\"20\" bgcolor=\"\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\" width=\"52\">Party Invite Protection</td>");
		if (activeChar.isPartyInRefuse())
			tb.append("<td width=\"16\"><button action=\"bypass -h partyin\" width=30 height=18 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.isPartyInRefuse())
			tb.append("<td width=\"16\"><button action=\"bypass -h partyin\" width=30 height=18 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<table border=\"1\" width=\"280\" height=\"20\" bgcolor=\"\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\" width=\"52\">Experience Gain Protection</td>");
		if (activeChar.cantGainXP())
			tb.append("<td width=\"16\"><button action=\"bypass -h xpnot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.cantGainXP())
			tb.append("<td width=\"16\"><button action=\"bypass -h xpnot\" width=30 height=18 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("</tr>");
		tb.append("</table>");		
		tb.append("<br>");
		tb.append("<img src=L2UI_CH3.herotower_deco width=256 height=32>");
		tb.append("</body>");
		tb.append("</center>");
		tb.append("</html>");
		
		nhm.setHtml(tb.toString());
		activeChar.sendPacket(nhm);
	}
	
	public static void mainHtml2(Player activeChar)
	{
		NpcHtmlMessage nhm = new NpcHtmlMessage(5);
		final StringBuilder tb = new StringBuilder();
		
		tb.append("<html><head><title>Personal Menu</title></head><body>");
		tb.append("<center>");
		tb.append("<table width=\"250\" cellpadding=\"5\" bgcolor=\"000000\">");
		tb.append("<tr>");
		tb.append("<td width=\"45\" valign=\"top\" align=\"center\"><img src=\"L2ui_ch3.menubutton4\" width=\"38\" height=\"38\"></td>");
		tb.append("<td valign=\"top\">Players online <font color=\"FF6600\"> " + (World.getInstance().getPlayers().size()) + "</font>");
		tb.append("<br1><font color=\"00FF00\">" + activeChar.getName() + "</font>, use this menu for everything related to your gameplay.<br1></td>");
    	tb.append("</tr>");
	    tb.append("</table>");
	    tb.append("</center>");
	    tb.append("<center>");
	    tb.append("<table border=\"1\" width=\"100\" height=\"12\" bgcolor=\"000000\">");
		tb.append("<tr>");
	    tb.append("<td width=\"52\">ON</td>");
		tb.append("<td width=\"16\"><button width=16 height=12 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		tb.append("<td><button width=32 height=12 back=\"L2UI_CH3.tutorial_pointer1\" fore=\"L2UI_CH3.tutorial_pointer1\"></td>");
	    tb.append("</tr>");
		tb.append("<tr>");
	    tb.append("<td width=\"52\">OFF</td>");
	    tb.append("<td width=\"16\"><button width=16 height=12 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
	    tb.append("<td><button width=32 height=12 back=\"L2UI_CH3.tutorial_pointer1\" fore=\"L2UI_CH3.tutorial_pointer1\"></td>");
		tb.append("</tr>");
	    tb.append("</table><br>");
	    tb.append("<table border=\"1\" width=\"250\" height=\"12\" bgcolor=\"000000\">");
       	tb.append("<tr>");
	    tb.append("<td align=\"center\" width=\"52\">Party Invite Protection</td>");
	if (activeChar.isPartyInRefuse())
		tb.append("<td width=\"16\"><button action=\"bypass -h partyin\" width=24 height=12 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.isPartyInRefuse())
		tb.append("<td width=\"16\"><button action=\"bypass -h partyin\" width=24 height=12 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
		tb.append("</tr>");
		tb.append("<tr><td width=\"250\"><font color=\"00FF00\">By enabling that you won't be able to recieve ANY party invite from another character.</font></td></tr>");
		tb.append("</table>");
		tb.append("<table border=\"1\" width=\"250\" height=\"12\" bgcolor=\"000000\">");
		tb.append("<tr>");
		tb.append("<td align=\"center\" width=\"52\">Exp Gain Protection</td>");
	if (activeChar.cantGainXP())
			tb.append("<td width=\"16\"><button action=\"bypass -h xpnot\" width=24 height=12 back=\"L2UI_CH3.br_bar1_hp\" fore=\"L2UI_CH3.br_bar1_hp\"></td>");
		if (!activeChar.cantGainXP())
		tb.append("<td width=\"16\"><button action=\"bypass -h xpnot\" width=24 height=12 back=\"L2UI_CH3.br_bar1_mp\" fore=\"L2UI_CH3.br_bar1_mp\"></td>");
			tb.append("</tr>");
		tb.append("<tr><td width=\"250\"><font color=\"00FF00\">By enabling that you won't be able to recieve expirience from killing monsters.</font></td><td align=\"center\" valign=\"middle\"><button action=\"bypass -h page1\" width=16 height=16 back=\"L2UI_CH3.back1\" fore=\"L2UI_CH3.next1\"></td></tr>");
		tb.append("</table>");
		tb.append("</center>");
		tb.append("</body></html>");
		
	nhm.setHtml(tb.toString());
		activeChar.sendPacket(nhm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}