const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const app = express();
const PORT = 3000;

// Middleware
app.use(bodyParser.json());
app.use(cors());

// Временное хранилище данных (в реальном проекте используйте базу данных)
const users = [];

// Обработчик для корневого пути
app.get('/', (req, res) => {
    res.json({ message: 'Сервер Fitness Tracker работает!' });
});

// Регистрация
app.post('/auth/register', async (req, res) => {
    const { email, password, height, weight } = req.body;

    // Проверка, существует ли пользователь
    const userExists = users.some(user => user.email === email);
    if (userExists) {
        return res.status(400).json({ success: false, message: 'Пользователь уже существует' });
    }

    // Хеширование пароля
    const hashedPassword = await bcrypt.hash(password, 10);

    // Создание пользователя
    const user = {
        id: users.length + 1,
        email,
        password: hashedPassword,
        height,
        weight
    };

    users.push(user);

    // Возвращаем успешный ответ
    res.status(201).json({ success: true, message: 'Пользователь зарегистрирован' });
});

// Вход
app.post('/auth/login', async (req, res) => {
    const { email, password } = req.body;

    // Проверка наличия email и password
    if (!email || !password) {
        return res.status(400).json({ success: false, message: 'Email и пароль обязательны' });
    }

    // Поиск пользователя
    const user = users.find(user => user.email === email);
    if (!user) {
        return res.status(400).json({ success: false, message: 'Пользователь не найден' });
    }

    // Проверка пароля
    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
        return res.status(400).json({ success: false, message: 'Неверный пароль' });
    }

    // Создание JWT-токена
    const token = jwt.sign({ id: user.id, email: user.email }, 'your-secret-key', { expiresIn: '1h' });

    // Возвращаем успешный ответ с токеном
    res.status(200).json({ success: true, message: 'Вход выполнен', token });
});
// Запуск сервера
app.listen(PORT, () => {
    console.log(`Сервер запущен на http://localhost:${PORT}`);
});