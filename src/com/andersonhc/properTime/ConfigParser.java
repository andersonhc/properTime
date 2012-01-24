package com.andersonhc.properTime;

import java.io.File;

import org.bukkit.configuration.Configuration;
import org.bukkit.World;

public class ConfigParser {

	private final ProperTime plugin;

	public enum Origin {
		SPECIFIC, PRESET, FOLLOW
	};

	private Conf[] configs = null;
	private ProperTimeFixTime[] fixTimes;
	private Configuration conf = null;

	class Conf {
		World w;
		int perma = -1;
		double factorday = 1;
		double factornight = 1;
		double factordawn = 1;
		double factordusk = 1;
		Origin origin = null;
		World followWorld = null;
		double followWorldDelay = 0;

		Conf(World w) {
			this.w = w;
		}
	}

	public ConfigParser(ProperTime plugin) {
		this.plugin = plugin;

		configs = new Conf[plugin.getServer().getWorlds().size()];
		fixTimes = new ProperTimeFixTime[plugin.getServer().getWorlds().size()];

		for (int i = 0; i < configs.length; i++) {
			configs[i] = new Conf(plugin.getServer().getWorlds().get(i));
		}

		this.conf = plugin.getConfig();

		if ((new File("plugins" + File.separator + "ProperTime"
				+ File.separator + "config.yml").exists())) {
			parseConfigFile();
		} else {
			generateNewConfigFile();
		}

		for (int i = 0; i < configs.length; i++) {
			if (configs[i].origin != Origin.SPECIFIC
					&& configs[i].origin != Origin.PRESET) {
				plugin.logInfo("Applying default config on world "
						+ configs[i].w.getName(), false);
			}
			fixTimes[i] = new ProperTimeFixTime(plugin, configs[i]);
		}
	}

	private void parseConfigFile() {

		java.util.Set<String> worlds = null;

		if ((!conf.contains("propertime")) || conf.getConfigurationSection("propertime").getKeys(false).size() == 0) {
			generateNewConfigFile();
			return;
		} else {
			worlds = conf.getConfigurationSection("propertime").getKeys(false);
		}

		for (String curWorld : worlds) {

			System.out.println("now: " + curWorld);

			Conf c = null;
			for (int i = 0; i < configs.length; i++) {
				if (configs[i].w.getName().equals(curWorld)) {
					c = configs[i];
					plugin.logInfo("World set to: " + curWorld + ".", false);
				}
			}

			if (c == null) {
				plugin.logWarn("World " + curWorld + "not found!");
				continue;
			}

			System.out.println("propertime." + curWorld + ".followworld");
			if (conf.contains("propertime." + curWorld + ".preset")) {
				String preset = conf.getString("propertime." + curWorld	+ ".preset");
				boolean found = false;
				for (Preset p : Preset.values()) {
					if (p.name().equalsIgnoreCase(preset)) {
						found = true;
						c.factorday = p.getFactorday();
						c.factornight = p.getFactornight();
						c.factordusk = p.getFactordusk();
						c.factordawn = p.getFactordawn();
						c.perma = p.getPerma();
						plugin.logInfo("Preset \"" + preset + "\" applyed to " + curWorld, false);
						c.origin = Origin.PRESET;
					}
				}
				if (!found) {
					plugin.logWarn("Preset \"" + preset + "\" not found. Applying default.");
				}

			} else if (conf.contains("propertime." + curWorld + ".followworld")) {
				String followWorld = conf.getString("propertime." + curWorld + ".followworld");
				World worldFollowed = plugin.getServer().getWorld(followWorld);
				if (worldFollowed == null) {
					plugin.logWarn("World " + followWorld + "not found");
				}
				c.followWorld = worldFollowed;
				c.origin = Origin.FOLLOW;
				c.followWorldDelay = conf.getDouble("propertime." + curWorld + ".followworlddelay", 0.0);
				plugin.logInfo("World " + curWorld + " will follow " + followWorld + "'s time with a delay of " + c.followWorldDelay, false);
			} else {
				c.origin = Origin.SPECIFIC;
				c.factorday = conf.getDouble("propertime." + curWorld + ".timespeedday", 1.0);
				c.factornight = conf.getDouble("propertime." + curWorld + ".timespeednight", 1.0);
				c.factordusk = conf.getDouble("propertime." + curWorld + ".timespeeddusk", 1.0);
				c.factordawn = conf.getDouble("propertime." + curWorld + ".timespeeddawn", 1.0);

				String perma = conf.getString("propertime." + curWorld + ".perma", "-1");
				if (perma.equalsIgnoreCase("day")) {
					c.perma = 6000;
				} else if (perma.equalsIgnoreCase("night")) {
					c.perma = 18000;
				} else if (perma.equalsIgnoreCase("none")) {
					c.perma = -1;
				} else {
					c.perma = Integer.parseInt(perma);
				}
				plugin.logInfo("World: " + curWorld + " - SpeedDay: " + c.factorday, false);
				plugin.logInfo("World: " + curWorld + " - SpeedNight: " + c.factornight, false);
				plugin.logInfo("World: " + curWorld + " - SpeedDusk: " + c.factordusk, false);
				plugin.logInfo("World: " + curWorld + " - SpeedDawn: " + c.factordawn, false);
				plugin.logInfo("World: " + curWorld + " - Perma: " + c.perma, false);
			}

		}

		for (int i = 0; i < configs.length; i++) {

			if (configs[i].origin != Origin.SPECIFIC
					&& configs[i].origin != Origin.PRESET) {
				plugin.logInfo("Applying default config on world "
						+ configs[i].w.getName(), false);
				configs[i].perma = -1;
				configs[i].factorday = 1;
				configs[i].factornight = 1;
				configs[i].factordawn = 1;
				configs[i].factordusk = 1;
			}

			fixTimes[i] = new ProperTimeFixTime(plugin, configs[i]);
		}
	}

	private void generateNewConfigFile() {
		for (int i = 0; i < plugin.getServer().getWorlds().size(); i++) {
			String worldName = plugin.getServer().getWorlds().get(i).getName().toLowerCase();
			conf.set("propertime." + worldName + ".timespeedday", 1.0);
			conf.set("propertime." + worldName + ".timespeednight", 1.0);
			conf.set("propertime." + worldName + ".timespeeddusk", 1.0);
			conf.set("propertime." + worldName + ".timespeeddawn", 1.0);
			conf.set("propertime." + worldName + ".perma", "none");
		}
		plugin.saveConfig();
		plugin.logInfo("New configuration file created", false);
	}

	public ProperTimeFixTime[] getFixTimes() {
		return this.fixTimes;
	}

}