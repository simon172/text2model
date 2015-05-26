/**
 * copyright
 * Inubit AG
 * Schoeneberger Ufer 89
 * 10785 Berlin
 * Germany
 */
package etc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import nodes.Cluster;
import nodes.FlowObject;
import nodes.ProcessEdge;
import nodes.ProcessNode;
import nodes.ProcessObject;
import models.EPCModel;
import build.EPCExporter;
import build.Repairer;
import processing.T2PStanfordWrapper;
import text.T2PSentence;
import text.Text;
import textModel.SentenceNode;
import textModel.TextModel;
import textModel.TextModelControler;
import transform.EPCModelBuilder;
import transform.TextAnalyzer;
import transform.TextModelBuilder;
import worldModel.Action;
import worldModel.SpecifiedElement;
import edu.stanford.nlp.trees.TypedDependency;
import epc.Connector;
import epc.ConnectorAND;
import epc.ConnectorOR;
import epc.ConnectorXOR;
import epc.Function;
import epc.Organisation;
import epc.SequenceFlow;

/**
 * wraps all of the functionality to create processes from text.
 * Load and analyze a text using "parseText".
 * To reanalyze a text simple use "analyzeText".
 * All information (created Text Model after parsing or generated BPMN model) is
 * returned to the TextToProcessListener.
 * @author ff
 *
 */
public class TextToProcess {
	
private T2PStanfordWrapper f_stanford = new T2PStanfordWrapper();
	
	private Text f_text;
	
	private TextModelControler f_textModelControler = null;
	private TextAnalyzer f_analyzer = new TextAnalyzer();
	private TextModelBuilder f_builder = new TextModelBuilder();
    private EPCModel f_generatedModelEPC = null;
    
	private HashMap<Action, FlowObject> f_elementsMap = new HashMap<Action, FlowObject>();
	private HashMap<FlowObject, Action> f_elementsMapInv = new HashMap<FlowObject, Action>();
	
	
	/**
	 * 
	 */
	public TextToProcess() {
		
	}
	
	/**
	 * 
	 */
	public TextToProcess(TextModelControler tmControler) {
		 f_textModelControler = tmControler;
	}
	
	/*public void setLaneSplitOffContoler(LaneSplitOffControler lsoControler) {
		f_lsoControler = lsoControler;
	}*/
	
	
	 /**
     * (Re-)starts analyzing the loaded text and creates a process model
     */
	public void analyzeText(boolean rebuildTextModel) {
		f_analyzer.analyze(f_text);
        if(rebuildTextModel) {
			TextModel _model = f_builder.createModel(f_analyzer);	
			if(f_textModelControler != null)
				f_textModelControler.setModels(this, f_analyzer,f_builder,_model);
        }	
        EPCModelBuilder _builder = new EPCModelBuilder(this);
        f_generatedModelEPC = (EPCModel) _builder.createProcessModel(f_analyzer.getWorld());
        Repairer rep = new Repairer(f_generatedModelEPC);
        rep.repairModel();
        EPCExporter exp = new EPCExporter(f_generatedModelEPC);
        exp.addFunctions(rep.getFunctions());
        exp.addEvents(rep.getEvents());
        ArrayList<ConnectorAND> and = new ArrayList<ConnectorAND>();
        ArrayList<ConnectorOR> or = new ArrayList<ConnectorOR>();
        ArrayList<ConnectorXOR> xor = new ArrayList<ConnectorXOR>();
        and.addAll(rep.getAndJoins());
        and.addAll(rep.getAndSplits());
        or.addAll(rep.getOrJoins());
        or.addAll(rep.getOrSplits());
        xor.addAll(rep.getXorJoins());
        xor.addAll(rep.getXorSplits());
        exp.addConnectors(and, or, xor);
        exp.addFlows(rep.getFlows());
        exp.end();
        exp.export();
	}
	
	public void parseText(File file, boolean bpmn) {
		f_text = f_stanford.createText(file);
		f_analyzer.clear();
		analyzeText(true);
	}
	
	/**
	 * Sets the element map which comes from the ProcessModelBuilder
	 * and can be used to map actions to task nodes etc.
	 * @param map
	 */
	public void setElementMapping(HashMap<Action, FlowObject> map) {
		f_elementsMap = map;
		for(Entry<Action, FlowObject> e:f_elementsMap.entrySet()) {
			//building inverted list
			f_elementsMapInv.put(e.getValue(), e.getKey());			
		}
	}

	/**
	 * @param o
	 */
	public void textModelElementClicked(ProcessObject o) {
		if(o instanceof SentenceNode) {
    		SentenceNode n = (SentenceNode) o;
	    	T2PSentence _sentence = f_text.getSentences().get(n.getIndex());
	       	if(_sentence != null) {
	    		
	    		Collection<TypedDependency> _list = _sentence.getGrammaticalStructure().typedDependenciesCollapsed();
	    		
	    		StringBuffer _depText = new StringBuffer();
	    		for(TypedDependency td:_list) {
	    			_depText.append(td.toString());
	    		}
	    		
	    		f_analyzer.analyzeSentence(_sentence,1,true);
	    	}
    	}
    	else {
    		if(f_elementsMapInv.containsKey(o)) {
    			Action _ac = f_elementsMapInv.get(o);
    			if(f_textModelControler != null)
    				f_textModelControler.highlightAction(_ac);    			
    		}
    	}
	}

	/**
	 * @return
	 */
	public TextStatistics getTextStatistics() {
		TextStatistics _result = new TextStatistics();
		_result.setNumberOfSentences(f_text.getSize());
		_result.setAvgSentenceLength(f_text.getAvgSentenceLength());
		_result.setNumOfReferences(f_analyzer.getNumberOfReferences());
		_result.setNumOfLinks(f_analyzer.getNumberOfLinks());
		return _result;
	}

	/**
	 * @return
	 */
	public TextAnalyzer getAnalyzer() {
		return f_analyzer;
	}

	/**
	 * @param _element
	 */
	public void textElementClicked(SpecifiedElement _element) {
		if(_element instanceof Action) {
			FlowObject _corr = f_elementsMap.get(_element);
			if(_corr != null) {						
			}
		}
	}

	/**
	 * @param sentenceWordID
	 * @param sentenceWordID2
	 */
	public void addManualReferenceResolution(SentenceWordID sentenceWordID,	SentenceWordID sentenceWordID2) {
		f_analyzer.addManualReference(sentenceWordID, sentenceWordID2);
	}

	
}
