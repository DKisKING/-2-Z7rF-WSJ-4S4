package osv.model.players.packets;

import osv.model.items.bank.BankItem;
import osv.model.players.PacketType;
import osv.model.players.Player;

public class BankAllButOne implements PacketType {

	@Override
	public void processPacket(Player player, int packetType, int packetSize) {
		int interfaceId = player.getInStream().readSignedWordBigEndianA();
		int itemId = player.getInStream().readSignedWordBigEndianA();
		@SuppressWarnings("unused")
		int itemSlot = player.getInStream().readSignedWordBigEndian();
		if (player.getInterfaceEvent().isActive()) {
			player.sendMessage("Please finish what you're doing.");
			return;
		}
		if (player.getTutorial().isActive()) {
			player.getTutorial().refresh();
			return;
		}
		switch (interfaceId) {
		case 5382:
			int amount = player.getBank().getCurrentBankTab().getItemAmount(new BankItem(itemId + 1));
			if (amount < 1)
				return;
			if (amount == 1) {
				player.sendMessage("Your bank only contains one of this item.");
				return;
			}
			if (player.getBank().getBankSearch().isSearching()) {
				player.getBank().getBankSearch().removeItem(itemId, amount - 1);
				return;
			}
			if ((player.getBank().getCurrentBankTab().getItemAmount(new BankItem(itemId + 1)) - 1) > 1)
				player.getItems().removeFromBank(itemId, amount - 1, true);
			break;
		}
	}

}
