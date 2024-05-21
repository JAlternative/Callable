package utils.downloading;

import org.apache.http.client.utils.URIBuilder;
import utils.Links;
import utils.Projects;
import utils.tools.Pairs;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.Role;
import wfm.models.DateInterval;
import wfm.models.OrgUnit;
import wfm.models.SystemProperty;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.Params.DATE;
import static utils.tools.RequestFormers.makePath;
import static wfm.repository.SystemPropertyRepository.getSystemProperties;
import static wfm.repository.SystemPropertyRepository.getSystemPropertyFromList;

public class FileDownloadCheckerForReport extends FileDownloadChecker {

    private final TypeOfReports typeOfReports;
    private final List<OrgUnit> listOfOm;
    private LocalDate dateForReport;
    private static final Projects project = Projects.WFM;
    private DateInterval dateInterval;


    /**
     * Инициализирует экземпляр подкласса и супрекласса для ReportsClass
     *
     * @param role             - роль
     * @param typeOfFiles      - тип скачиваемого файла
     * @param listOmId         - список оргюнитов для отчета
     * @param typeOfReportFile - тип отчета
     */
    public FileDownloadCheckerForReport(Role role, TypeOfReports typeOfReportFile,
                                        List<OrgUnit> listOmId, TypeOfFiles typeOfFiles) {
        super(project, role, typeOfFiles);
        this.typeOfReports = typeOfReportFile;
        this.listOfOm = listOmId;
    }

    /**
     * Делает тоже самое только с учетом даты/месяца формирования отчета
     *
     * @param dateForReport- дата отчета
     */
    public FileDownloadCheckerForReport(Role role, TypeOfReports typeOfReportFile,
                                        List<OrgUnit> listOmId, LocalDate dateForReport, TypeOfFiles typeOfFiles) {
        super(project, role, typeOfFiles);
        this.typeOfReports = typeOfReportFile;
        this.listOfOm = listOmId;
        this.dateForReport = dateForReport;
    }

    /**
     * Делает тоже самое только с учетом временного интервала для взятия отчета
     *
     * @param dateInterval- интервал дат для взятия отчета
     */
    public FileDownloadCheckerForReport(Role role, List<OrgUnit> listOfOm,
                                        DateInterval dateInterval, TypeOfFiles typeOfFiles, TypeOfReports typeOfReports) {
        super(project, role, typeOfFiles);
        this.typeOfReports = typeOfReports;
        this.listOfOm = listOfOm;
        this.dateInterval = dateInterval;
    }

    private List<OrgUnit> getListOfOm() {
        return listOfOm;
    }

    private TypeOfReports getTypeOfReports() {
        return typeOfReports;
    }

    private LocalDate getDateForReport() {
        return dateForReport;
    }

    private String omListToString() {
        return getListOfOm().stream().map(OrgUnit::getId).map(Object::toString).collect(Collectors.joining(", "));
    }

    public DateInterval getDates() {
        return dateInterval;
    }

    public String getFileName() {
        return fileNameBuilder();
    }

    /**
     * Метод для построения ожидаемого названия скачанного файла
     *
     * @return - имя файла с нижними подчеркиваниями
     */
    private String fileNameBuilder() {
        String fileName = getTypeOfReports().getFilePrefix();
        switch (getTypeOfReports()) {
            case SHIFTS:
            case TIME_SHEET:
                fileName = underscoreMaker(fileName, dateForReport.with(TemporalAdjusters.firstDayOfMonth()).toString(),
                        dateForReport.with(TemporalAdjusters.lastDayOfMonth()).toString());
                break;
            case TARGET_NUMBER:
                fileName = underscoreMaker(fileName, dateInterval.getStartDate().toString(), dateInterval.getEndDate().toString());
                break;
            case EMPLOYEE_WORKING_FACT:
                fileName = underscoreMaker(dateInterval.getStartDate().toString(), dateInterval.getEndDate().toString());
                break;
            default:
                break;
        }
        switch (getTypeOfFiles()) {
            case CSV_GRID:
                fileName = underscoreMaker(fileName, "grid");
                break;
            case ZIP:
                fileName = underscoreMaker(fileName, "xlsx");
                break;
        }
        return fileName;
    }

