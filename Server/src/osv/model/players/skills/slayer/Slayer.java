package osv.model.players.skills.slayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Range;

import osv.Config;
import osv.model.content.achievement.AchievementType;
import osv.model.content.achievement.Achievements;
import osv.model.content.achievement_diary.karamja.KaramjaDiaryEntry;
import osv.model.content.achievement_diary.morytania.MorytaniaDiaryEntry;
import osv.model.content.achievement_diary.western_provinces.WesternDiaryEntry;
import osv.model.items.ItemAssistant;
import osv.model.npcs.NPC;
import osv.model.players.Boundary;
import osv.model.players.Player;
import osv.model.players.Right;
import osv.model.players.mode.ModeType;
import osv.model.players.skills.Skill;
import osv.util.Misc;

public class Slayer {

	/**
	 * The amount of experience gained after finishing a boss task.
	 */
	private static final int BOSS_TASK_EXPERIENCE = 15_000;

	/**
	 * The current task for this player
	 */
	private Optional<Task> task = Optional.empty();

	/**
	 * The {@link NPC} id of the master that this player receives tasks from
	 */
	private int master;

	/**
	 * The amount of tasks that the player has completed consecutively from the
	 * same slayer master
	 */
	private int consecutiveTasks;

	/**
	 * The amount of slayer points the player has
	 */
	private int points;

	/**
	 * The amount of the task the player has left to slay
	 */
	private int taskAmount;

	/**
	 * The player that will be referenced in slayer related operations
	 */
	private final Player player;

	/**
	 * Determines if this player can create the slayer helm
	 */
	private boolean helmetCreatable;

	/**
	 * Determines if the player can create the imbued slayer helm
	 */
	private boolean helmetImbuedCreatable;

	/**
	 * Determines if this player can obtain larger boss tasks
	 */
	private boolean biggerBossTasks;

	/**
	 * Determines if this player can navigate to cerberus
	 */
	private boolean learnedCerberusRoute;

	/**
	 * Sets the color of which you want to turn your slayer helmet into
	 */
	private String color;

	/**
	 * The task master names that the player has decided to remove
	 */
	private String[] removed = Misc.nullToEmpty(4);

	/**
	 * Creates a new class for managing slayer operations
	 * 
	 * @param player
	 *            the player this is created for
	 */
	public Slayer(Player player) {
		this.player = player;
	}

	/**
	 * Creates a new random slayer task for the player by grabbing a random task
	 * from the slayer master that the player is capable of completing.
	 * 
	 * @param masterId
	 *            the id of the master
	 */
	public void createNewTask(int masterId) {
		SlayerMaster.get(masterId).ifPresent(m -> {
			if (player.calculateCombatLevel() < m.getLevel()) {
				player.getDH().sendNpcChat("You need a combat level of " + m.getLevel() + " to receive tasks from me.",
						"Please come back when you have this combat level.");
				return;
			}
			if (masterId == 401 && master != 401 && consecutiveTasks > 0 && taskAmount > 0) {
				consecutiveTasks = 0;
				player.sendMessage("Your consecutive tasks have been reset as you have switched to an easy task.");
			}
			switch (masterId) {
			case 405:
				player.getDiaryManager().getKaramjaDiary().progress(KaramjaDiaryEntry.DURADEL);
				break;

			case 6797:
				if (player.fullVoidMelee()) {
					player.getDiaryManager().getWesternDiary().progress(WesternDiaryEntry.FULL_VOID);
				}
				break;
			}
			Task[] available = retainObtainable(m.getAvailable());
			task = Optional.of(Misc.randomSearch(available, 0, available.length - 1));

			taskAmount = m.getId() == 6797 && !task.equals("tztok-jad") && biggerBossTasks
					? Misc.random(Range.between(task.get().getMinimum(), task.get().getMaximum())) + Misc.random(40)
							+ 10
					: Misc.random(Range.between(task.get().getMinimum(), task.get().getMaximum()));

			player.talkingNpc = m.getId();
			player.getDH().sendNpcChat("You have been assigned " + taskAmount + " " + task.get().getPrimaryName() + ".",
					"Come talk to me when you finish this task.");
			player.nextChat = -1;
			master = m.getId();
		});
	}

