package com.andersonhc.properTime;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import org.bukkit.World;

import com.andersonhc.properTime.ConfigParser.Conf;

public final class ProperTimeFixTime extends Thread {
	protected Timer t; // cancel this in onDisable!

	private ProperTime plugin;
	
	private int desiredStepDay;
	private int desiredStepNight;
	private int desiredStepDusk;
	private int desiredStepDawn;
	private World world;
	private long lasttime;
	private int perma = -1;
	
	
	
	private World worldFollow;
	private int worldFollowDelay;

	// Constructor
	ProperTimeFixTime(ProperTime plugin, Conf c){
		this.plugin = plugin;
		desiredStepDawn = (int) (c.factordawn * plugin.getDefaultStep());
		desiredStepDusk = (int) (c.factordusk * plugin.getDefaultStep());
		desiredStepDay = (int) (c.factorday * plugin.getDefaultStep());
		desiredStepNight = (int) (c.factornight * plugin.getDefaultStep());
		world = c.w;
		perma = c.perma;
		worldFollow = c.followWorld;
		worldFollowDelay = (int) c.followWorldDelay;
	}

	
	private int getStep(long a){
		if ((a % 24000) < 12000){ // day
			if ((a + plugin.getDefaultStep())%24000 > 12000) return desiredStepDusk;
			return desiredStepDay;
		}else if((a % 24000) < 13800){ // sundown
			if ((a + plugin.getDefaultStep())%24000 > 13800) return desiredStepNight;
			return desiredStepDusk;
		}else if((a % 24000) < 22200){ // night
			if ((a + plugin.getDefaultStep())%24000 > 22200) return desiredStepDawn;
			return desiredStepNight;
		}else{ // sunrise
			if ((a + plugin.getDefaultStep())%24000 < 12000) return desiredStepDay;
			return desiredStepDawn;
		}
	}

	public void run(){
		t = new Timer();
		t.schedule(new ttask(), plugin.getStepSize() * 1000, plugin.getStepSize() * 1000);
	}

	private final class ttask extends TimerTask{
		public void run(){
			plugin.getServer().getScheduler().callSyncMethod(plugin, new Step());
		}
	}


	private final class Step implements Callable<Void>{
		@Override
		public Void call() {
			long ctime = world.getTime();
			if (perma >= 0) {  /* Permanent time */
				world.setTime(ctime - (ctime % 24000) + perma);
			} if (worldFollow != null) {  /* World time follow */
				long newTime = worldFollow.getTime() - worldFollowDelay;
				if (newTime < 0) {
					newTime += plugin.getMcDayLength();
				}
				world.setTime(newTime);
			}else{
				if (ctime < lasttime + 3 * plugin.getDefaultStep() && ctime > lasttime - plugin.getDefaultStep()){
					long ntime = lasttime + getStep(lasttime);
					world.setTime(ntime);
					plugin.logInfo("Synchronized time on world \"" + world.getName() + "\", diff was "+ (ntime - ctime) + ".", true);
					lasttime = ntime;
				}else{ // someone used settime
					plugin.logInfo("(debug) Apparently someone used setTime, not synchronizing.", true);
					lasttime = ctime;
				}
			}
			return null;
		}

	}
}