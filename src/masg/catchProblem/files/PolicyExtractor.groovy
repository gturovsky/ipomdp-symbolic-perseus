package masg.catchProblem.files

import masg.test.symbolicPerseus.DD
import masg.test.symbolicPerseus.POMDP

class PolicyExtractor {
	class PolicyNode {
		int alphaId=-1;
		int actId = 1;
		DD belief
		Map<String,Integer> nextNode = [:]	
	}
	
	List<PolicyNode> policyNodes = []
	
	public PolicyExtractor(POMDP p) {
		PolicyNode nodeCurr = new PolicyNode()
		
		nodeCurr.belief = p.initialBelState
		nodeCurr.alphaId = p.policyBestAlphaMatch(nodeCurr.belief, p.alphaVectors, p.policy);
		nodeCurr.actId = p.policy[nodeCurr.alphaId]
		
		List<PolicyNode> policyLeaves = [nodeCurr]
		
		while(!policyLeaves.isEmpty()) {
			
			nodeCurr = policyLeaves.pop()
			
			List newLeaves = []
			2.times { collNum ->
				25.times{ alocNum ->
					List obs = ["aloc_$alocNum","coll_$collNum"]
					PolicyNode nodeNext = new PolicyNode()
					nodeNext.belief = p.beliefUpdate(nodeCurr.belief,nodeCurr.actId,obs as String[])
					
					if(nodeNext.belief!=DD.one) {
						nodeNext.alphaId = p.policyBestAlphaMatch(nodeNext.belief, p.alphaVectors, p.policy);
						nodeCurr.nextNode[obs] = nodeNext.alphaId
						nodeNext.actId = p.policy[nodeNext.alphaId]
						newLeaves << nodeNext
					}

				}
			}
			
			nodeCurr.belief = null
			
			
			PolicyNode matchedNode = policyNodes.find{nodeOther -> 
				nodeOther.alphaId == nodeCurr.alphaId &&
				nodeOther.actId == nodeCurr.actId &&
				mapsEqual(nodeOther.nextNode,nodeCurr.nextNode)	
			}
			
			if(matchedNode) {
				println "Found same policy node!"
			}
			else {
				policyNodes << nodeCurr
				policyLeaves.addAll(newLeaves)
			}
			
			println "${policyNodes.size()} policy nodes"
		}
		
		policyNodes.each{ node ->
			print node.alphaId + ":" + p.actions[node.actId].name + " -> {"
			node.nextNode.each{ k,v ->
				print v + " "
			}
			println "}"
		}
	}
	
	public boolean mapsEqual(Map m1, Map m2) {
		if(m1.size() != m2.size()) 
			return false;
			
		m1.each{k1,v1 ->
			m2.each{k2,v2 ->
				if(m1[k1]!=m2[k2])
					return false
			}
		}
		
		return true
	}
}
