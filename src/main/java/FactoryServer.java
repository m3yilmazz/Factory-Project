import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FactoryServer
{
	private static ServerSocket serverSocket;
	private static final int PORT = 9999;
	private static ArrayList<Machine> machines = new ArrayList<Machine>();
	private static ArrayList<Job> jobs = new ArrayList<Job>();
	private static ExecutorService executorService = Executors.newCachedThreadPool();
	private static CustomMap sharedLocation = new SynchedCustomMap();

	public static void main(String[] args) throws IOException
	{
		User.USERNAME_PASSWORD_MAP.put("admin", "admin");
		User.USERNAME_PASSWORD_MAP.put("admin1", "admin1");
		User.USERNAME_PASSWORD_MAP.put("admin2", "admin2");

		JobTypes.METRICS.put(JobTypes.CNC, JobTypes.CNC_METRIC);
		JobTypes.METRICS.put(JobTypes.DOKUM, JobTypes.DOKUM_METRIC);
		JobTypes.METRICS.put(JobTypes.KILIF, JobTypes.KILIF_METRIC);
		JobTypes.METRICS.put(JobTypes.KAPLAMA, JobTypes.KAPLAMA_METRIC);

		try
		{
			serverSocket = new ServerSocket(PORT);
		}
		catch (IOException ioEx)
		{
			System.out.println("\nUnable to set up port!");
			System.exit(1);
		}

		do
		{
			Socket client = serverSocket.accept();
			System.out.println("\nNew client accepted.\n");

			ClientHandler handler = new ClientHandler(client, machines, jobs, executorService, sharedLocation);

			handler.start();
		}while (true);
	}
}

class ClientHandler extends Thread
{
	private Socket client;
	private Scanner input;
	private PrintWriter output;
	private ArrayList<Machine> machines;
	private ArrayList<Job> jobs;
	private ExecutorService executorService;
	private CustomMap sharedLocation;

	public ClientHandler(Socket socket, ArrayList<Machine> machines, ArrayList<Job> jobs, ExecutorService executorService, CustomMap sharedLocation)
	{
		client = socket;
		this.machines = machines;
		this.jobs = jobs;
		this.executorService = executorService;
		this.sharedLocation = sharedLocation;

		try
		{
			input = new Scanner(client.getInputStream());
			output = new PrintWriter(client.getOutputStream(),true);
		}
		catch(IOException ioEx)
		{
			ioEx.printStackTrace();
		}
	}

	public void run() {
		String received;
		String command;
		StringBuffer stringBuffer;
		do {
			received = input.nextLine();
			String[] splittedReceived = received.split("\\u002A");

			command = splittedReceived[0];

			String userName, userPassword;
			String machineIdString, machineName, machineType ,machineProductionSpeed, metric;
			String jobIdString, jobType, jobLength;
			int machineId;

			switch (command) {
				case Commands.LOGIN:
					try{
						userName = splittedReceived[1];
						userPassword = splittedReceived[2];
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					if(userPassword.equals(User.USERNAME_PASSWORD_MAP.get(userName))){
						output.println(100);
					}
					else {
						output.println(200);
					}
					
					break;

				case Commands.ADD_MACHINE:
					Machine newMachine = new Machine();
					boolean machineIdIsAlreadyExist = false;

					try{
						machineIdString = splittedReceived[1];
						machineName = splittedReceived[2];
						machineType = splittedReceived[3];
						machineProductionSpeed = splittedReceived[4];
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					machineId = Integer.parseInt(machineIdString);

					for(Machine machine: machines){
						if(machine.MachineUniqueId == machineId){
							machineIdIsAlreadyExist = true;
							break;
						}
					}

					if(machineIdIsAlreadyExist) {
						output.println(210);
						break;
					}
					else {
						newMachine.MachineUniqueId = machineId;
						newMachine.MachineName = machineName;

						boolean isMachineTypeValid = false;
						if(machineType.equals(JobTypes.CNC)){
							isMachineTypeValid = true;
						}
						else if (machineType.equals(JobTypes.DOKUM)) {
							isMachineTypeValid = true;
						}
						else if (machineType.equals(JobTypes.KILIF)){
							isMachineTypeValid = true;
						}
						else if(machineType.equals(JobTypes.KAPLAMA)){
							isMachineTypeValid = true;
						}

						if (isMachineTypeValid) {
							newMachine.MachineType = machineType;
						} else {
							output.println(211);
							break;
						}

						metric = machineProductionSpeed.split(" ")[1];
						if(metric.equals(JobTypes.METRICS.get(machineType))){
							newMachine.MachineProductionSpeed = machineProductionSpeed;

							newMachine.MachineState = Machine.MACHINE_STATE_EMPTY;
							machines.add(newMachine);

							output.println(110);

							assignJobToMachine();
						}
						else {
							output.println(212);
							break;
						}
					}
					break;

				case Commands.GET_MACHINES:
					stringBuffer = new StringBuffer();
					if(!machines.isEmpty()){
						stringBuffer.append("MACHINES ORDERED BY TYPES" + "**");

						stringBuffer.append(JobTypes.CNC + " MACHINES " + "**");
						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.CNC)) {
								stringBuffer.append(
										"Machine Unique Id: " + machine.MachineUniqueId + "*" +
										"Machine Name: " + machine.MachineName + "*" +
										"Machine Production Speed: " + machine.MachineProductionSpeed + "*" +
										"Machine State: " + machine.MachineState + "**"
								);
							}
						}

						stringBuffer.append(JobTypes.DOKUM + " MACHINES " + "**");
						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.DOKUM)) {
								stringBuffer.append(
										"Machine Unique Id: " + machine.MachineUniqueId + "*" +
										"Machine Name: " + machine.MachineName + "*" +
										"Machine Production Speed: " + machine.MachineProductionSpeed + "*" +
										"Machine State: " + machine.MachineState + "**"
								);
							}
						}

