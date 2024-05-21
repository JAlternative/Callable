import re
import sys

import result_collector
import test_collector
from utils import downloader, file_handler, printer
from utils.list_intersections import find_intersections_and_omitted_tests


def main():
    if len(sys.argv) != 2:
        sys.exit("Отсутствует обязательный параметр. Использование скрипта:\n"
                 "python main.py (ссылка из дженкинса на скачивание архива с результатами регрессовго джоба)")
    link = sys.argv[1]
    output_file = downloader.download_results(link)
    job_name = re.findall('[\d_]+_(WFM_regress[\d-]+)', output_file)[0]
    tests_to_run = test_collector.collect(job_name)
    test_results = result_collector.collect()
    file_handler.clean_up(output_file)
    report_both, report_test_only, report_result_only = find_intersections_and_omitted_tests(tests_to_run, test_results)
    printer.to_csv([report_both, report_test_only, report_result_only], output_file)
    printer.to_json([report_both, report_test_only, report_result_only], output_file)
    printer.to_confluence_markup(f"output/{output_file}.csv")

main()
