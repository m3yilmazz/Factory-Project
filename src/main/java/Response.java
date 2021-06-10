public class Response<T> {
    int ResponseCode;
    T ResponseMessage;

    Response(int ResponseCode, T ResponseMessage){
        this.ResponseCode = ResponseCode;
        this.ResponseMessage = ResponseMessage;
    }
}
