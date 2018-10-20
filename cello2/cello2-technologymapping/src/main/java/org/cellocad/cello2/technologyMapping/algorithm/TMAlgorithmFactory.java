/**
 * Copyright (C) 2017 Massachusetts Institute of Technology (MIT)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.cellocad.cello2.technologyMapping.algorithm;

import org.cellocad.cello2.common.algorithm.AlgorithmFactory;
import org.cellocad.cello2.technologyMapping.algorithm.Cello_JY_TP.Cello_JY_TP;
import org.cellocad.cello2.technologyMapping.algorithm.SimulatedAnnealing.SimulatedAnnealing;
import org.cellocad.cello2.technologyMapping.algorithm.DSGRN.DSGRN;

/**
 * The TMAlgorithmFactory is an algorithm factory for the <i>technologyMapping</i> stage. 
 * 
 * @author Vincent Mirian
 * 
 * @date 2018-05-21
 *
 */
public class TMAlgorithmFactory extends AlgorithmFactory<TMAlgorithm>{

	/**
	 *  Returns the TMAlgorithm that has the same name as the parameter <i>name</i> within this instance
	 *  
	 *  @param name string used for searching the TMAlgorithmFactory
	 *  @return TMAlgorithm instance if the TMAlgorithm exists within the TMAlgorithmFactory, otherwise null
	 */
	@Override
	protected TMAlgorithm getAlgorithm(final String name) {
		TMAlgorithm rtn = null;
		if (name.equals("Cello_JY_TP")){
			rtn = new Cello_JY_TP();
		}
		if (name.equals("SimulatedAnnealing")){
			rtn = new SimulatedAnnealing();
		}
		if (name.equals("DSGRN")){
			rtn = new DSGRN();
		}
		return rtn;
	}
	
}
