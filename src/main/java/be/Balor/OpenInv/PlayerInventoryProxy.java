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
package be.Balor.OpenInv;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.bukkit.entity.Player;

import be.Balor.Tools.Compatibility.ACMinecraftReflection;
import be.Balor.Tools.Compatibility.Reflect.FieldUtils;
import be.Balor.Tools.Compatibility.Reflect.MethodHandler;
import be.Balor.Tools.Compatibility.Reflect.Fuzzy.FuzzyReflection;
import be.Balor.Tools.Debug.DebugLog;

/**
 * @author Antoine
 * 
 */
public class PlayerInventoryProxy implements InvocationHandler {

	protected final Object obj;
	protected final Player proprietary;
	private final Object[] extra = new Object[5];
	private Object[] armor;
	private Object[] items;
	private final int size;

	/**
	 * @param obj2
	 */
	protected PlayerInventoryProxy(final Player prop, final Object obj) {
		this.proprietary = prop;
		this.obj = obj;
		final Object inventory = ACMinecraftReflection.getInventory(this.proprietary);

		final List<Field> fieldList = FuzzyReflection.fromObject(inventory).getFieldList(ACMinecraftReflection.INVENTORY_ITEMSTACK_CONTRACT);
		for (final Field field : fieldList) {
			try {
				final Object[] array = (Object[]) field.get(inventory);
				if (array.length == 4) {
					armor = array;
				} else if (array.length == 36) {
					items = array;
				}
			} catch (final Exception e) {
				throw new RuntimeException("Can't set armor and items of player ", e);
			}
		}

		size = armor.length + items.length + extra.length;
		InventoryManager.setInventoryArmorItems(this.obj, armor, items);
	}