	/**
	 * A function referenced when a monster is killed. We manage cancelling the
	 * task and appending additional experience from this function.
	 * 
	 * @param npc
	 *            the non-playable character being killing.
	 */
	public void killTaskMonster(NPC npc) {
		if (npc == null) {
			return;
		}
		if (player == null) {
			return;
		}
		if (!task.isPresent()) {
			return;
		}
		if (taskAmount == 0) {
			return;
		}

		task.ifPresent(task -> {
			String name = npc.getDefinition().getNpcName().toLowerCase().replaceAll("_", " ");

			if (name.equals(task.getPrimaryName()) || ArrayUtils.contains(task.getNames(), name)) {
				Optional<SlayerMaster> master = SlayerMaster.get(this.master);

				master.ifPresent(m -> {
					taskAmount--;
					player.getPA()
							.addSkillXP(
									player.getRechargeItems().hasAnyItem(13113, 13114, 13115)
											&& Boundary.isIn(player, Boundary.SLAYER_TOWER_BOUNDARY)
													? (int) (task.getExperience() * 1.10)
															* (player.getMode().getType().equals(ModeType.OSRS) ? 1
																	: Config.SLAYER_EXPERIENCE)
													: task.getExperience()
															* (player.getMode().getType().equals(ModeType.OSRS) ? 3
																	: Config.SLAYER_EXPERIENCE),
									Skill.SLAYER.getId(), true);
					if (taskAmount == 0) {
						int consecutive = consecutiveTasks + 1;
						this.consecutiveTasks++;
						this.points += m.getPointReward(0);
						this.task = Optional.empty();
						player.sendMessage(
								"You have completed your slayer task, talk to a slayer master to receive another.");

						int bonusPoints = this.consecutiveTasks == 10 ? m.getPointReward(0)
								: this.consecutiveTasks == 30 ? m.getPointReward(1)
										: this.consecutiveTasks == 50 ? m.getPointReward(2)
												: this.consecutiveTasks == 70 ? m.getPointReward(3)
														: this.consecutiveTasks == 100 ? m.getPointReward(4)
																: this.consecutiveTasks == 150 ? m.getPointReward(5)
																		: 0;

						if (bonusPoints > 0) {
							points += bonusPoints;
							player.sendMessage("<col=255>You have completed " + consecutive
									+ " tasks in a row and receive " + bonusPoints + " additional points.</col>");
						}

						player.getPA().sendFrame126("@or1@- Slayer Points: @whi@" + player.getSlayer().getPoints(),
								29173);
						player.getPA().sendFrame126(
								"@or1@- Consecutive Tasks: @whi@" + player.getSlayer().getConsecutiveTasks(), 29174);
						if (consecutive == 10) {
							player.getDiaryManager().getMorytaniaDiary().progress(MorytaniaDiaryEntry.TEN_CONSECUTIVE);
						}
						switch (m.getId()) {
						case 402:
							player.getDiaryManager().getMorytaniaDiary().progress(MorytaniaDiaryEntry.MAZCHNA);
							break;

						case 6797:
							player.getPA().addSkillXP(BOSS_TASK_EXPERIENCE, Skill.SLAYER.getId(), true);
							player.sendMessage("You have completed a boss task and have gained an additional "
									+ Misc.insertCommas(Integer.toString(BOSS_TASK_EXPERIENCE)) + " experience.", 255);
							break;
						}
						Achievements.increase(player, AchievementType.SLAY, 1);
					}
				});
			}
		});
	}

