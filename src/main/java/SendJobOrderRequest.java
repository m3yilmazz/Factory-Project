public class SendJobOrderRequest {
    int JobUniqueId;
    String JobType;
    String JobLength;

    SendJobOrderRequest(int JobUniqueId, String JobType, String JobLength){
        this.JobUniqueId = JobUniqueId;
        this.JobType = JobType;
        this.JobLength = JobLength;
    }
}
