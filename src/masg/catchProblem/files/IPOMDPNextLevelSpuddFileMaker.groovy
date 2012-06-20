package masg.catchProblem.files

import java.util.List;

import masg.catchProblem.files.PolicyExtractor.PolicyNode;
import masg.test.symbolicPerseus.POMDP
import org.flyhighplato.spudder.Spudder

class IPOMDPNextLevelSpuddFileMaker extends AbstractSpuddFileMaker {
	
	private int width, height
	private POMDP pomdpOther
	private List<PolicyNode> policyNodesOther
	
	public IPOMDPNextLevelSpuddFileMaker(int w, int h, POMDP p,List<PolicyNode> policyNodesOther) {
		width = w
		height = h
		pomdpOther = p
		
		
		Map<Integer,Integer> idTranslate = [:]
		policyNodesOther.eachWithIndex { node, ix ->
			idTranslate[node.alphaId] = ix
			node.alphaId = ix
		}
		
		policyNodesOther.each{ node ->
			Map<List,PolicyNode> newNextNode = [:]
			node.nextNode.each{ obs, nextNodeId ->
				newNextNode[obs] = idTranslate[nextNodeId]
			}
			node.nextNode = newNextNode
		}
		
		
		
		this.policyNodesOther = policyNodesOther
		
		println "Policy rearranged..."
		this.policyNodesOther.each{ node ->
			print node.alphaId + ":" + p.actions[node.actId].name + " -> {"
			node.nextNode.each{ k,v ->
				print v + " "
			}
			println "}"
		}
	}
	
