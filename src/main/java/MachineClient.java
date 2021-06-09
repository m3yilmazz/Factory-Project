import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class MachineClient extends JFrame {
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
	private final JTextArea responseJTextArea = new JTextArea();
	
	MachineClient(Scanner networkInput, PrintWriter networkOutput, Gson gson){
		super( "Machine Client" );
		setLayout( new GridLayout( 2, 1, 10, 10 ) );

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
					Thread newThread = new Thread(() -> {
						String message, machineIdString, machineName, machineType ,machineProductionSpeed, arguments;

						machineIdString = machineUniqueIdJTextField.getText();
						machineName = machineNameJTextField.getText();
						machineType = machineTypeJTextField.getText();
						machineProductionSpeed = machineProductionSpeedJTextField.getText();

						arguments = gson.toJson(new AddMachineRequest(Integer.parseInt(machineIdString), machineName, machineType, machineProductionSpeed));

						message = Commands.ADD_MACHINE + "*" + arguments;

						CommandSender commandSender = new CommandSender(message, networkInput, networkOutput, responseJTextArea);
						commandSender.execute();
					});
					newThread.start();
				}
			}
		);

		buttonAndResponseCodePanel.add( addMachineJButton );
		buttonAndResponseCodePanel.add( responseJTextArea );

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
		machineClient.setDefaultCloseOperation( EXIT_ON_CLOSE );
	}

	private static String sendCommand(String message, Scanner networkInput, PrintWriter networkOutput)
	{
		String responseCode;
		networkOutput.println(message);
		responseCode = networkInput.nextLine();
		return  responseCode;
	}
}

