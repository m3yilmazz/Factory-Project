public class Request<T> {
    String RequestCommand;
    T RequestBody;

    Request(String ResponseMessage, T ResponseBody){
        this.RequestCommand = ResponseMessage;
        this.RequestBody = ResponseBody;
    }
}
