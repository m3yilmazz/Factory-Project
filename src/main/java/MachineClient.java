import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class MachineClient extends JFrame implements KeyListener {
	private static InetAddress host;
	private static final int PORT = 9999;
	static Socket socket = null;
	static Scanner networkInput;
	static PrintWriter networkOutput;

	static private GsonBuilder gsonBuilder = new GsonBuilder();
	static private Gson gson = gsonBuilder.serializeNulls().create();

	private final JPanel machinePropsPanel = new JPanel( new GridLayout( 4, 2, 5, 5 ) );
	private final JPanel buttonAndResponseCodePanel = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
	private final JTextField machineUniqueIdJTextField = new JTextField();
	private final JTextField machineNameJTextField = new JTextField();
	private final JTextField machineTypeJTextField = new JTextField();
	private final JTextField machineProductionSpeedJTextField = new JTextField();
	private final JButton addMachineJButton = new JButton( "Add Machine" );
	private final JButton removeMachineJButton = new JButton( "Remove Machine" );
	private final JTextArea responseJTextArea = new JTextArea();

	Window window;
	JFrame jFrame;

	MachineClient(Scanner networkInput, PrintWriter networkOutput, Gson gson){
		super( "Machine Client" );
		setLayout( new GridLayout( 2, 1, 10, 10 ) );

		window = this;
		jFrame = this;

		machinePropsPanel.add( new JLabel( "Machine Unique Id" ) );
		machinePropsPanel.add( machineUniqueIdJTextField );
		machinePropsPanel.add( new JLabel( "Machine Name" ) );
		machinePropsPanel.add( machineNameJTextField );
		machinePropsPanel.add( new JLabel( "Machine Type" ) );
		machinePropsPanel.add( machineTypeJTextField );
		machinePropsPanel.add( new JLabel( "Machine Production Speed per minute" ) );
		machinePropsPanel.add( machineProductionSpeedJTextField );
		addMachineJButton.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event )
				{
					String request, machineIdString, machineName, machineType ,machineProductionSpeed;
					int machineId;

					machineIdString = machineUniqueIdJTextField.getText();
					machineName = machineNameJTextField.getText();
					machineType = machineTypeJTextField.getText();
					machineProductionSpeed = machineProductionSpeedJTextField.getText();

					try {
						machineId = Integer.parseInt(machineIdString);

						request = gson.toJson(new Request<AddMachineRequest>(Commands.ADD_MACHINE, new AddMachineRequest(machineId, machineName, machineType, machineProductionSpeed)));

						MachineCommandSender machineCommandSender = new MachineCommandSender(
								request,
								networkInput,
								networkOutput,
								responseJTextArea,
								machineUniqueIdJTextField,
								machineNameJTextField,
								machineTypeJTextField,
								machineProductionSpeedJTextField,
								buttonAndResponseCodePanel,
								addMachineJButton,
								removeMachineJButton,
								window,
								jFrame
						);
						machineCommandSender.execute();
					}
					catch (NumberFormatException e){
						responseJTextArea.setText("Please enter valid Machine Unique Id.");
					}
				}
			}
		);
		addMachineJButton.addKeyListener(this);
		buttonAndResponseCodePanel.add( addMachineJButton );

		JScrollPane responseJScrollPane = new JScrollPane(responseJTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		buttonAndResponseCodePanel.add(responseJScrollPane);

		removeMachineJButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				 String request = gson.toJson(new Request<MachineIdRequest>(Commands.REMOVE_MACHINE, new MachineIdRequest(Integer.parseInt(machineUniqueIdJTextField.getText()))));

				 MachineCommandSender machineCommandSender = new MachineCommandSender(
						 request,
						 networkInput,
						 networkOutput,
						 responseJTextArea,
						 machineUniqueIdJTextField,
						 machineNameJTextField,
						 machineTypeJTextField,
						 machineProductionSpeedJTextField,
						 buttonAndResponseCodePanel,
						 addMachineJButton,
						 removeMachineJButton,
						 window,
						 jFrame
				 );
				 machineCommandSender.execute();
				}
			}
		);

		add( machinePropsPanel );
		add( buttonAndResponseCodePanel );
		setSize( 500, 400 );
		setVisible( true );
	}
	
	public static void main(String[] args)
	{
		try {
			host = InetAddress.getLocalHost();

			try{
				socket = new Socket(host,PORT);

				networkInput = new Scanner(socket.getInputStream());
				networkOutput = new PrintWriter(socket.getOutputStream(),true);

			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		catch(UnknownHostException uhEx) {
			System.out.println("\nHost ID not found!\n");
			System.exit(1);
		}
		MachineClient machineClient = new MachineClient(networkInput, networkOutput, gson);
		machineClient.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == 10){
			addMachineJButton.doClick();
		}
	}
	@Override
	public void keyTyped(KeyEvent e) { }
	@Override
	public void keyReleased(KeyEvent e) { }
}

