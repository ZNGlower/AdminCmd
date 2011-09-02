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
package be.Balor.bukkit.AdminCmd;

import org.bukkit.plugin.java.JavaPlugin;

import be.Balor.Manager.CommandManager;
import be.Balor.Manager.Permissions.PermissionLinker;

/**
 * @author Balor (aka Antoine Aflalo)
 * 
 */
public abstract class AbstractAdminCmdPlugin extends JavaPlugin {
	protected final PermissionLinker permissionLinker;
	protected final String name;
	private final int hashCode;

	/**
	 * Create the AdminCmd plugin.
	 * 
	 * @param name
	 *            the name used for the plugin.
	 */
	public AbstractAdminCmdPlugin(String name) {
		this.name = name;
		permissionLinker = PermissionLinker.getPermissionLinker(name);
		ACPluginManager.registerACPlugin(this);
		hashCode = name.hashCode();
	}

	/**
	 * @return the permissionLinker
	 */
	public PermissionLinker getPermissionLinker() {
		return permissionLinker;
	}

	/**
	 * Definition of the Permissions used by the plugin
	 */
	protected abstract void registerPermParents();

	/**
	 * Definitions of the command used by the plugin
	 */
	public abstract void registerCmds();

	/**
	 * Definitions of the locale used by the plugin
	 */
	protected abstract void setDefaultLocale();

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bukkit.plugin.Plugin#onEnable()
	 */
	@Override
	public void onEnable() {
		registerPermParents();
		CommandManager.getInstance().registerACPlugin(this);
		registerCmds();
		CommandManager.getInstance().checkAlias(this);
		setDefaultLocale();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractAdminCmdPlugin))
			return false;
		if (obj.hashCode() == this.hashCode)
			return true;
		return false;
	}
}