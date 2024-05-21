import base64

def encode_image_to_base64(image_path, output_path):
    try:
        # Открыть файл в режиме записи, чтобы очистить его содержимое
        with open(output_path, "w") as output_file:
            pass

        # Открыть изображение в бинарном режиме
        with open(image_path, "rb") as img_file:
            # Читаем данные изображения
            image_data = img_file.read()
            # Кодируем данные в base64
            base64_data = base64.b64encode(image_data).decode('utf-8')

        # Записать закодированные данные в файл
        with open(output_path, "w") as output_file:
            output_file.write(base64_data)

        print("Изображение успешно закодировано в base64 и записано в файл", output_path)
    except Exception as e:
        print("Ошибка:", e)


# Путь к исходному изображению
image_path = "imphotosamsung.jpg"
# Путь для сохранения закодированных данных в файле
output_path = "encodebase64.txt"

# Кодируем изображение в base64 и записываем в файл
encode_image_to_base64(image_path, output_path)
