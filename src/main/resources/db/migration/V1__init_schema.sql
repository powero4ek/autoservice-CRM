
-- ============================================================
-- CRM АВТОСЕРВИС — Базовая схема PostgreSQL
-- Версия: 1.0 (MVP для практики)
-- ============================================================

-- Расширения
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 1. ПЕРЕЧИСЛЕНИЯ (ENUM)
-- ============================================================

DO $$ BEGIN
    CREATE TYPE order_status AS ENUM (
        'CREATED',        -- Создан
        'IN_PROGRESS',    -- В работе
        'WAITING_PARTS',  -- Ожидание запчастей
        'COMPLETED',      -- Выполнен
        'CLOSED'          -- Закрыт (оплачен)
    );
EXCEPTION WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE item_type AS ENUM ('SERVICE', 'PART');
EXCEPTION WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE transaction_type AS ENUM (
        'IN',          -- Приход на склад
        'OUT',         -- Расход по наряду
        'ADJUSTMENT'   -- Корректировка (возврат, инвентаризация)
    );
EXCEPTION WHEN duplicate_object THEN null;
END $$;

-- ============================================================
-- 2. ТАБЛИЦЫ
-- ============================================================

-- Роли пользователей (гибкая модель, не хардкод в приложении)
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions JSONB NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE roles IS 'Роли пользователей с правами в формате JSONB';

-- Пользователи системы
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE users IS 'Пользователи системы (админ, менеджер, механик)';

