package com.aehdev.commandshops;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import cuboidLocale.BookmarkedResult;
import cuboidLocale.PrimitiveCuboid;
import cuboidLocale.QuadTree;

// TODO: Auto-generated Javadoc
/**
 * The Class ShopData.
 */
public class ShopData
{

	/** The plugin. */
	private CommandShops plugin = null;

	/** The shops. */
	private HashMap<UUID,Shop> shops = new HashMap<UUID,Shop>();

	// Logging
	/** The log. */
	private final Logger log = Logger.getLogger("Minecraft");

	/**
	 * Instantiates a new shop data.
	 * @param plugin
	 * the plugin
	 */
	public ShopData(CommandShops plugin)
	{
		this.plugin = plugin;
	}

	/**
	 * Gets the shop.
	 * @param uuid
	 * the uuid
	 * @return the shop
	 */
	public Shop getShop(UUID uuid)
	{
		return shops.get(uuid);
	}

	/**
	 * Gets the shop.
	 * @param partialUuid
	 * the partial uuid
	 * @return the shop
	 */
	public Shop getShop(String partialUuid)
	{
		Iterator<Shop> it = shops.values().iterator();
		while(it.hasNext())
		{
			Shop cShop = it.next();
			if(cShop.getUuid().toString()
					.matches(".*" + partialUuid.toLowerCase() + "$")){ return cShop; }
		}

		return null;
	}

	/**
	 * Adds the shop.
	 * @param shop
	 * the shop
	 */
	public void addShop(Shop shop)
	{
		if(Config.DEBUG)
		{
			log.info(String.format("[%s] Adding %s", plugin.pdfFile.getName(),
					shop.toString()));
		}
		String uuid = shop.getUuid().toString();
		while(true)
		{
			if(Config.UUID_LIST.contains(uuid.substring(uuid.length()
					- Config.UUID_MIN_LENGTH)))
			{
				calcShortUuidSize();
			}else
			{
				Config.UUID_LIST.add(uuid.substring(uuid.length()
						- Config.UUID_MIN_LENGTH));
				break;
			}
		}
		shops.put(shop.getUuid(), shop);
	}

	/**
	 * Calc short uuid size.
	 */
	private void calcShortUuidSize()
	{
		if(Config.UUID_MIN_LENGTH < 36)
		{
			Config.UUID_MIN_LENGTH++;
		}
		Config.UUID_LIST.clear();
		Iterator<Shop> it = shops.values().iterator();
		while(it.hasNext())
		{
			Shop cShop = it.next();
			String cUuid = cShop.getUuid().toString();
			String sUuid = cUuid.substring(cUuid.length()
					- Config.UUID_MIN_LENGTH);
			if(Config.UUID_LIST.contains(sUuid))
			{
				calcShortUuidSize();
			}else
			{
				Config.UUID_LIST.add(sUuid);
			}
		}
	}

	/**
	 * Gets the all shops.
	 * @return the all shops
	 */
	public List<Shop> getAllShops()
	{
		return new ArrayList<Shop>(shops.values());
	}

	/**
	 * Gets the num shops.
	 * @return the num shops
	 */
	public int getNumShops()
	{
		return shops.size();
	}

	/**
	 * Num owned shops.
	 * @param playerName
	 * the player name
	 * @return the int
	 */
	public int numOwnedShops(String playerName)
	{
		int numShops = 0;
		for(Shop shop: shops.values())
		{
			if(shop.getOwner().equals(playerName))
			{
				numShops++;
			}
		}
		return numShops;
	}

