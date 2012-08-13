package hungerGames;

import java.util.HashMap;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class main extends JavaPlugin implements Listener
{
	private static String prefix = "[Hunger Games] ";
	
	private static Server server;
	private static Logger logger = Logger.getLogger("Minecraft");
	
	private static boolean isPlaying = false;
	private static HashMap<String, HG_Player> players = new HashMap<String, HG_Player>();
	private static HashMap<String, HG_PlayerOut> playersout = new HashMap<String, HG_PlayerOut>(); 
	
	public static Permission perm = null;
	
	
	public main()
	{
		
	}
	
	@Override
	public void onEnable()
	{
		server = getServer();
		
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null)
            perm = permissionProvider.getProvider();
		
		logger.info(prefix + "Registering events...");
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		
		PluginDescriptionFile pdf = this.getDescription();
		logger.info(prefix +  pdf.getName() + " version " + pdf.getVersion() + " is enabled");
	}
	@Override
	public void onDisable()
	{
		PluginDescriptionFile pdf = this.getDescription();
		
		logger.info(prefix + pdf.getName() + " is disabled");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		Player p = (Player)sender;
		
		if (p.isOp())
		{
			if (commandLabel.equalsIgnoreCase("start"))
			{
				for (Player pl : server.getOnlinePlayers())
				{
					if (players.containsKey(pl.getName()) && !pl.isOp())
					{
						pl.getInventory().clear();
						pl.setGameMode(GameMode.SURVIVAL);
						pl.setHealth(20);
						pl.setSaturation(20);
					}
				}
				
				isPlaying = true;
				
				server.broadcastMessage(ChatColor.GOLD + "-------------------------");
				server.broadcastMessage(ChatColor.GOLD + "The games have started!");
				server.broadcastMessage(ChatColor.GOLD + "-------------------------");
				server.broadcastMessage(ChatColor.BLUE + "Players:");
				for (HG_Player pl : players.values())
					server.broadcastMessage(ChatColor.GOLD + pl.player.getName() + " (" + ChatColor.WHITE + pl.team + ChatColor.GOLD + ")");
			}
			else if (commandLabel.equalsIgnoreCase("reset"))
			{
				players.clear();
				playersout.clear();
				isPlaying = false;
				
				for (Player pl : server.getOnlinePlayers())
				{
					if (!pl.isOp())
						perm.playerRemove(pl, "spectate.use");
				}
				
				server.broadcastMessage(ChatColor.GOLD + "-------------------------");
				server.broadcastMessage(ChatColor.GOLD + "The games have ended!");
				server.broadcastMessage(ChatColor.GOLD + "-------------------------");
			}
			else if (commandLabel.equalsIgnoreCase("hglist"))
			{
				server.broadcastMessage(ChatColor.BLUE + "Players:");
				for (HG_Player pl : players.values())
					server.broadcastMessage(ChatColor.GOLD + pl.player.getName() + " (" + ChatColor.WHITE + pl.team + ChatColor.GOLD + ")");
			}
		}
		else
		{
			if (commandLabel.equalsIgnoreCase("join"))
			{
				if (!isPlaying)
				{
					if (args.length < 1)
					{
						p.sendMessage(ChatColor.GOLD + "Syntax:");
						p.sendMessage(ChatColor.GOLD + "/join [team]");
					}
					else
					{						
						if (players.containsKey(p))
							players.remove(p);
						
						players.put(p.getName(), new HG_Player(p, args[0]));
						
						p.sendMessage(ChatColor.GOLD + "You have joined the " + ChatColor.BLUE + args[0] + ChatColor.GOLD + " team");
						logger.info(p.getName() + " has joined " + args[0]);
					}
				}
				else
					p.sendMessage(ChatColor.RED + "You cannot change teams while the game is running!");
			}
			else if (commandLabel.equalsIgnoreCase("team"))
			{
				if (players.containsKey(p))
					p.sendMessage(ChatColor.GOLD + "You are in the following team: " + ChatColor.BLUE + players.get(p));
				else
					p.sendMessage(ChatColor.GOLD + "You are not in a team");
			}
			else if (commandLabel.equalsIgnoreCase("undready"))
			{
				if (players.containsKey(p.getName()))
				{
					players.remove(p.getName());
					p.sendMessage(ChatColor.GOLD + "You have left your team");
				}
			}
		}
		
		return false;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		if (isPlaying)
		{
			if (playersout.containsKey(e.getPlayer().getName()))
			{
				players.put(e.getPlayer().getName(), new HG_Player(e.getPlayer(), playersout.get(e.getPlayer().getName()).TeamName));
				playersout.remove(e.getPlayer().getName());
				
				server.broadcastMessage(ChatColor.GREEN + e.getPlayer().getName() + " has joined the server and is back in the game!");
			}
		}
	}
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		if (isPlaying)
		{
			if (players.containsKey(e.getPlayer().getName()))
			{
				playersout.put(e.getPlayer().getName(), new HG_PlayerOut(e.getPlayer().getName(), players.get(e.getPlayer().getName()).team));
				players.remove(e.getPlayer().getName());
				
				server.broadcastMessage(ChatColor.RED + e.getPlayer().getName() + " has left the server while playing!");
			}
		}
	}
	@EventHandler
	public void onPlayerKick(PlayerKickEvent e)
	{
		if (isPlaying)
		{
			if (players.containsKey(e.getPlayer().getName()))
			{
				playersout.put(e.getPlayer().getName(), new HG_PlayerOut(e.getPlayer().getName(), players.get(e.getPlayer().getName()).team));
				players.remove(e.getPlayer().getName());
				
				server.broadcastMessage(ChatColor.RED + e.getPlayer().getName() + " has left the server while playing!");
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e)
	{
		Player p = e.getPlayer();
		
		if (!isPlaying)
		{		
			if (!p.isOp())
				e.setCancelled(true);
		}
		else
		{
			if (!p.isOp())
			{
				if (players.containsKey(p.getName()))
				{
					if (e.getBlock().getTypeId() != 18 && e.getBlock().getTypeId() != 106 && e.getBlock().getTypeId() != 39 && e.getBlock().getTypeId() != 40)
						e.setCancelled(true);
				}
				else
					e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e)
	{
		if (!isPlaying)
			e.setCancelled(true);
		else
		{
			if (e.getCause() == DamageCause.ENTITY_ATTACK && e.getEntityType() == EntityType.PLAYER)
			{
				try
				{
					String p = ((Player)e.getEntity()).getName();
					String pl = ((Player)e.getDamager()).getName();
					
					if (!players.containsKey(p) || !players.containsKey(pl))
						e.setCancelled(true);
					
					if (players.get(p).team.equalsIgnoreCase(players.get(pl).team))
						e.setCancelled(true);
				}
				catch (Exception ex)
				{ }
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		Player p = e.getPlayer();
		
		if (!p.isOp())
		{
			if (!players.containsKey(p.getName()))
				e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e)
	{
		Player p = (Player)e.getEntity();
		
		if (players.containsKey(p.getName()))
			players.remove(p.getName());
		
		server.broadcastMessage(ChatColor.BLUE + p.getName() + ChatColor.GOLD + " has died!");
		p.setGameMode(GameMode.SURVIVAL);
		perm.playerAdd(p, "spectate.use");
		
		server.broadcastMessage(ChatColor.BLUE + "Players left:");
		for (HG_Player pl : players.values())
			server.broadcastMessage(ChatColor.GOLD + pl.player.getName() + " (" + ChatColor.WHITE + pl.team + ChatColor.GOLD + ")");
		
		p.sendMessage(ChatColor.GOLD + "Use " + ChatColor.BLUE + "/spectate [playername] " + ChatColor.GOLD + "to observe a player");
	}
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e)
	{
		if (!e.getPlayer().isOp())
		{
			if (isPlaying)
			{
				if (players.containsKey(e.getPlayer().getName()))
					e.setMessage(ChatColor.GREEN + e.getMessage());
				else
					e.setMessage(ChatColor.GRAY + e.getMessage());
			}
			else
			{
				if (players.containsKey(e.getPlayer().getName()))
					e.setMessage(ChatColor.BLUE + e.getMessage());
				else
					e.setMessage(ChatColor.GRAY + e.getMessage());
			}
		}
	}
}
