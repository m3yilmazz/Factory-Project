import java.util.ArrayList;

public interface CustomArrayList<T> {
    public void set(T object) throws InterruptedException;
    public ArrayList<T> get() throws InterruptedException;
}
