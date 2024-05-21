package wfm.repository.listener;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.AbstractWebDriverEventListener;

public class WebDriverEventCapture extends AbstractWebDriverEventListener {

    public CallBack callBack;
    String result;

    public void registerCallback(CallBack callBack) {
        this.callBack = callBack;
    }

    public void doSomething(Event result) {
        System.out.println("Ожидание события");
        callBack.callingBack(result);
    }

    @Override
    public void beforeNavigateTo(String url, WebDriver driver) {
        result = "beforeNavigateTo " + url;
        doSomething(Event.BEFORE_NAVIGATE_TO);
    }

    @Override
    public void afterNavigateTo(String url, WebDriver driver) {
//        System.out.println("afterNavigateTo - " + url);
        Event result = Event.AFTER_NAVIGATE_TO;
        result.setValue(url);
        doSomething(result);
    }

    @Override
    public void afterNavigateBack(WebDriver driver) {
//        System.out.println("afterNavigateBack");
        result = "afterNavigateBack";
        doSomething(Event.AFTER_NAVIGATE_BACK);
    }

    @Override
    public void beforeNavigateBack(WebDriver driver) {
//        System.out.println("beforeNavigateBack");
        result = "beforeNavigateBack";
        doSomething(Event.BEFORE_NAVIGATE_BACK);
    }

    @Override
    public void beforeNavigateForward(WebDriver driver) {
//        System.out.println("beforeNavigateForward");
        result = "beforeNavigateForward";
        doSomething(Event.BEFORE_NAVIGATE_FORWARD);
    }

    @Override
    public void afterNavigateForward(WebDriver driver) {
//        System.out.println("afterNavigateForward");
        result = "afterNavigateForward";
        doSomething(Event.AFTER_NAVIGATE_FORWARD);
    }

    @Override
    public void beforeNavigateRefresh(WebDriver driver) {
//        System.out.println("beforeNavigateRefresh Сработало");
        result = "beforeNavigateRefresh Сработало";
        doSomething(Event.BEFORE_NAVIGATE_REFRESH);
    }

    @Override
    public void afterNavigateRefresh(WebDriver driver) {
//        System.out.println("beforeNavigateRefresh Сработало");
        result = "beforeNavigateRefresh Сработало";
        doSomething(Event.AFTER_NAVIGATE_REFRESH);
    }

    @Override
    public void beforeScript(String script, WebDriver driver) {
//        System.out.println("beforeNavigateRefresh Сработало");
        result = "beforeNavigateRefresh Сработало";
        doSomething(Event.BEFORE_SCRIPT);
    }

    @Override
    public void afterScript(String script, WebDriver driver) {
//        System.out.println("afterNavigateRefresh Сработало");
        result = "afterNavigateRefresh Сработало";
        doSomething(Event.AFTER_SCRIPT);
    }

    @Override
    public void onException(Throwable throwable, WebDriver driver) {
//        System.out.println("onException" + throwable.getMessage());
//        result = "onException" + throwable.getMessage();
        Event result = Event.ON_EXCEPTION;
        result.setValue(throwable.getMessage());
        doSomething(result);
    }

    public static enum Event {

        BEFORE_NAVIGATE_TO("BEFORE_NAVIGATE_TO", ""),
        AFTER_NAVIGATE_TO("AFTER_NAVIGATE_TO", ""),
        BEFORE_NAVIGATE_BACK("BEFORE_NAVIGATE_BACK", ""),
        AFTER_NAVIGATE_BACK("AFTER_NAVIGATE_BACK", ""),
        BEFORE_NAVIGATE_FORWARD("BEFORE_NAVIGATE_FORWARD", ""),
        AFTER_NAVIGATE_FORWARD("AFTER_NAVIGATE_FORWARD", ""),
        BEFORE_NAVIGATE_REFRESH("BEFORE_NAVIGATE_REFRESH", ""),
        AFTER_NAVIGATE_REFRESH("AFTER_NAVIGATE_REFRESH", ""),
        BEFORE_SCRIPT("BEFORE_SCRIPT", ""),
        AFTER_SCRIPT("AFTER_SCRIPT", ""),
        ON_EXCEPTION("ON_EXCEPTION", "");

        String value;
        String name;

        Event(String name, String url) {
            this.value = url;
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }


    }
}