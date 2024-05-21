<h1>Тестирование скриптов бд интеграции</h1>
<h2>Для установки зависимостей:</h2>
<li>Рекомендуется установить venv</li>
<li>Нужен `python3+`</li>
<li>`python -m pip install -r requirements.txt`</li>
<h2>Для запуска тестов:</h2>
<li>`pytest --alluredir=./allure`</li>
<h3>Дополнительные аргументы запуска:</h3>
<li>--host 'Имя сервера'</li>
<li>--password 'Пароль от бд'</li>
<li>--database 'Имя БД'</li>
<h3>Для генерации отчета, должен быть allure, и находится в PATH</h3>
<li>allure serve ./allure</li>