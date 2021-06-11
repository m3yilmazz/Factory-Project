public class JobAssignmentRemover implements Runnable
{
   private final CustomMap sharedLocation;
   private final int machineId;

   public JobAssignmentRemover(CustomMap shared, int machineId)
   {
      sharedLocation = shared;
      this.machineId = machineId;
   }

   public void run()
   {
      try
      {
         sharedLocation.remove(machineId);
      }
      catch ( InterruptedException exception )
      {
         exception.printStackTrace();
      }
   }
}