	/**
	 * Load shops.
	 * @param shopsDir
	 * the shops dir
	 */
	public void loadShops(File shopsDir)
	{
		if(Config.DEBUG)
		{
			log.info(String.format("[%s] %s.%s", plugin.pdfFile.getName(),
					"ShopData", "loadShops(File shopsDir)"));
		}

		CommandShops.setCuboidTree(new QuadTree());

		File[] shopsList = shopsDir.listFiles();
		for(File file: shopsList)
		{

			if(Config.DEBUG)
			{
				log.info(String.format("[%s] Loading Shop file \"%s\".",
						plugin.pdfFile.getName(), file.toString()));
			}
			Shop shop = null;

			// Determine if filename is a UUID or not
			if(file.getName()
					.matches(
							"^(\\{{0,1}([0-9a-fA-F]){8}-([0-9a-fA-F]){4}-([0-9a-fA-F]){4}-([0-9a-fA-F]){4}-([0-9a-fA-F]){12}\\}{0,1})\\.shop$"))
			{
				try
				{
					shop = loadShop(file);
				}catch(Exception e)
				{
					// log error
					log.info(String.format(
							"[%s] Error loading Shop file \"%s\", ignored.",
							plugin.pdfFile.getName(), file.toString()));
				}
			}else
			{
				// Convert old format & delete the file...immediately save using
				// the new format (will generate a new UUID for this shop)
				shop = convertShopOldFormat(file);
			}

			// Check if not null, and add to world
			if(shop != null)
			{
				if(Config.DEBUG)
				{
					log.info(String.format("[%s] Loaded %s",
							plugin.pdfFile.getName(), shop.toString()));
				}
				CommandShops.getCuboidTree().insert(shop.getCuboid());
				plugin.getShopData().addShop(shop);
			}else
			{
				log.warning(String.format(
						"[%s] Failed to load Shop file: \"%s\"",
						plugin.pdfFile.getName(), file.getName()));
			}
		}

	}

