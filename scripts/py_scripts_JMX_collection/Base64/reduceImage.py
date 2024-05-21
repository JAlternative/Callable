from PIL import Image

def reduce_image_quality(input_path, output_path, quality):
    try:
        # Открываем изображение
        with Image.open(input_path) as img:
            # Уменьшаем качество изображения
            img.save(output_path, quality=quality)

        print("Изображение успешно сохранено с уменьшенным качеством в файл", output_path)
    except Exception as e:
        print("Ошибка:", e)

# Путь к исходному изображению
input_path = "imphotosamsung.jpg"
# Путь для сохранения изображения с уменьшенным качеством
output_path = "ReduceImage.jpg"
# Уровень качества (от 1 до 95)
quality = 94

# Уменьшаем качество изображения
reduce_image_quality(input_path, output_path, quality)

# https://imagecompressor.com/ru/ лучше тут сжимать, с этого сайта хотябы при апи запросе лицо распрознаётся