	public static Object newInstance(final Player prop, final Object obj) {
		if (!ACMinecraftReflection.getPlayerInventoryClass().isAssignableFrom(obj.getClass())) {
			throw new RuntimeException("The object must be of the type of PlayerInventory");
		}
		return Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(), new PlayerInventoryProxy(prop, obj));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
	 * java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		final String methodName = method.getName();
		final Class<?>[] parameterTypes = method.getParameterTypes();
		if (methodName.equals("onClose")) {
			this.onClose(args[0]);
			return null;
		} else if (methodName.equals("getContents")
				|| (method.getReturnType().equals(ACMinecraftReflection.getItemStackArrayClass()) && parameterTypes.length == 0)) {
			return ACMinecraftReflection.getItemStackArrayClass().cast(getContents());
		} else if (methodName.equals("getSize") || (method.getReturnType().equals(int.class) && parameterTypes.length == 0)) {
			return getSize();
		} else if (methodName.equals("a_")
				|| methodName.equals("a")
				|| (method.getReturnType().equals(boolean.class) && parameterTypes.length == 1 && ACMinecraftReflection.getEntityPlayerClass()
						.isAssignableFrom(args[0].getClass()))) {
			return a_();
		} else if (methodName.equals("getName") || method.getReturnType().equals(String.class)) {
			return getName();
		} else if (methodName.equals("getItem") || methodName.equals("func_70301_a")) {
			return getItem((Integer) args[0]);
		} else if (methodName.equals("splitStack")
				|| (parameterTypes.length == 2 && parameterTypes[0].equals(int.class) && parameterTypes[1].equals(int.class) && method.getReturnType().equals(
						ACMinecraftReflection.getItemStackClass()))) {
			return splitStack((Integer) args[0], (Integer) args[1]);
		} else if (methodName.equals("splitWithoutUpdate")
				|| (parameterTypes.length == 1 && parameterTypes[0].equals(int.class) && method.getReturnType().equals(
						ACMinecraftReflection.getItemStackClass()))) {
			return splitWithoutUpdate((Integer) args[0]);
		} else if (methodName.equals("setItem")
				|| (method.getReturnType().equals(void.class) && parameterTypes.length == 2 && ACMinecraftReflection.getItemStackClass().isAssignableFrom(
						args[1].getClass()))) {
			setItem((Integer) args[0], args[1]);
			return null;
		} else {
			return method.invoke(obj, args);
		}
	}

	protected Object[] getItems() {
		return items;
	}

	protected Object[] getArmor() {
		return armor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.minecraft.server.PlayerInventory#onClose(org.bukkit.craftbukkit.entity
	 * .CraftHumanEntity)
	 */
	private void onClose(final Object who) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final MethodHandler superOnClose = new MethodHandler(obj.getClass(), "onClose", ACMinecraftReflection.getCraftHumanEntityClass());
		superOnClose.invoke(obj, who);
		checkCloseEvent();
	}

	protected void checkCloseEvent() {
		final Object transactions = FieldUtils.getAttribute(obj, "transaction");
		final MethodHandler isEmpty = new MethodHandler(transactions.getClass(), "isEmpty");
		final boolean empty = isEmpty.invoke(transactions);
		if (empty && !proprietary.isOnline()) {
			InventoryManager.INSTANCE.closeOfflineInv(proprietary);
		}
	}

	protected Object getContents() {
		final Object C = Array.newInstance(ACMinecraftReflection.getItemStackClass(), getSize());
		final Object[] items = getItems();
		final Object[] armor = getArmor();
		System.arraycopy(items, 0, C, 0, items.length);
		System.arraycopy(armor, 0, C, items.length, armor.length);
		return C;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.server.PlayerInventory#getSize()
	 */
	protected int getSize() {
		return size;
	}

	protected boolean a_() {
		return true;
	}

	protected String getName() {
		if (proprietary.getName().length() > 16) {
			return proprietary.getName().substring(0, 16);
		}
		return proprietary.getName();
	}

	private int getReversedItemSlotNum(final int i) {
		if (i >= 27) {
			return i - 27;
		} else {
			return i + 9;
		}
	}

	private int getReversedArmorSlotNum(final int i) {
		if (i == 0) {
			return 3;
		}
		if (i == 1) {
			return 2;
		}
		if (i == 2) {
			return 1;
		}
		if (i == 3) {
			return 0;
		} else {
			return i;
		}
	}

	protected Object getItem(int i) {
		Object[] is = getItems();

		if (i >= is.length) {
			i -= is.length;
			is = getArmor();
		} else {
			i = getReversedItemSlotNum(i);
		}

		if (i >= is.length) {
			i -= is.length;
			is = this.extra;
		} else if (is == getArmor()) {
			i = getReversedArmorSlotNum(i);
		}

		return is[i];
	}

	private int getCount(final Object itemstack) {
		try {
			return FieldUtils.getAttribute(itemstack, "count");
		} catch (final RuntimeException e) {
			return FieldUtils.getAttribute(itemstack, "field_77994_a");
		}

	}

	protected Object splitStack(int i, final int j) {
		DebugLog.beginInfo("[PlayerInventoryProxy] splitStack");
		DebugLog.addInfo("Index : " + i);
		DebugLog.addInfo("Number : " + j);
		try {
			Object[] is = getItems();

			if (i >= is.length) {
				i -= is.length;
				is = getArmor();
			} else {
				i = getReversedItemSlotNum(i);
			}

			if (i >= is.length) {
				i -= is.length;
				is = this.extra;
			} else if (is == getArmor()) {
				i = getReversedArmorSlotNum(i);
			}

			if (is[i] != null) {
				Object itemstack;

				if (getCount(is[i]) <= j) {
					itemstack = is[i];
					is[i] = null;
					return itemstack;
				} else {
					final MethodHandler a = new MethodHandler(is[i].getClass(), "a", int.class);
					itemstack = a.invoke(is[i], j);
					if (getCount(is[i]) == 0) {
						is[i] = null;
					}

					return itemstack;
				}
			} else {
				return null;
			}
		} finally {
			DebugLog.endInfo();
		}
	}

	protected Object splitWithoutUpdate(int i) {
		Object[] is = getItems();

		if (i >= is.length) {
			i -= is.length;
			is = getArmor();
		} else {
			i = getReversedItemSlotNum(i);
		}

		if (i >= is.length) {
			i -= is.length;
			is = this.extra;
		} else if (is == getArmor()) {
			i = getReversedArmorSlotNum(i);
		}

		if (is[i] != null) {
			final Object itemstack = is[i];

			is[i] = null;
			return itemstack;
		} else {
			return null;
		}
	}

	protected void setItem(int i, final Object itemstack) {
		DebugLog.beginInfo("[PlayerInventoryProxy] SetItem");
		DebugLog.addInfo("Index : " + i);
		DebugLog.addInfo("Item : " + itemstack);
		try {
			Object[] is = getItems();

			if (i >= is.length) {
				i -= is.length;
				is = getArmor();
			} else {
				i = getReversedItemSlotNum(i);
			}

			if (i >= is.length) {
				i -= is.length;
				is = this.extra;
			} else if (is == getArmor()) {
				i = getReversedArmorSlotNum(i);
			}
			is[i] = itemstack;
		} finally {
			DebugLog.endInfo();
		}
	}

}