	/**
	 * Retains an array of {@link Task} objects that the player can operate with
	 * the required slayer level.
	 * 
	 * @param tasks
	 *            the original array of tasks
	 * @return the retained array of obtainable tasks
	 */
	private Task[] retainObtainable(Task[] tasks) {
		List<Task> retainable = new ArrayList<>();
		List<String> blocked = new ArrayList<>(Arrays.asList(removed));

		for (int index = 0; index < tasks.length; index++) {
			if (tasks[index].getLevel() <= player.playerLevel[Skill.SLAYER.getId()]
					&& !blocked.contains(tasks[index].getPrimaryName())
					|| tasks[index].getPrimaryName() == "cerberus" && learnedCerberusRoute) {
				retainable.add(tasks[index]);
			}
		}

		return retainable.toArray(new Task[retainable.size()]);
	}

	public void handleInterface(String shop) {
		if (shop.equalsIgnoreCase("buy")) {
			player.getPA().sendFrame126("Slayer Points: " + points, 41011);
			player.getPA().showInterface(41000);
		} else if (shop.equalsIgnoreCase("learn")) {
			player.getPA().sendFrame126("Slayer Points: " + points, 41511);
			player.getPA().showInterface(41500);
		} else if (shop.equalsIgnoreCase("assignment")) {
			player.getPA().sendFrame126("Slayer Points: " + points, 42011);
			updateCurrentlyRemoved();
			player.getPA().showInterface(42000);
		}
	}

	public void cancelTask() {
		int rankPoints = player.getRights().getPrimary() == Right.MEGA_VIP ? 20
				: player.getRights().getPrimary() == Right.LEGENDARY ? 10 : 30;

		if (!task.isPresent()) {
			player.sendMessage("You must have a task to cancel first.");
			return;
		}
		if (task.get().getPrimaryName().equals("cerberus") && !learnedCerberusRoute) {
			player.sendMessage("You have cancelled your current task of " + taskAmount + " "
					+ task.get().getPrimaryName() + " for free.");
			task = Optional.empty();
			taskAmount = 0;
			return;
		}
		if (points < rankPoints) {
			player.sendMessage("This requires atleast " + rankPoints + " slayer points, which you don't have.");
			return;
		}
		player.sendMessage(
				"You have cancelled your current task of " + taskAmount + " " + task.get().getPrimaryName() + ".");
		task = Optional.empty();
		taskAmount = 0;
		points -= rankPoints;
	}

	public void removeTask() {
		int rankPoints = player.getRights().getPrimary() == Right.MEGA_VIP ? 70
				: player.getRights().getPrimary() == Right.LEGENDARY ? 50 : 100;
		if (!task.isPresent()) {
			player.sendMessage("You must have a task to remove first.");
			return;
		}
		if (points < rankPoints) {
			player.sendMessage("This requires atleast " + rankPoints + " slayer points, which you don't have.");
			return;
		}
		for (int index = 0; index < removed.length; index++) {
			if (!removed[index].isEmpty()) {
				continue;
			}
			if (index == 4) {
				player.sendMessage("You don't have any open slots left to remove tasks.");
				return;
			}
			if (removed[index].isEmpty()) {
				removed[index] = task.get().getPrimaryName();
				points -= rankPoints;
				task = Optional.empty();
				taskAmount = 0;
				player.sendMessage("Your current slayer task has been removed, you can't obtain this task again.");
				updateCurrentlyRemoved();
				updatePoints();
				return;
			}
		}
	}

	public void updatePoints() {
		player.getPA().sendFrame126("Slayer Points: " + points, 41011);
		player.getPA().sendFrame126("Slayer Points: " + points, 41511);
		player.getPA().sendFrame126("Slayer Points: " + points, 42011);
		player.getPA().sendFrame126("@red@Slayer Points: @or2@" + points, 7336);
	}

	public void updateCurrentlyRemoved() {
		for (int index = 0; index < removed.length; index++) {
			if (removed[index].isEmpty()) {
				player.getPA().sendFrame126("", 42014 + index);
			} else {
				player.getPA().sendFrame126(removed[index], 42014 + index);
			}
		}
	}

