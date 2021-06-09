import javax.swing.*;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class CommandSender extends SwingWorker<String, Object> {
    private final String message;
    Scanner networkInput;
    PrintWriter networkOutput;
    JTextArea responseJTextArea;

    CommandSender(String message, Scanner networkInput, PrintWriter networkOutput, JTextArea responseJTextArea){
        this.message = message;
        this.networkInput = networkInput;
        this.networkOutput = networkOutput;
        this.responseJTextArea = responseJTextArea;
    }

    @Override
    protected String doInBackground() throws Exception {
        String response;
        networkOutput.println(message);
        response = networkInput.nextLine();
        return  response;
    }

    protected void done()
    {
        String response, command;
        String [] splitted;
        StringBuffer buffer = new StringBuffer();
        try
        {
            response = get();
            command = message.split("\\u002A")[0];
            switch (command){
                case Commands.ADD_MACHINE:
                    if(response.equals("410")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
                    }
                    else if(response.equals("210")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The given machine id has already in use.");
                    }
                    else if(response.equals("211")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine type is not valid.");
                    }
                    else if(response.equals("212")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine type does not match with its production metric.");
                    }
                    else if(response.equals("110")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine added successfully.");
                    }
                    break;

                case Commands.GET_MACHINES:
                    if(response.equals("220")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                    }
                    else {
                        splitted = response.split("\\u002A");
                        for(String line: splitted){
                            buffer.append(line);
                            buffer.append("\n");
                        }

                        responseJTextArea.setText(String.valueOf(buffer));
                    }
                    break;

                case Commands.GET_MACHINE_INFORMATIONS:
                    if(response.equals("410")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
                    }
                    else if(response.equals("230")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no machine with the given id.");
                    }
                    else {
                        splitted = response.split("\\u002A");
                        for(String line: splitted){
                            buffer.append(line);
                            buffer.append("\n");
                        }

                        responseJTextArea.setText(String.valueOf(buffer));
                    }
                    break;

                case Commands.SEND_JOB_ORDER:
                    if(response.equals("410")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
                    }
                    else if(response.equals("240")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The given job id has already in use.");
                    }
                    else if(response.equals("241")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The job type is not valid.");
                    }
                    else if(response.equals("242")){
                        responseJTextArea.setText("Response Code: " + response + "\nThe job type does not match with its production metric.");
                    }
                    else if(response.equals("140")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The job added successfully.");
                    }
                    break;

                case Commands.GET_PENDING_JOB_ORDERS:
                    splitted = response.split("\\u002A");
                    for(String line: splitted){
                        buffer.append(line);
                        buffer.append("\n");
                    }

                    responseJTextArea.setText(String.valueOf(buffer));
                    break;

                case Commands.GET_MACHINE_STATES:
                    buffer = new StringBuffer();
                    if(response.equals("220")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                    }
                    else {
                        splitted = response.split("\\u002A");

                        for(String line: splitted){
                            buffer.append(line);
                            buffer.append("\n");
                        }

                        responseJTextArea.setText(String.valueOf(buffer));
                    }
                    break;

                case Commands.GET_PROCESSING_JOB_ORDERS:
                    if(response.equals("220")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                    }
                    else if(response.equals("250")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no busy machine.");
                    }
                    else {
                        splitted = response.split("\\u002A");
                        for(String line: splitted){
                            buffer.append(line);
                            buffer.append("\n");
                        }

                        responseJTextArea.setText(String.valueOf(buffer));
                    }
                    break;
            }
        }
        catch ( InterruptedException ex )
        {
            responseJTextArea.setText("Interrupted while waiting for results.");
        }
        catch ( ExecutionException ex )
        {
            responseJTextArea.setText("Error encountered while performing request.");
        }
    }
}
