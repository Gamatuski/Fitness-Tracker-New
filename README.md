# Fitness Tracker - Мобильное приложение для отслеживания физической активности

## Обзор проекта
Fitness Tracker - это мобильное приложение для Android, разработанное для помощи пользователям в отслеживании их физической активности. Приложение позволяет считать шаги, пройденное расстояние, сожженные калории, а также сохранять данные о тренировках. Пользователи могут ставить цели и отслеживать свой прогресс.

## Основные функции
- 📊 Отслеживание шагов и расстояния в реальном времени
- 🔥 Расчет сожженных калорий на основе активности и антропометрических данных
- 🏃‍♂️ Таймер тренировок с автоматическим сохранением результатов
- 📅 История активностей с группировкой по месяцам
- 🎯 Установка персональных целей по шагам и расстоянию
- 📊 Визуализация прогресса с помощью графиков и круговых индикаторов
- 🔔 Напоминания о тренировках
- 🔄 Синхронизация данных с сервером
- 📅 Выбор плана тренировок

## Технологический стек
- **Язык программирования**: Java
- **Архитектура**: MVVM (Model-View-ViewModel)
- **Сетевые запросы**: Retrofit
- **База данных**: MongoDB (серверная часть)
- **Графики**: MPAndroidChart
- **Карты**: Google Maps API
- **Работа с сенсорами**: Android Sensor API
- **Фоновые процессы**: Android Services

## Скриншоты приложения

### Экран входа
<img src="https://github.com/Gamatuski/Fitness-Tracker-New/blob/Images/login.png" width="300">

### Главный экран с картой и таймером
<img src="https://github.com/Gamatuski/Fitness-Tracker-New/blob/Images/Начать.jpg" width="300">

### Статистика шагов и расстояния

<div style="display: flex; justify-content: space-around;">
  <img src="https://github.com/Gamatuski/Fitness-Tracker-New/blob/Images/Шаги.jpg"  width="300" height="auto" style="margin-right: 10px;">
  <img src="https://github.com/Gamatuski/Fitness-Tracker-New/blob/Images/Растояние.jpg"  width="300" height="auto">
</div>

### История активностей
<div style="display: flex; justify-content: space-around;">
  <img src="https://github.com/Gamatuski/Fitness-Tracker-New/blob/Images/Прогресс.png"  width="300" height="auto" style="margin-right: 10px;">
  <img src="https://github.com/Gamatuski/Fitness-Tracker-New/blob/Images/детали%20активности.jpg"  width="300" height="auto">
</div>

## Установка и настройка

### Предварительные требования
- Android Studio (последняя версия)
- Устройство Android или эмулятор с API 24 (Nougat) или выше
- Серверная часть (см. ниже)

### Установка серверной части
1. Установите Node.js и MongoDB
2. Клонируйте репозиторий
3. Установите зависимости:
```bash
cd server
npm install
```
4. Запустите сервер:
```bash
node server.js
```

### Установка клиентской части
1. Откройте проект в Android Studio
2. В файле `RetrofitClient.java` укажите базовый URL вашего сервера:
```java
private static final String BASE_URL = "http://your-server-ip:3000/";
```
3. Запустите приложение на устройстве или эмуляторе

## Структура проекта
```
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── java/com/example/fitnesstracker
│   │   │   │   ├── activities       # Активности приложения
│   │   │   │   ├── adapters         # Адаптеры для RecyclerView
│   │   │   │   ├── api              # Сетевая логика
│   │   │   │   ├── fragments        # Фрагменты UI
│   │   │   │   ├── models           # Модели данных
│   │   │   │   ├── receivers        # BroadcastReceiver'ы
│   │   │   │   ├── service          # Сервисы приложения
│   │   │   │   ├── utils            # Вспомогательные классы
│   │   │   │   └── viewmodels       # ViewModel'и
│   │   │   └── res                  # Ресурсы приложения
│   ├── build.gradle
├── server
│   ├── server.js                   # Серверный код
```

## Особенности реализации
1. **Фоновое отслеживание шагов**:
   - Использует `StepCounterService` для работы с датчиком шагомера
   - Автоматически сохраняет данные при изменении
   - Работает в фоновом режиме

2. **Точный расчет дистанции**:
   ```java
   double stepLength = ((userHeight / 100) / 4) + 0.37;
   double distanceInMeters = steps * stepLength;
   double distanceInKilometers = distanceInMeters / 1000;
   ```

3. **Расчет калорий**:
   ```java
   double caloriesBurned = userWeight * met * durationInHours;
   ```

4. **Визуализация данных**:
   - Круговые индикаторы прогресса
   - Столбчатые диаграммы недельной статистики
   - Группировка активностей по месяцам

## Лицензия
Этот проект распространяется под лицензией MIT. См. файл [LICENSE](LICENSE) для получения дополнительной информации.

