package wfm.repository;

import com.mchange.util.AssertException;
import org.json.JSONArray;
import utils.Projects;
import wfm.components.orgstructure.ConstraintViolations;
import wfm.models.ConstraintViolationTypes;

import java.net.URI;
import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.*;
import static utils.tools.CustomTools.getListFromJsonArray;
import static utils.tools.RequestFormers.getJsonArrayFromUri;
import static utils.tools.RequestFormers.makePath;

public class ConstraintViolationTypesRepository {

     /**
      * Проверить, что переданный тип содержится в полном списке конфликтов
      */
     public static ConstraintViolationTypes getConstraintViolationByType(ConstraintViolations violation) {
          List<ConstraintViolationTypes> constrViolationList = getConstraintViolationTypes();
          return constrViolationList.stream()
              .filter(constr -> constr.getValue().equals(violation.toString()))
              .findFirst()
              .orElseThrow(() -> new AssertException(String.format(NO_TEST_DATA + "Конфликт с указанным типом %s не найден в полном списке", violation.toString())));
     }

     /**
      * Возвращает все доступные настройки у оргюнита с апи
      */
     public static List<ConstraintViolationTypes> getConstraintViolationTypes() {
          String link = makePath(CommonRepository.URL_BASE, API_V1, SEARCH, CONSTRAINT_VIOLATION_TYPES);
          JSONArray settings = getJsonArrayFromUri(Projects.WFM, URI.create(link));
          return getListFromJsonArray(settings, ConstraintViolationTypes.class);
     }
}