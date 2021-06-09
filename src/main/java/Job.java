public class Job {
    static final String JOB_STATE_PENDING = "PENDING";
    static final String JOB_STATE_PROCESSING = "PROCESSING";
    static final String JOB_STATE_DONE = "DONE";

    int JobUniqueId;
    String JobType;
    String JobLength;
    String JobState;

    Job(int JobUniqueId, String JobType, String JobLength){
        this.JobUniqueId = JobUniqueId;
        this.JobType = JobType;
        this.JobLength = JobLength;
        this.JobState = Job.JOB_STATE_PENDING;
    }
}
