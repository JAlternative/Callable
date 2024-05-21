import xml.etree.ElementTree as ET

file = 'Просмотр расписания' # название файла в текущей папке, без расширения
tree = ET.parse(f"{file}.jmx")
root = tree.getroot()


http_samplers = root.findall('.//HTTPSamplerProxy')
get_methods = ["GET","OPTIONS","HEAD"]
for sampler in http_samplers:
    path_prop = sampler.find('stringProp[@name="HTTPSampler.path"]')
    method_prop = sampler.find('stringProp[@name="HTTPSampler.method"]')
    path_value = path_prop.text.strip()
    method_value = method_prop.text.strip()
    if '${' in path_value:
        path_value = path_value.replace('${', '{')
    if method_value in get_methods:
        path_value = f"<_{path_value}"
    else:
        path_value = f">_{path_value}"
    firts = path_value.split("?")
    sampler.set('testname', firts[0])

tree.write(f"{file}_rename.jmx", encoding='UTF-8', xml_declaration=True)