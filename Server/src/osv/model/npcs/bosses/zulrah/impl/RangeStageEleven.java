package osv.model.npcs.bosses.zulrah.impl;

import java.util.Arrays;

import osv.event.CycleEventContainer;
import osv.event.CycleEventHandler;
import osv.model.npcs.bosses.zulrah.DangerousEntity;
import osv.model.npcs.bosses.zulrah.DangerousLocation;
import osv.model.npcs.bosses.zulrah.SpawnDangerousEntity;
import osv.model.npcs.bosses.zulrah.Zulrah;
import osv.model.npcs.bosses.zulrah.ZulrahLocation;
import osv.model.npcs.bosses.zulrah.ZulrahStage;
import osv.model.players.Player;
import osv.model.players.combat.CombatType;

public class RangeStageEleven extends ZulrahStage {

	private int finishedAttack;

	public RangeStageEleven(Zulrah zulrah, Player player) {
		super(zulrah, player);
	}

	@Override
	public void execute(CycleEventContainer container) {
		if (container.getOwner() == null || zulrah == null || zulrah.getNpc() == null || zulrah.getNpc().isDead || player == null || player.isDead
				|| zulrah.getInstancedZulrah() == null) {
			container.stop();
			return;
		}
		int ticks = container.getTotalTicks();
		if (zulrah.getNpc().totalAttacks >= 5 && finishedAttack == 0) {
			finishedAttack = ticks;
			zulrah.getNpc().attackTimer = 20;
			zulrah.getNpc().setFacePlayer(false);
			CycleEventHandler.getSingleton().addEvent(player, new SpawnDangerousEntity(zulrah, player, Arrays.asList(DangerousLocation.values()), DangerousEntity.TOXIC_SMOKE, 40),
					1);
		}
		if (finishedAttack > 0) {
			zulrah.getNpc().setFacePlayer(false);
			if (ticks - finishedAttack == 18) {
				zulrah.getNpc().setFacePlayer(false);
				zulrah.getNpc().totalAttacks = 0;
				zulrah.changeStage(2, CombatType.MELEE, ZulrahLocation.NORTH);
				container.stop();
			}
		}
	}
}