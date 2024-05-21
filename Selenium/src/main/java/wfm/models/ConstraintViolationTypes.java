package wfm.models;

import org.json.JSONObject;

import static utils.Params.*;

public class ConstraintViolationTypes {

     private final String description;
     private final String name;
     private final String value;

     public ConstraintViolationTypes(JSONObject jsonObject) {
          this.description = jsonObject.getString(DESCRIPTION);
          this.name = jsonObject.getString(NAME);
          this.value = jsonObject.getString(VALUE);
     }

     public String getDescription() {
          return description;
     }

     public String getName() {
          return name;
     }

     public String getValue() {
          return value;
     }
}