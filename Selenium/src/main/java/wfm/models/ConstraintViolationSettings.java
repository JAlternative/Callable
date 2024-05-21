package wfm.models;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import wfm.HasLinks;
import wfm.components.orgstructure.ConstraintViolationLevel;

import java.net.URI;

import static utils.Params.*;
import static utils.tools.RequestFormers.getJsonFromUri;

public class ConstraintViolationSettings implements HasLinks {

     private final int orgUnitId;
     private final String level;
     private final String type;
     private final boolean applicableForWorkedRoster;
     private final JSONObject links;

     public ConstraintViolationSettings(JSONObject jsonObject) {
          this.links = jsonObject.getJSONObject(LINKS);
          this.orgUnitId = jsonObject.optInt(ORG_UNIT_ID);
          this.level = jsonObject.getString(LEVEL);
          this.type = jsonObject.getString(TYPE);
          this.applicableForWorkedRoster = jsonObject.optBoolean("applicableForWorkedRoster");
     }

     public String getType() {
          return type;
     }

     public ConstraintViolationLevel getLevel() {
          return ConstraintViolationLevel.valueOf(level);
     }

     @Override
     public JSONObject getLinks() {
          return links;
     }

     public int getId() {
          return orgUnitId;
     }
     public ConstraintViolationSettings refresh() {
          URI uri = URI.create(this.getSelfLink());
          return CustomTools.getClassObjectFromJson(ConstraintViolationSettings.class, getJsonFromUri(Projects.WFM, uri));
     }
}
