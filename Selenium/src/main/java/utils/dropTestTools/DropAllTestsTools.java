package utils.dropTestTools;

import org.testng.ITestResult;
import org.testng.SkipException;

import static utils.dropTestTools.DropLoginTestReadWriter.getStatusLoginTest;
import static utils.dropTestTools.DropLoginTestReadWriter.writeBrokenLoginTest;

/**
 * Инструмент был создан затем что если падает логин тест то, все остальные тест в джобе автоматически роняются.
 * До следующего запуска логин теста
 */
public class DropAllTestsTools {

    private static final String methodName = "loginTest";

    private DropAllTestsTools() {
        throw new IllegalStateException("Utility class");
    }


    /**
     * Читает название теста, если это не логин тест и в файлике имеется запись о том что логин тест упал скипает тест
     */
    public static void checkLoginTestDrop(ITestResult result) {
        String tempNameTest = result.getName();
        if (!tempNameTest.equals(methodName) && getStatusLoginTest()) {
            throw new SkipException("Логин тест не прошел, поэтому этот тест не выполняется");
        }
    }

    /**
     * Проверяет сообщение об ошибке упавшего теста, если упал логин тест то делает запись в файлик
     */
    public static void checkLoginTestFail(ITestResult result) {
        String tempNameTest = result.getName();
        if (tempNameTest.equals(methodName)) {
            writeBrokenLoginTest();
        }
    }
}
