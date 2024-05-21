package utils.downloading;

import org.apache.http.client.utils.URIBuilder;
import utils.Projects;
import utils.tools.Pairs;
import wfm.components.utils.Role;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static utils.Params.BIO_API;
import static utils.tools.RequestFormers.makePath;

public class FileDownloadCheckerForBio extends FileDownloadChecker {
    private final TypeOfPhotos typeOfPhotos;
    private String personId;
    private static final Projects project = Projects.BIO;

    /**
     * Инициализирует экземпляр подкласса и супрекласса для BioControl
     *
     * @param role         - роль
     * @param typeOfPhotos - тип скачиваемого отчета
     * @param typeOfFiles  - тип скачиваемого файла
     */
    public FileDownloadCheckerForBio(Role role, TypeOfFiles typeOfFiles, TypeOfPhotos typeOfPhotos) {
        super(project, role, typeOfFiles);
        this.typeOfPhotos = typeOfPhotos;
    }

    /**
     * Делает тоже самое только с учетом ID пользователя у которого будем скачивать
     *
     * @param personId - айди пользователя
     */
    public FileDownloadCheckerForBio(Role role, TypeOfFiles typeOfFiles, String personId, TypeOfPhotos typeOfPhotos) {
        super(project, role, typeOfFiles);
        this.personId = personId;
        this.typeOfPhotos = typeOfPhotos;
    }

    /**
     * Формирует URI скачивания файла для BioControl, используя метод определенный в интерфейсе
     */
    public URIBuilder downloadUrlFormer() {
        URI uri = getUri();
        URIBuilder uriBuilder = new URIBuilder(uri);
        //убираем порт, так как при скачивании порта нет
        uriBuilder.setPort(-1);
        String path = makePath(BIO_API, typeOfPhotos.getType(), getTypeOfFiles().getFileFormat());
        Pairs.DownloadBuilder pairs = Pairs.newDownloadBuilder();
        if (typeOfPhotos == TypeOfPhotos.FACE_DESCRIPTORS) {
            pairs.personIds(personId);
        }
        if (typeOfPhotos == TypeOfPhotos.JOURNAL) {
            pairs.size(10000)
                    .from(getDate())
                    .photoUrl(true);
        }
        uriBuilder.setPath(path);
        uriBuilder.setParameters(pairs.build());
        return uriBuilder;
    }

    /**
     * Метод для правильного взятия даты и передачи ее в параметрах ссылки на скачивание
     */
    public String getDate() {
        LocalDate date = LocalDate.now().minusMonths(1).minusDays(1);
        LocalTime time = LocalTime.of(19, 0, 0, 0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnn'Z'");
        return LocalDateTime.of(date, time).format(formatter);
    }
}
