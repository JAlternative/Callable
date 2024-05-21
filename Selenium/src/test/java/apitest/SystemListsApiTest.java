package apitest;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import reporting.TestListener;
import testutils.BaseTest;
import wfm.PresetClass;
import wfm.components.systemlists.LimitType;
import wfm.models.JobTitle;
import wfm.models.Limits;
import wfm.models.OrgUnit;
import wfm.models.PositionGroup;
import wfm.repository.JobTitleRepository;
import wfm.repository.OrgUnitRepository;
import wfm.repository.PositionGroupRepository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static utils.tools.CustomTools.getRandomFromList;

@Listeners({TestListener.class})
public class SystemListsApiTest extends BaseTest {
    @DataProvider(name = "limits")
    private Object[][] limits() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{
                Collections.singletonList(new Limits(LimitType.ADD_WORK, false, false, false, 10, null)),
                new Limits(LimitType.GENERAL, false, false, false, 9, null), true};
        array[1] = new Object[]{
                Collections.singletonList(new Limits(LimitType.ADD_WORK, false, false, true, 10, null)),
                new Limits(LimitType.GENERAL, false, false, false, 9, null), true};
        array[2] = new Object[]{
                Collections.singletonList(new Limits(LimitType.ADD_WORK, false, true, true, 10, null)),
                new Limits(LimitType.GENERAL, false, false, false, 9, null), true};
        return array;
    }

    @Test(groups = "@After remove limits", dataProvider = "limits")
    private void createLimit(List<Limits> presetLimitBodies, Limits limitToCreate, boolean error) {
        createAllPresetLimits(presetLimitBodies);
        if (error) {
            PresetClass.createLimit(limitToCreate, 500,
                    "The created limit must be greater than or equal to the \\w+ limit.");
        } else {
            PresetClass.createLimit(limitToCreate);
        }
    }

    private void createAllPresetLimits(List<Limits> presetLimitBodies) {
        Limits generalLimitBody = new Limits().setLimitType(LimitType.GENERAL.toString())
                .setLimit(250).setOrgType("СВП");
        Limits generalLimit = PresetClass.createLimit(generalLimitBody);
        createPresetLimits(presetLimitBodies);
        PresetClass.removeLimit(generalLimit);
    }

    private void createPresetLimits(List<Limits> presetLimitBodies) {
        JobTitle jobTitle = null;
        PositionGroup positionGroup = null;
        OrgUnit orgUnit = null;
        for (Limits l : presetLimitBodies) {
            jobTitle = (JobTitle) addOptionalParam(l.isHasJobTitle(), jobTitle, l, JobTitle.class);
            orgUnit = (OrgUnit) addOptionalParam(l.isHasOrgUnit(), orgUnit, l, OrgUnit.class);
            positionGroup = (PositionGroup) addOptionalParam(l.isHasPositionGroup(), positionGroup, l, PositionGroup.class);
            PresetClass.createLimit(l);
        }
    }

    private Object addOptionalParam(boolean var, Object commonObject, Limits l, Class cl) {
        if (var) {
            if (commonObject == null) {
                if (cl.equals(JobTitle.class)) commonObject = getRandomFromList(JobTitleRepository.getAllJobTitles());
                if (cl.equals(PositionGroup.class)) commonObject = getRandomFromList(PositionGroupRepository.getAllPositionGroups());
                if (cl.equals(OrgUnit.class)) commonObject = OrgUnitRepository.getRandomAvailableOrgUnit();
            }
            Method method;
            try {
                method = l.getClass().getDeclaredMethod("set" + cl.getSimpleName(), cl);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            try {
                method.invoke(l, commonObject);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return commonObject;
    }
}
