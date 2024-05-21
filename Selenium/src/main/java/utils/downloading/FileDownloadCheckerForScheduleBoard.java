package utils.downloading;

import org.apache.http.client.utils.URIBuilder;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.Role;
import wfm.models.SystemProperty;
import wfm.repository.CommonRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static utils.Links.*;
import static utils.tools.RequestFormers.makePath;
import static wfm.repository.CommonRepository.URL_BASE;
import static wfm.repository.SystemPropertyRepository.getSystemProperties;
import static wfm.repository.SystemPropertyRepository.getSystemPropertyFromList;

public class FileDownloadCheckerForScheduleBoard extends FileDownloadChecker {

    private int omId;
    private String positionsIds;
    private String employeesIds;
    private String employeePositionsIds;
    private String rosterId;
    private static final Projects project = Projects.WFM;
    private TypeOfReports typeOfReports;

    /**
     * Инициализирует экземпляр подкласса и супрекласса для ScheduleBoard
     *
     * @param role          - роль
     * @param typeOfFiles   - тип файла
     * @param typeOfReports - тип отчета
     * @param omId          - айди оргюнита
     * @param positionsIds  - айди сотрудников
     */
    public FileDownloadCheckerForScheduleBoard(Role role, TypeOfFiles typeOfFiles, TypeOfReports typeOfReports, int omId, String positionsIds) {
        super(project, role, typeOfFiles);
        this.omId = omId;
        this.positionsIds = positionsIds;
        this.typeOfReports = typeOfReports;
    }

    /**
     * Инициализирует экземпляр подкласса и супрекласса для ScheduleBoard
     *
     * @param role        - роль
     * @param typeOfFiles - тип файла
     */
    public FileDownloadCheckerForScheduleBoard(Role role, int omId, TypeOfFiles typeOfFiles, TypeOfReports typeOfReports) {
        super(project, role, typeOfFiles);
        this.typeOfReports = typeOfReports;
        this.omId = omId;
    }

    /**
     * Инициализирует экземпляр подкласса и супрекласса для ScheduleBoard
     *
     * @param role          - роль
     * @param typeOfFiles   - тип файла
     * @param typeOfReports - тип отчета
     * @param omId          - айди оргюнита
     * @param rosterId      - айди ростера
     * @param employeesIds  - айди сотрудников
     */
    public FileDownloadCheckerForScheduleBoard(Role role, int omId, TypeOfFiles typeOfFiles, TypeOfReports typeOfReports,
                                               String rosterId, String employeesIds, String employeePositionsIds) {
        super(project, role, typeOfFiles);
        this.typeOfReports = typeOfReports;
        this.omId = omId;
        this.rosterId = rosterId;
        this.employeesIds = employeesIds;
        this.employeePositionsIds = employeePositionsIds;
    }

    /**
     * Инициализирует экземпляр подкласса и супрекласса для ScheduleBoard
     *
     * @param role        - роль
     * @param typeOfFiles - тип файла
     * @param rosterId    - айди ростера
     */
    public FileDownloadCheckerForScheduleBoard(Role role, TypeOfFiles typeOfFiles, String rosterId) {
        super(project, role, typeOfFiles);
        this.rosterId = rosterId;
    }

    private int getOmId() {
        return omId;
    }

    private String getPositionsIDs() {
        return positionsIds;
    }

    private String getRosterId() {
        return rosterId;
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
        String fileName = SHIFTS;
        if (getTypeOfFiles() == TypeOfFiles.XLSX) {
            fileName = underscoreMaker(fileName, LocalDateTools.getFirstDate().toString(), LocalDateTools.getLastDate().toString());
        } else if (getTypeOfFiles() == TypeOfFiles.ONE_C) {
            fileName = underscoreMaker(String.valueOf(LocalDateTools.now().getYear()), String.valueOf(LocalDateTools.now().getMonthValue()),
                                       "SML_2");
        }
        return fileName;
    }

