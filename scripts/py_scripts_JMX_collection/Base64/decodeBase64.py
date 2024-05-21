import base64

def decode_base64_to_image(base64_string, output_path):
    try:
        # Декодировать строку base64 в бинарные данные
        image_data = base64.b64decode(base64_string)

        # Записать бинарные данные в файл
        with open(output_path, 'wb') as image_file:
            image_file.write(image_data)

        print("Изображение успешно сохранено в файл", output_path)
    except Exception as e:
        print("Ошибка:", e)

# Пример строки base64
base64_string = "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"
# Путь для сохранения изображения
output_path = "DecodeImage.jpg"

# Декодирование и сохранение изображения
decode_base64_to_image(base64_string, output_path)