package commons;

import io.gatling.javaapi.core.OpenInjectionStep;

import static commons.Utils.propertyReturner;
import static io.gatling.javaapi.core.CoreDsl.*;

public class PopulationBuilders {
    private PopulationBuilders() {

    }

    private static final int USERS = Integer.parseInt(propertyReturner("simultaneous_users"));

    public static final OpenInjectionStep INCREASE_BY_STEPS = incrementUsersPerSec(USERS)
            .times(5)
            .eachLevelLasting(10)
            .separatedByRampsLasting(10)
            .startingFrom(10);

    public static final OpenInjectionStep RAMP = rampUsers(USERS).during(30);
    public static final OpenInjectionStep HOLD = constantUsersPerSec(USERS).during(30);

    public static final OpenInjectionStep INJECT_ONCE = atOnceUsers(USERS);

}
