const express = require('express');
const mongoose = require('mongoose');
const bodyParser = require('body-parser');
const cors = require('cors');
const bcrypt = require('bcrypt'); // Добавляем bcrypt для хеширования паролей

const app = express();
const PORT = 3000;

app.use(bodyParser.json());
app.use(cors());

mongoose.connect('mongodb://localhost:27017/fitness_db', {
    useNewUrlParser: true,
    useUnifiedTopology: true,
}).then(() => {
    console.log('Connected to MongoDB');
}).catch((err) => {
    console.error('Failed to connect to MongoDB', err);
    process.exit(1); // Завершаем процесс при ошибке подключения
});

const userSchema = new mongoose.Schema({
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    height: { type: Number, required: true },
    weight: { type: Number, required: true },
    stepsGoal: { type: Number, required: true, default: 5000 }, // Цель шагов по умолчанию
    distanceGoal: { type: Number,required: true, default: 10 }, // Цель расстояния по умолчанию
    steps: { type: [Number], default: [0, 0, 0, 0, 0, 0, 0] },
    distance: { type: [Number], default: [0, 0, 0, 0, 0, 0, 0] },
    activities: [{
        action: { type: String, required: true },
        distance: { type: Number, required: true },
        calories: { type: Number, required: true },
        steps: { type: Number, required: true },
        duration: { type: Number, required: true },
        date: { type: Date, default: Date.now }
    }]
});

const User = mongoose.model('User', userSchema);

const workoutSchema = new mongoose.Schema({
    action: { type: String, required: true },
    duration: { type: Number, required: true },
    difficulty: { type: String, enum: ["легкий", "умеренный", "сложный"], default: "умеренный" },
    workoutsPerWeek: { type: Number, default: 3 },
    image: {
        imageUrl: { type: String },
        contentType: { type: String }
    }
});

const Workout = mongoose.model('Workout', workoutSchema);

const activitySchema = new mongoose.Schema({
    activityName: {
        type: String,
        required: true,
        unique: true, // Опционально: если имена активностей должны быть уникальными
        trim: true // Опционально: удаление пробелов в начале и конце имени
    },
    MET: {
        type: Number,
        required: true,
        min: 0 // Опционально: MET не может быть отрицательным
    }
});

const Activity = mongoose.model('Activity', activitySchema, 'activities'); // 'activities' - имя коллекции в MongoDB

module.exports = Activity;

app.get('/', (req, res) => {
    res.json({ message: 'Сервер Fitness Tracker работает!' });
});

app.post('/auth/register', async (req, res) => {
    const { email, password, height, weight } = req.body;

    try {
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            return res.status(400).json({ success: false, message: 'Пользователь уже существует' });
        }

        const hashedPassword = await bcrypt.hash(password, 10); // Хешируем пароль
        const user = new User({ email, password: hashedPassword, height, weight });
        await user.save();

        res.status(201).json({ success: true, message: 'Пользователь зарегистрирован' });
    } catch (err) {
        console.error('Error during registration:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.post('/auth/login', async (req, res) => {
    const { email, password } = req.body;

    try {
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(400).json({ success: false, message: 'Пользователь не найден' });
        }

        const passwordMatch = await bcrypt.compare(password, user.password); // Сравниваем хеши паролей
        if (!passwordMatch) {
            return res.status(400).json({ success: false, message: 'Неверный пароль' });
        }

        res.status(200).json({ success: true, message: 'Вход выполнен', user });
    } catch (err) {
        console.error('Error during login:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.get('/steps/:userId', async (req, res) => {
    const { userId } = req.params;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден' });
        }

        res.status(200).json({ success: true, steps: user.steps });
    } catch (err) {
        console.error('Error getting steps:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.post('/steps/:userId', async (req, res) => {
    const { userId } = req.params;
    const { steps, dayIndex } = req.body; // Получаем данные из тела запроса

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'Пользователь не найден'
            });
        }

        // Проверяем корректность индекса
        if (dayIndex < 0 || dayIndex >= 7) {
            return res.status(400).json({
                success: false,
                message: 'Некорректный индекс дня'
            });
        }

        // Обновляем значение шагов для конкретного дня
        user.steps[dayIndex] = steps;
        await user.save();

        res.status(200).json({
            success: true,
            steps: user.steps,
            message: 'Шаги успешно обновлены'
        });
    } catch (err) {
        console.error('Ошибка обновления шагов:', err);
        res.status(500).json({
            success: false,
            message: 'Ошибка сервера при обновлении шагов'
        });
    }
});

