package bio.repository;

import bio.components.terminal.CheckBoxAndStatus;
import bio.models.Journal;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Projects;
import utils.tools.Pairs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static bio.repository.CommonBioRepository.BIO_URL;
import static utils.Links.JOURNAL;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class JournalRepository {

    private static final Logger LOG = LoggerFactory.getLogger(JournalRepository.class);

    /**
     * Берет все журналы за указанный диапазон даты, с учетом того какие статусы журнала нам нужны
     *
     * @param startDate     - дата начала поиска записей журнала
     * @param endDate       - дата окончания поиска записей журнала
     * @param allMarked     - какие статусы журнала нам нужны
     * @param personGroupId - оргюнит для которого ищем журнал
     */
    public static List<Journal> getJournals(LocalDate startDate, LocalDate endDate, List<CheckBoxAndStatus> allMarked,
                                            String personGroupId) {
        LocalDateTime startDateTime = startDate.atTime(19, 0, 0, 0).minusDays(1);
        LocalDateTime endDateTime = endDate.atTime(18, 59, 59, 999);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnn'Z'");
        Pairs.BioBuilder pairs = Pairs.newBioBuilder()
                .size(100000)
                .from(startDateTime.format(formatter))
                .to(endDateTime.format(formatter))
                .photoUrl(true)
                .personGroups(personGroupId);
        if (!allMarked.contains(CheckBoxAndStatus.ALL)) {
            String statuses = allMarked.stream().map(CheckBoxAndStatus::getApiStatus).collect(Collectors.joining(","));
            pairs.purposes(statuses);
        }
        JSONObject temp = getJsonFromUri(Projects.BIO, BIO_URL, JOURNAL, pairs.build());
        List<Journal> listFromJsonObject = getListFromJsonObject(temp, Journal.class);
        LOG.info("Количество событий в журнале в интервале от {} до {} : {}",
                startDate.toString(), endDate.toString(), listFromJsonObject.size());
        return listFromJsonObject;
    }
}
