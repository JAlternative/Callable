package wfm.components.utils;


import utils.tools.Pairs;

import static utils.tools.LocalDateTools.now;


public enum SorterOptions {
    EMP_FIRED(Pairs.newBuilder().closeBeforeDate(now())),
    EMP_WITHOUT_POS(Pairs.newBuilder().withUnattached(true)),
    EMP_WORKING(Pairs.newBuilder().includeDate(now())),
    WITH_CHILDREN_REQUEST(Pairs.newBuilder().withChildren(true)),
    EMP_BY_ORG_UNIT(WITH_CHILDREN_REQUEST.getSorter()), //проверить был пустой ORG_UNIT_IDS
    EMP_PLANNED(Pairs.newBuilder().openAfterDate(now())),
    EMP_BY_POS(Pairs.newBuilder()),  // проверить был пустой POSITION_NAMES
    EMP_ALL(Pairs.newBuilder().openAfterDate(now()).includeDate(now()).withUnattached(true).closeBeforeDate(now())),
    EMP_WITHOUT(Pairs.newBuilder()),  //Проверить был пустой лист
    EMP_DISCHARGE(Pairs.newBuilder().includeDate(now()).withUnattached(true)),
    OM_DISCHARGE(Pairs.newBuilder().openAfterDate(now()).includeDate(now())),
    OM_BY_TAG(Pairs.newBuilder()), // проверить был пустой TAG_IDS
    OM_BY_ID(WITH_CHILDREN_REQUEST.getSorter()), //проверить был пустой ORG_UNIT_IDS
    OM_BY_TYPE_ID(Pairs.newBuilder()), //проверить был пустой ORG_UNIT_TYPE_IDS
    OM_ACTIVE(Pairs.newBuilder().includeDate(now())),
    OM_CLOSED(Pairs.newBuilder().closeBeforeDate(now())),
    OM_WITHOUT(Pairs.newBuilder()),
    OM_NEW(Pairs.newBuilder().openAfterDate(now())),
    EMP_BY_NAME(Pairs.newBuilder()), //проверить был пустой FULL_NAME
    OM_BY_NAME(Pairs.newBuilder()), //проверить был пустой NAME
    OM_ALL(Pairs.newBuilder().includeDate(now()).openAfterDate(now()).closeBeforeDate(now()));

    private final Pairs.Builder sorter;


    SorterOptions(Pairs.Builder sorter) {
        this.sorter = sorter;
    }


    public Pairs.Builder getSorter() {
        return sorter;
    }
}
