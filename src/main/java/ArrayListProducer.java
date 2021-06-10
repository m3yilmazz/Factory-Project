public class ArrayListProducer<T> implements Runnable{
    private final CustomArrayList<T> sharedArrayList;
    private final T object;

    public ArrayListProducer(CustomArrayList<T> sharedArrayList, T object){
        this.sharedArrayList = sharedArrayList;
        this.object = object;
    }

    @Override
    public void run() {
        try
        {
            sharedArrayList.set( object );
        }
        catch ( InterruptedException exception )
        {
            exception.printStackTrace();
        }
    }
}
