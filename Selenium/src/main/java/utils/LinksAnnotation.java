package utils;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import utils.deserialization.JSONObjectDeserializer;
import utils.serialization.JSONObjectSerializer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static utils.Params.LINKS;

@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonProperty(LINKS)
@JsonSerialize(using = JSONObjectSerializer.class)
@JsonDeserialize(using = JSONObjectDeserializer.class)

public @interface LinksAnnotation {}
