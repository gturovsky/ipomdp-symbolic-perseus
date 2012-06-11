package masg.catchProblem.visualization

import processing.core.PApplet

class WumpusLocationExperimentViewer extends PApplet{
	// 2D Array of objects
	WumpusLocationExperimentCell[][] grid;
	
	// Number of columns and rows in the grid
	int cols = 5;
	int rows = 5;
	
	Map cellValues = [:]
	int drawCount = 0;
	int colorMultiplier=3;
	
	FileReader reader;
	String fileName
	
	//public static void main(String args[]) {
		//PApplet.main(new String[] { "--present", "MyProcessingSketch" });
	//}
	
	public static void main(String[] args){
		PApplet.main(["--present", "masg.visualization.WumpusLocationExperimentViewer"])
	}
	
	void setup() {
		fileName = "/Users/garyturovsky/Code/ipomdp-symbolic-perseus/wumpusLocationStats.txt"
		reader = new FileReader(fileName);
		
		int windowWidth=800;
		int windowHeight=800;
		
		size(windowWidth,windowHeight);
		int cellWidth = windowWidth/cols;
		int cellHeight = windowHeight/rows

		grid = new WumpusLocationExperimentCell[cols][rows];

		for (int i = 0; i < cols; i ++ ) {
			for (int j = 0; j < rows; j ++ ) {
				grid[i][j] = new WumpusLocationExperimentCell(this,i*cellWidth,j*cellHeight,cellWidth,cellHeight,255);
			}
		}
	}
	
	void draw() {
		drawCount++;
		
		if(drawCount>10) {
			drawCount=0;
			
			String line = reader.readLine()
			
			/*if(line==null)
			{
				reader = new FileReader(fileName);
				line = reader.readLine()
			}*/
			
			if(line!=null) {
				cellValues = [:]
				if(line!=null) {
					int stepId = Integer.parseInt(line)
		
					while(line=reader.readLine()) {
						String[] vals = line.split(" ")
						cellValues[Integer.parseInt(vals[0])-1] = Integer.parseInt(vals[1])
					}
				}
			}
		}
	  background(0);
	  for (int i = 0; i < cols; i ++ ) {
		for (int j = 0; j < rows; j ++ ) {
			if(cellValues.containsKey(i*j))
				grid[i][j].fillColor=cellValues[i*j]*colorMultiplier
			else
				grid[i][j].fillColor=0
			grid[i][j].display();
		}
	  }
	}
}
