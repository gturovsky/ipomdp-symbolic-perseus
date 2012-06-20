package masg.catchProblem.visualization

import processing.core.PApplet

class AgentBeliefViewer extends PApplet{
	private class Step {
		int a1Pos = -1
		int a2Pos = -1
		int wPos = -1
		
		List<Map> aBeliefs = []
		
		public Step() {
			aBeliefs << [:]
			aBeliefs << [:]
		}
	}
	
	
	// 2D Array of objects
	AgentBeliefViewerCell[][] grid;
	
	// Number of columns and rows in the grid
	int cols = 5;
	int rows = 5;
	
	Map cellValues = [:]
	int drawCount = 0;
	float colorMultiplier=255.0f*25.0f;
	
	FileReader reader;
	String fileName
	
	int windowWidth=800;
	int windowHeight=800;
	
	List<Step>  steps = []
	Step currentStep = null
	int currentStepIx = 0
	int agentPOV = 0
	
	public static void main(String[] args){
		PApplet.main(["--present", "masg.visualization.AgentBeliefViewer"] as String[])
	}
	
	void setup() {
		fileName = "/Users/garyturovsky/Code/ipomdp-symbolic-perseus/runStats99.txt"
		reader = new FileReader(fileName);

		size(windowWidth,windowHeight+200);
		int cellWidth = windowWidth/cols;
		int cellHeight = windowHeight/rows

		grid = new AgentBeliefViewerCell[cols][rows];

		for (int i = 0; i < cols; i ++ ) {
			for (int j = 0; j < rows; j ++ ) {
				grid[i][j] = new AgentBeliefViewerCell(this,i*cellWidth,j*cellHeight,cellWidth,cellHeight,[0.0f,0.0f,0.0f] as float[]);
			}
		}
		
		steps = readInAllSteps()
		currentStepIx = 0
		currentStep = steps[currentStepIx]
	}
	
	void draw() {
	  background(0);

	  for (int i = 0; i < cols; i ++ ) {
		  for (int j = 0; j < rows; j ++ ) {
		
			int ix = j*5 + i + 1
			int[] cellCoords = getCoords(ix)
			
			assert cellCoords[0] == j
			assert cellCoords[1] == i

			if(currentStep.aBeliefs[agentPOV].containsKey(ix) ) {
				grid[i][j].fillColor[0]=currentStep.aBeliefs[agentPOV][ix]*255.0f*25.0f
			}
			else {
				grid[i][j].fillColor[0]=0
			}
			
			grid[i][j].display();
			
			StringBuilder sb = new StringBuilder();
			Formatter formatter = new Formatter(sb, Locale.US);
			formatter.format("%f", currentStep.aBeliefs[agentPOV][ix])
	
			fill(150,150,150)
			text("$sb",(float)grid[i][j].x+5.0,(float)grid[i][j].y+grid[i][j].h-grid[i][j].h/8,(float)grid[i][j].w,(float)grid[i][j].h/4)
			
			int[] a1Coords = getCoords(currentStep.a1Pos)
			if(j == a1Coords[0] && i == a1Coords[1]) {
				fill(0,255,0)
				float x = grid[i][j].x+grid[i][j].w/4
				float y = grid[i][j].y+grid[i][j].w/4
				float w = grid[i][j].w/4
				float h = grid[i][j].h/4
				ellipse(x, y, w, h)
				fill(0,0,0)
				text("1",(float)x-5.0f,(float)y-5.0f,w,h)
			}
			
			int[] a2Coords = getCoords(currentStep.a2Pos)
			if(j == a2Coords[0] && i == a2Coords[1]) {
				fill(0,255,0)
				float x = grid[i][j].x+grid[i][j].w/2
				float y = grid[i][j].y+grid[i][j].w/4
				float w = grid[i][j].w/4
				float h = grid[i][j].h/4
				ellipse(x, y, w, h)
				fill(0,0,0)
				text("2",(float)x-5.0f,(float)y-5.0f,w,h)
			}
			
			if(j == Math.floor((currentStep.wPos-1)/5) && i == (currentStep.wPos-1) - Math.floor((currentStep.wPos-1)/5)*5) {
				fill(0,0,255)
				float x = grid[i][j].x+grid[i][j].w/2
				float y = grid[i][j].y + grid[i][j].h/2
				float w = grid[i][j].w/4
				float h = grid[i][j].h/4
				ellipse(x, y, w, h)
				fill(0,0,0)
				text("W",(float)x-5.0f,(float)y-5.0f,w,h)
			}
		  }
	  	}
	  
	  fill(255,255,255)
	  text("Step #$currentStepIx",(float)windowWidth/2,(float)windowHeight+10,windowWidth/2,100)
	}

	private Step readNextStep() {
		
		Step nextStep
		
		String line = reader.readLine()
		
		double maxValue = 0.0f
		
		if(line!=null) {
			nextStep = new Step()
			String[] positions = line.split(" ")
			
			nextStep.a1Pos = Integer.parseInt(positions[0])
			nextStep.a2Pos = Integer.parseInt(positions[1])
			nextStep.wPos = Integer.parseInt(positions[2])
			
			
			//Read in all the belief lines
			while(true) {
				line = reader.readLine()
				
				if(!line || line == "")
					break

				String[] wposB = line.split(" ")
				
				double val = Double.parseDouble(wposB[2])
				int ix = Integer.parseInt(wposB[1].split("_")[1])+1
				
				if(wposB[0]=="1") {
					nextStep.aBeliefs[0][ix]=val;
				}
				else if(wposB[0]=="2") {
					nextStep.aBeliefs[1][ix]=val;
				}
			}
		}
		
		return nextStep;
	}
	
	private List<Step> readInAllSteps() {
		List<Step>  steps = []
		Step step = readNextStep()
		while(step) {
			steps << step
			step = readNextStep()
		}
		return steps
	}
	
	void nextStep() {
		if(currentStepIx < steps.size()-1)
			currentStepIx++
		
		currentStep = steps[currentStepIx]
	}
	
	void previousStep() {
		if(currentStepIx > 0)
			currentStepIx--
		
		currentStep = steps[currentStepIx]
	}
	
	void keyPressed() {
		if(key==CODED) {
			if(keyCode == RIGHT) {
				nextStep()
			}
			else if(keyCode == LEFT) {
				previousStep()
			}
		}
	}
	
	public int[] getCoords(int pos) {
		return [Math.floor((pos-1)/5), (pos-1) - Math.floor((pos-1)/5)*5 ] as int[]
	}
}
