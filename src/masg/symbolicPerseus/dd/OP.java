package masg.symbolicPerseus.dd;

import java.util.HashMap;

import masg.symbolicPerseus.Config;
import masg.symbolicPerseus.Global;
import masg.symbolicPerseus.MySet;
import masg.symbolicPerseus.TripletConfig;
import masg.symbolicPerseus.TripletSet;

public class OP {
	public static DD add(DD dd1, DD dd2) {

		// dd1 precedes dd2
		if (dd1.getVar() > dd2.getVar()) {

			if (dd2.getVar() == 0 && dd2.getVal() == 0
					&& dd2.getConfig() == null)
				return dd1;

			Pair pair = new Pair(dd1, dd2);
			DD storedResult = (DD) Global.addHashtable.get(pair);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd1.getChildren().length];
			for (int i = 0; i < dd1.getChildren().length; i++) {
				children[i] = OP.add(dd1.getChildren()[i], dd2);
			}
			DD result = DDnode.myNew(dd1.getVar(), children);
			Global.addHashtable.put(pair, result);
			return result;
		}

		// dd2 precedes dd1 {
		else if (dd2.getVar() > dd1.getVar()) {

			if (dd1.getVar() == 0 && dd1.getVal() == 0
					&& dd1.getConfig() == null)
				return dd2;

			Pair pair = new Pair(dd1, dd2);
			DD storedResult = (DD) Global.addHashtable.get(pair);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd2.getChildren().length];
			for (int i = 0; i < dd2.getChildren().length; i++) {
				children[i] = OP.add(dd2.getChildren()[i], dd1);
			}
			DD result = DDnode.myNew(dd2.getVar(), children);
			Global.addHashtable.put(pair, result);
			return result;
		}

		// dd2 and dd1 have same root var
		else if (dd1.getVar() > 0) {

			Pair pair = new Pair(dd1, dd2);
			DD storedResult = (DD) Global.addHashtable.get(pair);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd1.getChildren().length];
			for (int i = 0; i < dd1.getChildren().length; i++) {
				children[i] = OP
						.add(dd1.getChildren()[i], dd2.getChildren()[i]);
			}
			DD result = DDnode.myNew(dd1.getVar(), children);
			Global.addHashtable.put(pair, result);
			return result;
		}

		// dd1 and dd2 are leaves
		else {
			double newVal = dd1.getVal() + dd2.getVal();
			int[][] newConfig = Config.merge(dd1.getConfig(), dd2.getConfig());
			return DDleaf.myNew(newVal, newConfig);
		}
	}

	public static DD addMultVarElim(DD dd, int[] vars) {
		DD[] dds = new DD[1];
		dds[0] = dd;
		return addMultVarElim(dds, vars);
	}

	public static DD addMultVarElim(DD[] dds, int var) {
		int[] vars = new int[1];
		vars[0] = var;
		return addMultVarElim(dds, vars);
	}

	public static DD addMultVarElim(DD dd, int var) {
		int[] vars = new int[1];
		vars[0] = var;
		return addMultVarElim(dd, vars);
	}

	public static DD addMultVarElim(DD[] dds, int[] vars) {

		// check if any of the dds are zero
		for (int i = 0; i < dds.length; i++) {
			if (dds[i].getVar() == 0 && dds[i].getVal() == 0)
				return DD.zero;
		}

		// eliminate variables one by one
		while (vars != null && vars.length > 0) {

			// eliminate deterministic variables
			boolean deterministic = true;
			while (deterministic && vars.length > 0) {
				deterministic = false;
				for (int ddId = 0; ddId < dds.length; ddId++) {
					int[] varIds = dds[ddId].getVarSet();
					if (varIds.length == 1 && MySet.find(vars, varIds[0]) >= 0) {
						DD[] children = dds[ddId].getChildren();
						int valId = -1;
						for (int childId = 0; childId < children.length; childId++) {
							double value = children[childId].getVal();
							if (value == 1 && !deterministic) {
								deterministic = true;
								valId = childId + 1;
							} else if ((value != 0 && value != 1)
									|| (value == 1 && deterministic)) {
								deterministic = false;
								break;
							}
						}
						if (deterministic) {
							vars = MySet.remove(vars, varIds[0]);
							int[][] config = new int[2][1];
							config[0][0] = varIds[0];
							config[1][0] = valId;
							dds = DDcollection.removeIth(dds, ddId);
							for (int i = 0; i < dds.length; i++) {
								if (MySet.find(dds[i].getVarSet(), varIds[0]) >= 0)
									dds[i] = OP.restrictOrdered(dds[i], config);
							}
							break;
						}
					}
				}
			}
			if (vars.length <= 0)
				break;

			// greedily choose var to eliminate
			int bestVar = OP.selectVarGreedily(dds, vars);

			// multiply together trees that depend on var
			DD newDd = DD.one;
			for (int ddId = 0; ddId < dds.length; ddId++) {
				if (MySet.find(dds[ddId].getVarSet(), bestVar) >= 0) {
					newDd = OP.mult(newDd, dds[ddId]);
					dds = DDcollection.removeIth(dds, ddId);
					ddId--;
				}
			}

			// sumout bestVar from newDd
			newDd = OP.addout(newDd, bestVar);
			if (newDd.getVar() == 0 && newDd.getVal() == 0)
				return DD.zero;

			// add new tree to dds
			dds = DDcollection.add(dds, newDd);

			// remove bestVar from vars
			vars = MySet.remove(vars, bestVar);
		}

		// multiply remaining trees and the newly added one; the resulting tree
		// is now free of any variable that appeared in vars
		return OP.multN(dds);
	}

	// TODO: Why does this function exist?!!
	public static DD addN(DD dd) {
		return dd;
	}

	public static DD addN(DD[] ddArray) {
		DD ddSum = DD.zero;
		for (int i = 0; i < ddArray.length; i++) {
			ddSum = OP.add(ddSum, ddArray[i]);
		}
		return ddSum;
	}

	public static DD addout(DD dd, int var) {

		HashMap<DD, DD> hashtable = new HashMap<DD, DD>();
		return addout(dd, var, hashtable);
	}

	public static DD addout(DD dd, int var, HashMap<DD, DD> hashtable) {

		// it's a leaf
		if (dd.getVar() == 0) {
			return DDleaf.myNew(Global.varDomSize[var - 1] * dd.getVal(),
					dd.getConfig());
		}

		DD result = (DD) hashtable.get(dd);
		if (result != null)
			return result;

		// root is variable that must be eliminated
		if (dd.getVar() == var) {
			// have to collapse all children into a new node
			result = OP.addN(dd.getChildren());
		}

		// descend down the tree until 'var' is found
		else {
			DD children[];
			children = new DD[dd.getChildren().length];
			for (int i = 0; i < dd.getChildren().length; i++) {
				children[i] = OP.addout(dd.getChildren()[i], var);
			}
			result = DDnode.myNew(dd.getVar(), children);
		}

		// store result
		hashtable.put(dd, result);
		return result;
	}

	public static DD abs(DD dd) {

		// dd is a leaf
		if (dd.getVar() == 0) {
			if (dd.getVal() >= 0)
				return dd;
			else
				return DDleaf.myNew(-dd.getVal(), dd.getConfig());
		}

		// dd is a node
		else {
			DD children[];
			children = new DD[dd.getChildren().length];
			for (int i = 0; i < dd.getChildren().length; i++) {
				children[i] = OP.abs(dd.getChildren()[i]);
			}
			return DDnode.myNew(dd.getVar(), children);
		}
	}

	public static DD approximate(DD dd, double tolerance,
			double[] prescribedLeafValues) {

		HashMap<DD, DD> hashtable = new HashMap<DD, DD>();
		double[] leafValues = new double[OP.nLeaves(dd)
				+ prescribedLeafValues.length];
		for (int i = 0; i < prescribedLeafValues.length; i++) {
			leafValues[i] = prescribedLeafValues[i];
		}
		int[] nLeavesPtr = new int[1];
		nLeavesPtr[0] = prescribedLeafValues.length;
		return OP.approximate(dd, tolerance, hashtable, leafValues, nLeavesPtr);
	}

	public static DD approximate(DD dd, double tolerance,
			HashMap<DD, DD> hashtable, double[] leafValues, int[] nLeavesPtr) {

		// lookup apprDd
		DD apprDd = (DD) hashtable.get(dd);
		if (apprDd != null)
			return apprDd;

		// it's a leaf
		if (dd.getVar() == 0) {
			double val = dd.getVal();

			// binary search
			int ubId = nLeavesPtr[0];
			int lbId = -1;
			int middleId = (ubId + lbId) / 2;
			while (lbId + 1 < ubId) {
				if (leafValues[middleId] >= val)
					ubId = middleId;
				else
					lbId = middleId;
				middleId = (ubId + lbId) / 2;
			}
			middleId = ubId;

			// find closest value
			double ubVal = Double.POSITIVE_INFINITY;
			double lbVal = Double.NEGATIVE_INFINITY;
			double closestVal;
			if (lbId >= 0)
				lbVal = leafValues[lbId];
			if (ubId < nLeavesPtr[0])
				ubVal = leafValues[ubId];
			if (ubVal - val <= val - lbVal)
				closestVal = ubVal;
			else
				closestVal = lbVal;

			// replace node
			if (val - closestVal <= tolerance && closestVal - val <= tolerance)
				apprDd = DDleaf.myNew(closestVal);

			// insert val in leafValues
			else {
				for (int i = nLeavesPtr[0]; i > middleId; i--) {
					leafValues[i] = leafValues[i - 1];
				}
				leafValues[middleId] = val;
				nLeavesPtr[0] += 1;
				apprDd = dd;
			}

			// store apprDd
			hashtable.put(dd, apprDd);
			return apprDd;
		}

		// it's a node
		else {
			DD[] children = dd.getChildren();
			DD[] newChildren = new DD[children.length];
			for (int i = 0; i < children.length; i++) {
				newChildren[i] = OP.approximate(children[i], tolerance,
						hashtable, leafValues, nLeavesPtr);
			}
			apprDd = DDnode.myNew(dd.getVar(), newChildren);
			hashtable.put(dd, apprDd);
			return apprDd;
		}
	}

	public static double[] convert2array(DD dd) {

		int[] varList = dd.getVarSet();
		return convert2array(dd, varList);
	}

	public static double[][] convert2array(DD[] ddArray, int[] varList) {

		double[][] results = new double[ddArray.length][];
		for (int id = 0; id < ddArray.length; id++) {
			results[id] = convert2array(ddArray[id], varList);
		}
		return results;
	}

	public static double[] convert2array(DD dd, int[] varList) {

		// double check that varList contains all the variables in the tree...
		int[] diffSet = MySet.diff(dd.getVarSet(), varList);
		if (diffSet != null && diffSet.length >= 1) {
			throw new Error(
					"varList does not contain all the variables in the tree\n");
		} else
			varList = MySet.reverse(MySet.sort(varList));

		int arrayLength = 1;
		for (int i = 0; i < varList.length; i++) {
			arrayLength *= Global.varDomSize[varList[i] - 1];
		}

		double[] result = new double[arrayLength];
		convert2arrayRecursive(dd, varList, 0, result, 0);
		return result;
	}

	public static int convert2arrayRecursive(DD dd, int[] varList,
			int varListIndex, double[] array, int arrayIndex) {

		if (varListIndex == varList.length) {
			array[arrayIndex] = dd.getVal();
			arrayIndex += 1;
		}

		else {
			int varId = dd.getVar();
			if (varId < varList[varListIndex]) {
				for (int i = 0; i < Global.varDomSize[varList[varListIndex] - 1]; i++) {
					arrayIndex = convert2arrayRecursive(dd, varList,
							varListIndex + 1, array, arrayIndex);
				}
			}

			else {
				DD[] children = dd.getChildren();
				for (int i = 0; i < children.length; i++) {
					arrayIndex = convert2arrayRecursive(children[i], varList,
							varListIndex + 1, array, arrayIndex);
				}
			}
		}

		return arrayIndex;
	}

	public static DD div(DD dd1, DD dd2) {
		return OP.mult(dd1, OP.inv(dd2));
	}

	public static double dotProduct(DD dd1, DD dd2, int[] vars) {

		if ((dd1.getVar() == 0 && dd1.getVal() == 0)
				|| (dd2.getVar() == 0 && dd2.getVal() == 0))
			return 0;

		// dd1 precedes dd2
		if (dd1.getVar() > dd2.getVar()) {

			TripletSet triplet = new TripletSet(dd1, dd2, vars);
			Double storedResult = (Double) Global.dotProductHashtable
					.get(triplet);
			if (storedResult != null)
				return storedResult.doubleValue();

			int[] remainingVars = MySet.remove(vars, dd1.getVar());
			double dp = 0;
			for (int i = 0; i < dd1.getChildren().length; i++) {
				dp += OP.dotProduct(dd1.getChildren()[i], dd2, remainingVars);
			}
			Global.dotProductHashtable.put(triplet, new Double(dp));
			return dp;
		}

		// dd2 precedes dd1 {
		else if (dd2.getVar() > dd1.getVar()) {

			TripletSet triplet = new TripletSet(dd1, dd2, vars);
			Double storedResult = (Double) Global.dotProductHashtable
					.get(triplet);
			if (storedResult != null)
				return storedResult.doubleValue();

			int[] remainingVars = MySet.remove(vars, dd2.getVar());
			double dp = 0;
			for (int i = 0; i < dd2.getChildren().length; i++) {
				dp += OP.dotProduct(dd2.getChildren()[i], dd1, remainingVars);
			}
			Global.dotProductHashtable.put(triplet, new Double(dp));
			return dp;
		}

		// dd2 and dd1 have same root var
		else if (dd1.getVar() > 0) {

			TripletSet triplet = new TripletSet(dd1, dd2, vars);
			Double storedResult = (Double) Global.dotProductHashtable
					.get(triplet);
			if (storedResult != null)
				return storedResult.doubleValue();

			int[] remainingVars = MySet.remove(vars, dd1.getVar());
			double dp = 0;
			for (int i = 0; i < dd1.getChildren().length; i++) {
				dp += OP.dotProduct(dd1.getChildren()[i], dd2.getChildren()[i],
						remainingVars);
			}
			Global.dotProductHashtable.put(triplet, new Double(dp));
			return dp;
		}

		// dd1 and dd2 are leaves
		else {
			double result = dd1.getVal() * dd2.getVal();
			for (int i = 0; i < vars.length; i++) {
				result *= Global.varDomSize[vars[i] - 1];
			}
			return result;
		}
	}

	public static double factoredExpectationSparse(DD[] factDist, DD dd) {

		DD[] factDistArray = new DD[Global.varDomSize.length + 1];
		for (int i = 0; i < factDist.length; i++) {
			factDistArray[factDist[i].getVar()] = factDist[i];
		}
		HashMap<DD, Double> hashtable = new HashMap<DD, Double>();

		return factoredExpectationSparse(factDistArray, dd, hashtable);
	}

	public static double factoredExpectationSparse(DD[] factDistArray, DD dd,
			HashMap<DD, Double> hashtable) {

		// it's a leaf
		int varId = dd.getVar();
		if (varId == 0)
			return dd.getVal();

		// it's a node
		else {
			Double storedResult = (Double) hashtable.get(dd);
			if (storedResult != null)
				return storedResult.doubleValue();

			DD[] children = dd.getChildren();
			double result = 0;

			if (factDistArray[varId] != null) {
				DD[] scalingConstants = factDistArray[varId].getChildren();
				for (int i = 0; i < children.length; i++) {
					if (scalingConstants[i].getVal() != 0) {
						result += scalingConstants[i].getVal()
								* OP.factoredExpectationSparse(factDistArray,
										children[i], hashtable);
					}
				}
			} else {
				for (int i = 0; i < children.length; i++) {
					result += 1.0
							/ children.length
							* OP.factoredExpectationSparse(factDistArray,
									children[i], hashtable);
				}
			}
			hashtable.put(dd, new Double(result));
			return result;
		}
	}

	public static double[][] factoredExpectationSparse(DD[][] factDist,
			DD[] ddArray) {

		double[][] results = new double[factDist.length][ddArray.length];
		DD[][] factDistArray = new DD[factDist.length][Global.varDomSize.length + 1];
		for (int i = 0; i < factDist.length; i++) {
			for (int j = 0; j < factDist[i].length; j++) {
				factDistArray[i][factDist[i][j].getVar()] = factDist[i][j];
			}
		}
		for (int i = 0; i < factDistArray.length; i++) {
			for (int j = 0; j < ddArray.length; j++) {
				HashMap<DD, Double> hashtable = new HashMap<DD, Double>();
				results[i][j] = factoredExpectationSparse(factDistArray[i],
						ddArray[j], hashtable);
			}
		}
		return results;
	}

	public static double[] factoredExpectationSparse(DD[][] factDist, DD dd) {

		double[] results = new double[factDist.length];
		DD[][] factDistArray = new DD[factDist.length][Global.varDomSize.length + 1];
		for (int i = 0; i < factDist.length; i++) {
			for (int j = 0; j < factDist[i].length; j++) {
				factDistArray[i][factDist[i][j].getVar()] = factDist[i][j];
			}
			HashMap<DD, Double> hashtable = new HashMap<DD, Double>();
			results[i] = factoredExpectationSparse(factDistArray[i], dd,
					hashtable);
		}

		return results;
	}

	public static double factoredExpectationSparseNoMem(DD[] factDist, DD dd) {

		DD[] factDistArray = new DD[Global.varDomSize.length + 1];
		for (int i = 0; i < factDist.length; i++) {
			factDistArray[factDist[i].getVar()] = factDist[i];
		}

		return factoredExpectationSparseNoMemRecursive(factDistArray, dd);
	}

	public static double[][] factoredExpectationSparseNoMem(DD[][] factDist,
			DD[] ddArray) {

		double[][] results = new double[factDist.length][ddArray.length];
		DD[][] factDistArray = new DD[factDist.length][Global.varDomSize.length + 1];
		for (int i = 0; i < factDist.length; i++) {
			for (int j = 0; j < factDist[i].length; j++) {
				factDistArray[i][factDist[i][j].getVar()] = factDist[i][j];
			}
		}
		for (int i = 0; i < factDistArray.length; i++) {
			for (int j = 0; j < ddArray.length; j++) {
				results[i][j] = factoredExpectationSparseNoMemRecursive(
						factDistArray[i], ddArray[j]);
			}
		}
		return results;
	}

	public static double[] factoredExpectationSparseNoMem(DD[] factDist,
			DD[] ddArray) {

		DD[] factDistArray = new DD[Global.varDomSize.length + 1];
		for (int i = 0; i < factDist.length; i++) {
			factDistArray[factDist[i].getVar()] = factDist[i];
		}

		double[] results = new double[ddArray.length];
		for (int j = 0; j < ddArray.length; j++) {
			results[j] = factoredExpectationSparseNoMemRecursive(factDistArray,
					ddArray[j]);
		}
		return results;
	}

	public static double[] factoredExpectationSparseNoMem(DD[][] factDist, DD dd) {

		double[] results = new double[factDist.length];
		DD[][] factDistArray = new DD[factDist.length][Global.varDomSize.length + 1];
		for (int i = 0; i < factDist.length; i++) {
			for (int j = 0; j < factDist[i].length; j++) {
				factDistArray[i][factDist[i][j].getVar()] = factDist[i][j];
			}
			results[i] = factoredExpectationSparseNoMemRecursive(
					factDistArray[i], dd);
		}
		return results;
	}

	public static double factoredExpectationSparseNoMemRecursive(
			DD[] factDistArray, DD dd) {

		// it's a leaf
		int varId = dd.getVar();
		if (varId == 0)
			return dd.getVal();

		// it's a node
		else {
			DD[] children = dd.getChildren();
			double result = 0;

			if (factDistArray[varId] != null) {
				DD[] scalingConstants = factDistArray[varId].getChildren();
				for (int i = 0; i < children.length; i++) {
					if (scalingConstants[i].getVal() != 0) {
						result += scalingConstants[i].getVal()
								* OP.factoredExpectationSparseNoMemRecursive(
										factDistArray, children[i]);
					}
				}
			} else {
				for (int i = 0; i < children.length; i++) {
					result += 1.0
							/ children.length
							* OP.factoredExpectationSparseNoMemRecursive(
									factDistArray, children[i]);
				}
			}
			return result;
		}
	}

	public static double[] getMax(double[][] pbv, int len) {
		double[] mv = new double[pbv.length];
		for (int i = 0; i < pbv.length; i++)
			mv[i] = OP.max(pbv[i], len);
		return mv;
	}

	public static double[] getMax(double[][] pbv, int len, int[] pids) {
		double[] mv = new double[pids.length];
		for (int i = 0; i < pids.length; i++)
			mv[i] = OP.max(pbv[pids[i]], len);
		return mv;
	}

	public static double[] getMax(double[][] pbv, int[] pids) {
		double[] mv = new double[pids.length];
		for (int i = 0; i < pids.length; i++)
			mv[i] = OP.max(pbv[pids[i]]);
		return mv;
	}

	public static DD inv(DD dd) {

		// dd is a leaf
		if (dd.getVar() == 0)
			return DDleaf.myNew(1 / dd.getVal(), dd.getConfig());

		// dd is a node
		else {
			DD children[];
			children = new DD[dd.getChildren().length];
			for (int i = 0; i < dd.getChildren().length; i++) {
				children[i] = OP.inv(dd.getChildren()[i]);
			}
			return DDnode.myNew(dd.getVar(), children);
		}
	}

	public static DD[] marginals(DD[] cpts, int[] margIds, int[] summoutIds) {
		int[] otherVars = new int[margIds.length + summoutIds.length - 1];
		for (int i = 0; i < margIds.length - 1; i++)
			otherVars[i] = margIds[i + 1];
		for (int i = 0; i < summoutIds.length; i++)
			otherVars[i + margIds.length - 1] = summoutIds[i];

		double[] zero = new double[1];
		zero[0] = 0;
		DD[] arrayMargs = new DD[margIds.length + 1];
		for (int i = 0; i < margIds.length; i++) {
			if (i >= 1)
				otherVars[i - 1] = margIds[i - 1];
			arrayMargs[i] = OP.addMultVarElim(cpts, otherVars);
			arrayMargs[i] = OP.approximate(arrayMargs[i], 1e-6, zero);
		}
		arrayMargs[arrayMargs.length - 1] = OP
				.addout(arrayMargs[0], margIds[0]);
		DD normalizationFactor = OP.replace(arrayMargs[arrayMargs.length - 1],
				0, 1);
		for (int i = 0; i < margIds.length; i++) {
			arrayMargs[i] = OP.div(arrayMargs[i], normalizationFactor);
		}
		return arrayMargs;
	}

	public static DD max(DD dd1, DD dd2) {
		return max(dd1, dd2, null);
	}

	public static double max(double[] t) {
		return max(t, t.length);
	}

	public static double max(double[] t, int len) {
		double maximum = 0;
		if (len > 0) {
			maximum = t[0]; // start with the first value
			for (int i = 1; i < len; i++) {
				if (t[i] > maximum) {
					maximum = t[i]; // new maximum
				}
			}
		}
		return maximum;
	}

	public static DD max(DD dd1, DD dd2, int[][] config) {

		// dd2 parent of dd1
		if (dd1.getVar() < dd2.getVar()) {
			TripletConfig triplet = new TripletConfig(dd1, dd2, config);
			DD storedResult = (DD) Global.maxHashtable.get(triplet);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd2.getChildren().length];
			for (int i = 0; i < dd2.getChildren().length; i++) {
				children[i] = OP.max(dd1, dd2.getChildren()[i], config);
			}
			DD result = DDnode.myNew(dd2.getVar(), children);
			Global.maxHashtable.put(triplet, result);
			return result;
		}

		// dd1 parent of dd2
		else if (dd2.getVar() < dd1.getVar()) {

			TripletConfig triplet = new TripletConfig(dd1, dd2, config);
			DD storedResult = (DD) Global.maxHashtable.get(triplet);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd1.getChildren().length];
			for (int i = 0; i < dd1.getChildren().length; i++) {
				children[i] = OP.max(dd1.getChildren()[i], dd2, config);
			}
			DD result = DDnode.myNew(dd1.getVar(), children);
			Global.maxHashtable.put(triplet, result);
			return result;
		}

		// the two variables have equal id
		else if (dd1.getVar() > 0) {

			TripletConfig triplet = new TripletConfig(dd1, dd2, config);
			DD storedResult = (DD) Global.maxHashtable.get(triplet);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd1.getChildren().length];
			for (int i = 0; i < dd1.getChildren().length; i++) {
				children[i] = OP.max(dd1.getChildren()[i],
						dd2.getChildren()[i], config);
			}
			DD result = DDnode.myNew(dd1.getVar(), children);
			Global.maxHashtable.put(triplet, result);
			return result;
		}

		// they are both leaves
		else {
			if (dd1.getVal() < dd2.getVal()) {
				int[][] newConfig = Config.merge(config, dd2.getConfig());
				return DDleaf.myNew(dd2.getVal(), newConfig);
			} else {
				return dd1;
			}
		}
	}

	public static double maxabs(double[] t) {
		double maximum = Math.abs(t[0]); // start with the first value
		double tt;
		for (int i = 1; i < t.length; i++) {
			tt = Math.abs(t[i]);
			if (tt > maximum) {
				maximum = tt; // new maximum
			}
		}
		return maximum;
	}

	public static double maxAll(DD dd) {
		HashMap<DD, Double> hashtable = new HashMap<DD, Double>();
		return maxAll(dd, hashtable);
	}

	public static double maxAll(DD dd, HashMap<DD, Double> hashtable) {

		Double storedResult = (Double) hashtable.get(dd);
		if (storedResult != null)
			return storedResult.doubleValue();

		// it's a leaf
		double result = Double.NEGATIVE_INFINITY;
		if (dd.getVar() == 0)
			result = dd.getVal();
		else {
			DD[] children = dd.getChildren();
			for (int i = 0; i < children.length; i++) {
				double maxVal = OP.maxAll(children[i], hashtable);
				if (result < maxVal)
					result = maxVal;
			}
		}
		hashtable.put(dd, new Double(result));
		return result;
	}

	public static double[] maxAllN(DD[] dds) {

		double[] results = new double[dds.length];
		for (int i = 0; i < dds.length; i++)
			results[i] = OP.maxAll(dds[i]);
		return results;
	}

	public static double maxAllN(DD dd) {
		HashMap<DD, Double> hashtable = new HashMap<DD, Double>();
		return maxAll(dd, hashtable);
	}

	public static DD maxN(DD[] ddArray) {
		DD ddMax = ddArray[0];
		for (int i = 1; i < ddArray.length; i++) {
			ddMax = OP.max(ddMax, ddArray[i]);
		}
		return ddMax;
	}

	public static double[] minAllN(DD[] dds) {

		double[] results = new double[dds.length];
		for (int i = 0; i < dds.length; i++)
			results[i] = OP.minAll(dds[i]);
		return results;
	}

	public static double minAllN(DD dd) {
		HashMap<DD, Double> hashtable = new HashMap<DD, Double>();
		return minAll(dd, hashtable);
	}

	public static double minAll(DD dd) {
		HashMap<DD, Double> hashtable = new HashMap<DD, Double>();
		return minAll(dd, hashtable);
	}

	public static double minAll(DD dd, HashMap<DD, Double> hashtable) {

		Double storedResult = (Double) hashtable.get(dd);
		if (storedResult != null)
			return storedResult.doubleValue();

		// it's a leaf
		double result = Double.POSITIVE_INFINITY;
		if (dd.getVar() == 0)
			result = dd.getVal();
		else {
			DD[] children = dd.getChildren();
			for (int i = 0; i < children.length; i++) {
				double minVal = OP.minAll(children[i], hashtable);
				if (result > minVal)
					result = minVal;
			}
		}
		hashtable.put(dd, new Double(result));
		return result;
	}

	public static DD mult(DD dd1, DD dd2) {

		// dd1 precedes dd2
		if (dd1.getVar() > dd2.getVar()) {

			if (dd2.getVar() == 0 && dd2.getVal() == 0)
				return dd2;
			else if (dd2.getVar() == 0 && dd2.getVal() == 1
					&& dd2.getConfig() == null)
				return dd1;

			Pair pair = new Pair(dd1, dd2);
			DD storedResult = (DD) Global.multHashtable.get(pair);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd1.getChildren().length];
			for (int i = 0; i < dd1.getChildren().length; i++) {
				children[i] = OP.mult(dd1.getChildren()[i], dd2);
			}
			DD result = DDnode.myNew(dd1.getVar(), children);
			Global.multHashtable.put(pair, result);
			return result;
		}

		// dd2 precedes dd1 {
		else if (dd2.getVar() > dd1.getVar()) {

			if (dd1.getVar() == 0 && dd1.getVal() == 0)
				return dd1;
			else if (dd1.getVar() == 0 && dd1.getVal() == 0
					&& dd1.getConfig() == null)
				return dd2;

			Pair pair = new Pair(dd1, dd2);
			DD storedResult = (DD) Global.multHashtable.get(pair);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd2.getChildren().length];
			for (int i = 0; i < dd2.getChildren().length; i++) {
				children[i] = OP.mult(dd2.getChildren()[i], dd1);
			}
			DD result = DDnode.myNew(dd2.getVar(), children);
			Global.multHashtable.put(pair, result);
			return result;
		}

		// dd2 and dd1 have same root var
		else if (dd1.getVar() > 0) {

			Pair pair = new Pair(dd1, dd2);
			DD storedResult = (DD) Global.multHashtable.get(pair);
			if (storedResult != null)
				return storedResult;

			DD children[];
			children = new DD[dd1.getChildren().length];
			for (int i = 0; i < dd1.getChildren().length; i++) {
				children[i] = OP.mult(dd1.getChildren()[i],
						dd2.getChildren()[i]);
			}
			DD result = DDnode.myNew(dd1.getVar(), children);
			Global.multHashtable.put(pair, result);
			return result;
		}

		// dd1 and dd2 are leaves
		else {
			double newVal = dd1.getVal() * dd2.getVal();
			int[][] newConfig = Config.merge(dd1.getConfig(), dd2.getConfig());
			return DDleaf.myNew(newVal, newConfig);
		}
	}

	public static DD multN(DD[] ddArray) {
		DD ddProd = DD.one;
		for (int i = 0; i < ddArray.length; i++) {
			ddProd = OP.mult(ddProd, ddArray[i]);
		}
		return ddProd;
	}

	public static int nEdges(DD dd) {
		// it's a leaf
		if (dd.getVar() == 0)
			return 0;

		// it's a node
		else {
			Integer numEdges = (Integer) Global.nEdgesHashtable.get(dd);

			// recursively compute numEdges
			if (numEdges == null) {
				HashMap<DD, Integer> hashtable = new HashMap<DD, Integer>();
				int nEdges = OP.nEdges(dd, hashtable);
				Global.nEdgesHashtable.put(dd, new Integer(nEdges));
				return nEdges;
			}

			else
				return numEdges.intValue();
		}
	}

	public static int nEdges(DD dd, HashMap<DD, Integer> hashtable) {

		// it's a leaf
		if (dd.getVar() == 0)
			return 0;

		// it's a node
		else {
			Integer numEdges = (Integer) hashtable.get(dd);

			// compute recursively numEdges
			if (numEdges == null) {
				DD[] children = dd.getChildren();
				int nEdges = children.length;
				for (int i = 0; i < children.length; i++) {
					nEdges += OP.nEdges(children[i], hashtable);
				}
				hashtable.put(dd, new Integer(nEdges));
				return nEdges;
			}

			else
				return 0;
		}
	}

	public static DD neg(DD dd) {

		// dd is a leaf
		if (dd.getVar() == 0)
			return DDleaf.myNew(-dd.getVal(), dd.getConfig());

		// dd is a node
		else {
			DD children[];
			children = new DD[dd.getChildren().length];
			for (int i = 0; i < dd.getChildren().length; i++) {
				children[i] = OP.neg(dd.getChildren()[i]);
			}
			return DDnode.myNew(dd.getVar(), children);
		}
	}

	public static int nLeaves(DD dd) {
		// it's a leaf
		if (dd.getVar() == 0)
			return 1;

		// it's a node
		else {
			Integer numLeaves = (Integer) Global.nLeavesHashtable.get(dd);

			// recursively compute numLeaves
			if (numLeaves == null) {
				HashMap<DD, Integer> hashtable = new HashMap<DD, Integer>();
				int nLeaves = OP.nLeaves(dd, hashtable);
				Global.nLeavesHashtable.put(dd, new Integer(nLeaves));
				return nLeaves;
			}

			else
				return numLeaves.intValue();
		}
	}

	public static int nLeaves(DD dd, HashMap<DD, Integer> hashtable) {

		Integer numLeaves = (Integer) hashtable.get(dd);

		// recursively compute numLeaves
		if (numLeaves == null) {
			int nLeaves;

			// it's a leaf
			if (dd.getVar() == 0)
				nLeaves = 1;

			// it's a node
			else {
				nLeaves = 0;
				DD[] children = dd.getChildren();
				for (int i = 0; i < children.length; i++) {
					nLeaves += OP.nLeaves(children[i], hashtable);
				}
			}

			hashtable.put(dd, new Integer(nLeaves));
			return nLeaves;
		}

		else
			return 0;
	}

	public static int nNodes(DD dd) {
		// it's a leaf
		if (dd.getVar() == 0)
			return 0;

		// it's a node
		else {
			Integer numNodes = (Integer) Global.nNodesHashtable.get(dd);

			// recursively compute numNodes
			if (numNodes == null) {
				HashMap<DD, Integer> hashtable = new HashMap<DD, Integer>();
				int nNodes = OP.nNodes(dd, hashtable);
				Global.nNodesHashtable.put(dd, new Integer(nNodes));
				return nNodes;
			}

			else
				return numNodes.intValue();
		}
	}

	public static int nNodes(DD dd, HashMap<DD, Integer> hashtable) {

		// it's a leaf
		if (dd.getVar() == 0)
			return 0;

		// it's a node
		else {
			Integer numNodes = (Integer) hashtable.get(dd);

			// recursively compute numNodes
			if (numNodes == null) {
				int nNodes = 1;
				DD[] children = dd.getChildren();
				for (int i = 0; i < children.length; i++) {
					nNodes += OP.nNodes(children[i], hashtable);
				}
				hashtable.put(dd, new Integer(nNodes));
				return nNodes;
			}

			else
				return 0;
		}
	}

	public static DD primeVars(DD dd, int n) {
		HashMap<DD, DD> hashtable = new HashMap<DD, DD>();
		return primeVars(dd, n, hashtable);
	}

	public static DD primeVars(DD dd, int n, HashMap<DD, DD> hashtable) {

		// dd is a leaf
		if (dd.getVar() == 0)
			return dd;

		// dd is a node
		else {
			DD result = (DD) hashtable.get(dd);
			if (result != null)
				return result;

			DD children[];
			children = new DD[dd.getChildren().length];
			for (int i = 0; i < dd.getChildren().length; i++) {
				children[i] = OP.primeVars(dd.getChildren()[i], n);
			}
			result = DDnode.myNew(dd.getVar() + n, children);
			hashtable.put(dd, result);
			return result;
		}
	}

	public static DD[] primeVarsN(DD[] dds, int n) {
		DD[] primedDds = new DD[dds.length];
		for (int i = 0; i < dds.length; i++) {
			primedDds[i] = OP.primeVars(dds[i], n);
		}
		return primedDds;
	}

	public static DD reorder(DD dd) {

		// it's a leaf
		if (dd.getVar() == 0)
			return dd;

		// it's a node
		int[] varSet = dd.getVarSet();
		int highestVar = varSet[varSet.length - 1];
		int[][] config = new int[2][1];
		config[0][0] = highestVar;
		DD[] children = new DD[Global.varDomSize[highestVar - 1]];
		for (int i = 0; i < Global.varDomSize[highestVar - 1]; i++) {
			config[1][0] = i + 1;
			DD restDd = OP.restrict(dd, config);
			children[i] = OP.reorder(restDd);
		}
		return DDnode.myNew(highestVar, children);
	}

	public static DD replace(DD dd, double val1, double val2) {

		// dd is a leaf
		if (dd.getVar() == 0) {
			if (dd.getVal() == val1)
				return DDleaf.myNew(val2, dd.getConfig());
			else
				return dd;
		}

		// dd is a node
		else {
			DD children[];
			children = new DD[dd.getChildren().length];
			for (int i = 0; i < dd.getChildren().length; i++) {
				children[i] = OP.replace(dd.getChildren()[i], val1, val2);
			}
			return DDnode.myNew(dd.getVar(), children);
		}
	}

	public static DD restrict(DD dd, int[][] config) {

		// it's a leaf
		if (dd.getVar() == 0) {
			return dd;
		}

		// root is variable that must be restricted
		int index = MySet.find(config[0], dd.getVar());
		if (index >= 0) {
			int[][] restConfig = Config.removeIth(config, index);

			// terminate early
			if (config[0].length == 0)
				return dd.getChildren()[config[1][index] - 1];

			// recurse
			else
				return OP.restrict(dd.getChildren()[config[1][index] - 1],
						restConfig);
		}

		// have to restrict all children recursively
		DD children[];
		children = new DD[dd.getChildren().length];
		for (int i = 0; i < dd.getChildren().length; i++) {
			children[i] = OP.restrict(dd.getChildren()[i], config);
		}
		return DDnode.myNew(dd.getVar(), children);
	}

	public static DD[] restrictN(DD[] dds, int[][] config) {
		DD[] restrictedDds = new DD[dds.length];
		for (int i = 0; i < dds.length; i++) {
			restrictedDds[i] = OP.restrict(dds[i], config);
		}
		return restrictedDds;
	}

	public static DD restrictOrdered(DD dd, int[][] config) {

		// optimized to terminate early by exploiting variable ordering

		// find var index
		int variable = dd.getVar();
		boolean smallerVar = false;
		int index = -1;
		for (int i = 0; i < config[0].length; i++) {
			if (config[0][i] < variable)
				smallerVar = true;
			if (config[0][i] == variable) {
				index = i;
				break;
			}
		}

		// nothing to restrict
		if (index == -1 && !smallerVar)
			return dd;

		// root is variable that must be restricted
		if (index >= 0) {
			return OP.restrict(dd.getChildren()[config[1][index] - 1], config);
		}

		// have to restrict all children recursively
		DD[] children = new DD[dd.getChildren().length];
		for (int i = 0; i < children.length; i++) {
			children[i] = OP.restrict(dd.getChildren()[i], config);
		}
		return DDnode.myNew(variable, children);
	}

	public static int sampleMultinomial(double[] pdist) {
		double thesum = 0.0;
		int i = 0;
		for (i = 0; i < pdist.length; i++)
			thesum += pdist[i];
		double ssum = 0.0;
		double r = Global.random.nextDouble();
		// System.out.println("r is "+r);
		i = 0;
		while (ssum < r && i < pdist.length)
			ssum += pdist[i++] / thesum;
		return (i - 1);
	}

	public static int[][] sampleMultinomial(DD[] ddArray, int[] varSet) {
		int[][] config = null;
		while (varSet.length > 0) {
			int varId = varSet[0];
			varSet = MySet.removeIth(varSet, 0);
			DD marginal = OP.addMultVarElim(ddArray, varSet);
			int[][] binding = OP.sampleMultinomial(marginal, varId);
			ddArray = OP.restrictN(ddArray, binding);
			config = Config.merge(config, binding);
		}
		return config;
	}

	public static int[][] sampleMultinomial(DD dd, int[] varSet) {
		DD[] ddArray = new DD[1];
		ddArray[0] = dd;
		return sampleMultinomial(ddArray, varSet);
	}

	public static int[][] sampleMultinomial(DD dd, int varId) {

		int[][] config = new int[2][1];
		config[0][0] = varId;

		// it's a leaf
		if (dd.getVar() == 0) {
			config[1][0] = Global.random.nextInt(Global.varDomSize[varId - 1]) + 1;
			return config;
		}

		// it's a node
		else {
			double sum = 0;
			DD[] children = dd.getChildren();
			for (int childId = 0; childId < children.length; childId++) {
				sum += children[childId].getVal();
			}

			double randomVal = Global.random.nextDouble() * sum;
			sum = 0;
			for (int childId = 0; childId < children.length; childId++) {
				sum += children[childId].getVal();
				if (sum >= randomVal) {
					config[1][0] = childId + 1;
					return config;
				}
			}

			// return last non-zero child
			for (int childId = children.length - 1; childId >= 0; childId--) {
				if (children[childId].getVal() > 0) {
					config[1][0] = childId + 1;
					return config;
				}
			}

			// otherwise there is a bug
			return config;
		}
	}

	public static int selectVarGreedily(DD[] ddArray, int[] vars) {

		// estimate cost of eliminating each var
		double bestSize = Double.POSITIVE_INFINITY;
		int bestVar = 0;
		for (int i = 0; i < vars.length; i++) {
			int[] newVarSet = new int[0];
			double sizeEstimate = 1;
			int nAffectedDds = 0;
			for (int ddId = 0; ddId < ddArray.length; ddId++) {
				if (ddArray[ddId] == null)
					System.out.println("ddArray[" + ddId + "] is null");
				int[] varSet = ddArray[ddId].getVarSet();
				if (MySet.find(varSet, vars[i]) >= 0) {
					newVarSet = MySet.union(varSet, newVarSet);
					sizeEstimate *= ddArray[ddId].getNumLeaves();
					nAffectedDds += 1;
				}
			}

			// # of affected DDs <= 1 or # of vars is <= 2
			if (nAffectedDds <= 1 || newVarSet.length <= 2) {
				return vars[i];
			}

			// compute sizeUpperBound:
			// sizeUpperBound = min(sizeEstimate, prod(varDomSize(newScope)));
			double sizeUpperBound = 1;
			for (int j = 0; j < newVarSet.length; j++) {
				sizeUpperBound *= Global.varDomSize[newVarSet[j] - 1];
				if (sizeUpperBound >= sizeEstimate)
					break;
			}
			if (sizeUpperBound < sizeEstimate)
				sizeEstimate = sizeUpperBound;

			// revise bestVar
			if (sizeUpperBound < bestSize) {
				bestSize = sizeUpperBound;
				bestVar = vars[i];
			}
		}
		return bestVar;
	}

	public static DD sub(DD dd1, DD dd2) {
		return OP.add(dd1, OP.neg(dd2));
	}

	public static double[] sub(double[] t1, double[] t2) {
		double[] t = new double[t1.length];
		for (int i = 0; i < t1.length; i++)
			t[i] = t1[i] - t2[i];
		return t;

	}

	// ////////////////////////////////////////////////////
	// threshold dd - if parity > 0, is dd whereever dd1 >= val, and 0 elsewhere
	// if parity < 0, is dd wherever dd <= val, and 0 elsewhere
	// if parity ==0, is dd wherever dd == val, and 0 elsewhere
	// ////////////////////////////////////////////////////
	public static DD threshold(DD dd, double val, int parity) {

		// dd is a leaf
		if (dd.getVar() == 0) {
			if (parity > 0 && dd.getVal() <= val) {
				return DD.zero;
			} else if (parity < 0 && dd.getVal() >= val) {
				return DD.zero;
			} else if (parity == 0 && dd.getVal() != val) {
				return DD.zero;
			} else {
				return dd;
			}

		} // dd is a node
		else {
			DD children[];
			children = new DD[dd.getChildren().length];
			for (int i = 0; i < dd.getChildren().length; i++) {
				children[i] = OP.threshold(dd.getChildren()[i], val, parity);
			}
			return DDnode.myNew(dd.getVar(), children);
		}
	}
}
