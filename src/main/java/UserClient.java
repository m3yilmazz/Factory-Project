import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class UserClient extends JFrame {
	private static InetAddress host;
	private static final int PORT = 9999;
	static Socket socket = null;
	static Scanner networkInput;
	static PrintWriter networkOutput;

	private final JPanel userPropsPanel = new JPanel( new GridLayout( 2, 2, 5, 5 ) );
	private final JPanel buttonAndResponseCodePanel = new JPanel( new GridLayout( 2, 1, 5, 5 ) );

	private final JTextField userNameJTextField = new JTextField();
	private final JTextField userPasswordJTextField = new JTextField();
	private final JButton loginJButton = new JButton( "LOGIN" );
	private final JTextArea loginResponseJTextArea = new JTextArea();

	private final JPanel commandButtonsPanel = new JPanel( new GridLayout( 8, 1, 5, 5 ) );
	private final JPanel getMachineInformationInputPanel = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
	private final JPanel sendJobOrderInputPanel = new JPanel( new GridLayout( 4, 2, 5, 5 ) );
	private final JPanel responseJPanel = new JPanel( new GridLayout( 1, 1, 5, 5 ) );

	private final JButton getMachinesJButton = new JButton( "Get Machines" );
	private final JButton getMachineInformationsJButton = new JButton( "Get Machine Informations" );
	private final JButton sendJobOrderJButton = new JButton( "Send Job Order" );
	private final JButton getPendingJobOrdersJButton = new JButton( "Get Pending Job Orders" );
	private final JButton getMachineStatesJButton = new JButton( "Get Machine States" );
	private final JButton getProcessingJobOrdersInMachinesStatesJButton = new JButton( "Get Processing Job Orders In Machines" );

	private final JTextField machineUniqueIdJTextField = new JTextField();
	private final JTextField jobOrderUniqueIdJTextField = new JTextField();
	private final JTextField jobOrderTypeJTextField = new JTextField();
	private final JTextField jobOrderProductionLengthJTextField = new JTextField();

	private final JTextArea responseJTextArea = new JTextArea();

	UserClient(Scanner networkInput, PrintWriter networkOutput){
		super( "User Client" );
		setLayout( new GridLayout( 2, 1, 10, 10 ) );

		userPropsPanel.add( new JLabel( "Username" ) );
		userPropsPanel.add( userNameJTextField );
		userPropsPanel.add( new JLabel( "Password" ) );
		userPropsPanel.add( userPasswordJTextField );
		loginJButton.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event )
				{
					Thread newThread = new Thread(() -> {
						String message, userName, userPassword, response;

						userName = userNameJTextField.getText();
						userPassword = userPasswordJTextField.getText();

						message = "LOGIN" + "*" + userName + "*" + userPassword;

						response = sendCommand(message, networkInput, networkOutput);

						if(response.equals("410")){
							loginResponseJTextArea.setText("Response Code: 410 \nResponse Message: There is one or more missing argument in the command.");
						}
						else if(response.equals("200")){
							loginResponseJTextArea.setText("Response Code: 200 \nResponse Message: The username or password is wrong.");
						}
						else if(response.equals("100")){
							remove( userPropsPanel );
							remove( buttonAndResponseCodePanel );
							setLayout( new GridLayout( 1, 2, 10, 10 ) );
							setSize( 1000, 900 );
							add(commandButtonsPanel);
							add(responseJPanel);
						}
					});
					newThread.start();
				}
			}
		);

		buttonAndResponseCodePanel.add( loginJButton );
		JScrollPane loginResponseJScrollPane = new JScrollPane(loginResponseJTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		buttonAndResponseCodePanel.add( loginResponseJScrollPane );

		commandButtonsPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "ACTIONS" ) );
		getMachinesJButton.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event )
				{
					Thread newThread = new Thread(() -> {
						String message, response;
						String [] splitted;
						message = Commands.GET_MACHINES;

						response = sendCommand(message, networkInput, networkOutput);

						if(response.equals("220")){
							responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
						}
						else {
							splitted = response.split("\\u002A");
							StringBuffer buffer = new StringBuffer();
							for(String line: splitted){
								buffer.append(line);
								buffer.append("\n");
							}

							responseJTextArea.setText(String.valueOf(buffer));
						}
					});
					newThread.start();
				}
			}
		);
		commandButtonsPanel.add( getMachinesJButton );


		getMachineInformationInputPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "Get Machine Information" ) );
		getMachineInformationInputPanel.add( new JLabel( "Machine Unique Id" ) );
		getMachineInformationInputPanel.add( machineUniqueIdJTextField );
		commandButtonsPanel.add( getMachineInformationInputPanel );

		getMachineInformationsJButton.addActionListener( new ActionListener() {
			  public void actionPerformed( ActionEvent event )
			  {
				  Thread newThread = new Thread(() -> {
					  String message, machineId, response;
					  String [] splitted;
					  machineId = machineUniqueIdJTextField.getText();
					  message = Commands.GET_MACHINE_INFORMATIONS + "*" + machineId;

					  response = sendCommand(message, networkInput, networkOutput);

					  if(response.equals("410")){
						  responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
					  }
					  else if(response.equals("230")){
						  responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no machine with the given id.");
					  }
					  else {
						  splitted = response.split("\\u002A");
						  StringBuffer buffer = new StringBuffer();
						  for(String line: splitted){
							  buffer.append(line);
							  buffer.append("\n");
						  }

						  responseJTextArea.setText(String.valueOf(buffer));
					  }
				  });
				  newThread.start();
			  }
		  }
		);
		commandButtonsPanel.add( getMachineInformationsJButton );
		sendJobOrderInputPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "Send Job Order" ) );
		sendJobOrderInputPanel.add( new JLabel( "Job Order Unique Id" ) );
		sendJobOrderInputPanel.add( jobOrderUniqueIdJTextField );
		sendJobOrderInputPanel.add( new JLabel( "Job Order Type" ) );
		sendJobOrderInputPanel.add( jobOrderTypeJTextField );
		sendJobOrderInputPanel.add( new JLabel( "Job Order Production Length" ) );
		sendJobOrderInputPanel.add( jobOrderProductionLengthJTextField );
		commandButtonsPanel.add( sendJobOrderInputPanel );

		sendJobOrderJButton.addActionListener( new ActionListener() {
				 public void actionPerformed( ActionEvent event )
				 {
					 Thread newThread = new Thread(() -> {
						 String message, jobId, jobType, jobLength, response;
						 jobId = jobOrderUniqueIdJTextField.getText();
						 jobType = jobOrderTypeJTextField.getText();
						 jobLength = jobOrderProductionLengthJTextField.getText();

						 message = Commands.SEND_JOB_ORDER  + "*" + jobId + "*" + jobType + "*" + jobLength;

						 response = sendCommand(message, networkInput, networkOutput);

						 if(response.equals("410")){
							 responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
						 }
						 else if(response.equals("240")){
							 responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The given job id has already in use.");
						 }
						 else if(response.equals("241")){
							 responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The job type does not match with its production metric.");
						 }
						 else if(response.equals("140")){
							 responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The job added successfully.");
						 }
					 });
					 newThread.start();
				 }
			 }
		);
		commandButtonsPanel.add( sendJobOrderJButton );

		getPendingJobOrdersJButton.addActionListener( new ActionListener() {
			  public void actionPerformed( ActionEvent event )
			  {
				  Thread newThread = new Thread(() -> {
					  String message, response;
					  String [] splitted;
					  message = Commands.GET_PENDING_JOB_ORDERS;

					  response = sendCommand(message, networkInput, networkOutput);
					  splitted = response.split("\\u002A");
					  StringBuffer buffer = new StringBuffer();
					  for(String line: splitted){
						  buffer.append(line);
						  buffer.append("\n");
					  }

					  responseJTextArea.setText(String.valueOf(buffer));
				  });
				  newThread.start();
			  }
		  }
		);
		commandButtonsPanel.add( getPendingJobOrdersJButton );

		getMachineStatesJButton.addActionListener( new ActionListener() {
			  public void actionPerformed( ActionEvent event )
			  {
				  Thread newThread = new Thread(() -> {
					  String message, response;
					  String [] splitted;
					  message = Commands.GET_MACHINE_STATES;

					  response = sendCommand(message, networkInput, networkOutput);

					  if(response.equals("220")){
						  responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
					  }
					  else {
						  splitted = response.split("\\u002A");
						  StringBuffer buffer = new StringBuffer();
						  for(String line: splitted){
							  buffer.append(line);
							  buffer.append("\n");
						  }

						  responseJTextArea.setText(String.valueOf(buffer));
					  }
				  });
				  newThread.start();
			  }
		  }
		);
		commandButtonsPanel.add( getMachineStatesJButton );

		getProcessingJobOrdersInMachinesStatesJButton.addActionListener( new ActionListener() {
			   public void actionPerformed( ActionEvent event )
			   {
				   Thread newThread = new Thread(() -> {
					   String message, response;
					   String [] splitted;
					   message = Commands.GET_PROCESSING_JOB_ORDERS;

					   response = sendCommand(message, networkInput, networkOutput);

					   if(response.equals("220")){
						   responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
					   }
					   else if(response.equals("250")){
						   responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no busy machine.");
					   }
					   else {
						   splitted = response.split("\\u002A");
						   StringBuffer buffer = new StringBuffer();
						   for(String line: splitted){
							   buffer.append(line);
							   buffer.append("\n");
						   }

						   responseJTextArea.setText(String.valueOf(buffer));
					   }
				   });
				   newThread.start();
			   }
		   }
		);
		commandButtonsPanel.add( getProcessingJobOrdersInMachinesStatesJButton );

		responseJPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "RESPONSE PANEL" ) );
		JScrollPane responseJScrollPane = new JScrollPane(responseJTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		responseJPanel.add(responseJScrollPane);

		add( userPropsPanel );
		add( buttonAndResponseCodePanel );
		setSize( 400, 300 );
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
		UserClient userClient = new UserClient(networkInput, networkOutput);
		userClient.setDefaultCloseOperation( EXIT_ON_CLOSE );
	}

	private static String sendCommand(String message, Scanner networkInput, PrintWriter networkOutput)
	{
		String responseCode;
		networkOutput.println(message);
		responseCode = networkInput.nextLine();
		return  responseCode;
	}
}

