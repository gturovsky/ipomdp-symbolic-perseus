package masg.catchProblem.visualization

import processing.core.PApplet

class WumpusLocationExperimentCell {
	float x,y; 
	float w,h;
	float fillColor;
	PApplet parent;
	
	// Cell Constructor
	WumpusLocationExperimentCell(PApplet p, float tempX, float tempY, float tempW, float tempH, float fillColor) {
	  x = tempX;
	  y = tempY;
	  w = tempW;
	  h = tempH;
	  this.fillColor = fillColor;
	  parent = p
	}
	
	void display() {
	  parent.stroke(255);
	  parent.fill(fillColor)
	  parent.rect(x,y,w,h)
	}
}
