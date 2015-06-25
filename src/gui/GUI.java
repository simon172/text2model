package gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.BoxLayout;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;

public class GUI extends JFrame {

	private JPanel contentPane;
	private JTextField processDescription;
	private JButton btnTransform;
	private static Initiator init;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		if (args.length<1){
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						GUI frame = new GUI();
						frame.setVisible(true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			String fileName = args[0];
			File file = new File(fileName);
			convertFromConsole(file);
		}
	}
	
	public static void convertFromConsole(File file){
		init = new Initiator();
		init.convert(file);
	}

	/**
	 * Create the frame.
	 */
	public GUI() {
		init = new Initiator();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Text2EPC");
		setBounds(100, 100, 720, 480);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
		
		processDescription = new JTextField();
		contentPane.add(processDescription);
		processDescription.setColumns(10);
		
		btnTransform = new JButton("Transform");
		btnTransform.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
					try {
						init.convert(processDescription.getText());
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		});
		contentPane.add(btnTransform);
	}

}
