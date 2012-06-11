package masg.catchProblem.files

abstract class AbstractSpuddFileMaker {
	abstract public void makeSpuddFile(String spuddFileName)
	
	protected int numAdjacent(int pos) {
		int posx=pos%5;
		int posy=pos/5;
		
		int adj=3;
		
		if(posx>0 && posx<4)
			adj++
		
		if(posy>0 && posy<4)
			adj++
	
		
		return adj
	}
	
	protected int distance(int pos1, int pos2) {
		int pos1x=pos1%5;
		int pos1y=pos1/5;
		
		int pos2x=pos2%5;
		int pos2y=pos2/5;
		
		return Math.abs(pos1x-pos2x) + Math.abs(pos1y-pos2y)
	}
}
