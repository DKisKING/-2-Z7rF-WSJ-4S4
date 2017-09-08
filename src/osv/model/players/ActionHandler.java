package osv.model.players;

import osv.model.players.packets.npcoptions.NpcOptionFour;
import osv.model.players.packets.npcoptions.NpcOptionOne;
import osv.model.players.packets.npcoptions.NpcOptionThree;
import osv.model.players.packets.npcoptions.NpcOptionTwo;
import osv.model.players.packets.objectoptions.ObjectOptionFour;
import osv.model.players.packets.objectoptions.ObjectOptionOne;
import osv.model.players.packets.objectoptions.ObjectOptionThree;
import osv.model.players.packets.objectoptions.ObjectOptionTwo;

public class ActionHandler {

	private Player c;

	public ActionHandler(Player Client) {
		this.c = Client;
	}

	public void firstClickObject(int objectType, int obX, int obY) {
		ObjectOptionOne.handleOption(c, objectType, obX, obY);
	}

	public void secondClickObject(int objectType, int obX, int obY) {
		ObjectOptionTwo.handleOption(c, objectType, obX, obY);
	}

	public void thirdClickObject(int objectType, int obX, int obY) {
		ObjectOptionThree.handleOption(c, objectType, obX, obY);
	}

	public void fourthClickObject(int objectType, int obX, int obY) {
		ObjectOptionFour.handleOption(c, objectType, obX, obY);
	}

	public void firstClickNpc(int npcType) {
		NpcOptionOne.handleOption(c, npcType);
	}

	public void secondClickNpc(int npcType) {
		NpcOptionTwo.handleOption(c, npcType);
	}

	public void thirdClickNpc(int npcType) {
		NpcOptionThree.handleOption(c, npcType);
	}

	public void fourthClickNpc(int npcType) {
		NpcOptionFour.handleOption(c, npcType);
	}

}