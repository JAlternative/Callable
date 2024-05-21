import xml.etree.ElementTree as ET
from urllib.parse import unquote
import re

file = 'TC_главная_страница_emp' # название файла в текущей папке, без расширения
tree = ET.parse(f"{file}.jmx")
root = tree.getroot()

for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):

    meta_body = sampler_proxy.find('./boolProp[@name="HTTPSampler.postBodyRaw"]')

    if meta_body is not None:
        arg_x = sampler_proxy.find('.//stringProp[@name="Argument.value"]')
        arg = arg_x.text
        x = meta_body.text

        if re.match(r"\w+\{.*}", arg):

            sampler_proxy.find('.//stringProp[@name="Argument.value"]').text = "{}"

            path = sampler_proxy.get('testname')
            parts = path.split('?')
            print(parts)
            if len(parts) == 2:
                part = "?" + parts[1]
                sampler_proxy.find('./stringProp[@name="HTTPSampler.path"]').text = sampler_proxy.find('./stringProp[@name="HTTPSampler.path"]').text+part
                sampler_proxy.find('./stringProp[@name="HTTPSampler.path"]').text = unquote(sampler_proxy.find('./stringProp[@name="HTTPSampler.path"]').text)

        tree.write(f"{file}_no.jmx", encoding='UTF-8', xml_declaration=True)