	public boolean onActionButton(int actionId) {
		switch (actionId) {

		case 160052:
			int amount = player.getMode().isOsrs() ? 10_000 : 60_000;
			if (System.currentTimeMillis() - player.buySlayerTimer < 500) {
				return true;
			}
			if (points < 50) {
				player.sendMessage("You need at least 50 slayer points to gain " + amount + " Experience.");
				return true;
			}
			player.buySlayerTimer = System.currentTimeMillis();
			points -= 50;
			player.getPA().addSkillXP(amount, 18, true);
			player.sendMessage("You spend 50 slayer points and gain " + amount + " experience in slayer.");
			updatePoints();
			return true;

		case 160054:
			if (System.currentTimeMillis() - player.buySlayerTimer < 500) {
				return true;
			}
			if (points < 35) {
				player.sendMessage("You need at least 35 slayer points to buy Slayer darts.");
				return true;
			}
			if (player.getItems().freeSlots() < 2 && !player.getItems().playerHasItem(560)
					&& !player.getItems().playerHasItem(558)) {
				player.sendMessage("You need at least 2 free lots to purchase this.");
				return true;
			}
			player.buySlayerTimer = System.currentTimeMillis();
			points -= 35;
			player.sendMessage("You spend 35 slayer points and aquire 250 casts of Slayer darts.");
			player.getItems().addItem(558, 1000);
			player.getItems().addItem(560, 250);
			updatePoints();
			return true;

		case 160055:
			if (System.currentTimeMillis() - player.buySlayerTimer < 500) {
				return true;
			}
			if (points < 25) {
				player.sendMessage("You need at least 25 slayer points to buy Broad arrows.");
				return true;
			}
			if (player.getItems().freeSlots() < 1 && !player.getItems().playerHasItem(4160)) {
				player.sendMessage("You need at least 1 free lot to purchase this.");
				return true;
			}
			player.buySlayerTimer = System.currentTimeMillis();
			points -= 25;
			player.sendMessage("You spend 35 slayer points and aquire 250 Broad arrows.");
			player.getItems().addItem(4160, 250);
			updatePoints();
			return true;

		case 160053:
			// if (System.currentTimeMillis() - player.buySlayerTimer < 1000) {
			// return true;
			// }
			// if (points < 25) {
			// player.sendMessage("You need at least 25 slayer points to buy
			// Slayer's respite.");
			// return true;
			// }
			// if (player.getItems().freeSlots() < 1) {
			// player.sendMessage("You need at least 1 free lot to purchase
			// this.");
			// return true;
			// }
			// player.buySlayerTimer = System.currentTimeMillis();
			// points -= 25;
			// player.sendMessage("You spend 25 slayer points and aquire a
			// useful Slayer's respite.");
			// player.getItems().addItem(5759, 1);
			// updatePoints();
			player.sendMessage("You cannot purchase this at the moment.");
			return true;

		case 160056:
			if (System.currentTimeMillis() - player.buySlayerTimer < 1000) {
				return true;
			}
			int slayerHelmet = 11864;
			int slayerHelmetI = 11865;
			// TODO: make sure you can imbue colored helmets as well
			if (!helmetImbuedCreatable) {
				player.sendMessage("You must know how to create an imbued slayer helmet to do this.");
				return true;
			}
			if (!player.getItems().playerHasItem(11864) && !player.getItems().playerHasItem(19639)
					&& !player.getItems().playerHasItem(19643) && !player.getItems().playerHasItem(19647)) {
				player.sendMessage("You need a slayer helmet in your inventory to do this.");
				return true;
			}
			if (player.getItems().playerHasItem(11864)) {
				slayerHelmet = 11864;
				slayerHelmetI = 11865;
			}
			if (player.getItems().playerHasItem(19639)) {
				slayerHelmet = 19639;
				slayerHelmetI = 19641;
			}
			if (player.getItems().playerHasItem(19643)) {
				slayerHelmet = 19643;
				slayerHelmetI = 19645;
			}
			if (player.getItems().playerHasItem(19647)) {
				slayerHelmet = 19647;
				slayerHelmetI = 19649;
			}
			player.getItems().deleteItem2(slayerHelmet, 1);
			player.getItems().addItem(slayerHelmetI, 1);
			player.buySlayerTimer = System.currentTimeMillis();
			player.sendMessage("You imbue the slayer helmet and create an imbued slayer helmet.");
			return true;

		case 160057:
			if (System.currentTimeMillis() - player.buySlayerTimer < 3000) {
				return true;
			}
			if (biggerBossTasks) {
				player.getDH().sendDialogues(75, 6797);
				return true;
			}
			if (points < 100) {
				player.sendMessage("You need 100 slayer points to extend boss tasks.");
				return true;
			}
			points -= 100;
			biggerBossTasks = true;
			player.buySlayerTimer = System.currentTimeMillis();
			player.sendMessage("You will now get extended boss tasks.");
			updatePoints();
			return true;

		case 162040:
			if (helmetCreatable) {
				player.sendMessage("You already know how to create a slayer helmet.");
				return true;
			}
			if (points < 350) {
				player.sendMessage("You need 350 slayer points to learn this.");
				return true;
			}
			helmetCreatable = true;
			points -= 350;
			player.getDH().sendDialogues(667, 402);
			updatePoints();
			return true;

		case 162041:
			if (!helmetCreatable) {
				player.sendMessage("You need to learn how to create a regular slayer helmet before doing this.");
				return true;
			}
			if (helmetImbuedCreatable) {
				player.sendMessage("You already know how to create an imbued slayer helmet.");
				return true;
			}
			if (points < 150) {
				player.sendMessage("You need 150 slayer points to learn this.");
				return true;
			}
			helmetImbuedCreatable = true;
			points -= 150;
			player.getDH().sendDialogues(668, 402);
			updatePoints();
			return true;

		case 162042:
			if (learnedCerberusRoute) {
				player.sendMessage("You already know this.");
				return false;
			}
			if (points < 1250) {
				player.sendMessage("You need 1250 slayer points to learn this.");
				return true;
			}
			points -= 1250;
			learnedCerberusRoute = true;
			player.buySlayerTimer = System.currentTimeMillis();
			player.sendMessage("You've successfully learned the route to cerberus.");
			updatePoints();
			return true;

		case 40132:
			setColor("black");
			player.sendMessage("Color chosen: Black");
			return true;

		case 40133:
			setColor("green");
			player.sendMessage("Color chosen: Green");
			return true;

		case 25155:
			setColor("red");
			player.sendMessage("Color chosen: Red");
			return true;

		case 25160:
			setColor("revert");
			player.sendMessage("Color chosen: Revert");
			return true;

		case 160058:
			player.getPA().showInterface(10294);
			return true;

		case 40122:
			if (getColor() == null) {
				player.sendMessage("Please choose a color.");
				return false;
			}
			if (!player.getItems().playerHasItem(SLAYER_HELMETS.REVERT.getRegular())
					&& !player.getItems().playerHasItem(SLAYER_HELMETS.REVERT.getImbued())) {
				player.sendMessage("You must have a slayer helmet to color.");
				return false;
			}
			if (getColor() == "revert") {
				player.sendMessage("Currently you must do this by right clicking the item in question.");
				return false;
			}
			if (getPoints() < 1000) {
				player.sendMessage("You do not have enough slayer points to do this.");
				return false;
			}
			if (player.getItems().playerHasItem(SLAYER_HELMETS.REVERT.getRegular())) {
				switch (getColor()) {
				case "black":
					if (player.getItems().playerHasItem(SLAYER_HELMETS.BLACK.getHead(), 1)) {
						player.getItems().deleteItem(SLAYER_HELMETS.REVERT.getRegular(), 1);
						player.getItems().deleteItem(SLAYER_HELMETS.BLACK.getHead(), 1);
						player.getItems().addItem(SLAYER_HELMETS.BLACK.getRegular(), 1);
						points -= 1000;
					} else {
						player.sendMessage("You need a KBD Head to do this.");
						return false;
					}
					break;
				case "green":
					if (player.getItems().playerHasItem(SLAYER_HELMETS.GREEN.getHead(), 1)) {
						player.getItems().deleteItem(SLAYER_HELMETS.REVERT.getRegular(), 1);
						player.getItems().deleteItem(SLAYER_HELMETS.GREEN.getHead(), 1);
						player.getItems().addItem(SLAYER_HELMETS.GREEN.getRegular(), 1);
						points -= 1000;
					} else {
						player.sendMessage("You need a KQ Head to do this.");
						return false;
					}
					break;
				case "red":
					if (player.getItems().playerHasItem(SLAYER_HELMETS.RED.getHead(), 1)) {
						player.getItems().deleteItem(SLAYER_HELMETS.REVERT.getRegular(), 1);
						player.getItems().deleteItem(SLAYER_HELMETS.RED.getHead(), 1);
						player.getItems().addItem(SLAYER_HELMETS.RED.getRegular(), 1);
						points -= 1000;
					} else {
						player.sendMessage("You need an Abyssal Head to do this.");
						return false;
					}
					break;
				}
			} else if (player.getItems().playerHasItem(SLAYER_HELMETS.REVERT.getImbued())) {
				switch (getColor()) {
				case "black":
					if (player.getItems().playerHasItem(SLAYER_HELMETS.BLACK.getHead(), 1)) {
						player.getItems().deleteItem(SLAYER_HELMETS.REVERT.getImbued(), 1);
						player.getItems().deleteItem(SLAYER_HELMETS.BLACK.getHead(), 1);
						player.getItems().addItem(SLAYER_HELMETS.BLACK.getImbued(), 1);
						points -= 1000;
					} else {
						player.sendMessage("You need a KBD Head to do this.");
						return false;
					}
					break;
				case "green":
					if (player.getItems().playerHasItem(SLAYER_HELMETS.GREEN.getHead(), 1)) {
						player.getItems().deleteItem(SLAYER_HELMETS.REVERT.getImbued(), 1);
						player.getItems().deleteItem(SLAYER_HELMETS.GREEN.getHead(), 1);
						player.getItems().addItem(SLAYER_HELMETS.GREEN.getImbued(), 1);
						points -= 1000;
					} else {
						player.sendMessage("You need a KQ Head to do this.");
						return false;
					}
					break;
				case "red":
					if (player.getItems().playerHasItem(SLAYER_HELMETS.RED.getHead(), 1)) {
						player.getItems().deleteItem(SLAYER_HELMETS.REVERT.getImbued(), 1);
						player.getItems().deleteItem(SLAYER_HELMETS.RED.getHead(), 1);
						player.getItems().addItem(SLAYER_HELMETS.RED.getImbued(), 1);
						points -= 1000;
					} else {
						player.sendMessage("You need an Abyssal Head to do this.");
						return false;
					}
					break;
				}
			}
			return true;
		}
		return false;
	}