    /**
     * Формирует URI скачивания файла для ScheduleBoard, используя метод определенный в интерфейсе
     */
    public URIBuilder downloadUrlFormer() {
        URI uri = getUri();
        String path = "";
        Pairs.DownloadBuilder pairs = Pairs.newDownloadBuilder();
        if (typeOfReports == null) {
            path = getSimplePath(path, pairs);
        } else {
            switch (typeOfReports) {
                case PLANNED_GRAPH:
                    List<SystemProperty> systemProperties = getSystemProperties();
                    SystemProperty jasperServer = getSystemPropertyFromList(SystemProperties.JASPER_SERVER_URL, systemProperties);
                    SystemProperty userName = getSystemPropertyFromList(SystemProperties.JASPER_USERNAME, systemProperties);
                    SystemProperty password = getSystemPropertyFromList(SystemProperties.JASPER_PASSWORD, systemProperties);
                    uri = URI.create(jasperServer.getValue().toString());
                    path = uri.getPath() + "/rest_v2/reports/reports/RosterPrintForm_main_1." + getTypeOfFiles().getFileExtension();
                    if (URL_BASE.contains("magnit")) {
                        pairs.jUsername(userName.getValue().toString())
                                .jPassword(password.getValue())
                                .departmentId(omId)
                                .startDate(LocalDateTools.getFirstDate())
                                .endDate(LocalDateTools.getLastDate())
                                .employeePositionIds(employeePositionsIds);
                    } else {
                        pairs.jUsername(userName.getValue().toString())
                                .jPassword(password.getValue())
                                .departmentId(omId)
                                .rosterId(rosterId)
                                .startDate(LocalDateTools.getFirstDate())
                                .endDate(LocalDateTools.getLastDate())
                                .employeeIds(employeesIds)
                                .employeePositionIds(employeePositionsIds)
                                .excludeEmployeesFromOtherOrganizations(true);
                    }
                    break;
                case NORMALIZED_SHIFTS:
                    path = "/schedule-board/pdf";
                    pairs.orgUnitIds(getOmId())
                            .employeePositionIds(getPositionsIDs())
                            .date(LocalDate.now())
                            .week(false)
                            .planOnly(false)
                            .normalized(true);
                    break;
                case UPLOAD_1C:
                    path = "/export/schedule-dbf";
                    pairs.organizationUnitId(getOmId())
                            .year(LocalDateTools.now().getYear())
                            .month(LocalDateTools.now().getMonthValue());
                    break;
                case PRINT_SCHEDULE_WITH_COMMENT:
                    path = "/schedule-board/pdf";
                    pairs.orgUnitIds(getOmId())
                            .employeePositionIds(getPositionsIDs())
                            .date(LocalDate.now())
                            .week(false)
                            .planOnly(true)
                            .normalized(false);
                    break;
                default:
                    path = getSimplePath(path, pairs);
                    break;
            }
        }
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.setPath(path);
        uriBuilder.setParameters(pairs.build());
        return uriBuilder;
    }

    /**
     * Метод для построения простого типа отчета
     */
    private String getSimplePath(String path, Pairs.DownloadBuilder pairs) {
        switch (this.getTypeOfFiles()) {
            case PDF:
                path = "/schedule-board/pdf";
                pairs.orgUnitIds(getOmId())
                        .employeePositionIds(getPositionsIDs())
                        .date(LocalDate.now())
                        .week(false)
                        .planOnly(false)
                        .normalized(false);
                break;
            case PDF_ONLY_SCHEDULE:
                path = "/schedule-board/pdf";
                pairs.orgUnitIds(getOmId())
                        .date(LocalDate.now())
                        .week(false)
                        .planOnly(true)
                        .positionTypeIds(getPositionsIDs());
                break;
            case XLSX:
                String exportPrefix = REPORTS;
                path = makePath(API_V1, exportPrefix, SCHEDULE_CSV);
                pairs.from(LocalDateTools.getFirstDate())
                        .to(LocalDateTools.getLastDate())
                        .roster_Id(getRosterId())
                        .format("xlsx");
                break;
        }
        return path;
    }
}
