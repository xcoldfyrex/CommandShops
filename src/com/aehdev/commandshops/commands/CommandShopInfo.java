package com.aehdev.commandshops.commands;

import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aehdev.commandshops.CommandShops;
import com.aehdev.commandshops.InventoryItem;
import com.aehdev.commandshops.PlayerData;
import com.aehdev.commandshops.Search;
import com.aehdev.commandshops.Shop;

// TODO: Auto-generated Javadoc
/**
 * The Class CommandShopInfo.
 */
public class CommandShopInfo extends Command
{

	/**
	 * Instantiates a new command shop info.
	 * @param plugin
	 * the plugin
	 * @param commandLabel
	 * the command label
	 * @param sender
	 * the sender
	 * @param command
	 * the command
	 */
	public CommandShopInfo(CommandShops plugin, String commandLabel,
			CommandSender sender, String command)
	{
		super(plugin, commandLabel, sender, command);
	}

	/**
	 * Instantiates a new command shop info.
	 * @param plugin
	 * the plugin
	 * @param commandLabel
	 * the command label
	 * @param sender
	 * the sender
	 * @param command
	 * the command
	 */
	public CommandShopInfo(CommandShops plugin, String commandLabel,
			CommandSender sender, String[] command)
	{
		super(plugin, commandLabel, sender, command);
	}

	/* (non-Javadoc)
	 * @see com.aehdev.commandshops.commands.Command#process() */
	public boolean process()
	{
		Shop shop = null;

		// Get current shop
		if(sender instanceof Player)
		{
			// Get player & data
			Player player = (Player)sender;
			PlayerData pData = plugin.getPlayerData().get(player.getName());

			// info (player only command)
			Pattern pattern = Pattern.compile("(?i)info$");
			Matcher matcher = pattern.matcher(command);
			if(matcher.find())
			{
				// Get Current Shop
				UUID shopUuid = pData.getCurrentShop();
				if(shopUuid != null)
				{
					shop = plugin.getShopData().getShop(shopUuid);
				}
				if(shop == null)
				{
					sender.sendMessage("You are not in a shop!");
					return false;
				}
			}

			// info id
			matcher.reset();
			pattern = Pattern.compile("(?i)info\\s+(.*)$");
			matcher = pattern.matcher(command);
			if(matcher.find())
			{
				String input = matcher.group(1);
				shop = plugin.getShopData().getShop(input);
				if(shop == null)
				{
					sender.sendMessage("Could not find shop with ID " + input);
					return false;
				}
			}

		}else
		{
			sender.sendMessage("Console is not implemented yet.");
			return false;
		}

		int managerCount = shop.getManagers().size();

		sender.sendMessage(String.format(ChatColor.DARK_AQUA
				+ "Shop Info about " + ChatColor.WHITE + "\"%s\""
				+ ChatColor.DARK_AQUA + " ID: " + ChatColor.WHITE + "%s",
				shop.getName(), shop.getShortUuidString()));
		if(shop.getCreator().equalsIgnoreCase(shop.getOwner()))
		{
			if(managerCount == 0)
			{
				sender.sendMessage(String.format(
						"  Owned & Created by %s with no managers.",
						shop.getCreator()));
			}else
			{
				sender.sendMessage(String.format(
						"  Owned & Created by %s with %d managers.",
						shop.getCreator(), managerCount));
			}
		}else
		{
			if(managerCount == 0)
			{
				sender.sendMessage(String.format(
						"  Owned by %s, created by %s with no managers.",
						shop.getOwner(), shop.getCreator()));
			}else
			{
				sender.sendMessage(String.format(
						"  Owned by %s created by %s with %d managers.",
						shop.getOwner(), shop.getCreator(), managerCount));
			}
		}
		if(managerCount > 0)
		{
			sender.sendMessage(String.format("  Managed by %s",
					Search.join(shop.getManagers(), " ")));
		}

		if(command.matches("info\\s+full"))
		{
			sender.sendMessage(String.format("  Full Id: %s", shop.getUuid()
					.toString()));
		}

		sender.sendMessage(String.format("  Located at %s x %s in \"%s\"", shop
				.getLocationA().toString(), shop.getLocationB().toString(),
				shop.getWorld()));

		// Calculate values
		int sellCount = 0;
		int buyCount = 0;
		int worth = 0;

		Iterator<InventoryItem> it = shop.getItems().iterator();
		while(it.hasNext())
		{
			InventoryItem i = it.next();
			if(i.getBuyPrice() > 0)
			{
				sellCount++;
				worth += (i.getStock() / i.getBuySize()) * i.getBuyPrice();
			}

			if(i.getSellPrice() > 0)
			{
				buyCount++;
			}
		}

		// Selling %d items & buying %d items
		sender.sendMessage(String.format(
				"  Selling %d items & buying %d items", sellCount, buyCount));

		// Shop stock is worth %d coins
		sender.sendMessage(String.format("  Inventory worth %s", plugin
				.getEconManager().format(worth)));

		if(shop.isUnlimitedMoney() || shop.isUnlimitedStock())
		{
			sender.sendMessage(String.format(
					"  Shop %s unlimited money and %s unlimited stock.",
					shop.isUnlimitedMoney() ? "has" : "doesn't have",
					shop.isUnlimitedStock() ? "has" : "doesn't have"));
		}

		return true;
	}
}