	/**
	 * Convert shop old format.
	 * @param file
	 * the file
	 * @return the shop
	 */
	public Shop convertShopOldFormat(File file)
	{
		if(Config.DEBUG)
		{
			log.info(String.format("[%s] %s.%s", plugin.pdfFile.getName(),
					"ShopData", "loadShopOldFormat(File file)"));
		}

		try
		{
			// Create new empty shop (this format has no UUID, so generate one)
			Shop shop = new Shop(UUID.randomUUID());

			// Retrieve Shop Name (from filename)
			shop.setName(file.getName().split("\\.")[0]);

			// Open file & iterate over lines
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			while(line != null)
			{
				if(Config.DEBUG)
				{
					log.info(String.format("[%s] %s", plugin.pdfFile.getName(),
							line));
				}

				// Skip comment lines / metadata
				if(line.startsWith("#"))
				{
					line = br.readLine();
					continue;
				}

				// Data is separated by =
				String[] cols = line.split("=");

				// Check if there are enough columns (needs key and value)
				if(cols.length < 2)
				{
					line = br.readLine();
					continue;
				}

				if(cols[0].equalsIgnoreCase("world"))
				{ // World
					shop.setWorld(cols[1]);
				}else if(cols[0].equalsIgnoreCase("owner"))
				{ // Owner
					shop.setOwner(cols[1]);
				}else if(cols[0].equalsIgnoreCase("managers"))
				{ // Managers
					String[] managers = cols[1].split(",");
					shop.setManagers(managers);
				}else if(cols[0].equalsIgnoreCase("creator"))
				{ // Creator
					shop.setCreator(cols[1]);
				}else if(cols[0].equalsIgnoreCase("position1"))
				{ // Position
					// A
					String[] xyzStr = cols[1].split(",");
					try
					{
						long x = Long.parseLong(xyzStr[0].trim());
						long y = Long.parseLong(xyzStr[1].trim());
						long z = Long.parseLong(xyzStr[2].trim());

						ShopLocation loc = new ShopLocation(x, y, z);
						shop.setLocationA(loc);
					}catch(NumberFormatException e)
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Location Data, Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Location Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}
						return null;
					}
				}else if(cols[0].equalsIgnoreCase("position2"))
				{ // Position
					// B
					String[] xyzStr = cols[1].split(",");
					try
					{
						long x = Long.parseLong(xyzStr[0].trim());
						long y = Long.parseLong(xyzStr[1].trim());
						long z = Long.parseLong(xyzStr[2].trim());

						ShopLocation loc = new ShopLocation(x, y, z);
						shop.setLocationB(loc);
					}catch(NumberFormatException e)
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Location Data, Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Location Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}
						return null;
					}
				}else if(cols[0].equalsIgnoreCase("unlimited-money"))
				{ // Unlimited
					// Money
					shop.setUnlimitedMoney(Boolean.parseBoolean(cols[1]));
				}else if(cols[0].equalsIgnoreCase("unlimited-stock"))
				{ // Unlimited
					// Stock
					shop.setUnlimitedStock(Boolean.parseBoolean(cols[1]));
				}else if(cols[0].matches("\\d+:\\d+"))
				{ // Items
					String[] itemInfo = cols[0].split(":");
					if(itemInfo.length < 2)
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}
						return null;
					}
					int itemId = Integer.parseInt(itemInfo[0]);
					short damageMod = Short.parseShort(itemInfo[1]);

					String[] dataCols = cols[1].split(",");
					if(dataCols.length < 3)
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}
						return null;
					}

					String[] buyInfo = dataCols[0].split(":");
					if(buyInfo.length < 2)
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}
						return null;
					}
					int buyPrice = Integer.parseInt(buyInfo[0]);
					int buySize = Integer.parseInt(buyInfo[1]);

					String[] sellInfo = dataCols[1].split(":");
					if(sellInfo.length < 2)
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}
						return null;
					}
					int sellPrice = Integer.parseInt(sellInfo[0]);
					int sellSize = Integer.parseInt(sellInfo[1]);

					String[] stockInfo = dataCols[2].split(":");
					if(stockInfo.length < 2)
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString()));
						}
						return null;
					}
					int stock = Integer.parseInt(stockInfo[0]);
					int maxStock = Integer.parseInt(stockInfo[1]);

					if(!shop.addItem(itemId, damageMod, buyPrice, buySize,
							sellPrice, sellSize, stock, maxStock))
					{
						if(isolateBrokenShopFile(file))
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data (%d:%d), Moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString(), itemId, damageMod));
						}else
						{
							log.warning(String
									.format("[%s] Shop File \"%s\" has bad Item Data (%d:%d), Error moving to \"plugins/CommandShops/broken-shops/\"",
											plugin.pdfFile.getName(),
											file.toString(), itemId, damageMod));
						}
						return null;
					}
				}else
				{ // Not defined
					log.info(String
							.format("[%s] Shop File \"%s\" has undefined data, ignoring.",
									plugin.pdfFile.getName(), file.toString()));
				}
				line = br.readLine();
			}

			br.close();

			File dir = new File("plugins/CommandShops/shops-converted/");
			dir.mkdir();
			if(file.renameTo(new File(dir, file.getName())))
			{
				file.delete();
				return shop;
			}else
			{
				return null;
			}

		}catch(IOException e)
		{
			if(isolateBrokenShopFile(file))
			{
				log.warning(String
						.format("[%s] Shop File \"%s\" Exception: %s, Moving to \"plugins/CommandShops/broken-shops/\"",
								plugin.pdfFile.getName(), file.toString(),
								e.toString()));
			}else
			{
				log.warning(String
						.format("[%s] Shop File \"%s\" Exception: %s, Error moving to \"plugins/CommandShops/broken-shops/\"",
								plugin.pdfFile.getName(), file.toString(),
								e.toString()));
			}
			return null;
		}
	}

	/**
	 * Convert string arrayto double array.
	 * @param sarray
	 * the sarray
	 * @return the double[]
	 */
	public static double[] convertStringArraytoDoubleArray(String[] sarray)
	{
		if(sarray != null)
		{
			double longArray[] = new double[sarray.length];
			for(int i = 0; i < sarray.length; i++)
			{
				longArray[i] = Long.parseLong(sarray[i]);
			}
			return longArray;
		}
		return null;
	}

	/**
	 * Load shop.
	 * @param file
	 * the file
	 * @return the shop
	 * @throws Exception
	 * the exception
	 */
	public Shop loadShop(File file) throws Exception
	{
		SortedProperties props = new SortedProperties();
		try
		{
			props.load(new FileInputStream(file));
		}catch(IOException e)
		{
			log.warning(String.format("[%s] %s", plugin.pdfFile.getName(),
					"IOException: " + e.getMessage()));
			return null;
		}

		// Shop attributes
		UUID uuid = UUID.fromString(props.getProperty("uuid",
				"00000000-0000-0000-0000-000000000000"));
		String name = props.getProperty("name", "Nameless Shop");
		boolean unlimitedMoney = Boolean.parseBoolean(props.getProperty(
				"unlimited-money", "false"));
		boolean unlimitedStock = Boolean.parseBoolean(props.getProperty(
				"unlimited-stock", "false"));
		double minBalance = Double.parseDouble((props.getProperty(
				"min-balance", "0.0")));
		boolean notification = Boolean.parseBoolean(props.getProperty(
				"notification", "true"));

		// Location - locationB=-88, 50, -127
		double[] locationA;
		double[] locationB;
		String world;
		try
		{
			locationA = convertStringArraytoDoubleArray(props.getProperty(
					"locationA").split(", "));
			locationB = convertStringArraytoDoubleArray(props.getProperty(
					"locationB").split(", "));
			world = props.getProperty("world", "world1");
		}catch(Exception e)
		{
			if(isolateBrokenShopFile(file))
			{
				log.warning(String
						.format("[%s] Shop File \"%s\" has bad Location Data, Moving to \"plugins/CommandShops/broken-shops/\"",
								plugin.pdfFile.getName(), file.toString()));
			}else
			{
				log.warning(String
						.format("[%s] Shop File \"%s\" has bad Location Data, Error moving to \"plugins/CommandShops/broken-shops/\"",
								plugin.pdfFile.getName(), file.toString()));
			}
			return null;
		}

		// People
		String owner = props.getProperty("owner", "");
		String[] managers = props.getProperty("managers", "")
				.replaceAll("[\\[\\]]", "").split(", ");
		String creator = props.getProperty("creator", "CommandShops");

		Shop shop = new Shop(uuid);
		shop.setName(name);
		shop.setUnlimitedMoney(unlimitedMoney);
		shop.setUnlimitedStock(unlimitedStock);
		shop.setLocationA(new ShopLocation(locationA));
		shop.setLocationB(new ShopLocation(locationB));
		shop.setWorld(world);
		shop.setOwner(owner);
		shop.setManagers(managers);
		shop.setCreator(creator);
		shop.setNotification(notification);

		// Make sure minimum balance isn't negative
		if(minBalance < 0)
		{
			shop.setMinBalance(0);
		}else
		{
			shop.setMinBalance(minBalance);
		}

		// Iterate through all keys, find items & parse
		// props.setProperty(String.format("%d:%d", info.typeId,
		// info.subTypeId), String.format("%d:%d,%d:%d,%d:%d", buyPrice,
		// buySize, sellPrice, sellSize, stock, maxStock));
		Iterator<Object> it = props.keySet().iterator();
		while(it.hasNext())
		{
			String key = (String)it.next();
			if(key.matches("\\d+:\\d+"))
			{
				String[] k = key.split(":");
				int id = Integer.parseInt(k[0]);
				short type = Short.parseShort(k[1]);

				String value = props.getProperty(key);
				String[] v = value.split(",");

				String[] buy = v[0].split(":");
				double buyPrice = Double.parseDouble(buy[0]);
				int buyStackSize = Integer.parseInt(buy[1]);

				String[] sell = v[1].split(":");
				double sellPrice = Double.parseDouble(sell[0]);
				int sellStackSize = Integer.parseInt(sell[1]);

				String[] stock = v[2].split(":");
				int currStock = Integer.parseInt(stock[0]);
				int maxStock = Integer.parseInt(stock[1]);

				if(!shop.addItem(id, type, buyPrice, buyStackSize, sellPrice,
						sellStackSize, currStock, maxStock))
				{
					if(isolateBrokenShopFile(file))
					{
						log.warning(String
								.format("[%s] Shop File \"%s\" has bad Item Data (%d:%d), Moving to \"plugins/CommandShops/broken-shops/\"",
										plugin.pdfFile.getName(),
										file.toString(), id, type));
					}else
					{
						log.warning(String
								.format("[%s] Shop File \"%s\" has bad Item Data (%d:%d), Error moving to \"plugins/CommandShops/broken-shops/\"",
										plugin.pdfFile.getName(),
										file.toString(), id, type));
					}
					return null;
				}
			}
		}

		// Sanity Checks
		// Check that filename == UUID from file
		if(!file.getName().equalsIgnoreCase(
				String.format("%s.shop", shop.getUuid().toString())))
		{
			shop = null;

			if(isolateBrokenShopFile(file))
			{
				log.warning(String
						.format("[%s] Shop file %s has bad data!  Moving to \"plugins/CommandShops/broken-shops/\"",
								plugin.pdfFile.getName(), file.getName()));
			}else
			{
				log.warning(String
						.format("[%s] Shop file %s has bad data!  Error moving to \"plugins/CommandShops/broken-shops/\"",
								plugin.pdfFile.getName(), file.getName()));
			}
		}

		return shop;
	}

	/**
	 * Checks if is olate broken shop file.
	 * @param file
	 * the file
	 * @return true, if is olate broken shop file
	 */
	public boolean isolateBrokenShopFile(File file)
	{
		File dir = new File("plugins/CommandShops/shops-broken/");
		dir.mkdir();
		if(file.renameTo(new File(dir, file.getName())))
		{
			file.delete();
			return true;
		}else
		{
			return false;
		}
	}

	/**
	 * Save all shops.
	 * @return true, if successful
	 */
	public boolean saveAllShops()
	{
		log.info(String.format("[%s] %s", plugin.pdfFile.getName(),
				"Saving All Shops"));
		Iterator<Shop> it = shops.values().iterator();
		while(it.hasNext())
		{
			Shop shop = it.next();
			saveShop(shop);
		}
		return true;
	}

	/**
	 * Save shop.
	 * @param shop
	 * the shop
	 * @return true, if successful
	 */
	public boolean saveShop(Shop shop)
	{
		SortedProperties props = new SortedProperties();

		// Config attributes
		props.setProperty("config-version", "2.0");

		// Shop attributes
		props.setProperty("uuid", shop.getUuid().toString());
		props.setProperty("name", shop.getName());
		props.setProperty("unlimited-money",
				String.valueOf(shop.isUnlimitedMoney()));
		props.setProperty("unlimited-stock",
				String.valueOf(shop.isUnlimitedStock()));
		props.setProperty("min-balance", String.valueOf(shop.getMinBalance()));
		props.setProperty("notification",
				String.valueOf(shop.getNotification()));

		// Location
		props.setProperty("locationA", shop.getLocationA().toString());
		props.setProperty("locationB", shop.getLocationB().toString());
		props.setProperty("world", shop.getWorld());

		// People
		props.setProperty("owner", shop.getOwner());
		props.setProperty("managers", Search.join(shop.getManagers(), ", "));
		props.setProperty("creator", shop.getCreator());

		// Inventory
		for(InventoryItem item: shop.getItems())
		{
			ItemInfo info = item.getInfo();
			double buyPrice = item.getBuyPrice();
			int buySize = item.getBuySize();
			double sellPrice = item.getSellPrice();
			int sellSize = item.getSellSize();
			int stock = item.getStock();
			int maxStock = item.getMaxStock();

			props.setProperty(String.format("%d:%d", info.typeId,
					info.subTypeId), String.format("%f:%d,%f:%d,%d:%d",
					buyPrice, buySize, sellPrice, sellSize, stock, maxStock));
		}

		String fileName = CommandShops.folderPath + CommandShops.shopsPath
				+ shop.getUuid().toString() + ".shop";
		try
		{
			props.store(new FileOutputStream(fileName),
					"CommandShops Config Version 2.0");
		}catch(IOException e)
		{
			log.warning("IOException: " + e.getMessage());
		}

		return true;
	}

	/**
	 * Delete shop.
	 * @param shop
	 * the shop
	 * @return true, if successful
	 */
	public boolean deleteShop(Shop shop)
	{
		String shortUuid = shop.getShortUuidString();
		ShopLocation loc = shop.getLocationCenter();
		BookmarkedResult res = new BookmarkedResult();

		res = CommandShops.getCuboidTree().relatedSearch(res.bookmark,
				loc.getX(), loc.getY(), loc.getZ());

		// get the shop's tree node and delete it
		for(PrimitiveCuboid shopLocation: res.results)
		{

			// for each shop that you find, check to see if we're already in it
			// this should only find one shop node
			if(shopLocation.uuid == null)
			{
				continue;
			}
			if(!shopLocation.world.equalsIgnoreCase(shop.getWorld()))
			{
				continue;
			}
			CommandShops.getCuboidTree().delete(shopLocation);

		}

		// remove string from uuid short list
		Config.UUID_LIST.remove(shortUuid);

		// delete the file from the directory
		String filePath = CommandShops.folderPath + CommandShops.shopsPath
				+ shop.getUuid() + ".shop";
		File shopFile = new File(filePath);
		shopFile.delete();

		// remove shop from data structure
		shops.remove(shop.getUuid());

		return true;
	}

	/**
	 * Log items.
	 * @param playerName
	 * the player name
	 * @param shopName
	 * the shop name
	 * @param action
	 * the action
	 * @param itemName
	 * the item name
	 * @param numberOfItems
	 * the number of items
	 * @param startNumberOfItems
	 * the start number of items
	 * @param endNumberOfItems
	 * the end number of items
	 * @return true, if successful
	 */
	public boolean logItems(String playerName, String shopName, String action,
			String itemName, int numberOfItems, int startNumberOfItems,
			int endNumberOfItems)
	{

		return logTransaciton(playerName, shopName, action, itemName,
				numberOfItems, startNumberOfItems, endNumberOfItems, 0, 0, 0);

	}

	/**
	 * Log payment.
	 * @param playerName
	 * the player name
	 * @param action
	 * the action
	 * @param moneyTransfered
	 * the money transfered
	 * @param startingbalance
	 * the startingbalance
	 * @param endingbalance
	 * the endingbalance
	 * @return true, if successful
	 */
	public boolean logPayment(String playerName, String action,
			double moneyTransfered, double startingbalance, double endingbalance)
	{

		return logTransaciton(playerName, null, action, null, 0, 0, 0,
				moneyTransfered, startingbalance, endingbalance);
	}

	/**
	 * Log transaciton.
	 * @param playerName
	 * the player name
	 * @param shopName
	 * the shop name
	 * @param action
	 * the action
	 * @param itemName
	 * the item name
	 * @param numberOfItems
	 * the number of items
	 * @param startNumberOfItems
	 * the start number of items
	 * @param endNumberOfItems
	 * the end number of items
	 * @param moneyTransfered
	 * the money transfered
	 * @param startingbalance
	 * the startingbalance
	 * @param endingbalance
	 * the endingbalance
	 * @return true, if successful
	 */
	public boolean logTransaciton(String playerName, String shopName,
			String action, String itemName, int numberOfItems,
			int startNumberOfItems, int endNumberOfItems,
			double moneyTransfered, double startingbalance, double endingbalance)
	{
		if(!Config.LOG_ENABLE) return false;

		String filePath = CommandShops.folderPath + "transactions.log";

		File logFile = new File(filePath);
		try
		{

			logFile.createNewFile();

			String fileOutput = "";

			DateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss z");
			Date date = new Date();
			fileOutput += dateFormat.format(date) + ": ";
			fileOutput += "Action: ";
			if(action != null) fileOutput += action;
			fileOutput += ": ";
			fileOutput += "Player: ";
			if(playerName != null) fileOutput += playerName;
			fileOutput += ": ";
			fileOutput += "Shop: ";
			if(shopName != null) fileOutput += shopName;
			fileOutput += ": ";
			fileOutput += "Item Type: ";
			if(itemName != null) fileOutput += itemName;
			fileOutput += ": ";
			fileOutput += "Number Transfered: ";
			fileOutput += numberOfItems;
			fileOutput += ": ";
			fileOutput += "Stating Stock: ";
			fileOutput += startNumberOfItems;
			fileOutput += ": ";
			fileOutput += "Ending Stock: ";
			fileOutput += endNumberOfItems;
			fileOutput += ": ";
			fileOutput += "Money Transfered: ";
			fileOutput += moneyTransfered;
			fileOutput += ": ";
			fileOutput += "Starting balance: ";
			fileOutput += startingbalance;
			fileOutput += ": ";
			fileOutput += "Ending balance: ";
			fileOutput += endingbalance;
			fileOutput += ": ";
			fileOutput += "\n";

			FileOutputStream logFileOut = new FileOutputStream(logFile, true);
			logFileOut.write(fileOutput.getBytes());
			logFileOut.close();

		}catch(IOException e1)
		{
			System.out.println(plugin.pdfFile.getName()
					+ ": Error - Could not write to file " + logFile.getName());
			return false;
		}

		return true;
	}
}