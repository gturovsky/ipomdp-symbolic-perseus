package masg.symbolicPerseus;

import java.io.Serializable;

import masg.symbolicPerseus.dd.DD;

public class AlphaVector implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 731981371484809741L;
	public DD alphaVector;
	public double value;
	public int actId;
	public int witness;
	int[] obsStrat;

	public AlphaVector(DD a, double v, int act, int[] os) {
		alphaVector = a;
		value = v;
		actId = act;
		obsStrat = os;
	}

	public AlphaVector(AlphaVector a) {
		alphaVector = a.alphaVector;
		value = a.value;
		actId = a.actId;
		witness = a.witness;
		obsStrat = a.obsStrat;
	}

	public void setWitness(int i) {
		witness = i;
	}

}