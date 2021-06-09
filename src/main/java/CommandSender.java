import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class CommandSender extends SwingWorker<String, Object> {
    private final String message;
    Scanner networkInput;
    PrintWriter networkOutput;
    JTextArea responseJTextArea;
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();
    Gson prettyGson = gsonBuilder.setPrettyPrinting().create();

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
                    buffer = new StringBuffer();
                    if(response.equals("220")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                    }
                    else {
                        GetMachinesResponse getMachinesResponse = gson.fromJson(response, GetMachinesResponse.class);
                        buffer.append("CNC MACHINES\n\n");
                        for(Machine machine: getMachinesResponse.CNC_MACHINES)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        buffer.append("DÖKÜM MACHINES\n\n");
                        for(Machine machine: getMachinesResponse.DOKUM_MACHINES)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        buffer.append("KILIF MACHINES\n\n");
                        for(Machine machine: getMachinesResponse.KILIF_MACHINES)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        buffer.append("KAPLAMA MACHINES\n\n");
                        for(Machine machine: getMachinesResponse.KAPLAMA_MACHINES)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));

                        responseJTextArea.setText(String.valueOf(buffer));
                    }
                    break;

                case Commands.GET_MACHINE_INFORMATIONS:
                    buffer = new StringBuffer();
                    if(response.equals("410")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
                    }
                    else if(response.equals("230")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no machine with the given id.");
                    }
                    else {
                        GetMachineInformationResponse getMachineInformationResponse = gson.fromJson(response, GetMachineInformationResponse.class);
                        buffer.append("MACHINE INFORMATIONS\n\n");
                        buffer.append(machineInformationFormatter(
                                getMachineInformationResponse.MACHINE.MachineUniqueId,
                                getMachineInformationResponse.MACHINE.MachineName,
                                getMachineInformationResponse.MACHINE.MachineType,
                                getMachineInformationResponse.MACHINE.MachineProductionSpeed,
                                getMachineInformationResponse.MACHINE.MachineState
                        ));

                        buffer.append("COMPLETED JOBS\n\n");
                        for(Job job: getMachineInformationResponse.COMPLETED_JOBS)
                            buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType,  job.JobLength, job.JobState));

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
                    buffer = new StringBuffer();
                    GetPendingJobOrdersResponse getPendingJobOrdersResponse = gson.fromJson(response, GetPendingJobOrdersResponse.class);
                    buffer.append("PENDING JOB ORDERS\n\n");

                    buffer.append("CNC JOB ORDERS\n\n");
                    for (Job job: getPendingJobOrdersResponse.CNC_JOB_ORDERS)
                        buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType,  job.JobLength, job.JobState));
                    buffer.append("DÖKÜM JOB ORDERS\n\n");
                    for (Job job: getPendingJobOrdersResponse.DOKUM_JOB_ORDERS)
                        buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType,  job.JobLength, job.JobState));
                    buffer.append("KILIF JOB ORDERS\n\n");
                    for (Job job: getPendingJobOrdersResponse.KILIF_JOB_ORDERS)
                        buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType,  job.JobLength, job.JobState));
                    buffer.append("KAPLAMA JOB ORDERS\n\n");
                    for (Job job: getPendingJobOrdersResponse.KAPLAMA_JOB_ORDERS)
                        buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType,  job.JobLength, job.JobState));

                    responseJTextArea.setText(String.valueOf(buffer));
                    break;

                case Commands.GET_MACHINE_STATES:
                    buffer = new StringBuffer();

                    if(response.equals("220")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                    }
                    else {
                        GetMachineStatesResponse getMachineStatesResponse = gson.fromJson(response, GetMachineStatesResponse.class);
                        for(Machine machine: getMachineStatesResponse.MACHINES)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));

                        responseJTextArea.setText(String.valueOf(buffer));
                    }
                    break;

                case Commands.GET_PROCESSING_JOB_ORDERS:
                    buffer = new StringBuffer();
                    if(response.equals("220")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                    }
                    else if(response.equals("250")){
                        responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no busy machine.");
                    }
                    else {
                        GetProcessingJobResponse getProcessingJobResponse = gson.fromJson(response, GetProcessingJobResponse.class);

                        for (int index = 0; index < getProcessingJobResponse.MACHINES.size(); ++index){
                            buffer.append("MACHINE\n\n");
                            buffer.append(machineInformationFormatter(
                                    getProcessingJobResponse.MACHINES.get(index).MachineUniqueId,
                                    getProcessingJobResponse.MACHINES.get(index).MachineName,
                                    getProcessingJobResponse.MACHINES.get(index).MachineType,
                                    getProcessingJobResponse.MACHINES.get(index).MachineProductionSpeed,
                                    getProcessingJobResponse.MACHINES.get(index).MachineState
                            ));

                            buffer.append("PROCESSING JOB IN THE MACHINE\n\n");
                            buffer.append(jobInformationFormatter(
                                    getProcessingJobResponse.JOBS.get(index).JobUniqueId,
                                    getProcessingJobResponse.JOBS.get(index).JobType,
                                    getProcessingJobResponse.JOBS.get(index).JobLength,
                                    getProcessingJobResponse.JOBS.get(index).JobState
                            ));
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

    String machineInformationFormatter(int machineId, String machineName, String machineType, String machineSpeed, String machineState){
        StringBuffer buffer = new StringBuffer();

        buffer.append(
            "Machine Unique Id:\t" + machineId + "\n" +
            "Machine Name:\t\t" + machineName + "\n" +
            "Machine Type:\t\t" + machineType + "\n" +
            "Machine Production Speed:\t" + machineSpeed + "\n" +
            "Machine State:\t\t" + machineState + "\n\n"
        );

        return String.valueOf(buffer);
    }

    String jobInformationFormatter(int jobId, String jobType, String jobLength, String jobState){
        StringBuffer buffer = new StringBuffer();

        buffer.append(
            "Job Unique Id:\t" + jobId + "\n" +
            "Job Type:\t" + jobType + "\n" +
            "Job Length:\t" + jobLength + "\n" +
            "Job State:\t" + jobState + "\n\n"
        );

        return String.valueOf(buffer);
    }
}