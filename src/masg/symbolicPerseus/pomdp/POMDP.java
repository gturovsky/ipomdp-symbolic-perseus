package masg.symbolicPerseus.pomdp;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import masg.symbolicPerseus.AlphaVector;
import masg.symbolicPerseus.Config;
import masg.symbolicPerseus.Global;
import masg.symbolicPerseus.MySet;
import masg.symbolicPerseus.NextBelState;
import masg.symbolicPerseus.RandomPermutation;
import masg.symbolicPerseus.dd.DD;
import masg.symbolicPerseus.dd.DDleaf;
import masg.symbolicPerseus.dd.OP;
import masg.symbolicPerseus.spudd.ParseSPUDD;

public class POMDP implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1805200931586799142L;

	public int nStateVars;
	public int nObsVars;
	public int nVars;
	public int nActions;
	public int nObservations;

	public int maxAlphaSetSize;

	public boolean debug;

	public boolean ignoremore;
	public boolean addbeldiff;

	public StateVar[] stateVars;
	public StateVar[] obsVars;
	public Action[] actions;
	public int[] varDomSize;
	public int[] primeVarIndices;
	public int[] varIndices;
	public int[] obsIndices;
	public int[] primeObsIndices;
	public int[] obsVarsArity;
	public String[] varName;
	public double discFact;
	public double tolerance;
	public double maxRewVal;
	public DD initialBelState;
	public DD ddDiscFact;

	public String[] adjunctNames;
	public int nAdjuncts;
	public DD[] adjuncts; // some additional DDs that can be used
	public DD[] initialBelState_f;
	public DD[] qFn;
	public int[] qPolicy;
	public DD[][] belRegion;
	// these three should really be combined into AlphaVector class
	public int[] policy;
	public boolean[] uniquePolicy;
	public double[] policyvalue;
	public DD[] alphaVectors;
	public DD[] origAlphaVectors;

	public double[][] currentPointBasedValues;
	public double[][] newPointBasedValues;
	public AlphaVector[] newAlphaVectors;
	public int numNewAlphaVectors;
	public double bestImprovement, worstDecline;

	public POMDP(String fileName) {
		readFromFile(fileName, false);
	}
	
	public DD beliefUpdate(DD belState, int actId, String[] obsnames) {
		if (obsnames.length != nObsVars)
			return null;
		int[] obsvals = new int[obsnames.length];
		for (int o = 0; o < obsnames.length; o++) {
			obsvals[o] = findObservationByName(o, obsnames[o]) + 1;
			if (obsvals[o] < 0)
				return null;
		}
		return beliefUpdate(belState, actId, obsvals);
	}

	public DD beliefUpdate(DD belState, int actId, int[] obsvals) {
		return beliefUpdate(belState, actId,
				stackArray(primeObsIndices, obsvals));
	}

	public DD beliefUpdate(DD belState, int actId, int[][] obsvals) {
		//double[] zerovalarray = new double[1];

		DD[] restrictedObsFn = OP.restrictN(actions[actId].obsFn, obsvals);
		DD nextBelState = OP.addMultVarElim(
										concatenateArray(belState, actions[actId].transFn,restrictedObsFn),
										varIndices);
		nextBelState = OP.primeVars(nextBelState, -nVars);
		DD obsProb = OP.addMultVarElim(nextBelState, varIndices);
		if (obsProb.getVal() < 1e-8) {
			System.out.println("WARNING: Zero-probability observation, resetting belief state to a uniform distribution");
			nextBelState = DD.one;
		}
		nextBelState = OP.div(nextBelState,OP.addMultVarElim(nextBelState, varIndices));
		return nextBelState;
	}
	
	public void boundedPerseus(int nIterations, int maxAlpha, int firstStep,
			int nSteps) {
		DD newAlpha, prevAlpha;
		double bellmanErr;
		double[] onezero = { 0 };
		boolean dominated;
		//double steptolerance;
		// check if the value function exists yet
		DD[] tmpalphaVectors = new DD[nActions];

		maxAlphaSetSize = maxAlpha;

		numNewAlphaVectors = 0;

		// this is done in pureStrategies now- still to do
		for (int actId = 0; actId < nActions; actId++) {
			System.out.println("computing pure strategy for action " + actId);
			newAlpha = DD.zero;
			bellmanErr = tolerance;
			for (int i = 0; i < 50; i++) {
				// prevAlpha = OP.sub(newAlpha,DDleaf.myNew(2*tolerance+1));
				// while (OP.maxAll(OP.abs(OP.sub(newAlpha,prevAlpha))) >
				// tolerance) {
				prevAlpha = newAlpha;
				newAlpha = OP.primeVars(newAlpha, nVars);
				newAlpha = OP.addMultVarElim(
						concatenateArray(ddDiscFact, actions[actId].transFn,
								newAlpha), primeVarIndices);
				newAlpha = OP.addN(concatenateArray(actions[actId].rewFn,
						newAlpha));
				newAlpha = OP.approximate(newAlpha, bellmanErr * (1 - discFact)
						/ 2.0, onezero);
				bellmanErr = OP.maxAll(OP.abs(OP.sub(newAlpha, prevAlpha)));
				if (bellmanErr <= tolerance)
					break;
				Global.newHashtables();
			}
			// now add this vector only if not dominated
			dominated = false;
			int aid = 0;
			while (!dominated && aid < numNewAlphaVectors) {
				if (OP.maxAll(OP.sub(newAlpha, tmpalphaVectors[aid])) < tolerance)
					dominated = true;
				aid++;
			}
			if (!dominated) {
				tmpalphaVectors[numNewAlphaVectors] = newAlpha;
				numNewAlphaVectors++;
			}
		}
		alphaVectors = new DD[numNewAlphaVectors];
		origAlphaVectors = new DD[numNewAlphaVectors];
		for (int aid = 0; aid < numNewAlphaVectors; aid++) {
			alphaVectors[aid] = tmpalphaVectors[aid];
			origAlphaVectors[aid] = tmpalphaVectors[aid];
		}
		boundedPerseusStartFromCurrent(maxAlpha, firstStep, nSteps);
	}

	public void boundedPerseusStartFromCurrent(int maxAlpha, int firstStep,
			int nSteps) {
		//DD newAlpha, prevAlpha;
		double bellmanErr;
		double[] onezero = { 0 };
		//boolean dominated;
		double steptolerance;

		maxAlphaSetSize = maxAlpha;

		bellmanErr = 20 * tolerance;

		currentPointBasedValues = OP.factoredExpectationSparseNoMem(belRegion,
				alphaVectors);
		DD[] primedV;
		double maxAbsVal = 0;
		for (int stepId = firstStep; stepId < firstStep + nSteps; stepId++) {
			steptolerance = tolerance;

			System.out.println(" there are " + alphaVectors.length
					+ " alpha vectors:");
			/*
			 * if (false && debug) { for (int i = 0; i < alphaVectors.length;
			 * i++) { System.out.println("alpha vector " + i + ":");
			 * displayAlphaVectorSums(alphaVectors[i]); } }
			 */
			primedV = new DD[alphaVectors.length];
			for (int i = 0; i < alphaVectors.length; i++) {
				primedV[i] = OP.primeVars(alphaVectors[i], nVars);
			}
			maxAbsVal = Math.max(
					OP.maxabs(concatenateArray(OP.maxAllN(alphaVectors),
							OP.minAllN(alphaVectors))), 1e-10);
			System.out.println("maxAbsVal: " + maxAbsVal);

			int count = 0;
			int choice;
			int nDpBackups = 0;
			RandomPermutation permutedIds = new RandomPermutation(
					Global.random, belRegion.length, debug);
			// could be one more than the maximum number at most
			newAlphaVectors = new AlphaVector[maxAlphaSetSize + 1];
			newPointBasedValues = new double[belRegion.length][maxAlphaSetSize + 1];
			numNewAlphaVectors = 0;

			AlphaVector newVector;
			double[] diff = new double[belRegion.length];
			double[] maxcurrpbv;
			double[] maxnewpbv;
			double[] newValues;
			double improvement;

			// we allow the number of new alpha vectors to get one bigger than
			// the maximum allowed size, since we may be able to cull more than
			// one
			// alpha vector when trimming, bringing us back below the cutoff
			while (numNewAlphaVectors < maxAlphaSetSize
					&& !permutedIds.isempty()) {

				if (nDpBackups >= 2 * alphaVectors.length) {
					computeMaxMinImprovement();
					if (bestImprovement > tolerance
							&& bestImprovement > -2 * worstDecline)
						break;
				}
				Global.newHashtables();
				count = count + 1;
				if (count % 100 == 0)
					System.out.println("count is " + count);
				if (numNewAlphaVectors == 0) {
					choice = 0;
				} else {
					maxcurrpbv = OP.getMax(currentPointBasedValues,
							permutedIds.permutation);
					maxnewpbv = OP.getMax(newPointBasedValues,
							numNewAlphaVectors, permutedIds.permutation);
					permutedIds.getNewDoneIds(maxcurrpbv, maxnewpbv,
							steptolerance);
					diff = permutedIds.getDiffs(maxcurrpbv, maxnewpbv,
							steptolerance);

					if (debug) {
						System.out.print("diff is ");
						for (int k = 0; k < diff.length; k++)
							System.out.print(" " + k + ":" + diff[k]);
						System.out.println();
					}
					if (permutedIds.isempty())
						break;
					choice = OP.sampleMultinomial(diff);
				}
				if (debug) {
					permutedIds.display();
				}
				int i = permutedIds.getSetDone(choice);
				System.out.println(" num backups so far " + nDpBackups
						+ " num belief points left " + permutedIds.getNumLeft()
						+ " choice " + choice + " i " + i + "tolerance "
						+ steptolerance);
				if (numNewAlphaVectors < 1
						|| (OP.max(newPointBasedValues[i], numNewAlphaVectors)
								- OP.max(currentPointBasedValues[i]) < steptolerance)) {
					// dpBackup
					newVector = dpBackup(belRegion[i], primedV, maxAbsVal);
					// newVector.alphaVector.display();

					newVector.alphaVector = OP.approximate(
							newVector.alphaVector, bellmanErr * (1 - discFact)
									/ 2.0, onezero);
					newVector.setWitness(i);

					System.out.print(" " + OP.nEdges(newVector.alphaVector)
							+ " edges, " + OP.nNodes(newVector.alphaVector)
							+ " nodes, " + OP.nLeaves(newVector.alphaVector)
							+ " leaves");
					nDpBackups = nDpBackups + 1;

					// merge and trim
					newValues = OP.factoredExpectationSparseNoMem(belRegion,
							newVector.alphaVector);
					if (numNewAlphaVectors < 1) {
						improvement = Double.POSITIVE_INFINITY;
					} else {
						improvement = OP.max(OP.sub(newValues, OP.getMax(
								newPointBasedValues, numNewAlphaVectors)));
					}
					if (improvement > tolerance) {
						for (int belid = 0; belid < belRegion.length; belid++)
							newPointBasedValues[belid][numNewAlphaVectors] = newValues[belid];
						newAlphaVectors[numNewAlphaVectors] = newVector;
						numNewAlphaVectors++;
					}
				}
			}
			// iteration is over,
			System.out.println("iteration " + stepId
					+ " is over...number of new alpha vectors: "
					+ numNewAlphaVectors + "   numdp backupds " + nDpBackups);

			// compute statistics
			//
			computeMaxMinImprovement();

			// save data and copy over new to old
			alphaVectors = new DD[numNewAlphaVectors];
			currentPointBasedValues = new double[newPointBasedValues.length][numNewAlphaVectors];
			System.out.println("policy/values are: ");
			policy = new int[numNewAlphaVectors];
			policyvalue = new double[numNewAlphaVectors];
			for (int j = 0; j < nActions; j++)
				uniquePolicy[j] = false;

			for (int j = 0; j < numNewAlphaVectors; j++) {
				alphaVectors[j] = newAlphaVectors[j].alphaVector;
				System.out.println(" " + newAlphaVectors[j].actId + "/"
						+ newAlphaVectors[j].value);
				policy[j] = newAlphaVectors[j].actId;
				policyvalue[j] = newAlphaVectors[j].value;
				/*if (false && debug)
					alphaVectors[j].display();*/
				uniquePolicy[policy[j]] = true;
			}
			System.out.println("unique policy :");
			for (int j = 0; j < nActions; j++)
				if (uniquePolicy[j])
					System.out.print(" " + j);
			System.out.println();

			for (int i = 0; i < alphaVectors.length; i++) {
				double bval = OP.factoredExpectationSparseNoMem(
						belRegion[newAlphaVectors[i].witness], alphaVectors[i]);
				System.err.println(" " + stepId + " " + policy[i] + " " + bval);
			}
			for (int j = 0; j < belRegion.length; j++) {
				System.arraycopy(newPointBasedValues[j], 0,
						currentPointBasedValues[j], 0, numNewAlphaVectors);
			}
			System.out.println("best improvement: " + bestImprovement
					+ "  worstDecline " + worstDecline);
			bellmanErr = Math.min(10, Math.max(bestImprovement, -worstDecline));
		}

	}

	public void computeMaxMinImprovement() {
		double imp;
		bestImprovement = Double.NEGATIVE_INFINITY;
		worstDecline = Double.POSITIVE_INFINITY;
		for (int j = 0; j < belRegion.length; j++) {
			// find biggest improvement at this belief point
			imp = OP.max(newPointBasedValues[j], numNewAlphaVectors)
					- OP.max(currentPointBasedValues[j]);
			if (imp > bestImprovement)
				bestImprovement = imp;
			if (imp < worstDecline)
				worstDecline = imp;
		}
	}

	public static DD[] concatenateArray(DD a, DD b) {
		DD[] d = new DD[2];
		d[0] = a;
		d[1] = b;
		return d;
	}

	public static DD[] concatenateArray(DD a, DD b, DD c) {
		DD[] d = new DD[3];
		d[0] = a;
		d[1] = b;
		d[2] = c;
		return d;
	}

	public static DD[] concatenateArray(DD a, DD[] b, DD c) {
		DD[] d = new DD[b.length + 2];
		d[0] = a;
		System.arraycopy(b, 0, d, 1, b.length);
		d[b.length + 1] = c;

		return d;
	}
	
	public static DD[] concatenateArray(DD a, DD[] b, DD[] c) {
		DD[] d = new DD[b.length + c.length + 1];
		d[0] = a;
		System.arraycopy(b, 0, d, 1, b.length);
		System.arraycopy(c, 0, d, 1 + b.length, c.length);
		return d;
	}

	public static DD[] concatenateArray(DD[] a, DD[] b, DD c) {
		DD[] d = new DD[b.length + a.length + 1];
		System.arraycopy(a, 0, d, 0, a.length);
		System.arraycopy(b, 0, d, a.length, b.length);
		d[b.length + a.length] = c;
		return d;
	}

	public static DD[] concatenateArray(DD[] a, DD[] b, DD[] c) {
		DD[] d = new DD[b.length + a.length + c.length];
		System.arraycopy(a, 0, d, 0, a.length);
		System.arraycopy(b, 0, d, a.length, b.length);
		System.arraycopy(c, 0, d, a.length + b.length, c.length);
		return d;
	}

	// assumes they're the same size along the first dimension
	public static int[][] concatenateArray(int[][] a, int[][] b) {
		int[][] d = new int[a.length][a[0].length + b[0].length];
		for (int i = 0; i < a.length; i++)
			d[i] = concatenateArray(a[i], b[i]);
		return d;
	}

	public static int[] concatenateArray(int[] a, int[] b) {
		int[] d = new int[a.length + b.length];
		int k = 0;
		for (int i = 0; i < a.length; i++)
			d[k++] = a[i];
		for (int i = 0; i < b.length; i++)
			d[k++] = b[i];
		return d;
	}

	public static double[] concatenateArray(double[] a, double[] b) {
		double[] d = new double[b.length + a.length];
		System.arraycopy(a, 0, d, 0, a.length);
		System.arraycopy(b, 0, d, a.length, b.length);
		return d;
	}

	public AlphaVector dpBackup(DD[] belState, DD[] primedV, double maxAbsVal) {
		NextBelState[] nextBelStates;
		// get next unnormalised belief states
		// System.out.println("smallestProb "+tolerance);
		double smallestProb;
		if (ignoremore)
			smallestProb = tolerance;
		else
			smallestProb = tolerance / maxAbsVal;
		nextBelStates = oneStepNZPrimeBelStates(belState, true, smallestProb);

		// precompute obsVals
		for (int actId = 0; actId < nActions; actId++) {
			nextBelStates[actId].getObsVals(primedV);
		}
		double bestValue = Double.NEGATIVE_INFINITY;
		double actValue;
		int bestActId = 0;
		int[] bestObsStrat = new int[nObservations];

		for (int actId = 0; actId < nActions; actId++) {
			actValue = 0.0;
			// compute immediate rewards
			actValue = actValue
					+ OP.factoredExpectationSparseNoMem(belState,
							actions[actId].rewFn);

			// compute observation strategy
			nextBelStates[actId].getObsStrat();
			actValue = actValue + discFact
					* nextBelStates[actId].getSumObsValues();
			// System.out.println(" actId "+actId+" actValue "+actValue+" sumobsvalues "+nextBelStates[actId].getSumObsValues());
			if (actValue > bestValue) {
				bestValue = actValue;
				bestActId = actId;
				bestObsStrat = nextBelStates[actId].obsStrat;
			}
		}
		// construct corresponding alpha vector
		DD newAlpha = DD.zero;
		DD nextValFn = DD.zero;
		DD obsDd;
		//int tobsid;
		int[] obsConfig = new int[nObsVars];
		for (int alphaId = 0; alphaId < alphaVectors.length; alphaId++) {
			if (MySet.find(bestObsStrat, alphaId) >= 0) {
				// System.out.println("alphaId is "+alphaId);
				obsDd = DD.zero;
				// for (int obsId = 0; obsId < bestObsStrat.length; obsId++) {
				for (int obsId = 0; obsId < nObservations; obsId++) {
					if (bestObsStrat[obsId] == alphaId) {
						obsConfig = statedecode(obsId + 1, nObsVars,
								obsVarsArity);
						obsDd = OP.add(obsDd, Config.convert2dd(stackArray(
								primeObsIndices, obsConfig)));
					}
				}
				nextValFn = OP.add(nextValFn, OP.multN(concatenateArray(
						DDleaf.myNew(discFact), obsDd, primedV[alphaId])));
			}
		}
		newAlpha = OP.addMultVarElim(
				concatenateArray(actions[bestActId].transFn,
						actions[bestActId].obsFn, nextValFn),
				concatenateArray(primeVarIndices, primeObsIndices));
		newAlpha = OP
				.addN(concatenateArray(newAlpha, actions[bestActId].rewFn));
		bestValue = OP.factoredExpectationSparse(belState, newAlpha);
		// package up to return
		AlphaVector returnAlpha = new AlphaVector(newAlpha, bestValue,
				bestActId, bestObsStrat);
		return returnAlpha;
	}
	
	public int findObservationByName(int ob, String oname) {
		for (int o = 0; o < obsVars[ob].arity; o++) {
			if (oname.equalsIgnoreCase(obsVars[ob].valNames[o]))
				return o;
		}
		return -1;
	}

	public double findSimilarFactBelief(DD[] belState, DD[][] belRegion,
			int count) {
		return findSimilarFactBelief(belState, belRegion, count, 0.001);
	}

	public double findSimilarFactBelief(DD[] belief, DD[][] belSet, int count,
			double threshold) {
		double smallestDist = Double.POSITIVE_INFINITY;
		// int closestBelId = 1;
		double maxnorm, dist;
		boolean done1, done2;
		done1 = false;
		for (int i = 0; !done1 & i < count; i++) {
			maxnorm = Double.NEGATIVE_INFINITY;
			done2 = false;
			for (int varId = 0; !done2 & varId < belief.length; varId++) {
				dist = OP
						.maxAll(OP.abs(OP.sub(belSet[i][varId], belief[varId])));
				if (dist > maxnorm) {
					maxnorm = dist;
					if (maxnorm >= smallestDist) {
						done2 = true;
					}
				}
			}
			if (maxnorm < smallestDist) {
				smallestDist = maxnorm;
				// closestBelId = i;
				if (smallestDist <= threshold) {
					done1 = true;
				}
			}
		}
		return smallestDist;
	}

	public NextBelState[] oneStepNZPrimeBelStates(DD[] belState,
			boolean normalize, double smallestProb) {
		int[][] obsConfig = new int[nObservations][nObsVars];
		double[] obsProbs;
		//double[] onezero = { 0 };
		DD[] marginals = new DD[nStateVars + 1];
		DD dd_obsProbs;
		for (int obsId = 0; obsId < nObservations; obsId++) {
			obsConfig[obsId] = statedecode(obsId + 1, nObsVars, obsVarsArity);
		}
		Global.newHashtables();
		NextBelState[] nextBelStates = new NextBelState[nActions];
		for (int actId = 0; actId < nActions; actId++) {
			dd_obsProbs = OP.addMultVarElim(
					concatenateArray(belState, actions[actId].transFn,
							actions[actId].obsFn),
					concatenateArray(varIndices, primeVarIndices));
			obsProbs = OP.convert2array(dd_obsProbs, primeObsIndices);
			nextBelStates[actId] = new NextBelState(obsProbs, smallestProb,
					this);
			// compute marginals
			if (!nextBelStates[actId].isempty()) {
				marginals = OP.marginals(
						concatenateArray(belState, actions[actId].transFn,
								actions[actId].obsFn), primeVarIndices,
						varIndices);
				nextBelStates[actId].restrictN(marginals, obsConfig);
			}
		}
		return nextBelStates;

	}

	public int policyQuery(DD[] belState) {
		return policyQuery(belState, alphaVectors, policy);
	}

	public int policyQuery(DD[] belState, DD[] alphaVectors, int[] policy) {
		// factored DD belief state
		double[] values = OP.factoredExpectationSparseNoMem(belState,
				alphaVectors);
		double bestVal = Double.NEGATIVE_INFINITY;
		// double val;
		int bestAlphaId = 0, bestActId;
		for (int alphaId = 0; alphaId < alphaVectors.length; alphaId++) {
			if (values[alphaId] > bestVal) {
				bestVal = values[alphaId];
				bestAlphaId = alphaId;
			}
		}
		bestActId = policy[bestAlphaId];
		return bestActId;
	}

	public int policyQuery(DD belState, DD[] alphaVectors, int[] policy) {
		// single DD belief state
		double bestVal = Double.NEGATIVE_INFINITY;
		double val;
		int bestAlphaId = 0, bestActId;
		for (int alphaId = 0; alphaId < alphaVectors.length; alphaId++) {
			val = OP.dotProduct(belState, alphaVectors[alphaId], varIndices);
			if (val > bestVal) {
				bestVal = val;
				bestAlphaId = alphaId;
			}
		}
		bestActId = policy[bestAlphaId];
		return bestActId;
	}

	public void printBeliefState(DD belState) {
		// first factor it
		DD[] fbs = new DD[nStateVars];
		for (int varId = 0; varId < nStateVars; varId++) {
			fbs[varId] = OP.addMultVarElim(belState,
					MySet.remove(varIndices, varId + 1));
		}
		printBeliefState(fbs);
	}

	public void printBeliefState(DD[] belState) {
		for (int j = 0; j < belState.length; j++) {
			belState[j].display();
		}
	}

	// compute the reachable belief region starting from the initial belief
	// state and no initial belief region (the default)
	public void reachableBelRegionCurrentPolicy(int maxSize, int maxTries,
			int episodeLength, double threshold, double explorProb, double mdpp) {
		// factored initial belief state
		DD[] iBelState_f = new DD[nStateVars];
		for (int varId = 0; varId < nStateVars; varId++) {
			iBelState_f[varId] = OP.addMultVarElim(initialBelState,
					MySet.remove(varIndices, varId + 1));
		}
		reachableBelRegionCurrentPolicy(iBelState_f, null, maxSize, maxTries,
				episodeLength, threshold, explorProb, mdpp);
	}

	// computes the reachable belief region using the current valueFunction and
	// Policy
	// computes starting from ibel (unfactored belief state), and updates
	// belRegion by adding input bRegion + the ones generated with this
	// simulation
	public void reachableBelRegionCurrentPolicy(DD[] iBelState_f,
			DD[][] iBelRegion, int maxSize, int maxTries, int episodeLength,
			double threshold, double explorProb, double mdpp) {

		int count;
		int choice, actId;
		double distance;

		DD[] nextBelState = new DD[nStateVars];
		DD obsDist;
		DD[] obsDistn, restrictedObsFn;
		DD[] ddState, belState, restrictedTransFn;
		int[][] stateConfig, nextStateConfig, obsConfig;

		double[] zerovalarray = new double[1];
		int[][] oneConfig = new int[2][1];
		double[] eprob = new double[2];
		double[] mdpprob = new double[2];
		zerovalarray[0] = 0;
		DD[][] tmpBelRegion = new DD[maxSize][];
		eprob[0] = 1 - explorProb;
		eprob[1] = explorProb;
		mdpprob[0] = 1 - mdpp;
		mdpprob[1] = mdpp;

		boolean isMDP = false;
		count = 0;
		int numtries = 0;
		tmpBelRegion[count] = new DD[iBelState_f.length];
		System.arraycopy(iBelState_f, 0, tmpBelRegion[count], 0,
				iBelState_f.length);
		stateConfig = null;
		nextStateConfig = null;
		double maxbeldiff, beldiff;
		while (count < maxSize && numtries < maxTries) {
			belState = iBelState_f;
			// figure out if we'll use the mdp or pomdp
			if (mdpp < 1.0)
				choice = OP.sampleMultinomial(mdpprob);
			else
				choice = 1;
			if (choice == 0)
				isMDP = false;
			else
				isMDP = true;
			if (isMDP)
				stateConfig = OP.sampleMultinomial(belState, varIndices);
			for (int stepId = 0; count < maxSize & stepId < episodeLength; stepId++) {
				// sample action
				choice = OP.sampleMultinomial(eprob);
				actId = 1;
				if (choice == 0) {
					if (isMDP)
						actId = policyQuery(Config.convert2dd(stateConfig),
								qFn, qPolicy);
					else
						actId = policyQuery(belState);
				} else {
					actId = Global.random.nextInt(nActions);
				}
				// System.out.println("choice "+choice+" action "+actId);
				// sample observation
				if (isMDP) {
					restrictedTransFn = OP.restrictN(actions[actId].transFn,
							stateConfig);
					nextStateConfig = OP.sampleMultinomial(restrictedTransFn,
							primeVarIndices);
					obsDistn = OP.restrictN(actions[actId].obsFn,
							concatenateArray(stateConfig, nextStateConfig));
					obsConfig = OP.sampleMultinomial(obsDistn, primeObsIndices);
				} else {
					obsDist = OP.addMultVarElim(
							concatenateArray(belState, actions[actId].transFn,
									actions[actId].obsFn),
							concatenateArray(varIndices, primeVarIndices));
					obsConfig = OP.sampleMultinomial(obsDist, primeObsIndices);
				}
				restrictedObsFn = OP.restrictN(actions[actId].obsFn, obsConfig);

				// update belState

				maxbeldiff = 0.0;
				for (int varId = 0; varId < nStateVars; varId++) {
					nextBelState[varId] = OP.addMultVarElim(
							concatenateArray(belState, actions[actId].transFn,
									restrictedObsFn),
							concatenateArray(
									MySet.remove(primeVarIndices, varId + nVars
											+ 1), varIndices));
					nextBelState[varId] = OP.approximate(nextBelState[varId],
							1e-6, zerovalarray);
					nextBelState[varId] = OP.div(nextBelState[varId], OP
							.addMultVarElim(nextBelState[varId],
									primeVarIndices[varId]));
					// nextBelState[varId].display();
					beldiff = OP.maxAll(OP.abs(OP.sub(
							OP.primeVars(nextBelState[varId], -nVars),
							belState[varId])));
					if (beldiff > maxbeldiff)
						maxbeldiff = beldiff;
				}
				numtries++;
				// make sure belief state has changed
				// System.out.println(" maxbeldiff "+maxbeldiff+" threshold "+threshold);
				if (stepId > 0 && maxbeldiff < threshold)
					break;

				belState = OP.primeVarsN(nextBelState, -nVars);
				if (isMDP)
					stateConfig = Config.primeVars(nextStateConfig, -nVars);

				// add belState to tmpBelRegion
				// printBeliefState(belState);
				distance = findSimilarFactBelief(belState, tmpBelRegion,
						count + 1, threshold);
				// System.out.println("distance "+distance);
				if (!debug && distance > threshold) {
					count = count + 1;
					if (count < maxSize) {
						// System.out.println("bel State : count "+count+" distance "+distance+" threshold "+threshold);

						tmpBelRegion[count] = new DD[belState.length];
						System.arraycopy(belState, 0, tmpBelRegion[count], 0,
								belState.length);
						if (count % 10 == 0)
							System.out.println(" " + count
									+ " belief states sampled");
					}
				}
				// System.out.println("Count is "+count);
				// add pure state to tmpBelRegion
				// if we're doing this for the MDP policy only
				if ((debug || isMDP) && count < maxSize) {
					ddState = new DD[nStateVars];
					for (int varId = 0; varId < nStateVars; varId++) {
						oneConfig[0][0] = stateConfig[0][varId];
						oneConfig[1][0] = stateConfig[1][varId];
						ddState[varId] = Config.convert2dd(oneConfig);
					}
					// printBeliefState(ddState);
					distance = findSimilarFactBelief(ddState, tmpBelRegion,
							count + 1, threshold);
					if (distance > threshold) {
						count = count + 1;
						if (count < maxSize) {
							// System.out.println("ddState : count "+count+" distance "+distance+" threshold "+threshold);

							tmpBelRegion[count] = new DD[ddState.length];
							System.arraycopy(ddState, 0, tmpBelRegion[count],
									0, ddState.length);
							if (count % 10 == 0)
								System.out.println(" " + count
										+ " belief states sampled");
						}
					}
				}
				Global.newHashtables();
			}
			// System.out.println("resetting to initial belief - "+count+" belief states so far");
		}
		// copy over
		if (count < maxSize)
			count = count + 1; // means we never found enough, so count is one
								// less than total we found
		System.out.println("finished sampling  " + count + " belief states  "
				+ tmpBelRegion.length);
		int ii = 0;
		if (iBelRegion != null) {
			belRegion = new DD[count + iBelRegion.length][];
			// copy over the ones that were passed in
			for (ii = 0; ii < iBelRegion.length; ii++) {
				belRegion[ii] = new DD[iBelRegion[ii].length];
				System.arraycopy(iBelRegion[ii], 0, belRegion[ii], 0,
						iBelRegion[ii].length);
			}

		} else {
			belRegion = new DD[count][];
			ii = 0;
		}
		// copy over the new ones
		for (int i = ii; i < ii + count; i++) {
			belRegion[i] = new DD[tmpBelRegion[i - ii].length];
			System.arraycopy(tmpBelRegion[i - ii], 0, belRegion[i], 0,
					tmpBelRegion[i - ii].length);
		}
	}

	// maxSize and maxTries here apply to each initial belief state - so there
	// will potentially be maxSize*number_of_inits belief states
	public void reachableBelRegionCurrentPolicyMultipleInits(int maxSize,
			int maxTries, int episodeLength, double threshold,
			double explorProb, double mdpp) {
		DD[] iBelState_f = new DD[nStateVars];
		belRegion = null;
		// search through adjuncts for names starting with init - these are the
		// multiple initial belief states
		for (int i = 0; i < adjunctNames.length; i++) {
			if (adjunctNames[i].startsWith("init")) {
				// adjuncts[i] is an initial belief state - do a simulation from
				// here
				for (int varId = 0; varId < nStateVars; varId++) {
					iBelState_f[varId] = OP.addMultVarElim(adjuncts[i],
							MySet.remove(varIndices, varId + 1));
				}
				System.out
						.println("generating belief region from initial belief "
								+ adjunctNames[i] + ":");
				printBeliefState(adjuncts[i]);
				reachableBelRegionCurrentPolicy(iBelState_f, belRegion,
						maxSize, maxTries, episodeLength, threshold,
						explorProb, mdpp);
			}
		}

	}

	public void readFromFile(String fileName) {
		readFromFile(fileName, false);
	}

	public void readFromFile(String fileName, boolean debb) {
		ParseSPUDD rawpomdp = new ParseSPUDD(fileName);
		rawpomdp.parsePOMDP(false);

		debug = debb;

		ignoremore = false;
		addbeldiff = false;
		nStateVars = rawpomdp.nStateVars;
		nObsVars = rawpomdp.nObsVars;
		nVars = nStateVars + nObsVars;
		stateVars = new StateVar[nStateVars];
		obsVars = new StateVar[nObsVars];

		varDomSize = new int[2 * (nStateVars + nObsVars)];
		varName = new String[2 * (nStateVars + nObsVars)];
		varIndices = new int[nStateVars];
		primeVarIndices = new int[nStateVars];
		obsIndices = new int[nObsVars];
		primeObsIndices = new int[nObsVars];

		// set up state variables
		int k = 0;
		for (int i = 0; i < nStateVars; i++) {
			stateVars[i] = new StateVar(rawpomdp.valNames.get(i).size(),
					rawpomdp.varNames.get(i), i);
			for (int j = 0; j < stateVars[i].arity; j++) {
				stateVars[i].addValName(j, ((String) rawpomdp.valNames.get(i)
						.get(j)));
			}
			// must be indices as in Matlab!
			varIndices[i] = i + 1;
			primeVarIndices[i] = i + nVars + 1;
			varDomSize[k] = stateVars[i].arity;
			varName[k++] = stateVars[i].name;
		}

		// set up observation variables
		nObservations = 1;
		obsVarsArity = new int[nObsVars];
		for (int i = 0; i < nObsVars; i++) {
			obsVars[i] = new StateVar(rawpomdp.valNames.get(i + nStateVars)
					.size(), rawpomdp.varNames.get(i + nStateVars), i
					+ nStateVars);
			for (int j = 0; j < obsVars[i].arity; j++) {
				obsVars[i]
						.addValName(j,
								((String) rawpomdp.valNames.get(nStateVars + i)
										.get(j)));
			}
			obsVarsArity[i] = obsVars[i].arity;
			nObservations = nObservations * obsVars[i].arity;
			obsIndices[i] = i + nStateVars + 1;
			primeObsIndices[i] = i + nVars + nStateVars + 1;
			varDomSize[k] = obsVars[i].arity;
			varName[k++] = obsVars[i].name;
		}
		for (int i = 0; i < nStateVars; i++) {
			varDomSize[k] = stateVars[i].arity;
			varName[k++] = stateVars[i].name + "_P";
		}
		for (int i = 0; i < nObsVars; i++) {
			varDomSize[k] = obsVars[i].arity;
			varName[k++] = obsVars[i].name + "_P";
		}

		// set up Globals
		Global.setVarDomSize(varDomSize);
		Global.setVarNames(varName);

		for (int i = 0; i < nStateVars; i++) {
			Global.setValNames(i + 1, stateVars[i].valNames);
		}
		for (int i = 0; i < nObsVars; i++) {
			Global.setValNames(nStateVars + i + 1, obsVars[i].valNames);
		}
		for (int i = 0; i < nStateVars; i++) {
			Global.setValNames(nVars + i + 1, stateVars[i].valNames);
		}
		for (int i = 0; i < nObsVars; i++) {
			Global.setValNames(nVars + nStateVars + i + 1, obsVars[i].valNames);
		}

		// set up dynamics
		nActions = rawpomdp.actTransitions.size();
		actions = new Action[nActions];
		uniquePolicy = new boolean[nActions];

		qFn = new DD[nActions];

		for (int a = 0; a < nActions; a++) {
			actions[a] = new Action(rawpomdp.actNames.get(a));
			actions[a].addTransFn(rawpomdp.actTransitions.get(a));
			actions[a].addObsFn(rawpomdp.actObserve.get(a));
			actions[a].rewFn = OP
					.sub(rawpomdp.reward, rawpomdp.actCosts.get(a));
			actions[a].buildRewTranFn();
			actions[a].rewFn = OP.addMultVarElim(actions[a].rewTransFn,
					primeVarIndices);

			// find stochastic transitions (and deterministic varMappings)
			// not done yet - where is this used?
		}
		// discount factor
		discFact = rawpomdp.discount.getVal();

		// make a DD version
		ddDiscFact = DDleaf.myNew(discFact);

		// the adjunct models
		nAdjuncts = rawpomdp.adjuncts.size();
		if (nAdjuncts > 0) {
			adjuncts = new DD[nAdjuncts];
			adjunctNames = new String[nAdjuncts];
			for (int a = 0; a < nAdjuncts; a++) {
				adjuncts[a] = rawpomdp.adjuncts.get(a);
				adjunctNames[a] = rawpomdp.adjunctNames.get(a);
			}
		}

		// max reward value

		double maxVal = Double.NEGATIVE_INFINITY;
		double minVal = Double.POSITIVE_INFINITY;
		for (int a = 0; a < nActions; a++) {
			maxVal = Math.max(maxVal, OP.maxAll(OP.addN(actions[a].rewFn)));
			minVal = Math.min(minVal, OP.minAll(OP.addN(actions[a].rewFn)));
		}
		maxRewVal = maxVal / (1 - discFact);
		// tolerance
		if (rawpomdp.tolerance == null) {
			double maxDiffRew = maxVal - minVal;
			double maxDiffVal = maxDiffRew / (1 - Math.min(0.95, discFact));
			tolerance = 1e-5 * maxDiffVal;
		} else {
			tolerance = rawpomdp.tolerance.getVal();
		}
		// initial belief
		initialBelState = rawpomdp.init;

		// factored initial belief state
		initialBelState_f = new DD[nStateVars];
		for (int varId = 0; varId < nStateVars; varId++) {
			initialBelState_f[varId] = OP.addMultVarElim(initialBelState,
					MySet.remove(varIndices, varId + 1));
		}

	}
	public void save(String filename) throws FileNotFoundException, IOException {

		FileOutputStream f_out;
		// save to disk
		// Use a FileOutputStream to send data to a file
		// called myobject.data.
		f_out = new FileOutputStream(filename);

		// Use an ObjectOutputStream to send object data to the
		// FileOutputStream for writing to disk.
		ObjectOutputStream obj_out = new ObjectOutputStream(f_out);

		// Pass our object to the ObjectOutputStream's
		// writeObject() method to cause it to be written out
		// to disk.
		obj_out.writeObject(this);
	}

	public static int[][] stackArray(int[] a, int[] b) {
		int[][] d = new int[2][a.length];
		System.arraycopy(a, 0, d[0], 0, a.length);
		System.arraycopy(b, 0, d[1], 0, b.length);
		return d;
	}

	public int[] statedecode(int statenum, int n) {
		int[] bases = new int[n];
		for (int i = 0; i < n; i++)
			bases[i] = 2;
		return statedecode(statenum, n, bases);
	}

	public int[] statedecode(int statenum, int n, int[] bases) {
		int[] statevec = new int[n];
		for (int i = 0; i < n; i++)
			statevec[i] = 0;

		if (statenum == 1) {
			for (int i = 0; i < n; i++)
				statevec[i] = 1;
			return statevec;
		}
		statenum--;
		int res = statenum;
		int remd;
		for (int i = 0; i < n; i++) {
			if (res == 1) {
				statevec[i] = 1;
				break;
			}
			remd = res % bases[i];
			res = ((int) Math.floor(res / bases[i]));
			statevec[i] = remd;
		}
		for (int i = 0; i < n; i++) {
			statevec[i]++;
		}
		return statevec;
	}
	
	public void solve(int nRounds, int maxSize, int maxTries,
			int episodeLength, double threshold, double explorProb,
			int nIterations, int maxAlpha, String basename) {
		solve(nRounds, maxSize, maxTries, episodeLength, threshold, explorProb,
				nIterations, maxAlpha, basename, false);
	}

	public void solve(int nRounds, int maxSize, int maxTries,
			int episodeLength, double threshold, double explorProb,
			int nIterations, int maxAlpha, String basename, boolean multinits) {

		// first solve the MDP underlying
		if (!debug && explorProb < 1.0)
			solveQMDP();

		// then loop over rounds
		// probability of using the MDP policy over the POMDP policy if the
		// choice is to exploit
		double mdpprob = 1.0;
		int firstStep = 0;
		String fname;
		int totaliterations = 0;
		maxAlphaSetSize = maxAlpha;

		for (int r = 0; r < nRounds; r++) {
			// generate a set of reachable belief points
			if (!multinits) {
				reachableBelRegionCurrentPolicy(maxSize, maxTries,
						episodeLength, threshold, explorProb, mdpprob);
			} else {
				reachableBelRegionCurrentPolicyMultipleInits(maxSize, maxTries,
						episodeLength, threshold, explorProb, mdpprob);
			}
			// possibly print this out (for debugging)
			// printBelRegion();
			// run symbolic Perseus
			if (debug)
				Global.setSeed(8837328);
			boundedPerseus(nIterations, maxAlphaSetSize, firstStep, nIterations);
			totaliterations = firstStep + nIterations;
			fname = basename + "-" + totaliterations + ".pomdp";
			System.out.println("saving current policy to " + fname);
			try {
				save(fname);
			} catch (FileNotFoundException err) {
				System.out.println("file not found error " + err);
				return;
			} catch (IOException terr) {
				System.out.println("file write error" + terr);
			}
			firstStep += nIterations;
			if (r == 0)
				mdpprob = 0.5;
		}
	}

	public void solveQMDP() {
		solveQMDP(50);
	}

	// solves the POMDP as an MDP
	public void solveQMDP(int count) {
		System.out.println("Computing qMDP policy");
		double bellmanErr = 2 * tolerance;
		DD valFn = DD.zero;
		DD prevValFn;
		DD[] cdArray;
		int actId1, actId2, actId;
		double[] zerovalarray = new double[1];
		DD[] tempQFn = new DD[nActions];
		zerovalarray[0] = 0;
		int iter = 0;
		while (bellmanErr > tolerance && iter < count) {
			System.out.println("iteration " + iter++);
			prevValFn = valFn;
			valFn = OP.primeVars(valFn, nVars);
			for (actId = 0; actId < nActions; actId++) {
				cdArray = concatenateArray(ddDiscFact, actions[actId].transFn,
						valFn);
				tempQFn[actId] = OP.addMultVarElim(cdArray, primeVarIndices);
				tempQFn[actId] = OP.add(actions[actId].rewFn, tempQFn[actId]);
				tempQFn[actId] = OP.approximate(tempQFn[actId], bellmanErr
						* (1 - discFact) / 2.0, zerovalarray);
			}
			valFn = OP.maxN(tempQFn);
			bellmanErr = OP.maxAll(OP.abs(OP.sub(valFn, prevValFn)));
			System.out.println("Bellman error: " + bellmanErr);
			Global.newHashtables();
		}
		// remove dominated alphaVectors
		boolean dominated;
		boolean[] notDominated = new boolean[nActions];
		for (actId1 = 0; actId1 < nActions; actId1++)
			notDominated[actId1] = false;
		for (actId1 = 0; actId1 < nActions; actId1++) {
			dominated = false;
			actId2 = 0;
			while (!dominated && actId2 < nActions) {
				if (notDominated[actId2]
						&& OP.maxAll(OP.sub(tempQFn[actId1], tempQFn[actId2])) < tolerance)
					dominated = true;
				actId2++;
			}
			if (!dominated)
				notDominated[actId1] = true;
		}
		int numleft = 0;
		for (actId1 = 0; actId1 < nActions; actId1++) {
			if (notDominated[actId1])
				numleft++;
		}
		qFn = new DD[numleft];
		qPolicy = new int[numleft];
		numleft = 0;
		for (actId1 = 0; actId1 < nActions; actId1++) {
			if (notDominated[actId1]) {
				qFn[numleft] = tempQFn[actId1];
				qPolicy[numleft] = actId1;
				numleft++;
			}
		}
	}

}
