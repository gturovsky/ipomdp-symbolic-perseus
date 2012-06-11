package masg.catchProblem.visualization

import Jama.EigenvalueDecomposition
import Jama.Matrix

class WumpusSteadyState {
	
	public static void main(String[] args){
		WumpusSteadyState s = new WumpusSteadyState()
	}
	
	WumpusSteadyState() {
		Matrix initState = new Matrix(1,25)
		25.times{initState.set(0, it, 1.0f/25.0f)}
		//initState.set(0, 14, 1.0f)

		Matrix transition = new Matrix(25,25)
		
		25.times{ i ->
			List adj = getAdjacent(i+1)
			adj.eachWithIndex { j, val ->
				if(i==j-1) {
					
					transition.set(i, j-1, 0.20f + (float)(5-adj.size())*0.20f)
				}
				else
					transition.set(i, j-1, 0.20f)
			}
		}
		
		transition.print(5,2)
		solveAlgebraically(transition.copy())
		println("")
		solveByIteration(initState.copy(),transition.copy())
		
		
	}
	
	void solveByIteration(Matrix startState, Matrix T) {
		100000.times {
			startState = startState.times(T)
		}
		
		double sum = 0
		5.times{ y ->
			5.times{ x->
				sum += startState.array[0][y*5+x]
				printf "|%f ",startState.array[0][y*5+x]
			}
			println "|"
		}
		println "Does it sum to 1? $sum"
	}
	void solveAlgebraically(Matrix T) {
		//q(T-I)=0 (where T is the transition matrix and I is the identity and we are solving for q)
		Matrix newT = T.minus(Matrix.identity(25, 25))
		
		//Add that q_1+q_2+...+q_25=1 to this solver
		Matrix P = new Matrix(25,26)
		25.times{ i ->
			25.times{ j ->
				P.set(i, j, newT.get(i, j))
			}
		}
		
		25.times{ i ->
			P.set(i, 25, 1.0f)
		}
		
		Matrix B = new Matrix(1,26)
		B.set(0, 25, 1.0)
		
		//Solve it
		P = P.solveTranspose(B)
		
		//Display it
		P.array.eachWithIndex{ item, ix ->
			printf "|%f ",item[0]
			
			if(ix>0 && (ix+1)%5==0)
				println "|"
		}
	}
	
	List getAdjacent(int ix) {
		ix = ix-1;
		int y = ix/5;
		int x = ix%5;
		
		List possibleLocs = [ix+1]
		
		if(x>0)
			possibleLocs << ix+1-1
		if(x<4)
			possibleLocs << ix+1+1
		if(y>0)
			possibleLocs << ix+1-5
		if(y<4)
			possibleLocs << ix+1+5
			
		return possibleLocs
	}
}
