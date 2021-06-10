import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ArrayListConsumer<T> implements Callable<ArrayList<T>> {
    private final CustomArrayList<T> sharedArrayList;

    ArrayListConsumer(CustomArrayList<T> sharedArrayList){
        this.sharedArrayList = sharedArrayList;
    }
    @Override
    public ArrayList<T> call() throws Exception {
        ArrayList<T> arrayList = new ArrayList<T>();
        try
        {
            arrayList = sharedArrayList.get();
        }
        catch ( InterruptedException exception )
        {
            exception.printStackTrace();
        }
        return arrayList;
    }
}
