package nl.xupwup.properTime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public final class properTime extends JavaPlugin{
	private static int dayLength = 20*60; // 20 minutes
	private static int mcDayLength = 24000;
	private static int stepSize = 5; // seconds 
	private static int steps = dayLength / stepSize;
	private static int defaultStep = mcDayLength / steps;
	private boolean debug = false;
	
    private String name;
    private String version = "";
	private Logger log;
	private Plugin self = this;
	
	private File config;

	Server server;
	private Conf[] configs;
	private FixTime[] fixtimes;

	private enum Origin { DEFAULT, SPECIFIC, PRESET };
	
	class Conf{
		World w;
		int perma = -1;
		double factorday = 1;
		double factornight = 1;
		double factordawn = 1;
		double factordusk = 1;
		Conf(World w){
			this.w = w;
		}
		Origin origin = null;
	}
	
		
	@Override
	public void onDisable() {
		try {
			for(int i = 0; i < configs.length; i++){
				fixtimes[i].t.cancel();
				fixtimes[i].join();
				log.info("Thread "+ i + " successfully joined.");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("[" + name + " " + version + "] signing off.");
	}

	@Override
	public void onEnable() {
		PluginDescriptionFile pd = this.getDescription();
		name = pd.getName();
		version = pd.getVersion();
		server = getServer();
		configs = new Conf[server.getWorlds().size()];
		fixtimes = new FixTime[configs.length];
		for(int i = 0; i < configs.length; i++){
			configs[i] = new Conf(server.getWorlds().get(i));
		}
		Conf defaultConf = new Conf(null);
		
        log = Logger.getLogger("Minecraft");
        
        File folder = new File("plugins" + File.separator + "properTime");
        if (!folder.exists()) {
            folder.mkdir();
        }
        config = new File(folder.getAbsolutePath() + File.separator + "properTime.conf");
		
		if (config.exists()){
			try {
				log.info(name + " reading from config at " + config.getCanonicalPath());
				Scanner sc = new Scanner(config);
				
				String lCurrentWorld = "";
				boolean lValidWorld = false;

				Conf c = null;
				while (sc.hasNext()){
					String s = sc.nextLine();
					if (s.length() == 0) continue; // skip empty
					if (s.charAt(0) == '#') continue; // skip comments
					
					if (s.equals("debug")) {
						debug = true;
						log.info("properTime debugging enabled");
						continue;
					}
					
					String[] split = s.split("[ :]+");
					
					if(split.length < 2 && !split[0].equalsIgnoreCase("default")){
						log.info("properTime: Ignoring line: \"" + s + "\"");
						continue;
					}
					
					if(split[0].equalsIgnoreCase("world")){
						boolean found = false;
						split = s.split("\""); // at slot 0 we should see "world: " and slot 1 should contain the world name
						if (split.length < 2){
							log.warning("properTime: Bad config file, world name needs double quotes around it. Declaration dropped.");
							continue;
						}
						for(int i = 0; i < configs.length; i++){
							if(configs[i].w.getName().equals(split[1])){
								c = configs[i];
								found = true;
								if (debug) log.info("World set to: " + split[1] + ".");
								lCurrentWorld = "World " + split[1];
								lValidWorld = true;
							}
						}
						if(!found){
							log.warning("properTime: World '"+ split[1] + "' not found");
						}
						
					}else if (split[0].equalsIgnoreCase("default")) {
						c = defaultConf;
						if (debug) log.info("properTime default config:");
						lCurrentWorld = "Default ";
						lValidWorld = true;
						
					}else if (split[0].equalsIgnoreCase("preset")){
						if (c.origin != null) {
							log.warning("properTime: invalid config file");
						}else {
							applyPreset(c, split[1], lCurrentWorld);							
						}
					}else if (split[0].equalsIgnoreCase("timespeedDay")){
						if (isValidWorld(c, lValidWorld) & isValidOrigin(c)) {
							c.factorday = Double.parseDouble(split[1]);
							if (debug) log.info(lCurrentWorld + " timespeedDay is now " + c.factorday);
						}
					}else if (split[0].equalsIgnoreCase("timespeedNight")){
						if (isValidWorld(c, lValidWorld) & isValidOrigin(c)) {
							c.factornight = Double.parseDouble(split[1]);
							if (debug) log.info(lCurrentWorld + " timespeedNight is now " + c.factornight);
						}
					}else if (split[0].equalsIgnoreCase("timespeedDusk")){
						if (isValidWorld(c, lValidWorld) & isValidOrigin(c)) {
							c.factordusk = Double.parseDouble(split[1]);
							if (debug) log.info(lCurrentWorld + " timespeedDusk is now " + c.factordusk);
						}
					}else if (split[0].equalsIgnoreCase("timespeedDawn")){
						if (isValidWorld(c, lValidWorld) & isValidOrigin(c)) {
							c.factordawn = Double.parseDouble(split[1]);
							if (debug) log.info(lCurrentWorld + " timespeedDawn is now " + c.factordawn);
						}
					}else if (split[0].equalsIgnoreCase("perma")){
						if (isValidWorld(c, lValidWorld) & isValidOrigin(c)) {
							if (split[1].equalsIgnoreCase("day")) {
									c.perma = 6000;
							}else if (split[1].equalsIgnoreCase("night")){
									c.perma = 18000;
							}else if (split[1].equalsIgnoreCase("none")){
								c.perma = -1;
							}else {	c.perma = Integer.parseInt(split[1]);}
							if (debug) log.info(/*"World " + c.w.getName() + ", */"time is now fixed to " + c.perma);
						}
					}
				}
				
				for(int i = 0; i < configs.length; i++){
					
					if (configs[i].origin != Origin.SPECIFIC && configs[i].origin != Origin.PRESET) {
						log.info("Applying default config on world " + configs[i].w.getName());
						configs[i].perma = defaultConf.perma;
						configs[i].factorday = defaultConf.factorday;
						configs[i].factornight = defaultConf.factornight;
						configs[i].factordawn = defaultConf.factordawn;
						configs[i].factordusk = defaultConf.factordusk;
					}
					
					fixtimes[i] = new FixTime(configs[i]);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try {
				config.createNewFile();
				BufferedWriter out = new BufferedWriter(new FileWriter(config));
				out.write("# Use 2 for double speed, 3 for triple, etc.\n# At 1x speed, the complete day-night cycle takes 20 minutes.\n\n");
				out.write("# To freeze the time, use perma = (value between 0 and 24000)\n# Or use perma = night or perma = day\n\n");
				out.write("# To activate debug mode, uncomment the next line.\n# debug\n");
				for(int i = 0; i < fixtimes.length; i++){
					out.write("\nworld: \"" + server.getWorlds().get(i).getName() + "\"\n");
					out.write("timespeedDay: 1.0\ntimespeedNight: 1.0\ntimespeedDusk: 1.0\ntimespeedDawn: 1.0\nperma: none\n");
				}
				
				
				out.close();
				log.info("Created config file at " + config.getCanonicalPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.info(name +" started using defaults.");
			for(int i = 0; i < configs.length; i++){
				fixtimes[i] = new FixTime(configs[i]);
			}
		}
		for(int i = 0; i < configs.length; i++){
			fixtimes[i].start();
		}
		log.info(name + " " + version + " initialized");
	}
	
	private boolean isValidWorld(Conf c, boolean lValidWorld) {
		if(c == null | !lValidWorld) {
			log.warning("Invalid config file, no world specified");
			return false;
		}
		return true;
	}
	
	private boolean isValidOrigin(Conf c) {
		if (c.origin != null & c.origin != Origin.SPECIFIC) {
			log.warning("Invalid config file");
			return false;
		}
		c.origin = Origin.SPECIFIC;
		return true;
	}
	
	private void applyPreset (Conf c, String preset, String world) {
		boolean found = false;
		for (Preset p: Preset.values()) {
			if (p.name().equalsIgnoreCase(preset)) {
				found = true;
				c.factorday = p.getFactorday();
				c.factornight = p.getFactornight();
				c.factordusk = p.getFactordusk();
				c.factordawn = p.getFactordawn();
				c.perma = p.getPerma();
				log.info("properTime: Preset \"" + preset + "\" applyed to " + world);
				c.origin = Origin.PRESET;
			}
		}
		if (!found) {
			log.warning("properTime: preset \"" + preset + "\" not found." );
		}
	}
	
	
	public final class FixTime extends Thread {
		public Timer t; // cancel this in ondisable!
		
		private int desiredStepDay;
		private int desiredStepNight;
		private int desiredStepDusk;
		private int desiredStepDawn;
		private World wrld;
		private long lasttime;
		private int perma = -1;
		
		private int getStep(long a){
			if ((a % 24000) < 12000){ // day
				if ((a + defaultStep)%24000 > 12000) return desiredStepDusk;
				return desiredStepDay;
			}else if((a % 24000) < 13800){ // sundown
				if ((a + defaultStep)%24000 > 13800) return desiredStepNight;
				return desiredStepDusk;
			}else if((a % 24000) < 22200){ // night
				if ((a + defaultStep)%24000 > 22200) return desiredStepDawn;
				return desiredStepNight;
			}else{ // sunrise
				if ((a + defaultStep)%24000 < 12000) return desiredStepDay;
				return desiredStepDawn;
			}
		}
		
		FixTime(Conf c){
			desiredStepDawn = (int) (c.factordawn * defaultStep);
			desiredStepDusk = (int) (c.factordusk * defaultStep);
			desiredStepDay = (int) (c.factorday * defaultStep);
			desiredStepNight = (int) (c.factornight * defaultStep);
			wrld = c.w;
			perma = c.perma; 
		}
		
		public void run(){
			t = new Timer();
			t.schedule(new ttask(), stepSize * 1000, stepSize * 1000);
		}
		
		private class ttask extends TimerTask{
			public void run(){
				getServer().getScheduler().callSyncMethod(self, new Step());
			}
		}
		
		
		private final class Step implements Callable<Void>{
			@Override
			public Void call() {
				long ctime = wrld.getTime();
				if (perma >= 0){
					wrld.setTime(ctime - (ctime % 24000) + perma);
				}else{
					if (ctime < lasttime + 3*defaultStep && ctime > lasttime - defaultStep){
						
						long ntime = lasttime + getStep(lasttime);
						wrld.setTime(ntime);
						if (debug) log.info("Synchronized time on world \"" + wrld.getName() + "\", diff was "+ (ntime - ctime) + ".");
						lasttime = ntime;
					}else{ // someone used settime
						if (debug) log.info("Apparently someone used setTime, not synchronizing.");
						lasttime = ctime;
					}
				}
				return null;
			}
			
		}
	}
}
