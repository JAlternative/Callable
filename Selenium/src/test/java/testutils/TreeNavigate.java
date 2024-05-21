package testutils;

import io.qameta.allure.Allure;
import io.qameta.atlas.core.Atlas;
import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Locatable;
import pages.TreeBehavior;
import wfm.components.utils.Direction;
import wfm.repository.CommonRepository;

import java.util.*;
import java.util.stream.Collectors;

public class TreeNavigate {

    private final List<List<String>> path;

    public TreeNavigate(List<List<String>> path) {
        this.path = path;
    }

    private List<List<String>> getPath() {
        return path;
    }

    /**
     * Определяется нажимать на шеврон или на радиобатом для ОМ в дереве
     *
     * @param direction определяет направление обхода, в дальнейшем будут ли открывыться или наоборот закрываться шевроны
     * @return Отсортированную мапа по типу ИМЯ_ОМ - true/false
     * true - для нажатия на радиобатом
     * false - для нажатия на шеврон
     */
    private LinkedHashMap<String, Boolean> treeStatusFormer(Direction direction) {
        List<List<String>> flatList = new ArrayList<>();
        List<List<String>> listForWork = getPath();
        for (int i = 0; i < listForWork.size(); i++) {
            flatList.add(new LinkedList<>());
            switch (direction) {
                case DOWN:
                    for (int g = listForWork.get(i).size() - 1; g >= 0; g--) {
                        flatList.get(i).add(listForWork.get(i).get(g));
                    }
                    break;
                case UP:
                    for (int g = 0; g < listForWork.get(i).size(); g++) {
                        flatList.get(i).add(listForWork.get(i).get(g));
                    }
                    break;
            }
        }
        //Развернули весь список
        List<String> allNameFromTreeTravel = flatList.stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        //Определяем ОМ на которые нужно выставить чекбоксы
        List<String> lastOmForChecks = lastCheckedOmFormer();
        LinkedHashMap<String, Boolean> omStatusInTree = new LinkedHashMap<>();
        //Соотносим ОМ на которые нужно нажать в чекбокс или в шеврон
        for (String s : allNameFromTreeTravel) {
            omStatusInTree.putIfAbsent(s, lastOmForChecks.contains(s));
        }
        return omStatusInTree;
    }

    /**
     * Определяет на каких ОМ выставить radioButton
     *
     * @return список названий ОМ
     */
    private List<String> lastCheckedOmFormer() {
        //Определяем ОМ на которые нужно выставить чекбоксы
        List<String> lastOmForChecks = new ArrayList<>();
        List<List<String>> listForWork = getPath();
        for (List<String> strings : listForWork) {
            lastOmForChecks.add(strings.get(0));
        }
        return lastOmForChecks;
    }

    /**
     * Для отображения в аллюр отчете в какой последовательносты были нажатия на шевроны или радиобатомы.
     * Если элемент в переданном списке последний в проходе, ему присваивается клик на шеврон, иначе клик на чекбокс.
     *
     * @param forAttachmentShow - список ОМ в котором производились нажатия
     */
    private void allureReportLogic(LinkedList<String> forAttachmentShow) {
        List<String> lastOmForChecks = lastCheckedOmFormer();
        StringBuilder attachmentString = new StringBuilder();
        //Формирование атачмента для отчета, на основе выполненых кликов
        for (String temp : forAttachmentShow) {
            if (lastOmForChecks.contains(temp)) {
                attachmentString.append("Нажали на чекбокс ОМ: ").append(temp).append("\n");
            } else {
                attachmentString.append("Нажали на шеврон ОМ: ").append(temp).append("\n");
            }
        }
        Allure.addAttachment("Список нажатий в дереве", "text/plain", attachmentString.toString());
    }

    /**
     * При получении testutils.TreeNavigate#treeStatusFormer(testutils.Direction) - формируется статусы каждого оргюнита,
     * при значении статуса True - активируется чекбокс у оргюнита, в ином случае происходит раскрытие дальше
     * Как правило, последний оргюнит в списке имеет статус true - следовательно будет активирован в дереве
     *
     * @param treeBehaviour - Передается интерфейс из elements в котором должны быть переопределены chevronButton и CheckBoxButton
     * @param direction     - Передается направление обхода, UP - вверх, DOWN - вниз
     */
    public void workWithTree(TreeBehavior treeBehaviour, Direction direction) {
        boolean isMaster = CommonRepository.URL_BASE.contains("pochta-wfm-qa") || CommonRepository.URL_BASE.contains("magnit-master");
        LinkedHashMap<String, Boolean> omStatusInTree = treeStatusFormer(direction);
        Iterator iterator = omStatusInTree.keySet().iterator();
        LinkedList<String> forAttachmentShow = new LinkedList<>();
        //Перебираем все ОМ и для добавления в отчет запоминаем на что нажимали
        while (iterator.hasNext()) {
            String temp = iterator.next().toString();
            if (omStatusInTree.get(temp)) {
                int x = treeBehaviour.checkBoxButton(temp).getCoordinates().onPage().getX();
                int y = treeBehaviour.checkBoxButton(temp).getCoordinates().onPage().getY();
                ((Locatable) treeBehaviour).getCoordinates().onPage().move(x, y);
                treeBehaviour.checkBoxButton(temp).click();
                forAttachmentShow.add(temp);
            } else if (!omStatusInTree.get(temp)) {
                int x = treeBehaviour.chevronButton(temp).getCoordinates().onPage().getX();
                int y = treeBehaviour.chevronButton(temp).getCoordinates().onPage().getY();
                ((Locatable) treeBehaviour).getCoordinates().onPage().move(x, y);
                treeBehaviour.chevronButton(temp).click();
                forAttachmentShow.add(temp);
            }
        }
        allureReportLogic(forAttachmentShow);
    }
}