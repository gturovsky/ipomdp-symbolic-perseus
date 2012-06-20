package masg.catchProblem.simulator

import masg.catchProblem.simulator.grid.AgentState
import masg.catchProblem.simulator.grid.CatchGridDirection
import masg.catchProblem.simulator.grid.CatchRectangularGrid
import masg.catchProblem.simulator.grid.WorldState
import masg.catchProblem.visualization.TraceViewer
import masg.test.symbolicPerseus.DD
import masg.test.symbolicPerseus.POMDP
import masg.test.symbolicPerseus.OP
import masg.test.symbolicPerseus.Global
/*import masg.symbolicPerseus.Global
import masg.symbolicPerseus.dd.DD
import masg.symbolicPerseus.dd.OP
import masg.symbolicPerseus.pomdp.POMDP*/

class SimulatorPOMDP {
	private final Map<Integer,POMDP> p = [:]
	static private SimulatorPOMDP sim
	
	public SimulatorPOMDP(Map<Integer,POMDP> solvedPomdps) {
		p = solvedPomdps
	}
	
	private int getActionFromPolicy(Integer agentId, DD belState) {
		return p[agentId].policyQuery(belState)
	}
	
	private DD getNextBelief(Integer agentId,DD currBelief, int actId, String[] obsNames) {
		return p[agentId].beliefUpdate(currBelief, actId, obsNames);
	}
	
	private DD getInitialBelief(Integer agentId) {
		return p[agentId].initialBelState
	}
	
	private String getCollObservation(AgentState agent, AgentState wumpus, String actionName) {
		if(Math.abs(agent.x-wumpus.x) + Math.abs(agent.y - wumpus.y)<=1)
			return "coll_1"
		else
			return "coll_0"
	}
	
	private String getAlocObservation(AgentState agent,CatchRectangularGrid grid) {
		return "aloc_${agent.y*grid.width + agent.x}"
	}
	
	public boolean isColocation(AgentState a, AgentState w) {
		return (a.x==w.x && a.y==w.y)
	}
	
	public int simulate(int numTrials, int runLength) {
		
		Map<Integer, Map<Integer,Integer>> wumpusPositionsPerStep = [:]
		
		int totalColocations = 0
		numTrials.times { runNumber ->
			//FileWriter statsOut = new FileWriter("runStats${runNumber}.txt")
			println "Starting run #$runNumber"
			
			Map<Integer,DD> belStates = [:]
			
			p.each{k,v ->
				belStates[k] = v.initialBelState
			}
			
			CatchRectangularGrid grid = new CatchRectangularGrid(5,5)
			WorldState ws = WorldState.randomState(["apos1","apos2","wpos"],grid)
			
			TraceViewer viewer = new TraceViewer()
			
			int colocations = 0
			runLength.times { stepNumber ->
				
				if(!wumpusPositionsPerStep.containsKey(stepNumber))
					wumpusPositionsPerStep[stepNumber]=[:]
					
				println "Actual state: $ws.states"
				
				viewer.a1Pos = ws.states["apos1"].stateNumber(grid) + 1
				viewer.a2Pos = ws.states["apos2"].stateNumber(grid) + 1
				viewer.wPos = ws.states["wpos"].stateNumber(grid) + 1
				
				int wumpusLoc = ws.states["wpos"].stateNumber(grid) + 1
				if(!wumpusPositionsPerStep[stepNumber].containsKey(wumpusLoc))
					wumpusPositionsPerStep[stepNumber][wumpusLoc]=1
				else
					wumpusPositionsPerStep[stepNumber][wumpusLoc]+=1
				
				viewer.drawTextGrid(grid.height, grid.width)
				
				Map<Integer,String> actNames = [:]
				Map<Integer,String> actIds = [:]
				belStates.each{agentId, belState ->
					
					int actId = getActionFromPolicy(agentId,belState)
					String actName = p[agentId].actions[actId].name
					
					println "Agent #$agentId action: $actId (${actName})"
					
					actNames[agentId] = actName
					actIds[agentId] = actId
					
					if(actNames[agentId] == "north")
						ws.states["apos$agentId"].moveDirection(CatchGridDirection.N, grid)
					else if(actNames[agentId] == "south")
						ws.states["apos$agentId"].moveDirection(CatchGridDirection.S, grid)
					else if(actNames[agentId] == "east")
						ws.states["apos$agentId"].moveDirection(CatchGridDirection.E, grid)
					else if(actNames[agentId] == "west")
						ws.states["apos$agentId"].moveDirection(CatchGridDirection.W, grid)
				}
				println "Wumpus action: " +  ws.states["wpos"].moveRandomDirection(grid)
				
				//statsOut.write("" + (ws.states["apos1"].stateNumber(grid) + 1) + " " + (ws.states["apos2"].stateNumber(grid) + 1) + " " + (ws.states["wpos"].stateNumber(grid) + 1) + "\n")
				
				Map<Integer,DD> belStatesNew = [:]
				belStates.each{agentId, belState ->
					String[] obsNames = new String[2]
					obsNames[0] = getAlocObservation( ws.states["apos$agentId"], grid )
					obsNames[1] = getCollObservation( ws.states["apos$agentId"], ws.states["wpos"], p[agentId].actions[actIds[agentId]].name )
					
					assert obsNames[0] == "aloc_" + ws.states["apos$agentId"].stateNumber(grid)
					
					println "Agent #$agentId observation: $obsNames"
					
					belStatesNew[agentId] = getNextBelief(agentId,belState, actIds[agentId], obsNames)
					
					
					
					/*DD wumpusPosBelief = OP.addMultVarElim(belStatesNew[agentId],[1] as int[])
					
					for (int i = 0; i < wumpusPosBelief.children.length; i++) {
						statsOut.write("$agentId ${Global.valNames[wumpusPosBelief.var - 1][i]} ${wumpusPosBelief.children[i].val}\n")
					}*/
					
				}
				
				//statsOut.write("\n")
				belStates = belStatesNew
				
				if(isColocation(ws.states["apos1"], ws.states["wpos"]) || isColocation(ws.states["apos2"], ws.states["wpos"]))
					colocations ++
				
				println "Colocations: $colocations"
				println " "
				
			}
			//statsOut.close()
			totalColocations+=colocations
		
		}
		
		new File("wumpusLocationStats.txt").withWriter { out ->
			wumpusPositionsPerStep.each { stepNumber, locStats ->
				out.writeLine("$stepNumber")
				locStats.each { locNumber, locTimes ->
					out.writeLine("$locNumber $locTimes")
				}
				out.writeLine("")
			}
		}
		
		return totalColocations
	}
	
}
