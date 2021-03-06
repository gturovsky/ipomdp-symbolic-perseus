package masg
import masg.catchProblem.files.AbstractSpuddFileMaker;
import masg.catchProblem.files.IPOMDPL0SpuddFileMaker
import masg.catchProblem.files.IPOMDPNextLevelSpuddFileMaker
import masg.catchProblem.files.POMDPSpuddFileMaker;
import masg.catchProblem.files.PolicyExtractor
import masg.catchProblem.simulator.SimulatorPOMDP;
//import masg.symbolicPerseus.pomdp.POMDP

import org.flyhighplato.spudder.Spudder
import masg.test.symbolicPerseus.POMDP
import masg.test.symbolicPerseus.OP


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
		maxAlphaSetSize = 100
		numBelStates = 100
		maxBelStates = 100
		
		String spuddFileName = "problem_POMDP.SPUDD"
		
		AbstractSpuddFileMaker ps
		
		ps = new POMDPSpuddFileMaker(width, height)
		//ps = new IPOMDPL0SpuddFileMaker(width,height)
		ps.makeSpuddFile(spuddFileName)
		
		POMDPRunner runner = new POMDPRunner()
		POMDP solvedPOMDP1 = runner.solveSpuddFile(spuddFileName)
		
		POMDP solvedPOMDP2 = runner.solveSpuddFile(spuddFileName)
		
		PolicyExtractor extractor = new PolicyExtractor(solvedPOMDP2)
		
		spuddFileName = "problem_IPOMDPL1.SPUDD"
		IPOMDPNextLevelSpuddFileMaker ps1 = new IPOMDPNextLevelSpuddFileMaker(width,height,solvedPOMDP2,extractor.policyNodes)
		ps1.makeSpuddFile(spuddFileName)
		
		POMDP solvedPOMDP1_L1 = runner.solveSpuddFile(spuddFileName)
		
		SimulatorPOMDP sim = new SimulatorPOMDP([1:solvedPOMDP1_L1,2:solvedPOMDP2])
		
		int numRuns = 1000
		int runLength = 100
		
		int totColocations = sim.simulate(numRuns,runLength)
		
		println "Total colocations: $totColocations"
		println "Avg colocations:${(float)totColocations/(float)numRuns}"
	}
	
	public static void savePolicy(POMDP p, String fileName) {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName, false));
		out.writeObject(p.policy)
		
		p.alphaVectors.each{
			out.writeObject(OP.convert2array(it))
		}
		
		out.close()
	}
	
	public POMDP solveSpuddFile(String spuddFileName) {
		POMDP pomdp = new POMDP(spuddFileName)
		pomdp.solve(nRounds, numBelStates, maxBelStates, episodeLength,
					threshold, explorProb, nIterations, maxAlphaSetSize,
					spuddFileName.substring(0, spuddFileName.lastIndexOf(".")), false)
		
		return pomdp
	}

}
