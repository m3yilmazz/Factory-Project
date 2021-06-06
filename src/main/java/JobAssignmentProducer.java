public class JobAssignmentProducer implements Runnable
{
   private final CustomMap sharedLocation;
   private final int machineId;
   private final int jobId;

   public JobAssignmentProducer(CustomMap shared, int machineId, int jobId)
   {
       sharedLocation = shared;
       this.machineId = machineId;
       this.jobId = jobId;
   }

   public void run()                             
   {
      try
      {
         sharedLocation.set( machineId, jobId );
      }
      catch ( InterruptedException exception )
      {
         exception.printStackTrace();
      }
   }
}