	public void revertHelmet(int helmet) {
		System.out.println("Name: " + ItemAssistant.getItemName(helmet));
		if (ItemAssistant.getItemName(helmet).contains("(i)")) {
			player.getItems().deleteItem(helmet, 1);
			player.getItems().addItem(SLAYER_HELMETS.REVERT.getImbued(), 1);
		} else {
			player.getItems().deleteItem(helmet, 1);
			player.getItems().addItem(SLAYER_HELMETS.REVERT.getRegular(), 1);
		}
		player.sendMessage("You successfully reverted your slayer helmet to normal.");
	}

	private enum SLAYER_HELMETS {
		BLACK(19639, 19641, 7980), GREEN(19643, 19645, 7981), RED(19647, 19649, 7979), REVERT(11864, 11865, -1);

		private int regular, imbued, head;

		public int getRegular() {
			return regular;
		}

		public int getImbued() {
			return imbued;
		}

		public int getHead() {
			return head;
		}

		SLAYER_HELMETS(int regular, int imbued, int head) {
			this.regular = regular;
			this.imbued = imbued;
			this.head = head;
		}
	}

	/**
	 * Modifies the current amount of slayer points the player has
	 * 
	 * @param points
	 *            the amount of points
	 */
	public void setPoints(int points) {
		this.points = points;
	}

