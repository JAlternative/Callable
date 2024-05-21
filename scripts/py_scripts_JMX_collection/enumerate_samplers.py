import xml.etree.ElementTree as ET

file = 'Просмотр расписания' # название файла в текущей папке, без расширения
tree = ET.parse(f"{file}.jmx")
root = tree.getroot()


http_samplers = root.findall('.//HTTPSamplerProxy')
for i, sampler in enumerate(http_samplers):
    current_name = sampler.get('testname')
    enumerate_name = f"{current_name}_{i}"
    sampler.set('testname', enumerate_name)
tree.write(f"{file}_num.jmx", encoding='UTF-8', xml_declaration=True)