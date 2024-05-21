package testng.retry;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import utils.tools.LocaleKeys;

public class RetryAnalyzer implements IRetryAnalyzer {

    private int counter = 0;
    private final int retryLimit = Integer.parseInt(LocaleKeys.getAssertProperty("retryLimit"));

    @Override
    public boolean retry(ITestResult result) {
        if (counter < retryLimit) {
            counter++;
            return true;
        }
        return false;
    }
}
