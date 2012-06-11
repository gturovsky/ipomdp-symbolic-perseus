package masg.catchProblem.simulator.grid

import java.util.Random;

class AgentState {
	
	public x = 0
	public y = 0
	
	static Random random = new Random()
	
	int stateNumber(CatchRectangularGrid grid) {
		return y*grid.width+x
	}
	
	CatchGridDirection moveRandomDirection(CatchRectangularGrid grid) {
		int NSorEW = random.nextInt(2)
		CatchGridDirection dir
		
		if(NSorEW == 1) //north south
		{
			int NorS = random.nextInt(2)
			
			if(NorS == 1) //north
			{
				dir = CatchGridDirection.N
			}
			else{
				dir = CatchGridDirection.S
			}
		}
		else //east west
		{
			int EorW = random.nextInt(2)
			
			if(EorW == 1) //east
			{
				dir = CatchGridDirection.E
				
			}
			else {
				dir = CatchGridDirection.W
			}
		}
		
		moveDirection(dir,grid)
		return dir
	}
	
	void moveDirection(CatchGridDirection dir, CatchRectangularGrid grid) {
		//println "DEBUG:Moving $dir from $x , $y"
		switch(dir) {
			case CatchGridDirection.N:
				if(y<grid.height-1)
					y++
				break
			case CatchGridDirection.S:
				if(y>0)
					y--
				break
			case CatchGridDirection.E:
				if(x<grid.width-1)
					x++
				break
			case CatchGridDirection.W:
				if(x>0)
					x--
				break
		}
		
		//println "DEBUG: to $x , $y"
		
	}
	
	public String toString() {
		return "x:$x y:$y"
	}
}
