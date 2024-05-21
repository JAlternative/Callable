import json
import xml.etree.ElementTree as ET
from urllib.parse import unquote, unquote_plus
import re

file = 'Просмотр расписания' # название файла в текущей папке, без расширения
tree = ET.parse(f"{file}.jmx")
root = tree.getroot()
methods = ["POST", "DELETE", "PUT", "PATCH"]
for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):

    method = sampler_proxy.find('.//stringProp[@name="HTTPSampler.method"]')
    if method.text in methods:
       arguments = sampler_proxy.find('.//stringProp[@name="Argument.value"]')
       if arguments is not None:
          if arguments.text is not None:
             arguments_value = arguments.text
             if re.match(r"\{.*\}",arguments_value):
                body_prop = ET.Element('boolProp', {'name': 'HTTPSampler.postBodyRaw'})
                x = body_prop.text = 'true'
                sampler_proxy.append(body_prop)
                # sampler_proxy.append(body_prop)
                arguments = sampler_proxy.find('.//stringProp[@name="Argument.value"]')
                arguments_value = arguments.text
                arguments.text = arguments_value.replace('^\\^', '"').replace('^', '').replace('\\"','').replace('"[','')
                arguments_value = arguments.text
                arguments_value = unquote(arguments.text)

                try:
                   arguments_value = json.dumps(json.loads(arguments_value), indent=4, ensure_ascii=False)
                except:
                   print(f"invalid json {arguments_value}")
                arguments.text = arguments_value
             else:
                arguments_value = unquote(arguments.text)
                arguments.text = arguments_value

tree.write(f"{file}_BodyValid.jmx", encoding='utf-8', xml_declaration=True)

file = f"{file}_BodyValid.jmx" # название файла в текущей папке, без расширения
tree = ET.parse(file)
root = tree.getroot()
for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):
   for arguments in sampler_proxy.findall('.//stringProp[@name="Argument.value"]'):
       if arguments is not None:
          arguments_value = arguments.text
          try:
             arguments_value = unquote(arguments.text)
          except:
             print(f"cant unquote {arguments_value}")
          arguments.text = arguments_value

tree.write(f"{file}_b.jmx", encoding='utf-8', xml_declaration=True)
