import xml.etree.ElementTree as ET
from urllib.parse import unquote
import re

file = 'Просмотр расписания' # название файла в текущей папке, без расширения
tree = ET.parse(f"{file}.jmx")
root = tree.getroot()

for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):

    meta_body = sampler_proxy.find('./boolProp[@name="HTTPSampler.postBodyRaw"]')

    if meta_body is not None:
        arg_x = sampler_proxy.find('.//stringProp[@name="Argument.value"]')
        arg = arg_x.text
        x = meta_body.text

        if not re.match(r"\s*{.*}\s*", arg):
            sampler_proxy.find('./boolProp[@name="HTTPSampler.postBodyRaw"]').text = "false"
            sampler_proxy.find('.//stringProp[@name="Argument.value"]').text = ""

            path = sampler_proxy.get('testname')
            parts = path.split('?')
            print(parts)
            if len(parts) == 2:
                params = parts[1].split('&')

                arguments_dict = {}

                for param in params:
                    if '=' in param:
                        name, value = param.split('=')
                    else:
                        name = param
                        value = None

                    if name not in arguments_dict:
                        arguments_dict[name] = [value]
                    else:
                        arguments_dict[name].append(value)

                arguments = sampler_proxy.find('.//collectionProp[@name="Arguments.arguments"]')

                for argument in arguments.findall('.//elementProp[@name="Argument"]'):
                    argument_name = argument.find('./stringProp[@name="Argument.name"]').text
                    arguments.remove(argument)
                if True:

                    for name, values in arguments_dict.items():

                       element_prop = ET.Element('elementProp', {'name': 'Argument', 'elementType': 'HTTPArgument'})
                       name_prop = ET.Element('stringProp', {'name': 'Argument.name'})
                       name_prop.text = name
                       meta_prop = ET.Element('stringProp', {'name': 'Argument.metadata'})
                       meta_prop.text = "="
                       element_prop.append(name_prop)
                       element_prop.append(meta_prop)

                       for value in values:
                           value_prop = ET.Element('stringProp', {'name': 'Argument.value'})
                           if value is not None:
                               value_prop.text = value
                               value_prop.text = unquote(value_prop.text.replace('^',''))
                           element_prop.append(value_prop)
                       arguments.append(element_prop)
                    path = parts[0]
                    sampler_proxy.find('./stringProp[@name="HTTPSampler.path"]').text = path

        tree.write(f"{file}_NNN.jmx", encoding='UTF-8', xml_declaration=True)
