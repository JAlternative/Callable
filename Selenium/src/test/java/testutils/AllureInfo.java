package testutils;

import com.github.automatedowl.tools.AllureEnvironmentWriter;
import com.google.common.collect.ImmutableMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import utils.BuildInfo;
import wfm.PresetClass;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static utils.Params.KEY;
import static utils.Params.VALUE;

/**
 * Данный класс позволяет записывать информацию в блок "Окружение" отчета Allure.
 * Ранее он использовался для фиксации версии стенда и даты последнего обновления, но т.к. джоба теперь может проходить на нескольких стендах,
 * эту информацию надо фиксировать для всех стендов, что на данный момент делается прямо в описании теста.
 * Возможно доработать этот класс, чтобы он читал содержимое файла /build/allure-results/environment.xml и дополнял его данными для всех стендов,
 * но на данный момент это не приоритетная задача.
 */
public class AllureInfo {

    private AllureInfo() {
    }

    public static BuildInfo setAllureEnvironmentInformation() {
        BuildInfo info = PresetClass.getBuildInfo();
        String path = System.getProperty("user.dir") + "/build/allure-results/";
        if (info == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                .withZone(ZoneId.systemDefault())
                .withLocale(new Locale("ru"));
        HashMap<String, String> map = new HashMap<>();
        map.put(info.getShortName() + " - последнее обновление", formatter.format(info.getCreated()));
        map.put(info.getShortName() + " - версия", info.getVersion());
        Document doc;
        try {
            doc = new SAXBuilder().build(path + "environment.xml");
        } catch (JDOMException | IOException e) {
            AllureEnvironmentWriter.allureEnvironmentWriter(ImmutableMap.<String, String>builder().putAll(map).build(), path);
            return info;
        }
        List<Element> parameters = doc.getRootElement().getChildren("parameter");
        for (Element parameter : parameters) {
            map.put(parameter.getChildText(KEY), parameter.getChildText(VALUE));
        }
        AllureEnvironmentWriter.allureEnvironmentWriter(ImmutableMap.<String, String>builder().putAll(map).build(), path);
        return info;
    }
}
