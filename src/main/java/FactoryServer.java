import com.google.gson.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FactoryServer
{
	private static ServerSocket serverSocket;
	private static final int PORT = 9999;
	private static CustomArrayList<Machine> machinesSharedLocation = new SynchedCustomArrayList<Machine>();
	private static CustomArrayList<Job> jobsSharedLocation = new SynchedCustomArrayList<Job>();
	private static ArrayList<User> users = new ArrayList<User>();
	private static final ExecutorService executorService = Executors.newCachedThreadPool();
	private static CustomMap jobSchedulingSharedLocation = new SynchedCustomMap();

	static Map<String, String> USERNAME_PASSWORD_MAP = new HashMap<String, String>();

	private static final GsonBuilder gsonBuilder = new GsonBuilder();
	private static final Gson gson = gsonBuilder.serializeNulls().create();

	public static void main(String[] args) throws IOException
	{
		users.add(new User("admin", "admin"));
		users.add(new User("admin1", "admin1"));
		users.add(new User("admin2", "admin2"));

		USERNAME_PASSWORD_MAP.put("admin", "admin");
		USERNAME_PASSWORD_MAP.put("admin1", "admin1");
		USERNAME_PASSWORD_MAP.put("admin2", "admin2");

		JobTypes.METRICS.put(JobTypes.CNC, JobTypes.CNC_METRIC);
		JobTypes.METRICS.put(JobTypes.DOKUM, JobTypes.DOKUM_METRIC);
		JobTypes.METRICS.put(JobTypes.KILIF, JobTypes.KILIF_METRIC);
		JobTypes.METRICS.put(JobTypes.KAPLAMA, JobTypes.KAPLAMA_METRIC);

		try {
			serverSocket = new ServerSocket(PORT);
		}
		catch (IOException ioEx) {
			System.out.println("\nUnable to set up port!");
			System.exit(1);
		}

		do {
			Socket client = serverSocket.accept();
			System.out.println("\nNew client accepted.\n");

			ClientHandler handler = new ClientHandler(client, machinesSharedLocation, jobsSharedLocation, users, executorService, jobSchedulingSharedLocation, gson);

			handler.start();
		}while (true);
	}
}

class ClientHandler extends Thread
{
	private final Socket client;
	private Scanner input;
	private PrintWriter output;
	private CustomArrayList<Machine> machinesSharedLocation;
	private CustomArrayList<Job> jobsSharedLocation;
	private ArrayList<User> users;
	private final ExecutorService executorService;
	private CustomMap jobSchedulingSharedLocation;
	private final Gson gson;

	private Future<ArrayList<Integer>> jobsArrayListFeature;
	private ArrayList<Integer> jobsArrayList = new ArrayList<Integer>();

	private Future<ArrayList<Job>> jobsFuture;
	private ArrayList<Job> jobs = new ArrayList<Job>();

	private Future<ArrayList<Machine>> machinesFuture;
	private ArrayList<Machine> machines = new ArrayList<Machine>();

	public ClientHandler(Socket socket, CustomArrayList<Machine> machinesSharedLocation, CustomArrayList<Job> jobsSharedLocation, ArrayList<User> users, ExecutorService executorService, CustomMap jobSchedulingSharedLocation, Gson gson)
	{
		this.client = socket;
		this.machinesSharedLocation = machinesSharedLocation;
		this.jobsSharedLocation = jobsSharedLocation;
		this.users = users;
		this.executorService = executorService;
		this.jobSchedulingSharedLocation = jobSchedulingSharedLocation;
		this.gson = gson;

		try {
			input = new Scanner(client.getInputStream());
			output = new PrintWriter(client.getOutputStream(),true);
		}
		catch(IOException ioEx) {
			ioEx.printStackTrace();
		}
	}

