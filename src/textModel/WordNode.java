/**
 * copyright
 * Inubit AG
 * Schoeneberger Ufer 89
 * 10785 Berlin
 * Germany
 */
package textModel;

import epc.SequenceFlow;
import nodes.ProcessNode;


/**
 * @author ff
 *
 */
public class WordNode extends ProcessNode {
	
	

	/**
	 * 
	 */
	public WordNode(String word) {
		setText(word.replaceAll("\\/", "/"));
	}

	/**
	 * 
	 */
	public WordNode() {
		// TODO Auto-generated constructor stub
	}
	

	/**
	 * @return
	 */
	public String getWord() {
		return getText();
	}
	
	@Override
	public String toString() {
		return "WordNode ("+getText()+")";
	}

	@Override
	public void setIncoming(SequenceFlow flow) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOutgoing(SequenceFlow flow) {
		// TODO Auto-generated method stub
		
	}
}
