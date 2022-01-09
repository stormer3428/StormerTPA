package fr.stormer3428.TPA;

import java.util.HashMap;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
		if(all.containsKey(receiver)) 
			if(all.get(receiver).processed) all.remove(receiver);
			else {
				Message.error(sender, "This player already has a pending request");
				return null;
			}
		return new TeleportRequest(sender, receiver, type);
	}

	private TeleportRequest(Player sender, Player receiver, TeleportRequestType type) {
		this.sender = sender;
		this.receiver = receiver;
		this.type = type;
		all.put(receiver, this);
		Message.normal(sender, "Request sent to " + receiver.getName() 
		+ ", it will expire in " + (Tpa.teleportRequestDuration / 20) + " seconds");
		Message.normal(receiver, sender.getName() + " has sent you a request to teleport " + (type == TeleportRequestType.TPA ? "to you" : " you to them") + ", type /tpaccept to accept or /tpdeny to refuse" 
		+ ", it will expire in " + (Tpa.teleportRequestDuration / 20) + " seconds");
		new BukkitRunnable() {
			
			@Override
			public void run() {
				if(TeleportRequest.this.processed || TeleportRequest.this.teleporting) {
					cancel();
					return;
				}
				TeleportRequest.this.processed = true;
				all.remove(TeleportRequest.this.receiver);
				Message.normal(TeleportRequest.this.receiver, "The request from " + TeleportRequest.this.sender.getName() + " expired");
				Message.normal(TeleportRequest.this.sender, "The request sent to " + TeleportRequest.this.receiver.getName() + " expired");
			}
		}.runTaskLater(Tpa.i, Tpa.teleportRequestDuration);
	}
	
	public static void accept(Player p) {
		if(!all.containsKey(p) || all.get(p).processed) {
			all.remove(p);
			Message.error(p, "You currently do not have any request pending");
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

		Message.normal(destination, "Request accepted, teleporting " + target.getName() + " to you");
		Message.normal(target, "Request accepted, teleporting you to " + destination.getName());
		
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
			Message.error(p, "You currently do not have any request pending");
			return;
		}
		all.get(p).deny();
	}

	public void deny() {
		Message.normal(this.sender, this.receiver.getName() + " has refused your teleport request");
		Message.normal(this.receiver, "you have refused the teleport request of " + this.sender.getName());
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
		Block b = destination.getWorld().getHighestBlockAt(target.getLocation());
		while(b.getLocation().getY() > 0 && b.isPassable()) b = b.getRelative(0, -1, 0);
		if(Tpa.unsafeTypes.contains(b.getType()) || Tpa.unsafeTypes.contains(b.getRelative(0,1,0).getType())) {
			Message.error(target, "Teleport destination is unsafe, cancelling...");
			Message.error(destination, "Teleport destination is unsafe, cancelling...");
		}else target.teleport(b.getLocation().getBlock().getLocation().add(0.5, 1, 0.5));
		this.processed = true;
		all.remove(this.receiver);
	}
	
	public void cancel() {
		this.processed = true;
		Message.normal(this.sender, "The teleport request has been cancelled");
		Message.normal(this.receiver, "The teleport request has been cancelled");
	}
}
