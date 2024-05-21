package utils.downloading;

import org.apache.http.client.utils.URIBuilder;
import utils.Projects;
import wfm.components.utils.Role;

import java.net.URI;

import static utils.Links.BATCH;
import static utils.tools.RequestFormers.makePath;

public class FileDownloadCheckerForBatchCalculation extends FileDownloadChecker {

    private final TypeOfBatch typeOfBatch;
    private static final Projects project = Projects.WFM;


    /**
     * Инициализирует экземпляр подкласса и супрекласса для BatchCalculation
     *
     * @param role        - роль
     * @param typeOfBatch - тип скачиваемого отчета
     */
    public FileDownloadCheckerForBatchCalculation(Role role, TypeOfBatch typeOfBatch) {
        super(project, role, TypeOfFiles.JSON);
        this.typeOfBatch = typeOfBatch;
    }

    /**
     * Формирует URI скачивания файла для BatchCalculation, используя метод определенный в интерфейсе
     */
    public URIBuilder downloadUrlFormer() {
        URI uri = getUri();
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.setPath(makePath(BATCH, typeOfBatch.getName()));
        return uriBuilder;
    }

}
