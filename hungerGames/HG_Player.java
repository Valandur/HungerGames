package hungerGames;

import org.bukkit.entity.Player;

public class HG_Player
{
	public Player player;
	public String team;
	
	
	public HG_Player(Player Player, String TeamName)
	{
		this.player = Player;
		this.team = TeamName;
	}
}
