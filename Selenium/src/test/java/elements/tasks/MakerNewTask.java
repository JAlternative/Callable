package elements.tasks;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface MakerNewTask extends AtlasWebElement {
    @FindBy("//button[contains(@class,'mdl-button mdl-button--fab mdl-js-ripple-effect mdl-button')]")
    AtlasWebElement createNewTaskButton();

    @FindBy("//input[@id='task-name']")
    AtlasWebElement taskNamField();

    @FindBy("//input[@id='task-short-description']")
    AtlasWebElement taskShortDiscriptionField();

    @FindBy("//textarea[@id='task-description']")
    AtlasWebElement taskFullDiscriptionField();

    @FindBy("//input[@id='task-tags']")
    AtlasWebElement TagField();

    @FindBy("//div[@class='mdl-list__item']//div[@class='mdl-list__item-secondary-action']")
    AtlasWebElement AllDayOnOffSwitch();

    @FindBy("//input[@id='task-date']")
    AtlasWebElement taskDate();

    @FindBy("//div[@class='mdl-list__text-field mdl-list__text-field--33 mdl-textfield mdl-textfield--floating-label au-target mdl-js-textfield is-upgraded']//button[@class='mdl-button mdl-button--in-text-field mdl-button--icon au-target']")
    AtlasWebElement taskStartTimeButton();

    @FindBy("//div[@class='mdl-list__text-field mdl-textfield mdl-list__text-field--33 mdl-textfield--floating-label au-target mdl-js-textfield is-upgraded']//button[@class='mdl-button mdl-button--in-text-field mdl-button--icon au-target']")
    AtlasWebElement taskEndTimeButton();

    @FindBy("//input[@id='task-periodicity']")
    AtlasWebElement taskPeriod();

    @FindBy("//div[@class='menu au-target is-visible']/div")
    List<AtlasWebElement> listOfTaskPeriod();

    @FindBy("//input[@id='task-reminder']")
    AtlasWebElement taskRemindButton();

    @FindBy("//div[@class='menu au-target is-visible']/div")
    List<AtlasWebElement> variantsOfRemind();

    @FindBy("//input[@id='task-executor-type']")
    AtlasWebElement executorType();

    @FindBy("//div[@class='menu au-target is-visible']/div")
    List<AtlasWebElement> variantsOfExecutorTypes();

    @FindBy("//div[@class='mdl-list__item au-target']//label[@class='mdl-switch mdl-js-ripple-effect au-target mdl-js-switch mdl-js-ripple-effect--ignore-events is-upgraded']")
    AtlasWebElement oneOrManyExecutors();

    //кнопка чтобы нажать первую строку для модуля и сотрудника
    @FindBy("//input[@value.bind='executor.assignee.name']")
    List<AtlasWebElement> manyExecutorsOrModuleButtons();

    @FindBy("//input[@value.bind='executor.assignee.name']")
    AtlasWebElement oneExecutorOrModuleButton();

    //
    @FindBy("//input[@value.bind='place.name']")
    AtlasWebElement onePlaceExecutorButton();

    //в первых строках каждого исполнителя выпадает список
    @FindBy("//div[@class='menu menu--shadow-16dp au-target menu--bottom is-visible']//div[@class='au-target']/div")
    List<AtlasWebElement> listOfAllExecutors();

    //для сотрудников ЕЩЕ  есть поле места

    @FindBy("//input[@value.bind='place.name']")
    List<AtlasWebElement> placesForWorker();

    @FindBy("//div[@class='menu menu--shadow-16dp au-target menu--bottom is-visible']//div[@class='au-target']/div")
    List<AtlasWebElement> listOfAllPlaces();

    @FindBy("//div[@class='mdl-list']//div[16]//div[1]/button[2]")
    AtlasWebElement saveButtonForTask();

    @FindBy("//div[@class='mdl-list']//div[16]//div[1]/button[1]")
    AtlasWebElement cancelButtonForTask();

    @FindBy("//div[@class='mdl-list__item mdl-list--no-margin-top mdl-list--no-margin-bottom']")
    AtlasWebElement justClickOnItToRunFromAnyList();

    @FindBy("//input[@type='file']")
    AtlasWebElement uploadFile();

    @FindBy("//button[@class='mdl-button mdl-button--icon au-target mdl-js-button']//i[@class='mdi mdi-close']")
    AtlasWebElement cancelMakingTask();

    @FindBy("//div[@class='menu menu--shadow-16dp au-target menu--bottom is-visible']/div[1]/input")
    AtlasWebElement searchForEmployee();

    @FindBy("//div/div[contains(@class.bind,'executor._errors.executor.')]/button")
    List<AtlasWebElement> deleteEmployeeOrModule();

    @FindBy("//div/div[contains(@class.bind,'executor._errors.places')]/button")
    List<AtlasWebElement> deletePlace();

}
