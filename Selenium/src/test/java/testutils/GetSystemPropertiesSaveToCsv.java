package testutils;

import org.json.JSONObject;
import org.testng.annotations.Test;
import utils.Projects;
import utils.tools.Pairs;
import wfm.models.OrgUnit;
import wfm.models.SystemProperty;
import wfm.repository.CommonRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.Links.ORGANIZATION_UNITS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static wfm.repository.SystemPropertyRepository.getSystemProperties;

/**
 * @author Evgeny Gurkin 26.08.2020
 */
public class GetSystemPropertiesSaveToCsv {

    @Test(groups = "Сохранить описание системных настроек в файл")
    private void getPropertiesToCSV() throws IOException {
        List<SystemProperty> systemProperties = getSystemProperties();
        String fileName = "src/test/resources/systemProp.csv";
        File file = new File(fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
        for (SystemProperty systemProperty : systemProperties) {
            String key = systemProperty.getKey();
            String title = systemProperty.getTitle();
            String description = systemProperty.getDescription();
            writer.append(key).append(";").append(title).append(";").append(description);
            writer.append('\n');
        }
        writer.close();
    }

    @Test(groups = "Конвертировать названия оргюнитов в айди и сохранить в файл")
    private void getOrgUnitsToCSV() throws IOException {
        List<String> result;
        try (Stream<String> lines = Files.lines(Paths.get("C:\\Jmeter\\apache-jmeter-4.0\\bin\\org_units_for_shifts_calc.csv"))) {
            result = lines.collect(Collectors.toList());
        }

        String fileName = "src/test/resources/org_ids_for_shift_calc.csv";
        File file = new File(fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));

        for (int i = 0; i < 5; i++) {
            List<OrgUnit> orgUnits = orgUnits(i);
            for (OrgUnit systemProperty : orgUnits) {
                if (result.contains(systemProperty.getName())) {
                    int key = systemProperty.getId();
                    writer.append(String.valueOf(key));
                    writer.append('\n');
                }
            }
        }
        writer.close();
    }

    private List<OrgUnit> orgUnits(int page) {
        Pairs.Builder pairs = Pairs.newBuilder()
                .withChildren(false)
                .page(page)
                .size(10000);
        JSONObject jsonFromUri = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNITS, pairs.build());
        return getListFromJsonObject(jsonFromUri, OrgUnit.class);
    }

}
