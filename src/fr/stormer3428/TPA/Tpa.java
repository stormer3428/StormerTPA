package fr.stormer3428.TPA;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import fr.stormer3428.TPA.TeleportRequest.TeleportRequestType;

public class Tpa extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
	
	public static Tpa i;

	public static int teleportationTicksDelay = 0;
	public static int teleportRequestDuration = 0;
	public static boolean requiresImmobile = false;

	public static List<Material> unsafeTypes = new ArrayList<>();
	
	static {
		unsafeTypes.add(Material.LAVA);
		unsafeTypes.add(Material.MAGMA_BLOCK);
	}
	
	@Override
	public void onEnable() {
		super.onEnable();
		i = this;
		
		getCommand("tpa").setExecutor(this);
		getCommand("tpa").setTabCompleter(this);
		
		ArrayList<String> tpahere_aliases = new ArrayList<>();
		tpahere_aliases.add("tpah");
		getCommand("tpahere").setAliases(tpahere_aliases);
		getCommand("tpahere").setExecutor(this);
		getCommand("tpahere").setTabCompleter(this);
		
		ArrayList<String> tpaccept_aliases = new ArrayList<>();
		tpaccept_aliases.add("tpyes");
		getCommand("tpaccept").setAliases(tpaccept_aliases);
		getCommand("tpaccept").setExecutor(this);
		getCommand("tpaccept").setTabCompleter(this);
		
		ArrayList<String> tpdeny_aliases = new ArrayList<>();
		tpdeny_aliases.add("tpno");
		getCommand("tpdeny").setAliases(tpdeny_aliases);
		getCommand("tpdeny").setExecutor(this);
		getCommand("tpdeny").setTabCompleter(this);
		
		getServer().getPluginManager().registerEvents(this, this);
		loadConfig();
	}

	public void loadConfig() {
		getConfig().options().copyDefaults(true);
		saveConfig();
		teleportationTicksDelay = getConfig().getInt("teleportationTicksDelay");
		teleportRequestDuration = getConfig().getInt("teleportRequestDuration");
		requiresImmobile = getConfig().getBoolean("requiresImmobile");
	}
	
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			Message.error(sender, "Sorry, but only a player may run this command");
			return false;
		}
		Player p = (Player) sender;
		if(command.getName().equalsIgnoreCase("tpa") || command.getName().equalsIgnoreCase("tpahere")) {
			if(args.length == 0) {
				Message.error(p, "You need to specify the player you wish to send the request to");
				return false;
			}
			for(Player pls : Bukkit.getOnlinePlayers()) {
				if(pls.getName().equals(args[0])) {
					TeleportRequest.createRequest(p, pls, command.getName().equalsIgnoreCase("tpa") ? TeleportRequestType.TPA : TeleportRequestType.TPAHERE);
					return true;
				}
			}
			Message.error(p, "No player with such name");
			return false;
		}
		if(command.getName().equalsIgnoreCase("tpaccept")) {
			TeleportRequest.accept(p);
			return true;
		}
		if(command.getName().equalsIgnoreCase("tpdeny")) {
			TeleportRequest.deny(p);
			return true;
		}
		return super.onCommand(sender, command, label, args);
	}
	
	@EventHandler
	public void onmove(PlayerMoveEvent e) {
		if(!requiresImmobile) return;
		if(TeleportRequest.all.containsKey(e.getPlayer()) && TeleportRequest.all.get(e.getPlayer()).teleporting) {
			Message.normal("You moved! Teleportation cancelled...");
			TeleportRequest.all.get(e.getPlayer()).cancel();
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		if(TeleportRequest.all.containsKey(e.getPlayer())) TeleportRequest.all.get(e.getPlayer()).cancel();
	}
	
}
