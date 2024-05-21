package guice;

import com.google.inject.AbstractModule;
import guice.pagesproviders.*;
import org.openqa.selenium.WebDriver;
import pages.*;

public class TestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(WebDriver.class).toProvider(WebDriverProvider.class);
        bind(AddOrgUnitPage.class).toProvider(AddOrgUnitProvider.class);
        bind(AnalyticsPage.class).toProvider(AnalyticsIntProvider.class);
        bind(BatchCalculationPage.class).toProvider(BatchCalculationProvider.class);
        bind(LoginPage.class).toProvider(CommonModuleProvider.class);
        bind(MathParametersPage.class).toProvider(MathParametersProvider.class);
        bind(PersonalSchedulePage.class).toProvider(PersonalScheduleProvider.class);
        bind(ReportsPage.class).toProvider(ReportsProvider.class);
        bind(ScheduleBoardPage.class).toProvider(ScheduleBoardProvider.class);
        bind(TasksPage.class).toProvider(TasksIntProvider.class);
        bind(OrgStructurePage.class).toProvider(OrgStructurePageProvider.class);
        bind(BioPage.class).toProvider(BioControlModuleProvider.class);
        bind(TerminalPage.class).toProvider(TerminalModuleProvider.class);
        bind(PositionTypesPage.class).toProvider(PositionTypesProvider.class);
        bind(SystemListsPage.class).toProvider(SystemListsProvider.class);
        bind(FteOperationValuesPage.class).toProvider(FteOperationValuesProvider.class);
        bind(RolesPage.class).toProvider(RolesManageProvider.class);
        bind(SupportPage.class).toProvider(SupportProvider.class);
        bind(SystemSettingsPage.class).toProvider(SystemSettingProvider.class);
        bind(MessagesPage.class).toProvider(MessagesProvider.class);
        bind(StaffNumberPage.class).toProvider(StaffNumberProvider.class);
        bind(StaffNumberPage.class).toProvider(StaffNumberProvider.class);
    }

}