	/**
	 * The amount of points the player has in slayer
	 * 
	 * @return the amount of points
	 */
	public int getPoints() {
		return points;
	}

	/**
	 * The amount of the slayer task the player has remaining
	 * 
	 * @return the amount of the task
	 */
	public int getTaskAmount() {
		return taskAmount;
	}

	/**
	 * Modifies the amount of a slayer task the player has
	 * 
	 * @param taskAmount
	 *            the new task amount
	 */
	public void setTaskAmount(int taskAmount) {
		this.taskAmount = taskAmount;
	}

	/**
	 * The identification value of the slayer master this player goes to
	 * 
	 * @return the identification value of the slayer master
	 */
	public int getMaster() {
		return master;
	}

	/**
	 * Modifies the variable that represents the master the player goes to
	 * 
	 * @param master
	 *            the new slayer master
	 */
	public void setMaster(int master) {
		this.master = master;
	}

	/**
	 * The amount of tasks that a player has completed from the same master
	 * consecutively
	 * 
	 * @return
	 */
	public int getConsecutiveTasks() {
		return consecutiveTasks;
	}

	/**
	 * Modifies the current amount of consecutive tasks completed.
	 * 
	 * @param consecutiveTasks
	 *            the amount of consecutive tasks completed
	 */
	public void setConsecutiveTasks(int consecutiveTasks) {
		this.consecutiveTasks = consecutiveTasks;
	}