    /**
     * Формирует URI скачивания файла для ReportsClass, используя метод определенный в интерфейсе
     */
    public URIBuilder downloadUrlFormer() {
        URI uri = getUri();
        String path = "";
        Pairs.DownloadBuilder pairs = Pairs.newDownloadBuilder();
        switch (getTypeOfReports()) {
            case PRINTED_FORMS:
                path = "/schedule-board/pdf";
                pairs.orgUnitIds(omListToString()).date(getDateForReport()).zip(true);
                break;
            case SHIFTS:
                path = makePath(API_V1, REPORTS, SHIFTS_CSV);
                pairs.from(dateForReport.with(TemporalAdjusters.firstDayOfMonth()))
                        .to(dateForReport.with(TemporalAdjusters.lastDayOfMonth()))
                        .orgUnitIdsSelf(omListToString())
                        .orgUnitIds(omListToString())
                        .format(getTypeOfFiles().getFileFormat());
                break;
            case NUMBER_OF_GRAPHS:
                //тут редирект на другое окно, нужно считывать инфу из формы
                path = "/report-employees-calculated/date/" + getDateForReport();
                break;
            case QUALITY_HISTORICAL_DATA:
                path = makePath(API_V1, EXPORT_PREFIX, KPI_STATISTICS);
                pairs.format("csv")
                        .orgUnitIdsSelf(omListToString())
                        .orgUnitIdsChildren("");
                break;
            case HOLIDAY_QUOTAS:
                path = makePath("/report-quotas", ORG_UNITS, omListToString(), DATE, getDateForReport());
                break;
            case ATTENDANCE:
                path = makePath(API_V1, EXPORT_PREFIX, "attendance");
                pairs.from(dateInterval.getStartDate())
                        .to(dateInterval.getEndDate())
                        .orgUnitIdsSelf(omListToString())
                        .orgUnitIdsChildren("");
                break;
            case AVERAGE_CONVERSION:
                path = makePath("/report-conversion-average/", ORG_UNITS, omListToString());
                break;
            case PLAN_FACT_CONVERSION:
                path = makePath("/report-conversion-planned/", ORG_UNITS, omListToString());
                break;
            case NUMBER_OF_STAFF:
                path = makePath("/report-employees-number", ORG_UNITS, omListToString(), DATE, getDateForReport());
                break;
            case TIME_SHEET:
                path = makePath(API_V1, EXPORT_PREFIX, WORKED_SHIFTS_CSV);
                pairs.from(dateForReport.with(TemporalAdjusters.firstDayOfMonth()))
                        .to(dateForReport.with(TemporalAdjusters.lastDayOfMonth()))
                        .format(this.getTypeOfFiles().getFileFormat())
                        .orgUnitIdsSelf("")
                        .orgUnitIdsChildren(omListToString());
                break;
            case VALUES_OF_PARAMETERS:
                path = "/report-math-parameter-values/" + omListToString();
                break;
            case TARGET_NUMBER:
                path = makePath(API_V1, EXPORT_PREFIX, "stuff-and-operations");
                pairs.from(dateInterval.getStartDate())
                        .to(dateInterval.getEndDate())
                        .orgUnitIds(getListOfOm().get(0).getId());
                break;
            case EMPLOYEE_WORKING_FACT:
                path = makePath(API_V1, EXPORT_PREFIX, WORK_TIME);
                pairs.from(dateInterval.getStartDate())
                        .to(dateInterval.getEndDate())
                        .orgUnitIdsSelf(String.valueOf(getListOfOm().get(0).getId()))
                        .orgUnitIdsChildren("");
                break;
            case DATA_FOR_CALCULATION:
                path = makePath(API_V1, ORG_UNITS, listOfOm.get(0), "kpi-diagnostics");
                pairs.from(dateInterval.getStartDate())
                        .to(dateInterval.getEndDate())
                        .useMathParam(false)
                        .strategy("B")
                        .altAlgorithm(0);
                break;
            case SHIFTS_EXTERNAL_EMPLOYEE:
                path = makePath(API_V1, EXPORT_PREFIX, OUTSTAFF);
                pairs.from(dateInterval.getStartDate())
                        .to(dateInterval.getEndDate())
                        .orgUnitIdsSelf(omListToString())
                        .orgUnitIdsChildren("");
                break;
            case TECHNICAL_TABLE_UNLOADING:
                List<SystemProperty> systemProperties = getSystemProperties();
                SystemProperty jasperServer = getSystemPropertyFromList(SystemProperties.JASPER_SERVER_URL, systemProperties);
                SystemProperty userName = getSystemPropertyFromList(SystemProperties.JASPER_USERNAME, systemProperties);
                SystemProperty password = getSystemPropertyFromList(SystemProperties.JASPER_PASSWORD, systemProperties);
                uri = URI.create(jasperServer.getValue().toString());
                path = uri.getPath() + "/rest_v2/reports/reports/Lukoil_T13_forImport.xlsx"
                        + listOfOm.get(0) + "/kpi-diagnostics";
                pairs.jUsername(userName.getValue().toString())
                        .jPassword(password.getValue())
                        .departmentId(listOfOm.get(0).getId())
                        .startDate(dateForReport.with(TemporalAdjusters.firstDayOfMonth()))
                        .endDate(dateForReport.with(TemporalAdjusters.lastDayOfMonth()));
                break;

        }
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.setPath(path);
        uriBuilder.setParameters(pairs.build());
        return uriBuilder;
    }

}


