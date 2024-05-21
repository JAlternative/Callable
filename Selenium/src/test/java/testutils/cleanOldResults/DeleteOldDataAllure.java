package testutils.cleanOldResults;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;
import utils.models.AllureResultModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Сделан для очистки файлов отчета от всякого мусора и вообще чтобы нормально начали работать тренды в отчетах
 */
public class DeleteOldDataAllure {

    private final String ALLURE_RESULT_PATH = "build/allure-results";

    @Test(groups = {"clean"})
    private void deleteOldResultsAndAttachments() {
        List<AllureResultModel> allureResultModelList = readFilesInModel(ALLURE_RESULT_PATH);
        deleteOldResults(allureResultModelList, ALLURE_RESULT_PATH);
        String upDirectoryResult = getAbsoluteDirectoryPath() + "allure-results";
        List<AllureResultModel> allureResultModelListUP = readFilesInModel(upDirectoryResult);
        deleteOldResults(allureResultModelListUP, upDirectoryResult);
        copyHistory();
    }

    /**
     * Берет все файлы указанного типа из build/allure-results
     *
     * @param resultFileType - тип файла
     * @return список путей к файлам
     */
    private List<Path> getAllFilesByType(ResultFileType resultFileType, String path) {
        List<Path> filesInFolder = new ArrayList<>();
        try {
            filesInFolder = Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().contains(resultFileType.getResultType()))
                    .collect(Collectors.toList());
        } catch (NoSuchFileException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filesInFolder;
    }

    /**
     * По указанном пути возвращает JSON объект
     */
    public static JSONObject parseJSONFile(Path path) {
        String content = null;
        try {
            content = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (content != null && !content.equals("")) {
            return new JSONObject(content);
        } else {
            return null;
        }
    }

    /**
     * Ищет по резалтам ссылки на аттачменты и удаляет старые
     *
     * @param thisDayResults - резалты сегодняшнего дня
     */
    private void deleteOldAttachments(List<AllureResultModel> thisDayResults, String pathFolder) {
        List<String> thisDayAttachments = thisDayResults.stream()
                .map(AllureResultModel::getAttachments)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        List<String> allAttachments = getAllFilesByType(ResultFileType.ATTACHMENT, pathFolder).stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
        allAttachments.removeAll(thisDayAttachments);
        allAttachments.forEach(name -> new File(pathFolder + "/" + name).delete());
    }

    /**
     * Ищет в контейнерах чилдренов и удаляет контейнеры если ни один из чилдренов не соответсветствует айди резалта текущего дня
     *
     * @param thisDayResults - резалты сегодняшнего дня
     */
    private void deleteOldContainers(List<AllureResultModel> thisDayResults, String pathFolder) {
        List<String> thisDayID = thisDayResults.stream().map(AllureResultModel::getUuid).collect(Collectors.toList());
        List<Path> pathList = getAllFilesByType(ResultFileType.CONTAINER, pathFolder);
        for (Path path : pathList) {
            JSONObject jsonObject = parseJSONFile(path);
            if (jsonObject == null) {
                deleteFilesFromPath(path);
                continue;
            }
            JSONArray childrenArray = jsonObject.getJSONArray("children");
            List<String> childrenIDs = new ArrayList<>();
            for (int i = 0; i < childrenArray.length(); i++) {
                childrenIDs.add(childrenArray.getString(i));
            }
            if (Collections.disjoint(childrenIDs, thisDayID)) {
                deleteFilesFromPath(path);
            }
        }
    }

    /**
     * Удаляет все старые ресалты, контейнеры и аттачменты
     *
     * @param allureResultModelList - список utils.models.AllureResultModel объектов
     */
    private void deleteOldResults(List<AllureResultModel> allureResultModelList, String pathFolder) {
        long nowTime = new Date().getTime();
        //взял 18 часов чтобы более точно исключить именно вчерашние запуски
        long hoursInMilliseconds = TimeUnit.MILLISECONDS.convert(5, TimeUnit.HOURS);
        long timeBeforeStart = nowTime - hoursInMilliseconds;
        List<AllureResultModel> thisDayResults = allureResultModelList.stream()
                .filter(allureResultModel -> allureResultModel.getStart() > timeBeforeStart)
                .collect(Collectors.toList());
        //удаляем из списка резалтов все резалты сегодняшнего дня, а затем удаляем все оставшиеся старые файлы
        allureResultModelList.removeAll(thisDayResults);
        allureResultModelList.stream().map(AllureResultModel::getPath).forEach(this::deleteFilesFromPath);
        deleteOldAttachments(thisDayResults, pathFolder);
        deleteOldContainers(thisDayResults, pathFolder);
    }

    /**
     * Читает файлы по найденным путям до резалтов и возвращает список резалт моделей
     */
    private List<AllureResultModel> readFilesInModel(String pathFolder) {
        List<Path> pathList = getAllFilesByType(ResultFileType.RESULT, pathFolder);
        List<AllureResultModel> allureResultModels = new ArrayList<>();
        for (Path path : pathList) {
            JSONObject jsonObject = parseJSONFile(path);
            if (jsonObject == null) {
                deleteFilesFromPath(path);
                continue;
            }
            allureResultModels.add(new AllureResultModel(jsonObject, path));
        }
        return allureResultModels;
    }

    private List<JSONObject> readFilesJson(List<Path> pathList) {
        List<JSONObject> list = new ArrayList<>();
        for (Path path : pathList) {
            JSONObject jsonObject = parseJSONFile(path);
            if (jsonObject == null) {
                deleteFilesFromPath(path);
                continue;
            }
            list.add(jsonObject);
        }
        return list;
    }

    /**
     * Просто удаляет файлик по указанному пути
     */
    private void deleteFilesFromPath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Заставляет работать тренды и историю в  аллюр отчете
     */
    private void copyHistory() {
        String absolutePath = getAbsoluteDirectoryPath();

        File sourceDir = new File(absolutePath + "allure-report/history"); //директория с историей
        File destDir = new File(ALLURE_RESULT_PATH + "/history"); //новая директория куда мы ее перетащим
        File oneMoreDestDir = new File(absolutePath + "allure-results/history");//и еще в директорию в корне на всякий

        if (sourceDir.listFiles() != null) {
            destDir.mkdirs();
            oneMoreDestDir.mkdirs();
            try {
                FileUtils.cleanDirectory(destDir);
                FileUtils.cleanDirectory(oneMoreDestDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Path destPath = destDir.toPath();
            Path oneMoreDestPath = oneMoreDestDir.toPath();

            if (sourceDir.listFiles() != null) {
                for (File sourceFile : sourceDir.listFiles()) {
                    Path sourcePath = sourceFile.toPath();
                    try {
                        Files.copy(sourcePath, destPath.resolve(sourcePath.getFileName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //в одном трайкаче у начинались проблемы с копированием во вторую папку, либо не копировало, либо копировало не всё
                    try {
                        Files.copy(sourcePath, oneMoreDestPath.resolve(sourcePath.getFileName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //очищаем старую папку истории из корневой директории
            try {
                FileUtils.cleanDirectory(sourceDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * метод нужен для получения абсолютного пути до директории, в том случае когда нам надо забраться куда то повыше
     * чем директория
     */
    private String getAbsoluteDirectoryPath() {
        File file = new File(ALLURE_RESULT_PATH);
        String absolute = file.getAbsolutePath();
        return absolute.substring(0, absolute.indexOf("Selenium"));
    }

}
