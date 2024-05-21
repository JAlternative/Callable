package wfm.models;

import org.json.JSONObject;
import utils.Projects;
import wfm.HasLinks;

import java.net.URI;

import static utils.Params.*;
import static utils.tools.RequestFormers.setUrlAndInitiateForApi;

public class MathParameterValue <T> implements HasLinks {
    private T value;
    private Object template;
    private String type;
    private String name;
    private JSONObject links;

    public MathParameterValue(JSONObject json) {
        this.value = (T) json.opt(VALUE);
        this.template = json.opt("template");
        this.type = json.getString(TYPE);
        this.links = json.getJSONObject(LINKS);
        MathParameter param = new MathParameter(new JSONObject(setUrlAndInitiateForApi(URI.create(getLink(MATH_PARAMETER)), Projects.WFM)));
        this.name = param.getShortName();
    }

    public T getValue() {
        return value;
    }

    public MathParameterValue setValue(T value) {
        this.value = value;
        return this;
    }

    public Object getTemplate() {
        return template;
    }

    public MathParameterValue setTemplate(Object template) {
        this.template = template;
        return this;
    }

    public String getType() {
        return type;
    }

    public MathParameterValue setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public MathParameterValue setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public MathParameterValue setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, value);
    }
}
