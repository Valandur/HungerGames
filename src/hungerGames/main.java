package hungerGames;

import java.util.HashMap;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
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
	
	private static Permission perm = null;
	private static String hgworld = "hg";
	
	
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
        else
        {
        	logger.severe("Could not retrieve vault instance");
        	getServer().getPluginManager().disablePlugin(this);
        	return;
        }
		
		logger.info(prefix + "Registering events...");
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		
		logger.info(prefix + "Loading config...");
		
		getConfig().addDefault("hungerGamesWorld", "hg");
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		hgworld = getConfig().getString("hungerGamesWorld");
		
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
		if (commandLabel.equalsIgnoreCase("hg"))
		{
			if (sender.getClass() == CraftPlayer.class)
			{
				if (!((Player)sender).getWorld().getName().equalsIgnoreCase(hgworld))
				{
					sender.sendMessage(ChatColor.RED + "These commands can only be used in the Hunger Games world called '" + ChatColor.BLUE + hgworld + ChatColor.RED + "'");
					return false;
				}
			}
			
			if (args.length == 0)
			{
				sender.sendMessage(ChatColor.GOLD + "-------------------------");
				sender.sendMessage(ChatColor.GOLD + "Hunger Games - Commands");
				sender.sendMessage(ChatColor.GOLD + "-------------------------");
				sender.sendMessage(ChatColor.BLUE + "start" + ChatColor.WHITE + "     Starts the Hunger Games");
				sender.sendMessage(ChatColor.BLUE + "reset" + ChatColor.WHITE + "     Resets the Hunger Games");
				sender.sendMessage(ChatColor.BLUE + "list" + ChatColor.WHITE + "      Lists all the players taking part in the Hunger Games");
				sender.sendMessage(ChatColor.BLUE + "team" + ChatColor.WHITE + "      Tells you what team you're in");
				sender.sendMessage(ChatColor.BLUE + "join" + ChatColor.WHITE + "      Joins a team");
				sender.sendMessage(ChatColor.BLUE + "leave" + ChatColor.WHITE + "     Leave your current team");
				sender.sendMessage(ChatColor.GOLD + "-------------------------");
			}
			else
			{
				if (args[0].equalsIgnoreCase("start"))
				{
					if (!perm.has(sender, "hg.start"))
					{
						sender.sendMessage(ChatColor.RED + "You do not have the required permissions");
						return false;
					}
					
					for (Player pl : server.getOnlinePlayers())
					{
						if (pl.getWorld().getName().equalsIgnoreCase(hgworld) && players.containsKey(pl.getName()))
						{
							pl.getInventory().clear();
							pl.setGameMode(GameMode.SURVIVAL);
							pl.setHealth(20d);
							pl.setSaturation(20);
						}
					}
					
					isPlaying = true;
					
					server.broadcastMessage(ChatColor.GOLD + "-------------------------");
					server.broadcastMessage(ChatColor.GOLD + "Hunger Games - The games have started!");
					server.broadcastMessage(ChatColor.GOLD + "-------------------------");
					server.broadcastMessage(ChatColor.BLUE + "Players:");
					for (HG_Player pl : players.values())
						server.broadcastMessage("  " + ChatColor.GOLD + pl.player.getName() + " (" + ChatColor.WHITE + pl.team + ChatColor.GOLD + ")");
					server.broadcastMessage(ChatColor.GOLD + "-------------------------");
				}
				else if (args[0].equalsIgnoreCase("reset"))
				{
					if (!perm.has(sender, "hg.reset"))
					{
						sender.sendMessage(ChatColor.RED + "You do not have the required permissions");
						return false;
					}
					
					for (Player pl : server.getOnlinePlayers())
					{
						if (pl.getWorld().getName().equalsIgnoreCase(hgworld) && players.containsKey(pl.getName()) && !pl.isOp())
							pl.setGameMode(GameMode.CREATIVE);
					}
					
					players.clear();
					playersout.clear();
					isPlaying = false;
					
					server.broadcastMessage(ChatColor.GOLD + "-------------------------");
					server.broadcastMessage(ChatColor.GOLD + "Hunger Games - The games have ended!");
					server.broadcastMessage(ChatColor.GOLD + "-------------------------");
				}
				else if (args[0].equalsIgnoreCase("list"))
				{
					if (!perm.has(sender, "hg.list"))
					{
						sender.sendMessage(ChatColor.RED + "You do not have the required permissions");
						return false;
					}
					
					sender.sendMessage(ChatColor.GOLD + "-------------------------");
					sender.sendMessage(ChatColor.BLUE + "Hunger Games - Players:");
					sender.sendMessage(ChatColor.GOLD + "-------------------------");
					for (HG_Player pl : players.values())
						sender.sendMessage(ChatColor.GOLD + pl.player.getName() + " (" + ChatColor.WHITE + pl.team + ChatColor.GOLD + ")");
					sender.sendMessage(ChatColor.GOLD + "-------------------------");
				}
				else if (args[0].equalsIgnoreCase("join"))
				{
					if (!perm.has(sender, "hg.join"))
					{
						sender.sendMessage(ChatColor.RED + "You do not have the required permissions");
						return false;
					}
					
					if (!isPlaying)
					{
						if (args.length < 2)
						{
							sender.sendMessage(ChatColor.GOLD + "-------------------------");
							sender.sendMessage(ChatColor.WHITE + "Hunger Games - Join a team");
							sender.sendMessage(ChatColor.GOLD + "-------------------------");
							sender.sendMessage(ChatColor.GOLD + "Syntax:");
							sender.sendMessage(ChatColor.BLUE + "  /hg join [team]");
							sender.sendMessage(ChatColor.GOLD + "-------------------------");
						}
						else
						{						
							if (players.containsKey(sender.getName()))
								players.remove(sender.getName());
							
							players.put(sender.getName(), new HG_Player((Player)sender, args[1]));
							
							sender.sendMessage(ChatColor.GOLD + "You have joined the " + ChatColor.BLUE + args[1] + ChatColor.GOLD + " team");
							logger.info(sender.getName() + " has joined " + args[1]);
						}
					}
					else
						sender.sendMessage(ChatColor.RED + "You cannot change teams while the game is running!");
				}
				else if (args[0].equalsIgnoreCase("team"))
				{
					if (!perm.has(sender, "hg.team"))
					{
						sender.sendMessage(ChatColor.RED + "You do not have the required permissions");
						return false;
					}
					
					if (players.containsKey(sender.getName()))
						sender.sendMessage(ChatColor.GOLD + "You are in the following team: " + ChatColor.BLUE + players.get(sender.getName()).team);
					else
						sender.sendMessage(ChatColor.GOLD + "You are not in a team");
				}
				else if (args[0].equalsIgnoreCase("leave"))
				{
					if (!perm.has(sender, "hg.leave"))
					{
						sender.sendMessage(ChatColor.RED + "You do not have the required permissions");
						return false;
					}
					
					if (players.containsKey(sender.getName()))
					{
						players.remove(sender.getName());
						sender.sendMessage(ChatColor.GOLD + "You have left your team");
					}
				}
			}
		}
		
		return false;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Player p = e.getPlayer();
		
		if (isPlaying && p.getWorld().getName().equalsIgnoreCase(hgworld))
		{
			if (playersout.containsKey(e.getPlayer().getName()))
			{
				players.put(p.getName(), new HG_Player(p, playersout.get(p.getName()).TeamName));
				playersout.remove(p.getName());
				
				server.broadcastMessage(ChatColor.GREEN + p.getName() + " has joined the server and is back in the game!");
			}
		}
	}
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		Player p = e.getPlayer();
		
		if (isPlaying && p.getWorld().getName().equalsIgnoreCase(hgworld))
		{
			if (players.containsKey(p.getName()))
			{
				playersout.put(p.getName(), new HG_PlayerOut(p.getName(), players.get(p.getName()).team));
				players.remove(p.getName());
				
				server.broadcastMessage(ChatColor.RED + p.getName() + " has left the server while playing!");
			}
		}
	}
	@EventHandler
	public void onPlayerKick(PlayerKickEvent e)
	{
		Player p = e.getPlayer();
		
		if (isPlaying && p.getWorld().getName().equalsIgnoreCase(hgworld))
		{
			if (players.containsKey(p.getName()))
			{
				playersout.put(p.getName(), new HG_PlayerOut(p.getName(), players.get(p.getName()).team));
				players.remove(p.getName());
				
				server.broadcastMessage(ChatColor.RED + p.getName() + " has left the server while playing!");
			}
		}
	}
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent e)
	{
		Player p = e.getPlayer();
		
		if (e.getFrom().getName().equalsIgnoreCase(hgworld))
		{
			if (players.containsKey(p.getName()))
			{
				players.remove(p.getName());
				
				if (isPlaying)
					server.broadcastMessage(ChatColor.BLUE + p.getName() + ChatColor.GOLD + " has left the Hunger Games!");
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		Player p = e.getPlayer();
		
		if (p.getWorld().getName().equalsIgnoreCase(hgworld))
		{
			if (isPlaying)
			{
				if (!players.containsKey(p.getName()))
					e.setCancelled(true);
			}
			else
			{
				if (!perm.has(p, "hg.player.interact"))
				{
					p.sendMessage(ChatColor.RED + "You do not have the required permissions");
					e.setCancelled(true);
				}
			}
		}
	}
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e)
	{
		Player p = e.getPlayer();
		
		if (p.getWorld().getName().equalsIgnoreCase(hgworld))
		{
			if (isPlaying)
			{
				if (players.containsKey(p.getName()))
					e.setMessage(ChatColor.GREEN + e.getMessage());
				else
					e.setMessage(ChatColor.GRAY + e.getMessage());
			}
			else
			{
				if (players.containsKey(p.getName()))
					e.setMessage(ChatColor.BLUE + e.getMessage());
				else
					e.setMessage(ChatColor.GRAY + e.getMessage());
			}
		}
	}
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e)
	{
		Player p = (Player)e.getEntity();
		
		if (p.getWorld().getName().equalsIgnoreCase(hgworld))
		{
			if (players.containsKey(p.getName()))
				players.remove(p.getName());
			
			e.setDroppedExp(0);
			e.setKeepLevel(true);
			e.setDeathMessage(ChatColor.BLUE + p.getName() + ChatColor.GOLD + " has died during the Hunger Games!");
			p.setGameMode(GameMode.CREATIVE);
			
			for (Player pl : server.getOnlinePlayers())
			{
				if (pl.getWorld().getName().equalsIgnoreCase(hgworld) && players.containsKey(pl.getName()))
				{
					pl.sendMessage(ChatColor.GOLD + "-------------------------");
					pl.sendMessage(ChatColor.BLUE + "Hunger Games - Players left");
					pl.sendMessage(ChatColor.GOLD + "-------------------------");
					for (HG_Player pl_alive : players.values())
						pl.sendMessage(ChatColor.GOLD + pl_alive.player.getName() + " (" + ChatColor.WHITE + pl_alive.team + ChatColor.GOLD + ")");
					pl.sendMessage(ChatColor.GOLD + "-------------------------");
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e)
	{
		Player p = e.getPlayer();
		
		if (p.getWorld().getName().equalsIgnoreCase(hgworld))
		{
			if (!isPlaying)
			{
				if (!perm.has(p, "hg.block.break"))
				{
					p.sendMessage(ChatColor.RED + "You do not have the required permissions");
					e.setCancelled(true);
				}
			}
			else
			{
				if (players.containsKey(p.getName()))
				{
					if (e.getBlock().getType() != Material.LEAVES && e.getBlock().getType() != Material.LEAVES_2 && e.getBlock().getType() != Material.GRASS) 
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
		if (e.getEntity().getWorld().getName().equalsIgnoreCase(hgworld))
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
	}
}
