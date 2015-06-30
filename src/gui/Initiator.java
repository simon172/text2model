package gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import processing.FrameNetWrapper;
import processing.WordNetWrapper;
import etc.TextToEPC;

public class Initiator {
	
	private TextToEPC t2p = new TextToEPC();
	private File file = new File("data.txt");
	
	public Initiator(){
		WordNetWrapper.init();
		FrameNetWrapper.init();
	}
	
	public void convert (String input) throws FileNotFoundException{
		saveInput(input);
		t2p.parseText(file);
	   }
	
	//for CLI
	public void convert (File file){
		t2p.parseText(file);
	}
	
	//for GUI
	public TextToEPC getT2P (){
		return t2p;
	}
	
	public void saveInput (String input) throws FileNotFoundException{
		   PrintWriter out = new PrintWriter(file);
		   out.println(input);
		   out.close();
	   }
	
	

}
