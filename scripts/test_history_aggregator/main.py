import re
import sys
from typing import List

from model.HistoryItem import HistoryItem
from utils import collector, downloader, file_handler, printer


def main():
    if len(sys.argv) != 2:
        sys.exit("Отсутствует обязательный параметр. Использование скрипта:\n"
                 "python main.py (ссылка из дженкинса на скачивание архива с результатами джоба)")
    link: str = sys.argv[1]
    output_file: str = downloader.download_results(link)
    job_name: str = re.findall('[\d_]+_(WFM_nightly_[MR]+)', output_file)[0]
    history_items: List[HistoryItem] = collector.collect_history_items()
    printer.to_csv(history_items, job_name)
    file_handler.clean_up(output_file)


main()
