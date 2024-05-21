package utils.downloading;

import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import utils.Projects;
import wfm.components.utils.Role;

public interface DownloadBehaviors {
    HttpResponse downloadResponse(Role role, TypeOfAcceptContent certainAcceptContent);

    URIBuilder downloadUrlFormer();

    BasicCookieStore getBasicCookieStore(Role role, Projects projects);
}
