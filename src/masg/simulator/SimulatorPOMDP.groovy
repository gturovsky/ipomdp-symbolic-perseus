package masg.simulator
import masg.simulator.grid.AgentState
import masg.simulator.grid.CatchGridDirection
import masg.simulator.grid.CatchRectangularGrid
import masg.simulator.grid.WorldState
import symbolicPerseus.DD;
import symbolicPerseus.POMDP


class SimulatorPOMDP {
	private final Map<Integer,POMDP> p = [:]
	//private final Map<Integer,POMDP> p_alt = [:]
	static private SimulatorPOMDP sim
	
	public static void main(String[] args){
		POMDP p = POMDP.load("problem_POMDP-150.pomdp")
		sim = new SimulatorPOMDP(p)
	}
	
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
	
	public int simulate(int runLength) {
		
		
		
		Map<Integer,DD> belStates = [:]
		
		p.each{k,v ->
			belStates[k] = v.initialBelState
		}
		
		CatchRectangularGrid grid = new CatchRectangularGrid(5,5)
		WorldState ws = WorldState.randomState(["apos1","apos2","wpos"],grid)
		
		/*ws.states["apos1"].x = 1
		ws.states["apos1"].y = 4
		
		ws.states["apos2"].x = 2
		ws.states["apos2"].y = 0
		
		ws.states["wpos"].x = 0
		ws.states["wpos"].y = 0*/
		
		TraceViewer viewer = new TraceViewer()
		
		
		int colocations = 0
		runLength.times {
			
			println "Actual state: $ws.states"
			
			viewer.a1Pos = ws.states["apos1"].stateNumber(grid) + 1
			viewer.a2Pos = ws.states["apos2"].stateNumber(grid) + 1
			viewer.wPos = ws.states["wpos"].stateNumber(grid) + 1
		
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
			
			Map belStatesNew = [:]
			belStates.each{agentId, belState ->
				String[] obsNames = new String[2]
				obsNames[0] = getAlocObservation( ws.states["apos$agentId"], grid )
				obsNames[1] = getCollObservation( ws.states["apos$agentId"], ws.states["wpos"], p[agentId].actions[actIds[agentId]].name )
				
				assert obsNames[0] == "aloc_" + ws.states["apos$agentId"].stateNumber(grid)
				
				println "Agent #$agentId observation: $obsNames"
				
				belStatesNew[agentId] = getNextBelief(agentId,belState, actIds[agentId], obsNames)
				
			}
			belStates = belStatesNew
			
			if(isColocation(ws.states["apos1"], ws.states["wpos"]) || isColocation(ws.states["apos2"], ws.states["wpos"]))
				colocations ++
			
			println "Colocations: $colocations"
			println " "
			
		}
		
		return colocations
		
	}
	
}
