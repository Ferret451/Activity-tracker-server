# Activity Tracker Server — REST API

Документ описывает HTTP API приложения на момент синхронизации с исходным кодом. Машиночитаемая схема: [OpenAPI](#связь-с-openapi).

## Общие соглашения

- **Базовый URL**: `http://localhost:8080` (порт по умолчанию Spring Boot; в `application.properties` не переопределён).
- **Формат**: JSON, кодировка UTF-8.
- **Заголовок для тел с JSON**: `Content-Type: application/json`.
- **Даты**:
  - `LocalDate` — строка `yyyy-MM-dd`.
  - `OffsetDateTime` — ISO-8601 со смещением часового пояса, например `2026-05-09T12:30:00+03:00`.
- **UUID** — строка в стандартном формате.
- **Перечисления (enum)** в JSON — имена констант в верхнем регистре, как в Kotlin (`MALE`, `INCOME`, …).
- **Числа с плавающей точкой и BigDecimal**: в JSON приходят/уходят как числа (Jackson может сериализовать `BigDecimal` в зависимости от настроек; клиент должен принимать числовой формат).

## Авторизация

- Эндпоинты **`/api/auth/**`**, документация OpenAPI и Swagger UI доступны **без** токена.
- Все остальные маршруты требуют заголовок:

  `Authorization: Bearer <accessToken>`

- В JWT поле **`sub`** — UUID пользователя; сервер использует его для `/api/me/...` и для идентификации при защищённых операциях.
- Дополнительные claims access-токена: `email`, `username`, **`scope`** — строка `USER` или `ADMIN`. Spring Security маппит их в полномочия `SCOPE_USER` / `SCOPE_ADMIN`.
- Маршруты **`/api/users/**`** требуют **любой** валидный JWT (пользователь или администратор); дальнейшие ограничения задаются на уровне методов (`@PreAuthorize` / SpEL): список пользователей, создание, обновление (`PUT`), удаление и смена роли — только **`SCOPE_ADMIN`**; **`GET /api/users/{id}`** — сам пользователь (`sub` совпадает с `id`) **или** администратор.
- Данные другого пользователя по префиксу **`/api/users/{userId}/...`** (питание, активность, финансы, профиль/настройки/вес/шаги) — только если **`userId`** совпадает с **`sub`** в JWT **или** вызывающий — администратор (**`SCOPE_ADMIN`**).
- Каталог типов приёмов пищи и типов активности (**`GET .../meal-types`**, **`GET .../activity/types`**) — **общий** для всех пользователей; **`POST` / `PUT` / `DELETE`** по этим типам — только **`SCOPE_ADMIN`**.
- После смены роли пользователю нужно заново выполнить **`POST /api/auth/refresh`** (или login), чтобы в JWT попал новый `scope`.
- Назначение первых администраторов при старте: свойство `app.security.bootstrap-admin-emails` (список email через запятую, без пробелов или с пробелами — email обрезаются); при каждом запуске для найденных пользователей выставляется роль `ADMIN`.

## Формат ошибок

Ошибки возвращаются как **RFC 7807 Problem Details** (`Content-Type` обычно `application/problem+json`):

| Поле    | Описание |
|---------|----------|
| `type`  | URI типа проблемы (в коде задаётся `about:blank`) |
| `title` | Краткое название (часто фраза HTTP status) |
| `status`| HTTP-код |
| `detail`| Человекочитаемое описание |

Дополнительно:

- При ошибке валидации тела запроса (Bean Validation) в **`errors`** передаётся объект: имя поля → сообщение.
- При сбое запроса к внешнему API продуктов питания может быть поле **`upstreamError`** (текст ответа провайдера).

## Коды ответов и типичные ситуации

| Код | Когда |
|-----|--------|
| 200 | Успешное чтение или обновление с телом |
| 201 | Успешное создание (`Location` не используется в коде; тело — созданный ресурс) |
| 204 | Успех без тела (например `DELETE`, `logout`) |
| 400 | Невалидные аргументы, ошибка валидации, `IllegalArgumentException`, часть ответов прокси еды |
| 401 | Неверные учётные данные, невалидный refresh token |
| 403 | Недостаточно прав (например чужой `userId` в `/api/users/{userId}/...`, обновление пользователя без роли администратора, CRUD типов питания/активности без `SCOPE_ADMIN`) |
| 404 | Сущность не найдена (пользователь, профиль, цель, запись и т.д.) |
| 409 | Конфликт: дубликат email/username, дубликат типа активности/приёма пищи/категории финансов, тип/категория «используется» |
| 429 | Слишком много запросов к внешнему API еды |
| 502 | Ошибка шлюза к внешнему API еды (прочие статусы upstream) |
| 503 | Сервис недоступен (маппинг для части ошибок провайдера еды) |

---

## Общие типы ответов

### UserResponse

Используется в регистрации/логине и в CRUD пользователей.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор |
| `email` | string | Email |
| `username` | string | Имя пользователя |
| `firstName` | string | Имя |
| `lastName` | string | Фамилия |
| `role` | string | `USER` или `ADMIN` |
| `createdAt` | OffsetDateTime | Создан |
| `updatedAt` | OffsetDateTime | Обновлён |

### AuthResponse

| Поле | Тип | Описание |
|------|-----|----------|
| `accessToken` | string | JWT для `Authorization: Bearer` |
| `refreshToken` | string | Токен обновления |
| `tokenType` | string | Тип (как выдаёт сервис) |
| `accessTokenExpiresAt` | OffsetDateTime | Истечение access |
| `refreshTokenExpiresAt` | OffsetDateTime | Истечение refresh |
| `user` | UserResponse | Профиль пользователя в ответе |

---

## `/api/auth` — аутентификация

**Авторизация не требуется.**

### `POST /api/auth/register`

**Ответ:** `201 Created`, тело `AuthResponse`.

**Тело — RegisterRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `email` | string | да | email, не пустой, max 255 |
| `username` | string | да | не пустой, max 100 |
| `firstName` | string | да | не пустой, max 100 |
| `lastName` | string | да | не пустой, max 100 |
| `password` | string | да | не пустой, 8–100 символов |

### `POST /api/auth/login`

**Ответ:** `200 OK`, тело `AuthResponse`.

**Тело — LoginRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `email` | string | да | email, не пустой, max 255 |
| `password` | string | да | не пустой, 8–100 символов |

### `POST /api/auth/refresh`

**Ответ:** `200 OK`, тело `AuthResponse`.

**Тело — RefreshTokenRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `refreshToken` | string | да | не пустой |

### `POST /api/auth/logout`

**Ответ:** `204 No Content`, тело ответа отсутствует.

**Тело — RefreshTokenRequest** (как выше).

---

## `/api/users` — пользователи

**Требуется JWT.** Часть операций — только администратор (**`SCOPE_ADMIN`**).

### `GET /api/users`

**Требуется `SCOPE_ADMIN`.**

**Ответ:** `200 OK`, массив `UserResponse`.

### `GET /api/users/{id}`

**Доступ:** владелец (**`sub` в JWT совпадает с `id`**) **или** **`SCOPE_ADMIN`**.

**Path:** `id` — UUID.

**Ответ:** `200 OK`, `UserResponse`.

### `POST /api/users`

**Требуется `SCOPE_ADMIN`.** Полный CRUD по сущности пользователя (отдельно от регистрации через `/api/auth/register`).

**Ответ:** `201 Created`, `UserResponse`.

**Тело — CreateUserRequest** (как `RegisterRequest` + необязательное поле `role`):

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `email` | string | да | email, не пустой, max 255 |
| `username` | string | да | не пустой, max 100 |
| `firstName` | string | да | не пустой, max 100 |
| `lastName` | string | да | не пустой, max 100 |
| `password` | string | да | не пустой, 8–100 символов |
| `role` | string | нет | enum `USER` / `ADMIN`; по умолчанию `USER` |

### `PUT /api/users/{id}`

**Требуется `SCOPE_ADMIN`.**

**Path:** `id` — UUID.

**Ответ:** `200 OK`, `UserResponse`.

**Тело — UpdateUserRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `email` | string | да | email, не пустой, max 255 |
| `username` | string | да | не пустой, max 100 |
| `firstName` | string | да | не пустой, max 100 |
| `lastName` | string | да | не пустой, max 100 |
| `role` | string | нет | если передано — обновить роль (`USER` / `ADMIN`) |

Пароль в обновлении не передаётся; смена **своего** пароля — **`PUT /api/me/password`**.

### `PATCH /api/users/{id}/role`

**Требуется `SCOPE_ADMIN`.**

**Тело — UpdateUserRoleRequest:** объект `role` — `USER` или `ADMIN`.

**Ответ:** `200 OK`, `UserResponse`.

### `DELETE /api/users/{id}`

**Требуется `SCOPE_ADMIN`.**

**Path:** `id` — UUID.

**Ответ:** `204 No Content`.

---

## `/api/users/{userId}/...` — данные пользователя (сам пользователь или администратор)

**Требуется JWT.** Доступ: **`userId`** совпадает с **`sub`** в access-токене **или** у вызывающего есть **`SCOPE_ADMIN`**. Иначе **`403 Forbidden`**.

По смыслу и телам запросов/ответов совпадают с соответствующими маршрутами **`/api/me/...`**, но выполняются для указанного **`userId`**:

| Префикс | Содержимое (как у `/api/me/...`) |
|---------|----------------------------------|
| **`/api/users/{userId}/profile`**, **`.../bmr`** | профиль и расчёт BMR |
| **`/api/users/{userId}/settings`** | настройки приложения |
| **`/api/users/{userId}/weight/...`**, **`.../steps/...`** | вес и шаги |
| **`/api/users/{userId}/activity/...`** | записи активности, сводка, тренировки (`.../workouts/...`); **без** поддерева **`/types`** — каталог типов активности только в **`/api/me/activity/types`** |
| **`/api/users/{userId}/nutrition/...`** | продукты, записи питания, **`records/intake`**, `history`, `summary`, `daily`; **без** **`/meal-types`** — каталог типов приёмов пищи только в **`/api/me/nutrition/meal-types`** |
| **`/api/users/{userId}/finance/...`** | финансы (счета, категории, записи, отчёты) |

## `/api/me` — профиль текущего пользователя

**Требуется JWT.**

### Аватар

#### `POST /api/me/avatar` — `200`, AvatarResponse

Загрузка аватара текущего пользователя. Тело запроса — `multipart/form-data` с частью:

| Part | Тип | Обяз. | Описание |
|------|-----|-------|----------|
| `file` | file | да | `image/jpeg`, `image/png` или `image/webp` |

Ответ — **AvatarResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `url` | string | Публичный URL аватара |

#### `GET /api/me/avatar` — `200`, AvatarResponse

Возвращает текущий URL аватара пользователя.

#### `DELETE /api/me/avatar` — `204`

Удаляет аватар: ссылка очищается, локальный файл удаляется (если находится в локальном хранилище сервера).

### `GET /api/me/profile`

**Ответ:** `200 OK`, тело **UserProfileResponse**.

| Поле | Тип | Описание |
|------|-----|----------|
| `weightKg` | number | Вес, кг |
| `heightCm` | integer | Рост, см |
| `age` | integer | Возраст, лет |
| `sex` | string | См. enum `Sex` |
| `activityLevel` | string | См. enum `ActivityLevel` |
| `calorieGoal` | string | См. enum `CalorieGoal` |
| `dailyCalorieTargetKcal` | integer \| null | Явная суточная цель в ккал (если задана) |

### `PUT /api/me/profile`

**Ответ:** `200 OK`, `UserProfileResponse`.

**Тело — UpsertProfileRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `weightKg` | number | да | > 0 (`@Positive`) |
| `heightCm` | integer | да | 50–300 |
| `age` | integer | да | 10–120 |
| `sex` | string | да | enum `Sex` |
| `activityLevel` | string | да | enum `ActivityLevel` |
| `calorieGoal` | string | да | enum `CalorieGoal` |
| `dailyCalorieTargetKcal` | integer | нет | ≥ 0 (если передано) |

### `PUT /api/me/password`

**Ответ:** `204 No Content`, тело ответа отсутствует.

**Тело — ChangeOwnPasswordRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `currentPassword` | string | да | не пустой, 8–100 символов |
| `newPassword` | string | да | не пустой, 8–100 символов |

При неверном текущем пароле — **`400 Bad Request`**.

### `GET /api/me/bmr`

**Ответ:** `200 OK`, тело **BmrResponse**.

| Поле | Тип | Описание |
|------|-----|----------|
| `bmr` | number | Базовый метаболизм |
| `baseDailyCalories` | number | Суточная норма без учёта цели (`bmr * activityMultiplier`) |
| `goalAdjustedDailyCalories` | number | Суточная норма с учётом цели (`baseDailyCalories + goalDelta`) |

---

## `/api/me/settings` — настройки приложения

**Требуется JWT.**

### `GET /api/me/settings` — `200`, **UserSettingsResponse**

| Поле | Тип | Описание |
|------|-----|----------|
| `preferences` | object | Произвольный JSON-объект настроек (ключ → значение) |

Если строка в БД ещё не создана, возвращается пустой объект `{}`.

### `PUT /api/me/settings` — `200`, UserSettingsResponse

**Тело — UserSettingsUpdateRequest:** поле `preferences` — object, полностью заменяет сохранённый JSON.

---

## `/api/me/weight` — записи веса

**Требуется JWT.** При создании/изменении/удалении записи веса сервер обновляет поле **`weightKg`** в профиле пользователя по **последней** записи по полю `measuredAt` (если профиль существует).

### `POST /api/me/weight/entries` — `201`, WeightEntryResponse

**Тело — CreateWeightEntryRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `weightKg` | number | да | > 0 |
| `measuredAt` | OffsetDateTime | нет | по умолчанию текущий момент UTC |
| `note` | string | нет | max 500 |

### `GET /api/me/weight/entries` — `200`, массив WeightEntryResponse

**Query (необяз.):** `from`, `to` — `LocalDate`.

### `GET /api/me/weight/entries/{entryId}` — `200`

### `PUT /api/me/weight/entries/{entryId}` — `200`

**Тело — UpdateWeightEntryRequest:** `weightKg`, `measuredAt` (обяз.), `note`.

### `DELETE /api/me/weight/entries/{entryId}` — `204`

### `GET /api/me/weight/stats` — `200`, **WeightStatsResponse**

**Query:** `from`, `to` — LocalDate, обязательны.

**WeightStatsResponse:** `from`, `to`, `count`, `minWeightKg`, `maxWeightKg`, `avgWeightKg` (могут быть `null` при отсутствии данных), `entries` — массив WeightEntryResponse.

**WeightEntryResponse:** `id`, `weightKg`, `measuredAt`, `note`, `createdAt`, `updatedAt`.

---

## `/api/me/steps` — шаги

**Требуется JWT.**

### `PUT /api/me/steps/{date}` — `200`, **DailyStepsResponse**

**Path:** `date` — `LocalDate`.

**Тело — UpsertDailyStepsRequest:**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `steps` | integer | да | ≥ 1 (`@Positive`) |
| `burnedCaloriesKcal` | number | да | ≥ 0 |
| `walkingDurationMinutes` | integer | да | ≥ 0 |

Пример тела:

```json
{
  "steps": 8500,
  "burnedCaloriesKcal": 320.5,
  "walkingDurationMinutes": 75
}
```

### `GET /api/me/steps/daily` — `200`, DailyStepsResponse

**Query:** `date` — LocalDate. Если за день нет записи, `steps`, `burnedCaloriesKcal` и `walkingDurationMinutes` = 0.

### `GET /api/me/steps/stats` — `200`, **DailyStepsStatsResponse**

**Query:** `from`, `to` — LocalDate.

**DailyStepsStatsResponse:** `from`, `to`, `days` — массив **DailyStepsResponse** по каждому календарному дню (включая дни без записи: `steps`, `burnedCaloriesKcal`, `walkingDurationMinutes` = 0).

**DailyStepsResponse:** `date`, `steps`, `burnedCaloriesKcal`, `walkingDurationMinutes`, `createdAt`, `updatedAt` (последние два могут быть `null`, если записи не было).

---

## `/api/me/finance` — финансы

**Требуется JWT.** Базовый префикс: `/api/me/finance`.

**Перечисление `FinanceOperationType`:** на уровне категорий и записей сохранённый тип операции совпадает с константой `INCOME` или `EXPENSE`. Даты в query (`from`, `to`) — `LocalDate` (`yyyy-MM-dd`); каждая дата включается в интервал полностью. Границы периода в UTC: с `from` в 00:00:00 UTC до начала календарного дня после `to` (верхняя граница **не включается**). Время отдельной операции задаётся полем `occurredAt` (`OffsetDateTime`).

### Счета

#### `GET /api/me/finance/accounts` — `200`, массив **FinanceAccountResponse**

Список счетов владельца: сначала основной (`isPrimary: true`), затем по `createdAt` убыванию.

#### `POST /api/me/finance/accounts` — `201`, FinanceAccountResponse

**Тело — CreateFinanceAccountRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `name` | string | да | не пустой, max 120 |
| `setAsPrimary` | boolean | нет | по умолчанию `false` |

Если у пользователя ещё нет счетов, новый счёт всегда становится основным. Иначе при `setAsPrimary: true` прежний основной снимается.

#### `GET /api/me/finance/accounts/{accountId}` — `200`, FinanceAccountResponse

#### `PUT /api/me/finance/accounts/{accountId}` — `200`, FinanceAccountResponse

**Тело — UpdateFinanceAccountRequest:** `name` (string, обяз., max 120), `setAsPrimary` (boolean, по умолчанию `false`). При `setAsPrimary: true` этот счёт становится основным.

#### `DELETE /api/me/finance/accounts/{accountId}` — `204`

Удаление разрешено только для **неосновного** счёта **без** связанных операций. Иначе `409 Conflict`.

**FinanceAccountResponse:** `id`, `name`, `isPrimary`, `balance` (number, текущий остаток на счёте), `createdAt`, `updatedAt`.

### Категории доходов

#### `POST /api/me/finance/income/categories` — `201`, FinanceCategoryResponse

**Тело — CreateFinanceCategoryBranchRequest:** `name` (string, обяз., max 80), `iconId` (Long, нет), `colorId` (Long, нет). Тип операции категории всегда `INCOME`.

#### `GET /api/me/finance/income/categories` — `200`, массив FinanceCategoryResponse

#### `PUT /api/me/finance/income/categories/{categoryId}` — `200`, FinanceCategoryResponse

**Тело — UpdateFinanceCategoryBranchRequest** (аналогично `CreateFinanceCategoryBranchRequest`).

#### `DELETE /api/me/finance/income/categories/{categoryId}` — `204`

Удаление запрещено, если по категории есть операции — **`409 Conflict`**.

### Категории расходов

#### `POST /api/me/finance/expense/categories` — `201`, FinanceCategoryResponse

Тело как у доходных; тип категории всегда `EXPENSE`.

#### `GET /api/me/finance/expense/categories` — `200`, массив FinanceCategoryResponse

#### `PUT /api/me/finance/expense/categories/{categoryId}` — `200`, FinanceCategoryResponse

**Тело — UpdateFinanceCategoryBranchRequest** (как для доходов).

#### `DELETE /api/me/finance/expense/categories/{categoryId}` — `204`

Удаление запрещено при наличии операций — **`409 Conflict`**.

**FinanceCategoryResponse:** `id`, `name`, `operationType`, `iconId` (Long), `colorId` (Long), `createdAt`, `updatedAt`.

### Записи доходов

#### `POST /api/me/finance/income/records` — `201`, FinanceRecordResponse

**Тело — CreateFinanceRecordRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `categoryId` | UUID | да | категория с типом `INCOME` |
| `accountId` | UUID | нет | если не указан — **основной** счёт; при отсутствии счетов создаётся основной «Основной» |
| `amount` | number | да | ≥ 0.01 |
| `note` | string | нет | max 500 |
| `occurredAt` | OffsetDateTime | нет | по умолчанию текущий момент UTC |

#### `GET /api/me/finance/income/records` — `200`, массив FinanceRecordResponse

**Query (все необязательны):** `from`, `to` (LocalDate), `categoryId`, `accountId`. Возвращаются только операции с типом `INCOME`.

#### `GET /api/me/finance/income/records/{recordId}` — `200`, FinanceRecordResponse

#### `PUT /api/me/finance/income/records/{recordId}` — `200`, FinanceRecordResponse

**Тело — UpdateFinanceRecordRequest:** `categoryId`, `accountId` (нет — оставить текущий счёт), `amount`, `note`, `occurredAt` (обяз.). Категория должна оставаться доходной.

#### `DELETE /api/me/finance/income/records/{recordId}` — `204`

### Записи расходов

Те же шаблоны под префиксом `/api/me/finance/expense/records` с типом категории `EXPENSE`.

### Отчёт за период

#### `GET /api/me/finance/report` — `200`, **FinancePeriodReportResponse**

**Query**

| Параметр | Тип | Обяз. | Описание |
|----------|-----|-------|----------|
| `from` | LocalDate | да | начало периода (включительно) |
| `to` | LocalDate | да | конец периода (включительно) |
| `accountId` | UUID | нет | только операции по счёту |

**Ответ — FinancePeriodReportResponse**

| Поле | Тип |
|------|-----|
| `from`, `to` | LocalDate |
| `totalIncome` | number |
| `totalExpense` | number |
| `net` | number (доход − расход за период) |
| `byCategory` | массив **FinanceCategorySummaryResponse** |
| `incomeRecords` | массив FinanceRecordResponse |
| `expenseRecords` | массив FinanceRecordResponse |

**FinanceCategorySummaryResponse:** `categoryId`, `categoryName`, `operationType`, `totalAmount`.

**FinanceRecordResponse:** `id`, `account` (**FinanceAccountSummaryResponse:** `id`, `name`), `category` (FinanceCategoryResponse), `operationType`, `amount`, `note`, `occurredAt`, `createdAt`, `updatedAt`.

**Ошибки:** неверная дата (`from` > `to`) — `400`; категория не соответствует ветке доход/расход — `400`; запись не из этой ветки — `400`; дубликат имени категории для того же типа — `409`; счёт не найден — `404`; удаление основного или занятого счёта — `409`; удаление категории с операциями — `409`.

#### `GET /api/me/finance/report/timeseries` — `200`, **FinanceTimeseriesReportResponse**

**Query**

| Параметр | Тип | Обяз. | Описание |
|----------|-----|-------|----------|
| `from` | LocalDate | да | начало периода (включительно) |
| `to` | LocalDate | да | конец периода (включительно) |
| `accountId` | UUID | нет | фильтр по счёту |
| `granularity` | string | нет | сейчас поддерживается только **`DAY`** (значение по умолчанию) |

**Ответ — FinanceTimeseriesReportResponse**

| Поле | Тип |
|------|-----|
| `from`, `to` | LocalDate |
| `granularity` | string (`DAY`) |
| `points` | массив **FinanceTimeseriesPointResponse** |

**FinanceTimeseriesPointResponse:** `period` (LocalDate), `totalIncome`, `totalExpense`, `net` (числа).

---

## `/api/me/activity` — активность

**Требуется JWT.**

### Типы активности

Каталог **общий** для всех пользователей. **`POST` / `PUT` / `DELETE`** — только **`SCOPE_ADMIN`**.

#### `POST /api/me/activity/types`

**Требуется `SCOPE_ADMIN`.**

**Ответ:** `201 Created`, `ActivityTypeResponse`.

**Тело — CreateActivityTypeRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `name` | string | да | не пустой, max 80 |
| `met` | number | да | ≥ 0.1 (`@DecimalMin("0.1")`) |

#### `GET /api/me/activity/types`

**Ответ:** `200 OK`, массив `ActivityTypeResponse`.

#### `PUT /api/me/activity/types/{typeId}`

**Требуется `SCOPE_ADMIN`.**

**Path:** `typeId` — UUID.

**Ответ:** `200 OK`, `ActivityTypeResponse`.

**Тело — UpdateActivityTypeRequest** (те же поля и ограничения, что в create).

#### `DELETE /api/me/activity/types/{typeId}`

**Требуется `SCOPE_ADMIN`.**

**Path:** `typeId` — UUID.

**Ответ:** `204 No Content`.

**ActivityTypeResponse**

| Поле | Тип |
|------|-----|
| `id` | UUID |
| `name` | string |
| `met` | number |
| `updatedAt` | OffsetDateTime |

### Записи активности

#### `POST /api/me/activity/records`

**Ответ:** `201 Created`, `ActivityRecordResponse`.

**Тело — CreateActivityRecordRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `activityTypeId` | UUID | да | — |
| `durationMinutes` | integer | да | > 0 |
| `distanceKm` | number | нет | ≥ 0, если передан |
| `note` | string | нет | max 500 |
| `occurredAt` | OffsetDateTime | нет | если не передан — **текущий момент UTC** |

#### `GET /api/me/activity/records`

**Query (все необязательны):**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `from` | LocalDate | Начало периода |
| `to` | LocalDate | Конец периода |
| `activityTypeId` | UUID | Фильтр по типу |

**Ответ:** `200 OK`, массив `ActivityRecordResponse`.

#### `GET /api/me/activity/records/{recordId}`

**Path:** `recordId` — UUID.

**Ответ:** `200 OK`, `ActivityRecordResponse`.

#### `PUT /api/me/activity/records/{recordId}`

**Path:** `recordId` — UUID.

**Ответ:** `200 OK`, `ActivityRecordResponse`.

**Тело — UpdateActivityRecordRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `activityTypeId` | UUID | да | — |
| `durationMinutes` | integer | да | > 0 |
| `distanceKm` | number | нет | ≥ 0 |
| `note` | string | нет | max 500 |
| `occurredAt` | OffsetDateTime | **да** | момент события |

#### `DELETE /api/me/activity/records/{recordId}`

**Ответ:** `204 No Content`.

**ActivityRecordResponse**

| Поле | Тип |
|------|-----|
| `id` | UUID |
| `activityType` | ActivityTypeResponse |
| `durationMinutes` | integer |
| `distanceKm` | number \| null |
| `note` | string \| null |
| `occurredAt` | OffsetDateTime |
| `burnedCaloriesKcal` | number |
| `createdAt` | OffsetDateTime |
| `updatedAt` | OffsetDateTime |

### `GET /api/me/activity/summary`

**Query:** как у `GET /records` (`from`, `to`, `activityTypeId`), все необязательны.

**Ответ:** `200 OK`, **ActivitySummaryResponse**.

| Поле | Тип |
|------|-----|
| `totalDurationMinutes` | integer |
| `totalDistanceKm` | number |
| `totalBurnedCaloriesKcal` | number |
| `byType` | массив ActivityTypeSummaryResponse |

**ActivityTypeSummaryResponse:** `activityTypeId`, `activityTypeName`, `totalDurationMinutes`, `totalDistanceKm`, `totalBurnedCaloriesKcal`.

### Тренировки (сессии и маршрут)

Префикс: **`/api/me/activity/workouts`**. Одновременно может быть не более **одной** активной сессии на пользователя (иначе **`409 Conflict`** при старте).

#### `POST /api/me/activity/workouts/start` — `201`, **WorkoutSessionResponse**

**Тело — StartWorkoutRequest:** `activityTypeId` (UUID).

#### `GET /api/me/activity/workouts` — `200`, массив WorkoutSessionResponse

#### `GET /api/me/activity/workouts/{sessionId}` — `200`, WorkoutSessionResponse

#### `POST /api/me/activity/workouts/{sessionId}/route-points` — `204`

**Тело — AppendWorkoutRoutePointsRequest:** `points` — массив объектов с полями `latitude`, `longitude`, опционально `recordedAt` (иначе — текущий UTC). Не более **500** точек за один запрос.

#### `GET /api/me/activity/workouts/{sessionId}/route` — `200`, массив **WorkoutRoutePointResponse**

#### `POST /api/me/activity/workouts/{sessionId}/stop` — `200`, WorkoutSessionResponse

**Тело — StopWorkoutRequest** (необязательно): `distanceKm`, `note`, `endedAt`. Создаётся запись активности (`ActivityRecord`) с длительностью по разнице `startedAt` и `endedAt` (минимум 1 минута), калории считаются по MET и весу из профиля (как при ручном создании записи).

#### `POST /api/me/activity/workouts/{sessionId}/cancel` — `200`, WorkoutSessionResponse

Сессия переводится в `CANCELLED` без создания записи активности.

**WorkoutSessionResponse:** `id`, `activityType` (ActivityTypeResponse), `status` (`ACTIVE` \| `COMPLETED` \| `CANCELLED`), `startedAt`, `endedAt`, `activityRecordId` (UUID \| null), `note`, `createdAt`, `updatedAt`.

**WorkoutRoutePointResponse:** `id`, `pointSeq`, `latitude`, `longitude`, `recordedAt`.

---

## `/api/me/nutrition` — питание

**Требуется JWT.**

### Типы приёмов пищи (`meal-types`)

Каталог **общий** для всех пользователей. **`POST` / `PUT` / `DELETE`** — только **`SCOPE_ADMIN`**.

#### `POST /api/me/nutrition/meal-types` — `201`, **MealTypeResponse**

**Требуется `SCOPE_ADMIN`.**

**Тело — CreateMealTypeRequest:** `name` — string, обяз., не пустой, max 80.

#### `GET /api/me/nutrition/meal-types` — `200`, массив MealTypeResponse

#### `PUT /api/me/nutrition/meal-types/{mealTypeId}` — `200`, MealTypeResponse

**Требуется `SCOPE_ADMIN`.**

**Тело — UpdateMealTypeRequest:** как `name` в create.

#### `DELETE /api/me/nutrition/meal-types/{mealTypeId}` — `204`

**Требуется `SCOPE_ADMIN`.**

**MealTypeResponse:** `id`, `name`, `updatedAt`.

### Продукты (`food-items`)

#### `POST /api/me/nutrition/food-items` — `201`, FoodItemResponse

**Тело — CreateFoodItemRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `name` | string | да | не пустой, max 120 |
| `caloriesPer100g` | number | да | ≥ 0 |
| `proteinPer100g` | number | да | ≥ 0 |
| `fatPer100g` | number | да | ≥ 0 |
| `carbsPer100g` | number | да | ≥ 0 |
| `source` | string | нет | max 30 |
| `fdcId` | long | нет | внешний id USDA |
| `note` | string | нет | max 500 |
| `additionalNutrients` | массив **AdditionalNutrientRequest** | нет | до 100 элементов |

**AdditionalNutrientRequest** (элементы `additionalNutrients`):

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `number` | string | нет | max 16, код нутриента (например USDA), если известен |
| `name` | string | да | не пустой, max 200 |
| `unitName` | string | нет | max 32 |
| `amountPer100g` | number | да | ≥ 0 |

**Рекомендуемый способ зафиксировать приём пищи из USDA:** **`POST /api/me/nutrition/records/intake`** — одним запросом подтягиваются/обновляются данные FDC и создаётся запись дневника (см. ниже). Отдельного **`POST .../food-items/import/usda/{fdcId}`** больше нет.

#### `GET /api/me/nutrition/food-items` — `200`, массив FoodItemResponse

#### `GET /api/me/nutrition/food-items/{foodItemId}` — `200`, FoodItemResponse

#### `PUT /api/me/nutrition/food-items/{foodItemId}` — `200`, FoodItemResponse

**Тело — UpdateFoodItemRequest** — те же поля, что в create (все макросы и `name` обязательны при обновлении в смысле DTO).

#### `DELETE /api/me/nutrition/food-items/{foodItemId}` — `204`

**FoodItemResponse:** `id`, `name`, `caloriesPer100g`, `proteinPer100g`, `fatPer100g`, `carbsPer100g`, `source`, `fdcId`, `note`, `createdAt`, `updatedAt`, `additionalNutrientsPer100g` (массив **AdditionalNutrientPer100gResponse**: `number`, `name`, `unitName`, `amountPer100g`).

### Записи питания (`records`)

**Фильтры дат `from` / `to` (и те же параметры у `history` и `summary`):** значения — календарные **`LocalDate`**. На сервере интервал строится как **полуночи по UTC**: нижняя граница — `from` в `00:00:00Z`, верхняя — **до** `00:00:00Z` дня **`to` + 1 день** (т.е. `to` включается целиком в смысле UTC-календаря). Это **не** «локальные сутки» пользователя; для дня в конкретной зоне используйте **`GET /api/me/nutrition/daily`**.

#### `POST /api/me/nutrition/records/intake` — `201`, **NutritionRecordResponse**

Одна операция «съел»: сервер при необходимости **создаёт или обновляет** продукт в каталоге пользователя (для USDA — по `fdcId`, с запросом к FDC API) и сохраняет **запись приёма пищи** с пересчётом КБЖУ и снимком доп. нутриентов.

**Тело — LogIntakeRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `mealTypeId` | UUID | да | — |
| `grams` | number | да | ≥ 0.1 |
| `foodItemId` | UUID | нет* | продукт из своего каталога |
| `fdcId` | long | нет* | id продукта в USDA FDC, **> 0**, если указан |
| `note` | string | нет | max 500 |
| `consumedAt` | OffsetDateTime | нет | если не передан — **текущий момент UTC** |

\* **Ровно одно** из полей `foodItemId` или `fdcId` должно быть задано (иначе `400`).

Если передан **`fdcId`**: существующий у пользователя продукт с тем же `fdcId` обновляется данными из API; иначе создаётся новый элемент каталога (как раньше при импорте USDA). Затем создаётся запись дневника.

Если передан **`foodItemId`**: каталог не меняется по внешнему API; создаётся только запись (эквивалентно `POST .../records` с этим `foodItemId`).

Зеркало для админского доступа к чужому пользователю: **`POST /api/users/{userId}/nutrition/records/intake`** — то же тело и ответ.

#### `POST /api/me/nutrition/records` — `201`, NutritionRecordResponse

**Тело — CreateNutritionRecordRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `mealTypeId` | UUID | да | — |
| `foodItemId` | UUID | да | — |
| `grams` | number | да | ≥ 0.1 |
| `note` | string | нет | max 500 |
| `consumedAt` | OffsetDateTime | нет | если не передан — **текущий момент UTC** |

#### `GET /api/me/nutrition/records` — `200`, массив NutritionRecordResponse

**Query (необязательны):** `from`, `to` (LocalDate), `mealTypeId`, `foodItemId` (UUID). См. выше про **UTC-границы** для `from`/`to`.

#### `GET /api/me/nutrition/records/{recordId}` — `200`

#### `PUT /api/me/nutrition/records/{recordId}` — `200`

**Тело — UpdateNutritionRecordRequest:** `mealTypeId`, `foodItemId`, `grams` (≥ 0.1), `note` (опц.), **`consumedAt` обязателен**.

#### `DELETE /api/me/nutrition/records/{recordId}` — `204`

При создании и изменении записи дополнительные нутриенты **пересчитываются** из продукта и граммовки (снимок на момент сохранения).

**NutritionRecordResponse:** `id`, `mealType`, `foodItem`, `grams`, `totalCaloriesKcal`, `totalProteinG`, `totalFatG`, `totalCarbsG`, `note`, `consumedAt`, `createdAt`, `updatedAt`, `additionalNutrients` (массив **AdditionalNutrientPortionResponse**: `number`, `name`, `unitName`, `totalAmount`).

### `GET /api/me/nutrition/history` — `200`

**Query (необязательны):** `from`, `to` (LocalDate). См. раздел записей про **UTC-границы** для `from`/`to`.

**Ответ:** массив **NutritionHistoryDayResponse**.

| Поле | Тип |
|------|-----|
| `date` | LocalDate |
| `totalCaloriesKcal` | number |
| `totalProteinG` | number |
| `totalFatG` | number |
| `totalCarbsG` | number |
| `additionalNutrientsTotals` | массив **AdditionalNutrientTotalResponse** |
| `records` | массив NutritionRecordResponse |

**AdditionalNutrientTotalResponse:** `number` (string \| null), `name`, `unitName` (string \| null), `totalAmount` (number) — сумма по дню; строки с одинаковым ключом `(COALESCE(number,''), имя в нижнем регистре)` объединяются.

### `GET /api/me/nutrition/summary` — `200`

**Query (необязательны):** как у списка записей: `from`, `to`, `mealTypeId`, `foodItemId`. См. раздел записей про **UTC-границы** для `from`/`to`.

**Ответ — NutritionSummaryResponse**

| Поле | Тип |
|------|-----|
| `totalCaloriesKcal` | number |
| `totalProteinG` | number |
| `totalFatG` | number |
| `totalCarbsG` | number |
| `additionalNutrientsTotals` | массив **AdditionalNutrientTotalResponse** |
| `byMealType` | массив NutritionMealTypeSummaryResponse |

**NutritionMealTypeSummaryResponse:** `mealTypeId`, `mealTypeName`, `totalCaloriesKcal`, `totalProteinG`, `totalFatG`, `totalCarbsG`, `additionalNutrientsTotals` (массив **AdditionalNutrientTotalResponse**).

### `GET /api/me/nutrition/daily` — `200`, **NutritionDailyResponse**

Агрегат за **локальные сутки** в выбранной зоне: съеденные калории и макросы, суточная цель и остаток.

**Query**

| Параметр | Тип | Обяз. | Описание |
|----------|-----|-------|----------|
| `date` | LocalDate | нет | Календарный день в зоне `zone`. Если не передан — **сегодня** в этой зоне (`LocalDate.now(zone)`). |
| `zone` | string (ZoneId) | нет | IANA, например `Europe/Minsk`. Пустое / не передано — **`UTC`**. Некорректный идентификатор — **`400`**. |

**Цель калорий (`targetCaloriesKcal`):** если в профиле задано **`dailyCalorieTargetKcal`**, используется оно (**`calorieTargetSource`:** `EXPLICIT_DAILY_TARGET`); иначе **`goalAdjustedDailyCalories`** из расчёта BMR (**`BMR_GOAL_ADJUSTED`**), как у **`GET /api/me/bmr`**.

**Остаток:** `remainingCaloriesKcal` = `targetCaloriesKcal` − сумма `totalCaloriesKcal` за день (может быть отрицательным при перевыполнении).

**Требуется профиль:** как у **`GET /api/me/profile`** — при отсутствии профиля **`404`**.

**NutritionDailyResponse**

| Поле | Тип |
|------|-----|
| `date` | LocalDate |
| `zone` | string | Нормализованный id зоны (как в `ZoneId`, например `UTC`, `Europe/Minsk`). |
| `totalCaloriesKcal` | number |
| `totalProteinG` | number |
| `totalFatG` | number |
| `totalCarbsG` | number |
| `targetCaloriesKcal` | number |
| `calorieTargetSource` | string | `EXPLICIT_DAILY_TARGET` \| `BMR_GOAL_ADJUSTED` |
| `remainingCaloriesKcal` | number |
| `additionalNutrientsTotals` | массив **AdditionalNutrientTotalResponse** |

Зеркало: **`GET /api/users/{userId}/nutrition/daily`** — те же query-параметры, доступ как у остального дерева `.../nutrition/...`.

---

## `/api/me/goals` — цели

**Требуется JWT.**

### `POST /api/me/goals` — `201`, UserGoalResponse

**Тело — CreateGoalRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `goalType` | string | да | enum `GoalType` |
| `title` | string | да | не пустой, max 120 |
| `initialValue` | number | да | ≥ 0 |
| `targetValue` | number | да | ≥ 0 |
| `startDate` | LocalDate | да | — |
| `targetDate` | LocalDate | нет | если задана: не раньше `startDate`; на уровне валидации — «сегодня или позже» (`@FutureOrPresent`) для целевой даты |

### `GET /api/me/goals` — `200`, массив UserGoalResponse

### `GET /api/me/goals/{goalId}` — `200`, UserGoalResponse

### `PUT /api/me/goals/{goalId}/status` — `200`, UserGoalResponse

**Тело — UpdateGoalStatusRequest**

| Поле | Тип | Обяз. |
|------|-----|-------|
| `status` | string | enum `GoalStatus` |

### `POST /api/me/goals/{goalId}/progress` — `200`, UserGoalResponse

**Тело — AddGoalProgressRequest**

| Поле | Тип | Обяз. | Ограничения |
|------|-----|-------|-------------|
| `value` | number | да | ≥ 0 |
| `note` | string | нет | max 500 |

### `GET /api/me/goals/{goalId}/progress` — `200`, массив GoalProgressResponse

| Поле | Тип |
|------|-----|
| `id` | UUID |
| `value` | number |
| `note` | string \| null |
| `recordedAt` | OffsetDateTime |

**UserGoalResponse:** `id`, `goalType`, `status`, `title`, `initialValue`, `targetValue`, `currentValue`, `progressPercent`, `startDate`, `targetDate`, `createdAt`, `updatedAt`.

---

## `/api/foods` — поиск и сведения о продуктах (USDA FDC, прокси)

**Требуется JWT.** Ответы зависят от внешнего API; при ошибках возможны статусы из раздела [Коды ответов](#коды-ответов-и-типичные-ситуации) и поле `upstreamError`.

Дополнительно: сервер **не возвращает** продукты, у которых не найден хотя бы один из макронутриентов **белки/жиры/углеводы** (поля `proteinG`, `fatG`, `carbsG`). В выдачу **поиска и списков** (`/search`, `/list`) также **не попадают** продукты без **положительной** калорийности на 100 г: если `caloriesKcal` **отсутствует** (`null`) **или** ≤ 0, позиция отфильтровывается, даже при полных БЖУ.

### `GET /api/foods/search` — `200`, FoodSearchResponse

**Query**

| Параметр | Обяз. | Описание |
|----------|-------|----------|
| `query` | да | не пустой |
| `pageNumber` | нет | по умолчанию 1, ≥ 1 |
| `pageSize` | нет | по умолчанию 25, 1–200 |
| `dataType` | нет | список строк (повторяющийся query-параметр) |
| `sortBy` | нет | string |
| `sortOrder` | нет | `asc` или `desc` (регистронезависимо) |
| `brandOwner` | нет | string |

### `POST /api/foods/search` — `200`, FoodSearchResponse

**Тело — FoodSearchRequest**

| Поле | Обяз. | Ограничения |
|------|-------|-------------|
| `query` | да | 1–200 символов |
| `dataType` | нет | список строк |
| `pageSize` | нет | 1–200 |
| `pageNumber` | нет | ≥ 1 |
| `sortBy` | нет | — |
| `sortOrder` | нет | `asc` / `desc` |
| `brandOwner` | нет | — |

### `GET /api/foods/list` — `200`, массив FoodSummaryResponse

**Query:** `pageNumber` (по умолч. 1, ≥ 1), `pageSize` (по умолч. 25, 1–200), `dataType` (список), `sortBy` (опц.).

### `POST /api/foods/list` — `200`, массив FoodSummaryResponse

**Тело — FoodListRequest:** `dataType` (опц.), `pageSize` (1–200, опц.), `pageNumber` (≥ 1, опц.), `sortBy` (опц.).

### `GET /api/foods/{fdcId}` — `200`, FoodDetailsResponse

**Path:** `fdcId` — long **> 0**.

**Query (необязательны):**

| Параметр | Описание |
|----------|----------|
| `format` | `abridged` или `full` (регистронезависимо) |
| `nutrients` | до 100 положительных long (список) |

### `GET /api/foods` — `200`, массив FoodDetailsResponse (lookup по списку id)

**Обязательный query:** `fdcIds` — от 1 до 50 значений, каждый **> 0**.

**Необязательно:** `format` (`abridged`/`full`), `nutrients` (до 100 положительных long).

### `POST /api/foods/lookup` — `200`, массив FoodDetailsResponse

**Тело — FoodLookupRequest**

| Поле | Обяз. | Ограничения |
|------|-------|-------------|
| `fdcIds` | да | 1–50 элементов, каждый > 0 |
| `format` | нет | max 16 символов, `abridged` или `full` |
| `nutrients` | нет | до 100 положительных long |

### `POST /api/foods` — то же, что `POST /api/foods/lookup`

Тот же JSON **FoodLookupRequest** и тот же ответ. Дублирующий маршрут в коде контроллера.

### Типы ответов (еда)

**FoodSearchResponse:** `totalHits`, `currentPage`, `totalPages`, `foods` (массив FoodSummaryResponse).

**FoodSummaryResponse:** `fdcId`, `description`, `dataType`, `brandOwner`, `servingSize`, `servingSizeUnit`, `nutrients` (FoodNutrientsResponse).

**FoodDetailsResponse:** как summary плюс `ingredients`, `portions` (массив FoodPortionResponse).

**FoodNutrientsResponse:** `caloriesKcal`, `proteinG`, `fatG`, `carbsG` (все nullable) + `additionalNutrients` (массив).

**FoodNutrientEntry (additionalNutrients[]):**

| Поле | Тип | Описание |
|------|-----|----------|
| `name` | string | Название нутриента |
| `amount` | number | Количество (как отдаёт провайдер) |
| `unitName` | string \| null | Единица измерения |
| `number` | string \| null | Код нутриента (если доступен) |

**FoodPortionResponse:** `amount`, `gramWeight`, `description` (nullable).

---

## Справочник enum (домен)

### FinanceOperationType

- `INCOME`
- `EXPENSE`

### Sex

- `MALE`
- `FEMALE`

### ActivityLevel

Значения перечисления (множитель хранится в модели, в JSON уходит только имя константы):

- `SEDENTARY` — 1.2
- `LIGHT` — 1.375
- `MODERATE` — 1.55
- `HIGH` — 1.725
- `VERY_HIGH` — 1.9

### CalorieGoal

Суточная цель по калориям влияет на рассчитанную норму (смещение в ккал):

- `WEIGHT_LOSS` — -600
- `SLOW_WEIGHT_LOSS` — -300
- `MAINTAIN` — 0
- `WEIGHT_GAIN` — +300
- `FAST_WEIGHT_GAIN` — +600

### GoalType

- `WEIGHT_KG`
- `DAILY_CALORIES`
- `DAILY_PROTEIN_G`
- `DAILY_FAT_G`
- `DAILY_CARBS_G`

### GoalStatus

- `ACTIVE`
- `COMPLETED`
- `ARCHIVED`

---

## Связь с OpenAPI

- **JSON OpenAPI 3**: `GET /v3/api-docs` (путь из `springdoc.api-docs.path`).
- **Swagger UI**: `/swagger-ui.html` (из `springdoc.swagger-ui.path` в `application.properties`).

Этот файл — человекочитаемое описание «в одном месте»; при изменении DTO и контроллеров документ нужно обновлять вручную или сверять со сгенерированной схемой OpenAPI.
