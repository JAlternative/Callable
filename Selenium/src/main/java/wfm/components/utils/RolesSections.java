package wfm.components.utils;

public enum RolesSections {
    ANALYTICS(Section.ANALYTICS),
    MATH_PARAMETERS(Section.MATH_PARAMETERS),
    SCHEDULE_BOARD(Section.SCHEDULE_BOARD),
    BATCH_CALCULATION(Section.BATCH_CALCULATION),
    ORG_STRUCTURE(Section.ORG_STRUCTURE),
    POSITION_TYPES(Section.POSITION_TYPES),
    REPORTS(Section.REPORTS),
    FUNCTIONAL_ROLES(Section.POSITION_GROUPS),
    SUPPORT(Section.SUPPORT),
    INSTRUCTIONS(Section.INSTRUCTIONS),
    PROFILE(Section.PROFILE),
    PERSONAL_SCHEDULE_REQUESTS(Section.PERSONAL_SCHEDULE_REQUESTS)
    ;
    private final Section section;

    RolesSections(Section section) {
        this.section = section;
    }

    public Section getSection() {
        return section;
    }
}