						stringBuffer.append(JobTypes.KILIF + " MACHINES " + "**");
						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.KILIF)) {
								stringBuffer.append(
										"Machine Unique Id: " + machine.MachineUniqueId + "*" +
										"Machine Name: " + machine.MachineName + "*" +
										"Machine Production Speed: " + machine.MachineProductionSpeed + "*" +
										"Machine State: " + machine.MachineState + "**"
								);
							}
						}

						stringBuffer.append(JobTypes.KAPLAMA + " MACHINES " + "**");
						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.KAPLAMA)) {
								stringBuffer.append(
										"Machine Unique Id: " + machine.MachineUniqueId + "*" +
										"Machine Name: " + machine.MachineName + "*" +
										"Machine Production Speed: " + machine.MachineProductionSpeed + "*" +
										"Machine State: " + machine.MachineState + "**"
								);
							}
						}

						output.println(stringBuffer);
					}
					else {
						output.println(220);
					}
					break;

				case Commands.GET_MACHINE_INFORMATIONS:
					try {
						machineIdString = splittedReceived[1];
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					int uniqueId = Integer.parseInt(machineIdString);
					stringBuffer = new StringBuffer();

					for(Machine machine: machines){
						if(machine.MachineUniqueId == uniqueId){
							stringBuffer.append(
									"Machine Unique Id: " + machine.MachineUniqueId + "*" +
									"Machine Name: " + machine.MachineName + "*" +
									"Machine Production Speed: " + machine.MachineProductionSpeed + "*" +
									"Machine State: " + machine.MachineState + "**"
							);
							break;
						}
					}
					if(stringBuffer.isEmpty()){
						output.println(230);
						break;
					}
					else {
						try {
							stringBuffer.append("JOBS DONE BY THAT MACHINE" + "**");
							List<Integer> jobList = sharedLocation.get(uniqueId);

							if(jobList == null){
								stringBuffer.append("NONE" + "**");
								output.println(stringBuffer);
								break;
							}

							for(Job job: jobs){
								for (Integer jobId: jobList){
									if(job.JobUniqueId == jobId){
										if(job.JobState.equals(Job.JOB_STATE_DONE)){
											stringBuffer.append(
													"Job Unique Id: " + job.JobUniqueId + "*" +
													"Job Type: " + job.JobType + "*" +
													"Job Length: " + job.JobLength + "**"
											);
										}
									}
								}
							}
						}
						catch (InterruptedException e) { }

						output.println(stringBuffer);
					}
					break;

				case Commands.SEND_JOB_ORDER:
					Job newJob = new Job();

					try{
						jobIdString = splittedReceived[1];
						jobType = splittedReceived[2];
						jobLength = splittedReceived[3];
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					int jobId = Integer.parseInt(jobIdString);

					boolean jobIdIsAlreadyExist = false;
					for(Job job: jobs){
						if(job.JobUniqueId == jobId){
							jobIdIsAlreadyExist = true;
							break;
						}
					}

					if(jobIdIsAlreadyExist) {
						output.println(240);
						break;
					}
					else {
						newJob.JobUniqueId = jobId;
						newJob.JobType = jobType;
						newJob.JobLength = jobLength;
						newJob.JobState = Job.JOB_STATE_PENDING;

						jobs.add(newJob);

						output.println(140);

						assignJobToMachine();
					}
					break;

				case Commands.GET_PENDING_JOB_ORDERS:
					stringBuffer = new StringBuffer();
					stringBuffer.append("PENDING JOB ORDERS" + "**");

					stringBuffer.append(JobTypes.CNC + " JOBS " + "**");
					for(Job job: jobs) {
						if (job.JobState.equals(Job.JOB_STATE_PENDING)) {
							if (job.JobType.equals(JobTypes.CNC)) {
								stringBuffer.append("JobUniqueId: " + job.JobUniqueId + "*" + "JobLength: " + job.JobLength + "**");
							}
						}
					}

					stringBuffer.append(JobTypes.DOKUM + " JOBS " + "**");
					for(Job job: jobs){
						if (job.JobState.equals(Job.JOB_STATE_PENDING)) {
							if(job.JobType.equals(JobTypes.DOKUM)){
								stringBuffer.append("JobUniqueId: " + job.JobUniqueId + "*" + "JobLength: " + job.JobLength + "**");
							}
						}
					}

					stringBuffer.append(JobTypes.KILIF + " JOBS " + "**");
					for(Job job: jobs){
						if (job.JobState.equals(Job.JOB_STATE_PENDING)) {
							if(job.JobType.equals(JobTypes.KILIF)){
								stringBuffer.append("JobUniqueId: " + job.JobUniqueId + "*" + "JobLength: " + job.JobLength + "**");
							}
						}
					}

					stringBuffer.append(JobTypes.KAPLAMA + " JOBS " + "**");
					for(Job job: jobs){
						if (job.JobState.equals(Job.JOB_STATE_PENDING)) {
							if(job.JobType.equals(JobTypes.KAPLAMA)){
								stringBuffer.append("JobUniqueId: " + job.JobUniqueId + "*" + "JobLength: " + job.JobLength + "**");
							}
						}
					}
					output.println(stringBuffer);
					break;

				case Commands.GET_MACHINE_STATES:
					stringBuffer = new StringBuffer();
					stringBuffer.append("MACHINES STATES" + "**");
					if(!machines.isEmpty()){
						for(Machine machine: machines){
							stringBuffer.append(
									"Machine Unique Id: " + machine.MachineUniqueId + "*" +
									"Machine Name: " + machine.MachineName + "*" +
									"Machine Type: " + machine.MachineType + "*" +
									"Machine State: " + machine.MachineState + "**"
							);
						}

						output.println(stringBuffer);
					}
					else {
						output.println(220);
						break;
					}
					break;

				case Commands.GET_PROCESSING_JOB_ORDERS:
					stringBuffer = new StringBuffer();
					if(!machines.isEmpty()){
						for(Machine machine: machines){
							if(machine.MachineState.equals(Machine.MACHINE_STATE_BUSY)){
								stringBuffer.append("MACHINE" + "**");
								stringBuffer.append(
										"Machine Unique Id: " + machine.MachineUniqueId + "*" +
										"Machine Name: " + machine.MachineName + "*" +
										"Machine Type: " + machine.MachineType + "*" +
										"Machine State: " + machine.MachineState + "**"
								);

								try {
									stringBuffer.append("PROCESSING JOB IN THE MACHINE" + "**");
									List<Integer> jobList = sharedLocation.get(machine.MachineUniqueId);
									if(jobList == null){
										stringBuffer.append("NONE" + "**");
										output.println(stringBuffer);
										break;
									}
									for(Job job: jobs){
										for (Integer jobId1: jobList){
											if(job.JobUniqueId == jobId1){
												if(job.JobState.equals(Job.JOB_STATE_PROCESSING)){
													stringBuffer.append(
															"Job Unique Id: " + job.JobUniqueId + "*" +
															"Job Type: " + job.JobType + "*" +
															"Job Length: " + job.JobLength + "**"
													);
												}
											}
										}
									}
								}
								catch (InterruptedException e) { }
							}
						}

						if(stringBuffer.isEmpty()){
							output.println(250);
							break;
						}
						else {
							output.println(stringBuffer);
						}
					}
					else {
						output.println(220);
						break;
					}

					break;

				default:
					output.println(400);
					break;
			}
		} while (!received.equals("QUIT"));

		try {
			if (client != null) {
				System.out.println("Closing down connection...");
				client.close();
			}
		} catch (IOException ioEx) {
			System.out.println("Unable to disconnect!");
		}
	}

	void assignJobToMachine(){
		for (Job job: jobs){
			if(job.JobState.equals("PENDING")){
				for(Machine machine: machines){
					if(machine.MachineState.equals("EMPTY")){
						if(machine.MachineType.equals(job.JobType)){
							int productionSpeed = Integer.parseInt(machine.MachineProductionSpeed.split(" ")[0]);
							int productLength = Integer.parseInt(job.JobLength.split(" ")[0]);
							//long delay = productLength / productionSpeed * 1000L * 60L;
							long delay = productLength / productionSpeed * 1000L;
							machine.MachineState = "BUSY";
							job.JobState = "PROCESSING";
							executorService.execute(new JobAssignmentProducer(sharedLocation, machine.MachineUniqueId, job.JobUniqueId));
							System.out.println("Job assigned.");
							doneJob(machine.MachineUniqueId, job.JobUniqueId, delay);
							break;
						}
					}
				}
			}
		}
	}

	void doneJob(int machineId, int jobId, long delay){
		TimerTask task = new TimerTask() {
			public void run() {
				for(Machine machine: machines){
					if(machineId == machine.MachineUniqueId){
						machine.MachineState = "EMPTY";
						System.out.println("Machine State: " + machine.MachineState + " " + "Machine Id: " + machine.MachineUniqueId);
					}
				}

				for(Job job: jobs){
					if(jobId == job.JobUniqueId){
						job.JobState = "DONE";
						System.out.println("Job State: " + job.JobState + " " + "Job Id: " + job.JobUniqueId);
					}
				}
				assignJobToMachine();
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, delay);
	}
}

