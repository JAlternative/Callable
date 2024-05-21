from model.Gradle import Gradle
from model.Xml import Xml
from utils import file_reader


def get_xml(task: Gradle):
    from_file = file_reader.get_xml_lines(task.path)
    return Xml(from_file, task.file)