	@Override
	public void run() {
		String received, command, argumentsJsonString;

		do {
			try{
				received = input.nextLine();
			}
			catch (NoSuchElementException e){
				break;
			}

			String[] splitReceived = received.split("\\u002A");

			command = splitReceived[0];

			String metric;
			User receivedUser;

			switch (command) {
				case Commands.LOGIN -> {
					try {
						argumentsJsonString = splitReceived[1];

						try {
							receivedUser = gson.fromJson(argumentsJsonString, User.class);
							receivedUser.UserStatus = User.OFFLINE;
						} catch (JsonParseException e) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					} catch (ArrayIndexOutOfBoundsException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}
					if (receivedUser.UserName.isEmpty() || receivedUser.UserPassword.isEmpty()) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}
					boolean isUserNameOrPasswordIsWrong = true;
					for (User user : users) {
						if (receivedUser.UserName.equals(user.UserName)) {
							if (user.UserStatus.equals(User.ONLINE)) {
								output.println(gson.toJson(ErrorResponses.RESPONSE_201));
								break;
							}
							else {
								if (receivedUser.UserPassword.equals(user.UserPassword)) {
									user.UserStatus = User.ONLINE;
									output.println(gson.toJson(SuccessfulResponses.RESPONSE_100));
									isUserNameOrPasswordIsWrong = false;
									System.out.println(user.UserName + " has logged in.");
									break;
								}
								else {
									output.println(gson.toJson(ErrorResponses.RESPONSE_200));
									break;
								}
							}
						}
					}
					if (isUserNameOrPasswordIsWrong) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_200));
					}
				}

				case Commands.LOGOFF -> {
					try {
						argumentsJsonString = splitReceived[1];

						try {
							receivedUser = gson.fromJson(argumentsJsonString, User.class);
						}
						catch (JsonParseException e) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					try {
						if (receivedUser.UserName.isEmpty() || receivedUser.UserPassword.isEmpty()) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					} catch (NullPointerException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					for (User user : users) {
						if (receivedUser.UserName.equals(user.UserName)) {
							if (receivedUser.UserPassword.equals(user.UserPassword)) {
								if (user.UserStatus.equals(User.ONLINE)) {
									user.UserStatus = User.OFFLINE;
									output.println(gson.toJson(SuccessfulResponses.RESPONSE_101));
									System.out.println(user.UserName + " has logged off.");
									break;
								}
							}
						}
					}
					output.println(gson.toJson(ErrorResponses.RESPONSE_202));
				}

				case Commands.ADD_MACHINE -> {
					Machine receivedMachine;
					boolean machineIdIsAlreadyExist = false;

					try {
						argumentsJsonString = splitReceived[1];

						try {
							receivedMachine = gson.fromJson(argumentsJsonString, Machine.class);
							receivedMachine.MachineState = Machine.MACHINE_STATE_EMPTY;
						}
						catch (JsonParseException e) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					try {
						if (receivedMachine.MachineUniqueId == 0 ||
							receivedMachine.MachineName.isEmpty() ||
							receivedMachine.MachineType.isEmpty() ||
							receivedMachine.MachineProductionSpeed.isEmpty()) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (NullPointerException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					for (Machine machine : machines) {
						if (machine.MachineUniqueId == receivedMachine.MachineUniqueId) {
							machineIdIsAlreadyExist = true;
							break;
						}
					}

					if (machineIdIsAlreadyExist) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_210));
					}
					else {
						if (!(receivedMachine.MachineType.equals(JobTypes.CNC) ||
								receivedMachine.MachineType.equals(JobTypes.DOKUM) ||
								receivedMachine.MachineType.equals(JobTypes.KILIF) ||
								receivedMachine.MachineType.equals(JobTypes.KAPLAMA))) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_211));
							break;
						}

						try{
							Integer.parseInt(receivedMachine.MachineProductionSpeed.split(" ")[0]);
						}
						catch (NumberFormatException e){
							output.println(gson.toJson(ErrorResponses.RESPONSE_213));
							break;
						}

						try {
							metric = receivedMachine.MachineProductionSpeed.split(" ")[1];
							if (metric.equals(JobTypes.METRICS.get(receivedMachine.MachineType))) {
								executorService.execute(new ArrayListProducer<Machine>(machinesSharedLocation, receivedMachine));

								output.println(gson.toJson(new Response<String>(110, ResponseMessages.RESPONSE_MESSAGE_110, null)));

								System.out.println(
									"Added Machine: " +
									"Machine Unique Id: " + receivedMachine.MachineUniqueId + ", " +
									"Machine Name: " + receivedMachine.MachineName + ", " +
									"Machine Type: " + receivedMachine.MachineType + ", " +
									"Machine Production Speed: " + receivedMachine.MachineProductionSpeed + ", " +
									"Machine State: " + receivedMachine.MachineState
								);

								assignJobToMachine();
							} else {
								output.println(gson.toJson(ErrorResponses.RESPONSE_212));
							}
						}
						catch (ArrayIndexOutOfBoundsException e){
							output.println(gson.toJson(ErrorResponses.RESPONSE_212));
						}
					}
				}

				case Commands.GET_MACHINES -> {
					GetMachinesResponse getMachinesResponse = new GetMachinesResponse();
					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if (machines.size() != 0) {
						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.CNC))
								getMachinesResponse.CNCMachines.add(machine);

						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.DOKUM))
								getMachinesResponse.DOKUMMachines.add(machine);

						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.KILIF))
								getMachinesResponse.KILIFMachines.add(machine);

						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.KAPLAMA))
								getMachinesResponse.KAPLAMAMachines.add(machine);

						output.println(gson.toJson(new Response<GetMachinesResponse>(150, ResponseMessages.RESPONSE_MESSAGE_150, getMachinesResponse)));
					} else {
						output.println(gson.toJson(new Response<GetMachinesResponse>(420, ResponseMessages.RESPONSE_MESSAGE_420, null)));
					}
				}

				case Commands.GET_MACHINE_INFORMATIONS -> {
					GetMachineInformationRequest receivedMachineId;
					GetMachineInformationResponse getMachineInformationResponse = new GetMachineInformationResponse();

					try {
						argumentsJsonString = splitReceived[1];

						try {
							receivedMachineId = gson.fromJson(argumentsJsonString, GetMachineInformationRequest.class);

							if(receivedMachineId.MachineId == 0){
								output.println(gson.toJson(ErrorResponses.RESPONSE_410));
								break;
							}
						} catch (JsonParseException e) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if(machines.size() == 0){
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
						break;
					}

					for (Machine machine : machines) {
						if (machine.MachineUniqueId == receivedMachineId.MachineId) {
							getMachineInformationResponse.Machine = machine;
							break;
						}
					}

					if (getMachineInformationResponse.Machine == null) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_220));
					} else {
						jobsArrayListFeature = executorService.submit(new JobAssignmentConsumer(jobSchedulingSharedLocation, receivedMachineId.MachineId));

						try {
							jobsArrayList = jobsArrayListFeature.get();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}

						if (jobsArrayList == null) {
							output.println(gson.toJson(new Response<GetMachineInformationResponse>(
									120,
									ResponseMessages.RESPONSE_MESSAGE_120,
									getMachineInformationResponse))
							);
							break;
						}

						jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

						try {
							jobs = jobsFuture.get();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}

						for (Job job : jobs)
							for (Integer jobId : jobsArrayList)
								if (job.JobUniqueId == jobId)
									if (job.JobState.equals(Job.JOB_STATE_COMPLETED))
										getMachineInformationResponse.CompletedJobs.add(job);

						output.println(gson.toJson(new Response<GetMachineInformationResponse>(
								120,
								ResponseMessages.RESPONSE_MESSAGE_120,
								getMachineInformationResponse))
						);
					}
				}

				case Commands.SEND_JOB_ORDER -> {
					Job receivedJob;
					boolean isJobIdAlreadyExist = false;

					try {
						argumentsJsonString = splitReceived[1];

						try {
							receivedJob = gson.fromJson(argumentsJsonString, Job.class);
							receivedJob.JobState = Job.JOB_STATE_PENDING;
						}
						catch (JsonParseException e) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					try {
						if (receivedJob.JobUniqueId == 0 ||
							receivedJob.JobType.isEmpty() ||
							receivedJob.JobLength.isEmpty()) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (NullPointerException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

					try {
						jobs = jobsFuture.get();
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					for (Job job : jobs) {
						if (job.JobUniqueId == receivedJob.JobUniqueId) {
							isJobIdAlreadyExist = true;
							break;
						}
					}

					if (isJobIdAlreadyExist) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_230));
					}
					else {
						if (!(receivedJob.JobType.equals(JobTypes.CNC) ||
								receivedJob.JobType.equals(JobTypes.DOKUM) ||
								receivedJob.JobType.equals(JobTypes.KILIF) ||
								receivedJob.JobType.equals(JobTypes.KAPLAMA))) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_231));
							break;
						}

						try {
							metric = receivedJob.JobLength.split(" ")[1];

							if (metric.equals(JobTypes.METRICS.get(receivedJob.JobType))) {
								executorService.execute(new ArrayListProducer<Job>(jobsSharedLocation, receivedJob));

								output.println(gson.toJson(SuccessfulResponses.RESPONSE_130));

								System.out.println(
									"Added Job: " +
									"Job Unique Id: " + receivedJob.JobUniqueId + ", " +
									"Job Type: " + receivedJob.JobType + ", " +
									"Job Length: " + receivedJob.JobLength + ", " +
									"Job State: " + receivedJob.JobState
								);

								assignJobToMachine();
							} else {
								output.println(gson.toJson(ErrorResponses.RESPONSE_232));
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_232));
						}
					}
				}

				case Commands.GET_PENDING_JOB_ORDERS -> {
					GetPendingJobOrdersResponse getPendingJobOrdersResponse = new GetPendingJobOrdersResponse();
					jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

					boolean isCNCPendingJobExist = false, isDOKUMPendingJobExist = false, isKILIFPendingJobExist = false, isKAPLAMAPendingJobExist = false;

					try {
						jobs = jobsFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if(jobs.size() == 0){
						output.println(gson.toJson(ErrorResponses.RESPONSE_430));
						break;
					}

					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.CNC)) {
								getPendingJobOrdersResponse.CNCJobOrders.add(job);
								isCNCPendingJobExist = true;
							}
					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.DOKUM)) {
								getPendingJobOrdersResponse.DOKUMJobOrders.add(job);
								isDOKUMPendingJobExist = true;
							}
					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.KILIF)) {
								getPendingJobOrdersResponse.KILIFJobOrders.add(job);
								isKILIFPendingJobExist = true;
							}
					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.KAPLAMA)) {
								getPendingJobOrdersResponse.KAPLAMAJobOrders.add(job);
								isKAPLAMAPendingJobExist = true;
							}

					if(!(isCNCPendingJobExist || isDOKUMPendingJobExist || isKILIFPendingJobExist || isKAPLAMAPendingJobExist)){
						output.println(gson.toJson(ErrorResponses.RESPONSE_430));
						break;
					}
					output.println(gson.toJson(new Response<GetPendingJobOrdersResponse>(
							160,
							ResponseMessages.RESPONSE_MESSAGE_160,
							getPendingJobOrdersResponse))
					);
				}

				case Commands.GET_MACHINE_STATES -> {
					GetMachineStatesResponse getMachineStatesResponse = new GetMachineStatesResponse();
					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));
					try {
						machines = machinesFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
					if (machines.size() != 0) {
						for (Machine machine : machines)
							getMachineStatesResponse.Machines.add(machine);

						output.println(gson.toJson(new Response<GetMachineStatesResponse>(170, ResponseMessages.RESPONSE_MESSAGE_170, getMachineStatesResponse)));
					} else {
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
					}
				}

				case Commands.GET_PROCESSING_JOB_ORDERS -> {
					GetProcessingJobResponse getProcessingJobResponse = new GetProcessingJobResponse();

					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

					try {
						jobs = jobsFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if (machines.size() != 0) {
						for (Machine machine : machines) {
							if (machine.MachineState.equals(Machine.MACHINE_STATE_BUSY)) {
								jobsArrayListFeature = executorService.submit(new JobAssignmentConsumer(jobSchedulingSharedLocation, machine.MachineUniqueId));

								try {
									jobsArrayList = jobsArrayListFeature.get();
								} catch (InterruptedException | ExecutionException e) {
									e.printStackTrace();
								}

								if (jobsArrayList == null) {
									getProcessingJobResponse.Machines.add(machine);
									getProcessingJobResponse.Jobs.add(null);
									continue;
								}

								for (Job job : jobs) {
									for (Integer jobId1 : jobsArrayList) {
										if (job.JobUniqueId == jobId1) {
											if (job.JobState.equals(Job.JOB_STATE_PROCESSING)) {
												getProcessingJobResponse.Machines.add(machine);
												getProcessingJobResponse.Jobs.add(job);
											}
										}
									}
								}
							}
						}

						if (getProcessingJobResponse.Machines.size() == 0) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_240));
						} else {
							output.println(gson.toJson(new Response<GetProcessingJobResponse>(
									140,
									ResponseMessages.RESPONSE_MESSAGE_140,
									getProcessingJobResponse))
							);
						}
					} else {
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
					}
				}
				default -> output.println(gson.toJson(ErrorResponses.RESPONSE_400));
			}
		} while (!received.equals("EXIT"));

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
		jobsFuture =  executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

		try {
			jobs = jobsFuture.get();
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

		try {
			machines = machinesFuture.get();
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

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
							executorService.execute(new JobAssignmentProducer(jobSchedulingSharedLocation, machine.MachineUniqueId, job.JobUniqueId));
							System.out.println("Job assigned.");
							completeJob(machine.MachineUniqueId, job.JobUniqueId, delay);
							break;
						}
					}
				}
			}
		}
	}

	void completeJob(int machineId, int jobId, long delay){
		TimerTask task = new TimerTask() {
			public void run() {
				jobsFuture =  executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

				try {
					jobs = jobsFuture.get();
				}
				catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}

				machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

				try {
					machines = machinesFuture.get();
				}
				catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}

				for(Machine machine: machines){
					if(machineId == machine.MachineUniqueId){
						machine.MachineState = Machine.MACHINE_STATE_EMPTY;
						System.out.println("Machine State: " + machine.MachineState + " " + "Machine Id: " + machine.MachineUniqueId);
					}
				}

				for(Job job: jobs){
					if(jobId == job.JobUniqueId){
						job.JobState = Job.JOB_STATE_COMPLETED;
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