	/**
	 * Modifies the currently removed tasks
	 * 
	 * @param removed
	 *            the new removed tasks
	 */
	public void setRemoved(String[] removed) {
		this.removed = removed;
	}

	/**
	 * The array of task names that are removed
	 * 
	 * @return the removed tasks
	 */
	public String[] getRemoved() {
		return removed;
	}

	/**
	 * The slayer task that this player currently has assigned
	 * 
	 * @return the task the player has assigned
	 */
	public Optional<Task> getTask() {
		return task;
	}

	/**
	 * Sets the current task to that of the parameter. The current task is the
	 * task the player has received from their slayer master.
	 * 
	 * @param task
	 *            the new slayer task
	 */
	public void setTask(Optional<Task> task) {
		this.task = task;
	}

	/**
	 * Determines if the basic slayer helmet is creatable.
	 * 
	 * @return {@code true} if the player has learned how to create the helmet.
	 */
	public boolean isHelmetCreatable() {
		return helmetCreatable;
	}

	/**
	 * Sets the state of the basic helmet being creatable to that of the
	 * parameter
	 * 
	 * @param helmetCreatable
	 *            whether or not the helmet is creatable
	 */
	public void setHelmetCreatable(boolean helmetCreatable) {
		this.helmetCreatable = helmetCreatable;
	}

	/**
	 * Determines if the advanced slayer helmet (imbued) can be created;
	 * 
	 * @return {@code true} if the player has learned how to create the helmet
	 *         (imbued).
	 */
	public boolean isHelmetImbuedCreatable() {
		return helmetImbuedCreatable;
	}

	/**
	 * Sets the state of the helmet imbued being creatable to that of the
	 * parameter
	 * 
	 * @param helmetImbuedCreatable
	 *            the state of the helmet being creatable
	 */
	public void setHelmetImbuedCreatable(boolean helmetImbuedCreatable) {
		this.helmetImbuedCreatable = helmetImbuedCreatable;
	}

	public boolean isBiggerBossTasks() {
		return biggerBossTasks;
	}

	public void setBiggerBossTasks(boolean biggerBossTasks) {
		this.biggerBossTasks = biggerBossTasks;
	}

	public boolean isCerberusRoute() {
		return learnedCerberusRoute;
	}

	public void setCerberusRoute(boolean learnedCerberusRoute) {
		this.learnedCerberusRoute = learnedCerberusRoute;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

}
