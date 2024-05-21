package bio.repository;

import bio.models.FaceDescriptors;
import bio.models.Person;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import utils.Projects;
import utils.tools.Pairs;

import java.net.URI;
import java.util.List;

import static bio.repository.CommonBioRepository.BIO_URL;
import static utils.Links.FACE_DESCRIPTORS;
import static utils.tools.CustomTools.getListFromJsonArray;
import static utils.tools.RequestFormers.setUri;
import static utils.tools.RequestFormers.setUrlAndInitiateForApi;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class FaceDescriptorsRepository {

    /**
     * Берет и возвращает список дескрипторов распознавания для одного человека
     */
    public static List<FaceDescriptors> getDescriptors(Person person) {
        List<NameValuePair> pairs = Pairs.newBioBuilder().photoUrl(true).personId(person.getId()).build();
        URI uri = setUri(Projects.BIO, BIO_URL, FACE_DESCRIPTORS, pairs);
        JSONArray temp = new JSONArray(setUrlAndInitiateForApi(uri, Projects.BIO));
        return getListFromJsonArray(temp, FaceDescriptors.class);
    }
}
