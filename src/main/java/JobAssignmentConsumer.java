import java.util.ArrayList;
import java.util.concurrent.Callable;

public class JobAssignmentConsumer implements Callable<ArrayList<Integer>>
{
   private final CustomMap sharedLocation;
   private final int machineId;

   public JobAssignmentConsumer(CustomMap shared, int machineId)
   {
      sharedLocation = shared;
      this.machineId = machineId;
   }

   @Override
   public ArrayList<Integer> call() {
      ArrayList<Integer> jobList = new ArrayList<Integer>();
      try
      {
         jobList = sharedLocation.get(machineId);
      }
      catch ( InterruptedException exception )
      {
         exception.printStackTrace();
      }

      return jobList;
   }
}