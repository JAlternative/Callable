import xml.etree.ElementTree as ET

file = 'example' # название файла в текущей папке, без расширения
tree = ET.parse(f"{file}.jmx")
root = tree.getroot()

http_samplers = root.findall('.//HTTPSamplerProxy')

for timers in root.findall('.//UniformRandomTimer'):

    timers.set('enabled',"false")

    print(timers)



tree.write(f"{file}_TDS.jmx", encoding='UTF-8', xml_declaration=True)