/************************************************************************
 * This file is part of AdminCmd.									
 *																		
 * AdminCmd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by	
 * the Free Software Foundation, either version 3 of the License, or		
 * (at your option) any later version.									
 *																		
 * AdminCmd is distributed in the hope that it will be useful,	
 * but WITHOUT ANY WARRANTY; without even the implied warranty of		
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the			
 * GNU General Public License for more details.							
 *																		
 * You should have received a copy of the GNU General Public License
 * along with AdminCmd.  If not, see <http://www.gnu.org/licenses/>.
 ************************************************************************/
package be.Balor.Tools.Files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;

import au.com.bytecode.opencsv.CSVReader;
import be.Balor.Manager.Exceptions.WorldNotLoaded;
import be.Balor.Player.BannedPlayer;
import be.Balor.Player.TempBannedPlayer;
import be.Balor.Tools.MaterialContainer;
import be.Balor.Tools.Type;
import be.Balor.Tools.Utils;
import be.Balor.Tools.Configuration.ExtendedConfiguration;
import be.Balor.Tools.Configuration.ExtendedNode;
import be.Balor.bukkit.AdminCmd.ACPluginManager;

/**
 * @author Balor (aka Antoine Aflalo)
 * 
 */
public class FileManager implements DataManager {
	protected File pathFile;
	private static FileManager instance = null;
	private String lastDirectory = "";
	private String lastFilename = "";
	private File lastFile = null;
	private ExtendedConfiguration lastLoadedConf = null;
	private ExtendedConfiguration kits;

	/**
	 * @return the instance
	 */
	public static FileManager getInstance() {
		if (instance == null)
			instance = new FileManager();
		return instance;
	}

	/**
	 * @param path
	 *            the path to set
	 */
	public void setPath(String path) {
		pathFile = new File(path);
		if (!pathFile.exists()) {
			pathFile.mkdir();
		}
		File spawn = getFile(null, "spawnLocations.yml", false);
		File homeDir = new File(this.pathFile, "home");
		if (spawn.exists()) {
			File dir = new File(this.pathFile, "spawn");
			dir.mkdir();
			spawn.renameTo(new File(dir, "spawnLocations.yml.old"));
		}
		if (homeDir.exists())
			homeDir.renameTo(new File(this.pathFile, "userData"));
	}

	/**
	 * Open the file and return the ExtendedConfiguration object
	 * 
	 * @param directory
	 * @param filename
	 * @return the configuration file
	 */
	public ExtendedConfiguration getYml(String filename, String directory) {
		if (lastLoadedConf != null && lastDirectory.equals(directory == null ? "" : directory)
				&& lastFilename.equals(filename))
			return lastLoadedConf;
		ExtendedConfiguration config = new ExtendedConfiguration(getFile(directory, filename
				+ ".yml"));
		config.registerClass(BannedPlayer.class);
		config.registerClass(TempBannedPlayer.class);
		config.load();
		lastLoadedConf = config;
		return config;
	}

	public ExtendedConfiguration getYml(String filename) {
		return getYml(filename, null);
	}

	/**
	 * Open the file and return the File object
	 * 
	 * @param directory
	 * @param filename
	 * @return the configuration file
	 */
	public File getFile(String directory, String filename) {
		return getFile(directory, filename, true);
	}

	public File getFile(String directory, String filename, boolean create) {
		if (lastFile != null && lastDirectory.equals(directory == null ? "" : directory)
				&& lastFilename.equals(filename))
			return lastFile;
		File file = null;
		if (directory != null) {
			File directoryFile = new File(this.pathFile, directory);
			if (!directoryFile.exists() && create) {
				directoryFile.mkdir();
			}
			file = new File(directoryFile, filename);
		} else
			file = new File(pathFile, filename);

		if (!file.exists() && create) {

			try {
				file.createNewFile();
			} catch (IOException ex) {
				System.out.println("cannot create file " + file.getPath());
			}
		}
		lastFile = file;
		lastDirectory = directory == null ? "" : directory;
		lastFilename = filename;
		return file;
	}

	/**
	 * Write the alias in the yml file
	 * 
	 * @param alias
	 * @param mc
	 */
	public void addAlias(String alias, MaterialContainer mc) {
		ExtendedConfiguration conf = getYml("Alias");
		ArrayList<String> aliasList = (ArrayList<String>) conf.getStringList("alias",
				new ArrayList<String>());
		ArrayList<String> idList = (ArrayList<String>) conf.getStringList("ids",
				new ArrayList<String>());
		if (aliasList.contains(alias)) {
			int index = aliasList.indexOf(alias);
			aliasList.remove(index);
			idList.remove(index);
		}
		aliasList.add(alias);
		idList.add(mc.toString());
		conf.setProperty("alias", aliasList);
		conf.setProperty("ids", idList);
		conf.save();
	}

