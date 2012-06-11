package masg.catchProblem.files

import org.flyhighplato.spudder.Spudder

class IPOMDPL0SpuddFileMaker extends AbstractSpuddFileMaker{
	private int width, height
	public IPOMDPL0SpuddFileMaker(int w, int h) {
		width = w
		height = h
	}
	
	@Override
	public void makeSpuddFile(String spuddFileName) {
			double totalStates = Math.pow((double)width*(double)height,3.0f)
			Range posRange = 0..<(width*height)
			Range collRange = 0..<2
			Spudder spudder = new Spudder( 
									"problem.SPUDD",
									[	"apos1":posRange.collect{it-> "$it"},
										"apos2":posRange.collect{it-> "$it"},
										"wpos":posRange.collect{it-> "$it"}
									],
									[	"aloc":posRange.collect{it-> "$it"},
										"coll":collRange.collect{it-> "$it"}
									]
								)
			spudder.variables()
			spudder.observations()
			spudder.initBelief()
					.withVariable("apos1")
					.withVariable("apos2")
					.withVariable("wpos")
					.hasValue { args ->
						return String.format("%.20f",1.0f/totalStates)
					}
					
			spudder.dd("wumpusb")
					.withVariable("apos1")
					.withVariable("wpos")
					.then()
					.withVariable("wpos")
					.hasValue { args ->
						int numWalls = 5-numAdjacent(Integer.parseInt(args["wpos"]))
						if(args["wpos"] == args["wpos'"]){
							return String.format("%.5f",(1.0f+numWalls)/5.0f)
						}
						else if(distance(Integer.parseInt(args["wpos"]),Integer.parseInt(args["wpos'"]))==1)
						{
							return String.format("%.5f",0.2f)
						}
						else
						{
							return "0.0"
						}
					}
			
			spudder.dd("ldd")
					.then()
					.withVariable("apos1")
					.withObservation("aloc")
					.hasValue {args ->
						if(args["apos1'"] == args["aloc'"])
							return 1.0f
						else
							return 0.0f
					}
			
			spudder.dd("sensingdd")
					.then()
					.withVariable("apos1")
					.withVariable("wpos")
					.withObservation("coll")
					.hasValue { args ->
						if(args["coll'"] == "1")
							if(distance(Integer.parseInt(args["apos1'"]),Integer.parseInt(args["wpos'"]))<=1)
								return 1.0f
							else
								return 0.0f
						else
							if(distance(Integer.parseInt(args["apos1'"]),Integer.parseInt(args["wpos'"]))<=1)
								return 0.0f
							else
								return 1.0f
					}
			
			spudder.action("north")
					.withVariableTransition("apos1",
						spudder.actionTree()
								.withVariable("apos1")
								.then()
								.withVariable("apos1")
								.hasValue { args ->
									int oldPos = Integer.parseInt(args["apos1"])
									int newPos = Integer.parseInt(args["apos1'"])
									
									int oldPosY = oldPos/width
									int oldPosX = oldPos%width
									int newPosY = newPos/width
									int newPosX = newPos%width
									
									if(oldPosX==newPosX) {
										if(oldPosY<height-1) {
											if(oldPosY+1==newPosY)
												return 1.0f
											else
												return 0.0f
										}
										else {
											if(oldPosY==newPosY)
												return 1.0f
											else
												return 0.0f
										}
									}
									else
									{
										return 0.0f
									}
									
								}
					)
					.withVariableTransition("apos2",
						spudder.actionTree()
								.withVariable("apos2")
								.then()
								.withVariable("apos2")
								.hasValue { args ->
									if(distance(Integer.parseInt(args["apos2"]),Integer.parseInt(args["apos2'"]))<=1)
									{
										return String.format("%.20f",1.0f/(double)numAdjacent(Integer.parseInt(args["apos2"])))
									}
									else
									{
										return "0.0"
									}
								}
					)
					.withVariableTransition("wpos", "wumpusb")
					.withObsTransition("aloc", "ldd")
					.withObsTransition("coll", "sensingdd")
					.hasCost(1)
					
			
					
				spudder.action("south")
					.withVariableTransition("apos1",
						spudder.actionTree()
								.withVariable("apos1")
								.then()
								.withVariable("apos1")
								.hasValue { args ->
									int oldPos = Integer.parseInt(args["apos1"])
									int newPos = Integer.parseInt(args["apos1'"])
									
									int oldPosY = oldPos/width
									int oldPosX = oldPos%width
									int newPosY = newPos/width
									int newPosX = newPos%width
									
									if(oldPosX==newPosX) {
										if(oldPosY>0) {
											if(oldPosY-1==newPosY)
												return 1.0f
											else
												return 0.0f
										}
										else {
											if(oldPosY==newPosY)
												return 1.0f
											else
												return 0.0f
										}
									}
									else
									{
										return 0.0f
									}
									
								}
					)
					.withVariableTransition("apos2",
						spudder.actionTree()
								.withVariable("apos2")
								.then()
								.withVariable("apos2")
								.hasValue { args ->
									if(distance(Integer.parseInt(args["apos2"]),Integer.parseInt(args["apos2'"]))<=1)
									{
										return String.format("%.20f",1.0f/(double)numAdjacent(Integer.parseInt(args["apos2"])))
									}
									else
									{
										return "0.0"
									}
								}
					)
					.withVariableTransition("wpos", "wumpusb")
					.withObsTransition("aloc", "ldd")
					.withObsTransition("coll", "sensingdd")
					.hasCost(1)
					
			spudder.action("east")
					.withVariableTransition("apos1",
						spudder.actionTree()
								.withVariable("apos1")
								.then()
								.withVariable("apos1")
								.hasValue { args ->
									int oldPos = Integer.parseInt(args["apos1"])
									int newPos = Integer.parseInt(args["apos1'"])
									
									int oldPosY = oldPos/width
									int oldPosX = oldPos%width
									int newPosY = newPos/width
									int newPosX = newPos%width
									
									if(oldPosY==newPosY) {
										if(oldPosX<width-1) {
											if(oldPosX+1==newPosX)
												return 1.0f
											else
												return 0.0f
										}
										else {
											if(oldPosX==newPosX)
												return 1.0f
											else
												return 0.0f
										}
									}
									else
									{
										return 0.0f
									}
									
								}
					)
					.withVariableTransition("apos2",
						spudder.actionTree()
								.withVariable("apos2")
								.then()
								.withVariable("apos2")
								.hasValue { args ->
									if(distance(Integer.parseInt(args["apos2"]),Integer.parseInt(args["apos2'"]))<=1)
									{
										return String.format("%.20f",1.0f/(double)numAdjacent(Integer.parseInt(args["apos2"])))
									}
									else
									{
										return "0.0"
									}
								}
					)
					.withVariableTransition("wpos", "wumpusb")
					.withObsTransition("aloc", "ldd")
					.withObsTransition("coll", "sensingdd")
					.hasCost(1)
					
			spudder.action("west")
					.withVariableTransition("apos1",
						spudder.actionTree()
								.withVariable("apos1")
								.then()
								.withVariable("apos1")
								.hasValue { args ->
									int oldPos = Integer.parseInt(args["apos1"])
									int newPos = Integer.parseInt(args["apos1'"])
									
									int oldPosY = oldPos/width
									int oldPosX = oldPos%width
									int newPosY = newPos/width
									int newPosX = newPos%width
									
									if(oldPosY==newPosY) {
										if(oldPosX>0) {
											if(oldPosX-1==newPosX)
												return 1.0f
											else
												return 0.0f
										}
										else {
											if(oldPosX==newPosX)
												return 1.0f
											else
												return 0.0f
										}
									}
									else
									{
										return 0.0f
									}
									
								}
					)
					.withVariableTransition("apos2",
						spudder.actionTree()
								.withVariable("apos2")
								.then()
								.withVariable("apos2")
								.hasValue { args ->
									if(distance(Integer.parseInt(args["apos2"]),Integer.parseInt(args["apos2'"]))<=1)
									{
										return String.format("%.20f",1.0f/(double)numAdjacent(Integer.parseInt(args["apos2"])))
									}
									else
									{
										return "0.0"
									}
								}
					)
					.withVariableTransition("wpos", "wumpusb")
					.withObsTransition("aloc", "ldd")
					.withObsTransition("coll", "sensingdd")
					.hasCost(1)
					
			
					
			spudder.reward(
					spudder.rewardTree()
							.withVariable("apos1")
							.withVariable("apos2")
							.withVariable("wpos")
							.hasValue { args ->
								if(args["apos1"] == args["wpos"] || args["apos2"] == args["wpos"])
									return 10.0f
								else
									return 0.0f
							}
				)
			
			spudder.discount(0.95f)
			spudder.tolerance(0.0000001)
	}

}
