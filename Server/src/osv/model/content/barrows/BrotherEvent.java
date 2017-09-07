package osv.model.content.barrows;

import osv.event.Event;
import osv.model.npcs.NPC;
import osv.model.npcs.NPCHandler;
import osv.model.players.Player;

public class BrotherEvent extends Event<Player> {

	public BrotherEvent(Player attachment, int ticks) {
		super(attachment, ticks);
	}

	@Override
	public void execute() {
		if (attachment == null || !attachment.getBarrows().getActive().isPresent() || attachment.getBarrows().getActive().get().isDefeated()) {
			stop();
			return;
		}
		attachment.getBarrows().getActive().ifPresent(brother -> {
			if (brother.getNPC() == null) {
				stop();
				return;
			} else {
				NPC npc = brother.getNPC();
				if (attachment.distanceToPoint(npc.absX, npc.absY, npc.heightLevel) > 20) {
					stop();
					return;
				}
			}
		});
		if ((getElapsedTicks() + 1) % 30 == 0) {
			attachment.getBarrows().drainPrayer();
		}
	}

	@Override
	public void stop() {
		if (attachment == null) {
			super.stop();
			return;
		}
		attachment.getBarrows().getActive().ifPresent(brother -> {
			brother.setActive(false);
			NPC npc = brother.getNPC();
			if (npc != null) {
				NPCHandler.npcs[npc.getIndex()] = null;
			}
		});
		super.stop();
	}

}