	/**
	 * Remove the alias from the yml fileF
	 * 
	 * @param alias
	 */
	public void removeAlias(String alias) {
		ExtendedConfiguration conf = getYml("Alias");
		ArrayList<String> aliasList = (ArrayList<String>) conf.getStringList("alias",
				new ArrayList<String>());
		ArrayList<String> idList = (ArrayList<String>) conf.getStringList("ids",
				new ArrayList<String>());
		int index = aliasList.indexOf(alias);
		aliasList.remove(index);
		idList.remove(index);
		conf.setProperty("alias", aliasList);
		conf.setProperty("ids", idList);
		conf.save();
	}

	/**
	 * Get a file in the jar, copy it in the choose directory inside the plugin
	 * folder, open it and return it
	 * 
	 * @param filename
	 * @return
	 */
	public File getInnerFile(String filename) {
		return getInnerFile(filename, null, false);
	}

	public File getInnerFile(String filename, String directory, boolean replace) {
		final File file;
		if (directory != null) {
			File directoryFile = new File(this.pathFile, directory);
			if (!directoryFile.exists()) {
				directoryFile.mkdirs();
			}
			file = new File(directoryFile, filename);
		} else
			file = new File(pathFile, filename);
		if (file.exists() && replace)
			file.delete();
		if (!file.exists()) {
			final InputStream res = this.getClass().getResourceAsStream("/" + filename);
			FileWriter tx = null;
			try {
				tx = new FileWriter(file);
				for (int i = 0; (i = res.read()) > 0;) {
					tx.write(i);
				}
				tx.flush();
			} catch (IOException ex) {
				ex.printStackTrace();
				return file;
			} finally {
				try {
					res.close();
				} catch (Exception ex) {
				}
				try {
					if (tx != null) {
						tx.close();
					}
				} catch (Exception ex) {
				}
			}
		}
		return file;
	}

