package bio.repository;

import bio.models.Terminal;
import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;

import java.util.List;

import static bio.repository.CommonBioRepository.BIO_URL;
import static utils.Links.TERMINALS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class TerminalRepository {

    /**
     * Берет все терминалы
     */
    public static List<Terminal> getTerminals() {
        List<NameValuePair> pairs = Pairs.newBioBuilder().size(100000).build();
        JSONObject someEmployeePositions = getJsonFromUri(Projects.BIO, BIO_URL, TERMINALS, pairs);
        return getListFromJsonObject(someEmployeePositions, Terminal.class);
    }

    /**
     * Берет терминал по его айди
     */
    public static Terminal getTerminalById(String terminalId) {
        String urlEnding = makePath(TERMINALS, terminalId);
        JSONObject temp = getJsonFromUri(Projects.BIO, BIO_URL, urlEnding);
        return new Terminal(temp);
    }
}
