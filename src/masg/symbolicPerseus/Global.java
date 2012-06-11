package masg.symbolicPerseus;

import java.util.*;
import java.lang.ref.*;

import masg.symbolicPerseus.dd.*;

public class Global {
	public static int[] varDomSize = null;
	
	public static String[] varNames = null;
	public static String[][] valNames = null;

	// hash tables
	// public static WeakHashMap leafHashtable = new WeakHashMap();
	// public static WeakHashMap nodeHashtable = new WeakHashMap();
	public static CacheMap<Object,Object> leafHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> nodeHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> addHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> multHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> maxHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> minHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> dotProductHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> nEdgesHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> nLeavesHashtable = new CacheMap<Object,Object>();
	public static CacheMap<Object,Object> nNodesHashtable = new CacheMap<Object,Object>();

	// random number generator
	public static Random random = new Random();

	public static void setVarDomSize(int[] newVarDomSize) {
		Global.varDomSize = newVarDomSize;
	}

	public static void setVarNames(String[] newVarNames) {
		Global.varNames = newVarNames;
	}

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

	public static void setValNames(int varId, String[] newValNames) {
		if (Global.valNames == null) {
			Global.valNames = new String[varId][];
			Global.valNames[varId - 1] = newValNames;
		} else if (Global.valNames.length < varId) {
			String[][] tempValNames = new String[varId][];
			for (int i = 0; i < Global.valNames.length; i++) {
				tempValNames[i] = Global.valNames[i];
			}
			tempValNames[varId - 1] = newValNames;
			Global.valNames = tempValNames;
		} else {
			Global.valNames[varId - 1] = newValNames;
		}
	}

	public static void clearHashtables() {
		Global.leafHashtable.clear();
		Global.nodeHashtable.clear();
		Global.addHashtable.clear();
		Global.multHashtable.clear();
		Global.maxHashtable.clear();
		Global.minHashtable.clear();
		Global.dotProductHashtable.clear();
		Global.nEdgesHashtable.clear();
		Global.nLeavesHashtable.clear();
		Global.nNodesHashtable.clear();
		Global.leafHashtable.put(DD.zero, new WeakReference<DD>(DD.zero));
		Global.leafHashtable.put(DD.one, new WeakReference<DD>(DD.one));
	}

	public static void newHashtables() {
		// Global.leafHashtable = new WeakHashMap();
		// Global.nodeHashtable = new WeakHashMap();
		Global.leafHashtable = new CacheMap<Object,Object>();
		Global.nodeHashtable = new CacheMap<Object,Object>();
		Global.addHashtable = new CacheMap<Object,Object>();
		Global.multHashtable = new CacheMap<Object,Object>();
		Global.maxHashtable = new CacheMap<Object,Object>();
		Global.minHashtable = new CacheMap<Object,Object>();
		Global.dotProductHashtable = new CacheMap<Object,Object>();
		Global.nEdgesHashtable = new CacheMap<Object,Object>();
		Global.nLeavesHashtable = new CacheMap<Object,Object>();
		Global.nNodesHashtable = new CacheMap<Object,Object>();
		Global.leafHashtable.put(DD.zero, new WeakReference<DD>(DD.zero));
		Global.leafHashtable.put(DD.one, new WeakReference<DD>(DD.one));
	}

	public static int[] getKeyHashCodeSet(HashMap<Object,Object> hashMap) {
		Set<Object> keySet = hashMap.keySet();
		Iterator<Object> iterator = keySet.iterator();
		int[] hashCodeCollection = new int[hashMap.size()];
		int i = 0;
		while (iterator.hasNext()) {
			hashCodeCollection[i] = iterator.next().hashCode();
			i += 1;
		}
		return hashCodeCollection;
	}
}
