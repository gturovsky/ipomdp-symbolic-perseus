package masg.catchProblem.simulator.grid

class WorldState {
	static Random random = new Random()
	Map<String,AgentState> states = [:]
	
	static WorldState randomState(List<String> stateNames, CatchRectangularGrid grid) {
		WorldState ws = new WorldState()
		ws.states = [:]
		stateNames.each { it ->
			ws.states[it] = new AgentState()
			ws.states[it].x = random.nextInt(grid.width)
			ws.states[it].y = random.nextInt(grid.height)
		}
		
		return ws
	}
	
	
}
