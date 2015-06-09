/**
 * modified taken from https://github.com/FabianFriedrich/Text2Process
 */
package transform;


import java.util.HashMap;



import nodes.ProcessNode;
//import net.frapu.code.visualization.ProcessNode;
import text.T2PSentence;
import textModel.SentenceNode;
import textModel.TextEdge;
import textModel.TextLinkEdge;
import textModel.TextModel;
import textModel.WordNode;
import worldModel.Action;
import worldModel.ExtractedObject;
import worldModel.SpecifiedElement;

public class TextModelBuilder {
	

	public static final float DEFAULT_EDGE_ALPHA = 0.5f;
	private TextAnalyzer f_analyzer;
	private HashMap<T2PSentence, SentenceNode> f_sentenceMap = new HashMap<T2PSentence, SentenceNode>();
	
	/**
	 * 
	 */
	public TextModel createModel(TextAnalyzer analyzer) {
		f_analyzer = analyzer;
		return buildModel();
	}

    
	
	private TextModel buildModel() {
		TextModel _result = new TextModel();
		for(T2PSentence s: f_analyzer.getText().getSentences()) {
			SentenceNode _sn = new SentenceNode(s.getID());
			f_sentenceMap.put(s, _sn);
			_result.addNode(_sn);
			for(int w = 0; w<s.size(); w++) {
				WordNode _wn = new WordNode(s.get(w).value());
				_result.addNode(_wn);
				_sn.addWord(_wn);
			}
		}
		//building edges for all relative references
		for(ExtractedObject ele:f_analyzer.getWorld().getElements()) {
			if(ele.needsResolve()) {
				WordNode _start = (WordNode) getProcessNode(ele);
				if(ele.getReference() != null) {
					SpecifiedElement _target = ele.getReference();
					WordNode _end = (WordNode) getProcessNode(_target);
					TextEdge _edge = new TextEdge();
					_edge.setSource(_start);
					_edge.setTarget(_end);
					_result.addEdge(_edge);
				}else {
				}
			}			
		}			
		//building dashed edges for links
		for(Action a:f_analyzer.getWorld().getActions()) {
			if(a.getLink() == null) {
				continue;
			}
			ProcessNode _aNode = getProcessNode(a);
			ProcessNode _bNode = getProcessNode(a.getLink());		
			TextLinkEdge _edge = new TextLinkEdge();
			_edge.setSource(_aNode);
			_edge.setTarget(_bNode);
			_result.addEdge(_edge);				
		}
		return _result;
	}
	
	public ProcessNode getProcessNode(SpecifiedElement a) {
		return f_sentenceMap.get(a.getOrigin()).getProcessNodes().get(a.getWordIndex()-1);
	}
	
	public SentenceNode getSentenceNode(T2PSentence sent) {
		return f_sentenceMap.get(sent);
	}
}
