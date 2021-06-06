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

	private final JPanel machinePropsPanel = new JPanel( new GridLayout( 4, 2, 5, 5 ) );
	private final JPanel buttonAndResponseCodePanel = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
	private final JTextField machineUniqueIdJTextField = new JTextField();
	private final JTextField machineNameJTextField = new JTextField();
	private final JTextField machineTypeJTextField = new JTextField();
	private final JTextField machineProductionSpeedJTextField = new JTextField();
	private final JButton addMachineJButton = new JButton( "Add Machine" );
	private final JTextArea responseJTextArea = new JTextArea();
	
	MachineClient(Scanner networkInput, PrintWriter networkOutput){
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
					String message, machineIdString, machineName, machineType ,machineProductionSpeed, responseCode;

					machineIdString = machineUniqueIdJTextField.getText();
					machineName = machineNameJTextField.getText();
					machineType = machineTypeJTextField.getText();
					machineProductionSpeed = machineProductionSpeedJTextField.getText();

					message = "ADD" + "*" + machineIdString + "*" + machineName + "*" + machineType + "*" + machineProductionSpeed;

					responseCode = sendCommand(message, networkInput, networkOutput);

					if(responseCode.equals("410")){
						responseJTextArea.setText("Response Code: " + responseCode + "\nResponse Message: There is one or more missing argument in the command.");
					}
					else if(responseCode.equals("210")){
						responseJTextArea.setText("Response Code: " + responseCode + "\nResponse Message: The given machine id has already in use.");
					}
					else if(responseCode.equals("211")){
						responseJTextArea.setText("Response Code: " + responseCode + "\nResponse Message: The machine type is not valid.");
					}
					else if(responseCode.equals("212")){
						responseJTextArea.setText("Response Code: " + responseCode + "\nResponse Message: The machine type does not match with its production metric.");
					}
					else if(responseCode.equals("110")){
						responseJTextArea.setText("Response Code: " + responseCode + "\nResponse Message: The machine added successfully.");
					}

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
		MachineClient machineClient = new MachineClient(networkInput, networkOutput);
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