app.get('/distance/:userId', async (req, res) => {
    const { userId } = req.params;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден' });
        }

        res.status(200).json({ success: true, distance: user.distance });
    } catch (err) {
        console.error('Error getting distance:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.post('/distance/:userId', async (req, res) => {
    const { userId } = req.params;
    const { distance, dayIndex } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'Пользователь не найден'
            });
        }

        if (dayIndex < 0 || dayIndex >= 7) {
            return res.status(400).json({
                success: false,
                message: 'Некорректный индекс дня'
            });
        }

        user.distance[dayIndex] = distance;
        await user.save();

        res.status(200).json({
            success: true,
            distance: user.distance,
            message: 'Расстояние успешно обновлено'
        });
    } catch (err) {
        console.error('Ошибка обновления расстояния:', err);
        res.status(500).json({
            success: false,
            message: 'Ошибка сервера при обновлении расстояния'
        });
    }
});


app.get('/workouts', async (req, res) => {
    try {
        const workoutsData = await Workout.find(); // Получаем все тренировки из базы данных
        res.status(200).json(workoutsData.length > 0 ? workoutsData : []); // Возвращаем массив тренировок напрямую или пустой массив
    } catch (err) {
        console.error('Ошибка при получении тренировок:', err);
        res.status(500).json({ message: 'Ошибка сервера' });
    }
});

// Функция для получения MET по имени активности
async function getMETValueForActivity(activityName) {
    try {
        const activity = await Activity.findOne({ activityName: activityName }); // Поиск активности по имени
        if (activity) {
            return activity.MET; // Возвращаем значение MET, если активность найдена
        } else {
            return null; // Возвращаем null, если активность не найдена
        }
    } catch (error) {
        console.error("Ошибка при запросе MET для активности:", activityName, error);
        return null; // В случае ошибки также возвращаем null
    }
}


app.post('/users/:userId/activities', async (req, res) => {
    const { userId } = req.params;
    const { action, distance, calories, steps, duration, date } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден' });
        }

        user.activities.push({ action, distance, calories, steps, duration, date });

        // Обновление массива расстояний
        const currentDayOfWeek = new Date(date).getDay(); // Получаем день недели (0 - воскресенье, 1 - понедельник и т.д.)
        user.distance[currentDayOfWeek] += distance; // Добавляем пройденное расстояние к соответствующему дню

        await user.save();

        res.status(201).json({ success: true, message: 'Активность добавлена' });
    } catch (err) {
        console.error('Ошибка при добавлении активности:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.get('/users/:userId/activities', async (req, res) => {
    const { userId } = req.params;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден' });
        }

        res.status(200).json(user.activities);
    } catch (err) {
        console.error('Ошибка при получении активностей:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.get('/activities/:action', async (req, res) => {
    const { activityName } = req.params;

    try {
        const activity = await Activity.findOne({ activityName });
        if (activity) {
            res.status(200).json(activity);
        } else {
            res.status(404).json({ success: false, message: 'Активность не найдена' });
        }
    } catch (err) {
        console.error('Ошибка при получении активности:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.get('/users/:userId', async (req, res) => {
    const { userId } = req.params;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден' });
        }

        res.status(200).json({ success: true, user });
    } catch (err) {
        console.error('Ошибка при получении данных пользователя:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

// Эндпоинт для удаления активности
app.delete('/users/:userId/activities/:activityId', async (req, res) => {
    const { userId, activityId } = req.params;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден' });
        }

        const activityIndex = user.activities.findIndex(activity => activity._id.toString() === activityId);
        if (activityIndex === -1) {
            return res.status(404).json({ success: false, message: 'Активность не найдена' });
        }

        user.activities.splice(activityIndex, 1);
        await user.save();

        res.status(200).json({ success: true, message: 'Активность удалена' });
    } catch (err) {
        console.error('Ошибка при удалении активности:', err);
        res.status(500).json({ success: false, message: 'Ошибка сервера' });
    }
});

app.put('/users/:userId/goals', async (req, res) => {
    const { userId } = req.params;
    const { stepsGoal, distanceGoal } = req.body;

    try {
        const user = await User.findByIdAndUpdate(
            userId,
            { $set: { stepsGoal, distanceGoal } },
            { new: true }
        );

        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'Пользователь не найден'
            });
        }

        res.status(200).json({
            success: true,
            message: 'Цели обновлены',
            user
        });
    } catch (err) {
        console.error('Ошибка при обновлении целей:', err);
        res.status(500).json({
            success: false,
            message: 'Ошибка сервера'
        });
    }
});



app.listen(PORT, () => {
    console.log(`Сервер запущен на http://localhost:${PORT}`);
});