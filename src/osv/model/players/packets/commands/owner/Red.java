package osv.model.players.packets.commands.owner;

import osv.model.players.Player;
import osv.model.players.packets.commands.Command;

/**
 * Show a red skull above the player's head.
 * 
 * @author Emiel
 *
 */
public class Red extends Command {

	@Override
	public void execute(Player c, String input) {
		c.headIconPk = (1);
		c.getPA().requestUpdates();
	}
}
