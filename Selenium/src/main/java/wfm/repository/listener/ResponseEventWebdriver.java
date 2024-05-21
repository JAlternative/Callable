package wfm.repository.listener;

public class ResponseEventWebdriver implements CallBack {

    @Override
    public WebDriverEventCapture.Event callingBack(WebDriverEventCapture.Event response) {
        if (response != null) {
            System.out.println("callingBack = " + response.getName() + " " + response.getValue());
        }
        return response;
    }


}