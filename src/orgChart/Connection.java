/**
 * copyright
 * Inubit AG
 * Schoeneberger Ufer 89
 * 10785 Berlin
 * Germany
 */
package orgChart;


import nodes.ProcessEdge;
import nodes.ProcessNode;


/**
 * @author ff
 *
 */
public class Connection extends ProcessEdge {

	
	/**
	 * 
	 */
	public Connection() {
		super();
	}
	
	/**
	 * @param source
	 * @param target
	 */
	public Connection(ProcessNode source, ProcessNode target) {
		super(source,target);
	}


}
