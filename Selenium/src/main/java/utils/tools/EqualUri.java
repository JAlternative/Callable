package utils.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.*;

public class EqualUri {

    private static final Logger LOG = LoggerFactory.getLogger(EqualUri.class);

    /**
     * Сравнивает 2 URI на предмет их идентичности, в том числе по параметрам, даже если параметры обозначены в разном порядке
     */
    public static boolean areEquals(URI url1, URI url2) {
        //сравниваем части URL
        if (!url1.getScheme().equals(url2.getScheme()) ||
                !url1.getAuthority().equals(url2.getAuthority()) ||
                url1.getPort() != url2.getPort() ||
                !url1.getHost().equals(url2.getHost())) {
            return false;
        }
        //берем квери параметры
        String params1 = url1.getQuery();
        String params2 = url2.getQuery();
        LOG.info("params1: {} params2: {}", params1, params2);
        if ((params1 != null && params2 != null) && (params1.length() == params2.length())) {
            //берем параметры и сортируем
            Map<String, List<String>> list1 = extractParameters(params1);
            Map<String, List<String>> list2 = extractParameters(params2);
            //после того как параметры отсортированы мы можем их сравнить
            return list1.equals(list2);
        }
        //если параметры все же нулл то все ок, они равны, а если нет то значит у них была разная длина и они не равны
        return params1 == null && params2 == null;
    }

    /**
     * Достает параметры из URL и сортирует их, так чтобы их потом можно было сравнить при разной очередности этих параметров,
     * например, бывают случаи перечисления по типу: &id=1,2,3,4 vs &id=1,4,3,2
     * с точки зрения апи и здравого смысла это одинаковый набор параметров, но дефолтный equals считает что нет.
     *
     * @return мапу где ключи это название параметра, а значения это отсортированный список значений
     */
    private static Map<String, List<String>> extractParameters(String paramsString) {
        String[] parameters = paramsString.split("&");
        Map<String, List<String>> query = new HashMap<>();
        for (String s : parameters) {
            String[] keyParamValues = s.split("=");
            String values = keyParamValues[1];
            List<String> keyList = Arrays.asList(values.split(","));
            Collections.sort(keyList);
            query.put(keyParamValues[0], keyList);
        }
        return query;
    }
}
