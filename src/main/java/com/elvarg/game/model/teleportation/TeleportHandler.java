package com.elvarg.game.model.teleportation;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.EffectTimer;
import com.elvarg.game.model.Position;
import com.elvarg.game.model.Locations.Location;
import com.elvarg.game.model.movement.MovementStatus;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;


public class TeleportHandler {

	/**
	 * Teleports a player to the target location.
	 * @param player			The player teleporting.
	 * @param targetLocation	The location to teleport to.
	 * @param teleportType		The type of teleport.
	 */
	public static void teleport(final Player player, final Position targetLocation, final TeleportType teleportType) {
		player.getMovementQueue().setMovementStatus(MovementStatus.DISABLED).reset();
		cancelCurrentActions(player);
		player.performAnimation(teleportType.getStartAnimation());
		player.performGraphic(teleportType.getStartGraphic());
		player.setUntargetable(true);
		TaskManager.submit(new Task(1, player, true) {
			int tick = 0;
			@Override
			public void execute() {
				if(tick == teleportType.getStartTick()) {
					cancelCurrentActions(player);
					player.performAnimation(teleportType.getEndAnimation());
					player.performGraphic(teleportType.getEndGraphic());
					player.moveTo(targetLocation);
					onArrival(player, targetLocation);
				} else if(tick == teleportType.getStartTick() + 2) {
					player.getMovementQueue().setMovementStatus(MovementStatus.NONE).reset();
					stop();
				}
				tick++;
			}
			@Override
			public void stop() {
				setEventRunning(false);
				player.getClickDelay().reset(0);
				player.setUntargetable(false);
			}
		});
		player.getClickDelay().reset();
	}

	/**
	 * Player arrived at this position
	 * @param player
	 * @param targetLocation
	 */
	public static void onArrival(Player player, Position targetLocation) {

	}

	public static boolean checkReqs(Player player, Position targetLocation) {
		if(player.busy()) {
			player.getPacketSender().sendMessage("You cannot do that right now.");
			return false;
		}
		if(!player.getCombat().getTeleBlockTimer().finished()) {
			if(player.getLocation() == Location.WILDERNESS) {
				player.getPacketSender().sendMessage("A magical spell is blocking you from teleporting.");
				return false;
			} else {
				player.getCombat().getTeleBlockTimer().stop();
				player.getPacketSender().sendEffectTimer(0, EffectTimer.TELE_BLOCK);
			}
		}

		if(player.getMovementQueue().getMovementStatus() == MovementStatus.DISABLED || !player.getClickDelay().elapsed(4500)) {
			return false;
		}
		
		return player.getLocation().canTeleport(player);
	}

	public static void cancelCurrentActions(Player player) {
		//Stop skilling..
		player.getSkillManager().stopSkillable();
		player.getPacketSender().sendInterfaceRemoval();
		player.setWalkToTask(null);
		if(player.getInteractingEntity() != null) {
			player.setEntityInteraction(null);
		}
		player.getMovementQueue().setFollowCharacter(null);
		player.getCombat().reset();
	}
}
