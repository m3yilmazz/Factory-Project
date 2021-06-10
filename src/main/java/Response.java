public class Response<T> {
    int ResponseCode;
    String ResponseMessage;
    T ResponseBody;

    Response(int ResponseCode, String ResponseMessage, T ResponseBody){
        this.ResponseCode = ResponseCode;
        this.ResponseMessage = ResponseMessage;
        this.ResponseBody = ResponseBody;
    }
}
