package masg.catchProblem.visualization

import processing.core.PApplet

class AgentBeliefViewerCell {
	float x,y;
	float w,h;
	float[] fillColor;
	double val = 0.0f;
	PApplet parent;
	
	AgentBeliefViewerCell(PApplet p, float tempX, float tempY, float tempW, float tempH, float[] fillColor) {
	  x = tempX;
	  y = tempY;
	  w = tempW;
	  h = tempH;
	  this.fillColor = fillColor;
	  parent = p
	}
	
	void display() {
	  parent.stroke(255);
	  parent.fill(fillColor[0],fillColor[1],fillColor[2])
	  parent.rect(x,y,w,h)
	}
}