	@Override
	public void makeSpuddFile(String spuddFileName) {
		double totalStates = Math.pow((double)width*(double)height,3.0f)
		Range posRange = 0..<(width*height)
		Range collRange = 0..<2
		Range policyNodeRange = 0..policyNodesOther.size()-1
		
		Spudder spudder = new Spudder(
								spuddFileName,
								[	"apos1":posRange.collect{it-> "$it"},
									"apos2":posRange.collect{it-> "$it"},
									"wpos":posRange.collect{it-> "$it"},
									"aloc_a2":posRange.collect{it-> "$it"},
									"coll_a2":collRange.collect{it-> "$it"},
									"pnode_a2":policyNodeRange.collect{it-> "$it"}
								],
								[	"aloc":posRange.collect{it-> "$it"},
									"coll":collRange.collect{it-> "$it"},
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
		
		spudder.dd("a2policy")
				.withVariable("pnode_a2")
				.withVariable("aloc_a2")
				.withVariable("coll_a2")
				.then()
				.withVariable("pnode_a2")
				.hasValue { args ->
					int currNodeId = Integer.parseInt(args["pnode_a2"])
					int nextNodeId = Integer.parseInt(args["pnode_a2'"])
					
					PolicyNode currNode = policyNodesOther.find{it-> it.alphaId == currNodeId}
					
					int aloc = Integer.parseInt(args["aloc_a2"])
					int coll = Integer.parseInt(args["coll_a2"])
					
					List key = ["aloc_${aloc}","coll_${coll}"]
					if(currNode && currNode.nextNode[key] == nextNodeId){
						return String.format("%.5f",1.0f)
					}
					else {
						return "0.0"
					}
				}
		
		spudder.dd("a2aloc")
				.withVariable("apos2")
				.withVariable("pnode_a2")
				.then()
				.withVariable("aloc_a2")
				.hasValue { args ->
					int currNodeId = Integer.parseInt(args["pnode_a2"])
					PolicyNode currNode = policyNodesOther.find{it-> it.alphaId == currNodeId}

					if(currNode==null)
						return 0.0f
						
					String action = pomdpOther.actions[currNode.actId].name
					
					int oldPos = Integer.parseInt(args["apos2"])
					int newPos = Integer.parseInt(args["aloc_a2'"])
					
					int y = oldPos/width
					int x = oldPos - y*width
					
					switch(action) {
						case "north":
							if(y<height-1)
								y++
							break
						case "south":
							if(y>0)
								y--
							break
						case "east":
							if(x<width-1)
								x++
							break
						case "west":
							if(x>0)
								x--
							break
					}
					
					if(newPos == y*width + x)
						return 1.0f
					else
						return 0.0f
				}
				
		spudder.dd("a2pos")
				.withVariable("apos2")
				.withVariable("pnode_a2")
				.then()
				.withVariable("apos2")
				.hasValue { args ->
					int currNodeId = Integer.parseInt(args["pnode_a2"])
					PolicyNode currNode = policyNodesOther.find{it-> it.alphaId == currNodeId}

					if(currNode==null)
						return 0.0f
						
					String action = pomdpOther.actions[currNode.actId].name
					
					int oldPos = Integer.parseInt(args["apos2"])
					int newPos = Integer.parseInt(args["apos2'"])
					
					int y = oldPos/width
					int x = oldPos - y*width
					
					switch(action) {
						case "north":
							if(y<height-1)
								y++
							break
						case "south":
							if(y>0)
								y--
							break
						case "east":
							if(x<width-1)
								x++
							break
						case "west":
							if(x>0)
								x--
							break
					}
					
					if(newPos == y*width + x)
						return 1.0f
					else
						return 0.0f
				}
		
		spudder.dd("a2coll")
				.withVariable("apos2")
				.withVariable("wpos")
				.withVariable("pnode_a2")
				.then()
				.withVariable("coll_a2")
				.hasValue{ args ->
					int currNodeId = Integer.parseInt(args["pnode_a2"])
					PolicyNode currNode = policyNodesOther.find{it-> it.alphaId == currNodeId}

					if(currNode==null)
						return 0.0f
						
					String action = pomdpOther.actions[currNode.actId].name
					
					int oldPos = Integer.parseInt(args["apos2"])
					int oldWPos = Integer.parseInt(args["wpos"])
					
					int y = oldPos/width
					int x = oldPos - y*width
					
					switch(action) {
						case "north":
							if(y<height-1)
								y++
							break
						case "south":
							if(y>0)
								y--
							break
						case "east":
							if(x<width-1)
								x++
							break
						case "west":
							if(x>0)
								x--
							break
					}
					
					int newPos = y*width + x
					int numWalls = 5-numAdjacent(oldWPos)
					
					if(args["coll_a2'"] == "1") {
						if(newPos == oldWPos) 
						{
							return String.format("%.5f",(1.0f+numWalls)/5.0f)
						}
						else if(distance(newPos,oldWPos)==1) 
						{
							return String.format("%.5f",0.2f)
						}
						else
						{
							return "0.0"
						}
					}
					else {
						if(newPos == oldWPos)
						{
							return String.format("%.5f",1.0 - (1.0f+numWalls)/5.0f)
						}
						else if(distance(newPos,oldWPos)==1)
						{
							return String.format("%.5f",0.8f)
						}
						else
						{
							return "1.0"
						}
					}
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
				.withVariableTransition("wpos", "wumpusb")
				.withVariableTransition("apos2","a2pos")
				.withVariableTransition("coll_a2", "a2coll")
				.withVariableTransition("aloc_a2","a2aloc")
				.withVariableTransition("pnode_a2","a2policy")
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
				.withVariableTransition("wpos", "wumpusb")
				.withVariableTransition("apos2","a2pos")
				.withVariableTransition("coll_a2", "a2coll")
				.withVariableTransition("aloc_a2","a2aloc")
				.withVariableTransition("pnode_a2","a2policy")
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
				.withVariableTransition("wpos", "wumpusb")
				.withVariableTransition("apos2","a2pos")
				.withVariableTransition("coll_a2", "a2coll")
				.withVariableTransition("aloc_a2","a2aloc")
				.withVariableTransition("pnode_a2","a2policy")
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
				.withVariableTransition("wpos", "wumpusb")
				.withVariableTransition("apos2","a2pos")
				.withVariableTransition("coll_a2", "a2coll")
				.withVariableTransition("aloc_a2","a2aloc")
				.withVariableTransition("pnode_a2","a2policy")
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
