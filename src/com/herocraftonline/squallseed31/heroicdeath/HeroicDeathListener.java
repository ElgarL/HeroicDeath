package com.herocraftonline.squallseed31.heroicdeath;

import org.bukkit.block.Block;
import org.bukkit.entity.Player; 
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HeroicDeathListener implements Listener {
	
	private final HeroicDeath plugin;
	protected Map<String, DeathCertificate> deathRecords = new HashMap<String, DeathCertificate>();

	private Random RN = new Random();

	public HeroicDeathListener(HeroicDeath instance) {
		plugin = instance;
	}

	public String getMessage(ArrayList<String> ArrayString, DeathCertificate dc) {
		String attackerName = dc.getAttacker();
		String murderWeapon = null;
		if (dc.getMurderWeapon() != null)
			murderWeapon = HeroicDeath.Items.getItem(dc.getMurderWeapon());
		if (ArrayString.size() == 0)
			return dc.getDefender() + " died";
		if (ArrayString.size() > 1) {
			int num = this.RN.nextInt(ArrayString.size());
			String temp = (String) ArrayString.get(num);

			String temp2 = temp.replaceAll("%d",
					HeroicDeath.nameColor + dc.getDefender()
							+ HeroicDeath.messageColor);
			String temp3 = temp2.replaceAll("%a", HeroicDeath.nameColor
					+ attackerName + HeroicDeath.messageColor);
			temp = temp3.replaceAll("%i", HeroicDeath.itemColor + murderWeapon
					+ HeroicDeath.messageColor);
			temp = temp.replaceAll("%w", HeroicDeath.itemColor
					+ dc.getLocation().getWorld().getName()
					+ HeroicDeath.messageColor);

			return temp;
		}

		String temp = (String) ArrayString.get(0);

		String temp2 = temp.replaceAll("%d",
				HeroicDeath.nameColor + dc.getDefender()
						+ HeroicDeath.messageColor);
		String temp3 = temp2.replaceAll("%a", HeroicDeath.nameColor
				+ attackerName + HeroicDeath.messageColor);
		temp = temp3.replaceAll("%i", HeroicDeath.itemColor + murderWeapon
				+ HeroicDeath.messageColor);
		temp = temp.replaceAll("%w", HeroicDeath.itemColor
				+ dc.getLocation().getWorld().getName()
				+ HeroicDeath.messageColor);
		return temp;
	}

	public MaterialData getMurderWeapon(Player Attacker) {
		ItemStack item = Attacker.getItemInHand();
		int typeID = item.getTypeId();
		Short mData = item.getDurability();
		Byte matData = 0;
		if (mData < 256)
			matData = mData.byteValue();
		MaterialData mItem = new MaterialData(typeID, matData);
		return mItem;
	}

	public String getAttackerName(Entity damager) {
		
		if (damager instanceof Player) {
			Player attacker = (Player) damager;
			return HeroicDeath.getPlayerName(attacker);
		} else {
			
			return damager.getType().getName();
		}
		
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDeath(PlayerDeathEvent event) {
		
		((PlayerDeathEvent)event).setDeathMessage(null);
		Player player = (Player) event.getEntity();
		DeathCertificate dc = null;
		
		if (player.getLastDamageCause() != null)
			dc = deathRecords.containsKey(player.getName()) ? deathRecords.get(player.getName()) : processDamageEvent(player.getLastDamageCause());
			
		if (dc == null)
			dc = new DeathCertificate(player);
		
		if (dc.getMessage() == null)
			dc.setMessage(getMessage(HeroicDeath.DeathMessages.OtherMessages, dc));
		
		HeroicDeathEvent hde = new HeroicDeathEvent(dc);
		plugin.getServer().getPluginManager().callEvent(hde);
		dc = hde.getDeathCertificate();
		
		if (dc.getMessage() == null)
			dc.setMessage(getMessage(HeroicDeath.DeathMessages.OtherMessages, dc));
		
		if (!hde.isCancelled() && !plugin.getEventsOnly()) {
			plugin.broadcast(dc);
		}
		
		HeroicDeath.log.info("[HeroicDeath] "
				+ dc.getMessage().replaceAll("(?i)\u00A7[0-F]", ""));
		plugin.recordDeath(dc);
	}

	public DeathCertificate processDamageEvent(EntityDamageEvent event) {Player player;
		if (!(event.getEntity() instanceof Player)) {
			return null;
		} else {
			try {
				player = (Player) event.getEntity();
			} catch (ClassCastException e) {
				HeroicDeath.log.severe("Cannot cast entity as player: " + e);
				return null;
			}
		}
		String killString = HeroicDeath.getPlayerName(player) + " died.";
		DeathCertificate dc = new DeathCertificate(player, event.getCause());
		Entity damager = null;
		Block damageBlock = null;
		String blockName = null;
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
			damager = subEvent.getDamager();
			if (damager instanceof Projectile) {
				damager = ((Projectile) damager).getShooter();
			}
		} else if (event instanceof EntityDamageByBlockEvent) {
			EntityDamageByBlockEvent subEvent = (EntityDamageByBlockEvent) event;
			damageBlock = subEvent.getDamager();
			try {
				blockName = damageBlock.getType().toString();
			} catch (NullPointerException e) {
				blockName = "Unknown";
			}
		}
		
		switch (event.getCause()) {
		case PROJECTILE:
		case ENTITY_ATTACK:
		case ENTITY_EXPLOSION:
			
			if (damager == null) {
				dc.setAttacker("Dispenser");
				killString = getMessage(
						HeroicDeath.DeathMessages.DispenserMessages, dc);
			} else if (damager instanceof Player) {
				Player attacker = (Player) damager;
				dc.setAttacker(HeroicDeath.getPlayerName(attacker));
				dc.setMurderWeapon(getMurderWeapon(attacker));
				killString = getMessage(HeroicDeath.DeathMessages.PVPMessages,
						dc);
			} else {
				dc.setAttacker(getAttackerName(damager));
				if (damager instanceof TNTPrimed)
					killString = getMessage(
							HeroicDeath.DeathMessages.ExplosionMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("Creeper")
						&& !HeroicDeath.DeathMessages.CreeperExplosionMessages
								.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.CreeperExplosionMessages,
							dc);
				else if (dc.getAttacker().equalsIgnoreCase("Ghast")
						&& !HeroicDeath.DeathMessages.GhastMessages.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.GhastMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("Slime")
						&& !HeroicDeath.DeathMessages.SlimeMessages.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.SlimeMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("Zombie")
						&& !HeroicDeath.DeathMessages.ZombieMessages.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.ZombieMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("PigZombie")
						&& !HeroicDeath.DeathMessages.PigZombieMessages
								.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.PigZombieMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("Spider")
						&& !HeroicDeath.DeathMessages.SpiderMessages.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.SpiderMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("Skeleton")
						&& !HeroicDeath.DeathMessages.SkeletonMessages
								.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.SkeletonMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("Giant")
						&& !HeroicDeath.DeathMessages.GiantMessages.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.GiantMessages, dc);
				else if (dc.getAttacker().equalsIgnoreCase("Wolf")
						&& !HeroicDeath.DeathMessages.WolfMessages.isEmpty())
					killString = getMessage(
							HeroicDeath.DeathMessages.WolfMessages, dc);
				else
					killString = getMessage(
							HeroicDeath.DeathMessages.MonsterMessages, dc);
			}
			break;
		case BLOCK_EXPLOSION:
			killString = getMessage(
					HeroicDeath.DeathMessages.ExplosionMessages, dc);
			break;

		case CONTACT:
			dc.setAttacker(blockName);
			if (blockName != "CACTUS")
				HeroicDeath.log.info(player.getName()
						+ "was damaged by non-cactus block: " + blockName);
			killString = getMessage(HeroicDeath.DeathMessages.CactusMessages,
					dc);
			break;
		case FALL:
			killString = getMessage(HeroicDeath.DeathMessages.FallMessages, dc);
			break;
		case FIRE_TICK:
		case FIRE:
			killString = getMessage(HeroicDeath.DeathMessages.FireMessages, dc);
			break;
		case DROWNING:
			killString = getMessage(HeroicDeath.DeathMessages.DrownMessages, dc);
			break;
		case LAVA:
			killString = getMessage(HeroicDeath.DeathMessages.LavaMessages, dc);
			break;
		case SUFFOCATION:
			killString = getMessage(
					HeroicDeath.DeathMessages.SuffocationMessages, dc);
			break;
		case VOID:
			killString = getMessage(HeroicDeath.DeathMessages.VoidMessages, dc);
			break;
		case LIGHTNING:
			killString = getMessage(
					HeroicDeath.DeathMessages.LightningMessages, dc);
			break;
		case SUICIDE:
			killString = getMessage(HeroicDeath.DeathMessages.SuicideMessages,
					dc);
			break;
		case STARVATION:
			killString = getMessage(
					HeroicDeath.DeathMessages.StarvationMessages, dc);
			break;
		default: {
			killString = getMessage(HeroicDeath.DeathMessages.OtherMessages, dc);
		}
		}
		dc.setMessage(killString);
		return dc;
	}

	public class RespawnListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerQuit(PlayerQuitEvent event) {
			deathRecords.remove(event.getPlayer().getName());
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerRespawn(PlayerRespawnEvent event) {
			deathRecords.remove(event.getPlayer().getName());
		}
	}

	public class RecordPurge implements Runnable {
		private String player;

		public RecordPurge(String player) {
			this.player = player;
		}

		public void run() {
			deathRecords.remove(player);
			HeroicDeath.debug("Purging player: " + player);
		}

	}
}