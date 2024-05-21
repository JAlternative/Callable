package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.SupportPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.GoToPageSection;
import utils.Links;
import utils.Projects;
import utils.authorization.ClientReturners;
import wfm.components.utils.PopUpText;
import wfm.components.utils.Section;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.Links.MAIL_TO_SUPPORT;
import static utils.Links.NOTIFICATIONS;
import static utils.tools.RequestFormers.makePath;
import static utils.tools.RequestFormers.setUri;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class Support {
    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final Section SECTION = Section.SUPPORT;
    private static final String URL_SP = RELEASE_URL + SECTION.getUrlEnding();

    @Inject
    private SupportPage sp;

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        sp.getWrappedDriver().quit();
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        sp.getWrappedDriver().manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Step("Перейти в раздел \"Служба поддержки\"")
    private void goToSupportPage() {
        new GoToPageSection(sp).getPage(SECTION, 60);
    }

    @Step("Ввести текст в поле с подсказкой \"Опишите проблему в двух словах\"")
    private void sendTextInProblemTitle(String text) {
        sp.supportPanel().inputProblemTitle().sendKeys(text);
    }

    @Step("Ввести текст в поле с подсказкой \"Расскажите подробнее о проблеме\"")
    private void sendTextInProblemDescription(String text) {
        sp.supportPanel().inputProblemDescription().sendKeys(text);
    }

    @Step("Нажать на кнопку \"Отправить\"")
    private void pressSendButton() {
        sp.supportPanel().sendButton().click();
    }

    @Step("Отправить файлы в количестве {numberOfFiles} шт. и проверить что они успешно отправлены")
    private void uploadFileGetResponse(String title, String body, int numberOfFiles) {
        File testUploadFile = new File("src/test/resources/text.txt");
        String urlEnding = makePath(NOTIFICATIONS, MAIL_TO_SUPPORT);
        URI uri = setUri(Projects.WFM, URL_SP, urlEnding);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        for (int i = 0; i < numberOfFiles; i++) {
            builder.addBinaryBody("files[" + i + "]", testUploadFile);
            builder.addTextBody("title", title);
            builder.addTextBody("body", body);
        }
        HttpEntity postData = builder.setContentType(ContentType.MULTIPART_FORM_DATA).build();
        HttpUriRequest postRequest = RequestBuilder
                .post()
                .setUri(uri)
                .setEntity(postData)
                .build();
        HttpResponse response = null;
        try {
            response = ClientReturners.httpClientReturner(Projects.WFM).execute(postRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(response != null ? response.getStatusLine().getStatusCode() : 0, 200,
                            "Не удалось загрузить файлы");
        Allure.addAttachment("Выгрузка файла", "Файл был успешно добавлен");
    }

    @Step("Проверка того что отобразился поп-ап с текстом \"{popUpText.text}\"")
    private void popUpAssert(PopUpText popUpText) {
        String expectedText = popUpText.getText();
        sp.popupPanel().waitUntil("Поп-ап не отобразился", DisplayedMatcher.displayed(), 5);
        sp.popupPanel().popupText().should("На поп-апе должен отображаться текст: " + expectedText + ", но отобразился: "
                                                   + sp.popupPanel().popupText().getText(), text(containsString(expectedText)), 5);
    }

    @Test(groups = {"TEST-1079", "TK2587-1"}, description = "Отправка сообщения в службу поддержки")
    private void sendMessageInSupport() {
        goToSupportPage();
        String title = RandomStringUtils.randomAlphabetic(10);
        sendTextInProblemTitle(title);
        String problem = RandomStringUtils.randomAlphabetic(100);
        sendTextInProblemDescription(problem);
        pressSendButton();
        popUpAssert(PopUpText.MESSAGE_SEND);
    }

    @Test(groups = {"TEST-1079", "TK2587-2"}, description = "Отправка сообщения в службу поддержки без указания темы")
    private void sendMessageInSupportWithoutTitle() {
        goToSupportPage();
        String problem = RandomStringUtils.randomAlphabetic(100);
        sendTextInProblemDescription(problem);
        pressSendButton();
        popUpAssert(PopUpText.MUST_SPECIFY_TITLE_AND_BODY);
    }

    @Test(groups = {"TEST-1079", "TK2587-3"}, description = "Отправка сообщения в службу поддержки без текста обращения")
    private void sendMessageInSupportWithoutProblem() {
        goToSupportPage();
        String title = RandomStringUtils.randomAlphabetic(10);
        sendTextInProblemTitle(title);
        pressSendButton();
        popUpAssert(PopUpText.MUST_SPECIFY_TITLE_AND_BODY);
    }

    @Test(groups = {"TEST-1079", "TK2587-4"}, description = "Отправка сообщения в службу поддержки с прикрепленным файлом")
    private void sendMessageInSupportWithFile() {
        String title = RandomStringUtils.randomAlphabetic(10);
        String problem = RandomStringUtils.randomAlphabetic(100);
        uploadFileGetResponse(title, problem, 1);
    }

    @Test(groups = {"TEST-1079", "TK2587-5"}, description = "Отправка сообщения в службу поддержки с несколькими прикрепленными файлами")
    private void sendMessageInSupportWithMultiplyFile() {
        String title = RandomStringUtils.randomAlphabetic(10);
        String problem = RandomStringUtils.randomAlphabetic(100);
        uploadFileGetResponse(title, problem, new Random().nextInt(5) + 2);
    }
}
