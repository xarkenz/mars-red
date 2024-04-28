package mars.tools;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class FunctionUnitVisualization extends JFrame {

	private JPanel contentPane;
	private String instruction;
	private MipsXray.DatapathUnit currentUnit;

	/**
	 * Launch the application.
	 */


	/**
	 * Create the frame.
	 */
	public FunctionUnitVisualization(String instruction, MipsXray.DatapathUnit functionalUnit) {
		this.instruction = instruction;
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 840, 575);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		currentUnit = functionalUnit;
		UnitAnimation reg = new UnitAnimation(instruction, currentUnit);
		contentPane.add(reg);
		reg.startAnimation(instruction);
	}

	public void run() {
		FunctionUnitVisualization frame = new FunctionUnitVisualization(instruction, currentUnit);
		frame.setVisible(true);
	}

}
