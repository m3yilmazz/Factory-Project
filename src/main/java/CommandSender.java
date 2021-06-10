import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import java.io.PrintWriter;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class CommandSender extends SwingWorker<String, Object> {
    private final String message;
    Scanner networkInput;
    PrintWriter networkOutput;
    JTextArea responseJTextArea;
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();

    CommandSender(String message, Scanner networkInput, PrintWriter networkOutput, JTextArea responseJTextArea){
        this.message = message;
        this.networkInput = networkInput;
        this.networkOutput = networkOutput;
        this.responseJTextArea = responseJTextArea;
    }

    @Override
    protected String doInBackground() {
        String response;
        networkOutput.println(message);
        response = networkInput.nextLine();
        return  response;
    }

    protected void done()
    {
        String response, command;
        StringBuffer buffer = new StringBuffer();
        try
        {
            response = get();
            command = message.split("\\u002A")[0];
            switch (command){
                case Commands.ADD_MACHINE:
                    switch (response) {
                        case "410" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
                        case "210" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The given machine id has already in use.");
                        case "211" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine type is not valid.");
                        case "212" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine type does not match with its production metric.");
                        case "213" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The production speed must declare in numbers.");
                        case "110" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine added successfully.");
                    }
                    break;

                case Commands.GET_MACHINES:
                    switch (response) {
                        case "420" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                        default -> {
                            buffer = new StringBuffer();
                            GetMachinesResponse getMachinesResponse = gson.fromJson(response, GetMachinesResponse.class);
                            buffer.append("CNC MACHINES\n\n");
                            for (Machine machine : getMachinesResponse.CNCMachines)
                                buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                            buffer.append("DÖKÜM MACHINES\n\n");
                            for (Machine machine : getMachinesResponse.DOKUMMachines)
                                buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                            buffer.append("KILIF MACHINES\n\n");
                            for (Machine machine : getMachinesResponse.KILIFMachines)
                                buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                            buffer.append("KAPLAMA MACHINES\n\n");
                            for (Machine machine : getMachinesResponse.KAPLAMAMachines)
                                buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                            responseJTextArea.setText(String.valueOf(buffer));
                        }
                    }
                    break;

                case Commands.GET_MACHINE_INFORMATIONS:
                    switch (response) {
                        case "410" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
                        case "420" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                        case "220" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no machine with the given id.");
                        default -> {
                            buffer = new StringBuffer();
                            GetMachineInformationResponse getMachineInformationResponse = gson.fromJson(response, GetMachineInformationResponse.class);
                            buffer.append("MACHINE INFORMATIONS\n\n");
                            buffer.append(machineInformationFormatter(
                                    getMachineInformationResponse.Machine.MachineUniqueId,
                                    getMachineInformationResponse.Machine.MachineName,
                                    getMachineInformationResponse.Machine.MachineType,
                                    getMachineInformationResponse.Machine.MachineProductionSpeed,
                                    getMachineInformationResponse.Machine.MachineState
                            ));
                            buffer.append("COMPLETED JOBS\n\n");
                            for (Job job : getMachineInformationResponse.CompletedJobs)
                                buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                            responseJTextArea.setText(String.valueOf(buffer));
                        }
                    }
                    break;

                case Commands.SEND_JOB_ORDER:
                    switch (response) {
                        case "410" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is one or more missing argument in the command.");
                        case "230" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The given job id has already in use.");
                        case "231" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The job type is not valid.");
                        case "232" -> responseJTextArea.setText("Response Code: " + response + "\nThe job type does not match with its production metric.");
                        case "130" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The job added successfully.");
                    }
                    break;

                case Commands.GET_PENDING_JOB_ORDERS:
                    switch (response) {
                        case "430" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The job list is empty.");
                        default -> {
                            buffer = new StringBuffer();
                            GetPendingJobOrdersResponse getPendingJobOrdersResponse = new GetPendingJobOrdersResponse();
                            try {
                                getPendingJobOrdersResponse = gson.fromJson(response, GetPendingJobOrdersResponse.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            buffer.append("PENDING JOB ORDERS\n\n");
                            buffer.append("CNC JOB ORDERS\n\n");
                            for (Job job : getPendingJobOrdersResponse.CNCJobOrders)
                                buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                            buffer.append("DÖKÜM JOB ORDERS\n\n");
                            for (Job job : getPendingJobOrdersResponse.DOKUMJobOrders)
                                buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                            buffer.append("KILIF JOB ORDERS\n\n");
                            for (Job job : getPendingJobOrdersResponse.KILIFJobOrders)
                                buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                            buffer.append("KAPLAMA JOB ORDERS\n\n");
                            for (Job job : getPendingJobOrdersResponse.KAPLAMAJobOrders)
                                buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                            responseJTextArea.setText(String.valueOf(buffer));
                        }
                    }

                    break;

                case Commands.GET_MACHINE_STATES:
                    switch (response) {
                        case "420" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                        default -> {
                            buffer = new StringBuffer();
                            GetMachineStatesResponse getMachineStatesResponse = gson.fromJson(response, GetMachineStatesResponse.class);
                            for (Machine machine : getMachineStatesResponse.Machines)
                                buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                            responseJTextArea.setText(String.valueOf(buffer));
                        }
                    }
                    break;

                case Commands.GET_PROCESSING_JOB_ORDERS:
                    switch (response) {
                        case "420" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: The machine list is empty.");
                        case "240" -> responseJTextArea.setText("Response Code: " + response + "\nResponse Message: There is no busy machine.");
                        default -> {
                            buffer = new StringBuffer();
                            GetProcessingJobResponse getProcessingJobResponse = gson.fromJson(response, GetProcessingJobResponse.class);
                            for (int index = 0; index < getProcessingJobResponse.Machines.size(); ++index) {
                                buffer.append("MACHINE\n\n");
                                buffer.append(machineInformationFormatter(
                                        getProcessingJobResponse.Machines.get(index).MachineUniqueId,
                                        getProcessingJobResponse.Machines.get(index).MachineName,
                                        getProcessingJobResponse.Machines.get(index).MachineType,
                                        getProcessingJobResponse.Machines.get(index).MachineProductionSpeed,
                                        getProcessingJobResponse.Machines.get(index).MachineState
                                ));

                                buffer.append("PROCESSING JOB IN THE MACHINE\n\n");
                                buffer.append(jobInformationFormatter(
                                        getProcessingJobResponse.Jobs.get(index).JobUniqueId,
                                        getProcessingJobResponse.Jobs.get(index).JobType,
                                        getProcessingJobResponse.Jobs.get(index).JobLength,
                                        getProcessingJobResponse.Jobs.get(index).JobState
                                ));
                            }
                            responseJTextArea.setText(String.valueOf(buffer));
                        }
                    }
                    break;
            }
        }
        catch ( InterruptedException ex ) {
            responseJTextArea.setText("Interrupted while waiting for results.");
        }
        catch ( ExecutionException ex ) {
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
