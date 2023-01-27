package fr.stormer3428.TPA;

import java.util.HashMap;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import fr.stormer3428.TPA.common.Lang;
import fr.stormer3428.TPA.common.Message;

public class TeleportRequest {

	public static enum TeleportRequestType{
		TPA,
		TPAHERE;
	}

	public static HashMap<Player, TeleportRequest> all = new HashMap<>();

	private Player sender;
	private Player receiver;
	private TeleportRequestType type;
	public boolean teleporting = false;
	public boolean processed = false;

	public static TeleportRequest createRequest(Player sender, Player receiver, TeleportRequestType type) {
		Message.normal("Request from " + sender.getName() + " to " + receiver.getName() + " of type " + type.name());
		if(all.containsKey(receiver)) 
			if(all.get(receiver).processed) all.remove(receiver);
			else {
				Message.error(sender, Lang.ERROR_REQUEST_PENDING.toString());
				return null;
			}
		return new TeleportRequest(sender, receiver, type);
	}

	private TeleportRequest(Player sender, Player receiver, TeleportRequestType type) {
		this.sender = sender;
		this.receiver = receiver;
		this.type = type;
		all.put(receiver, this);
		Message.normal(sender, Lang.TPA_REQUEST_SENT.toString().replace("<PLAYER>", receiver.getName()).replace("<SECONDS>", ""+(Tpa.teleportRequestDuration / 20)));
		Message.normal(receiver, (type == TeleportRequestType.TPA ? Lang.TPA_REQUEST_RECEIVED_TPA : Lang.TPA_REQUEST_RECEIVED_TPAHERE).toString().replace("<PLAYER>", sender.getName()).replace("<SECONDS>", ""+(Tpa.teleportRequestDuration / 20)));
		
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				if(TeleportRequest.this.processed || TeleportRequest.this.teleporting) {
					cancel();
					return;
				}
				TeleportRequest.this.processed = true;
				all.remove(TeleportRequest.this.receiver);
				Message.normal(TeleportRequest.this.receiver, Lang.TPA_REQUEST_EXPIRED_RECEIVED.toString().replace("<PLAYER>", sender.getName()));
				Message.normal(TeleportRequest.this.sender, Lang.TPA_REQUEST_EXPIRED_SENT.toString().replace("<PLAYER>", receiver.getName()));
			}
		}.runTaskLater(Tpa.i, Tpa.teleportRequestDuration);
	}
	
	public static void accept(Player p) {
		if(!all.containsKey(p) || all.get(p).processed) {
			all.remove(p);
			Message.error(p, Lang.ERROR_NO_REQUEST_PENDING.toString());
			return;
		}
		all.get(p).accept();
	}
	
	public void accept() {
		Player target = this.sender;
		Player destination = this.receiver;

		if (this.type == TeleportRequestType.TPAHERE) {
			target = this.receiver;
			destination = this.sender;
		}

		Message.normal(destination, Lang.TPA_REQUEST_ACCEPTED_TO.toString().replace("<PLAYER>", target.getName()));
		Message.normal(target, Lang.TPA_REQUEST_ACCEPTED_FROM.toString().replace("<PLAYER>", destination.getName()));
		
		if(Tpa.teleportationTicksDelay <= 0) {
			teleport();
			return;
		}
		
		TeleportRequest.this.teleporting = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				if(TeleportRequest.this.processed) {
					cancel();
					return;
				}
				teleport();
			}
		}.runTaskLater(Tpa.i, Tpa.teleportationTicksDelay);
	}
	
	public static void deny(Player p) {
		if(!all.containsKey(p) || all.get(p).processed) {
			all.remove(p);
			Message.error(p, Lang.ERROR_NO_REQUEST_PENDING.toString());
			return;
		}
		all.get(p).deny();
	}

	public void deny() {
		Message.normal(this.sender, Lang.TPA_REQUEST_REFUSED_SENT.toString().replace("<PLAYER>", receiver.getName()));
		Message.normal(this.receiver, Lang.TPA_REQUEST_REFUSED_RECEIVED.toString().replace("<PLAYER>", sender.getName()));
		this.processed = true;
		all.remove(this.receiver);
	}

	private void teleport() {
		Player target = this.sender;
		Player destination = this.receiver;
		if (this.type == TeleportRequestType.TPAHERE) {
			target = this.receiver;
			destination = this.sender;
		}
		Block b = destination.getWorld().getHighestBlockAt(destination.getLocation());
		while(b.getLocation().getY() > 0 && b.isPassable() && !b.isLiquid()) b = b.getRelative(0, -1, 0);
		if(Tpa.unsafeTypes.contains(b.getType()) || Tpa.unsafeTypes.contains(b.getRelative(0,1,0).getType()) || Tpa.unsafeTypes.contains(b.getRelative(0,2,0).getType()) || Tpa.unsafeTypes.contains(b.getRelative(0,-1,0).getType())) {
			Message.error(target, Lang.TPA_CANCELLED_UNSAFE.toString());
			Message.error(destination, Lang.TPA_CANCELLED_UNSAFE.toString());
		}else target.teleport(b.getLocation().getBlock().getLocation().add(0.5, 1, 0.5));
		this.processed = true;
		all.remove(this.receiver);
	}
	
	public void cancel() {
		this.processed = true;
		Message.normal(this.sender, Lang.TPA_CANCELLED.toString());
		Message.normal(this.receiver, Lang.TPA_CANCELLED.toString());
	}
}
