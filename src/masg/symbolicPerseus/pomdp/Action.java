package masg.symbolicPerseus.pomdp;

import java.io.Serializable;

import masg.symbolicPerseus.dd.DD;

public class Action implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6078621497410273773L;
	
	public String name;
	public DD[] transFn;
	public DD[] obsFn;
	public DD rewFn;
	public DD[] rewTransFn;
	public DD initialBelState;

	public Action(String n) {
		name = n;
	}

	public void addTransFn(DD[] tf) {
		transFn = new DD[tf.length];
		for (int idx = 0; idx < tf.length; ++idx) {
			transFn[idx] = tf[idx];
		}
	}

	public void addObsFn(DD[] of) {
		obsFn = new DD[of.length];
		for (int idx = 0; idx < of.length; ++idx) {
			obsFn[idx] = of[idx];
		}
	}

	public void buildRewTranFn() {
		rewTransFn = new DD[transFn.length + 1];
		int k = 0;
		rewTransFn[k++] = rewFn;
		for (int idx = 0; idx < transFn.length; ++idx) {
			rewTransFn[k++] = transFn[idx];
		}
	}
}
