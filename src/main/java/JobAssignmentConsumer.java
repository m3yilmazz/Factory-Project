import java.util.ArrayList;

public class JobAssignmentConsumer implements Runnable
{
   private final CustomMap sharedLocation;
   private final int machineId;

   public JobAssignmentConsumer(CustomMap shared, int machineId)
   {
      sharedLocation = shared;
      this.machineId = machineId;
   }

   public void run()
   {
      try
      {
         ArrayList<Integer> jobList = sharedLocation.get(machineId);
         for(int index: jobList){
            System.out.println("index: " + index);
         }
      }
      catch ( InterruptedException exception )
      {
         exception.printStackTrace();
      }
   }

   protected void finalize() {

   }
}