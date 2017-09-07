package osv.model.items.item_combinations;

import java.util.List;
import java.util.Optional;

import osv.model.items.GameItem;
import osv.model.items.ItemCombination;
import osv.model.players.Player;

public class ElysianSpiritShield extends ItemCombination {

	public ElysianSpiritShield(GameItem outcome, Optional<List<GameItem>> revertedItems, GameItem[] items) {
		super(outcome, revertedItems, items);
	}

	@Override
	public void combine(Player player) {
		super.items.forEach(item -> player.getItems().deleteItem2(item.getId(), item.getAmount()));
		player.getItems().addItem(super.outcome.getId(), super.outcome.getAmount());
		player.getDH().sendItemStatement("You combined the items and created the Elysian Spirit Shield.", 12817);
		player.setCurrentCombination(Optional.empty());
		player.nextChat = -1;
	}

	@Override
	public void showDialogue(Player player) {
		player.getDH().sendStatement("Once the sigil is combined with the blessed spirit shield", "there is no going back. The items cannot be reverted.");
	}

}
