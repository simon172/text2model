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
	
	//for EPC export
	private ArrayList<Function> f_functions = new ArrayList<Function>();
	private ArrayList<SequenceFlow> f_flows = new ArrayList<SequenceFlow>();
	private ArrayList<epc.Event> f_events = new ArrayList<epc.Event>();
	private ArrayList<Organisation> f_orgs = new ArrayList<Organisation>();
	
	
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
        	for (Cluster c : new ArrayList<Cluster>(f_generatedModelEPC.getClusters())){
        		if (c instanceof Organisation){
        			Organisation org = (Organisation) c;
        			f_orgs.add(org);
        		}
        	}
        	EPCExporter exp = new EPCExporter(f_generatedModelEPC);
        	for (ProcessNode a : new ArrayList<ProcessNode>(f_generatedModelEPC.getFlowObjects())){
        		if (a instanceof Function){
        			Function f = (Function) a;
        			f_functions.add(f);
        		}
        	}
        	f_events = f_generatedModelEPC.getEvents();
        	f_generatedModelEPC.extractConnectors();
        	for (ProcessEdge a : new ArrayList<ProcessEdge>(f_generatedModelEPC.getFlows())){
        		if (a instanceof epc.SequenceFlow){
        			SequenceFlow f = (epc.SequenceFlow) a;
        			f_flows.add(f);
        		}
        	}
        	exp.addFunctions(f_functions);
        	exp.addOrgs(f_orgs);
        	exp.addEvents(f_events);
        	exp.addConnectors(f_generatedModelEPC.getConnectorAND(), f_generatedModelEPC.getConnectorOR(), f_generatedModelEPC.getConnectorXOR());
        	exp.addFlows(f_flows);
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
