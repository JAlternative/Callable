package wfm.repository;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.models.DateInterval;
import wfm.models.EmployeePosition;
import wfm.models.Position;
import wfm.models.JobTitle;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.Params.EMBEDDED;
import static utils.Params.LINKS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class PositionRepository {

    private  PositionRepository() {}

    /**
     * Возвращает список позиций для указанного типа должности и даты начала работы
     *
     * @param jobTitle - вариант должности
     * @param date     - дата начала работы
     * @param id       - айди для оргюнита
     * @return - лист позиций
     */
    public static List<Position> emptyPositionReturner(JobTitle jobTitle, LocalDate date, int id) {
        String tempJob = jobTitle.getFullName();
        List<Position> positionList = getPositionsArray(id);
        return positionList.stream()
                .filter(pos -> pos.getName().contains(tempJob)
                        && pos.getDateInterval().startDate.isEqual(date)
                        && pos.getDateInterval().endDate == null)
                .collect(Collectors.toList());
    }

    /**
     * Берет массив должностей
     *
     * @return должности в виде массива JSON
     */
    public static List<Position> getPositionsArray(int id) {
        String urlEnding = makePath(ORGANIZATION_UNITS, id, POSITIONS);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .includeChief(true)
                .includeOld(true)
                .size(100000)
                .build();
        JSONObject someObjectPositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        return getListFromJsonObject(someObjectPositions, Position.class);
    }

    /**
     * Возвращает позицию начальника подразделения по айди
     *
     * @return объект класса Position
     */
    public static Position getChief(int id) {
        int chiefId = OrgUnitRepository.getOrgUnit(id).getChiefPosition();
        if (chiefId == -1) {
            return null;
        } else {
            String urlEnding = makePath(POSITIONS, chiefId);
            return new Position(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding));
        }
    }

    /**
     * Смотрит даты должностей и возвращает если дата окончания должности равна сегодняшней
     *
     * @return хешма из айди должнности и названия должности
     */
    public static List<Position> checkApiPositionsDate(int id) {
        List<Position> positionList = getPositionsArray(id);
        return positionList.stream().filter(position -> position.getDateInterval().endDate.isEqual(LocalDateTools.now()))
                .collect(Collectors.toList());
    }

    /**
     * Просматривает список позиций в указанном интервале и возвращает свободные позиции
     *
     * @param positions    - список позиций
     * @param dateInterval - интервал в котором просматривается свободны они или нет
     */
    public static List<Position> getFreePositions(List<Position> positions, DateInterval dateInterval) {
        List<Position> freePositions = new ArrayList<>();
        positions.removeIf(position -> !position.getDateInterval().includesDate(LocalDate.now()));
        for (Position position : positions) {
            JSONObject object = getJsonFromUri(Projects.WFM, URI.create(position.getLink(REL_EMPLOYEE_POSITIONS).split("\\{")[0]));
            if (object.isNull(EMBEDDED) && !object.isNull(LINKS)) {
                freePositions.add(position);
                continue;
            }
            List<EmployeePosition> employeePositions = getListFromJsonObject(object, EmployeePosition.class);
            boolean hasNoActiveEmployeePosition = employeePositions.stream()
                    .noneMatch(employeePosition -> employeePosition.getDateInterval().hasIntersection(dateInterval));
            boolean hasActivePosition = employeePositions.stream()
                    .noneMatch(employeePosition1 -> employeePosition1.getPosition().getDateInterval().hasIntersection(dateInterval));
            if (hasNoActiveEmployeePosition && hasActivePosition) {
                freePositions.add(position);
            }
        }
        return freePositions;
    }

    /**
     * Возвращает актуальные свободные позиции из списка позиций
     *
     * @param positions - список позиций
     */
    public static List<Position> getFreePositions(List<Position> positions) {
        return getFreePositions(positions, new DateInterval(LocalDate.now().plusDays(1)));
    }

    public static ImmutablePair<Position, Integer> getRandomPosition(List<Position> sorted, List<Position> all) {
        Position random = getRandomFromList(sorted);
        List<Position> positionsWithRole = all.stream()
                .filter(position -> position.getName().equals(random.getName()))
                .collect(Collectors.toList());
        return new ImmutablePair<>(random, positionsWithRole.indexOf(random));
    }
}
