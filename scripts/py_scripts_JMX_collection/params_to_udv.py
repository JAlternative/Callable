import xml.etree.ElementTree as ET

file = 'Просмотр расписания' # название файла в текущей папке, без расширения
tree = ET.parse(f"{file}.jmx")
root = tree.getroot()

urls = set()
protocols = set()
ports = set()

for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):
    protocol = sampler_proxy.find('./stringProp[@name="HTTPSampler.protocol"]').text
    if protocol is not None:
       protocols.add(protocol)
    url = sampler_proxy.find('./stringProp[@name="HTTPSampler.domain"]').text
    if url is not None:
       urls.add(url)
    port = sampler_proxy.find('./stringProp[@name="HTTPSampler.port"]').text
    if port is not None:
       ports.add(port)
arguments = root.find('.//Arguments[@testname="User Defined Variables"]')
argument = arguments.find(".//collectionProp[@name='Arguments.arguments']")

counter = 1
for i in protocols:

    protocol_var = f"protocol_{counter}"
    protocol_val = i

    new_argument = ET.Element('elementProp', {'name': 'Argument', 'elementType':'Argument'})

    name_prop = ET.Element('stringProp', {'name': 'Argument.name'})
    name_prop.text = protocol_var
    new_argument.append(name_prop)
    value_prop = ET.Element('stringProp', {'name': 'Argument.value'})
    value_prop.text = protocol_val
    new_argument.append(value_prop)
    meta_prop = ET.Element('stringProp', {'name': 'Argument.metadata'})
    meta_prop.text = "="
    new_argument.append(meta_prop)

    argument.append(new_argument)
    counter = counter + 1

counter = 1
for i in urls:

        url_var = f"url_{counter}"
        url_val = i

        new_argument = ET.Element('elementProp', {'name': 'Argument', 'elementType': 'Argument'})

        name_prop = ET.Element('stringProp', {'name': 'Argument.name'})
        name_prop.text = url_var
        new_argument.append(name_prop)
        value_prop = ET.Element('stringProp', {'name': 'Argument.value'})
        value_prop.text = url_val
        new_argument.append(value_prop)
        meta_prop = ET.Element('stringProp', {'name': 'Argument.metadata'})
        meta_prop.text = "="
        new_argument.append(meta_prop)

        argument.append(new_argument)
        counter = counter + 1
counter = 1
for i in ports:

    port_var = f"port_{counter}"
    port_val = i

    new_argument = ET.Element('elementProp', {'name': 'Argument', 'elementType':'Argument'})

    name_prop = ET.Element('stringProp', {'name': 'Argument.name'})
    name_prop.text = port_var
    new_argument.append(name_prop)
    value_prop = ET.Element('stringProp', {'name': 'Argument.value'})
    value_prop.text = port_val
    new_argument.append(value_prop)
    meta_prop = ET.Element('stringProp', {'name': 'Argument.metadata'})
    meta_prop.text = "="
    new_argument.append(meta_prop)

    argument.append(new_argument)
    counter = counter + 1

vars_dict = dict()
counter = 0
for vars in argument.findall('.//stringProp[@name="Argument.name"]'):
    value_v = argument.findall('.//stringProp[@name="Argument.value"]')

    vars_dict[vars.text] = value_v[counter].text
    counter = counter+1


for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):
    protocol = sampler_proxy.find('./stringProp[@name="HTTPSampler.protocol"]')
    path_v = protocol.text.strip()
    for key, val in vars_dict.items():
        if protocol.text == val:
            path_v = f"${{{key}}}"


            protocol.text = path_v
for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):
    domain = sampler_proxy.find('./stringProp[@name="HTTPSampler.domain"]')
    path_v = domain.text.strip()
    for key, val in vars_dict.items():
        if domain.text == val:
            path_v = f"${{{key}}}"


            domain.text = path_v
for sampler_proxy in tree.findall('.//HTTPSamplerProxy'):
    port = sampler_proxy.find('./stringProp[@name="HTTPSampler.port"]')

    if port.text is not None:
        path_v = port.text.strip()
        for key, val in vars_dict.items():

            if port.text == (val):
               path_v = f"${{{key}}}"
               port.text = path_v

tree.write(f"{file}_udv.jmx", encoding='UTF-8', xml_declaration=True)
