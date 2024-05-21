package reporting;

import io.qameta.allure.Attachment;
//import io.qameta.atlas.webdriver.WebPage;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.xml.XmlClass;
import pages.*;
import testutils.AllureInfo;
import utils.tools.CustomTools;
import utils.tools.LocaleKeys;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import io.qameta.atlas.webdriver.WebPage;

import static utils.dropTestTools.DropAllTestsTools.checkLoginTestDrop;
import static utils.dropTestTools.DropAllTestsTools.checkLoginTestFail;
import static utils.dropTestTools.DropLoginTestReadWriter.writeLogs;


public class TestListener implements ITestListener {
    utils.BuildInfo allure;

    @SuppressWarnings("UnusedReturnValue")
    @Attachment(value = "Page screenshot", type = "image/png")
    private byte[] saveScreenshotPNG(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @SuppressWarnings("UnusedReturnValue")
    @Attachment(value = "Chromium console logs from Selenoid", type = "text/plain", fileExtension = ".txt")
    private String saveBrowserConsoleLogsTXT(WebDriver driver) {
        String output = "";
        LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
        Iterator<LogEntry> logEntryIterator = logEntries.iterator();
        while (logEntryIterator.hasNext()) {
            LogEntry entry = logEntryIterator.next();
            output = output.concat(entry.toString() + "\n");
        }
        return output;
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (LocaleKeys.getAssertProperty("remoteGradle").equals("remote")) {
            checkLoginTestDrop(result);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        CustomTools.recordSystemPropertiesInReport(result.getTestContext(), allure);
        checkLoginTestFail(result);
        takeScreenshot(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        CustomTools.recordSystemPropertiesInReport(result.getTestContext(), allure);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        CustomTools.recordSystemPropertiesInReport(result.getTestContext(), allure);
        takeScreenshot(result);
        result.getTestContext().getAttributeNames().stream()
                .filter(e -> e.equals("Capture logs"))
                .findFirst().ifPresent(logAttribute -> attachBrowserConsoleLog(result));
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        CustomTools.recordSystemPropertiesInReport(result.getTestContext(), allure);
        result.getTestContext().getAttributeNames().stream()
                .filter(e -> e.equals("Capture logs"))
                .findFirst().ifPresent(logAttribute -> attachBrowserConsoleLog(result));
    }

    @Override
    public void onStart(ITestContext context) {
        List<XmlClass> classes = context.getCurrentXmlTest().getClasses();
        if (classes.size() == 1 && classes.get(0).getName().contains("Bio")) {
            return;
        }
        allure = AllureInfo.setAllureEnvironmentInformation();
    }

    @Override
    public void onFinish(ITestContext context) {
        writeLogs(context.getCurrentXmlTest().getClasses());
        CustomTools.revertChangedSystemProperties(context);
    }

    private void takeScreenshot(ITestResult result) {
        if (result.getInstance().getClass().getName().toLowerCase().contains("api") ||
                result.getInstance().getClass().getName().toLowerCase().contains("database")) {
            return;
        }
        Class<?> c = result.getTestClass().getRealClass();
        Field[] fields = c.getDeclaredFields();
        String name = Arrays.stream(fields)
                .filter(field -> field.getType().isInterface()
                        && field.getType().getName().contains("pages")
                        && (field.getName().length() == 2 || field.getName().length() == 3)
                )
                .map(Field::getName)
                .findAny()
                .orElseThrow(() -> new AssertionError("Имя текущего интерфейса не совпадает с определенным в методе"));
        try {
            Field field = c.getDeclaredField(name);
            field.setAccessible(true);
            WebPage page;
            switch (name) {
                case "sb":
                    page = (ScheduleBoardPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "bp":
                    page = (BioPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "au":
                    page = (AddOrgUnitPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "ap":
                    page = (AnalyticsPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "bc":
                    page = (BatchCalculationPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "ps":
                    page = (PersonalSchedulePage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "os":
                    page = (OrgStructurePage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "ts":
                    page = (TasksPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "pp":
                    page = (MathParametersPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "rp":
                    page = (ReportsPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "bt":
                    page = (TerminalPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "lp":
                    page = (LoginPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "pt":
                    page = (PositionTypesPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "sl":
                    page = (SystemListsPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "fvp":
                    page = (FteOperationValuesPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "rm":
                    page = (RolesPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "sp":
                    page = (SupportPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "ssp":
                    page = (SystemSettingsPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "msp":
                    page = (MessagesPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                case "ns":
                    page = (StaffNumberPage) field.get(result.getInstance());
                    saveScreenshotPNG(page.getWrappedDriver());
                    break;
                default:
                    throw new AssertionError("Добавьте страницу в свитч для скриншотов");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void attachBrowserConsoleLog(ITestResult result) {
        if (result.getInstance().getClass().getName().toLowerCase().contains("api") ||
                result.getInstance().getClass().getName().toLowerCase().contains("database")) {
            return;
        }
        Class<?> c = result.getTestClass().getRealClass();
        Field[] fields = c.getDeclaredFields();
        String name = Arrays.stream(fields)
                .filter(field -> field.getType().isInterface()
                        && field.getType().getName().contains("pages")
                        && (field.getName().length() == 2 || field.getName().length() == 3)
                )
                .map(Field::getName)
                .findAny()
                .orElseThrow(() -> new AssertionError("Имя текущего интерфейса не совпадает с определенным в методе"));
        try {
            Field field = c.getDeclaredField(name);
            field.setAccessible(true);
            WebPage page;
            switch (name) {
                case "sb":
                    page = (ScheduleBoardPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "bp":
                    page = (BioPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "au":
                    page = (AddOrgUnitPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "ap":
                    page = (AnalyticsPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "bc":
                    page = (BatchCalculationPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "ps":
                    page = (PersonalSchedulePage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "os":
                    page = (OrgStructurePage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "ts":
                    page = (TasksPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "pp":
                    page = (MathParametersPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "rp":
                    page = (ReportsPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "bt":
                    page = (TerminalPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "lp":
                    page = (LoginPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "pt":
                    page = (PositionTypesPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "sl":
                    page = (SystemListsPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "fvp":
                    page = (FteOperationValuesPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "rm":
                    page = (RolesPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "sp":
                    page = (SupportPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "ssp":
                    page = (SystemSettingsPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "msp":
                    page = (MessagesPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                case "ns":
                    page = (StaffNumberPage) field.get(result.getInstance());
                    saveBrowserConsoleLogsTXT(page.getWrappedDriver());
                    break;
                default:
                    throw new AssertionError("Добавьте страницу в свитч для логов");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