-- Клиенты автосервиса
CREATE TABLE IF NOT EXISTS clients (
    id         BIGSERIAL PRIMARY KEY,
    full_name  VARCHAR(150) NOT NULL,
    phone      VARCHAR(20) NOT NULL,
    email      VARCHAR(100),
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE clients IS 'Клиенты автосервиса';

-- Автомобили клиентов
CREATE TABLE IF NOT EXISTS vehicles (
    id             BIGSERIAL PRIMARY KEY,
    client_id      BIGINT NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    brand          VARCHAR(50) NOT NULL,
    model          VARCHAR(50) NOT NULL,
    year           INTEGER CHECK (year BETWEEN 1900 AND EXTRACT(YEAR FROM CURRENT_DATE) + 1),
    vin            VARCHAR(17) UNIQUE,
    license_plate  VARCHAR(20) NOT NULL,
    color          VARCHAR(30),
    mileage        INTEGER CHECK (mileage >= 0),
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE vehicles IS 'Автомобили клиентов (1 клиент → N авто)';

-- Поставщики запчастей
CREATE TABLE IF NOT EXISTS suppliers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    phone      VARCHAR(20),
    email      VARCHAR(100),
    address    VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE suppliers IS 'Поставщики автозапчастей';

-- Справочник услуг
CREATE TABLE IF NOT EXISTS services (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    category        VARCHAR(50),
    default_price   NUMERIC(12,2) NOT NULL CHECK (default_price >= 0),
    estimated_hours NUMERIC(4,1) CHECK (estimated_hours > 0),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE services IS 'Справочник работ и услуг автосервиса';

-- Склад запчастей
CREATE TABLE IF NOT EXISTS parts (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    sku              VARCHAR(50) UNIQUE,
    category         VARCHAR(50),
    quantity_in_stock INTEGER NOT NULL DEFAULT 0 CHECK (quantity_in_stock >= 0),
    min_stock_level   INTEGER NOT NULL DEFAULT 5 CHECK (min_stock_level >= 0),
    purchase_price    NUMERIC(12,2) CHECK (purchase_price >= 0),
    sale_price        NUMERIC(12,2) CHECK (sale_price >= 0),
    supplier_id       BIGINT REFERENCES suppliers(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE parts IS 'Склад автозапчастей с контролем минимального остатка';

-- Заказ-наряды (ядро системы)
CREATE TABLE IF NOT EXISTS work_orders (
    id                  BIGSERIAL PRIMARY KEY,
    order_number        VARCHAR(20) UNIQUE NOT NULL,
    vehicle_id          BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT,
    client_id           BIGINT NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    status              order_status NOT NULL DEFAULT 'CREATED',
    description         TEXT NOT NULL DEFAULT '',
    total_cost          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_cost >= 0),
    assigned_mechanic_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_by          BIGINT NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    closed_by           BIGINT REFERENCES users(id) ON DELETE SET NULL,
    closed_at           TIMESTAMPTZ,

    -- Бизнес-логика: даты должны идти последовательно
    CONSTRAINT valid_dates CHECK (
        (completed_at IS NULL OR completed_at >= created_at) AND
        (closed_at IS NULL OR (closed_at >= created_at AND (completed_at IS NULL OR closed_at >= completed_at)))
    )
);

COMMENT ON TABLE work_orders IS 'Заказ-наряды (основная бизнес-сущность)';

-- Позиции наряда (работы + запчасти)
CREATE TABLE IF NOT EXISTS work_order_items (
    id              BIGSERIAL PRIMARY KEY,
    work_order_id   BIGINT NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    item_type       item_type NOT NULL,
    service_id      BIGINT REFERENCES services(id) ON DELETE RESTRICT,
    part_id         BIGINT REFERENCES parts(id) ON DELETE RESTRICT,
    quantity        NUMERIC(8,2) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price      NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    total_price     NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    notes           VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Проверка: либо услуга, либо запчасть, но не оба и не ноль
    CONSTRAINT valid_item_type CHECK (
        (item_type = 'SERVICE' AND service_id IS NOT NULL AND part_id IS NULL) OR
        (item_type = 'PART' AND part_id IS NOT NULL AND service_id IS NULL)
    )
);

COMMENT ON TABLE work_order_items IS 'Позиции наряда: услуги и запчасти';

-- Складские движения (единый источник правды для остатков)
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id              BIGSERIAL PRIMARY KEY,
    part_id         BIGINT NOT NULL REFERENCES parts(id) ON DELETE RESTRICT,
    transaction_type transaction_type NOT NULL,
    quantity        NUMERIC(8,2) NOT NULL CHECK (quantity > 0),
    reason          VARCHAR(255),
    work_order_id   BIGINT REFERENCES work_orders(id) ON DELETE SET NULL,
    created_by      BIGINT NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE inventory_transactions IS 'Складские движения: приход, расход, корректировка';

-- История изменения статусов нарядов (аудит)
CREATE TABLE IF NOT EXISTS status_history (
    id           BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    old_status   order_status,
    new_status   order_status NOT NULL,
    changed_by   BIGINT NOT NULL REFERENCES users(id),
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    comment      VARCHAR(255)
);

COMMENT ON TABLE status_history IS 'Аудит изменения статусов заказ-нарядов';

-- Сессии пользователей (для аудита входов)
CREATE TABLE IF NOT EXISTS user_sessions (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE user_sessions IS 'Активные сессии пользователей десктоп-приложения';

-- ============================================================
-- 3. ИНДЕКСЫ (производительность)
-- ============================================================

CREATE INDEX idx_vehicles_client ON vehicles(client_id);
CREATE INDEX idx_work_orders_status ON work_orders(status);
CREATE INDEX idx_work_orders_mechanic ON work_orders(assigned_mechanic_id);
CREATE INDEX idx_work_orders_dates ON work_orders(created_at, completed_at);
CREATE INDEX idx_work_order_items_order ON work_order_items(work_order_id);
CREATE INDEX idx_inventory_part ON inventory_transactions(part_id);
CREATE INDEX idx_inventory_order ON inventory_transactions(work_order_id);
CREATE INDEX idx_status_history_order ON status_history(work_order_id);
CREATE INDEX idx_parts_stock ON parts(quantity_in_stock) WHERE quantity_in_stock <= min_stock_level;

-- ============================================================
-- 4. ТРИГГЕРНЫЕ ФУНКЦИИ
-- ============================================================

-- 4.1 Автоматическая запись в историю статусов
CREATE OR REPLACE FUNCTION fn_log_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO status_history (work_order_id, old_status, new_status, changed_by, comment)
        VALUES (NEW.id, OLD.status, NEW.status, COALESCE(NEW.closed_by, NEW.created_by), 'Автоматическая запись при смене статуса');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4.2 Единая точка изменения складских остатков
-- Все изменения остатков происходят ТОЛЬКО через эту функцию
CREATE OR REPLACE FUNCTION fn_update_stock_from_transaction()
RETURNS TRIGGER AS $$
DECLARE
    v_current_stock INTEGER;
BEGIN
    SELECT quantity_in_stock INTO v_current_stock FROM parts WHERE id = NEW.part_id FOR UPDATE;

    IF NEW.transaction_type = 'IN' THEN
        UPDATE parts SET quantity_in_stock = v_current_stock + NEW.quantity WHERE id = NEW.part_id;
    ELSIF NEW.transaction_type = 'OUT' THEN
        IF v_current_stock < NEW.quantity THEN
            RAISE EXCEPTION 'Недостаточно запчастей на складе. Требуется: %, доступно: %', NEW.quantity, v_current_stock;
        END IF;
        UPDATE parts SET quantity_in_stock = v_current_stock - NEW.quantity WHERE id = NEW.part_id;
    ELSIF NEW.transaction_type = 'ADJUSTMENT' THEN
        UPDATE parts SET quantity_in_stock = v_current_stock + NEW.quantity WHERE id = NEW.part_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4.3 Автоматический расход при добавлении запчасти в наряд
CREATE OR REPLACE FUNCTION fn_create_out_transaction_from_item()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.item_type = 'PART' THEN
        INSERT INTO inventory_transactions (part_id, transaction_type, quantity, reason, work_order_id, created_by)
        VALUES (
            NEW.part_id, 
            'OUT', 
            NEW.quantity, 
            'Расход по наряду #' || NEW.work_order_id,
            NEW.work_order_id,
            (SELECT created_by FROM work_orders WHERE id = NEW.work_order_id)
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4.4 Возврат запчасти на склад при удалении позиции наряда
CREATE OR REPLACE FUNCTION fn_create_return_transaction_on_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.item_type = 'PART' THEN
        INSERT INTO inventory_transactions (part_id, transaction_type, quantity, reason, work_order_id, created_by)
        VALUES (
            OLD.part_id,
            'ADJUSTMENT',
            OLD.quantity,
            'Возврат при удалении позиции из наряда #' || OLD.work_order_id,
            OLD.work_order_id,
            (SELECT created_by FROM work_orders WHERE id = OLD.work_order_id)
        );
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- 4.5 Пересчёт итоговой стоимости наряда
CREATE OR REPLACE FUNCTION fn_recalculate_order_total()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id BIGINT;
    v_new_total NUMERIC(12,2);
BEGIN
    v_order_id := COALESCE(NEW.work_order_id, OLD.work_order_id);

    SELECT COALESCE(SUM(total_price), 0) INTO v_new_total
    FROM work_order_items
    WHERE work_order_id = v_order_id;

    UPDATE work_orders SET total_cost = v_new_total WHERE id = v_order_id;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 4.6 Генерация номера наряда (формат ORD-YYYY-NNNNN)
CREATE OR REPLACE FUNCTION fn_generate_order_number()
RETURNS TRIGGER AS $$
DECLARE
    v_next_id INTEGER;
    v_year INTEGER;
BEGIN
    v_year := EXTRACT(YEAR FROM CURRENT_DATE);
    SELECT COALESCE(MAX(id), 0) + 1 INTO v_next_id FROM work_orders;
    NEW.order_number := 'ORD-' || v_year || '-' || LPAD(v_next_id::TEXT, 5, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 5. ТРИГГЕРЫ
-- ============================================================

CREATE TRIGGER trg_status_history
    AFTER UPDATE OF status ON work_orders
    FOR EACH ROW
    EXECUTE FUNCTION fn_log_status_change();

CREATE TRIGGER trg_inventory_update_stock
    AFTER INSERT ON inventory_transactions
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_stock_from_transaction();

CREATE TRIGGER trg_work_order_item_out
    AFTER INSERT ON work_order_items
    FOR EACH ROW
    EXECUTE FUNCTION fn_create_out_transaction_from_item();

CREATE TRIGGER trg_work_order_item_return
    AFTER DELETE ON work_order_items
    FOR EACH ROW
    EXECUTE FUNCTION fn_create_return_transaction_on_delete();

CREATE TRIGGER trg_recalculate_total
    AFTER INSERT OR UPDATE OR DELETE ON work_order_items
    FOR EACH ROW
    EXECUTE FUNCTION fn_recalculate_order_total();

CREATE TRIGGER trg_generate_order_number
    BEFORE INSERT ON work_orders
    FOR EACH ROW
    EXECUTE FUNCTION fn_generate_order_number();

-- ============================================================
-- 6. ДЕМО-ДАННЫЕ
-- ============================================================

INSERT INTO roles (name, description, permissions) VALUES
('Администратор', 'Полный доступ ко всем модулям системы', '{"users": ["read", "write", "delete"], "reports": ["read"], "orders": ["read", "write", "delete"], "parts": ["read", "write", "delete"], "clients": ["read", "write", "delete"]}'),
('Менеджер', 'Приём заказов, работа с клиентами, отчёты', '{"users": [], "reports": ["read"], "orders": ["read", "write"], "parts": ["read"], "clients": ["read", "write"]}'),
('Механик', 'Выполнение работ, просмотр назначенных нарядов', '{"users": [], "reports": [], "orders": ["read", "write"], "parts": ["read"], "clients": ["read"]}');

-- Пароли захешированы через BCrypt (строка 'password' для демо, в проде — обязательно хешировать в приложении)
-- Хеш ниже соответствует паролю 'password' (10 раундов)
INSERT INTO users (role_id, username, password_hash, full_name, phone, is_active) VALUES
(1, 'admin', '$2a$10$u/HVFD2VzMgFNlfzfzbuPeR6RQFr9V2X8HT7pVoG7CkOrqEfDlynu', 'Иванов Алексей Петрович', '+7(900)111-22-33', TRUE),
(2, 'manager', '$2a$10$u/HVFD2VzMgFNlfzfzbuPeR6RQFr9V2X8HT7pVoG7CkOrqEfDlynu', 'Петрова Мария Сергеевна', '+7(900)222-33-44', TRUE),
(3, 'mechanic', '$2a$10$u/HVFD2VzMgFNlfzfzbuPeR6RQFr9V2X8HT7pVoG7CkOrqEfDlynu', 'Сидоров Дмитрий Викторович', '+7(900)333-44-55', TRUE);

INSERT INTO clients (full_name, phone, email, notes) VALUES
('Смирнов Андрей Иванович', '+7(900)444-55-66', 'smirnov@mail.ru', 'Постоянный клиент, предпочитает оригинальные запчасти'),
('Кузнецова Елена Дмитриевна', '+7(900)555-66-77', 'elena.k@yandex.ru', 'Требует срочный ремонт'),
('Попов Максим Олегович', '+7(900)666-77-88', NULL, 'Корпоративный клиент (3 авто)');

INSERT INTO vehicles (client_id, brand, model, year, vin, license_plate, color, mileage, notes) VALUES
(1, 'Toyota', 'Camry', 2019, 'JTNB11HK305123456', 'А123БВ777', 'Чёрный', 45000, 'Коробка автомат'),
(1, 'Kia', 'Rio', 2020, 'XWEPC81BCL0001234', 'В456КМ799', 'Белый', 32000, NULL),
(2, 'Hyundai', 'Creta', 2021, 'Z94CT41DBMR123456', 'Е789НО197', 'Серебристый', 18000, 'Первое ТО'),
(3, 'Volkswagen', 'Polo', 2018, 'XW8ZZZ61ZJ0009876', 'К012РС777', 'Синий', 67000, 'Трещина на лобовом'),
(3, 'Ford', 'Focus', 2017, 'WF0NXXGCDNS123456', 'М345ТУ799', 'Красный', 89000, 'Стук в подвеске');

INSERT INTO suppliers (name, phone, email, address) VALUES
('ООО "АвтоПартс"', '+7(495)123-45-67', 'info@autoparts.ru', 'г. Москва, ул. Автозаводская, 23'),
('ИП Козлов С.А.', '+7(495)987-65-43', 'kozlov@mail.ru', 'г. Москва, ш. Энтузиастов, 56, склад Б');

INSERT INTO services (name, category, default_price, estimated_hours, is_active) VALUES
('Замена масла ДВС', 'ТО', 2500.00, 0.5, TRUE),
('Замена тормозных колодок (перед)', 'Тормозная система', 3500.00, 1.0, TRUE),
('Диагностика подвески', 'Диагностика', 1500.00, 1.0, TRUE),
('Шиномонтаж (4 колеса)', 'Шиномонтаж', 2000.00, 1.0, TRUE),
('Замена свечей зажигания', 'ДВС', 1800.00, 0.8, TRUE);

INSERT INTO parts (name, sku, category, quantity_in_stock, min_stock_level, purchase_price, sale_price, supplier_id) VALUES
('Масло моторное 5W-30 (4л)', 'OIL-5W30-001', 'Масла', 24, 10, 850.00, 1200.00, 1),
('Фильтр масляный', 'FIL-OIL-002', 'Фильтры', 15, 8, 180.00, 350.00, 1),
('Тормозные колодки передние (к-кт)', 'BRK-PAD-003', 'Тормоза', 6, 4, 1200.00, 2200.00, 1),
('Свеча зажигания NGK', 'NGK-PLUG-004', 'ДВС', 20, 12, 220.00, 450.00, 2),
('Масляный поддон прокладка', 'GSK-OIL-005', 'Прокладки', 8, 5, 90.00, 250.00, 1),
('Антифриз G12 (1л)', 'COOL-G12-006', 'Охлаждение', 30, 15, 180.00, 320.00, 1),
('Шаровая опора (передняя)', 'SUS-BALL-007', 'Подвеска', 4, 3, 450.00, 950.00, 2),
('Ступичный подшипник', 'BRG-HUB-008', 'Подшипники', 5, 3, 650.00, 1400.00, 2),
('Ремень ГРМ', 'BELT-TIM-009', 'Ремни', 3, 2, 1100.00, 2400.00, 1),
('Помпа охлаждающей жидкости', 'PMP-COOL-010', 'Охлаждение', 4, 2, 800.00, 1800.00, 1);

-- ============================================================
-- 7. ПРЕДСТАВЛЕНИЯ (VIEW) для удобства разработки
-- ============================================================

-- Полная информация по наряду (для UI-списка)
CREATE OR REPLACE VIEW v_work_orders_full AS
SELECT 
    wo.id,
    wo.order_number,
    wo.status,
    wo.total_cost,
    wo.created_at,
    wo.completed_at,
    c.full_name AS client_name,
    c.phone AS client_phone,
    v.brand || ' ' || v.model AS vehicle_name,
    v.license_plate,
    m.full_name AS mechanic_name,
    creator.full_name AS created_by_name
FROM work_orders wo
JOIN clients c ON wo.client_id = c.id
JOIN vehicles v ON wo.vehicle_id = v.id
LEFT JOIN users m ON wo.assigned_mechanic_id = m.id
JOIN users creator ON wo.created_by = creator.id;

-- Запчасти с критическим остатком
CREATE OR REPLACE VIEW v_low_stock_parts AS
SELECT 
    p.*,
    s.name AS supplier_name
FROM parts p
LEFT JOIN suppliers s ON p.supplier_id = s.id
WHERE p.quantity_in_stock <= p.min_stock_level;

-- ============================================================
-- 8. КОНЕЦ СКРИПТА
-- ============================================================
