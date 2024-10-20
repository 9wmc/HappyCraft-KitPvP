package com.planetgallium.kitpvp;

import com.planetgallium.kitpvp.game.Infobase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.planetgallium.kitpvp.api.EventListener;
import com.planetgallium.kitpvp.command.*;
import com.planetgallium.kitpvp.game.Arena;
import com.planetgallium.kitpvp.listener.*;
import com.planetgallium.kitpvp.util.*;

public class Game extends JavaPlugin implements Listener {
	
	private static Game instance;
	private static String prefix = "None";

	private Arena arena;
	private Infobase database;
	private Resources resources;
	
	private String updateVersion = "Error";
	private boolean needsUpdate = false;
	private boolean hasPlaceholderAPI = false;
	private boolean hasWorldGuard = false;
	
	@Override
	public void onEnable() {
		Toolkit.printToConsole("&7[&b&l职业战争&7] &7正在启动 &b职业战争 &7版本 &b" + this.getDescription().getVersion() + "&7...");

		instance = this;
		resources = new Resources(this);
		prefix = resources.getMessages().fetchString("Messages.General.Prefix");
		database = new Infobase(this);
		arena = new Arena(this, resources);

		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(this, this);
		pm.registerEvents(new EventListener(this), this);
		pm.registerEvents(new ArenaListener(this), this);
		pm.registerEvents(new JoinListener(this), this);
		pm.registerEvents(new LeaveListener(this), this);
		pm.registerEvents(new ArrowListener(this), this);
		pm.registerEvents(new DeathListener(this), this);
		pm.registerEvents(new HitListener(this), this);
		pm.registerEvents(new AttackListener(this), this);
		pm.registerEvents(new ItemListener(this), this);
		pm.registerEvents(new SoupListener(this), this);
		pm.registerEvents(new ChatListener(this), this);
		pm.registerEvents(new SignListener(this), this);
		pm.registerEvents(new AliasCommand(this), this);
		pm.registerEvents(new AbilityListener(this), this);
		pm.registerEvents(new TrackerListener(this), this);
		pm.registerEvents(new MenuListener(this), this);
		pm.registerEvents(getArena().getKillStreaks(), this);
		
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
	    getCommand("kitpvp").setExecutor(new MainCommand(this));
		
		new Metrics(this);
		
		new BukkitRunnable() {
			@Override
			public void run() {
				checkUpdate();
			}
		}.runTaskAsynchronously(this);
		
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			Bukkit.getConsoleSender().sendMessage(Toolkit.translate("&7[&b&l职业战争&7] &7正在与 &bPlaceholderAPI&7 插件挂钩..."));
			new Placeholders(this).register();
			hasPlaceholderAPI = true;
		}

		if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
			Bukkit.getConsoleSender().sendMessage(Toolkit.translate("&7[&b&l职业战争&7] &7正在与 &bWorldGuard&7 插件挂钩..."));
			hasWorldGuard = true;
		}

		populateUUIDCacheForOnlinePlayers();

		Toolkit.printToConsole("&7[&b&l职业战争&7] &a加载完毕！");
	}

	private void populateUUIDCacheForOnlinePlayers() {
		// populates UUID cache if there are players online when doing /reload to avoid a lot of errors related
		// to database and UUIDs
		if (Bukkit.getOnlinePlayers().size() > 0) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				CacheManager.getUUIDCache().put(player.getName(), player.getUniqueId().toString());
			}
		}
	}

	private void checkUpdate() {
		Updater.of(this).resourceId(27107).handleResponse((versionResponse, version) -> {
			switch (versionResponse) {
				case FOUND_NEW:
					Bukkit.getConsoleSender().sendMessage(Toolkit.translate("&7[&b&l职业战争&7] &a新版本出现了！ 请在Spigot页面升级至 v" + version + " 版本！&7&o(当前为开发版本)"));
					needsUpdate = true;
					updateVersion = version;
					break;
				case UNAVAILABLE:
					Bukkit.getConsoleSender().sendMessage(Toolkit.translate("&7[&b&l职业战争&7] &c无法检查升级，请尝试检查你的网络！"));
					break;
			}
		}).check();
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();

		if (!resources.getConfig().contains("Items.Leave")) {
			return;
		}

		if (Toolkit.matchesConfigItem(Toolkit.getHandItemForInteraction(e), resources.getConfig(), "Items.Leave")) {
			if (resources.getConfig().getBoolean("Items.Leave.Enabled")) {
				if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
					if (resources.getConfig().getBoolean("Items.Leave.BungeeCord.Enabled")) {
						ByteArrayDataOutput out = ByteStreams.newDataOutput();
						out.writeUTF("Connect");

						String server = resources.getConfig().fetchString("Items.Leave.BungeeCord.Server");

						out.writeUTF(server);
						p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
					}
					e.setCancelled(true);
				}
			}
		}
	}

	@Override
	public void onDisable() {
		// for players that haven't died and had their stats pushed
//		for (String username : CacheManager.getStatsCache().keySet()) {
//			arena.getStats().pushCachedStatsToDatabase(username);
//		}
	}

	public boolean hasPlaceholderAPI() { return hasPlaceholderAPI; }

	public boolean hasWorldGuard() { return hasWorldGuard; }

	public boolean needsUpdate() { return needsUpdate; }
	
	public String getUpdateVersion() { return updateVersion; }
	
	public static Game getInstance() { return instance; }
	
	public Arena getArena() { return arena; }

	public Infobase getDatabase() { return database; }
	
	public static String getPrefix() { return prefix; }
	
	public Resources getResources() { return resources; }
	
}