	public HashMap<String, MaterialContainer> getAlias() {
		HashMap<String, MaterialContainer> result = new HashMap<String, MaterialContainer>();
		ExtendedConfiguration conf = getYml("Alias");
		ArrayList<String> aliasList = (ArrayList<String>) conf.getStringList("alias",
				new ArrayList<String>());
		ArrayList<String> idList = (ArrayList<String>) conf.getStringList("ids",
				new ArrayList<String>());
		int i = 0;
		try {
			CSVReader csv = new CSVReader(new FileReader(getInnerFile("items.csv")));
			String[] alias;
			while ((alias = csv.readNext()) != null) {
				try {
					result.put(alias[0], new MaterialContainer(alias[1], alias[2]));
				} catch (ArrayIndexOutOfBoundsException e) {
					result.put(alias[0], new MaterialContainer(alias[1]));
				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (String alias : aliasList) {
			result.put(alias, new MaterialContainer(idList.get(i)));
			i++;
		}
		return result;
	}

	/**
	 * Create a flat file with the location informations
	 * 
	 * @param loc
	 * @param filename
	 * @param directory
	 */
	@Override
	public void writeLocation(Location loc, String name, String filename, String directory) {
		ExtendedConfiguration conf = getYml(filename, directory);
		conf.setProperty(name + ".world", loc.getWorld().getName());
		conf.setProperty(name + ".x", loc.getX());
		conf.setProperty(name + ".y", loc.getY());
		conf.setProperty(name + ".z", loc.getZ());
		conf.setProperty(name + ".yaw", loc.getYaw());
		conf.setProperty(name + ".pitch", loc.getPitch());
		conf.save();
	}

	/**
	 * Return the location after parsing the flat file
	 * 
	 * @param property
	 * @param filename
	 * @param directory
	 * @return
	 */
	@Override
	public Location getLocation(String property, String filename, String directory)
			throws WorldNotLoaded {
		ExtendedConfiguration conf = getYml(filename, directory);
		if (conf.getProperty(property + ".world") == null) {
			Location loc = parseLocation(property, conf);
			if (loc != null)
				writeLocation(loc, property, filename, directory);
			return loc;
		} else {
			World w = ACPluginManager.getServer().getWorld(conf.getString(property + ".world"));
			if (w != null)
				return new Location(w, conf.getDouble(property + ".x", 0), conf.getDouble(property
						+ ".y", 0), conf.getDouble(property + ".z", 0), Float.parseFloat(conf
						.getString(property + ".yaw")), Float.parseFloat(conf.getString(property
						+ ".pitch")));
			else
				throw new WorldNotLoaded(conf.getString(property + ".world"));

		}
	}

	/**
	 * Remove the given location from the file
	 * 
	 * @param property
	 * @param filename
	 * @param directory
	 */
	@Override
	public void removeKey(String property, String filename, String directory) {
		ExtendedConfiguration conf = getYml(filename, directory);
		conf.removeProperty(property);
		conf.save();
	}

	/**
	 * Return a string Set containing all locations names
	 * 
	 * @param filename
	 * @param directory
	 * @return
	 */
	@Override
	public List<String> getKeys(String info, String filename, String directory) {
		List<String> keys = getYml(filename, directory).getKeys(info);
		if (keys == null)
			return new ArrayList<String>();
		else
			return keys;
	}

	/**
	 * Parse String to create a location
	 * 
	 * @param property
	 * @param conf
	 * @return
	 */
	private Location parseLocation(String property, ExtendedConfiguration conf) {
		String toParse = conf.getString(property, null);
		if (toParse == null)
			return null;
		if (toParse.isEmpty())
			return null;
		String infos[] = new String[5];
		Double coords[] = new Double[3];
		Float direction[] = new Float[2];
		infos = toParse.split(";");
		for (int i = 0; i < coords.length; i++)
			try {
				coords[i] = Double.parseDouble(infos[i]);
			} catch (NumberFormatException e) {
				return null;
			}
		for (int i = 3; i < infos.length - 1; i++)
			try {
				direction[i - 3] = Float.parseFloat(infos[i]);
			} catch (NumberFormatException e) {
				return null;
			}
		return new Location(ACPluginManager.getServer().getWorld(infos[5]), coords[0], coords[1],
				coords[2], direction[0], direction[1]);
	}

	/**
	 * Load the map
	 * 
	 * @param type
	 * @param directory
	 * @param filename
	 * @return
	 */
	public Map<String, Object> loadMap(Type type, String directory, String filename) {
		Map<String, Object> result = new HashMap<String, Object>();
		ExtendedConfiguration conf = getYml(filename, directory);
		if (conf.getKeys(type.toString()) != null) {
			ExtendedNode node = conf.getNode(type.toString());
			for (String key : node.getKeys())
				result.put(key, node.getProperty(key));
		}
		return result;
	}

	@Override
	public Map<String, BannedPlayer> loadBan() {
		Map<String, BannedPlayer> result = new HashMap<String, BannedPlayer>();
		ExtendedConfiguration conf = getYml("banned");
		if (conf.getProperty("bans") != null) {
			ExtendedNode node = conf.getNode("bans");
			for (String key : node.getKeys())
				result.put(key, (BannedPlayer) node.getProperty(key));

		}
		return result;
	}

	/**
	 * Load all the kits
	 * 
	 * @return
	 */
	public Map<String, KitInstance> loadKits() {
		Map<String, KitInstance> result = new HashMap<String, KitInstance>();
		List<MaterialContainer> items = new ArrayList<MaterialContainer>();
		kits = getYml("kits");
		boolean convert = false;

		ExtendedNode kitNodes = kits.getNode("kits");
		for (String kitName : kitNodes.getKeys()) {
			int delay = 0;
			ExtendedNode kitNode = kitNodes.getNode(kitName);
			ExtendedNode kitItems = null;
			try {
				kitItems = kitNode.getNode("items");
			} catch (NullPointerException e) {
				continue;
			}

			if (kitItems != null) {
				for (String item : kitItems.getKeys()) {
					MaterialContainer m = Utils.checkMaterial(item);
					m.setAmount(kitItems.getInt(item, 1));
					if (!m.isNull())
						items.add(m);
				}
				delay = kitNode.getInt("delay", 0);
			} else {
				kitItems = kitNode.createNode("items");
				for (String item : kitNode.getKeys()) {
					if (item.equals("items"))
						continue;
					MaterialContainer m = Utils.checkMaterial(item);
					int amount = kitNode.getInt(item, 1);
					m.setAmount(amount);
					if (!m.isNull()) {
						items.add(m);
						kitItems.setProperty(item, amount);
						kitNode.removeProperty(item);
					}
				}
				kitNode.setProperty("delay", 0);
				convert = true;
			}

			result.put(kitName, new KitInstance(kitName, delay, new ArrayList<MaterialContainer>(
					items)));
			items.clear();
		}

		ExtendedNode lastUsedNodes = kits.getNode("lastused");
		if (lastUsedNodes != null)
			for (String kitName : lastUsedNodes.getKeys()) {
				ExtendedNode kitNode = lastUsedNodes.getNode(kitName);

				Map<String, Long> playerLastUsed = new HashMap<String, Long>();
				for (String playerName : kitNode.getKeys()) {
					playerLastUsed.put(playerName,
							new ObjectContainer(kitNode.getString(playerName)).getLong(0));
				}
				result.get(kitName).setDelays(playerLastUsed);
			}
		else
			kits.createNode("lastused");
		if (convert)
			kits.save();
		return result;
	}

	/**
	 * Update the lastused for a single player
	 * 
	 * @param name
	 *            Player name
	 * @param systemtime
	 *            System.currentTimeMillis() formatted time
	 */
	public void saveKitInstanceUse(String kitname, String playername, long systemtime) {
		kits.setProperty("lastused." + kitname + "." + playername, systemtime);
		kits.save();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.Balor.Tools.Files.DataManager#addBannedPlayer(be.Balor.Player.BannedPlayer
	 * )
	 */
	@Override
	public void addBannedPlayer(BannedPlayer player) {
		ExtendedConfiguration banFile = getYml("banned");
		ExtendedNode bans = banFile.createNode("bans");
		bans.setProperty(player.getPlayer(), player);
		banFile.save();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.Balor.Tools.Files.DataManager#unbanPlayer(java.lang.String)
	 */
	@Override
	public void unBanPlayer(String player) {
		ExtendedConfiguration banFile = getYml("banned");
		ExtendedNode bans = banFile.createNode("bans");
		bans.removeProperty(player);
		banFile.save();

	}

}
