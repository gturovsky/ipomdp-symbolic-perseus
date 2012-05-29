package masg
import masg.simulator.SimulatorPOMDP;

import org.flyhighplato.spudder.Spudder
import symbolicPerseus.POMDP


class POMDPRunner {
	public static int nRounds = 1;
	public static int nIterations = 50; // backup iterations per round
	public static int maxAlphaSetSize = 10000;
	public static int numBelStates = 10000;
	public static int maxBelStates = 10000;
	public static int episodeLength = 50; // when generating belief points
	public static double threshold = 0.0001;
	public static double explorProb = 0.0;
	
	static int width = 5
	static int height = 5
	
	public static void main(String[] args){
		String spuddFileName = "problem_POMDP.SPUDD"
		
		AbstractSpuddFileMaker ps
		
		ps = new POMDPSpuddFileMaker(width, height)
		//ps = new IPOMDPL0SpuddFileMaker(width,height)
		ps.makeSpuddFile(spuddFileName)
		
		POMDPRunner runner = new POMDPRunner()
		POMDP solvedPOMDP1 = runner.solveSpuddFile(spuddFileName)
		POMDP solvedPOMDP2 = runner.solveSpuddFile(spuddFileName)
		
		SimulatorPOMDP sim = new SimulatorPOMDP([1:solvedPOMDP1,2:solvedPOMDP2])
		
		int numRuns = 1000
		int runLength = 100
		
		int totColocations = 0
		numRuns.times{
			println "Starting run #$it"
			totColocations += sim.simulate(runLength)
		}
		
		println "Total colocations: $totColocations"
		println "Avg colocations:${(float)totColocations/(float)numRuns}"
	}
	
	public POMDP solveSpuddFile(String spuddFileName) {
		POMDP pomdp = new POMDP(spuddFileName)
		pomdp.solve(nRounds, numBelStates, maxBelStates, episodeLength,
					threshold, explorProb, nIterations, maxAlphaSetSize,
					spuddFileName.substring(0, spuddFileName.lastIndexOf(".")), false)
		
		return pomdp
	}

}
