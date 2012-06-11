package masg.symbolicPerseus;

import masg.symbolicPerseus.dd.DD;
import masg.symbolicPerseus.dd.OP;
import masg.symbolicPerseus.pomdp.POMDP;

public class NextBelState {
	DD[][] nextBelStates;
	int[] nzObsIds;
	double[][] obsVals;
	int numValidObs;
	public int[] obsStrat;
	double[] obsValues;
	double sumObsValues;

	POMDP p;
	public NextBelState(double[] obsProbs, double smallestProb, POMDP pomdp) {
		p = pomdp;
				
		numValidObs = 0;
		for (int i = 0; i < obsProbs.length; i++)
			if (obsProbs[i] > smallestProb)
				numValidObs++;
		// System.out.println("number of valid observations "+numValidObs);
		nextBelStates = new DD[numValidObs][p.nStateVars + 1];
		nzObsIds = new int[numValidObs];
		obsStrat = new int[p.nObservations];
		obsValues = new double[numValidObs];
		int j = 0;
		for (int i = 0; i < obsProbs.length; i++)
			if (obsProbs[i] > smallestProb)
				nzObsIds[j++] = i;
		
		
	}

	public NextBelState(NextBelState a) {
		p = a.p;
		nextBelStates = new DD[a.nextBelStates.length][a.nextBelStates[0].length];
		for (int i = 0; i < a.nextBelStates.length; i++) {
			for (int j = 0; j < a.nextBelStates[i].length; j++)
				nextBelStates[i][j] = a.nextBelStates[i][j];
		}
		obsVals = new double[a.obsVals.length][];
		for (int i = 0; i < a.obsVals.length; i++)
			obsVals[i] = a.obsVals[i];
		obsStrat = a.obsStrat;

		nzObsIds = a.nzObsIds;
		numValidObs = a.numValidObs;
		obsValues = a.obsValues;
		sumObsValues = a.sumObsValues;
	}

	public boolean isempty() {
		return (numValidObs == 0);
	}

	public void restrictN(DD[] marginals, int[][] obsConfig) {
		int obsId;
		for (int obsPtr = 0; obsPtr < numValidObs; obsPtr++) {
			obsId = nzObsIds[obsPtr];
			nextBelStates[obsPtr] = OP.restrictN(marginals,
					POMDP.stackArray(p.primeObsIndices, obsConfig[obsId]));
		}
	}

	// get the observation values
	// obsVals[i][j] is the value expected if we see observation i
	// and then follow the conditional plan j
	public void getObsVals(DD[] primedV) {
		if (!isempty()) {
			obsVals = new double[numValidObs][primedV.length];
			obsVals = OP.factoredExpectationSparseNoMem(nextBelStates,
					primedV);
		}
	}

	public double getSumObsValues() {
		return sumObsValues;
	}

	// get observation strategy
	// obsStrat[i] is the best conditional to plan to follow
	// if observation i is seen
	// this is just the index that maximizes over the obsVals
	// and alphaValue is the value of that conditional plan given than
	// observation
	public void getObsStrat() {
		double alphaValue = 0;
		sumObsValues = 0;
		int obsId;
		double obsProb;
		for (int obsPtr = 0; obsPtr < p.nObservations; obsPtr++) {
			obsStrat[obsPtr] = 0;
		}
		for (int obsPtr = 0; obsPtr < numValidObs; obsPtr++) {
			obsId = nzObsIds[obsPtr];
			obsProb = nextBelStates[obsPtr][p.nStateVars].getVal();
			alphaValue = obsVals[obsPtr][0];
			for (int i = 1; i < obsVals[obsPtr].length; i++) {
				if (obsVals[obsPtr][i] > alphaValue) {
					alphaValue = obsVals[obsPtr][i];
					obsStrat[obsId] = i;
				}
			}
			obsValues[obsPtr] = obsProb * alphaValue;

			sumObsValues += obsValues[obsPtr];
		}
	}
}
